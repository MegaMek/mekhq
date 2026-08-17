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
package mekhq.gui.dialog;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.MHQConstants.BATTLE_OF_TUKAYYID;
import static mekhq.MHQConstants.CLAN_INVASION_FIRST_WAVE_BEGINS;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import megamek.common.enums.TechBase;
import megamek.common.ui.EnhancedTabbedPane;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.warriorsAlmanac.AlmanacTechAdvancementPhase;
import mekhq.campaign.universe.warriorsAlmanac.WarriorsAlmanacEntry;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;

/**
 * Displays the Warrior's Almanac: the parts and units whose development reached a new phase (prototype, production,
 * common, extinct, or reintroduced) in the current campaign year.
 *
 * <p>The almanac is presented as a nested set of tabs. The outer tabs separate Parts from Units; within each, an
 * inner tab per category (Warehouse part category / unit type) holds a sortable table of that year's developments.
 * Entries are filtered by tech base to reflect what the player force could plausibly know of at the current date.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class WarriorsAlmanacDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.WarriorsAlmanacDialog";

    private static final int NAME_COLUMN = 0;
    private static final int TECH_BASE_COLUMN = 1;
    private static final int DEVELOPMENT_COLUMN = 2;

    /** Extra pixels (pre-scaling) added to a content-sized column so its text is not visually cramped. */
    private static final int COLUMN_PADDING = 16;

    private static final int DEFAULT_WIDTH = scaleForGUI(700);
    private static final int DEFAULT_HEIGHT = scaleForGUI(600);
    private static final int MINIMUM_HEIGHT = scaleForGUI(400);

    /** Identifies a table row, so entries that would render identically can be collapsed into one. */
    private record RowKey(String name, TechBase techBase, AlmanacTechAdvancementPhase phase) {}

    public WarriorsAlmanacDialog(final Campaign campaign, final boolean isAutomaticDisplay) {
        final LocalDate currentDate = campaign.getLocalDate();
        final int gameYear = currentDate.getYear();

        final boolean isClanForce = campaign.getPlayerForce().isClanForce();
        final boolean hideClanAndMixed = currentDate.isBefore(BATTLE_OF_TUKAYYID) && !isClanForce;
        final boolean hideInnerSphereAndMixed = currentDate.isBefore(CLAN_INVASION_FIRST_WAVE_BEGINS) && isClanForce;

        final List<WarriorsAlmanacEntry> partsEntries = gatherEntries(campaign.getPartsAlmanac(), gameYear,
              hideClanAndMixed, hideInnerSphereAndMixed, true);
        final List<WarriorsAlmanacEntry> unitsEntries = gatherEntries(campaign.getUnitsAlmanac(), gameYear,
              hideClanAndMixed, hideInnerSphereAndMixed, false);

        if (partsEntries.isEmpty() && unitsEntries.isEmpty()) {
            if (!isAutomaticDisplay) {
                final String message = getFormattedTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.title",
                      String.valueOf(gameYear))
                                             + getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.blurb")
                                             + getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.nothing");
                new ImmersiveDialogNotification(campaign, message, true);
            }
            return;
        }

        showAlmanac(gameYear, partsEntries, unitsEntries);
    }

    /**
     * Collects this year's visible development events from an almanac.
     *
     * @param almanac                 the year-keyed almanac to read from
     * @param gameYear                the current campaign year
     * @param hideClanAndMixed        whether Clan (and, for units, mixed) entries should be hidden
     * @param hideInnerSphereAndMixed whether Inner Sphere (and, for units, mixed) entries should be hidden
     * @param isParts                 whether this is the parts almanac (affects how the {@code ALL} tech base is
     *                                treated)
     *
     * @return the visible entries for the current year
     */
    private static List<WarriorsAlmanacEntry> gatherEntries(Map<Integer, List<WarriorsAlmanacEntry>> almanac,
          int gameYear, boolean hideClanAndMixed, boolean hideInnerSphereAndMixed, boolean isParts) {
        final List<WarriorsAlmanacEntry> visible = new ArrayList<>();
        if (almanac == null) {
            return visible;
        }
        final List<WarriorsAlmanacEntry> forYear = almanac.get(gameYear);
        if (forYear == null) {
            return visible;
        }
        for (WarriorsAlmanacEntry entry : forYear) {
            if (isVisible(entry.techBase(), hideClanAndMixed, hideInnerSphereAndMixed, isParts)) {
                visible.add(entry);
            }
        }
        return visible;
    }

    private static boolean isVisible(TechBase techBase, boolean hideClanAndMixed, boolean hideInnerSphereAndMixed,
          boolean isParts) {
        // Parts with an ALL tech base are available to both Inner Sphere and Clan users, so they are always shown.
        // For units, ALL denotes mixed tech, which is gated alongside the opposing tech base.
        if (isParts && techBase == TechBase.ALL) {
            return true;
        }
        if (hideClanAndMixed) {
            return techBase == TechBase.IS;
        }
        if (hideInnerSphereAndMixed) {
            return techBase == TechBase.CLAN;
        }
        return true;
    }

    private void showAlmanac(int gameYear, List<WarriorsAlmanacEntry> partsEntries,
          List<WarriorsAlmanacEntry> unitsEntries) {
        final JDialog dialog = new JDialog();
        dialog.setTitle(getFormattedTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.dialogTitle",
              String.valueOf(gameYear)));
        dialog.setLayout(new BorderLayout());

        final JPanel header = new JPanel(new BorderLayout());
        header.add(buildTitleBlock(getFormattedTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.title",
              String.valueOf(gameYear))), BorderLayout.NORTH);
        header.add(buildIntroBlock(getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.blurb")), BorderLayout.CENTER);
        dialog.add(header, BorderLayout.NORTH);

        final EnhancedTabbedPane outerTabs = new EnhancedTabbedPane(false, false);
        if (!partsEntries.isEmpty()) {
            outerTabs.addTab(getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.tab.parts"),
                  buildCategoryTabs(partsEntries, true));
        }
        if (!unitsEntries.isEmpty()) {
            outerTabs.addTab(getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.tab.units"),
                  buildCategoryTabs(unitsEntries, false));
        }
        dialog.add(outerTabs, BorderLayout.CENTER);

        final JButton closeButton = new JButton(getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.close.text"));
        closeButton.addActionListener(evt -> dialog.dispose());
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        // The layout is tuned for the default width; forbid shrinking narrower than it (height may still shrink).
        dialog.setMinimumSize(new Dimension(DEFAULT_WIDTH, MINIMUM_HEIGHT));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Builds an inner tabbed pane with one tab per category, ordered by each category's canonical ordinal.
     *
     * @param entries the entries to group into per-category tabs
     * @param isParts whether these are parts (affects how the {@code ALL} tech base is labeled)
     *
     * @return the populated inner tabbed pane
     */
    private static EnhancedTabbedPane buildCategoryTabs(List<WarriorsAlmanacEntry> entries, boolean isParts) {
        final Map<Integer, List<WarriorsAlmanacEntry>> byCategory = new TreeMap<>();
        final Map<Integer, String> categoryLabels = new HashMap<>();
        final Map<Integer, String> categoryIntros = new HashMap<>();
        for (WarriorsAlmanacEntry entry : entries) {
            byCategory.computeIfAbsent(entry.categoryOrder(), ignored -> new ArrayList<>()).add(entry);
            categoryLabels.putIfAbsent(entry.categoryOrder(), entry.categoryLabel());
            categoryIntros.putIfAbsent(entry.categoryOrder(), entry.categoryIntro());
        }

        final EnhancedTabbedPane categoryTabs = new EnhancedTabbedPane(false, false);
        for (Map.Entry<Integer, List<WarriorsAlmanacEntry>> category : byCategory.entrySet()) {
            final JPanel tabPanel = new JPanel(new BorderLayout());
            tabPanel.add(buildIntroBlock(categoryIntros.get(category.getKey())), BorderLayout.NORTH);
            tabPanel.add(buildTable(category.getValue(), isParts), BorderLayout.CENTER);
            categoryTabs.addTab(categoryLabels.get(category.getKey()), tabPanel);
        }
        return categoryTabs;
    }

    /**
     * Builds a sortable, read-only table of development events, initially sorted by name.
     *
     * @param entries the entries to display
     * @param isParts whether these are parts (affects how the {@code ALL} tech base is labeled)
     *
     * @return a scroll pane wrapping the table
     */
    private static JScrollPane buildTable(List<WarriorsAlmanacEntry> entries, boolean isParts) {
        final String[] columns = {
              getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.column.name"),
              getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.column.techBase"),
              getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.column.development") };

        final DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        // The parts store holds many identically named parts (e.g. armor of differing tonnages); collapse rows that
        // would be indistinguishable in the table.
        final Set<RowKey> seen = new HashSet<>();
        for (WarriorsAlmanacEntry entry : entries) {
            if (seen.add(new RowKey(entry.name(), entry.techBase(), entry.phase()))) {
                model.addRow(new Object[] { entry.name(), techBaseLabel(entry.techBase(), isParts),
                                            entry.phase().getLabel() });
            }
        }

        final JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(NAME_COLUMN, SortOrder.ASCENDING)));

        // The tech-base and development columns draw from a small, fixed vocabulary; pin them to their content width
        // so the free-form Name column absorbs all remaining horizontal space.
        table.getColumnModel().getColumn(NAME_COLUMN).setMinWidth(scaleForGUI(150));
        pinColumnToContent(table, TECH_BASE_COLUMN);
        pinColumnToContent(table, DEVELOPMENT_COLUMN);

        return new JScrollPane(table);
    }

    /**
     * Fixes a column's width to fit its header and cell content, so it neither wastes space nor steals it from the
     * flexible Name column.
     *
     * @param table       the table whose column is being sized
     * @param columnIndex the column to pin
     */
    private static void pinColumnToContent(JTable table, int columnIndex) {
        final TableColumn column = table.getColumnModel().getColumn(columnIndex);

        int width = headerPreferredWidth(table, column);
        for (int row = 0; row < table.getRowCount(); row++) {
            final TableCellRenderer renderer = table.getCellRenderer(row, columnIndex);
            final Component rendered = table.prepareRenderer(renderer, row, columnIndex);
            width = Math.max(width, rendered.getPreferredSize().width);
        }
        width += scaleForGUI(COLUMN_PADDING);

        column.setMinWidth(width);
        column.setPreferredWidth(width);
        column.setMaxWidth(width);
    }

    private static int headerPreferredWidth(JTable table, TableColumn column) {
        TableCellRenderer renderer = column.getHeaderRenderer();
        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }
        final Component rendered = renderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false,
              -1, column.getModelIndex());
        return rendered.getPreferredSize().width;
    }

    /**
     * Renders the (single-line, HTML) almanac title, centered across the full dialog width.
     *
     * @param html the title HTML
     *
     * @return a centered label rendering the title
     */
    private static JComponent buildTitleBlock(String html) {
        final JLabel label = new JLabel("<html>" + html + "</html>");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(new EmptyBorder(scaleForGUI(8), scaleForGUI(12), 0, scaleForGUI(12)));
        return label;
    }

    /**
     * Wraps a plain-text category intro in a read-only, word-wrapping text area.
     *
     * <p>Unlike the fixed-width HTML header, the intros carry no markup, so a {@link JTextArea} with line wrapping can
     * reflow the text to whatever width the tab currently affords — widening or narrowing as the dialog is
     * resized.</p>
     *
     * @param text the plain intro text
     *
     * @return a transparent, non-interactive text area rendering the intro
     */
    private static JComponent buildIntroBlock(String text) {
        final JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(false);
        // Match the surrounding look-and-feel rather than the text area's default (monospaced) editor font.
        textArea.setFont(UIManager.getFont("Label.font"));
        textArea.setBorder(new EmptyBorder(scaleForGUI(8), scaleForGUI(12), scaleForGUI(8), scaleForGUI(12)));
        return textArea;
    }

    private static String techBaseLabel(TechBase techBase, boolean isParts) {
        return switch (techBase) {
            case IS -> getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.techBaseValue.innerSphere");
            case CLAN -> getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.techBaseValue.clan");
            // For parts, ALL means the item is available to both tech bases; for units it denotes mixed tech.
            case ALL -> getTextAt(RESOURCE_BUNDLE, isParts
                                                         ? "WarriorsAlmanacDialog.techBaseValue.isClan"
                                                         : "WarriorsAlmanacDialog.techBaseValue.mixed");
            case UNKNOWN -> getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.techBaseValue.unknown");
        };
    }
}
