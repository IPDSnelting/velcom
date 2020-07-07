package de.aaaaaaah.velcom.runner.revision.tmpdirs;

import de.aaaaaaah.velcom.runner.shared.util.compression.FileHelper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.Nullable;

public class BenchRepoDir {

	private final Path dirPath;
	private final Path hashFilePath;

	@Nullable
	private String currentHash;

	public BenchRepoDir(Path dirPath, Path hashFilePath) {
		this.dirPath = dirPath;
		this.hashFilePath = hashFilePath;

		currentHash = readHash();
	}

	public BenchRepoDir(Path dirPath) {
		this(dirPath, Path.of(dirPath.getParent().toString(), dirPath.getFileName() + ".hash"));
	}

	@Nullable
	private String readHash() {
		try {
			FileReader reader = new FileReader(hashFilePath.toFile());
			BufferedReader bufferedReader = new BufferedReader(reader);
			String line = bufferedReader.readLine();
			return line.strip();
		} catch (IOException ignore) {
			// TODO maybe add some logging
		}

		return null;
	}

	private void writeHash(String hash) throws IOException {
		FileWriter writer = new FileWriter(hashFilePath.toFile());
		writer.write(hash);
	}

	public void clear() throws IOException {
		setHash(null);
		FileHelper.deleteDirectoryOrFile(dirPath);
	}

	public Path getDirPath() {
		return dirPath;
	}

	public void setHash(@Nullable String hash) throws IOException {
		currentHash = hash;
		writeHash(currentHash);
	}

}
