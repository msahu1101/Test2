$files=(Get-ChildItem $($env:IT_FOLDER)  | % { $_.basename }) -join ','
Write-Output "Integration Test Cases: $files"
Write-Host ("##vso[task.setvariable variable=IT_V2_TEST_CLASSES]$files")