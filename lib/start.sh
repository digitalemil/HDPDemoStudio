#!/bin/bash

SOLR_HOME=$1
if [ "$SOLR_HOME" == "" ]
then SOLR_HOME=/opt/lucidworks-hdpsearch/solr
fi

sudo -u solr $SOLR_HOME/bin/solr stop

ambari-server restart

ambari-agent restart

sudo -u solr $SOLR_HOME/bin/solr start
