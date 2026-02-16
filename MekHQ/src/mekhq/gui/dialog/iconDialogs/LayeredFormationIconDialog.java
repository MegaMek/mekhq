/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.iconDialogs;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.preferences.JSplitPanePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.icons.LayeredFormationIcon;
import mekhq.campaign.icons.StandardFormationIcon;
import mekhq.campaign.icons.UnitIcon;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.panels.LayeredFormationIconCreationPanel;
import mekhq.gui.panels.StandardFormationIconChooser;

/**
 * A LayeredFormationIconDialog is used to select a Formation Icon, which may be either a LayeredFormationIcon or a
 * StandardFormationIcon. It allows one to swap a force between the two types without issue, and handles having both types
 * open at the same time.
 *
 * <p>Known as {@code LayeredForceIconDialog} prior to 0.50.12</p>
 *
 * @since 0.50.12
 */
public class LayeredFormationIconDialog extends AbstractMHQButtonDialog {
    private static final MMLogger LOGGER = MMLogger.create(LayeredFormationIconDialog.class);

    // region Variable Declarations
    private StandardFormationIcon originalFormationIcon;

    private JTabbedPane tabbedPane;
    private StandardFormationIconChooser standardFormationIconChooser;
    private LayeredFormationIconCreationPanel layeredFormationIconCreationPanel;
    // endregion Variable Declarations

    // region Constructors
    public LayeredFormationIconDialog(final JFrame parent, final @Nullable StandardFormationIcon originalFormationIcon) {
        super(parent, "LayeredFormationIconDialog", "LayeredFormationIconDialog.title");
        if (originalFormationIcon instanceof UnitIcon) {
            LOGGER.error(
                  "This dialog was never designed for Unit Icon selection. Creating a standard formation icon based on it, using the base null protections that provides.");
            setOriginalFormationIcon(
                  new StandardFormationIcon(originalFormationIcon.getCategory(), originalFormationIcon.getFilename()));
        } else {
            setOriginalFormationIcon(originalFormationIcon);
        }
        initialize();
    }
    // endregion Constructors

    // region Getters/Setters
    public @Nullable StandardFormationIcon getOriginalFormationIcon() {
        return originalFormationIcon;
    }

    public void setOriginalFormationIcon(final @Nullable StandardFormationIcon originalFormationIcon) {
        this.originalFormationIcon = originalFormationIcon;
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public void setTabbedPane(final JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
    }

    public StandardFormationIconChooser getStandardFormationIconChooser() {
        return standardFormationIconChooser;
    }

    public void setStandardFormationIconChooser(final StandardFormationIconChooser standardFormationIconChooser) {
        this.standardFormationIconChooser = standardFormationIconChooser;
    }

    public LayeredFormationIconCreationPanel getLayeredFormationIconCreationPanel() {
        return layeredFormationIconCreationPanel;
    }

    public void setLayeredFormationIconCreationPanel(final LayeredFormationIconCreationPanel layeredFormationIconCreationPanel) {
        this.layeredFormationIconCreationPanel = layeredFormationIconCreationPanel;
    }

    /**
     * @return the selected icon for this dialog, or null if no icon is selected
     */
    public @Nullable StandardFormationIcon getSelectedItem() {
        if (getResult().isCancelled()) {
            return getOriginalFormationIcon();
        } else if (getStandardFormationIconChooser().equals(getTabbedPane().getSelectedComponent())) {
            return getStandardFormationIconChooser().getSelectedItem();
        } else {
            return getLayeredFormationIconCreationPanel().createFormationIcon();
        }
    }
    // endregion Getters/Setters

    // region Initialization
    @Override
    protected Container createCenterPane() {
        setTabbedPane(new JTabbedPane());
        getTabbedPane().setName("iconSelectionPane");

        setStandardFormationIconChooser(new StandardFormationIconChooser(getFrame(), getOriginalFormationIcon()));
        getTabbedPane().addTab(resources.getString("StandardIconTab.title"), getStandardFormationIconChooser());

        setLayeredFormationIconCreationPanel(new LayeredFormationIconCreationPanel(getFrame(), getOriginalFormationIcon(), false));
        getTabbedPane().addTab(resources.getString("LayeredIconTab.title"), getLayeredFormationIconCreationPanel());
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

        if (getOriginalFormationIcon() instanceof LayeredFormationIcon) {
            getTabbedPane().setSelectedComponent(getLayeredFormationIconCreationPanel());
        }
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) throws Exception {
        super.setCustomPreferences(preferences);
        preferences.manage(new JSplitPanePreference(getStandardFormationIconChooser().getSplitPane()));
    }
    // endregion Initialization

    // region Button Actions

    /**
     * This does a complete directory refresh, starting with the StandardFormationIconChooser (which refreshes the Force
     * Icon directory and implements it), and then refreshing the implementations under the
     * LayeredFormationIconCreationPanel without actually refreshing the Formation Icon Directory.
     *
     * @param evt the triggering event
     */
    public void refreshDirectory(final @Nullable ActionEvent evt) {
        getStandardFormationIconChooser().refreshDirectory();
        getLayeredFormationIconCreationPanel().refreshDirectory(false);
    }
    // endregion Button Actions
}
