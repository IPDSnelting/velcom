package de.aaaaaaah.velcom.backend.data.benchrepo;

import de.aaaaaaah.velcom.backend.access.ArchiveAccess;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.exceptions.PrepareTransferException;
import de.aaaaaaah.velcom.backend.access.exceptions.TransferException;
import java.io.OutputStream;

public class BenchRepo {

	private final ArchiveAccess archiveAccess;

	public BenchRepo(ArchiveAccess archiveAccess) {
		this.archiveAccess = archiveAccess;
	}

	public CommitHash getCurrentHash() {
		return archiveAccess.getBenchRepoCommitHash();
	}

	public void transfer(OutputStream outputStream)
		throws TransferException, PrepareTransferException {
		archiveAccess.transferBenchRepo(outputStream);
	}

	public void checkForUpdates() {
		archiveAccess.updateBenchRepo();
	}

}
