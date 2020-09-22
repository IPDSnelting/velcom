-- Unnecessary old indices
DROP INDEX idx_measurement_id_rid;
DROP INDEX idx_measurement_value_id_rid;

-- This index speeds up some queries by two to three orders of magnitude...
CREATE INDEX idx_measurement_value_mid ON measurement_value(measurement_id);
-- This one only by one order of magnitude, but hey, that's not bad either.
CREATE INDEX idx_run_rid_ch ON run(repo_id, commit_hash);
