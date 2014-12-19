#!/bin/bash

/root/start_ambari.sh

wget ftp://ftp.halifax.rwth-aachen.de/apache/maven/maven-3/3.2.3/binaries/apache-maven-3.2.3-bin.tar.gz
tar xzf apache-maven-3.2.3-bin.tar.gz

wget http://mirror.serversupportforum.de/apache//ant/binaries/apache-ant-1.9.4-bin.tar.gz
tar xzf apache-ant-1.9.4-bin.tar.gz 


unalias cp

#sh ./install-kafka.sh

./apache-maven-3.2.3/bin/mvn clean compile assembly:single

curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start HBase via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/HBASE
curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Storm via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/STORM
curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Kafka via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/KAFKA


wget http://mirror.synyx.de/apache/tomcat/tomcat-7/v7.0.57/bin/apache-tomcat-7.0.57.tar.gz
tar xzf apache-tomcat-7.0.57.tar.gz
mv apache-tomcat-7.0.57 /opt/tomcat
cp lib/server.xml /opt/tomcat/conf

git clone https://github.com/LucidWorks/banana.git

cd StormTopology
../apache-maven-3.2.3/bin/mvn clean compile assembly:single
cd ..

echo After verifying that HBase and Storm are running you can build your application by executing: 
echo sh ./createapp.sh YourAppname samples/your.properties



