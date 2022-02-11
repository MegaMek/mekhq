/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
import megamek.client.ui.trees.AbstractIconChooserTree;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import mekhq.MekHQ;

import javax.swing.*;

/**
 * AbstractMHQIconChooser is an extension of AbstractIconChooser that moves the preferences to
 * MekHQ.
 * @see AbstractIconChooser
 */
public abstract class AbstractMHQIconChooser extends AbstractIconChooser {
    //region Constructors
    protected AbstractMHQIconChooser(final JFrame frame, final String name,
                                     final AbstractIconChooserTree tree,
                                     final @Nullable AbstractIcon icon) {
        super(frame, name, tree, icon);
    }

    protected AbstractMHQIconChooser(final JFrame frame, final String name,
                                     final AbstractIconChooserTree tree,
                                     final @Nullable AbstractIcon icon, final boolean initialize) {
        super(frame, name, tree, icon, initialize);
    }
    //endregion Constructors

    //region Initialization
    @Override
    protected void setPreferences() {
        setPreferences(MekHQ.getMHQPreferences().forClass(getClass()));
    }
    //endregion Initialization
}
