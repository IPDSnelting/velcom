package de.aaaaaaah.velcom.runner.shared.util.compression;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Helps with common file I/O Operations.
 */
public class FileHelper {

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
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
				throws IOException {
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

	/**
	 * Deletes the given path on VM exit.
	 *
	 * @param path the path to delete
	 */
	public static void deleteOnExit(Path path) {
		Runtime.getRuntime()
			.addShutdownHook(new Thread(() -> uncheckedDeleteDirectoryOrFile(path)));
	}
}
