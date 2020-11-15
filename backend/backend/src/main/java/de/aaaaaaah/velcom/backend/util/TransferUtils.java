package de.aaaaaaah.velcom.backend.util;

import static java.util.function.Predicate.not;

import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning.CloneException;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import de.aaaaaaah.velcom.shared.util.FileHelper;
import de.aaaaaaah.velcom.shared.util.compression.PermissionsHelper;
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
 * Some basic utils for transferring tars and repos to output streams.
 */
public class TransferUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(TransferUtils.class);

	private TransferUtils() {
		throw new UnsupportedOperationException("no");
	}

	/**
	 * Creates a non bare clone of a repository that resides in the given repo storage and corresponds
	 * to the provided {@code dirName} by cloning the repo into the specified {@code destDir}.
	 *
	 * @param repoStorage the repo storage that contains the repository which should be cloned
	 * @param dirName the directory name of the repository
	 * @param destDir where the repository should be cloned to
	 * @param hash which state of the repository the clone should be at
	 * @throws RepositoryAcquisitionException if the repository could not be acquired
	 * @throws CloneException if the clone operation itself failed
	 * @throws IOException if some io error occurred
	 */
	public static void cloneRepo(RepoStorage repoStorage, String dirName, Path destDir,
		CommitHash hash)
		throws RepositoryAcquisitionException, CloneException, IOException {

		long start = System.currentTimeMillis();

		try (Repository repository = repoStorage.acquireRepository(dirName)) {
			// Make sure that the commit actually exists before doing anything else
			ObjectId commitId = repository.resolve(hash.getHash());
			Objects.requireNonNull(commitId, "unknown commit hash "
				+ hash + " for local repo " + dirName);

			// Check destinationCloneDir
			if (Files.exists(destDir)) {
				return; // clone already exists, no need to clone again
			}

			// Clone does not yet exist => clone original local repo to cloneDir
			Files.createDirectories(destDir);

			Path originalRepoPath = repoStorage.getRepoDir(dirName);

			GuickCloning.getInstance().cloneCommit(
				originalRepoPath.toAbsolutePath().toString(),
				destDir,
				hash.getHash()
			);
		} catch (Exception e) {
			// clone operation failed => try to delete clone directory
			try {
				FileHelper.deleteDirectoryOrFile(destDir);
			} catch (Exception ignore) {
			}

			throw e;
		} finally {
			long end = System.currentTimeMillis();
			LOGGER.info("Clone operation took {} ms... ({})", end - start, dirName);
		}
	}

	/**
	 * Creates a tar of the repository at the specified {@code repoDir} and simultaneously writes that
	 * tar into the given output stream.
	 *
	 * <p> Note that this method closes the provided output stream after it has finished.
	 *
	 * @param repoDir the path to the repository
	 * @param out where the tar should be written to
	 * @throws IOException if some io error occurred
	 */
	public static void tarRepo(Path repoDir, OutputStream out) throws IOException {
		long start = System.currentTimeMillis();

		try (out) {
			var tarOut = new TarArchiveOutputStream(out);

			tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			tarOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

			Files.walk(repoDir)
				.filter(not(Files::isSymbolicLink))
				.forEach(handleError(file -> {
					String relativePath = repoDir.relativize(file).toString();

					TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), relativePath);
					entry.setMode(PermissionsHelper.toOctal(Files.getPosixFilePermissions(file)));

					tarOut.putArchiveEntry(entry);

					if (Files.isRegularFile(file)) {
						try (InputStream in = Files.newInputStream(file)) {
							IOUtils.copy(in, tarOut);
						}
					}

					tarOut.closeArchiveEntry();
				}));
		} finally {
			long end = System.currentTimeMillis();
			LOGGER.info("Tar operation took {} ms... ({})", end - start, repoDir);
		}
	}

	private static Consumer<Path> handleError(CheckedConsumer<Path, Exception> checkedConsumer) {
		return path -> {
			try {
				checkedConsumer.accept(path);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

}
