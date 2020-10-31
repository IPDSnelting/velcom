PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "repo"
(
    id         CHAR(36) PRIMARY KEY  NOT NULL,
    name       TEXT                  NOT NULL,
    remote_url TEXT                  NOT NULL
);
INSERT INTO repo VALUES('44bb5c8d-b20d-4bef-bdad-c92767dfa489','VelCom','https://github.com/IPDSnelting/velcom');
INSERT INTO repo VALUES('d471b648-ce65-41e2-9c44-84fb82b73100','Test repo (manual data)','https://github.com/IPDSnelting/velcom-test-repo.git');
CREATE TABLE IF NOT EXISTS repo_token
(
    repo_id   CHAR(36) PRIMARY KEY NOT NULL,
    token     TEXT                 NOT NULL,
    hash_algo INTEGER              NOT NULL,

    FOREIGN KEY (repo_id) REFERENCES "repo" (id) ON DELETE CASCADE
);
-- 123456
INSERT INTO repo_token VALUES('44bb5c8d-b20d-4bef-bdad-c92767dfa489','$argon2id$v=19$m=5120,t=50,p=8$r9rjUS7WmT4TXV2AUEr8Fg$zNSJhtquJbfa47OXoB04yxbmT9j2Mn7cl88bmasV7mk',1);
CREATE TABLE IF NOT EXISTS "run" (
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
  -- If at least one value in (repo_id, commit_hash) is null, no corresponding row needs to exist in
  -- the foreign key table. See also https://sqlite.org/foreignkeys.html
  FOREIGN KEY (repo_id, commit_hash) REFERENCES "known_commit"(repo_id, hash) ON DELETE CASCADE,

  CHECK ((repo_id IS NOT NULL) OR (commit_hash IS NULL)),
  CHECK ((commit_hash IS NULL) = (tar_desc IS NOT NULL)),
  CHECK (error_type IS NULL OR error_type IN ('BENCH', 'VELCOM')),
  CHECK ((error_type IS NULL) = (error IS NULL))
);
INSERT INTO run VALUES('06b036c3-d43f-494d-8000-73d43c2e5cdc','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4316 MiB available\n','\n',char(10)),'2020-10-28 22:57:30.484724+00:00','2020-10-28 23:01:40.876614+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','ae263dd8fdc6a4284c53e8c52058e51e24b9f4bb',NULL,NULL,NULL);
INSERT INTO run VALUES('dfd93b6b-b101-437d-9e44-4ffc680fb630','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4317 MiB available\n','\n',char(10)),'2020-10-28 23:01:54.042955+00:00','2020-10-28 23:05:57.514381+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','7cb5ed6f920dd7ebbbb6c8d397d73ea0b9748fac',NULL,NULL,NULL);
INSERT INTO run VALUES('da506488-f203-4f34-84ff-afdcf59eb5c2','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4317 MiB available\n','\n',char(10)),'2020-10-28 23:06:10.516687+00:00','2020-10-28 23:09:54.676276+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','e718412e834e14e8713babb7293d23675fc1021b',NULL,NULL,NULL);
INSERT INTO run VALUES('ab0d5d67-a094-4cd2-a98d-8ffa252bc80b','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-28 23:10:07.726179+00:00','2020-10-28 23:14:12.272378+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','3fc6f37f0a7c78c2a8a41363441ecdde2faa701a',NULL,NULL,NULL);
INSERT INTO run VALUES('0a9e546e-3bd7-4409-80ba-60a547192bc1','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4316 MiB available\n','\n',char(10)),'2020-10-28 23:14:25.302623+00:00','2020-10-28 23:18:28.566669+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','1d4e59a0f8d60f3b4f8f0928b70438b034b97fb5',NULL,NULL,NULL);
INSERT INTO run VALUES('ea52c1e9-dc51-4b52-b88b-0928874e698f','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-28 23:18:41.829896+00:00','2020-10-28 23:22:36.246187+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','87d5409cbce147972e1420d93e5fcf8b32d3ef14',NULL,NULL,NULL);
INSERT INTO run VALUES('7913dc7a-45da-4231-bed0-d2caced87a60','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-28 23:22:49.37634+00:00','2020-10-28 23:26:58.029798+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','3c634ce7cc5b48ac55be7a214019ae9f1b5ede7d',NULL,NULL,NULL);
INSERT INTO run VALUES('a0eeff7f-2cd3-4150-a52e-a9db32cad046','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-28 23:27:11.074982+00:00','2020-10-28 23:31:02.680137+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','7f9c71d36e19d84c95e235051553cb9fe94cf371',NULL,NULL,NULL);
INSERT INTO run VALUES('8c410bf4-d391-4d5e-8053-d2b26732aca5','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-28 23:31:15.857693+00:00','2020-10-28 23:34:58.049893+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','a0e4196fadbe5355fec7822215f708808db91d0a',NULL,NULL,NULL);
INSERT INTO run VALUES('3b06277e-6d86-4ead-8851-5b9a2fae7d54','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-28 23:35:11.064913+00:00','2020-10-28 23:39:19.654319+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','ca8cb009257685bdcb0c2f69cda0cf901a74563b',NULL,NULL,NULL);
INSERT INTO run VALUES('f3c90181-0bee-4f99-aa15-148dd598419d','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4314 MiB available\n','\n',char(10)),'2020-10-28 23:39:32.770448+00:00','2020-10-28 23:43:39.40155+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','9965b93155b673677df2736a5ed5dbaf078f797c',NULL,NULL,NULL);
INSERT INTO run VALUES('342aeb0f-98e4-4cb2-849a-deae229c8833','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4314 MiB available\n','\n',char(10)),'2020-10-28 23:43:52.55797+00:00','2020-10-28 23:48:02.769083+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','1edf5d683a1bf7b2bc6a9158cc61c4a2cf25237c',NULL,NULL,NULL);
INSERT INTO run VALUES('ac712627-41e6-4b08-9ff9-7e99596698e4','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4314 MiB available\n','\n',char(10)),'2020-10-28 23:48:15.747241+00:00','2020-10-28 23:51:50.849353+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','c848b76b3336a6385600a146050b47605718bea4',NULL,NULL,NULL);
INSERT INTO run VALUES('7e9e031f-4ef1-42f5-b633-559fa0e4be38','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-28 23:52:03.997644+00:00','2020-10-28 23:55:52.237175+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','88a62d8fb03ffbae1cd98b0cdad4bb664ab9c6a0',NULL,NULL,NULL);
INSERT INTO run VALUES('4774972c-5fab-43d4-ba25-c735f6bb0d97','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-28 23:56:05.128676+00:00','2020-10-28 23:59:57.856936+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','9e6f04b7ab94ba3136fd8a3789ebaf3d2431852d',NULL,NULL,NULL);
INSERT INTO run VALUES('f734b19a-5660-4187-98e6-22f116e87c22','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-29 00:00:10.919999+00:00','2020-10-29 00:04:08.790756+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','a811eb70ec460ce4de68732944fafc18d62ba24d',NULL,NULL,NULL);
INSERT INTO run VALUES('1585f3b7-9af9-402f-aec0-dc00675330c1','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-29 00:04:21.843636+00:00','2020-10-29 00:08:09.370767+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','1c3351535574fc22591f5c1b3244d033557deb57',NULL,NULL,NULL);
INSERT INTO run VALUES('f397dcfd-de65-4342-a3c9-0e5fb103c80a','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-29 00:08:22.476926+00:00','2020-10-29 00:11:53.51331+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','342a56548b30cf2d01cfff06236329ecc62479b2',NULL,NULL,NULL);
INSERT INTO run VALUES('0019a48d-f48a-45ef-ba01-3090f5540bf7','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-29 00:12:07.018612+00:00','2020-10-29 00:15:41.897178+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','711a06c8326c5dc88d45a048214b8d10b40d29ff',NULL,NULL,NULL);
INSERT INTO run VALUES('c97f25f1-7964-4d2b-a96e-a1bb85ab8add','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-29 00:15:55.152226+00:00','2020-10-29 00:19:52.458584+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','24afbe0aab947030f46373ed807344d226183f20',NULL,NULL,NULL);
INSERT INTO run VALUES('e0979464-6a49-4b95-a85e-65819503ef86','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-29 00:20:05.617371+00:00','2020-10-29 00:23:36.393137+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','a7777dd3948de0c58fceb337689906aff63455cf',NULL,NULL,NULL);
INSERT INTO run VALUES('9201b88f-630f-4632-b3ac-ed8348347247','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4315 MiB available\n','\n',char(10)),'2020-10-29 00:23:49.394093+00:00','2020-10-29 00:27:37.895705+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','4b84fd6e082fb0a85ecd072ed5221463ebc6b85b',NULL,NULL,NULL);
INSERT INTO run VALUES('1e920833-8b91-484a-ac3d-31d2e754550f','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4314 MiB available\n','\n',char(10)),'2020-10-29 00:27:50.919144+00:00','2020-10-29 00:31:41.378025+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','3bfe8b46d5d2f5c1b2cc24f12757a3714f54dc9a',NULL,NULL,NULL);
INSERT INTO run VALUES('7b4fceec-0289-4ee7-9299-fae7a320506b','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4313 MiB available\n','\n',char(10)),'2020-10-29 00:31:54.363793+00:00','2020-10-29 00:35:42.104957+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','5a7d1549b5b3a403ba8ebda461eb672eb4053248',NULL,NULL,NULL);
INSERT INTO run VALUES('b53667cf-b7c1-4ef3-b94a-5d00831a1ed2','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4314 MiB available\n','\n',char(10)),'2020-10-29 00:35:55.202808+00:00','2020-10-29 00:39:20.678059+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','cd85ae28c8344d78028f757b25abb694d71da888',NULL,NULL,NULL);
INSERT INTO run VALUES('d444a462-733d-446e-acb9-6b655e8d2766','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4313 MiB available\n','\n',char(10)),'2020-10-29 00:39:33.673397+00:00','2020-10-29 00:43:23.960206+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','8f136ae5679210f528822777803608d9d4282886',NULL,NULL,NULL);
INSERT INTO run VALUES('bb0c5a09-62e4-46c0-9a62-3bd11e52a57f','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4314 MiB available\n','\n',char(10)),'2020-10-29 00:43:37.195103+00:00','2020-10-29 00:47:27.222745+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','128f51ab4f21d09da495ccb7d2443485a415090e',NULL,NULL,NULL);
INSERT INTO run VALUES('f40c9a37-1793-4814-9700-ab86ae2ac6eb','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4300 MiB available\n','\n',char(10)),'2020-10-30 17:16:37.125129+00:00','2020-10-30 17:21:59.705299+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','5c2d710e1b79f9c2df7111301739bbbe8bde1f94',NULL,NULL,NULL);
INSERT INTO run VALUES('fdf56078-e247-41c8-acfe-2000adad9df5','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4292 MiB available\n','\n',char(10)),'2020-10-30 17:22:13.470199+00:00','2020-10-30 17:26:21.080096+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','136f555203009a13d045f7306c211b8a8e91721f',NULL,NULL,NULL);
INSERT INTO run VALUES('c9eb181c-3b77-4ac8-9d8b-ae4126b8f840','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4293 MiB available\n','\n',char(10)),'2020-10-30 17:26:34.604395+00:00','2020-10-30 17:30:44.445992+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','133f1caf856d3d1ad586c9fcc558c77afb90ff00',NULL,NULL,NULL);
INSERT INTO run VALUES('6dcdbcf0-338a-4a8f-b224-14a9d6daafec','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4290 MiB available\n','\n',char(10)),'2020-10-30 17:30:57.663589+00:00','2020-10-30 17:35:21.637999+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','2b8843922010291c34e5aa2d129d09b022174ebf',NULL,NULL,NULL);
INSERT INTO run VALUES('39f245f4-d66e-4b89-aa3c-23778d96e79e','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4290 MiB available\n','\n',char(10)),'2020-10-30 17:35:34.967022+00:00','2020-10-30 17:39:39.081766+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','fcc88ada03b724ef7da11e1656e98d902e8a3311',NULL,NULL,NULL);
INSERT INTO run VALUES('1bb0a2b9-06a2-48cd-83a1-2c40d2b6ce3d','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4286 MiB available\n','\n',char(10)),'2020-10-30 17:39:52.321549+00:00','2020-10-30 17:44:01.985205+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','8f016ad85a8aec5706c265cce4763a00e2a6a6d3',NULL,NULL,NULL);
INSERT INTO run VALUES('5e4add69-ff4d-4033-85a2-3637c7c5e496','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4297 MiB available\n','\n',char(10)),'2020-10-31 13:35:57.34333+00:00','2020-10-31 13:41:14.288384+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','591f368a426c688f573d8ebde9159b8834b05221',NULL,NULL,NULL);
INSERT INTO run VALUES('4384847b-1ae9-4664-a30c-27b13bf2ef23','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4292 MiB available\n','\n',char(10)),'2020-10-31 13:42:13.225945+00:00','2020-10-31 13:47:27.443909+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','0e0cb99d4f7d882982c0c19d99830c9c370c5bc8',NULL,NULL,NULL);
INSERT INTO run VALUES('59bb6ef0-6f45-42ab-8fbc-db17ffab6679','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4285 MiB available\n','\n',char(10)),'2020-10-31 13:47:41.021425+00:00','2020-10-31 13:52:02.439621+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','71d5efa6c8fbbf4bcf37af7cff4167d20a16e6d0',NULL,NULL,NULL);
INSERT INTO run VALUES('fb483fd9-d8e0-4ae2-84c5-13adb8ce7d94','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4284 MiB available\n','\n',char(10)),'2020-10-31 13:52:15.697224+00:00','2020-10-31 13:56:44.399884+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','8542543fc52861a2b0c35cf1278c6e6d47feee3f',NULL,NULL,NULL);
INSERT INTO run VALUES('8fbb84eb-84f2-4817-961d-5e1d26a4c52a','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4284 MiB available\n','\n',char(10)),'2020-10-31 13:56:57.844331+00:00','2020-10-31 14:01:22.286578+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','12c7cadc31bba6717af752f9a470408e19e91004',NULL,NULL,NULL);
INSERT INTO run VALUES('698b47d4-8112-4d8d-97f7-ded04c0bab2e','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4282 MiB available\n','\n',char(10)),'2020-10-31 14:01:35.683883+00:00','2020-10-31 14:06:04.786895+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','74c17704865d301838dfdbd71ca2e91e3919de9e',NULL,NULL,NULL);
INSERT INTO run VALUES('9b6ba18e-cdf9-48f8-bb93-6487abb3e38f','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4281 MiB available\n','\n',char(10)),'2020-10-31 14:06:18.012273+00:00','2020-10-31 14:10:41.380963+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','5e8327cc80d8c5175f8e89226863d3a6184d5cb2',NULL,NULL,NULL);
INSERT INTO run VALUES('78d7a7c0-888c-47cb-ba51-adae35db835e','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4281 MiB available\n','\n',char(10)),'2020-10-31 14:10:54.623313+00:00','2020-10-31 14:15:04.75358+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','cb5a0b2d1831e51dfce64632111251e59286419f',NULL,NULL,NULL);
INSERT INTO run VALUES('2a43fb24-da80-42cd-879e-7c8755c11433','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4280 MiB available\n','\n',char(10)),'2020-10-31 14:15:17.919367+00:00','2020-10-31 14:19:28.561349+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','89660dacdbb3dd1bd23a748d3476af194ae2b4c0',NULL,NULL,NULL);
INSERT INTO run VALUES('68890c47-b87a-4715-87f3-c02fd92680d5','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4282 MiB available\n','\n',char(10)),'2020-10-31 14:19:41.964089+00:00','2020-10-31 14:23:47.56666+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','fb3d461312731964b65b11d7ab73498252ab457b',NULL,NULL,NULL);
INSERT INTO run VALUES('d918d568-c3d6-4ae7-911d-fe10050eba37','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4281 MiB available\n','\n',char(10)),'2020-10-31 14:24:04.050619+00:00','2020-10-31 14:28:11.662495+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','c7f4a4ba32f1376d0a9938761e5091f644efde61',NULL,NULL,NULL);
INSERT INTO run VALUES('6931e64d-77e8-4b9b-9e39-fffb864d0d86','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4282 MiB available\n','\n',char(10)),'2020-10-31 14:28:24.814046+00:00','2020-10-31 14:32:40.939571+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','cfcbccc1313798f103c0fa58ef98a74991245ae3',NULL,NULL,NULL);
INSERT INTO run VALUES('ab008ca3-6843-4127-b41a-ac0b02a49bab','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4281 MiB available\n','\n',char(10)),'2020-10-31 14:32:54.154918+00:00','2020-10-31 14:37:26.961213+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','35c1073cdfcca97dffff18c52bd920090e8acbcf',NULL,NULL,NULL);
INSERT INTO run VALUES('f55aaeb4-0a14-496f-8023-e98d617899bb','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4280 MiB available\n','\n',char(10)),'2020-10-31 14:37:40.107574+00:00','2020-10-31 14:42:14.763103+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','ace2d45f869f0094196629c76ee4c61bc550aaa6',NULL,NULL,NULL);
INSERT INTO run VALUES('2d5822ef-8c91-4414-9c5f-b689414e0b2a','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4280 MiB available\n','\n',char(10)),'2020-10-31 14:42:28.359138+00:00','2020-10-31 14:46:35.143097+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','514442b558c9d08b260de9b1f1ae9744e1afd200',NULL,NULL,NULL);
INSERT INTO run VALUES('b87a1c3a-2212-49b8-a4e4-9ed1e9ebdeba','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4280 MiB available\n','\n',char(10)),'2020-10-31 14:46:48.567145+00:00','2020-10-31 14:51:00.312867+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','deec6c90541a9064849af7011fa9d7c6f40a3de1',NULL,NULL,NULL);
INSERT INTO run VALUES('6150fd27-2a3d-4c69-b365-51124efe7c7f','Listener','I-Al-VPS - Runner',replace('System: Linux amd64 4.9.0-13-amd64\nCPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\nMemory: 7797 MiB total, 4279 MiB available\n','\n',char(10)),'2020-10-31 14:51:13.624457+00:00','2020-10-31 14:55:35.376732+00:00','44bb5c8d-b20d-4bef-bdad-c92767dfa489','a4159999515f26e8a53ee0cee19be8a925f3ca6c',NULL,NULL,NULL);
CREATE TABLE IF NOT EXISTS "measurement" (
  id             CHAR(36) NOT NULL PRIMARY KEY,
  run_id         CHAR(36) NOT NULL,
  benchmark      TEXT     NOT NULL,
  metric         TEXT     NOT NULL,
  unit           TEXT,
  interpretation TEXT,
  error          TEXT,

  FOREIGN KEY (run_id) REFERENCES "run"(id) ON DELETE CASCADE,

  CHECK (interpretation IN ('LESS_IS_BETTER', 'MORE_IS_BETTER', 'NEUTRAL'))
);
INSERT INTO measurement VALUES('e25c4582-ce4e-498b-abcb-5c73ce2fcd35','06b036c3-d43f-494d-8000-73d43c2e5cdc','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('d8dd235a-a6c4-4804-b3b2-1ab3212c2a00','06b036c3-d43f-494d-8000-73d43c2e5cdc','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('572a1574-e8db-4317-bd5f-964d9b337562','06b036c3-d43f-494d-8000-73d43c2e5cdc','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('970b7295-75d3-43b3-8daf-fc09e9a618d3','06b036c3-d43f-494d-8000-73d43c2e5cdc','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('099bb394-b2d7-42e6-804b-19cc01b30ea5','dfd93b6b-b101-437d-9e44-4ffc680fb630','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('fc07c68c-8613-47c4-b572-228b563bcb58','dfd93b6b-b101-437d-9e44-4ffc680fb630','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('ccbbd8bd-24f6-42c2-be6f-eaabb3e15b78','dfd93b6b-b101-437d-9e44-4ffc680fb630','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('a7483735-15d9-4b47-985a-9a12d8ce2dc1','dfd93b6b-b101-437d-9e44-4ffc680fb630','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('4a85e1f0-d050-45bb-9b5c-b0d3a0187dad','da506488-f203-4f34-84ff-afdcf59eb5c2','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('f1bc0bd7-c90e-4236-970c-2bb257099710','da506488-f203-4f34-84ff-afdcf59eb5c2','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('56293eb9-d142-4bb4-9c06-defda7a8ced6','da506488-f203-4f34-84ff-afdcf59eb5c2','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('4c99bd93-03a6-44ae-ac14-62288a23ad5c','da506488-f203-4f34-84ff-afdcf59eb5c2','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('a99d9de0-88ec-4172-9c8a-723cc85d72c9','ab0d5d67-a094-4cd2-a98d-8ffa252bc80b','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('74498c46-3f5a-4ac3-9b89-5d8d28ac8170','ab0d5d67-a094-4cd2-a98d-8ffa252bc80b','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('d3b26ccf-4f63-4ad1-a48e-98851bac0cd6','ab0d5d67-a094-4cd2-a98d-8ffa252bc80b','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('9dcff2d9-5e91-484b-8ab3-10b4ad1996e7','ab0d5d67-a094-4cd2-a98d-8ffa252bc80b','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('47aeb2d8-96a2-46ca-8189-0bac2316ccd4','0a9e546e-3bd7-4409-80ba-60a547192bc1','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('db20bf9c-813a-43b3-abbc-16ac6b4badda','0a9e546e-3bd7-4409-80ba-60a547192bc1','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('d3473b39-f369-4ce0-910a-bf85903f2a84','0a9e546e-3bd7-4409-80ba-60a547192bc1','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('207cf8d1-6698-4328-b6fa-27c75b62e6b2','0a9e546e-3bd7-4409-80ba-60a547192bc1','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('5219fdee-0701-4483-9914-7b184dd88cb9','ea52c1e9-dc51-4b52-b88b-0928874e698f','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('73fba1eb-43eb-4c56-85e5-743a4f55815e','ea52c1e9-dc51-4b52-b88b-0928874e698f','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('f9a696fb-473a-44a5-9269-908789cf45ad','ea52c1e9-dc51-4b52-b88b-0928874e698f','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('55ca829f-9860-4c46-be00-8863c1890ed6','ea52c1e9-dc51-4b52-b88b-0928874e698f','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('051b6b47-be54-4b66-9230-27b3a40d14d2','7913dc7a-45da-4231-bed0-d2caced87a60','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('3fb82fc6-a3fe-4785-925b-ce33ade27c92','7913dc7a-45da-4231-bed0-d2caced87a60','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('119f91d4-3504-40ab-9593-c3ec02288e6c','7913dc7a-45da-4231-bed0-d2caced87a60','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('fcce8833-ba72-42ca-8058-3578ccc3c96e','7913dc7a-45da-4231-bed0-d2caced87a60','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('e50c6144-81bd-4ab3-9157-0bb026816271','a0eeff7f-2cd3-4150-a52e-a9db32cad046','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('597f913c-6bd1-4245-8094-3680664f3412','a0eeff7f-2cd3-4150-a52e-a9db32cad046','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('76e18932-08ae-4525-80d7-8d947008610e','a0eeff7f-2cd3-4150-a52e-a9db32cad046','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('e5125b05-51b1-4a3d-87c7-8ae71631c553','a0eeff7f-2cd3-4150-a52e-a9db32cad046','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('3b205e7f-c831-42fa-a619-73403e3c7caa','8c410bf4-d391-4d5e-8053-d2b26732aca5','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('1c3024c6-5589-444b-b307-0ee485a39641','8c410bf4-d391-4d5e-8053-d2b26732aca5','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('f62afe64-67b2-4694-b531-f3c09fe71a7e','8c410bf4-d391-4d5e-8053-d2b26732aca5','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('b5d9409f-4952-489c-a0f6-81bc916c7d2a','8c410bf4-d391-4d5e-8053-d2b26732aca5','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('378e12b5-0c1c-4647-bd4d-31ec298e52a0','3b06277e-6d86-4ead-8851-5b9a2fae7d54','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('06d3928f-0d02-44fe-93a8-9360f9f6215a','3b06277e-6d86-4ead-8851-5b9a2fae7d54','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('f3ec5424-bc8b-4641-94c8-4b8826bf0c67','3b06277e-6d86-4ead-8851-5b9a2fae7d54','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('7a6c887f-f525-45f2-a17f-4eff427e334d','3b06277e-6d86-4ead-8851-5b9a2fae7d54','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('4c619993-c43a-4e75-9e8b-cbd817e1fcbf','f3c90181-0bee-4f99-aa15-148dd598419d','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('5c349040-fcb9-485a-a228-2fae79bdeb2f','f3c90181-0bee-4f99-aa15-148dd598419d','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('0acc1d9b-207e-4661-bbfe-1745f13778be','f3c90181-0bee-4f99-aa15-148dd598419d','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('1e003cf9-79e9-4d90-8bba-3e978d8b1dd4','f3c90181-0bee-4f99-aa15-148dd598419d','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('d3867f29-e410-4ec3-9068-00abe01c4fcc','342aeb0f-98e4-4cb2-849a-deae229c8833','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('72b4c624-8855-489c-84bb-4e202ab486cd','342aeb0f-98e4-4cb2-849a-deae229c8833','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('4a9eac34-63ad-4e4b-8404-3af8c2f84e96','342aeb0f-98e4-4cb2-849a-deae229c8833','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('aa4c8c9e-630a-4327-8056-9c2bd6ea18f3','342aeb0f-98e4-4cb2-849a-deae229c8833','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('488ba0f3-81b9-4acb-b162-2da7590020b3','ac712627-41e6-4b08-9ff9-7e99596698e4','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('c99e9ad7-ec37-479a-9818-fcdf73daa31b','ac712627-41e6-4b08-9ff9-7e99596698e4','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('1f4a63a6-346a-433c-903f-215fce86f477','ac712627-41e6-4b08-9ff9-7e99596698e4','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('8d56bb49-4066-45c3-8bae-fe2f61924b06','ac712627-41e6-4b08-9ff9-7e99596698e4','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('4a9ce1d7-306b-444a-86f2-d9c6cf836753','7e9e031f-4ef1-42f5-b633-559fa0e4be38','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('6fa818ba-3437-47cb-9868-92b78e6270c8','7e9e031f-4ef1-42f5-b633-559fa0e4be38','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('2a3c5899-bb64-4a34-b44f-1b2d91625933','7e9e031f-4ef1-42f5-b633-559fa0e4be38','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('63a7b289-bd3e-4ab5-9212-6598c87934b2','7e9e031f-4ef1-42f5-b633-559fa0e4be38','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('cc56b447-9ade-4f87-9a8b-5dd0df0b349a','4774972c-5fab-43d4-ba25-c735f6bb0d97','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('cf38311c-374f-4fd9-82fd-90cda58e837c','4774972c-5fab-43d4-ba25-c735f6bb0d97','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('9d90d0b8-81e9-4f1b-ad6e-c349a8c52111','4774972c-5fab-43d4-ba25-c735f6bb0d97','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('517075f6-53b4-4fff-bdbc-5db9d089dae0','4774972c-5fab-43d4-ba25-c735f6bb0d97','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('526ead50-655e-415f-b24f-9530adcb93f3','f734b19a-5660-4187-98e6-22f116e87c22','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('93f9f8ce-7fa3-4105-b522-7005ce1112cc','f734b19a-5660-4187-98e6-22f116e87c22','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('b38611e3-0bf1-4bca-b31a-5f47aa424749','f734b19a-5660-4187-98e6-22f116e87c22','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('bedc590d-fe1c-42c9-82d9-97fb34e3198f','f734b19a-5660-4187-98e6-22f116e87c22','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('ab624578-8a5d-4b01-b238-f31fd242ed70','1585f3b7-9af9-402f-aec0-dc00675330c1','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('e910777c-8b9d-4d67-beff-4445f9784c2c','1585f3b7-9af9-402f-aec0-dc00675330c1','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('e2de0817-1db0-4083-8340-fc6a47c3acda','1585f3b7-9af9-402f-aec0-dc00675330c1','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('2835cead-36fc-4dd1-a386-ec8a293185c4','1585f3b7-9af9-402f-aec0-dc00675330c1','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('fb8e6337-f93e-4b6e-9ca4-f4258901fe03','f397dcfd-de65-4342-a3c9-0e5fb103c80a','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('2dcd9a43-ab32-4db7-99a0-5ef6350a6f79','f397dcfd-de65-4342-a3c9-0e5fb103c80a','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('69e7a4fb-2598-43af-ac7b-036b270cb0b8','f397dcfd-de65-4342-a3c9-0e5fb103c80a','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('921b7f71-e597-4ab5-9306-b3259621cbbf','f397dcfd-de65-4342-a3c9-0e5fb103c80a','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('7a50908a-9ad0-4d4c-9727-e4b36031db69','0019a48d-f48a-45ef-ba01-3090f5540bf7','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('6adeb9fa-7c39-45f9-b627-cc401c3008b4','0019a48d-f48a-45ef-ba01-3090f5540bf7','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('89409c1b-0a8b-4a9a-bc92-342302b830be','0019a48d-f48a-45ef-ba01-3090f5540bf7','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('f3a1793e-7923-4af1-9cff-c21374f46535','0019a48d-f48a-45ef-ba01-3090f5540bf7','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('dae50eb2-41c3-44e8-946b-b140100d9a01','c97f25f1-7964-4d2b-a96e-a1bb85ab8add','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('d7a96291-7c2b-41b3-ad8c-0b37e4137387','c97f25f1-7964-4d2b-a96e-a1bb85ab8add','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('014e3a01-697c-4702-ab51-b6190f288105','c97f25f1-7964-4d2b-a96e-a1bb85ab8add','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('c9a66e61-2599-4b13-9cb1-fc99e8f22687','c97f25f1-7964-4d2b-a96e-a1bb85ab8add','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('f84e4699-89de-447d-8e21-5edcd2949e26','e0979464-6a49-4b95-a85e-65819503ef86','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('d8a9dd91-e95a-49d2-9cdd-312e7c2422db','e0979464-6a49-4b95-a85e-65819503ef86','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('61baed34-8bad-4bd6-be73-affe67d1e71c','e0979464-6a49-4b95-a85e-65819503ef86','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('fc8b7dd2-1792-42fc-a0f7-3abb93598072','e0979464-6a49-4b95-a85e-65819503ef86','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('77a1f868-5615-4d72-9478-9926dc459887','9201b88f-630f-4632-b3ac-ed8348347247','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('bc1e3d30-9eea-4b43-8d29-7ab597b93c1e','9201b88f-630f-4632-b3ac-ed8348347247','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('8e0a0e31-0778-453b-815c-cc14d2a3bb4f','9201b88f-630f-4632-b3ac-ed8348347247','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('bba4f06d-8d9f-448f-ad2a-8b19f0033a0c','9201b88f-630f-4632-b3ac-ed8348347247','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('c76e131c-00ca-4e1b-a8e9-00da1d433352','1e920833-8b91-484a-ac3d-31d2e754550f','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('af6e9253-3260-4a14-a430-c6389327bc1e','1e920833-8b91-484a-ac3d-31d2e754550f','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('36520022-73ce-414f-909f-0d971eabcb33','1e920833-8b91-484a-ac3d-31d2e754550f','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('6e26a81c-77c2-4a27-839c-abc53884cd23','1e920833-8b91-484a-ac3d-31d2e754550f','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('55c229e7-ad67-4eca-8e7a-6cdaeb0dc84c','7b4fceec-0289-4ee7-9299-fae7a320506b','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('203e2d4e-30a9-4066-9316-13f4676fbc2d','7b4fceec-0289-4ee7-9299-fae7a320506b','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('a7ea107e-64b5-4183-8757-08b074e1a370','7b4fceec-0289-4ee7-9299-fae7a320506b','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('5f270d2f-ffdf-4764-864f-1700b3b2fb0a','7b4fceec-0289-4ee7-9299-fae7a320506b','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('5c3c42cf-c9a4-4786-8315-a26a8fcc0e3d','b53667cf-b7c1-4ef3-b94a-5d00831a1ed2','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('76dfbe71-033d-492e-97cc-1edd0eced0e2','b53667cf-b7c1-4ef3-b94a-5d00831a1ed2','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('5ed2f432-87c8-4103-9dbb-8174e1342e2b','b53667cf-b7c1-4ef3-b94a-5d00831a1ed2','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('7af4a465-8308-4e8a-a6e8-58db3cf7575a','b53667cf-b7c1-4ef3-b94a-5d00831a1ed2','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('81ae9809-4fe6-475d-96a5-2db2b3120e4d','d444a462-733d-446e-acb9-6b655e8d2766','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('afe5cab6-1ee2-4bc2-865f-17f78377043e','d444a462-733d-446e-acb9-6b655e8d2766','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('8a69e312-ed13-4c84-87a3-be58b895ecd9','d444a462-733d-446e-acb9-6b655e8d2766','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('69c9eee5-9080-4373-b998-7c190d9af89c','d444a462-733d-446e-acb9-6b655e8d2766','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('932b9f9f-8bad-4a79-bc33-c68add08f644','bb0c5a09-62e4-46c0-9a62-3bd11e52a57f','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('b1e58b02-24fd-443f-a993-fc86803a0544','bb0c5a09-62e4-46c0-9a62-3bd11e52a57f','backend','build_time',NULL,NULL,replace('Command ''[''make'', ''-C'', ''/home/velcom-runner/config/aaaaaaah/task_repo'', ''backend'']'' returned non-zero exit status 2.\n StdOut: <Empty>\n StdErr: <Empty>','\n',char(10)));
INSERT INTO measurement VALUES('5a1b7a4b-dda8-4696-96df-8db3aaf2c793','bb0c5a09-62e4-46c0-9a62-3bd11e52a57f','backend','coverage',NULL,NULL,'[Errno 2] No such file or directory: ''/home/velcom-runner/config/aaaaaaah/task_repo/backend/aggregator/target/site/jacoco-aggregate/index.html''');
INSERT INTO measurement VALUES('8b5df61c-b077-4dc9-a337-7853867974c8','bb0c5a09-62e4-46c0-9a62-3bd11e52a57f','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('529fc683-336c-43d5-ab8a-daad8b5a24ae','f40c9a37-1793-4814-9700-ab86ae2ac6eb','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('08eba5bf-16d3-438f-a22b-bf5d26b0b386','f40c9a37-1793-4814-9700-ab86ae2ac6eb','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('8716abd9-23ea-4593-a5ae-8c58e6f4cb40','f40c9a37-1793-4814-9700-ab86ae2ac6eb','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('46293e08-75e6-4132-b2b1-d44d69fd0749','f40c9a37-1793-4814-9700-ab86ae2ac6eb','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('1f83f719-0e79-4c7a-a4bd-86177ea0a333','fdf56078-e247-41c8-acfe-2000adad9df5','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('129c2520-d430-469e-b58e-f0b0e38e8af6','fdf56078-e247-41c8-acfe-2000adad9df5','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('94bca89b-e3b8-45e2-89ca-b67a09f0f25c','fdf56078-e247-41c8-acfe-2000adad9df5','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('c4a7680b-b102-45bd-a850-f188ee3efa72','fdf56078-e247-41c8-acfe-2000adad9df5','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('4730f1eb-bb39-4350-88ff-a1aa22eae3ab','c9eb181c-3b77-4ac8-9d8b-ae4126b8f840','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('a1f70e2b-9a7a-494e-be1b-46413deb1611','c9eb181c-3b77-4ac8-9d8b-ae4126b8f840','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('8e06a1b0-64aa-47fe-b082-0edc98535708','c9eb181c-3b77-4ac8-9d8b-ae4126b8f840','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('b50dcd18-b320-4541-a9ee-6f8e43e22b0c','c9eb181c-3b77-4ac8-9d8b-ae4126b8f840','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('07637428-0094-42d2-b14f-ec5d0067c633','6dcdbcf0-338a-4a8f-b224-14a9d6daafec','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('7fc70af8-7b53-43a1-9d59-8cd981c1f957','6dcdbcf0-338a-4a8f-b224-14a9d6daafec','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('5f5eaa9b-e71f-48e2-9c6f-4c1988113710','6dcdbcf0-338a-4a8f-b224-14a9d6daafec','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('f7f40628-6592-4c69-8d92-507a3ff66b16','6dcdbcf0-338a-4a8f-b224-14a9d6daafec','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('84ed138f-177c-4ce6-9562-cc59d8243a57','39f245f4-d66e-4b89-aa3c-23778d96e79e','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('78b28689-497a-447a-bc4d-059b616c93f9','39f245f4-d66e-4b89-aa3c-23778d96e79e','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('2e084603-aba8-43b6-ac0a-87c9d207dcbc','39f245f4-d66e-4b89-aa3c-23778d96e79e','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('de6904b0-68b7-455c-8c24-107d83c50a09','39f245f4-d66e-4b89-aa3c-23778d96e79e','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('aecf8d32-451a-4659-9f0f-617f9ceaaa21','1bb0a2b9-06a2-48cd-83a1-2c40d2b6ce3d','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('7c22a2a2-93dc-4c76-87f5-140b908feec3','1bb0a2b9-06a2-48cd-83a1-2c40d2b6ce3d','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('ca2de8fb-a3b3-449d-adf5-c3dc664ddd2c','1bb0a2b9-06a2-48cd-83a1-2c40d2b6ce3d','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('62892f34-455b-4ec8-b576-cb08a7466800','1bb0a2b9-06a2-48cd-83a1-2c40d2b6ce3d','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('c895d4cf-ee05-4d51-859a-abae4baebe01','5e4add69-ff4d-4033-85a2-3637c7c5e496','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('39972943-54f2-479e-9421-060716fe215f','5e4add69-ff4d-4033-85a2-3637c7c5e496','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('487bf46c-1f5e-4c0d-8f3e-9ebfac08ffd5','5e4add69-ff4d-4033-85a2-3637c7c5e496','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('4ddbb2c4-ba2a-4cf2-9af6-a71a052f5cf0','5e4add69-ff4d-4033-85a2-3637c7c5e496','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('675dc690-b003-42e9-bf8c-146dd4e6a7dc','4384847b-1ae9-4664-a30c-27b13bf2ef23','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('ef7b86cd-b047-462b-aca8-54363342320c','4384847b-1ae9-4664-a30c-27b13bf2ef23','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('571af7d3-488e-4877-8796-37d4643b9219','4384847b-1ae9-4664-a30c-27b13bf2ef23','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('126059e5-2069-440f-8f1c-02f1712b492a','4384847b-1ae9-4664-a30c-27b13bf2ef23','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('d74c4e44-2b27-445c-afe3-88295214ca07','59bb6ef0-6f45-42ab-8fbc-db17ffab6679','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('77c077bd-150e-40dd-8346-7cb1fe5ff8f8','59bb6ef0-6f45-42ab-8fbc-db17ffab6679','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('3c65acc5-c160-4616-adfa-777415e7d2f3','59bb6ef0-6f45-42ab-8fbc-db17ffab6679','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('8e7a1256-642f-4d0a-b936-eca296a01740','59bb6ef0-6f45-42ab-8fbc-db17ffab6679','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('bdf76252-cd1f-4f18-b565-47f5d7cf0dc7','fb483fd9-d8e0-4ae2-84c5-13adb8ce7d94','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('6468697c-9319-434e-9d15-9466b1436a0d','fb483fd9-d8e0-4ae2-84c5-13adb8ce7d94','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('197bc730-91b9-4d3f-9fdd-4edac532c16d','fb483fd9-d8e0-4ae2-84c5-13adb8ce7d94','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('4a1c885e-60a7-46f9-956a-35711b7fb3f5','fb483fd9-d8e0-4ae2-84c5-13adb8ce7d94','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('3cfbedc4-e325-47a0-be4f-d6d12a2e9378','8fbb84eb-84f2-4817-961d-5e1d26a4c52a','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('ce819968-c580-4ff5-9869-b8855f9a9853','8fbb84eb-84f2-4817-961d-5e1d26a4c52a','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('4c28d0af-1ed9-475f-a5aa-7a7dc36d5e39','8fbb84eb-84f2-4817-961d-5e1d26a4c52a','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('e3ff4072-0436-4d08-9222-c6c6283d2326','8fbb84eb-84f2-4817-961d-5e1d26a4c52a','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('ba3aa511-c0ba-4702-b1fb-6e68c91166ae','698b47d4-8112-4d8d-97f7-ded04c0bab2e','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('e1716fae-8069-4a16-8a9c-804d86319f62','698b47d4-8112-4d8d-97f7-ded04c0bab2e','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('b1adb16c-0b33-487f-bd87-b8b37de1801d','698b47d4-8112-4d8d-97f7-ded04c0bab2e','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('48446c39-171a-4154-88b6-be7652b5d931','698b47d4-8112-4d8d-97f7-ded04c0bab2e','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('afe1bac0-1c32-453e-9579-9f84ba5b0d86','9b6ba18e-cdf9-48f8-bb93-6487abb3e38f','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('83079ecc-57e1-4692-b93f-103be9a140e9','9b6ba18e-cdf9-48f8-bb93-6487abb3e38f','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('c5a293da-4621-41e8-a398-63e98a344489','9b6ba18e-cdf9-48f8-bb93-6487abb3e38f','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('af9aa98f-2206-4be0-933b-01ee70e4583d','9b6ba18e-cdf9-48f8-bb93-6487abb3e38f','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('e6a13dae-3e0c-4670-abcd-1c6019f4d1c2','78d7a7c0-888c-47cb-ba51-adae35db835e','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('bafb3982-7e88-44a9-976e-7923faa41a92','78d7a7c0-888c-47cb-ba51-adae35db835e','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('9e12602c-cdea-4e77-b0f8-8782103ceaac','78d7a7c0-888c-47cb-ba51-adae35db835e','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('4f74074c-425b-4f32-b67a-52ee81a606f3','78d7a7c0-888c-47cb-ba51-adae35db835e','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('32b6fe83-577b-45ca-ade1-bec9cd2b0d81','2a43fb24-da80-42cd-879e-7c8755c11433','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('fc4e1221-a2f0-4ce1-ba16-04712bc10691','2a43fb24-da80-42cd-879e-7c8755c11433','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('77b55823-0a88-4ec0-9cc0-e2c5766926f5','2a43fb24-da80-42cd-879e-7c8755c11433','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('cb3494d7-a2cb-4455-99a6-980bbe31a107','2a43fb24-da80-42cd-879e-7c8755c11433','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('444fc257-3ba4-433e-a3bf-8015c27e5764','68890c47-b87a-4715-87f3-c02fd92680d5','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('5e1aafb2-0454-4ca8-a8f5-6da42051ac3e','68890c47-b87a-4715-87f3-c02fd92680d5','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('b9b71390-6512-434d-8439-edc16cecb914','68890c47-b87a-4715-87f3-c02fd92680d5','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('9aa14a5f-ed85-439b-9fee-c03deb21624f','68890c47-b87a-4715-87f3-c02fd92680d5','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('0fe07903-7491-44ba-808c-1d7d16dec74f','d918d568-c3d6-4ae7-911d-fe10050eba37','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('604185f9-8e95-45d5-aecf-dadcec2028ce','d918d568-c3d6-4ae7-911d-fe10050eba37','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('74811461-2e07-4cc4-bb39-3eef92dbae69','d918d568-c3d6-4ae7-911d-fe10050eba37','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('ecb33948-b7c4-4c61-aca6-7528fb0403e3','d918d568-c3d6-4ae7-911d-fe10050eba37','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('0c52edb4-cf39-4701-b9fe-e2958790454f','6931e64d-77e8-4b9b-9e39-fffb864d0d86','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('eaebfb7c-0187-4013-98a8-2dfae2e297a4','6931e64d-77e8-4b9b-9e39-fffb864d0d86','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('de06ad13-ce34-49cd-969f-d4ade3fc9b61','6931e64d-77e8-4b9b-9e39-fffb864d0d86','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('d550cb38-3674-498d-ae03-dc04521a31c6','6931e64d-77e8-4b9b-9e39-fffb864d0d86','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('f3602ae9-38ad-490d-9528-c77f7b2fb43a','ab008ca3-6843-4127-b41a-ac0b02a49bab','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('e026bb95-5d10-48ef-af91-d15acdab4d77','ab008ca3-6843-4127-b41a-ac0b02a49bab','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('47595699-2c5d-48c0-9a1b-3bfb3d489e75','ab008ca3-6843-4127-b41a-ac0b02a49bab','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('24ff7d35-3f2b-4edf-8ae8-e5356bea6409','ab008ca3-6843-4127-b41a-ac0b02a49bab','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('df20feff-02f9-4197-831c-a151292b0e8e','f55aaeb4-0a14-496f-8023-e98d617899bb','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('54fb99ce-56a5-4f04-8eb0-09da71a98c02','f55aaeb4-0a14-496f-8023-e98d617899bb','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('8fca8ed5-1ede-4953-a213-128330462451','f55aaeb4-0a14-496f-8023-e98d617899bb','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('7d42547e-b0dd-4f62-8190-910702abb801','f55aaeb4-0a14-496f-8023-e98d617899bb','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('8d64d452-10bc-47d0-a3b8-4f1166916a65','2d5822ef-8c91-4414-9c5f-b689414e0b2a','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('209c2c32-d4fc-4d78-bc71-1bfe77405814','2d5822ef-8c91-4414-9c5f-b689414e0b2a','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('27dc54ca-7773-4d1a-9d46-fe123f83fda5','2d5822ef-8c91-4414-9c5f-b689414e0b2a','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('79eec84b-b47a-4f5f-9922-15c63cada344','2d5822ef-8c91-4414-9c5f-b689414e0b2a','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('efb2f006-823a-4857-8469-5c992dc7f0a3','b87a1c3a-2212-49b8-a4e4-9ed1e9ebdeba','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('e10d141f-22fa-4c9e-a749-ee4240ec3276','b87a1c3a-2212-49b8-a4e4-9ed1e9ebdeba','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('2d08fc7d-8820-484f-9065-bbb0c4dd88e1','b87a1c3a-2212-49b8-a4e4-9ed1e9ebdeba','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('4c00e4b6-6942-43b6-bd90-d5768c65dffb','b87a1c3a-2212-49b8-a4e4-9ed1e9ebdeba','misc','always zero','cats','NEUTRAL',NULL);
INSERT INTO measurement VALUES('88a55fce-8d91-41d7-a502-36b0262a96a2','6150fd27-2a3d-4c69-b365-51124efe7c7f','frontend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('ec9f4adc-b27a-4a3d-a0cd-c9ff41bed969','6150fd27-2a3d-4c69-b365-51124efe7c7f','backend','build_time','seconds','LESS_IS_BETTER',NULL);
INSERT INTO measurement VALUES('d87b7794-8721-45b2-bbd9-28e758b8d7f9','6150fd27-2a3d-4c69-b365-51124efe7c7f','backend','coverage','percent','MORE_IS_BETTER',NULL);
INSERT INTO measurement VALUES('a11834f2-cc98-47b1-9210-4bf6ae3feeef','6150fd27-2a3d-4c69-b365-51124efe7c7f','misc','always zero','cats','NEUTRAL',NULL);
CREATE TABLE IF NOT EXISTS "measurement_value" (
  measurement_id CHAR(36) NOT NULL,
  value          DOUBLE   NOT NULL,

  FOREIGN KEY (measurement_id) REFERENCES "measurement"(id) ON DELETE CASCADE
);
INSERT INTO measurement_value VALUES('e25c4582-ce4e-498b-abcb-5c73ce2fcd35',187.29599714279174804);
INSERT INTO measurement_value VALUES('970b7295-75d3-43b3-8daf-fc09e9a618d3',0.0);
INSERT INTO measurement_value VALUES('099bb394-b2d7-42e6-804b-19cc01b30ea5',180.77048611640930175);
INSERT INTO measurement_value VALUES('a7483735-15d9-4b47-985a-9a12d8ce2dc1',0.0);
INSERT INTO measurement_value VALUES('4a85e1f0-d050-45bb-9b5c-b0d3a0187dad',161.55755066871643066);
INSERT INTO measurement_value VALUES('4c99bd93-03a6-44ae-ac14-62288a23ad5c',0.0);
INSERT INTO measurement_value VALUES('a99d9de0-88ec-4172-9c8a-723cc85d72c9',182.27560091018676757);
INSERT INTO measurement_value VALUES('9dcff2d9-5e91-484b-8ab3-10b4ad1996e7',0.0);
INSERT INTO measurement_value VALUES('47aeb2d8-96a2-46ca-8189-0bac2316ccd4',181.58292412757873535);
INSERT INTO measurement_value VALUES('207cf8d1-6698-4328-b6fa-27c75b62e6b2',0.0);
INSERT INTO measurement_value VALUES('5219fdee-0701-4483-9914-7b184dd88cb9',170.25616765022277832);
INSERT INTO measurement_value VALUES('55ca829f-9860-4c46-be00-8863c1890ed6',0.0);
INSERT INTO measurement_value VALUES('051b6b47-be54-4b66-9230-27b3a40d14d2',186.49359083175659179);
INSERT INTO measurement_value VALUES('fcce8833-ba72-42ca-8058-3578ccc3c96e',0.0);
INSERT INTO measurement_value VALUES('e50c6144-81bd-4ab3-9157-0bb026816271',169.25130891799926757);
INSERT INTO measurement_value VALUES('e5125b05-51b1-4a3d-87c7-8ae71631c553',0.0);
INSERT INTO measurement_value VALUES('3b205e7f-c831-42fa-a619-73403e3c7caa',158.89185428619384765);
INSERT INTO measurement_value VALUES('b5d9409f-4952-489c-a0f6-81bc916c7d2a',0.0);
INSERT INTO measurement_value VALUES('378e12b5-0c1c-4647-bd4d-31ec298e52a0',182.29372763633728027);
INSERT INTO measurement_value VALUES('7a6c887f-f525-45f2-a17f-4eff427e334d',0.0);
INSERT INTO measurement_value VALUES('4c619993-c43a-4e75-9e8b-cbd817e1fcbf',183.31072664260864257);
INSERT INTO measurement_value VALUES('1e003cf9-79e9-4d90-8bba-3e978d8b1dd4',0.0);
INSERT INTO measurement_value VALUES('d3867f29-e410-4ec3-9068-00abe01c4fcc',186.60060906410217284);
INSERT INTO measurement_value VALUES('aa4c8c9e-630a-4327-8056-9c2bd6ea18f3',0.0);
INSERT INTO measurement_value VALUES('488ba0f3-81b9-4acb-b162-2da7590020b3',167.90845441818237304);
INSERT INTO measurement_value VALUES('8d56bb49-4066-45c3-8bae-fe2f61924b06',0.0);
INSERT INTO measurement_value VALUES('4a9ce1d7-306b-444a-86f2-d9c6cf836753',181.44691848754882813);
INSERT INTO measurement_value VALUES('63a7b289-bd3e-4ab5-9212-6598c87934b2',0.0);
INSERT INTO measurement_value VALUES('cc56b447-9ade-4f87-9a8b-5dd0df0b349a',186.58073711395263672);
INSERT INTO measurement_value VALUES('517075f6-53b4-4fff-bdbc-5db9d089dae0',0.0);
INSERT INTO measurement_value VALUES('526ead50-655e-415f-b24f-9530adcb93f3',191.43142199516296387);
INSERT INTO measurement_value VALUES('bedc590d-fe1c-42c9-82d9-97fb34e3198f',0.0);
INSERT INTO measurement_value VALUES('ab624578-8a5d-4b01-b238-f31fd242ed70',185.34536743164062499);
INSERT INTO measurement_value VALUES('2835cead-36fc-4dd1-a386-ec8a293185c4',0.0);
INSERT INTO measurement_value VALUES('fb8e6337-f93e-4b6e-9ca4-f4258901fe03',164.12181711196899414);
INSERT INTO measurement_value VALUES('921b7f71-e597-4ab5-9306-b3259621cbbf',0.0);
INSERT INTO measurement_value VALUES('7a50908a-9ad0-4d4c-9727-e4b36031db69',168.04552912712097167);
INSERT INTO measurement_value VALUES('f3a1793e-7923-4af1-9cff-c21374f46535',0.0);
INSERT INTO measurement_value VALUES('dae50eb2-41c3-44e8-946b-b140100d9a01',191.97131204605102539);
INSERT INTO measurement_value VALUES('c9a66e61-2599-4b13-9cb1-fc99e8f22687',0.0);
INSERT INTO measurement_value VALUES('f84e4699-89de-447d-8e21-5edcd2949e26',165.26211190223693847);
INSERT INTO measurement_value VALUES('fc8b7dd2-1792-42fc-a0f7-3abb93598072',0.0);
INSERT INTO measurement_value VALUES('77a1f868-5615-4d72-9478-9926dc459887',180.83477783203125);
INSERT INTO measurement_value VALUES('bba4f06d-8d9f-448f-ad2a-8b19f0033a0c',0.0);
INSERT INTO measurement_value VALUES('c76e131c-00ca-4e1b-a8e9-00da1d433352',186.34708523750305175);
INSERT INTO measurement_value VALUES('6e26a81c-77c2-4a27-839c-abc53884cd23',0.0);
INSERT INTO measurement_value VALUES('55c229e7-ad67-4eca-8e7a-6cdaeb0dc84c',185.46129417419433593);
INSERT INTO measurement_value VALUES('5f270d2f-ffdf-4764-864f-1700b3b2fb0a',0.0);
INSERT INTO measurement_value VALUES('5c3c42cf-c9a4-4786-8315-a26a8fcc0e3d',163.11842989921569824);
INSERT INTO measurement_value VALUES('7af4a465-8308-4e8a-a6e8-58db3cf7575a',0.0);
INSERT INTO measurement_value VALUES('81ae9809-4fe6-475d-96a5-2db2b3120e4d',187.15369486808776855);
INSERT INTO measurement_value VALUES('69c9eee5-9080-4373-b998-7c190d9af89c',0.0);
INSERT INTO measurement_value VALUES('932b9f9f-8bad-4a79-bc33-c68add08f644',184.52216172218322753);
INSERT INTO measurement_value VALUES('8b5df61c-b077-4dc9-a337-7853867974c8',0.0);
INSERT INTO measurement_value VALUES('529fc683-336c-43d5-ab8a-daad8b5a24ae',217.1568460464477539);
INSERT INTO measurement_value VALUES('08eba5bf-16d3-438f-a22b-bf5d26b0b386',105.26466679573059082);
INSERT INTO measurement_value VALUES('8716abd9-23ea-4593-a5ae-8c58e6f4cb40',16.0);
INSERT INTO measurement_value VALUES('46293e08-75e6-4132-b2b1-d44d69fd0749',0.0);
INSERT INTO measurement_value VALUES('1f83f719-0e79-4c7a-a4bd-86177ea0a333',171.0230875015258789);
INSERT INTO measurement_value VALUES('129c2520-d430-469e-b58e-f0b0e38e8af6',76.429417848587036131);
INSERT INTO measurement_value VALUES('94bca89b-e3b8-45e2-89ca-b67a09f0f25c',16.0);
INSERT INTO measurement_value VALUES('c4a7680b-b102-45bd-a850-f188ee3efa72',0.0);
INSERT INTO measurement_value VALUES('4730f1eb-bb39-4350-88ff-a1aa22eae3ab',170.69685673713684082);
INSERT INTO measurement_value VALUES('a1f70e2b-9a7a-494e-be1b-46413deb1611',79.036541223526000977);
INSERT INTO measurement_value VALUES('8e06a1b0-64aa-47fe-b082-0edc98535708',16.0);
INSERT INTO measurement_value VALUES('b50dcd18-b320-4541-a9ee-6f8e43e22b0c',0.0);
INSERT INTO measurement_value VALUES('07637428-0094-42d2-b14f-ec5d0067c633',188.4518587589263916);
INSERT INTO measurement_value VALUES('7fc70af8-7b53-43a1-9d59-8cd981c1f957',75.395543813705444336);
INSERT INTO measurement_value VALUES('5f5eaa9b-e71f-48e2-9c6f-4c1988113710',16.0);
INSERT INTO measurement_value VALUES('f7f40628-6592-4c69-8d92-507a3ff66b16',0.0);
INSERT INTO measurement_value VALUES('84ed138f-177c-4ce6-9562-cc59d8243a57',171.59618926048278808);
INSERT INTO measurement_value VALUES('78b28689-497a-447a-bc4d-059b616c93f9',72.431448936462402344);
INSERT INTO measurement_value VALUES('2e084603-aba8-43b6-ac0a-87c9d207dcbc',16.0);
INSERT INTO measurement_value VALUES('de6904b0-68b7-455c-8c24-107d83c50a09',0.0);
INSERT INTO measurement_value VALUES('aecf8d32-451a-4659-9f0f-617f9ceaaa21',174.52454686164855957);
INSERT INTO measurement_value VALUES('7c22a2a2-93dc-4c76-87f5-140b908feec3',75.039863109588623045);
INSERT INTO measurement_value VALUES('ca2de8fb-a3b3-449d-adf5-c3dc664ddd2c',16.0);
INSERT INTO measurement_value VALUES('62892f34-455b-4ec8-b576-cb08a7466800',0.0);
INSERT INTO measurement_value VALUES('c895d4cf-ee05-4d51-859a-abae4baebe01',212.63106942176818847);
INSERT INTO measurement_value VALUES('39972943-54f2-479e-9421-060716fe215f',104.17088556289672851);
INSERT INTO measurement_value VALUES('487bf46c-1f5e-4c0d-8f3e-9ebfac08ffd5',16.0);
INSERT INTO measurement_value VALUES('4ddbb2c4-ba2a-4cf2-9af6-a71a052f5cf0',0.0);
INSERT INTO measurement_value VALUES('675dc690-b003-42e9-bf8c-146dd4e6a7dc',212.88790273666381835);
INSERT INTO measurement_value VALUES('ef7b86cd-b047-462b-aca8-54363342320c',101.16943955421447753);
INSERT INTO measurement_value VALUES('571af7d3-488e-4877-8796-37d4643b9219',16.0);
INSERT INTO measurement_value VALUES('126059e5-2069-440f-8f1c-02f1712b492a',0.0);
INSERT INTO measurement_value VALUES('d74c4e44-2b27-445c-afe3-88295214ca07',185.6712646484375);
INSERT INTO measurement_value VALUES('77c077bd-150e-40dd-8346-7cb1fe5ff8f8',75.627390384674072266);
INSERT INTO measurement_value VALUES('3c65acc5-c160-4616-adfa-777415e7d2f3',16.0);
INSERT INTO measurement_value VALUES('8e7a1256-642f-4d0a-b936-eca296a01740',0.0);
INSERT INTO measurement_value VALUES('bdf76252-cd1f-4f18-b565-47f5d7cf0dc7',186.00094914436340332);
INSERT INTO measurement_value VALUES('6468697c-9319-434e-9d15-9466b1436a0d',82.610993623733520504);
INSERT INTO measurement_value VALUES('197bc730-91b9-4d3f-9fdd-4edac532c16d',19.0);
INSERT INTO measurement_value VALUES('4a1c885e-60a7-46f9-956a-35711b7fb3f5',0.0);
INSERT INTO measurement_value VALUES('3cfbedc4-e325-47a0-be4f-d6d12a2e9378',188.25984072685241699);
INSERT INTO measurement_value VALUES('ce819968-c580-4ff5-9869-b8855f9a9853',76.047655105590820311);
INSERT INTO measurement_value VALUES('4c28d0af-1ed9-475f-a5aa-7a7dc36d5e39',16.0);
INSERT INTO measurement_value VALUES('e3ff4072-0436-4d08-9222-c6c6283d2326',0.0);
INSERT INTO measurement_value VALUES('ba3aa511-c0ba-4702-b1fb-6e68c91166ae',190.11505889892578125);
INSERT INTO measurement_value VALUES('e1716fae-8069-4a16-8a9c-804d86319f62',78.878773450851440429);
INSERT INTO measurement_value VALUES('b1adb16c-0b33-487f-bd87-b8b37de1801d',19.0);
INSERT INTO measurement_value VALUES('48446c39-171a-4154-88b6-be7652b5d931',0.0);
INSERT INTO measurement_value VALUES('afe1bac0-1c32-453e-9579-9f84ba5b0d86',188.26991128921508789);
INSERT INTO measurement_value VALUES('83079ecc-57e1-4692-b93f-103be9a140e9',75.008082628250122071);
INSERT INTO measurement_value VALUES('c5a293da-4621-41e8-a398-63e98a344489',16.0);
INSERT INTO measurement_value VALUES('af9aa98f-2206-4be0-933b-01ee70e4583d',0.0);
INSERT INTO measurement_value VALUES('e6a13dae-3e0c-4670-abcd-1c6019f4d1c2',168.99283814430236816);
INSERT INTO measurement_value VALUES('bafb3982-7e88-44a9-976e-7923faa41a92',81.047199964523315429);
INSERT INTO measurement_value VALUES('9e12602c-cdea-4e77-b0f8-8782103ceaac',19.0);
INSERT INTO measurement_value VALUES('4f74074c-425b-4f32-b67a-52ee81a606f3',0.0);
INSERT INTO measurement_value VALUES('32b6fe83-577b-45ca-ade1-bec9cd2b0d81',176.14054489135742187);
INSERT INTO measurement_value VALUES('fc4e1221-a2f0-4ce1-ba16-04712bc10691',74.394045114517211914);
INSERT INTO measurement_value VALUES('77b55823-0a88-4ec0-9cc0-e2c5766926f5',16.0);
INSERT INTO measurement_value VALUES('cb3494d7-a2cb-4455-99a6-980bbe31a107',0.0);
INSERT INTO measurement_value VALUES('444fc257-3ba4-433e-a3bf-8015c27e5764',163.79596304893493652);
INSERT INTO measurement_value VALUES('5e1aafb2-0454-4ca8-a8f5-6da42051ac3e',81.694649457931518554);
INSERT INTO measurement_value VALUES('b9b71390-6512-434d-8439-edc16cecb914',19.0);
INSERT INTO measurement_value VALUES('9aa14a5f-ed85-439b-9fee-c03deb21624f',0.0);
INSERT INTO measurement_value VALUES('0fe07903-7491-44ba-808c-1d7d16dec74f',172.05723214149475097);
INSERT INTO measurement_value VALUES('604185f9-8e95-45d5-aecf-dadcec2028ce',75.448721647262573242);
INSERT INTO measurement_value VALUES('74811461-2e07-4cc4-bb39-3eef92dbae69',16.0);
INSERT INTO measurement_value VALUES('ecb33948-b7c4-4c61-aca6-7528fb0403e3',0.0);
INSERT INTO measurement_value VALUES('0c52edb4-cf39-4701-b9fe-e2958790454f',170.50984358787536621);
INSERT INTO measurement_value VALUES('eaebfb7c-0187-4013-98a8-2dfae2e297a4',85.514102220535278316);
INSERT INTO measurement_value VALUES('de06ad13-ce34-49cd-969f-d4ade3fc9b61',19.0);
INSERT INTO measurement_value VALUES('d550cb38-3674-498d-ae03-dc04521a31c6',0.0);
INSERT INTO measurement_value VALUES('f3602ae9-38ad-490d-9528-c77f7b2fb43a',195.12156534194946288);
INSERT INTO measurement_value VALUES('e026bb95-5d10-48ef-af91-d15acdab4d77',77.565819740295410154);
INSERT INTO measurement_value VALUES('47595699-2c5d-48c0-9a1b-3bfb3d489e75',16.0);
INSERT INTO measurement_value VALUES('24ff7d35-3f2b-4edf-8ae8-e5356bea6409',0.0);
INSERT INTO measurement_value VALUES('df20feff-02f9-4197-831c-a151292b0e8e',189.86721825599670409);
INSERT INTO measurement_value VALUES('54fb99ce-56a5-4f04-8eb0-09da71a98c02',84.677231550216674801);
INSERT INTO measurement_value VALUES('8fca8ed5-1ede-4953-a213-128330462451',19.0);
INSERT INTO measurement_value VALUES('7d42547e-b0dd-4f62-8190-910702abb801',0.0);
INSERT INTO measurement_value VALUES('8d64d452-10bc-47d0-a3b8-4f1166916a65',169.48604393005371093);
INSERT INTO measurement_value VALUES('209c2c32-d4fc-4d78-bc71-1bfe77405814',77.208740234374999998);
INSERT INTO measurement_value VALUES('27dc54ca-7773-4d1a-9d46-fe123f83fda5',16.0);
INSERT INTO measurement_value VALUES('79eec84b-b47a-4f5f-9922-15c63cada344',0.0);
INSERT INTO measurement_value VALUES('efb2f006-823a-4857-8469-5c992dc7f0a3',175.1674962043762207);
INSERT INTO measurement_value VALUES('e10d141f-22fa-4c9e-a749-ee4240ec3276',76.449127197265625);
INSERT INTO measurement_value VALUES('2d08fc7d-8820-484f-9065-bbb0c4dd88e1',16.0);
INSERT INTO measurement_value VALUES('4c00e4b6-6942-43b6-bd90-d5768c65dffb',0.0);
INSERT INTO measurement_value VALUES('88a55fce-8d91-41d7-a502-36b0262a96a2',189.31902098655700683);
INSERT INTO measurement_value VALUES('ec9f4adc-b27a-4a3d-a0cd-c9ff41bed969',72.318909645080566407);
INSERT INTO measurement_value VALUES('d87b7794-8721-45b2-bbd9-28e758b8d7f9',16.0);
INSERT INTO measurement_value VALUES('a11834f2-cc98-47b1-9210-4bf6ae3feeef',0.0);
COMMIT;
