<#
.SYNOPSIS
    Import workouts/sets from scripts/import.json into FitTrack (JWT via config).

.DESCRIPTION
    Uses scripts/FitTrack. Requires -Exercise (import-file exercise name). Only sets for
    that exercise are imported (after optional name→FitTrack map). Weights in lbs are
    converted to kg for the API. When a FitTrack workout with the same name already
    exists, sets are appended to it (other exercises kept). Use -Force to replace sets
    for this exercise if they are already present.

.PARAMETER ValidateToken
    Test the configured JWT against GET /api/v1/me and print the authenticated
    user's display name and email. No import or writes. -Exercise is not required.

.PARAMETER Exercise
    Mandatory. Exercise name as it appears in import.json (source name).

.PARAMETER CheckOnly
    Resolve the exercise (via map), verify import sets match FitTrack trackedParameters;
    no writes.

.PARAMETER ConfigPath
    JSON config with token, optional baseUrl, optional exerciseMap.

.PARAMETER ImportPath
    Path to import.json (default: scripts/import.json beside this script).

.PARAMETER Map
    Hashtable of importName → FitTrack name or UUID; merged over config exerciseMap.

.PARAMETER Force
    When a FitTrack workout already has sets for this exercise, replace them.
    Without -Force those workouts are skipped with a warning.

.EXAMPLE
    .\scripts\Import-FitTrackWorkouts.ps1 -ValidateToken

.EXAMPLE
    .\scripts\Import-FitTrackWorkouts.ps1 -Exercise 'Dumbbell Hammer Curl' -CheckOnly

.EXAMPLE
    .\scripts\Import-FitTrackWorkouts.ps1 -Exercise 'Cable Face Pull' -ConfigPath .\scripts\import.config.json

.EXAMPLE
    .\scripts\Import-FitTrackWorkouts.ps1 -Exercise 'Barbell Bench Press' -Force -Map @{
      'Barbell Bench Press' = 'Barbell Bench Press - Medium Grip'
    }
#>
[CmdletBinding(SupportsShouldProcess = $true, DefaultParameterSetName = 'Import')]
param(
    [Parameter(ParameterSetName = 'Import', Mandatory)]
    [string] $Exercise,

    [Parameter(ParameterSetName = 'ValidateToken')]
    [switch] $ValidateToken,

    [switch] $CheckOnly,

    [string] $ConfigPath,

    [string] $ImportPath,

    [hashtable] $Map,

    [switch] $Force
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

function Show-AuthenticatedUser {
    <#
    .SYNOPSIS
        Call GET /api/v1/me and print display name and email.
    #>
    try {
        $me = Get-FitTrackMe
    }
    catch {
        throw "Token validation failed: $($_.Exception.Message)"
    }

    $displayName = Get-ObjectPropertyValue -Object $me -Name 'displayName'
    $email = Get-ObjectPropertyValue -Object $me -Name 'email'
    $username = Get-ObjectPropertyValue -Object $me -Name 'username'
    $name = if ($displayName) { [string]$displayName } elseif ($username) { [string]$username } else { '(unknown)' }
    $emailText = if ($email) { [string]$email } else { '(no email)' }

    Write-Host "Authenticated user: $name"
    Write-Host "Email: $emailText"
    return $me
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
        ExerciseId         = if ($resolved) { $resolved.id } else { $null }
        ExerciseName       = if ($resolved) { $resolved.name } else { $null }
        TrackedParameters  = if ($resolved -and ($null -ne $resolved.trackedParameters)) {
            [int]$resolved.trackedParameters
        } else {
            $null
        }
    }
    $Cache[$SourceName] = $result
    return $result
}

function Get-OptionalSetProperty {
    param(
        $Set,
        [Parameter(Mandatory)][string] $Name
    )
    $prop = $Set.PSObject.Properties[$Name]
    if ($null -eq $prop) {
        return $null
    }
    return $prop.Value
}

function Get-ObjectPropertyValue {
    param(
        $Object,
        [Parameter(Mandatory)][string] $Name
    )
    if ($null -eq $Object) {
        return $null
    }
    $prop = $Object.PSObject.Properties[$Name]
    if ($null -eq $prop) {
        return $null
    }
    return $prop.Value
}

function Normalize-WorkoutName {
    param([AllowNull()][string] $Name)
    if ($null -eq $Name) {
        return $null
    }
    return $Name.Trim()
}

function ConvertTo-FitTrackItemArray {
    <#
    .SYNOPSIS
        Normalize list cmdlet output to a flat object array (handles Object[] wrapping).
    #>
    param($InputObject)
    if ($null -eq $InputObject) {
        return @()
    }
    if ($InputObject -is [System.Array]) {
        return $InputObject
    }
    return @($InputObject)
}

function Build-FitTrackWorkoutNameIndex {
    <#
    .SYNOPSIS
        Map normalized workout name → id from GET /api/v1/workouts.
    #>
    $index = @{}
    $listed = ConvertTo-FitTrackItemArray (Get-FitTrackWorkout)
    foreach ($ew in $listed) {
        $name = Normalize-WorkoutName (Get-ObjectPropertyValue -Object $ew -Name 'name')
        $id = Get-ObjectPropertyValue -Object $ew -Name 'id'
        if ($name -and $id) {
            $index[$name] = [string]$id
        }
    }
    # Comma prevents PowerShell from enumerating the hashtable into DictionaryEntry output.
    return ,$index
}

function Find-FitTrackWorkoutIdByName {
    param(
        [Parameter(Mandatory)][string] $Name,
        [Parameter(Mandatory)][hashtable] $Index,
        [switch] $Refresh
    )
    $key = Normalize-WorkoutName $Name
    if (-not $key) {
        return $null
    }
    if ($Index.ContainsKey($key)) {
        return [string]$Index[$key]
    }
    if (-not $Refresh) {
        return $null
    }

    $fresh = Build-FitTrackWorkoutNameIndex
    $Index.Clear()
    foreach ($k in @($fresh.Keys)) {
        $Index[$k] = $fresh[$k]
    }
    if ($Index.ContainsKey($key)) {
        return [string]$Index[$key]
    }
    return $null
}

function ConvertTo-KeptSetRequest {
    param($ExistingSet)
    $keep = @{
        exerciseId = [string]$ExistingSet.exerciseId
        setNumber  = [int]$ExistingSet.setNumber
        completed  = [bool]$ExistingSet.completed
    }
    if ($null -ne $ExistingSet.reps) { $keep.reps = [int]$ExistingSet.reps }
    if ($null -ne $ExistingSet.weightKg) { $keep.weightKg = [double]$ExistingSet.weightKg }
    if ($null -ne $ExistingSet.durationSeconds) { $keep.durationSeconds = [int]$ExistingSet.durationSeconds }
    if ($null -ne $ExistingSet.distanceMeters) { $keep.distanceMeters = [double]$ExistingSet.distanceMeters }
    if ($ExistingSet.rpe) { $keep.rpe = [string]$ExistingSet.rpe }
    return $keep
}

function Test-ApiConflictError {
    param([Parameter(Mandatory)][string] $Message)
    return ($Message -match '\(409\)' -or $Message -match '(?i)\bconflict\b')
}

# FitTrack TrackedParameters bit flags (backend TrackedParameters.java)
$script:TP_REPS = 1
$script:TP_WEIGHT = 2
$script:TP_DURATION = 4
$script:TP_DISTANCE = 8

function Format-TrackedParameters {
    param([int] $Flags)
    $labels = @()
    if (($Flags -band $script:TP_REPS) -ne 0) { $labels += 'reps' }
    if (($Flags -band $script:TP_WEIGHT) -ne 0) { $labels += 'weight' }
    if (($Flags -band $script:TP_DURATION) -ne 0) { $labels += 'duration' }
    if (($Flags -band $script:TP_DISTANCE) -ne 0) { $labels += 'distance' }
    if ($labels.Count -eq 0) { return '(none)' }
    return ($labels -join ', ')
}

function Test-HasImportValue {
    param($Value)
    return ($null -ne $Value -and "$Value" -ne '')
}

function Test-ImportSetTrackedMatch {
    <#
    .SYNOPSIS
        Compare one import.json set to FitTrack trackedParameters flags.
    .OUTPUTS
        Object with Missing (required but absent) and Extra (present but not tracked).
    #>
    param(
        $Set,
        [Parameter(Mandatory)][int] $TrackedParameters
    )

    $present = @{
        reps     = (Test-HasImportValue (Get-OptionalSetProperty -Set $Set -Name 'reps'))
        weight   = (Test-HasImportValue (Get-OptionalSetProperty -Set $Set -Name 'weight'))
        duration = (Test-HasImportValue (Get-OptionalSetProperty -Set $Set -Name 'duration'))
        distance = (Test-HasImportValue (Get-OptionalSetProperty -Set $Set -Name 'distance'))
    }

    $required = @{
        reps     = (($TrackedParameters -band $script:TP_REPS) -ne 0)
        weight   = (($TrackedParameters -band $script:TP_WEIGHT) -ne 0)
        duration = (($TrackedParameters -band $script:TP_DURATION) -ne 0)
        distance = (($TrackedParameters -band $script:TP_DISTANCE) -ne 0)
    }

    $missing = @()
    $extra = @()
    foreach ($key in @('reps', 'weight', 'duration', 'distance')) {
        if ($required[$key] -and -not $present[$key]) {
            $missing += $key
        }
        if (-not $required[$key] -and $present[$key]) {
            $extra += $key
        }
    }

    [pscustomobject]@{
        SetNumber = [int]$Set.set
        Missing   = $missing
        Extra     = $extra
        Ok        = ($missing.Count -eq 0 -and $extra.Count -eq 0)
    }
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

    $reps = Get-OptionalSetProperty -Set $Set -Name 'reps'
    if ($null -ne $reps -and $reps -ne '') {
        $req.reps = [int]$reps
    }

    $weight = Get-OptionalSetProperty -Set $Set -Name 'weight'
    if ($null -ne $weight -and $weight -ne '') {
        $unit = Get-OptionalSetProperty -Set $Set -Name 'unit'
        if (-not $unit) { $unit = 'lbs' }
        $req.weightKg = ConvertTo-StorageWeightKg -Weight $weight -Unit ([string]$unit)
    }

    $duration = Get-OptionalSetProperty -Set $Set -Name 'duration'
    if ($null -ne $duration -and $duration -ne '') {
        $req.durationSeconds = [int]$duration
    }

    $distance = Get-OptionalSetProperty -Set $Set -Name 'distance'
    if ($null -ne $distance -and $distance -ne '') {
        $req.distanceMeters = [double]$distance
    }

    return $req
}

# --- main ---
$config = Read-ImportConfig -Path $ConfigPath
$exerciseMap = Merge-ExerciseMap -ConfigMap $config.ExerciseMap -OverrideMap $Map

Connect-FitTrack -Token $config.Token -BaseUrl $config.BaseUrl

if ($ValidateToken) {
    Show-AuthenticatedUser
    Write-Host "Token is valid." -ForegroundColor Green
    exit 0
}

Show-AuthenticatedUser

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
    if ($null -eq $resolution.TrackedParameters) {
        Write-Host "CHECK FAILED: resolved exercise has no trackedParameters." -ForegroundColor Red
        exit 1
    }

    $flags = [int]$resolution.TrackedParameters
    Write-Host "FitTrack tracked parameters ($flags): $(Format-TrackedParameters -Flags $flags)"

    $issueCount = 0
    foreach ($c in $candidates) {
        Write-Host ("  - {0} ({1} set(s))" -f $c.Workout.name, $c.MatchingSets.Count)
        foreach ($s in $c.MatchingSets) {
            $match = Test-ImportSetTrackedMatch -Set $s -TrackedParameters $flags
            if ($match.Ok) {
                continue
            }
            $issueCount++
            $parts = @()
            if ($match.Missing.Count -gt 0) {
                $parts += ("missing required: {0}" -f ($match.Missing -join ', '))
            }
            if ($match.Extra.Count -gt 0) {
                $parts += ("extra (not tracked): {0}" -f ($match.Extra -join ', '))
            }
            Write-Host ("      set {0}: {1}" -f $match.SetNumber, ($parts -join '; ')) -ForegroundColor Yellow
        }
    }

    if ($issueCount -gt 0) {
        Write-Host "CHECK FAILED: $issueCount set(s) do not match FitTrack tracked parameters." -ForegroundColor Red
        exit 1
    }

    Write-Host "CHECK OK: exercise exists; $totalSets set(s) across $($candidates.Count) workout(s) match tracked parameters." -ForegroundColor Green
    exit 0
}

if (-not $resolution.Found) {
    throw "Cannot import: exercise '$Exercise' (→ '$($resolution.TargetKey)') not found in FitTrack. Fix exerciseMap or create the exercise."
}

# Index existing FitTrack workouts by name for incremental merge (unique per user).
$existingByName = Build-FitTrackWorkoutNameIndex
Write-Host "Existing FitTrack workouts indexed by name: $($existingByName.Count)"

$created = 0
$updated = 0
$skipped = 0
$failed = 0

foreach ($c in $candidates) {
    $w = $c.Workout
    $workoutName = Normalize-WorkoutName $w.name
    $newSets = @(
        foreach ($s in $c.MatchingSets) {
            ConvertTo-WorkoutSetRequest -Set $s -ExerciseId $resolution.ExerciseId
        }
    )

    $existingId = Find-FitTrackWorkoutIdByName -Name $workoutName -Index $existingByName

    $mergedSets = @()
    $priorNotes = $null
    if ($existingId) {
        $full = Get-FitTrackWorkout -Id $existingId
        $priorNotes = Get-ObjectPropertyValue -Object $full -Name 'notes'
        $existingForExercise = @(
            @($full.sets) | Where-Object {
                (Get-ObjectPropertyValue -Object $_ -Name 'exerciseId') -eq $resolution.ExerciseId
            }
        )
        if ($existingForExercise.Count -gt 0 -and -not $Force) {
            Write-Warning ("Skipping '{0}': already has {1} set(s) for '{2}'. Re-run with -Force to overwrite." -f `
                $workoutName, $existingForExercise.Count, $Exercise)
            $skipped++
            continue
        }
        foreach ($es in @($full.sets)) {
            if ((Get-ObjectPropertyValue -Object $es -Name 'exerciseId') -eq $resolution.ExerciseId) {
                continue
            }
            $mergedSets += ConvertTo-KeptSetRequest -ExistingSet $es
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
        name      = $workoutName
        startedAt = ConvertTo-FitTrackInstant -LocalText $w.startTime
        endedAt   = ConvertTo-FitTrackInstant -LocalText $w.endTime
        completed = $true
        useMetric = $true
        notes     = $notes
        sets      = $mergedSets
    }

    $action = if ($existingId) { 'Update' } else { 'Create' }
    $targetLabel = "workout '$workoutName' ($($newSets.Count) new set(s) for '$Exercise')"
    if (-not $PSCmdlet.ShouldProcess($targetLabel, "$action FitTrack workout")) {
        $skipped++
        continue
    }

    try {
        if ($existingId) {
            $result = Set-FitTrackWorkout -Id $existingId -Body $body -Confirm:$false
            Write-Host "Updated $($result.name) id=$($result.id) sets=$($result.setCount) (appended '$Exercise')" -ForegroundColor Cyan
            $updated++
            continue
        }

        try {
            $result = Set-FitTrackWorkout -Body $body -Confirm:$false
            Write-Host "Created $($result.name) id=$($result.id) sets=$($result.setCount)" -ForegroundColor Green
            $existingByName[(Normalize-WorkoutName $result.name)] = [string]$result.id
            $created++
            continue
        }
        catch {
            if (-not (Test-ApiConflictError -Message $_.Exception.Message)) {
                throw
            }
            # Name already exists but was missing from the index — resolve and append.
            $existingId = Find-FitTrackWorkoutIdByName -Name $workoutName -Index $existingByName -Refresh
            if (-not $existingId) {
                throw "Workout '$workoutName' returned 409 Conflict but could not be found by name after refresh."
            }
            Write-Host "Workout '$workoutName' already exists (id=$existingId); appending sets for '$Exercise'." -ForegroundColor Yellow

            $full = Get-FitTrackWorkout -Id $existingId
            $priorNotes = Get-ObjectPropertyValue -Object $full -Name 'notes'
            $existingForExercise = @(
                @($full.sets) | Where-Object {
                    (Get-ObjectPropertyValue -Object $_ -Name 'exerciseId') -eq $resolution.ExerciseId
                }
            )
            if ($existingForExercise.Count -gt 0 -and -not $Force) {
                Write-Warning ("Skipping '{0}': already has {1} set(s) for '{2}'. Re-run with -Force to overwrite." -f `
                    $workoutName, $existingForExercise.Count, $Exercise)
                $skipped++
                continue
            }

            $mergedSets = @()
            foreach ($es in @($full.sets)) {
                if ((Get-ObjectPropertyValue -Object $es -Name 'exerciseId') -eq $resolution.ExerciseId) {
                    continue
                }
                $mergedSets += ConvertTo-KeptSetRequest -ExistingSet $es
            }
            foreach ($ns in $newSets) {
                $mergedSets += $ns
            }
            $n = 1
            foreach ($sr in $mergedSets) {
                $sr.setNumber = $n
                $n++
            }

            $notes = $noteLine
            if ($priorNotes) {
                if ($priorNotes -notlike "*$noteLine*") {
                    $notes = "$priorNotes`n$noteLine"
                }
                else {
                    $notes = $priorNotes
                }
            }
            $body.notes = $notes
            $body.sets = $mergedSets

            $result = Set-FitTrackWorkout -Id $existingId -Body $body -Confirm:$false
            Write-Host "Updated $($result.name) id=$($result.id) sets=$($result.setCount) (appended '$Exercise')" -ForegroundColor Cyan
            $updated++
        }
    }
    catch {
        Write-Host "FAILED $($workoutName): $($_.Exception.Message)" -ForegroundColor Red
        $failed++
    }
}

Write-Host ""
Write-Host "Done. created=$created updated=$updated skipped=$skipped failed=$failed"
if ($failed -gt 0) {
    exit 1
}