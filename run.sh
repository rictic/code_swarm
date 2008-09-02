# run.sh : code_swarm launching script
# need the config file as first parameter

params=$@
default_config="data/sample.config"
code_swarm_jar="dist/code_swarm.jar"

# command line parameters basic check
if [ $# = 0 ]; then
    # asking user for a config file
    echo "code_swarm project !"
    echo -n "Specify a config file, or ENTER for default one [$default_config] : "
    read config
    if [ ${#config} = 0 ]; then
        params=$default_config
    else
        params=$config
    fi
else
    if [ $1 == "-h" ] || [ $1 == "--help" ]; then
        # if help needed, print it and exit
        echo "usage: run.sh <configfile>"
        echo ""
        echo "   data/sample.config  is the default config file"
        echo ""
        exit 1
    fi
fi

# checking for code_swarm java binaries
if [ ! -f $code_swarm_jar ]; then
    echo "no code_swarm binaries !"
    echo "needing to build it with 'ant' and 'javac' (java-sdk)"
    echo ""
    echo "auto-trying the ant command..."
    if ant; then
        echo ""
    else
        echo ""
        echo "ERROR, please verify 'ant' and 'java-sdk' installation"
        exit 2
    fi
fi

# running
java -Xmx1000m -server -classpath dist/code_swarm.jar:`ls lib/*.jar | tr '\n' ':'`:. code_swarm $params


