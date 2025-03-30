/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.utilities;

import static java.lang.Math.round;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;

public class ImageUtilities {
    /**
     * Scales an {@link ImageIcon} to the specified width while maintaining its aspect ratio.
     * The method dynamically determines the scaled height proportional to the original
     * image dimensions to retain visual quality and appearance.
     *
     * <p>The scaling process follows these steps:</p>
     * <ol>
     *     <li>Adjusts the provided {@code width} to account for GUI scaling factors
     *         by using {@link UIUtil#scaleForGUI(int)} to ensure consistent dimensions.</li>
     *     <li>Calculates the new height required to maintain the image's original aspect ratio
     *         using the formula {@code height = (width * originalHeight) / originalWidth}.</li>
     *     <li>Uses {@link Image#getScaledInstance(int, int, int)} to create a resized image
     *         with smooth scaling for better visual quality ({@code Image.SCALE_SMOOTH}).</li>
     *     <li>Packages the resulting scaled image into a new {@link ImageIcon} object
     *         and returns it.</li>
     * </ol>
     *
     * <p>By dynamically scaling both width and height based on the GUI scaling factor,
     * the method ensures that the resized icon adapts seamlessly across high-DPI displays
     * and varying screen resolutions.</p>
     *
     * @param icon  the {@link ImageIcon} to be resized. This represents the source image
     *              that requires scaling.
     * @param width the desired width (in pixels) to scale the {@link ImageIcon} to.
     *              The actual applied value will consider GUI scaling factors to ensure
     *              consistent appearance across different display settings.
     * @return a new {@link ImageIcon} instance representing the scaled image while
     *         preserving the original aspect ratio.
     */
    public static ImageIcon scaleImageIconToWidth(ImageIcon icon, int width) {
        width = UIUtil.scaleForGUI(width);
        int height = (int) Math.ceil((double) width * icon.getIconHeight() / icon.getIconWidth());

        Image image = icon.getImage();
        Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        return new ImageIcon(scaledImage);
    }

    /**
     * Adds a default tint to the provided image with standard transparency settings.
     *
     * <p>This is a simplified utility method that applies a given tint color to only the
     * non-transparent portions of an image, with a default transparency level of 75%
     * (25% opaque).</p>
     *
     * @param image     the {@link Image} to which the tint will be applied.
     * @param tintColor the {@link Color} used for the tint.
     *
     * @return an {@link ImageIcon} containing the tinted image.
     *
     * @see #addTintToImageIcon(Image, Color, boolean, Double) for more advanced configurations.
     */
    public static ImageIcon addTintToImageIcon(Image image, Color tintColor) {
        return addTintToImageIcon(image, tintColor, true, null);
    }

    /**
     * Adds a customizable tint to the given image, with options to control the transparency
     * and the areas of the image affected by the tint.
     *
     * <p>This method processes the input {@link Image} and applies a tint (a blend of the
     * given color and transparency) across the image. You may specify whether the tint should
     * apply only to non-transparent areas or the entire image. Additionally, the transparency
     * level can be customized or left as the default (50% transparency, 50% opaque).</p>
     *
     * @param image              the {@link Image} to which the tint will be applied.
     * @param tint               the {@link Color} used for the tint.
     * @param nonTransparentOnly if {@code true}, applies the tint only to non-transparent areas of
     *                           the image. Otherwise, it applies the tint globally.
     * @param transparencyPercent an optional transparency level for the tint. If {@code null}, it
     *                            defaults to 50% transparency (0.5). Must be between {@code 0.0}
     *                            and {@code 1.0}.
     *
     * @return an {@link ImageIcon} containing the image with the applied tint.
     *
     * @see #addTintToImageIcon(Image, Color) for default behavior.
     */
    public static ImageIcon addTintToImageIcon(Image image, Color tint, boolean nonTransparentOnly,
                                           @Nullable Double transparencyPercent) {
        BufferedImage tintedImage = new BufferedImage(
              image.getWidth(null),
              image.getHeight(null),
              BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = tintedImage.createGraphics();
        graphics.drawImage(image, 0, 0, null);

        if (nonTransparentOnly) {
            // This applies the tint to only the non-transparent areas
            graphics.setComposite(AlphaComposite.SrcAtop);
        }

        graphics.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(),
              getAlpha(transparencyPercent == null ? 0.5 : transparencyPercent)));
        graphics.fillRect(0, 0, tintedImage.getWidth(), tintedImage.getHeight());

        // Clean up, so we're not leaving objects in memory everywhere if the player is jumping
        // between a lot of images (such as the personnel table).
        graphics.dispose();

        return new ImageIcon(tintedImage);
    }

    /**
     * Applies a default tint to a {@link BufferedImage} and returns the modified image.
     *
     * <p>This method adds a tint overlay of the specified color to the provided {@link BufferedImage}.
     * By default, the tint is applied only to non-transparent areas, and a transparency level of 50% is used if none is
     * specified.
     *
     * @param image     The {@link BufferedImage} on which the tint will be applied.
     * @param tintColor The {@link Color} to use as the tint.
     *
     * @return A new {@link BufferedImage} containing the original image with the tint applied.
     *
     * @see #addTintToBufferedImage(BufferedImage, Color, boolean, Double)
     */
    public static BufferedImage addTintToBufferedImage(BufferedImage image, Color tintColor) {
        return addTintToBufferedImage(image, tintColor, true, null);
    }

    /**
     * Applies a tint to a BufferedImage and returns the modified image.
     *
     * <p>The method overlays a specified color tint on the original image. You can optionally apply
     * the tint only to non-transparent areas or specify the transparency level of the tint.
     *
     * @param image               The original {@link BufferedImage} to which the tint will be added.
     * @param tint                The {@link Color} to use as the tint.
     * @param nonTransparentOnly  If {@code true}, applies the tint only to non-transparent areas.
     * @param transparencyPercent The transparency level of the tinted overlay (0.0 to 1.0), where {@code 1.0} is fully
     *                            opaque and {@code 0.0} is fully transparent. If {@code null}, a default of 50%
     *                            transparency is applied.
     *
     * @return A new {@link BufferedImage} with the specified tint applied.
     */
    public static BufferedImage addTintToBufferedImage(BufferedImage image, Color tint, boolean nonTransparentOnly, @Nullable Double transparencyPercent) {
        // Create a new BufferedImage with the same dimensions and type as the original
        BufferedImage tintedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        // Draw the original image onto the new BufferedImage
        Graphics2D graphics = tintedImage.createGraphics();
        graphics.drawImage(image, 0, 0, null);

        // If applying the tint only to non-transparent areas, set the appropriate composite
        if (nonTransparentOnly) {
            graphics.setComposite(AlphaComposite.SrcAtop);
        }

        // Generate a new color with the specified transparency
        int alpha = getAlpha(transparencyPercent == null ? 0.5 : transparencyPercent);
        graphics.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), alpha));

        // Apply the tint color to the entire image
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        // Dispose of the Graphics2D object to free resources
        graphics.dispose();

        // Return the tinted BufferedImage
        return tintedImage;
    }

    /**
     * Converts a transparency percentage into an alpha value for ARGB colors.
     *
     * <p>This method maps a transparency percentage (from {@code 0.0} to {@code 1.0})
     * to an integer alpha value (from 0 to 255) usable with ARGB colors.</p>
     * <ul>
     *     <li>A value of {@code 1.0} (fully opaque) will return {@code 0} for maximum alpha.</li>
     *     <li>A value of {@code 0.0} (fully transparent) will return {@code 255} for full transparency.</li>
     * </ul>
     *
     * @param transparencyPercent A percentage representing transparency. Must be between {@code 0.0} and {@code 1.0},
     *                            inclusive.
     *
     * @return An integer alpha value ranging from 0 (fully opaque) to 255 (fully transparent), calculated from the
     *       provided percentage.
     *
     * @throws IllegalArgumentException If {@code transparencyPercent} is outside the range of {@code 0.0} to
     *                                  {@code 1.0}.
     */
    private static int getAlpha(double transparencyPercent) {
        if (transparencyPercent < 0.0 || transparencyPercent > 1.0) {
            throw new IllegalArgumentException("Transparency percent must be between 0.0 and 1.0.");
        }

        return (int) round(255 - (255 * transparencyPercent));
    }
}
