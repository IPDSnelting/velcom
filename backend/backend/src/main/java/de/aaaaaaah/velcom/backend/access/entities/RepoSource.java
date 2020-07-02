package de.aaaaaaah.velcom.backend.access.entities;

import java.util.Objects;

public class RepoSource {

    private final RepoId repoId;
    private final CommitHash hash;

    public RepoSource(RepoId repoId, CommitHash hash) {
        this.repoId = Objects.requireNonNull(repoId);
        this.hash = Objects.requireNonNull(hash);
    }

    public RepoId getRepoId() {
        return repoId;
    }

    public CommitHash getHash() {
        return hash;
    }

    @Override
    public String toString() {
        return "RepoSource{" +
                "repoId=" + repoId +
                ", hash=" + hash +
                '}';
    }

}
