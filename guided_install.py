#!/usr/bin/env python3
import shutil
import re
import subprocess
from pathlib import Path

PORT_REGEX = r"\d{1,5}"
API_ADDRESS_REGEX = r"https?://(\w+\.)+(\w+)/?"
RUNNER_ADDRESS_REGEX = r"(\w+\.)+(\w+)(/.*)?"
NUMBER_REGEX = r"\d+"

def request(prompt, regex=".*"):
    while True:
        result = input(prompt + ": ")
        if re.fullmatch(regex, result):
            return result
        else:
            print("Invalid input format")

def escape_quotes(string):
    return string.replace("\\", "\\\\").replace('"', '\\"')

def main():
    print()
    print("##################")
    print("## Initializing ##")
    print("##################")
    print()

    cur_dir = Path(__file__).parent.resolve()
    tmp_dir = cur_dir / "tmp"
    print("- Running in directory " + str(cur_dir))
    print("- Creating temporary directory " + str(tmp_dir))
    tmp_dir.mkdir(parents=True, exist_ok=True)

    print()
    print("##################################")
    print("## Prompting for config options ##")
    print("##################################")
    print()

    api_port = request("- Port the frontend should use to contact the backend (e. g. 8080)", PORT_REGEX)
    api_address = request ("- Address the frontend should use to contact the backend (requires http or https, e. g. https://example.com/)", API_ADDRESS_REGEX)
    if api_address.endswith("/"): api_address = api_address[:-1]

    runner_port = request("- Port the runner should use to contact the backend (e. g. 8081)", PORT_REGEX)
    runner_address = request("- Address the runner should use to contact the backend (just the address without http or https, e. g. 192.168.0.74)", RUNNER_ADDRESS_REGEX)
    if runner_address.endswith("/"): runner_address = runner_address[:-1]
    runner_ssl = request("- Should the runner use ssl? (yes/no)", r"y(es)?|no?")
    runner_ssl = "wss://" if runner_ssl.startswith("y") else "ws://"

    bench_repo_url = request("- URL of the benchmark repo (e. g. git@git.scc.kit.edu:aaaaaaah/pseubench.git)")
    web_admin_token = request("- Web admin token", ".+")
    web_admin_token = escape_quotes(web_admin_token)
    runner_token = request("- Runner token", ".+")
    runner_token = escape_quotes(runner_token)

    hash_memory = request("- Memory in KiB that the hash algorithm should use (e. g. 5120)", NUMBER_REGEX)
    hash_time = request("- Time in milliseconds that the hash algorithm should take (e. g. 500)", NUMBER_REGEX)

    print()
    print("###############################################################")
    print("## Generating config files, compiling and collecting results ##")
    print("###############################################################")
    print()

    print("- Preparing frontend/.env.production.local")
    with open(cur_dir / "frontend" / ".env.production.local", "w") as f:
        f.write('VUE_APP_BASE_URL="' + api_address + ":" + api_port + '/"')

    print("- Running make")
    subprocess.run(["make", "-C", cur_dir])

    print("- Figuring out the amount of hash iterations... ", end="", flush=True)
    result = subprocess.run(["java", "-jar", cur_dir / "backend" / "backend" / "target" / "backend.jar", "hashPerformance", hash_time, hash_memory], stdout=subprocess.PIPE)
    hash_iterations = re.match(r"^(\d+)\s", result.stdout.decode("utf-8")).group(1)
    print("It's " + hash_iterations)

    print("- Preparing tmp/backend_config.yaml")
    with open(cur_dir / "backend" / "backend" / "src" / "main" / "resources" / "example_config.yml") as f:
        backend_config = f.read()
    backend_config = backend_config.replace(
        'webAdminToken: "12345"',
        'webAdminToken: "' + web_admin_token + '"'
    ).replace(
        "restApiPort: 80",
        "restApiPort: " + api_port
    ).replace(
        "runnerPort: 3546",
        "runnerPort: " + runner_port
    ).replace(
        'benchmarkRepoRemoteUrl: "git@git.scc.kit.edu:aaaaaaah/pseubench.git"',
        'benchmarkRepoRemoteUrl: "' + bench_repo_url + '"'
    ).replace(
        'runnerToken: "Correct-Horse_Battery Staple"',
        'runnerToken: "' + runner_token + '"'
    ).replace(
        'hashMemory: 5120',
        'hashMemory: ' + hash_memory
    ).replace(
        'hashIterations: 50',
        'hashIterations: ' + hash_iterations
    )
    with open(tmp_dir / "backend_config.yaml", "w") as f:
        f.write(backend_config)

    print("- Preparing tmp/runner_config.json")
    with open(cur_dir / "backend" / "runner" / "src" / "main" / "resources" / "example_config.json") as f:
        runner_config = f.read()
    runner_config = runner_config.replace(
        '"runnerToken": "Correct-Horse_Battery Staple"',
        '"runnerToken": "' + runner_token + '"'
    ).replace(
        '"serverUrl": "ws://localhost:3546/runner-connector"',
        '"serverUrl": "' + runner_ssl + runner_address + ":" + runner_port + '/runner-connector"'
    )
    with open(tmp_dir / "runner_config.json", "w") as f:
        f.write(runner_config)

    print("- Copying tmp/backend.jar and tmp/runner.jar")
    shutil.copyfile(cur_dir / "backend" / "backend" / "target" / "backend.jar", tmp_dir / "backend.jar")
    shutil.copyfile(cur_dir / "backend" / "runner" / "target" / "runner.jar", tmp_dir / "runner.jar")

    print()
    print("##########################")
    print("## Further instructions ##")
    print("##########################")
    print()

    print("  CONTINUE INSTALLATION")
    print("1. Copy tmp/backend.jar and tmp/backend_config.yaml to your backend installation directory")
    print("2. Copy tmp/runner.jar and tmp/runner_config.json to your runner installation directory")
    print("3. Assign a unique name to each of your runners!")
    print("4. Configure your web server according to the rules in the readme (see docs/nginx-site for an example nginx config)")
    print("5. Copy frontend/dist/ to a directory where it is served by your web server")
    print()

    print("  START VELCOM")
    print("1. Run 'java -jar backend.jar server backend_config.yaml' in your backend installation directory")
    print("2. Run 'java -jar runner.jar runner_config.json' in each runner's installation directory")
    print("3. Ensure your web server is running and serving the frontend")
    print()

    print("Have a nice day :D")

if __name__ == '__main__':
    main()
