#!/bin/bash

export FIELDS="id location event_timestamp deviceid heartrate user "
java -cp target/classes com.hortonworks.digitalemil.hdpappstudio.GenerateData 4 ";" $FIELDS

