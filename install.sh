#!/bin/bash

/root/start_ambari.sh

wget ftp://ftp.halifax.rwth-aachen.de/apache/maven/maven-3/3.2.3/binaries/apache-maven-3.2.3-bin.tar.gz
tar xzvf apache-maven-3.2.3-bin.tar.gz

unalias cp

sh ./install-kafka.sh

./apache-maven-3.2.3/bin/mvn clean compile assembly:single

curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start HBase via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/HBASE
curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Storm via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/STORM

cd StormTopology
../apache-maven-3.2.3/bin/mvn clean compile assembly:single
cd ..

echo After verifying that HBase and Storm are running you can build your application by executing:
echo java -cp target/classes com.hortonworks.digitalemil.hdpappstudio.Setup YourAppname samples/your.properties



