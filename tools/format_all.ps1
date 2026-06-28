$files = Get-ChildItem -Path "src" -Filter "*.java" -Recurse | Where-Object { !(Get-Content $_.FullName -Raw).Contains("SPDX-FileCopyrightText") }

$spdx1 = "// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales"
$spdx2 = "// SPDX-License-Identifier: LGPL-3.0-or-later"

foreach ($file in $files) {
    $lines = Get-Content $file.FullName
    $newLines = @()
    $insertedSpdx = $false

    foreach ($line in $lines) {
        # 1. Skip ASCII borders
        if ($line -match "^// [=\-]+$") {
            continue
        }

        # 2. Insert SPDX logic
        if (-not $insertedSpdx) {
            if ($line -match "^// Reading Order:") {
                $newLines += $line
                $newLines += $spdx1
                $newLines += $spdx2
                $insertedSpdx = $true
                continue
            } else {
                $newLines += $spdx1
                $newLines += $spdx2
                $insertedSpdx = $true
            }
        }

        # 3. Translate Spanish Author tags
        $modifiedLine = $line -replace "\* AUTORIDAD:.*", "* @author Marvin Alexander Flores Canales"
        $modifiedLine = $modifiedLine -replace "// Autor:.*", "// @author Marvin Alexander Flores Canales"
        $modifiedLine = $modifiedLine -replace "\* @author Marvin-Dev", "* @author Marvin Alexander Flores Canales"
        
        $newLines += $modifiedLine
    }

    Set-Content -Path $file.FullName -Value $newLines -Encoding UTF8
    Write-Host "Formatted: $($file.Name)"
}

Write-Host "Batch formatting complete."
