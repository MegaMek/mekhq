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

import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.preferences.JTabbedPanePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.icons.ForcePieceIcon;
import mekhq.campaign.icons.LayeredForceIcon;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.AbstractMHQPanel;

/**
 * This panel is used to create, display, and export a LayeredForceIcon based on
 * a tabbed pane
 * containing a ForcePieceIconChooser for every potential LayeredForceIconLayer
 * layer.
 */
public class LayeredForceIconCreationPanel extends AbstractMHQPanel {
    private static final MMLogger logger = MMLogger.create(LayeredForceIconCreationPanel.class);

    // region Variable Declarations
    private LayeredForceIcon forceIcon;
    private final boolean includeRefreshButton;

    private JTabbedPane tabbedPane;
    private Map<LayeredForceIconLayer, ForcePieceIconChooser> choosers;
    private JLabel lblIcon;
    // endregion Variable Declarations

    // region Constructors
    public LayeredForceIconCreationPanel(final JFrame frame,
            final @Nullable StandardForceIcon forceIcon,
            final boolean includeRefreshButton) {
        super(frame, "LayeredForceIconCreationPanel", new GridBagLayout());
        setForceIcon((forceIcon instanceof LayeredForceIcon)
                ? ((LayeredForceIcon) forceIcon).clone()
                : new LayeredForceIcon());
        this.includeRefreshButton = includeRefreshButton;
        initialize();
    }
    // endregion Constructors

    // region Getters/Setters
    public LayeredForceIcon getForceIcon() {
        return forceIcon;
    }

    public void setForceIcon(final LayeredForceIcon forceIcon) {
        this.forceIcon = forceIcon;
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

    public Map<LayeredForceIconLayer, ForcePieceIconChooser> getChoosers() {
        return choosers;
    }

    public void setChoosers(final Map<LayeredForceIconLayer, ForcePieceIconChooser> choosers) {
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
        for (final LayeredForceIconLayer layer : LayeredForceIconLayer.values()) {
            final ForcePieceIconChooser chooser = new ForcePieceIconChooser(getFrame(), layer, getForceIcon());
            chooser.getImageList().addListSelectionListener(evt -> {
                getForceIcon().getPieces().put(layer, chooser.getSelectedItems());
                getLblIcon().setIcon(getForceIcon().getImageIcon());
            });
            getChoosers().put(layer, chooser);
            getTabbedPane().addTab(layer.toString(), chooser);
        }
        add(getTabbedPane(), gbc);

        setLblIcon(new JLabel(getForceIcon().getImageIcon()));
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
                "LayeredForceIconCreationPanel.btnExport.toolTipText", evt -> exportAction()), gbc);

        if (isIncludeRefreshButton()) {
            gbc.gridx++;
            add(new MMButton("btnRefreshDirectory", resources, "RefreshDirectory.text",
                    "RefreshDirectory.toolTipText", evt -> refreshDirectory(true)), gbc);
        }

        try {
            setPreferences();
        } catch (Exception ex) {
            logger.error(
                    "Error setting the Layered Force Icon Creation Panel's preferences. Keeping the created panel, but this is likely to cause some oddities.",
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
     * Creates a new LayeredForceIcon to use as both the current and original icon
     */
    public void newIcon() {
        final LayeredForceIcon icon = new LayeredForceIcon();
        setForceIcon(icon);
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
     * Exports the current LayeredForceIcon to a .png file
     */
    private void exportAction() {
        File file = FileDialogs.exportLayeredForceIcon(getFrame()).orElse(null);
        if (file == null) {
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".png")) {
            path += ".png";
            file = new File(path);
        }

        createForceIcon();

        try {
            final BufferedImage image = (BufferedImage) getForceIcon().getImage();
            ImageIO.write(image, "png", file);
        } catch (Exception ex) {
            logger.error("", ex);
        }
    }

    /**
     * @param performDirectoryRefresh whether to perform the actual refresh of the
     *                                Force Icons
     *                                directory or just handle the changes required
     *                                for an already
     *                                refreshed directory.
     */
    public void refreshDirectory(final boolean performDirectoryRefresh) {
        if (performDirectoryRefresh) {
            MHQStaticDirectoryManager.refreshForceIcons();
        }

        createForceIcon();
        for (final ForcePieceIconChooser chooser : getChoosers().values()) {
            chooser.setOriginalIcon(getForceIcon());
            chooser.refreshTree();
        }
    }
    // endregion Button Actions

    /**
     * Creates a force icon based on the individual selections for each piece
     * chooser, and then
     * sets the force icon stored in this panel to that new icon.
     * 
     * @return the newly created force icon
     */
    public LayeredForceIcon createForceIcon() {
        final LayeredForceIcon icon = new LayeredForceIcon();
        for (final Map.Entry<LayeredForceIconLayer, ForcePieceIconChooser> entry : getChoosers().entrySet()) {
            final List<ForcePieceIcon> pieces = entry.getValue().getSelectedItems();
            if (!pieces.isEmpty()) {
                icon.getPieces().put(entry.getKey(), pieces);
            }
        }
        setForceIcon(icon);
        return icon;
    }
}
