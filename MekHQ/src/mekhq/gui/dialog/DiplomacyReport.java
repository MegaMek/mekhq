/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import mekhq.MHQConstants;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionHints.FactionHint;
import mekhq.campaign.universe.factionHints.FactionHints;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;

/**
 * A dialog that displays a comprehensive, sortable report of all current wars, alliances, rivalries, and neutral
 * exceptions between factions.
 *
 * <p>The DiplomacyReport presents this information in a {@link JTable} with columns for Faction, Relationship, Other
 * Faction, and Notes. Relationships are populated for the given date, and the table columns are made sortable for user
 * convenience.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class DiplomacyReport extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.DiplomacyReport";

    private static final String DIALOG_TITLE = getTextAt(RESOURCE_BUNDLE,
          "DiplomacyReport.title");
    private static final String COLUMN_LABEL_FACTION = getTextAt(RESOURCE_BUNDLE,
          "DiplomacyReport.column.faction");
    private static final String COLUMN_LABEL_RELATIONSHIP = getTextAt(RESOURCE_BUNDLE,
          "DiplomacyReport.column.relationship");
    private static final String COLUMN_LABEL_OTHER_FACTION = getTextAt(RESOURCE_BUNDLE,
          "DiplomacyReport.column.otherFaction");
    private static final String COLUMN_LABEL_NOTES = getTextAt(RESOURCE_BUNDLE,
          "DiplomacyReport.column.notes");
    String[] COLUMNS = { COLUMN_LABEL_FACTION, COLUMN_LABEL_RELATIONSHIP, COLUMN_LABEL_OTHER_FACTION,
                         COLUMN_LABEL_NOTES };

    private static final Dimension DEFAULT_SIZE = scaleForGUI(800, 600);
    private static final int COLUMN_WIDTH_NORMAL = scaleForGUI(100);
    private static final int COLUMN_WIDTH_SMALL = scaleForGUI(20);
    private static final int[] COLUMN_WIDTHS = { COLUMN_WIDTH_NORMAL, COLUMN_WIDTH_NORMAL, COLUMN_WIDTH_SMALL,
                                                 COLUMN_WIDTH_NORMAL };

    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0);
    private final LocalDate today;
    private final boolean isClanCampaign;
    private final boolean isBeforeClanInvasionFirstWave;

    /**
     * Constructs the {@link DiplomacyReport} dialog.
     *
     * @param owner The parent frame which owns this dialog.
     * @param today The date for which faction relationships are reported.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public DiplomacyReport(Frame owner, boolean isClanCampaign, LocalDate today) {
        super(owner, DIALOG_TITLE, true);
        this.isClanCampaign = isClanCampaign;
        this.today = today;

        isBeforeClanInvasionFirstWave = today.isBefore(MHQConstants.CLAN_INVASION_FIRST_WAVE_BEGINS);

        setLayout(new BorderLayout());

        JTable table = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(COLUMN_WIDTHS[i]);
        }

        JScrollPane srlTable = new JScrollPane(table);
        add(srlTable, BorderLayout.CENTER);

        JButton btnClose = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "DiplomacyReport.button.close"));
        btnClose.addActionListener(e -> dispose());
        JPanel pnlControls = new JPanel();
        pnlControls.add(btnClose);
        add(pnlControls, BorderLayout.SOUTH);

        populateTable();

        setSize(DEFAULT_SIZE);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Populates the table model with all faction relationship data including wars, rivalries, alliances, and neutrality
     * exceptions for the date provided.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void populateTable() {
        FactionHints hints = FactionHints.getInstance();
        final int currentYear = today.getYear();

        addRows(hints.getWars(), getTextAt(RESOURCE_BUNDLE, "DiplomacyReport.relationship.war"), currentYear);

        addRows(hints.getRivals(), getTextAt(RESOURCE_BUNDLE, "DiplomacyReport.relationship.rivalry"), currentYear);

        addRows(hints.getNeutralExceptions(), getTextAt(RESOURCE_BUNDLE, "DiplomacyReport.relationship.neutral"),
              currentYear);

        addRows(hints.getAlliances(), getTextAt(RESOURCE_BUNDLE, "DiplomacyReport.relationship.alliance"), currentYear);
    }

    /**
     * Iterates all relationships described in the outer and inner maps and adds appropriate rows to the table model for
     * the specified relationship type. Duplicate relationship rows are prevented using the 'seenPairs' set.
     *
     * @param relationMap  The nested map of faction relationships.
     * @param relationType The textual description for the type of relationship.
     * @param currentYear  The year (used for retrieving full faction names).
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void addRows(Map<Faction, Map<Faction, List<FactionHint>>> relationMap, String relationType,
          int currentYear) {
        final Set<String> seenPairs = new HashSet<>();
        for (Map.Entry<Faction, Map<Faction, List<FactionHint>>> outerEntry : relationMap.entrySet()) {
            processEntry(relationType, outerEntry, currentYear, seenPairs);
        }
    }

    /**
     * Processes a single entry in the outer relationship map, further iterating its inner map and adding table rows for
     * all applicable faction hints.
     *
     * @param relationType The textual description for the type of relationship.
     * @param outerEntry   The outer map entry (from faction to map of related factions).
     * @param currentYear  The year in context, for getting full faction names.
     * @param seenPairs    Set used to avoid duplicate table entries.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void processEntry(String relationType, Map.Entry<Faction, Map<Faction, List<FactionHint>>> outerEntry,
          int currentYear, Set<String> seenPairs) {
        Faction primaryFaction = outerEntry.getKey();
        String primaryFactionName = primaryFaction.getFullName(currentYear);

        if (shouldHideFaction(primaryFaction)) {
            return;
        }

        Map<Faction, List<FactionHint>> innerMap = outerEntry.getValue();
        for (Map.Entry<Faction, List<FactionHint>> innerEntry : innerMap.entrySet()) {
            Faction otherFaction = innerEntry.getKey();
            String otherFactionName = otherFaction.getFullName(currentYear);

            String key = primaryFactionName + relationType + otherFactionName;
            if (seenPairs.contains(key)) {
                continue; // Avoid duplicates due to symmetry
            }

            List<FactionHint> factionHints = innerEntry.getValue();
            for (FactionHint factionHint : factionHints) {
                if (factionHint.isInDateRange(today)) {
                    String notes = factionHint.toString();
                    model.addRow(new Object[] {
                          primaryFaction.getFullName(currentYear), relationType, otherFactionName, notes
                    });
                    seenPairs.add(key);
                }
            }
        }
    }

    /**
     * Determines whether a given faction should be hidden from diplomacy reports based on campaign type and timeline.
     *
     * <p>If the current date is before the Clan Invasion first wave, this method will hide non-Clan factions in
     * Clan campaigns and Clan factions in non-Clan campaigns. Otherwise, no factions are hidden.</p>
     *
     * @param primaryFaction the faction to test for hiding
     *
     * @return {@code true} if the faction should be hidden
     *
     * @author Illiani
     * @since 0.50.10
     */
    private boolean shouldHideFaction(Faction primaryFaction) {
        if (isBeforeClanInvasionFirstWave) {
            return isClanCampaign != primaryFaction.isClan();
        }
        return false;
    }
}
