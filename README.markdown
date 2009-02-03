code_swarm is an experiment in organic software visualization.

See http://vis.cs.ucdavis.edu/~ogawa/codeswarm for a picture of what we want
to produce.

Google Code Project :      http://code.google.com/p/codeswarm/
Google Group/Mailing List: http://groups.google.com/group/codeswarm 


# Building #

## Prerequisites ##

You will need Apache's "ant" build tool, at least version 5 of the Java SDK from Sun, and for some features, Python version 2.4 or later.  Linux users: we've been unable to get code_swarm to run under the GNU jvm, Sun's jvm is strongly recommended. 


### Linux ###

These instructions were tested with Ubuntu 8.04, the process is probably
similar to other linux distributions.

This should ensure that ant and java6 are installed and configured:

    sudo apt-get install ant 
    sudo apt-get install sun-java6-jdk 
    sudo update-java-alternatives -s java-6-sun 

You should also ensure that Python is installed.

### Windows ###

* download ant for all platforms at http://ant.apache.org/bindownload.cgi
* unpack it where you want it to be installed, and add the location of
  its binaries to the the `PATH` environment variable. For instance, add at the end : 
  `C:\apache-ant-1.7.0\bin;` 

* download Sun Java SDK at http://java.sun.com/javase/downloads/index.jsp
* install it and add the "javac" Java compiler to the PATH : 
  `C:\Program Files\Java\jdk1.6.0_06\bin;`

* then create a new environment variable called JAVA_HOME and set its path to 
  `C:\Program Files\Java\jdk1.6.0_06;`

* download and install Python from http://www.python.org/download/

### Mac ###

Recent versions of OS X come with a good java compiler and Python interpreter, so all you need to do is get and install Apache ant.

If you have DarwinPorts/MacPorts, you can simply:
    sudo port install apache-ant

## Getting the source code ##

### git ###

A git fork of the main code_swarm repository is maintained at <http://github.com/rictic/code_swarm/tree/master>

This fork contains a number of patches which improve the speed of `code_swarm` significantly, as well as the `code_swarm` executable, which makes visualizing a new project on Linux and the Mac a one-step process.  

To obtain a clone of the repository, simply use:

    git clone git://github.com/rictic/code_swarm.git
    

### Subversion ###

code_swarm's source code is on a Google Code Subversion repository (svn) :
http://codeswarm.googlecode.com/svn/trunk/

See Subversion homepage at http://subversion.tigris.org/ for all appropriate 
tools and documents. I would recommend [TortoiseSVN](http://tortoisesvn.tigris.org/) for Windows users. 
Unix-like users would get their native "Subversion" package.

For more information, see http://code.google.com/p/codeswarm/source/checkout


## Running code_swarm ##

With Java and ant installed, and the code_swarm source downloaded, running it on a git, svn, or hg based project is easy:

* Add `code_swarm/bin` to your PATH.  A line like `export PATH=$PATH:/path/to/code_swarm/bin` in your `~/.profile` or `~/.bash_profile` should do it
* `cd project/to/visualize`
* `code_swarm`


## Other ways of running code_swarm ##

There are a couple of other ways of invoking code_swarm.  For an experimental GUI where you can specify a svn url to visualize:

* cd path/to/code_swarm
* ant run

While code_swarm was developed to visualize source code repositories, its input format is generic, and some have experimented with visualizing other collaborative environments, including user activity on wikis and freebase.com.

code_swarm can be invoked by pointing it at a project config file, which contains a number of options for customizing the visualization.  The config file must point at a repository xml file, which contains a set of events, which each describe a file, edited by a person, at a specific time.  The time is specified as the number of milliseconds since January 1st, 1970.

Example config files can be seen in `data/sample.config` and `bin/config.template`

To invoke `code_swarm` with a given config file, use `./run.sh path/to/project.config`


Other commands:

* `ant` will build, but not run the project
* `ant all` will also generate Javadoc HTML documentation
* `ant clean` will delete all intermediate and binary files 
