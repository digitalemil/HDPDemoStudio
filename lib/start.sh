#!/bin/bash

ambari-server restart

sleep 10

ambari-agent restart

echo Stopping Kafka
/opt/kafka/bin/kafka-server-stop.sh

sleep 2 

echo Starting Kafka
nohup /opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/server.properties >/opt/kafka/kafka.out 2>/opt/kafka/kafka.err </dev/null &

sleep 10

cwd=$(pwd)
cd /opt/solr/solr/hdp
echo Stoping Solr
java -DSTOP.KEY=secret -DSTOP.PORT=8983 -jar start.jar --stop

sleep 5

echo Starting Solr
nohup java -DSTOP.KEY=secret -jar start.jar >solr.out 2>solr.err </dev/null &
cd $cwd


