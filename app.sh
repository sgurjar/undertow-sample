#!/bin/bash

basedir=$(cd $(dirname $0) && pwd)
libdir=$basedir/lib
javadir=/opt/app/java

start() {
 $javadir/bin/java -classpath $libdir/\* testing.SampleApi
 PID=$!
 echo started $PID
}

con() {
 /usr/sbin/lsof -i -a -Pnp $(pgrep -f testing.SampleApi)
}

client() {
 unset http_proxy https_proxy
 apiurl='http://your.real.hostname.com:8080/api/v1'
 #curl='curl -i -v'
 curl='curl -i'
 case $1 in
   list) $curl $apiurl/list ;;
   get) $curl $apiurl/get/utc ;;
   convert) echo '{"from":"EST", "to":"IST"}' | $curl -d@- $apiurl/get ;;
   *) echo "error: missing or unknow api name: $1" ;;
 esac
}

case $1 in
  s|start) start ;;
  x|con) con ;;
  t|test) shift ; client "$@" ;;
  *) echo "usage: $(basename $0) s[tart]|x|con|test" ;;
esac
