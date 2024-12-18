#!/bin/bash

      echo " Copy the AppD Agent Installer files from S3 "
      rm -rf /opt/appdynamics > /dev/null 2>&1


# Check and extraction of the Java Agent
if [ -f /tmp/appagent.zip ]
then
    mkdir -p /opt/appdynamics/appagent
    unzip /tmp/appagent.zip -d /opt/appdynamics/appagent/

    sed -i "s#<node-name></node-name>#<node-name>"$HOSTNAME"</node-name>#g"  /opt/appdynamics/appagent/ver20.4.0.29862/conf/controller-info.xml
    chown -R webapp:webapp /opt/appdynamics/appagent
    chmod -R 775 /opt/appdynamics      
    cp /tmp/appd.sh /opt/appdynamics/appd.sh
    rm -rf /tmp/appagent.zip > /dev/null 2>&1

else
    "AppDynamics App Agent does not exist. Exiting."
    exit 1
fi
      

# Set environment variables
       sudo rm -rf /etc/environment /etc/profile.d/appd_profile.sh
       echo "APPDYNAMICS_AGENT_UNIQUE_HOST_ID=$HOSTNAME" >> /etc/environment
       echo "export APPDYNAMICS_AGENT_UNIQUE_HOST_ID=$HOSTNAME" >> /etc/profile.d/appd_profile.sh
       source /etc/profile.d/appd_profile.sh


APPDYNAMICS_CONTROLLER_HOST_NAME=$(/opt/elasticbeanstalk/bin/get-config environment -k APPDYNAMICS_CONTROLLER_HOST_NAME 2>&1)
APPDYNAMICS_CONTROLLER_PORT=$(/opt/elasticbeanstalk/bin/get-config environment -k APPDYNAMICS_CONTROLLER_PORT 2>&1)
APPDYNAMICS_CONTROLLER_SSL_ENABLED=$(/opt/elasticbeanstalk/bin/get-config environment -k APPDYNAMICS_CONTROLLER_SSL_ENABLED 2>&1)
APPDYNAMICS_AGENT_ACCOUNT_NAME=$(/opt/elasticbeanstalk/bin/get-config environment -k APPDYNAMICS_AGENT_ACCOUNT_NAME 2>&1)
APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY=$(/opt/elasticbeanstalk/bin/get-config environment -k APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY 2>&1)
APPDYNAMICS_AGENT_APPLICATION_NAME=$(/opt/elasticbeanstalk/bin/get-config environment -k APPDYNAMICS_AGENT_APPLICATION_NAME 2>&1)
APPDYNAMICS_SIM_ENABLED=$(/opt/elasticbeanstalk/bin/get-config environment -k APPDYNAMICS_SIM_ENABLED 2>&1)


# Environment Variable Checks and persistence
if [ -n "${APPDYNAMICS_CONTROLLER_HOST_NAME:+1}" ]
then
    echo "APPDYNAMICS_CONTROLLER_HOST_NAME=$APPDYNAMICS_CONTROLLER_HOST_NAME" >> /etc/environment
    echo "export APPDYNAMICS_CONTROLLER_HOST_NAME=$APPDYNAMICS_CONTROLLER_HOST_NAME" >> /etc/profile.d/appd_profile.sh
else
    echo "APPDYNAMICS_CONTROLLER_HOST_NAME not set. Exiting."
    exit 1
fi

if [ -n "${APPDYNAMICS_CONTROLLER_PORT:+1}" ]
then
    echo "APPDYNAMICS_CONTROLLER_PORT=$APPDYNAMICS_CONTROLLER_PORT" >> /etc/environment
    echo "export APPDYNAMICS_CONTROLLER_PORT=$APPDYNAMICS_CONTROLLER_PORT" >> /etc/profile.d/appd_profile.sh
else
    echo "APPDYNAMICS_CONTROLLER_PORT not set. Exiting."
    exit 1
fi

if [ -n "${APPDYNAMICS_CONTROLLER_SSL_ENABLED:+1}" ]
then
    echo "APPDYNAMICS_CONTROLLER_SSL_ENABLED=$APPDYNAMICS_CONTROLLER_SSL_ENABLED" >> /etc/environment
    echo "export APPDYNAMICS_CONTROLLER_SSL_ENABLED=$APPDYNAMICS_CONTROLLER_SSL_ENABLED" >> /etc/profile.d/appd_profile.sh
    if [ "$APPDYNAMICS_CONTROLLER_SSL_ENABLED" == "false" ]
    then
        APPDYNAMICS_CONTROLLER_PROTOCOL="http"
    elif [ "$APPDYNAMICS_CONTROLLER_SSL_ENABLED" == "true" ]
    then
        APPDYNAMICS_CONTROLLER_PROTOCOL="https"
    fi
else
    echo "APPDYNAMICS_CONTROLLER_SSL_ENABLED not set. It will default to false."
    echo "APPDYNAMICS_CONTROLLER_SSL_ENABLED=false" >> /etc/environment
    echo "export APPDYNAMICS_CONTROLLER_SSL_ENABLED=false" >> /etc/profile.d/appd_profile.sh
    APPDYNAMICS_CONTROLLER_PROTOCOL="http"
fi

if [ -n "${APPDYNAMICS_AGENT_ACCOUNT_NAME:+1}" ]
then
    echo "APPDYNAMICS_AGENT_ACCOUNT_NAME=$APPDYNAMICS_AGENT_ACCOUNT_NAME" >> /etc/environment
    echo "export APPDYNAMICS_AGENT_ACCOUNT_NAME=$APPDYNAMICS_AGENT_ACCOUNT_NAME" >> /etc/profile.d/appd_profile.sh
else
    echo "APPDYNAMICS_AGENT_ACCOUNT_NAME not set. It will default to customer1."
    echo "APPDYNAMICS_AGENT_ACCOUNT_NAME=customer1" >> /etc/environment
    echo "export APPDYNAMICS_AGENT_ACCOUNT_NAME=customer1" >> /etc/profile.d/appd_profile.sh
    APPDYNAMICS_AGENT_ACCOUNT_NAME=customer1
fi

if [ -n "${APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY:+1}" ]
then
    echo "APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY=$APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY" >> /etc/environment
    echo "export APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY=$APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY" >> /etc/profile.d/appd_profile.sh
else
    echo "APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY not set. Exiting."
    exit 1
fi

if [ -n "${APPDYNAMICS_AGENT_APPLICATION_NAME:+1}" ]
then
    echo "APPDYNAMICS_AGENT_APPLICATION_NAME=$APPDYNAMICS_AGENT_APPLICATION_NAME" >> /etc/environment
    echo "export APPDYNAMICS_AGENT_APPLICATION_NAME=$APPDYNAMICS_AGENT_APPLICATION_NAME" >> /etc/profile.d/appd_profile.sh
else
    echo "APPDYNAMICS_AGENT_APPLICATION_NAME not set. Exiting."
    exit 1
fi

if [ -n "${APPDYNAMICS_SIM_ENABLED:+1}" ]
then
    echo "APPDYNAMICS_SIM_ENABLED=$APPDYNAMICS_SIM_ENABLED" >> /etc/environment
    echo "export APPDYNAMICS_SIM_ENABLED=$APPDYNAMICS_SIM_ENABLED" >> /etc/profile.d/appd_profile.sh
else
    echo "APPDYNAMICS_SIM_ENABLED not set. Default to false."
    echo "APPDYNAMICS_SIM_ENABLED=false" >> /etc/environment
    echo "export APPDYNAMICS_SIM_ENABLED=false" >> /etc/profile.d/appd_profile.sh
fi




# Check and extraction of the Java Agent
if [ -f /tmp/machineagent.zip ]
then
        # MachineAgent Download, Installation and Configuration
        mkdir -p /opt/appdynamics/machineagent
        unzip /tmp/machineagent.zip -d /opt/appdynamics/machineagent/
        rm -rf /tmp/appagent.zip > /dev/null 2>&1

      #permissions for agents
      echo " Giving permissions for agents"      
      chown -R webapp:webapp /opt/appdynamics
      chmod -R 777 /opt/appdynamics      

       #starting Machine Agent      
       sed -i "s#<unique-host-id></unique-host-id>#<unique-host-id>"$HOSTNAME"</unique-host-id>#g"  /opt/appdynamics/machineagent/conf/controller-info.xml
       echo "Time now is $(date) - Starting AppDynamics Machine Agent"

        pkill -f machineagent
        sleep 10
        /opt/appdynamics/machineagent/bin/machine-agent -d -p /opt/appdynamics/machineagent/bin/machine.pid
        sleep 20


else
    "AppDynamics Machine Agent does not exist. Exiting."
    exit 1
fi
