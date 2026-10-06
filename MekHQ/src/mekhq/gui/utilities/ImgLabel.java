/*
 * Copyright (C) 2019-2026 The MegaMek Team. All Rights Reserved.
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
 * Code borrowed from:
 * <a href="https://stackoverflow.com/questions/10245220/java-image-resize-maintain-aspect-ratio">Stack Overflow</a>
 *
 * @author Taharqa
 */
public class ImgLabel extends JLabel {
    Image image;
    private final int maxWidth;
    private final int maxHeight;

    /**
     * Creates a label that paints the given image without an upper bound on its preferred size, so it reports the
     * image's natural dimensions and grows to whatever space a stretching layout gives it.
     *
     * @param image the image to paint
     */
    public ImgLabel(Image image) {
        this(image, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Creates a label that paints the given image, reporting a preferred size that fits the image (aspect ratio
     * preserved) within the given bounds. The bounds matter for layouts that size a component to its preferred size
     * (e.g. {@code GridBagConstraints.fill == NONE}); without a non-zero preferred size such layouts collapse the label
     * to nothing and the image is never seen.
     *
     * @param i         the image to paint
     * @param maxWidth  the maximum preferred width, in pixels
     * @param maxHeight the maximum preferred height, in pixels
     */
    public ImgLabel(Image i, int maxWidth, int maxHeight) {
        super();
        this.image = i;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    /**
     * Reports the image's dimensions scaled to fit within the configured maximum bounds, preserving aspect ratio. This
     * gives the label a non-zero footprint so that layouts which honor the preferred size still show the image.
     *
     * @return the preferred size for this label
     */
    @Override
    public Dimension getPreferredSize() {
        int imageWidth = image.getWidth(this);
        int imageHeight = image.getHeight(this);
        // If the image's dimensions are not yet known, defer to the default preferred size.
        if ((imageWidth <= 0) || (imageHeight <= 0)) {
            return super.getPreferredSize();
        }
        double scale = Math.min(1.0, Math.min((double) maxWidth / imageWidth, (double) maxHeight / imageHeight));
        return new Dimension((int) Math.round(imageWidth * scale), (int) Math.round(imageHeight * scale));
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
