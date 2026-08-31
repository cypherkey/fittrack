@{
    RootModule        = 'FitTrack.psm1'
    ModuleVersion     = '0.1.4'
    GUID              = 'a7c3e9f1-4b2d-4e8a-9c1f-6d5e8a2b3c4d'
    Author            = 'FitTrack'
    Description       = 'PowerShell client for the FitTrack REST API (JWT Bearer auth).'
    PowerShellVersion = '5.1'
    FunctionsToExport = @(
        'Connect-FitTrack',
        'Disconnect-FitTrack',
        'Get-FitTrackSession',
        'Get-FitTrackMe',
        'Get-FitTrackExercise',
        'Get-FitTrackTemplate',
        'Get-FitTrackWorkout',
        'Set-FitTrackWorkout',
        'Set-FitTrackWorkoutSet',
        'Set-FitTrackExerciseFavorite'
    )
    CmdletsToExport   = @()
    VariablesToExport = @()
    AliasesToExport   = @()
}