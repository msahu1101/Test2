#!/bin/bash

# Step 1: 
# This script gets the latest production tag from GIT(sorted by date to get latest)
# It increments the tag version by 0.1 to create new tag name/EB artifact name.
# 

git for-each-ref --format="%(refname:lstrip=2)" --sort=-taggerdate --count=1 refs/tags/*-release-$CIRCLE_BRANCH > recent-tag.txt

if [ -s recent-tag.txt ]
then
     echo "Reading recent-tag.txt file"
     filelines=`cat recent-tag.txt`
     
     for var in $filelines ; do
          IFS="-"  read -ra TAG_NAME_SPLIT_BY_DASH <<< "$var"
          echo "----${TAG_NAME_SPLIT_BY_DASH[0]}-----${TAG_NAME_SPLIT_BY_DASH[1]}----${TAG_NAME_SPLIT_BY_DASH[2]}----"
          label_val=$(echo ${TAG_NAME_SPLIT_BY_DASH[0]} 0.1 | awk '{print $1 + $2}')
     done     
else
     echo "File: recent-tag.txt does not exist. Set to default label"
     label_val=1.0
     echo "$label_val"
fi
     #strip off slashes in the branch name as EB does not accept slash in the artifact names
     #varStripValue=$(echo $CIRCLE_BRANCH | sed 's/\//\-/g') # may not be required for prod/preprod branch
     echo 'export EB_LABEL_NAME=$label_val-release-$CIRCLE_BRANCH' >> $BASH_ENV
     source $BASH_ENV


# Step 2:
# Creates a production release tag and pushes to GIT Remote
     cd ~/project
     echo "Printing EB artifact label name: $EB_LABEL_NAME"
     echo "Tagging a production release in GIT as IT tests are successful"
     #configure git
     git config --global user.email "mgmcibot@mgmresorts.com"
     git config --global user.name "mgmcibot"
     git tag -a $EB_LABEL_NAME -m "Automatic Circle CI Tagging at $(date)"
     git push origin $EB_LABEL_NAME
     echo "Tagging in GIT is complete"
