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
package mekhq.gui.dialog.glossary;

import static mekhq.gui.baseComponents.hud.HudStyle.AMBER;
import static mekhq.gui.baseComponents.hud.HudStyle.GROUND;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.IntFunction;
import javax.swing.JComponent;

import jakarta.annotation.Nullable;
import megamek.client.ui.util.UIUtil;
import mekhq.gui.dialog.glossary.GuideDocument.PageSize;

/**
 * One page of a guide, drawn at the viewer's zoom. The page asks its renderer for an image the first time it is
 * painted at a new size, and shows the last image it had, stretched, until the new one arrives; pages scrolled far
 * out of view can let their images go to save memory.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class GuidePageView extends JComponent {
    /** Pages are never drawn finer than this many pixels per point, however far the player zooms in. */
    private static final float MAXIMUM_RENDER_SCALE = 6f;
    private static final Color PAPER = new Color(244, 244, 242);

    private final GuideRenderer renderer;
    private final int pageIndex;
    private final PageSize size;
    private final IntFunction<String> matchesLabel;
    private float zoom;
    private @Nullable BufferedImage image;
    private float imageScale;
    private float requestedScale;
    /** Set when the image is let go, so a drawing still on its way is not kept; cleared when the page is painted. */
    private boolean isDropped;
    private int matches;

    /**
     * @param renderer     the renderer of the guide the page belongs to
     * @param pageIndex    the page, from 0
     * @param size         the page's size, in points
     * @param zoom         screen pixels per point
     * @param matchesLabel gives the label of a page's match count
     */
    GuidePageView(GuideRenderer renderer, int pageIndex, PageSize size, float zoom,
          IntFunction<String> matchesLabel) {
        this.renderer = renderer;
        this.pageIndex = pageIndex;
        this.size = size;
        this.zoom = zoom;
        this.matchesLabel = matchesLabel;
        setOpaque(false);
    }

    int pageIndex() {
        return pageIndex;
    }

    /** Sets the zoom, in screen pixels per point; the page keeps its old image until a sharper one is drawn. */
    void setZoom(float zoom) {
        if (this.zoom != zoom) {
            this.zoom = zoom;
            requestedScale = 0;
            revalidate();
            repaint();
        }
    }

    /** Shows how many times the find box's words appear on the page; 0 for none. */
    void setMatches(int matches) {
        if (this.matches != matches) {
            this.matches = matches;
            repaint();
        }
    }

    /** Lets the page's image go; it is drawn again if the page comes back into view. */
    void dropImage() {
        image = null;
        imageScale = 0;
        requestedScale = 0;
        isDropped = true;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Math.max(1, Math.round(size.width() * zoom)),
              Math.max(1, Math.round(size.height() * zoom)));
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        // Being painted means the page is in view again, so a drawing on its way is wanted after all.
        isDropped = false;
        try {
            int width = getWidth();
            int height = getHeight();
            canvas.setColor(PAPER);
            canvas.fillRect(0, 0, width, height);

            // Drawn for the screen's own pixels, so pages stay sharp on high-resolution displays.
            float deviceScale = (float) canvas.getTransform().getScaleX();
            float wantedScale = Math.min(MAXIMUM_RENDER_SCALE, zoom * Math.max(1f, deviceScale));
            if (image != null) {
                canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                canvas.drawImage(image, 0, 0, width, height, null);
            }
            if ((image == null || imageScale != wantedScale) && requestedScale != wantedScale) {
                requestedScale = wantedScale;
                renderer.render(pageIndex, wantedScale, rendered -> {
                    // A zoom changed while this was drawing asks for another size; this one is still better than none.
                    // A page let go while this was drawing is far from view, so the drawing is not kept.
                    if (!isDropped && (image == null || requestedScale == wantedScale)) {
                        image = rendered;
                        imageScale = wantedScale;
                        repaint();
                    }
                });
            }

            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (matches > 0) {
                canvas.setColor(AMBER);
                canvas.setStroke(new BasicStroke(UIUtil.scaleForGUI(2)));
                canvas.drawRect(1, 1, width - 3, height - 3);
                String flag = matchesLabel.apply(matches);
                canvas.setFont(hudFont(Font.BOLD, 0.74f, 0.04f));
                FontMetrics metrics = canvas.getFontMetrics();
                int padding = UIUtil.scaleForGUI(7);
                int flagWidth = metrics.stringWidth(flag) + (padding * 2);
                int flagHeight = metrics.getHeight() + UIUtil.scaleForGUI(2);
                canvas.fillRect(width - flagWidth, 0, flagWidth, flagHeight);
                canvas.setColor(GROUND);
                canvas.drawString(flag, width - flagWidth + padding, metrics.getAscent() + UIUtil.scaleForGUI(1));
            }
        } finally {
            canvas.dispose();
        }
    }
}
