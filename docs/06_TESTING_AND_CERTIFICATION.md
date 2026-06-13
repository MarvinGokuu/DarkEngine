# 🛡️ 06. TESTING AND CERTIFICATION

En el ecosistema DarkEngine, ninguna pieza de código entra a producción sin pasar por un umbral estricto de certificación de latencia. Si el hardware subyacente o los cambios recientes arruinan la estabilidad o la latencia de procesamiento, el motor **se niega a continuar**.

## Suite de Integridad AAA+ (`test.bat`)

El archivo `test.bat` automatiza el flujo de compilación en frío seguido de un ataque implacable de 14 pruebas al motor. Todas las pruebas arrojan su salida al log unificado `aaa_test_report.log`, garantizando que la salida no interfiera visualmente durante la compilación.

### Tipos de Pruebas Fundamentales

1. **Pruebas de Latencia Extrema**: El `UltraFastBootTest` califica el inicio en frío. Si la instanciación de memoria, mapeo de Kernel y despliegue del dispatcher toma más de 1.00 milisegundos, el test falla categóricamente.
2. **Benchmark del Bus Atómico**: El `BusBenchmarkTest` satura el MPSC RingBus. Se requiere que alcance más de 10 Millones de operaciones por segundo (Ops/s).
3. **Prueba de Gráficos de Dependencia**: El `SystemDependencyGraph` es atacado inyectando miles de sistemas circulares para asegurar que el algoritmo lineal valide $O(V+E)$ en tiempo real, previniendo *deadlocks*.
4. **Shutdown Elegante**: `GracefulShutdownTest`. Verifica que todos los buffers y la memoria Off-Heap liberen limpiamente en menos de 1 segundo sin llamadas de OS (kill signal).

## La Certificación "AAA+"

Un motor de este nivel no utiliza la frase "pasa los tests". Utilizamos la métrica **AAA+ Certification**.
Para que un componente sea marcado como AAA+ y anotado con el metadato `@AAACertified`, debe probar que:
- Cero instanciaciones dinámicas (allocations) en su Hot-Path.
- Operar a < 150ns en operaciones atómicas locales.
- No depender en lo absoluto del Heap Memory para almacenamiento de datos intensivo.

## Validadores Internos Activos
Adicional a `test.bat`, el motor posee validación al vuelo durante su propio *Boot*. El `BusSymmetryValidator` dispara pulsos de verificación a todos los buses cargados. Si un bus en tu hardware particular reporta colisiones altas o su latencia de entrega de evento excede la cuota permitida, lanza un error fatal de validación de simetría antes de ceder el control al Kernel.
