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
import megamek.common.icons.AbstractIcon;
import megamek.common.util.fileUtils.AbstractDirectory;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.Iterator;

public class ForcePieceIconChooserTree extends AbstractIconChooserTree {
    //region Variable Declarations
    private LayeredForceIconLayer layer;
    //endregion Variable Declarations

    //region Constructors
    public ForcePieceIconChooserTree(final LayeredForceIconLayer layer) {
        super(layer.getListSelectionModel(), false);
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
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(AbstractIcon.ROOT_CATEGORY);
        final AbstractDirectory directory = MHQStaticDirectoryManager.getForceIcons().getCategory(getLayer().getLayerPath());
        if (directory != null) {
            final Iterator<String> catNames = directory.getCategoryNames();
            while (catNames.hasNext()) {
                final String catName = catNames.next();
                if ((catName != null) && !catName.isBlank()) {
                    final String[] names = catName.split("/");
                    addCategoryToTree(root, names);
                }
            }
        }
        return new DefaultTreeModel(root);
    }
    //endregion Initialization
}
