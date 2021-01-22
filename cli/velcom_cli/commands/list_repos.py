import json

import requests


def register(subparsers):
    parser = subparsers.add_parser(
        "list-repos",
        aliases=["lr"],
        help="list all existing repos and their ids",
    )
    parser.set_defaults(f=command)


def command(config):
    api_url = config.geturl("api_url")

    response = requests.get(api_url + "all-repos")
    repos = json.loads(response.text)["repos"]
    for repo in repos:
        print(f"{repo['id']} - {repo['name']}")
