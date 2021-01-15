import io


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

    for k, v in config.profile_section.items():
        print(f"{k} = {v}")

    print()
