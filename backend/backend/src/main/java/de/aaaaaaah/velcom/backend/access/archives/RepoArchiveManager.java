package de.aaaaaaah.velcom.backend.access.archives;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning.CloneException;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import de.aaaaaaah.velcom.backend.util.TransferUtils;
import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages archives for ordinary repositories.
 */
public class RepoArchiveManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepoArchiveManager.class);

	private final Path rootDir;
	private final RepoStorage repoStorage;

	public RepoArchiveManager(Path repoArchivesRootDir, RepoStorage repoStorage) throws IOException {
		this.rootDir = repoArchivesRootDir;
		this.repoStorage = repoStorage;
		Files.createDirectories(repoArchivesRootDir);
	}

	/**
	 * Attempts to create an archive of the specified repo at the specified state.
	 *
	 * @param repoId the repo
	 * @param commitHash the state of the repo
	 * @return the path to the repo clone directory
	 * @throws RepositoryAcquisitionException if the repo in the given repo storage is inaccessible
	 * @throws CloneException if the clone operation fails
	 * @throws IOException if some other io related operation fails
	 */
	public Path createIfNecessary(RepoId repoId, CommitHash commitHash)
		throws RepositoryAcquisitionException, CloneException, IOException {

		Path archivePath = getArchivePath(repoId, commitHash);

		if (!Files.exists(archivePath)) {
			try {
				TransferUtils.cloneRepo(repoStorage, repoId.getDirectoryName(), archivePath, commitHash);
			} catch (RepositoryAcquisitionException | CloneException | IOException e) {
				deleteArchive(repoId, commitHash);
				throw e;
			}
		}

		return archivePath;
	}

	public Path getArchivePath(RepoId repoId, CommitHash commitHash) {
		return rootDir.resolve(repoId.getDirectoryName() + "_" + commitHash.getHash());
	}

	/**
	 * Deletes the archive corresponding to the given repo id and commit hash.
	 *
	 * @param repoId the repo id
	 * @param commitHash the commit hash
	 */
	public void deleteArchive(RepoId repoId, CommitHash commitHash) {
		Path path = getArchivePath(repoId, commitHash);

		try {
			FileHelper.deleteDirectoryOrFile(path);
		} catch (IOException e) {
			LOGGER.error("Failed to delete repo archive: " + path, e);
		}
	}

	/**
	 * Deletes all archives.
	 *
	 * @throws IOException if the delete operation failed
	 */
	public void deleteAll() throws IOException {
		FileHelper.deleteDirectoryOrFile(rootDir);
		Files.createDirectories(rootDir);
	}

}
