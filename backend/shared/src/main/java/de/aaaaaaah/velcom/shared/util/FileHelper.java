package de.aaaaaaah.velcom.shared.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Helps with common file I/O operations.
 */
public final class FileHelper {

	private FileHelper() {
		throw new UnsupportedOperationException("No");
	}

	/**
	 * Deletes a file or directory.
	 *
	 * @param path the path to the file to delete
	 * @throws IOException if an error occurs
	 */
	public static void deleteDirectoryOrFile(Path path) throws IOException {
		if (Files.notExists(path)) {
			return;
		}

		Files.walkFileTree(path, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				file.toFile().setWritable(true);
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Deletes a file or directory.
	 *
	 * @param path the path to the file to delete
	 * @throws UncheckedIOException wrapping an IOException if an error occurs
	 */
	public static void uncheckedDeleteDirectoryOrFile(Path path) throws UncheckedIOException {
		try {
			deleteDirectoryOrFile(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
