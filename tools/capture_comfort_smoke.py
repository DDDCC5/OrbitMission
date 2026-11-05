"""Capture native emulator evidence. CLI tag log was empty; adb is read-only log fallback."""
from pathlib import Path
import subprocess,time,json
cli='/Users/chanzungsan/.local/lib/nodejs/node-v22.23.2-darwin-arm64/bin/pico-cli'
adb='/Users/chanzungsan/Library/Android/sdk/platform-tools/adb'
target='emulator-5554'
expected={'phase=SPLASH':'comfort-logo.png','SELECT_READY':'comfort-overview.png','PLACEMENT_READY':'comfort-placement.png','FLIGHT_READY':'comfort-flight.png','SUCCESS_READY':'comfort-success.png','SETTINGS_READY':'comfort-settings.png'}
subprocess.run([adb,'-s',target,'logcat','-c'],check=True)
found={};deadline=time.monotonic()+240
while time.monotonic()<deadline:
    r=subprocess.run([adb,'-s',target,'logcat','-d','-s','OrbitSmoke:I','OrbitMission:I'],capture_output=True,text=True,timeout=10)
    for key,name in expected.items():
        if key in r.stdout and key not in found:
            time.sleep(2.5 if key=='phase=SPLASH' else .5)
            p=Path('output')/name
            res=subprocess.run([cli,'capture','screenshot','--device',target,'--out',str(p),'--replace'],capture_output=True,text=True,timeout=20)
            found[key]={'path':str(p),'captured':res.returncode==0,'mode':'emulator display fallback','output':res.stdout+res.stderr}
            print(key,found[key]['captured'],flush=True)
    if len(found)==len(expected):break
    time.sleep(.5)
Path('reports/comfort-runtime-captures.json').write_text(json.dumps(found,indent=2))
print('Captured',len(found),'of',len(expected),flush=True)
