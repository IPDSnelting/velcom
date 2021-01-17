import json
import tarfile
import tempfile
from pathlib import Path

import requests


def register(subparsers):
    parser = subparsers.add_parser(
        "bench-dir",
        aliases=["bd"],
        help="benchmark a directory by uploading it as a tar file",
    )
    parser.add_argument(
        "bench_dir",
        nargs="?",
        type=Path,
        metavar="BENCHDIR",
        help="directory to benchmark (default: current directory)",
    )
    parser.add_argument(
        "--repo", "-r",
        metavar="REPO",
        help="id or name of the repo this task should be attached to",
    )
    parser.set_defaults(f=command)


def normalize(s):
    """
    Remove all of a string's whitespace characters and lowercase it.
    """

    return "".join(c for c in s.lower() if not c.isspace())


def find_repo_id(repos, repo):
    # Search by ID
    ids = {repo["id"] for repo in repos}
    if repo in ids:
        return repo

    # Strict name search
    ids_by_name = {repo["name"]: repo["id"] for repo in repos}
    if repo in ids_by_name:
        return ids_by_name[repo]

    # More lenient name search
    ids_by_name = {normalize(repo["name"]): repo["id"] for repo in repos}
    if repo in ids_by_name:
        return ids_by_name[normalize(repo)]

    print(f"Invalid repo id or name: {repo!r}")
    print("Available repos:")
    for repo in repos:
        print(f"{repo['id']} - {repo['name']}")
    exit(1)


def command(config):
    site_url = config.geturl("site_url")
    api_url = config.geturl("api_url")
    admin_pw = config.get("admin_pw")

    bench_dir = config.args.bench_dir or Path.cwd()

    # Find repo id
    repo_id = None
    if repo := config.args.repo:
        response = requests.get(api_url + "all-repos")
        repos = json.loads(response.text)["repos"]
        repo_id = find_repo_id(repos, repo)

    print("Creating temporary file")
    with tempfile.TemporaryFile() as tmpf:
        print(f"Tar-ing {bench_dir.resolve()}")

        # Using gzip compression
        with tarfile.open(mode="x:gz", fileobj=tmpf) as tarf:
            tarf.add(bench_dir, arcname="")  # Recursive by default
        tmpf.seek(0)  # Otherwise we'd be sending no data

        upload_api_url = api_url + "queue/upload/tar"
        description = f"CLI upload of {bench_dir.resolve().name}"
        print(f"Uploading tar file to {upload_api_url}")
        response = requests.post(
            upload_api_url,
            auth=("admin", admin_pw),
            data={"description": description, "repo_id": repo_id},
            # Specifying a file name ending in ".tar.gz" so the server knows
            # that this is a gzipped tar file
            files={"file": ("upload.tar.gz", tmpf)},
        )

        info = json.loads(response.text)
        print(f"Task at {site_url}task-detail/{info['task']['id']}")
        print(f"Run soon at {site_url}run-detail/{info['task']['id']}")
