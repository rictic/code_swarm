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


import os
import sys


def root_path():
    path = os.path.dirname(__file__)
    path = os.path.dirname(path)
    return path


def lib_path():
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
    platform = os.uname()[0]
    if platform == "Linux":
        return "LD_LIBRARY_PATH"
    elif platform == "Darwin":
        return "DYLD_LIBRARY_PATH"
    else:
        raise RuntimeError("Invalid OS: " + platform)


def code_swarm_dir(repo):
    if repo[0] == '.':
        repo = repo[1:]
    return os.path.join(os.getcwd(), "." + repo, ".code_swarm")


def do_cmds(cmds):
    for cmd in cmds:
        if not os.system(cmd) == 0:
            print >>sys.stderr, "Error at '%s'" % cmd
            return False
    return True


def do_git():
    ds = '-' * 72
    fmt = "%%n%s%%nr%%h | %%ae | %%ai (%%aD) | x lines%%nChanged paths: " % ds
    dir = code_swarm_dir("git")
    tmp = os.path.join(dir, "temp.log")
    xml = os.path.join(dir, "log.xml")

    cmds = []
    cmds.append('git log --name-status --pretty=format:"%s" > %s' % (fmt, tmp))
    cmds.append("convert_logs.py -g %s -o %s" % (tmp, xml))
    cmds.append("rm " + tmp)
    return do_cmds(cmds)


def do_svn():
    dir = code_swarm_dir("svn")
    tmp = os.path.join(dir, "temp.log")
    xml = os.path.join(dir, "log.xml")

    cmds = []
    cmds.append("svn log -v > " + tmp)
    cmds.append("convert_logs.py -s %s -o %s" % (tmp, xml))
    cmds.append("rm " + tmp)
    return do_cmds(cmds)


def do_hg():
    dir = code_swarm_dir("hg")
    tmp = os.path.join(dir, "unsorted_log.xml")
    xml = os.path.join(dir, "log.xml")

    cmds = []
    cmds.append("hg_log.py -o " + tmp)
    cmds.append("sort_code_swarm_input.py < %s > %s" % (tmp, xml))
    cmds.append("rm " + tmp)
    return do_cmds(cmds)


def main(argv):
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
