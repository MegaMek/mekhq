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

/**
 * The ForcePieceIconChooser allows one to select ForcePieceIcons from a single
 * LayeredForceIconLayer layer's directory within the Force Icon Directory. It allows for single or
 * multiple selection, based on the layer's individual ListSelectionModel. Finally, it is coded to
 * have multiple initializations, namely one per layer, and to handle their individual preferences.
 *
 * It is designed to be used as part of creating a LayeredForceIcon, and not to be used as its
 * own chooser.
 * @see AbstractMHQIconChooser
 * @see AbstractIconChooser
 */
public class ForcePieceIconChooser extends AbstractMHQIconChooser {
    //region Variable Declarations
    private LayeredForceIconLayer layer;
    //endregion Variable Declarations

    //region Constructors
    public ForcePieceIconChooser(final JFrame frame, final LayeredForceIconLayer layer,
                                 final @Nullable AbstractIcon icon) {
        super(frame, layer.name() + "ForcePieceIconChooser", new ForcePieceIconChooserTree(layer),
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
    /**
     * This overrides the base initialization finalization to allow individualized preferences by
     * layer and to set the selection mode on the image list (thereby allowing for multiselect).
     */
    @Override
    protected void finalizeInitialization() throws Exception {
        // The first two are required for the preferences to be individual based on the layer
        getSplitPane().setName(getLayer().name() + "Pane");
        getChkIncludeSubdirectories().setName("chkIncludeSubdirectories" + getLayer().name());

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

    /**
     * This method should not be used for this icon chooser, as the method is not designed for
     * multiselect.
     *
     * @return the first selected ForcePieceIcons, which may be null if there is nothing selected.
     */
    @Override
    public @Nullable ForcePieceIcon getSelectedItem() {
        final AbstractIcon icon = super.getSelectedItem();
        return (icon instanceof ForcePieceIcon) ? (ForcePieceIcon) icon : null;
    }

    /**
     * @return the selected ForcePieceIcons, which may be empty if there are no selected icons
     */
    public List<ForcePieceIcon> getSelectedItems() {
        return getImageList().getSelectedValuesList().stream()
                .map(icon -> (ForcePieceIcon) icon)
                .collect(Collectors.toList());
    }

    /**
     * This is overridden as it is required, but the general use case is to instead use the
     * refreshTree method below.
     */
    @Override
    public void refreshDirectory() {
        MHQStaticDirectoryManager.refreshForceIcons();
        refreshTree();
    }

    /**
     * This is separated as the general use case for refreshing is to have the force icon directory
     * refreshed first, which is then followed by refreshing each individual force piece icon chooser
     * without refreshing the actual directory.
     */
    public void refreshTree() {
        refreshDirectory(new ForcePieceIconChooserTree(getLayer()));
    }

    /**
     * This override enables multiple categories to be selected based on the provided AbstractIcon.
     *
     * Selects the given categories in the tree, updates the shown images to these categories, and
     * selects the items given by the filenames in the image list.
     * @param icon the icon to select, which should be a LayeredForceIcon. It may be null to set the
     *             origin alone as selected.
     */
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
