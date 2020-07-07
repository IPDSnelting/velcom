package de.aaaaaaah.velcom.backend.access;

import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.RepoSource;
import de.aaaaaaah.velcom.backend.access.entities.TarSource;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.exceptions.CloneRepoException;
import de.aaaaaaah.velcom.backend.access.exceptions.PrepareTransferException;
import de.aaaaaaah.velcom.backend.access.exceptions.TransferException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ArchiveAccess {

	private final Path archiveRootDir;
	private final RepoWriteAccess repoAccess;

	public ArchiveAccess(Path archiveRootDir,
		RepoWriteAccess repoAccess) {
		this.archiveRootDir = archiveRootDir;
		this.repoAccess = repoAccess;
	}

	public void transferTask(Task task, OutputStream outputStream)
		throws PrepareTransferException, TransferException {

		if (task.getSource().isRight()) {
			TarSource tarSource = task.getSource().getRight().orElseThrow();
			Path tarPath = archiveRootDir.resolve("tars").resolve(tarSource.getTarName());

			try {
				Files.newInputStream(tarPath).transferTo(outputStream);
			} catch (IOException e) {
				throw new TransferException(e, task);
			}
		} else {
			RepoSource repoSource = task.getSource().getLeft().orElseThrow();
			RepoId repoId = repoSource.getRepoId();

			Path cloneDest = archiveRootDir.resolve("repos")
				.resolve(repoId.getDirectoryName());

			try {
				repoAccess.cloneRepo(repoId, repoSource.getHash(), cloneDest);
			} catch (CloneRepoException e) {
				throw new PrepareTransferException(e, task);
			}

			try {
				transferRepo(cloneDest, outputStream);
			} catch (IOException e) {
				throw new TransferException(e, task);
			}
		}
	}

	private void transferRepo(Path repoCloneDir, OutputStream out) throws IOException {

	}

}
