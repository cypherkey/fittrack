<#
.SYNOPSIS
    Import workouts/sets from scripts/import.json into FitTrack (JWT via config).

.DESCRIPTION
    Uses scripts/FitTrack. Requires -Exercise (import-file exercise name). Only sets for
    that exercise are imported (after optional name→FitTrack map). Weights in lbs are
    converted to kg for the API.

.PARAMETER Exercise
    Mandatory. Exercise name as it appears in import.json (source name).

.PARAMETER CheckOnly
    Resolve the exercise (via map) and report workouts/sets that would import; no writes.

.PARAMETER ConfigPath
    JSON config with token, optional baseUrl, optional exerciseMap.

.PARAMETER ImportPath
    Path to import.json (default: scripts/import.json beside this script).

.PARAMETER Map
    Hashtable of importName → FitTrack name or UUID; merged over config exerciseMap.

.EXAMPLE
    .\scripts\Import-FitTrackWorkouts.ps1 -Exercise 'Dumbbell Hammer Curl' -CheckOnly

.EXAMPLE
    .\scripts\Import-FitTrackWorkouts.ps1 -Exercise 'Cable Face Pull' -ConfigPath .\scripts\import.config.json

.EXAMPLE
    .\scripts\Import-FitTrackWorkouts.ps1 -Exercise 'Barbell Bench Press' -Map @{
      'Barbell Bench Press' = 'Barbell Bench Press - Medium Grip'
    }
#>
[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [Parameter(Mandatory)]
    [string] $Exercise,

    [switch] $CheckOnly,

    [string] $ConfigPath,

    [string] $ImportPath,

    [hashtable] $Map
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptDir = $PSScriptRoot
if (-not $scriptDir) {
    $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
}

if (-not $ImportPath) {
    $ImportPath = Join-Path $scriptDir 'import.json'
}
if (-not $ConfigPath) {
    $ConfigPath = Join-Path $scriptDir 'import.config.json'
}

$moduleManifest = Join-Path $scriptDir 'FitTrack\FitTrack.psd1'
if (-not (Test-Path -LiteralPath $moduleManifest)) {
    throw "FitTrack module not found at $moduleManifest"
}
Import-Module $moduleManifest -Force

$KG_TO_LB = 2.2046226218

function Read-ImportConfig {
    param([string] $Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Config file not found: $Path (copy import.config.example.json and set token / exerciseMap)"
    }
    $raw = Get-Content -LiteralPath $Path -Raw -Encoding UTF8
    $cfg = $raw | ConvertFrom-Json
    if (-not $cfg.token) {
        throw "Config $Path must include a non-empty 'token' (JWT)."
    }
    $exerciseMap = @{}
    if ($cfg.exerciseMap) {
        foreach ($p in $cfg.exerciseMap.PSObject.Properties) {
            $exerciseMap[$p.Name] = [string]$p.Value
        }
    }
    [pscustomobject]@{
        BaseUrl     = if ($cfg.baseUrl) { [string]$cfg.baseUrl } else { 'http://localhost:8080' }
        Token       = [string]$cfg.token
        ExerciseMap = $exerciseMap
    }
}

function Merge-ExerciseMap {
    param(
        [hashtable] $ConfigMap,
        [hashtable] $OverrideMap
    )
    $merged = @{}
    if ($ConfigMap) {
        foreach ($k in $ConfigMap.Keys) { $merged[$k] = $ConfigMap[$k] }
    }
    if ($OverrideMap) {
        foreach ($k in $OverrideMap.Keys) { $merged[$k] = $OverrideMap[$k] }
    }
    return $merged
}

function ConvertTo-FitTrackInstant {
    param([Parameter(Mandatory)][string] $LocalText)

    $formats = @('yyyy-MM-dd HH:mm', 'yyyy-MM-dd H:mm', 'yyyy-MM-dd HH:mm:ss')
    $parsed = $null
    foreach ($fmt in $formats) {
        try {
            $parsed = [DateTime]::ParseExact(
                $LocalText.Trim(),
                $fmt,
                [Globalization.CultureInfo]::InvariantCulture,
                [Globalization.DateTimeStyles]::None
            )
            break
        }
        catch {
            # try next
        }
    }
    if (-not $parsed) {
        $parsed = [DateTime]::Parse($LocalText, [Globalization.CultureInfo]::InvariantCulture)
    }
    $local = [DateTime]::SpecifyKind($parsed, [DateTimeKind]::Local)
    return $local.ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ss.fffZ')
}

function ConvertTo-StorageWeightKg {
    param(
        $Weight,
        [string] $Unit
    )
    if ($null -eq $Weight -or $Weight -eq '') {
        return $null
    }
    $n = [double]$Weight
    $u = if ($Unit) { $Unit.Trim().ToLowerInvariant() } else { 'lbs' }
    if ($u -eq 'kg' -or $u -eq 'kgs') {
        return [math]::Round($n, 2)
    }
    # default / lbs
    return [math]::Round($n / $KG_TO_LB, 2)
}

function Resolve-FitTrackExercise {
    param(
        [Parameter(Mandatory)][string] $SourceName,
        [hashtable] $ExerciseMap,
        [hashtable] $Cache
    )

    if ($Cache.ContainsKey($SourceName)) {
        return $Cache[$SourceName]
    }

    $target = $SourceName
    if ($ExerciseMap -and $ExerciseMap.ContainsKey($SourceName)) {
        $target = [string]$ExerciseMap[$SourceName]
    }

    $resolved = $null
    $guidPattern = '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
    if ($target -match $guidPattern) {
        try {
            $resolved = Get-FitTrackExercise -Id $target
        }
        catch {
            $resolved = $null
        }
    }
    else {
        $page = Get-FitTrackExercise -Query $target -Size 100
        $items = @()
        if ($page -and $page.content) {
            $items = @($page.content)
        }
        $exact = @(
            $items | Where-Object { $_.name -and ($_.name.Trim().ToLowerInvariant() -eq $target.Trim().ToLowerInvariant()) }
        )
        if ($exact.Count -eq 1) {
            $resolved = $exact[0]
        }
        elseif ($exact.Count -gt 1) {
            throw "Multiple FitTrack exercises named '$target' (from source '$SourceName'). Map to a UUID instead."
        }
        elseif ($items.Count -eq 1 -and -not ($target.Contains(' '))) {
            # single fuzzy hit for a short query — still require exact name for safety
            $resolved = $null
        }
    }

    $result = [pscustomobject]@{
        SourceName   = $SourceName
        TargetKey    = $target
        Found        = [bool]$resolved
        ExerciseId   = if ($resolved) { $resolved.id } else { $null }
        ExerciseName = if ($resolved) { $resolved.name } else { $null }
    }
    $Cache[$SourceName] = $result
    return $result
}

function ConvertTo-WorkoutSetRequest {
    param(
        $Set,
        [string] $ExerciseId
    )

    $req = @{
        exerciseId = $ExerciseId
        setNumber  = [int]$Set.set
        completed  = $true
    }

    if ($null -ne $Set.reps -and $Set.reps -ne '') {
        $req.reps = [int]$Set.reps
    }
    if ($null -ne $Set.weight -and $Set.weight -ne '') {
        $unit = if ($Set.PSObject.Properties.Name -contains 'unit') { [string]$Set.unit } else { 'lbs' }
        $req.weightKg = ConvertTo-StorageWeightKg -Weight $Set.weight -Unit $unit
    }
    if ($null -ne $Set.duration -and $Set.duration -ne '') {
        $req.durationSeconds = [int]$Set.duration
    }
    if ($null -ne $Set.distance -and $Set.distance -ne '') {
        $req.distanceMeters = [double]$Set.distance
    }

    return $req
}

# --- main ---
$config = Read-ImportConfig -Path $ConfigPath
$exerciseMap = Merge-ExerciseMap -ConfigMap $config.ExerciseMap -OverrideMap $Map

Connect-FitTrack -Token $config.Token -BaseUrl $config.BaseUrl

if (-not (Test-Path -LiteralPath $ImportPath)) {
    throw "Import file not found: $ImportPath"
}
$workouts = Get-Content -LiteralPath $ImportPath -Raw -Encoding UTF8 | ConvertFrom-Json
if (-not $workouts) {
    throw "No workouts found in $ImportPath"
}

$cache = @{}
$resolution = Resolve-FitTrackExercise -SourceName $Exercise -ExerciseMap $exerciseMap -Cache $cache

Write-Host "Exercise filter (import name): $Exercise"
Write-Host "Maps to FitTrack key: $($resolution.TargetKey)"
if ($resolution.Found) {
    Write-Host "Resolved: $($resolution.ExerciseName) ($($resolution.ExerciseId))"
}
else {
    Write-Host "Resolved: NOT FOUND" -ForegroundColor Yellow
}

$candidates = @()
foreach ($w in @($workouts)) {
    $matchingSets = @($w.sets | Where-Object { $_.name -eq $Exercise })
    if ($matchingSets.Count -eq 0) {
        continue
    }
    $candidates += [pscustomobject]@{
        Workout     = $w
        MatchingSets = $matchingSets
    }
}

Write-Host "Workouts containing '$Exercise': $($candidates.Count)"
$totalSets = ($candidates | ForEach-Object { $_.MatchingSets.Count } | Measure-Object -Sum).Sum
if (-not $totalSets) { $totalSets = 0 }
Write-Host "Sets to process: $totalSets"

if ($CheckOnly) {
    if (-not $resolution.Found) {
        Write-Host "CHECK FAILED: exercise does not exist in FitTrack (after map)." -ForegroundColor Red
        exit 1
    }
    Write-Host "CHECK OK: exercise exists; $totalSets set(s) across $($candidates.Count) workout(s) would be imported." -ForegroundColor Green
    foreach ($c in $candidates) {
        Write-Host ("  - {0} ({1} set(s))" -f $c.Workout.name, $c.MatchingSets.Count)
    }
    exit 0
}

if (-not $resolution.Found) {
    throw "Cannot import: exercise '$Exercise' (→ '$($resolution.TargetKey)') not found in FitTrack. Fix exerciseMap or create the exercise."
}

# Index existing FitTrack workouts by name for incremental merge (unique per user).
$existingByName = @{}
foreach ($ew in @(Get-FitTrackWorkout)) {
    if ($ew.name) {
        $existingByName[$ew.name] = $ew.id
    }
}

$created = 0
$updated = 0
$skipped = 0
$failed = 0

foreach ($c in $candidates) {
    $w = $c.Workout
    $newSets = @(
        foreach ($s in $c.MatchingSets) {
            ConvertTo-WorkoutSetRequest -Set $s -ExerciseId $resolution.ExerciseId
        }
    )

    $existingId = $null
    if ($existingByName.ContainsKey($w.name)) {
        $existingId = $existingByName[$w.name]
    }

    $mergedSets = @()
    $priorNotes = $null
    if ($existingId) {
        $full = Get-FitTrackWorkout -Id $existingId
        $priorNotes = $full.notes
        foreach ($es in @($full.sets)) {
            if ($es.exerciseId -eq $resolution.ExerciseId) {
                continue
            }
            $keep = @{
                exerciseId = $es.exerciseId
                setNumber  = [int]$es.setNumber
                completed  = [bool]$es.completed
            }
            if ($null -ne $es.reps) { $keep.reps = [int]$es.reps }
            if ($null -ne $es.weightKg) { $keep.weightKg = [double]$es.weightKg }
            if ($null -ne $es.durationSeconds) { $keep.durationSeconds = [int]$es.durationSeconds }
            if ($null -ne $es.distanceMeters) { $keep.distanceMeters = [double]$es.distanceMeters }
            if ($es.rpe) { $keep.rpe = [string]$es.rpe }
            $mergedSets += $keep
        }
    }

    foreach ($ns in $newSets) {
        $mergedSets += $ns
    }

    $n = 1
    foreach ($sr in $mergedSets) {
        $sr.setNumber = $n
        $n++
    }

    $noteLine = "Imported sets for '$Exercise' from legacy export"
    $notes = $noteLine
    if ($priorNotes) {
        if ($priorNotes -notlike "*$noteLine*") {
            $notes = "$priorNotes`n$noteLine"
        }
        else {
            $notes = $priorNotes
        }
    }

    $body = @{
        name      = $w.name
        startedAt = ConvertTo-FitTrackInstant -LocalText $w.startTime
        endedAt   = ConvertTo-FitTrackInstant -LocalText $w.endTime
        completed = $true
        useMetric = $true
        notes     = $notes
        sets      = $mergedSets
    }

    $action = if ($existingId) { 'Update' } else { 'Create' }
    $targetLabel = "workout '$($w.name)' ($($newSets.Count) new set(s) for '$Exercise')"
    if (-not $PSCmdlet.ShouldProcess($targetLabel, "$action FitTrack workout")) {
        $skipped++
        continue
    }

    try {
        if ($existingId) {
            $result = Set-FitTrackWorkout -Id $existingId -Body $body -Confirm:$false
            Write-Host "Updated $($result.name) id=$($result.id) sets=$($result.setCount)" -ForegroundColor Cyan
            $updated++
        }
        else {
            $result = Set-FitTrackWorkout -Body $body -Confirm:$false
            Write-Host "Created $($result.name) id=$($result.id) sets=$($result.setCount)" -ForegroundColor Green
            $existingByName[$result.name] = $result.id
            $created++
        }
    }
    catch {
        Write-Host "FAILED $($w.name): $($_.Exception.Message)" -ForegroundColor Red
        $failed++
    }
}

Write-Host ""
Write-Host "Done. created=$created updated=$updated skipped=$skipped failed=$failed"
if ($failed -gt 0) {
    exit 1
}