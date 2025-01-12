/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import mekhq.MHQConstants;
import mekhq.MekHQ;

import javax.swing.*;
import java.util.ResourceBundle;

public class ActiveContractWarning {
    private final String RESOURCE_KEY = "mekhq.resources.GUI";
    private final transient ResourceBundle resources =
        ResourceBundle.getBundle(RESOURCE_KEY, MekHQ.getMHQOptions().getLocale());

    /**
     * Displays a warning dialog to the user indicating that the campaign has
     * an active contract. The warning informs the user to complete the active
     * contract before updating to the specified version of the application.
     * <p>
     * The displayed message includes the current application version retrieved
     * from {@code MHQConstants.VERSION}.
     * <p>
     * The dialog uses an error message icon and is displayed in a modal popup.
     *
     * @see JOptionPane#showMessageDialog(java.awt.Component, Object, String, int)
     */
    public ActiveContractWarning() {
        String message = String.format(resources.getString("ActiveContractWarning.message"),
            MHQConstants.VERSION);

        JOptionPane.showMessageDialog(
            null,
            message,
            resources.getString("ActiveContractWarning.title"),
            JOptionPane.ERROR_MESSAGE
        );
    }
}
