from pathlib import Path
import subprocess,time,json
cli='/Users/chanzungsan/.local/lib/nodejs/node-v22.23.2-darwin-arm64/bin/pico-cli'
expected={'SELECT_READY':'runtime-selection.png','PLACEMENT_READY':'runtime-placement.png','FLIGHT_READY':'runtime-flight.png','SUCCESS_READY':'runtime-success.png','SETTINGS_READY':'runtime-settings.png'}
found={};deadline=time.monotonic()+180
while time.monotonic()<deadline:
 r=subprocess.run([cli,'log','--device','emulator-5554','--tag','OrbitSmoke','--lines','40'],capture_output=True,text=True,timeout=10)
 for key,name in expected.items():
  if key in r.stdout and key not in found:
   p=Path('output')/name
   res=subprocess.run([cli,'capture','screenshot','--device','emulator-5554','--out',str(p),'--replace'],capture_output=True,text=True,timeout=15)
   found[key]={'path':str(p),'captured':res.returncode==0,'output':res.stdout+res.stderr};print(key,found[key]['captured'],flush=True)
 if len(found)==len(expected):break
 time.sleep(.5)
Path('reports/runtime-captures.json').write_text(json.dumps(found,indent=2))
print('Captured',len(found),'of',len(expected),flush=True)
