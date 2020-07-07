package de.aaaaaaah.velcom.backend.access.archive;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.exceptions.ArchiveFailedPermanently;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning.CloneException;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import de.aaaaaaah.velcom.backend.util.CheckedConsumer;
import de.aaaaaaah.velcom.runner.shared.util.OSCheck;
import de.aaaaaaah.velcom.runner.shared.util.FileHelper;
import de.aaaaaaah.velcom.runner.shared.util.compression.PermissionsHelper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the functionality to archive local repositories.
 * <p>This class is thread safe.</p>
 */
public class Archiver {

	private static final Logger LOGGER = LoggerFactory.getLogger(Archiver.class);

	private final RepoStorage repoStorage;
	private final Path archivesRootDir;

	/**
	 * Constructs a new instance of {@link Archiver}.
	 *
	 * @param repoStorage the storage containing the local repositories
	 */
	public Archiver(RepoStorage repoStorage, Path archivesRootDir) {
		this.repoStorage = repoStorage;
		this.archivesRootDir = archivesRootDir;
	}

	/**
	 * Tries to delete all local clones that were created for the archival of the local repository
	 * referenced under {@code dirName} which were not automatically deleted because {@code
	 * keepDeepClone} was set to {@code true} in {@link #archive(String, CommitHash, OutputStream,
	 * boolean)}.
	 *
	 * @param dirName the directory name of the repository
	 */
	public synchronized void deleteArchives(String dirName) {
		Path archivesDir = archivesRootDir.resolve(dirName);

		try {
			FileHelper.deleteDirectoryOrFile(archivesDir);
		} catch (IOException e) {
			LOGGER.warn("Failed to delete archives in: {}", archivesDir);
		}
	}

	/**
	 * Archives the local repository at the state of the given commit and writes the archive to the
	 * given output stream. This is done by cloning the original local repository and changing the
	 * HEAD state of the clone to the given commit and subsequently writing the tar archived version
	 * of the clone into the given output stream.
	 *
	 * <p>Note that the given output stream is closed after the archive operation has ended.</p>
	 *
	 * @param dirName the directory name of the repository to archive
	 * @param commitHash the state at which the repository shall be archived
	 * @param out where the archive is written to
	 * @param keepDeepClone whether or not to remove the clone of the local repository after the
	 * 	archive process is finished
	 * @throws ArchiveException if an error occurs while archiving or cloning the repository
	 * @throws ArchiveFailedPermanently if an error occurs that will probably not get better when
	 * 	retrying
	 */
	public synchronized void archive(String dirName, CommitHash commitHash, OutputStream out,
		boolean keepDeepClone) throws ArchiveException {

		LOGGER.info("Creating archive for: {}/{} (keepDeepClone = {})", dirName,
			commitHash.getHash(), keepDeepClone);

		Path cloneDir = archivesRootDir.resolve(dirName).resolve(commitHash.getHash());

		try {
			// (1): Clone repository
			cloneRepo(dirName, cloneDir, commitHash);
		} catch (Exception e) {
			throw new ArchiveFailedPermanently(e, dirName, commitHash);
		}

		try {
			// (2): Tar repository
			tarDirectory(cloneDir, out);
		} catch (Exception e) {
			throw new ArchiveException(e, dirName, commitHash);
		} finally {
			if (!keepDeepClone) {
				// Regardless of whether or not the tar process failed,
				// delete cloneDir if keepDeepClone is false
				try {
					FileHelper.deleteDirectoryOrFile(cloneDir);
				} catch (IOException ignore) {
				}
			}
		}
	}

	private void tarDirectory(Path cloneDir, OutputStream out) throws IOException {
		long start = System.currentTimeMillis();

		try (out) {
			var tarOut = new TarArchiveOutputStream(out);

			tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			tarOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

			Files.walk(cloneDir)
				.filter(Files::isRegularFile)
				.forEach(handleError(file -> {
						String relativePath = cloneDir.relativize(file).toString();

						TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), relativePath);
						if (!OSCheck.isStupidWindows()) {
							entry.setMode(
								PermissionsHelper.toOctal(Files.getPosixFilePermissions(file))
							);
						}

						try (InputStream in = Files.newInputStream(file)) {
							tarOut.putArchiveEntry(entry);

							IOUtils.copy(in, tarOut);

							tarOut.closeArchiveEntry();
						}
					}
				));
		} finally {
			long end = System.currentTimeMillis();
			LOGGER.info("Tar operation took {} ms... ({})", end - start, cloneDir);
		}
	}

	private void cloneRepo(String originalRepoDirName, Path destinationCloneDir, CommitHash hash)
		throws IOException, RepositoryAcquisitionException, CloneException {

		long start = System.currentTimeMillis();

		try (Repository repository = repoStorage.acquireRepository(originalRepoDirName)) {
			// Make sure that the commit actually exists before doing anything else
			ObjectId commitId = repository.resolve(hash.getHash());
			Objects.requireNonNull(commitId, "unknown commit hash "
				+ hash + " for local repo " + originalRepoDirName);

			// Check destinationCloneDir
			if (Files.exists(destinationCloneDir)) {
				// clone already exists, no need to clone again
				return;
			}

			// Clone does not yet exist => clone original local repo to cloneDir
			Files.createDirectories(destinationCloneDir);

			Path originalRepoPath = repoStorage.getRepoDir(originalRepoDirName);

			GuickCloning.getInstance().cloneCommit(
				originalRepoPath.toAbsolutePath().toString(),
				destinationCloneDir,
				hash.getHash()
			);
		} catch (Exception e) {
			// clone operation failed => try to delete clone directory
			try {
				FileHelper.deleteDirectoryOrFile(destinationCloneDir);
			} catch (Exception ignore) {
			}

			throw e;
		} finally {
			long end = System.currentTimeMillis();
			LOGGER.info("Clone operation took {} ms... ({})", end - start, originalRepoDirName);
		}
	}

	private Consumer<Path> handleError(CheckedConsumer<Path, Exception> checkedConsumer) {
		return path -> {
			try {
				checkedConsumer.accept(path);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

}
