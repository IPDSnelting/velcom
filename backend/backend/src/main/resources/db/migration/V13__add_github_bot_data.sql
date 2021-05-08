-----------------------------------
-- Renaming to-be-deleted tables --
-----------------------------------

ALTER TABLE repo RENAME TO repo_old;

-------------------------
-- Creating new tables --
-------------------------

CREATE TABLE repo (
  id                    CHAR(36) PRIMARY KEY  NOT NULL,
  name                  TEXT                  NOT NULL,
  remote_url            TEXT                  NOT NULL,
  github_auth_token     TEXT,
  github_comment_cutoff TIMESTAMP,

  CHECK ((github_auth_token IS NULL) == (github_comment_cutoff IS NULL))
);

CREATE TABLE github_pr (
  repo_id      CHAR(36) NOT NULL,
  pr           BIGINT   NOT NULL,
  last_comment BIGINT   NOT NULL,

  PRIMARY KEY (repo_id, pr),
  FOREIGN KEY (repo_id) REFERENCES repo(id)
);

CREATE TABLE github_command (
  repo_id     CHAR(36) NOT NULL,
  pr          BIGINT   NOT NULL,
  comment     BIGINT   NOT NULL,
  commit_hash CHAR(40) NOT NULL,
  state       TEXT     NOT NULL DEFAULT "NEW",
  tries_left  INT      NOT NULL,

  PRIMARY KEY (repo_id, comment),
  FOREIGN KEY (repo_id) REFERENCES repo(id),
  -- No foreign key for the commit because VelCom might not yet have the commit synchronized when a
  -- new row is added to this table.

  CHECK (state in ("NEW", "MARKED_SEEN", "QUEUED", "RESPONDED", "ERROR"))
);

------------------------
-- Filling new tables --
------------------------

INSERT INTO repo
SELECT id, name, remote_url, NULL, NULL
FROM repo_old;

-------------------------
-- Deleting old tables --
-------------------------

DROP TABLE repo_old;

-- A little dance to capture all foreign key constraints previously pointing to repo
ALTER TABLE repo RENAME TO repo_old;
ALTER TABLE repo_old RENAME TO repo;

-----------------------------------
-- Ensure foreign keys are valid --
-----------------------------------

PRAGMA foreign_key_check;
