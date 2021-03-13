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
import mekhq.campaign.force.Force;

/**
 * Handles rendering of individual lances in the StratCon scenario wizard.
 * @author NickAragua
 */
public class ScenarioWizardLanceRenderer extends JLabel implements ListCellRenderer<Force> {
    private final Campaign campaign;

    public ScenarioWizardLanceRenderer(Campaign campaign) {
        this.campaign = campaign;
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Force> list, Force value, int index,
            boolean isSelected, boolean cellHasFocus) {

        setText(String.format("%s (BV: %d)", value.getName(), value.getTotalBV(campaign)));

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