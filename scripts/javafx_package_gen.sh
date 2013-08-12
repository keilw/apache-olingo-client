#!/bin/bash
ROOT_DIR=./

if [ ! -z $1 ]; then
	ROOT_DIR=$1
fi

APP_CLASS=org.apache.olingo.odata2.client.MainApp
SRC_DIR=$ROOT_DIR/target/classes
OUT_DIR=$ROOT_DIR/target/
OUT_FILE=olingo-client-app

echo "Create from root dir '$ROOT_DIR' into '$OUT_DIR' with name '$OUT_FILE'"

${JAVA_HOME}/bin/javafxpackager -createjar -nocss2bin -appclass $APP_CLASS -srcdir $SRC_DIR -outdir $OUT_DIR -outfile $OUT_FILE Javafxpackager
