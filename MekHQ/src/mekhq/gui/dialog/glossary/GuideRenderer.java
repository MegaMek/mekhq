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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

import megamek.logging.MMLogger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Draws the pages of one open guide, one at a time on a thread of its own, so scrolling never waits on PDFBox.
 *
 * <p>A PDFBox document must not be used from two threads at once, so the document is only ever touched on this
 * renderer's thread, and is closed there too. Pages are drawn in the order they are asked for, which is the order the
 * viewer paints them, so the pages on screen come first.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class GuideRenderer {
    private static final MMLogger LOGGER = MMLogger.create(GuideRenderer.class);

    private final PDDocument document;
    private final PDFRenderer renderer;
    private final ExecutorService thread = Executors.newSingleThreadExecutor(task -> {
        Thread worker = new Thread(task, "Guide page renderer");
        worker.setDaemon(true);
        return worker;
    });
    /** The pages already waiting to be drawn, at the scale asked, so a page is not queued twice. */
    private final Set<String> pending = ConcurrentHashMap.newKeySet();
    private volatile boolean isClosed;

    /**
     * @param document the open guide; the renderer closes it when it is itself closed
     */
    GuideRenderer(PDDocument document) {
        this.document = document;
        this.renderer = new PDFRenderer(document);
        this.renderer.setSubsamplingAllowed(true);
    }

    /**
     * Draws a page in the background.
     *
     * @param pageIndex  the page, from 0
     * @param scale      pixels per PDF point
     * @param onRendered given the image on the Swing thread; not called if the page could not be drawn or the
     *                   renderer was closed first
     */
    void render(int pageIndex, float scale, Consumer<BufferedImage> onRendered) {
        String key = pageIndex + "@" + scale;
        if (isClosed || !pending.add(key)) {
            return;
        }
        try {
            thread.submit(() -> {
                try {
                    if (isClosed) {
                        return;
                    }
                    BufferedImage image = renderer.renderImage(pageIndex, scale, ImageType.RGB);
                    SwingUtilities.invokeLater(() -> {
                        if (!isClosed) {
                            onRendered.accept(image);
                        }
                    });
                } catch (IOException | RuntimeException ex) {
                    LOGGER.warn(ex, "Unable to draw page {} of a guide", pageIndex + 1);
                } finally {
                    pending.remove(key);
                }
            });
        } catch (RejectedExecutionException ex) {
            pending.remove(key);
        }
    }

    /** Stops drawing and closes the guide, once any page being drawn is finished. */
    void close() {
        if (isClosed) {
            return;
        }
        isClosed = true;
        try {
            thread.submit(() -> {
                try {
                    document.close();
                } catch (IOException ex) {
                    LOGGER.warn(ex, "Unable to close a guide");
                }
            });
        } catch (RejectedExecutionException ignored) {
            // Already shut down.
        }
        thread.shutdown();
    }
}
