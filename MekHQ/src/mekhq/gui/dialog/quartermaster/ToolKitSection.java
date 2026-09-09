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
package mekhq.gui.dialog.quartermaster;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import megamek.common.equipment.EquipmentType;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.quartermaster.RepairKitCatalog;
import mekhq.campaign.personnel.quartermaster.RepairKitIssuer;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * The "Tool Kits" tab of the shared kit-issue dialog: technician tool kits (specialized repair kits, toolkits, and
 * diagnostic scanners) issued to the selected technicians. Unlike an armor kit, a technician may own several tool kits
 * at once, so this offers a per-kit bulk action - issue to all selected techs who lack it (drawn from local stores, a
 * shortfall ordered), or remove it from all who have it.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ToolKitSection implements KitIssueSection {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.IssueEquipmentDialog";

    /** What the player wants done with a kit across the selected technicians. */
    private enum Action {NONE, ISSUE, REMOVE}

    private final transient Campaign campaign;
    private final transient List<Person> technicians;
    private final transient Runnable onChange;

    private final transient Map<String, Action> selections = new LinkedHashMap<>();
    private transient RosterModel rosterModel;

    public ToolKitSection(Campaign campaign, List<Person> technicians, Runnable onChange) {
        this.campaign = campaign;
        this.technicians = technicians;
        this.onChange = onChange;
        for (String kitName : RepairKitCatalog.allKitNames()) {
            selections.put(kitName, Action.NONE);
        }
    }

    @Override
    public String getTitle() {
        return getTextAt(RESOURCE_BUNDLE, "tab.tools");
    }

    @Override
    public JComponent getComponent() {
        JPanel tab = new JPanel(new BorderLayout(0, scaleForGUI(6)));
        tab.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(8), scaleForGUI(6), scaleForGUI(8)));

        JLabel hint = new JLabel(getTextAt(RESOURCE_BUNDLE, "tools.hint"));
        hint.setFont(hint.getFont().deriveFont(hint.getFont().getSize2D() - 1f));
        tab.add(hint, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        for (String kitName : RepairKitCatalog.allKitNames()) {
            EquipmentType kit = EquipmentType.get(kitName);
            if (kit != null) {
                rows.add(buildKitRow(kit));
                rows.add(javax.swing.Box.createVerticalStrut(scaleForGUI(4)));
            }
        }
        JScrollPane rowScroll = new JScrollPane(rows);
        rowScroll.setBorder(null);
        rowScroll.getVerticalScrollBar().setUnitIncrement(scaleForGUI(16));
        tab.add(rowScroll, BorderLayout.CENTER);

        rosterModel = new RosterModel();
        JTable roster = new JTable(rosterModel);
        roster.setEnabled(false);
        roster.getTableHeader().setReorderingAllowed(false);
        JScrollPane rosterScroll = new JScrollPane(roster);
        rosterScroll.setBorder(RoundedLineBorder.createSubtleRoundedLineBorder());
        rosterScroll.setPreferredSize(scaleForGUI(760, 130));
        tab.add(rosterScroll, BorderLayout.SOUTH);

        return tab;
    }

    private JPanel buildKitRow(EquipmentType kit) {
        JPanel row = new JPanel(new BorderLayout(scaleForGUI(8), 0));
        row.setBorder(BorderFactory.createCompoundBorder(RoundedLineBorder.createSubtleRoundedLineBorder(),
              BorderFactory.createEmptyBorder(scaleForGUI(4), scaleForGUI(8), scaleForGUI(4), scaleForGUI(8))));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel name = new JLabel(kit.getName());
        name.setFont(name.getFont().deriveFont(Font.BOLD));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(name);

        String effect = kitEffect(kit.getInternalName());
        if (effect != null) {
            JLabel effectLabel = new JLabel(effect);
            effectLabel.setFont(effectLabel.getFont().deriveFont(effectLabel.getFont().getSize2D() - 1f));
            effectLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            info.add(effectLabel);
        }

        String meta = getFormattedTextAt(RESOURCE_BUNDLE, "tools.row.meta",
              RepairKitIssuer.unitPrice(kit, campaign).toAmountString(), stockFor(kit), acquisitionText(kit));
        JLabel metaLabel = new JLabel(meta);
        metaLabel.setFont(metaLabel.getFont().deriveFont(metaLabel.getFont().getSize2D() - 1f));
        metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(metaLabel);
        row.add(info, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actionPanel.setOpaque(false);
        JComboBox<String> action = new JComboBox<>(new String[] {
              getTextAt(RESOURCE_BUNDLE, "tools.action.none"),
              getTextAt(RESOURCE_BUNDLE, "tools.action.issue"),
              getTextAt(RESOURCE_BUNDLE, "tools.action.remove")
        });
        action.setSelectedIndex(selections.getOrDefault(kit.getInternalName(), Action.NONE).ordinal());
        action.addActionListener(evt -> {
            selections.put(kit.getInternalName(), Action.values()[action.getSelectedIndex()]);
            if (rosterModel != null) {
                rosterModel.fireTableDataChanged();
            }
            onChange.run();
        });
        actionPanel.add(action);
        row.add(actionPanel, BorderLayout.EAST);

        return row;
    }

    @Override
    public boolean hasPendingChanges() {
        return selections.values().stream().anyMatch(action -> action != Action.NONE);
    }

    @Override
    public Tally computeTally() {
        Tally tally = Tally.empty();
        for (Map.Entry<String, Action> entry : selections.entrySet()) {
            if (entry.getValue() != Action.ISSUE) {
                continue;
            }
            EquipmentType kit = EquipmentType.get(entry.getKey());
            if (kit == null) {
                continue;
            }
            int need = countLacking(kit);
            int drawn = Math.min(stockFor(kit), need);
            int ordered = need - drawn;
            Money cost = RepairKitIssuer.unitPrice(kit, campaign).multipliedBy(ordered);
            tally = tally.plus(new Tally(drawn, ordered, cost));
        }
        return tally;
    }

    @Override
    public void commit(CommitTotals totals) {
        for (Map.Entry<String, Action> entry : selections.entrySet()) {
            EquipmentType kit = EquipmentType.get(entry.getKey());
            if (kit == null) {
                continue;
            }
            if (entry.getValue() == Action.ISSUE) {
                int shortfall = 0;
                for (Person tech : technicians) {
                    if (tech.hasRepairKit(kit.getInternalName())) {
                        continue;
                    }
                    if (RepairKitIssuer.issueFromStock(tech, kit, campaign)) {
                        totals.issued++;
                        totals.changed.add(tech);
                    } else {
                        shortfall++;
                    }
                }
                if (shortfall > 0) {
                    RepairKitIssuer.order(kit, shortfall, campaign);
                    totals.ordered += shortfall;
                }
            } else if (entry.getValue() == Action.REMOVE) {
                for (Person tech : technicians) {
                    if (RepairKitIssuer.removeKit(tech, kit, campaign)) {
                        totals.removed++;
                        totals.changed.add(tech);
                    }
                }
            }
        }
    }

    /**
     * The authored one-line effect summary for a kit (bonus magnitude and what it improves), looked up under
     * {@code tools.effect.<kit name with non-alphanumerics stripped>}. Returns {@code null} when no summary is
     * authored, so the row simply omits the effect line rather than showing a missing-resource marker.
     */
    private static String kitEffect(String internalName) {
        String text = getTextAt(RESOURCE_BUNDLE, "tools.effect." + internalName.replaceAll("[^A-Za-z0-9]", ""));
        return isResourceKeyValid(text) ? text : null;
    }

    /** How hard a Regular acquirer would find this kit, rendered for the row (mirrors the armor-kit cards). */
    private String acquisitionText(EquipmentType kit) {
        TargetRoll target = RepairKitIssuer.acquisitionTarget(kit, campaign);
        if (target.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            return getTextAt(RESOURCE_BUNDLE, "card.acquire.automatic");
        }
        if (target.cannotSucceed()) {
            return getTextAt(RESOURCE_BUNDLE, "card.acquire.unavailable");
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "card.acquire.tn", target.getValue());
    }

    /** Selected technicians who do not yet own the given kit. */
    private int countLacking(EquipmentType kit) {
        int count = 0;
        for (Person tech : technicians) {
            if (!tech.hasRepairKit(kit.getInternalName())) {
                count++;
            }
        }
        return count;
    }

    /** Kit stock is per location, so sum across each distinct local warehouse the technicians draw from. */
    private int stockFor(EquipmentType kit) {
        Set<LocalWarehouse> counted = new HashSet<>();
        int total = 0;
        for (Person tech : technicians) {
            LocalWarehouse warehouse = tech.getWarehouse();
            if ((warehouse != null) && counted.add(warehouse)) {
                total += RepairKitIssuer.localStock(tech, kit);
            }
        }
        return total;
    }

    /** The tool kits a technician will own after the pending selections are applied, by display name. */
    private List<String> pendingOwnedKits(Person tech) {
        List<String> owned = new ArrayList<>();
        for (String kitName : RepairKitCatalog.allKitNames()) {
            Action action = selections.getOrDefault(kitName, Action.NONE);
            boolean has = tech.hasRepairKit(kitName);
            boolean willHave = switch (action) {
                case ISSUE -> true;
                case REMOVE -> false;
                case NONE -> has;
            };
            if (willHave) {
                EquipmentType kit = EquipmentType.get(kitName);
                owned.add((kit != null) ? kit.getName() : kitName);
            }
        }
        return owned;
    }

    private final class RosterModel extends AbstractTableModel {
        private final String[] columnNames = {
              getTextAt(RESOURCE_BUNDLE, "tools.col.name"),
              getTextAt(RESOURCE_BUNDLE, "tools.col.assignment"),
              getTextAt(RESOURCE_BUNDLE, "tools.col.owned")
        };

        @Override
        public int getRowCount() {
            return technicians.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Person tech = technicians.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> tech.getFullTitle();
                case 1 -> (tech.getUnit() != null)
                                ? tech.getUnit().getName()
                                : getTextAt(RESOURCE_BUNDLE, "roster.unassigned");
                case 2 -> {
                    List<String> owned = pendingOwnedKits(tech);
                    yield owned.isEmpty() ? getTextAt(RESOURCE_BUNDLE, "tools.owned.none") : String.join(", ", owned);
                }
                default -> "";
            };
        }
    }
}
