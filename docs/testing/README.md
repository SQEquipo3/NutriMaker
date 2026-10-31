# Automatización QA — NutriMaker

## Fuentes
- Casos: anotaciones `@TC(id="UTS-01", rf="RF-01", modulo="login")` precedidas de Javadoc con `@objetivo`, `@precondicion`, `@pasos` (numerados: `1.- …; 2.- …`), `@esperado` y, opcional, `@nota` (sale en el campo *Comments*).
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

## Excel generados
Cada push a `main` publica los Excel en el release **`qa-latest`** (pestaña *Releases* del repo), reemplazando los anteriores:
- `SW_Test_Plan_<equipo>_<proyecto>.xlsx`, `TC_Design_...xlsx`, `SWT_summary_report_...xlsx`
- `entregables-qa.zip` (los Excel y los diagramas)

En los PR solo quedan como artifact `entregables-qa` de la corrida.

Las plantillas en `docs/testing/plantillas/` se conservan intactas.
