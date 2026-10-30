#!/usr/bin/env python3
"""Consulta TC y recuerda el flujo de issues. No crea incidencias automáticamente."""
from pathlib import Path
import yaml
root=Path(__file__).resolve().parent.parent
for path in sorted((root/'docs/testing/tc').glob('*.yml')):
    c=yaml.safe_load(path.read_text(encoding='utf-8'))
    print(f"{c['id']} | {c['name']} | {', '.join(c['reqs'])}")
print('Para registrar un defecto, usa el formulario .github/ISSUE_TEMPLATE/defecto.yml con label bug.')
