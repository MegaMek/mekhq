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
import static mekhq.gui.stratCon.deployment.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog;
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.Category;
import mekhq.campaign.personnel.quartermaster.ArmorKitIssuer;
import mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog;
import mekhq.campaign.personnel.quartermaster.KitSlot;
import mekhq.campaign.unit.Unit;
import mekhq.gui.stratCon.deployment.HudButton;

/**
 * The quartermaster's counter for issuing kits: personal armor kits, and the two equipment-kit slots. It takes a
 * selection of personnel — or units, whose crews stand in for them — sorts them into the groups that draw from the
 * same armor kits, and lets the player pick one kit per group, or one kit per equipment slot for everyone. Kits come
 * out of stores; a shortfall is ordered. Each page ends with a roster of everyone being kitted and what they will carry
 * after the issue, so a bulk issue is legible before it is committed.
 *
 * <p>The dialog is drawn as a heads-up display in the style of the interstellar-map tab, the StratCon deployment
 * wizard, and the contract debrief console (see {@link KitHud}): a command bar with live procurement tiles and a
 * segmented section strip, HUD kit cards, and a footer bar with the HUD buttons.</p>
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
    /** Per-kit unit price and acquisition-difficulty text, computed once and shared with the equipment-kit tabs. */
    private final transient KitPricing pricing;
    /** Each group's kit stock, tallied once when its tab is built, so a click's footer tally does not rescan stores. */
    private final transient Map<Category, Map<EquipmentType, Integer>> stockByCategory = new EnumMap<>(Category.class);

    private transient RosterModel rosterModel;
    private transient KitHud.TabStrip sectionStrip;
    private transient KitHud.StatTile fromStoresTile;
    private transient KitHud.StatTile toProcureTile;
    private transient KitHud.StatTile costTile;
    private transient JLabel summaryLabel;
    private transient HudButton issueButton;

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
        this.pricing = new KitPricing(campaign);

        for (Person person : armorPersonnel) {
            byCategory.computeIfAbsent(ArmorKitCatalog.categoryFor(person), key -> new ArrayList<>()).add(person);
        }

        if (!armorPersonnel.isEmpty()) {
            sections.add(armorSection());
        }
        if (!toolTechnicians.isEmpty()) {
            sections.add(new ToolKitSection(campaign, toolTechnicians, KitSlot.PRIMARY, pricing, this::recalculate));
            sections.add(new ToolKitSection(campaign, toolTechnicians, KitSlot.SECONDARY, pricing,
                  this::recalculate));
        }

        buildUI();
        recalculate();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(scaleForGUI(820, 640));
        getContentPane().setPreferredSize(scaleForGUI(960, 760));
        pack();
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);

        CardLayout pageLayout = new CardLayout();
        JPanel pages = new JPanel(pageLayout);
        pages.setOpaque(true);
        pages.setBackground(GROUND);
        List<String> titles = new ArrayList<>();
        for (int index = 0; index < sections.size(); index++) {
            KitIssueSection section = sections.get(index);
            titles.add(section.getTitle());
            pages.add(section.getComponent(), Integer.toString(index));
        }
        sectionStrip = new KitHud.TabStrip(titles, false, index -> {
            pageLayout.show(pages, Integer.toString(index));
            sectionStrip.setSelected(index);
        });
        sectionStrip.setSelected(0);

        content.add(buildCommandBar(sectionStrip), BorderLayout.NORTH);
        content.add(pages, BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(content);
    }

    // region Command bar
    /**
     * The command bar across the top, in the style of the deployment wizard's: a faint eyebrow, the tracked title and a
     * muted subtitle on the left, the live procurement tiles on the right, and the section strip beneath.
     */
    private JPanel buildCommandBar(KitHud.TabStrip sectionStrip) {
        JPanel commandBar = new JPanel(new BorderLayout(0, scaleForGUI(12)));
        commandBar.setOpaque(true);
        commandBar.setBackground(GROUND);
        commandBar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(12), scaleForGUI(16))));

        JPanel identity = new JPanel();
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.setOpaque(false);

        JLabel eyebrow = new JLabel(getTextAt(RESOURCE_BUNDLE, "window.name").toUpperCase(Locale.ROOT));
        eyebrow.setForeground(TEXT_FAINT);
        eyebrow.setFont(hudFont(Font.BOLD, 0.72f, 0.18f));
        identity.add(KitHud.leftAligned(eyebrow));
        identity.add(Box.createVerticalStrut(scaleForGUI(3)));

        JLabel title = new JLabel(getTextAt(RESOURCE_BUNDLE, "title").toUpperCase(Locale.ROOT));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.35f, 0.16f));
        identity.add(KitHud.leftAligned(title));
        identity.add(Box.createVerticalStrut(scaleForGUI(4)));

        Set<Person> everyone = new HashSet<>(personnel);
        everyone.addAll(toolTechnicians);
        JLabel subtitle = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "header.subtitle", everyone.size()));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        identity.add(KitHud.leftAligned(subtitle));

        fromStoresTile = new KitHud.StatTile(getTextAt(RESOURCE_BUNDLE, "tile.fromStores"),
              getTextAt(RESOURCE_BUNDLE, "tile.fromStores.sub"));
        toProcureTile = new KitHud.StatTile(getTextAt(RESOURCE_BUNDLE, "tile.toProcure"),
              getTextAt(RESOURCE_BUNDLE, "tile.toProcure.sub"));
        costTile = new KitHud.StatTile(getTextAt(RESOURCE_BUNDLE, "tile.cost"),
              getTextAt(RESOURCE_BUNDLE, "tile.cost.sub"));
        JPanel tiles = KitHud.tileRow(fromStoresTile, toProcureTile, costTile);

        JPanel topRow = new JPanel(new BorderLayout(scaleForGUI(24), 0));
        topRow.setOpaque(false);
        topRow.add(identity, BorderLayout.CENTER);
        topRow.add(tiles, BorderLayout.EAST);

        commandBar.add(topRow, BorderLayout.CENTER);
        commandBar.add(sectionStrip, BorderLayout.SOUTH);
        return commandBar;
    }
    // endregion Command bar

    // region Sections
    /** Tab order by unit type: MekWarrior, Vehicle, Aircraft, Soldiers. */
    private static final List<Category> TAB_ORDER = List.of(Category.MEKWARRIOR, Category.INFANTRY,
          Category.AIRCRAFT, Category.SOLDIER);

    /**
     * The armor family's group pages under a compact tab strip. Every unit-type page is offered, even with nobody of
     * that type selected, so the player sees the options exist; the strip opens on the first group that has anyone.
     */
    private JComponent buildSections() {
        CardLayout groupLayout = new CardLayout();
        JPanel groupPages = new JPanel(groupLayout);
        groupPages.setOpaque(true);
        groupPages.setBackground(GROUND);

        List<String> labels = new ArrayList<>();
        int firstPopulated = -1;
        for (int index = 0; index < TAB_ORDER.size(); index++) {
            Category category = TAB_ORDER.get(index);
            List<Person> people = byCategory.getOrDefault(category, List.of());
            labels.add(getFormattedTextAt(RESOURCE_BUNDLE, "tab.title",
                  getTextAt(RESOURCE_BUNDLE, "section." + category.name()), people.size()));
            groupPages.add(buildTab(category, people), category.name());
            if ((firstPopulated < 0) && !people.isEmpty()) {
                firstPopulated = index;
            }
        }
        int opening = Math.max(firstPopulated, 0);

        KitHud.TabStrip[] strip = new KitHud.TabStrip[1];
        strip[0] = new KitHud.TabStrip(labels, true, index -> {
            groupLayout.show(groupPages, TAB_ORDER.get(index).name());
            strip[0].setSelected(index);
        });
        strip[0].setSelected(opening);
        groupLayout.show(groupPages, TAB_ORDER.get(opening).name());

        JPanel panel = new JPanel(new BorderLayout(0, scaleForGUI(10)));
        panel.setOpaque(false);
        panel.add(strip[0], BorderLayout.NORTH);
        panel.add(groupPages, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTab(Category category, List<Person> people) {
        JPanel tab = new JPanel(new BorderLayout(0, scaleForGUI(8)));
        tab.setOpaque(true);
        tab.setBackground(GROUND);

        if (people.isEmpty()) {
            tab.add(KitHud.notice(getFormattedTextAt(RESOURCE_BUNDLE, "tab.empty",
                  getTextAt(RESOURCE_BUNDLE, "section." + category.name()))), BorderLayout.CENTER);
            return tab;
        }

        String hintKey = (category == Category.SOLDIER) ? "soldier.note" : "section.hint";
        tab.add(KitHud.hint(getTextAt(RESOURCE_BUNDLE, hintKey)), BorderLayout.NORTH);

        List<EquipmentType> kits = new ArrayList<>(ArmorKitCatalog.availableKits(category));
        kits.sort(Comparator.comparing(this::price));

        // Tally this group's warehouse stock once, rather than rescanning per kit inside each card.
        Map<EquipmentType, Integer> stock = stockFor(category);

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

        FastJScrollPane scroll = new FastJScrollPane(KitCard.grid(cards, GROUND),
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        KitHud.styleScroll(scroll, GROUND, false);
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

    /** The armor kit family as a section: the group cards plus the "what everyone wears" roster. */
    private KitIssueSection armorSection() {
        return new KitIssueSection() {
            @Override
            public String getTitle() {
                return getTextAt(RESOURCE_BUNDLE, "tab.armor");
            }

            @Override
            public JComponent getComponent() {
                JPanel panel = new JPanel(new BorderLayout(0, scaleForGUI(12)));
                panel.setOpaque(true);
                panel.setBackground(GROUND);
                panel.setBorder(pagePadding());
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

    /** The padding every section page sits inside. */
    static javax.swing.border.Border pagePadding() {
        return BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(12), scaleForGUI(16));
    }

    private JComponent buildArmorRoster() {
        rosterModel = new RosterModel();
        return roster(rosterModel);
    }

    /**
     * A roster block: a "Roster" section heading over a HUD-styled, read-only table of everyone being kitted and what
     * they will carry after the issue.
     */
    static JComponent roster(AbstractTableModel model) {
        JTable table = new JTable(model);
        KitHud.styleTable(table);
        FastJScrollPane scroll = new FastJScrollPane(table,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        KitHud.styleScroll(scroll, SURFACE_DEEP, true);
        scroll.setPreferredSize(scaleForGUI(760, 160));

        JPanel block = new JPanel(new BorderLayout(0, scaleForGUI(8)));
        block.setOpaque(false);
        block.add(KitHud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "heading.roster")), BorderLayout.NORTH);
        block.add(scroll, BorderLayout.CENTER);
        return block;
    }

    /** The footer bar, as the debrief console's: a muted summary on the left, Cancel and Issue on the right. */
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(scaleForGUI(16), 0));
        footer.setOpaque(true);
        footer.setBackground(SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(18), scaleForGUI(12), scaleForGUI(18))));

        summaryLabel = new JLabel();
        summaryLabel.setForeground(TEXT_MUTED);
        summaryLabel.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        footer.add(summaryLabel, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setOpaque(false);
        HudButton cancel = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.cancel").toUpperCase(Locale.ROOT),
              false);
        cancel.addActionListener(evt -> dispose());
        issueButton = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.issue").toUpperCase(Locale.ROOT), true);
        issueButton.addActionListener(evt -> onIssue()); // a disarmed HudButton never fires
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(10)));
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

        fromStoresTile.setValue(Integer.toString(tally.fromStores()), (tally.fromStores() > 0) ? READY : TEXT_MUTED);
        toProcureTile.setValue(Integer.toString(tally.toProcure()), (tally.toProcure() > 0) ? AMBER : TEXT_MUTED);
        costTile.setValue(tally.cost().toAmountString(), tally.cost().isPositive() ? TEXT : TEXT_MUTED);
        if (anyChoice) {
            summaryLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "footer.summary",
                  tally.fromStores(), tally.toProcure(), tally.cost().toAmountString()));
        } else {
            summaryLabel.setText(getTextAt(RESOURCE_BUNDLE, "footer.summary.none"));
        }
        issueButton.setArmed(anyChoice);

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
            int quantity = countLacking(category, entry.getValue(), kit);
            if (quantity == 0) {
                continue;
            }
            int stock = stockFor(category).getOrDefault(kit, 0);
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
                commitCrewStrip(people, campaign, totals);
                continue;
            }

            EquipmentType kit = chosenKit.get(category);
            if (kit != null) {
                commitCrewKit(people, category, kit, campaign, totals);
            }
        }
    }

    /**
     * Strips a group of crew back to coveralls, returning their kits to stores and cancelling any awaited kit.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void commitCrewStrip(List<Person> people, Campaign campaign, KitIssueSection.CommitTotals totals) {
        for (Person person : people) {
            person.setIntendedArmorKitName(null);
            if (!ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME.equals(person.getArmorKitName())) {
                ArmorKitIssuer.strip(person, campaign);
                totals.changed.add(person);
                totals.removed++;
            }
        }
    }

    /**
     * Issues a kit to every member of a (non-soldier) group who does not already wear it: drawn from stores where
     * possible, otherwise remembered as awaited and the shortfall ordered in one go.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void commitCrewKit(List<Person> people, Category category, EquipmentType kit, Campaign campaign,
          KitIssueSection.CommitTotals totals) {
        int shortfall = 0;
        for (Person person : people) {
            if (wearsKit(person, category, kit)) {
                // already wearing it — nothing to draw, order, or report
                person.setIntendedArmorKitName(null);
                continue;
            }
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

    /**
     * How many of a group do not already wear the given kit, and so would draw or order one.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int countLacking(Category category, List<Person> people, EquipmentType kit) {
        int count = 0;
        for (Person person : people) {
            if (!wearsKit(person, category, kit)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Whether a person already wears the given kit. A soldier's kit is their platoon's, so it is read from the unit.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean wearsKit(Person person, Category category, EquipmentType kit) {
        String worn;
        if (category == Category.SOLDIER) {
            worn = (person.getUnit() != null) ? person.getUnit().getArmorKitName() : null;
        } else {
            worn = person.getArmorKitName();
        }
        return kit.getInternalName().equals(worn);
    }

    /**
     * A group's kit stock across the distinct stores it draws from, tallied once (a single pass over each warehouse)
     * and reused for the tab's cards and every later footer tally.
     */
    private Map<EquipmentType, Integer> stockFor(Category category) {
        return stockByCategory.computeIfAbsent(category,
              key -> ArmorKitIssuer.localStock(byCategory.getOrDefault(key, List.of()), campaign));
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

    /** The unit price of a kit (see {@link KitPricing}). */
    private Money price(EquipmentType kit) {
        return pricing.price(kit);
    }

    /** How hard a Regular acquirer would find this kit, rendered for the card (see {@link KitPricing}). */
    private String acquisitionText(EquipmentType kit) {
        return pricing.acquisitionText(kit);
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

    /** The top-band colour identifying each kit group's cards, drawn from the HUD palette. */
    private static Color accentFor(Category category) {
        return switch (category) {
            case MEKWARRIOR -> AMBER;
            case AIRCRAFT -> ACCENT;
            case INFANTRY -> READY;
            case SOLDIER -> TEXT_MUTED;
        };
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
