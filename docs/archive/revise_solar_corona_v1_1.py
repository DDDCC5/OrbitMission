"""Author rooted prominence ribbons and a soft corona; no replacement planet textures."""
from pathlib import Path
import json, shutil
import numpy as np
from PIL import Image
import trimesh
from trimesh.visual.material import PBRMaterial

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'assets/models'
rng=np.random.default_rng(1102)
old=trimesh.load(OUT/'sun.glb',force='scene')
body=old.geometry['sun'].copy()
scene=trimesh.Scene()
scene.add_geometry(body,node_name='Body',geom_name='sun')

def material(name,texture,color=(255,145,36,255)):
    return PBRMaterial(name=name,baseColorTexture=texture,baseColorFactor=color,
        emissiveTexture=texture,emissiveFactor=[1,.55,.15],roughnessFactor=1,
        metallicFactor=0,alphaMode='BLEND',doubleSided=True)

# A sampled Gaussian cross section makes the ribbons translucent rather than solid tubes.
stripe=np.zeros((32,128,4),dtype=np.uint8)
x=np.linspace(-1,1,128)
stripe[:,:,:3]=[255,190,80]
stripe[:,:,3]=(np.exp(-x*x*8)*235).astype(np.uint8)[None,:]
plasma=material('FineProminencePlasma',Image.fromarray(stripe))
paths=[]
for i in range(12):
    phi=2*np.pi*i/12+.17
    latitude=rng.uniform(-.55,.55)
    n=np.array([np.sin(phi)*np.cos(latitude),np.sin(latitude),np.cos(phi)*np.cos(latitude)])
    t=np.cross(n,[0,1,0]);t/=np.linalg.norm(t)
    b=np.cross(n,t)
    u=np.linspace(0,1,65)
    span=rng.uniform(.10,.19);height=rng.uniform(.08,.23)
    radial=n[None,:]*np.cos((u-.5)*2*span)[:,None]+t[None,:]*np.sin((u-.5)*2*span)[:,None]
    curve=radial*(1.0003+height*np.sin(np.pi*u)**.85)[:,None]
    # Numerical asset gate: each magnetic loop has two photospheric footpoints.
    assert abs(np.linalg.norm(curve[0])-1.0003)<1e-6
    assert abs(np.linalg.norm(curve[-1])-1.0003)<1e-6
    assert np.linalg.norm(curve,axis=1).min()>=1
    paths.append(curve.tolist())
    meshes=[]
    for strand in range(4):
        offset=(strand-1.5)*.004
        c=curve+b[None,:]*(offset*np.sin(np.pi*u))[:,None]
        for layer,width in enumerate([.011,.003]):
            vertices=[];uv=[];faces=[]
            for k,p in enumerate(c):
                # Two crossed wispy ribbons retain volume from oblique views.
                tangent=c[min(k+1,64)]-c[max(k-1,0)]
                side=b if layer==0 else np.cross(tangent,b)/np.linalg.norm(np.cross(tangent,b))
                w=width*(.25+.75*np.sin(np.pi*u[k])**.35)
                vertices.extend([p-side*w,p+side*w]);uv.extend([[0,u[k]],[1,u[k]]])
                if k<64:
                    a=k*2;faces.extend([[a,a+2,a+1],[a+1,a+2,a+3]])
            m=trimesh.Trimesh(vertices,faces,process=False)
            m.visual=trimesh.visual.TextureVisuals(uv=uv,material=plasma);meshes.append(m)
    merged=trimesh.util.concatenate(meshes)
    scene.add_geometry(merged,node_name=f'CoronaLoop_{i:02d}',geom_name=f'CoronaLoop_{i:02d}')

# Low-contrast irregular corona is concentrated at the limb and decays smoothly outward.
yy,xx=np.mgrid[-1.4:1.4:512j,-1.4:1.4:512j]
r=np.sqrt(xx*xx+yy*yy);angle=np.arctan2(yy,xx)
irregular=.65+.18*np.sin(angle*7+.7)+.10*np.sin(angle*19)+.07*np.sin(angle*37-r*17)
alpha=np.clip((r-.997)/.018,0,1)*np.exp(-np.maximum(r-1.01,0)*17)*irregular*.24*np.clip((1.32-r)/.10,0,1)
halo=np.zeros((512,512,4),dtype=np.uint8);halo[:,:,:3]=[255,122,35];halo[:,:,3]=(np.clip(alpha,0,1)*255).astype(np.uint8)
halo_mat=material('SoftCoronaGlow',Image.fromarray(halo))
vertices=[];uv=[];faces=[]
# Round annular geometry avoids the faint rectangular blend footprint seen in native rendering.
for row,radius in enumerate(np.linspace(1.0,1.32,24)):
    for k,a in enumerate(np.linspace(0,2*np.pi,193)):
        x,y=radius*np.cos(a),radius*np.sin(a)
        vertices.append([x,y,0]);uv.append([(x+1.4)/2.8,(y+1.4)/2.8])
        if row<23 and k<192:
            index=row*193+k;faces.extend([[index,index+1,index+193],[index+1,index+194,index+193]])
plane=trimesh.Trimesh(vertices,faces,process=False)
plane.visual=trimesh.visual.TextureVisuals(uv=uv,material=halo_mat)
scene.add_geometry(plane,node_name='CoronaHalo',geom_name='CoronaHalo')

dot=trimesh.creation.icosphere(subdivisions=1,radius=.0025)
dot.visual=trimesh.visual.TextureVisuals(material=PBRMaterial(name='FlowingPlasma',baseColorFactor=[255,217,125,225],emissiveFactor=[1,.7,.25],alphaMode='BLEND'))
scene.geometry['PlasmaBead']=dot
for i,curve in enumerate(paths):
    for j in range(4):
        transform=np.eye(4);transform[:3,3]=curve[j*16]
        scene.graph.update(frame_to=f'Plasma_{i:02d}_{j}',frame_from=scene.graph.base_frame,matrix=transform,geometry='PlasmaBead')

(OUT/'sun.glb').write_bytes(scene.export(file_type='glb'))
(OUT/'corona-paths.json').write_text(json.dumps({'paths':paths},separators=(',',':')))
for name in ['sun.glb','corona-paths.json']:
    shutil.copy2(OUT/name,ROOT/'app/src/main/assets/models'/name)
print('Authored',len(paths),'rooted loops, 48 flowing plasma markers, soft halo; Sun GLB', (OUT/'sun.glb').stat().st_size,'bytes')
