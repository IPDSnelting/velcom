# VelCom

VelCom lets you progressively benchmark one or more projects and monitor or
compare the benchmark results. This makes it easier to notice performance
regressions or see how different branches compare. It also lets you visualize
how different metrics have developed over time.

Here are a few links to get started:
- [Installation](docs/install.md)
- [CLI readme](cli/README.md)
- [Benchmark repo specification](docs/bench_repo_specification.md)
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
