from PIL import Image, ImageDraw
import os

# 创建游戏资源目录
os.makedirs('android/assets/textures', exist_ok=True)
os.makedirs('android/assets/sounds', exist_ok=True)
os.makedirs('android/assets/music', exist_ok=True)
os.makedirs('android/assets/fonts', exist_ok=True)

# 生成菜单背景
img = Image.new('RGB', (1920, 1080), (34, 139, 34))
pixels = img.load()
for y in range(1080):
    for x in range(1920):
        if y > 600:
            g = 139 - int((y - 600) / 480 * 80)
            pixels[x, y] = (20, max(g, 60), 20)
img.save('android/assets/textures/menu_bg.png')
print('Created menu_bg.png')

# 生成地图缩略图
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
img.save('android/assets/textures/map_houshan.png')
print('Created map_houshan.png')

# 生成 Android 图标
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
    print(f'Created icon {name}: {size}x{size}')

# 创建空音频文件
open('android/assets/sounds/button_click.mp3', 'w').close()
open('android/assets/sounds/climb.mp3', 'w').close()
open('android/assets/sounds/summit.mp3', 'w').close()
open('android/assets/music/bgm.mp3', 'w').close()
print('Done!')
