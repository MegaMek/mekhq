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

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.processWrapSize;
import static mekhq.gui.commandGeneration.components.CommandGenerationUtilities.getCommandGenerationResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

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
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
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

    /** Coverage spinner range. 100 = full canonical coverage; >100 = redundancy. */
    private static final int COVERAGE_SPINNER_MIN = 0;
    private static final int COVERAGE_SPINNER_MAX = 300;
    private static final int COVERAGE_SPINNER_STEP = 5;
    private static final int COVERAGE_SPINNER_DEFAULT = 100;

    /**
     * Skill-level options offered for support personnel. Five tiers — Ultra-Green through Elite.
     * Heroic / Legendary are deliberately excluded since they're reserved for one-off Person
     * customizations, not bulk generation.
     */
    private static final SkillLevel[] SUPPORT_SKILL_LEVELS = {
          SkillLevel.ULTRA_GREEN,
          SkillLevel.GREEN,
          SkillLevel.REGULAR,
          SkillLevel.VETERAN,
          SkillLevel.ELITE
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
    private CommandGenerationCheckBox chkGenerateMercenaryCompanyCommandLance;
    private MMComboBox<ForceNamingMethod> comboForceNamingMethod;

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
          PersonnelRole.ADMINISTRATOR_COMMAND,
          PersonnelRole.ADMINISTRATOR_LOGISTICS,
          PersonnelRole.ADMINISTRATOR_TRANSPORT,
          PersonnelRole.ADMINISTRATOR_HR
    };

    public JPanel createTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setName("pnlSetupTab");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(3, 6, 3, 6);

        // Row 0: Force Shape spans the full width — it's only two controls so giving it both
        // columns avoids leaving an awkward gap on the right.
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        panel.add(buildForceShapeSection(), gbc);

        // Row 1: Support Personnel spans both columns. With the new role/percent/skill grid the
        // section is wider than the column-split could comfortably hold, and giving it the full
        // width lets the headers and per-role rows breathe.
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        panel.add(buildSupportPersonnelSection(), gbc);

        // Row 2: left column stacks Assistants + Naming & Ranks; right column stacks Officer
        // Selection + Tech Assignment. Officer Selection is the tallest single section in the tab,
        // and Tech Assignment is small (4 rows), so the right-column stack roughly matches the
        // height of the left-column pair.
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.add(buildAssistantsSection());
        leftColumn.add(Box.createVerticalStrut(6));
        leftColumn.add(buildNamingAndRanksSection());
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        panel.add(leftColumn, gbc);

        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.add(buildOfficerSelectionSection());
        rightColumn.add(Box.createVerticalStrut(6));
        rightColumn.add(buildTechAssignmentSection());
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        panel.add(rightColumn, gbc);

        // Row 3: Random Origin spans full width — it's a dense sub-panel with its own internal
        // layout, so giving it the whole width keeps its controls from being cramped.
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        panel.add(buildRandomOriginSection(), gbc);

        return panel;
    }

    private JPanel buildForceShapeSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "ForceShape", true, "ForceShape");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();

        chkGenerateMercenaryCompanyCommandLance =
              new CommandGenerationCheckBox("GenerateMercenaryCompanyCommandLance");

        comboForceNamingMethod = new MMComboBox<>("comboForceNamingMethod", ForceNamingMethod.values());
        comboForceNamingMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                  int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ForceNamingMethod m) {
                    // Append the first-three preview inline so the user can see what each scheme
                    // produces without hovering for the tooltip.
                    setText(m.toString() + " — " + m.getExample());
                    list.setToolTipText(m.getToolTipText());
                }
                return this;
            }
        });

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        section.add(chkGenerateMercenaryCompanyCommandLance, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        section.add(new CommandGenerationLabel("ForceNamingMethod"), gbc);
        gbc.gridx = 1;
        section.add(comboForceNamingMethod, gbc);

        addLeftAlignFiller(section, 2);
        return section;
    }

    private JPanel buildSupportPersonnelSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "SupportPersonnel", true, "SupportPersonnel");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();

        spnSupportCoveragePercents.clear();
        cmbSupportSkillLevels.clear();

        String spinnerTooltipTemplate = getTextAt(getCommandGenerationResourceBundle(),
              "supportCoveragePercent.toolTipText");
        String skillTooltipTemplate = getTextAt(getCommandGenerationResourceBundle(),
              "supportSkillLevel.toolTipText");

        // Two logical columns of (role label, % spinner, skill dropdown). Layout grid columns:
        // [label][%][skill]   [label][%][skill]  → 6 columns total. Tech roles populate the left
        // column, admin roles the right. Doctor anchors the bottom of the left column so the two
        // sides share the visual baseline.
        int halfCount = (SUPPORT_ROLES.length + 1) / 2;

        // Header row labels
        addColumnHeader(section, gbc, 0, 0, "lblSupportPersonnelColumnRole.text");
        addColumnHeader(section, gbc, 1, 0, "lblSupportPersonnelColumnPercent.text");
        addColumnHeader(section, gbc, 2, 0, "lblSupportPersonnelColumnSkill.text");
        addColumnHeader(section, gbc, 3, 0, "lblSupportPersonnelColumnRole.text");
        addColumnHeader(section, gbc, 4, 0, "lblSupportPersonnelColumnPercent.text");
        addColumnHeader(section, gbc, 5, 0, "lblSupportPersonnelColumnSkill.text");

        for (int i = 0; i < SUPPORT_ROLES.length; i++) {
            PersonnelRole role = SUPPORT_ROLES[i];
            String roleDisplay = role.getLabel(campaign != null && campaign.getFaction().isClan());

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

            MMComboBox<SkillLevel> skillCombo = new MMComboBox<>(
                  "cmbSupportSkill" + role.name(), SUPPORT_SKILL_LEVELS);
            skillCombo.setSelectedItem(SkillLevel.REGULAR);
            if (skillTooltipTemplate != null && !skillTooltipTemplate.isEmpty()) {
                skillCombo.setToolTipText(String.format(skillTooltipTemplate, roleDisplay));
            }
            cmbSupportSkillLevels.put(role, skillCombo);

            boolean leftColumn = i < halfCount;
            int baseX = leftColumn ? 0 : 3;
            int row = (leftColumn ? i : i - halfCount) + 1; // +1 for header
            gbc.gridy = row;
            gbc.gridx = baseX;
            section.add(roleLabel, gbc);
            gbc.gridx = baseX + 1;
            section.add(spinner, gbc);
            gbc.gridx = baseX + 2;
            section.add(skillCombo, gbc);
        }

        addLeftAlignFiller(section, 6);
        return section;
    }

    private static void addColumnHeader(JPanel section, GridBagConstraints gbc, int gridX, int gridY,
          String bundleKey) {
        JLabel header = new JLabel(
              "<html><b>" + getTextAt(getCommandGenerationResourceBundle(), bundleKey) + "</b></html>");
        gbc.gridx = gridX;
        gbc.gridy = gridY;
        section.add(header, gbc);
    }

    private JPanel buildAssistantsSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "Assistants", true, "Assistants");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();

        // Astech block
        chkGenerateAstechs = new CommandGenerationCheckBox("GenerateAstechs");
        rdoAstechsAsPool = makeStyledRadio("AstechsAsPool");
        rdoAstechsAsPersonnel = makeStyledRadio("AstechsAsPersonnel");
        ButtonGroup astechGroup = new ButtonGroup();
        astechGroup.add(rdoAstechsAsPool);
        astechGroup.add(rdoAstechsAsPersonnel);
        rdoAstechsAsPool.setSelected(true);
        cmbAstechSkillLevel = new MMComboBox<>("cmbAstechSkillLevel", SUPPORT_SKILL_LEVELS);
        cmbAstechSkillLevel.setSelectedItem(SkillLevel.REGULAR);

        // Medic block
        chkGenerateMedics = new CommandGenerationCheckBox("GenerateMedics");
        rdoMedicsAsPool = makeStyledRadio("MedicsAsPool");
        rdoMedicsAsPersonnel = makeStyledRadio("MedicsAsPersonnel");
        ButtonGroup medicGroup = new ButtonGroup();
        medicGroup.add(rdoMedicsAsPool);
        medicGroup.add(rdoMedicsAsPersonnel);
        rdoMedicsAsPool.setSelected(true);
        cmbMedicSkillLevel = new MMComboBox<>("cmbMedicSkillLevel", SUPPORT_SKILL_LEVELS);
        cmbMedicSkillLevel.setSelectedItem(SkillLevel.REGULAR);

        // Enable/disable wiring: parent checkbox controls the radios + skill dropdown;
        // "as Personnel" radio controls whether the skill dropdown is live.
        chkGenerateAstechs.addActionListener(evt -> refreshAstechEnablement());
        rdoAstechsAsPool.addActionListener(evt -> refreshAstechEnablement());
        rdoAstechsAsPersonnel.addActionListener(evt -> refreshAstechEnablement());
        chkGenerateMedics.addActionListener(evt -> refreshMedicEnablement());
        rdoMedicsAsPool.addActionListener(evt -> refreshMedicEnablement());
        rdoMedicsAsPersonnel.addActionListener(evt -> refreshMedicEnablement());

        // Layout
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        section.add(chkGenerateAstechs, gbc);
        gbc.gridwidth = 1;
        indentAsSubOption(rdoAstechsAsPool);
        gbc.gridy = 1;
        gbc.gridx = 0;
        section.add(rdoAstechsAsPool, gbc);
        indentAsSubOption(rdoAstechsAsPersonnel);
        gbc.gridy = 2;
        gbc.gridx = 0;
        section.add(rdoAstechsAsPersonnel, gbc);
        gbc.gridx = 1;
        section.add(cmbAstechSkillLevel, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        section.add(chkGenerateMedics, gbc);
        gbc.gridwidth = 1;
        indentAsSubOption(rdoMedicsAsPool);
        gbc.gridy = 4;
        gbc.gridx = 0;
        section.add(rdoMedicsAsPool, gbc);
        indentAsSubOption(rdoMedicsAsPersonnel);
        gbc.gridy = 5;
        gbc.gridx = 0;
        section.add(rdoMedicsAsPersonnel, gbc);
        gbc.gridx = 1;
        section.add(cmbMedicSkillLevel, gbc);

        // Medical reserve: generation-only spare MekWarriors as injury replacements, sized by a
        // percentage of the generated combatants. The spinner is live only when the box is checked.
        chkGenerateMedicalReserve = new CommandGenerationCheckBox("GenerateMedicalReserve");
        spnMedicalReservePercent = new JSpinner(new SpinnerNumberModel(10, 0, 100, 5));
        chkGenerateMedicalReserve.addActionListener(evt ->
              spnMedicalReservePercent.setEnabled(chkGenerateMedicalReserve.isSelected()));
        gbc.gridy = 6;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        section.add(chkGenerateMedicalReserve, gbc);
        gbc.gridx = 1;
        section.add(spnMedicalReservePercent, gbc);

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
        GridBagConstraints gbc = sectionConstraints();

        chkAssignTechsToUnits = new CommandGenerationCheckBox("AssignTechsToUnits");
        cmbTechAssignmentPrimary = new MMComboBox<>("cmbTechAssignmentPrimary", SORT_FACTORS);
        cmbTechAssignmentSecondary = new MMComboBox<>("cmbTechAssignmentSecondary", SORT_FACTORS);
        cmbTechAssignmentTertiary = new MMComboBox<>("cmbTechAssignmentTertiary", SORT_FACTORS);
        cmbTechAssignmentPrimaryDirection = new MMComboBox<>("cmbTechAssignmentPrimaryDirection", SORT_DIRECTIONS);
        cmbTechAssignmentSecondaryDirection = new MMComboBox<>("cmbTechAssignmentSecondaryDirection", SORT_DIRECTIONS);
        cmbTechAssignmentTertiaryDirection = new MMComboBox<>("cmbTechAssignmentTertiaryDirection", SORT_DIRECTIONS);

        chkAssignTechsToUnits.addActionListener(evt -> refreshTechAssignmentEnablement());
        // Each factor combo toggles its own direction combo when set to NONE.
        cmbTechAssignmentPrimary.addActionListener(evt -> refreshTechAssignmentEnablement());
        cmbTechAssignmentSecondary.addActionListener(evt -> refreshTechAssignmentEnablement());
        cmbTechAssignmentTertiary.addActionListener(evt -> refreshTechAssignmentEnablement());

        // Row 0: master enable checkbox spans all columns.
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        section.add(chkAssignTechsToUnits, gbc);
        gbc.gridwidth = 1;

        // Rows 1-3: three sort slots, each (label, factor combo, direction combo).
        addTechSortRow(section, gbc, 1, "TechAssignmentPrimary",
              cmbTechAssignmentPrimary, cmbTechAssignmentPrimaryDirection);
        addTechSortRow(section, gbc, 2, "TechAssignmentSecondary",
              cmbTechAssignmentSecondary, cmbTechAssignmentSecondaryDirection);
        addTechSortRow(section, gbc, 3, "TechAssignmentTertiary",
              cmbTechAssignmentTertiary, cmbTechAssignmentTertiaryDirection);

        addLeftAlignFiller(section, 3);
        return section;
    }

    private static void addTechSortRow(JPanel section, GridBagConstraints gbc, int row,
          String labelName, MMComboBox<TechAssignmentSortFactor> factorCombo,
          MMComboBox<SortDirection> directionCombo) {
        CommandGenerationLabel label = new CommandGenerationLabel(labelName);
        indentAsSubOption(label);
        gbc.gridy = row;
        gbc.gridx = 0;
        section.add(label, gbc);
        gbc.gridx = 1;
        section.add(factorCombo, gbc);
        gbc.gridx = 2;
        section.add(directionCombo, gbc);
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
        GridBagConstraints gbc = sectionConstraints();
        gbc.gridwidth = 1;

        chkAssignBestCompanyCommander = new CommandGenerationCheckBox("AssignBestCompanyCommander");
        chkPrioritizeCompanyCommanderCombatSkills =
              new CommandGenerationCheckBox("PrioritizeCompanyCommanderCombatSkills");
        indentAsSubOption(chkPrioritizeCompanyCommanderCombatSkills);
        chkAssignBestCompanyCommander.addActionListener(evt ->
              chkPrioritizeCompanyCommanderCombatSkills.setEnabled(chkAssignBestCompanyCommander.isSelected()));

        chkAssignBestOfficers = new CommandGenerationCheckBox("AssignBestOfficers");
        chkPrioritizeOfficerCombatSkills =
              new CommandGenerationCheckBox("PrioritizeOfficerCombatSkills");
        indentAsSubOption(chkPrioritizeOfficerCombatSkills);
        chkAssignBestOfficers.addActionListener(evt ->
              chkPrioritizeOfficerCombatSkills.setEnabled(chkAssignBestOfficers.isSelected()));

        chkAssignMostSkilledToPrimaryLances = new CommandGenerationCheckBox("AssignMostSkilledToPrimaryLances");
        chkGenerateCaptains = new CommandGenerationCheckBox("GenerateCaptains");
        chkAssignCompanyCommanderFlag = new CommandGenerationCheckBox("AssignCompanyCommanderFlag");
        chkApplyOfficerStatBonusToWorstSkill =
              new CommandGenerationCheckBox("ApplyOfficerStatBonusToWorstSkill");

        stack(section, gbc,
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
        GridBagConstraints gbc = sectionConstraints();
        gbc.gridwidth = 1;

        chkAutomaticallyAssignRanks = new CommandGenerationCheckBox("AutomaticallyAssignRanks");
        chkUseSpecifiedFactionToAssignRanks = new CommandGenerationCheckBox("UseSpecifiedFactionToAssignRanks");
        indentAsSubOption(chkUseSpecifiedFactionToAssignRanks);
        chkAutomaticallyAssignRanks.addActionListener(evt ->
              chkUseSpecifiedFactionToAssignRanks.setEnabled(chkAutomaticallyAssignRanks.isSelected()));
        chkAssignMekWarriorsCallSigns = new CommandGenerationCheckBox("AssignMekWarriorsCallSigns");
        chkAssignFounderFlag = new CommandGenerationCheckBox("AssignFounderFlag");

        stack(section, gbc,
              chkAutomaticallyAssignRanks,
              chkUseSpecifiedFactionToAssignRanks,
              chkAssignMekWarriorsCallSigns,
              chkAssignFounderFlag);

        addLeftAlignFiller(section, 1);
        return section;
    }

    private JPanel buildRandomOriginSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "RandomOrigin", true, "RandomOrigin");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        randomOriginOptionsPanel = new RandomOriginOptionsPanel(null, campaign,
              campaign == null ? null : campaign.getFaction());
        randomOriginOptionsPanel.setBorder(BorderFactory.createEmptyBorder());
        section.add(randomOriginOptionsPanel, gbc);

        return section;
    }

    private static GridBagConstraints sectionConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 6, 3, 6);
        return gbc;
    }

    private static void stack(JPanel section, GridBagConstraints gbc, JComponent... components) {
        gbc.gridx = 0;
        for (int i = 0; i < components.length; i++) {
            gbc.gridy = i;
            section.add(components[i], gbc);
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
     * Pushes values from the supplied options onto this tab's controls. Field-by-field copy.
     */
    public void loadValuesFromOptions(CommandGenerationOptions sourceOptions) {
        this.options = sourceOptions;
        if (sourceOptions == null) {
            return;
        }
        chkGenerateMercenaryCompanyCommandLance.setSelected(sourceOptions.isGenerateMercenaryCompanyCommandLance());
        comboForceNamingMethod.setSelectedItem(sourceOptions.getForceNamingMethod());

        // Per-role coverage % and skill level
        for (Map.Entry<PersonnelRole, JSpinner> entry : spnSupportCoveragePercents.entrySet()) {
            Integer percent = sourceOptions.getSupportPersonnelCoveragePercents().get(entry.getKey());
            entry.getValue().setValue(percent == null ? COVERAGE_SPINNER_DEFAULT : percent);
        }
        for (Map.Entry<PersonnelRole, MMComboBox<SkillLevel>> entry : cmbSupportSkillLevels.entrySet()) {
            SkillLevel level = sourceOptions.getSupportPersonnelSkillLevels().get(entry.getKey());
            entry.getValue().setSelectedItem(level == null ? SkillLevel.REGULAR : level);
        }

        // Assistants
        chkGenerateAstechs.setSelected(sourceOptions.isGenerateAstechs());
        if (sourceOptions.isAstechsAsPersonnel()) {
            rdoAstechsAsPersonnel.setSelected(true);
        } else {
            rdoAstechsAsPool.setSelected(true);
        }
        cmbAstechSkillLevel.setSelectedItem(
              sourceOptions.getAstechSkillLevel() == null
                    ? SkillLevel.REGULAR : sourceOptions.getAstechSkillLevel());
        refreshAstechEnablement();

        chkGenerateMedics.setSelected(sourceOptions.isGenerateMedics());
        if (sourceOptions.isMedicsAsPersonnel()) {
            rdoMedicsAsPersonnel.setSelected(true);
        } else {
            rdoMedicsAsPool.setSelected(true);
        }
        cmbMedicSkillLevel.setSelectedItem(
              sourceOptions.getMedicSkillLevel() == null
                    ? SkillLevel.REGULAR : sourceOptions.getMedicSkillLevel());
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
    }

    /**
     * Reads values back from this tab's controls into the supplied options. Same mapping as
     * {@link #loadValuesFromOptions} in reverse.
     */
    public void writeValuesToOptions(CommandGenerationOptions targetOptions) {
        if (targetOptions == null) {
            return;
        }
        targetOptions.setGenerateMercenaryCompanyCommandLance(chkGenerateMercenaryCompanyCommandLance.isSelected());
        Object selectedNamingMethod = comboForceNamingMethod.getSelectedItem();
        if (selectedNamingMethod instanceof ForceNamingMethod m) {
            targetOptions.setForceNamingMethod(m);
        }

        // Per-role coverage % and skill level
        Map<PersonnelRole, Integer> coverageMap = targetOptions.getSupportPersonnelCoveragePercents();
        for (Map.Entry<PersonnelRole, JSpinner> entry : spnSupportCoveragePercents.entrySet()) {
            coverageMap.put(entry.getKey(), (Integer) entry.getValue().getValue());
        }
        Map<PersonnelRole, SkillLevel> skillMap = targetOptions.getSupportPersonnelSkillLevels();
        for (Map.Entry<PersonnelRole, MMComboBox<SkillLevel>> entry : cmbSupportSkillLevels.entrySet()) {
            Object selected = entry.getValue().getSelectedItem();
            if (selected instanceof SkillLevel s) {
                skillMap.put(entry.getKey(), s);
            }
        }

        // Assistants
        targetOptions.setGenerateAstechs(chkGenerateAstechs.isSelected());
        targetOptions.setAstechsAsPersonnel(rdoAstechsAsPersonnel.isSelected());
        if (cmbAstechSkillLevel.getSelectedItem() instanceof SkillLevel s) {
            targetOptions.setAstechSkillLevel(s);
        }
        targetOptions.setGenerateMedics(chkGenerateMedics.isSelected());
        targetOptions.setMedicsAsPersonnel(rdoMedicsAsPersonnel.isSelected());
        targetOptions.setGenerateMedicalReserve(chkGenerateMedicalReserve.isSelected());
        targetOptions.setMedicalReservePercent((Integer) spnMedicalReservePercent.getValue());
        if (cmbMedicSkillLevel.getSelectedItem() instanceof SkillLevel s) {
            targetOptions.setMedicSkillLevel(s);
        }
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
