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
package mekhq.gui.dialog.iconDialogs;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.preferences.JSplitPanePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.icons.LayeredForceIcon;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.icons.UnitIcon;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.panels.LayeredForceIconCreationPanel;
import mekhq.gui.panels.StandardForceIconChooser;

/**
 * A LayeredForceIconDialog is used to select a Force Icon, which may be either
 * a LayeredForceIcon
 * or a StandardForceIcon. It allows one to swap a force between the two types
 * without issue, and
 * handles having both types open at the same time.
 */
public class LayeredForceIconDialog extends AbstractMHQButtonDialog {
    private static final MMLogger logger = MMLogger.create(LayeredForceIconDialog.class);

    // region Variable Declarations
    private StandardForceIcon originalForceIcon;

    private JTabbedPane tabbedPane;
    private StandardForceIconChooser standardForceIconChooser;
    private LayeredForceIconCreationPanel layeredForceIconCreationPanel;
    // endregion Variable Declarations

    // region Constructors
    public LayeredForceIconDialog(final JFrame parent, final @Nullable StandardForceIcon originalForceIcon) {
        super(parent, "LayeredForceIconDialog", "LayeredForceIconDialog.title");
        if (originalForceIcon instanceof UnitIcon) {
            logger.error(
                    "This dialog was never designed for Unit Icon selection. Creating a standard force icon based on it, using the base null protections that provides.");
            setOriginalForceIcon(
                    new StandardForceIcon(originalForceIcon.getCategory(), originalForceIcon.getFilename()));
        } else {
            setOriginalForceIcon(originalForceIcon);
        }
        initialize();
    }
    // endregion Constructors

    // region Getters/Setters
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

    public LayeredForceIconCreationPanel getLayeredForceIconCreationPanel() {
        return layeredForceIconCreationPanel;
    }

    public void setLayeredForceIconCreationPanel(final LayeredForceIconCreationPanel layeredForceIconCreationPanel) {
        this.layeredForceIconCreationPanel = layeredForceIconCreationPanel;
    }

    /**
     * @return the selected icon for this dialog, or null if no icon is selected
     */
    public @Nullable StandardForceIcon getSelectedItem() {
        if (getResult().isCancelled()) {
            return getOriginalForceIcon();
        } else if (getStandardForceIconChooser().equals(getTabbedPane().getSelectedComponent())) {
            return getStandardForceIconChooser().getSelectedItem();
        } else {
            return getLayeredForceIconCreationPanel().createForceIcon();
        }
    }
    // endregion Getters/Setters

    // region Initialization
    @Override
    protected Container createCenterPane() {
        setTabbedPane(new JTabbedPane());
        getTabbedPane().setName("iconSelectionPane");

        setStandardForceIconChooser(new StandardForceIconChooser(getFrame(), getOriginalForceIcon()));
        getTabbedPane().addTab(resources.getString("StandardIconTab.title"), getStandardForceIconChooser());

        setLayeredForceIconCreationPanel(new LayeredForceIconCreationPanel(getFrame(), getOriginalForceIcon(), false));
        getTabbedPane().addTab(resources.getString("LayeredIconTab.title"), getLayeredForceIconCreationPanel());
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
    protected void finalizeInitialization() throws Exception {
        super.finalizeInitialization();

        if (getOriginalForceIcon() instanceof LayeredForceIcon) {
            getTabbedPane().setSelectedComponent(getLayeredForceIconCreationPanel());
        }
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) throws Exception {
        super.setCustomPreferences(preferences);
        preferences.manage(new JSplitPanePreference(getStandardForceIconChooser().getSplitPane()));
    }
    // endregion Initialization

    // region Button Actions
    /**
     * This does a complete directory refresh, starting with the
     * StandardForceIconChooser (which
     * refreshes the Force Icon directory and implements it), and then refreshing
     * the
     * implementations under the LayeredForceIconCreationPanel without actually
     * refreshing the Force
     * Icon Directory.
     *
     * @param evt the triggering event
     */
    public void refreshDirectory(final @Nullable ActionEvent evt) {
        getStandardForceIconChooser().refreshDirectory();
        getLayeredForceIconCreationPanel().refreshDirectory(false);
    }
    // endregion Button Actions
}
