package de.aaaaaaah.designproto.backend.storage.repo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.attributes.AttributesNodeProvider;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.events.ListenerList;
import org.eclipse.jgit.events.RepositoryEvent;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.BaseRepositoryBuilder;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefRename;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.FS;

/**
 * A wrapper for a jgit {@link Repository} instance that is able to hold a read lock while the
 * repository is used.
 */
public class RepositoryLock extends Repository {

	private final Repository repository;
	private final Lock readLock;

	/**
	 * Constructs a new repository lock.
	 *
	 * @param readLock the read lock to immediately acquire
	 * @param options options used to create the internal jgit repository instance
	 * @throws IOException if the repository could not be accessed to configure the rest of the
	 * 	builder's parameters.
	 */
	RepositoryLock(Lock readLock, BaseRepositoryBuilder options) throws IOException {
		super(options);
		this.repository = options.build();
		this.readLock = readLock;
		this.readLock.lock();
	}

	public static ListenerList getGlobalListenerList() {
		return Repository.getGlobalListenerList();
	}

	@Override
	@NonNull
	public ListenerList getListenerList() {
		return repository.getListenerList();
	}

	@Override
	public void fireEvent(RepositoryEvent<?> event) {
		repository.fireEvent(event);
	}

	@Override
	public void create() throws IOException {
		repository.create();
	}

	@Override
	public void create(boolean b) throws IOException {
		repository.create(b);
	}

	@Override
	public File getDirectory() {
		return repository.getDirectory();
	}

	@Override
	public String getIdentifier() {
		return repository.getIdentifier();
	}

	@Override
	@NonNull
	public ObjectDatabase getObjectDatabase() {
		return repository.getObjectDatabase();
	}

	@Override
	@NonNull
	public ObjectInserter newObjectInserter() {
		return repository.newObjectInserter();
	}

	@Override
	@NonNull
	public ObjectReader newObjectReader() {
		return repository.newObjectReader();
	}

	@Override
	@NonNull
	public RefDatabase getRefDatabase() {
		return repository.getRefDatabase();
	}

	@Override
	@NonNull
	public StoredConfig getConfig() {
		return repository.getConfig();
	}

	@Override
	@NonNull
	public AttributesNodeProvider createAttributesNodeProvider() {
		return repository.createAttributesNodeProvider();
	}

	@Override
	public FS getFS() {
		return repository.getFS();
	}

	@Override
	@Deprecated
	public boolean hasObject(AnyObjectId objectId) {
		return repository.hasObject(objectId);
	}

	@Override
	@NonNull
	public ObjectLoader open(AnyObjectId objectId) throws MissingObjectException, IOException {
		return repository.open(objectId);
	}

	@Override
	@NonNull
	public ObjectLoader open(AnyObjectId objectId,
		int typeHint) throws MissingObjectException, IncorrectObjectTypeException, IOException {
		return repository.open(objectId, typeHint);
	}

	@Override
	@NonNull
	public RefUpdate updateRef(String ref) throws IOException {
		return repository.updateRef(ref);
	}

	@Override
	@NonNull
	public RefUpdate updateRef(String ref, boolean detach) throws IOException {
		return repository.updateRef(ref, detach);
	}

	@Override
	@NonNull
	public RefRename renameRef(String fromRef, String toRef) throws IOException {
		return repository.renameRef(fromRef, toRef);
	}

	@Override
	@Nullable
	public ObjectId resolve(String revstr)
		throws AmbiguousObjectException, IncorrectObjectTypeException, RevisionSyntaxException, IOException {
		return repository.resolve(revstr);
	}

	@Override
	@Nullable
	public String simplify(String revstr) throws AmbiguousObjectException, IOException {
		return repository.simplify(revstr);
	}

	@Override
	public void incrementOpen() {
		repository.incrementOpen();
	}

	@Override
	public void close() {
		readLock.unlock();
		repository.close();
	}

	@Override
	@NonNull
	public String toString() {
		return repository.toString();
	}

	@Override
	@Nullable
	public String getFullBranch() throws IOException {
		return repository.getFullBranch();
	}

	@Override
	@Nullable
	public String getBranch() throws IOException {
		return repository.getBranch();
	}

	@Override
	@NonNull
	public Set<ObjectId> getAdditionalHaves() {
		return repository.getAdditionalHaves();
	}

	@Override
	@NonNull
	@Deprecated
	public Map<String, Ref> getAllRefs() {
		return repository.getAllRefs();
	}

	@Override
	@NonNull
	@Deprecated
	public Map<String, Ref> getTags() {
		return repository.getTags();
	}

	@Override
	@NonNull
	@Deprecated
	public Ref peel(Ref ref) {
		return repository.peel(ref);
	}

	@Override
	@NonNull
	public Map<AnyObjectId, Set<Ref>> getAllRefsByPeeledObjectId() {
		return repository.getAllRefsByPeeledObjectId();
	}

	@Override
	@NonNull
	public File getIndexFile() throws NoWorkTreeException {
		return repository.getIndexFile();
	}

	@Override
	public RevCommit parseCommit(AnyObjectId id)
		throws IncorrectObjectTypeException, IOException, MissingObjectException {
		return repository.parseCommit(id);
	}

	@Override
	@NonNull
	public DirCache readDirCache() throws NoWorkTreeException, CorruptObjectException, IOException {
		return repository.readDirCache();
	}

	@Override
	@NonNull
	public DirCache lockDirCache() throws NoWorkTreeException, CorruptObjectException, IOException {
		return repository.lockDirCache();
	}

	@Override
	@NonNull
	public RepositoryState getRepositoryState() {
		return repository.getRepositoryState();
	}

	/**
	 * Check validity of a ref name. It must not contain character that has a special meaning in a
	 * Git object reference expression. Some other dangerous characters are also excluded.
	 *
	 * <p>For portability reasons '\' is excluded</p>
	 *
	 * @param refName a {@link java.lang.String} object.
	 * @return true if refName is a valid ref name
	 */
	public static boolean isValidRefName(String refName) {
		return Repository.isValidRefName(refName);
	}

	/**
	 * Normalizes the passed branch name into a possible valid branch name. The validity of the
	 * returned name should be checked by a subsequent call to {@link #isValidRefName(String)}.
	 *
	 * <p>
	 * Future implementations of this method could be more restrictive or more lenient about the
	 * validity of specific characters in the returned name.
	 *
	 * <p>
	 * The current implementation returns the trimmed input string if this is already a valid branch
	 * name. Otherwise it returns a trimmed string with special characters not allowed by {@link
	 * #isValidRefName(String)} replaced by hyphens ('-') and blanks replaced by underscores ('_').
	 * Leading and trailing slashes, dots, hyphens, and underscores are removed.
	 *
	 * @param name to normalize
	 * @return The normalized name or an empty String if it is {@code null} or empty.
	 * @see #isValidRefName(String)
	 * @since 4.7
	 */
	public static String normalizeBranchName(String name) {
		return Repository.normalizeBranchName(name);
	}

	/**
	 * Strip work dir and return normalized repository path.
	 *
	 * @param workDir Work dir
	 * @param file File whose path shall be stripped of its workdir
	 * @return normalized repository relative path or the empty string if the file is not relative
	 * 	to the work directory.
	 */
	@NonNull
	public static String stripWorkDir(File workDir, File file) {
		return Repository.stripWorkDir(workDir, file);
	}

	@Override
	public boolean isBare() {
		return repository.isBare();
	}

	@Override
	@NonNull
	public File getWorkTree() throws NoWorkTreeException {
		return repository.getWorkTree();
	}

	@Override
	public void scanForRepoChanges() throws IOException {
		repository.scanForRepoChanges();
	}

	@Override
	public void notifyIndexChanged(boolean b) {
		repository.notifyIndexChanged(b);
	}

	/**
	 * Get a shortened more user friendly ref name.
	 *
	 * @param refName a {@link java.lang.String} object.
	 * @return a more user friendly ref name
	 */
	@NonNull
	public static String shortenRefName(String refName) {
		return Repository.shortenRefName(refName);
	}

	@Override
	@Nullable
	public String shortenRemoteBranchName(String refName) {
		return repository.shortenRemoteBranchName(refName);
	}

	@Override
	@Nullable
	public String getRemoteName(String refName) {
		return repository.getRemoteName(refName);
	}

	@Override
	@Nullable
	public String getGitwebDescription() throws IOException {
		return repository.getGitwebDescription();
	}

	@Override
	public void setGitwebDescription(String description) throws IOException {
		repository.setGitwebDescription(description);
	}

	@Override
	@Nullable
	public ReflogReader getReflogReader(String s) throws IOException {
		return repository.getReflogReader(s);
	}

	@Override
	@Nullable
	public String readMergeCommitMsg() throws IOException, NoWorkTreeException {
		return repository.readMergeCommitMsg();
	}

	@Override
	public void writeMergeCommitMsg(String msg) throws IOException {
		repository.writeMergeCommitMsg(msg);
	}

	@Override
	@Nullable
	public String readCommitEditMsg() throws IOException, NoWorkTreeException {
		return repository.readCommitEditMsg();
	}

	@Override
	public void writeCommitEditMsg(String msg) throws IOException {
		repository.writeCommitEditMsg(msg);
	}

	@Override
	@Nullable
	public List<ObjectId> readMergeHeads() throws IOException, NoWorkTreeException {
		return repository.readMergeHeads();
	}

	@Override
	public void writeMergeHeads(List<? extends ObjectId> heads) throws IOException {
		repository.writeMergeHeads(heads);
	}

	@Override
	@Nullable
	public ObjectId readCherryPickHead() throws IOException, NoWorkTreeException {
		return repository.readCherryPickHead();
	}

	@Override
	@Nullable
	public ObjectId readRevertHead() throws IOException, NoWorkTreeException {
		return repository.readRevertHead();
	}

	@Override
	public void writeCherryPickHead(ObjectId head) throws IOException {
		repository.writeCherryPickHead(head);
	}

	@Override
	public void writeRevertHead(ObjectId head) throws IOException {
		repository.writeRevertHead(head);
	}

	@Override
	public void writeOrigHead(ObjectId head) throws IOException {
		repository.writeOrigHead(head);
	}

	@Override
	@Nullable
	public ObjectId readOrigHead() throws IOException, NoWorkTreeException {
		return repository.readOrigHead();
	}

	@Override
	@Nullable
	public String readSquashCommitMsg() throws IOException {
		return repository.readSquashCommitMsg();
	}

	@Override
	public void writeSquashCommitMsg(String msg) throws IOException {
		repository.writeSquashCommitMsg(msg);
	}

	@Override
	@NonNull
	public List<RebaseTodoLine> readRebaseTodo(String path,
		boolean includeComments) throws IOException {
		return repository.readRebaseTodo(path, includeComments);
	}

	@Override
	public void writeRebaseTodoFile(String path,
		List<RebaseTodoLine> steps, boolean append) throws IOException {
		repository.writeRebaseTodoFile(path, steps, append);
	}

	@Override
	@NonNull
	public Set<String> getRemoteNames() {
		return repository.getRemoteNames();
	}

	@Override
	public void autoGC(ProgressMonitor monitor) {
		repository.autoGC(monitor);
	}

}
