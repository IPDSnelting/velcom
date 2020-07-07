package de.aaaaaaah.velcom.runner.revision.tmpdirs;

import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nullable;

public class BenchRepoDir {

	private final Path dirPath;
	private final Path hashFilePath;

	@Nullable
	private String currentHash;

	public BenchRepoDir(Path dirPath) throws IOException {
		this.dirPath = dirPath;
		this.hashFilePath = dirPath.getParent().resolve(dirPath.getFileName() + ".hash");

		currentHash = readHash();
	}

	@Nullable
	private String readHash() throws IOException {
		try {
			FileReader reader = new FileReader(hashFilePath.toFile());
			BufferedReader bufferedReader = new BufferedReader(reader);
			String line = bufferedReader.readLine();
			return line.strip();
		} catch (FileNotFoundException ignore) {
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

	public Optional<String> getHash() {
		return Optional.ofNullable(currentHash);
	}

}
