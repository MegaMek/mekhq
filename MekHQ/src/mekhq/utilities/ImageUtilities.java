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

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static java.lang.Math.round;

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
     * @see #addTintToImage(Image, Color, boolean, Double) for more advanced configurations.
     */
    public static ImageIcon addTintToImage(Image image, Color tintColor) {
        return addTintToImage(image, tintColor, true, null);
    }

    /**
     * Adds a customizable tint to the given image, with options to control the transparency
     * and the areas of the image affected by the tint.
     *
     * <p>This method processes the input {@link Image} and applies a tint (a blend of the
     * given color and transparency) across the image. You may specify whether the tint should
     * apply only to non-transparent areas or the entire image. Additionally, the transparency
     * level can be customized or left as the default (75% transparency, 25% opaque).</p>
     *
     * @param image              the {@link Image} to which the tint will be applied.
     * @param tint               the {@link Color} used for the tint.
     * @param nonTransparentOnly if {@code true}, applies the tint only to non-transparent areas of
     *                           the image. Otherwise, it applies the tint globally.
     * @param transparencyPercent an optional transparency level for the tint. If {@code null}, it
     *                            defaults to 75% transparency (0.75). Must be between {@code 0.0}
     *                            and {@code 1.0}.
     *
     * @return an {@link ImageIcon} containing the image with the applied tint.
     *
     * @see #addTintToImage(Image, Color) for default behavior.
     */
    public static ImageIcon addTintToImage(Image image, Color tint, boolean nonTransparentOnly,
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
              getAlpha(transparencyPercent == null ? 0.75 : transparencyPercent)));
        graphics.fillRect(0, 0, tintedImage.getWidth(), tintedImage.getHeight());

        // Clean up, so we're not leaving objects in memory everywhere if the player is jumping
        // between a lot of images (such as the personnel table).
        graphics.dispose();

        return new ImageIcon(tintedImage);
    }

    private static int getAlpha(double transparencyPercent) {
        if (transparencyPercent < 0.0 || transparencyPercent > 1.0) {
            throw new IllegalArgumentException("Transparency percent must be between 0.0 and 1.0.");
        }

        return (int) round(255 - (255 * transparencyPercent));
    }
}
