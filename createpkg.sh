#!/bin/bash

mkdir dist
cd target
tar -cvf ../dist/HDPAppStudio-bin-0.1.8.tar *jar 
cd ../lib
tar --append --file=../dist/HDPAppStudio-bin-0.1.8.tar kafka_2.9.2-0.8.1.1.tar install.sh start.sh
cd ..
