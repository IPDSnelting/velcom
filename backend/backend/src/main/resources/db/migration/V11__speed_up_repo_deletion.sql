CREATE INDEX idx_commit_relationship_parent
ON commit_relationship(repo_id, parent_hash);

CREATE INDEX idx_measurement_rid
ON measurement(run_id);
