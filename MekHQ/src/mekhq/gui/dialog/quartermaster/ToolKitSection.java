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
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;

import megamek.common.equipment.EquipmentType;
import megamek.common.rolls.TargetRoll;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog;
import mekhq.campaign.personnel.quartermaster.EquipmentKitIssuer;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * The "Tool Kits" tab of the shared kit-issue dialog. It reuses the same {@link KitCard} tiles and top-anchored grid as
 * the armor-kit tab, and works the same way: the player picks a single card and it applies to everyone selected -
 * choose a kit to issue it to all who lack it (drawn from local stores, a shortfall ordered), or the "No kit" card to
 * strip the tool kit from everyone. A technician carries at most one tool kit, exactly like an armor kit.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ToolKitSection implements KitIssueSection {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.IssueEquipmentDialog";

    /** The selection sentinel meaning "remove the tool kit from the selected technicians". */
    private static final String STRIP = " strip ";
    /** The accent color for the tool-kit cards (distinct from the four armor-category accents). */
    private static final Color ACCENT = new Color(0x5A, 0x7A, 0x9A);

    private final transient Campaign campaign;
    private final transient List<Person> technicians;
    private final transient Runnable onChange;

    /** The single active selection: {@code null} = no change, {@link #STRIP} = strip, else a kit internal name. */
    private String selected;
    private final transient List<KitCard> cards = new ArrayList<>();
    private transient RosterModel rosterModel;
    /** Kit stock tallied once across the technicians' warehouses; see {@link #stockFor(EquipmentType)}. */
    private transient Map<EquipmentType, Integer> stockCache;

    public ToolKitSection(Campaign campaign, List<Person> technicians, Runnable onChange) {
        this.campaign = campaign;
        this.technicians = technicians;
        this.onChange = onChange;
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
        hint.setForeground(KitCard.mutedColor());
        tab.add(hint, BorderLayout.NORTH);

        cards.clear();
        cards.add(stripCard());
        for (String kitName : EquipmentKitCatalog.allKitNames()) {
            EquipmentType kit = EquipmentType.get(kitName);
            if (kit != null) {
                cards.add(kitCard(kit));
            }
        }

        FastJScrollPane scroll = new FastJScrollPane(KitCard.grid(cards),
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(scaleForGUI(16));
        tab.add(scroll, BorderLayout.CENTER);

        rosterModel = new RosterModel(technicians);
        JTable roster = new JTable(rosterModel);
        roster.setEnabled(false);
        roster.getTableHeader().setReorderingAllowed(false);
        FastJScrollPane rosterScroll = new FastJScrollPane(roster);
        rosterScroll.setBorder(RoundedLineBorder.createSubtleRoundedLineBorder());
        rosterScroll.setPreferredSize(scaleForGUI(760, 130));
        tab.add(rosterScroll, BorderLayout.SOUTH);

        return tab;
    }

    private KitCard stripCard() {
        return new KitCard(ACCENT, getTextAt(RESOURCE_BUNDLE, "tools.strip.name"), true,
              List.of(getTextAt(RESOURCE_BUNDLE, "tools.strip.desc")), List.of(), null, false, null,
              () -> STRIP.equals(selected),
              () -> select(STRIP));
    }

    private KitCard kitCard(EquipmentType kit) {
        List<String> detail = new ArrayList<>();
        String effect = kitEffect(kit.getInternalName());
        if (effect != null) {
            detail.add(effect);
        }
        detail.add(acquisitionText(kit));
        int stock = stockFor(kit);
        String priceText = EquipmentKitIssuer.unitPrice(kit, campaign).toAmountString()
                                 + " " + getTextAt(RESOURCE_BUNDLE, "card.each");
        return new KitCard(ACCENT, kit.getName(), false, detail, List.of(),
              getFormattedTextAt(RESOURCE_BUNDLE, "card.stock", stock), stock < technicians.size(), priceText,
              () -> kit.getInternalName().equals(selected),
              () -> select(kit.getInternalName()));
    }

    /** Selects (or, when re-clicked, clears) a card and refreshes the cards, roster, and footer tally. */
    private void select(String value) {
        selected = value.equals(selected) ? null : value;
        for (KitCard card : cards) {
            card.refreshSelected();
        }
        if (rosterModel != null) {
            rosterModel.setSelected(selected);
        }
        onChange.run();
    }

    @Override
    public boolean hasPendingChanges() {
        return selected != null;
    }

    @Override
    public Tally computeTally() {
        if ((selected == null) || STRIP.equals(selected)) {
            return Tally.empty();
        }
        EquipmentType kit = EquipmentType.get(selected);
        if (kit == null) {
            return Tally.empty();
        }
        int need = countLacking(kit);
        int drawn = Math.min(stockFor(kit), need);
        int ordered = need - drawn;
        Money cost = EquipmentKitIssuer.unitPrice(kit, campaign).multipliedBy(ordered);
        return new Tally(drawn, ordered, cost);
    }

    @Override
    public void commit(CommitTotals totals) {
        if (selected == null) {
            return;
        }
        if (STRIP.equals(selected)) {
            for (Person tech : technicians) {
                String wornName = tech.getRepairKitName();
                if (wornName == null) {
                    continue;
                }
                EquipmentType worn = EquipmentType.get(wornName);
                if ((worn != null) && EquipmentKitIssuer.removeKit(tech, worn, campaign)) {
                    totals.removed++;
                    totals.changed.add(tech);
                }
            }
            return;
        }
        EquipmentType kit = EquipmentType.get(selected);
        if (kit == null) {
            return;
        }
        int shortfall = 0;
        for (Person tech : technicians) {
            if (tech.hasRepairKit(kit.getInternalName())) {
                continue;
            }
            if (EquipmentKitIssuer.issueFromStock(tech, kit, campaign)) {
                totals.issued++;
                totals.changed.add(tech);
            } else {
                shortfall++;
            }
        }
        if (shortfall > 0) {
            EquipmentKitIssuer.order(kit, shortfall, campaign);
            totals.ordered += shortfall;
        }
    }

    /**
     * The authored one-line effect summary for a kit (bonus magnitude and what it improves), looked up under
     * {@code tools.effect.<kit name with non-alphanumerics stripped>}. Returns {@code null} when no summary is
     * authored, so the card simply omits the effect line rather than showing a missing-resource marker.
     */
    private static String kitEffect(String internalName) {
        String text = getTextAt(RESOURCE_BUNDLE, "tools.effect." + internalName.replaceAll("[^A-Za-z0-9]", ""));
        return isResourceKeyValid(text) ? text : null;
    }

    /** How hard a Regular acquirer would find this kit, rendered for the card (mirrors the armor-kit cards). */
    private String acquisitionText(EquipmentType kit) {
        TargetRoll target = EquipmentKitIssuer.acquisitionTarget(kit, campaign);
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

    /**
     * Kit stock across the technicians' distinct local warehouses. Tallied once (a single spare-parts pass) and cached
     * for the dialog's lifetime, so building all the cards does not rescan each warehouse per kit.
     */
    private int stockFor(EquipmentType kit) {
        if (stockCache == null) {
            stockCache = EquipmentKitIssuer.localStock(technicians);
        }
        return stockCache.getOrDefault(kit, 0);
    }

    private static final class RosterModel extends AbstractTableModel {
        private final List<Person> technicians;
        private String selected;

        private final String[] columnNames = {
              getTextAt(RESOURCE_BUNDLE, "tools.col.name"),
              getTextAt(RESOURCE_BUNDLE, "tools.col.assignment"),
              getTextAt(RESOURCE_BUNDLE, "tools.col.owned")
        };

        RosterModel(List<Person> technicians) {
            this.technicians = technicians;
        }

        void setSelected(String selected) {
            this.selected = selected;
            fireTableDataChanged();
        }

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
                    String owned = pendingOwnedKit(tech);
                    yield (owned == null) ? getTextAt(RESOURCE_BUNDLE, "tools.owned.none") : owned;
                }
                default -> "";
            };
        }

        private String pendingOwnedKit(Person tech) {
            String kitName;
            if (STRIP.equals(selected)) {
                kitName = null;
            } else if (selected != null) {
                kitName = selected;
            } else {
                kitName = tech.getRepairKitName();
            }

            if (kitName == null) {
                return null;
            }

            EquipmentType kit = EquipmentType.get(kitName);
            return (kit != null) ? kit.getName() : kitName;
        }
    }
}
