import argparse
from pathlib import Path

from . import commands
from .config import Config


# TODO Move to separate module
def cmd_bench_tar(config):
    print("Placeholder bench-tar")
    print(f"{config = }")


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

    default_config = subparsers.add_parser(
        "default-config",
        aliases=["dc"],
        help="print a default config file on stdout",
    )
    default_config.set_defaults(f=commands.default_config)

    bench_tar = subparsers.add_parser(
        "bench-tar",
        aliases=["bt"],
        help="benchmark a directory by uploading it as a tar file",
    )
    bench_tar.add_argument(
        "bench_dir",
        nargs="?",
        type=Path,
        metavar="BENCHDIR",
        help="directory to benchmark (default: current directory)"
    )
    bench_tar.set_defaults(f=cmd_bench_tar)

    return parser


def main():
    args = make_parser().parse_args()
    config = Config.load(args)
    args.f(config)
