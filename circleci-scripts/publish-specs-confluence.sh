#!/bin/bash
aws s3 cp s3://elasticbeanstalk-nonprod-services-files/utilities/stringify.jar . --profile eb-roombooking-profle
currentversion=$(curl -X GET \
    https://mgmdigitalventures.atlassian.net/wiki/rest/api/content/$CI_CONFLUENCE_CONTDID \
    -H "Authorization: Basic $CI_CONFLUENCE_AUTH" \
    -H "Cache-Control: no-cache"   | jq '.version.number')
newversion=$(( $currentversion + 1 ))
echo "---new version---"+$newversion
java -jar stringify.jar swagger/room-booking-openapi-apigtwy-specs.yaml $newversion "Room Booking APIs" "Automatic API Specification update from Circle CI" > update-content.json

# cat escaped-spec-file.txt | jq --arg newversnum "$newversion" '.version.number=$newversnum' >> update-content.json
#head -5 update-content.json
curl -X PUT https://mgmdigitalventures.atlassian.net/wiki/rest/api/content/$CI_CONFLUENCE_CONTDID -v -H "Content-Type: application/json" -H "Authorization: Basic $CI_CONFLUENCE_AUTH" -H "Cache-Control: no-cache" -d @update-content.json > temp.out           
echo " -- Displaying first 5 lines of the output--"
#head -5 temp.out

