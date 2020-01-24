CREATE TABLE repository
(
    id         CHAR(36) PRIMARY KEY  NOT NULL,
    name       TEXT                  NOT NULL,
    remote_url TEXT                  NOT NULL
);

CREATE TABLE tracked_branch
(
    repo_id     CHAR(36) NOT NULL,
    branch_name TEXT     NOT NULL,

    PRIMARY KEY (repo_id, branch_name),
    FOREIGN KEY (repo_id) REFERENCES repository (id) ON DELETE CASCADE
);

CREATE TABLE known_commit
(
    repo_id     CHAR(36)  NOT NULL,
    hash        CHAR(40)  NOT NULL,
    status      INTEGER   NOT NULL,
    update_time TIMESTAMP NOT NULL,
    insert_time TIMESTAMP NOT NULL,

    PRIMARY KEY (repo_id, hash),
    FOREIGN KEY (repo_id) REFERENCES repository (id) ON DELETE CASCADE
);

CREATE TABLE run
(
    id            CHAR(36) PRIMARY KEY NOT NULL,
    repo_id       CHAR(36)             NOT NULL,
    commit_hash   CHAR(40)             NOT NULL,
    start_time    TIMESTAMP            NOT NULL,
    stop_time     TIMESTAMP            NOT NULL,
    error_message TEXT,

    FOREIGN KEY (repo_id, commit_hash) REFERENCES known_commit (repo_id, hash) ON DELETE CASCADE
);

CREATE TABLE run_measurement
(
    id             CHAR(36) PRIMARY KEY NOT NULL,
    run_id         CHAR(36)             NOT NULL,
    benchmark      TEXT                 NOT NULL,
    metric         TEXT                 NOT NULL,
    unit           TEXT,
    interpretation TEXT,
    error_message  TEXT,

    FOREIGN KEY (run_id) REFERENCES run (id) ON DELETE CASCADE
);

CREATE TABLE run_measurement_value
(
    measurement_id CHAR(36) NOT NULL,
    value          DOUBLE   NOT NULL,

    FOREIGN KEY (measurement_id) REFERENCES run_measurement (id) ON DELETE CASCADE
);

CREATE TABLE repo_token
(
    repo_id   CHAR(36) PRIMARY KEY NOT NULL,
    token     TEXT                 NOT NULL,
    hash_algo INTEGER              NOT NULL,

    FOREIGN KEY (repo_id) REFERENCES repository (id) ON DELETE CASCADE
);
