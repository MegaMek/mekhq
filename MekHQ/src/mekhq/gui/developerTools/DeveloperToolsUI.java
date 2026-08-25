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
package mekhq.gui.developerTools;

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

/**
 * Shared UI helpers for the Developer Tools editor dialogs.
 */
final class DeveloperToolsUI {

    private DeveloperToolsUI() {
    }

    /**
     * Applies the tooltip for a form row to its label and control, sourced from the {@code <labelKey>.tooltip} key in
     * the given bundle. Rows whose key has no tooltip entry are left untouched. When the control is a scroll pane (as
     * text areas and lists are wrapped), the tooltip is set on the inner view so it shows when hovering the field
     * itself, not just its border.
     *
     * @param bundleName the resource bundle name
     * @param labelKey   the row's label key; the tooltip key is {@code labelKey + ".tooltip"}
     * @param label      the row label
     * @param control    the row control
     */
    static void applyRowTooltip(String bundleName, String labelKey, JLabel label, Component control) {
        String tooltip = getTextAt(bundleName, labelKey + ".tooltip");
        if (!isResourceKeyValid(tooltip)) {
            return;
        }

        label.setToolTipText(tooltip);
        if ((control instanceof JScrollPane scrollPane)
                  && (scrollPane.getViewport().getView() instanceof JComponent inner)) {
            inner.setToolTipText(tooltip);
        } else if (control instanceof JComponent component) {
            component.setToolTipText(tooltip);
        }
    }
}
