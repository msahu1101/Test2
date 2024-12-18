#!/bin/bash
testclasseslist=""
while read name
do
   if [[ -z $testclasseslist ]]; then
      testclasseslist="$name"
   else
      testclasseslist="$testclasseslist,$name"
   fi
done < ~/project/classnames-this-node.txt
