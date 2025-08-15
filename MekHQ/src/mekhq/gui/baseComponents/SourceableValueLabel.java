/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import javax.swing.JLabel;

import mekhq.campaign.universe.SourceableValue;

/**
 * This class extends a basic JLabel to handle the characteristics of a sourceable value, namely adding the sourced
 * information as a tooltip.
 */
public class SourceableValueLabel extends JLabel {

    SourceableValue sourcedValue;
    String format;


    public SourceableValueLabel(SourceableValue v) {
        this(v, "%s");

    }

    public SourceableValueLabel(SourceableValue v, String f) {
        super();
        sourcedValue = v;
        format = f;
        initialize();
    }

    private void initialize() {
        setText(String.format(format, sourcedValue.getValue()));
        setToolTipText("<html><b>Source:</b> " +
                             (sourcedValue.isCanon() ? sourcedValue.getSource() : "noncanon") +
                             "</html>");
    }
}
