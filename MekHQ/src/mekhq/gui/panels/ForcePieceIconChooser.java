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

import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.util.fileUtils.AbstractDirectory;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.icons.ForcePieceIcon;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;
import mekhq.gui.trees.ForcePieceIconChooserTree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class ForcePieceIconChooser extends StandardForceIconChooser {
    //region Variable Declarations
    private LayeredForceIconLayer layer;
    //endregion Variable Declarations

    //region Constructors
    public ForcePieceIconChooser(final LayeredForceIconLayer layer, final @Nullable AbstractIcon icon) {
        super(null, null, false);
        setLayer(layer);
        initialize(new ForcePieceIconChooserTree(getLayer()));
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

    @Override
    protected @Nullable AbstractDirectory getDirectory() {
        return (MHQStaticDirectoryManager.getForceIcons() == null) ? null
                : MHQStaticDirectoryManager.getForceIcons().getCategory(getLayer().getLayerPath());
    }

    @Override
    protected ForcePieceIcon createIcon(final @Nullable String category, final @Nullable String filename) {
        return new ForcePieceIcon(getLayer(), category, filename);
    }

    @Override
    protected List<AbstractIcon> getItems(final String category) {
        final List<AbstractIcon> result = new ArrayList<>();
        // The portraits of the selected category are presented.
        // When the includeSubDirs flag is true, all categories
        // below the selected one are also presented.
        if (includeSubDirs) {
            for (final Iterator<String> catNames = getDirectory().getCategoryNames(); catNames.hasNext(); ) {
                final String tcat = catNames.next();
                if (tcat.startsWith(category)) {
                    addCategoryItems(tcat, result);
                }
            }
        } else {
            addCategoryItems(category, result);
        }
        return result;
    }

    @Override
    public @Nullable ForcePieceIcon getSelectedItem() {
        final AbstractIcon icon = super.getSelectedItem();
        return (icon instanceof ForcePieceIcon) ? (ForcePieceIcon) icon : null;
    }

    @Override
    public void refreshDirectory() {
        MHQStaticDirectoryManager.refreshForceIcons();
        refreshDirectory(new ForcePieceIconChooserTree(getLayer()));
    }

    @Override
    protected void setSelection(final @Nullable AbstractIcon icon) {
        if (getTreeCategories() == null) {
            return;
        }

        // TODO : Fixme

        // This cumbersome code takes the category name and transforms it into
        // a TreePath so it can be selected in the dialog
        // When the icon directory has changes, the previous selection might not be found
        boolean found = false;
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) getTreeCategories().getModel().getRoot();
        DefaultMutableTreeNode currentNode = root;
        if (icon != null) {
            for (String name : icon.getCategory().split(Pattern.quote("/"))) {
                found = false;
                for (Enumeration<?> children = currentNode.children(); children.hasMoreElements(); ) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                    if (name.equals(child.getUserObject())) {
                        currentNode = child;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    break;
                }
            }
        }
        // Select the root if the selection could not be found
        if (found) {
            getTreeCategories().setSelectionPath(new TreePath(currentNode.getPath()));
            getImageList().setSelectedValue(icon, true);
        } else {
            getTreeCategories().setSelectionPath(new TreePath(root.getPath()));
        }
    }
}
