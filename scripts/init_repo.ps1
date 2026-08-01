[CmdletBinding()]
param([switch]$Reinitialize)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if ((Test-Path '.git') -and -not $Reinitialize)
{
    Write-Host "Git repository already exists at $repoRoot"
    git status --short
    exit 0
}

if ($Reinitialize -and (Test-Path '.git'))
{
    throw 'Refusing to delete existing Git metadata. Remove .git manually only if you intentionally want a new history.'
}

git init
git branch -M main
git status
