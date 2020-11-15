package de.aaaaaaah.velcom.backend.data.benchrepo;

import de.aaaaaaah.velcom.backend.newaccess.archiveaccess.ArchiveReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.archiveaccess.exceptions.TarRetrieveException;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the benchmark repository which is used to run various benchmarks on tasks.
 */
public class BenchRepo {

	private final ArchiveReadAccess archiveAccess;

	public BenchRepo(ArchiveReadAccess archiveAccess) {
		this.archiveAccess = Objects.requireNonNull(archiveAccess);
	}

	/**
	 * @return the name of the bench repo's directory.
	 */
	public String getDirName() {
		return archiveAccess.getBenchRepoDirName();
	}

	/**
	 * @return the bench repo's remote url
	 */
	public String getRemoteUrl() {
		return archiveAccess.getBenchRepoRemoteUrl();
	}

	/**
	 * @return the current commit hash of the benchmark repository.
	 */
	public Optional<CommitHash> getCurrentHash() {
		return archiveAccess.getBenchRepoCommitHash();
	}

	/**
	 * Transfers the benchmark repository to the given {@link OutputStream}.
	 *
	 * <p> Note that the provided output stream will be closed after the transfer operation is done.
	 *
	 * @param outputStream the output stream
	 * @throws TarRetrieveException if the tar file could not be retrieved
	 */
	public void transfer(OutputStream outputStream) throws TarRetrieveException {
		archiveAccess.transferBenchRepo(Objects.requireNonNull(outputStream));
	}
}
