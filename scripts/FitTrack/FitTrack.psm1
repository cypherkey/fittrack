# FitTrack API PowerShell client.
# Keep in sync with REST: see docs/POWERSHELL.md (and FRONTEND.md §8 for DTO field names).
# Auth: JWT Bearer token via Connect-FitTrack.
#
# Import-Module .\scripts\FitTrack\FitTrack.psd1
# Connect-FitTrack -Token '<jwt>' -BaseUrl 'http://localhost:8080'
# Get-FitTrackExercise -Query 'bench'
# Get-FitTrackTemplate
# Get-FitTrackWorkout -Id '<uuid>'
# Set-FitTrackWorkout -Body @{ name = 'Session'; sets = @(@{ exerciseId = '...'; setNumber = 1; reps = 8 }) }
# Set-FitTrackWorkoutSet -WorkoutId '...' -SetId '...' -Reps 10 -WeightKg 40 -Completed $true

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:FitTrackBaseUrl = 'http://localhost:8080'
$script:FitTrackToken = $null

function Connect-FitTrack {
    <#
    .SYNOPSIS
        Configure FitTrack API base URL and JWT Bearer token for this session.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [string] $Token,

        [string] $BaseUrl = 'http://localhost:8080'
    )

    $script:FitTrackBaseUrl = $BaseUrl.TrimEnd('/')
    $script:FitTrackToken = $Token.Trim()
    if (-not $script:FitTrackToken) {
        throw 'Token must not be empty.'
    }
}

function Disconnect-FitTrack {
    <#
    .SYNOPSIS
        Clear the session JWT (keeps last BaseUrl).
    #>
    [CmdletBinding()]
    param()

    $script:FitTrackToken = $null
}

function Get-FitTrackSession {
    <#
    .SYNOPSIS
        Show current module session (token is redacted).
    #>
    [CmdletBinding()]
    param()

    [pscustomobject]@{
        BaseUrl     = $script:FitTrackBaseUrl
        HasToken    = [bool]$script:FitTrackToken
        TokenPrefix = if ($script:FitTrackToken -and $script:FitTrackToken.Length -gt 12) {
            $script:FitTrackToken.Substring(0, 12) + '...'
        } elseif ($script:FitTrackToken) {
            '***'
        } else {
            $null
        }
    }
}

function Assert-FitTrackConnected {
    if (-not $script:FitTrackToken) {
        throw 'Not connected. Call Connect-FitTrack -Token <jwt> first.'
    }
}

function Invoke-FitTrackApi {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [ValidateSet('GET', 'POST', 'PUT', 'PATCH', 'DELETE')]
        [string] $Method,

        [Parameter(Mandatory)]
        [string] $Path,

        [hashtable] $Query,

        [object] $Body
    )

    Assert-FitTrackConnected

    $uriBuilder = [System.UriBuilder]"$($script:FitTrackBaseUrl)$Path"
    if ($Query -and $Query.Count -gt 0) {
        $pairs = foreach ($key in $Query.Keys) {
            $value = $Query[$key]
            if ($null -eq $value -or ($value -is [string] -and [string]::IsNullOrWhiteSpace($value))) {
                continue
            }
            if ($value -is [bool]) {
                $value = if ($value) { 'true' } else { 'false' }
            }
            '{0}={1}' -f [uri]::EscapeDataString([string]$key), [uri]::EscapeDataString([string]$value)
        }
        $uriBuilder.Query = ($pairs -join '&')
    }

    $headers = @{
        Authorization = "Bearer $($script:FitTrackToken)"
        Accept        = 'application/json'
    }

    $params = @{
        Method  = $Method
        Uri     = $uriBuilder.Uri.AbsoluteUri
        Headers = $headers
    }

    if ($PSBoundParameters.ContainsKey('Body')) {
        $params.ContentType = 'application/json; charset=utf-8'
        if ($null -eq $Body) {
            $params.Body = 'null'
        } elseif ($Body -is [string]) {
            $params.Body = $Body
        } else {
            $params.Body = ConvertTo-FitTrackJson -InputObject $Body
        }
    }

    try {
        return Invoke-RestMethod @params
    }
    catch {
        $message = $_.Exception.Message
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            $message = $_.ErrorDetails.Message
        }
        throw "FitTrack API $Method $($uriBuilder.Path) failed: $message"
    }
}

function ConvertTo-FitTrackJson {
    param([Parameter(Mandatory)][object] $InputObject)

    # Depth high enough for workout sets; PowerShell 5.1 defaults to 2.
    return ($InputObject | ConvertTo-Json -Depth 20 -Compress)
}

function Get-FitTrackExercise {
    <#
    .SYNOPSIS
        Get one exercise by id, or list/search exercises.
    .EXAMPLE
        Get-FitTrackExercise -Id '4141e1e4-90b0-4c1d-89ef-c7eb1652bf56'
    .EXAMPLE
        Get-FitTrackExercise -Query 'bench' -Size 20
    #>
    [CmdletBinding(DefaultParameterSetName = 'List')]
    param(
        [Parameter(ParameterSetName = 'ById', Mandatory, Position = 0)]
        [string] $Id,

        [Parameter(ParameterSetName = 'List')]
        [string] $Query,

        [Parameter(ParameterSetName = 'List')]
        [string] $Muscle,

        [Parameter(ParameterSetName = 'List')]
        [string] $Equipment,

        [Parameter(ParameterSetName = 'List')]
        [string] $Category,

        [Parameter(ParameterSetName = 'List')]
        [switch] $CustomOnly,

        [Parameter(ParameterSetName = 'List')]
        [int] $Page = 0,

        [Parameter(ParameterSetName = 'List')]
        [int] $Size = 50
    )

    if ($PSCmdlet.ParameterSetName -eq 'ById') {
        return Invoke-FitTrackApi -Method GET -Path "/api/v1/exercise/$Id"
    }

    $queryParams = @{
        page = $Page
        size = $Size
    }
    if ($Query) { $queryParams.q = $Query }
    if ($Muscle) { $queryParams.muscle = $Muscle }
    if ($Equipment) { $queryParams.equipment = $Equipment }
    if ($Category) { $queryParams.category = $Category }
    if ($CustomOnly) { $queryParams.customOnly = $true }

    return Invoke-FitTrackApi -Method GET -Path '/api/v1/exercise' -Query $queryParams
}

function Get-FitTrackTemplate {
    <#
    .SYNOPSIS
        Get one template by id, or list templates.
    .EXAMPLE
        Get-FitTrackTemplate -Id '<uuid>'
    .EXAMPLE
        Get-FitTrackTemplate -Visibility PUBLIC
    #>
    [CmdletBinding(DefaultParameterSetName = 'List')]
    param(
        [Parameter(ParameterSetName = 'ById', Mandatory, Position = 0)]
        [string] $Id,

        [Parameter(ParameterSetName = 'List')]
        [ValidateSet('PRIVATE', 'PUBLIC')]
        [string] $Visibility
    )

    if ($PSCmdlet.ParameterSetName -eq 'ById') {
        return Invoke-FitTrackApi -Method GET -Path "/api/v1/templates/$Id"
    }

    $queryParams = @{}
    if ($Visibility) {
        $queryParams.visibility = $Visibility
    }
    return Invoke-FitTrackApi -Method GET -Path '/api/v1/templates' -Query $queryParams
}

function Get-FitTrackWorkout {
    <#
    .SYNOPSIS
        Get one workout (with sets) by id, or list workouts.
    .EXAMPLE
        Get-FitTrackWorkout -Id '<uuid>'
    .EXAMPLE
        Get-FitTrackWorkout -From '2026-08-01T00:00:00Z' -To '2026-08-31T23:59:59Z'
    #>
    [CmdletBinding(DefaultParameterSetName = 'List')]
    param(
        [Parameter(ParameterSetName = 'ById', Mandatory, Position = 0)]
        [string] $Id,

        [Parameter(ParameterSetName = 'List')]
        [string] $From,

        [Parameter(ParameterSetName = 'List')]
        [string] $To
    )

    if ($PSCmdlet.ParameterSetName -eq 'ById') {
        return Invoke-FitTrackApi -Method GET -Path "/api/v1/workouts/$Id"
    }

    $queryParams = @{}
    if ($From) { $queryParams.from = $From }
    if ($To) { $queryParams.to = $To }
    return Invoke-FitTrackApi -Method GET -Path '/api/v1/workouts' -Query $queryParams
}

function Set-FitTrackWorkout {
    <#
    .SYNOPSIS
        Create or replace a workout (including sets).
    .DESCRIPTION
        Without -Id, POSTs a new workout. With -Id, PUTs a full update (sets replace).
        -Body should match WorkoutRequest JSON (startedAt, endedAt, name, completed,
        useMetric, difficulty, notes, sourceTemplateId, sets[]).
        Each set: exerciseId, setNumber, optional reps/weightKg/durationSeconds/
        distanceMeters/completed/rpe.
    .EXAMPLE
        Set-FitTrackWorkout -Body @{
            name = 'Push'
            sets = @(
                @{ exerciseId = '4141e1e4-90b0-4c1d-89ef-c7eb1652bf56'; setNumber = 1; reps = 8; weightKg = 60 }
            )
        }
    .EXAMPLE
        $w = Get-FitTrackWorkout -Id $id
        $body = @{
            startedAt = $w.startedAt
            endedAt = $w.endedAt
            name = $w.name
            completed = $w.completed
            useMetric = $w.useMetric
            difficulty = $w.difficulty
            notes = $w.notes
            sourceTemplateId = $w.sourceTemplateId
            sets = @(
                $w.sets | ForEach-Object {
                    @{
                        exerciseId = $_.exerciseId
                        setNumber = $_.setNumber
                        reps = $_.reps
                        weightKg = $_.weightKg
                        durationSeconds = $_.durationSeconds
                        distanceMeters = $_.distanceMeters
                        completed = $_.completed
                        rpe = $_.rpe
                    }
                }
            )
        }
        Set-FitTrackWorkout -Id $id -Body $body
    #>
    [CmdletBinding(SupportsShouldProcess)]
    param(
        [string] $Id,

        [Parameter(Mandatory)]
        [object] $Body
    )

    if (-not ($Body.PSObject.Properties.Name -contains 'sets') -and -not ($Body -is [hashtable] -and $Body.ContainsKey('sets'))) {
        throw 'Body must include a sets array (may be empty).'
    }

    if ($Id) {
        if ($PSCmdlet.ShouldProcess("workout/$Id", 'PUT')) {
            return Invoke-FitTrackApi -Method PUT -Path "/api/v1/workouts/$Id" -Body $Body
        }
    }
    else {
        if ($PSCmdlet.ShouldProcess('workouts', 'POST')) {
            return Invoke-FitTrackApi -Method POST -Path '/api/v1/workouts' -Body $Body
        }
    }
}

function Set-FitTrackWorkoutSet {
    <#
    .SYNOPSIS
        Partially update one workout set (PATCH).
    .DESCRIPTION
        Only provided fields are sent. Pass $null explicitly to clear nullable fields
        (reps, weightKg, durationSeconds, distanceMeters, rpe).
    .EXAMPLE
        Set-FitTrackWorkoutSet -WorkoutId $w -SetId $s -Reps 10 -WeightKg 42.5 -Completed $true
    .EXAMPLE
        Set-FitTrackWorkoutSet -WorkoutId $w -SetId $s -Rpe HARD
    #>
    [CmdletBinding(SupportsShouldProcess)]
    param(
        [Parameter(Mandatory)]
        [string] $WorkoutId,

        [Parameter(Mandatory)]
        [string] $SetId,

        [bool] $Completed,

        [AllowNull()]
        [Nullable[int]] $Reps,

        [AllowNull()]
        [Nullable[double]] $WeightKg,

        [AllowNull()]
        [Nullable[int]] $DurationSeconds,

        [AllowNull()]
        [Nullable[double]] $DistanceMeters,

        [ValidateSet('EASY', 'CHALLENGING', 'HARD')]
        [AllowNull()]
        [string] $Rpe
    )

    $patch = @{}
    if ($PSBoundParameters.ContainsKey('Completed')) {
        $patch.completed = [bool]$Completed
    }
    if ($PSBoundParameters.ContainsKey('Reps')) {
        $patch.reps = if ($null -eq $Reps) { $null } else { [int]$Reps }
    }
    if ($PSBoundParameters.ContainsKey('WeightKg')) {
        $patch.weightKg = if ($null -eq $WeightKg) { $null } else { [double]$WeightKg }
    }
    if ($PSBoundParameters.ContainsKey('DurationSeconds')) {
        $patch.durationSeconds = if ($null -eq $DurationSeconds) { $null } else { [int]$DurationSeconds }
    }
    if ($PSBoundParameters.ContainsKey('DistanceMeters')) {
        $patch.distanceMeters = if ($null -eq $DistanceMeters) { $null } else { [double]$DistanceMeters }
    }
    if ($PSBoundParameters.ContainsKey('Rpe')) {
        $patch.rpe = $Rpe
    }

    if ($patch.Count -eq 0) {
        throw 'Provide at least one field to update (Completed, Reps, WeightKg, DurationSeconds, DistanceMeters, Rpe).'
    }

    if ($PSCmdlet.ShouldProcess("workout/$WorkoutId/sets/$SetId", 'PATCH')) {
        return Invoke-FitTrackApi -Method PATCH -Path "/api/v1/workouts/$WorkoutId/sets/$SetId" -Body $patch
    }
}

Export-ModuleMember -Function @(
    'Connect-FitTrack',
    'Disconnect-FitTrack',
    'Get-FitTrackSession',
    'Get-FitTrackExercise',
    'Get-FitTrackTemplate',
    'Get-FitTrackWorkout',
    'Set-FitTrackWorkout',
    'Set-FitTrackWorkoutSet'
)