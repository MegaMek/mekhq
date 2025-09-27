/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.reportDialogs;

import java.awt.Container;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;

import mekhq.gui.baseComponents.AbstractMHQDialogBasic;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * This displays a standard report for MekHQ.
 * <p>
 * Inheriting classes must call initialize() in their constructors and override getText.
 */
public abstract class AbstractReportDialog extends AbstractMHQDialogBasic {
    //region Constructors
    protected AbstractReportDialog(final JFrame frame, final String name, final String title) {
        super(frame, name, title);
    }
    //endregion Constructors

    //region Initialization
    @Override
    protected Container createCenterPane() {
        final JScrollPane scrollPane = new JScrollPaneWithSpeed(createTxtReport());
        scrollPane.setBorder(new EmptyBorder(2, 10, 2, 2));
        scrollPane.setName("reportPane");

        return scrollPane;
    }

    protected abstract JTextPane createTxtReport();
    //endregion Initialization
}
