#!/bin/bash

export HDPAPPSTUDIO_HOME=/root/HDPAppStudio

export FIELDS=id location
export APPNAME=hdp
export SOLRCORE=hdp
export HBASETABLE=HDP
export TOPIC=default
export HIVETABLE=hdp
export DDL=Create Table bar \(id BigInt, foo String\)\;

echo Starting...

echo Restarting Ambari-Server
ambari-server restart

echo Restarting Ambari-Agent
ambari-agent restart

echo Creating HBase Table: $HBASETABLE
sudo -u hbase echo create \'$HBASETABLE\',\'all\' | hbase shell

echo Starting Kafka
nohup /opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/server.properties &
sleep 10

echo Creating Kafka Topic: $TOPIC
/opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 2 --topic $TOPIC

cwd=$(pwd)
cd /opt/solr/solr/hdp
echo Stoping Solr
java -DSTOP.KEY=secret -DSTOP.PORT=8983 -jar start.jar --stop
echo Starting Solr
nohup java -DSTOP.KEY=secret -jar start.jar >solr.out 2>solr.err </dev/null &
cd $cwd

echo Creating Hive Table: $HIVETABLE

sudo -u hdfs hadoop fs -mkdir -p /user/guest/hdpappstudio/+$HBASETABLE

sudo -u hive echo $DDL | hive

echo Deploying Storm topology
storm jar $HDPAPPSTUDIO_HOME/StormTopology/target/HDPAppStudioStormTopology-0.1.1-distribution.jar com.hortonworks.digitalemil.hdpappstudio.storm.Topology $APPNAME 127.0.0.1:2181 http://127.0.0.1:8983/solr/$SOLRCORE/update/json?commit=true $HBASETABLE all $TOPIC $HIVETABLE $FIELDS

echo Execute 
echo tail -f /var/log/ambari-server/ambari-server.log
echo and wait until you see your Ambari View being deployed \(might take a couple of minutes\). Then go to Ambari Web: http://127.0.0.1:8080/#/main/views 

