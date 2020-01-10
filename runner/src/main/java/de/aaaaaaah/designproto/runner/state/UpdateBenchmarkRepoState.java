package de.aaaaaaah.designproto.runner.state;

import de.aaaaaaah.designproto.runner.entity.BenchmarkRepoOrganizer;
import de.aaaaaaah.designproto.runner.entity.RunnerConfiguration;
import de.aaaaaaah.designproto.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.designproto.runner.util.compression.FileHelper;
import de.aaaaaaah.designproto.runner.util.compression.TarHelper;
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
	public RunnerState onSelected(RunnerConfiguration configuration) {
		configuration.getRunnerStateMachine().setCurrentRunnerStateEnum(RunnerStatusEnum.IDLE);
		return this;
	}

	@Override
	public RunnerState onFileReceived(Path path, RunnerConfiguration configuration)
		throws IOException {
		Path tempDirectory = Files.createTempDirectory("work-temp");
		TarHelper.untar(path, tempDirectory);
		configuration.getBenchmarkRepoOrganizer().copyToYourself(
			tempDirectory, newRepoHeadHash
		);

		FileHelper.deleteDirectoryOrFile(tempDirectory);
		FileHelper.deleteDirectoryOrFile(path);
		return new IdleState();
	}
}
