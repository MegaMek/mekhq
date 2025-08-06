/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.Graphics;
import java.awt.Image;
import javax.swing.JLabel;

/**
 * A custom label that paints an image to the label that resizes based on the size of the label while maintaining the
 * aspect ratio of the original image.
 * <p>
 * Code borrowed from: https://stackoverflow.com/questions/10245220/java-image-resize-maintain-aspect-ratio
 *
 * @author Taharqa
 */
public class ImgLabel extends JLabel {
    Image image;

    public ImgLabel(Image i) {
        super();
        this.image = i;
    }

    /**
     * Get the scaled dimensions for the image that allow it to fit into the label's current size
     *
     * @return <code>Dimension</code> giving the new scaled dimensions
     */
    private Dimension getScaledDimension() {
        int original_width = image.getWidth(this);
        int original_height = image.getHeight(this);
        int bound_width = getWidth();
        int bound_height = getHeight();
        int new_width = original_width;
        int new_height = original_height;

        // first check if we need to scale width
        if (original_width > bound_width) {
            //scale width to fit
            new_width = bound_width;
            //scale height to maintain aspect ratio
            new_height = (new_width * original_height) / original_width;
        }

        // then check if we need to scale even with the new height
        if (new_height > bound_height) {
            //scale height to fit instead
            new_height = bound_height;
            //scale width to maintain aspect ratio
            new_width = (new_height * original_width) / original_height;
        }
        return new Dimension(new_width, new_height);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension dims = getScaledDimension();
        g.drawImage(image, 0, 0, (int) dims.getWidth(), (int) dims.getHeight(), this);
    }
}
