# VelCom

Logo?

**TODO:** Write this section
**TODO:** Add screenshots

Short description should mention (with screenshots):

- Tracks one or more git repos
- Runs user-defined benchmarks against every new commit
- Show graphs of benchmark results over time (screenshot)
- Show commits with unusally large changes (screenshot)
- Compare different repos against each other (screenshot after implemented)
- Compare two commits directly (screenshot)

## Overview

**TODO:** Write this section

Features in more detail:

- Tracks one or more git repos via remote url
- One benchmark repo per backend
    - Defines which benchmarks and how to run them
    - Is applied to all tracked repos
- Benchmarks are (usually) run on one or more dedicated machines
    - One backend can use multiple benchmark machines
    - One machine can be used for multiple backends
- Runs only on linux systems, windows not supported

**TODO:** Write this section
**TODO:** Maybe move this below or make it a separate section?

Project structure (related to setup):

- "backend" is server that coordinates everything and presents data via REST API
- "frontend" is Vue SPA (static files), usually served via nginx
- "runner" connects to backend and receives and executes benchmarks

## Installation and Setup

**TODO:** Write this section

Requirements:

- Linux server to host backend and frontend (usually on same machine)
    - Since compiled frontend is static files, it may also be hosted somewhere else
    - This option is not explored in the setup guide though
- One or more linux machines to run benchmarks
- Benchmark script

- Frontend and backend's API must be reachable by user viewing web page
- Backend's runner port must be reachable by benchmark runners
    - Runners do not need to communicate with backend over internet
- More information and detailed steps in installation guide

- [Installation](docs/install.md)
- [Benchmark repo specification](docs/bench_repo_specification.md)

## Other links

**TODO:** Say more about CLI?

- [CLI readme](cli/README.md)
- [Guide to contributing](docs/contributing.md)

## Uploading a tar via curl

If you quickly want to benchmark your current directory from the command line,
run this command, replacing `VELCOM_ADDRESS` and `ADMIN_PASSWORD` with the
proper values:

```
tar -czO . | curl VELCOM_ADDRESS/api/queue/upload/tar \
  -F 'file=@-;filename=file.tar.gz' \
  -F description='console upload' \
  -u admin:ADMIN_PASSWORD
```

If you want to assign the tar to a specific repository, append this flag to the
command above, replacing `REPO_ID` with the repo's UUID (can be found on the
repo detail page):

```
-F repo_id=REPO_ID
```

Here is an example with all of the above:

```
tar -czO . | curl https://velcom.aaaaaaah.de/api/queue/upload/tar \
  -F 'file=@-;filename=file.tar.gz' \
  -F description='console upload' \
  -u admin:12345 \
  -F repo_id=44bb5c8d-b20d-4bef-bdad-c92767dfa489
```

For a nicer looking result, pipe the output from `curl` into `jq`.
