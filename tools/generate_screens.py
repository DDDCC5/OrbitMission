from generate_ui import *
import base64, resvg_py
V=R/'assets/preview';S=R/'assets/ui/screens';S.mkdir(exist_ok=True)
def picture(path,x,y,w,h):
 data=base64.b64encode(Path(path).read_bytes()).decode();return f'<image xmlns:xlink="http://www.w3.org/1999/xlink" x="{x}" y="{y}" width="{w}" height="{h}" xlink:href="data:image/png;base64,{data}"/>'
def header(label):return text(70,80,'轨道使命 / ORBIT MISSION',26,C)+text(1840,80,label,19,M,'end')
def output(name,body):
 s=svg(1920,1080,body,True);(S/(name+'.svg')).write_text(s);(V/(name+'.png')).write_bytes(resvg_py.svg_to_bytes(svg_string=s))
output('01-splash',f'<g transform="translate(360 320) scale(1.5)">{logo}</g>'+text(960,855,'5 秒开场标识 · 随后渐隐',22,M,'middle'))
body=header('行星选择 / SELECT PLANET')+text(960,210,'请选择一个行星',46,W,'middle')
for i,n in enumerate(['sun','mercury','venus','earth','mars','jupiter','saturn','uranus','neptune']):
 x=40+i*205;body+=('<g filter="url(#sat50)">' if i not in [0,3] else '<g>')+picture(V/(n+'-render.png'),x,360,200,200)+'</g>'+text(x+100,625,['太阳','水星','金星','地球','火星','木星','土星','天王星','海王星'][i],24,W,'middle')
body+=f'<circle cx="755" cy="460" r="112" fill="none" stroke="{A}" stroke-width="3" stroke-dasharray="490 210"/>'+text(755,705,'凝视确认 1.4 / 2 s',22,A,'middle')+text(960,870,'注视 2 秒确认 · 或使用手柄射线选择',25,M,'middle')+button(1590,930,250,'设置')
output('02-planet-selection',body)
body=header('首次操作指引 / FLIGHT BRIEFING')+panel(250,220,1420,620)+text(310,285,'操作指引',38)
for i,key in enumerate(['tutorial_select','tutorial_grab','tutorial_thrust']):
 y=380+i*135;body+=text(320,y,'0'+str(i+1),40,A)+text(440,y,rows[key][0],28)
body+=button(1260,745,300,'开始任务')+button(310,745,260,'跳过教程')
output('03-tutorial',body)
def flight(label,prompt):
 body=header(label)+picture(V/'earth-render.png',495,110,900,900)
 body+=f'<ellipse cx="945" cy="570" rx="510" ry="225" transform="rotate(-18 945 570)" fill="none" stroke="{C}" stroke-width="3" stroke-dasharray="18 8"/>'
 body+=f'<path d="M1200 345L1340 300L1450 470" fill="none" stroke="{A}" stroke-width="4"/>'+text(1420,285,'推荐轨道区域',22,A)
 body+=panel(70,185,330,610)+text(100,230,'FLIGHT TELEMETRY',20,C)
 for i,(k,val,unit) in enumerate([('speed','7.81','km/s'),('radius','6,971','km'),('gravity','8,203','N'),('elapsed','23.0','s'),('eccentricity','0.012','')]):
  y=285+i*100;body+=text(100,y,rows[k][0],17,M)+text(100,y+44,val,38)+text(370,y+44,unit,18,C,'end')
 body+=panel(1540,400,300,300)+text(1690,445,'剩余任务时间',21,M,'middle')+f'<circle cx="1690" cy="565" r="87" fill="none" stroke="{C}" stroke-width="8" stroke-dasharray="400 148" transform="rotate(-90 1690 565)"/>'+text(1690,580,'67 s',44,W,'middle')
 body+=picture(V/'rocket-asset-render.png',1180,475,210,350)
 body+=panel(440,930,1050,100)+text(965,992,prompt,32,W,'middle');return body
output('04-rocket-placement',flight('火箭放置 / ORBIT INSERTION','请将火箭放置在轨道上'))
output('05-flight-telemetry',flight('引力飞行 / FLIGHT IN PROGRESS','按住扳机持续加速 · 环绕进度 126° / 360°'))
body=flight('任务完成 / MISSION ACCOMPLISHED','入轨成功 · 火箭继续环绕飞行')+panel(1320,150,520,180,A)+text(1580,220,'入轨成功',38,A,'middle')+text(1580,275,'稳定轨道 · 已完成一圈',24,W,'middle')
output('06-success',body)
body=header('任务结果 / MISSION RESULT')+panel(510,260,900,520,A)+text(960,355,'任务失败',48,A,'middle')+text(960,430,'90 秒任务时间已结束',30,W,'middle')+text(960,485,'调整初始速度和切向方向，重新尝试。',23,M,'middle')+button(600,625,320,'重新再来')+button(1000,625,320,'选择其他行星')
output('07-failure',body)
for i,loc in enumerate(['zh-Hans','zh-Hant','en']):
 body=header(rows['settings'][i].upper())+panel(510,180,900,720)+text(575,260,rows['settings'][i],42)+text(575,335,rows['language'][i],24,M)
 for k,v in enumerate(['简体中文','繁體中文','English']):body+=button(575+k*255,375,235,v,'focus' if i==k else 'idle')
 for k,key in enumerate(['music','voice','sound']):
  y=540+k*95;body+=text(575,y,rows[key][i],27)+button(1140,y-35,180,rows['on'][i],'focus')
 body+=button(575,790,250,rows['back'][i]);output('08-settings-'+loc,body)
# Three language samples of the longest prompt and result buttons.
body=text(70,90,'三语排版 / LOCALIZATION',38)
for i in range(3):
 y=185+i*270;body+=panel(70,y,1780,230)+text(110,y+65,['简体中文','繁體中文','ENGLISH'][i],20,C)+text(110,y+120,rows['place_rocket'][i],34)+button(110,y+155,340,rows['retry'][i])+button(490,y+155,420,rows['other_planet'][i])
output('09-localization',body)
print('Created splash, selection, tutorial, placement, flight, success, failure, three settings-language previews and localization layout.')
