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

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.unit.Unit;

import javax.swing.*;
import java.awt.*;

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
        Campaign campaign = value.getCampaign();

        int valueForceId = value.getForceId();
        Force force = campaign.getForce(valueForceId);

        String forceName = "";
        if (force != null) {
            forceName = force.getFullName();
            String originNodeName = ", " + campaign.getForce(0).getName();
            forceName = forceName.replaceAll(originNodeName, "");
        }

        setText(String.format("<html><b>%s (%s/%s)</b> - %s - Base BV: %d<br><i>%s</i></html>",
            value.getName(), value.getEntity().getCrew().getGunnery(), value.getEntity().getCrew().getPiloting(),
            value.getCondition(), value.getEntity().calculateBattleValue(true, true),
            forceName));

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
