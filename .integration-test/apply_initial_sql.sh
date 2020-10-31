#!/bin/bash

# This script needs to be run in this folder (.integration-test)

echo -e "\nSetting up this folder"
if ! [ -d data ]; then
    mkdir data
fi

echo -e "\nSpeed-building backend to get a clean db"
cd ../backend
mvn clean package -Dmaven.test.skip=true

echo -e "\n\nCopying db and jar"
cp backend/target/data.db ../.integration-test/data/data.db
cp backend/target/backend.jar ../.integration-test/backend.jar

echo -e "\n\nGoing back"
cd ../.integration-test

echo -e "\n\nCleaning up if needed"
rm -f data/data.db-wal data/data.db-shm

echo -e "\n\nImporting data"
cat init_data.sql | sqlite3 data/data.db
