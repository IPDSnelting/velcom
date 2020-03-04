package de.aaaaaaah.velcom.runner.entity;

import de.aaaaaaah.velcom.runner.shared.util.compression.FileHelper;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

/**
 * Uses a temporary folder to store the repo.
 */
public class TempFileBenchmarkRepoOrganizer implements BenchmarkRepoOrganizer {

	private Path rootTempDir;
	private String headHash;

	/**
	 * Creates a new temp file benchmark repo organizer.
	 *
	 * @throws IOException if a temporary file could not be created
	 */
	public TempFileBenchmarkRepoOrganizer() throws IOException {
		this.rootTempDir = Files.createTempDirectory("velcom-runner-benchmark");
		FileHelper.deleteOnExit(rootTempDir);
	}

	@Override
	public Path getPathToRepo() {
		return rootTempDir.resolve("benchmark-repo");
	}

	@Override
	public Path getBenchmarkScript() {
		return getPathToRepo().resolve("bench");
	}

	@Override
	public boolean hasLocalCopy() {
		return Files.exists(getPathToRepo());
	}

	@Override
	public void copyToYourself(Path source, String headHash) throws IOException {
		if (hasLocalCopy()) {
			FileHelper.deleteDirectoryOrFile(getPathToRepo());
		}
		copyDirectory(source, getPathToRepo());
		this.headHash = headHash;
	}

	private void copyDirectory(Path source, Path target) throws IOException {
		Files.walkFileTree(source, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
				throws IOException {
				Files.createDirectories(target.resolve(source.relativize(dir)));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
				Files.copy(file, target.resolve(source.relativize(file)));
				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Override
	public Optional<String> getHeadHash() {
		return Optional.ofNullable(headHash);
	}
}
