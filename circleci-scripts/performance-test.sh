#!/bin/bash
#This initiates performance test suite from another Git Repo.

mkdir ~/output
curl -u $CI_API_TOKEN: \
-d build_parameters[CIRCLE_JOB]=build \
https://circleci.com/api/v1.1/project/github/MGMResorts/room-booking-services-perf-tests/tree/$CI_PERFTEST_BRANCH \
    -o ~/output/result.txt 
