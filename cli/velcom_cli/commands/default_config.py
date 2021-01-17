import io


def register(subparsers):
    parser = subparsers.add_parser(
        "default-config",
        aliases=["dc"],
        help="print the default config file on stdout",
    )
    parser.set_defaults(f=command)


def command(config):
    with io.StringIO() as stream:
        config.default_parser().write(stream)
        print(stream.getvalue(), end="")
