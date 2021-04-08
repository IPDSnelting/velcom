ALTER TABLE known_commit RENAME TO known_commit_old;

CREATE TABLE known_commit (
  repo_id        CHAR(36)  NOT NULL,
  hash           CHAR(40)  NOT NULL,
  reachable      BOOLEAN   NOT NULL, -- Whether this commit is reachable from any branch
  tracked        BOOLEAN   NOT NULL, -- Whether this commit is reachable from a tracked branch
  ever_tracked   BOOLEAN   NOT NULL, -- Whether this commit has ever been tracked
  author         TEXT      NOT NULL,
  author_date    TIMESTAMP NOT NULL,
  committer      TEXT      NOT NULL,
  committer_date TIMESTAMP NOT NULL,
  message        TEXT      NOT NULL,

  -- "tracked" implies "ever_tracked"
  CHECK (NOT tracked OR ever_tracked),

  PRIMARY KEY (repo_id, hash),
  FOREIGN KEY (repo_id) REFERENCES repo (id) ON UPDATE CASCADE ON DELETE CASCADE
);

INSERT INTO known_commit
SELECT
  repo_id,
  hash,
  reachable,
  tracked,
  tracked,
  author,
  author_date,
  committer,
  committer_date,
  message
FROM known_commit_old;

DROP TABLE known_commit_old;

-- Recapture foreign key references
ALTER TABLE known_commit RENAME TO known_commit_old;
ALTER TABLE known_commit_old RENAME TO known_commit;
