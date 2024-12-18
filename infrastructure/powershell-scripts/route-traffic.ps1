param (
    $resourceGroupName,
    $appName,
    $trafficPercent)

# route trafficPercent% of live traffic to staging slot
az webapp traffic-routing set --distribution staging=$trafficPercent --name $appName --resource-group $resourceGroupName