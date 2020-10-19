package de.aaaaaaah.velcom.backend.listener.jgitutils;


import static java.util.stream.Collectors.toSet;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public class JgitCommitWalk implements AutoCloseable {

	private final Repository repo;
	private final RevWalk walk;

	public JgitCommitWalk(Repository repo) {
		this.repo = repo;
		this.walk = new RevWalk(repo);
	}

	private static String formatPersonIdent(PersonIdent ident) {
		final String name = ident.getName();

		if (name.isEmpty()) {
			return "<" + ident.getEmailAddress() + ">";
		} else {
			return name + " <" + ident.getEmailAddress() + ">";
		}
	}

	private JgitCommit revCommitToJgitCommit(RevCommit revCommit) {
		CommitHash hash = new CommitHash(revCommit.getId().getName());

		Set<CommitHash> parentHashes = List.of(revCommit.getParents()).stream()
			.map(RevCommit::getId)
			.map(AnyObjectId::getName)
			.map(CommitHash::new)
			.collect(toSet());

		PersonIdent authorIdent = revCommit.getAuthorIdent();
		PersonIdent committerIdent = revCommit.getCommitterIdent();

		return new JgitCommit(
			hash,
			parentHashes,
			formatPersonIdent(authorIdent),
			authorIdent.getWhen().toInstant(),
			formatPersonIdent(committerIdent),
			committerIdent.getWhen().toInstant(),
			revCommit.getFullMessage()
		);
	}

	public Optional<JgitCommit> getCommit(CommitHash hash) {
		try {
			ObjectId commitPtr = repo.resolve(hash.getHash());
			RevCommit revCommit = walk.parseCommit(commitPtr);
			return Optional.of(revCommitToJgitCommit(revCommit));
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	@Override
	public void close() {
		walk.close();
		repo.close();
	}

}
