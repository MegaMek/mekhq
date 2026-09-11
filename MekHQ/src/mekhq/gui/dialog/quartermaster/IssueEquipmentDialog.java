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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.rolls.TargetRoll;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
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
import mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog;
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
public class IssueEquipmentDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(IssueEquipmentDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.IssueEquipmentDialog";

    private final transient Campaign campaign;
    private final transient List<Person> personnel;
    private final transient List<Person> toolTechnicians;
    private final transient List<KitIssueSection> sections = new ArrayList<>();
    private final transient Map<Category, List<Person>> byCategory = new EnumMap<>(Category.class);

    /** The kit chosen for a group, or absent if the group is left unchanged. */
    private final transient Map<Category, EquipmentType> chosenKit = new EnumMap<>(Category.class);
    /** Groups the player has chosen to strip back to coveralls. */
    private final transient Set<Category> stripped = new HashSet<>();
    /** Soldier groups the player has chosen to return to their platoons' designed armor. */
    private final transient Set<Category> restoreDesigned = new HashSet<>();
    /** The cards drawn for each group, so a click can re-mark the selected one. */
    private final transient Map<Category, List<KitCard>> cardsByCategory = new EnumMap<>(Category.class);
    /** Per-kit unit price and acquisition-difficulty text, cached so building many cards prices each kit once. */
    private final transient Map<EquipmentType, Money> priceCache = new HashMap<>();
    private final transient Map<EquipmentType, String> acquireTextCache = new HashMap<>();

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
        List<Person> armorPersonnel = new ArrayList<>(ArmorKitIssuer.gatherPersonnel(people, units));
        List<Person> toolTechnicians = gatherTechnicians(people, units);
        if (armorPersonnel.isEmpty() && toolTechnicians.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                  getTextAt(RESOURCE_BUNDLE, "empty.message"),
                  getTextAt(RESOURCE_BUNDLE, "empty.title"),
                  JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        IssueEquipmentDialog dialog = new IssueEquipmentDialog(parent, campaign, armorPersonnel, toolTechnicians);
        dialog.setPreferences(dialog); // Must be before setVisible
        dialog.setVisible(true);
    }

    /** Every selected person (and selected units' crew) who can be issued technician tool kits, without duplicates. */
    private static List<Person> gatherTechnicians(Collection<Person> people, Collection<Unit> units) {
        LinkedHashSet<Person> gathered = new LinkedHashSet<>();
        if (people != null) {
            for (Person person : people) {
                if (EquipmentKitCatalog.canBeIssuedKit(person)) {
                    gathered.add(person);
                }
            }
        }
        if (units != null) {
            for (Unit unit : units) {
                for (Person crew : unit.getCrew()) {
                    if (EquipmentKitCatalog.canBeIssuedKit(crew)) {
                        gathered.add(crew);
                    }
                }
            }
        }
        return new ArrayList<>(gathered);
    }

    private IssueEquipmentDialog(JFrame parent, Campaign campaign, List<Person> armorPersonnel,
          List<Person> toolTechnicians) {
        super(parent, getTextAt(RESOURCE_BUNDLE, "title"), true);
        this.campaign = campaign;
        this.personnel = armorPersonnel;
        this.toolTechnicians = toolTechnicians;

        for (Person person : armorPersonnel) {
            byCategory.computeIfAbsent(ArmorKitCatalog.categoryFor(person), key -> new ArrayList<>()).add(person);
        }

        if (!armorPersonnel.isEmpty()) {
            sections.add(armorSection());
        }
        if (!toolTechnicians.isEmpty()) {
            sections.add(new ToolKitSection(campaign, toolTechnicians, this::recalculate));
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

        JTabbedPane tabs = new JTabbedPane();
        for (KitIssueSection section : sections) {
            tabs.addTab(section.getTitle(), section.getComponent());
        }
        content.add(tabs, BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);

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
        Set<Person> everyone = new HashSet<>(personnel);
        everyone.addAll(toolTechnicians);
        JLabel subtitle = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "header.subtitle", everyone.size()));
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
        kits.sort(Comparator.comparing(this::price));

        // Tally this group's warehouse stock once, rather than rescanning per kit inside each card.
        Map<EquipmentType, Integer> stock = ArmorKitIssuer.localStock(people);

        List<KitCard> cards = new ArrayList<>();
        for (EquipmentType kit : kits) {
            cards.add(buildArmorCard(category, kit, false, people, stock));
        }
        // Every group can strip to coveralls; soldiers can also return to the platoon's designed armor.
        cards.add(buildArmorCard(category, null, false, people, stock));
        if (category == Category.SOLDIER) {
            cards.add(buildArmorCard(category, null, true, people, stock));
        }
        cardsByCategory.put(category, cards);

        FastJScrollPane scroll = new FastJScrollPane(KitCard.grid(cards),
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(scaleForGUI(16));
        tab.add(scroll, BorderLayout.CENTER);
        return tab;
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

    /** The armor kit family as a section: the category cards plus the "what everyone wears" roster. */
    private KitIssueSection armorSection() {
        return new KitIssueSection() {
            @Override
            public String getTitle() {
                return getTextAt(RESOURCE_BUNDLE, "tab.armor");
            }

            @Override
            public JComponent getComponent() {
                JPanel panel = new JPanel(new BorderLayout(0, scaleForGUI(6)));
                panel.add(buildSections(), BorderLayout.CENTER);
                panel.add(buildArmorRoster(), BorderLayout.SOUTH);
                return panel;
            }

            @Override
            public boolean hasPendingChanges() {
                return !chosenKit.isEmpty() || !stripped.isEmpty() || !restoreDesigned.isEmpty();
            }

            @Override
            public Tally computeTally() {
                return armorComputeTally();
            }

            @Override
            public void commit(CommitTotals totals) {
                armorCommit(totals);
            }
        };
    }

    private JComponent buildArmorRoster() {
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
        return rosterScroll;
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
        KitIssueSection.Tally tally = KitIssueSection.Tally.empty();
        boolean anyChoice = false;
        for (KitIssueSection section : sections) {
            tally = tally.plus(section.computeTally());
            anyChoice |= section.hasPendingChanges();
        }

        tallyLabel.setText(tally.cost().toAmountString());
        if (anyChoice) {
            summaryLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "footer.summary",
                  tally.fromStores(), tally.toProcure(), tally.cost().toAmountString()));
        } else {
            summaryLabel.setText(getTextAt(RESOURCE_BUNDLE, "footer.summary.none"));
        }
        issueButton.setEnabled(anyChoice);

        if (rosterModel != null) {
            rosterModel.fireTableDataChanged();
        }
    }

    /** The armor section's footer tally: how many kits come from stores, how many are ordered, and the cost. */
    private KitIssueSection.Tally armorComputeTally() {
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
            total = total.plus(price(kit).multipliedBy(ordered));
        }

        return new KitIssueSection.Tally(fromStores, toProcure, total);
    }

    private void onIssue() {
        KitIssueSection.CommitTotals totals = new KitIssueSection.CommitTotals();
        for (KitIssueSection section : sections) {
            section.commit(totals);
        }

        Set<Unit> units = new HashSet<>();
        for (Person person : totals.changed) {
            MekHQ.triggerEvent(new PersonChangedEvent(person));
            if (person.getUnit() != null) {
                units.add(person.getUnit());
            }
        }
        for (Unit unit : units) {
            unit.resetPilotAndEntity();
        }

        if ((totals.issued > 0) || (totals.ordered > 0)) {
            campaign.addReport(DailyReportType.PERSONNEL,
                  getFormattedTextAt(RESOURCE_BUNDLE, "report.issued",
                        totals.issued + totals.ordered, totals.issued, totals.ordered));
        }
        if (totals.removed > 0) {
            campaign.addReport(DailyReportType.PERSONNEL,
                  getFormattedTextAt(RESOURCE_BUNDLE, "report.stripped", totals.removed));
        }
        if (totals.platoons > 0) {
            campaign.addReport(DailyReportType.PERSONNEL,
                  getFormattedTextAt(RESOURCE_BUNDLE, "report.platoons", totals.platoons));
        }

        dispose();
    }

    /** Applies the armor kit selections, accumulating counts into the shared commit totals. */
    private void armorCommit(KitIssueSection.CommitTotals totals) {
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
                    totals.platoons++;
                }
                continue;
            }

            if (stripped.contains(category)) {
                for (Person person : people) {
                    person.setIntendedArmorKitName(null);
                    if (!ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME.equals(person.getArmorKitName())) {
                        ArmorKitIssuer.strip(person, campaign);
                        totals.changed.add(person);
                        totals.removed++;
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
                    totals.changed.add(person);
                    totals.issued++;
                } else {
                    // out of stock now — remember what they are meant to wear so it is issued when a kit arrives
                    person.setIntendedArmorKitName(kit.getInternalName());
                    shortfall++;
                }
            }
            if (shortfall > 0) {
                ArmorKitIssuer.order(kit, shortfall, campaign);
                totals.ordered += shortfall;
            }
        }
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

    /** The unit price of a kit, cached so repeated card/sort lookups price each kit only once. */
    private Money price(EquipmentType kit) {
        return priceCache.computeIfAbsent(kit, candidate -> ArmorKitIssuer.unitPrice(candidate, campaign));
    }

    /** How hard a Regular acquirer would find this kit, rendered for the card. Cached per kit (see {@link #price}). */
    private String acquisitionText(EquipmentType kit) {
        return acquireTextCache.computeIfAbsent(kit, this::computeAcquisitionText);
    }

    private String computeAcquisitionText(EquipmentType kit) {
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
        return KitCard.mutedColor();
    }

    /**
     * Builds one shared {@link KitCard} for the armor family: a real kit (divisor/encumbrance, acquisition, survival
     * badges, stock and price), or - when {@code kit} is null - the "strip to coveralls" or "return to designed" action
     * tile. Selection and click are wired back to this section's per-category state.
     */
    private KitCard buildArmorCard(Category category, EquipmentType kit, boolean designed, List<Person> people,
          Map<EquipmentType, Integer> stockByKit) {
        Color accent = accentFor(category);
        if (kit == null) {
            String title = getTextAt(RESOURCE_BUNDLE, designed ? "card.designed.name" : "card.strip.name");
            String desc = getTextAt(RESOURCE_BUNDLE, designed ? "card.designed.desc" : "card.strip.desc");
            return new KitCard(accent, title, true, List.of(desc), List.of(), null, false, null,
                  () -> isSelected(category, null, designed),
                  () -> select(category, null, designed));
        }

        double divisor = (kit instanceof MiscType misc) ? misc.getDamageDivisor() : 1.0;
        String stats = getFormattedTextAt(RESOURCE_BUNDLE, "card.divisor", KitCard.formatDivisor(divisor));
        if (kit.hasFlag(MiscTypeFlag.S_ENCUMBERING)) {
            stats += "  ·  " + getTextAt(RESOURCE_BUNDLE, "card.encumbering");
        }
        List<KitCard.Badge> badges = new ArrayList<>();
        List<String> flags = survivalBadges(kit);
        if (flags.isEmpty()) {
            badges.add(new KitCard.Badge(getTextAt(RESOURCE_BUNDLE, "card.noProtection"), false));
        } else {
            for (String flag : flags) {
                badges.add(new KitCard.Badge(flag, true));
            }
        }
        int stock = stockByKit.getOrDefault(kit, 0);
        String priceText = price(kit).toAmountString() + " " + getTextAt(RESOURCE_BUNDLE, "card.each");
        return new KitCard(accent, kit.getName(), false,
              List.of(stats, acquisitionText(kit)), badges,
              getFormattedTextAt(RESOURCE_BUNDLE, "card.stock", stock), stock < people.size(), priceText,
              () -> isSelected(category, kit, false),
              () -> select(category, kit, false));
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

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences(JDialog dialog) {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(IssueEquipmentDialog.class);
            dialog.setName("IssueEquipmentDialog");
            preferences.manage(new JWindowPreference(dialog));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
