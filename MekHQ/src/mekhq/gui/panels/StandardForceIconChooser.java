/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.util.fileUtils.AbstractDirectory;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.gui.trees.StandardForceIconChooserTree;

import javax.swing.*;

public class StandardForceIconChooser extends AbstractIconChooser {
    //region Constructors
    public StandardForceIconChooser(final @Nullable AbstractIcon icon) {
        super(new StandardForceIconChooserTree(), icon);
    }
    //endregion Constructors

    @Override
    protected @Nullable AbstractDirectory getDirectory() {
        return MHQStaticDirectoryManager.getForceIcons();
    }

    @Override
    protected StandardForceIcon createIcon(String category, final String filename) {
        return new StandardForceIcon(category, filename);
    }

    @Override
    public @Nullable StandardForceIcon getSelectedItem() {
        final AbstractIcon icon = super.getSelectedItem();
        return (icon instanceof StandardForceIcon) ? (StandardForceIcon) icon : null;
    }

    @Override
    public void refreshDirectory() {
        MHQStaticDirectoryManager.refreshForceIcons();
        refreshDirectory(new StandardForceIconChooserTree());
    }
}
