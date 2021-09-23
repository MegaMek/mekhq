/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.icons.ForcePieceIcon;
import mekhq.campaign.icons.LayeredForceIcon;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;
import mekhq.gui.trees.ForcePieceIconChooserTree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.List;
import java.util.stream.Collectors;

public class ForcePieceIconChooser extends AbstractIconChooser {
    //region Variable Declarations
    private LayeredForceIconLayer layer;
    //endregion Variable Declarations

    //region Constructors
    public ForcePieceIconChooser(final JFrame frame, final LayeredForceIconLayer layer,
                                 final @Nullable AbstractIcon icon) {
        super(frame, "ForcePieceIconChooser" + layer.name(), new ForcePieceIconChooserTree(layer),
                null, false);
        setLayer(layer);
        initialize();
        setSelection(icon);
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
    protected void finalizeInitialization() {
        super.finalizeInitialization();
        getImageList().setSelectionMode(getLayer().getListSelectionMode());
    }
    //endregion Initialization

    @Override
    protected @Nullable AbstractDirectory getDirectory() {
        return (MHQStaticDirectoryManager.getForceIcons() == null) ? null
                : MHQStaticDirectoryManager.getForceIcons().getCategory(getLayer().getLayerPath());
    }

    @Override
    protected ForcePieceIcon createIcon(String category, final String filename) {
        category = category.replace(getLayer().getLayerPath(), "");
        return new ForcePieceIcon(getLayer(), category, filename);
    }

    @Override
    public @Nullable ForcePieceIcon getSelectedItem() {
        final AbstractIcon icon = super.getSelectedItem();
        return (icon instanceof ForcePieceIcon) ? (ForcePieceIcon) icon : null;
    }

    public List<ForcePieceIcon> getSelectedItems() {
        return getImageList().getSelectedValuesList().stream().map(icon -> (ForcePieceIcon) icon)
                .collect(Collectors.toList());
    }

    @Override
    public void refreshDirectory() {
        MHQStaticDirectoryManager.refreshForceIcons();
        refreshTree();
    }

    /**
     * This is separated as the general use case for refreshing is to have the force icon directory
     * refreshed first and then followed by refreshing each individual force piece icon chooser
     * without refreshing the actual directory.
     */
    public void refreshTree() {
        refreshDirectory(new ForcePieceIconChooserTree(getLayer()));
    }

    @Override
    protected void setSelection(final @Nullable AbstractIcon icon) {
        if (getTreeCategories() == null) {
            return;
        }

        // Always start with the origin selected
        getTreeCategories().setSelectionPath(new TreePath(
                ((DefaultMutableTreeNode) getTreeCategories().getModel().getRoot()).getPath()));

        if (icon instanceof LayeredForceIcon) {
            final List<ForcePieceIcon> forcePieceIcons = ((LayeredForceIcon) icon).getPieces().get(getLayer());
            if ((forcePieceIcons != null) && !forcePieceIcons.isEmpty()) {
                getImageList().setSelectedValues(forcePieceIcons.toArray(new ForcePieceIcon[]{}));
            } else {
                getImageList().clearSelection();
            }
        }
    }
}
