import argparse
import configparser
from pathlib import Path

from . import commands
from .config import Config


def make_parser():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--config", "-c",
        type=Path,
        metavar="CONFIGFILE",
        help="load config from CONFIGFILE instead of the default"
        " paths (~/.config/velcom/velcom.conf, ~/.velcom.conf)",
    )
    parser.add_argument(
        "--profile", "-p",
        metavar="PROFILE",
        help="name of the config file section to use",
    )

    subparsers = parser.add_subparsers(
        required=True,
        # A metavar or dest is necessary for "required=True" to not crash when
        # arguments contain no command. See https://bugs.python.org/issue29298
        metavar="COMMAND",
        help="one of the following commands:",
    )

    commands.default_config.register(subparsers)
    commands.print_config.register(subparsers)
    commands.bench_dir.register(subparsers)

    return parser


def main():
    args = make_parser().parse_args()
    config = Config.load(args)
    try:
        args.f(config)
    except configparser.NoOptionError as e:
        print(e)
        exit(1)
