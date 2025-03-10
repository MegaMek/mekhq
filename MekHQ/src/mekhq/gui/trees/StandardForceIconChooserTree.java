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
 */
package mekhq.gui.trees;

import megamek.client.ui.trees.AbstractIconChooserTree;
import megamek.common.icons.AbstractIcon;
import mekhq.MHQStaticDirectoryManager;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * StandardForceIconChooserTree is an implementation of AbstractIconChooserTree that initializes the
 * tree using the Force Icon Directory.
 * @see AbstractIconChooserTree
 */
public class StandardForceIconChooserTree extends AbstractIconChooserTree {
    //region Constructors
    public StandardForceIconChooserTree() {
        super();
    }
    //endregion Constructors

    //region Initialization
    @Override
    protected DefaultTreeModel createTreeModel() {
        return createTreeModel(new DefaultMutableTreeNode(AbstractIcon.ROOT_CATEGORY),
                MHQStaticDirectoryManager.getForceIcons());
    }
    //endregion Initialization
}
