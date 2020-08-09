package de.aaaaaaah.velcom.backend.access;

import de.aaaaaaah.velcom.backend.util.TransferUtils;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.entities.TarSource;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.exceptions.AddRepoException;
import de.aaaaaaah.velcom.backend.access.exceptions.PrepareTransferException;
import de.aaaaaaah.velcom.backend.access.exceptions.RepoAccessException;
import de.aaaaaaah.velcom.backend.access.exceptions.TransferException;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning.CloneException;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.AddRepositoryException;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

	private final Path repoArchivesRootDir;
	private final Path tarArchivesRootDir;
	private final RepoStorage repoStorage;

	private final Object benchRepoLock = new Object();
	private final RemoteUrl benchRepoUrl;
	private final Path benchRepoClonePath;
	private final Path benchRepoTarPath;
	private CommitHash currentBenchRepoHash;

	public ArchiveAccess(Path archivesRootDir, RemoteUrl benchRepoUrl, RepoStorage repoStorage)
		throws IOException {

		this.repoArchivesRootDir = archivesRootDir.resolve("repos");
		this.tarArchivesRootDir = archivesRootDir.resolve("tars");
		this.repoStorage = repoStorage;

		this.benchRepoClonePath = archivesRootDir.resolve("benchrepo");
		this.benchRepoTarPath = archivesRootDir.resolve("benchrepo.tar");
		this.benchRepoUrl = benchRepoUrl;

		// Initialize bench repo if it does not yet exist
		if (!repoStorage.containsRepository(BENCH_REPO_DIR_NAME)) {
			LOGGER.info("Could not find bench repo locally. Cloning...");
			try {
				repoStorage.addRepository(BENCH_REPO_DIR_NAME, benchRepoUrl.getUrl());
				LOGGER.info("Bench repo successfully cloned.");
			} catch (AddRepositoryException e) {
				throw new AddRepoException(BENCH_REPO_DIR_NAME, benchRepoUrl,
					"failed to clone benchmark repo from: " + benchRepoUrl, e);
			}
		}

		// Check if bench repo is valid
		try {
			currentBenchRepoHash = loadLatestBenchRepoHash();
		} catch (Exception e) {
			LOGGER.error("Failed to get latest commit hash from benchmark repo! "
				+ "Please make sure that the benchmark repository has at least one commit.");
		}

		// Clean up any archives that exist at this moment
		FileHelper.deleteDirectoryOrFile(repoArchivesRootDir);
		FileHelper.deleteDirectoryOrFile(tarArchivesRootDir);
		Files.createDirectories(repoArchivesRootDir);
		Files.createDirectories(tarArchivesRootDir);
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
				if (!repoStorage.containsRepository(BENCH_REPO_DIR_NAME)) {
					// bench repo not on disk => clone it
					LOGGER.info("Missing benchmark repo on disk! Cloning it from: {}",
						benchRepoUrl);
					repoStorage.addRepository(BENCH_REPO_DIR_NAME, benchRepoUrl.getUrl());
				}

				// Check if remote url has changed
				String localRemoteUrl = loadBenchmarkRepoRemoteUrl();

				if (!localRemoteUrl.equals(this.benchRepoUrl.getUrl())) {
					// remote url changed! => need to clone repo again and delete old archives
					LOGGER.info("Remote url for benchmark repo has changed! Cloning it again...");
					repoStorage.deleteRepository(BENCH_REPO_DIR_NAME);
					FileHelper.deleteDirectoryOrFile(benchRepoClonePath);
					FileHelper.deleteDirectoryOrFile(benchRepoTarPath);
					repoStorage.addRepository(BENCH_REPO_DIR_NAME, benchRepoUrl.getUrl());
				}

				// Check if commit hash has changed
				CommitHash oldHash = getBenchRepoCommitHash();

				try (Repository repo = repoStorage.acquireRepository(BENCH_REPO_DIR_NAME)) {
					GuickCloning.getInstance().updateBareRepo(repo.getDirectory().toPath());
				}

				CommitHash newHash = loadLatestBenchRepoHash();

				if (!oldHash.equals(newHash)) {
					// commit hash was changed => bench repo was updated => remove old archives
					this.currentBenchRepoHash = newHash;
					FileHelper.deleteDirectoryOrFile(benchRepoClonePath);
					FileHelper.deleteDirectoryOrFile(benchRepoTarPath);
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
	public void transferBenchRepo(OutputStream outputStream)
		throws PrepareTransferException, TransferException {

		synchronized (this.benchRepoLock) {
			try {
				// 1.) Create copy clone
				if (!Files.exists(benchRepoClonePath)) {
					// Create bench repo tar
					TransferUtils.cloneRepo(
						repoStorage, BENCH_REPO_DIR_NAME, benchRepoClonePath,
						getBenchRepoCommitHash()
					);
				}

				// 2.) Create tar file
				if (!Files.exists(benchRepoTarPath)) {
					try {
						OutputStream tarOut = Files.newOutputStream(benchRepoTarPath);
						TransferUtils.tarRepo(benchRepoClonePath, tarOut);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			} catch (Exception e) {
				throw new PrepareTransferException("Failed to prepare bench repo for transfer", e);
			}

			// 3.) Transfer tar
			try {
				TransferUtils.transferTar(benchRepoTarPath, outputStream);
			} catch (IOException e) {
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
	public void transferTask(Task task, OutputStream outputStream)
		throws PrepareTransferException, TransferException {

		if (task.getSource().isRight()) {
			TarSource tarSource = task.getSource().getRight().orElseThrow();
			Path tarPath = tarArchivesRootDir.resolve(tarSource.getTarName());

			try {
				TransferUtils.transferTar(tarPath, outputStream);
			} catch (IOException e) {
				throw new TransferException(task, e);
			}
		} else {
			String dirName = task.getSource().getLeft().get().getRepoId().getDirectoryName();
			CommitHash hash = task.getSource().getLeft().get().getHash();

			// 1.) Create copy clone (and delete old one if there is one)
			Path clonePath = repoArchivesRootDir.resolve(dirName + "_" + hash.getHash());

			try {
				FileHelper.deleteDirectoryOrFile(clonePath);
				TransferUtils.cloneRepo(repoStorage, dirName, clonePath, hash);
			} catch (Exception e) {
				throw new PrepareTransferException(task, e);
			}

			// 2.) Transfer with tar
			try {
				TransferUtils.tarRepo(clonePath, outputStream);
			} catch (IOException e) {
				throw new TransferException(task, e);
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
		try (Repository repo = repoStorage.acquireRepository(BENCH_REPO_DIR_NAME)) {
			return repo.getConfig().getString("remote", "origin", "url");
		}
	}

}
