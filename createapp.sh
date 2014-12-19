#!/bin/bash

ip=$(ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p') 


java -cp target/classes:lib/json-20140107.jar com.hortonworks.digitalemil.hdpappstudio.Setup $1 $2 127.0.0.1
