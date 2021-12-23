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
package mekhq.gui.trees;

import megamek.client.ui.trees.AbstractIconChooserTree;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * ForcePieceIconChooserTree is an implementation of AbstractIconChooserTree that uses a delayed
 * initialization so that the layer can first be specified, and then initializes the tree using the
 * subset of the Force Icon Directory specified by the layer's path.
 * @see AbstractIconChooserTree
 */
public class ForcePieceIconChooserTree extends AbstractIconChooserTree {
    //region Variable Declarations
    private LayeredForceIconLayer layer;
    //endregion Variable Declarations

    //region Constructors
    public ForcePieceIconChooserTree(final LayeredForceIconLayer layer) {
        super(false);
        setLayer(layer);
        setModel(createTreeModel());
    }
    //endregion Constructors

    //region Getters/Setters
    public LayeredForceIconLayer getLayer() {
        return layer;
    }

    public void setLayer(final LayeredForceIconLayer layer) {
        this.layer = layer;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected DefaultTreeModel createTreeModel() {
        return createTreeModel(new DefaultMutableTreeNode(getLayer()),
                MHQStaticDirectoryManager.getForceIcons().getCategory(getLayer().getLayerPath()));
    }
    //endregion Initialization
}
