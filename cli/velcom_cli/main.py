import argparse
import configparser
from pathlib import Path


# TODO Move to separate module
def cmd_bench_tar(args, config):
    print("Placeholder bench-tar")
    print(f"{args = }")
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

    subparsers = parser.add_subparsers(
        required=True,
        # A metavar or dest is necessary for "required=True" to not crash when
        # arguments contain no command. See https://bugs.python.org/issue29298
        metavar="COMMAND",
        help="one of the following commands:",
    )

    bench_tar = subparsers.add_parser(
        "bench-tar",
        aliases=["bt"],
        help="benchmark a directory by uploading it as a tar file",
    )
    bench_tar.add_argument("bench_dir", type=Path, metavar="BENCHDIR")
    bench_tar.set_defaults(f=cmd_bench_tar)

    return parser


def read_config(config_file_path=None):
    if config_file_path is None:
        paths = [
            Path("~/.config/velcom/velcom.conf").expanduser(),
            Path("~/.velcom.conf").expanduser(),
        ]
    else:
        paths = [config_file_path]

    config = configparser.ConfigParser()
    config.read(paths)

    return config


def main():
    args = make_parser().parse_args()
    config = read_config(args.config)
    args.f(args, config)
