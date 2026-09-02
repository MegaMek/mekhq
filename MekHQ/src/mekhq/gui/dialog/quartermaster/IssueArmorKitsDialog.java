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

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.rolls.TargetRoll;
import megamek.common.ui.FastJScrollPane;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog;
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.Category;
import mekhq.campaign.personnel.quartermaster.ArmorKitIssuer;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * The quartermaster's counter for issuing personal armor kits. It takes a selection of personnel — or units, whose
 * crews stand in for them — sorts them into the groups that draw from the same kits, and lets the player pick one kit
 * per group. Kits come out of the character's local stores; a shortfall is ordered. A panel along the bottom lists
 * everyone being kitted and what they wear now, so a bulk issue is legible before it is committed.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class IssueArmorKitsDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.IssueArmorKitsDialog";

    private final transient Campaign campaign;
    private final transient List<Person> personnel;
    private final transient Map<Category, List<Person>> byCategory = new EnumMap<>(Category.class);

    /** The kit chosen for a group, or absent if the group is left unchanged. */
    private final transient Map<Category, EquipmentType> chosenKit = new EnumMap<>(Category.class);
    /** Groups the player has chosen to strip back to coveralls. */
    private final transient Set<Category> stripped = new HashSet<>();
    /** Soldier groups the player has chosen to return to their platoons' designed armor. */
    private final transient Set<Category> restoreDesigned = new HashSet<>();
    /** The cards drawn for each group, so a click can re-mark the selected one. */
    private final transient Map<Category, List<KitCard>> cardsByCategory = new EnumMap<>(Category.class);

    private transient RosterModel rosterModel;
    private transient JLabel tallyLabel;
    private transient JLabel tallyNote;
    private transient JLabel summaryLabel;
    private transient RoundedJButton issueButton;

    /**
     * Opens the dialog for a selection of personnel and/or units. Units contribute their crews. If nothing in the
     * selection can be issued a kit, a short notice is shown instead.
     *
     * @param parent   the window to center on
     * @param campaign the campaign being played
     * @param people   directly selected personnel, or {@code null}
     * @param units    selected units whose crews should be kitted, or {@code null}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void showFor(JFrame parent, Campaign campaign, Collection<Person> people, Collection<Unit> units) {
        List<Person> gathered = new ArrayList<>(ArmorKitIssuer.gatherPersonnel(people, units));
        if (gathered.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                  getTextAt(RESOURCE_BUNDLE, "empty.message"),
                  getTextAt(RESOURCE_BUNDLE, "empty.title"),
                  JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new IssueArmorKitsDialog(parent, campaign, gathered).setVisible(true);
    }

    private IssueArmorKitsDialog(JFrame parent, Campaign campaign, List<Person> gathered) {
        super(parent, getTextAt(RESOURCE_BUNDLE, "title"), true);
        this.campaign = campaign;
        this.personnel = gathered;

        for (Person person : gathered) {
            byCategory.computeIfAbsent(ArmorKitCatalog.categoryFor(person), key -> new ArrayList<>()).add(person);
        }

        buildUI();
        recalculate();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(scaleForGUI(720, 560));
        pack();
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        int pad = scaleForGUI(6);
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

        content.add(buildHeader(), BorderLayout.NORTH);
        content.add(buildSections(), BorderLayout.CENTER);
        content.add(buildBottom(), BorderLayout.SOUTH);

        setContentPane(content);
    }

    // region Header
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(6),
              scaleForGUI(6),
              scaleForGUI(10),
              scaleForGUI(6)));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(getTextAt(RESOURCE_BUNDLE, "title"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 5f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "header.subtitle", personnel.size()));
        subtitle.setForeground(mutedColor());
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(title);
        left.add(Box.createVerticalStrut(scaleForGUI(2)));
        left.add(subtitle);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel stores = new JLabel(getTextAt(RESOURCE_BUNDLE, "header.stores"));
        stores.setForeground(mutedColor());
        stores.setAlignmentX(Component.RIGHT_ALIGNMENT);
        tallyLabel = new JLabel("0");
        tallyLabel.setFont(tallyLabel.getFont().deriveFont(Font.BOLD, tallyLabel.getFont().getSize2D() + 8f));
        tallyLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        tallyNote = new JLabel(getTextAt(RESOURCE_BUNDLE, "header.tally.unit"));
        tallyNote.setForeground(mutedColor());
        tallyNote.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(stores);
        right.add(tallyLabel);
        right.add(tallyNote);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }
    // endregion Header

    // region Sections
    /** Tab order by unit type: MekWarrior, Vehicle, Aircraft, Soldiers. */
    private static final List<Category> TAB_ORDER = List.of(Category.MEKWARRIOR, Category.INFANTRY,
          Category.AIRCRAFT, Category.SOLDIER);

    private Component buildSections() {
        JTabbedPane tabs = new JTabbedPane();
        // Every unit-type tab is shown, even with nobody of that type selected, so the player sees the options exist.
        for (Category category : TAB_ORDER) {
            List<Person> people = byCategory.getOrDefault(category, List.of());
            String title = getFormattedTextAt(RESOURCE_BUNDLE, "tab.title",
                  getTextAt(RESOURCE_BUNDLE, "section." + category.name()), people.size());
            tabs.addTab(title, buildTab(category, people));
        }
        tabs.setPreferredSize(scaleForGUI(820, 340));
        return tabs;
    }

    private JPanel buildTab(Category category, List<Person> people) {
        JPanel tab = new JPanel(new BorderLayout(0, scaleForGUI(6)));
        tab.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(8), scaleForGUI(6), scaleForGUI(8)));

        if (people.isEmpty()) {
            JLabel notice = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "tab.empty",
                  getTextAt(RESOURCE_BUNDLE, "section." + category.name())), SwingConstants.CENTER);
            notice.setForeground(mutedColor());
            tab.add(notice, BorderLayout.CENTER);
            return tab;
        }

        String hintKey = (category == Category.SOLDIER) ? "soldier.note" : "section.hint";
        JLabel hint = new JLabel(getTextAt(RESOURCE_BUNDLE, hintKey));
        hint.setForeground(mutedColor());
        tab.add(hint, BorderLayout.NORTH);

        List<EquipmentType> kits = new ArrayList<>(ArmorKitCatalog.availableKits(category));
        kits.sort(Comparator.comparing(candidate -> ArmorKitIssuer.unitPrice(candidate, campaign)));

        List<KitCard> cards = new ArrayList<>();
        for (EquipmentType kit : kits) {
            cards.add(new KitCard(category, kit, false, people));
        }
        // Every group can strip to coveralls; soldiers can also return to the platoon's designed armor.
        cards.add(new KitCard(category, null, false, people));
        if (category == Category.SOLDIER) {
            cards.add(new KitCard(category, null, true, people));
        }
        cardsByCategory.put(category, cards);

        FastJScrollPane scroll = new FastJScrollPane(buildTwoRowGrid(cards),
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(null);
        scroll.getHorizontalScrollBar().setUnitIncrement(scaleForGUI(16));
        tab.add(scroll, BorderLayout.CENTER);
        return tab;
    }

    /** Lays the cards out on two rows to use the space, splitting them as evenly as possible. */
    private JPanel buildTwoRowGrid(List<KitCard> cards) {
        int perRow = (cards.size() + 1) / 2;
        JPanel grid = new JPanel();
        grid.setLayout(new BoxLayout(grid, BoxLayout.Y_AXIS));
        grid.add(buildCardRow(cards.subList(0, Math.min(perRow, cards.size()))));
        if (perRow < cards.size()) {
            grid.add(Box.createVerticalStrut(scaleForGUI(10)));
            grid.add(buildCardRow(cards.subList(perRow, cards.size())));
        }
        grid.add(Box.createVerticalGlue());
        return grid;
    }

    private JPanel buildCardRow(List<KitCard> cards) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        boolean first = true;
        for (KitCard card : cards) {
            if (!first) {
                row.add(Box.createHorizontalStrut(scaleForGUI(10)));
            }
            first = false;
            row.add(card);
        }
        row.add(Box.createHorizontalGlue());
        return row;
    }

    private void select(Category category, EquipmentType kit, boolean designed) {
        chosenKit.remove(category);
        stripped.remove(category);
        restoreDesigned.remove(category);
        if (designed) {
            restoreDesigned.add(category);
        } else if (kit == null) {
            stripped.add(category);
        } else {
            chosenKit.put(category, kit);
        }
        for (KitCard card : cardsByCategory.getOrDefault(category, List.of())) {
            card.refreshSelected();
        }
        recalculate();
    }

    private boolean isSelected(Category category, EquipmentType kit, boolean designed) {
        if (designed) {
            return restoreDesigned.contains(category);
        }
        if (kit == null) {
            return stripped.contains(category);
        }
        return kit.equals(chosenKit.get(category));
    }
    // endregion Sections

    // region Bottom (roster + footer)
    private JPanel buildBottom() {
        JPanel bottom = new JPanel(new BorderLayout(0, scaleForGUI(6)));

        rosterModel = new RosterModel();
        JTable table = new JTable(rosterModel);
        table.setRowHeight(scaleForGUI(22));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        FastJScrollPane rosterScroll = new FastJScrollPane(table,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        rosterScroll.setBorder(RoundedLineBorder.createSubtleRoundedLineBorder());
        rosterScroll.setPreferredSize(scaleForGUI(760, 150));
        bottom.add(rosterScroll, BorderLayout.CENTER);

        bottom.add(buildFooter(), BorderLayout.SOUTH);
        return bottom;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(8),
              scaleForGUI(6),
              scaleForGUI(2),
              scaleForGUI(6)));

        summaryLabel = new JLabel();
        summaryLabel.setForeground(mutedColor());
        footer.add(summaryLabel, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, scaleForGUI(8), 0));
        RoundedJButton cancel = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.cancel"));
        cancel.addActionListener(evt -> dispose());
        issueButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.issue"));
        issueButton.addActionListener(evt -> onIssue());
        buttons.add(cancel);
        buttons.add(issueButton);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }
    // endregion Bottom

    // region Actions & totals
    private void recalculate() {
        int fromStores = 0;
        int toProcure = 0;
        Money total = Money.zero();

        for (Map.Entry<Category, List<Person>> entry : byCategory.entrySet()) {
            Category category = entry.getKey();
            if (stripped.contains(category)) {
                continue;
            }
            EquipmentType kit = chosenKit.get(category);
            if (kit == null) {
                continue;
            }
            int quantity = entry.getValue().size();
            int stock = stockFor(entry.getValue(), kit);
            int drawn = Math.min(stock, quantity);
            int ordered = quantity - drawn;
            fromStores += drawn;
            toProcure += ordered;
            total = total.plus(ArmorKitIssuer.unitPrice(kit, campaign).multipliedBy(ordered));
        }

        tallyLabel.setText(total.toAmountString());
        boolean anyChoice = !chosenKit.isEmpty() || !stripped.isEmpty() || !restoreDesigned.isEmpty();
        if (anyChoice) {
            summaryLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "footer.summary",
                  fromStores, toProcure, total.toAmountString()));
        } else {
            summaryLabel.setText(getTextAt(RESOURCE_BUNDLE, "footer.summary.none"));
        }
        issueButton.setEnabled(anyChoice);

        if (rosterModel != null) {
            rosterModel.fireTableDataChanged();
        }
    }

    private void onIssue() {
        Set<Person> changed = new HashSet<>();
        int drawn = 0;
        int ordered = 0;
        int returned = 0;
        int platoons = 0;

        for (Map.Entry<Category, List<Person>> entry : byCategory.entrySet()) {
            Category category = entry.getKey();
            List<Person> people = entry.getValue();

            // Soldiers are kitted at the platoon: the kit is the unit's field armor, not the individual's.
            if (category == Category.SOLDIER) {
                EquipmentType kit = chosenKit.get(category);
                boolean strip = stripped.contains(category);
                boolean designed = restoreDesigned.contains(category);
                if ((kit == null) && !strip && !designed) {
                    continue;
                }
                Set<Unit> handled = new HashSet<>();
                for (Person soldier : people) {
                    Unit unit = soldier.getUnit();
                    if ((unit == null) || !handled.add(unit)) {
                        continue;
                    }
                    if (designed) {
                        ArmorKitIssuer.restorePlatoonDesigned(unit, campaign);
                    } else if (strip) {
                        ArmorKitIssuer.issuePlatoonKit(unit, coveralls(), campaign);
                    } else {
                        ArmorKitIssuer.issuePlatoonKit(unit, kit, campaign);
                    }
                    platoons++;
                }
                continue;
            }

            if (stripped.contains(category)) {
                for (Person person : people) {
                    person.setIntendedArmorKitName(null);
                    if (!ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME.equals(person.getArmorKitName())) {
                        ArmorKitIssuer.strip(person, campaign);
                        changed.add(person);
                        returned++;
                    }
                }
                continue;
            }

            EquipmentType kit = chosenKit.get(category);
            if (kit == null) {
                continue;
            }
            int shortfall = 0;
            for (Person person : people) {
                if (ArmorKitIssuer.issueFromStock(person, kit, campaign)) {
                    person.setIntendedArmorKitName(null);
                    changed.add(person);
                    drawn++;
                } else {
                    // out of stock now — remember what they are meant to wear so it is issued when a kit arrives
                    person.setIntendedArmorKitName(kit.getInternalName());
                    shortfall++;
                }
            }
            if (shortfall > 0) {
                ArmorKitIssuer.order(kit, shortfall, campaign);
                ordered += shortfall;
            }
        }

        Set<Unit> units = new HashSet<>();
        for (Person person : changed) {
            MekHQ.triggerEvent(new PersonChangedEvent(person));
            if (person.getUnit() != null) {
                units.add(person.getUnit());
            }
        }
        for (Unit unit : units) {
            unit.resetPilotAndEntity();
        }

        if ((drawn > 0) || (ordered > 0)) {
            campaign.addReport(DailyReportType.PERSONNEL,
                  getFormattedTextAt(RESOURCE_BUNDLE, "report.issued", drawn + ordered, drawn, ordered));
        }
        if (returned > 0) {
            campaign.addReport(DailyReportType.PERSONNEL,
                  getFormattedTextAt(RESOURCE_BUNDLE, "report.stripped", returned));
        }
        if (platoons > 0) {
            campaign.addReport(DailyReportType.PERSONNEL,
                  getFormattedTextAt(RESOURCE_BUNDLE, "report.platoons", platoons));
        }

        dispose();
    }

    /** Kits stock is per location, so sum across each distinct local warehouse the group draws from. */
    private int stockFor(List<Person> people, EquipmentType kit) {
        Set<LocalWarehouse> counted = new HashSet<>();
        int total = 0;
        for (Person person : people) {
            LocalWarehouse warehouse = person.getWarehouse();
            if ((warehouse != null) && counted.add(warehouse)) {
                total += ArmorKitIssuer.localStock(person, kit);
            }
        }
        return total;
    }
    // endregion Actions & totals

    // region Kit display helpers
    private String kitDisplayName(String internalName) {
        if ((internalName == null) || internalName.equals(ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME)) {
            return getTextAt(RESOURCE_BUNDLE, "roster.coveralls");
        }
        EquipmentType kit = EquipmentType.get(internalName);
        return (kit != null) ? kit.getName() : internalName;
    }

    private static EquipmentType coveralls() {
        return EquipmentType.get(ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME);
    }

    /** How hard a Regular acquirer would find this kit, rendered for the card. */
    private String acquisitionText(EquipmentType kit) {
        TargetRoll target = ArmorKitIssuer.acquisitionTarget(kit, campaign);
        if (target.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            return getTextAt(RESOURCE_BUNDLE, "card.acquire.automatic");
        }
        if (target.cannotSucceed()) {
            return getTextAt(RESOURCE_BUNDLE, "card.acquire.unavailable");
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "card.acquire.tn", target.getValue());
    }

    private static List<String> survivalBadges(EquipmentType kit) {
        List<String> badges = new ArrayList<>();
        boolean combatSuit = kit.hasFlag(MiscTypeFlag.S_COMBAT_SUIT);
        if (kit.hasFlag(MiscTypeFlag.S_SPACE_SUIT) || kit.hasFlag(MiscTypeFlag.S_XCT_VACUUM)) {
            badges.add(getTextAt(RESOURCE_BUNDLE, "badge.vacuum"));
        }
        if (kit.hasFlag(MiscTypeFlag.S_COLD_WEATHER)) {
            badges.add(getTextAt(RESOURCE_BUNDLE, "badge.cold"));
        }
        if (kit.hasFlag(MiscTypeFlag.S_HOT_WEATHER) || combatSuit) {
            badges.add(getTextAt(RESOURCE_BUNDLE, "badge.hot"));
        }
        if (kit.hasFlag(MiscTypeFlag.S_TAINTED_ATMOSPHERE) || combatSuit) {
            badges.add(getTextAt(RESOURCE_BUNDLE, "badge.tainted"));
        }
        if (kit.hasFlag(MiscTypeFlag.S_TOXIC_ATMOSPHERE) || combatSuit) {
            badges.add(getTextAt(RESOURCE_BUNDLE, "badge.toxic"));
        }
        return badges;
    }

    private static Color accentFor(Category category) {
        return switch (category) {
            case MEKWARRIOR -> new Color(0xC0, 0x8A, 0x2A);
            case AIRCRAFT -> new Color(0x2E, 0x88, 0xAB);
            case INFANTRY -> new Color(0x4E, 0x8F, 0x3C);
            case SOLDIER -> new Color(0x9A, 0x6E, 0x4A);
        };
    }

    private static Color mutedColor() {
        Color base = new JLabel().getForeground();
        Color bg = new JPanel().getBackground();
        return blend(base, bg, 0.45f);
    }

    private static Color blend(Color a, Color b, float t) {
        return new Color(
              Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
              Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
              Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    // endregion Kit display helpers

    // region Kit card

    /** One selectable kit tile — a real kit, or (when {@code kit} is null) the "strip to coveralls" tile. */
    private final class KitCard extends JPanel {
        private final Category category;
        private final transient EquipmentType kit;
        /**
         * A real kit has a non-null kit; kit==null and !designed is "strip to coveralls"; designed is "return to
         * designed armor".
         */
        private final boolean designed;
        private final Color accent;
        private final Color baseBackground;
        private final Color defaultForeground;
        private final JPanel band;
        private final JPanel body;
        private final JLabel nameLabel;

        private KitCard(Category category, EquipmentType kit, boolean designed, List<Person> people) {
            super(new BorderLayout());
            this.category = category;
            this.kit = kit;
            this.designed = designed;
            this.accent = accentFor(category);
            this.baseBackground = getBackground();

            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Dimension size = scaleForGUI(250, 168);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setAlignmentY(TOP_ALIGNMENT);

            band = new JPanel();
            band.setPreferredSize(scaleForGUI(1, 6));
            add(band, BorderLayout.NORTH);

            String specialName = designed
                                       ? getTextAt(RESOURCE_BUNDLE, "card.designed.name")
                                       : getTextAt(RESOURCE_BUNDLE, "card.strip.name");
            // Word-wrap the title so long kit names wrap within the card instead of running off the edge.
            nameLabel = new JLabel(wordWrap((kit == null) ? specialName : kit.getName(), 26));
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
            nameLabel.setAlignmentX(LEFT_ALIGNMENT);
            this.defaultForeground = nameLabel.getForeground();

            body = (kit == null) ? buildSpecialBody(designed) : buildKitBody(people);
            add(body, BorderLayout.CENTER);

            refreshSelected();
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    select(category, KitCard.this.kit, KitCard.this.designed);
                }
            });
        }

        private JPanel buildKitBody(List<Person> people) {
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setOpaque(false);
            content.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(8),
                  scaleForGUI(10),
                  scaleForGUI(8),
                  scaleForGUI(10)));
            content.add(nameLabel);

            content.add(Box.createVerticalStrut(scaleForGUI(3)));
            double divisor = (kit instanceof MiscType misc) ? misc.getDamageDivisor() : 1.0;
            String statsText = getFormattedTextAt(RESOURCE_BUNDLE, "card.divisor", formatDivisor(divisor));
            if (kit.hasFlag(MiscTypeFlag.S_ENCUMBERING)) {
                statsText += "  ·  " + getTextAt(RESOURCE_BUNDLE, "card.encumbering");
            }
            JLabel stats = new JLabel(statsText);
            stats.setForeground(mutedColor());
            stats.setFont(stats.getFont().deriveFont(stats.getFont().getSize2D() - 1f));
            stats.setAlignmentX(LEFT_ALIGNMENT);
            content.add(stats);

            content.add(Box.createVerticalStrut(scaleForGUI(2)));
            JLabel acquire = new JLabel(acquisitionText(kit));
            acquire.setForeground(mutedColor());
            acquire.setFont(acquire.getFont().deriveFont(acquire.getFont().getSize2D() - 1f));
            acquire.setAlignmentX(LEFT_ALIGNMENT);
            content.add(acquire);

            content.add(Box.createVerticalStrut(scaleForGUI(5)));
            JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(4), 0));
            badges.setOpaque(false);
            badges.setAlignmentX(LEFT_ALIGNMENT);
            List<String> flags = survivalBadges(kit);
            if (flags.isEmpty()) {
                badges.add(badge(getTextAt(RESOURCE_BUNDLE, "card.noProtection"), mutedColor()));
            } else {
                for (String flag : flags) {
                    badges.add(badge(flag, accent));
                }
            }
            content.add(badges);

            content.add(Box.createVerticalGlue());
            JPanel foot = new JPanel(new BorderLayout());
            foot.setOpaque(false);
            foot.setAlignmentX(LEFT_ALIGNMENT);
            int stock = stockFor(people, kit);
            JLabel stockLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "card.stock", stock));
            if (stock < people.size()) {
                stockLabel.setForeground(new Color(0xC0, 0x70, 0x1F));
            }
            JLabel price = new JLabel(ArmorKitIssuer.unitPrice(kit, campaign).toAmountString()
                                            + " " + getTextAt(RESOURCE_BUNDLE, "card.each"));
            price.setFont(price.getFont().deriveFont(Font.BOLD));
            foot.add(stockLabel, BorderLayout.WEST);
            foot.add(price, BorderLayout.EAST);
            content.add(foot);
            return content;
        }

        private JPanel buildSpecialBody(boolean designed) {
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setOpaque(false);
            content.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(8),
                  scaleForGUI(10),
                  scaleForGUI(8),
                  scaleForGUI(10)));
            String descKey = designed ? "card.designed.desc" : "card.strip.desc";
            JLabel desc = new JLabel(wordWrap(getTextAt(RESOURCE_BUNDLE, descKey), 30));
            desc.setForeground(mutedColor());
            desc.setFont(desc.getFont().deriveFont(desc.getFont().getSize2D() - 1f));
            desc.setAlignmentX(LEFT_ALIGNMENT);
            content.add(nameLabel);
            content.add(Box.createVerticalStrut(scaleForGUI(3)));
            content.add(desc);
            return content;
        }

        private void refreshSelected() {
            boolean selected = isSelected(category, kit, designed);
            if (selected) {
                Color tint = blend(accent, baseBackground, 0.72f);
                setBackground(tint);
                body.setOpaque(true);
                body.setBackground(tint);
                band.setBackground(accent);
                nameLabel.setForeground(accent);
                setBorder(new RoundedLineBorder(accent, scaleForGUI(3), scaleForGUI(16)));
            } else {
                setBackground(baseBackground);
                body.setOpaque(false);
                band.setBackground(kit == null ? mutedColor() : accent);
                nameLabel.setForeground(defaultForeground);
                setBorder(RoundedLineBorder.createSubtleRoundedLineBorder());
            }
            repaint();
        }
    }

    private static JLabel badge(String text, Color accent) {
        JLabel badge = new JLabel(text);
        badge.setFont(badge.getFont().deriveFont(badge.getFont().getSize2D() - 2f));
        badge.setForeground(accent);
        badge.setBorder(BorderFactory.createCompoundBorder(
              new RoundedLineBorder(accent, 1, scaleForGUI(8)),
              BorderFactory.createEmptyBorder(scaleForGUI(1), scaleForGUI(5), scaleForGUI(1), scaleForGUI(5))));
        return badge;
    }

    private static String formatDivisor(double divisor) {
        if (divisor == Math.rint(divisor)) {
            return String.valueOf((int) divisor);
        }
        return String.valueOf(divisor);
    }
    // endregion Kit card

    // region Roster model
    private final class RosterModel extends AbstractTableModel {
        private final String[] columns = {
              getTextAt(RESOURCE_BUNDLE, "roster.col.name"),
              getTextAt(RESOURCE_BUNDLE, "roster.col.assignment"),
              getTextAt(RESOURCE_BUNDLE, "roster.col.group"),
              getTextAt(RESOURCE_BUNDLE, "roster.col.current"),
              getTextAt(RESOURCE_BUNDLE, "roster.col.new")
        };

        @Override
        public int getRowCount() {
            return personnel.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Person person = personnel.get(rowIndex);
            Category category = ArmorKitCatalog.categoryFor(person);
            return switch (columnIndex) {
                case 0 -> person.getFullTitle();
                case 1 -> (person.getUnit() != null)
                                ? person.getUnit().getName()
                                : getTextAt(RESOURCE_BUNDLE, "roster.unassigned");
                case 2 -> getTextAt(RESOURCE_BUNDLE, "section." + category.name());
                case 3 -> currentKitFor(person, category);
                case 4 -> newKitFor(category);
                default -> "";
            };
        }

        private String currentKitFor(Person person, Category category) {
            // A soldier's kit is the platoon's, not their own personal kit.
            if (category == Category.SOLDIER) {
                String unitKit = (person.getUnit() != null) ? person.getUnit().getArmorKitName() : null;
                return (unitKit != null) ? kitDisplayName(unitKit) : getTextAt(RESOURCE_BUNDLE, "roster.designed");
            }
            return kitDisplayName(person.getArmorKitName());
        }

        private String newKitFor(Category category) {
            if (restoreDesigned.contains(category)) {
                return getTextAt(RESOURCE_BUNDLE, "roster.designed");
            }
            if (stripped.contains(category)) {
                return getTextAt(RESOURCE_BUNDLE, "roster.coveralls");
            }
            EquipmentType kit = chosenKit.get(category);
            return (kit != null) ? kit.getName() : getTextAt(RESOURCE_BUNDLE, "roster.unchanged");
        }
    }
    // endregion Roster model
}
