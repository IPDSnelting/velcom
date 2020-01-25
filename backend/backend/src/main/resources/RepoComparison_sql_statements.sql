SELECT run.id, run_measurement.id, run_measurement.benchmark, run_measurement.metric, run_measurement.interpretation, run_measurement.unit
FROM run
	JOIN run_measurement ON run.id = run_measurement.run_id
WHERE run.repo_id = "4922ce89-f92f-462e-8fd5-2aad070c0757"
AND run.commit_hash IN (SELECT run.commit_hash FROM run ORDER BY run.id DESC LIMIT 1000)
AND run_measurement.error_message IS NULL;

SELECT run.id, run_measurement.benchmark, run_measurement.metric, run_measurement_value."value"
FROM run
	JOIN run_measurement ON run.id = run_measurement.run_id
	JOIN run_measurement_value ON run_measurement.id = run_measurement_value.measurement_id
WHERE run.repo_id = "4922ce89-f92f-462e-8fd5-2aad070c0757"
AND run.commit_hash IN (SELECT run.commit_hash FROM run ORDER BY run.id DESC LIMIT 1000)
AND run_measurement.error_message IS NULL;