#!/bin/bash

echo "Pruning tables..."
sqlite3 data/data.db "PRAGMA foreign_keys = ON; DELETE FROM RUN WHERE id NOT IN (SELECT id FROM run ORDER BY start_time DESC LIMIT 50);"
sqlite3 data/data.db "DROP TABLE known_commit; DROP TABLE commit_relationship"
sqlite3 data/data.db "DELETE FROM repo WHERE id NOT IN ('44bb5c8d-b20d-4bef-bdad-c92767dfa489', 'd471b648-ce65-41e2-9c44-84fb82b73100');"
sqlite3 data/data.db "DELETE FROM repo_token WHERE repo_id NOT IN ('44bb5c8d-b20d-4bef-bdad-c92767dfa489', 'd471b648-ce65-41e2-9c44-84fb82b73100');"

echo -e "\nDumping INSERTs"
sqlite3 data/data.db '.dump "repo" "repo_token" "run" "measurement" "measurement_value"' > init_data.sql
sed -i 's/CREATE TABLE repo_token/CREATE TABLE IF NOT EXISTS repo_token/' init_data.sql
