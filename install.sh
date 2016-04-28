#!/bin/bash

ADMINPASSWD=admin

echo Increasing yarn nodemanager max memory to allow Spark-Streaming
/var/lib/ambari-server/resources/scripts/configs.sh set sandbox Sandbox yarn-site "yarn.scheduler.maximum-allocation-mb" "4500"

echo Restarting YARN:
curl -u admin:$ADMINPASSWD -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Stop YARN via REST"}, "Body": {"ServiceInfo": {"state": "INSTALLED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/YARN
echo
read -p "Press any key when YARN is stopped." -n1 -s

curl -u admin:$ADMINPASSWD -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start YARN via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/YARN

echo
read -p "Press any key when YARN is restarted." -n1 -s

sleep 2

SOLR_HOME=/opt/lucidworks-hdpsearch/solr
rm -fr /opt/lucidworks-hdpsearch/solr/server/solr/myCollection_shard1_replica1
rm -fr /opt/lucidworks-hdpsearch/solr/server/logs/*
chown -R solr /opt/lucidworks-hdpsearch/solr/server/logs

ambari-server restart
ambari-agent restart

wget http://digitalemil.de/hortonsgym/apache-maven-3.2.5-bin.tar.gz
tar xzf apache-maven-3.2.5-bin.tar.gz

./apache-maven-3.2.5/bin/mvn clean compile assembly:single

sleep 10
curl -u admin:$ADMINPASSWD -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/KAFKA
curl -u admin:$ADMINPASSWD -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/HBASE
curl -u admin:$ADMINPASSWD -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/STORM
curl -u admin:$ADMINPASSWD -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/SPARK

sleep 10

curl -u admin:$ADMINPASSWD -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Kafka via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/KAFKA
curl -u admin:$ADMINPASSWD -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start HBase via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/HBASE
curl -u admin:$ADMINPASSWD -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Storm via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/STORM
curl -u admin:$ADMINPASSWD -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Spark via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/SPARK


cd StormTopology
../apache-maven-3.2.5/bin/mvn clean compile package
cd ..

cd SparkStreaming
../apache-maven-3.2.5/bin/mvn clean compile assembly:single
cd ..

echo
echo $'\nIMPORTANT:\n'
echo After verifying that HDFS, YARN, Hive, HBase, Storm and Spark are running you can build your application by executing: 
echo sh ./createapp.sh YourAppname samples/your.properties JDK_HOME $SOLR_HOME
echo



