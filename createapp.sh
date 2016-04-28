#!/bin/bash

SOLR_HOME=$4

ip=$(ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p') 

virt=$(virt-what)
if [ "$virt" == "virtualbox" ]
then ip=127.0.0.1
fi

echo Using $ip for the Banana Dashboard

sudo -u solr $SOLR_HOME/bin/solr start
sudo -u solr $SOLR_HOME/bin/solr create -c hdp


java -cp target/classes:lib/json-20140107.jar com.hortonworks.digitalemil.hdpappstudio.Setup $1 $2 $3 http://$ip:8983/solr $4 sandbox:8020

#echo $'\nIMPORTANT:\n'
#echo If you plan to use Spark Streaming instead of Storm then now go to Ambari and change the node memory to a minimum of 4500MB \('Memory allocated for all YARN containers on a node'\). Leave the 'Maximum Container Size (Memory)' at 2250MB. Then restart all affected services 
#echo and remove the '#' \(uncomment\) in the front of line 57 \(search for spark-submit\) in apps/*/start.sh.
  
