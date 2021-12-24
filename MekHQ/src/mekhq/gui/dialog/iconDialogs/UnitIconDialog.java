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
package mekhq.gui.dialog.iconDialogs;

import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.dialogs.AbstractIconChooserDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import mekhq.campaign.icons.UnitIcon;
import mekhq.gui.panels.UnitIconChooser;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Enumeration;

/**
 * UnitIconDialog is an implementation of StandardForceIconDialog that is used to select
 * a UnitIcon from the Force Icon Directory. It defaults to the Force/Units/ category, as that's
 * the primary location for them, and allows the selected icon to be overridden when necessary
 * (such as when we want to specify that a Unit does not have an icon)
 * @see StandardForceIconDialog
 * @see AbstractIconChooserDialog
 */
public class UnitIconDialog extends StandardForceIconDialog {
    //region Variable Declarations
    private UnitIcon override;
    //endregion Variable Declarations

    //region Constructors
    public UnitIconDialog(final JFrame frame, final @Nullable AbstractIcon icon) {
        super(frame, "UnitIconDialog", "UnitIconDialog.title",
                new UnitIconChooser(frame, icon));
    }
    //endregion Constructors

    //region Getters
    @Override
    protected UnitIconChooser getChooser() {
        return (UnitIconChooser) super.getChooser();
    }

    @Override
    public @Nullable UnitIcon getSelectedItem() {
        return (getOverride() == null) ? getChooser().getSelectedItem() : getOverride();
    }

    /**
     * This is used to override the selected UnitIcon, to allow it to be specified outside of the
     * possible selections. The primary use is in setting up a non-existent UnitIcon.
     * @return the current selected item override, which is null when there isn't an override.
     */
    public @Nullable UnitIcon getOverride() {
        return override;
    }

    public void setOverride(final @Nullable UnitIcon override) {
        this.override = override;
    }
    //endregion Getters

    //region Initialization
    @Override
    protected JPanel createButtonPanel() {
        final JPanel panel = new JPanel(new GridLayout(1, 4));
        panel.setName("buttonPanel");

        panel.add(new MMButton("btnOk", resources, "Ok.text", "Ok.toolTipText",
                this::okButtonActionPerformed));
        panel.add(new MMButton("btnNone", resources, "None.text",
                "UnitIconDialog.btnNone.toolTipText", this::noneActionPerformed));
        panel.add(new MMButton("btnCancel", resources, "Cancel.text", "Cancel.toolTipText",
                this::cancelActionPerformed));
        panel.add(new MMButton("btnRefresh", resources, "RefreshDirectory.text",
                "RefreshDirectory.toolTipText", evt -> getChooser().refreshDirectory()));

        return panel;
    }

    /**
     * This adds initializing the override to null and defaulting to the Units category over root to
     * finalizing the initialization of the AbstractIconChooserDialog
     */
    @Override
    protected void finalizeInitialization() {
        super.finalizeInitialization();
        setOverride(null);

        // Default to the Units folder when no icon is selected, as that's the primary location for
        // Unit Icons
        if ((getChooser().getTreeCategories() != null) && ((getChooser().getOriginalIcon() == null)
                || (getChooser().getOriginalIcon().getFilename() == null))) {
            final DefaultMutableTreeNode root = (DefaultMutableTreeNode) getChooser().getTreeCategories()
                    .getModel().getRoot();
            for (final Enumeration<?> children = root.children(); children.hasMoreElements(); ) {
                final DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                if ("Units".equals(child.getUserObject())) {
                    getChooser().getTreeCategories().setSelectionPath(new TreePath(child.getPath()));
                    break;
                }
            }
        }
    }
    //endregion Initialization

    //region Button Actions
    @Override
    protected void okButtonActionPerformed(final @Nullable ActionEvent evt) {
        okAction();
        setResult(((getChooser().getSelectedItem() == null) && (getOverride() == null))
                ? DialogResult.CANCELLED : DialogResult.CONFIRMED);
        setVisible(false);
    }

    /**
     * Performs the action where the selected unit icon will not display an image.
     * @param evt the triggering event
     */
    private void noneActionPerformed(final ActionEvent evt) {
        setOverride(new UnitIcon(null, null));
        okButtonActionPerformed(evt);
    }
    //endregion Button Actions
}
