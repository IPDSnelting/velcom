# Docker deployment guide

A guide to packaging VelCom as a Docker image using the provided Docker image.
Before starting this guide, you should have **built VelCom** according to the
[installation overview](install.md). You do not need to copy any files around,
but the backend needs to be built and the frontend configured and built.

All commands in this guide should be executed in the repo's base directory
unless noted otherwise. For example, if you cloned the repo to `~/src/velcom/`,
the commands should also be executed from `~/src/velcom`.

## Recap: Project structure

A VelCom instance consists of three main parts: The **frontend**, the
**backend** and one or more **runners**.

1. The **frontend** is a vue-based single page app running in the user's
   browser. It consists of a bunch of static files that need to be served in a
   particular way. More details can be found in the [frontend
   section](#Frontend).

2. The **backend** is a java-based server that clones repos, coordinates
   benchmarks for those repos and stores the results. It provides a REST API
   ([OpenAPI spec](public-api/public-api.v2.yaml)) for the **frontend**. It also
   provides a websocket server which the **runners** connect to.

3. The **runners** are java programs that run benchmarks they receive from the
   **backend**. These should usually run on dedicated machines which are all
   equally fast. They connect to the backend on a dedicated port.

The Docker image contains the backend, frontend files and an nginx server
routing requests correctly. Below is an adjusted version of the VelCom
structure diagram that includes the Docker image boundaries:

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
|                                       | runner port |<---|-PORT 82 ----| runner |
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

You need to have *built* the frontend and backend according to the [manual
installation](install_manual.md).

Once that is done you can generate a docker image using the `build-docker` script:
```bash
$ scripts/docker/build-docker DEV
```
The script will take care of copying all necessary files to a temporary
`.docker` folder in the project root and invokes the correct docker command to
build it.
The script can optionally include the AspectJ runtime weaver in the docker
image, which allows VelCom to provide more detailed metrics in
prometheus/dropwizard metrics format.
If you want to know more about available flags and options refer to `scripts/docker/build-docker --help`.

## Running it

When running the image you need to be aware of a few general things:
- The exposed port for the front- and backend is `80`
- The exposed port for runner connections is `82`

VelCom also uses three main folders described in the example config that you
might want to mount outside the image:
- `/home/velcom/config` - The directory for your config file
- `/home/velcom/data` - The directory for all data VelCom needs to persist
- `/home/velcom/cache` - The directory for data that might speedup application
  startup but is not needed to operate correctly

The VelCom Docker image tries to be safe-ish and runs VelCom as a
non-privileged `velcom` user after starting nginx. This might lead to conflicts
with your `-v` mounted volumes: VelCom might not have sufficient permissions to
access them.
To solve this problem the `build-docker` script has a `UID` flag that can be
used to specify the UID of the `velcom` user inside the docker image. You can
then `chown` the host directories to the same UID.

### The run-docker helper script

To make that a bit easier and act as a reference a `run-docker` script is provided:
```bash
$ scripts/docker/run-docker <directory>
```
The script will create the passed directory if it doesn't exist and create two sub-directories inside:
- A `config` directory with the example config. The script will *not* overwrite it if it already exists.
- A `data` directory that is world read- and writable (or instead owned by the given UID if you used the `--uid` flag)

After those folders were configured it will run the image for you, binding the
front- and backend to port `8080` and the runner connection port to `8082` by
default.
