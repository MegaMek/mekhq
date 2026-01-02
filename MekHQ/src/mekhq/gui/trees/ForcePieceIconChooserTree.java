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
package mekhq.gui.trees;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import megamek.client.ui.trees.AbstractIconChooserTree;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.icons.enums.LayeredFormationIconLayer;

/**
 * ForcePieceIconChooserTree is an implementation of AbstractIconChooserTree that uses a delayed initialization so that
 * the layer can first be specified, and then initializes the tree using the subset of the Formation Icon Directory
 * specified by the layer's path.
 *
 * @see AbstractIconChooserTree
 */
public class ForcePieceIconChooserTree extends AbstractIconChooserTree {
    //region Variable Declarations
    private LayeredFormationIconLayer layer;
    //endregion Variable Declarations

    //region Constructors
    public ForcePieceIconChooserTree(final LayeredFormationIconLayer layer) {
        super(false);
        setLayer(layer);
        setModel(createTreeModel());
    }
    //endregion Constructors

    //region Getters/Setters
    public LayeredFormationIconLayer getLayer() {
        return layer;
    }

    public void setLayer(final LayeredFormationIconLayer layer) {
        this.layer = layer;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected DefaultTreeModel createTreeModel() {
        return createTreeModel(new DefaultMutableTreeNode(getLayer()),
              MHQStaticDirectoryManager.getFormationIcons().getCategory(getLayer().getLayerPath()));
    }
    //endregion Initialization
}
