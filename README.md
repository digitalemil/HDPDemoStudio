HDPAppStudio
============

Making HDP Demos easy


Start Ambari
/root/start_ambari.sh

Check that all services are running esp. HBase & Storm!

Install maven on sandbox:
wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
yum install apache-maven

Install kafka:
sh ./install-kafka.sh

Build HDPAppStudio:
mvn clean compile assembly:single

Build Storm Topology
cd StormToplogy
mvn clean compile assembly:single
cd ..

Create app with: 
java -cp target/classes com.hortonworks.digitalemil.hdpappstudio.Setup YourAppname samples/your.properties



git repo:
https://github.com/digitalemil/HDPAppStudio.git
