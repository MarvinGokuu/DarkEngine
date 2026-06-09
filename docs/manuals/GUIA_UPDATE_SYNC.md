# Script de Sincronización - Dark Engine

## Autoridad

**Documento**: Guía de Sincronización de Archivos  
**Nivel**: Documentation Engineering  
**Versión**: 1.0  
**Fecha**: 2026-01-05  
**Autor**: Marvin-Dev  
**Propósito**: Documentar el uso del script update.bat para sincronización de archivos y documentación

---

## Descripción

El script `update.bat` es una herramienta de sincronización que verifica la consistencia entre la documentación y los archivos del proyecto.

---

## Uso

### Ejecución Básica

```batch
cd c:\Users\theca\Documents\GitHub\DarkEngine
update.bat
```

### Salida Esperada

```
═══════════════════════════════════════════════════════════
  DARK ENGINE - DOCUMENTATION SYNC TOOL
═══════════════════════════════════════════════════════════

[1/5] Verificando estructura de directorios...
[OK] Estructura de directorios validada

[2/5] Listando archivos de documentacion...
[OK] Encontrados 13 archivos de documentacion

[3/5] Verificando archivos criticos...
[OK] AAA_CERTIFICATION.md
[OK] AAA_CODING_STANDARDS.md
[OK] ESTANDAR_DOCUMENTACION.md
[OK] ACTUALIZACIONES_PENDIENTES.md
[OK] GUIA_COMMITS.md
[OK] LISTA_PENDIENTES.md

[4/5] Verificando archivos de codigo criticos...
[OK] src\sv\dark\bus\DarkAtomicBus.java
[OK] src\sv\dark\bus\DarkRingBus.java
[OK] src\sv\dark\bus\BusHardwareTest.java
[OK] src\sv\dark\kernel\EngineKernel.java

[5/5] Generando reporte de sincronizacion...
[OK] Reporte generado: sync_report_20260105.txt

═══════════════════════════════════════════════════════════
  RESUMEN DE SINCRONIZACION
═══════════════════════════════════════════════════════════

Documentos encontrados: 13
Reporte generado: sync_report_20260105.txt
```

---

## Archivos Verificados

### Documentación Crítica

1. `AAA_CERTIFICATION.md` - Protocolo de certificación AAA+
2. `AAA_CODING_STANDARDS.md` - Estándares de codificación
3. `ESTANDAR_DOCUMENTACION.md` - Formato de documentación
4. `ACTUALIZACIONES_PENDIENTES.md` - Registro de pendientes
5. `GUIA_COMMITS.md` - Guía de commits
6. `LISTA_PENDIENTES.md` - Lista completa de tareas

### Código Crítico

1. `src\sv\dark\bus\DarkAtomicBus.java` - Bus atómico lock-free
2. `src\sv\dark\bus\DarkRingBus.java` - Ring buffer
3. `src\sv\dark\bus\BusHardwareTest.java` - Test de padding
4. `src\sv\dark\kernel\EngineKernel.java` - Kernel principal

---

## Reporte Generado

El script genera un archivo `sync_report_YYYYMMDD.txt` con:

- Fecha y hora de ejecución
- Lista completa de documentos
- Estado de archivos críticos
- Archivos faltantes (si existen)

### Ejemplo de Reporte

```
DARK ENGINE - SYNC REPORT
Fecha: 05/01/2026 21:15:00
═══════════════════════════════════════════════════════════

DOCUMENTACION:
AAA_CERTIFICATION.md
AAA_CODING_STANDARDS.md
ARQUITECTURA_DARK_ENGINE.md
DOCUMENTACION_BUS.md
ESTANDAR_DOCUMENTACION.md
ACTUALIZACIONES_PENDIENTES.md
GUIA_COMMITS.md
LISTA_PENDIENTES.md
PLAN_DOCUMENTACION_UNIFICADA.md
SIGNAL_DISPATCHER_GUIDE.md
TECHNICAL_GLOSSARY.md
TASK_LEGACY.md
WALKTHROUGH_LEGACY.md

CODIGO CRITICO:
[OK] src\sv\dark\bus\DarkAtomicBus.java
[OK] src\sv\dark\bus\DarkRingBus.java
[OK] src\sv\dark\bus\BusHardwareTest.java
[OK] src\sv\dark\kernel\EngineKernel.java
```

---

## Integración con Workflow

### Antes de Commit

```batch
# 1. Sincronizar archivos
update.bat

# 2. Revisar reporte
type sync_report_20260105.txt

# 3. Si todo OK, hacer commit
git add .
git commit -m "[DOCS] Actualizar documentacion"
```

### Después de Pull

```batch
# 1. Actualizar desde repositorio
git pull

# 2. Verificar sincronización
update.bat

# 3. Revisar cambios
type sync_report_20260105.txt
```

---

## Solución de Problemas

### Error: Directorio no encontrado

**Síntoma**:
```
[ERROR] Directorio de documentacion no encontrado
```

**Solución**:
```batch
# Verificar que estás en el directorio raíz del proyecto
cd c:\Users\theca\Documents\GitHub\DarkEngine
update.bat
```

---

### Advertencia: Archivo crítico no encontrado

**Síntoma**:
```
[WARN] AAA_CERTIFICATION.md - NO ENCONTRADO
```

**Solución**:
1. Revisar si el archivo fue eliminado accidentalmente
2. Restaurar desde backup o repositorio
3. Verificar nombre del archivo (debe seguir convenciones)

---

### Reporte muestra archivos faltantes

**Síntoma**:
```
[FALTA] src\sv\dark\bus\BusBenchmarkTest.java
```

**Acción**:
1. Crear el archivo faltante
2. Actualizar `LISTA_PENDIENTES.md`
3. Ejecutar `update.bat` nuevamente

---

## Extensión del Script

### Agregar Nuevos Archivos Críticos

Editar `update.bat` línea 60:

```batch
set CRITICAL_CODE=src\sv\dark\bus\DarkAtomicBus.java src\sv\dark\bus\DarkRingBus.java src\sv\dark\bus\BusHardwareTest.java src\sv\dark\bus\BusBenchmarkTest.java src\sv\dark\kernel\EngineKernel.java
```

### Agregar Nuevos Documentos Críticos

Editar `update.bat` línea 45:

```batch
set CRITICAL_DOCS=AAA_CERTIFICATION.md AAA_CODING_STANDARDS.md ESTANDAR_DOCUMENTACION.md ACTUALIZACIONES_PENDIENTES.md GUIA_COMMITS.md LISTA_PENDIENTES.md NUEVO_DOCUMENTO.md
```

---

## Automatización

### Ejecutar Automáticamente

Crear `auto_sync.bat`:

```batch
@echo off
:LOOP
update.bat
timeout /t 3600
goto LOOP
```

Esto ejecutará `update.bat` cada hora.

---

## Checklist de Sincronización

Antes de considerar el proyecto sincronizado:

- [ ] `update.bat` ejecutado sin errores
- [ ] Todos los archivos críticos encontrados
- [ ] Reporte generado correctamente
- [ ] Sin advertencias de archivos faltantes
- [ ] Documentación actualizada con última fecha
- [ ] `ACTUALIZACIONES_PENDIENTES.md` refleja estado actual

---

**Versión**: 1.0  
**Fecha**: 2026-01-05  
**Autor**: Marvin-Dev  
**Estado**: Activo
