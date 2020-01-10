package de.aaaaaaah.designproto.runner.util.compression;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Helps with unpacking tar files.
 */
public class TarHelper {

	/**
	 * Unpacks a tape archive.
	 *
	 * @param tarFile the tar file
	 * @param targetDir the target directory to unpack it to
	 * @throws IOException if an error occurs
	 */
	public static void untar(Path tarFile, Path targetDir) throws IOException {
		try (InputStream fileInput = Files.newInputStream(tarFile);
			TarArchiveInputStream inputStream = new TarArchiveInputStream(fileInput)) {

			TarArchiveEntry entry;
			while ((entry = inputStream.getNextTarEntry()) != null) {
				Path entryPath = targetDir.resolve(Path.of(entry.getName()));
				if (entry.isDirectory()) {
					Files.createDirectories(entryPath);
				} else {
					Files.createFile(entryPath);
					try (var fileOut = Files.newOutputStream(entryPath)) {
						IOUtils.copy(inputStream, fileOut);
					}
					Files.setPosixFilePermissions(
						entryPath,
						PermissionsHelper.fromOctal(entry.getMode())
					);
				}
			}
		}
	}
}
