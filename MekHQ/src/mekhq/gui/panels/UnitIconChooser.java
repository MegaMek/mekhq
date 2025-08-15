/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.panels;

import javax.swing.JFrame;

import megamek.client.ui.panels.abstractPanels.abstractIconChooserPanel;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import mekhq.campaign.icons.UnitIcon;

/**
 * UnitIconChooser is an implementation of StandardForceIconChooser that is used to select a UnitIcon from the Force
 * Icon Directory.
 * <p>
 * The only differences from its originator are that it specifies the icon creation and selection methods to be for a
 * UnitIcon instead of a StandardForceIcon.
 *
 * @see StandardForceIconChooser
 * @see AbstractMHQIconChooser
 * @see abstractIconChooserPanel
 */
public class UnitIconChooser extends StandardForceIconChooser {
    //region Constructors
    public UnitIconChooser(final JFrame frame, final @Nullable AbstractIcon icon) {
        super(frame, "UnitIconChooser", icon);
    }
    //endregion Constructors

    @Override
    protected UnitIcon createIcon(String category, final String filename) {
        return new UnitIcon(category, filename);
    }

    @Override
    public @Nullable UnitIcon getSelectedItem() {
        final AbstractIcon icon = super.getSelectedItem();
        return (icon instanceof UnitIcon) ? (UnitIcon) icon : null;
    }
}
