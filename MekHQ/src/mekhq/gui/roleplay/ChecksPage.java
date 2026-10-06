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
package mekhq.gui.roleplay;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.roleplay.AskPage.column;
import static mekhq.gui.roleplay.AskPage.heading;
import static mekhq.gui.roleplay.AskPage.rightButtonRow;
import static mekhq.gui.roleplay.AskPage.scroll;
import static mekhq.gui.roleplay.OracleConsole.RESOURCE_BUNDLE;
import static mekhq.gui.roleplay.OracleConsole.escape;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.ActionCheckRoll.RollType;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.roleplay.CheckDifficulty;
import mekhq.campaign.roleplay.CheckOdds;
import mekhq.campaign.roleplay.CheckRecord;
import mekhq.campaign.roleplay.CheckTarget;
import mekhq.campaign.roleplay.CheckTrait;
import mekhq.campaign.roleplay.JournalEntry;
import mekhq.campaign.roleplay.NpcRating;
import mekhq.campaign.roleplay.OracleCharacter;
import mekhq.campaign.roleplay.PlotThread;
import mekhq.campaign.roleplay.RoleplayChecks;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCard;
import mekhq.gui.baseComponents.hud.HudCard.Tag;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudSegmentedControl;
import mekhq.gui.baseComponents.hud.HudSegmentedControl.Segment;
import mekhq.gui.baseComponents.hud.HudVerdictBanner;

/**
 * The console's Checks page: skill and attribute checks for one or more people, and opposed checks between two sides.
 * The left column picks who rolls, what they roll and how hard it is; the right column shows the chance of success
 * before the roll, the result after it, and the most recent checks. Every check is logged to the Oracle log and the
 * daily report.
 */
class ChecksPage implements ConsoleSection {
    private static final MMLogger LOGGER = MMLogger.create(ChecksPage.class);

    private static final int RECENT_CHECKS = 5;
    private static final int PREVIEW_PEOPLE = 6;
    private static final int MAXIMUM_OTHER = 6;
    private static final int FLAVOUR_LINES = 50;

    /** What the page is set up to check. */
    enum Mode {
        SKILL, ATTRIBUTE, OPPOSED
    }

    private final OracleConsole console;
    private final RoleplayChecks checks;
    private final JPanel root = new JPanel(new BorderLayout());

    private Mode mode = Mode.SKILL;
    private final HudSegmentedControl<Mode> modeControl;
    private final CardLayout modeLayout = new CardLayout();
    private final JPanel modeCards = Hud.transparentPanel(modeLayout);

    // Who
    private boolean wholeCompany;
    private final HudChip castChip;
    private final HudChip companyChip;
    private final DefaultListModel<Person> peopleModel = new DefaultListModel<>();
    private final JList<Person> peopleList = new JList<>(peopleModel);
    private final JLabel peopleHint = Hud.hint("");
    private final Set<UUID> picked = new LinkedHashSet<>();
    /**
     * People chosen from the Personnel tab who are not active, such as prisoners or the retired. The company list
     * leaves them out, so they are added to it; otherwise the check would quietly roll for someone else.
     */
    private final List<Person> presetOutsiders = new ArrayList<>();
    private boolean updatingPeople;
    /** Everyone the Who list offers, before the name filter hides any. */
    private final List<Person> listedPeople = new ArrayList<>();
    private final JTextField peopleFilter = new JTextField();

    // What
    private final CardLayout traitLayout = new CardLayout();
    private final JPanel traitCards = Hud.transparentPanel(traitLayout);
    private final JTextField skillSearch = new JTextField();
    private final DefaultListModel<String> skillModel = new DefaultListModel<>();
    private final JList<String> skillList = new JList<>(skillModel);
    private final Map<String, String> skillTargets = new LinkedHashMap<>();
    private final Set<String> trainedSkills = new LinkedHashSet<>();
    private String skill;
    private final JComboBox<SkillAttribute> firstAttribute = new JComboBox<>();
    private final JComboBox<Object> secondAttribute = new JComboBox<>();

    // Opposed
    private final OpposedSidePanel acting;
    private final OpposedSidePanel defending;

    // Situation
    private final JPanel situationSection = column();
    private final HudSegmentedControl<CheckDifficulty> difficulty;
    private int other;
    private final JLabel otherValue = new JLabel();
    private final HudCheckBox useEdge;
    private final JTextField reason = new JTextField();
    private final JComboBox<Object> thread = new JComboBox<>();

    // Right column
    private final JLabel preview = new JLabel();
    private final HudButton rollButton;
    private final HudVerdictBanner verdict = new HudVerdictBanner();
    private final JPanel details = column();
    private final JPanel recent = column();

    ChecksPage(final OracleConsole console) {
        this.console = console;
        this.checks = RoleplayChecks.forCampaign(console.campaign(), console.actions());
        root.setOpaque(true);
        root.setBackground(GROUND);

        modeControl = buildModeControl();
        castChip = new HudChip(text("ChecksPage.who.cast"), () -> setWholeCompany(false));
        companyChip = new HudChip(text("ChecksPage.who.company"), () -> setWholeCompany(true));
        acting = new OpposedSidePanel(this, checks, false);
        defending = new OpposedSidePanel(this, checks, true);
        difficulty = buildDifficulty();
        useEdge = buildUseEdge();
        rollButton = new HudButton(text("ChecksPage.roll").toUpperCase(Locale.ROOT), true);
        rollButton.addActionListener(event -> roll());

        traitCards.add(topAligned(buildSkillCard()), Mode.SKILL.name());
        traitCards.add(topAligned(buildAttributeCard()), Mode.ATTRIBUTE.name());
        // The cards share the height of the tallest, so each sits at the top of its own holder.
        modeCards.add(topAligned(buildGroupCard()), "GROUP");
        modeCards.add(topAligned(buildOpposedCard()), Mode.OPPOSED.name());
        buildSituation();

        JScrollPane leftScroll = scroll(buildLeftColumn(), GROUND);
        leftScroll.setPreferredSize(new Dimension(scaleForGUI(440), 0));
        root.add(leftScroll, BorderLayout.WEST);
        root.add(scroll(buildRightColumn(), SURFACE_DEEP), BorderLayout.CENTER);

        setWholeCompanyChips(false);
    }

    // region Building

    private HudSegmentedControl<Mode> buildModeControl() {
        HudSegmentedControl<Mode> control = new HudSegmentedControl<>(this::setMode);
        control.setColumns(Mode.values().length);
        List<Segment<Mode>> modes = new ArrayList<>();
        for (Mode value : Mode.values()) {
            modes.add(new Segment<>(value, text("ChecksPage.mode." + value.name()),
                  text("ChecksPage.mode." + value.name() + ".sub"), true));
        }
        control.setSegments(modes);
        control.setSelected(Mode.SKILL);
        return control;
    }

    /** The chips choosing cast or whole company, a name filter, and the list of people to tick. */
    private JComponent buildWhoSection() {
        JPanel chips = Hud.transparentPanel(null);
        chips.setLayout(new BoxLayout(chips, BoxLayout.X_AXIS));
        chips.add(castChip);
        chips.add(Box.createHorizontalStrut(scaleForGUI(6)));
        chips.add(companyChip);

        peopleList.setSelectionModel(new ToggleSelectionModel());
        peopleList.setBackground(SURFACE_DEEP);
        peopleList.setCellRenderer(new PersonRenderer());
        AskPage.useFixedCardRows(peopleList);
        peopleList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !updatingPeople) {
                readPicks();
            }
        });
        JScrollPane peopleScroll = new JScrollPane(peopleList);
        Hud.styleScroll(peopleScroll, SURFACE_DEEP, true);
        peopleScroll.setPreferredSize(new Dimension(scaleForGUI(360), scaleForGUI(190)));
        peopleScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, scaleForGUI(190)));

        Hud.styleField(peopleFilter);
        peopleFilter.setToolTipText(text("ChecksPage.who.filter.toolTipText"));
        peopleFilter.getDocument().addDocumentListener(onChange(this::showPeople));

        JPanel section = column();
        section.add(leftAligned(chips));
        section.add(Box.createVerticalStrut(scaleForGUI(6)));
        section.add(leftAligned(peopleFilter));
        section.add(Box.createVerticalStrut(scaleForGUI(6)));
        section.add(leftAligned(peopleScroll));
        section.add(Box.createVerticalStrut(scaleForGUI(4)));
        section.add(leftAligned(peopleHint));
        return leftAligned(section);
    }

    private JPanel buildSkillCard() {
        Hud.styleField(skillSearch);
        skillSearch.setToolTipText(text("ChecksPage.skill.search.toolTipText"));
        skillSearch.getDocument().addDocumentListener(onChange(this::fillSkills));
        skillList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        skillList.setBackground(SURFACE_DEEP);
        skillList.setCellRenderer(new SkillRenderer());
        skillList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && skillList.getSelectedValue() != null
                      && !skillList.getSelectedValue().equals(skill)) {
                skill = skillList.getSelectedValue();
                peopleList.repaint();
                refreshPreview();
            }
        });
        JScrollPane skillScroll = new JScrollPane(skillList);
        Hud.styleScroll(skillScroll, SURFACE_DEEP, true);
        skillScroll.setPreferredSize(new Dimension(scaleForGUI(360), scaleForGUI(170)));
        skillScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, scaleForGUI(170)));
        JPanel skillCard = column();
        skillCard.add(leftAligned(skillSearch));
        skillCard.add(Box.createVerticalStrut(scaleForGUI(6)));
        skillCard.add(leftAligned(skillScroll));
        skillCard.add(Box.createVerticalStrut(scaleForGUI(4)));
        skillCard.add(leftAligned(hint(text("ChecksPage.skill.hint"))));
        return skillCard;
    }

    private JPanel buildAttributeCard() {
        firstAttribute.setModel(new DefaultComboBoxModel<>(checkableAttributes().toArray(new SkillAttribute[0])));
        Hud.styleComboBox(firstAttribute);
        Hud.styleComboBox(secondAttribute);
        firstAttribute.setRenderer(labelRenderer());
        secondAttribute.setRenderer(labelRenderer());
        firstAttribute.addActionListener(event -> {
            fillSecondAttribute();
            peopleList.repaint();
            refreshPreview();
        });
        secondAttribute.addActionListener(event -> {
            peopleList.repaint();
            refreshPreview();
        });
        fillSecondAttribute();
        JPanel attributeRow = Hud.transparentPanel(new GridLayout(1, 2, scaleForGUI(6), 0));
        attributeRow.add(firstAttribute);
        attributeRow.add(secondAttribute);
        JPanel attributeCard = column();
        attributeCard.add(leftAligned(attributeRow));
        attributeCard.add(Box.createVerticalStrut(scaleForGUI(4)));
        attributeCard.add(leftAligned(hint(text("ChecksPage.attribute.hint"))));
        return attributeCard;
    }

    /** Who rolls and what they roll, for skill and attribute checks. */
    private JPanel buildGroupCard() {
        JPanel groupCard = column();
        groupCard.add(heading("ChecksPage.who", "ChecksPage.who.help"));
        groupCard.add(Box.createVerticalStrut(scaleForGUI(6)));
        groupCard.add(buildWhoSection());
                groupCard.add(Box.createVerticalStrut(scaleForGUI(14)));
        groupCard.add(heading("ChecksPage.what", "ChecksPage.what.help"));
        groupCard.add(Box.createVerticalStrut(scaleForGUI(6)));
        groupCard.add(leftAligned(traitCards));
        return groupCard;
    }

    private JPanel buildOpposedCard() {
        JPanel opposedCard = column();
        opposedCard.add(heading("ChecksPage.opposed.acting", "ChecksPage.opposed.acting.help"));
        opposedCard.add(Box.createVerticalStrut(scaleForGUI(6)));
        opposedCard.add(leftAligned(acting.getPanel()));
        opposedCard.add(Box.createVerticalStrut(scaleForGUI(14)));
        opposedCard.add(heading("ChecksPage.opposed.defending", "ChecksPage.opposed.defending.help"));
        opposedCard.add(Box.createVerticalStrut(scaleForGUI(6)));
        opposedCard.add(leftAligned(defending.getPanel()));
        return opposedCard;
    }

    private HudSegmentedControl<CheckDifficulty> buildDifficulty() {
        HudSegmentedControl<CheckDifficulty> control = new HudSegmentedControl<>(value -> refreshPreview());
        control.setColumns(CheckDifficulty.values().length);
        List<Segment<CheckDifficulty>> levels = new ArrayList<>();
        for (CheckDifficulty value : CheckDifficulty.values()) {
            levels.add(new Segment<>(value, value.getLabel(), value.getSignedModifier(), true));
        }
        control.setSegments(levels);
        control.setSelected(CheckDifficulty.NORMAL);
        return control;
    }

    private HudCheckBox buildUseEdge() {
        HudCheckBox checkBox = new HudCheckBox(text("ChecksPage.edge"));
        // Off by default: Edge is a limited resource, so spending it is the player's choice.
        checkBox.setSelected(false);
        checkBox.setToolTipText(text("ChecksPage.edge.toolTipText"));
        checkBox.addActionListener(event -> refreshPreview());
        return checkBox;
    }

    /** How hard the situation is: the difficulty, any other modifier and whether to spend Edge. */
    private void buildSituation() {
        HudButton otherDown = new HudButton(OracleConsole.symbol("minus"), false, true);
        otherDown.setToolTipText(text("ChecksPage.other.down"));
        otherDown.addActionListener(event -> changeOther(-1));
        HudButton otherUp = new HudButton("+", false, true);
        otherUp.setToolTipText(text("ChecksPage.other.up"));
        otherUp.addActionListener(event -> changeOther(1));
        otherValue.setForeground(TEXT);
        otherValue.setFont(hudFont(Font.BOLD, 1.0f, 0.0f));
        otherValue.setHorizontalAlignment(JLabel.CENTER);
        otherValue.setPreferredSize(new Dimension(scaleForGUI(34), otherValue.getPreferredSize().height));
        JLabel otherLabel = Hud.hint(text("ChecksPage.other"));
        otherLabel.setToolTipText(text("ChecksPage.other.toolTipText"));
        JPanel otherRow = Hud.transparentPanel(null);
        otherRow.setLayout(new BoxLayout(otherRow, BoxLayout.X_AXIS));
        otherRow.add(otherLabel);
        otherRow.add(Box.createHorizontalStrut(scaleForGUI(8)));
        otherRow.add(otherDown);
        otherRow.add(otherValue);
        otherRow.add(otherUp);
        otherRow.add(Box.createHorizontalGlue());
        refreshOther();

        situationSection.add(heading("ChecksPage.situation", "ChecksPage.situation.help"));
        situationSection.add(Box.createVerticalStrut(scaleForGUI(6)));
        situationSection.add(leftAligned(difficulty));
        situationSection.add(Box.createVerticalStrut(scaleForGUI(8)));
        situationSection.add(leftAligned(otherRow));
        situationSection.add(Box.createVerticalStrut(scaleForGUI(6)));
        situationSection.add(leftAligned(useEdge));

    }

    /** The mode, the chosen mode's controls, the situation, the reason and the dice roller. */
    private JPanel buildLeftColumn() {
        Hud.styleField(reason);
        reason.setToolTipText(text("ChecksPage.reason.toolTipText"));
        reason.addActionListener(event -> roll());
        Hud.styleComboBox(thread);
        thread.setRenderer(labelRenderer());
        thread.setToolTipText(text("ChecksPage.thread.toolTipText"));

        JPanel left = column();
        left.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(14),
              scaleForGUI(16)));
        left.add(leftAligned(modeControl));
        left.add(Box.createVerticalStrut(scaleForGUI(14)));
        left.add(leftAligned(modeCards));
        left.add(Box.createVerticalStrut(scaleForGUI(14)));
        left.add(leftAligned(situationSection));
        left.add(Box.createVerticalStrut(scaleForGUI(14)));
        left.add(heading("ChecksPage.reason", "ChecksPage.reason.help"));
        left.add(Box.createVerticalStrut(scaleForGUI(6)));
        left.add(leftAligned(reason));
        left.add(Box.createVerticalStrut(scaleForGUI(6)));
        left.add(leftAligned(thread));
        left.add(Box.createVerticalStrut(scaleForGUI(20)));
        left.add(leftAligned(new DiceRoller(console).getPanel()));
        return left;
    }

    /** The preview and Roll button, the result and recent checks. */
    private JPanel buildRightColumn() {
        preview.setForeground(TEXT_MUTED);
        preview.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        JPanel previewBox = Hud.transparentPanel(new BorderLayout());
        previewBox.setOpaque(true);
        previewBox.setBackground(SURFACE);
        previewBox.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER,
              scaleForGUI(1)), BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(10),
              scaleForGUI(12))));
        previewBox.add(preview, BorderLayout.CENTER);

        JPanel right = column();
        right.setOpaque(true);
        right.setBackground(SURFACE_DEEP);
        right.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(14), scaleForGUI(16))));
        right.add(leftAligned(Hud.sectionHeading(text("ChecksPage.preview"))));
        right.add(Box.createVerticalStrut(scaleForGUI(8)));
        right.add(leftAligned(previewBox));
        right.add(Box.createVerticalStrut(scaleForGUI(8)));
        right.add(leftAligned(rightButtonRow(null, rollButton)));
        right.add(Box.createVerticalStrut(scaleForGUI(14)));
        right.add(leftAligned(Hud.sectionHeading(text("ChecksPage.result"))));
        right.add(Box.createVerticalStrut(scaleForGUI(8)));
        right.add(leftAligned(verdict));
        right.add(Box.createVerticalStrut(scaleForGUI(8)));
        right.add(leftAligned(details));
        right.add(Box.createVerticalStrut(scaleForGUI(14)));
        right.add(leftAligned(Hud.sectionHeading(text("ChecksPage.recent"))));
        right.add(Box.createVerticalStrut(scaleForGUI(8)));
        right.add(leftAligned(recent));
        verdict.setVerdict(text("ChecksPage.waiting"), text("ChecksPage.waiting.reason"),
              OracleConsole.symbol("none"), ACCENT);
        return right;
    }

    /** @return a listener that runs {@code action} whenever a text field's contents change */
    private static DocumentListener onChange(final Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                action.run();
            }
        };
    }

    // endregion Building

    @Override
    public JComponent getComponent() {
        return root;
    }

    @Override
    public void refresh() {
        fillPeople();
        fillSkills();
        acting.fill();
        defending.fill();
        fillThreads();
        useEdge.setVisible(checks.isEdgeAllowed());
        refreshPreview();
        refreshRecent();
    }

    // region Presets

    /**
     * Sets the page up to check the given people.
     *
     * @param people    the people to tick
     * @param attribute {@code true} for an attribute check, {@code false} for a skill check
     */
    void preset(final List<Person> people, final boolean attribute) {
        picked.clear();
        presetOutsiders.clear();
        final List<Person> company = companyPeople();
        boolean allInCast = true;
        for (Person person : people) {
            picked.add(person.getId());
            if (!company.contains(person)) {
                presetOutsiders.add(person);
            }
            allInCast &= castMember(person.getId()) != null;
        }
        setWholeCompanyChips(!allInCast);
        modeControl.setSelected(attribute ? Mode.ATTRIBUTE : Mode.SKILL);
        setMode(attribute ? Mode.ATTRIBUTE : Mode.SKILL);
    }

    /**
     * Sets the page up for an opposed check with a cast member as the defending side.
     *
     * @param character the cast member
     */
    void presetOpposed(final OracleCharacter character) {
        modeControl.setSelected(Mode.OPPOSED);
        setMode(Mode.OPPOSED);
        defending.fill();
        defending.choose(character.getId(), character.getPersonId());
        // The acting side starts on the first entry, which may be this same character.
        acting.chooseOtherThan(defending);
        refreshPreview();
    }

    // endregion Presets

    // region Mode and who

    private void setMode(final Mode newMode) {
        mode = newMode;
        modeLayout.show(modeCards, newMode == Mode.OPPOSED ? Mode.OPPOSED.name() : "GROUP");
        if (newMode != Mode.OPPOSED) {
            traitLayout.show(traitCards, newMode.name());
        }
        // An opposed check has a situation for each side, so the shared one is hidden.
        situationSection.setVisible(newMode != Mode.OPPOSED);
        rollButton.setText(text(newMode == Mode.OPPOSED ? "ChecksPage.roll.opposed" : "ChecksPage.roll")
                                 .toUpperCase(Locale.ROOT));
        refresh();
    }

    private void setWholeCompany(final boolean company) {
        setWholeCompanyChips(company);
        refresh();
    }

    private void setWholeCompanyChips(final boolean company) {
        wholeCompany = company;
        castChip.setActive(!company);
        companyChip.setActive(company);
    }

    private void fillPeople() {
        List<Person> listed = new ArrayList<>();
        if (wholeCompany) {
            listed.addAll(companyPeople());
            listed.addAll(presetOutsiders);
        } else {
            for (OracleCharacter character : console.roleplay().getActiveCharacters()) {
                Person person = personOf(character);
                if (person != null) {
                    listed.add(person);
                }
            }
        }
        listedPeople.clear();
        listedPeople.addAll(listed);
        // People who are ticked but no longer listed stay ticked only if they are still in the list.
        int before = picked.size();
        picked.retainAll(listed.stream().map(Person::getId).toList());
        if (picked.size() < before) {
            LOGGER.debug("[OracleConsole] {} picked people are no longer listed and were unticked",
                  before - picked.size());
        }
        if (picked.isEmpty() && !listed.isEmpty()) {
            picked.add(listed.get(0).getId());
        }
        showPeople();
        peopleHint.setText(listed.isEmpty() ? text(wholeCompany ? "ChecksPage.who.noneCompany"
                                                           : "ChecksPage.who.noneCast")
                                 : getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.who.count", picked.size()));
    }

    /**
     * Shows the listed people whose names match the filter. Filtering only hides rows: anyone ticked stays ticked
     * and still rolls.
     */
    private void showPeople() {
        String filter = peopleFilter.getText().strip().toLowerCase(Locale.ROOT);
        List<Person> shown = listedPeople.stream()
                                   .filter(person -> filter.isEmpty()
                                                           || person.getFullName().toLowerCase(Locale.ROOT)
                                                                    .contains(filter))
                                   .toList();
        updatingPeople = true;
        try {
            peopleModel.clear();
            peopleModel.addAll(shown);
            peopleList.clearSelection();
            for (int index = 0; index < shown.size(); index++) {
                if (picked.contains(shown.get(index).getId())) {
                    peopleList.getSelectionModel().addSelectionInterval(index, index);
                }
            }
        } finally {
            updatingPeople = false;
        }
    }

    private void readPicks() {
        // Only the rows on show can change; people hidden by the filter keep their ticks.
        for (int index = 0; index < peopleModel.size(); index++) {
            UUID id = peopleModel.get(index).getId();
            if (peopleList.isSelectedIndex(index)) {
                picked.add(id);
            } else {
                picked.remove(id);
            }
        }
        peopleHint.setText(getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.who.count", picked.size()));
        fillSkills();
        refreshPreview();
    }

    private List<Person> pickedPeople() {
        return listedPeople.stream().filter(person -> picked.contains(person.getId())).toList();
    }

    /** @return the console the page belongs to */
    OracleConsole console() {
        return console;
    }

    /** @return the company's active people, by name */
    List<Person> companyPeople() {
        List<Person> people = new ArrayList<>(console.campaign().getPlayerForce().getHumanResources()
                                                    .getActivePersonnel(false, false));
        people.sort(Comparator.comparing(Person::getFullName, String.CASE_INSENSITIVE_ORDER));
        return people;
    }

    /** @return the person a cast member is linked to, or {@code null} */
    @Nullable Person personOf(final @Nullable OracleCharacter character) {
        if (character == null || !character.isLinked()) {
            return null;
        }
        return console.campaign().getPlayerForce().getHumanResources().getPerson(character.getPersonId());
    }

    private @Nullable OracleCharacter castMember(final UUID personId) {
        for (OracleCharacter character : console.roleplay().getActiveCharacters()) {
            if (personId.equals(character.getPersonId())) {
                return character;
            }
        }
        return null;
    }

    // endregion Mode and who

    // region What

    static List<SkillAttribute> checkableAttributes() {
        return List.of(SkillAttribute.BODY, SkillAttribute.CHARISMA, SkillAttribute.DEXTERITY,
              SkillAttribute.INTELLIGENCE, SkillAttribute.REFLEXES, SkillAttribute.STRENGTH, SkillAttribute.WILLPOWER);
    }

    private void fillSecondAttribute() {
        SkillAttribute first = (SkillAttribute) firstAttribute.getSelectedItem();
        Object previous = secondAttribute.getSelectedItem();
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        model.addElement(text("ChecksPage.attribute.none"));
        for (SkillAttribute attribute : checkableAttributes()) {
            if (attribute != first) {
                model.addElement(attribute);
            }
        }
        secondAttribute.setModel(model);
        if (previous instanceof SkillAttribute && previous != first) {
            secondAttribute.setSelectedItem(previous);
        }
    }

    private void fillSkills() {
        List<Person> people = pickedPeople();
        String query = skillSearch.getText().strip().toLowerCase(Locale.ROOT);
        skillTargets.clear();
        trainedSkills.clear();
        List<String> trained = new ArrayList<>();
        List<String> untrained = new ArrayList<>();
        for (String name : SkillType.getSkillList()) {
            boolean isTrained = people.stream().anyMatch(person -> person.hasSkill(name));
            if (isTrained) {
                trainedSkills.add(name);
            }
            if (!query.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            (isTrained ? trained : untrained).add(name);
        }
        trained.sort(String.CASE_INSENSITIVE_ORDER);
        untrained.sort(String.CASE_INSENSITIVE_ORDER);
        List<String> shown = new ArrayList<>(trained);
        shown.addAll(untrained);
        for (String name : shown) {
            skillTargets.put(name, describeTargets(people, CheckTrait.skill(name), 0));
        }
        String keep = skill;
        skillModel.clear();
        skillModel.addAll(shown);
        if (keep == null || !shown.contains(keep)) {
            keep = trained.isEmpty() ? (shown.isEmpty() ? keep : shown.get(0)) : trained.get(0);
        }
        skill = keep;
        if (keep != null && shown.contains(keep)) {
            skillList.setSelectedValue(keep, true);
        }
    }

    private String describeTargets(final List<Person> people, final CheckTrait trait, final int modifier) {
        List<String> targets = new ArrayList<>();
        for (Person person : people.subList(0, Math.min(3, people.size()))) {
            targets.add(checks.target(person, trait, modifier).describe());
        }
        return String.join(" / ", targets) + (people.size() > 3 ? " " + OracleConsole.symbol("more") : "");
    }

    private @Nullable CheckTrait currentTrait() {
        if (mode == Mode.SKILL) {
            return (skill == null) ? null : CheckTrait.skill(skill);
        }
        SkillAttribute first = (SkillAttribute) firstAttribute.getSelectedItem();
        if (first == null) {
            return null;
        }
        Object second = secondAttribute.getSelectedItem();
        return CheckTrait.attributes(first, (second instanceof SkillAttribute attribute) ? attribute : null);
    }

    // endregion What

    // region Situation

    private void changeOther(final int delta) {
        other = Math.clamp(other + delta, -MAXIMUM_OTHER, MAXIMUM_OTHER);
        refreshOther();
        refreshPreview();
    }

    private void refreshOther() {
        otherValue.setText(CheckDifficulty.signed(other));
    }

    private CheckDifficulty currentDifficulty() {
        return Objects.requireNonNullElse(difficulty.getSelected(), CheckDifficulty.NORMAL);
    }

    private void fillThreads() {
        Object previous = thread.getSelectedItem();
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        model.addElement(text("ChecksPage.thread.none"));
        for (PlotThread plotThread : console.roleplay().getPlotThreads()) {
            if (!plotThread.isComplete()) {
                model.addElement(plotThread);
            }
        }
        thread.setModel(model);
        if (previous instanceof PlotThread && model.getIndexOf(previous) >= 0) {
            thread.setSelectedItem(previous);
        }
    }

    private @Nullable PlotThread currentThread() {
        return (thread.getSelectedItem() instanceof PlotThread plotThread) ? plotThread : null;
    }

    // endregion Situation

    // region Preview

    /** Recomputes the chances shown and whether Roll is ready. */
    void refreshPreview() {
        List<String> lines = new ArrayList<>();
        boolean ready;
        if (mode == Mode.OPPOSED) {
            boolean sameSide = acting.isReady() && defending.isReady() && acting.isSameAs(defending);
            ready = acting.isReady() && defending.isReady() && !sameSide;
            if (sameSide) {
                LOGGER.debug("[OracleConsole] Opposed check not armed: both sides are {}", acting.name());
                lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.preview.sameSide", escape(acting.name())));
            } else if (ready) {
                CheckTarget actingTarget = acting.target();
                CheckTarget defendingTarget = defending.target();
                double chance = CheckOdds.opposedWinChance(actingTarget, acting.rerolls(), defendingTarget,
                      defending.rerolls());
                lines.add(big(actingTarget.describe()) + "  " + escape(acting.name()) + "   "
                                + big(defendingTarget.describe()) + "  " + escape(defending.name()));
                lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.preview.opposed", escape(acting.name()),
                      percent(chance), hex(READY)));
                lines.add(faint(text("ChecksPage.preview.opposedRules")));
            } else {
                lines.add(text("ChecksPage.preview.pickSides"));
            }
        } else {
            List<Person> people = pickedPeople();
            CheckTrait trait = currentTrait();
            ready = !people.isEmpty() && trait != null;
            if (ready) {
                int modifier = currentDifficulty().getModifier() + other;
                for (Person person : people.subList(0, Math.min(PREVIEW_PEOPLE, people.size()))) {
                    CheckTarget target = checks.target(person, trait, modifier);
                    boolean edge = useEdge.isSelected() && checks.canUseEdge(person);
                    String line = big(target.describe()) + "  " + OracleConsole.joined(escape(person.getFullName()),
                          getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.preview.chance",
                                percent(CheckOdds.chance(target, edge)), hex(READY)));
                    List<String> notes = new ArrayList<>();
                    if (target.rollType() != RollType.NORMAL) {
                        notes.add(text("ChecksPage.preview.aptitude"));
                    }
                    if (!RoleplayChecks.isTrained(person, trait)) {
                        notes.add(text("ChecksPage.preview.untrained"));
                    }
                    if (checks.isEdgeAllowed()) {
                        notes.add(getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.preview.edge",
                              person.getCurrentEdge()));
                    }
                    lines.add(line + (notes.isEmpty() ? "" : "  " + faint(OracleConsole.joined(notes))));
                }
                if (people.size() > PREVIEW_PEOPLE) {
                    lines.add(faint(getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.preview.more",
                          people.size() - PREVIEW_PEOPLE)));
                }
                lines.add(faint(getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.preview.modifiers",
                      trait.getLabel(), currentDifficulty().getLabel(), currentDifficulty().getSignedModifier(),
                      CheckDifficulty.signed(other))));
            } else {
                lines.add(text(people.isEmpty() ? "ChecksPage.preview.pickPeople" : "ChecksPage.preview.pickTrait"));
            }
        }
        preview.setText("<html><div style='width:" + scaleForGUI(380) + "px'>" + String.join("<br>", lines)
                              + "</div></html>");
        if (!ready) {
            LOGGER.debug("[OracleConsole] Roll not armed in {} mode: {}", mode, String.join(" / ", lines));
        }
        rollButton.setArmed(ready);
    }

    private static String big(final String text) {
        return "<b><font color='" + hex(ACCENT_BRIGHT) + "' size='+1'>" + escape(text) + "</font></b>";
    }

    private static String faint(final String text) {
        return "<font color='" + hex(TEXT_FAINT) + "'>" + text + "</font>";
    }

    static String percent(final double chance) {
        return Math.round(chance * 100) + "%";
    }

    // endregion Preview

    // region Rolling and results

    private void roll() {
        if (!rollButton.isArmed()) {
            return;
        }
        CheckRecord record;
        if (mode == Mode.OPPOSED) {
            record = checks.opposed(acting.opponent(), defending.opponent(), reason.getText(),
                  console.roleplay().getCharacters(), currentThread());
        } else {
            CheckTrait trait = currentTrait();
            if (trait == null) {
                return;
            }
            record = checks.check(pickedPeople(), trait, currentDifficulty(), other, useEdge.isSelected(),
                  reason.getText(), console.roleplay().getCharacters(), currentThread());
        }
        showResult(record, true);
        console.changed();
    }

    private void showResult(final CheckRecord record, final boolean fresh) {
        List<CheckRecord.Side> sides = record.sides();
        String because = record.reason().isBlank() ? ""
                               : "<i>" + escape(OracleConsole.quoted(record.reason())) + "</i><br>";
        if (record.opposed() && sides.size() == 2) {
            CheckRecord.Side winner = sides.get(0).won() ? sides.get(0) : sides.get(1);
            int difference = record.winningDifference();
            String why = (difference == 0) ? text("ChecksPage.result.tie")
                               : getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.result.by", difference);
            verdict.setVerdict(getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.result.wins", winner.name()),
                  "<html>" + because + escape(why) + "</html>", getFormattedTextAt(RESOURCE_BUNDLE,
                        "ChecksPage.result.badge.margin", difference), sides.get(0).won() ? READY : AMBER);
        } else if (sides.size() == 1) {
            CheckRecord.Side side = sides.get(0);
            verdict.setVerdict(verdictWord(side.margin()), "<html>" + because + escape(OracleConsole.joined(side.name(),
                                                                                                   side.action()))
                                                                   + "</html>",
                  getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.result.badge.roll", side.roll()),
                  colorFor(side.margin()));
        } else {
            int won = record.countWon();
            Color color = (won == sides.size()) ? READY : (won == 0) ? DANGER : AMBER;
            verdict.setVerdict(getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.result.group", won, sides.size()),
                  "<html>" + because + escape(sides.get(0).action()) + "</html>",
                  won + "/" + sides.size(), color);
        }

        details.removeAll();
        for (CheckRecord.Side side : sides) {
            details.add(leftAligned(sideCard(side, record.opposed())));
        }
        if (fresh && sides.size() == 1 && !record.opposed()) {
            details.add(Box.createVerticalStrut(scaleForGUI(8)));
            details.add(leftAligned(AskPage.wrapped(text("ChecksPage.flavour." + randomInt(FLAVOUR_LINES)),
                  TEXT_MUTED)));
        }
        details.revalidate();
        details.repaint();
    }

    private static HudCard sideCard(final CheckRecord.Side side, final boolean opposed) {
        List<String> dice = side.dice().stream().map(String::valueOf).toList();
        String sub = getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.result.side", OracleConsole.joined(dice),
              side.roll(), side.target());
        if (side.usedEdge()) {
            sub = OracleConsole.joined(sub, text("ChecksPage.result.edge"));
        }
        String word = opposed ? text(side.won() ? "ChecksPage.result.won" : "ChecksPage.result.lost")
                            : verdictWord(side.margin());
        Color color = opposed ? (side.won() ? READY : AMBER) : colorFor(side.margin());
        return new HudCard().show(color, true, side.name(), side.action(), List.of(new Tag(word, color)), sub,
              CheckDifficulty.signed(side.margin()), text("ChecksPage.result.margin"), false);
    }

    static String verdictWord(final int margin) {
        return text(margin >= 4 ? "ChecksPage.verdict.outstanding" : margin >= 0 ? "ChecksPage.verdict.passed"
                                                                         : margin >= -2 ? "ChecksPage.verdict.failed"
                                                                                   : "ChecksPage.verdict.botched");
    }

    static Color colorFor(final int margin) {
        return margin >= 4 ? READY : margin >= 0 ? ACCENT : margin >= -2 ? AMBER : DANGER;
    }

    /**
     * @param record a check
     *
     * @return a one-line summary for the journal list, such as "Passed · Natasha Kerensky"
     */
    static String summarize(final CheckRecord record) {
        List<CheckRecord.Side> sides = record.sides();
        if (record.opposed() && sides.size() == 2) {
            CheckRecord.Side winner = sides.get(0).won() ? sides.get(0) : sides.get(1);
            return getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.result.wins", winner.name());
        }
        if (sides.size() == 1) {
            return OracleConsole.joined(verdictWord(sides.get(0).margin()), sides.get(0).name());
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.result.group", record.countWon(), sides.size());
    }

    /**
     * @param record a check
     *
     * @return each side's roll on one line, such as "Rook 9 vs 6+ · Magistrate Dace 7 vs 7+"
     */
    static String describeSides(final CheckRecord record) {
        List<String> parts = new ArrayList<>();
        for (CheckRecord.Side side : record.sides()) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.result.short", side.name(), side.action(),
                  side.roll(), side.target()));
        }
        return OracleConsole.joined(parts);
    }

    private void refreshRecent() {
        recent.removeAll();
        List<JournalEntry> log = console.roleplay().getOracleLog();
        int shown = 0;
        for (int index = log.size() - 1; index >= 0 && shown < RECENT_CHECKS; index--) {
            JournalEntry entry = log.get(index);
            CheckRecord record = entry.getCheck();
            if (record == null) {
                continue;
            }
            shown++;
            Color color = record.opposed() ? ACCENT : (record.countWon() == record.sides().size()) ? READY
                                                                : (record.countWon() == 0) ? DANGER : AMBER;
            HudCard card = new HudCard().show(color, true, summarize(record), null, List.of(), describeSides(record),
                  OracleConsole.formatDate(entry.getDate()), null, false);
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.setToolTipText(text("ChecksPage.recent.toolTipText"));
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    showResult(record, false);
                }
            });
            recent.add(leftAligned(card));
        }
        if (shown == 0) {
            recent.add(leftAligned(Hud.hint(text("ChecksPage.recent.none"))));
        }
        recent.revalidate();
        recent.repaint();
    }

    // endregion Rolling and results

    // region Renderers

    static String text(final String key) {
        return getTextAt(RESOURCE_BUNDLE, key);
    }

    private static JPanel topAligned(final JComponent content) {
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.add(content, BorderLayout.NORTH);
        return holder;
    }

    /** A hint that wraps to the width of the left column. */
    static JLabel hint(final String text) {
        return Hud.hint(wrap(text));
    }

    static String wrap(final String text) {
        return text.isEmpty() ? "" : "<html><div style='width:" + scaleForGUI(280) + "px'>" + escape(text)
                                           + "</div></html>";
    }

    static ListCellRenderer<Object> labelRenderer() {
        ListCellRenderer<Object> hud = Hud.comboRenderer();
        return (list, value, index, isSelected, cellHasFocus) -> {
            Object display = (value instanceof SkillAttribute attribute) ? attribute.getLabel()
                                   : (value instanceof PlotThread plotThread) ? plotThread.getName()
                                           : (value instanceof NpcRating rating) ? rating.toString() : value;
            return hud.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
        };
    }

    /** A selection model where a click ticks or unticks one row, so several people can be picked without Ctrl. */
    private static final class ToggleSelectionModel extends DefaultListSelectionModel {
        private ToggleSelectionModel() {
            setSelectionMode(MULTIPLE_INTERVAL_SELECTION);
        }

        @Override
        public void setSelectionInterval(int index0, int index1) {
            if (index0 == index1 && isSelectedIndex(index0)) {
                removeSelectionInterval(index0, index0);
            } else {
                addSelectionInterval(index0, index1);
            }
        }
    }

    private final class PersonRenderer implements ListCellRenderer<Person> {
        private final HudCard card = new HudCard();

        @Override
        public Component getListCellRendererComponent(JList<? extends Person> source, Person person, int index,
              boolean isSelected, boolean cellHasFocus) {
            List<Tag> tags = new ArrayList<>();
            if (checks.isEdgeAllowed()) {
                tags.add(new Tag(getFormattedTextAt(RESOURCE_BUNDLE, "ChecksPage.who.edge", person.getCurrentEdge()),
                      TEXT_MUTED));
            }
            CheckTrait trait = currentTrait();
            String figure = "";
            if (trait != null) {
                figure = checks.target(person, trait, 0).describe();
                if (!RoleplayChecks.isTrained(person, trait)) {
                    tags.add(new Tag(text("ChecksPage.who.untrained"), AMBER));
                }
            }
            String sub = text(isSelected ? "ChecksPage.who.ticked" : "ChecksPage.who.notTicked");
            return card.show(isSelected ? ACCENT : TEXT_FAINT, isSelected, person.getFullName(),
                  person.getPrimaryRoleDesc(), tags, sub, figure, text("ChecksPage.who.target"), isSelected);
        }
    }

    private final class SkillRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            String name = (String) value;
            boolean trained = trainedSkills.contains(name);
            String label = "<html><b>" + escape(name.replace(" (RP Only)", "")) + "</b>"
                                 + (trained ? "" : "  <font color='" + hex(TEXT_FAINT) + "'>"
                                                         + text("ChecksPage.skill.untrained") + "</font>")
                                 + "  <font color='" + hex(ACCENT_BRIGHT) + "'>"
                                 + escape(skillTargets.getOrDefault(name, "")) + "</font></html>";
            JLabel cell = (JLabel) super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            cell.setOpaque(true);
            cell.setBackground(isSelected ? SURFACE_HIGHLIGHT : SURFACE_DEEP);
            cell.setForeground(trained ? TEXT : TEXT_MUTED);
            cell.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), scaleForGUI(8), scaleForGUI(4),
                  scaleForGUI(8)));
            return cell;
        }
    }

    // endregion Renderers
}
