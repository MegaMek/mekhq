/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.panels;

import javax.swing.JFrame;

import megamek.client.ui.panels.AbstractIconChooser;
import megamek.client.ui.trees.AbstractIconChooserTree;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import mekhq.MekHQ;

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
    protected void setPreferences() throws Exception {
        setPreferences(MekHQ.getMHQPreferences().forClass(getClass()));
    }
    //endregion Initialization
}
