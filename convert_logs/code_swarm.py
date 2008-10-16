#!/usr/bin/env python

# Copyright (C) 2008 Michael Dippery <mpd@cs.wm.edu>
#
# This file is part of code_swarm.
#
# code_swarm is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# code_swarm is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with code_swarm. If not, see <http://www.gnu.org/licenses/>.


def root_path():
    import os
    path = os.path.dirname(__file__)
    path = os.path.dirname(path)
    return path


def lib_path():
    import os
    try:
        old_path = os.environ[lib_environ()]
    except KeyError:
        old_path = ""
    path = os.path.join(root_path(), "lib")
    (platform, _, _, _, arch) = os.uname()
    if platform == "Linux" and arch == "x86_64":
        path_64 = os.path.join(path, "linux-x86_64")
        path = "%s:%s" % (path_64, path)
    return "%s:%s" % (path, old_path)


def lib_environ():
    import os
    (platform, _, _, _, _) = os.uname()
    if platform == "Linux":
        return "LD_LIBRARY_PATH"
    elif platform == "Darwin":
        return "DYLD_LIBRARY_PATH"
    else:
        raise RuntimeError("Invalid OS: " + platform)


def code_swarm_dir(repo):
    import os
    return os.path.join(os.getcwd(), "." + repo, ".code_swarm")


def do_git():
    pass


def do_svn():
    pass


def do_hg():
    pass


def main(argv):
    return 0


if __name__ == "__main__":
    import sys
    sys.exit(main(sys.argv))
