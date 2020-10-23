-----------------------------------
-- Renaming to-be-deleted tables --
-----------------------------------

ALTER TABLE known_commit RENAME TO known_commit_old;
ALTER TABLE branch RENAME TO branch_old;

-------------------------
-- Creating new tables --
-------------------------

CREATE TABLE known_commit (
  repo_id        CHAR(36)  NOT NULL,
  hash           CHAR(40)  NOT NULL,
  reachable      BOOLEAN   NOT NULL,
  tracked        BOOLEAN   NOT NULL,
  author         TEXT      NOT NULL,
  author_date    TIMESTAMP NOT NULL,
  committer      TEXT      NOT NULL,
  committer_date TIMESTAMP NOT NULL,
  message        TEXT      NOT NULL,

  PRIMARY KEY (repo_id, hash),
  FOREIGN KEY (repo_id) REFERENCES repo (id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE branch (
  repo_id            CHAR(36) NOT NULL,
  name               TEXT     NOT NULL,
  latest_commit_hash CHAR(40) NOT NULL,
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
  repo_id,
  hash,
  reachable,
  tracked,
  author,
  author_date,
  committer,
  committer_date,
  message
FROM known_commit_old;

INSERT INTO branch
SELECT
  repo_id,
  name,
  latest_commit_hash,
  tracked
FROM branch_old;

-------------------------
-- Deleting old tables --
-------------------------

DROP TABLE known_commit_old;
DROP TABLE branch_old;

-- A little dance to capture all foreign key constraints previously pointing to known_commit
ALTER TABLE known_commit RENAME TO known_commit_old;
ALTER TABLE known_commit_old RENAME TO known_commit;

-----------------------------------
-- Ensure foreign keys are valid --
-----------------------------------

PRAGMA foreign_key_check;
