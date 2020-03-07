package de.aaaaaaah.velcom.runner.state;

import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.util.compression.FileHelper;
import de.aaaaaaah.velcom.runner.shared.util.compression.TarHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A state where the runner is updating its local benchmark repository copy.
 */
public class UpdateBenchmarkRepoState implements RunnerState {

	private String newRepoHeadHash;

	/**
	 * Creates a new state.
	 *
	 * @param newRepoHeadHash the hash of the head commit of the update the server will send soon
	 */
	public UpdateBenchmarkRepoState(String newRepoHeadHash) {
		this.newRepoHeadHash = newRepoHeadHash;
	}

	@Override
	public RunnerStatusEnum getStatus() {
		return RunnerStatusEnum.PREPARING_WORK;
	}

	@Override
	public RunnerState onFileReceived(Path path, RunnerConfiguration configuration)
		throws IOException {
		Path tempDirectory = Files.createTempDirectory("work-temp");
		FileHelper.deleteOnExit(tempDirectory);
		FileHelper.deleteOnExit(path);

		TarHelper.untar(path, tempDirectory);
		configuration.getBenchmarkRepoOrganizer().copyToYourself(
			tempDirectory, newRepoHeadHash
		);

		FileHelper.deleteDirectoryOrFile(tempDirectory);
		FileHelper.deleteDirectoryOrFile(path);
		return new IdleState();
	}
}
