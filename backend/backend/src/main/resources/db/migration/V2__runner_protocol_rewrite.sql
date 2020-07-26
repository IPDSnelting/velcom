-- Following this guide:
-- https://sqlite.org/lang_altertable.html#making_other_kinds_of_table_schema_changes

---------------------------
-- General small changes --
---------------------------

ALTER TABLE repository RENAME TO repo;
ALTER TABLE tracked_branch RENAME COLUMN branch_name TO name;

-------------------------
-- Creating new tables --
-------------------------

CREATE TABLE new_known_commit (
  repo_id    CHAR(36)  NOT NULL,
  hash       CHAR(40)  NOT NULL,
  first_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (repo_id, hash),
  FOREIGN KEY (repo_id) REFERENCES repo(id) ON DELETE CASCADE
);

CREATE TABLE task (
  id          CHAR(36)  NOT NULL PRIMARY KEY,
  author      TEXT      NOT NULL,
  priority    INTEGER   NOT NULL,
  insert_time TIMESTAMP NOT NULL,
  update_time TIMESTAMP NOT NULL,
  repo_id     CHAR(36),
  commit_hash CHAR(40),
  tar_name    TEXT,
  tar_desc    TEXT,
  in_process  BOOLEAN   NOT NULL DEFAULT false,

  FOREIGN KEY (repo_id) REFERENCES repo(id) ON DELETE CASCADE,
  FOREIGN KEY (repo_id, commit_hash) REFERENCES new_known_commit(repo_id, hash) ON DELETE CASCADE,

  CHECK ((repo_id IS NULL) = (commit_hash IS NULL)),
  CHECK ((tar_name IS NULL) = (tar_desc IS NULL)),
  CHECK ((commit_hash IS NULL) = (tar_name IS NOT NULL))
);

CREATE TABLE new_run (
  id          CHAR(36)  NOT NULL PRIMARY KEY,
  author      TEXT      NOT NULL,
  runner_name TEXT      NOT NULL,
  runner_info TEXT      NOT NULL,
  start_time  TIMESTAMP NOT NULL,
  stop_time   TIMESTAMP NOT NULL,
  repo_id     CHAR(36),
  commit_hash CHAR(36),
  tar_desc    TEXT,
  error_type  TEXT,
  error       TEXT,

  FOREIGN KEY (repo_id) REFERENCES repo(id) ON DELETE CASCADE,
  FOREIGN KEY (repo_id, commit_hash) REFERENCES new_known_commit(repo_id, hash) ON DELETE CASCADE,

  CHECK ((repo_id IS NULL) = (commit_hash IS NULL)),
  CHECK ((commit_hash IS NULL) = (tar_desc IS NOT NULL)),
  CHECK (error_type IS NULL OR error_type IN ('BENCH', 'VELCOM')),
  CHECK ((error_type IS NULL) = (error IS NULL))
);

CREATE TABLE new_measurement (
  id             CHAR(36) NOT NULL PRIMARY KEY,
  run_id         CHAR(36) NOT NULL,
  benchmark      TEXT     NOT NULL,
  metric         TEXT     NOT NULL,
  unit           TEXT,
  interpretation TEXT,
  error          TEXT,

  FOREIGN KEY (run_id) REFERENCES new_run(id) ON DELETE CASCADE,

  CHECK (interpretation IN ('LESS_IS_BETTER', 'MORE_IS_BETTER', 'NEUTRAL'))
);

CREATE TABLE new_measurement_value (
  measurement_id CHAR(36) NOT NULL,
  value          DOUBLE   NOT NULL,

  FOREIGN KEY (measurement_id) REFERENCES new_measurement(id) ON DELETE CASCADE
);

------------------------
-- Filling new tables --
------------------------

INSERT INTO new_known_commit
SELECT
  repo_id,
  hash,
  insert_time -- first_seen
FROM known_commit;

INSERT INTO "task"
SELECT
  ( -- Create a random UUID
    -- Based on https://stackoverflow.com/a/22725697 and
    -- https://stackoverflow.com/a/105074
    SELECT
      SUBSTR(u, 1, 8)
      || '-' || SUBSTR(u, 9, 4)
      || '-4' || SUBSTR(u, 14, 3)
      || '-' || v || SUBSTR(u, 18, 3)
      || '-' || SUBSTR(u, 21, 12)
    FROM (SELECT
      LOWER(HEX(RANDOMBLOB(16))) AS u,
      SUBSTR('89ab', ABS(RANDOM())%4+1, 1) AS v
    )
  ), -- id
  'migrated', -- author
  CASE
    WHEN status = 1 THEN 2 -- benchmark required
    WHEN status = 2 THEN 0 -- benchmark required, manual priority
  END, -- priority
  insert_time,
  update_time,
  repo_id,
  hash, -- commit_hash
  NULL, -- tar_name
  NULL, -- tar_desc
  false -- in_process
FROM known_commit
WHERE status IN (1, 2);

INSERT INTO new_run
SELECT
  id,
  'migrated',    -- author
  'migrated',    -- runner_name
  'migrated',    -- runner_info
  start_time,
  stop_time,
  repo_id,
  commit_hash,
  NULL,          -- tar_desc
  CASE
    WHEN error_message IS NULL THEN NULL
    ELSE 'VELCOM'
  END, -- error_type
  error_message -- error
FROM run;

INSERT INTO new_measurement
SELECT
  id,
  run_id,
  benchmark,
  metric,
  unit,
  interpretation,
  error_message -- error
FROM run_measurement;

INSERT INTO new_measurement_value
SELECT measurement_id, value
FROM run_measurement_value;

-------------------------------
-- Getting rid of old tables --
-------------------------------

DROP TABLE known_commit;
DROP TABLE run_measurement_value;
DROP TABLE run_measurement;
DROP TABLE run;

ALTER TABLE new_known_commit RENAME TO known_commit;
ALTER TABLE new_measurement_value RENAME TO measurement_value;
ALTER TABLE new_measurement RENAME TO measurement;
ALTER TABLE new_run RENAME TO run;

------------------------
-- Recreating indices --
------------------------

CREATE INDEX idx_measurement_rid ON measurement(run_id);
CREATE INDEX idx_measurement_id_rid ON measurement(id, run_id);
CREATE INDEX idx_measurement_value_id_rid ON measurement(id, run_id);

-----------------------------------
-- Ensure foreign keys are valid --
-----------------------------------

PRAGMA foreign_key_check;
