#!/bin/sh

set -e

for POSTGRES_SCHEMA in "opendataindex" "dsp_esterra" "dsp_ileo" "ville_Armentières" "ville_Villeneuve-d\'Ascq"
do
	echo "Importing sample datasets sample-datasets.gpkg into $POSTGRES_HOST:$POSTGRES_DB, schema \"$POSTGRES_SCHEMA\", as user $POSTGRES_USER"
	export SQLITE_LIST_ALL_TABLES=YES
	ogr2ogr -f PostgreSQL \
	-lco LAUNDER=NO \
	PG:"host='$POSTGRES_HOST' active_schema='$POSTGRES_SCHEMA' user='$POSTGRES_USER' dbname='$POSTGRES_DB' password='$POSTGRES_PASSWORD'" \
	-oo LIST_ALL_TABLES=YES \
	-lco OVERWRITE=YES \
	/sample-data/sample-datasets.gpkg
done

