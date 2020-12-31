# A python-based CLI for VelCom

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
