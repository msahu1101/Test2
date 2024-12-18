#!/bin/bash
# This deploys artifact created during package phase into elatic beanstalk environment
  cd ~/project

# Setting Label name and Tagging in GIT for production branch
if [[ $CIRCLE_BRANCH == "preprod" ]] || [[ $CIRCLE_BRANCH == "prod" ]] 
then
# Set EB Artifact Label Name
chmod +x ./circleci-scripts/tagging-and-label-name.sh
source ./circleci-scripts/tagging-and-label-name.sh
else
    datevar=$(date +%H:%M:%S:%N)
    echo 'export EB_LABEL_NAME=release-$datevar' >> $BASH_ENV
    source $BASH_ENV
fi  

for i in $(echo $CI_EB_ENVIRONMENT_NAME | sed "s/|/ /g")
do

var=$(echo $i | awk -F":" '{print $1,$2,$3,$4,$5}')   
set -- $var

if [ $1 == $CIRCLE_BRANCH ]; then
echo " Found a Match -  EB Environment Name: $2 and Spring Profile: $3" 
eb use $2 --profile=eb-roombooking-profle
eb setenv SERVER_PORT=5000 SPRING_PROFILES_ACTIVE=$3 --profile=eb-roombooking-profle
eb deploy $2 --profile=eb-roombooking-profle --label $EB_LABEL_NAME --timeout $CI_EB_DEPLOY_TIMEOUT
eb status $2 --profile=eb-roombooking-profle
echo "Updating the latest swagger specification in AWS API Gateway"    

# import into aws api gateway
aws apigateway put-rest-api --body file://swagger/room-booking-openapi-apigtwy-specs.yaml --mode overwrite --rest-api-id $4 --profile=eb-roombooking-profle
echo "Deploying the latest swagger specification into AWS API Gateway Stage"    

# update the aws api gateway resource policy to allow traffic only from shape source ips
chmod +x ./circleci-scripts/update-apigtwyrespolicy.sh 
bash ./circleci-scripts/update-apigtwyrespolicy.sh $4

# deploy into aws api gateway stages
description="Automatic deployment from CircleCI at $(date)"
aws apigateway create-deployment --rest-api-id $4 --stage-name $5 --description "$description" --profile=eb-roombooking-profle
fi

done