package de.aaaaaaah.velcom.backend.newaccess.archiveaccess;

import de.aaaaaaah.velcom.backend.newaccess.archiveaccess.exceptions.TarRetrieveException;
import de.aaaaaaah.velcom.backend.newaccess.archiveaccess.exceptions.TarTransferException;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning.CloneException;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import de.aaaaaaah.velcom.backend.storage.tar.TarFileStorage;
import de.aaaaaaah.velcom.backend.util.TransferUtils;
import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An access for retrieving tar archives for tasks and for the bench repo.
 */
public class ArchiveReadAccess {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveReadAccess.class);
	private static final String BENCH_REPO_DIR_NAME = "benchrepo";

	private final Path rootDir;
	private final RepoStorage repoStorage;
	private final TarFileStorage tarFileStorage;
	private final String benchRepoUrl;

	private AtomicInteger nextArchivePathNr;

	public ArchiveReadAccess(Path rootDir, RepoStorage repoStorage, TarFileStorage tarFileStorage,
		String benchRepoUrl) throws IOException {

		this.rootDir = rootDir;
		this.repoStorage = repoStorage;
		this.tarFileStorage = tarFileStorage;
		this.benchRepoUrl = benchRepoUrl;

		nextArchivePathNr = new AtomicInteger(0);

		Files.createDirectories(rootDir);
	}

	/**
	 * Transfer the tar file for a task to an {@link OutputStream}. Creates the tar file if
	 * necessary.
	 *
	 * @param task the task whose tar file to transfer
	 * @param outputStream the tar file is transferred to this stream
	 * @throws TarRetrieveException if the tar file could not be retrieved
	 * @throws TarTransferException if the tar file could not be transferred
	 */
	public void transferTask(Task task, OutputStream outputStream)
		throws TarRetrieveException, TarTransferException {
		if (task.getSource().getLeft().isPresent()) {
			CommitSource commitSource = task.getSource().getLeft().get();
			transferCommitTask(task, commitSource, outputStream);
		} else {
			transferTarTask(task, outputStream);
		}
	}

	/**
	 * Returns a new unique directory name to clone a repo into. Guaranteed to return a different name
	 * every call. Is only based on the repo dir name and commit hash so the directory names are more
	 * human-readable for debugging purposes.
	 *
	 * @param repoDirName the name of the repo's directory
	 * @param commitHash the commit hash that will be checked out here
	 * @return a new unique name that should be safe to use as a directory name
	 */
	private Path getArchivePath(String repoDirName, CommitHash commitHash) {
		String hashAsString = commitHash.getHash();
		String numberPart = Integer.toString(nextArchivePathNr.getAndIncrement());
		return rootDir.resolve(repoDirName + "_" + hashAsString + "_" + numberPart);
	}

	private void transferCommitTask(Task task, CommitSource commitSource, OutputStream outputStream)
		throws TarRetrieveException, TarTransferException {

		transferCommit(
			task,
			commitSource.getRepoId().getDirectoryName(),
			commitSource.getHash(),
			outputStream
		);
	}

	private void transferCommit(@Nullable Task task, String repoDirName, CommitHash commitHash,
		OutputStream outputStream) throws TarRetrieveException, TarTransferException {

		Path archivePath = getArchivePath(repoDirName, commitHash);

		try {
			try {
				TransferUtils.cloneRepo(repoStorage, repoDirName, archivePath, commitHash);
			} catch (RepositoryAcquisitionException | CloneException | IOException e) {
				throw new TarRetrieveException(e, task);
			}

			try {
				TransferUtils.tarRepo(archivePath, outputStream);
			} catch (IOException e) {
				throw new TarTransferException(e, task);
			}
		} finally {
			try {
				FileHelper.deleteDirectoryOrFile(archivePath);
			} catch (IOException e) {
				LOGGER.warn("Failed to delete " + archivePath);
			}
		}
	}

	private void transferTarTask(Task task, OutputStream outputStream) throws TarTransferException {
		try {
			tarFileStorage.retrieveTarFile(task.getIdAsString(), outputStream);
		} catch (IOException e) {
			throw new TarTransferException(e, task);
		}
	}

	// Bench repo functions

	public String getBenchRepoDirName() {
		return BENCH_REPO_DIR_NAME;
	}

	public String getBenchRepoRemoteUrl() {
		return benchRepoUrl;
	}

	/**
	 * @return the bench repo's current hash or {@link Optional#empty()} if it could not be retrieved
	 */
	public Optional<CommitHash> getBenchRepoCommitHash() {
		try (Repository repository = repoStorage.acquireRepository(BENCH_REPO_DIR_NAME)) {
			return Optional.ofNullable(repository.resolve(Constants.HEAD))
				.map(AnyObjectId::getName)
				.map(CommitHash::new);
		} catch (RepositoryAcquisitionException | IOException e) {
			return Optional.empty();
		}
	}

	/**
	 * Transfer the bench repo's current state as a tar file to the output stream.
	 *
	 * @param outputStream the tar file is transferred to this stream
	 * @throws TarRetrieveException if the tar file could not be retrieved
	 * @throws TarTransferException if the tar file could not be transferred
	 */
	public void transferBenchRepo(OutputStream outputStream)
		throws TarRetrieveException, TarTransferException {

		Optional<CommitHash> hash = getBenchRepoCommitHash();
		if (hash.isPresent()) {
			transferCommit(null, BENCH_REPO_DIR_NAME, hash.get(), outputStream);
		} else {
			throw new TarRetrieveException();
		}
	}
}
