#!/bin/bash
#This will skip IT tests based on the Flag value

if [ 'false' == $EXEC_ITTESTS_FLAG ]; then
    echo ' Halting the integration-tests step as Skip Tests is TRUE ...'
    circleci step halt               
fi
