# Automatización QA — NutriMaker

## Fuentes
- Casos: anotaciones `@TC(id="UTS-01", rf="RF-01", modulo="login")` precedidas de Javadoc con `@objetivo`, `@precondicion`, `@pasos`, `@esperado`.
- Resultados: `target/surefire-reports/TEST-*.xml` (el nombre del test debe incluir el ID, por ejemplo `UTS_01_correoInvalido`).
- Cobertura: `target/site/jacoco/jacoco.xml`.
- Defectos: issues de GitHub con etiqueta `bug` y referencia al ID TC en el cuerpo.

## Ejecución
```bash
python -m pip install openpyxl pyyaml matplotlib pillow
python tools/extraer_tc.py --repo SQEquipo3/NutriMaker --strict
python tools/generar_documentos.py
```

**Estado:** infraestructura adaptada; no hay todavía 40 pruebas JUnit con `@TC`. El modo `--strict` y el generador deben fallar hasta completar el catálogo. No se fabrican resultados de pruebas.

Las plantillas en `docs/testing/plantillas/` se conservan intactas.
