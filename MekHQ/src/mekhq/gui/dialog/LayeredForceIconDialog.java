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
package mekhq.gui.dialog;

import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.preferences.JSplitPanePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.icons.UnitIcon;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.panels.StandardForceIconChooser;
import mekhq.gui.panes.LayeredForceIconCreationPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LayeredForceIconDialog extends AbstractMHQButtonDialog {
    //region Variable Declarations
    private StandardForceIcon originalForceIcon;

    private JTabbedPane tabbedPane;
    private StandardForceIconChooser standardForceIconChooser;
    private LayeredForceIconCreationPane layeredForceIconCreationPane;
    //endregion Variable Declarations

    //region Constructors
    public LayeredForceIconDialog(final JFrame parent, final @Nullable StandardForceIcon originalForceIcon) {
        super(parent, "LayeredForceIconDialog", "LayeredForceIconDialog.title");
        if (originalForceIcon instanceof UnitIcon) {
            MekHQ.getLogger().error("This dialog was never designed for Unit Icon selection. Creating a standard force icon based on it, with the base null protections that provides.");
            setOriginalForceIcon(new StandardForceIcon(originalForceIcon.getCategory(), originalForceIcon.getFilename()));
        } else {
            setOriginalForceIcon(originalForceIcon);
        }
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public @Nullable StandardForceIcon getOriginalForceIcon() {
        return originalForceIcon;
    }

    public void setOriginalForceIcon(final @Nullable StandardForceIcon originalForceIcon) {
        this.originalForceIcon = originalForceIcon;
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public void setTabbedPane(final JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
    }

    public StandardForceIconChooser getStandardForceIconChooser() {
        return standardForceIconChooser;
    }

    public void setStandardForceIconChooser(final StandardForceIconChooser standardForceIconChooser) {
        this.standardForceIconChooser = standardForceIconChooser;
    }

    public LayeredForceIconCreationPane getLayeredForceIconCreationPane() {
        return layeredForceIconCreationPane;
    }

    public void setLayeredForceIconCreationPane(final LayeredForceIconCreationPane layeredForceIconCreationPane) {
        this.layeredForceIconCreationPane = layeredForceIconCreationPane;
    }

    public @Nullable StandardForceIcon getSelectedItem() {
        if (getResult().isCancelled()) {
            return getOriginalForceIcon();
        } else if (getStandardForceIconChooser().equals(getTabbedPane().getSelectedComponent())) {
            return getStandardForceIconChooser().getSelectedItem();
        } else {
            return getLayeredForceIconCreationPane().getForceIcon();
        }
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        setTabbedPane(new JTabbedPane());
        getTabbedPane().setName("iconSelectionPane");

        setStandardForceIconChooser(new StandardForceIconChooser(getOriginalForceIcon()));
        getTabbedPane().addTab(resources.getString("StandardIconTab.title"), getStandardForceIconChooser());

        setLayeredForceIconCreationPane(new LayeredForceIconCreationPane(getFrame(), getOriginalForceIcon(), false));
        getTabbedPane().addTab(resources.getString("LayeredIconTab.title"), getLayeredForceIconCreationPane());
        return getTabbedPane();
    }

    @Override
    protected JPanel createButtonPanel() {
        final JPanel panel = new JPanel(new GridLayout(1, 3));
        panel.setName("buttonPanel");

        panel.add(new MMButton("btnOk", resources, "Ok.text", "Ok.toolTipText",
                this::okButtonActionPerformed));
        panel.add(new MMButton("btnCancel", resources, "Cancel.text", "Cancel.toolTipText",
                this::cancelActionPerformed));
        panel.add(new MMButton("btnRefresh", resources, "RefreshDirectory.text",
                "RefreshDirectory.toolTipText", this::refreshDirectory));

        return panel;
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) {
        super.setCustomPreferences(preferences);
        preferences.manage(new JSplitPanePreference(getStandardForceIconChooser().getSplitPane()));
    }
    //endregion Initialization

    public void refreshDirectory(final @Nullable ActionEvent evt) {
        getStandardForceIconChooser().refreshDirectory();
        getLayeredForceIconCreationPane().refreshDirectory();
    }
}
