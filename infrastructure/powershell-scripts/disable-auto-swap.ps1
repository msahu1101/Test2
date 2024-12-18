param (
    $resourceGroupName,
    $appName,
    $stagingSlotName)

# Disable auto-swap from staging to prod
az functionapp deployment slot auto-swap --name $appName --resource-group $resourceGroupName --slot $stagingSlotName --auto-swap-slot production --disable