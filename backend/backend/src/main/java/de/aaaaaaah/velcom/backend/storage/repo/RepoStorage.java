package de.aaaaaaah.velcom.backend.storage.repo;

import de.aaaaaaah.velcom.backend.KnownHostsIgnoringSshdFactory;
import de.aaaaaaah.velcom.backend.storage.repo.exception.AddRepositoryException;
import de.aaaaaaah.velcom.backend.storage.repo.exception.DirectoryAlreadyExistsException;
import de.aaaaaaah.velcom.backend.storage.repo.exception.NoSuchRepositoryException;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import de.aaaaaaah.velcom.backend.util.CheckedConsumer;
import de.aaaaaaah.velcom.backend.util.DirectoryRemover;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.sshd.DefaultProxyDataFactory;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * // TODO: 27.12.19 Repo vs Repository?
 *
 * <p>
 * A repo storage is able to store git repositories on the file system.
 */
public class RepoStorage {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepoStorage.class);

	private final Path rootDir;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * Initializes a new repo storage with {@code "data/repos"} as root directory.
	 *
	 * @see #RepoStorage(Path)
	 */
	public RepoStorage() throws IOException {
		this(Paths.get("data/repos"));
	}

	/**
	 * Initializes a new repo storage.
	 *
	 * @param rootDirStr the directory where all repositories will be stored in
	 */
	public RepoStorage(String rootDirStr) throws IOException {
		this(Paths.get(rootDirStr));
	}

	/**
	 * Initializes a new repo storage.
	 *
	 * @param rootDir the directory where all repositories will be stored in
	 */
	public RepoStorage(Path rootDir) throws IOException {
		this.rootDir = rootDir;
		Files.createDirectories(rootDir);

		SshdSessionFactory factory = new KnownHostsIgnoringSshdFactory(
			new JGitKeyCache(),
			new DefaultProxyDataFactory()
		);

		SshSessionFactory.setInstance(factory);
	}

	/**
	 * Checks whether or not this storage contains a repository located under the given directory
	 * name.
	 *
	 * @param dirName the directory name
	 * @return true, if there is a repository currently stored under the given directory name
	 */
	public boolean containsRepository(String dirName) {
		Path path = rootDir.resolve(dirName);
		return Files.exists(path) && Files.isDirectory(path);
	}

	/**
	 * Collects all locally stored repositories in a collection and returns it.
	 *
	 * @return a collection of paths pointing to all locally stored repositories
	 * @throws IOException if an I/O error occurs when trying to read the directories
	 */
	public Collection<Path> getRepoDirectories() throws IOException {
		this.lock.readLock().lock();

		try (Stream<Path> fileStream = Files.list(rootDir)) {
			return fileStream
				.filter(Files::isDirectory)
				.collect(Collectors.toUnmodifiableList());
		} finally {
			this.lock.readLock().unlock();
		}
	}

	/**
	 * Adds a new repository to this storage by cloning it from the specified remote url and storing
	 * it to the directory with the specified name relative to the default root directory of this
	 * storage.
	 *
	 * @param dirName A unique directory name for this repository
	 * @param remoteUrl The remote url to clone the repository from
	 * @return the path of the directory containing the new repository
	 * @throws AddRepositoryException if an exception occurs while trying to add the repository
	 */
	public Path addRepository(String dirName, String remoteUrl) throws AddRepositoryException {
		Path repoDir = rootDir.resolve(dirName);

		this.lock.writeLock().lock();
		try {
			if (Files.exists(repoDir)) {
				throw new DirectoryAlreadyExistsException(repoDir);
			}

			Files.createDirectory(repoDir);

			long start = System.currentTimeMillis();
			LOGGER.info("Cloning repo from {} into {}", remoteUrl, dirName);

			GuickCloning.getInstance().cloneMirror(remoteUrl, repoDir);

			LOGGER.info("Cloning took {} ms", System.currentTimeMillis() - start);

			return repoDir;
		} catch (DirectoryAlreadyExistsException e) {
			throw new AddRepositoryException(dirName, remoteUrl, e);
		} catch (Exception e) {
			// try to clean up directory
			try {
				DirectoryRemover.deleteDirectoryRecursive(repoDir);
			} catch (Exception ignore) {
			}

			throw new AddRepositoryException(dirName, remoteUrl, e);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Removes a repository from this storage by deleting its directory.
	 *
	 * @param dirName the name of the repository
	 * @throws IOException if an I/O exception occurs trying to delete the directory of the
	 * 	repository
	 */
	public void deleteRepository(String dirName) throws IOException {
		this.lock.writeLock().lock();

		try {
			DirectoryRemover.deleteDirectoryRecursive(rootDir.resolve(dirName));
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Acquires the repository located under the specified directory by acquiring the repo storage
	 * lock and executing the given handler.
	 *
	 * @param dirName the name of the directory
	 * @param handler the handler to execute
	 * @throws NoSuchRepositoryException if no repository is found under the specified directory
	 * @throws RepositoryAcquisitionException if the handler throws any exception
	 */
	public void acquireRepository(String dirName, CheckedConsumer<Repository, Exception> handler)
		throws NoSuchRepositoryException, RepositoryAcquisitionException {

		this.lock.readLock().lock();

		try {
			Path repoDir = getRepoDir(dirName);

			try (Git git = Git.open(repoDir.toFile())) {
				Repository repository = git.getRepository();

				handler.accept(repository);

			} catch (Exception e) {
				throw new RepositoryAcquisitionException(this, dirName, e);
			}
		} finally {
			this.lock.readLock().unlock();
		}
	}

	/**
	 * Acquires the repository located under the specified directory by acquiring the repo storage
	 * lock and returning the repository.
	 *
	 * <p>Note that, in order to release the repo storage lock, the returned repository instance
	 * <em>must be closed.</em></p>
	 *
	 * @param dirName the name of the directory
	 * @return Returns the jgit repository instance for this repository
	 * @throws RepositoryAcquisitionException if an exception occurs while trying to acquire the
	 * 	repository
	 */
	public Repository acquireRepository(String dirName) throws RepositoryAcquisitionException {
		Path repoDir = getRepoDir(dirName);

		RepositoryBuilder b = new RepositoryBuilder();
		b.setGitDir(repoDir.toFile());

		try {
			// constructor acquires read lock for this storage
			return new RepositoryLock(this.lock.readLock(), b);
		} catch (IOException e) {
			throw new RepositoryAcquisitionException(this, dirName, e);
		}
	}

	/**
	 * Gets the path to the directory whose name is the one specified.
	 *
	 * @param dirName the name of the directory where the repository is located at
	 * @return the path of a repository directory by the given directory name
	 * @throws NoSuchRepositoryException if no repository can be found at the specified directory
	 */
	public Path getRepoDir(String dirName) throws NoSuchRepositoryException {
		Path repoDir = rootDir.resolve(dirName);

		if (Files.exists(repoDir) && Files.isDirectory(repoDir)) {
			return rootDir.resolve(dirName);
		} else {
			throw new NoSuchRepositoryException(dirName, this);
		}
	}

}
