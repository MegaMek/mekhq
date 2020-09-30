/*
 * ShowUnitBvAction.java
 *
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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

package mekhq.campaign.unit.actions;

import java.awt.Dimension;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;

/**
 * Launches a window displaying the BV calculation for a unit.
 */
public class ShowUnitBvAction implements IUnitAction {

	@Override
	public void Execute(Campaign campaign, Unit unit) {
        if (unit == null) {
            return;
        }

        final Entity entity = unit.getEntity();
        if (entity == null) {
            return;
        }

        entity.calculateBattleValue();

        final JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setText(entity.getBVText());
        editorPane.setCaretPosition(0);

        final JScrollPane scrollPane = new JScrollPane(editorPane,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        scrollPane.setPreferredSize(new Dimension(550, 300));

        JOptionPane.showMessageDialog(null, scrollPane, "BV",
            JOptionPane.INFORMATION_MESSAGE, null);
	}

}
