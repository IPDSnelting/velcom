import io


def command(config):
    with io.StringIO() as stream:
        config.default_parser().write(stream)
        print(stream.getvalue(), end="")
