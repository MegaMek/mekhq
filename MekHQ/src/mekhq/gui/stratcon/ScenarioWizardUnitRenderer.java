/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package mekhq.gui.stratcon;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;

/**
 * Handles rendering individual units in lists in the StratCon scenario wizard.
 * @author NickAragua
 */
public class ScenarioWizardUnitRenderer extends JLabel implements ListCellRenderer<Unit> {
    public ScenarioWizardUnitRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Unit> list, Unit value, int index,
            boolean isSelected, boolean cellHasFocus) {

        setText(String.format("%s (BV: %d)", value.getName(), value.getEntity().calculateBattleValue()));

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        return this;
    }

}