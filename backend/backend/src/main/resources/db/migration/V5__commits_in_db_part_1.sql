-----------------------------------
-- Renaming to-be-deleted tables --
-----------------------------------

ALTER TABLE known_commit RENAME TO known_commit_old;

-------------------------
-- Creating new tables --
-------------------------

CREATE TABLE known_commit (
  migrated       BOOLEAN  NOT NULL,
  repo_id        CHAR(36) NOT NULL,
  hash           CHAR(40) NOT NULL,
  reachable      BOOLEAN  NOT NULL,
  tracked        BOOLEAN  NOT NULL,
  author         TEXT,
  author_date    TIMESTAMP,
  committer      TEXT,
  committer_date TIMESTAMP,
  message        TEXT,

  PRIMARY KEY (repo_id, hash),
  FOREIGN KEY (repo_id) REFERENCES repo (id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE commit_relationship (
  repo_id     CHAR(36) NOT NULL,
  parent_hash CHAR(40) NOT NULL,
  child_hash  CHAR(40) NOT NULL,

  FOREIGN KEY (repo_id) REFERENCES repo (id) ON UPDATE CASCADE ON DELETE CASCADE,
  FOREIGN KEY (repo_id, parent_hash) REFERENCES known_commit (repo_id, hash) ON UPDATE CASCADE ON DELETE CASCADE,
  FOREIGN KEY (repo_id, child_hash) REFERENCES known_commit (repo_id, hash) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE branch (
  migrated           BOOLEAN  NOT NULL,
  repo_id            CHAR(36) NOT NULL,
  name               TEXT     NOT NULL,
  latest_commit_hash CHAR(40),
  tracked            BOOLEAN  NOT NULL,

  PRIMARY KEY (repo_id, name),
  FOREIGN KEY (repo_id) REFERENCES repo (id) ON UPDATE CASCADE ON DELETE CASCADE,
  FOREIGN KEY (repo_id, latest_commit_hash) REFERENCES known_commit (repo_id, hash) ON UPDATE CASCADE ON DELETE CASCADE
);

------------------------
-- Filling new tables --
------------------------

INSERT INTO known_commit
SELECT
  FALSE,   -- migrated
  repo_id, -- repo_id
  hash,    -- hash
  FALSE,   -- reachable
  FALSE,   -- tracked
  NULL,    -- author
  NULL,    -- author_date
  NULL,    -- committer
  NULL,    -- committer_date
  NULL     -- message
FROM known_commit_old;

INSERT INTO branch
SELECT
  FALSE,   -- migrated
  repo_id, -- repo_id
  name,    -- name
  NULL,    -- latest_commit_hash
  TRUE     -- tracked
FROM tracked_branch;

-------------------------------
-- Getting rid of old tables --
-------------------------------

DROP TABLE known_commit_old;
DROP TABLE tracked_branch;

-- A little dance to capture all foreign key constraints previously pointing to known_commit
ALTER TABLE known_commit RENAME TO known_commit_old;
ALTER TABLE known_commit_old RENAME TO known_commit;

--------------------------------
-- Sprinkling in some indices --
--------------------------------

CREATE INDEX idx_commit_relationship_upwards
  ON commit_relationship(repo_id, child_hash, parent_hash);

-----------------------------------
-- Ensure foreign keys are valid --
-----------------------------------

PRAGMA foreign_key_check;
