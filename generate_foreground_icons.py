#!/usr/bin/env python3
"""
Generate Android adaptive icon foreground images from a source PNG image.
Adaptive icons need 108dp with content centered in 72dp safe zone.
"""

import os
from PIL import Image

# Foreground icon sizes for different densities (108dp)
FOREGROUND_SIZES = {
    'mdpi': 108,
    'hdpi': 162,
    'xhdpi': 216,
    'xxhdpi': 324,
    'xxxhdpi': 432
}

def generate_foreground_icons(source_image_path, output_base_dir):
    """Generate adaptive icon foreground images from source image."""

    # Load the source image
    try:
        img = Image.open(source_image_path)
        print(f"Loaded image: {source_image_path}")
        print(f"Original size: {img.size}")
    except Exception as e:
        print(f"Error loading image: {e}")
        return False

    # Convert to RGBA if needed
    if img.mode != 'RGBA':
        img = img.convert('RGBA')

    # Generate foreground icons for each density
    for density, full_size in FOREGROUND_SIZES.items():
        # Create output directory
        output_dir = os.path.join(output_base_dir, f'mipmap-{density}')
        os.makedirs(output_dir, exist_ok=True)

        # Calculate the safe zone size (72dp in 108dp canvas = 66.67%)
        safe_zone_size = int(full_size * 0.6667)

        # Resize the icon to fit in the safe zone
        resized_img = img.resize((safe_zone_size, safe_zone_size), Image.Resampling.LANCZOS)

        # Create a transparent canvas of full size
        canvas = Image.new('RGBA', (full_size, full_size), (0, 0, 0, 0))

        # Calculate position to center the resized image
        position = ((full_size - safe_zone_size) // 2, (full_size - safe_zone_size) // 2)

        # Paste the resized image onto the canvas
        canvas.paste(resized_img, position, resized_img)

        # Save the foreground icon
        output_path = os.path.join(output_dir, 'ic_launcher_foreground.png')
        canvas.save(output_path, 'PNG')
        print(f"Generated {density} foreground: {output_path}")

    print("\nForeground icon generation complete!")
    return True

if __name__ == '__main__':
    # Paths
    script_dir = os.path.dirname(os.path.abspath(__file__))
    source_image = os.path.join(script_dir, 'app-icon-cropped.png')
    output_dir = os.path.join(script_dir, 'app', 'src', 'main', 'res')

    # Check if source image exists
    if not os.path.exists(source_image):
        print(f"Error: Source image not found at {source_image}")
        exit(1)

    # Generate foreground icons
    success = generate_foreground_icons(source_image, output_dir)
    exit(0 if success else 1)

