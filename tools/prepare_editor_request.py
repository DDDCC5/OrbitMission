import json,sys
from pathlib import Path
j=json.load(open(sys.argv[1]));j=json.loads(next(c['text'] for c in j['content'] if c['type']=='text'))
Path('reports/editor-state.json').write_text(json.dumps(j,ensure_ascii=False,indent=2))
print('RUN',j.get('runId'),'CURRENT',j.get('current'),'NEXT',j.get('next',{}).get('action'),'ERROR',j.get('error'))
if len(sys.argv)>2:
 inp=json.load(open(sys.argv[2]));req={'name':j['next']['tool'],'arguments':dict(j['next']['arguments'],input=inp)};Path('reports/editor-request.json').write_text(json.dumps(req,ensure_ascii=False))
else:
 Path('reports/editor-next-schema.json').write_text(json.dumps(j.get('next',{}).get('submission',{}),ensure_ascii=False,indent=2))
