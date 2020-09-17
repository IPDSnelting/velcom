package de.aaaaaaah.velcom.backend.access.archives;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning.CloneException;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import de.aaaaaaah.velcom.backend.util.TransferUtils;
import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the benchmark repository archive.
 */
public class BenchRepoArchive {

	private static final Logger LOGGER = LoggerFactory.getLogger(BenchRepoArchive.class);

	private final Path clonePath;
	private final Path tarPath;

	public BenchRepoArchive(Path clonePath, Path tarPath) {
		this.clonePath = clonePath;
		this.tarPath = tarPath;
	}

	public Path getTar() throws IllegalStateException {
		if (!hasTar()) {
			throw new IllegalStateException("tar does not exist");
		}

		return tarPath;
	}

	/**
	 * Attempts to create the benchmark repo clone directory and tar file.
	 *
	 * @param repoStorage repo storage which contains the bench repo
	 * @param repoDir the directory name of the bench repo in the provided repo storage
	 * @param commitHash which state of the bench repo to archive
	 * @throws RepositoryAcquisitionException if the bench repo in the given repo storage is
	 * 	inaccessible
	 * @throws CloneException if the clone operation fails
	 * @throws IOException if some other io related operation fails
	 */
	public void createIfNecessary(RepoStorage repoStorage, String repoDir, CommitHash commitHash)
		throws RepositoryAcquisitionException, CloneException, IOException {

		// 1.) Create copy clone
		if (!hasClone()) {
			delete();

			try {
				TransferUtils.cloneRepo(repoStorage, repoDir, clonePath, commitHash);
			} catch (RepositoryAcquisitionException | CloneException | IOException e) {
				delete();
				throw e;
			}
		}

		// 2.) Create tar file
		if (!hasTar()) {
			try (OutputStream tarOut = Files.newOutputStream(tarPath)) {
				TransferUtils.tarRepo(clonePath, tarOut); // tarRepo() also closes tarOut
			} catch (Exception e) {
				delete();
				throw e;
			}
		}
	}

	/**
	 * Deletes the bench repo archive.
	 */
	public void delete() {
		try {
			FileHelper.deleteDirectoryOrFile(clonePath);
		} catch (IOException e) {
			LOGGER.error("Failed to delete bench repo clone directory", e);
		}

		try {
			FileHelper.deleteDirectoryOrFile(tarPath);
		} catch (IOException e) {
			LOGGER.error("Failed to delete bench tar file", e);
		}
	}

	/**
	 * Checks whether or not the clone directory for the bench repo exists.
	 *
	 * @return {@code true} if the clone directory exists
	 */
	public boolean hasClone() {
		return Files.exists(clonePath);
	}

	/**
	 * Checks whether or not the tar file for the bench repo exists.
	 *
	 * @return {@code true} if the tar file exists
	 */
	public boolean hasTar() {
		return Files.exists(tarPath);
	}

}
