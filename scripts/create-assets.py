from PIL import Image, ImageDraw
import os
import wave
import urllib.request

ASSETS = 'android/src/main/assets'
os.makedirs(f'{ASSETS}/textures', exist_ok=True)
os.makedirs(f'{ASSETS}/sounds', exist_ok=True)
os.makedirs(f'{ASSETS}/music', exist_ok=True)
os.makedirs(f'{ASSETS}/fonts', exist_ok=True)

# 菜单背景
img = Image.new('RGB', (1920, 1080), (34, 139, 34))
pixels = img.load()
for y in range(1080):
    for x in range(1920):
        if y > 600:
            g = 139 - int((y - 600) / 480 * 80)
            pixels[x, y] = (20, max(g, 60), 20)
img.save(f'{ASSETS}/textures/menu_bg.png')
print('Created menu_bg.png')

# 地图缩略图
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

# 图标
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

# 音频
def create_wav(path, duration=0.5, sample_rate=22050):
    nframes = int(duration * sample_rate)
    with wave.open(path, 'w') as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate)
        wav.writeframes(b'\x00' * (nframes * 2))

create_wav(f'{ASSETS}/sounds/button_click.wav', 0.1)
create_wav(f'{ASSETS}/sounds/climb.wav', 1.0)
create_wav(f'{ASSETS}/sounds/summit.wav', 0.5)
create_wav(f'{ASSETS}/music/bgm.wav', 3.0)
print('Created audio files')

# 下载中文字体（约8MB）
print('Downloading Chinese font...')
try:
    urllib.request.urlretrieve(
        'https://github.com/googlefonts/noto-cjk/raw/main/Sans/OTF/SimplifiedChinese/NotoSansSC-Regular.otf',
        f'{ASSETS}/fonts/NotoSansSC.otf'
    )
    print('Downloaded font')
except Exception as e:
    print(f'Font download failed: {e}')

print('Done!')
