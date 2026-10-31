#!/usr/bin/env python3
"""Convierte @TC + Javadoc, Surefire, JaCoCo e issues bug al formato interno del generador.

Formato de anotación previsto: @TC(id="UTS-01", rf="RF-01", modulo="login")
Javadoc: @objetivo, @precondicion, @pasos ("1.- A; 2.- B"), @esperado y @nota opcional (sale en Comments).
Los casos sin pruebas no se inventan: el validador informará qué falta.
"""
import argparse
import json
import os
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path
import yaml

ROOT = Path(__file__).resolve().parent.parent
META = ROOT / 'docs/testing/meta.yml'

def catalogo():
    cases = []
    # Entre el Javadoc y @TC puede haber otras anotaciones (@Test antes de @TC); el Javadoc no cruza otro */
    pattern = re.compile(r'/\*\*((?:(?!\*/).)*)\*/\s*(?:@(?!TC\b)\w+(?:\([^)]*\))?\s*)*@TC\s*\((.*?)\)', re.S)
    for file in (ROOT/'src/test/java').rglob('*.java'):
        content = file.read_text(encoding='utf-8')
        for doc, args in pattern.findall(content):
            attrs = dict(re.findall(r'(\w+)\s*=\s*"([^"]*)"', args))
            id_ = attrs.get('id', '')
            if not re.fullmatch(r'(UTS|ITS|STS|ATS)-\d{2,}', id_):
                raise ValueError(f'{file}: @TC sin ID válido: {id_}')
            # Cada etiqueta llega hasta la siguiente; puede ocupar varias líneas del Javadoc
            tags = dict(re.findall(r'@(objetivo|precondicion|pasos|esperado|nota)\s+(.*?)(?=@(?:objetivo|precondicion|pasos|esperado|nota)\b|\Z)', doc, re.S))
            def clean(s): return re.sub(r'\s+', ' ', s.replace('*','')).strip()
            # "1.- A; 2.- B" -> ["A", "B"]: el generador vuelve a numerar cada paso
            steps = [p.strip(' ;') for p in re.split(r'\d+\.-\s*', clean(tags.get('pasos','Ejecutar prueba JUnit'))) if p.strip(' ;')]
            case = {'id':id_, 'level':id_.split('-')[0], 'module':attrs.get('modulo','general'),
                    'name':clean(tags.get('objetivo',id_)), 'objective':clean(tags.get('objetivo',id_)),
                    'pre':clean(tags.get('precondicion','Preparar entorno de prueba')),
                    'steps':steps,
                    'expected':[clean(tags.get('esperado','Aserciones satisfactorias'))],
                    'comments':clean(tags.get('nota','')),
                    'reqs':[x.strip() for x in attrs.get('rf','').split(',') if x.strip()],
                    'tests':[id_]}
            if not case['reqs']: raise ValueError(f'{file}: {id_} sin RF')
            cases.append(case)
    return cases

def surefire(folder):
    results = {}
    for file in folder.rglob('TEST-*.xml'):
        tree=ET.parse(file)
        for test in tree.iter('testcase'):
            classname=test.get('classname','')
            name=test.get('name','')
            tc_match=re.search(r'(UTS|ITS|STS|ATS)-\d{2,}', name)
            # Se admite ID en display name de JUnit o prefijo de método UTS_01_...
            if not tc_match:
                tc_match=re.search(r'(UTS|ITS|STS|ATS)_\d{2,}', name)
            if not tc_match: continue
            tc=tc_match.group().replace('_','-')
            outcome='failed' if test.find('failure') is not None or test.find('error') is not None else 'skipped' if test.find('skipped') is not None else 'passed'
            results[f'{tc}::{classname}::{name}']={'outcome':outcome}
    return results

def jacoco(path):
    if not path.exists(): return None
    root=ET.parse(path).getroot()
    files={}
    total_covered=total_missed=0
    for package in root.findall('package'):
        for source in package.findall('sourcefile'):
            count=source.find("counter[@type='LINE']")
            if count is None: continue
            covered=int(count.get('covered',0)); missed=int(count.get('missed',0))
            total_covered+=covered; total_missed+=missed
            files[f"{package.get('name','')}/{source.get('name','')}"]={'summary':{'percent_covered':100*covered/(covered+missed) if covered+missed else 0}}
    return {'files':files,'totals':{'percent_covered':100*total_covered/(total_covered+total_missed) if total_covered+total_missed else 0}}

def issues(repo):
    if not repo: return []
    proc=subprocess.run(['gh','issue','list','--repo',repo,'--label','bug','--state','all','--limit','500','--json','number,title,body,state,author,assignees'],capture_output=True,text=True)
    if proc.returncode: raise RuntimeError('No se pudieron consultar issues bug: '+proc.stderr)
    # Found by = quien abrió el issue; nombres reales desde meta.yml (usuario de GitHub -> nombre)
    nombres=(yaml.safe_load(META.read_text(encoding='utf8')) or {}).get('nombres') or {}
    nombre=lambda login: nombres.get(login, login)
    out=[]
    for issue in json.loads(proc.stdout):
        body=issue.get('body') or ''
        match=re.search(r'(UTS|ITS|STS|ATS)-\d{2,}',body)
        if not match: continue
        sev=re.search(r'([123]-\s*(?:High|Medium|Low))',body,re.I)
        out.append({'id':f"GH-{issue['number']}",'tc':match.group(), 'description':issue['title'],
                    'steps':body[:2000], 'severity':sev.group(1) if sev else '3- Low',
                    'state':'Closed' if issue['state']=='CLOSED' else 'Open',
                    'found_by':nombre(issue['author']['login']),
                    'assigned_to':', '.join(nombre(a['login']) for a in issue.get('assignees',[])) or 'Sin asignar'})
    return out

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('--repo',default=os.getenv('GITHUB_REPOSITORY',''))
    ap.add_argument('--strict',action='store_true',help='Exigir 10 TC por nivel y todos los RF')
    a=ap.parse_args()
    base=ROOT/'docs/testing'; (base/'tc').mkdir(parents=True,exist_ok=True); (base/'defectos').mkdir(parents=True,exist_ok=True)
    cases=catalogo(); ids=[c['id'] for c in cases]
    if len(ids)!=len(set(ids)): raise ValueError('ID @TC duplicado')
    if a.strict:
        meta=yaml.safe_load(META.read_text(encoding='utf8'))
        for level in meta['levels']:
            if sum(c['level']==level for c in cases)<10: raise ValueError(f'{level}: faltan TC (mínimo 10)')
        covered={r for c in cases for r in c['reqs']}
        if set(meta['requirements'])-covered: raise ValueError('RF sin cobertura: '+str(set(meta['requirements'])-covered))
    # Las fichas sin prueba (tests: []) vienen del Test Plan y se conservan hasta que exista su @TC
    for file in (base/'tc').glob('*.yml'):
        if yaml.safe_load(file.read_text(encoding='utf8')).get('tests') or file.stem in ids: file.unlink()
    for c in cases: (base/'tc'/f"{c['id']}.yml").write_text(yaml.safe_dump(c,allow_unicode=True,sort_keys=False),encoding='utf8')
    for file in (base/'defectos').glob('*.yml'): file.unlink()
    for defect in issues(a.repo): (base/'defectos'/f"{defect['id']}.yml").write_text(yaml.safe_dump(defect,allow_unicode=True,sort_keys=False),encoding='utf8')
    reports=ROOT/'reports'; reports.mkdir(exist_ok=True)
    (reports/'results.json').write_text(json.dumps(surefire(ROOT/'target/surefire-reports'),indent=2),encoding='utf8')
    cov=jacoco(ROOT/'target/site/jacoco/jacoco.xml')
    if cov: (reports/'coverage.json').write_text(json.dumps(cov,indent=2),encoding='utf8')
    print(f'Extraídos {len(cases)} TC, {len(issues(a.repo)) if a.repo else 0} defectos; reportes preparados')
if __name__=='__main__': main()
