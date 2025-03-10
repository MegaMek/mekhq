/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 */
package mekhq.gui.dialog.helpDialogs;

import java.awt.Dimension;

import javax.swing.JFrame;

import megamek.client.ui.dialogs.helpDialogs.AbstractHelpDialog;
import megamek.common.internationalization.Internationalization;

public class AutoResolveBehaviorSettingsHelpDialog extends AbstractHelpDialog {

    /**
     * Creates a new instance of AutoResolveBehaviorSettingsHelpDialog.
     * This screen opens a help dialog, using the megamek help dialog, which open an HTML file
     * @param frame  parent frame
     */
    public AutoResolveBehaviorSettingsHelpDialog(final JFrame frame) {
        super(frame, Internationalization.getText("AutoResolveBehaviorSettingsDialog.title"),
            Internationalization.getText("AutoResolveBehaviorSettingsDialog.autoResolveHelpPath"));

        setMinimumSize(new Dimension(400, 400));
        setModalExclusionType(ModalExclusionType.TOOLKIT_EXCLUDE);
    }

}
