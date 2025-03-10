/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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

import java.awt.Image;

import javax.swing.ImageIcon;

import megamek.client.ui.swing.util.UIUtil;

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
}
