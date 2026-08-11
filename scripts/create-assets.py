from PIL import Image, ImageDraw
import os
import shutil
import wave

ASSETS = 'android/src/main/assets'
for d in ['textures', 'sounds', 'music', 'fonts']:
    os.makedirs(f'{ASSETS}/{d}', exist_ok=True)

# ===== 复制系统字体 =====
font_paths = [
    '/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.otf',
    '/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc',
    '/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc',
    '/usr/share/fonts/truetype/wqy/wqy-microhei.ttc',
]

font_copied = False
for path in font_paths:
    if os.path.exists(path):
        try:
            shutil.copy2(path, f'{ASSETS}/fonts/NotoSansSC.otf')
            size = os.path.getsize(f'{ASSETS}/fonts/NotoSansSC.otf')
            print(f'Font copied: {path} ({size} bytes)')
            font_copied = True
            break
        except Exception as e:
            print(f'Failed: {path}: {e}')

if not font_copied:
    print('WARNING: No font found!')

# ===== 生成图片 =====
img = Image.new('RGB', (1920, 1080), (34, 139, 34))
pixels = img.load()
for y in range(1080):
    for x in range(1920):
        if y > 600:
            g = 139 - int((y - 600) / 480 * 80)
            pixels[x, y] = (20, max(g, 60), 20)
img.save(f'{ASSETS}/textures/menu_bg.png')
print('Created menu_bg.png')

img = Image.new('RGB', (512, 512), (70, 130, 180))
pixels = img.load()
for y in range(512):
    for x in range(512):
        cx, cy = 256, 400
        dist = ((x-cx)**2 + (y-cy)**2) ** 0.5
        if dist < 200 and y < cy:
            h = 1 - dist / 200
            if y < cy - h * 150:
                g = int(200 + h * 55)
                pixels[x, y] = (34, min(g, 255), 34)
img.save(f'{ASSETS}/textures/map_houshan.png')
print('Created map_houshan.png')

sizes = {'mdpi':48, 'hdpi':72, 'xhdpi':96, 'xxhdpi':144, 'xxxhdpi':192}
for name, size in sizes.items():
    os.makedirs(f'android/src/main/res/mipmap-{name}', exist_ok=True)
    img = Image.new('RGBA', (size, size), (76, 175, 80, 255))
    draw = ImageDraw.Draw(img)
    margin = size // 8
    peak_x = size // 2
    peak_y = margin + size // 10
    base_left = margin
    base_right = size - margin
    base_y = size - margin
    draw.polygon([(peak_x, peak_y), (base_left, base_y), (base_right, base_y)], fill=(255, 255, 255, 255))
    snow_size = max(size // 15, 2)
    draw.ellipse([peak_x - snow_size, peak_y - snow_size, peak_x + snow_size, peak_y + snow_size], fill=(255, 255, 255, 200))
    img.save(f'android/src/main/res/mipmap-{name}/ic_launcher.png')
    print(f'Created icon {name}')

# ===== 生成测试音 =====
def create_tone(path, duration=0.5, freq=440, sample_rate=22050):
    import struct, math
    nframes = int(duration * sample_rate)
    with wave.open(path, 'w') as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate)
        data = b''.join(struct.pack('<h', int(2000 * math.sin(2 * math.pi * freq * t / sample_rate))) for t in range(nframes))
        wav.writeframes(data)

create_tone(f'{ASSETS}/sounds/button_click.wav', 0.1, 880)
create_tone(f'{ASSETS}/sounds/climb.wav', 1.0, 220)
create_tone(f'{ASSETS}/sounds/summit.wav', 0.5, 660)
create_tone(f'{ASSETS}/music/bgm.wav', 3.0, 330)
print('Created audio')

# 列出所有文件
print('\n=== Generated ===')
for root, dirs, files in os.walk(ASSETS):
    for f in files:
        path = os.path.join(root, f)
        print(f'  {path}: {os.path.getsize(path)} bytes')
