# afterFileEdit: convert UTF-16 LE files to UTF-8 no BOM (fail open)
$ErrorActionPreference = 'Continue'
try {
  $json = [Console]::In.ReadToEnd()
  if ([string]::IsNullOrWhiteSpace($json)) { Write-Output '{}'; exit 0 }

  $payload = $null
  try { $payload = $json | ConvertFrom-Json } catch { Write-Output '{}'; exit 0 }

  $filePath = $payload.file_path
  if ([string]::IsNullOrWhiteSpace($filePath)) { Write-Output '{}'; exit 0 }
  if (-not (Test-Path -LiteralPath $filePath -PathType Leaf)) { Write-Output '{}'; exit 0 }

  $ext = [IO.Path]::GetExtension($filePath).ToLowerInvariant()
  $skipExt = @(
    '.png', '.jpg', '.jpeg', '.gif', '.webp', '.ico',
    '.db', '.jar', '.class', '.zip', '.gz',
    '.woff', '.woff2', '.ttf', '.eot', '.pdf',
    '.mp4', '.exe', '.dll', '.so'
  )
  if ($skipExt -contains $ext) { Write-Output '{}'; exit 0 }

  $bytes = [IO.File]::ReadAllBytes($filePath)
  if ($bytes.Length -lt 2) { Write-Output '{}'; exit 0 }

  $isUtf16 = $false
  if ($bytes[0] -eq 0xFF -and $bytes[1] -eq 0xFE) {
    $isUtf16 = $true
  } elseif ($bytes.Length -ge 4 -and $bytes[1] -eq 0 -and $bytes[3] -eq 0 -and $bytes[0] -ne 0) {
    $isUtf16 = $true
  }

  if (-not $isUtf16) { Write-Output '{}'; exit 0 }

  $text = [Text.Encoding]::Unicode.GetString($bytes)
  if ($text.Length -gt 0 -and [int][char]$text[0] -eq 0xFEFF) {
    $text = $text.Substring(1)
  }
  $utf8Enc = New-Object System.Text.UTF8Encoding $false
  [IO.File]::WriteAllText($filePath, $text, $utf8Enc)
  [Console]::Error.WriteLine("fix-utf8: converted UTF-16 -> UTF-8 no BOM: $filePath")
} catch {
  # fail open
}
Write-Output '{}'
exit 0