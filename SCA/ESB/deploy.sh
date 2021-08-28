#!/bin/bash
#DAS_HOST=`grep j2ee.server.instance ./nbproject/private/private.properties|cut -d":" -f5`
DAS_HOST="localhost"
echo "Server IP Address: $DAS_HOST"

USER=admin
PASS=3jiGt7a2Dfs
echo OE_ADMIN_PASSWORD=$PASS > /tmp/apV2.txt 
PORT=4848
SA_NAME=ESB
JAR=./dist/ESB.zip
AS=/opt/openesb/bin/oeadmin.sh

JBI_SHUTDOWN_CMD="$AS shut-down-jbi-service-assembly --user $USER --host $DAS_HOST --passwordfile /tmp/apV2.txt --port $PORT"
JBI_UNDEPLOY_CMD="$AS undeploy-jbi-service-assembly --user $USER --host $DAS_HOST --passwordfile /tmp/apV2.txt  --port $PORT"
JBI_START_CMD="$AS start-jbi-service-assembly --user $USER --host $DAS_HOST --passwordfile /tmp/apV2.txt --port $PORT"
JBI_DEPLOY_CMD="$AS deploy-jbi-service-assembly --user $USER --host $DAS_HOST --passwordfile /tmp/apV2.txt --port $PORT"

$JBI_SHUTDOWN_CMD $SA_NAME 
$JBI_UNDEPLOY_CMD $SA_NAME 
$JBI_DEPLOY_CMD $JAR
$JBI_START_CMD $SA_NAME
