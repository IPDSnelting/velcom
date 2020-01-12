package de.aaaaaaah.velcom.backend.access.repo.archive;

import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import de.aaaaaaah.velcom.backend.util.CheckedConsumer;
import de.aaaaaaah.velcom.backend.util.DirectoryRemover;
import de.aaaaaaah.velcom.runner.shared.util.compression.PermissionsHelper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * Provides the functionality to archive local repositories.
 */
public class Archiver {

	private static final Path ARCHIVES_ROOT_DIR = Paths.get("data/archives/");

	private final RepoStorage repoStorage;

	/**
	 * Constructs a new instance of {@link Archiver}.
	 *
	 * @param repoStorage the storage containing the local repositories
	 */
	public Archiver(RepoStorage repoStorage) {
		this.repoStorage = repoStorage;
	}

	/**
	 * Archives the local repository at the state of the given commit and writes the archive to the
	 * given output stream. This is done by cloning the original local repository and changing the
	 * HEAD state of the clone to the given commit and subsequently writing the tar archived version
	 * of the clone into the given output stream.
	 *
	 * <p>Note that the given output stream is closed after the archive operation has ended.</p>
	 *
	 * @param dirName the directory name of the repository to archive
	 * @param commitHash the state at which the repository shall be archived
	 * @param out where the archive is written to
	 * @param keepDeepClone whether or not to remove the clone of the local repository after the
	 * 	archive process is finished
	 * @throws ArchiveException if an error occurs while archiving or cloning the repository
	 */
	public void archive(String dirName, CommitHash commitHash, OutputStream out,
		boolean keepDeepClone) throws ArchiveException {

		try (out) {
			Path cloneDir = cloneRepo(dirName, commitHash);

			tarDirectory(cloneDir, out);

			if (!keepDeepClone) {
				DirectoryRemover.deleteDirectoryRecursive(cloneDir);
			}
		} catch (Exception e) {
			throw new ArchiveException(e, dirName, commitHash);
		}
	}

	private void tarDirectory(Path cloneDir, OutputStream out) throws IOException {
		var tarOut = new TarArchiveOutputStream(out);

		tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
		tarOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

		Files.walk(cloneDir)
			.filter(Files::isRegularFile)
			.forEach(handleError(file -> {
					String relativePath = cloneDir.relativize(file).toString();

					TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), relativePath);
					entry.setMode(PermissionsHelper.toOctal(Files.getPosixFilePermissions(file)));

					try (InputStream in = Files.newInputStream(file)) {
						tarOut.putArchiveEntry(entry);

						IOUtils.copy(in, tarOut);

						tarOut.closeArchiveEntry();
					}
				}
			));
	}

	private Path cloneRepo(String dirName, CommitHash commitHash)
		throws IOException, GitAPIException, RepositoryAcquisitionException {

		try (Repository repository = repoStorage.acquireRepository(dirName)) {
			// Make sure that the commit actually exists before doing anything else
			ObjectId commitId = repository.resolve(commitHash.getHash());
			Objects.requireNonNull(commitId, "unknown commit hash "
				+ commitHash + " for local repo " + dirName);

			// Create archives root dir
			if (!Files.exists(ARCHIVES_ROOT_DIR)) {
				Files.createDirectories(ARCHIVES_ROOT_DIR);
			}

			// Create repo archives dir
			Path repoArchivesDir = ARCHIVES_ROOT_DIR.resolve(dirName);
			if (!Files.exists(repoArchivesDir)) {
				Files.createDirectory(repoArchivesDir);
			}

			// Create cloneDir
			Path cloneDir = repoArchivesDir.resolve(commitHash.getHash());

			// FIXME: 27.12.19 But you still need to checkout, no?
			if (Files.exists(cloneDir)) {
				// clone already exists, no need to clone again
				return cloneDir;
			}

			// Clone does not yet exist => clone original local repo to cloneDir
			Files.createDirectory(cloneDir);

			Path originalRepoPath = repoStorage.getRepoDir(dirName);

			// TODO: 27.12.19 Does this also clone our commit if it is on another branch?
			try (Git clone = Git.cloneRepository()
				.setBare(false)
				.setCloneSubmodules(true)
				.setCloneAllBranches(false)
				.setURI(originalRepoPath.toUri().toString())
				.setDirectory(cloneDir.toFile())
				.call()) {

				clone.checkout().setName(commitHash.getHash()).call();

				// Use git clean to remove untracked submodules
				clone.clean()
					.setCleanDirectories(true)
					.setForce(true)
					.call();
			} catch (Exception e) {
				// clone operation failed => try to delete clone directory
				try {
					DirectoryRemover.deleteDirectoryRecursive(cloneDir);
				} catch (Exception ignore) {
				}

				throw e;
			}

			return cloneDir;
		}
	}

	private Consumer<Path> handleError(CheckedConsumer<Path, Exception> checkedConsumer) {
		return path -> {
			try {
				checkedConsumer.accept(path);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

}
