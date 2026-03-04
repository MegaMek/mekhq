/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.trees;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import megamek.client.ui.trees.AbstractIconChooserTree;
import megamek.common.icons.AbstractIcon;
import mekhq.MHQStaticDirectoryManager;

/**
 * StandardFormationIconChooserTree is an implementation of AbstractIconChooserTree that initializes the tree using the
 * Formation Icon Directory.
 *
 * <p>Known as {@code StandardForceIconChooserTree} prior to 0.50.12</p>
 *
 * @see AbstractIconChooserTree
 *
 * @since 0.50.12
 */
public class StandardFormationIconChooserTree extends AbstractIconChooserTree {
    //region Constructors
    public StandardFormationIconChooserTree() {
        super();
    }
    //endregion Constructors

    //region Initialization
    @Override
    protected DefaultTreeModel createTreeModel() {
        return createTreeModel(new DefaultMutableTreeNode(AbstractIcon.ROOT_CATEGORY),
              MHQStaticDirectoryManager.getFormationIcons());
    }
    //endregion Initialization
}
