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
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * A compact two-column form panel for campaign option pages.
 */
public class CampaignOptionsFormPanel extends JPanel {
    private static final int LABEL_RIGHT_PADDING = 12;
    private static final int ROW_VERTICAL_PADDING = 5;
    private static final int CHECK_BOX_COLUMN_GAP = 32;
    private static final int DEFAULT_CONTROL_WIDTH = 220;

    private final int labelWidth;
    private final int controlWidth;
    private int row;

    public CampaignOptionsFormPanel(String name) {
        this(name, 0, DEFAULT_CONTROL_WIDTH);
    }

    public CampaignOptionsFormPanel(String name, int labelWidth) {
        this(name, labelWidth, DEFAULT_CONTROL_WIDTH);
    }

    public CampaignOptionsFormPanel(String name, int labelWidth, int controlWidth) {
        this.labelWidth = labelWidth;
        this.controlWidth = controlWidth;
        setName("pnl" + name);
        setOpaque(false);
        setLayout(new GridBagLayout());
    }

    public void addCheckBox(JCheckBox checkBox) {
        alignCheckBoxToStart(checkBox);

        int currentRow = row++;
        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = currentRow;
        layout.gridwidth = 2;
        layout.weightx = 0.0;
        layout.anchor = GridBagConstraints.WEST;
        layout.fill = GridBagConstraints.NONE;
        layout.insets = new Insets(ROW_VERTICAL_PADDING, 0, ROW_VERTICAL_PADDING, 0);
        add(checkBox, layout);
        addTrailingFiller(currentRow, 2);
    }

    public void addCheckBoxGrid(int columnCount, JCheckBox... checkBoxes) {
        if (columnCount <= 1) {
            for (JCheckBox checkBox : checkBoxes) {
                addCheckBox(checkBox);
            }
            return;
        }

        int firstRow = row;
        for (int index = 0; index < checkBoxes.length; index++) {
            int column = index % columnCount;
            alignCheckBoxToStart(checkBoxes[index]);

            GridBagConstraints layout = new GridBagConstraints();
            layout.gridx = column;
            layout.gridy = firstRow + index / columnCount;
            layout.weightx = 0.0;
            layout.anchor = GridBagConstraints.WEST;
            layout.fill = GridBagConstraints.NONE;
            layout.insets = new Insets(ROW_VERTICAL_PADDING,
                    0,
                    ROW_VERTICAL_PADDING,
                    getCheckBoxGridRightPadding(column, columnCount));
            setMinimumCheckBoxGridWidth(checkBoxes[index], column);
            add(checkBoxes[index], layout);
        }

        int rowCount = (checkBoxes.length + columnCount - 1) / columnCount;
        for (int rowOffset = 0; rowOffset < rowCount; rowOffset++) {
            addTrailingFiller(firstRow + rowOffset, columnCount);
        }

        row += rowCount;
    }

    private int getCheckBoxGridRightPadding(int column, int columnCount) {
        if (column == columnCount - 1) {
            return 0;
        }
        return labelWidth > 0 ? LABEL_RIGHT_PADDING : CHECK_BOX_COLUMN_GAP;
    }

    private void setMinimumCheckBoxGridWidth(JCheckBox checkBox, int column) {
        if (labelWidth <= 0 || column != 0) {
            return;
        }

        setMinimumWidth(checkBox, labelWidth);
    }

    private void alignCheckBoxToStart(JCheckBox checkBox) {
        checkBox.setHorizontalAlignment(SwingConstants.LEADING);
    }

    public void addRow(JComponent label, JComponent control) {
        setMinimumLabelWidth(label);
        setMinimumControlWidth(control);

        int currentRow = row++;
        GridBagConstraints labelLayout = new GridBagConstraints();
        labelLayout.gridx = 0;
        labelLayout.gridy = currentRow;
        labelLayout.weightx = 0.0;
        labelLayout.anchor = GridBagConstraints.WEST;
        labelLayout.fill = GridBagConstraints.NONE;
        labelLayout.insets = new Insets(ROW_VERTICAL_PADDING, 0, ROW_VERTICAL_PADDING, LABEL_RIGHT_PADDING);
        add(label, labelLayout);

        GridBagConstraints controlLayout = new GridBagConstraints();
        controlLayout.gridx = 1;
        controlLayout.gridy = currentRow;
        controlLayout.weightx = 0.0;
        controlLayout.anchor = GridBagConstraints.WEST;
        controlLayout.fill = GridBagConstraints.NONE;
        controlLayout.insets = new Insets(ROW_VERTICAL_PADDING, 0, ROW_VERTICAL_PADDING, 0);
        add(control, controlLayout);
        addTrailingFiller(currentRow, 2);
    }

    private void addTrailingFiller(int rowIndex, int columnIndex) {
        JPanel filler = new JPanel();
        filler.setOpaque(false);

        GridBagConstraints fillerLayout = new GridBagConstraints();
        fillerLayout.gridx = columnIndex;
        fillerLayout.gridy = rowIndex;
        fillerLayout.weightx = 1.0;
        fillerLayout.fill = GridBagConstraints.HORIZONTAL;
        add(filler, fillerLayout);
    }

    private void setMinimumControlWidth(JComponent control) {
        setMinimumWidth(control, controlWidth);
    }

    private void setMinimumLabelWidth(JComponent label) {
        if (labelWidth <= 0) {
            return;
        }

        setMinimumWidth(label, labelWidth);
    }

    private void setMinimumWidth(JComponent component, int minimumWidth) {
        Dimension preferredSize = component.getPreferredSize();
        int width = Math.max(preferredSize.width, minimumWidth);
        Dimension adjustedSize = new Dimension(width, preferredSize.height);
        component.setPreferredSize(adjustedSize);
        component.setMinimumSize(adjustedSize);
    }
}