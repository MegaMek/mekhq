/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
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
     * Resizes an image, keeping its aspect ratio, in order to fit in the given
     * boundaries. It will not resize in case
     * the image already fits the boundaries.
     *
     * @param image               Image to resize
     * @param boundary            Maximum size the image can have
     * @param resamplingAlgorithm flags to indicate the type of algorithm to use for
     *                            image resampling.
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
