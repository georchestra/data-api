#!/bin/bash

set -e

# Load sample data PostGIS into $POSTGRES_DB
for csv in `ls /sample-data/*csv`
do
	echo "Importing sample dataset $csv into database $POSTGRES_DB as user $POSTGRES_USER"
	ogr2ogr -f PostgreSQL PG:"host='localhost' active_schema='opendataindex' user='$POSTGRES_USER' dbname='$POSTGRES_DB' password='$POSTGRES_PASSWORD'" \
	$csv -oo AUTODETECT_TYPE=YES \
	-oo X_POSSIBLE_NAMES=Longitude,LON \
	-oo Y_POSSIBLE_NAMES=Latitude,LAT \
	-oo KEEP_GEOM_COLUMNS=NO
done

