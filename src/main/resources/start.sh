#!/bin/bash

export APPNAME=hdp
export FIELDS=id location
export DDL=Create Table bar \(id BigInt, foo String\)\;

export SOLRURL=http://127.0.0.1:8983/solr/
export SOLRCORE=hdp
export HBASETABLE=HDP
export HBASECF=all
export HBASEROOTDIR=hdfs://127.0.0.1:8020/apps/hbase/data/
export ZOOKEEPERZNODEPARENT=/hbase-unsecure
export ZOOKEEPER=127.0.0.1:2181
export TOPIC=default
export BROKERLIST=127.0.0.1:9092
export HIVETABLE=hdp

echo Starting...

#echo Restarting Ambari-Server
#ambari-server restart

#echo Restarting Ambari-Agent
#ambari-agent restart

echo Creating HBase Table: $HBASETABLE
sudo -u hbase echo create \'$HBASETABLE\',\'all\' | hbase shell


#echo Stopping Kafka
#/opt/kafka/bin/kafka-server-stop.sh
#sleep 2 
#echo Starting Kafka
#nohup /opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/server.properties >/opt/kafka/kafka.out 2>/opt/kafka/kafka.err </dev/null &
#sleep 10

echo Creating Kafka Topic: $TOPIC
/usr/hdp/2.2.4.2-2/kafka/bin/kafka-topics.sh --create --zookeeper $ZOOKEEPER --replication-factor 1 --partitions 2 --topic $TOPIC
echo Create Horton's Gym topic: 
/usr/hdp/2.2.4.2-2/kafka/bin/kafka-topics.sh --create --zookeeper $ZOOKEEPER --replication-factor 1 --partitions 2 --topic color

echo Create Horton's Gym zk path
zkCli.sh create /hortonsgym ''
zkCli.sh create /hortonsgym/pmml ''

cd banana
: ${JAVA_HOME:=/usr/lib/jvm/java-1.7.0-openjdk.x86_64}
export JAVA_HOME

../apache-ant-1.9.4/bin/ant
cp build/banana-*.war /opt/solr/solr/hdp/webapps/banana.war
cp jetty-contexts/banana-context.xml /opt/solr/solr/hdp/contexts
cd ..

cwd=$(pwd)
cd /opt/solr/solr/hdp
#echo Stoping Solr
#java -DSTOP.KEY=secret -DSTOP.PORT=8983 -jar start.jar --stop
#sleep 5

echo Starting Solr
nohup java -DSTOP.KEY=secret -jar start.jar >solr.out 2>solr.err </dev/null &
cd $cwd

echo Creating Hive Table: $HIVETABLE
sudo -u hive echo $DDL | hive

echo Deploying Storm topology
cwd=$(pwd)
storm jar $cwd/StormTopology/target/HDPDemoStudioStormTopology-*-distribution.jar com.hortonworks.digitalemil.hdpappstudio.storm.Topology $APPNAME $ZOOKEEPER $SOLRURL$SOLRCORE/update/json?commit=true $HBASETABLE $HBASECF $TOPIC $HIVETABLE $HBASEROOTDIR $ZOOKEEPERZNODEPARENT $FIELDS

echo Execute 
echo tail -f /var/log/ambari-server/ambari-server.log
echo and wait until you see your Ambari View being deployed \(might take a couple of minutes\). Then go to Ambari Web: http://127.0.0.1:8080/#/main/views 

