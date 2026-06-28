$files = Get-ChildItem -Path "src" -Filter "*.java" -Recurse
$utf8NoBom = New-Object System.Text.UTF8Encoding $false

foreach ($file in $files) {
    $content = [System.IO.File]::ReadAllText($file.FullName)
    [System.IO.File]::WriteAllText($file.FullName, $content, $utf8NoBom)
}
Write-Host "Purga de BOM (\ufeff) completada con exito."
