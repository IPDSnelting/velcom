-- Changes:
-- * Tars are now identified by the task ID, not a separate Tar name
--   * The "tar_name" field has been removed
--   * The corresponding checks have been removed or adjusted
-- * The "tar_desc" field has been renamed to a more generic "description"
--
-- These changes affect only the "task" table.
-- Since sqlite can't delete columns, we need to re-create the task table for this change.
-- Luckily, it is not referenced via foreign key by any other tables.

-------------------------
-- Creating new tables --
-------------------------

CREATE TABLE new_task (
  id          CHAR(36)  NOT NULL PRIMARY KEY,
  author      TEXT      NOT NULL,
  priority    INTEGER   NOT NULL,
  insert_time TIMESTAMP NOT NULL,
  update_time TIMESTAMP NOT NULL,
  repo_id     CHAR(36),
  commit_hash CHAR(40),
  -- At the moment, this field is only used for tar file descriptions
  description TEXT,
  in_process  BOOLEAN   NOT NULL DEFAULT false,

  FOREIGN KEY (repo_id) REFERENCES repo(id) ON DELETE CASCADE,
  -- If at least one value in (repo_id, commit_hash) is null, no corresponding row needs to exist in
  -- the foreign key table. See also https://sqlite.org/foreignkeys.html
  FOREIGN KEY (repo_id, commit_hash) REFERENCES known_commit(repo_id, hash) ON DELETE CASCADE,

  -- If a commit hash exists, a corresponding repo id must exist
  -- (commit_hash IS NOT NULL) => (repo_id IS NOT NULL)
  -- (a => b) is equivalent to (b or not a), resulting in this condition:
  CHECK ((repo_id IS NOT NULL) OR (commit_hash IS NULL)),

  -- There must always be either a commit hash or a (tar) description, but never both
  CHECK ((commit_hash IS NULL) = (description IS NOT NULL))
);

------------------------
-- Filling new tables --
------------------------

INSERT INTO new_task
SELECT
  id,
  author,
  priority,
  insert_time,
  update_time,
  repo_id,
  commit_hash,
  tar_desc,
  in_process
FROM task;

-------------------------------
-- Getting rid of old tables --
-------------------------------

DROP TABLE task;
ALTER TABLE new_task RENAME TO task;

-----------------------------------
-- Ensure foreign keys are valid --
-----------------------------------

PRAGMA foreign_key_check;
