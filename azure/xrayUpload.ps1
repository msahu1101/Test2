param ($xrayClientId,
    $xrayClientSecret,
    $xrayAuthUri,
    $xrayImportUri,
    $resultsFolder,
    $basicAuthUser,
    $basicAuthPass,
    $jiraUri,
    $projectKey)

add-type @"
    using System.Net;
    using System.Security.Cryptography.X509Certificates;
    public class TrustAllCertsPolicy : ICertificatePolicy {
        public bool CheckValidationResult(
            ServicePoint srvPoint, X509Certificate certificate,
            WebRequest request, int certificateProblem) {
            return true;
        }
    }
"@
[System.Net.ServicePointManager]::CertificatePolicy = New-Object TrustAllCertsPolicy

Write-Output "Uploading Test Results to X-RAY"

#Create Auth token to import test results into xRay
$authorizationBody = @{
    client_id="$xrayClientId"
    client_secret="$xrayClientSecret"
}

$XRAY_AUTH_TOKEN=Invoke-RestMethod "$xrayAuthUri" -Method Post -Body $authorizationBody -Headers @{ Accept = "application/json" }


#Extract from pom file
[xml]$pomXml = Get-Content .\$env:RELEASE_PRIMARYARTIFACTSOURCEALIAS\drop\pom.xml
$releaseVersion=$pomXml.project.properties.releaseVersion
$componentName=$pomXml.project.properties.componentName


#Create request headers to access Jira
$basicAuth = [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($basicAuthUser + ":" + $basicAuthPass))
$jiraApiReqHeaders = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
$jiraApiReqHeaders = @{}
$jiraApiReqHeaders.Add("Authorization","Basic $basicAuth")
$jiraApiReqHeaders.Add("Accept","application/json")


#Check if release version already created in project, if not create the version
$versionExists = $false
$projectVersions = Invoke-RestMethod -Uri "$jiraUri/project/$projectKey/versions" -ContentType 'application/json' -Method Get -Headers $jiraApiReqHeaders;
$projectVersions | ForEach-Object {
    If($_.name -match "$releaseVersion") {$versionExists = $true}
}

If(!$versionExists) {
    $versionReqBody = @{}
    $versionReqBody.Add("name",$releaseVersion)
    $versionReqBody.Add("archived",$false)
    $versionReqBody.Add("released",$false)
    $versionReqBody.Add("projectId",10130)
    $createVersionReqBody =  $versionReqBody | ConvertTo-Json -Depth 10
    $createVersionResp = Invoke-RestMethod -Uri "$jiraUri/version" -ContentType 'application/json' -Method Post -Body $createVersionReqBody -Headers $jiraApiReqHeaders;
}


#Create request body to add additional fields to Test Execution result
$fieldsBody = @{}
$bodyArray = @{}
$fixVersionList = New-Object System.Collections.ArrayList
$fixVersionList.Add(@{name="$releaseVersion"})
$bodyArray.Add("fixVersions",$fixVersionList)
$bodyArray.Add("customfield_10343",@{value="$componentName"})
$fieldsBody.Add("fields",$bodyArray)
$addFieldsReqBody =  $fieldsBody | ConvertTo-Json -Depth 10



Get-ChildItem -Path $resultsFolder/TEST-*.xml |
        Foreach-Object {
            Write-Output $_.FullName;
            $xrayResponse = Invoke-RestMethod -Uri "$xrayImportUri" -ContentType 'text/xml' -Method Post -Headers @{Authorization="Bearer $XRAY_AUTH_TOKEN"} -InFile $_.FullName;
            $testExecutionKey = $xrayResponse.key;
            Write-Output "Xray import succeeded. Please refer to the below link for test execution results.";
            Write-Output "https://mgmdigitalventures.atlassian.net/browse/$testExecutionKey";
            $selfLink = $xrayResponse.self;
            $addFieldsResponse = Invoke-RestMethod -Uri $selfLink -ContentType 'application/json' -Method Put -Body $addFieldsReqBody -Headers $jiraApiReqHeaders;
            Write-Output "Xray Add Fields to TestExecution succeeded."
        }
