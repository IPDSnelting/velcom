# Manual installation guide

A guide to compiling and configuring a single VelCom instance from scratch. For
other ways of installing VelCom, see the [installation overview](install.md).

All commands in this guide should be executed in the repo's base directory
unless noted otherwise. For example, if you cloned the repo to `~/src/velcom/`,
the commands should also be executed from `~/src/velcom`.

## Project structure

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

Here is a diagram of how all the different components work together:

```
                +------+                     +------+
                | user |                     | repo |
                +------+                     +------+
                    |                            ^
                    v                            |
               +---------+                       |
            .--| browser |--.                    |
            |  +---------+  |                    |
            |               |                    |
          +-------------------+                  |
          | |     nginx     | |                  |
      .---|-´   (or other   `-|---.              |
      |   |    web server)    |   |              |
      |   +-------------------+   |              |
      |                           |              |
https://url/*             https://url/api/*      |
      |                           |              |
      v                           |              |
+----------+                      |   +-------------+
| frontend |                      |   | backend     |
+----------+                      |   |             |
                   +--------+     `-->| api port    |
                   | runner |--(ws)-->| runner port |
                   +--------+         +-------------+
```

## Required software

To build velcom, the following software must be installed on your computer:

- [Python 3](https://www.python.org/)
- [Yarn](https://yarnpkg.com/)
- [Maven](https://maven.apache.org/)
- JDK 11

To run the frontend, you will need to install and configure a web server like
`nginx`. To run the backend, you will need a JRE 11. We recommend you also
install `git` for faster clone times. To run the runner, you will only need a
JRE 11.

## Frontend

First, edit `frontend/.env.production`. Set `BASE_URL` to the path where the
frontend will be served. Set `VUE_APP_BASE_URL` to the URL where the backend's
REST API can be reached. Leave the other fields untouched.

If your velcom instance should be reachable under `https://example.com/velcom/`
and your API can be reached under `https://example.com/velcom/api/`, the file
should look like this:

```
NODE_ENV="production"
BASE_URL="/velcom/"
VUE_APP_BASE_URL="https://example.com/velcom/api/"
```

Then, run the following command (see its `--help` for available flags):

```bash
$ scripts/build-frontend
```

The frontend's static files are now located in the directory `frontend/dist/`.
Copy them to wherever your web server requires them to be. If the web server
encounters a request to the base url or its children but no corresponding file
exists in `frontend/dist/`, it must respond with the `frontend/dist/index.html`
file. The only exception to this rule is the path to the API and its children,
which must instead be redirected to the backend.

An example nginx config is available with the [docker build
scripts](../scripts/docker/). It includes `nginx.conf`, which is the main nginx
configuration, and `example-site`, which should be placed in the
`sites-available/` directory and symlinked to in the `sites-enabled/` directory.

## Backend

First, run the following command to build the `backend.jar` and `runner.jar`:

```bash
$ scripts/build-backend
```

The `backend.jar` can be found at `backend/backend/target/backend.jar`. Copy
this to the directory you want your backend to run in. Also copy over the
[example config](../backend/backend/src/main/resources/example_config.yml) from
`backend/backend/src/main/resources/example_config.yml` and rename it to
`config.yml`.

Read carefully through the example config and choose appropriate config values.
All available options are described in detail in the example config. The backend
will create and manage the directories `data/`, `cache/` and `tmp/` in your
installation directory. These directories are further described in the example
config.

To launch the backend, **navigate to your backend installation directory** and
execute the following command:

```bash
$ java -jar backend.jar server config.yml
```

## Runner

After compiling the backend in the previous section, the `runner.jar` can be
found at `backend/runner/target/runner.jar`. Copy this to the directory you want
your runner to run in. Also copy over the example config from
`backend/runner/src/main/resources/example_config.json` and rename it to
`config.json**.


Since the config file can only contain rudimentary comments, here is a detailed
description of the various config options. If you are unsure where a specific
option should go, see the example config.

- `name`: A unique identifier for this runner. The backend uses the runner names
  to identify runners. This way, the backend can, for example, distinguish
  between a known runner reconnecting and a new runner connecting.

- `backends`: A list of backends the runner should connect to. A runner can be
  connected to multiple backends at the same time. If multiple backends try to
  schedule benchmarks at the same time, the runner uses a basic round-robin
  scheme to choose which one to run next.

- `address`: The url under which the backend's runner websocket server (*not*
  the REST API) can be reached. Make sure to use `ws` or `wss` as the protocol,
  *not* `http` or `https`.

- `token`: The respective backend's runner authentication token. See the backend
  example config for more details.

- `directory`: The directory under which files received from this backend should
  be placed and benchmarks for this backend should be executed. It is safe to
  delete these directories when the runner is stopped. They don't contain any
  data meant to be persisted.

To launch the backend, **navigate to your runner installation directory** and
execute the following command:

```bash
$ java -jar runner.jar config.json
```
