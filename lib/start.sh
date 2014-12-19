#!/bin/bash

ambari-server restart

echo Starting Solr
cd /opt/solr/solr/hdp
nohup java -DSTOP.KEY=secret -jar start.jar >solr.out 2>solr.err </dev/null &
cd $cwd
