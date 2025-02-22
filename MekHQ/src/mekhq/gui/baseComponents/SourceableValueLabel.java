/*
 * Copyright (C) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.baseComponents;

import mekhq.campaign.universe.SourceableValue;

import javax.swing.*;

/**
 * This class extends a basic JLabel to handle the characteristics of a sourceable value,
 * namely adding the sourced information as a tooltip.
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
        setToolTipText("<html><b>Source:</b> " + (sourcedValue.isCanon() ? sourcedValue.getSource() : "noncanon") + "</html>");
    }
}
