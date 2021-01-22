# A python-based CLI for VelCom

## Installation

Run this command to install or update the velcom CLI:
```bash
$ pip install 'git+https://github.com/IPDSnelting/velcom/#subdirectory=cli'
```

After that, the `velcom` command should be available. Run `velcom --help` to get
an overview of the available commands (and their shorter aliases).

## Configuration

The config file can be placed at `~/.config/velcom/velcom.conf` or
`~/.velcom.conf`. A custom config file location can also be passed via the
`--config` or `-c` flags. To print the default config, run `velcom
default-config`. To print the config currently in use, run `velcom
print-config`.

The config file is split into sections like an `.ini` file. If a section doesn't
contain a value, the corresponding value from the `DEFAULT` section is used. For
more file format details, see [this example config
file](https://docs.python.org/3/library/configparser.html#supported-ini-file-structure).

Each section represents a separate *profile*. Profiles are useful when using the
velcom CLI with multiple velcom servers. When executing a command, the profile
to use can be specified via the `--profile` or `-p` flags. The default profile
is specified by the `default_profile` option in the `DEFAULT` section.

| name | description |
|------|-------------|
| `default_profile` | Name of the default profile to use. Default value: `DEFAULT` |
| `site_url` | URL of the velcom UI. Must start with `http://` or `https://` |
| `api_url` | URL of the velcom API. Must start with `http://` or `https://` |
| `admin_pw` | The admin password, if known |

An example config file specifying multiple different profiles:
```ini
[DEFAULT]
default_profile = local

[local] # For development
site_url = http://localhost:8080/
api_url = http://localhost:9001/
admin_pw = 12345

[aaaaaaah]
site_url = https://velcom.aaaaaaah.de/
api_url = https://velcom.aaaaaaah.de/api/
```

## Development

This sub-project uses `venv` and `setuptools`. To set up a development
environment, run these commands in the same directory as this `README`:

```bash
$ python -m venv .venv
$ . .venv/bin/activate
$ pip install --editable .
```

After this, `velcom_cli` and all its dependencies will be installed in the venv.
The installation of `velcom_cli` uses the local files, meaning that any changes
will immediately be reflected in the `velcom` command.

Whenever `setup.cfg` is changed, re-run `$ pip install --editable .` inside the
`venv`.
