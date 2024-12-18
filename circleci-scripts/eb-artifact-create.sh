#!/bin/bash
#This creates elatic beanstalk deployment ready artifact


#!/bin/bash
findVal () {
        KEY=$(grep "$1" $2)
        if test -z "$KEY"
        then 
            echo "NONE"; 
        else 
            IFS="="  read -ra VAL <<< "${KEY}"
            echo ${VAL[1]}  
        fi   
                   
}

mkdir applbundle                  
mv /tmp/workspace/spring-jar/app.jar ./applbundle/application.jar

# Create a Procfile by reading jvm.options with branch names
while IFS='' read -r line || [[ -n "$line" ]]; do
branchName=$(echo ${line} | cut -d"=" -f1) #find branch name
if [ "$branchName" == "$CIRCLE_BRANCH" ]; then  #split string by = and find second part
  line=$(echo ${line} | cut -d"=" -f2-)
  echo "web: $line" > Procfile
fi
done <  ./src/main/resources/jvm.options

# Create a default Procfile if it was not created in earlier step
# Works for all those branches which does not have entry in jvm.options file
if [ ! -f ./Procfile ]; then
    echo "No Branch mapping found from src/main/resources/jvm.options file. Creating procfile from defaults mapping"
        DEFAULT_JVM_OPTS=$(grep "default" ./src/main/resources/jvm.options)
        IFS="="  read -ra DEFAULT_JVM_OPTS_LN <<< "${DEFAULT_JVM_OPTS}"
        echo "web: ${DEFAULT_JVM_OPTS_LN[1]}" >> Procfile
fi                

# Setting Ulimits for Amazon Linux
uLimitVal=$(findVal "$CIRCLE_BRANCH" "./src/main/resources/ulimits.txt" )

if [ "$uLimitVal" = "NONE" ]; then
    rm -rf .ebextensions/update-ulimit.config
else
    sed -i "s/#ULIMIT_VAL#/$uLimitVal/g" .ebextensions/update-ulimit.config
fi

# Setting worker connections for nginx
uWorkConnsVal=$(findVal "$CIRCLE_BRANCH" "./src/main/resources/nginx.txt" )

if [ "$uWorkConnsVal" = "NONE" ]; then
    rm -rf .ebextensions/nginx/nginx.conf
else
    sed -i "s/#WORKER_CONNS#/$uWorkConnsVal/g" .ebextensions/nginx/nginx.conf
fi

# # For Prod branch, replace related files like ssl keys etc.. 
# if [[ $CIRCLE_BRANCH == prod*  ]]; then
#     echo "Production Branch. Replacing Production Related SSL Key Files..."    
#     rm -rf applbundle/.ebextensions/https-instance.config
#     mv applbundle/.ebextensions/https-instance.config.prod applbundle/.ebextensions/https-instance.config
# else
#     rm -rf applbundle/.ebextensions/https-instance.config.prod
# fi 

pwd
echo
ls
echo

case $CIRCLE_BRANCH in

  prod)
    echo "Production Branch. Replacing Production Related SSL Keys and AppD files..."    
    rm -rf .ebextensions/https-instance.config .ebextensions/appd.config
    mv .ebextensions/https-instance.config.prod .ebextensions/https-instance.config
    mv .ebextensions/appd.config.prod .ebextensions/appd.config	
    
    sed -i "s/#APPD_CNTRLR#/$(echo $APPD_CONTEXT | jq -r '.appd.prod.controller')/g" .ebextensions/appd.config
    sed -i "s/#APPD_ACCT_NAME#/$(echo $APPD_CONTEXT | jq -r '.appd.prod.acctname')/g" .ebextensions/appd.config
    sed -i "s/#APPD_ACCT_ACCSKEY#/$(echo $APPD_CONTEXT | jq -r '.appd.prod.acctaccesskey')/g" .ebextensions/appd.config

    ;;

  preprod)
    echo -n "Preprod Branch - Remove Prod related eb configuration files"
    rm -rf .ebextensions/*.prod
    
    sed -i "s/#APPD_CNTRLR#/$(echo $APPD_CONTEXT | jq -r '.appd.nonprod.controller')/g" .ebextensions/appd.config
    sed -i "s/#APPD_ACCT_NAME#/$(echo $APPD_CONTEXT | jq -r '.appd.nonprod.acctname')/g" .ebextensions/appd.config
    sed -i "s/#APPD_ACCT_ACCSKEY#/$(echo $APPD_CONTEXT | jq -r '.appd.nonprod.acctaccesskey')/g" .ebextensions/appd.config
    sed -i "s/#APPD_APP_NAME#/PREPROD-Booking-RBS/g" .ebextensions/appd.config
    
    ;;
    
  qa)
    echo -n "QA Branch - Remove Prod related eb configuration files"
    rm -rf .ebextensions/*.prod
    
    sed -i "s/#APPD_CNTRLR#/$(echo $APPD_CONTEXT | jq -r '.appd.nonprod.controller')/g" .ebextensions/appd.config
    sed -i "s/#APPD_ACCT_NAME#/$(echo $APPD_CONTEXT | jq -r '.appd.nonprod.acctname')/g" .ebextensions/appd.config
    sed -i "s/#APPD_ACCT_ACCSKEY#/$(echo $APPD_CONTEXT | jq -r '.appd.nonprod.acctaccesskey')/g" .ebextensions/appd.config
    sed -i "s/#APPD_APP_NAME#/UAT-Booking-RBS/g" .ebextensions/appd.config
    
    ;;

  *)
    echo -n "Other Branches - Remove Prod related or AppD related eb configuration files"
    rm -rf .ebextensions/*.prod .ebextensions/appd.config
    ;;
esac

# Copy/Move deployable files into folder
mv Procfile applbundle/
cp ./src/main/resources/aurora_public.key applbundle/
cp -R .ebextensions applbundle/

cd applbundle/

ls .ebextensions
echo

# Create final EB deployable artifact
zip -r room-booking-services.zip application.jar aurora_public.key .ebextensions Procfile
cd ..
