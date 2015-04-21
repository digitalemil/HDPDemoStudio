#!/bin/bash

mkdir dist
cd target
tar -cvf ../dist/HDPDemoStudio-bin-2.2.4.2-2.tar *jar
cd ../lib
tar --append --file=../dist/HDPDemoStudio-bin-2.2.4.2-2.tar install.sh start.sh json-20140107.jar apache-ant-1.9.4-bin.tar
cd ..
