import io


def register(subparsers):
    parser = subparsers.add_parser(
        "print-config",
        aliases=["pc"],
        help="print the currently active configuration on stdout",
    )
    parser.set_defaults(f=command)


def command(config):
    print("####################")
    print("## Current config ##")
    print("####################")
    print()

    with io.StringIO() as stream:
        config.parser.write(stream)
        print(stream.getvalue(), end="")

    print()

    profile_name = config.profile_name
    profile_hashes = "#" * len(profile_name)

    print("######################" + profile_hashes)
    print(f"## Active profile: {profile_name} ##")
    print("######################" + profile_hashes)
    print()

    for k, v in config.items():
        print(f"{k} = {v}")

    print()
