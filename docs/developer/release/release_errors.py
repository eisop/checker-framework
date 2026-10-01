"""The exception type shared by the release scripts.

It is kept in its own module, rather than in release_vars.py or release_utils.py, because
release_utils.py already imports from release_vars.py (execute); either of those two importing
this exception back from the other would be a circular import.
"""


class ReleaseError(Exception):
    """Raised for any failure in the release scripts.

    The release scripts are release_vars.py, release_utils.py, release_build.py, release_push.py,
    and sanity_checks.py.
    """
