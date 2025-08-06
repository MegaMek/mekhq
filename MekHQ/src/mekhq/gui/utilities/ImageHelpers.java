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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.utilities;

import java.awt.Dimension;
import java.awt.Image;

/**
 * This class will contain helper methods to deal with images
 *
 * @author Miguel Azevedo aka Bandildo
 */
public class ImageHelpers {
    /**
     * Resizes an image, keeping its aspect ratio, in order to fit in the given boundaries. It will not resize in case
     * the image already fits the boundaries.
     *
     * @param image               Image to resize
     * @param boundary            Maximum size the image can have
     * @param resamplingAlgorithm flags to indicate the type of algorithm to use for image resampling.
     *
     * @return Imputed image, resized if necessary.
     */
    public static Image getScaledForBoundaries(Image image, Dimension boundary, int resamplingAlgorithm) {

        double original_width = image.getWidth(null);
        double original_height = image.getHeight(null);
        int bound_width = boundary.width;
        int bound_height = boundary.height;
        double new_width = original_width;
        double new_height = original_height;

        double ratio = original_width / original_height;

        if (original_width > bound_width || original_height > bound_height) {
            if (original_width > original_height) {
                new_width = bound_width;
                new_height = (int) (new_width / ratio);
            }

            if (original_height > original_width) {
                new_height = bound_height;
                new_width = (int) (new_height * ratio);
            }
        }

        return image.getScaledInstance((int) Math.round(new_width), (int) Math.round(new_height), resamplingAlgorithm);
    }
}
