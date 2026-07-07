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

package mekhq.gui.utilities;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import megamek.logging.MMLogger;

/**
 * A reusable decorator for {@link TableCellRenderer}s that measures rendering performance.
 * Used for development, do not remove because it's not used in the code.
 *
 * @author Hokk
 */
public class InstrumentedTableCellRenderer implements TableCellRenderer {

    private static final MMLogger LOGGER = MMLogger.create(InstrumentedTableCellRenderer.class);
    private static final int REPORT_FREQUENCY = 5_000;

    private final TableCellRenderer delegate;
    private long totalRenderTimeNanos = 0;
    private long renderCalls = 0;

    public InstrumentedTableCellRenderer(TableCellRenderer delegate) {
        this.delegate = delegate;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
          boolean hasFocus, int row, int column) {
        long start = System.nanoTime();

        Component result = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        long end = System.nanoTime();
        recordRenderTime(end - start);

        return result;
    }

    private void recordRenderTime(long durationNanos) {
        totalRenderTimeNanos += durationNanos;
        renderCalls++;

        if (renderCalls % REPORT_FREQUENCY == 0) {
            double avgMillis = (totalRenderTimeNanos / (double) renderCalls) / 1_000_000.0;
            LOGGER.info(String.format("Avg %s render time over last %,d calls: %.4f ms per cell",
                  delegate.getClass().getSimpleName(), renderCalls, avgMillis));
        }
    }
}
