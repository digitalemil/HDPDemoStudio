#!/bin/bash
cwd=$(pwd)
mv kafka_2.9.2-0.8.1.1.tar /opt
cd /opt
tar xf kafka_2.9.2-0.8.1.1.tar
ln -s kafka_2.9.2-0.8.1.1 kafka
cd $cwd

cp HDPAppStudio*jar /var/lib/ambari-server/resources/views

/root/start_ambari.sh

sleep 120

curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start HBase via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/HBASE
curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Storm via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/STORM


