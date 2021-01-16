import json
import tarfile
import tempfile
from pathlib import Path

import requests


# TODO Allow specifying a repo
def command(config):
    site_url = config.geturl("site_url")
    api_url = config.geturl("api_url")
    admin_pw = config.get("admin_pw")

    bench_dir = config.args.bench_dir or Path.cwd()

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
            data={"description": description},
            # Specifying a file name ending in ".tar.gz" so the server knows
            # that this is a gzipped tar file
            files={"file": ("upload.tar.gz", tmpf)},
        )

        info = json.loads(response.text)
        print(f"Task at {site_url}task-detail/{info['task']['id']}")
        print(f"Run soon at {site_url}run-detail/{info['task']['id']}")
