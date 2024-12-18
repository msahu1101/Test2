#bin/bash
# This scripts reads shape source ips from aws secret mgr and updates api. gtwy resource policy

#Gets the shape source ips from the AWS Secret Mgr. 
if [[ $CIRCLE_BRANCH == "prod" ]]
then
    aws secretsmanager get-secret-value --secret-id rbs/prod/secrets --version-stage AWSCURRENT --profile=eb-roombooking-profle | jq '.SecretString' > secretMgr.json
else
    aws secretsmanager get-secret-value --secret-id rbs/dev/secrets --version-stage AWSCURRENT --profile=eb-roombooking-profle | jq '.SecretString' > secretMgr.json
fi

#Set the python version to 3.x
pyenv global 3.7.0

#The script reads the given Secret mgr. json string and removes escape characters like "\" or "\\"
python3 ./circleci-scripts/parse-json.py | sed 's/^.\(.*\).$/\1/' > secretMgr1.json

#get the value for shape source ip json key
cat secretMgr1.json | jq '."shape-sourceIps"' | sed 's/^.\(.*\).$/\1/' > secretMgr2.txt

#prepare the final apigtwy update resource policy aws command and stringify it for the final run.
if [[ $CIRCLE_BRANCH == "prod" ]]
then
    aws s3 cp s3://elasticbeanstalk-prod-services-files/utilities/stringifyv2.jar . --profile=eb-roombooking-profle
    java -jar stringifyv2.jar "./src/main/resources/apigateway-resource-policy-prod.json" "secretMgr2.txt" "$1" "eb-roombooking-profle" > apigtwy-rp-cmd.sh
else
    aws s3 cp s3://elasticbeanstalk-nonprod-services-files/utilities/stringifyv2.jar . --profile=eb-roombooking-profle
    java -jar stringifyv2.jar "./src/main/resources/apigateway-resource-policy.json" "secretMgr2.txt" "$1" "eb-roombooking-profle" > apigtwy-rp-cmd.sh
fi

echo " Updating API Gateway Resource Policy with Shape Source IPs... "
#give minimal permission for owner and run the apigtwy update resource policy aws command.
chmod 0700 apigtwy-rp-cmd.sh | bash apigtwy-rp-cmd.sh                                                         