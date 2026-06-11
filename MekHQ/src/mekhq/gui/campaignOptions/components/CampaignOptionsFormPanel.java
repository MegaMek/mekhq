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

import jakarta.annotation.Nonnull;

/**
 * A compact, vertically stacked form panel for campaign option pages.
 *
 * <p>
 * The panel arranges content as a sequence of rows using a
 * {@link GridBagLayout}, exposing higher-level helpers so
 * callers describe <em>what</em> to add (a labelled row, a checkbox, a grid of
 * checkboxes) rather than managing
 * {@link GridBagConstraints} themselves. Rows are appended top to bottom; an
 * internal row counter is shared across every
 * {@code add*} method, so different row styles can be freely interleaved and
 * they stack in call order.
 * </p>
 *
 * <p>
 * Two fixed column widths drive alignment. The optional {@code labelWidth}
 * sizes the left (label) column so labels
 * line up across rows; a {@code labelWidth} of {@code 0} disables label-column
 * sizing. The {@code controlWidth} sets the
 * minimum width of the right (control) column. Single-control rows added
 * through {@link #addRow(JComponent, JComponent)}
 * stretch their control to the panel's right edge so it aligns with the
 * section's right edge, while grid helpers keep
 * their inner columns left-packed and only stretch the rightmost column.
 * </p>
 */
public class CampaignOptionsFormPanel extends JPanel {
    /**
     * The standard label-column width for campaign option form sections, shared across the dialog so every page's
     * labels and controls line up at the same x-positions. Use this unless a section genuinely needs something
     * different (for example, unusually long labels). It comfortably fits the longest checkbox/label text on the
     * standard pages, which is also why a two-column checkbox grid aligns with a labelled row at this width.
     */
    public static final int DEFAULT_LABEL_WIDTH = 300;

    /** The standard control-column width for campaign option form sections. */
    public static final int DEFAULT_CONTROL_WIDTH = 220;

    private static final int LABEL_RIGHT_PADDING = 12;
    private static final int ROW_VERTICAL_PADDING = 5;
    /**
     * Horizontal gap, in pixels, between adjacent columns in a grid layout: between
     * label/control pairs in
     * {@link #addRowGrid} and between checkbox columns in {@link #addCheckBoxGrid}.
     * Exposed so a section that mixes a
     * paired grid with single-control rows can line its control column up with one
     * of the grid's later columns.
     */
    public static final int GRID_COLUMN_GAP = 32;

    private final int labelWidth;
    private final int controlWidth;
    private int row;

    /**
     * Creates a form panel with no fixed label-column width and the default
     * control-column width.
     *
     * @param name the panel's base name; the Swing component name becomes
     *             {@code "pnl" + name}
     */
    public CampaignOptionsFormPanel(@Nonnull String name) {
        this(name, 0, DEFAULT_CONTROL_WIDTH);
    }

    /**
     * Creates a form panel with a fixed label-column width and the default
     * control-column width.
     *
     * @param name       the panel's base name; the Swing component name becomes
     *                   {@code "pnl" + name}
     * @param labelWidth the minimum width of the label column, or {@code 0} to
     *                   leave labels at their natural width
     */
    public CampaignOptionsFormPanel(@Nonnull String name, int labelWidth) {
        this(name, labelWidth, DEFAULT_CONTROL_WIDTH);
    }

    /**
     * Creates a form panel with explicit label- and control-column widths.
     *
     * @param name         the panel's base name; the Swing component name becomes
     *                     {@code "pnl" + name}
     * @param labelWidth   the minimum width of the label column, or {@code 0} to
     *                     leave labels at their natural width
     * @param controlWidth the minimum width of the control column
     */
    public CampaignOptionsFormPanel(@Nonnull String name, int labelWidth, int controlWidth) {
        this.labelWidth = labelWidth;
        this.controlWidth = controlWidth;
        setName("pnl" + name);
        setOpaque(false);
        setLayout(new GridBagLayout());
    }

    /**
     * Appends a single checkbox spanning the full width of the form on its own row.
     *
     * @param checkBox the checkbox to add
     */
    public void addCheckBox(@Nonnull JCheckBox checkBox) {
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

    /**
     * Appends a component that spans the full width of the form on its own row,
     * stretching horizontally to fill the
     * available space.
     *
     * @param component the component to add
     */
    public void addFullWidthComponent(@Nonnull JComponent component) {
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

    /**
     * Appends a grid of components, packing {@code columnCount} components onto
     * each row in call order. When the label
     * width is set, the first column of each row is sized to it so the grid lines
     * up with labelled rows. The component
     * in the rightmost column stretches to the form's right edge; the remaining
     * columns keep their natural width. A
     * {@code columnCount} of {@code 1} or less falls back to one full-width
     * component per row.
     *
     * @param columnCount the number of components to place side by side on each row
     * @param components  the components to add, in row-major order
     */
    public void addComponentGrid(int columnCount, @Nonnull JComponent... components) {
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
                    getGridColumnRightPadding(column, columnCount));
            setMinimumFirstColumnWidth(components[index], column);
            add(components[index], layout);
        }

        int rowCount = (components.length + columnCount - 1) / columnCount;
        for (int rowOffset = 0; rowOffset < rowCount; rowOffset++) {
            addTrailingFiller(firstRow + rowOffset, columnCount);
        }

        row += rowCount;
    }

    /**
     * Appends a grid of checkboxes, packing {@code columnCount} checkboxes onto
     * each row in call order. When the label
     * width is set, the first column is sized to it so the grid lines up with
     * labelled rows. A {@code columnCount} of
     * {@code 1} or less falls back to one full-width checkbox per row.
     *
     * @param columnCount the number of checkboxes to place side by side on each row
     * @param checkBoxes  the checkboxes to add, in row-major order
     */
    public void addCheckBoxGrid(int columnCount, @Nonnull JCheckBox... checkBoxes) {
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
                    getGridColumnRightPadding(column, columnCount));
            setMinimumFirstColumnWidth(checkBoxes[index], column);
            add(checkBoxes[index], layout);
        }

        int rowCount = (checkBoxes.length + columnCount - 1) / columnCount;
        for (int rowOffset = 0; rowOffset < rowCount; rowOffset++) {
            addTrailingFiller(firstRow + rowOffset, columnCount);
        }

        row += rowCount;
    }

    /**
     * Returns the right-hand inset for a grid cell: zero for the last column,
     * otherwise the label-to-control gap when a
     * label width is configured, or the wider inter-checkbox gap when it is not.
     */
    private int getGridColumnRightPadding(int column, int columnCount) {
        if (column == columnCount - 1) {
            return 0;
        }
        return labelWidth > 0 ? LABEL_RIGHT_PADDING : GRID_COLUMN_GAP;
    }

    /**
     * Sizes the first column of a grid row to the configured label width so grids
     * line up with labelled rows. Does
     * nothing when no label width is set or the cell is not in the first column.
     */
    private void setMinimumFirstColumnWidth(JComponent component, int column) {
        if (labelWidth <= 0 || column != 0) {
            return;
        }

        setMinimumWidth(component, labelWidth);
    }

    private void alignCheckBoxToStart(JCheckBox checkBox) {
        checkBox.setHorizontalAlignment(SwingConstants.LEADING);
    }

    /**
     * Appends a labelled row: a label in the left column and a control in the right
     * column. The control stretches to
     * the form's right edge so it aligns with controls in other single-control rows
     * and sections. When a label width is
     * set, the label column is sized to it; the control is given the configured
     * minimum control width.
     *
     * @param label   the label component for the left column
     * @param control the control component for the right column
     */
    public void addRow(@Nonnull JComponent label, @Nonnull JComponent control) {
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
    public void addRowGrid(int pairsPerRow, @Nonnull JComponent... labelsAndControls) {
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
                controlLayout.insets = new Insets(ROW_VERTICAL_PADDING, 0, ROW_VERTICAL_PADDING, GRID_COLUMN_GAP);
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
