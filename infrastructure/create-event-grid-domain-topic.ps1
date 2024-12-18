param (
$resourceGroupName,
$domainName, 
$topicName)

$result = Get-AzEventGridDomainTopic -ResourceGroup $resourceGroupName -DomainName $domainName -DomainTopicName $topicName

if($result.DomainTopicName -eq $Null) 
{	
	Write-Host "Domain topic not found. Creating new one"
	New-AzEventGridDomainTopic -ResourceGroupName digengsharedservices-uw-rg-d -DomainName booking-uw-egd-d -Name $topicName
	Write-Host "New topic--" $topicName " created in domain-- " $domainName " in resource group--" $resourceGroupName 	
}else{
    
	Write-Host "Domain topic--" $topicName " already present."
}