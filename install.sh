#!/bin/bash

/root/start_ambari.sh

wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
yum install -y apache-maven

unalias cp

sh ./install-kafka.sh

mvn clean compile assembly:single

cd StormTopology
mvn clean compile assembly:single
cd ..