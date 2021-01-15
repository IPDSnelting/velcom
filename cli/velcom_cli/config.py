import configparser
from pathlib import Path


class Config:
    DEFAULT_URL = "https://velcom.aaaaaaah.de/api/"

    def __init__(self, args, parser):
        self.args = args
        self.parser = parser

    @classmethod
    def default_parser(cls):
        config = configparser.ConfigParser()
        config[configparser.DEFAULTSECT]["url"] = cls.DEFAULT_URL
        return config

    @classmethod
    def load(cls, args):
        if args.config is None:
            paths = [
                Path("~/.config/velcom/velcom.conf").expanduser(),
                Path("~/.velcom.conf").expanduser(),
            ]
        else:
            paths = [args.config]

        config = cls(args, cls.default_parser())
        config.parser.read(paths)

        return config

    @property
    def profile(self):
        if self.args.profile is None:
            return self.parser[configparser.DEFAULTSECT]
        else:
            return self.parser[self.args.profile]