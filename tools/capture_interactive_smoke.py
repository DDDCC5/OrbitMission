from pathlib import Path
import subprocess,time,json,uuid,argparse
parser=argparse.ArgumentParser();parser.add_argument("--cli",default="pico-cli");parser.add_argument("--device",default="emulator-5554");parser.add_argument("--out",default="output/interaction-1.4.0-final");parser.add_argument("--size",default="1920x1080");args=parser.parse_args();capture_size=args.size
cli=args.cli
target=args.device;run='interactive-'+uuid.uuid4().hex[:8]
out=Path(args.out);out.mkdir(exist_ok=True)
log=open('reports/interaction-native-final.log','w')
p=subprocess.Popen([cli,'shell','am','instrument','-w','-e','class','com.example.orbitmission.InteractiveRuntimeTest,com.example.orbitmission.AppIdentityTest','-e','evidenceRun',run,'com.example.orbitmission.test/androidx.test.runner.AndroidJUnitRunner','--device',target],stdout=log,stderr=subprocess.STDOUT)
seen={};end=time.monotonic()+160
while time.monotonic()<end:
 r=subprocess.run([cli,'shell','run-as','com.example.orbitmission','cat','cache/interactive-state.txt','--device',target],capture_output=True,text=True,timeout=10)
 marker=r.stdout.strip().split(':')
 if len(marker)==2 and marker[0]==run and marker[1] not in seen:
  name=marker[1];path=out/(name.lower()+'.png')
  args=[cli,'capture','screenshot','--device',target,'--spatial','--size',capture_size,'--out',str(path)]
  shot=subprocess.run(args,capture_output=True,text=True,timeout=25)
  mode='spatial'
  if shot.returncode!=0:
   Path('reports/interaction-spatial-capture-failure.txt').write_text(shot.stdout+shot.stderr)
   mode='emulator display fallback'
   shot=subprocess.run([cli,'capture','screenshot','--device',target,'--out',str(path)],capture_output=True,text=True,timeout=25)
  seen[name]={'path':str(path),'mode':mode,'success':shot.returncode==0,'result':shot.stdout+shot.stderr}
  print(name,seen[name]['success'],flush=True)
 if p.poll() is not None:break
 time.sleep(.5)
if p.poll() is None:print('Native test still running',flush=True)
else:print('Instrumentation exit',p.returncode,flush=True)
log.close();Path('reports/interaction-captures-final.json').write_text(json.dumps(seen,ensure_ascii=False,indent=2))
