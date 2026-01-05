#!/usr/bin/env python3
"""
Generate Android app icons from a source PNG image.
Creates icons in all required densities for Android.
"""

import os
from PIL import Image

# Icon sizes for different densities
ICON_SIZES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

def generate_icons(source_image_path, output_base_dir):
    """Generate app icons from source image."""

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

    # Generate icons for each density
    for density, size in ICON_SIZES.items():
        # Create output directory
        output_dir = os.path.join(output_base_dir, f'mipmap-{density}')
        os.makedirs(output_dir, exist_ok=True)

        # Resize image
        resized_img = img.resize((size, size), Image.Resampling.LANCZOS)

        # Save both regular and round icons
        output_path = os.path.join(output_dir, 'ic_launcher.png')
        resized_img.save(output_path, 'PNG')
        print(f"Generated {density}: {output_path}")

        output_path_round = os.path.join(output_dir, 'ic_launcher_round.png')
        resized_img.save(output_path_round, 'PNG')
        print(f"Generated {density}: {output_path_round}")

    print("\nIcon generation complete!")
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

    # Generate icons
    success = generate_icons(source_image, output_dir)
    exit(0 if success else 1)

