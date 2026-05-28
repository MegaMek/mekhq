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
import javax.swing.JPanel;

/**
 * A custom {@link JPanel} that enforces minimum and maximum width constraints.
 * <p>
 * This component is useful in flexible UI layouts (such as {@code BoxLayout}) where a panel's horizontal growth and
 * shrinkage must be explicitly limited. It overrides the standard sizing methods to ensure the width always respects
 * the specified {@code minWidth} and {@code maxWidth}, while allowing the height to scale dynamically based on the
 * layout manager's calculations.
 * </p>
 */
public class HorizontallyConstrainedPanel extends JPanel {
    private final int minWidth;
    private final int maxWidth;

    public HorizontallyConstrainedPanel(int minWidth, int maxWidth) {
        if (minWidth > maxWidth) {
            throw new IllegalArgumentException("minWidth cannot be greater than maxWidth");
        }
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(minWidth, super.getMinimumSize().height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(maxWidth, Integer.MAX_VALUE);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(maxWidth, super.getPreferredSize().height);
    }
}
