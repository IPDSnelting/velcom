#!/usr/bin/env python3
from http import server
from pathlib import Path
import socketserver
import sys
import os


class FallbackHandler(server.SimpleHTTPRequestHandler):
    """Custom handler to be tested"""

    def do_GET(self):
        file_path = self.path[1:]

        if Path(file_path).exists():
            return super().do_GET()

        self.path = "/index.html"
        return super().do_GET()


if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage:", sys.argv[0], "<path>", "<port>")
        sys.exit(1)

    os.chdir(sys.argv[1])
    port = int(sys.argv[2])

    with socketserver.TCPServer(("", port), FallbackHandler) as httpd:
        print("Serving at port", port)
        httpd.serve_forever()
