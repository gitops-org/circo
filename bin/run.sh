#!/bin/bash

#
#  Copyright (c) 2012, the authors.
#
#    This file is part of 'Circo'.
#
#    Circo is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    Circo is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with Circo.  If not, see <http://www.gnu.org/licenses/>.
#
 
#set -e
#set -u
#set -o errexit

# the application 'base' folder 
bin_dir=`dirname "$0"`
bin_dir=`cd "$bin_dir"; pwd`
base_dir=`dirname $bin_dir`

# define the java env
java=java
if test -n "$JAVA_HOME"; then
	java="$JAVA_HOME/bin/java"
fi

#
# Variable definition
#
declare -a args=()
DEBUG=''
MAIN_CLASS=${MAIN_CLASS:-$1}
JVM_ARGS+=" -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true -Dsigar.lib.path=$base_dir/libsigar"

#
# classpath when the application is compiled with gradle 
#
if [ -e "$base_dir/build/classes/main" ]; then
  CLASSPATH="$base_dir/build/classes/main"
  CLASSPATH+=":$base_dir/build/classes/test"
  CLASSPATH+=":$base_dir/build/resources/main"
  for file in $base_dir/build/dependency-libs/*.jar; do
    CLASSPATH+=":$file";
  done

#
# deployed application class -- only jar in the libs folder
#
elif [ -e $base_dir/libs ]; then
  CLASSPATH="$base_dir/conf"
  for file in $base_dir/libs/*.jar; do
    CLASSPATH+=":$file";
  done

else
  echo "Missing application libraries -- Circo cannot start"
  exit 1
fi  


#
# Handle special program cli options 
#
while [ "$*" != "" ]; do
  if [[ "$1" == '--debug' || "$1" == '--trace' ]]; then
    args+=("$1")

  elif [ "$1" == '--with-jrebel' ]; then
    if [ "$JREBEL_HOME" ]; then
    JVM_ARGS+=" -javaagent:$JREBEL_HOME/jrebel.jar -Drebel.log.file=./jrebel-client.log"
    else
    echo "WARN: To use JReber define the JREBEL_HOME variable in environment"
    fi

  elif [ "$1" == '--client' ]; then
    MAIN_CLASS='circo.client.ClientApp'

  elif [ "$1" == '--daemon' ]; then
    MAIN_CLASS='circo.ClusterDaemon'

  elif [ "$1" == '--remote-debugger' ]; then
    DEBUG='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8010'

  else
   args+=("$1")
  fi 
  # move to the next option
  shift
done

# Show some variable when in DEBUG mode
if [ "$DEBUG" != '' ]; then
  echo Launch environment
  echo ------------------
  echo base_dir: $base_dir
  echo jvmargs: $JVM_ARGS
  echo debug: $DEBUG
  echo classpath:
  echo $CLASSPATH | tr ":" "\n" | sort
  echo ''
  echo Launching it!
  echo ------------------
fi

# Launch the APP
exec java $JVM_ARGS $DEBUG -cp "$CLASSPATH" "$MAIN_CLASS" "${args[@]}"