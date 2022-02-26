/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.panels;

import megamek.client.ui.panels.AbstractIconChooser;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import mekhq.campaign.icons.UnitIcon;

import javax.swing.*;

/**
 * UnitIconChooser is an implementation of StandardForceIconChooser that is used to select a
 * UnitIcon from the Force Icon Directory.
 *
 * The only differences from its originator are that it specifies the icon creation and selection
 * methods to be for a UnitIcon instead of a StandardForceIcon.
 * @see StandardForceIconChooser
 * @see AbstractMHQIconChooser
 * @see AbstractIconChooser
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
