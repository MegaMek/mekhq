/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package mekhq.gui.baseComponents;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.Objects;
import javax.swing.JComponent;

import megamek.common.util.ImageUtil;

/**
 * Displays an image that fills the available vertical space up to a specified maximum height,
 * while preserving its original aspect ratio. Includes a ratcheting minimum size mechanism:
 * as the component is resized larger, its minimum size increases (up to the maximum height),
 * preventing it from being shrunk back past its largest realized size. Also supports an internal
 * scaling factor to zoom the image during rendering.
 */
public class VerticalFillImage extends JComponent {

    // minimum height allowed
    private static final int MIN_HEIGHT = 20;
    // maximum height allowed
    private int maxHeight;
    // minimum height that grows as the component is resized
    private int ratchetedHeight = MIN_HEIGHT;

    // caches the current file path to prevent redundant image loading
    private String filePath;
    // image to be rendered, corresponds to filePath
    private Image image;
    // image's width-to-height ratio
    private float aspectRatio = 1.0f;
    // scaling factor applied to the image during rendering, does not affect the component size
    private float scale = 1.0f;

    /**
     * Listener that adjusts the ratcheted height upwards as the component grows.
     */
    private final ComponentAdapter resizeListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            int newHeight = Math.min(getHeight(), maxHeight);
            int delta = newHeight - ratchetedHeight;
            if (delta <= 0) {
                return;
            }
            ratchetedHeight = newHeight;
        }
    };

    public VerticalFillImage() {
        super();
        setOpaque(true);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        addComponentListener(resizeListener);
    }

    @Override
    public void removeNotify() {
        removeComponentListener(resizeListener);
        super.removeNotify();
    }

    /**
     * Loads the image from the specified file and applies a rendering scale.
     * If the specified file and scale match the currently loaded state, the load is skipped.
     *
     * @param file  the {@code File} containing the image to load
     * @param scale a multiplier used to zoom the image during rendering
     */
    public void setImage(File file, float scale) {
        String filePath = file.getPath();
        if (Objects.equals(this.filePath, filePath)) {
            if (this.scale == scale) {
                return;
            }
        } else {
            this.filePath = filePath;
            image = ImageUtil.loadImageFromFile(filePath);
            if (image != null) {
                int w = image.getWidth(null);
                int h = image.getHeight(null);
                if (w > 0 && h > 0) {
                    aspectRatio = (float) w / h;
                }
            }
        }
        this.scale = scale;
        revalidate();
    }

    /**
     * Sets the maximum height this component is allowed to reach.
     *
     * @param maxHeight the maximum height in pixels
     * @throws IllegalArgumentException if {@code maxHeight} is less than or equal to 0
     */
    public void setMaxHeight(int maxHeight) {
        if (maxHeight <= 0) {
            throw new IllegalArgumentException("maxHeight <= 0");
        }
        this.maxHeight = Math.max(maxHeight, MIN_HEIGHT);
        ratchetedHeight = Math.min(this.maxHeight, ratchetedHeight);
    }

    @Override
    public Dimension getPreferredSize() {
        if (image == null) {
            return new Dimension(0, 0);
        }
        int h = maxHeight; // prefer max size
        int w = (int) (h * aspectRatio);
        return new Dimension(w, h);
    }

    @Override
    public Dimension getMinimumSize() {
        if (image == null) {
            return new Dimension(0, 0);
        }
        return new Dimension((int) (ratchetedHeight * aspectRatio), ratchetedHeight);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension((int) (maxHeight * aspectRatio), maxHeight);
    }

    /**
     * Custom rendering logic. The image is centered, scaled by the internal scale factor,
     * and rendered using bicubic interpolation and anti-aliasing.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            float renderHeight = Math.min(getHeight(), getWidth() / aspectRatio);
            float renderWidth = renderHeight * aspectRatio;
            float scaledWidth = renderWidth * scale;
            float scaledHeight = renderHeight * scale;
            float x = (renderWidth - scaledWidth) / 2;
            float y = (renderHeight - scaledHeight) / 2;

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.clipRect(0, 0, (int) renderWidth, (int) renderHeight);
            g2d.drawImage(image, (int) x, (int) y, (int) scaledWidth, (int) scaledHeight, this);
        } finally {
            g2d.dispose();
        }
    }
}
