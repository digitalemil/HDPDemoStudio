#!/bin/bash
cwd=$(pwd)

cp HDPDemoStudio*jar /var/lib/ambari-server/resources/views

ambari-server restart
ambari-agent restart

SOLR_HOME=$1
if [ "$SOLR_HOME" == "" ]
then SOLR_HOME=/opt/lucidworks-hdpsearch/solr
fi

$SOLR_HOME/bin/solr start
$SOLR_HOME/bin/solr create -c hdp

echo Waiting until Ambari is fully up
sleep 120

cwd=$(pwd)

cp json-20140107.jar /var/lib/ambari-server/resources/views/work/HDPDemoStudio\{2.3.0\}/
cd /var/lib/ambari-server/resources/views/work/HDPDemoStudio\{2.3.0\}/
cd ${pwd}

curl -u admin:admin -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/KAFKA
curl -u admin:admin -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/HBASE
curl -u admin:admin -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/STORM
curl -u admin:admin -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/SPARK


sleep 10

curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Kafka via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/KAFKA
curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start HBase via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/HBASE
curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Storm via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/STORM
curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Spark via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/SPARK


echo Please go to Ambari and checkout HDP Demo Studio under Views. Before creating an app please make sure Hive, HBase, Kafka, Solr and Storm are all up and running.
