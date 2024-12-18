#   Install and Configure APIM

param ($subscription,
$resourceGroupName,
$apimServiceName, 
$Env, $apiSpecificationPath, 
$AppServiceApplicationUrl,
$TargetPath, $policyFilePath, $appServiceName)

Write-Output "Installing PowerShell YAML Reader Module"

Install-Module powershell-yaml -Force -Verbose -Scope CurrentUser

Import-Module -Name powershell-yaml


Write-Output "$appServiceName"
Write-Output "$appServiceName-$Env"

((Get-Content -Path $apiSpecificationPath) -replace "$appServiceName", "$appServiceName-$Env") | Set-Content -Path $apiSpecificationPath

$RawYaml = [IO.File]::ReadAllText($apiSpecificationPath)
$PsYaml = (ConvertFrom-Yaml -Yaml $RawYaml)

$ApiName = $PsYaml.info.title
$VersionDesc = $PsYaml.info.description
$ApiId = $ApiName.ToLower() -replace '[\W]+', '-'
$ApiVersionStr = $PsYaml.info.version.Split('.')
$ApiVersionMajor = [int]$ApiVersionStr[0]
$ApiVersionMinor = [int]$ApiVersionStr[1]

Write-Output "API Name: $ApiName"
Write-Output "API Id: $ApiId"
Write-Output "API Version: $ApiVersionStr"

Connect-AzAccount -Identity
Set-AzContext -SubscriptionId $subscription

$apiManagementContextParams = @{
    ResourceGroupName = $resourceGroupName
    ServiceName = $apimServiceName
}
$apiManagementContext = New-AzApiManagementContext @apiManagementContextParams

#Get All API Metadata
$apiSpecifications = Get-AzApiManagementApi -Context $apiManagementContext

#now we filter out the specification object that we want
$apiSpecificationByName = $apiSpecifications | Where-Object { $_.Name -eq $ApiName }
 Write-Output  "apiSpecificationByName: $apiSpecificationByName"

 #Get the contents of policy file
$policyString = (Get-Content -Path $policyFilePath -Raw)

 Write-Host  "policyString: $policyString"

if($null -eq $apiSpecificationByName){
        
  # API Doesn's exist, create new one
  Write-Output "First time deploying: $ApiName"

  #Create a new API
  $newApiParams = @{
          Context = $apiManagementContext
          ApiId = $ApiId
          Name = $ApiName
  #        ApiVersionSetId = $apiVersionSet.ApiVersionSetId
          Protocols = @("https")
          ServiceUrl = $AppServiceApplicationUrl
          Path = $TargetPath
  }
  $newApi = New-AzApiManagementApi @newApiParams
  
  $newApi.SubscriptionRequired=$false
  
  Set-AzApiManagementApi -InputObject $newApi -Name $newApi.Name -ServiceUrl $newApi.ServiceUrl -Protocols $newApi.Protocols
  
  #Import the Swagger file
  $ApiImportParams = @{
          ApiId = $ApiId
          Context = $apiManagementContext
          SpecificationFormat = "OpenApi"
          SpecificationPath = $apiSpecificationPath
          Path = $TargetPath
          Protocol = "Https"
          ServiceUrl = $AppServiceApplicationUrl
  }
  Import-AzApiManagementApi @ApiImportParams

  #set path. . During API creation it is not working.
  Set-AzApiManagementApi -Context $apiManagementContext  -ApiId $ApiId  -Path $TargetPath

 # Setting the APIM Policy from the policy file.
  $OperationID = Get-AzApiManagementOperation -Context $apiManagementContext -ApiId $ApiId | Select-Object -Property "OperationId"
  Write-Output  "OperationID: $OperationID.OperationId"

  # Setting the APIM Policy from the policy file
  Set-AzApiManagementPolicy -Context $apiManagementContext -ApiId $ApiId -Policy $policyString

  
}else{
        $apiSpecification = $apiSpecificationByName | Where-Object { $_.ApiId -eq $ApiId }
        if($null -eq $apiSpecification){
                #New Version
                # Dont need this now as versioning is not on APIM but through API specification and function app
                Write-Output "Handle New Version Creation"
        }else{
                #New Revision
                Write-Output "Deploying a new revision for: $ApiName"
                $newRevision = [int]$apiSpecification.ApiRevision + 1;
                Write-Output "Printing newRevision: $newRevision"

                $newApiRevisionParams = @{
                        Context = $apiManagementContext
                        ApiId = $apiSpecification.ApiId
                        ApiRevision = $newRevision 
                        SourceApiRevision = $apiSpecification.ApiRevision
                }
        
                $newApiRevision = New-AzApiManagementApiRevision @newApiRevisionParams
        
                $newApiManagementApiReleaseParams = @{
                        Context = $apiManagementContext
                        ApiId = $apiSpecification.ApiId
                        ApiRevision = $newApiRevision.ApiRevision
                }
                New-AzApiManagementApiRelease @newApiManagementApiReleaseParams
        
                $importApiParams = @{
                        Context = $apiManagementContext
                        ApiId = $apiSpecification.ApiId
                        ApiRevision = $newApiRevision.ApiRevision
                        SpecificationFormat = "OpenApi"
                        SpecificationPath = $apiSpecificationPath
                        Path = $apiSpecification.Path
                        Protocol = $apiSpecification.Protocols
                        ServiceUrl = $apiSpecification.ServiceUrl
                }
        
                Import-AzApiManagementApi @importApiParams
        
                
                #set path. . During API creation it is not working.
                Set-AzApiManagementApi -Context $apiManagementContext  -ApiId $apiSpecification.ApiId  -Path $TargetPath

                # Setting the APIM Policy from the policy file.
                $OperationID = Get-AzApiManagementOperation -Context $apiManagementContext -ApiId $apiSpecification.ApiId | Select-Object -Property "OperationId"
                Write-Output  "OperationID: $OperationID.OperationId"

                # Setting the APIM Policy from the policy file
                Set-AzApiManagementPolicy -Context $apiManagementContext -ApiId $apiSpecification.ApiId -Policy $policyString

        }
    
}