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
    def lib_environ():
        platform = os.uname()[0]
        if platform == "Linux":
            return "LD_LIBRARY_PATH"
        elif platform == "Darwin":
            return "DYLD_LIBRARY_PATH"
        else:
            raise RuntimeError("Invalid OS: " + platform)
    
    old_path = os.environ.get(lib_environ(), "")
    path = os.path.join(root_path(), "lib")
    (platform, _, _, _, arch) = os.uname()
    if platform == "Linux" and arch == "x86_64":
        path_64 = os.path.join(path, "linux-x86_64")
        path = os.pathsep.join([path_64, path])
    return os.pathsep.join([path, old_path])


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


def parse_args():
    from optparse import OptionParser

    parser = OptionParser()
    parser.add_option("-r", "--reload",
                      help="Reload the XML log file", action="store_true")
    parser.add_option("-d", "--debug",
                      help="Enable Java debugging", action="store_true")

    return parser.parse_args()


def main():
    def get_jars():
        lib_dir = os.path.join(root_path(), "lib")
        lib = os.listdir(lib_dir)
        jars = []
        for f in lib:
            if f.endswith(".jar"):
                jars.append(f)
        jars = [lib_dir + os.sep + j for j in jars]
        return os.pathsep.join(jars)

    options = parse_args()[0]
    
    log = None
    dir = None
    if os.path.exists(".git"):
        dir = code_swarm_dir("git")
        log = do_git
    elif os.path.exists(".svn"):
        dir = code_swarm_dir("svn")
        log = do_svn
    elif os.path.exists(".hg"):
        dir = code_swarm_dir("hg")
        log = do_hg
    else:
        msg  = "This directory isn't an svn, git, or hg project. "
        msg += "Run in the base directory of a source-controlled project."
        print >>sys.stderr, msg

    if not os.path.exists(dir):
        os.mkdir(dir)

    if options.reload or not os.path.exists(os.path.join(dir, "log.xml")):
        log()

    params = os.path.join(dir, "project.config")
    code_swarm_jar = os.path.join(root_path(), "dist", "code_swarm.jar")

    if not os.path.exists(code_swarm_jar):
        print >>sys.stderr, "no code_swarm binaries!"
        print >>sys.stderr, "need to build with 'ant' and 'javac' (java-sdk)"
        print >>sys.stderr, ""
        print >>sys.stderr, "auto-trying the ant command..."
        build = os.path.join(root_path(), "build.xml")
        retn = os.system("ant -buildfile " + build)
        print >>sys.stderr, ""
        if not retn == 0:
            print >>sys.stderr, "ERROR, please verify 'ant' installation"
            sys.exit(2)

    cp = os.pathsep.join([code_swarm_jar, get_jars(), "."])
    ea = "-ea" if options.debug else "-da"
    args = [ "java"
           , "-classpath"
           , cp
           , "-Djava.library.path=" + lib_path()
           , ea
           , "-Xmx1000m"
           , "-server"
           , "code_swarm"
           , params
           ]
    os.execvp("java", args)


if __name__ == "__main__":
    main()
