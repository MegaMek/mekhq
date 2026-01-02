/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.util.fileUtils.AbstractDirectory;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.icons.StandardFormationIcon;
import mekhq.gui.trees.StandardFormationIconChooserTree;

/**
 * StandardFormationIconChooser is an implementation of AbstractMHQIconChooser that is used to select a StandardFormationIcon
 * from the Formation Icon Directory.
 *
 * @see AbstractMHQIconChooser
 * @see abstractIconChooserPanel
 */
public class StandardFormationIconChooser extends AbstractMHQIconChooser {
    //region Constructors
    public StandardFormationIconChooser(final JFrame frame, final @Nullable AbstractIcon icon) {
        this(frame, "StandardFormationIconChooser", icon);
    }

    protected StandardFormationIconChooser(final JFrame frame, final String name,
          final @Nullable AbstractIcon icon) {
        super(frame, name, new StandardFormationIconChooserTree(), icon);
    }
    //endregion Constructors

    @Override
    protected @Nullable AbstractDirectory getDirectory() {
        return MHQStaticDirectoryManager.getFormationIcons();
    }

    @Override
    protected StandardFormationIcon createIcon(String category, final String filename) {
        return new StandardFormationIcon(category, filename);
    }

    @Override
    public @Nullable StandardFormationIcon getSelectedItem() {
        final AbstractIcon icon = super.getSelectedItem();
        return (icon instanceof StandardFormationIcon) ? (StandardFormationIcon) icon : null;
    }

    @Override
    public void refreshDirectory() {
        MHQStaticDirectoryManager.refreshFormationIcons();
        refreshDirectory(new StandardFormationIconChooserTree());
    }
}
