from generate_ui import *
import resvg_py, wave, numpy as np, base64
V=R/'assets/preview';AUDIO=R/'assets/audio'
for loc,i in [('zh-Hans',0),('zh-Hant',1),('en',2)]:
 title=rows['app_name'][i];(U/('logo-'+loc+'.svg')).write_text(svg(900,240,logo.replace('轨道使命',title)))
(U/'countdown_gauge.svg').write_text(svg(320,320,panel(2,2,316,316)+f'<circle cx="160" cy="160" r="105" fill="none" stroke="#254452" stroke-width="8"/><circle cx="160" cy="160" r="105" fill="none" stroke="#66ddec" stroke-width="8" stroke-dasharray="490 170" transform="rotate(-90 160 160)"/>'))
(U/'gaze_reticle.svg').write_text(svg(160,160,f'<path d="M20 60V20H60M100 20H140V60M140 100V140H100M60 140H20V100" fill="none" stroke="#66ddec" stroke-width="2"/><circle cx="80" cy="80" r="52" fill="none" stroke="#ffbf63" stroke-width="3" stroke-dasharray="245 82"/>'))
for state in ['on','off']:
 (U/('toggle_'+state+'.svg')).write_text(svg(120,48,panel(2,2,116,44, '#66ddec' if state=='on' else '#456071')+f'<path d="M{72 if state=="on" else 16} 12h30v24h-30Z" fill="{C if state=="on" else M}"/>'))
for f in U.glob('*.svg'):(V/(f.stem+'.png')).write_bytes(resvg_py.svg_to_bytes(svg_path=str(f)))
# One reel of all five original sound effects.
clips=[]
for n in ['button_click','rocket_engine','orbit_success','mission_failure','collision']:
 with wave.open(str(AUDIO/(n+'.wav')),'rb') as w:clips.append(np.frombuffer(w.readframes(w.getnframes()),'<i2'))
 clips.append(np.zeros(12000,dtype='<i2'))
with wave.open(str(AUDIO/'sfx_audition.wav'),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(24000);w.writeframes(np.concatenate(clips).astype('<i2').tobytes())
# Static offline review document. This gallery is not runtime application code.
imgs=['celestial-model-renders.png','rocket-model-renders.png','sun-earth-motion.gif','components-board.png','01-splash.png','02-planet-selection.png','03-tutorial.png','04-rocket-placement.png','05-flight-telemetry.png','06-success.png','07-failure.png','08-settings-zh-Hans.png','08-settings-zh-Hant.png','08-settings-en.png','09-localization.png']
labels=['太阳与八大行星','载人火箭三视角','太阳粒子与自转预览（加速）','独立 UI 素材与按钮状态','开场 Logo','选择行星','首次教程','火箭放置','实时参数','成功','失败','设置 · 简体','設定 · 繁體','Settings · English','三语排版']
body='<h1>轨道使命 / Orbit Mission</h1><p>第一阶段素材审阅 · 尚未进入应用开发 · 包名 com.example.orbitmission</p><p>模型为本地生成 GLB。Editor 工作流因官方授权提示服务连接失败而终止。效果图为素材渲染与静态设计稿，非 PICO 运行截图。语音为本机 TTS 合成，请试听确认播报风格。</p>'
body+='<nav><a href="#visuals">视觉</a> <a href="#audio">音频</a> <a href="#models">模型文件</a></nav><h2 id="visuals">视觉素材</h2>'
for name,label in zip(imgs,labels):body+=f'<figure><figcaption>{label}</figcaption><a href="preview/{name}"><img loading="lazy" src="preview/{name}" alt="{label}"></a></figure>'
body+='<h2 id="audio">音频试听</h2>'
for f,label in [('deep_space_loop_60s.wav','深空配乐 / 60 秒循环'),('voice_audition_all_languages.wav','三语全部 18 条提示串联'),('sfx_audition.wav','五类音效串联')]:body+=f'<p>{label}</p><audio controls preload="none" src="audio/{f}"></audio>'
for locale in ['zh-Hans','zh-Hant','en']:
 body+=f'<h3>{locale}</h3>'
 script=json.loads((AUDIO/locale/'script.json').read_text())
 for key,line in script.items():body+=f'<p>{line}</p><audio controls preload="none" src="audio/{locale}/{key}.wav"></audio>'
body+='<h2 id="models">模型与源素材</h2><ul>'
for f in sorted((R/'assets/models').glob('*.glb')):body+=f'<li><a href="models/{f.name}" download>{f.name}</a></li>'
body+='</ul><p><a href="ui/">SVG 素材</a> · <a href="locales/">三语文字 JSON</a> · <a href="../ASSET_REVIEW.md">交付说明</a></p>'
(R/'assets/index.html').write_text('<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Orbit Mission — 素材审阅</title><style>body{margin:auto;max-width:1200px;padding:32px;background:#050d19;color:#e9f7ff;font-family:system-ui;line-height:1.7}h1,h2{color:#66ddec}a{color:#ffbf63}nav{position:sticky;top:0;background:#050d19;padding:12px}figure{margin:30px 0}figcaption{font-size:22px;margin:12px 0}img{display:block;width:100%;height:auto}audio{width:min(100%,600px)}p{max-width:1000px}</style>'+body+'</html>')
print('Completed independent UI primitives, locale logos, audio reels and offline review gallery.')
