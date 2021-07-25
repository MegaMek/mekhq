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
package mekhq.gui.panes;

import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.preferences.JTabbedPanePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.icons.LayeredForceIcon;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.AbstractMHQScrollPane;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.panels.ForcePieceIconChooser;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LayeredForceIconCreationPane extends AbstractMHQScrollPane {
    //region Variable Declarations
    private LayeredForceIcon forceIcon;
    private final boolean includeButtons;

    private JTabbedPane tabbedPane;
    private Map<LayeredForceIconLayer, ForcePieceIconChooser> choosers;
    private JLabel lblIcon;
    //endregion Variable Declarations

    //region Constructors
    public LayeredForceIconCreationPane(final JFrame frame,
                                        final @Nullable StandardForceIcon forceIcon,
                                        final boolean includeButtons) {
        super(frame, "LayeredForceIconCreationPane");
        setForceIcon((forceIcon instanceof LayeredForceIcon)
                ? ((LayeredForceIcon) forceIcon).clone() : new LayeredForceIcon());
        this.includeButtons = includeButtons;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public LayeredForceIcon getForceIcon() {
        return forceIcon;
    }

    public void setForceIcon(final LayeredForceIcon forceIcon) {
        this.forceIcon = forceIcon;
    }

    public boolean isIncludeButtons() {
        return includeButtons;
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
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected void initialize() {
        final JPanel panel = new JScrollablePanel(new GridBagLayout());
        panel.setName("piecesPanel");

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        setTabbedPane(new JTabbedPane());
        getTabbedPane().setName("piecesTabbedPane");
        setChoosers(new HashMap<>());
        for (final LayeredForceIconLayer layer : LayeredForceIconLayer.values()) {
            getChoosers().put(layer, new ForcePieceIconChooser(layer, getForceIcon()));
            getTabbedPane().addTab(layer.toString(), getChoosers().get(layer));
        }
        panel.add(getTabbedPane(), gbc);

        setLblIcon(new JLabel(getForceIcon().getImageIcon()));
        getLblIcon().setToolTipText(resources.getString("lblIcon.toolTipText"));
        getLblIcon().setName("lblIcon");
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.SOUTH;
        panel.add(getLblIcon(), gbc);

        if (isIncludeButtons()) {
            gbc.gridy++;
            gbc.gridwidth = 1;
            panel.add(new MMButton("btnExport", resources, "Export.text",
                    "LayeredForceIconCreationPane.btnExport.toolTipText", evt -> exportAction()), gbc);

            gbc.gridx++;
            panel.add(new MMButton("btnRefreshDirectory", resources, "RefreshDirectory.text",
                    "RefreshDirectory.toolTipText", evt -> refreshDirectory()), gbc);
        }

        setViewportView(panel);
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) {
        super.setCustomPreferences(preferences);
        preferences.manage(new JTabbedPanePreference(getTabbedPane()));
    }
    //endregion Initialization

    //region Button Actions
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

        try {
            final BufferedImage image = (BufferedImage) getForceIcon().getImage();
            ImageIO.write(image, "png", file);
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }
    }

    public void refreshDirectory() {
        // TODO : Implement me
    }
    //endregion Button Actions
}
