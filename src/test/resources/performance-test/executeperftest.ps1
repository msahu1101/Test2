## Powershell script for blazemeter

Param(
 #[Parameter(Mandatory=$True)]
 [string]$Filepath,
 [string]$ProjectId,
 [string]$ProjectName,
 [string]$User,
 [string]$Password,
 [string]$Iterations,
 [string]$Threads
)

$TestId = ''
$TestRunId = ''
$FileNameUpdated = 'RBS-CRS-PERFORMANCE-TEST_UPDATED.jmx'

Write-Host "FilePath: $FilePath"
Write-Host "Updated FileName: $FileNameUpdated"
Write-Host "Project ID: $ProjectId"
Write-Host "ProjectName: $ProjectName"
Write-Host "User: $User"
Write-Host "Password: $Password"
Write-Host "Iterations: $Iterations"
Write-Host "Threads: $Threads"

# loading properties file
[xml]$jmxContents = Get-Content $FilePath

$elementProps = $jmxContents.jmeterTestPlan.hashTree.hashTree.Arguments.collectionProp.elementProp

forEach ($elementProp in $elementProps) {	
    if ($elementProp.name -eq "no_of_threads") {
        $elementProp.stringProp[1]."#text" = $Threads
    }
    if ($elementProp.name -eq "loop_count") {
        $elementProp.stringProp[1]."#text" = $Iterations
    }    
}	

$jmxContents.OuterXml | Out-File $FileNameUpdated
[xml]$jmxContentsUpdated = Get-Content $FileNameUpdated	

## Authentication
$webClient = New-Object System.Net.WebClient
$credCache = new-object System.Net.CredentialCache
$creds = new-object System.Net.NetworkCredential($User,$Password)
$webclient.Credentials = $credCache
$AuthHeader = @{"Authorization" = "Basic "+[System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($User+":"+$Password))}

## Create Test
$Payload = '{"projectId": "' + $ProjectId + '", "name": "' + $ProjectName + '", "overrideExecutions": [{ "concurrency": ' + $Threads + ', "iterations": "' + $Iterations + '", "rampUp": "0m"} ], "configuration": { "type": "taurus", "filename": "' + $FileNameUpdated + '", "scriptType": "jmeter"} }'

Write-Host "Payload: $Payload"

$TestResponse = Invoke-RestMethod -Method POST -Header $AuthHeader -ContentType "application/json" -uri "https://a.blazemeter.com/api/v4/tests" -Body $Payload
$TestId = $TestResponse.result.id

Write-Host "TestId: $TestId"

## Upload Performance Suite
$UploadUrl = 'https://a.blazemeter.com/api/v4/tests/' + $TestId + '/files'
$credCache.Add($UploadUrl, "Basic", $creds)
$webClient.UploadFile($UploadUrl, "POST", (Get-ChildItem $FileNameUpdated))


## Run Tests
$RunUrl = 'https://a.blazemeter.com:443/api/v4/tests/'+$TestId+'/start'
$RunResponse = Invoke-RestMethod -Method POST -Header $AuthHeader -uri $RunUrl -ContentType "application/json"
$TestRunId = $RunResponse.result.id

## Get Status
$RunUrl = 'https://a.blazemeter.com/api/v4/masters/'+$TestRunId+'/status'
$TestStatus = 'INITIATED' 
$StatusChecker = 0
do{
    Write-Host 'Checking run status #'$StatusChecker ' | Last Status: ' $TestStatus 
    $StatusChecker = $StatusChecker+1
    $StatusResponse = Invoke-RestMethod -Method GET -Header $AuthHeader -uri $RunUrl -ContentType "application/json" 
    $TestStatus = $StatusResponse.result.status     
    Start-Sleep -Seconds 5
 }

until(($TestStatus -eq 'ENDED') -OR ($StatusChecker -gt 100))


$RunUrl = 'https://a.blazemeter.com/api/v4/masters/'+$TestRunId+'/reports/aggregatereport/data'
$StatsResponse = Invoke-RestMethod -Method GET -Header $AuthHeader -uri $RunUrl -ContentType "application/json"
$StatsResponse.result |ConvertTo-Json| Out-File output.json


## Print out
Write-Host Project Name: $ProjectName
Write-Host Project ID: $ProjectId
Write-Host Test ID: $TestId
Write-Host Test Run ID: $TestRunId
write-Host Response: $StatsResponse.result