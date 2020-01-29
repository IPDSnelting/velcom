package de.aaaaaaah.velcom.backend.storage.repo;

import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows for quicker git cloning if supported.
 */
public abstract class GuickCloning {

	private static final Logger LOGGER = LoggerFactory.getLogger(GuickCloning.class);

	private static final GuickCloning INSTANCE = findInstanceToUse();

	private static GuickCloning findInstanceToUse() {
		try {
			int exitCode = new ProcessBuilder("git", "--version")
				.start()
				.waitFor();
			if (exitCode == 0) {
				LOGGER.info("git executable found, using fast path for cloning");
				return new CmdGitCloning();
			}
		} catch (InterruptedException | IOException ignored) {
		}
		LOGGER.info("git executable not found, falling back to slow path for cloning");

		return new JgitCloning();
	}

	/**
	 * Runs a usable instance of {@link GuickCloning}.
	 *
	 * @return the instance
	 */
	public static GuickCloning getInstance() {
		return INSTANCE;
	}

	/**
	 * Clones a given repo from source to target.
	 *
	 * @param source the source
	 * @param targetDir the target directory
	 * @throws CloneException if an error occurs
	 */
	public abstract void cloneMirror(String source, Path targetDir) throws CloneException;

	/**
	 * Clones a given repo and checks out the given commit.
	 *
	 * @param source the source to clone from
	 * @param targetDir the target directory
	 * @param commitHash the hash of the commit to check out
	 * @throws CloneException if an error occurs
	 */
	public abstract void cloneCommit(String source, Path targetDir, String commitHash)
		throws CloneException;


	/**
	 * An exception that occurred while cloning a repo.
	 */
	public static class CloneException extends Exception {

		public CloneException(String message) {
			super(message);
		}

		public CloneException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	/**
	 * Uses JGit for cloning.
	 */
	static class JgitCloning extends GuickCloning {

		private JgitCloning() {
		}

		@Override
		public void cloneMirror(String source, Path targetDir) throws CloneException {
			try {
				CloneCommand cloneCommand = Git.cloneRepository()
					.setURI(source)
					.setDirectory(targetDir.toFile())
					.setBare(true);
				cloneCommand.call().close();
			} catch (GitAPIException e) {
				throw new CloneException("Could not clone bare repo", e);
			}
		}

		@Override
		public void cloneCommit(String source, Path targetDir, String commitHash)
			throws CloneException {
			try (Git clone = Git.cloneRepository()
				.setBare(false)
				.setCloneSubmodules(true)
				.setCloneAllBranches(false)
				.setURI(source)
				.setDirectory(targetDir.toFile())
				.call()) {

				clone.checkout().setName(commitHash).call();

				// Use git clean to remove untracked submodules
				clone.clean()
					.setCleanDirectories(true)
					.setForce(true)
					.call();
			} catch (GitAPIException e) {
				throw new CloneException("Error checking out commit", e);
			}
		}
	}

	/**
	 * Uses the git executable for cloning.
	 */
	static class CmdGitCloning extends GuickCloning {

		@Override
		public void cloneMirror(String source, Path targetDir) throws CloneException {
			try {
				Process process = new ProcessBuilder(
					"git", "clone", "--mirror", "--recursive", "--recurse-submodules",
					source,
					targetDir.toAbsolutePath().toString()
				)
					.start();
				int exitCode = process.waitFor();
				if (exitCode != 0) {
					throw new CloneException("Clone failed for " + source + " with " + exitCode);
				}
			} catch (IOException | InterruptedException e) {
				throw new CloneException("Clone failed for " + source + " (mirror)", e);
			}
		}

		@Override
		public void cloneCommit(String source, Path targetDir, String commitHash)
			throws CloneException {

			try {
				int exitCode = new ProcessBuilder(
					"git", "clone", "--recursive", "--recurse-submodules",
					source,
					targetDir.toAbsolutePath().toString()
				)
					.start()
					.waitFor();
				if (exitCode != 0) {
					throw new IOException("Clone exited with exit code " + exitCode);
				}
				exitCode = new ProcessBuilder(
					"git", "checkout", commitHash
				)
					.directory(targetDir.toFile())
					.start()
					.waitFor();
				if (exitCode != 0) {
					throw new IOException("Checkout exited with exit code " + exitCode);
				}
			} catch (InterruptedException | IOException e) {
				throw new CloneException("Clone failed for " + source + " at " + commitHash, e);
			}
		}
	}
}
