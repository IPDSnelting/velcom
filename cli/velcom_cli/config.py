import configparser
from pathlib import Path


class Config:
    DEFAULT = configparser.DEFAULTSECT

    def __init__(self, args, parser):
        self.args = args
        self.parser = parser

    @classmethod
    def default_parser(cls):
        config = configparser.ConfigParser()
        config[cls.DEFAULT]["default_profile"] = cls.DEFAULT
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
    def profile_name(self):
        if self.args.profile is None:
            return self.parser.get(self.DEFAULT, "default_profile")
        else:
            return self.args.profile

    @property
    def profile_section(self):
        return self.parser[self.profile_name]

    def get(self, *args, **kwargs):
        return self.parser.get(self.profile_name, *args, **kwargs)

    def geturl(self, *args, **kwargs):
        """
        Like get, but automatically appends a "/" if the url doesn't already
        end with one.
        """

        url = self.get(*args, **kwargs)

        if not url.endswith("/"):
            url += "/"

        return url

    def getint(self, *args, **kwargs):
        return self.parser.getint(self.profile_name, *args, **kwargs)

    def getfloat(self, *args, **kwargs):
        return self.parser.getfloat(self.profile_name, *args, **kwargs)

    def getboolean(self, *args, **kwargs):
        return self.parser.getboolean(self.profile_name, *args, **kwargs)
