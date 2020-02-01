package de.aaaaaaah.velcom.backend.storage.repo;

import de.aaaaaaah.velcom.runner.shared.ProgramExecutor;
import de.aaaaaaah.velcom.runner.shared.ProgramExecutor.ProgramResult;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
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
	private static final String GIT_EXECUTABLE = "git";

	private static final GuickCloning INSTANCE = findInstanceToUse();

	private static GuickCloning findInstanceToUse() {
		try {
			ProgramResult git = new ProgramExecutor()
				.execute(GIT_EXECUTABLE, "--version")
				.get();
			if (git.getExitCode() == 0) {
				LOGGER.info("git executable found, using fast path for cloning");
				return new CmdGitCloning();
			}
		} catch (InterruptedException | ExecutionException ignored) {
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
	private static class JgitCloning extends GuickCloning {

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
	private static class CmdGitCloning extends GuickCloning {

		@Override
		public void cloneMirror(String source, Path targetDir) throws CloneException {
			try {
				ProgramResult programResult = new ProgramExecutor()
					.execute(
						"git", "clone", "--mirror", "--recursive", "--recurse-submodules",
						source,
						targetDir.toAbsolutePath().toString()
					).get();
				guardResult(source, programResult);
			} catch (InterruptedException | ExecutionException e) {
				throw new CloneException("Clone failed for " + source + " (mirror)", e);
			}
		}

		private void guardResult(String message, ProgramResult programResult)
			throws CloneException {
			if (programResult.getExitCode() == 0) {
				return;
			}
			throw new CloneException(
				message
					+ "\nExit code: " + programResult.getExitCode()
					+ "\nStdout: " + programResult.getStdOut()
					+ "\nStderr: " + programResult.getStdErr()
					+ "\nAfter: " + programResult.getRuntime()
			);
		}

		@Override
		public void cloneCommit(String source, Path targetDir, String commitHash)
			throws CloneException {

			try {
				ProgramResult programResult = new ProgramExecutor()
					.execute(
						"git", "clone", "--recursive", "--recurse-submodules",
						source,
						targetDir.toAbsolutePath().toString()
					)
					.get();
				guardResult(
					"Clone failed",
					programResult
				);

				programResult = new ProgramExecutor()
					.execute(
						"git", "-C", targetDir.toAbsolutePath().toString(), "checkout", commitHash

					)
					.get();

				guardResult(
					"Checkout failed!",
					programResult
				);
			} catch (InterruptedException | ExecutionException e) {
				throw new CloneException("Clone failed for " + source + " at " + commitHash, e);
			}
		}
	}
}
