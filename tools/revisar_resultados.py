#!/usr/bin/env python3
"""Decide si la corrida de pruebas queda en verde o en rojo.

Una prueba que falla por un defecto conocido (issue abierto con label «bug» que cita su TC)
no pone la corrida en rojo, igual que un xfail de pytest. Cualquier otra falla sí.
Si un bug sigue abierto pero sus pruebas ya pasan, se avisa para cerrarlo o revisar la prueba.
"""
import argparse
import os
import re
import sys
import xml.etree.ElementTree as ET

from extraer_tc import ROOT, issues

def fallas(folder):
    """[(TC o None, 'Clase.metodo')] de cada prueba con <failure> o <error>, y TC que pasaron."""
    fallidas, pasaron = [], set()
    for file in sorted(folder.glob('TEST-*.xml')):
        for test in ET.parse(file).iter('testcase'):
            name = test.get('name', '')
            match = re.search(r'(UTS|ITS|STS|ATS)[-_]\d{2,}', name)
            tc = match.group().replace('_', '-') if match else None
            if test.find('failure') is not None or test.find('error') is not None:
                fallidas.append((tc, f"{test.get('classname', '').split('.')[-1]}.{name}"))
            elif tc and test.find('skipped') is None:
                pasaron.add(tc)
    return fallidas, pasaron

def main():
    ap = argparse.ArgumentParser(); ap.add_argument('--repo', default=os.getenv('GITHUB_REPOSITORY', ''))
    a = ap.parse_args()
    fallidas, pasaron = fallas(ROOT/'target/surefire-reports')
    abiertos = {}
    for bug in issues(a.repo):
        if bug['state'] == 'Open': abiertos.setdefault(bug['tc'], []).append(bug['id'].replace('GH-', '#'))
    # Un TC con algún método fallando no cuenta como «ya pasa»
    pasaron -= {tc for tc, _ in fallidas}

    nuevas = 0
    for tc, prueba in fallidas:
        if tc in abiertos:
            print(f"Falla conocida: {prueba} ({tc}, bug {', '.join(abiertos[tc])})")
        else:
            nuevas += 1
            print(f"::error::{prueba} falla y no hay un bug abierto que cite {tc or 'su TC'}")
    for tc in sorted(set(abiertos) & pasaron):
        print(f"::warning::{tc} ya pasa pero el bug {', '.join(abiertos[tc])} sigue abierto: ciérrenlo o revisen la prueba")
    print(f"{len(fallidas)} pruebas fallidas: {len(fallidas) - nuevas} con bug abierto, {nuevas} sin bug")
    sys.exit(1 if nuevas else 0)

if __name__ == '__main__': main()
