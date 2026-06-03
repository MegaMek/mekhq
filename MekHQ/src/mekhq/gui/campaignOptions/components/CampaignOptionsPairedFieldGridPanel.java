/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.components;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * A dense field grid for sections with many label/control pairs.
 */
public class CampaignOptionsPairedFieldGridPanel extends JPanel {
    private static final int LABEL_CONTROL_GAP = 8;
    private static final int ROW_VERTICAL_GAP = 5;

    private final int firstPairWidth;
    private final int followingPairWidth;
    private final int controlWidth;
    private final int columnCount;

    public CampaignOptionsPairedFieldGridPanel(String name, int firstPairWidth, int followingPairWidth,
          int controlWidth, int columnCount) {
        if (columnCount < 1) {
            throw new IllegalArgumentException("Paired field grids require at least one column.");
        }

        this.firstPairWidth = firstPairWidth;
        this.followingPairWidth = followingPairWidth;
        this.controlWidth = controlWidth;
        this.columnCount = columnCount;

        setName("pnl" + name);
        setOpaque(false);
        setLayout(new GridBagLayout());
    }

    public void addPairs(JComponent[] labels, JComponent[] controls) {
        if (labels.length != controls.length) {
            throw new IllegalArgumentException("Paired field grids require one control per label.");
        }

        for (int index = 0; index < labels.length; index++) {
            addPair(labels[index], controls[index], index);
        }

        addTrailingFiller();
    }

    private void addPair(JComponent label, JComponent control, int index) {
        int column = index % columnCount;
        int row = index / columnCount;
        JPanel pairPanel = createPairPanel(label, control, column);

        GridBagConstraints pairLayout = new GridBagConstraints();
        pairLayout.gridx = column;
        pairLayout.gridy = row;
        pairLayout.anchor = GridBagConstraints.WEST;
        pairLayout.fill = GridBagConstraints.NONE;
        pairLayout.insets = new Insets(ROW_VERTICAL_GAP, 0, ROW_VERTICAL_GAP, 0);
        add(pairPanel, pairLayout);
    }

    private JPanel createPairPanel(JComponent label, JComponent control, int column) {
        JPanel pairPanel = new JPanel(new GridBagLayout());
        pairPanel.setOpaque(false);

        setPreferredWidth(control, controlWidth);
        alignLabel(label);

        GridBagConstraints labelLayout = new GridBagConstraints();
        labelLayout.gridx = 0;
        labelLayout.gridy = 0;
        labelLayout.weightx = 1.0;
        labelLayout.anchor = GridBagConstraints.WEST;
        labelLayout.fill = GridBagConstraints.HORIZONTAL;
        labelLayout.insets = new Insets(0, 0, 0, LABEL_CONTROL_GAP);
        pairPanel.add(label, labelLayout);

        GridBagConstraints controlLayout = new GridBagConstraints();
        controlLayout.gridx = 1;
        controlLayout.gridy = 0;
        controlLayout.anchor = GridBagConstraints.EAST;
        controlLayout.fill = GridBagConstraints.NONE;
        controlLayout.insets = new Insets(0, 0, 0, column == 0 ? LABEL_CONTROL_GAP : 0);
        pairPanel.add(control, controlLayout);

        setPreferredWidth(pairPanel, getPairWidth(column));

        return pairPanel;
    }

    private int getPairWidth(int column) {
        return column == 0 ? firstPairWidth : followingPairWidth;
    }

    private void addTrailingFiller() {
        GridBagConstraints fillerLayout = new GridBagConstraints();
        fillerLayout.gridx = columnCount;
        fillerLayout.gridy = 0;
        fillerLayout.weightx = 1.0;
        fillerLayout.fill = GridBagConstraints.HORIZONTAL;
        add(createTransparentSpacer(), fillerLayout);
    }

    private void alignLabel(JComponent component) {
        if (component instanceof JLabel label) {
            label.setHorizontalAlignment(SwingConstants.LEADING);
        }
    }

    private void setPreferredWidth(JComponent component, int preferredWidth) {
        Dimension preferredSize = component.getPreferredSize();
        Dimension adjustedSize = new Dimension(preferredWidth, preferredSize.height);
        component.setPreferredSize(adjustedSize);
        component.setMinimumSize(adjustedSize);
    }

    private JComponent createTransparentSpacer() {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(1, 1));
        spacer.setMinimumSize(new Dimension(1, 1));
        return spacer;
    }
}