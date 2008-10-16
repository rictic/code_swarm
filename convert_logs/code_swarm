#!/usr/bin/env ruby

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


require 'optparse'


def root_path
    path = File.expand_path $0
    path = `dirname #{path}`.strip
    path = `dirname #{path}`.strip
    path
end

def lib_path
    old_path = ENV[lib_environ]
    path = File.join root_path, "lib"
    if `uname -sp`.strip == "Linux x86_64"
        path_64 = File.join path, "linux-x86_64"
        path = "#{path_64}:#{path}"
    end
    "#{path}:#{old_path}"
end

def lib_environ
    case `uname -s`.strip
    when "Linux"  then "LD_LIBRARY_PATH"
    when "Darwin" then "DYLD_LIBRARY_PATH"
    else          raise "Invalid OS: #{`uname -s`.strip}"
    end
end

def code_swarm_dir(repo)
    cwd = `pwd`.strip
    "#{cwd}/.#{repo}/.code_swarm"
end

def do_git
    fmt = "%n#{'-' * 72}%nr%h | %ae | %ai (%aD) | x lines%nChanged paths:"
    dir = code_swarm_dir "git"
    `git log --name-status --pretty=format:"#{fmt}" > #{dir}/temp.log`
    `convert_logs.py -g #{dir}/temp.log -o #{dir}/log.xml`
    `rm #{dir}/temp.log`
end

def do_svn
    dir = code_swarm_dir "svn"
    `svn log -v > #{dir}/temp.log`
    `convert_logs.py -s #{dir}/temp.log -o #{dir}/log.xml`
    `rm #{dir}/temp.log`
end

def do_hg
    dir = code_swarm_dir "hg"
    `hg_log.py -o #{dir}/unsorted_log.xml`
    `sort_code_swarm_input.py < #{dir}/unsorted_log.xml > #{dir}/log.xml`
    `rm #{dir}/unsorted_log.xml`
end


# Parse arguments
options = {}
OptionParser.new do |opts|
    opts.banner = "Usage: code_swarm [options]"

    opts.on("-r", "--reload", "Reload the XML log file") do |r|
        options[:reload] = true
    end

    opts.on("-d", "--debug", "Enable Java debugging") do |d|
        options[:debug] = true
    end
end.parse!

# Set up paths for dynamically-loaded libraries
ENV[lib_environ] = lib_path

# Set up log function
log = nil
dir = nil
if File.exists? ".git"
    dir = code_swarm_dir "git"
    log = do_git
elsif File.exists? ".svn"
    dir = code_swarm_dir "svn"
    log = do_svn
elsif File.exists? ".hg"
    dir = code_swarm_dir "hg"
    log = do_hg
else
    $stderr.print "This directory isn't an svn, git, or hg project. "
    $stderr.puts  "Run in the base directory of a source-controlled project."
    exit 2
end

# Configure the project environment and launch
`mkdir -p #{dir}`

log() if options[:reload] or not File.exists? "#{dir}/log.xml"

if not File.exists? "#{dir}/project.config"
    puts "creating default config file at #{dir}/project.config"
    cp_dir = "#{root_path}/convert_logs"
    `cp #{cp_dir}/config.template #{dir}/project.config`
end

# Launch the JAR. This is taken from the original run.sh script.
params = "#{dir}/project.config"
code_swarm_jar = "#{root_path}/dist/code_swarm.jar"

if not File.exists? code_swarm_jar
    $stderr.puts "no code_swarm binaries!"
    $stderr.puts "need to build it with 'ant' and 'javac' (java-sdk)"
    $stderr.puts
    $stderr.puts "auto-trying the ant command..."
    system "ant -buildfile #{root_path}/build.xml"
    $stderr.puts
    unless $?.to_i == 0
        $stderr.puts "ERROR, please verify 'ant' and 'java-sdk' installation"
        exit 2
    end
end

jars = `ls #{root_path}/lib/*.jar | tr '\n' ':'`
classpath = "-classpath #{code_swarm_jar}:#{jars}:."

ea = options[:debug] ? "-ea" : ""

exec "java #{ea} -Xmx1000m -server #{classpath} code_swarm #{params}"
