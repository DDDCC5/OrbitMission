from pathlib import Path
import json,html,math
R=Path(__file__).resolve().parents[1];U=R/'assets/ui';L=R/'assets/locales';L.mkdir(parents=True,exist_ok=True)
rows={
'app_name':['轨道使命','軌道使命','Orbit Mission'], 'select_planet':['请选择一个行星','請選擇一個行星','Please select a planet'], 'place_rocket':['请将火箭放置在轨道上','請將火箭放置在軌道上','Place the rocket on the orbit'], 'orbit_success':['入轨成功','入軌成功','Orbital insertion successful'], 'mission_failure':['任务失败','任務失敗','Mission failed'], 'collision_warning':['撞击警告','撞擊警告','Collision warning'], 'boundary_warning':['飞出边界警告','飛出邊界警告','Boundary warning'],
'speed':['飞行速度','飛行速度','Velocity'],'radius':['距行星中心','距行星中心','Distance to planet center'],'gravity':['引力大小','引力大小','Gravitational force'],'elapsed':['飞行时长','飛行時長','Flight time'],'eccentricity':['轨道偏心率','軌道偏心率','Eccentricity'],'countdown':['剩余任务时间','剩餘任務時間','Mission time remaining'], 'retry':['重新再来','重新再來','Try again'],'other_planet':['选择其他行星','選擇其他行星','Choose another planet'],'settings':['设置','設定','Settings'],'language':['语言','語言','Language'],'music':['背景音乐','背景音樂','Background music'],'voice':['语音提示','語音提示','Voice prompts'],'sound':['音效','音效','Sound effects'],'back':['返回','返回','Back'],'confirm':['确认','確認','Confirm'],'continue':['继续','繼續','Continue'],'start':['开始任务','開始任務','Begin mission'],'tutorial':['操作指引','操作指引','Flight briefing'],'skip':['跳过教程','跳過教學','Skip tutorial'],'tutorial_select':['注视行星 2 秒，或用手柄射线指向后按确认键。','注視行星 2 秒，或用控制器射線指向後按確認鍵。','Look at a planet for 2 seconds, or point and confirm with your controller.'],'tutorial_grab':['用手柄射线抓取火箭，调整位置和朝向，松手开始飞行。','用控制器射線抓取火箭，調整位置和朝向，放手開始飛行。','Grab the rocket with the controller ray. Adjust its position and heading, then release to launch.'],'tutorial_thrust':['按住扳机持续加速。完成稳定环绕一圈，即可达成任务。','按住扳機持續加速。完成穩定環繞一圈，即可達成任務。','Hold the trigger to accelerate. Complete one stable orbit to succeed.'],'timeout':['90 秒任务时间已结束','90 秒任務時間已結束','The 90-second mission window has ended'],'collision':['火箭与天体发生碰撞','火箭與天體發生碰撞','The rocket collided with a celestial body'],'escaped':['火箭已飞出任务边界','火箭已飛出任務邊界','The rocket left the mission boundary'],'stable':['稳定轨道','穩定軌道','Stable orbit'],'orbit_progress':['环绕进度','環繞進度','Orbit progress'],'gaze_progress':['凝视确认','凝視確認','Gaze confirmation'],'recommended':['推荐轨道区域','建議軌道區域','Recommended orbit region'],'on':['开启','開啟','On'],'off':['关闭','關閉','Off'],'sun':['太阳','太陽','Sun'],'mercury':['水星','水星','Mercury'],'venus':['金星','金星','Venus'],'earth':['地球','地球','Earth'],'mars':['火星','火星','Mars'],'jupiter':['木星','木星','Jupiter'],'saturn':['土星','土星','Saturn'],'uranus':['天王星','天王星','Uranus'],'neptune':['海王星','海王星','Neptune']}
for i,loc in enumerate(['zh-Hans','zh-Hant','en']):(L/(loc+'.json')).write_text(json.dumps({k:v[i] for k,v in rows.items()},ensure_ascii=False,indent=2))
C='#66ddec';A='#ffbf63';W='#e9f7ff';M='#87a8ba';BG='#050d19'
def svg(w,h,body,bg=False):return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}"><defs><filter id="sat50"><feColorMatrix type="saturate" values="0.5"/></filter><linearGradient id="surface" x2="0" y2="1"><stop stop-color="#142b3d" stop-opacity=".94"/><stop offset="1" stop-color="#06111f" stop-opacity=".94"/></linearGradient></defs>'+ (f'<path fill="{BG}" d="M0 0H{w}V{h}H0Z"/>' if bg else '')+body+'</svg>'
def text(x,y,s,size=20,color=W,anchor='start'):return f'<text x="{x}" y="{y}" fill="{color}" font-size="{size}" text-anchor="{anchor}" font-family="PingFang SC,Arial,sans-serif">{html.escape(s)}</text>'
def panel(x,y,w,h,color=C,fill='url(#surface)'):
 d=f'M{x+18} {y}H{x+w-32}L{x+w} {y+32}V{y+h-18}L{x+w-18} {y+h}H{x+32}L{x} {y+h-32}V{y+18}Z'
 return f'<path d="{d}" fill="{fill}" stroke="{color}" stroke-width="1"/><path d="M{x+26} {y+7}H{x+85}M{x+w-7} {y+39}V{y+min(85,h-10)}M{x+w-25} {y+h-7}H{x+w-85}" stroke="{color}" stroke-width="3"/>'
def button(x,y,w,label,state='idle'):
 color=A if state=='focus' else ('#456071' if state=='disabled' else C)
 s=panel(x,y,w,56,color, '#163544' if state=='focus' else 'url(#surface)')
 s+=text(x+w/2,y+35,label,18,color,'middle')
 if state=='pressed':s+=f'<path d="M{x+24} {y+49}H{x+w-24}" stroke="{A}" stroke-width="3"/>'
 return s
logo='<g transform="translate(128 120)"><circle r="71" fill="none" stroke="#4d7b96" stroke-width="1"/><ellipse rx="114" ry="34" transform="rotate(-28)" fill="none" stroke="#ffbf63" stroke-width="5"/><path d="M-12 54L-8 -50Q0 -75 8 -50L12 54L0 43Z" fill="#e9f7ff" transform="rotate(26)"/><path d="M-3 -39H4V-19H-3Z" fill="#102535" transform="rotate(26)"/><path d="M0 49L-5 76L6 61Z" fill="#66ddec" transform="rotate(26)"/><circle cx="86" cy="-66" r="4" fill="#ffbf63"/></g>'+text(270,110,'轨道使命',58)+text(273,160,'ORBIT MISSION',25,C)
(U/'logo.svg').write_text(svg(800,240,logo))
for name,w,h in [('telemetry',340,580),('instruction',880,120),('result',600,380),('settings',580,520),('tutorial',720,380),('planet_label',240,80)]:
 (U/(name+'_frame.svg')).write_text(svg(w,h,panel(2,2,w-4,h-4)))
for state in ['idle','focus','pressed','disabled']:(U/('button_'+state+'.svg')).write_text(svg(280,60,button(2,2,276,'',state)))
# All icon paths are custom and carry no raster baked text.
icons={'retry':'M18 7A7 7 0 1 0 19 16M18 3V8H13','back':'M15 4L7 12L15 20M7 12H22','play':'M7 3L21 12L7 21Z','confirm':'M3 12L9 18L21 5','settings':'M12 3L15 6L20 6L21 11L18 14L17 20L11 21L8 18L3 17L3 11L6 8L7 3ZM15 12A3 3 0 1 1 9 12A3 3 0 1 1 15 12','warning':'M12 2L23 21H1ZM12 8V14M12 17V18','planet':'M20 12A8 8 0 1 1 4 12A8 8 0 1 1 20 12M2 19L22 5','audio':'M3 9H7L13 4V20L7 15H3ZM17 7Q23 12 17 17'}
for name,d in icons.items():(U/('icon_'+name+'.svg')).write_text(svg(24,24,f'<path d="{d}" fill="none" stroke="{C}" stroke-width="1.6" stroke-linecap="square" stroke-linejoin="bevel"/>'))
body=text(64,70,'ORBIT MISSION / FLIGHT INSTRUMENTS',30)+text(64,105,'自定义 UI 素材 · 切角结构 / 青蓝反馈 / 琥珀告警',18,M)
body+=f'<g transform="translate(24 125) scale(.65)">{logo}</g>'
for i,state in enumerate(['idle','focus','pressed','disabled']):body+=button(590,150+i*82,300,['开始任务','凝视确认 1.4 / 2 s','确认 · 按下','暂不可用'][i],state)+text(920,185+i*82,state.upper(),15,M)
body+=panel(64,345,380,560)+text(90,387,'FLIGHT TELEMETRY',20,C)
for i,(key,value,unit) in enumerate([('speed','7.81','km/s'),('radius','6,971','km'),('gravity','8,203','N'),('elapsed','23.0','s'),('eccentricity','0.012','')]):
 y=430+i*91;body+=text(90,y,rows[key][0],16,M)+text(90,y+40,value,34)+text(350,y+40,unit,16,C)
body+=panel(490,525,520,245)+text(525,570,'任务失败',32,A)+text(525,612,'90 秒任务时间已结束',19)+button(525,665,195,'重新再来')+button(740,665,235,'选择其他行星')
body+=panel(1060,150,460,620)+text(1090,198,'设置 / SETTINGS',28)+text(1090,253,'语言 / LANGUAGE',16,M)
for i,s in enumerate(['简体中文','繁體中文','English']):body+=button(1090,280+i*76,390,s,'focus' if i==0 else 'idle')
for i,s in enumerate(['背景音乐','语音提示','音效']):body+=text(1090,545+i*58,s,18)+text(1470,545+i*58,'ON',18,C,'end')
body+=panel(490,815,1030,90)+text(1005,870,'请将火箭放置在轨道上',28,W,'middle')
body+=text(64,961,'简体：请选择一个行星     繁體：請選擇一個行星     EN: Please select a planet',19,M)
body+=text(64,1010,'矢量边框与图标独立交付；文字由三语 JSON 动态排版，不烘焙进按钮。',17,M)
(U/'components-board.svg').write_text(svg(1600,1060,body,True))
print('Created',len(list(U.glob('*.svg'))),'SVG assets and',len(rows),'translation keys × 3 languages.')
