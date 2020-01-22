#!/bin/sh

nginx &
java -jar /home/velcom/velcom.jar server "$1"
