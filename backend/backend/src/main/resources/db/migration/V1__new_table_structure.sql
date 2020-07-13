CREATE TABLE repo
(
    id         CHAR(36) PRIMARY KEY NOT NULL,
    name       TEXT                 NOT NULL,
    remote_url TEXT                 NOT NULL
);

CREATE TABLE tracked_branch
(
    repo_id CHAR(36) NOT NULL,
    name    TEXT     NOT NULL,

    PRIMARY KEY (repo_id, name),
    FOREIGN KEY (repo_id) REFERENCES repo (id) ON DELETE CASCADE
);

CREATE TABLE known_commit
(
    repo_id    CHAR(36)  NOT NULL,
    hash       CHAR(40)  NOT NULL,
    first_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (repo_id, hash),
    FOREIGN KEY (repo_id) REFERENCES repo (id) ON DELETE CASCADE
);

CREATE TABLE run
(
    id          CHAR(36) PRIMARY KEY NOT NULL,
    author      TEXT                 NOT NULL,
    runner_name TEXT                 NOT NULL,
    runner_info TEXT                 NOT NULL,
    start_time  TIMESTAMP            NOT NULL,
    stop_time   TIMESTAMP            NOT NULL,
    repo_id     CHAR(36),
    commit_hash CHAR(40),
    error       TEXT,
    error_type  TEXT CHECK ( error_type IN ('BENCH', 'VELCOM')),

    FOREIGN KEY (repo_id, commit_hash) REFERENCES known_commit (repo_id, hash) ON DELETE CASCADE,

    CHECK ( (repo_id IS NOT NULL AND commit_hash IS NOT NULL) OR
            (repo_id IS NULL AND commit_hash IS NULL) ),
    CHECK ( (error IS NOT NULL AND error_type IS NOT NULL) OR
            (error IS NULL AND error_type IS NULL) )
);

CREATE TABLE measurement
(
    id             CHAR(36) PRIMARY KEY NOT NULL,
    run_id         CHAR(36)             NOT NULL,
    benchmark      TEXT                 NOT NULL,
    metric         TEXT                 NOT NULL,
    unit           TEXT                 NOT NULL,
    interpretation TEXT                 NOT NULL CHECK ( interpretation IN
                                                         ('NEUTRAL', 'LESS_IS_BETTER', 'MORE_IS_BETTER') ),
    error          TEXT,

    FOREIGN KEY (run_id) REFERENCES run (id) ON DELETE CASCADE
);

CREATE TABLE measurement_value
(
    measurement_id CHAR(36) NOT NULL,
    value          DOUBLE   NOT NULL,

    FOREIGN KEY (measurement_id) REFERENCES measurement (id) ON DELETE CASCADE
);

CREATE TABLE task
(
    id          CHAR(36) PRIMARY KEY NOT NULL,
    author      TEXT                 NOT NULL,
    priority    INTEGER              NOT NULL,
    insert_time TIMESTAMP            NOT NULL,
    update_time TIMESTAMP            NOT NULL,
    tar_name    TEXT,
    repo_id     CHAR(36),
    commit_hash CHAR(40),
    in_process  BOOLEAN              NOT NULL DEFAULT false,

    FOREIGN KEY (repo_id, commit_hash) REFERENCES known_commit (repo_id, hash) ON DELETE CASCADE,

    CHECK ( (repo_id IS NOT NULL AND commit_hash IS NOT NULL) OR
            (repo_id IS NULL AND commit_hash IS NULL) ),
    CHECK ( (tar_name IS NOT NULL AND repo_id IS NULL) OR
            (tar_name IS NULL AND repo_id IS NOT NULL) )
);

CREATE TABLE repo_token
(
    repo_id   CHAR(36) PRIMARY KEY NOT NULL,
    token     TEXT                 NOT NULL,
    hash_algo INTEGER              NOT NULL,

    FOREIGN KEY (repo_id) REFERENCES repo (id) ON DELETE CASCADE
);

CREATE INDEX idx_rm_rid ON measurement (run_id);

CREATE INDEX idx_rm_id_rid ON measurement (id, run_id);

CREATE INDEX idx_rmv_mid ON measurement_value (measurement_id);
