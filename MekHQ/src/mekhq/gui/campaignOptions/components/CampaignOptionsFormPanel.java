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

    public void addFullWidthComponent(JComponent component) {
        int currentRow = row++;
        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = currentRow;
        layout.gridwidth = 2;
        layout.weightx = 1.0;
        layout.anchor = GridBagConstraints.WEST;
        layout.fill = GridBagConstraints.HORIZONTAL;
        layout.insets = new Insets(ROW_VERTICAL_PADDING, 0, ROW_VERTICAL_PADDING, 0);
        add(component, layout);
    }

    public void addComponentGrid(int columnCount, JComponent... components) {
        if (columnCount <= 1) {
            for (JComponent component : components) {
                addFullWidthComponent(component);
            }
            return;
        }

        int firstRow = row;
        for (int index = 0; index < components.length; index++) {
            int column = index % columnCount;

            GridBagConstraints layout = new GridBagConstraints();
            layout.gridx = column;
            layout.gridy = firstRow + index / columnCount;
            layout.weightx = 0.0;
            layout.anchor = GridBagConstraints.WEST;
            layout.fill = GridBagConstraints.NONE;
            layout.insets = new Insets(ROW_VERTICAL_PADDING,
                    0,
                    ROW_VERTICAL_PADDING,
                    getComponentGridRightPadding(column, columnCount));
            setMinimumComponentGridWidth(components[index], column);
            add(components[index], layout);
        }

        int rowCount = (components.length + columnCount - 1) / columnCount;
        for (int rowOffset = 0; rowOffset < rowCount; rowOffset++) {
            addTrailingFiller(firstRow + rowOffset, columnCount);
        }

        row += rowCount;
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

    private int getComponentGridRightPadding(int column, int columnCount) {
        if (column == columnCount - 1) {
            return 0;
        }
        return labelWidth > 0 ? LABEL_RIGHT_PADDING : CHECK_BOX_COLUMN_GAP;
    }

    private void setMinimumComponentGridWidth(JComponent component, int column) {
        if (labelWidth <= 0 || column != 0) {
            return;
        }

        setMinimumWidth(component, labelWidth);
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
        // REMAINDER + weightx/fill makes the control absorb all horizontal slack so its right edge reaches the form's
        // (and therefore the section's) right edge. The page stretches every section to a common width, so without this
        // the fixed-width control would be left stranded with a trailing filler eating the extra space, ending short of
        // the right edge whenever another section is wider. The minimum control width keeps it from collapsing.
        controlLayout.gridwidth = GridBagConstraints.REMAINDER;
        controlLayout.weightx = 1.0;
        controlLayout.anchor = GridBagConstraints.WEST;
        controlLayout.fill = GridBagConstraints.HORIZONTAL;
        controlLayout.insets = new Insets(ROW_VERTICAL_PADDING, 0, ROW_VERTICAL_PADDING, 0);
        add(control, controlLayout);
    }

    /**
     * Adds a grid of label/control pairs, packing {@code pairsPerRow} pairs onto each row. Components are supplied as
     * alternating {@code label, control, label, control, ...}. Every label column is sized to the form's label width
     * and every control column to the control width, so all pairs line up regardless of individual label lengths. The
     * control in the rightmost column stretches so its right edge aligns with the section's right edge (matching the
     * single-control rows added via {@link #addRow}); the remaining columns keep their natural left-packed widths.
     *
     * @param pairsPerRow       the number of label/control pairs to place side by side on each row
     * @param labelsAndControls alternating label and control components
     */
    public void addRowGrid(int pairsPerRow, JComponent... labelsAndControls) {
        if (pairsPerRow <= 1) {
            for (int index = 0; index + 1 < labelsAndControls.length; index += 2) {
                addRow(labelsAndControls[index], labelsAndControls[index + 1]);
            }
            return;
        }

        int pairCount = labelsAndControls.length / 2;
        int firstRow = row;
        for (int pairIndex = 0; pairIndex < pairCount; pairIndex++) {
            int columnPair = pairIndex % pairsPerRow;
            int gridRow = firstRow + pairIndex / pairsPerRow;

            JComponent label = labelsAndControls[pairIndex * 2];
            JComponent control = labelsAndControls[pairIndex * 2 + 1];
            setMinimumLabelWidth(label);
            setMinimumControlWidth(control);

            GridBagConstraints labelLayout = new GridBagConstraints();
            labelLayout.gridx = columnPair * 2;
            labelLayout.gridy = gridRow;
            labelLayout.weightx = 0.0;
            labelLayout.anchor = GridBagConstraints.WEST;
            labelLayout.fill = GridBagConstraints.NONE;
            labelLayout.insets = new Insets(ROW_VERTICAL_PADDING, 0, ROW_VERTICAL_PADDING, LABEL_RIGHT_PADDING);
            add(label, labelLayout);

            boolean lastColumn = (columnPair == pairsPerRow - 1);
            GridBagConstraints controlLayout = new GridBagConstraints();
            controlLayout.gridx = columnPair * 2 + 1;
            controlLayout.gridy = gridRow;
            controlLayout.anchor = GridBagConstraints.WEST;
            if (lastColumn) {
                // The rightmost control absorbs all horizontal slack so its right edge reaches the form's (and
                // therefore the section's) right edge, aligning with the controls in single-column sections.
                controlLayout.gridwidth = GridBagConstraints.REMAINDER;
                controlLayout.weightx = 1.0;
                controlLayout.fill = GridBagConstraints.HORIZONTAL;
                controlLayout.insets = new Insets(ROW_VERTICAL_PADDING, 0, ROW_VERTICAL_PADDING, 0);
            } else {
                controlLayout.weightx = 0.0;
                controlLayout.fill = GridBagConstraints.NONE;
                controlLayout.insets = new Insets(ROW_VERTICAL_PADDING, 0, ROW_VERTICAL_PADDING, CHECK_BOX_COLUMN_GAP);
            }
            add(control, controlLayout);
        }

        int rowCount = (pairCount + pairsPerRow - 1) / pairsPerRow;
        row += rowCount;
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