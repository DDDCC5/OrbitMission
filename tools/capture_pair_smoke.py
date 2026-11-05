"""Native emulator snapshots and a recording of the focus/return cycle."""
from pathlib import Path
import subprocess,time,json,sys
cli='/Users/chanzungsan/.local/lib/nodejs/node-v22.23.2-darwin-arm64/bin/pico-cli'
adb='/Users/chanzungsan/Library/Android/sdk/platform-tools/adb'
target='emulator-5554'
expected={'SELECT_READY':'pair-overview.png','PLACEMENT_READY':'pair-earth-focus.png','SUCCESS_READY':'pair-success.png','RETURN_READY':'pair-returned.png','JUPITER_READY':'pair-jupiter-focus.png','RETURN_AGAIN_READY':'pair-returned-again.png'}
if '--focus-only' in sys.argv:expected={key:expected[key] for key in ['SELECT_READY','JUPITER_READY','RETURN_AGAIN_READY']}
subprocess.run([adb,'-s',target,'logcat','-c'],check=True)
found={};record=None;record_log=None;deadline=time.monotonic()+260
while time.monotonic()<deadline:
    result=subprocess.run([adb,'-s',target,'logcat','-d','-s','OrbitSmoke:I'],capture_output=True,text=True,timeout=10)
    if 'PAIR_MOTION_START' in result.stdout and record is None:
        record_log=open('reports/pair-recording.log','w')
        record=subprocess.Popen([cli,'capture','record','--device',target,'--out','output/pair-translation.mp4','--time','20','--replace'],stdout=record_log,stderr=subprocess.STDOUT)
    for key,name in expected.items():
        if key in result.stdout and key not in found:
            time.sleep(.7)
            p=Path('output')/name
            shot=subprocess.run([cli,'capture','screenshot','--device',target,'--out',str(p),'--replace'],capture_output=True,text=True,timeout=20)
            found[key]={'path':str(p),'captured':shot.returncode==0,'mode':'emulator display fallback','output':shot.stdout+shot.stderr}
            print(key,found[key]['captured'],flush=True)
    if len(found)==len(expected):break
    time.sleep(.5)
if record is not None:
    record.wait(timeout=30);record_log.close();found['recording']={'path':'output/pair-translation.mp4','captured':record.returncode==0}
Path('reports/pair-runtime-captures.json').write_text(json.dumps(found,indent=2))
print('Captured',len(found),flush=True)
