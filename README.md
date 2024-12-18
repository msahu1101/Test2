# Room Booking Services
This project provides all services for cross-channel room booking and reservation functionalities.

High level functions are:
- Room Prices
- Pre-reserve or preview complete charges
- Confirm Reservation
- Preview reservation modifications
- Confirm reservation modification
- Cancel reservation

## Swagger - API Documentation
- https://mgmdigitalventures.atlassian.net/wiki/spaces/SS/pages/223871445/Room+Booking+APIs

## How to run locally
Prerequisites 
- AWS CLI should be installed and running locally - https://mgmdigitalventures.atlassian.net/wiki/spaces/SS/pages/203423745/AWS+CLI+for+Local+Machine
- Local Redis running on port 6379

### Package or Jar Build (Runs Unit and IT tests as well)
```
mvn clean package
```

### Start - No Profile
Uses default profile and launches all aurora connections with complete ehcache operations
```
cd target
java -jar app.jar
```

### Start - Local Profile
Only create minimal aurora connections and with ehcache operation for 1 or 2 properties
```
cd target
java -jar -Dspring.profiles.active=local app.jar
```

Application requires a standalone redis or hosted redis on AWS for session management.
Local profile expects the redis instance to be available on localhost:6379

All configurations for local profile is available in application-local.properties. 
Update the path for aurora public key as applicable for the local environment. 

### Run Unit Tests Alone
```
mvn clean package -Dskip.integration.tests=true
```

### Run Integration Tests Alone
Below commands uses localhost:8080 as host for running integration tests
```
mvn clean verify -Dskip.unit.tests=true
```

To run integration tests against a particular environment, use -DbaseUrl. Example below:
```
mvn clean verify -Dskip.unit.tests=true -DbaseUrl=http://dev-rbs-api.us-west-2.elasticbeanstalk.com
```

## Guidelines

* Import Eclipse_Java_Profile.xml from this code repository into Eclipse as formatter profile. Profile can be imported in *Eclipse -> Preferences -> Java -> Code Style -> Formatter*

## Instructions on how to setup RBS locally
- https://mgmdigitalventures.atlassian.net/wiki/spaces/UCP/pages/510230981/How+to+setup+RBS+locally
