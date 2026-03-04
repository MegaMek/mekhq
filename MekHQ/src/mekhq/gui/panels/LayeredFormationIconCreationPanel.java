/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.panels;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.preferences.JTabbedPanePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.icons.FormationPieceIcon;
import mekhq.campaign.icons.LayeredFormationIcon;
import mekhq.campaign.icons.StandardFormationIcon;
import mekhq.campaign.icons.enums.LayeredFormationIconLayer;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.AbstractMHQPanel;

/**
 * This panel is used to create, display, and export a LayeredFormationIcon based on a tabbed pane containing a
 * ForcePieceIconChooser for every potential LayeredFormationIconLayer layer.
 *
 * <p>Known as {@code LayeredForceIconCreationPanel} prior to 0.50.12</p>
 *
 * @since 0.50.12
 */
public class LayeredFormationIconCreationPanel extends AbstractMHQPanel {
    private static final MMLogger LOGGER = MMLogger.create(LayeredFormationIconCreationPanel.class);

    // region Variable Declarations
    private LayeredFormationIcon formationIcon;
    private final boolean includeRefreshButton;

    private JTabbedPane tabbedPane;
    private Map<LayeredFormationIconLayer, ForcePieceIconChooser> choosers;
    private JLabel lblIcon;
    // endregion Variable Declarations

    // region Constructors
    public LayeredFormationIconCreationPanel(final JFrame frame,
          final @Nullable StandardFormationIcon formationIcon,
          final boolean includeRefreshButton) {
        super(frame, "LayeredFormationIconCreationPanel", new GridBagLayout());
        setFormationIcon((formationIcon instanceof LayeredFormationIcon)
                           ? ((LayeredFormationIcon) formationIcon).clone()
                           : new LayeredFormationIcon());
        this.includeRefreshButton = includeRefreshButton;
        initialize();
    }
    // endregion Constructors

    // region Getters/Setters
    public LayeredFormationIcon getFormationIcon() {
        return formationIcon;
    }

    public void setFormationIcon(final LayeredFormationIcon formationIcon) {
        this.formationIcon = formationIcon;
    }

    public boolean isIncludeRefreshButton() {
        return includeRefreshButton;
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public void setTabbedPane(final JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
    }

    public Map<LayeredFormationIconLayer, ForcePieceIconChooser> getChoosers() {
        return choosers;
    }

    public void setChoosers(final Map<LayeredFormationIconLayer, ForcePieceIconChooser> choosers) {
        this.choosers = choosers;
    }

    public JLabel getLblIcon() {
        return lblIcon;
    }

    public void setLblIcon(final JLabel lblIcon) {
        this.lblIcon = lblIcon;
    }
    // endregion Getters/Setters

    // region Initialization
    @Override
    protected void initialize() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridwidth = isIncludeRefreshButton() ? 4 : 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        setTabbedPane(new JTabbedPane());
        getTabbedPane().setName("piecesTabbedPane");
        getTabbedPane().setPreferredSize(new Dimension(700, 1100));
        setChoosers(new HashMap<>());
        for (final LayeredFormationIconLayer layer : LayeredFormationIconLayer.values()) {
            final ForcePieceIconChooser chooser = new ForcePieceIconChooser(getFrame(), layer, getFormationIcon());
            chooser.getImageList().addListSelectionListener(evt -> {
                getFormationIcon().getPieces().put(layer, chooser.getSelectedItems());
                getLblIcon().setIcon(getFormationIcon().getImageIcon());
            });
            getChoosers().put(layer, chooser);
            getTabbedPane().addTab(layer.toString(), chooser);
        }
        add(getTabbedPane(), gbc);

        setLblIcon(new JLabel(getFormationIcon().getImageIcon()));
        getLblIcon().setName("lblIcon");
        getLblIcon().getAccessibleContext().setAccessibleName(resources.getString("lblIcon.accessibleName"));
        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTH;
        add(getLblIcon(), gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        add(new MMButton("btnNewIcon", resources, "btnNewIcon.text",
              "btnNewIcon.toolTipText", evt -> newIcon()), gbc);

        gbc.gridx++;
        add(new MMButton("btnClearCurrentTab", resources, "btnClearCurrentTab.text",
              "btnClearCurrentTab.toolTipText", evt -> clearSelectedTab()), gbc);

        gbc.gridx++;
        add(new MMButton("btnExport", resources, "Export.text",
              "LayeredFormationIconCreationPanel.btnExport.toolTipText", evt -> exportAction()), gbc);

        if (isIncludeRefreshButton()) {
            gbc.gridx++;
            add(new MMButton("btnRefreshDirectory", resources, "RefreshDirectory.text",
                  "RefreshDirectory.toolTipText", evt -> refreshDirectory(true)), gbc);
        }

        try {
            setPreferences();
        } catch (Exception ex) {
            LOGGER.error(
                  "Error setting the Layered Formation Icon Creation Panel's preferences. Keeping the created panel, but this is likely to cause some oddities.",
                  ex);
        }
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) throws Exception {
        super.setCustomPreferences(preferences);
        preferences.manage(new JTabbedPanePreference(getTabbedPane()));
    }
    // endregion Initialization

    // region Button Actions

    /**
     * Creates a new LayeredFormationIcon to use as both the current and original icon
     */
    public void newIcon() {
        final LayeredFormationIcon icon = new LayeredFormationIcon();
        setFormationIcon(icon);
        for (final ForcePieceIconChooser chooser : getChoosers().values()) {
            chooser.setOriginalIcon(icon);
            chooser.setSelection(icon);
        }
    }

    /**
     * Clears any selected items on the currently selected tab
     */
    public void clearSelectedTab() {
        ((ForcePieceIconChooser) getTabbedPane().getSelectedComponent()).clearSelectedItems();
    }

    /**
     * Exports the current LayeredFormationIcon to a .png file
     */
    private void exportAction() {
        File file = FileDialogs.exportLayeredFormationIcon(getFrame()).orElse(null);
        if (file == null) {
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".png")) {
            path += ".png";
            file = new File(path);
        }

        createFormationIcon();

        try {
            final BufferedImage image = (BufferedImage) getFormationIcon().getImage();
            ImageIO.write(image, "png", file);
        } catch (Exception ex) {
            LOGGER.error("", ex);
        }
    }

    /**
     * @param performDirectoryRefresh whether to perform the actual refresh of the Formation Icons directory or just handle
     *                                the changes required for an already refreshed directory.
     */
    public void refreshDirectory(final boolean performDirectoryRefresh) {
        if (performDirectoryRefresh) {
            MHQStaticDirectoryManager.refreshFormationIcons();
        }

        createFormationIcon();
        for (final ForcePieceIconChooser chooser : getChoosers().values()) {
            chooser.setOriginalIcon(getFormationIcon());
            chooser.refreshTree();
        }
    }
    // endregion Button Actions

    /**
     * Creates a formation icon based on the individual selections for each piece chooser, and then sets the formation icon
     * stored in this panel to that new icon.
     *
     * @return the newly created formation icon
     */
    public LayeredFormationIcon createFormationIcon() {
        final LayeredFormationIcon icon = new LayeredFormationIcon();
        for (final Map.Entry<LayeredFormationIconLayer, ForcePieceIconChooser> entry : getChoosers().entrySet()) {
            final List<FormationPieceIcon> pieces = entry.getValue().getSelectedItems();
            if (!pieces.isEmpty()) {
                icon.getPieces().put(entry.getKey(), pieces);
            }
        }
        setFormationIcon(icon);
        return icon;
    }
}
