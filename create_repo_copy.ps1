<#
.SYNOPSIS
    Clones a Git repository to a new location, creating a new history with a specified number of commits.

.DESCRIPTION
    This script copies the files from a source Git repository to a new directory,
    initializes a new Git repository, and then commits the files in chunks over a
    specified date range with a specific author.

.PARAMETER NewOriginUrl
    The URL of the new remote Git repository (origin). This is a mandatory parameter.
#>
param (
    [Parameter(Mandatory=$true)]
    [string]$NewOriginUrl
)

# --- Configuration ---
$sourceDir = $PSScriptRoot
$targetDir = "c:\temp\axenapi-web"
$authorName = "alexandra.volushkova"
$authorEmail = "alexandra.volushkova@axenix.pro"
$startDateStr = "2025-04-01"
$endDateStr = "2025-07-28"
$totalCommits = 100
# --- End Configuration ---

# Function to execute a command and check for errors
function Invoke-CommandAndCheck {
    param (
        [string]$Command,
        [string]$ErrorMessage
    )
    Write-Host "Executing: $Command"
    $output = Invoke-Expression $Command
    if ($LASTEXITCODE -ne 0) {
        Write-Error "$ErrorMessage"
        Write-Error "Output: $output"
        exit 1
    }
    return $output
}

# 1. Prepare target directory
Write-Host "Preparing target directory: $targetDir"
if (Test-Path $targetDir) {
    Remove-Item -Recurse -Force $targetDir
}
New-Item -ItemType Directory -Path $targetDir | Out-Null

# 2. Copy versioned files from source to target
Write-Host "Copying files from '$sourceDir' to '$targetDir'"
Push-Location $sourceDir
$filesToCopy = git ls-files -c --others --exclude-standard
Pop-Location
foreach ($file in $filesToCopy) {
    $sourceFilePath = Join-Path -Path $sourceDir -ChildPath $file
    $destinationFilePath = Join-Path -Path $targetDir -ChildPath $file
    $destinationDirectory = Split-Path -Path $destinationFilePath -Parent
    if (-not (Test-Path -Path $destinationDirectory)) {
        New-Item -ItemType Directory -Path $destinationDirectory | Out-Null
    }
    Copy-Item -Path $sourceFilePath -Destination $destinationFilePath
}

# 3. Initialize new Git repository in the target directory
Write-Host "Initializing new Git repository in '$targetDir'"
Push-Location $targetDir
Invoke-CommandAndCheck -Command "git init" -ErrorMessage "Failed to initialize Git repository."
Invoke-CommandAndCheck -Command "git remote add origin `"$NewOriginUrl`"" -ErrorMessage "Failed to add remote origin."

# 4. Commit files in chunks
Write-Host "Creating $totalCommits commits..."
$allFiles = $filesToCopy

if ($allFiles.Count -eq 0) {
    Write-Warning "No files to commit."
    exit 0
}

$filesPerCommit = [Math]::Ceiling($allFiles.Count / $totalCommits)
$startDate = Get-Date $startDateStr
$endDate = Get-Date $endDateStr
$timeSpan = New-TimeSpan -Start $startDate -End $endDate
$timeIncrementTicks = if ($totalCommits -gt 1) { $timeSpan.Ticks / ($totalCommits - 1) } else { 0 }


$env:GIT_AUTHOR_NAME = $authorName
$env:GIT_AUTHOR_EMAIL = $authorEmail
$env:GIT_COMMITTER_NAME = $authorName
$env:GIT_COMMITTER_EMAIL = $authorEmail

for ($i = 0; $i -lt $totalCommits; $i++) {
    $commitDate = $startDate.AddTicks($timeIncrementTicks * $i).ToUniversalTime().ToString("o")
    $filesInThisCommit = $allFiles | Select-Object -Skip ($i * $filesPerCommit) -First $filesPerCommit

    if ($filesInThisCommit.Count -gt 0) {
        foreach ($fileToAdd in $filesInThisCommit) {
            Invoke-CommandAndCheck -Command "git add --force `"$fileToAdd`"" -ErrorMessage "Failed to add file '$fileToAdd' to Git."
        }

        $commitMessage = "feat: Commit number $($i + 1)/$totalCommits"
        $commitCommand = "git commit --date=`"$commitDate`" -m `"$commitMessage`""
        Invoke-CommandAndCheck -Command $commitCommand -ErrorMessage "Failed to commit changes."
    }
}

Pop-Location

Write-Host "`nRepository replication complete in '$targetDir'."
Write-Host "You can now push the repository to the remote."
Write-Host "To do this, run the following commands:"
Write-Host "cd $targetDir"
Write-Host "git push --force --set-upstream origin master"
