#!/bin/bash

if [[ -z "$XIMXI_CONSOLE_HOME" ]]; then
	L=`dirname $0`
	XIMIX_HOME="$L/../"
	 
fi

if [[ ! -f "$XIMIX_CONSOLE_HOME/libs/node.jar" ]]; then
	echo "Could not find libs/node.jar off XIMIX_CONSOLE_HOME ( $XIMIX_CONSOLE_HOME )"
	exit -1
fi


if [[ -z "$JAVA_HOME" ]]; then
     	echo "JAVA_HOME is not specified";  
fi


if [[ ! -z "$1" ]]; then
    MIX="$XIMIX_HOME/$1/conf/mixnet.xml"
	CONF="$XIMIX_HOME/$1/conf/config.xml"
	PIDFILE="$XIMIX_HOME/$1/$1.pid"
fi

if [[ ! -f "$MIX" ]]; then
	echo "Network config not found for $1, path was $MIX";
	exit -1
fi

if [[ ! -f "$CONF" ]]; then
	echo "Node config was not found for $1, path was $CONF";
	exit -1;
fi

$JAVA_HOME/bin/java -cp "$XIMIX_CONSOLE_HOME/libs/*" org.cryptoworkshop.ximix.console.Main $CONF $MIX "$@" &
PID=$!

echo $PID > $PIDFILE
