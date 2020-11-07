package de.aaaaaaah.velcom.backend.access;

import de.aaaaaaah.velcom.backend.access.archives.BenchRepoArchive;
import de.aaaaaaah.velcom.backend.access.archives.RepoArchiveManager;
import de.aaaaaaah.velcom.backend.access.archives.TarArchiveManager;
import de.aaaaaaah.velcom.backend.access.exceptions.AddRepoException;
import de.aaaaaaah.velcom.backend.access.exceptions.MalformedRepoException;
import de.aaaaaaah.velcom.backend.access.exceptions.PrepareTransferException;
import de.aaaaaaah.velcom.backend.access.exceptions.RepoAccessException;
import de.aaaaaaah.velcom.backend.access.exceptions.TransferException;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning.CloneException;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.AddRepositoryException;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import de.aaaaaaah.velcom.backend.util.TransferUtils;
import io.micrometer.core.annotation.Timed;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import javax.annotation.Nullable;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the benchmark repository and all kinds of archives.
 */
public class ArchiveAccess {

	//.../archive_root_dir/
	//.../archive_root_dir/benchrepo/                               (<- copy clone)
	//.../archive_root_dir/benchrepo.tar
	//.../archive_root_dir/tars/<tar_id>.tar
	//.../archive_root_dir/tars/...
	//.../archive_root_dir/repos/<repo_dir_name>_<commit_hash>/     (<- copy clone)
	//.../archive_root_dir/repos/...

	private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveAccess.class);
	private static final String BENCH_REPO_DIR_NAME = "benchrepo";

	private final RepoStorage repoStorage;

	private final BenchRepoArchive benchRepoArchive;
	private final RepoArchiveManager repoArchives;
	private final TarArchiveManager tarArchives;

	private final Object benchRepoLock = new Object();
	private final RemoteUrl benchRepoUrl;
	@Nullable
	private CommitHash currentBenchRepoHash;

	public ArchiveAccess(Path archivesRootDir, RemoteUrl benchRepoUrl, RepoStorage repoStorage)
		throws IOException, MalformedRepoException {

		this.repoStorage = repoStorage;

		this.repoArchives = new RepoArchiveManager(archivesRootDir.resolve("repos"), repoStorage);

		this.tarArchives = new TarArchiveManager(archivesRootDir.resolve("tars"));

		this.benchRepoArchive = new BenchRepoArchive(
			archivesRootDir.resolve("benchrepo"),
			archivesRootDir.resolve("benchrepo.tar")
		);

		this.benchRepoUrl = benchRepoUrl;

		// Check if we have a bench repo with a valid hash
		if (repoStorage.containsRepository(BENCH_REPO_DIR_NAME)) {
			try {
				currentBenchRepoHash = loadLatestBenchRepoHash();
			} catch (Exception e) {
				LOGGER.warn("Failed to get latest commit hash from benchmark repo. "
					+ "Trying to re-clone it to maybe fix it.");
				repoStorage.deleteRepository(BENCH_REPO_DIR_NAME);
			}
		}

		// Initialize bench repo if it doesn't exist
		if (!repoStorage.containsRepository(BENCH_REPO_DIR_NAME)) {
			LOGGER.info("Cloning bench repo...");
			try {
				repoStorage.addRepository(BENCH_REPO_DIR_NAME, benchRepoUrl.getUrl());
				LOGGER.info("Bench repo successfully cloned.");
			} catch (AddRepositoryException e) {
				throw new AddRepoException(BENCH_REPO_DIR_NAME, benchRepoUrl,
					"failed to clone benchmark repo from: " + benchRepoUrl, e);
			}
		}

		// Try again to acquire bench repo hash, if we didn't already get it earlier
		if (currentBenchRepoHash == null) {
			try {
				currentBenchRepoHash = loadLatestBenchRepoHash();
			} catch (Exception e) {
				LOGGER.error("Failed to get latest commit hash from benchmark repo. "
					+ "Please make sure that the benchmark repository has at least one commit.");
				throw new MalformedRepoException("bench repo has no commits in its default branch");
			}
		}

		// Clean up any archives that exist at this moment
		benchRepoArchive.delete();
		repoArchives.deleteAll();
		tarArchives.deleteAll();
	}

	public String getBenchRepoDirName() {
		return BENCH_REPO_DIR_NAME;
	}

	public CommitHash getBenchRepoCommitHash() {
		return currentBenchRepoHash;
	}

	/**
	 * Updates the benchmark repo by checking if there is a new head commit.
	 *
	 * @throws RepoAccessException if an error occurred while trying to update the bench repo
	 */
	public void updateBenchRepo() throws RepoAccessException {
		synchronized (this.benchRepoLock) {
			try {
				// 1.) Check if bench repo is currently in repo storage and clone again if not
				if (!repoStorage.containsRepository(BENCH_REPO_DIR_NAME)) {
					LOGGER.info("Missing benchmark repo on disk! Cloning it from: {}", benchRepoUrl);
					this.benchRepoArchive.delete(); // just to be safe
					this.repoStorage.addRepository(BENCH_REPO_DIR_NAME, benchRepoUrl.getUrl());
				}

				// 2.) Check if remote url has changed and reclone if that is the case
				String localRemoteUrl = loadBenchmarkRepoRemoteUrl();

				if (!localRemoteUrl.equals(this.benchRepoUrl.getUrl())) {
					LOGGER.info("Remote url for benchmark repo has changed! Cloning it again...");
					this.benchRepoArchive.delete();
					this.repoStorage.deleteRepository(BENCH_REPO_DIR_NAME);
					this.repoStorage.addRepository(BENCH_REPO_DIR_NAME, this.benchRepoUrl.getUrl());
				}

				// 3.) Check if commit hash has changed and delete archives if that is the case
				CommitHash oldHash = getBenchRepoCommitHash();

				try (Repository repo = repoStorage.acquireRepository(BENCH_REPO_DIR_NAME)) {
					GuickCloning.getInstance().updateBareRepo(repo.getDirectory().toPath());
				}

				CommitHash newHash = loadLatestBenchRepoHash();

				if (!oldHash.equals(newHash)) {
					LOGGER.info("Commit hash for benchmark repo has changed to: {}", newHash);
					this.benchRepoArchive.delete();
					this.currentBenchRepoHash = newHash;
				}
			} catch (RepositoryAcquisitionException | AddRepositoryException | IOException |
				CloneException e) {

				throw new RepoAccessException(
					"failed to fetch/clone benchmark repo with remote url: " + benchRepoUrl, e
				);
			}
		}
	}

	/**
	 * Transfers the benchmark repository to the provided output stream.
	 *
	 * <p>Note that the provided output stream will be closed after the transfer operation is
	 * done.</p>
	 *
	 * @param outputStream the output stream
	 * @throws PrepareTransferException if something goes wrong trying to prepare the transfer
	 * @throws TransferException if the transfer itself fails
	 */
	@Timed(histogram = true)
	public void transferBenchRepo(OutputStream outputStream)
		throws PrepareTransferException, TransferException {

		synchronized (this.benchRepoLock) {
			// 1.) Create archive if necessary
			try {
				this.benchRepoArchive.createIfNecessary(
					repoStorage, BENCH_REPO_DIR_NAME, getBenchRepoCommitHash()
				);
			} catch (Exception e) {
				this.benchRepoArchive.delete();
				throw new PrepareTransferException("Failed to prepare bench repo for transfer", e);
			}

			// 2.) Transfer bench repo
			try {
				Path tarFile = this.benchRepoArchive.getTar();
				TransferUtils.transferTar(tarFile, outputStream);
			} catch (Exception e) {
				this.benchRepoArchive.delete();
				throw new TransferException("Failed to transfer bench repo", e);
			}
		}
	}

	/**
	 * Transfers the given task to the provided {@link OutputStream}.
	 *
	 * <p>Note that the provided output stream will be closed after the transfer operation is
	 * done.</p>
	 *
	 * @param task the task to transfer
	 * @param outputStream the output stream
	 * @throws PrepareTransferException if something goes wrong trying to prepare the transfer
	 * @throws TransferException if the transfer itself fails
	 */
	@Timed(histogram = true)
	public void transferTask(Task task, OutputStream outputStream)
		throws PrepareTransferException, TransferException {

		if (task.getSource().isRight()) {
			throw new PrepareTransferException("tar transfers unsupported");
		} else {
			RepoId repoId = task.getSource().getLeft().get().getRepoId();
			CommitHash hash = task.getSource().getLeft().get().getHash();

			// 1.) Create archive
			Path archive;

			try {
				archive = repoArchives.createIfNecessary(repoId, hash);
			} catch (Exception e) {
				throw new PrepareTransferException(task, e);
			}

			// 2.) Transfer with tar
			try {
				TransferUtils.tarRepo(archive, outputStream);
			} catch (IOException e) {
				throw new TransferException(task, e);
			} finally {
				repoArchives.deleteArchive(repoId, hash); // Cleanup
			}
		}
	}

	private CommitHash loadLatestBenchRepoHash()
		throws IllegalStateException, RepositoryAcquisitionException, IOException {

		synchronized (this.benchRepoLock) {
			try (Repository localRepo = repoStorage.acquireRepository(BENCH_REPO_DIR_NAME)) {
				ObjectId refPtr = localRepo.resolve(Constants.HEAD);

				if (refPtr == null) {
					throw new IllegalStateException(
						"could not find latest bench repo hash because refPtr is null");
				}

				return new CommitHash(refPtr.getName()); // returns sha-1 hash
			}
		}
	}

	private String loadBenchmarkRepoRemoteUrl() throws RepositoryAcquisitionException {
		synchronized (this.benchRepoLock) {
			try (Repository repo = repoStorage.acquireRepository(BENCH_REPO_DIR_NAME)) {
				return repo.getConfig().getString("remote", "origin", "url");
			}
		}
	}

}
