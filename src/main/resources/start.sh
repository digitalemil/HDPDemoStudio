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

echo Creating HBase Table: $HBASETABLE
sudo -u hbase echo create \'$HBASETABLE\',\'all\' | hbase shell


#echo Stopping Kafka
#/opt/kafka/bin/kafka-server-stop.sh
#sleep 2 
#echo Starting Kafka
#nohup /opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/server.properties >/opt/kafka/kafka.out 2>/opt/kafka/kafka.err </dev/null &
#sleep 10

echo Creating Kafka Topic: $TOPIC
/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --create --zookeeper $ZOOKEEPER --replication-factor 1 --partitions 2 --topic $TOPIC
echo Create Horton's Gym topic: 
/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --create --zookeeper $ZOOKEEPER --replication-factor 1 --partitions 2 --topic color

echo Create Horton's Gym zk path
/usr/hdp/current/zookeeper-client/bin/zkCli.sh create /hortonsgym ''
/usr/hdp/current/zookeeper-client/bin/zkCli.sh create /hortonsgym/pmml ''

#Starting Solr

sudo -u hdfs hadoop fs -mkdir /user/root
sudo -u hdfs hadoop fs -chmod 777 /user/guest /user/root

echo Creating Hive Table: $HIVETABLE
sudo -u hive echo $DDL | hive

cwd=$(pwd)

echo Starting Spark-Streaming
SPARKTOPIC=$TOPIC-spark
# Remove the following "#" in case you want to stream via Spark. Make sure you adjusted the YARN memory settings before.
/usr/hdp/current/spark-client/bin/spark-submit --class com.hortonworks.digitalemil.hdpdemostudio.Spark --master yarn-cluster --num-executors 2 --driver-memory 512m --executor-memory 512m --executor-cores 1 $cwd/SparkStreaming/target/HDPDemoStudioSparkStreaming-*-distribution.jar  $ZOOKEEPER $SPARKTOPIC $SPARKTOPIC 1 $SOLRURL $SOLRCORE $HBASETABLE $HBASECF >/tmp/spark.out 2>/tmp/spark.err &

echo Deploying Storm topology

storm jar $cwd/StormTopology/target/HDPDemoStudioStormTopology-*-distribution.jar com.hortonworks.digitalemil.hdpappstudio.storm.Topology $APPNAME $ZOOKEEPER $SOLRURL$SOLRCORE/update/json?commit=true $HBASETABLE $HBASECF $TOPIC $HIVETABLE $HBASEROOTDIR $ZOOKEEPERZNODEPARENT $FIELDS

echo IMPORTANT:
echo Please restart Ambari for your view to become visible
