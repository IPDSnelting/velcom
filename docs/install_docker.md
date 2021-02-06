# Docker installation and deployment guide

A guide to packaging and running a VelCom server using the provided Docker
image. For other ways of installing VelCom, see the [installation
overview](install.md). This guide does not include the runner.

All commands in this guide should be executed in the repo's base directory
unless noted otherwise. For example, if you cloned the repo to `~/src/velcom/`,
the commands should also be executed from `~/src/velcom`.

## Project structure

A VelCom instance consists of three main parts: The **frontend**, the
**backend** and one or more **runners**.

1. The **frontend** is a vue-based single page app running in the user's
   browser. It consists of a bunch of static files that need to be served in a
   particular way.

2. The **backend** is a java-based server that clones repos, coordinates
   benchmarks for those repos and stores the results. It provides a REST API
   ([OpenAPI spec](public_api.yaml)) for the **frontend**. It also provides a
   websocket server which the **runners** connect to.

3. The **runners** are java programs that run benchmarks they receive from the
   **backend**. These should usually run on dedicated machines which are all
   equally fast. They connect to the backend on a dedicated port.

The Docker image contains the backend, frontend files and an nginx server
routing requests correctly. Below is an adjusted version of the [VelCom
structure diagram](install_manual.md#project-structure) that includes the Docker
image boundaries:

```
                  +------+                     +------+
                  | user |                     | repo |
                  +------+                     +------+
                      |                            ^
                      v                            |
                 +---------+                       |
                 | browser |                       |
                 +---------+                       |
                      |                            |
                      | PORT 80                    |
+----------------------------------------------------------+
| Docker              |                            |       |
|                     v                            |       |
|           +-------------------+                  |       |
|           |       nginx       |                  |       |
|       .---|     (routed by    |---.              |       |
|       |   |    path prefix)   |   |              |       |
|       |   +-------------------+   |              |       |
|       |                           |              |       |
| https://url/*             https://url/api/*      |       |
|       |                           |              |       |
|       v                           |              |       |
| +----------+                      |   +-------------+    |
| | frontend |                      |   | backend     |    |
| +----------+                      |   |             |    |
|                                   `-->| api port    |    |             +--------+
|                                       | runner port |<---|- PORT 82 ---| runner |
|                                       +-------------+    |   (ws)      +--------+
|                                              |           |
+----------------------------------------------------------+
                                  VOLUME /home/velcom/config
                                               |
                                           +--------+
                                           | config |
                                           | (yaml) |
                                           +--------+
```

## Building

You need to first build the frontend and backend according to the [manual
installation guide](install_manual.md). Then you can generate a docker image
using the `build-docker` script:

```bash
# scripts/docker/build-docker DEV
```

The script will copy all necessary files to a temporary `.docker` folder in the
project root and invoke the correct docker command to build it. If you want to
know more about available flags and options refer to
`scripts/docker/build-docker --help`.

The script can optionally include the AspectJ runtime weaver in the docker
image, which allows VelCom to provide more detailed metrics in
prometheus/dropwizard metrics format.

## Configuring and running

When running the image you need to be aware of a few general things:
- The exposed port for the front- and backend is `80`
- The exposed port for runner connections is `82`

The docker image also exports three volumes you might want to mount:
- `/home/velcom/config` (required) - The config file is placed here
- `/home/velcom/data` (required) - All data VelCom needs to persist
- `/home/velcom/cache` (optional) - Speeds up startup if persisted between restarts

Copy the [example
config](../backend/backend/src/main/resources/example_config.yml) from
`backend/backend/src/main/resources/example_config.yml` into your mounted config
directory and rename it to `config.yml`. Read carefully through the example
config and choose appropriate config values. All available options are described
in detail in the example config. **Don't change the `dataDir`, `cacheDir` and
`tmpDir` options!**

The VelCom Docker image tries to be safe-ish and runs VelCom as a non-privileged
`velcom` user after starting nginx. This might lead to conflicts with your `-v`
mounted volumes: VelCom might not have sufficient permissions to access them. To
solve this problem the `build-docker` script has a `UID` flag that can be used
to specify the UID of the `velcom` user inside the docker image. You can then
`chown` the host directories to the same UID.

### The run-docker helper script

To make running the docker container a bit easier and act as a reference, a
`run-docker` script is provided:

```bash
# scripts/docker/run-docker <directory>
```

The script will create the passed directory if it doesn't exist and create two
sub-directories inside:
- A `config` directory with the example config. The script will *not* overwrite
  it if it already exists.
- A `data` directory that is world read- and writable (or instead owned by the
  given UID if you used the `--uid` flag)

After those folders are configured, the script will run the image for you,
binding the front- and backend to port `8080` and the runner connection port to
`8082` by default.
