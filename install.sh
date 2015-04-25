#!/bin/bash

/root/start_ambari.sh

wget http://artfiles.org/apache.org/maven/maven-3/3.2.5/binaries/apache-maven-3.2.5-bin.tar.gz
tar xzf apache-maven-3.2.5-bin.tar.gz

wget http://mirror.serversupportforum.de/apache//ant/binaries/apache-ant-1.9.4-bin.tar.gz
tar xzf apache-ant-1.9.4-bin.tar.gz 

yum install -y npm

git clone https://github.com/apache/incubator-zeppelin

./apache-maven-3.2.5/bin/mvn clean compile assembly:single

sleep 10
curl -u admin:admin -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/KAFKA
curl -u admin:admin -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/HBASE
curl -u admin:admin -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/STORM
curl -u admin:admin -i -H 'X-Requested-By: ambari' -X PUT -d '{"RequestInfo":{"context":"Turn Off Maintenance Mode"},"Body":{"ServiceInfo":{"maintenance_state":"OFF"}}}'  http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/SPARK

sleep 10

curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Kafka via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/KAFKA
curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start HBase via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/HBASE
curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Storm via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/STORM
curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d '{"RequestInfo": {"context" :"Start Spark via REST"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}' http://127.0.0.1:8080/api/v1/clusters/Sandbox/services/SPARK

cd incubator-zeppelin
#../apache-maven-3.2.5/bin/mvn clean install -Dhadoop.version=2.6.0 -Dspark.version=1.2.1
../apache-maven-3.2.5/bin/mvn clean install -DskipTests -Pspark-1.2 -Phadoop-2.4 -Dhadoop.version=2.6.0 -Pyarn
cd ..
cp src/main/resources/zeppelin-env.sh incubator-zeppelin/conf/
./incubator-zeppelin/bin/zeppelin-daemon.sh start &

#wget http://ftp-stud.hs-esslingen.de/pub/Mirrors/ftp.apache.org/dist/tomcat/tomcat-7/v7.0.59/bin/apache-tomcat-7.0.59.tar.gz
#tar xzf apache-tomcat-7.0.59.tar.gz
#mv apache-tomcat-7.0.59 /opt/tomcat
#cp lib/server.xml /opt/tomcat/conf

git clone https://github.com/LucidWorks/banana.git

cd StormTopology
../apache-maven-3.2.5/bin/mvn clean compile assembly:single
cd ..

cd SparkStreaming
../apache-maven-3.2.5/bin/mvn clean compile assembly:single
cd ..

echo IMPORTANT
echo In Ambari go to YARN->Configs and set yarn.nodemanager.resource.memory-mb to 4096. Restart YARN

echo After verifying that HBase and Storm are running you can build your application by executing: 
echo sh ./createapp.sh YourAppname samples/your.properties



