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
package mekhq.gui.commandGeneration.contents;

import java.util.EnumMap;
import java.util.EnumSet;
import megamek.client.ui.util.UIUtil;
import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.processWrapSize;
import static mekhq.gui.commandGeneration.components.CommandGenerationUtilities.getCommandGenerationResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.annotations.Nullable;
import megamek.common.enums.NeuralInterfaceMode;
import megamek.common.enums.SkillLevel;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.TemporaryCrewRole;
import mekhq.campaign.universe.enums.ForceNamingMethod;
import mekhq.campaign.universe.enums.TechAssignmentSortFactor;
import mekhq.gui.commandGeneration.components.CommandGenerationCheckBox;
import mekhq.gui.commandGeneration.components.CommandGenerationLabel;
import mekhq.gui.commandGeneration.components.CommandGenerationStandardPanel;
import mekhq.gui.panels.RandomOriginOptionsPanel;

/**
 * Pre-generation rules tab. Six titled sections, laid out across three rows:
 *
 * <ol>
 *   <li><b>Force Shape</b> — Company Command Lance toggle, formation-naming method.</li>
 *   <li><b>Support Personnel</b> — per-role coverage percentage and skill picker for the nine
 *       SUPPORT roles (Mek Tech, Mechanic, Aero Tek, BA Tech, Doctor, Administrator × 4). 100% =
 *       the canonical CamOps demand computed from the force composition; values above add
 *       redundancy, below under-staffs. Per-role skill picker drives experience tier for every
 *       Person of that role.</li>
 *   <li><b>Assistants</b> — astech and medic generation. Each auxiliary type has an independent
 *       enable toggle and a pool-vs-individual-Personnel radio. Pool mode (default) calls
 *       {@code campaign.changeAstechPool / changeMedicPool}; Personnel mode creates named Persons
 *       with the selected skill level.</li>
 *   <li><b>Officer Selection</b> — eight toggles controlling commander / officer picks and skill
 *       weighting.</li>
 *   <li><b>Naming &amp; Ranks</b> — four toggles for rank auto-assignment, callsigns, and the founder
 *       flag.</li>
 *   <li><b>Random Origin</b> — the existing {@link RandomOriginOptionsPanel} sub-panel.</li>
 * </ol>
 *
 * <p>Coverage / skill / astech / medic fields bind to the new {@code supportPersonnelCoveragePercents},
 * {@code supportPersonnelSkillLevels}, and astech/medic triplet on {@link CommandGenerationOptions} —
 * not the legacy absolute-count {@code supportPersonnel} map (which AtB / Windchild paths still
 * use). The legacy {@code poolAssistants} flag is mirrored from {@code generateAstechs ||
 * generateMedics} on write so old presets stay compatible.</p>
 */
public class SetupTab {
    private static final MMLogger LOGGER = MMLogger.create(SetupTab.class);


    /** Coverage spinner range. 100 = full canonical coverage; >100 = redundancy. */
    private static final int COVERAGE_SPINNER_MIN = 0;
    private static final int COVERAGE_SPINNER_MAX = 300;
    private static final int COVERAGE_SPINNER_STEP = 5;
    /** A grid row no section reaches, so the vertical filler always lands below the last control. */
    private static final int BOTTOM_SLACK_ROW = 99;

    private static final int COVERAGE_SPINNER_DEFAULT = 100;

    /**
     * Skill-level options offered for support personnel. Five tiers — Ultra-Green through Elite.
     * Heroic / Legendary are deliberately excluded since they're reserved for one-off Person
     * customizations, not bulk generation.
     */
    // Skill-picker options: a leading null renders as "Random" (each person rolls their own level),
    // followed by every fixed tier through Legendary.
    private static final SkillLevel[] SUPPORT_SKILL_LEVELS = {
          null,
          SkillLevel.ULTRA_GREEN,
          SkillLevel.GREEN,
          SkillLevel.REGULAR,
          SkillLevel.VETERAN,
          SkillLevel.ELITE,
          SkillLevel.HEROIC,
          SkillLevel.LEGENDARY
    };

    /**
     * Direction toggle for one slot of the Tech Assignment sort grid. Localized via the
     * {@code lblTechAssignmentDirection.*} bundle keys; the assigner reads
     * {@link #isDescending()} when building its comparator chain.
     */
    private enum SortDirection {
        DESCENDING("lblTechAssignmentDirection.descending"),
        ASCENDING("lblTechAssignmentDirection.ascending");

        private final String bundleKey;

        SortDirection(String bundleKey) {
            this.bundleKey = bundleKey;
        }

        boolean isDescending() {
            return this == DESCENDING;
        }

        static SortDirection of(boolean descending) {
            return descending ? DESCENDING : ASCENDING;
        }

        @Override
        public String toString() {
            return getTextAt(getCommandGenerationResourceBundle(), bundleKey);
        }
    }

    private static final SortDirection[] SORT_DIRECTIONS = { SortDirection.DESCENDING, SortDirection.ASCENDING };

    private static final TechAssignmentSortFactor[] SORT_FACTORS = {
          TechAssignmentSortFactor.NONE,
          TechAssignmentSortFactor.PILOT_RANK,
          TechAssignmentSortFactor.UNIT_WEIGHT,
          TechAssignmentSortFactor.PILOT_SKILL
    };

    private final Campaign campaign;
    private CommandGenerationOptions options;

    // Force shape
    private MMComboBox<ForceNamingMethod> comboForceNamingMethod;
    private CommandGenerationCheckBox chkAlwaysNumberRegiments;
    // Notified when the naming-method combo changes, so the Force Generator tab can refresh its
    // preview callsigns mid-dialog (the options object only receives the combo's value on OK).
    private Runnable namingMethodChangeListener;

    // Support personnel — per-role coverage % + skill level
    private final Map<PersonnelRole, JSpinner> spnSupportCoveragePercents = new LinkedHashMap<>();
    private final Map<PersonnelRole, MMComboBox<SkillLevel>> cmbSupportSkillLevels = new LinkedHashMap<>();

    // Assistants — astech / medic generation
    private CommandGenerationCheckBox chkGenerateAstechs;
    private JRadioButton rdoAstechsAsPool;
    private JRadioButton rdoAstechsAsPersonnel;
    private MMComboBox<SkillLevel> cmbAstechSkillLevel;
    private CommandGenerationCheckBox chkGenerateMedics;
    private JRadioButton rdoMedicsAsPool;
    private JRadioButton rdoMedicsAsPersonnel;
    private MMComboBox<SkillLevel> cmbMedicSkillLevel;

    // Tech Assignment — three-slot sort grid + per-slot direction
    private CommandGenerationCheckBox chkGenerateMedicalReserve;
    private JSpinner spnMedicalReservePercent;

    private CommandGenerationCheckBox chkAssignTechsToUnits;
    private MMComboBox<TechAssignmentSortFactor> cmbTechAssignmentPrimary;
    private MMComboBox<TechAssignmentSortFactor> cmbTechAssignmentSecondary;
    private MMComboBox<TechAssignmentSortFactor> cmbTechAssignmentTertiary;
    private MMComboBox<SortDirection> cmbTechAssignmentPrimaryDirection;
    private MMComboBox<SortDirection> cmbTechAssignmentSecondaryDirection;
    private MMComboBox<SortDirection> cmbTechAssignmentTertiaryDirection;

    // Officer selection
    private CommandGenerationCheckBox chkAssignBestCompanyCommander;
    private CommandGenerationCheckBox chkPrioritizeCompanyCommanderCombatSkills;
    private CommandGenerationCheckBox chkAssignBestOfficers;
    private CommandGenerationCheckBox chkPrioritizeOfficerCombatSkills;
    private CommandGenerationCheckBox chkAssignMostSkilledToPrimaryLances;
    private CommandGenerationCheckBox chkGenerateCaptains;
    private CommandGenerationCheckBox chkAssignCompanyCommanderFlag;
    private CommandGenerationCheckBox chkApplyOfficerStatBonusToWorstSkill;

    // Naming & ranks
    private CommandGenerationCheckBox chkAutomaticallyAssignRanks;
    private CommandGenerationCheckBox chkUseSpecifiedFactionToAssignRanks;
    private CommandGenerationCheckBox chkAssignMekWarriorsCallSigns;
    private CommandGenerationCheckBox chkAssignFounderFlag;

    // Augmentation. These three live on the campaign and on MegaMek's game options rather than on a
    // generation run; they are surfaced here because all three are off in a new campaign and a player
    // who has not gone looking through two other options dialogs cannot generate an augmented command.
    private CommandGenerationCheckBox chkUseImplants;
    private CommandGenerationCheckBox chkUseManeiDomini;
    private MMComboBox<NeuralInterfaceMode> cmbNeuralInterfaceMode;

    // Temporary crew. Campaign settings, surfaced here for the same reason as the augmentation toggles: a
    // player building a starting force decides here whether a tank's crew are named warriors or an
    // anonymous pool, without going through the campaign options dialog first.
    private final Map<TemporaryCrewRole, CommandGenerationCheckBox> chkTemporaryCrew =
          new EnumMap<>(TemporaryCrewRole.class);

    // Random origin
    private RandomOriginOptionsPanel randomOriginOptionsPanel;

    public SetupTab(Campaign campaign, CommandGenerationOptions options) {
        this.campaign = campaign;
        this.options = options;
    }

    /**
     * The ordered list of support roles surfaced as spinners in the Support Personnel section. Order
     * matches the legacy panel.
     */
    private static final PersonnelRole[] SUPPORT_ROLES = {
          PersonnelRole.MEK_TECH,
          PersonnelRole.MECHANIC,
          PersonnelRole.AERO_TEK,
          PersonnelRole.BA_TECH,
          PersonnelRole.DOCTOR,
          PersonnelRole.ADMINISTRATOR
    };

    public JPanel createTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setName("pnlSetupTab");

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(6),
              UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(6));

        // Two columns. The left runs from how the command is named to who staffs it; the right holds the
        // settings that shape individual people - where they are from, whether their seats are named or
        // pooled, who maintains their units, and augmentation.
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.add(buildNamingAndRanksSection());
        leftColumn.add(Box.createVerticalStrut(UIUtil.scaleForGUI(6)));
        leftColumn.add(buildOfficerSelectionSection());
        leftColumn.add(Box.createVerticalStrut(UIUtil.scaleForGUI(6)));
        leftColumn.add(buildSupportPersonnelSection());
        leftColumn.add(Box.createVerticalStrut(UIUtil.scaleForGUI(6)));
        leftColumn.add(buildAssistantsSection());
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 0.5;
        panel.add(leftColumn, constraints);

        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.add(buildRandomOriginSection());
        rightColumn.add(Box.createVerticalStrut(UIUtil.scaleForGUI(6)));
        rightColumn.add(buildTemporaryCrewSection());
        rightColumn.add(Box.createVerticalStrut(UIUtil.scaleForGUI(6)));
        rightColumn.add(buildTechAssignmentSection());
        rightColumn.add(Box.createVerticalStrut(UIUtil.scaleForGUI(6)));
        rightColumn.add(buildAugmentationSection());
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 0.5;
        panel.add(rightColumn, constraints);
        return panel;
    }

    private JPanel buildSupportPersonnelSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "SupportPersonnel", true, "SupportPersonnel");
        section.setLayout(new GridBagLayout());
        GridBagConstraints constraints = sectionConstraints();

        spnSupportCoveragePercents.clear();
        cmbSupportSkillLevels.clear();

        String spinnerTooltipTemplate = getTextAt(getCommandGenerationResourceBundle(),
              "supportCoveragePercent.toolTipText");
        String skillTooltipTemplate = getTextAt(getCommandGenerationResourceBundle(),
              "supportSkillLevel.toolTipText");

        // One column of (role label, % spinner, skill dropdown). The section shares its row with the
        // Temporary Crew toggles, so the roles stack rather than splitting into two side-by-side columns
        // that would not fit in half the width.

        // Header row labels
        addColumnHeader(section, constraints, 0, 0, "lblSupportPersonnelColumnRole.text");
        addColumnHeader(section, constraints, 1, 0, "lblSupportPersonnelColumnPercent.text");
        addColumnHeader(section, constraints, 2, 0, "lblSupportPersonnelColumnSkill.text");

        for (int i = 0; i < SUPPORT_ROLES.length; i++) {
            PersonnelRole role = SUPPORT_ROLES[i];
            String roleDisplay = role.getLabel(campaign != null && campaign.getPlayerForce().getFaction().isClan());

            JLabel roleLabel = new JLabel(roleDisplay);
            roleLabel.setName("lblSupport" + role.name());

            JSpinner spinner = new JSpinner(new SpinnerNumberModel(
                  COVERAGE_SPINNER_DEFAULT, COVERAGE_SPINNER_MIN, COVERAGE_SPINNER_MAX, COVERAGE_SPINNER_STEP));
            spinner.setName("spnSupportCoverage" + role.name());
            if (spinnerTooltipTemplate != null && !spinnerTooltipTemplate.isEmpty()) {
                spinner.setToolTipText(String.format(spinnerTooltipTemplate, roleDisplay));
            }
            roleLabel.setLabelFor(spinner);
            spnSupportCoveragePercents.put(role, spinner);

            MMComboBox<SkillLevel> skillCombo = buildSkillLevelCombo("cmbSupportSkill" + role.name());
            if (skillTooltipTemplate != null && !skillTooltipTemplate.isEmpty()) {
                skillCombo.setToolTipText(String.format(skillTooltipTemplate, roleDisplay));
            }
            cmbSupportSkillLevels.put(role, skillCombo);

            constraints.gridy = i + 1; // +1 for header
            constraints.gridx = 0;
            section.add(roleLabel, constraints);
            constraints.gridx = 1;
            section.add(spinner, constraints);
            constraints.gridx = 2;
            section.add(skillCombo, constraints);
        }

        addLeftAlignFiller(section, 3);
        return section;
    }

    private static void addColumnHeader(JPanel section, GridBagConstraints constraints, int gridX, int gridY,
          String bundleKey) {
        JLabel header = new JLabel(
              "<html><b>" + getTextAt(getCommandGenerationResourceBundle(), bundleKey) + "</b></html>");
        constraints.gridx = gridX;
        constraints.gridy = gridY;
        section.add(header, constraints);
    }

    private JPanel buildAssistantsSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "Assistants", true, "Assistants");
        section.setLayout(new GridBagLayout());
        GridBagConstraints constraints = sectionConstraints();

        // Astech block
        chkGenerateAstechs = new CommandGenerationCheckBox("GenerateAstechs");
        rdoAstechsAsPool = makeStyledRadio("AstechsAsPool");
        rdoAstechsAsPersonnel = makeStyledRadio("AstechsAsPersonnel");
        ButtonGroup astechGroup = new ButtonGroup();
        astechGroup.add(rdoAstechsAsPool);
        astechGroup.add(rdoAstechsAsPersonnel);
        rdoAstechsAsPool.setSelected(true);
        cmbAstechSkillLevel = buildSkillLevelCombo("cmbAstechSkillLevel");

        // Medic block
        chkGenerateMedics = new CommandGenerationCheckBox("GenerateMedics");
        rdoMedicsAsPool = makeStyledRadio("MedicsAsPool");
        rdoMedicsAsPersonnel = makeStyledRadio("MedicsAsPersonnel");
        ButtonGroup medicGroup = new ButtonGroup();
        medicGroup.add(rdoMedicsAsPool);
        medicGroup.add(rdoMedicsAsPersonnel);
        rdoMedicsAsPool.setSelected(true);
        cmbMedicSkillLevel = buildSkillLevelCombo("cmbMedicSkillLevel");

        // Enable/disable wiring: parent checkbox controls the radios + skill dropdown;
        // "as Personnel" radio controls whether the skill dropdown is live.
        chkGenerateAstechs.addActionListener(event -> refreshAstechEnablement());
        rdoAstechsAsPool.addActionListener(event -> refreshAstechEnablement());
        rdoAstechsAsPersonnel.addActionListener(event -> refreshAstechEnablement());
        chkGenerateMedics.addActionListener(event -> refreshMedicEnablement());
        rdoMedicsAsPool.addActionListener(event -> refreshMedicEnablement());
        rdoMedicsAsPersonnel.addActionListener(event -> refreshMedicEnablement());

        // Layout
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        section.add(chkGenerateAstechs, constraints);
        constraints.gridwidth = 1;
        indentAsSubOption(rdoAstechsAsPool);
        constraints.gridy = 1;
        constraints.gridx = 0;
        section.add(rdoAstechsAsPool, constraints);
        indentAsSubOption(rdoAstechsAsPersonnel);
        constraints.gridy = 2;
        constraints.gridx = 0;
        section.add(rdoAstechsAsPersonnel, constraints);
        constraints.gridx = 1;
        section.add(cmbAstechSkillLevel, constraints);

        constraints.gridy = 3;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        section.add(chkGenerateMedics, constraints);
        constraints.gridwidth = 1;
        indentAsSubOption(rdoMedicsAsPool);
        constraints.gridy = 4;
        constraints.gridx = 0;
        section.add(rdoMedicsAsPool, constraints);
        indentAsSubOption(rdoMedicsAsPersonnel);
        constraints.gridy = 5;
        constraints.gridx = 0;
        section.add(rdoMedicsAsPersonnel, constraints);
        constraints.gridx = 1;
        section.add(cmbMedicSkillLevel, constraints);

        // Medical reserve: generation-only spare MekWarriors as injury replacements, sized by a
        // percentage of the generated combatants. The spinner is live only when the box is checked.
        chkGenerateMedicalReserve = new CommandGenerationCheckBox("GenerateMedicalReserve");
        spnMedicalReservePercent = new JSpinner(new SpinnerNumberModel(10, 0, 100, 5));
        spnMedicalReservePercent.setToolTipText(chkGenerateMedicalReserve.getToolTipText());
        chkGenerateMedicalReserve.addActionListener(event ->
              spnMedicalReservePercent.setEnabled(chkGenerateMedicalReserve.isSelected()));
        constraints.gridy = 6;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        section.add(chkGenerateMedicalReserve, constraints);
        constraints.gridx = 1;
        section.add(spnMedicalReservePercent, constraints);

        addLeftAlignFiller(section, 2);
        return section;
    }

    /**
     * Builds a {@link JRadioButton} that pulls its text and tooltip from the Company Generation
     * bundle using the same {@code lbl<name>.text} / {@code lbl<name>.tooltip} keys as
     * {@link CommandGenerationCheckBox}. We can't subclass JRadioButton with the existing styled
     * components because they extend JCheckBox; this helper provides the bundle wiring inline.
     */
    private static JRadioButton makeStyledRadio(String name) {
        String text = getTextAt(getCommandGenerationResourceBundle(), "lbl" + name + ".text");
        JRadioButton button = new JRadioButton(text);
        button.setName("rdo" + name);
        String tooltip = getTextAt(getCommandGenerationResourceBundle(), "lbl" + name + ".tooltip");
        if (tooltip != null && !tooltip.isEmpty()) {
            button.setToolTipText(wordWrap(tooltip, processWrapSize(null)));
        }
        return button;
    }

    private void refreshAstechEnablement() {
        boolean parentOn = chkGenerateAstechs.isSelected();
        rdoAstechsAsPool.setEnabled(parentOn);
        rdoAstechsAsPersonnel.setEnabled(parentOn);
        cmbAstechSkillLevel.setEnabled(parentOn && rdoAstechsAsPersonnel.isSelected());
    }

    private void refreshMedicEnablement() {
        boolean parentOn = chkGenerateMedics.isSelected();
        rdoMedicsAsPool.setEnabled(parentOn);
        rdoMedicsAsPersonnel.setEnabled(parentOn);
        cmbMedicSkillLevel.setEnabled(parentOn && rdoMedicsAsPersonnel.isSelected());
    }

    private JPanel buildTechAssignmentSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "TechAssignment", true, "TechAssignment");
        section.setLayout(new GridBagLayout());
        GridBagConstraints constraints = sectionConstraints();

        chkAssignTechsToUnits = new CommandGenerationCheckBox("AssignTechsToUnits");
        cmbTechAssignmentPrimary = new MMComboBox<>("cmbTechAssignmentPrimary", SORT_FACTORS);
        cmbTechAssignmentSecondary = new MMComboBox<>("cmbTechAssignmentSecondary", SORT_FACTORS);
        cmbTechAssignmentTertiary = new MMComboBox<>("cmbTechAssignmentTertiary", SORT_FACTORS);
        cmbTechAssignmentPrimaryDirection = new MMComboBox<>("cmbTechAssignmentPrimaryDirection", SORT_DIRECTIONS);
        cmbTechAssignmentSecondaryDirection = new MMComboBox<>("cmbTechAssignmentSecondaryDirection", SORT_DIRECTIONS);
        cmbTechAssignmentTertiaryDirection = new MMComboBox<>("cmbTechAssignmentTertiaryDirection", SORT_DIRECTIONS);

        chkAssignTechsToUnits.addActionListener(event -> refreshTechAssignmentEnablement());
        // Each factor combo toggles its own direction combo when set to NONE.
        cmbTechAssignmentPrimary.addActionListener(event -> refreshTechAssignmentEnablement());
        cmbTechAssignmentSecondary.addActionListener(event -> refreshTechAssignmentEnablement());
        cmbTechAssignmentTertiary.addActionListener(event -> refreshTechAssignmentEnablement());

        // Row 0: master enable checkbox spans all columns.
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.gridwidth = 3;
        section.add(chkAssignTechsToUnits, constraints);
        constraints.gridwidth = 1;

        // Rows 1-3: three sort slots, each (label, factor combo, direction combo).
        addTechSortRow(section, constraints, 1, "TechAssignmentPrimary",
              cmbTechAssignmentPrimary, cmbTechAssignmentPrimaryDirection);
        addTechSortRow(section, constraints, 2, "TechAssignmentSecondary",
              cmbTechAssignmentSecondary, cmbTechAssignmentSecondaryDirection);
        addTechSortRow(section, constraints, 3, "TechAssignmentTertiary",
              cmbTechAssignmentTertiary, cmbTechAssignmentTertiaryDirection);

        addLeftAlignFiller(section, 3);
        return section;
    }

    private static void addTechSortRow(JPanel section, GridBagConstraints constraints, int row,
          String labelName, MMComboBox<TechAssignmentSortFactor> factorCombo,
          MMComboBox<SortDirection> directionCombo) {
        CommandGenerationLabel label = new CommandGenerationLabel(labelName);
        factorCombo.setToolTipText(label.getToolTipText());
        directionCombo.setToolTipText(wordWrap(getTextAt(getCommandGenerationResourceBundle(),
              "cmbTechAssignmentDirection.tooltip"), processWrapSize(null)));
        indentAsSubOption(label);
        constraints.gridy = row;
        constraints.gridx = 0;
        section.add(label, constraints);
        constraints.gridx = 1;
        section.add(factorCombo, constraints);
        constraints.gridx = 2;
        section.add(directionCombo, constraints);
    }

    /**
     * Enables / disables the three sort dropdowns and their direction toggles based on the master
     * checkbox. A direction combo is also greyed out when its factor combo is set to
     * {@link TechAssignmentSortFactor#NONE} — direction is meaningless without a sort factor.
     */
    private void refreshTechAssignmentEnablement() {
        boolean masterOn = chkAssignTechsToUnits.isSelected();
        cmbTechAssignmentPrimary.setEnabled(masterOn);
        cmbTechAssignmentSecondary.setEnabled(masterOn);
        cmbTechAssignmentTertiary.setEnabled(masterOn);
        cmbTechAssignmentPrimaryDirection.setEnabled(masterOn && !isNone(cmbTechAssignmentPrimary));
        cmbTechAssignmentSecondaryDirection.setEnabled(masterOn && !isNone(cmbTechAssignmentSecondary));
        cmbTechAssignmentTertiaryDirection.setEnabled(masterOn && !isNone(cmbTechAssignmentTertiary));
    }

    private static boolean isNone(MMComboBox<TechAssignmentSortFactor> combo) {
        Object value = combo.getSelectedItem();
        return !(value instanceof TechAssignmentSortFactor f) || f == TechAssignmentSortFactor.NONE;
    }

    private JPanel buildOfficerSelectionSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "OfficerSelection", true, "OfficerSelection");
        section.setLayout(new GridBagLayout());
        GridBagConstraints constraints = sectionConstraints();
        constraints.gridwidth = 1;

        chkAssignBestCompanyCommander = new CommandGenerationCheckBox("AssignBestCompanyCommander");
        chkPrioritizeCompanyCommanderCombatSkills =
              new CommandGenerationCheckBox("PrioritizeCompanyCommanderCombatSkills");
        indentAsSubOption(chkPrioritizeCompanyCommanderCombatSkills);
        chkAssignBestCompanyCommander.addActionListener(event ->
              chkPrioritizeCompanyCommanderCombatSkills.setEnabled(chkAssignBestCompanyCommander.isSelected()));

        chkAssignBestOfficers = new CommandGenerationCheckBox("AssignBestOfficers");
        chkPrioritizeOfficerCombatSkills =
              new CommandGenerationCheckBox("PrioritizeOfficerCombatSkills");
        indentAsSubOption(chkPrioritizeOfficerCombatSkills);
        chkAssignBestOfficers.addActionListener(event ->
              chkPrioritizeOfficerCombatSkills.setEnabled(chkAssignBestOfficers.isSelected()));

        chkAssignMostSkilledToPrimaryLances = new CommandGenerationCheckBox("AssignMostSkilledToPrimaryLances");
        chkGenerateCaptains = new CommandGenerationCheckBox("GenerateCaptains");
        chkAssignCompanyCommanderFlag = new CommandGenerationCheckBox("AssignCompanyCommanderFlag");
        chkApplyOfficerStatBonusToWorstSkill =
              new CommandGenerationCheckBox("ApplyOfficerStatBonusToWorstSkill");

        stack(section, constraints,
              chkAssignBestCompanyCommander,
              chkPrioritizeCompanyCommanderCombatSkills,
              chkAssignBestOfficers,
              chkPrioritizeOfficerCombatSkills,
              chkAssignMostSkilledToPrimaryLances,
              chkGenerateCaptains,
              chkAssignCompanyCommanderFlag,
              chkApplyOfficerStatBonusToWorstSkill);

        addLeftAlignFiller(section, 1);
        return section;
    }

    private JPanel buildNamingAndRanksSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "NamingAndRanks", true, "NamingAndRanks");
        section.setLayout(new GridBagLayout());
        GridBagConstraints constraints = sectionConstraints();
        constraints.gridwidth = 1;

        chkAutomaticallyAssignRanks = new CommandGenerationCheckBox("AutomaticallyAssignRanks");
        chkUseSpecifiedFactionToAssignRanks = new CommandGenerationCheckBox("UseSpecifiedFactionToAssignRanks");
        indentAsSubOption(chkUseSpecifiedFactionToAssignRanks);
        chkAutomaticallyAssignRanks.addActionListener(event ->
              chkUseSpecifiedFactionToAssignRanks.setEnabled(chkAutomaticallyAssignRanks.isSelected()));
        chkAssignMekWarriorsCallSigns = new CommandGenerationCheckBox("AssignMekWarriorsCallSigns");
        chkAssignFounderFlag = new CommandGenerationCheckBox("AssignFounderFlag");

        // How formations are named sits with how people are named and ranked: the two together are
        // everything about the command that is a matter of style rather than strength.
        comboForceNamingMethod = new MMComboBox<>("comboForceNamingMethod", ForceNamingMethod.values());
        comboForceNamingMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                  int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ForceNamingMethod namingMethod) {
                    // Append the first-three preview inline so the user can see what each scheme
                    // produces without hovering for the tooltip.
                    setText(namingMethod.toString() + " - " + namingMethod.getExample());
                    list.setToolTipText(namingMethod.getToolTipText());
                }
                return this;
            }
        });
        comboForceNamingMethod.addActionListener(actionEvent -> {
            if (namingMethodChangeListener != null) {
                namingMethodChangeListener.run();
            }
        });

        chkAlwaysNumberRegiments = new CommandGenerationCheckBox("AlwaysNumberRegiments");
        chkAlwaysNumberRegiments.addActionListener(actionEvent -> {
            if (namingMethodChangeListener != null) {
                namingMethodChangeListener.run();
            }
        });

        constraints.gridy = 0;
        constraints.gridx = 0;
        CommandGenerationLabel namingMethodLabel = new CommandGenerationLabel("ForceNamingMethod");
        section.add(namingMethodLabel, constraints);
        // The picker itself carries the label's explanation; the renderer's per-item tooltip only shows on
        // the open list.
        comboForceNamingMethod.setToolTipText(namingMethodLabel.getToolTipText());
        constraints.gridx = 1;
        section.add(comboForceNamingMethod, constraints);

        constraints.gridwidth = 2;
        constraints.gridx = 0;
        constraints.gridy = 1;
        section.add(chkAlwaysNumberRegiments, constraints);
        constraints.gridy = 2;
        section.add(chkAutomaticallyAssignRanks, constraints);
        constraints.gridy = 3;
        section.add(chkUseSpecifiedFactionToAssignRanks, constraints);
        constraints.gridy = 4;
        section.add(chkAssignMekWarriorsCallSigns, constraints);
        constraints.gridy = 5;
        section.add(chkAssignFounderFlag, constraints);

        addLeftAlignFiller(section, 2);
        return section;
    }

    private JPanel buildRandomOriginSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "RandomOrigin", true, "RandomOrigin");
        section.setLayout(new GridBagLayout());
        GridBagConstraints constraints = sectionConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        randomOriginOptionsPanel = new RandomOriginOptionsPanel(null, campaign,
              campaign == null ? null : campaign.getPlayerForce().getFaction());
        randomOriginOptionsPanel.setBorder(BorderFactory.createEmptyBorder());
        section.add(randomOriginOptionsPanel, constraints);

        addLeftAlignFiller(section, 1);
        return section;
    }

    /**
     * Cybernetic augmentation: whether the campaign tracks implants at all, and which of MegaMek's
     * augmentation rules are in play.
     *
     * <p>All three are off in a new campaign, and all three have to be on before the generator will
     * fit anything - Manei Domini implants to a Shadow Division, enhanced imaging to Clan warriors.
     * Setting them anywhere else means finding one in Campaign Options and two in MegaMek's game
     * options, which is why they are repeated here: this is the screen where the decision is being
     * made. What is chosen here is written to the campaign, so it holds for the saved game too.</p>
     *
     * <p>Neither rule can be applied to warriors after the fact, so the choice has to be made before
     * generating rather than discovered afterwards.</p>
     */
    private JPanel buildAugmentationSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "Augmentation", true, "Augmentation");
        section.setLayout(new GridBagLayout());
        GridBagConstraints constraints = sectionConstraints();

        chkUseImplants = new CommandGenerationCheckBox("UseImplants");
        chkUseManeiDomini = new CommandGenerationCheckBox("UseManeiDomini");
        cmbNeuralInterfaceMode = new MMComboBox<>("cmbNeuralInterfaceMode",
              NeuralInterfaceMode.values());
        cmbNeuralInterfaceMode.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                  int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof NeuralInterfaceMode mode) {
                    // The option's own stored wording, which is what MegaMek's game options dialog
                    // shows, so the same setting reads the same in both places.
                    setText(mode.optionValue());
                }
                return this;
            }
        });

        // Implants gate the other two: with the campaign not tracking them, neither rule has anything
        // to act on.
        chkUseImplants.addActionListener(actionEvent -> refreshAugmentationEnablement());

        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        section.add(chkUseImplants, constraints);

        constraints.gridy = 1;
        constraints.gridwidth = 2;
        indentAsSubOption(chkUseManeiDomini);
        section.add(chkUseManeiDomini, constraints);

        CommandGenerationLabel neuralInterfaceLabel = new CommandGenerationLabel("NeuralInterfaceMode");
        indentAsSubOption(neuralInterfaceLabel);
        cmbNeuralInterfaceMode.setToolTipText(neuralInterfaceLabel.getToolTipText());
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        section.add(neuralInterfaceLabel, constraints);
        constraints.gridx = 1;
        section.add(cmbNeuralInterfaceMode, constraints);

        addLeftAlignFiller(section, 2);
        return section;
    }

    /** Greys the two rules out while the campaign is not tracking implants at all. */
    private void refreshAugmentationEnablement() {
        boolean tracksImplants = chkUseImplants.isSelected();
        chkUseManeiDomini.setEnabled(tracksImplants);
        cmbNeuralInterfaceMode.setEnabled(tracksImplants);
    }

    /**
     * The eight temporary-crew toggles, laid out as the campaign options dialog lays them out so the same
     * setting reads the same in both places.
     */
    private JPanel buildTemporaryCrewSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "TemporaryCrew", true, "TemporaryCrew");
        section.setLayout(new GridBagLayout());
        GridBagConstraints constraints = sectionConstraints();

        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        section.add(new CommandGenerationLabel("TemporaryCrewDescription"), constraints);

        constraints.gridwidth = 1;
        chkTemporaryCrew.clear();
        int index = 0;
        for (TemporaryCrewRole role : TemporaryCrewRole.values()) {
            CommandGenerationCheckBox checkBox = new CommandGenerationCheckBox(role.getLabelKey());
            chkTemporaryCrew.put(role, checkBox);
            constraints.gridy = 1 + (index / 2);
            constraints.gridx = index % 2;
            section.add(checkBox, constraints);
            index++;
        }

        addLeftAlignFiller(section, 2);
        return section;
    }

    private static GridBagConstraints sectionConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(6),
              UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(6));
        return constraints;
    }

    private static void stack(JPanel section, GridBagConstraints constraints, JComponent... components) {
        constraints.gridx = 0;
        for (int i = 0; i < components.length; i++) {
            constraints.gridy = i;
            section.add(components[i], constraints);
        }
    }

    /**
     * Adds an invisible filler in column {@code gridX} (to the right of the section's real content)
     * that soaks up all spare horizontal width. Without a weighted column a GridBagLayout centers
     * the whole grid, which is what left every section floating mid-panel with dead space on both
     * sides; the filler keeps the real columns at their natural size and anchors them left.
     */
    private static void addLeftAlignFiller(JPanel section, int gridX) {
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = gridX;
        filler.gridy = 0;
        filler.weightx = 1.0;
        filler.fill = GridBagConstraints.HORIZONTAL;
        section.add(Box.createHorizontalGlue(), filler);
        // A section stretched to its neighbour's height would otherwise centre its controls in the space,
        // leaving a blank band above them; the vertical slack goes below the last row instead.
        GridBagConstraints slack = new GridBagConstraints();
        slack.gridx = 0;
        slack.gridy = BOTTOM_SLACK_ROW;
        slack.weighty = 1.0;
        slack.fill = GridBagConstraints.VERTICAL;
        section.add(Box.createVerticalGlue(), slack);
    }

    /**
     * Adds a left-margin border to a checkbox so it visually reads as a sub-option of the checkbox
     * above it. Pairs with the existing parent-toggle ActionListener that disables the sub-option
     * when the parent is unchecked.
     */
    private static void indentAsSubOption(JComponent component) {
        component.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
    }

    /**
     * Builds a skill-level picker: the {@link #SUPPORT_SKILL_LEVELS} options (a leading {@code null}
     * rendered as "Random", then Ultra-Green through Legendary) defaulted to Random. A {@code null}
     * selection tells the generator to roll each person's own level.
     *
     * @param name the component name (for preferences / test lookup)
     *
     * @return the configured combo box, defaulted to Random
     */
    private MMComboBox<SkillLevel> buildSkillLevelCombo(String name) {
        MMComboBox<SkillLevel> combo = new MMComboBox<>(name, SUPPORT_SKILL_LEVELS);
        final String randomLabel = getTextAt(getCommandGenerationResourceBundle(), "skillLevelRandom.text");
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText(randomLabel);
                }
                return this;
            }
        });
        combo.setSelectedItem(null);
        return combo;
    }

    /**
     * Pushes values from the supplied options onto this tab's controls. Field-by-field copy.
     */
    /**
     * The naming method currently selected in the Formation Naming Method combo. Unlike the options
     * object — which only receives this value on OK — this reads the live control, so the Force
     * Generator tab's preview callsigns track mid-dialog changes.
     *
     * @return the selected naming method, or {@code null} if the combo has no selection
     */
    /**
     * Sets the Formation Naming Method combo.
     *
     * <p>Used when another part of the dialog implies a naming convention - picking a Clan faction
     * switches this to the Greek alphabet, which is how the Clans name their formations.</p>
     *
     * @param namingMethod the method to select; ignored when {@code null} or already selected
     */
    public void setSelectedForceNamingMethod(@Nullable ForceNamingMethod namingMethod) {
        if ((namingMethod == null) || (comboForceNamingMethod == null)) {
            return;
        }
        if (namingMethod == comboForceNamingMethod.getSelectedItem()) {
            LOGGER.info("[NamingMethod] already {}; leaving it", namingMethod);
            return;
        }
        LOGGER.info("[NamingMethod] switching from {} to {}",
              comboForceNamingMethod.getSelectedItem(), namingMethod);
        comboForceNamingMethod.setSelectedItem(namingMethod);
    }

    public @Nullable ForceNamingMethod getSelectedForceNamingMethod() {
        Object selectedNamingMethod = comboForceNamingMethod == null
              ? null
              : comboForceNamingMethod.getSelectedItem();
        return (selectedNamingMethod instanceof ForceNamingMethod namingMethod) ? namingMethod : null;
    }

    /**
     * Registers the callback fired whenever the Formation Naming Method combo changes.
     *
     * @param listener the callback, or {@code null} to clear
     */
    /**
     * Whether the player asked for regiments to be numbered ("1st Mek Regiment") rather than taking the
     * selected naming alphabet. Read live, like {@link #getSelectedForceNamingMethod()}, so the Force
     * Generator tab's preview tracks the checkbox before OK writes it to the options.
     */
    public boolean isAlwaysNumberRegimentsSelected() {
        return (chkAlwaysNumberRegiments != null) && chkAlwaysNumberRegiments.isSelected();
    }

    public void setNamingMethodChangeListener(@Nullable Runnable listener) {
        this.namingMethodChangeListener = listener;
    }

    public void loadValuesFromOptions(CommandGenerationOptions sourceOptions) {
        this.options = sourceOptions;
        if (sourceOptions == null) {
            return;
        }
        comboForceNamingMethod.setSelectedItem(sourceOptions.getForceNamingMethod());
        chkAlwaysNumberRegiments.setSelected(sourceOptions.isAlwaysNumberRegiments());

        // Per-role coverage % and skill level
        for (Map.Entry<PersonnelRole, JSpinner> entry : spnSupportCoveragePercents.entrySet()) {
            Integer percent = sourceOptions.getSupportPersonnelCoveragePercents().get(entry.getKey());
            entry.getValue().setValue(percent == null ? COVERAGE_SPINNER_DEFAULT : percent);
        }
        for (Map.Entry<PersonnelRole, MMComboBox<SkillLevel>> entry : cmbSupportSkillLevels.entrySet()) {
            // null selects the "Random" option.
            entry.getValue().setSelectedItem(sourceOptions.getSupportPersonnelSkillLevels().get(entry.getKey()));
        }

        // Assistants
        chkGenerateAstechs.setSelected(sourceOptions.isGenerateAstechs());
        if (sourceOptions.isAstechsAsPersonnel()) {
            rdoAstechsAsPersonnel.setSelected(true);
        } else {
            rdoAstechsAsPool.setSelected(true);
        }
        cmbAstechSkillLevel.setSelectedItem(sourceOptions.getAstechSkillLevel());
        refreshAstechEnablement();

        chkGenerateMedics.setSelected(sourceOptions.isGenerateMedics());
        if (sourceOptions.isMedicsAsPersonnel()) {
            rdoMedicsAsPersonnel.setSelected(true);
        } else {
            rdoMedicsAsPool.setSelected(true);
        }
        cmbMedicSkillLevel.setSelectedItem(sourceOptions.getMedicSkillLevel());
        refreshMedicEnablement();

        chkGenerateMedicalReserve.setSelected(sourceOptions.isGenerateMedicalReserve());
        spnMedicalReservePercent.setValue(sourceOptions.getMedicalReservePercent());
        spnMedicalReservePercent.setEnabled(chkGenerateMedicalReserve.isSelected());

        // Tech Assignment
        chkAssignTechsToUnits.setSelected(sourceOptions.isAssignTechsToUnits());
        cmbTechAssignmentPrimary.setSelectedItem(
              sourceOptions.getTechAssignmentPrimarySort() == null
                    ? TechAssignmentSortFactor.PILOT_RANK : sourceOptions.getTechAssignmentPrimarySort());
        cmbTechAssignmentPrimaryDirection.setSelectedItem(
              SortDirection.of(sourceOptions.isTechAssignmentPrimaryDescending()));
        cmbTechAssignmentSecondary.setSelectedItem(
              sourceOptions.getTechAssignmentSecondarySort() == null
                    ? TechAssignmentSortFactor.UNIT_WEIGHT : sourceOptions.getTechAssignmentSecondarySort());
        cmbTechAssignmentSecondaryDirection.setSelectedItem(
              SortDirection.of(sourceOptions.isTechAssignmentSecondaryDescending()));
        cmbTechAssignmentTertiary.setSelectedItem(
              sourceOptions.getTechAssignmentTertiarySort() == null
                    ? TechAssignmentSortFactor.PILOT_SKILL : sourceOptions.getTechAssignmentTertiarySort());
        cmbTechAssignmentTertiaryDirection.setSelectedItem(
              SortDirection.of(sourceOptions.isTechAssignmentTertiaryDescending()));
        refreshTechAssignmentEnablement();

        chkAssignBestCompanyCommander.setSelected(sourceOptions.isAssignBestCompanyCommander());
        chkPrioritizeCompanyCommanderCombatSkills.setSelected(sourceOptions.isPrioritizeCompanyCommanderCombatSkills());
        chkPrioritizeCompanyCommanderCombatSkills.setEnabled(chkAssignBestCompanyCommander.isSelected());

        chkAssignBestOfficers.setSelected(sourceOptions.isAssignBestOfficers());
        chkPrioritizeOfficerCombatSkills.setSelected(sourceOptions.isPrioritizeOfficerCombatSkills());
        chkPrioritizeOfficerCombatSkills.setEnabled(chkAssignBestOfficers.isSelected());

        chkAssignMostSkilledToPrimaryLances.setSelected(sourceOptions.isAssignMostSkilledToPrimaryLances());
        chkGenerateCaptains.setSelected(sourceOptions.isGenerateCaptains());
        chkAssignCompanyCommanderFlag.setSelected(sourceOptions.isAssignCompanyCommanderFlag());
        chkApplyOfficerStatBonusToWorstSkill.setSelected(sourceOptions.isApplyOfficerStatBonusToWorstSkill());

        chkAutomaticallyAssignRanks.setSelected(sourceOptions.isAutomaticallyAssignRanks());
        chkUseSpecifiedFactionToAssignRanks.setSelected(sourceOptions.isUseSpecifiedFactionToAssignRanks());
        chkUseSpecifiedFactionToAssignRanks.setEnabled(chkAutomaticallyAssignRanks.isSelected());
        chkAssignMekWarriorsCallSigns.setSelected(sourceOptions.isAssignMekWarriorsCallSigns());
        chkAssignFounderFlag.setSelected(sourceOptions.isAssignFounderFlag());

        loadAugmentationValues();
        loadTemporaryCrewValues();
    }

    /**
     * Fills the augmentation controls from the campaign, or from the last choice made where the
     * campaign has none of its own.
     *
     * <p>These mirror live campaign and game settings, so a campaign that has made a choice must see
     * it reported rather than overridden. A new campaign has made none - all three sit at their
     * all-off defaults - and seeding from those meant answering the same question again for every new
     * campaign, which is the one case where the remembered answer is the better one to show.</p>
     */
    private void loadAugmentationValues() {
        boolean tracksImplants = campaign.getCampaignOptions().get(CampaignOption.USE_IMPLANTS);
        boolean usesManeiDomini =
              campaign.getGameOptions().booleanOption(OptionsConstants.RPG_MANEI_DOMINI);
        NeuralInterfaceMode mode = NeuralInterfaceMode.from(campaign.getGameOptions());

        boolean campaignHasChosen = tracksImplants || usesManeiDomini || mode.isOn();
        if (!campaignHasChosen) {
            tracksImplants = MekHQ.getMHQOptions().getLastUseImplants();
            usesManeiDomini = MekHQ.getMHQOptions().getLastUseManeiDomini();
            mode = MekHQ.getMHQOptions().getLastNeuralInterfaceMode();
        }

        chkUseImplants.setSelected(tracksImplants);
        chkUseManeiDomini.setSelected(usesManeiDomini);
        cmbNeuralInterfaceMode.setSelectedItem(mode);
        refreshAugmentationEnablement();
    }

    /**
     * Shows the campaign's current temporary-crew settings. These are campaign options rather than generation
     * options, so what the campaign holds is what the player must see; the toggles are written back to the
     * campaign when the command is generated.
     */
    private void loadTemporaryCrewValues() {
        if (campaign == null) {
            return;
        }
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        for (Map.Entry<TemporaryCrewRole, CommandGenerationCheckBox> entry : chkTemporaryCrew.entrySet()) {
            entry.getValue().setSelected(campaignOptions.get(entry.getKey().getCampaignOption()));
        }
    }

    /**
     * Reads values back from this tab's controls into the supplied options. Same mapping as
     * {@link #loadValuesFromOptions} in reverse.
     */
    public void writeValuesToOptions(CommandGenerationOptions targetOptions) {
        if (targetOptions == null) {
            return;
        }
        Object selectedNamingMethod = comboForceNamingMethod.getSelectedItem();
        if (selectedNamingMethod instanceof ForceNamingMethod namingMethod) {
            targetOptions.setForceNamingMethod(namingMethod);
        }
        targetOptions.setAlwaysNumberRegiments(chkAlwaysNumberRegiments.isSelected());

        // Per-role coverage % and skill level
        Map<PersonnelRole, Integer> coverageMap = targetOptions.getSupportPersonnelCoveragePercents();
        for (Map.Entry<PersonnelRole, JSpinner> entry : spnSupportCoveragePercents.entrySet()) {
            coverageMap.put(entry.getKey(), (Integer) entry.getValue().getValue());
        }
        Map<PersonnelRole, SkillLevel> skillMap = targetOptions.getSupportPersonnelSkillLevels();
        for (Map.Entry<PersonnelRole, MMComboBox<SkillLevel>> entry : cmbSupportSkillLevels.entrySet()) {
            // A null selection is the "Random" option; store it as-is so the generator rolls per person.
            skillMap.put(entry.getKey(), entry.getValue().getSelectedItem());
        }

        // Assistants
        targetOptions.setGenerateAstechs(chkGenerateAstechs.isSelected());
        targetOptions.setAstechsAsPersonnel(rdoAstechsAsPersonnel.isSelected());
        // A null selection is the "Random" option; store it as-is.
        targetOptions.setAstechSkillLevel(cmbAstechSkillLevel.getSelectedItem());
        targetOptions.setGenerateMedics(chkGenerateMedics.isSelected());
        targetOptions.setMedicsAsPersonnel(rdoMedicsAsPersonnel.isSelected());
        targetOptions.setGenerateMedicalReserve(chkGenerateMedicalReserve.isSelected());
        targetOptions.setMedicalReservePercent((Integer) spnMedicalReservePercent.getValue());
        targetOptions.setMedicSkillLevel(cmbMedicSkillLevel.getSelectedItem());
        // Tech Assignment
        targetOptions.setAssignTechsToUnits(chkAssignTechsToUnits.isSelected());
        if (cmbTechAssignmentPrimary.getSelectedItem() instanceof TechAssignmentSortFactor f) {
            targetOptions.setTechAssignmentPrimarySort(f);
        }
        if (cmbTechAssignmentPrimaryDirection.getSelectedItem() instanceof SortDirection d) {
            targetOptions.setTechAssignmentPrimaryDescending(d.isDescending());
        }
        if (cmbTechAssignmentSecondary.getSelectedItem() instanceof TechAssignmentSortFactor f) {
            targetOptions.setTechAssignmentSecondarySort(f);
        }
        if (cmbTechAssignmentSecondaryDirection.getSelectedItem() instanceof SortDirection d) {
            targetOptions.setTechAssignmentSecondaryDescending(d.isDescending());
        }
        if (cmbTechAssignmentTertiary.getSelectedItem() instanceof TechAssignmentSortFactor f) {
            targetOptions.setTechAssignmentTertiarySort(f);
        }
        if (cmbTechAssignmentTertiaryDirection.getSelectedItem() instanceof SortDirection d) {
            targetOptions.setTechAssignmentTertiaryDescending(d.isDescending());
        }

        targetOptions.setAssignBestCompanyCommander(chkAssignBestCompanyCommander.isSelected());
        targetOptions.setPrioritizeCompanyCommanderCombatSkills(chkPrioritizeCompanyCommanderCombatSkills.isSelected());
        targetOptions.setAssignBestOfficers(chkAssignBestOfficers.isSelected());
        targetOptions.setPrioritizeOfficerCombatSkills(chkPrioritizeOfficerCombatSkills.isSelected());
        targetOptions.setAssignMostSkilledToPrimaryLances(chkAssignMostSkilledToPrimaryLances.isSelected());
        targetOptions.setGenerateCaptains(chkGenerateCaptains.isSelected());
        targetOptions.setAssignCompanyCommanderFlag(chkAssignCompanyCommanderFlag.isSelected());
        targetOptions.setApplyOfficerStatBonusToWorstSkill(chkApplyOfficerStatBonusToWorstSkill.isSelected());

        targetOptions.setAutomaticallyAssignRanks(chkAutomaticallyAssignRanks.isSelected());
        targetOptions.setUseSpecifiedFactionToAssignRanks(chkUseSpecifiedFactionToAssignRanks.isSelected());
        targetOptions.setAssignMekWarriorsCallSigns(chkAssignMekWarriorsCallSigns.isSelected());
        targetOptions.setAssignFounderFlag(chkAssignFounderFlag.isSelected());

        EnumSet<TemporaryCrewRole> temporaryCrewRoles = EnumSet.noneOf(TemporaryCrewRole.class);
        for (Map.Entry<TemporaryCrewRole, CommandGenerationCheckBox> entry : chkTemporaryCrew.entrySet()) {
            if (entry.getValue().isSelected()) {
                temporaryCrewRoles.add(entry.getKey());
            }
        }
        targetOptions.setTemporaryCrewRoles(temporaryCrewRoles);

        targetOptions.setUseImplants(chkUseImplants.isSelected());
        targetOptions.setUseManeiDomini(chkUseManeiDomini.isSelected());
        NeuralInterfaceMode mode = NeuralInterfaceMode.OFF;
        if (cmbNeuralInterfaceMode.getSelectedItem() instanceof NeuralInterfaceMode selected) {
            mode = selected;
            targetOptions.setNeuralInterfaceMode(selected);
        }

        // Remembered as the player's own answer, so the next new campaign opens on it rather than
        // asking again. The campaign still holds its own copy; this only decides what a campaign that
        // has chosen nothing is shown.
        MekHQ.getMHQOptions().setLastUseImplants(chkUseImplants.isSelected());
        MekHQ.getMHQOptions().setLastUseManeiDomini(chkUseManeiDomini.isSelected());
        MekHQ.getMHQOptions().setLastNeuralInterfaceMode(mode);
    }

    public RandomOriginOptionsPanel getRandomOriginOptionsPanel() {
        return randomOriginOptionsPanel;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public CommandGenerationOptions getOptions() {
        return options;
    }
}
