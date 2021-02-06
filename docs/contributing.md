# Guide to contributing to VelCom

## Workflow

Some general directions on how development around this repository is structured.

### Branches

There are two main branches, `main` and `stable`.

The `stable` branch is always stable and can be used for CD.
The `main` branch is continuously deployed to a test instance at [velcom.aaaaaaah.de](https://velcom.aaaaaaah.de/).
When the `main` branch appears to be stable enough, the `stable` branch is fast-forwarded to its current state.

The `main` branch is used for development roughly following GitHub flow:
- Feature branches originate in the `main` branch and are merged back into it via PRs.
- It should be stable, but exceptions can be made in certain situations.


### PRs

PRs should originate from and merge back into the `main` branch.
Usually, a PR is merged by the initial author.

### Labels

Labels should always be used in lowercase.
The following labels can be used, if applicable:
- `frontend` (concerns the frontend)
- `backend` (concerns the backend)
- `runner` (concerns the runner)
- `shared` (concerns the shared package)
- `meta` (concerns dependencies and other build tool config files)
- `docs` (improvements or additions to documentation)
- `cli` (concerns the CLI)

Commits should be labeled by prefixing the commit summary with one or more labels, for example:
- `[runner] Always delete received benchrepo tar`
- `[backend,runner] Do not set results when runner is reset on connect`

Issues and PRs should not contain labels in the title like commits.
Instead, they are labeled via GitHub.

## Setup

Directions on how to set up a development environment.

First, install all build requirements specified in the [manual install guide](install_manual.md#required-software).

We use intellij for backend and runner development.
Backend and runner development is confined to the `backend/` directory.

### IntelliJ autoformatting

This is an attempt at providing instructions for reproducible code autoformatting in intellij.

1. Import the [google style](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml) as project style
2. Editor > General
    1. Enable `Ensure an empty line at the end of a file on Save`
3. Enable `Optimize imports` when formatting files
4. Editor > Code style > Java > Tabs and Indents
    1. Enable tabs and smart tabs
    2. Set the continuation indentation to 2
5. Editor > Code style > Java > JavaDoc
    1. Disable `Align parameter descriptions` and `Align thrown exception descriptions`
    2. Enable `Indent continuation lines`
