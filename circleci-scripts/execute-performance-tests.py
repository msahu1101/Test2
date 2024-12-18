#---------------------------------------------------------------------------------------------------
# This script is used for executing the jmeter scripts in blazemeter.
# The scripts utilizes the APIs exposed by blazemeter to create test, upload script and run the tests
# More information on APIs can be found here https://api.blazemeter.com/performance/
#---------------------------------------------------------------------------------------------------


import os
import json
from datetime import datetime
import time
import requests


#These variables needs to moved to context and read as environment variables
base_path="../performance-scripts/"              # The base path.
blazemeter_creds="Yjc5MzZjODRiNzNmNTg1NTk1MWIxZDlhOmE1YjFhOWEzOTc4OGRhZjEwYWVjNjkwODIxMzAzNTYyOGU1MDJhYzBiOWZmODgxYzIxM2Y2ZTUwYzY1MzRmNGEwODJiMmI2ZQ=="
                                                 # Base 64 encoded string of key and secret.
project_id=559869                                # The project id in blazemeter portal.
test_id_list = []


def execute_tests_for_file(file_name):
        # Methos to execute the performance tests for the file.
        # This operation includes creating the test case, upload the file to blazemeter and start the test case.
        # In case, if any of the operation failed during execution, skips other operations.

        print("executing for {}".format(file_name))
        is_success = create_test(file_name)
        if not is_success:
                return;
        time.sleep(2)
        is_success = upload_file(file_name)
        time.sleep(10)
        update_test_params();
        if not is_success:
                return;
        run_test()
                



def create_test(file_name):
        # This method is to create the test in blazemeter
        # This method utilizes "/api/v4/tests" endpoints which is exposed by blazemeter
        # Method returns True if successfully created tests.
        print("----------------------------------------------------------------")
        print("creating test" + file_name)
        headers_values = {"Content-Type": "application/json", "Authorization":"Basic "+blazemeter_creds}
        global test_name
        test_name="Jmeter Test - "+file_name+" | "+datetime.now().strftime("%d/%m/%Y %H:%M:%S")

        # Request body can be enhanced by adding the number of users, location etc.
        # Refer https://api.blazemeter.com/performance/#test-object-response-attributes for details on the complete request schema.
        request_body_str={
           "name":test_name,
           "configuration":{
              "type":"taurus",
              "scriptType":"jmeter",
              "filename":file_name,
              "targetThreads": 250,
              "threads": 250,
              "plugins":{
                      "remoteControl":
                              config.get("remoteControl")
                }
           },
           "overrideExecutions":[
                   config.get("overrideExecutions")
                ],
           "executions":[
                   config.get("executions")
                ], 
           "shouldSendReportEmail":True,
           "projectId":project_id
        }
        
        request_body = json.dumps(request_body_str)
        url = url = "https://a.blazemeter.com/api/v4/tests"
        response = requests.post(url, data=request_body, headers=headers_values)
        resp_data = response.content;
        print("Request Data : {} ".format(request_body))
        print("Response Data : {}, Response status : {} ".format(str(resp_data), str(response.status_code)))

        if response.status_code == 200 or response.status_code == 201 or response.status_code == 202:
                print("Rest call execution is successfull..")
                json_response = json.loads(resp_data)
                if json_response.get("error") == None:
                        #If responce body contains no error, process the response.
                        global test_id
                        test_id=json_response.get("result").get("id")
                        test_id_list.append(test_id)
                        print("The Test Id created is : {}".format(test_id))
                        return True
                else:
                        print("Received detected in response payload {}, response : {}".format(json_response.get("error"),resp_data))
                        return False
        else:
                print("Invalid response, status_code : {}, response{}".format(response.status_code,resp_data))
                return False


def upload_file(file_name):
       # This method is to upload the jmx file to blazemeter cloud.
       # This method utilizes "/api/v4/tests/<test_id>/files" endpoint exposed by blazemeter
       # Method returns True if file upload is successfull.
       print("----------------------------------------------------------------")
       headers_values = { "Authorization":"Basic "+blazemeter_creds}
       url = "https://a.blazemeter.com/api/v4/tests/"+str(test_id)+"/files"
       file_path=base_path+file_name;
       print("upload file : {}".format(file_path))
       print("The URL : {}".format(url))
       print("The File Path : {}".format(file_path))
       files = {'file': open(file_path, 'rb')}
       response = requests.post(url, files=files,headers=headers_values)
       print(response.content)
       print("Response status : {}, Response : {}".format(str(response.content), str(response.status_code)))
       if response.status_code == 200 or response.status_code == 201:
            print("Rest call execution is successfull..")
            return True
       else:
            print("Invalid response, status_code : {}, response{}".format(response.status,resp_data))
            return False

def get_configuration_json(configuration_name):
        file_name="./config/test_config_"+configuration_name
        print("Reading configuration from : {}".format(file_name))
        with open(file_name, 'r') as f:
                f_data = f.read();
        return f_data
        
        

def update_test_params():
        # This method is to start the run in blazemeter coud.
        # This method utilizes "/api/v4/tests/<test_id>/start" endpoint exposed by blazemeter
        # Method returns True if file upload is successfull.
        print("----------------------------------------------------------------")
        print("getFiles  of the test")
        headers_values = { "Content-type":"application/json", "Authorization":"Basic "+blazemeter_creds}
        url = "https://a.blazemeter.com/api/v4/tests/"+str(test_id)
        print(url)

        request_body_str= {
           "name": test_name,
           "overrideExecutions":[
              config.get("overrideExecutions")
           ],
           "executions":[
              config.get("executions")
           ],
           "projectId":project_id
        }

        print("The request body : {}".format(request_body_str))
        request_body = json.dumps(request_body_str)

        response = requests.patch(url, data=request_body, headers=headers_values)
        resp_data = response.content;
        print("Response status : {}, Response : {}".format(str(resp_data), str(response.status_code)))
        if response.status_code == 200 or response.status_code == 201 or response.status_code == 202:
                print("Rest call execution is successfull..")
                return True
        else:
                print("Invalid response, status_code : {}, response{}".format(response.status_code,resp_data))
                return False

def run_test():
        # This method is to start the run in blazemeter coud.
        # This method utilizes "/api/v4/tests/<test_id>/start" endpoint exposed by blazemeter
        # Method returns True if file upload is successfull.
        print("----------------------------------------------------------------")
        print("Run the test")
        headers_values = { "Content-type":"application/json", "Authorization":"Basic "+blazemeter_creds}
        url = "https://a.blazemeter.com/api/v4/tests/"+str(test_id)+"/start"
        print(url)

        response = requests.post(url, headers=headers_values)
        resp_data = response.content;
        print("Response status : {}, Response : {}".format(str(resp_data), str(response.status_code)))
        if response.status_code == 200 or response.status_code == 201 or response.status_code == 202:
                print("Rest call execution is successfull..")
                return True
        else:
                print("Invalid response, status_code : {}, response{}".format(response.status_code,resp_data))
                return False


def check_status(_test_id):
        # This method is to start the run in blazemeter coud.
        # This method utilizes "/api/v4/tests/<test_id>/start" endpoint exposed by blazemeter
        # Method returns True if file upload is successfull.
        print("----------------------------------------------------------------")
        print("Run the test")
        headers_values = { "Content-type":"application/json", "Authorization":"Basic "+blazemeter_creds}
        url = "https://a.blazemeter.com/api/v4/tests/"+str(_test_id)+"/start"
        print(url)

        response = requests.post(url, headers=headers_values)
        resp_data = response.content;
        print("Response status : {}, Response : {}".format(str(resp_data), str(response.status_code)))
        if response.status_code == 200 or response.status_code == 201 or response.status_code == 202:
                print("Rest call execution is successfull..")
                json_response = json.loads(resp_data);
                status=json_response.get("result").get("id")
                return status;
        else:
                print("Error occurren, Response status : {}, payload: {}",response.status_code, resp_data )
                return "REMOTE_SERVER_ERROR";


def fetch_test_results(_test_id):
        # This method is to start the run in blazemeter coud.
        # This method utilizes "/api/v4/tests/<test_id>/start" endpoint exposed by blazemeter
        # Method returns True if file upload is successfull.
        print("----------------------------------------------------------------")
        print("Run the test")
        headers_values = { "Content-type":"application/json", "Authorization":"Basic "+blazemeter_creds}
        url = "https://a.blazemeter.com/api/v4/masters/"+str(_test_id)+"/reports/aggregatereport/data'"
        print(url)

        response = requests.get(url, headers=headers_values)
        resp_data = response.content;
        print("Response status : {}, Response : {}".format(str(resp_data), str(response.status_code)))
        if response.status_code == 200 or response.status_code == 201 or response.status_code == 202:
                print("Rest call execution is successfull..")
                json_response = json.loads(resp_data);
                return status;
        else:
                print("Error occurren, Response status : {}, payload: {}",response.status_code, resp_data )
                return "REMOTE_SERVER_ERROR";

def main():
        # The starting point of the script.
        # This iterates through each file in the performace script directory, creates test, upload file and start the tests.
        brarnch_name = os.environ.get('CIRCLE_BRANCH')
        print ("Branch Name : {}".format(brarnch_name))
        global config
        config = json.loads(get_configuration_json(brarnch_name))
        
        entries = os.listdir(base_path)
        for entry in entries:
                execute_tests_for_file(entry)

if __name__ == '__main__':
        main()
       
