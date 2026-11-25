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
package mekhq.gui.dialog.markets.personnelMarket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import mekhq.gui.dialog.markets.personnelMarket.PersonnelMarketHud.LazyIcon;
import org.junit.jupiter.api.Test;

class PersonnelMarketHudTest {
    private static final int SIZE = 4;

    private static Icon redIcon() {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, SIZE, SIZE);
        graphics.dispose();
        return new ImageIcon(image);
    }

    private static BufferedImage paint(Icon icon) {
        BufferedImage canvas = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = canvas.getGraphics();
        icon.paintIcon(null, graphics, 0, 0);
        graphics.dispose();
        return canvas;
    }

    @Test
    void lazyIconIsMeasuredWithoutLoadingItsImage() {
        AtomicInteger loads = new AtomicInteger();
        LazyIcon icon = new LazyIcon(SIZE, () -> {
            loads.incrementAndGet();
            return redIcon();
        });

        assertEquals(SIZE, icon.getIconWidth());
        assertEquals(SIZE, icon.getIconHeight());
        assertEquals(0, loads.get());
    }

    @Test
    void lazyIconLoadsItsImageOnceAndPaintsIt() {
        AtomicInteger loads = new AtomicInteger();
        LazyIcon icon = new LazyIcon(SIZE, () -> {
            loads.incrementAndGet();
            return redIcon();
        });

        assertEquals(Color.RED.getRGB(), paint(icon).getRGB(1, 1));
        assertEquals(Color.RED.getRGB(), paint(icon).getRGB(1, 1));
        assertEquals(1, loads.get());
    }

    @Test
    void lazyIconWithNoImageLooksForOneOnceAndPaintsNothing() {
        AtomicInteger loads = new AtomicInteger();
        LazyIcon icon = new LazyIcon(SIZE, () -> {
            loads.incrementAndGet();
            return null;
        });

        assertDoesNotThrow(() -> paint(icon));
        assertEquals(0, paint(icon).getRGB(1, 1));
        assertEquals(1, loads.get());
    }
}
