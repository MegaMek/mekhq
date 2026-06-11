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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A compact matrix panel for option groups that compare numeric modifiers across shared row labels.
 */
public class CampaignOptionsModifierTablePanel extends JPanel {
    private static final int DEFAULT_ROW_LABEL_WIDTH = 120;
    private static final int DEFAULT_CONTROL_WIDTH = 104;
    private static final int ROW_LABEL_RIGHT_PADDING = 12;
    private static final int COLUMN_RIGHT_PADDING = 20;
    private static final int ROW_HORIZONTAL_PADDING = 4;
    private static final int ROW_VERTICAL_PADDING = 3;
    private static final int HEADER_BOTTOM_PADDING = 5;
    private static final int TABLE_VERTICAL_PADDING = 2;
    private static final int TRAILING_RAIL_WIDTH = 48;
    private static final int BACKGROUND_BLEND_WEIGHT = 3;

    private final JPanel matrixPanel = new JPanel(new GridBagLayout());
    private final int rowLabelWidth;
    private final int controlWidth;
    private final int columnCount;
    /**
     * Minimum width of each data column, indexed by column. Computed from the
     * header row as {@code max(controlWidth,
     * header width)} so a wide header (e.g. "Random SPA Chances") cannot clip,
     * while the same per-column width is
     * reused for the data rows so every column lines up across the header and data
     * rows when they stretch.
     */
    private final int[] columnWidths;
    private int row;

    /**
     * Creates a modifier table with the default row-label and control column
     * widths.
     *
     * @param name          the panel's base name; the Swing component name becomes
     *                      {@code "pnl" + name}
     * @param columnHeaders the header component for each data column; the column
     *                      count is taken from this array
     */
    public CampaignOptionsModifierTablePanel(@Nonnull String name, @Nonnull JComponent... columnHeaders) {
        this(name, DEFAULT_ROW_LABEL_WIDTH, DEFAULT_CONTROL_WIDTH, columnHeaders);
    }

    /**
     * Creates a modifier table with explicit row-label and control column widths.
     *
     * @param name          the panel's base name; the Swing component name becomes
     *                      {@code "pnl" + name}
     * @param rowLabelWidth the minimum width of the leading row-label column
     * @param controlWidth  the minimum width of each data column
     * @param columnHeaders the header component for each data column; the column
     *                      count is taken from this array
     */
    public CampaignOptionsModifierTablePanel(@Nonnull String name, int rowLabelWidth, int controlWidth,
          @Nonnull JComponent... columnHeaders) {
        this.rowLabelWidth = rowLabelWidth;
        this.controlWidth = controlWidth;
        this.columnCount = columnHeaders.length;
        this.columnWidths = new int[columnCount];

        setName("pnl" + name);
        setOpaque(false);
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(TABLE_VERTICAL_PADDING, 0, TABLE_VERTICAL_PADDING, 0));

        matrixPanel.setOpaque(false);
        addMatrixPanel();

        addHeaderRow(columnHeaders);
    }

    /**
     * Adds one row to the modifier table. Pass {@code null} for cells that do not apply to the row.
     *
     * @param rowLabel the component used as the row heading
     * @param cells    row controls, one entry for each table column
     */
    public void addRow(@Nonnull JComponent rowLabel, @Nullable JComponent... cells) {
        if (cells.length != columnCount) {
            throw new IllegalArgumentException("Modifier table row must provide one cell per column.");
        }

        JPanel rowPanel = createRowPanel(row % 2 == 0);
        setMinimumWidth(rowLabel, rowLabelWidth);
        setLabelAlignment(rowLabel, SwingConstants.TRAILING);
        rowPanel.add(rowLabel, createCellLayout(0, GridBagConstraints.EAST, ROW_LABEL_RIGHT_PADDING, 0.0));

        for (int column = 0; column < columnCount; column++) {
            JComponent cell = cells[column] == null ? createPlaceholder() : cells[column];
            setMinimumWidth(cell, columnWidths[column]);
            rowPanel.add(cell,
                    createCellLayout(column + 1, GridBagConstraints.CENTER, getColumnRightPadding(column), 1.0));
        }
        rowPanel.add(createSpacer(TRAILING_RAIL_WIDTH),
                        createCellLayout(columnCount + 1, GridBagConstraints.CENTER, 0, 0.0));

        addTableRow(rowPanel);
    }

    private void addHeaderRow(JComponent... columnHeaders) {
        // Style the headers first so we can measure their (bold) preferred widths, then
        // size each data column to
        // max(controlWidth, header width). Reusing the same per-column width for the
        // data rows below keeps the
        // columns aligned once they stretch, and guarantees a wide header (e.g. "Random
        // SPA Chances") never clips.
        for (int column = 0; column < columnHeaders.length; column++) {
            JComponent header = columnHeaders[column];
            styleHeader(header);
            setLabelAlignment(header, SwingConstants.CENTER);
            columnWidths[column] = Math.max(controlWidth, header.getPreferredSize().width);
        }

        JPanel headerPanel = createRowPanel(false);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, 1, 0, getGridColor()),
              BorderFactory.createEmptyBorder(0, ROW_HORIZONTAL_PADDING, HEADER_BOTTOM_PADDING,
                    ROW_HORIZONTAL_PADDING)));
        headerPanel.add(createSpacer(rowLabelWidth),
                createCellLayout(0, GridBagConstraints.CENTER, ROW_LABEL_RIGHT_PADDING, 0.0));

        for (int column = 0; column < columnHeaders.length; column++) {
            JComponent header = columnHeaders[column];
            setMinimumWidth(header, columnWidths[column]);
            // Let the header label fill its (wider, stretched) cell rather than sitting at
            // its frozen preferred
            // width. With FlatLaf font scaling the painted bold text can be wider than the
            // build-time measurement,
            // so a non-filling label would clip ("Random SPA Chanc..."); filling guarantees
            // the centred text fits.
            headerPanel.add(header,
                    createCellLayout(column + 1, GridBagConstraints.CENTER, getColumnRightPadding(column), 1.0,
                            GridBagConstraints.HORIZONTAL));
        }
        headerPanel.add(createSpacer(TRAILING_RAIL_WIDTH),
                createCellLayout(columnCount + 1, GridBagConstraints.CENTER, 0, 0.0));

        addTableRow(headerPanel);
    }

    private void addMatrixPanel() {
        // Let the matrix fill the section width so the data columns can share the
        // available space (the rows give
        // their data cells weight); this keeps headers from clipping and lets the
        // centred headers spread out.
        GridBagConstraints matrixLayout = new GridBagConstraints();
        matrixLayout.gridx = 0;
        matrixLayout.gridy = 0;
        matrixLayout.weightx = 1.0;
        matrixLayout.anchor = GridBagConstraints.WEST;
        matrixLayout.fill = GridBagConstraints.HORIZONTAL;
        add(matrixPanel, matrixLayout);
    }

    private JPanel createRowPanel(boolean banded) {
        JPanel rowPanel = new JPanel(new GridBagLayout());
        rowPanel.setBorder(BorderFactory.createEmptyBorder(0, ROW_HORIZONTAL_PADDING, 0, ROW_HORIZONTAL_PADDING));

        Color alternateRowColor = getAlternateRowColor();
        rowPanel.setOpaque(banded && alternateRowColor != null);
        if (alternateRowColor != null) {
            rowPanel.setBackground(alternateRowColor);
        }

        return rowPanel;
    }

    private void addTableRow(JPanel rowPanel) {
        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = row++;
        layout.weightx = 1.0;
        layout.anchor = GridBagConstraints.WEST;
        layout.fill = GridBagConstraints.HORIZONTAL;
        matrixPanel.add(rowPanel, layout);
    }

    private GridBagConstraints createCellLayout(int gridX, int anchor, int rightPadding, double weightX) {
        return createCellLayout(gridX, anchor, rightPadding, weightX, GridBagConstraints.NONE);
    }

    private GridBagConstraints createCellLayout(int gridX, int anchor, int rightPadding, double weightX, int fill) {
        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = gridX;
        layout.gridy = 0;
        layout.weightx = weightX;
        layout.anchor = anchor;
        layout.fill = fill;
        layout.insets = new Insets(ROW_VERTICAL_PADDING, 0, ROW_VERTICAL_PADDING, rightPadding);
        return layout;
    }

    private int getColumnRightPadding(int column) {
        return column == columnCount - 1 ? 0 : COLUMN_RIGHT_PADDING;
    }

    private JComponent createPlaceholder() {
        JLabel placeholder = new JLabel("-");
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        Color placeholderColor = UIManager.getColor("Label.disabledForeground");
        if (placeholderColor != null) {
            placeholder.setForeground(placeholderColor);
        }
        return placeholder;
    }

    private JComponent createSpacer(int width) {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        Dimension size = new Dimension(width, 1);
        spacer.setPreferredSize(size);
        spacer.setMinimumSize(size);
        return spacer;
    }

    private void setLabelAlignment(JComponent component, int alignment) {
        if (component instanceof JLabel) {
            ((JLabel) component).setHorizontalAlignment(alignment);
        }
    }

    private void styleHeader(JComponent component) {
        Color headerForeground = UIManager.getColor("TableHeader.foreground");
        if (headerForeground != null) {
            component.setForeground(headerForeground);
        }

        if (component instanceof JLabel) {
            JLabel label = (JLabel) component;
            label.setFont(label.getFont().deriveFont(Font.BOLD));
        }
    }

    private @Nullable Color getAlternateRowColor() {
        Color color = UIManager.getColor("Table.alternateRowColor");
        Color background = UIManager.getColor("Panel.background");
        if (color == null || background == null) {
            return color == null ? background : color;
        }

        return blend(background, color);
    }

    private Color blend(Color background, Color foreground) {
        int red = ((background.getRed() * BACKGROUND_BLEND_WEIGHT) + foreground.getRed()) /
              (BACKGROUND_BLEND_WEIGHT + 1);
        int green = ((background.getGreen() * BACKGROUND_BLEND_WEIGHT) + foreground.getGreen()) /
              (BACKGROUND_BLEND_WEIGHT + 1);
        int blue = ((background.getBlue() * BACKGROUND_BLEND_WEIGHT) + foreground.getBlue()) /
              (BACKGROUND_BLEND_WEIGHT + 1);
        return new Color(red, green, blue);
    }

    private Color getGridColor() {
        Color color = UIManager.getColor("Table.gridColor");
        if (color == null) {
            color = UIManager.getColor("Component.borderColor");
        }
        if (color == null) {
            color = UIManager.getColor("Separator.foreground");
        }
        return color == null ? getForeground() : color;
    }

    private void setMinimumWidth(JComponent component, int minimumWidth) {
        Dimension preferredSize = component.getPreferredSize();
        int width = Math.max(preferredSize.width, minimumWidth);
        Dimension adjustedSize = new Dimension(width, preferredSize.height);
        component.setPreferredSize(adjustedSize);
        component.setMinimumSize(adjustedSize);
    }
}
