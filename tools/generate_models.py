from pathlib import Path
import json,struct,math
import numpy as np
from PIL import Image,ImageDraw,ImageFont,ImageFilter
import trimesh
from trimesh.visual.material import PBRMaterial
R=Path(__file__).resolve().parents[1];T=R/'assets/textures';O=R/'assets/models';P=R/'assets/preview'
rng=np.random.default_rng(1030)
FONT='/System/Library/Fonts/STHeiti Medium.ttc'
def font(n):return ImageFont.truetype(FONT,n)
names=['sun','mercury','venus','earth','mars','jupiter','saturn','uranus','neptune']
files=['sun','mercury','venus_atmosphere','earth_daymap','mars','jupiter','saturn','uranus','neptune']
cn=['太阳','水星','金星','地球','火星','木星','土星','天王星','海王星']
textures={n:Image.open(T/f'2k_{f}.jpg').convert('RGB') for n,f in zip(names,files)}
# Composite original Earth day/cloud maps as the authored base-color material.
a=np.array(textures['earth']).astype(float);cloud=np.array(Image.open(T/'2k_earth_clouds.jpg').convert('RGB').resize(textures['earth'].size))[:,:,0]/255
textures['earth']=Image.fromarray(np.uint8(a*(1-cloud[:,:,None]*.6)+255*cloud[:,:,None]*.6));textures['earth'].save(T/'earth_with_clouds.png')
def material(name,tex=None,color=None,emission=False,alpha='OPAQUE'):
 kw=dict(name=name,roughnessFactor=.85,metallicFactor=0,alphaMode=alpha,doubleSided=True)
 if tex is not None:kw.update(baseColorTexture=tex)
 if color is not None:kw.update(baseColorFactor=color)
 if emission:kw.update(emissiveFactor=[1,.7,.3],emissiveTexture=tex)
 return PBRMaterial(**kw)
def lathe(profile,segments=64,mat=None):
 v=[];uv=[];faces=[]
 for i,(y,r) in enumerate(profile):
  for j in range(segments+1):
   p=2*np.pi*j/segments;v.append([r*np.cos(p),y,r*np.sin(p)]);uv.append([j/segments,i/(len(profile)-1)])
 for i in range(len(profile)-1):
  for j in range(segments):
   a=i*(segments+1)+j;b=a+segments+1;faces.extend([[a,a+1,b],[a+1,b+1,b]])
 m=trimesh.Trimesh(v,faces,process=False);m.visual=trimesh.visual.TextureVisuals(uv=np.array(uv),material=mat);m.fix_normals();m.invert() if m.volume<0 else None;return m
# Sphere latitude uses real metric-free display radius, spin around +Y.
def sphere(mat):
 nlat,nlon=64,128;v=[];uv=[];f=[]
 for i in range(nlat+1):
  theta=np.pi*i/nlat
  for j in range(nlon+1):
   phi=2*np.pi*j/nlon;v.append([np.sin(theta)*np.cos(phi),np.cos(theta),np.sin(theta)*np.sin(phi)]);uv.append([j/nlon,1-i/nlat])
 for i in range(nlat):
  for j in range(nlon):
   a=i*(nlon+1)+j;b=a+nlon+1
   if i>0:f.append([a,b,a+1])
   if i<nlat-1:f.append([a+1,b,b+1])
 m=trimesh.Trimesh(v,f,process=False);m.visual=trimesh.visual.TextureVisuals(uv=uv,material=mat);m.fix_normals();m.invert() if m.volume<0 else None;return m

def ring(mat):
 v=[];uv=[];f=[]
 for j in range(257):
  p=2*np.pi*j/256
  for i,r in enumerate([1.22,2.28]):v.append([r*np.cos(p),0,r*np.sin(p)]);uv.append([i,.5])
 for j in range(256):a=2*j;f.extend([[a,a+2,a+1],[a+1,a+2,a+3]])
 m=trimesh.Trimesh(v,f,process=False);m.visual=trimesh.visual.TextureVisuals(uv=uv,material=mat);return m

def save_animated(scene,path,period=24,particle_nodes=False):
 raw=scene.export(file_type='glb');jlen=struct.unpack_from('<I',raw,12)[0];j=json.loads(raw[20:20+jlen]);pos=20+jlen;blen=struct.unpack_from('<I',raw,pos)[0];binary=bytearray(raw[pos+8:pos+8+blen]);j.setdefault('accessors',[]);j.setdefault('bufferViews',[])
 def acc(a,typ):
  a=np.array(a,dtype='<f4');binary.extend(b'\0'*((-len(binary))%4));off=len(binary);binary.extend(a.tobytes());bv=len(j['bufferViews']);j['bufferViews'].append({'buffer':0,'byteOffset':off,'byteLength':a.nbytes});ix=len(j['accessors']);item={'bufferView':bv,'componentType':5126,'count':len(a),'type':typ};
  if typ=='SCALAR':item.update(min=[float(a.min())],max=[float(a.max())])
  j['accessors'].append(item);return ix
 samplers=[];channels=[];times=acc(np.linspace(0,period,5),'SCALAR');q=[[0,np.sin(p/2),0,np.cos(p/2)] for p in np.linspace(0,2*np.pi,5)]
 root=next(i for i,n in enumerate(j['nodes']) if n.get('name')=='Body')
 n=j['nodes'][root];n.pop('matrix',None);n['rotation']=[0,0,0,1]
 samplers.append({'input':times,'output':acc(q,'VEC4'),'interpolation':'LINEAR'});channels.append({'sampler':0,'target':{'node':root,'path':'rotation'}})
 if particle_nodes:
  pt=acc(np.linspace(0,period,49),'SCALAR')
  for i,n in enumerate(j['nodes']):
   if n.get('name','').startswith('Flame_'):
    # Flame vertex positions are world-authored; scale changes around the Sun center produce a radial flicker.
    s=acc([[1+.065*(1+np.sin(t*np.pi*2+i)),1+.065*(1+np.sin(t*np.pi*2+i)),1+.065*(1+np.sin(t*np.pi*2+i))] for t in np.linspace(0,period,49)],'VEC3');n.pop('matrix',None);n['scale']=[1,1,1];samplers.append({'input':pt,'output':s,'interpolation':'LINEAR'});channels.append({'sampler':len(samplers)-1,'target':{'node':i,'path':'scale'}})
 j['animations']=[{'name':'PreviewAxialSpin' if not particle_nodes else 'SolarSpinAndParticlePulse','samplers':samplers,'channels':channels}];j['buffers'][0]['byteLength']=len(binary)
 js=json.dumps(j,separators=(',',':')).encode();js+=b' '*((-len(js))%4);binary+=b'\0'*((-len(binary))%4)
 path.write_bytes(struct.pack('<III',0x46546c67,2,12+8+len(js)+8+len(binary))+struct.pack('<II',len(js),0x4e4f534a)+js+struct.pack('<II',len(binary),0x004e4942)+binary)
for name in names:
 sc=trimesh.Scene();sc.add_geometry(sphere(material(name,textures[name],emission=name=='sun')),node_name='Body',geom_name=name)
 if name=='saturn':
  ringtex=Image.open(T/'2k_saturn_ring_alpha.png').convert('RGBA');m=ring(material('SaturnRings',ringtex,alpha='BLEND'));rot=trimesh.transformations.rotation_matrix(.46,[0,0,1]);m.apply_transform(rot);sc.add_geometry(m,node_name='Rings',parent_node_name='Body')
 # 1.2: the Sun is photosphere-only; do not recreate removed flame attachments.
 save_animated(sc,O/(name+'.glb'),particle_nodes=False)
# Original crew-rocket livery, loosely inspired by Long March rather than a scale replica.
livery=Image.new('RGB',(2048,2048),'#ecefeb');d=ImageDraw.Draw(livery)
for y in [120,540,1020,1610,1960]:d.rectangle([0,y,2048,y+12],fill='#34454e')
for y in range(650,1510,90):d.line([0,y,2048,y],fill='#c1cbc9',width=3)
for x in [440,1450]:
 d.rectangle([x,200,x+160,300],fill='#d42328');d.regular_polygon((x+35,234,16),5,rotation=-90,fill='#ffde62')
 for i,c in enumerate('中国航天'):d.text((x+40,760+i*110),c,font=font(70),fill='#183d60')
livery.save(T/'crew_rocket_livery.png')
white=material('IvoryCeramic',livery);red=material('ChinaRed',color=[180,25,32,255]);metal=material('EngineMetal',color=[50,66,75,255]);metal.metallicFactor=.7;metal.roughnessFactor=.38
sc=trimesh.Scene();meshes=[]
def part(name,profile,mat,offset=(0,0,0),segments=64):
 m=lathe(profile,segments,mat);m.apply_translation(offset);sc.add_geometry(m,node_name=name,geom_name=name);meshes.append(m)
part('CoreStage',[(0,0),(0,.42),(.12,.46),(4.8,.46),(4.9,.4),(5.4,.4),(5.6,.51),(6.5,.51),(6.9,.27),(7.05,.15),(7.05,0)],white)
for y in [.2,2.8,4.9,5.63]:part('StageBand_'+str(y),[(y,.461),(y+.06,.461)],metal)
part('RedNoseBand',[(6.2,.515),(6.38,.515)],red)
part('EscapeTower',[(6.93,.09),(7.55,.09),(7.75,.04),(8.0,0)],white,segments=32)
for i,p in enumerate([0,np.pi/2,np.pi,3*np.pi/2]):
 off=(.73*np.cos(p),0,.73*np.sin(p));part(f'Booster_{i}',[(0,0),(0,.23),(.12,.26),(2.8,.26),(3.05,.2),(3.38,0)],white,off)
 part(f'BoosterRedBand_{i}',[(2.35,.265),(2.52,.265)],red,off)
 part(f'BoosterNozzle_{i}',[(-.32,.22),(-.23,.21),(-.06,.11),(.1,.11)],metal,off)
for i,p in enumerate([0,np.pi/2,np.pi,3*np.pi/2]):part(f'CoreNozzle_{i}',[(-.4,.17),(-.2,.15),(-.02,.08),(.08,.08)],metal,(.21*np.cos(p),0,.21*np.sin(p)),32)
(O/'crew_rocket.glb').write_bytes(sc.export(file_type='glb'))
# Orthographic software renderer of the same generated geometry and UV maps.
def rocket_render(angle,w=600,h=1000,transparent=False):
 ay=np.radians(angle);ax=np.radians(-10);Ry=np.array([[np.cos(ay),0,np.sin(ay)],[0,1,0],[-np.sin(ay),0,np.cos(ay)]]);Rx=np.array([[1,0,0],[0,np.cos(ax),-np.sin(ax)],[0,np.sin(ax),np.cos(ax)]]);rot=Rx@Ry
 canvas=np.zeros((h,w,3),dtype=float);canvas[:]=[5,13,25];depth=np.full((h,w),-np.inf);light=np.array([-.5,.45,1]);light/=np.linalg.norm(light)
 for m in meshes:
  vertices=(m.vertices-[0,3.8,0])@rot.T;pts=np.column_stack([vertices[:,0]*100+w/2,-vertices[:,1]*100+h/2]);normals=m.face_normals@rot.T;uv=m.visual.uv;mat=m.visual.material;tex=np.array(mat.baseColorTexture)[:,:,:3] if mat.baseColorTexture is not None else None;color=np.array(mat.baseColorFactor[:3] if mat.baseColorFactor is not None else [255,255,255],float)
  if color.max()<=1:color*=255
  for fi,face in enumerate(m.faces):
   a,b,c=pts[face];xmin=max(0,int(min(a[0],b[0],c[0])));xmax=min(w-1,int(max(a[0],b[0],c[0])+1));ymin=max(0,int(min(a[1],b[1],c[1])));ymax=min(h-1,int(max(a[1],b[1],c[1])+1))
   if xmin>xmax or ymin>ymax:continue
   denom=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
   if abs(denom)<1e-8:continue
   yy,xx=np.mgrid[ymin:ymax+1,xmin:xmax+1];u=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/denom;v=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/denom;q=1-u-v;z=u*vertices[face[0],2]+v*vertices[face[1],2]+q*vertices[face[2],2];mask=(u>=0)&(v>=0)&(q>=0)&(z>depth[ymin:ymax+1,xmin:xmax+1]);shade=.24+.76*max(0,np.dot(normals[fi],light))
   if tex is not None:
    coords=u[:,:,None]*uv[face[0]]+v[:,:,None]*uv[face[1]]+q[:,:,None]*uv[face[2]];ix=np.clip((coords[:,:,0]*(tex.shape[1]-1)).astype(int),0,tex.shape[1]-1);iy=np.clip(((1-coords[:,:,1])*(tex.shape[0]-1)).astype(int),0,tex.shape[0]-1);rgb=tex[iy,ix]*shade
   else:rgb=np.broadcast_to(color*shade,(*u.shape,3))
   canvas[ymin:ymax+1,xmin:xmax+1][mask]=rgb[mask];depth[ymin:ymax+1,xmin:xmax+1][mask]=z[mask]
 im=Image.fromarray(np.uint8(np.clip(canvas,0,255)))
 if transparent:
  im=im.convert('RGBA');im.putalpha(Image.fromarray(np.uint8(np.isfinite(depth))*255))
 return im
rocket_board=Image.new('RGB',(1800,1120),'#050d19');draw=ImageDraw.Draw(rocket_board);draw.text((65,30),'中国载人火箭 / CREW LAUNCH VEHICLE',font=font(36),fill='#e9f7ff')
for i,angle in enumerate([0,45,100]):im=rocket_render(angle);rocket_board.paste(im,(i*600,90));draw.text((i*600+50,1060),['正面','三分之四侧面','侧面'][i],font=font(24),fill='#66ddec')
rocket_board.save(P/'rocket-model-renders.png')
rocket_render(40,transparent=True).save(P/'rocket-asset-render.png')
# Ray-render UV spheres; identical materials to GLBs, directional lighting except for emissive Sun.
def planet_render(name,size=420,phase=0,flame_phase=0):
 yy,xx=np.mgrid[:size,:size];scale=size*.19 if name=='saturn' else size*.39;x=(xx-size/2)/scale;y=-(yy-size/2)/scale;rr=x*x+y*y;mask=rr<=1;z=np.sqrt(np.clip(1-rr,0,1));phi=np.arctan2(z,x)+phase;u=(phi/(2*np.pi))%1;v=np.arccos(np.clip(y,-1,1))/np.pi;tex=np.array(textures[name]);tx=np.int32(u*(tex.shape[1]-1));ty=np.int32(v*(tex.shape[0]-1));col=tex[ty,tx].astype(float);shade=.12+.88*np.clip(-.45*x+.3*y+.84*z,0,1);col*=1 if name=='sun' else shade[:,:,None]
 canvas=np.full((size,size,3),[5.,13.,25.]);canvas[mask]=col[mask]
 if name=='saturn':
  # Tilted ring projected as an ellipse, hiding the far half behind the planet.
  ang=.36;rx=x*np.cos(ang)+y*np.sin(ang);ry=-x*np.sin(ang)+y*np.cos(ang);radius=np.sqrt(rx*rx+(ry/.36)**2);ringmask=(radius>1.22)&(radius<2.28)&((ry<0)|(~mask));ringtex=np.array(Image.open(T/'2k_saturn_ring_alpha.png').convert('RGBA'));ix=np.clip(((radius-1.22)/1.06*(ringtex.shape[1]-1)).astype(int),0,ringtex.shape[1]-1);rgba=ringtex[ringtex.shape[0]//2,ix].astype(float);alpha=rgba[:,:,3]/255*.9;canvas[ringmask]=(rgba[:,:,:3]*alpha[:,:,None]+canvas*(1-alpha[:,:,None]))[ringmask]
 if name in ['sun','earth','neptune','uranus']:
  r=np.sqrt(rr);g=np.exp(-np.maximum(r-1,0)*27)*(r>1);color=np.array([255,111,20] if name=='sun' else [30,130,220]);canvas+=g[:,:,None]*color*.5
 im=Image.fromarray(np.uint8(np.clip(canvas,0,255)))
 if name=='sun':
  overlay=Image.new('RGBA',im.size);d=ImageDraw.Draw(overlay)
  for i in range(28):
   a=i*2*np.pi/28;r=scale*(1.025+.045*np.sin(i+flame_phase));sx=size/2+np.cos(a)*r;sy=size/2+np.sin(a)*r;d.ellipse([sx-3,sy-3,sx+3,sy+3],fill=(255,140,23,180))
  im=Image.alpha_composite(im.convert('RGBA'),overlay.filter(ImageFilter.GaussianBlur(2))).convert('RGB')
 return im
board=Image.new('RGB',(1500,1680),'#050d19');d=ImageDraw.Draw(board);d.text((56,40),'轨道使命 / CELESTIAL ASSETS',font=font(38),fill='#e9f7ff');d.text((56,100),'实际模型配套材质渲染 · 非物理比例展示',font=font(20),fill='#87a8ba')
for i,name in enumerate(names):
 im=planet_render(name,460);x=(i%3)*500+20;y=(i//3)*490+160;board.paste(im,(x,y));d.text((x+20,y+425),cn[i]+' / '+name.upper(),font=font(22),fill='#e9f7ff');im.save(P/(name+'-render.png'))
board.save(P/'celestial-model-renders.png')
frames=[]
for k in range(24):
 frame=Image.new('RGB',(1080,500),'#050d19');frame.paste(planet_render('sun',430,k/24*.5,k*.6),(30,30));frame.paste(planet_render('earth',430,k/24*.5),(580,30));d=ImageDraw.Draw(frame);d.text((50,458),'太阳表面粒子 / SOLAR PARTICLES',font=font(20),fill='#66ddec');d.text((600,458),'地球自转 / AXIAL ROTATION',font=font(20),fill='#66ddec');frames.append(frame)
frames[0].save(P/'sun-earth-motion.gif',save_all=True,append_images=frames[1:],duration=100,loop=0)
manifest={'source':'Locally authored geometry, not a completed PICO Editor workflow delivery.','units':'Unit sphere radius for celestial GLBs; rocket dimensions are display design units, not a scale replica.','textures':'Solar System Scope / INOVE, CC BY 4.0; NASA-derived, some unmapped areas fictional.','rotation':'GLB preview clips use 24 seconds per turn for review. Runtime must apply signed physical sidereal periods with an explicit time-scale.','sunParticles':'32 emissive mesh particle nodes with scale animation, visual preview included; runtime particle engine integration remains stage 2.','bodies':[]}
periods=[2192832,5067360,-20997000,86164.2,88560,35640,38520,-61920,57960]
for name,period in zip(names,periods):manifest['bodies'].append({'id':name,'file':name+'.glb','siderealRotationSeconds':period})
manifest['rocket']={'file':'crew_rocket.glb','forwardAxis':'+Y','assembly':'CoreStage, EscapeTower, four boosters, four core nozzles, booster nozzles, bands. Long March inspired original artwork.'}
(O/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
print('Created 10 GLB assets, actual geometry/material renders and animated preview.')
