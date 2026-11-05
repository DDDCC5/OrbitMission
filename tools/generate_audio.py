from pathlib import Path
import numpy as np, wave, json, subprocess
root=Path(__file__).resolve().parents[1]; out=root/'assets/audio'; out.mkdir(parents=True,exist_ok=True)
sr=24000
rng=np.random.default_rng(241030)
def save(p,a):
 a=np.asarray(a); peak=np.max(np.abs(a)); a=a/max(1,peak/0.86)
 with wave.open(str(p),'wb') as w:
  w.setnchannels(1 if a.ndim==1 else a.shape[1]);w.setsampwidth(2);w.setframerate(sr);w.writeframes((a*32767).astype('<i2').tobytes())
def tone(freq,t):return np.sin(2*np.pi*freq*t)
t=np.arange(sr*60)/sr
music=np.zeros((len(t),2))
for k,f in enumerate([65.406,97.999,130.813,155.563,195.998,261.626]):
 f=round(f*60)/60
 env=.7+.3*np.sin(2*np.pi*t/60+k)
 for c in range(2):music[:,c]+=.038*env*(tone(f,t+(c*.002))+0.22*tone(f*2,t+(c*.002)))
for k in range(12):
 center=k*5; phase=(t-center)%60; env=np.exp(-phase*1.0)*(1-np.exp(-phase*12))
 f=round([523.25,622.25,783.99,392][k%4]*60)/60
 for c in range(2):music[:,c]+=.07*env*tone(f,t+c*.001)
save(out/'deep_space_loop_60s.wav',music)
for name,duration in [('button_click',.16),('rocket_engine',4),('orbit_success',2.2),('mission_failure',1.6),('collision',1.2)]:
 t=np.arange(int(sr*duration))/sr; noise=rng.normal(0,1,len(t))
 if name=='button_click': a=.3*tone(1800,t)*np.exp(-t*55)+.07*noise*np.exp(-t*100)
 elif name=='rocket_engine':
  from scipy.signal import butter,sosfilt
  a=.45*sosfilt(butter(3,1500,fs=sr,output='sos'),noise)+.13*tone(58,t)+.07*tone(116,t);a*=np.minimum(t*5,1)*np.minimum((duration-t)*5,1)
 elif name=='orbit_success':
  a=sum(.2*tone(f,t)*np.exp(-np.maximum(t-i*.22,0)*2.8)*(t>=i*.22) for i,f in enumerate([523.25,659.25,783.99,1046.5]))
 elif name=='mission_failure':a=.2*np.sin(2*np.pi*(330*t-70*t*t))*np.exp(-t*2)+.13*tone(164.81,t)*np.exp(-t*2)
 else:
  from scipy.signal import butter,sosfilt
  a=.65*sosfilt(butter(2,2200,fs=sr,output='sos'),noise)*np.exp(-t*8)+.3*np.sin(2*np.pi*(65*t-15*t*t))*np.exp(-t*7)
 a*=np.minimum((duration-t)*40,1);save(out/(name+'.wav'),a)
phrases={
 'select_planet':['请选择一个行星。','請選擇一個行星。','Please select a planet.'],
 'place_rocket':['请将火箭放置在轨道上。','請將火箭放置在軌道上。','Place the rocket on the orbit.'],
 'orbit_success':['入轨成功。','入軌成功。','Orbital insertion successful.'],
 'mission_failure':['任务失败。','任務失敗。','Mission failed.'],
 'collision_warning':['撞击警告。','撞擊警告。','Warning. Collision risk.'],
 'boundary_warning':['飞出边界警告。','飛出邊界警告。','Warning. Approaching the system boundary.']}
voices=[('zh-Hans','Binbin (Enhanced)',145),('zh-Hant','Eddy (中文（台湾）)',145),('en','Daniel',145)]
for index,(locale,voice,rate) in enumerate(voices):
 folder=out/locale;folder.mkdir(exist_ok=True)
 for key,text in phrases.items():
  subprocess.run(['say','-v',voice,'-r',str(rate),'-o',str(folder/(key+'.wav')),'--file-format=WAVE','--data-format=LEI16@24000',text[index]],check=True)
 (folder/'script.json').write_text(json.dumps({key:vals[index] for key,vals in phrases.items()},ensure_ascii=False,indent=2))
clips=[]
for locale,_,_ in voices:
 for key in phrases:
  with wave.open(str(out/locale/(key+'.wav')),'rb') as w:clips.extend(np.frombuffer(w.readframes(w.getnframes()),'<i2')/32768)
  clips.extend(np.zeros(int(sr*.55)))
save(out/'voice_audition_all_languages.wav',np.array(clips))
print('Created ambient music, 5 SFX, 18 voice prompts, and combined audition.')
