#!/usr/bin/env python3
"""Checks the links of the HTML pages that the Checker Framework release zip ships.

Usage: check-release-links.py [--online] UNPACKED_ZIP_DIR

UNPACKED_ZIP_DIR is the checker-framework-VERSION directory that results from unpacking the
release zip.  The pages checked are docs/checker-framework-webpage.html and
docs/checker-framework-quick-start.html.  See the comment at the top of the first one for why its
links are a mixture of relative and absolute.

Offline checks (always run; no network):
 * a relative link must name a file or directory that the zip contains, and a fragment must name an
   anchor of that file;
 * an absolute path must be one of the site singletons below, and the zip download must be this
   release's;
 * an external (http/https) link must be well-formed.
With --online, each external link is also fetched.  That is flaky, so it is not for pull requests;
the release build uses it.
"""

import argparse
import re
import sys
import urllib.error
import urllib.request
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urldefrag, urlparse

PAGES = ["docs/checker-framework-webpage.html", "docs/checker-framework-quick-start.html"]

# Relative targets that the website generator produces rather than the zip carrying them, mapped
# to the zip file the generator produces them from.  A link to the key resolves on the website.
GENERATED = {
    "quick-start.html": "docs/checker-framework-quick-start.html",
    "api/checker-javadoc/": "checker/dist/checker-javadoc.jar",
    "CFLogo.png": "docs/tutorial/CFLogo.png",
}

# Absolute paths: site singletons, which exist only on the website, not in the zip.
SITE_SINGLETONS = {"/cf/releases/releases.html", "/afu/annotation-file-utilities.html"}


class LinkParser(HTMLParser):
    """Collects the link targets and the anchors of a page."""

    def __init__(self) -> None:
        super().__init__()
        self.links: list[str] = []
        self.anchors: set[str] = set()

    def handle_starttag(self, tag, attrs):
        for name, value in attrs:
            if value is None:
                continue
            if name in ("href", "src") and tag in ("a", "link", "img", "script", "area"):
                self.links.append(value)
            elif name == "id" or (name == "name" and tag == "a"):
                self.anchors.add(value)


def parse(path: Path) -> LinkParser:
    parser = LinkParser()
    parser.feed(path.read_text(encoding="utf-8", errors="replace"))
    return parser


def check_external(url: str, online: bool, errors: list[str], where: str) -> None:
    parts = urlparse(url)
    if not parts.netloc or " " in url:
        errors.append(f"{where}: malformed URL {url}")
        return
    if not online:
        return
    request_url = urldefrag(url)[0]
    last = None
    for method in ("HEAD", "GET"):
        req = urllib.request.Request(
            request_url, method=method, headers={"User-Agent": "Mozilla/5.0 check-release-links"}
        )
        try:
            with urllib.request.urlopen(req, timeout=30):
                return
        except urllib.error.HTTPError as e:
            last = f"HTTP {e.code}"
        except (urllib.error.URLError, OSError) as e:
            last = str(e)
    errors.append(f"{where}: cannot fetch {url}: {last}")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--online", action="store_true", help="also fetch the external links")
    ap.add_argument("root", type=Path, help="the unpacked checker-framework-VERSION directory")
    args = ap.parse_args()
    root: Path = args.root.resolve()
    version = root.name.removeprefix("checker-framework-")
    zip_link = f"/cf/checker-framework-{version}.zip"
    errors: list[str] = []
    parsed: dict[Path, LinkParser] = {}

    def anchors_of(path: Path) -> set[str]:
        if path not in parsed:
            parsed[path] = parse(path)
        return parsed[path].anchors

    for page in PAGES:
        page_path = root / page
        if not page_path.is_file():
            errors.append(f"{page}: not in the release")
            continue
        anchors_of(page_path)
        links = parsed[page_path].links
        if not links:
            errors.append(f"{page}: no links found; is the page parsed correctly?")
        for link in links:
            where = f"{page}: {link}"
            if re.match(r"(mailto|javascript):", link):
                continue
            if re.match(r"https?://", link):
                check_external(link, args.online, errors, page)
                continue
            if re.match(r"[a-z][a-z0-9+.-]*:", link, re.I) or link.startswith("//"):
                errors.append(f"{where}: unexpected URL scheme")
                continue
            target, fragment = urldefrag(link)
            target = unquote(target)
            if target.startswith("/"):
                if target in SITE_SINGLETONS or target == zip_link:
                    continue
                if re.fullmatch(r"/cf/checker-framework-.*\.zip", target):
                    errors.append(f"{where}: this release is {version}, so expected {zip_link}")
                else:
                    errors.append(f"{where}: absolute path that is not a known site singleton")
                continue
            if target == "":
                resolved = page_path
            elif target in GENERATED:
                resolved = root / GENERATED[target]
            else:
                resolved = (page_path.parent / target).resolve()
                if root not in resolved.parents and resolved != root:
                    errors.append(f"{where}: points outside the release")
                    continue
                if target.endswith("/"):
                    resolved = resolved / "index.html"
            if not resolved.is_file():
                errors.append(f"{where}: {resolved.relative_to(root)} is not in the release")
                continue
            if fragment and resolved.suffix == ".html":
                if fragment not in anchors_of(resolved):
                    errors.append(
                        f"{where}: no anchor '{fragment}' in {resolved.relative_to(root)}"
                    )

    for e in errors:
        print(f"check-release-links: {e}", file=sys.stderr)
    if errors:
        print(f"check-release-links: {len(errors)} problem(s).", file=sys.stderr)
        return 1
    print("check-release-links: all links OK.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
