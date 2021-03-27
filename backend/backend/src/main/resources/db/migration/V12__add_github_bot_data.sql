ALTER TABLE repo
ADD COLUMN github_auth_token TEXT;

ALTER TABLE repo
ADD COLUMN github_last_known_comment TIMESTAMP;

CREATE TABLE github_open_commands (
  repo_id     CHAR(36) NOT NULL,
  commit_hash CHAR(40) NOT NULL,
  pr_number   INT      NOT NULL,

  FOREIGN KEY (repo_id) REFERENCES repo(id)
);
