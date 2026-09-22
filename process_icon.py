import os
from PIL import Image, ImageDraw

input_path = r'C:\Users\furka\.gemini\antigravity\brain\fd3f7d6a-c1a7-47a6-8706-98e6c8a8e467\.user_uploaded\media_1790073951422.jpg'
out_base = r'android\app\src\main\res'

sizes = {
    'mdpi': (48, 108),
    'hdpi': (72, 162),
    'xhdpi': (96, 216),
    'xxhdpi': (144, 324),
    'xxxhdpi': (192, 432)
}

img = Image.open(input_path).convert('RGBA')

for density, (legacy_size, adaptive_size) in sizes.items():
    folder = os.path.join(out_base, f'mipmap-{density}')
    os.makedirs(folder, exist_ok=True)
    
    # Adaptive foreground (simply resize image to adaptive size)
    # The logo should ideally be in the center 72x72 of a 108x108 image (2/3 size)
    # So we pad the image by 1/6th on each side to make it smaller in the center.
    fg_img = Image.new('RGBA', (adaptive_size, adaptive_size), (0,0,0,255))
    inner_size = int(adaptive_size * 0.8) # scale logo to 80% to fit safe zone well
    offset = (adaptive_size - inner_size) // 2
    resized_inner = img.resize((inner_size, inner_size), Image.Resampling.LANCZOS)
    fg_img.paste(resized_inner, (offset, offset))
    
    fg_img.save(os.path.join(folder, 'ic_launcher_foreground.png'))
    
    # Legacy round icon
    mask = Image.new('L', (legacy_size, legacy_size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, legacy_size, legacy_size), fill=255)
    
    legacy_img = img.resize((legacy_size, legacy_size), Image.Resampling.LANCZOS)
    legacy_img.putalpha(mask)
    legacy_img.save(os.path.join(folder, 'ic_launcher_round.png'))
    
    # Legacy square icon
    legacy_sq = img.resize((legacy_size, legacy_size), Image.Resampling.LANCZOS)
    legacy_sq.save(os.path.join(folder, 'ic_launcher.png'))
    
    # Remove old webp if they exist
    for f in ['ic_launcher.webp', 'ic_launcher_round.webp']:
        old_file = os.path.join(folder, f)
        if os.path.exists(old_file):
            os.remove(old_file)

print('Icons generated!')
