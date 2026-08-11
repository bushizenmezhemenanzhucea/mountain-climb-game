#!/bin/bash
# ============================================
# 3D爬山游戏 - 完整修复脚本
# 在 Termux 执行：cd ~/storage/shared/mountain-climb-game && bash push-all-fixes.sh
# ============================================
set -e
cd ~/storage/shared/mountain-climb-game

echo "=========================================="
echo "3D爬山游戏 - 完整修复"
echo "=========================================="

# ----------------------------------------
# 1. android/build.gradle
# ----------------------------------------
cat > android/build.gradle << 'GRADLE'
plugins {
    id 'com.android.application'
}

android {
    compileSdkVersion 33
    defaultConfig {
        applicationId "com.mountainclimb.game"
        minSdkVersion 21
        targetSdkVersion 33
        versionCode 1
        versionName "1.0.0"
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    sourceSets {
        main {
            jniLibs.srcDirs = ['src/main/jniLibs']
        }
    }
}

dependencies {
    implementation project(':core')
    implementation "com.badlogicgames.gdx:gdx-backend-android:$gdxVersion"
    implementation "com.badlogicgames.gdx:gdx-bullet:$gdxVersion"
    implementation "com.badlogicgames.gdx:gdx-freetype:$gdxVersion"
}
GRADLE
echo "[OK] android/build.gradle"

# ----------------------------------------
# 2. scripts/create-font.py（生成位图字体）
# ----------------------------------------
cat > scripts/create-font.py << 'PYEOF'
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
PYEOF
echo "[OK] scripts/create-font.py"

# ----------------------------------------
# 3. scripts/create-assets.py
# ----------------------------------------
cat > scripts/create-assets.py << 'PYEOF'
from PIL import Image, ImageDraw
import os
import subprocess
import wave
import struct
import math

ASSETS = 'android/src/main/assets'
for d in ['textures', 'sounds', 'music', 'fonts']:
    os.makedirs(f'{ASSETS}/{d}', exist_ok=True)

# 生成字体
print("=== Generating font ===")
result = subprocess.run(['python3', 'scripts/create-font.py'], capture_output=True, text=True)
print(result.stdout)
if result.returncode != 0:
    print("Font generation failed:", result.stderr)

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
def create_tone(path, duration=0.5, freq=440, sample_rate=22050):
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

# 验证
print('\n=== Assets ===')
for root, dirs, files in os.walk(ASSETS):
    for f in files:
        path = os.path.join(root, f)
        print(f'  {path}: {os.path.getsize(path)} bytes')

print('\nDone!')
PYEOF
echo "[OK] scripts/create-assets.py"

# ----------------------------------------
# 4. MountainClimbGame.java（位图字体 + WindowStyle）
# ----------------------------------------
cat > core/src/main/java/com/mountainclimb/game/MountainClimbGame.java << 'JAVA'
package com.mountainclimb.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mountainclimb.game.audio.AudioManager;
import com.mountainclimb.game.screen.MainMenuScreen;

public class MountainClimbGame extends Game {
    private Skin skin;
    private BitmapFont font;

    @Override
    public void create() {
        try { AudioManager.getInstance().loadSounds(); } catch (Exception e) {}
        try { AudioManager.getInstance().loadBGM(); } catch (Exception e) {}
        createSkin();
        setScreen(new MainMenuScreen(this));
    }

    private void createSkin() {
        skin = new Skin();
        font = loadBitmapFont();
        skin.add("default", font);

        Pixmap defaultPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        defaultPixmap.setColor(1f, 1f, 1f, 1f);
        defaultPixmap.fill();
        TextureRegionDrawable defaultDrawable = new TextureRegionDrawable(new Texture(defaultPixmap));
        skin.add("default", defaultDrawable);

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", labelStyle);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.downFontColor = Color.YELLOW;

        Pixmap btnUp = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        btnUp.setColor(0.2f, 0.25f, 0.3f, 0.8f);
        btnUp.fill();
        buttonStyle.up = new TextureRegionDrawable(new Texture(btnUp));

        Pixmap btnDown = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        btnDown.setColor(0.3f, 0.4f, 0.5f, 0.9f);
        btnDown.fill();
        buttonStyle.down = new TextureRegionDrawable(new Texture(btnDown));

        skin.add("default", buttonStyle);

        Window.WindowStyle windowStyle = new Window.WindowStyle(font, Color.WHITE, defaultDrawable);
        skin.add("default", windowStyle);
        skin.add("dialog", windowStyle);

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        Pixmap knobPixmap = new Pixmap(30, 30, Pixmap.Format.RGBA8888);
        knobPixmap.setColor(Color.LIGHT_GRAY);
        knobPixmap.fillCircle(15, 15, 14);
        sliderStyle.knob = new TextureRegionDrawable(new Texture(knobPixmap));

        Pixmap bgPixmap = new Pixmap(200, 10, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(0.3f, 0.3f, 0.3f, 1f);
        bgPixmap.fill();
        sliderStyle.background = new TextureRegionDrawable(new Texture(bgPixmap));
        skin.add("default-horizontal", sliderStyle);

        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        skin.add("default", scrollStyle);
    }

    private BitmapFont loadBitmapFont() {
        try {
            com.badlogic.gdx.files.FileHandle fntFile = Gdx.files.internal("fonts/game_font.fnt");
            com.badlogic.gdx.files.FileHandle pngFile = Gdx.files.internal("fonts/game_font.png");
            if (fntFile.exists() && pngFile.exists()) {
                BitmapFont f = new BitmapFont(fntFile, pngFile, false);
                Gdx.app.log("Font", "Bitmap font loaded: " + pngFile.length() + " bytes");
                return f;
            }
        } catch (Exception e) {
            Gdx.app.error("Font", "Bitmap font failed: " + e.getMessage());
        }
        BitmapFont defaultFont = new BitmapFont();
        defaultFont.getData().setScale(2f);
        return defaultFont;
    }

    public Skin getSkin() { return skin; }
    public BitmapFont getFont() { return font; }

    @Override
    public void dispose() {
        super.dispose();
        if (skin != null) skin.dispose();
        if (font != null) font.dispose();
        AudioManager.getInstance().dispose();
    }
}
JAVA
echo "[OK] MountainClimbGame.java"

# ----------------------------------------
# 5. UpdateManager.java（完整热更新）
# ----------------------------------------
cat > core/src/main/java/com/mountainclimb/game/update/UpdateManager.java << 'JAVA'
package com.mountainclimb.game.update;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.mountainclimb.game.GameConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UpdateManager {
    private static final String TAG = "UpdateManager";
    private UpdateListener listener;
    private VersionInfo localVersion;
    private boolean checking = false;

    public UpdateManager() {
        this.localVersion = new VersionInfo(GameConfig.VERSION_CODE, GameConfig.VERSION);
    }

    public void setListener(UpdateListener listener) {
        this.listener = listener;
    }

    public void checkForUpdate() {
        if (checking) return;
        checking = true;

        String versionUrl = GameConfig.GITHUB_RAW_BASE + GameConfig.UPDATE_VERSION_FILE;
        Gdx.app.log(TAG, "Checking: " + versionUrl);

        try {
            Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
            request.setUrl(versionUrl);
            request.setTimeOut(15000);

            Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    checking = false;
                    try {
                        int status = httpResponse.getStatus().getStatusCode();
                        Gdx.app.log(TAG, "HTTP status: " + status);
                        if (status != 200) {
                            notifyNoUpdate("服务器返回 " + status);
                            return;
                        }
                        String body = httpResponse.getResultAsString();
                        if (body == null || body.isEmpty()) {
                            notifyNoUpdate("服务器返回空数据");
                            return;
                        }
                        Json json = new Json();
                        VersionInfo remoteVersion = json.fromJson(VersionInfo.class, body);
                        if (remoteVersion == null) {
                            notifyNoUpdate("版本数据解析失败");
                            return;
                        }
                        if (remoteVersion.versionCode > localVersion.versionCode) {
                            Gdx.app.log(TAG, "New version: " + remoteVersion.versionName);
                            final VersionInfo finalRemote = remoteVersion;
                            Gdx.app.postRunnable(() -> {
                                if (listener != null) listener.onUpdateFound(finalRemote);
                            });
                        } else {
                            notifyNoUpdate("当前已是最新版本 (v" + localVersion.versionName + ")");
                        }
                    } catch (Exception e) {
                        Gdx.app.error(TAG, "Parse error: " + e.getMessage());
                        notifyNoUpdate("版本数据解析失败: " + e.getMessage());
                    }
                }
                @Override public void failed(Throwable t) {
                    checking = false;
                    Gdx.app.error(TAG, "Request failed: " + t.getMessage());
                    notifyNoUpdate("网络请求失败: " + t.getMessage());
                }
                @Override public void cancelled() {
                    checking = false;
                    notifyNoUpdate("请求已取消");
                }
            });
        } catch (Exception e) {
            checking = false;
            notifyNoUpdate("检查更新失败: " + e.getMessage());
        }
    }

    public void downloadUpdate(VersionInfo versionInfo) {
        String patchUrl = GameConfig.GITHUB_RAW_BASE + GameConfig.UPDATE_PATCH_FILE;
        Gdx.app.log(TAG, "Downloading: " + patchUrl);
        try {
            Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
            request.setUrl(patchUrl);
            request.setTimeOut(30000);
            Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    try {
                        if (httpResponse.getStatus().getStatusCode() != 200) {
                            notifyDownloadFailed("下载失败: HTTP " + httpResponse.getStatus().getStatusCode());
                            return;
                        }
                        byte[] bytes = httpResponse.getResult();
                        if (bytes == null || bytes.length == 0) {
                            notifyDownloadFailed("下载的数据为空");
                            return;
                        }
                        Gdx.app.log(TAG, "Downloaded " + bytes.length + " bytes");
                        if (extractPatch(bytes)) {
                            notifyDownloadComplete(versionInfo);
                        } else {
                            notifyDownloadFailed("补丁解压失败");
                        }
                    } catch (Exception e) {
                        notifyDownloadFailed("下载处理失败: " + e.getMessage());
                    }
                }
                @Override public void failed(Throwable t) {
                    notifyDownloadFailed("下载失败: " + t.getMessage());
                }
                @Override public void cancelled() {
                    notifyDownloadFailed("下载已取消");
                }
            });
        } catch (Exception e) {
            notifyDownloadFailed("下载启动失败: " + e.getMessage());
        }
    }

    private boolean extractPatch(byte[] patchData) {
        try {
            FileHandle updateDir = Gdx.files.external(GameConfig.UPDATE_DIR);
            updateDir.mkdirs();
            ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(patchData));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    updateDir.child(entry.getName()).mkdirs();
                    continue;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                FileHandle outFile = updateDir.child(entry.getName());
                outFile.parent().mkdirs();
                outFile.writeBytes(baos.toByteArray(), false);
                Gdx.app.log(TAG, "Extracted: " + entry.getName());
            }
            zis.close();
            FileHandle versionFile = updateDir.child(GameConfig.UPDATE_VERSION_FILE);
            versionFile.writeString("{\"versionCode\":" + (localVersion.versionCode + 1) + ",\"versionName\":\"\"}", false);
            return true;
        } catch (Exception e) {
            Gdx.app.error(TAG, "Extract failed: " + e.getMessage());
            return false;
        }
    }

    private void notifyNoUpdate(String message) {
        Gdx.app.postRunnable(() -> {
            if (listener != null) listener.onNoUpdate(message);
        });
    }
    private void notifyDownloadFailed(String message) {
        Gdx.app.postRunnable(() -> {
            if (listener != null) listener.onDownloadFailed(message);
        });
    }
    private void notifyDownloadComplete(VersionInfo version) {
        Gdx.app.postRunnable(() -> {
            if (listener != null) listener.onDownloadComplete(version);
        });
    }
}
JAVA
echo "[OK] UpdateManager.java"

# ----------------------------------------
# 6. UpdateListener.java
# ----------------------------------------
cat > core/src/main/java/com/mountainclimb/game/update/UpdateListener.java << 'JAVA'
package com.mountainclimb.game.update;

public interface UpdateListener {
    void onUpdateFound(VersionInfo newVersion);
    void onNoUpdate(String message);
    void onDownloadComplete(VersionInfo version);
    void onDownloadFailed(String message);
}
JAVA
echo "[OK] UpdateListener.java"

# ----------------------------------------
# 7. AudioManager.java（.wav）
# ----------------------------------------
cat > core/src/main/java/com/mountainclimb/game/audio/AudioManager.java << 'JAVA'
package com.mountainclimb.game.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.mountainclimb.game.GameConfig;
import com.mountainclimb.game.save.SaveManager;

public class AudioManager {
    private static AudioManager instance;
    private Music bgm;
    private Sound btnSound;
    private Sound climbSound;
    private Sound summitSound;
    private float soundVolume = 0.7f;
    private float musicVolume = 0.5f;
    private boolean bgmPlaying = false;

    private AudioManager() {
        loadFromSave();
    }

    public static AudioManager getInstance() {
        if (instance == null) instance = new AudioManager();
        return instance;
    }

    public void loadFromSave() {
        soundVolume = SaveManager.getInstance().getSoundVolume();
        musicVolume = SaveManager.getInstance().getMusicVolume();
    }

    private FileHandle getAudioFile(String path) {
        FileHandle external = Gdx.files.external(GameConfig.UPDATE_DIR + "/" + path);
        if (external.exists()) return external;
        return Gdx.files.internal(path);
    }

    public void loadSounds() {
        try { btnSound = Gdx.audio.newSound(getAudioFile("sounds/button_click.wav")); } catch (Exception e) {}
        try { climbSound = Gdx.audio.newSound(getAudioFile("sounds/climb.wav")); } catch (Exception e) {}
        try { summitSound = Gdx.audio.newSound(getAudioFile("sounds/summit.wav")); } catch (Exception e) {}
    }

    public void loadBGM() {
        try {
            if (bgm != null) { bgm.stop(); bgm.dispose(); }
            bgm = Gdx.audio.newMusic(getAudioFile("music/bgm.wav"));
            bgm.setLooping(true);
            bgm.setVolume(musicVolume);
        } catch (Exception e) {}
    }

    public void playBGM() {
        if (bgm != null && !bgmPlaying) { bgm.play(); bgmPlaying = true; }
    }
    public void pauseBGM() {
        if (bgm != null && bgmPlaying) { bgm.pause(); bgmPlaying = false; }
    }
    public void stopBGM() {
        if (bgm != null) { bgm.stop(); bgmPlaying = false; }
    }

    public void playButtonSound() {
        if (btnSound != null) btnSound.play(soundVolume);
    }
    public void playClimbSound() {
        if (climbSound != null) climbSound.loop(soundVolume);
    }
    public void stopClimbSound() {
        if (climbSound != null) climbSound.stop();
    }
    public void playSummitSound() {
        if (summitSound != null) summitSound.play(soundVolume);
    }

    public void setSoundVolume(float volume) {
        soundVolume = Math.max(0f, Math.min(1f, volume));
        SaveManager.getInstance().setSoundVolume(soundVolume);
    }
    public void setMusicVolume(float volume) {
        musicVolume = Math.max(0f, Math.min(1f, volume));
        SaveManager.getInstance().setMusicVolume(musicVolume);
        if (bgm != null) bgm.setVolume(musicVolume);
    }

    public void dispose() {
        if (btnSound != null) btnSound.dispose();
        if (climbSound != null) climbSound.dispose();
        if (summitSound != null) summitSound.dispose();
        if (bgm != null) bgm.dispose();
    }
}
JAVA
echo "[OK] AudioManager.java"

# ----------------------------------------
# 8. MainMenuScreen.java（安全 Dialog）
# ----------------------------------------
cat > core/src/main/java/com/mountainclimb/game/screen/MainMenuScreen.java << 'JAVA'
package com.mountainclimb.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mountainclimb.game.GameConfig;
import com.mountainclimb.game.MountainClimbGame;
import com.mountainclimb.game.audio.AudioManager;
import com.mountainclimb.game.save.SaveManager;
import com.mountainclimb.game.update.UpdateListener;
import com.mountainclimb.game.update.UpdateManager;
import com.mountainclimb.game.update.VersionInfo;

public class MainMenuScreen implements Screen, UpdateListener {
    private MountainClimbGame game;
    private Stage stage;
    private Viewport viewport;
    private Table rootTable;
    private ScrollPane scrollPane;
    private Table menuTable;
    private Label versionLabel;
    private TextButton btnContinue;
    private UpdateManager updateManager;
    private Dialog updateDialog;
    private Texture bgTexture;

    public MainMenuScreen(MountainClimbGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        viewport = new ScreenViewport();
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        try {
            bgTexture = new Texture(Gdx.files.internal("textures/menu_bg.png"));
            bgTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            Gdx.app.log("MainMenu", "Background not found");
        }

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        menuTable = new Table();
        menuTable.top();

        Skin skin = game.getSkin();
        float btnWidth = Gdx.graphics.getWidth() * 0.5f;
        float btnHeight = 70f;

        TextButton btnStart = new TextButton("开始游戏", skin);
        btnStart.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.setScreen(new MapSelectScreen(game));
            }
        });
        menuTable.add(btnStart).width(btnWidth).height(btnHeight).padTop(20f).row();

        btnContinue = new TextButton("继续游戏", skin);
        btnContinue.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.setScreen(new GameScreen(game, true));
            }
        });
        menuTable.add(btnContinue).width(btnWidth).height(btnHeight).padTop(20f).row();

        TextButton btnSettings = new TextButton("设置", skin);
        btnSettings.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.setScreen(new SettingsScreen(game));
            }
        });
        menuTable.add(btnSettings).width(btnWidth).height(btnHeight).padTop(20f).row();

        TextButton btnChangelog = new TextButton("更新日志", skin);
        btnChangelog.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                showChangelogDialog();
            }
        });
        menuTable.add(btnChangelog).width(btnWidth).height(btnHeight).padTop(20f).row();

        TextButton btnUpdate = new TextButton("检查更新", skin);
        btnUpdate.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                checkUpdate();
            }
        });
        menuTable.add(btnUpdate).width(btnWidth).height(btnHeight).padTop(20f).row();

        scrollPane = new ScrollPane(menuTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        rootTable.add(scrollPane).expand().fill().pad(50f).row();

        versionLabel = new Label("v" + GameConfig.VERSION, skin);
        versionLabel.setColor(Color.GRAY);
        rootTable.add(versionLabel).padBottom(10f);

        refreshContinueButton();

        updateManager = new UpdateManager();
        updateManager.setListener(this);

        AudioManager.getInstance().playBGM();
    }

    private void refreshContinueButton() {
        boolean hasProgress = SaveManager.getInstance().hasProgress();
        btnContinue.setDisabled(!hasProgress);
        if (!hasProgress) {
            btnContinue.getLabel().setColor(Color.GRAY);
        } else {
            btnContinue.getLabel().setColor(Color.WHITE);
        }
    }

    private void checkUpdate() {
        showInfoDialog("正在检查更新...");
        updateManager.checkForUpdate();
    }

    private void showChangelogDialog() {
        try {
            Dialog dialog = new Dialog("更新日志", game.getSkin(), "dialog") {
                @Override
                protected void result(Object object) {}
            };
            dialog.getContentTable().add(new Label(
                "V1.0.0 初始版本\n- 3D爬山游戏\n- 后山地图\n- 热更新系统",
                game.getSkin()
            )).pad(20f);
            dialog.button("关闭", true).padBottom(10f);
            dialog.show(stage);
        } catch (Exception e) {
            Gdx.app.error("MainMenu", "Changelog dialog error: " + e.getMessage());
            showInfoDialog("更新日志: V1.0.0 初始版本");
        }
    }

    private void showInfoDialog(String message) {
        try {
            if (updateDialog != null) updateDialog.hide();
            updateDialog = new Dialog("提示", game.getSkin(), "dialog") {
                @Override
                protected void result(Object object) {}
            };
            updateDialog.text(message);
            updateDialog.button("确定", true);
            updateDialog.show(stage);
        } catch (Exception e) {
            Gdx.app.error("MainMenu", "Dialog error: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateFound(VersionInfo newVersion) {
        try {
            if (updateDialog != null) updateDialog.hide();
            updateDialog = new Dialog("发现新版本", game.getSkin(), "dialog") {
                @Override
                protected void result(Object object) {
                    if (Boolean.TRUE.equals(object)) {
                        showInfoDialog("正在下载更新...");
                        updateManager.downloadUpdate(newVersion);
                    }
                }
            };
            updateDialog.text("发现新版本: v" + newVersion.versionName + "\n是否下载更新?");
            updateDialog.button("下载", true).pad(10f);
            updateDialog.button("取消", false).pad(10f);
            updateDialog.show(stage);
        } catch (Exception e) {
            Gdx.app.error("MainMenu", "Update dialog error: " + e.getMessage());
        }
    }

    @Override
    public void onNoUpdate(String message) {
        showInfoDialog(message);
    }

    @Override
    public void onDownloadComplete(VersionInfo version) {
        showInfoDialog("更新已下载完成!\n请重启游戏以应用更新。");
    }

    @Override
    public void onDownloadFailed(String message) {
        showInfoDialog("下载失败: " + message);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.3f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (bgTexture != null) {
            stage.getBatch().begin();
            stage.getBatch().draw(bgTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            stage.getBatch().end();
        }
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { Gdx.input.setInputProcessor(null); }
    @Override public void dispose() {
        stage.dispose();
        if (bgTexture != null) bgTexture.dispose();
    }
}
JAVA
echo "[OK] MainMenuScreen.java"

# ----------------------------------------
# 9. MapSelectScreen.java
# ----------------------------------------
cat > core/src/main/java/com/mountainclimb/game/screen/MapSelectScreen.java << 'JAVA'
package com.mountainclimb.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mountainclimb.game.MountainClimbGame;
import com.mountainclimb.game.audio.AudioManager;

public class MapSelectScreen implements Screen {
    private MountainClimbGame game;
    private Stage stage;
    private Texture mapPreviewTexture;

    public MapSelectScreen(MountainClimbGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        Skin skin = game.getSkin();
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label("选择地图", skin);
        title.setFontScale(1.5f);
        table.add(title).padBottom(30f).row();

        Table mapCard = new Table();
        try {
            Pixmap bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            bgPixmap.setColor(0.15f, 0.18f, 0.22f, 0.9f);
            bgPixmap.fill();
            mapCard.setBackground(new TextureRegionDrawable(new Texture(bgPixmap)));
        } catch (Exception e) {
            Gdx.app.error("MapSelect", "BG error: " + e.getMessage());
        }

        try {
            mapPreviewTexture = new Texture(Gdx.files.internal("textures/map_houshan.png"));
            Image preview = new Image(mapPreviewTexture);
            mapCard.add(preview).width(300f).height(180f).pad(15f).row();
        } catch (Exception e) {
            mapCard.add(new Label("[预览]", skin)).pad(20f).row();
        }

        Label mapName = new Label("后山", skin);
        mapName.setFontScale(1.3f);
        mapCard.add(mapName).padTop(10f).row();

        Label mapDesc = new Label(
            "这是一座位于北郊的后山，海拔不高但地形复杂。\n" +
            "几座凸起的山峰连绵起伏，山顶有平坦的平台。\n" +
            "挑战者需要从山脚出发，沿着斜坡攀登，\n" +
            "最终登顶主峰，俯瞰整个山谷。\n" +
            "注意：边界有空气墙保护，请勿尝试越界。",
            skin
        );
        mapDesc.setWrap(true);
        mapCard.add(mapDesc).width(400f).pad(15f).row();

        table.add(mapCard).padBottom(30f).row();

        TextButton btnEnter = new TextButton("进入后山", skin);
        btnEnter.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.setScreen(new GameScreen(game, false));
            }
        });
        table.add(btnEnter).width(200f).height(60f);

        TextButton btnBack = new TextButton("返回", skin);
        btnBack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.setScreen(new MainMenuScreen(game));
            }
        });
        table.add(btnBack).width(150f).height(60f).padLeft(20f);
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        stage.dispose();
        if (mapPreviewTexture != null) mapPreviewTexture.dispose();
    }
}
JAVA
echo "[OK] MapSelectScreen.java"

# ----------------------------------------
# 10. Player.java（第三人称 + 站立）
# ----------------------------------------
cat > core/src/main/java/com/mountainclimb/game/player/Player.java << 'JAVA'
package com.mountainclimb.game.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.mountainclimb.game.GameConfig;

public class Player {
    private Model playerModel;
    private ModelInstance playerInstance;
    private Vector3 position;
    private Vector3 velocity;
    private float yaw;
    private float pitch;
    private boolean grounded = true;
    private float height;
    private float radius;

    public Player(float startX, float startY, float startZ) {
        this.position = new Vector3(startX, startY, startZ);
        this.velocity = new Vector3(0, 0, 0);
        this.yaw = 0f;
        this.pitch = 20f;
        this.height = GameConfig.PLAYER_HEIGHT;
        this.radius = GameConfig.PLAYER_RADIUS;
        buildModel();
    }

    private void buildModel() {
        ModelBuilder builder = new ModelBuilder();
        Material bodyMat = new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.4f, 0.8f, 1f)));
        playerModel = builder.createCapsule(
            radius, height - radius * 2f, 16,
            bodyMat,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        playerInstance = new ModelInstance(playerModel);
        updateTransform();
    }

    public void move(Vector2 moveDir, float delta, float terrainHeight) {
        if (moveDir.len2() < GameConfig.JOYSTICK_DEADZONE * GameConfig.JOYSTICK_DEADZONE) {
            velocity.x = 0;
            velocity.z = 0;
            return;
        }
        float radYaw = (float)Math.toRadians(yaw);
        float sin = MathUtils.sin(radYaw);
        float cos = MathUtils.cos(radYaw);
        float worldMoveX = moveDir.x * cos + moveDir.y * sin;
        float worldMoveZ = -moveDir.x * sin + moveDir.y * cos;
        velocity.x = worldMoveX * GameConfig.PLAYER_SPEED;
        velocity.z = worldMoveZ * GameConfig.PLAYER_SPEED;
        position.x += velocity.x * delta;
        position.z += velocity.z * delta;
        if (moveDir.len2() > 0.01f) {
            float targetYaw = (float)Math.toDegrees(MathUtils.atan2(worldMoveX, worldMoveZ));
            yaw = lerpAngle(yaw, targetYaw, 5f * delta);
        }
    }

    public void applyGravity(float delta, float terrainHeight) {
        float feetY = position.y - height / 2f;
        if (feetY > terrainHeight + 0.1f) {
            velocity.y += GameConfig.GRAVITY * delta;
            position.y += velocity.y * delta;
            grounded = false;
        } else {
            position.y = terrainHeight + height / 2f;
            velocity.y = 0;
            grounded = true;
        }
    }

    public void rotate(float deltaYaw, float deltaPitch, float sensitivityMultiplier) {
        yaw += deltaYaw * sensitivityMultiplier;
        pitch += deltaPitch * sensitivityMultiplier;
        pitch = MathUtils.clamp(pitch, 10f, 60f);
        yaw = (yaw % 360f + 360f) % 360f;
    }

    public void updateTransform() {
        playerInstance.transform.idt();
        playerInstance.transform.translate(position);
        playerInstance.transform.rotate(Vector3.Y, yaw);
    }

    public Vector3 getCameraPosition() {
        float radYaw = (float)Math.toRadians(yaw);
        float radPitch = (float)Math.toRadians(pitch);
        float camDist = GameConfig.CAMERA_DISTANCE;
        float camYaw = radYaw + MathUtils.PI;
        float camX = position.x + MathUtils.sin(camYaw) * MathUtils.cos(radPitch) * camDist;
        float camZ = position.z + MathUtils.cos(camYaw) * MathUtils.cos(radPitch) * camDist;
        float camY = position.y + GameConfig.CAMERA_HEIGHT + MathUtils.sin(radPitch) * camDist * 0.5f;
        return new Vector3(camX, camY, camZ);
    }

    public Vector3 getLookAt() {
        return new Vector3(position.x, position.y + height * 0.5f, position.z);
    }

    public Vector3 getPosition() { return position; }
    public void setPosition(Vector3 pos) {
        this.position.set(pos);
        updateTransform();
    }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public ModelInstance getModelInstance() { return playerInstance; }
    public boolean isGrounded() { return grounded; }

    private float lerpAngle(float from, float to, float t) {
        float diff = to - from;
        while (diff > 180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        return from + diff * t;
    }

    public void dispose() {
        if (playerModel != null) playerModel.dispose();
    }
}
JAVA
echo "[OK] Player.java"

# ----------------------------------------
# 11. TerrainGenerator.java（地面平面）
# ----------------------------------------
cat > core/src/main/java/com/mountainclimb/game/world/TerrainGenerator.java << 'JAVA'
package com.mountainclimb.game.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.mountainclimb.game.GameConfig;

import java.util.ArrayList;
import java.util.List;

public class TerrainGenerator {
    public static class PeakInfo {
        public Vector3 center;
        public float baseRadius;
        public float height;
        public float flatTopRadius;
        public BoundingBox bounds;
        public boolean summitReached = false;

        public PeakInfo(float x, float z, float radius, float h, float flatR) {
            this.center = new Vector3(x, 0, z);
            this.baseRadius = radius;
            this.height = h;
            this.flatTopRadius = flatR;
            this.bounds = new BoundingBox(
                new Vector3(x - radius, 0, z - radius),
                new Vector3(x + radius, h, z + radius)
            );
        }

        public float getHeightAt(float x, float z) {
            float dx = x - center.x;
            float dz = z - center.z;
            float dist = (float)Math.sqrt(dx*dx + dz*dz);
            if (dist > baseRadius) return 0;
            if (dist < flatTopRadius) return height;
            float t = (dist - flatTopRadius) / (baseRadius - flatTopRadius);
            return height * (1f - t * t);
        }

        public boolean isOnSummit(Vector3 pos, float threshold) {
            float dx = pos.x - center.x;
            float dz = pos.z - center.z;
            float dist = (float)Math.sqrt(dx*dx + dz*dz);
            return dist < flatTopRadius && pos.y >= height - threshold;
        }
    }

    private Model terrainModel;
    private ModelInstance terrainInstance;
    private Model groundModel;
    private ModelInstance groundInstance;
    private Model wallModel;
    private ModelInstance[] wallInstances;
    private List<PeakInfo> peaks = new ArrayList<>();
    private static final int TERRAIN_SEGMENTS = 80;

    public TerrainGenerator() {
        generatePeaks();
        buildGround();
        buildTerrain();
        buildWalls();
    }

    private void generatePeaks() {
        float half = GameConfig.WORLD_SIZE / 2f;
        peaks.add(new PeakInfo(half * 0.3f, half * 0.2f, 25f, 30f, 4f));
        peaks.add(new PeakInfo(-half * 0.4f, half * 0.1f, 20f, 22f, 3f));
        peaks.add(new PeakInfo(half * 0.1f, -half * 0.5f, 15f, 15f, 2.5f));
        peaks.add(new PeakInfo(half * 0.6f, -half * 0.2f, 18f, 18f, 3f));
    }

    private void buildGround() {
        ModelBuilder builder = new ModelBuilder();
        Material groundMat = new Material(ColorAttribute.createDiffuse(new Color(0.15f, 0.35f, 0.1f, 1f)));
        float size = GameConfig.WORLD_SIZE * 2f;
        groundModel = builder.createBox(size, 0.2f, size, groundMat,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        groundInstance = new ModelInstance(groundModel);
        groundInstance.transform.translate(0, -0.1f, 0);
    }

    private void buildTerrain() {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        Material groundMat = new Material(ColorAttribute.createDiffuse(new Color(0.25f, 0.45f, 0.15f, 1f)));
        MeshPartBuilder mpb = builder.part("ground", GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, groundMat);
        float half = GameConfig.WORLD_SIZE / 2f;
        float step = GameConfig.WORLD_SIZE / TERRAIN_SEGMENTS;
        for (int i = 0; i < TERRAIN_SEGMENTS; i++) {
            for (int j = 0; j < TERRAIN_SEGMENTS; j++) {
                float x0 = -half + i * step;
                float z0 = -half + j * step;
                float x1 = x0 + step;
                float z1 = z0 + step;
                float h00 = getTerrainHeight(x0, z0);
                float h10 = getTerrainHeight(x1, z0);
                float h01 = getTerrainHeight(x0, z1);
                float h11 = getTerrainHeight(x1, z1);
                Vector3 v00 = new Vector3(x0, h00, z0);
                Vector3 v10 = new Vector3(x1, h10, z0);
                Vector3 v01 = new Vector3(x0, h01, z1);
                Vector3 v11 = new Vector3(x1, h11, z1);
                Vector3 n1 = calculateNormal(v00, v10, v01);
                Vector3 n2 = calculateNormal(v10, v11, v01);
                short i00 = mpb.vertex(v00, n1, null, null);
                short i10 = mpb.vertex(v10, n1, null, null);
                short i01 = mpb.vertex(v01, n1, null, null);
                short i11 = mpb.vertex(v11, n2, null, null);
                mpb.triangle(i00, i10, i01);
                mpb.triangle(i10, i11, i01);
            }
        }
        terrainModel = builder.end();
        terrainInstance = new ModelInstance(terrainModel);
    }

    public float getTerrainHeight(float x, float z) {
        float y = 0f;
        for (PeakInfo peak : peaks) {
            float ph = peak.getHeightAt(x, z);
            if (ph > y) y = ph;
        }
        boolean onPeak = false;
        for (PeakInfo peak : peaks) {
            float dx = x - peak.center.x;
            float dz = z - peak.center.z;
            if (Math.sqrt(dx*dx + dz*dz) < peak.baseRadius) {
                onPeak = true;
                break;
            }
        }
        if (!onPeak) {
            y += MathUtils.sin(x * 0.1f) * MathUtils.cos(z * 0.1f) * 0.5f;
        }
        return y;
    }

    private Vector3 calculateNormal(Vector3 v1, Vector3 v2, Vector3 v3) {
        Vector3 a = v2.cpy().sub(v1);
        Vector3 b = v3.cpy().sub(v1);
        return a.crs(b).nor();
    }

    private void buildWalls() {
        float half = GameConfig.WORLD_SIZE / 2f;
        float height = 50f;
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        Material wallMat = new Material(
            ColorAttribute.createDiffuse(new Color(0.3f, 0.5f, 0.9f, 0.15f)),
            new ColorAttribute(ColorAttribute.createSpecular(Color.WHITE)),
            new com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.15f)
        );
        MeshPartBuilder mpb = builder.part("walls", GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, wallMat);
        buildWallQuadIndexed(mpb, new Vector3(-half, 0, half), new Vector3(half, 0, half),
            new Vector3(-half, height, half), new Vector3(half, height, half));
        buildWallQuadIndexed(mpb, new Vector3(half, 0, -half), new Vector3(-half, 0, -half),
            new Vector3(half, height, -half), new Vector3(-half, height, -half));
        buildWallQuadIndexed(mpb, new Vector3(-half, 0, -half), new Vector3(-half, 0, half),
            new Vector3(-half, height, -half), new Vector3(-half, height, half));
        buildWallQuadIndexed(mpb, new Vector3(half, 0, half), new Vector3(half, 0, -half),
            new Vector3(half, height, half), new Vector3(half, height, -half));
        wallModel = builder.end();
        wallInstances = new ModelInstance[] { new ModelInstance(wallModel) };
    }

    private void buildWallQuadIndexed(MeshPartBuilder mpb, Vector3 bl, Vector3 br, Vector3 tl, Vector3 tr) {
        Vector3 normal = calculateNormal(bl, br, tl);
        short iBl = mpb.vertex(bl, normal, null, null);
        short iBr = mpb.vertex(br, normal, null, null);
        short iTl = mpb.vertex(tl, normal, null, null);
        short iTr = mpb.vertex(tr, normal, null, null);
        mpb.triangle(iBl, iBr, iTl);
        mpb.triangle(iBr, iTr, iTl);
    }

    public boolean isInsideWorld(float x, float z) {
        float half = GameConfig.WORLD_SIZE / 2f - GameConfig.PLAYER_RADIUS;
        return x >= -half && x <= half && z >= -half && z <= half;
    }

    public Vector3 clampToWorld(Vector3 pos) {
        float half = GameConfig.WORLD_SIZE / 2f - GameConfig.PLAYER_RADIUS;
        pos.x = MathUtils.clamp(pos.x, -half, half);
        pos.z = MathUtils.clamp(pos.z, -half, half);
        return pos;
    }

    public List<PeakInfo> getPeaks() { return peaks; }
    public ModelInstance getTerrainInstance() { return terrainInstance; }
    public ModelInstance getGroundInstance() { return groundInstance; }
    public ModelInstance[] getWallInstances() { return wallInstances; }

    public void dispose() {
        if (terrainModel != null) terrainModel.dispose();
        if (groundModel != null) groundModel.dispose();
        if (wallModel != null) wallModel.dispose();
    }
}
JAVA
echo "[OK] TerrainGenerator.java"

# ----------------------------------------
# 12. GameScreen.java（渲染地面 + 相机）
# ----------------------------------------
cat > core/src/main/java/com/mountainclimb/game/screen/GameScreen.java << 'JAVA'
package com.mountainclimb.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mountainclimb.game.GameConfig;
import com.mountainclimb.game.MountainClimbGame;
import com.mountainclimb.game.audio.AudioManager;
import com.mountainclimb.game.input.Joystick;
import com.mountainclimb.game.player.Player;
import com.mountainclimb.game.save.SaveManager;
import com.mountainclimb.game.world.TerrainGenerator;

public class GameScreen implements Screen {
    private MountainClimbGame game;
    private boolean continueGame;
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private TerrainGenerator terrain;
    private Player player;
    private Stage uiStage;
    private Joystick joystick;
    private InputMultiplexer inputMultiplexer;
    private GestureDetector gestureDetector;
    private boolean paused = false;
    private boolean summitShown = false;
    private float summitTimer = 0f;
    private boolean wasMoving = false;
    private Label summitLabel;
    private Dialog pauseDialog;
    private Skin skin;

    public GameScreen(MountainClimbGame game, boolean continueGame) {
        this.game = game;
        this.continueGame = continueGame;
    }

    @Override
    public void show() {
        skin = game.getSkin();
        modelBatch = new ModelBatch();
        float aspect = (float) Gdx.graphics.getWidth() / Gdx.graphics.getHeight();
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 300f;
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.7f, -0.5f, -1f, -0.5f));
        environment.add(new DirectionalLight().set(0.3f, 0.3f, 0.4f, 0.5f, 0.5f, -0.5f));
        terrain = new TerrainGenerator();
        Vector3 startPos = new Vector3(0, 10, 0);
        if (continueGame) {
            Vector3 saved = SaveManager.getInstance().loadProgress();
            if (saved != null) startPos = saved;
        }
        float terrainH = terrain.getTerrainHeight(startPos.x, startPos.z);
        startPos.y = Math.max(startPos.y, terrainH + GameConfig.PLAYER_HEIGHT + 0.5f);
        player = new Player(startPos.x, startPos.y, startPos.z);
        uiStage = new Stage(new ScreenViewport());
        setupUI();
        setupInput();
        AudioManager.getInstance().playBGM();
    }

    private void setupUI() {
        float joySize = Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) * 0.25f;
        float knobSize = joySize * 0.4f;
        joystick = new Joystick(joySize, knobSize);
        joystick.setPosition(30f, 30f);
        uiStage.addActor(joystick);
        summitLabel = new Label("成功登顶!", skin);
        summitLabel.setFontScale(2f);
        summitLabel.setColor(Color.GOLD);
        summitLabel.setPosition(Gdx.graphics.getWidth() / 2f - summitLabel.getWidth(), Gdx.graphics.getHeight() / 2f);
        summitLabel.setVisible(false);
        uiStage.addActor(summitLabel);
        TextButton btnPause = new TextButton("||", skin);
        float btnSize = 60f;
        btnPause.setSize(btnSize, btnSize);
        btnPause.setPosition(Gdx.graphics.getWidth() - btnSize - 20f, Gdx.graphics.getHeight() - btnSize - 20f);
        btnPause.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                togglePause();
            }
        });
        uiStage.addActor(btnPause);
    }

    private void setupInput() {
        gestureDetector = new GestureDetector(new GestureDetector.GestureAdapter() {
            @Override
            public boolean touchDown(float x, float y, int pointer, int button) {
                return x > Gdx.graphics.getWidth() / 2f;
            }
            @Override
            public boolean pan(float x, float y, float deltaX, float deltaY) {
                if (paused) return false;
                if (x > Gdx.graphics.getWidth() / 2f) {
                    float sensitivity = SaveManager.getInstance().getSensitivityMultiplier();
                    float rotSpeed = GameConfig.DEFAULT_SENSITIVITY * sensitivity * 0.1f;
                    player.rotate(-deltaX * rotSpeed, deltaY * rotSpeed, 1f);
                    return true;
                }
                return false;
            }
        });
        InputProcessor gameInput = new InputProcessor() {
            @Override public boolean keyDown(int keycode) {
                if (keycode == com.badlogic.gdx.Input.Keys.BACK) {
                    togglePause();
                    return true;
                }
                return false;
            }
            @Override public boolean keyUp(int keycode) { return false; }
            @Override public boolean keyTyped(char character) { return false; }
            @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
            @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
            @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
            @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
            @Override public boolean scrolled(float amountX, float amountY) { return false; }
            @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
        };
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(uiStage);
        inputMultiplexer.addProcessor(gestureDetector);
        inputMultiplexer.addProcessor(gameInput);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            showPauseDialog();
            AudioManager.getInstance().pauseBGM();
        } else {
            if (pauseDialog != null) pauseDialog.hide();
            AudioManager.getInstance().playBGM();
        }
    }

    private void showPauseDialog() {
        try {
            pauseDialog = new Dialog("暂停", game.getSkin(), "dialog") {
                @Override
                protected void result(Object object) {
                    String action = (String) object;
                    if ("continue".equals(action)) togglePause();
                    else if ("save".equals(action)) {
                        SaveManager.getInstance().saveProgress(player.getPosition());
                        AudioManager.getInstance().playButtonSound();
                    } else if ("menu".equals(action)) {
                        AudioManager.getInstance().playButtonSound();
                        game.setScreen(new MainMenuScreen(game));
                    }
                }
            };
            pauseDialog.getContentTable().pad(20f);
            pauseDialog.button("继续游戏", "continue").pad(10f);
            pauseDialog.button("保存进度", "save").pad(10f);
            pauseDialog.button("返回主菜单", "menu").pad(10f);
            pauseDialog.show(uiStage);
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Pause dialog error: " + e.getMessage());
        }
    }

    @Override
    public void render(float delta) {
        if (!paused) updateGame(delta);
        Gdx.gl.glClearColor(0.5f, 0.7f, 0.9f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        camera.position.set(player.getCameraPosition());
        camera.lookAt(player.getLookAt());
        camera.up.set(Vector3.Y);
        camera.update();
        modelBatch.begin(camera);
        modelBatch.render(terrain.getGroundInstance(), environment);
        modelBatch.render(terrain.getTerrainInstance(), environment);
        for (ModelInstance wall : terrain.getWallInstances()) {
            modelBatch.render(wall, environment);
        }
        modelBatch.render(player.getModelInstance(), environment);
        modelBatch.end();
        uiStage.act(delta);
        uiStage.draw();
        if (summitShown) {
            summitTimer += delta;
            if (summitTimer > 3f) {
                summitLabel.setVisible(false);
                summitShown = false;
            } else {
                float alpha = 1f - (summitTimer / 3f);
                summitLabel.setColor(Color.GOLD.r, Color.GOLD.g, Color.GOLD.b, alpha);
            }
        }
    }

    private void updateGame(float delta) {
        Vector2 moveDir = joystick.getDirection();
        boolean moving = moveDir.len2() > 0.01f;
        if (moving && player.isGrounded() && !wasMoving) {
            AudioManager.getInstance().playClimbSound();
        }
        wasMoving = moving;
        player.move(moveDir, delta, 0);
        float terrainH = terrain.getTerrainHeight(player.getPosition().x, player.getPosition().z);
        player.applyGravity(delta, terrainH);
        Vector3 pos = player.getPosition();
        pos = terrain.clampToWorld(pos);
        player.setPosition(pos);
        player.updateTransform();
        checkSummit();
    }

    private void checkSummit() {
        Vector3 pos = player.getPosition();
        for (TerrainGenerator.PeakInfo peak : terrain.getPeaks()) {
            if (!peak.summitReached && peak.isOnSummit(pos, 2f)) {
                peak.summitReached = true;
                summitShown = true;
                summitTimer = 0f;
                summitLabel.setVisible(true);
                summitLabel.setPosition(Gdx.graphics.getWidth() / 2f - summitLabel.getPrefWidth() / 2f, Gdx.graphics.getHeight() / 2f);
                AudioManager.getInstance().playSummitSound();
                SaveManager.getInstance().saveProgress(pos);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        uiStage.getViewport().update(width, height, true);
    }

    @Override public void pause() { SaveManager.getInstance().saveProgress(player.getPosition()); }
    @Override public void resume() {}
    @Override public void hide() { Gdx.input.setInputProcessor(null); }
    @Override public void dispose() {
        modelBatch.dispose();
        terrain.dispose();
        player.dispose();
        joystick.dispose();
        uiStage.dispose();
    }
}
JAVA
echo "[OK] GameScreen.java"

# ----------------------------------------
# 13. SettingsScreen.java（.wav + 使用 "dialog"）
# ----------------------------------------
cat > core/src/main/java/com/mountainclimb/game/screen/SettingsScreen.java << 'JAVA'
package com.mountainclimb.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mountainclimb.game.MountainClimbGame;
import com.mountainclimb.game.audio.AudioManager;
import com.mountainclimb.game.save.SaveManager;

public class SettingsScreen implements Screen {
    private MountainClimbGame game;
    private Stage stage;
    private Slider soundSlider;
    private Slider musicSlider;
    private Slider sensitivitySlider;
    private Label soundValueLabel;
    private Label musicValueLabel;
    private Label sensitivityValueLabel;

    public SettingsScreen(MountainClimbGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        Skin skin = game.getSkin();
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label("设置", skin);
        title.setFontScale(1.5f);
        table.add(title).padBottom(40f).row();

        float sliderWidth = 400f;
        float labelWidth = 120f;

        Table soundRow = new Table();
        soundRow.add(new Label("音效音量", skin)).width(labelWidth).padRight(20f);
        soundSlider = new Slider(0f, 100f, 1f, false, skin);
        soundSlider.setValue(SaveManager.getInstance().getSoundVolume() * 100f);
        soundRow.add(soundSlider).width(sliderWidth).padRight(15f);
        soundValueLabel = new Label((int)soundSlider.getValue() + "%", skin);
        soundRow.add(soundValueLabel).width(60f);
        table.add(soundRow).padBottom(25f).row();

        soundSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float vol = soundSlider.getValue() / 100f;
                soundValueLabel.setText((int)soundSlider.getValue() + "%");
                AudioManager.getInstance().setSoundVolume(vol);
            }
        });

        Table musicRow = new Table();
        musicRow.add(new Label("背景音乐", skin)).width(labelWidth).padRight(20f);
        musicSlider = new Slider(0f, 100f, 1f, false, skin);
        musicSlider.setValue(SaveManager.getInstance().getMusicVolume() * 100f);
        musicRow.add(musicSlider).width(sliderWidth).padRight(15f);
        musicValueLabel = new Label((int)musicSlider.getValue() + "%", skin);
        musicRow.add(musicValueLabel).width(60f);
        table.add(musicRow).padBottom(25f).row();

        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float vol = musicSlider.getValue() / 100f;
                musicValueLabel.setText((int)musicSlider.getValue() + "%");
                AudioManager.getInstance().setMusicVolume(vol);
            }
        });

        Table sensRow = new Table();
        sensRow.add(new Label("视角灵敏度", skin)).width(labelWidth).padRight(20f);
        sensitivitySlider = new Slider(0f, 300f, 1f, false, skin);
        sensitivitySlider.setValue(SaveManager.getInstance().getSensitivityPercent());
        sensRow.add(sensitivitySlider).width(sliderWidth).padRight(15f);
        sensitivityValueLabel = new Label((int)sensitivitySlider.getValue() + "%", skin);
        sensRow.add(sensitivityValueLabel).width(60f);
        table.add(sensRow).padBottom(40f).row();

        sensitivitySlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float pct = sensitivitySlider.getValue();
                sensitivityValueLabel.setText((int)pct + "%");
                SaveManager.getInstance().setSensitivity(pct);
            }
        });

        TextButton btnBack = new TextButton("返回", skin);
        btnBack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playButtonSound();
                game.setScreen(new MainMenuScreen(game));
            }
        });
        table.add(btnBack).width(200f).height(60f).row();
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
JAVA
echo "[OK] SettingsScreen.java"

# ----------------------------------------
# 14. .github/workflows/build-apk.yml
# ----------------------------------------
cat > .github/workflows/build-apk.yml << 'YAML'
name: Build Android APK
on:
  push:
    branches: [ main, master ]
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
      with:
        fetch-depth: 0
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle
    - name: Setup Android SDK
      uses: android-actions/setup-android@v3
    - name: Install tools
      run: |
        sdkmanager "build-tools;33.0.0" "platforms;android-33"
        sudo apt-get update
        sudo apt-get install -y fonts-wqy-zenhei fontconfig
        pip install Pillow fonttools
    - name: Generate all assets
      run: |
        python3 scripts/create-assets.py
        echo "=== Verifying font ==="
        ls -lh android/src/main/assets/fonts/
        python3 -c "
import os
fnt = 'android/src/main/assets/fonts/game_font.fnt'
png = 'android/src/main/assets/fonts/game_font.png'
if os.path.exists(fnt) and os.path.exists(png):
    print(f'Font OK: fnt={os.path.getsize(fnt)} bytes, png={os.path.getsize(png)} bytes')
else:
    print('Font MISSING!')
    exit(1)
"
    - name: List all assets
      run: find android/src/main/assets -type f | sort
    - name: Download native libraries
      run: |
        GDX_VER="1.12.1"
        JNI_DIR="android/src/main/jniLibs"
        mkdir -p $JNI_DIR/armeabi-v7a $JNI_DIR/arm64-v8a
        for lib in gdx gdx-bullet gdx-freetype; do
          for arch in armeabi-v7a arm64-v8a; do
            curl -L -o /tmp/${lib}-${arch}.jar "https://repo1.maven.org/maven2/com/badlogicgames/gdx/${lib}-platform/${GDX_VER}/${lib}-platform-${GDX_VER}-natives-${arch}.jar" 2>/dev/null
            unzip -o /tmp/${lib}-${arch}.jar -d /tmp/${lib}-${arch}/ 2>/dev/null || true
            so_file=$(find /tmp/${lib}-${arch}/ -name "*.so" | head -1)
            [ -n "$so_file" ] && cp "$so_file" $JNI_DIR/${arch}/
          done
        done
        find $JNI_DIR -type f | sort
    - name: Build APKs
      run: |
        chmod +x gradlew
        ./gradlew android:assembleDebug --no-daemon
        ./gradlew android:assembleRelease --no-daemon
      env:
        ANDROID_SDK_ROOT: ${{ env.ANDROID_SDK_ROOT }}
    - name: Upload Debug APK
      uses: actions/upload-artifact@v4
      with:
        name: debug-apk
        path: android/build/outputs/apk/debug/android-debug.apk
    - name: Upload Release APK
      uses: actions/upload-artifact@v4
      with:
        name: release-apk
        path: android/build/outputs/apk/release/android-release-unsigned.apk
YAML
echo "[OK] build-apk.yml"

# ----------------------------------------
# 15. Git 提交推送
# ----------------------------------------
git add -A
git commit -m "fix: 位图字体+WindowStyle+热更新+第三人称+地面"
git push origin main

echo ""
echo "=========================================="
echo "推送完成！"
echo "=========================================="
echo ""
echo "修复内容："
echo "1. 字体：Python预生成位图字体（.fnt+.png），100%可靠"
echo "2. Dialog：添加WindowStyle，修复Dialog/Window崩溃"
echo "3. 热更新：完整实现版本检查+下载+解压+重启提示"
echo "4. 相机：第三人称，位于玩家后方上方"
echo "5. 地面：添加大面积地面平面"
echo "6. 玩家：初始位置确保在地面上"
echo "7. 音频：.mp3改为.wav"
echo ""
echo "去 GitHub Actions 查看最新构建，下载 debug-apk 测试。"
