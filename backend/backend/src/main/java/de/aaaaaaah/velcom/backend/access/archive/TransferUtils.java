package de.aaaaaaah.velcom.backend.access.archive;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning.CloneException;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import de.aaaaaaah.velcom.backend.util.CheckedConsumer;
import de.aaaaaaah.velcom.shared.util.FileHelper;
import de.aaaaaaah.velcom.shared.util.OSCheck;
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

public class TransferUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(TransferUtils.class);

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

	public static void tarRepo(Path repoDir, OutputStream out) throws IOException {
		long start = System.currentTimeMillis();

		try (out) {
			var tarOut = new TarArchiveOutputStream(out);

			tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			tarOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

			Files.walk(repoDir)
				.filter(Files::isRegularFile)
				.forEach(handleError(file -> {
						String relativePath = repoDir.relativize(file).toString();

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
			LOGGER.info("Tar operation took {} ms... ({})", end - start, repoDir);
		}
	}

	public static void transferTar(Path tarPath, OutputStream out) throws IOException {
		Files.newInputStream(tarPath).transferTo(out);
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
