"""生成 LibGDX 位图字体（.fnt + .png），不需要 FreeType"""
from PIL import Image, ImageDraw, ImageFont
import os
import sys

ASSETS = 'android/src/main/assets'
os.makedirs(f'{ASSETS}/fonts', exist_ok=True)

all_chars = (
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    ".,:;!?-+/()[]{}%&$#@*_= '"'"'"'"'"'"|\\~`"
    "开始游戏继续设置更新日志检查退出后山选择地图进入"
    "音效音量背景音乐视角灵敏度保存返回主菜单"
    "V初始版本D爬山热系统关闭成功登顶保存进度"
    "暂停继续确认取消"
    "这是一款位于北郊的后山海拔不高但地形复杂几座凸起的山峰连绵起伏"
    "山顶有平坦的平台挑战者需要从山脚出发沿着斜坡攀登最终登顶主峰"
    "俯瞰整个山谷注意边界有空气墙保护请勿尝试越界"
    "发现新版本当前已是最新版本正在检查更新"
    "服务器返回空数据版本数据解析失败网络请求失败下载补丁失败"
    "补丁解压失败热更新完成请重启游戏生效"
    "提示确定关闭新版本更新说明下载安装"
    "%"
)

seen = set()
chars = ''.join(c for c in all_chars if not (c in seen or seen.add(c)))
print(f"Generating bitmap font for {len(chars)} characters")

font_paths = [
    '/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc',
    '/usr/share/fonts/truetype/wqy/wqy-microhei.ttc',
    '/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.otf',
    '/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc',
    '/usr/share/fonts/truetype/arphic/uming.ttc',
    '/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf',
]

font_path = None
for p in font_paths:
    if os.path.exists(p):
        font_path = p
        print(f"Using font: {p}")
        break

if not font_path:
    print("ERROR: No Chinese font found!")
    sys.exit(1)

font_size = 32
try:
    font = ImageFont.truetype(font_path, font_size)
except Exception as e:
    print(f"Font load error: {e}")
    sys.exit(1)

char_info = []
max_w = 0
max_h = 0
for char in chars:
    bbox = font.getbbox(char)
    if bbox:
        w = bbox[2] - bbox[0] + 4
        h = bbox[3] - bbox[1] + 4
    else:
        w = font_size
        h = font_size
    char_info.append((char, w, h))
    max_w = max(max_w, w)
    max_h = max(max_h, h)

line_h = max_h
chars_per_row = 32
num_rows = (len(chars) + chars_per_row - 1) // chars_per_row
tex_w = chars_per_row * max_w
tex_h = num_rows * line_h

if tex_w > 4096:
    chars_per_row = 4096 // max_w
    num_rows = (len(chars) + chars_per_row - 1) // chars_per_row
    tex_w = chars_per_row * max_w
    tex_h = num_rows * line_h

print(f"Texture size: {tex_w}x{tex_h}")

img = Image.new('RGBA', (tex_w, tex_h), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

char_positions = {}
for i, (char, w, h) in enumerate(char_info):
    row = i // chars_per_row
    col = i % chars_per_row
    x = col * max_w + (max_w - w) // 2
    y = row * line_h + (line_h - h) // 2
    draw.text((x, y), char, font=font, fill=(255, 255, 255, 255))
    char_positions[char] = (x, y, w, h)

png_path = f'{ASSETS}/fonts/game_font.png'
img.save(png_path)
print(f"Saved: {png_path}")

fnt_path = f'{ASSETS}/fonts/game_font.fnt'
with open(fnt_path, 'w', encoding='utf-8') as f:
    f.write(f'info face="GameFont" size={font_size} bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=1,1\n')
    f.write(f'common lineHeight={line_h} base={font_size} scaleW={tex_w} scaleH={tex_h} pages=1 packed=0\n')
    f.write('page id=0 file="game_font.png"\n')
    f.write(f'chars count={len(chars)}\n')
    for char, (x, y, w, h) in char_positions.items():
        code = ord(char)
        f.write(f'char id={code}   x={x}     y={y}     width={w}    height={h}    xoffset=0     yoffset=0     xadvance={w}    page=0  chnl=15\n')
    f.write('kernings count=0\n')

print(f"Saved: {fnt_path}")
print("Font generation complete!")
