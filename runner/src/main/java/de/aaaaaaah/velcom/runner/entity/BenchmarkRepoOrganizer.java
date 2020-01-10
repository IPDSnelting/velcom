package de.aaaaaaah.velcom.runner.entity;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Organizes the local copy of the benchmark repo.
 */
public interface BenchmarkRepoOrganizer {

	/**
	 * Returns the path to the benchmark repo. The path does not exist yet if {@link
	 * #hasLocalCopy()} is false.
	 *
	 * @return the path to the benchmark repo
	 */
	Path getPathToRepo();

	/**
	 * Returns the path to the benchmark script.
	 *
	 * @return the path to the benchmark script
	 * @throws IOException if the {@link #getPathToRepo()} can not be traversed
	 */
	Path getBenchmarkScript() throws IOException;

	/**
	 * Copies the repository from the given path to yourself.
	 *
	 * @param source the source file. Must be a <em>folder</em>.
	 * @param headHash the hash of the head commit
	 * @throws IOException if an error occurs copying the files
	 */
	void copyToYourself(Path source, String headHash) throws IOException;

	/**
	 * Returns true if a local benchmark repo copy exists at {@link #getPathToRepo()}.
	 *
	 * @return true if a local copy of the benchmark repo exists
	 */
	boolean hasLocalCopy();

	/**
	 * Returns the hash of the HEAD commit in the local copy. Empty if {@link #hasLocalCopy()} is
	 * false.
	 *
	 * @return the hash of the HEAD commit in the local copy
	 */
	Optional<String> getHeadHash();
}
