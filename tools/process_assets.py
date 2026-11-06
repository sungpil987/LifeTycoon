from PIL import Image
from collections import deque
from pathlib import Path

root=Path("app/src/main/assets")
out=root/"processed"
out.mkdir(exist_ok=True)
nav=Image.open(root/"file_00000000c3248209a554b1caf956df3c.png").convert("RGBA")
w,h=nav.size
for i in range(6):
    tile=nav.crop((round(i*w/6),round(h*.10),round((i+1)*w/6),round(h*.91)))
    # Only remove the black backdrop connected to the tile edge.
    # Dark outlines enclosed inside the artwork remain intact.
    pix=tile.load();tw,th=tile.size
    seen=bytearray(tw*th)
    q=deque()
    for x in range(tw):
        q.append((x,0));q.append((x,th-1))
    for y in range(th):
        q.append((0,y));q.append((tw-1,y))
    while q:
        x,y=q.popleft()
        if x<0 or y<0 or x>=tw or y>=th:continue
        idx=y*tw+x
        if seen[idx]:continue
        seen[idx]=1
        r,g,b,a=pix[x,y]
        if max(r,g,b)>45 or a==0:continue
        pix[x,y]=(r,g,b,0)
        q.extend(((x+1,y),(x-1,y),(x,y+1),(x,y-1)))
    bbox=tile.getbbox()
    if bbox:tile=tile.crop(bbox)
    target=Image.new("RGBA",(180,180))
    tile.thumbnail((164,164),Image.Resampling.LANCZOS)
    target.alpha_composite(tile,((180-tile.width)//2,(180-tile.height)//2))
    target.save(out/f"nav_{i}.png",optimize=True)
print("Processed six navigation icons")
