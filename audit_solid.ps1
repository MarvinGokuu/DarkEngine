$reportFile = "C:\Users\theca\.gemini\antigravity-ide\brain\ec97e546-16d9-46d2-81a3-4cdcf53c0c64\resultado_auditoria_solid.md"
$javaFiles = Get-ChildItem -Path "src" -Filter "*.java" -Recurse

foreach ($file in $javaFiles) {
    $lines = Get-Content $file.FullName
    $lineCount = $lines.Count
    $packageName = $file.Directory.Name
    $className = $file.Name

    # Skip already audited packages
    if ($packageName -match "admin|config|bus|audio") {
        continue
    }

    $violations = @()

    # SRP Check
    if ($lineCount -gt 400) {
        $violations += "- **Violación (SRP):** Clase masiva ($lineCount líneas). Posible 'God Class'.`n  - **Solución Propuesta:** Extraer responsabilidades en clases más pequeñas."
    }

    # Line by line checks
    for ($i = 0; $i -lt $lineCount; $i++) {
        $line = $lines[$i]
        $num = $i + 1

        # DIP Check
        if ($line -match "invokeExact" -and $packageName -notmatch "rhi") {
            $violations += "- **Violación (DIP):** Llamada FFI nativa cruda en lógica de alto nivel.`n  - **Línea del problema:** L$num ($($line.Trim()))`n  - **Solución Propuesta:** Abstraer detrás de una interfaz (e.g. RHI, AHI, PlatformInterface)."
        }

        # OCP Check
        if ($line -match "instanceof") {
            $violations += "- **Violación (OCP):** Uso de 'instanceof' rompe polimorfismo.`n  - **Línea del problema:** L$num ($($line.Trim()))`n  - **Solución Propuesta:** Usar despacho dinámico (polimorfismo) o patrón visitante."
        }

        # LSP Check
        if ($line -match "UnsupportedOperationException") {
            $violations += "- **Violación (LSP):** Lanza UnsupportedOperationException, rompiendo el contrato de la interfaz base.`n  - **Línea del problema:** L$num ($($line.Trim()))`n  - **Solución Propuesta:** Segregar interfaces (ISP) para no forzar implementación de métodos no soportados."
        }
    }

    if ($violations.Count -gt 0) {
        Add-Content -Path $reportFile -Value "`n## Paquete: sv.dark.$packageName"
        Add-Content -Path $reportFile -Value "### $className"
        
        # Deduplicate to avoid huge files if many invokeExacts
        $uniqueViolations = $violations | Select-Object -Unique
        foreach ($v in $uniqueViolations) {
            Add-Content -Path $reportFile -Value $v
        }
    }
}

Write-Host "Audit Complete"
