package de.aaaaaaah.velcom.backend.data.benchrepo;

import de.aaaaaaah.velcom.backend.access.ArchiveAccess;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.exceptions.PrepareTransferException;
import de.aaaaaaah.velcom.backend.access.exceptions.RepoAccessException;
import de.aaaaaaah.velcom.backend.access.exceptions.TransferException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Represents the benchmark repository which is used to run various benchmarks on tasks.
 */
public class BenchRepo {

	private final ArchiveAccess archiveAccess;

	public BenchRepo(ArchiveAccess archiveAccess) {
		this.archiveAccess = Objects.requireNonNull(archiveAccess);
	}

	/**
	 * @return the current commit hash of the benchmark repository.
	 */
	public CommitHash getCurrentHash() {
		return archiveAccess.getBenchRepoCommitHash();
	}

	/**
	 * Transfers the benchmark repository to the given {@link OutputStream}.
	 *
	 * @param outputStream the output stream
	 * @throws TransferException if an error occurs while the benchmark repo is being written to
	 * 	the output stream.
	 * @throws PrepareTransferException if an error occurs before the benchmark repo is being
	 * 	written to the output stream.
	 */
	public void transfer(OutputStream outputStream)
		throws TransferException, PrepareTransferException {
		archiveAccess.transferBenchRepo(Objects.requireNonNull(outputStream));
	}

	/**
	 * Checks for any new commits on the benchmark repository.
	 *
	 * @throws RepoAccessException if an error occured trying to access the repo
	 */
	public void checkForUpdates() throws RepoAccessException {
		archiveAccess.updateBenchRepo();
	}

}
