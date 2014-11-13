#!/bin/bash
cd /opt
wget http://ftp.fau.de/apache/kafka/0.8.1.1/kafka_2.9.2-0.8.1.1.tgz
tar xzvf /root/kafka_2.9.2-0.8.1.1.tgz
ln -s kafka_2.9.2-0.8.1.1 kafka
nohup /opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/server.properties &
sleep 10
/opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 2 --topic default2
/opt/kafka/bin/kafka-server-stop.sh
