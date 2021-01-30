CREATE VIEW latest_run (
  id,
  author,
  runner_name,
  runner_info,
  start_time,
  stop_time,
  repo_id,
  commit_hash,
  tar_desc,
  error_type,
  error
) AS
SELECT
  id,
  author,
  runner_name,
  runner_info,
  MAX(start_time), -- sqlite-specific
  stop_time,
  repo_id,
  commit_hash,
  tar_desc,
  error_type,
  error
FROM run
WHERE commit_hash IS NOT NULL
GROUP BY repo_id, commit_hash
UNION
SELECT
  id,
  author,
  runner_name,
  runner_info,
  start_time,
  stop_time,
  repo_id,
  commit_hash,
  tar_desc,
  error_type,
  error
FROM run
WHERE commit_hash IS NULL;
