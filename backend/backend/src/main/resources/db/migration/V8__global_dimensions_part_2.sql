-----------------------------------
-- Renaming to-be-deleted tables --
-----------------------------------

ALTER TABLE measurement RENAME TO measurement_old;

-------------------------
-- Creating new tables --
-------------------------

CREATE TABLE IF NOT EXISTS measurement (
  id             CHAR(36) NOT NULL PRIMARY KEY,
  run_id         CHAR(36) NOT NULL,
  benchmark      TEXT     NOT NULL,
  metric         TEXT     NOT NULL,
  unit           TEXT,
  interpretation TEXT,
  error          TEXT,

  FOREIGN KEY (run_id) REFERENCES run(id) ON UPDATE CASCADE ON DELETE CASCADE,
  FOREIGN KEY (benchmark, metric) REFERENCES dimension(benchmark, metric) ON UPDATE CASCADE ON DELETE CASCADE,

  CHECK (interpretation IN ('LESS_IS_BETTER', 'MORE_IS_BETTER', 'NEUTRAL'))
);

------------------------
-- Filling new tables --
------------------------

INSERT INTO measurement
SELECT *
FROM measurement_old;

-------------------------
-- Deleting old tables --
-------------------------

DROP TABLE measurement_old;

-- A little dance to capture all foreign key constraints previously pointing to measurement
ALTER TABLE measurement RENAME TO measurement_old;
ALTER TABLE measurement_old RENAME TO measurement;

-----------------------------------
-- Ensure foreign keys are valid --
-----------------------------------

PRAGMA foreign_key_check;
