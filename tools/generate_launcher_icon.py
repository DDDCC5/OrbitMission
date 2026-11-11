"""Render the approved launcher emblem; keep Android adaptive safe-zone padding."""
from pathlib import Path
import re
import resvg_py

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
svg = (ROOT / 'assets/ui/launcher-icon.svg').read_text()
assert '<text' not in svg
mark = re.search(r'<g transform=.*?</g>', svg).group()

def render(body, size=1024, viewbox='0 0 512 512'):
    return resvg_py.svg_to_bytes(svg_string=f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="{viewbox}">{body}</svg>')

# Adaptive foreground uses 108/72 padding so the launcher crop preserves the approved proportions.
(RES / 'drawable/orbit_icon_foreground.png').write_bytes(render(mark, viewbox='-128 -128 768 768'))
# PICO uses the existing circular scaffold SDF as one solid identity layer.
spatial = render('<circle cx="256" cy="256" r="256" fill="#06111f"/>' + mark)
(RES / 'drawable/icon_3d_layer_0.png').write_bytes(spatial)
(RES / 'mipmap-xxxhdpi/ic_spatial_launcher.png').write_bytes(spatial)
(ROOT / 'assets/preview/launcher-icon.png').write_bytes(render('<rect width="512" height="512" fill="#06111f"/>' + mark, size=512))
print('Generated Android and single-layer PICO launcher icons')
