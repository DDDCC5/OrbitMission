"""Package portable Android sources/assets and current verification records."""
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import hashlib

root=Path(__file__).resolve().parents[1]
out=root/'output/OrbitMission-source.zip'
files=[]
for item in ['app/src','gradle','assets','docs']:
    files.extend(p for p in (root/item).rglob('*') if p.is_file() and p.name!='.DS_Store')
for item in ['README.md','ASSET_REVIEW.md','build.gradle.kts','settings.gradle.kts','gradle.properties','gradlew','gradlew.bat','app/build.gradle.kts','app/proguard-rules.pro','.gitignore']:
    p=root/item
    if p.exists():files.append(p)
for pattern in ['comfort-*','focus-*','pair-*','size-*']:
    for p in (root/'reports').glob(pattern):
        if p.is_file() and p.suffix in ['.json','.log','.txt']:files.append(p)
for item in ['gltf-validation.json','texture-downloads.json','apk-signature-1.3.txt','apk-badging-1.3.txt']:
    p=root/'reports'/item
    if p.exists():files.append(p)
for p in (root/'tools').iterdir():
    if p.is_file() and p.suffix in ['.py','.cjs']:files.append(p)
with ZipFile(out,'w',ZIP_DEFLATED,compresslevel=6) as archive:
    for p in sorted(set(files)):archive.write(p,'OrbitMission/'+str(p.relative_to(root)))
with ZipFile(out) as archive:
    assert archive.testzip() is None
    for required in ['app/src/main/AndroidManifest.xml','gradle/wrapper/gradle-wrapper.jar','app/src/main/assets/models/sun.glb']:
        assert 'OrbitMission/'+required in archive.namelist()
    print('Archive verified',len(archive.namelist()),'files',out.stat().st_size,'bytes')
artifacts=['OrbitMission-debug.apk','OrbitMission-source.zip']
(root/'output/SHA256SUMS.txt').write_text(''.join(hashlib.sha256((root/'output'/name).read_bytes()).hexdigest()+'  '+name+'\n' for name in artifacts))
