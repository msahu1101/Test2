#!/bin/bash
#This runs integration tests for prod / non prod branches.

# Reads Production Env IT test environment URL
if [[ $CIRCLE_BRANCH == prod*  ]]; then
    echo " Executing Production Integration Tests ... "
    echo $PROD_IT_TESTS_EXEC_ENV
    IFS="|" read -ra ITTESTS_ENV_STR <<< "${PROD_IT_TESTS_EXEC_ENV}"; 
else                
# Reads Non-Production Env IT test environment URL   
        function myfunc()
            {
                while IFS='' read -r line || [[ -n "$line" ]]; do
                echo $line |  awk -v env_var="$CIRCLE_BRANCH" '{split($0,array,"=")
                if(array[1]==env_var)
                    print array[2]
                delete array
                }' 
                done
            } < /home/circleci/project/src/main/resources/it-tests-env.txt

        IT_TESTS_EXEC_ENV="$(myfunc)"

# Defaults IT environment URL for branches which has no mapping in it-tests.env.txt file

        if [ -z "$IT_TESTS_EXEC_ENV" ]  
        then
            echo "No branch setup for IT Tests Execution as per src/main/resources/it-tests.env.txt file. Setting it to default IT Test Execution Environment"
            DEFAULT_TESTS_EXEC_ENV=$(grep "default" ./src/main/resources/it-tests-env.txt)
            IFS="="  read -ra DEFAULT_TESTS_EXEC_ENV_STR <<< "${DEFAULT_TESTS_EXEC_ENV}"
            IFS="|"  read -ra ITTESTS_ENV_STR <<< "${DEFAULT_TESTS_EXEC_ENV_STR[1]}"
        else
            IFS="|" read -ra ITTESTS_ENV_STR <<< "${IT_TESTS_EXEC_ENV}"; 
        fi
fi

# Setup environment variables for running IT Tests
echo ${ITTESTS_ENV_STR[1]} 
echo 'export ApiGtwy_Passthru=${ITTESTS_ENV_STR[0]}' >> $BASH_ENV
echo 'export apiKey=${ITTESTS_ENV_STR[1]}' >> $BASH_ENV
echo 'export apiVersion=${ITTESTS_ENV_STR[2]}' >> $BASH_ENV
echo 'export baseUrlV1=${ITTESTS_ENV_STR[3]}' >> $BASH_ENV
echo 'export envPrefix=${CIRCLE_BRANCH}' >> $BASH_ENV
source $BASH_ENV

# Run IT Tests through Maven
echo ------$baseUrlV1------$apiVersion-----$ApiGtwy_Passthru-----$envPrefix
if [[ $CIRCLE_BRANCH == "qa" ]] || $CIRCLE_BRANCH == "staging" ]] || [[ $CIRCLE_BRANCH == "qa4" ]] || [[ $CIRCLE_BRANCH == "preprod" ]] || [[ $CIRCLE_BRANCH == "prod" ]]
then
    mvn clean verify -Dgroups="com.mgm.services.booking.room.ProductionSanityTests" -Dskip.unit.tests=true
else
    cd /home/circleci/project/
    circleci tests glob "src/test/java/com/mgm/services/booking/room/it/**/*.java"  | circleci tests split --split-by=timings --timings-type=classname | sed -e 's#^src/test/java/com/mgm/services/booking/room/it/\(.*\)\.java#\1#' | tr "/" "."  > ~/project/classnames-this-node.txt
    chmod +x /home/circleci/project/circleci-scripts/prepare-test-classes.sh
    source /home/circleci/project/circleci-scripts/prepare-test-classes.sh

    echo $testclasseslist
    mvn clean verify -Dskip.unit.tests=true -Dit.test=$testclasseslist
fi
