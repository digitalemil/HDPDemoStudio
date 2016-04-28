#!/bin/bash

ADMINPASSWD=admin

cwd=$(pwd)

cp HDPDemoStudio*jar /var/lib/ambari-server/resources/views

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

echo Restarting Ambari so the HDPDemoStudio View becomes available
ambari-server restart
ambari-agent restart

echo Creating Solr core
SOLR_HOME=$1
if [ "$SOLR_HOME" == "" ]
then SOLR_HOME=/opt/lucidworks-hdpsearch/solr
fi
rm -fr /opt/lucidworks-hdpsearch/solr/server/solr/myCollection_shard1_replica1
rm -fr /opt/lucidworks-hdpsearch/solr/server/logs/*
chown -R solr /opt/lucidworks-hdpsearch/solr/server/logs

sudo -u solr $SOLR_HOME/bin/solr start
sudo -u solr $SOLR_HOME/bin/solr create -c hdp

echo Waiting until Ambari is fully up
sleep 30

cwd=$(pwd)

cp json-20140107.jar /var/lib/ambari-server/resources/views/work/HDPDemoStudio\{2.4.0\}/
cd /var/lib/ambari-server/resources/views/work/HDPDemoStudio\{2.4.0\}/
cd ${pwd}

echo Starting necessary services
curl -u admin:$ADMINPASSWD -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/KAFKA
curl -u admin:$ADMINPASSWD -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/HBASE
curl -u admin:$ADMINPASSWD -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/STORM
curl -u admin:$ADMINPASSWD -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/SPARK

sleep 10

curl -u admin:$ADMINPASSWD -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Kafka via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/KAFKA
curl -u admin:$ADMINPASSWD -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start HBase via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/HBASE
curl -u admin:$ADMINPASSWD -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Storm via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/STORM
curl -u admin:$ADMINPASSWD -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Spark via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/SPARK

echo
echo Please go to Ambari and checkout HDP Demo Studio under Views. Before creating an app please make sure HDFS, YARN, Hive, HBase, Kafka, Solr, Storm and Spark are all up and running.
