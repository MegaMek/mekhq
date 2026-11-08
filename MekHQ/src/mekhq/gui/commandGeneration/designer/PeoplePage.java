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
package mekhq.gui.commandGeneration.designer;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.commandGeneration.designer.DesignerUi.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jakarta.annotation.Nullable;
import megamek.client.ratgenerator.CrewDescriptor;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.enums.NeuralInterfaceMode;
import megamek.common.enums.SkillLevel;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.SupportUnitGenerator;
import mekhq.campaign.universe.commandGeneration.ratgen.SupportPersonnelCalculator;
import mekhq.campaign.universe.commandGeneration.ratgen.SupportPersonnelCalculator.SupportDemand;
import mekhq.campaign.universe.commandGeneration.ratgen.SupportPersonnelGenerator;
import mekhq.campaign.universe.enums.ForceNamingMethod;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.roleplay.SearchablePicker;
import mekhq.gui.utilities.SkillLevelPickerUtility;

/**
 * The Command Designer's People page: who leads and supports the command.
 *
 * <p>Three columns: leadership, with a preview of the chain of command the roll designates and the history the
 * command can be given before play; support staff, with the
 * Campaign Operations need for the rolled command beside each role's coverage and what that coverage hires; and the
 * names, ranks, origin and augmentation the people are generated with.</p>
 */
public final class PeoplePage {
    /** Wrap width for notes in the narrow outer columns, before GUI scaling; fits inside their padding. */
    private static final int WRAP_WIDTH = 230;
    private static final int COVERAGE_DEFAULT = 100;
    private static final int ASTECHS_PER_TECH = 6;
    private static final int MEDICS_PER_DOCTOR = 4;
    private static final int CHAIN_ROWS = 8;
    private static final int MIN_SIMULATION_YEARS = 1;
    private static final int MAX_SIMULATION_YEARS = 50;
    /** Column widths before GUI scaling: the outer two fit their longest option, the middle takes what is left. */
    private static final int LEADERSHIP_WIDTH = 400;
    private static final int SUPPORT_WIDTH = 560;
    private static final int NAMES_WIDTH = 330;
    private static final PersonnelRole[] SUPPORT_ROLES = {
          PersonnelRole.MEK_TECH, PersonnelRole.MECHANIC, PersonnelRole.AERO_TEK, PersonnelRole.BA_TECH,
          PersonnelRole.DOCTOR, PersonnelRole.ADMINISTRATOR };

    /** How assistants are generated. */
    enum AssistantMode {
        POOL, PEOPLE, NONE
    }

    private final Campaign campaign;
    private final Supplier<ForceSummary> summary;
    private final Function<ForceDescriptor, String> formationNamer;
    private final Runnable onChanged;
    private final JPanel root = new JPanel(new GridBagLayout());

    // Leadership
    private final HudCheckBox bestCommander = optionCheck("AssignBestCompanyCommander");
    private final HudCheckBox commanderCombat = optionCheck("PrioritizeCompanyCommanderCombatSkills");
    private final HudCheckBox bestOfficers = optionCheck("AssignBestOfficers");
    private final HudCheckBox officerCombat = optionCheck("PrioritizeOfficerCombatSkills");
    private final HudCheckBox mostSkilled = optionCheck("AssignMostSkilledToPrimaryLances");
    private final HudCheckBox captains = optionCheck("GenerateCaptains");
    private final HudCheckBox statBonus = optionCheck("ApplyOfficerStatBonusToWorstSkill");
    private final JPanel chain = column();

    // History
    private final HudCheckBox runSimulation = optionCheck("RunStartingSimulation");
    private final HudStepper simulationYears;
    private final HudCheckBox marriages = optionCheck("SimulateRandomMarriages");
    private final HudCheckBox procreation = optionCheck("SimulateRandomProcreation");

    // Support staff
    private final Map<PersonnelRole, HudStepper> coverage = new EnumMap<>(PersonnelRole.class);
    private final Map<PersonnelRole, HudChoice<SkillLevel>> skills = new EnumMap<>(PersonnelRole.class);
    private final Map<PersonnelRole, JLabel> needLabels = new EnumMap<>(PersonnelRole.class);
    private final Map<PersonnelRole, JLabel> hireLabels = new EnumMap<>(PersonnelRole.class);
    private final Map<PersonnelRole, Integer> hires = new EnumMap<>(PersonnelRole.class);
    private final JPanel hrNote = column();
    private final HudChoice<AssistantMode> astechMode;
    private final HudChoice<AssistantMode> medicMode;
    private final HudChoice<SkillLevel> astechSkill = skillChoice();
    private final HudChoice<SkillLevel> medicSkill = skillChoice();
    private JComponent astechSkillRow;
    private JComponent medicSkillRow;
    private final JLabel astechCaption = caption("");
    private final JLabel medicCaption = caption("");
    private final HudCheckBox reserve = optionCheck("GenerateMedicalReserve");
    private final HudStepper reservePercent;
    private final JLabel reserveLine = muted("");

    // Names and ranks
    private final HudChoice<ForceNamingMethod> naming;
    private final HudCheckBox numberRegiments = optionCheck("AlwaysNumberRegiments");
    private final HudCheckBox autoRanks = optionCheck("AutomaticallyAssignRanks");
    private final HudCheckBox callsigns = optionCheck("AssignMekWarriorsCallSigns");
    private final HudCheckBox founders = optionCheck("AssignFounderFlag");
    private final HudCheckBox commanderFlag = optionCheck("AssignCompanyCommanderFlag");
    private @Nullable Runnable namingChangeListener;

    // Origin
    private final HudCheckBox randomizeOrigin = check(text("Designer.people.origin.randomize"),
          text("Designer.people.origin.randomize.tooltip"));
    private final HudCheckBox dependents = check(text("Designer.people.origin.dependents"),
          text("Designer.people.origin.dependents.tooltip"));
    private final HudCheckBox extraRandom = check(text("Designer.people.origin.extraRandom"),
          text("Designer.people.origin.extraRandom.tooltip"));
    private final HudCheckBox clanOrigins = check(text("Designer.people.origin.clan"),
          text("Designer.people.origin.clan.tooltip"));
    private final HudCheckBox aroundPlanet = check(text("Designer.people.origin.around"),
          text("Designer.people.origin.around.tooltip"));
    private final SearchablePicker<PlanetarySystem> specifiedSystem;
    private final HudStepper radius;
    private final HudStepper distanceScale;
    private final JPanel radiusNote = column();
    private @Nullable Planet specifiedPlanet;

    // Augmentation
    private final HudCheckBox implants = optionCheck("UseImplants");
    private final HudChoice<NeuralInterfaceMode> neuralMode;
    private final JPanel augmentationBody = column();
    private final JComponent augmentationLocked = wrapped(text("Designer.people.augmentation.locked"), WRAP_WIDTH);
    private boolean augmentationVisible;

    /**
     * @param campaign       the campaign the command is built into
     * @param summary        the rolled command, as the Force page sums it up
     * @param formationNamer the name a rolled formation will have in the TO&amp;E
     * @param onChanged      told when a choice here changes what the tiles or Review show
     */
    public PeoplePage(Campaign campaign, Supplier<ForceSummary> summary,
          Function<ForceDescriptor, String> formationNamer, Runnable onChanged) {
        this.campaign = campaign;
        this.summary = summary;
        this.formationNamer = formationNamer;
        this.onChanged = onChanged;

        astechMode = assistantChoice();
        medicMode = assistantChoice();
        reservePercent = new HudStepper(0, 100, 5, value -> formatted("Designer.percent", value), 56,
              value -> changed());
        simulationYears = new HudStepper(MIN_SIMULATION_YEARS, MAX_SIMULATION_YEARS, 1,
              value -> formatted("Designer.people.history.years", value), 84, value -> changed());
        List<HudChoice.Choice<ForceNamingMethod>> namingChoices = new ArrayList<>();
        for (ForceNamingMethod method : ForceNamingMethod.values()) {
            namingChoices.add(new HudChoice.Choice<>(method, method.toString()));
        }
        naming = new HudChoice<>(0, method -> {
            if (namingChangeListener != null) {
                namingChangeListener.run();
            }
        });
        naming.setChoices(namingChoices, ForceNamingMethod.ICAO_1956);
        LocalDate today = campaign.getLocalDate();
        specifiedSystem = new SearchablePicker<>("designerSpecifiedSystem", system -> system.getName(today),
              () -> specifiedPlanet = null);
        List<PlanetarySystem> systems = new ArrayList<>(campaign.getSystems());
        systems.sort(Comparator.comparing(system -> system.getName(today)));
        specifiedSystem.setItems(systems, null);
        radius = new HudStepper(0, 2000, 25, value -> formatted("Designer.people.origin.radius.value", value), 72,
              value -> refreshOrigin());
        distanceScale = new HudStepper(1, 20, 1, value -> formatted("Designer.people.origin.scale.value",
              value / 10.0), 56, value -> { });
        List<HudChoice.Choice<NeuralInterfaceMode>> neuralChoices = new ArrayList<>();
        for (NeuralInterfaceMode mode : NeuralInterfaceMode.values()) {
            neuralChoices.add(new HudChoice.Choice<>(mode, mode.optionValue()));
        }
        neuralMode = new HudChoice<>(4, mode -> { });
        neuralMode.setChoices(neuralChoices, NeuralInterfaceMode.OFF);

        root.setOpaque(true);
        root.setBackground(GROUND);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1.0;
        constraints.gridy = 0;
        // The outer columns are sized to what they hold; the support table in the middle takes the rest.
        constraints.gridx = 0;
        constraints.weightx = 0.0;
        root.add(scrollingColumn(buildLeadership(), SURFACE_DEEP, false, LEADERSHIP_WIDTH), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        root.add(scrollingColumn(buildSupport(), GROUND, true, SUPPORT_WIDTH), constraints);
        constraints.gridx = 2;
        constraints.weightx = 0.0;
        root.add(scrollingColumn(buildNamesAndOrigin(), SURFACE_DEEP, true, NAMES_WIDTH), constraints);

        wireEnablement();
    }

    public JComponent getComponent() {
        return root;
    }

    private static JComponent scrollingColumn(JPanel content, Color background, boolean ruleOnLeft, int width) {
        padded(content, 12, 16, 18, 16);
        JComponent scroll = scroll(content, background);
        Dimension size = new Dimension(scaleForGUI(width), scaleForGUI(400));
        scroll.setPreferredSize(size);
        // The outer columns keep their width when the window is tight, rather than being squeezed with the rest.
        scroll.setMinimumSize(new Dimension(scaleForGUI(Math.min(width, NAMES_WIDTH)), scaleForGUI(200)));
        scroll.setBorder(ruleOnLeft ? BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER)
              : BorderFactory.createEmptyBorder());
        return scroll;
    }

    // region Layout

    private JPanel buildLeadership() {
        return stack(
              section(text("Designer.people.section.leadership"), null,
                    bestCommander, indented(commanderCombat), bestOfficers, indented(officerCombat), mostSkilled,
                    captains, statBonus),
              section(text("Designer.people.section.chain"), text("Designer.people.section.chain.aside"), chain),
              section(text("Designer.people.section.history"), text("Designer.people.section.history.aside"),
                    runSimulation, indented(field(text("lblSimulationDuration.text"), simulationYears)),
                    indented(marriages), indented(procreation)));
    }

    private JPanel buildSupport() {
        JPanel table = Hud.transparentPanel(new GridBagLayout());
        GridBagConstraints cell = new GridBagConstraints();
        cell.gridy = 0;
        cell.anchor = GridBagConstraints.WEST;
        cell.insets = new Insets(0, 0, scaleForGUI(6), scaleForGUI(10));
        // The rank band rides on the role's second line: a column of its own ran off the edge of the page.
        String[] headings = { "role", "coverage", "need", "hire", "skill" };
        for (int index = 0; index < headings.length; index++) {
            cell.gridx = index;
            table.add(caption(text("Designer.people.support.column." + headings[index])), cell);
        }
        for (PersonnelRole role : SUPPORT_ROLES) {
            cell.gridy++;
            HudStepper stepper = new HudStepper(0, 1000, 5, value -> formatted("Designer.percent", value), 56,
                  value -> changed());
            stepper.setToolTipText(text("supportCoveragePercent.toolTipText"));
            stepper.setValue(COVERAGE_DEFAULT);
            coverage.put(role, stepper);
            HudChoice<SkillLevel> skill = skillChoice();
            skill.setToolTipText(text("supportSkillLevel.toolTipText"));
            skills.put(role, skill);
            JLabel need = new JLabel();
            need.setForeground(TEXT);
            needLabels.put(role, need);
            JLabel hire = new JLabel();
            hire.setForeground(ACCENT_BRIGHT);
            hire.setFont(hudFont(Font.BOLD, 0.95f, 0.0f));
            hireLabels.put(role, hire);

            // Plain labels rather than HTML: an HTML label reports almost no minimum width, so a crowded table
            // squeezed the role names down to a letter or two.
            JLabel roleName = new JLabel(role.getLabel(false));
            roleName.setForeground(TEXT);
            roleName.setFont(hudFont(Font.BOLD, 0.92f, 0.0f));
            JLabel roleBasis = faint(formatted("Designer.people.support.roleLine",
                  text("Designer.people.support.basis." + role.name()), rankBand(role)));
            JPanel roleCell = column();
            roleCell.add(leftAligned(roleName));
            roleCell.add(leftAligned(roleBasis));

            cell.gridx = 0;
            cell.weightx = 1.0;
            cell.fill = GridBagConstraints.HORIZONTAL;
            table.add(roleCell, cell);
            cell.weightx = 0.0;
            cell.fill = GridBagConstraints.NONE;
            cell.gridx = 1;
            table.add(stepper, cell);
            cell.gridx = 2;
            table.add(need, cell);
            cell.gridx = 3;
            table.add(hire, cell);
            cell.gridx = 4;
            table.add(skill, cell);
        }

        astechSkillRow = indented(field(text("Designer.people.assistants.skill"), astechSkill));
        medicSkillRow = indented(field(text("Designer.people.assistants.skill"), medicSkill));
        JPanel astechs = column();
        addRows(astechs, astechCaption, astechMode, astechSkillRow);
        JPanel medics = column();
        addRows(medics, medicCaption, medicMode, medicSkillRow);
        JPanel assistants = Hud.transparentPanel(new GridLayout(1, 2, scaleForGUI(12), 0));
        assistants.add(astechs);
        assistants.add(medics);

        JPanel reserveRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        reserveRow.add(reserve, BorderLayout.CENTER);
        reserveRow.add(reservePercent, BorderLayout.EAST);
        reserveRow.setMaximumSize(new Dimension(Short.MAX_VALUE, reserveRow.getPreferredSize().height));

        return stack(
              section(text("Designer.people.section.support"), text("Designer.people.section.support.aside"),
                    table, hrNote),
              section(text("Designer.people.section.assistants"), null, assistants),
              section(text("Designer.people.section.reserve"), text("Designer.people.section.reserve.aside"),
                    reserveRow, reserveLine));
    }

    private JPanel buildNamesAndOrigin() {
        augmentationBody.removeAll();
        addRows(augmentationBody, implants, indented(field(text("lblNeuralInterfaceMode.text"), neuralMode)));
        JPanel augmentation = column();
        addRows(augmentation, augmentationBody, augmentationLocked);
        JPanel scaleRow = Hud.transparentPanel(new GridLayout(1, 2, scaleForGUI(10), 0));
        scaleRow.add(field(text("Designer.people.origin.radius"), radius));
        scaleRow.add(field(text("Designer.people.origin.scale"), distanceScale));
        return stack(
              section(text("Designer.people.section.names"), null,
                    field(text("lblForceNamingMethod.text"), naming), numberRegiments, autoRanks,
                    callsigns, founders, commanderFlag),
              section(text("Designer.people.section.origin"), text("Designer.people.section.origin.aside"),
                    randomizeOrigin, indented(dependents), indented(extraRandom), indented(clanOrigins),
                    indented(aroundPlanet), indented(specifiedSystem.getComponent()), scaleRow, radiusNote),
              section(text("lblAugmentation.text"), null, augmentation));
    }

    private HudChoice<AssistantMode> assistantChoice() {
        // Half the support column, beside the other assistant.
        HudChoice<AssistantMode> choice = new HudChoice<>(3, 170, mode -> {
            refreshAssistants();
            changed();
        });
        choice.setChoices(List.of(
              new HudChoice.Choice<>(AssistantMode.POOL, text("Designer.people.assistants.pool"),
                    text("Designer.people.assistants.pool.sub")),
              new HudChoice.Choice<>(AssistantMode.PEOPLE, text("Designer.people.assistants.people"),
                    text("Designer.people.assistants.people.sub")),
              new HudChoice.Choice<>(AssistantMode.NONE, text("Designer.people.assistants.none"), null)),
              AssistantMode.POOL);
        return choice;
    }

    private static HudChoice<SkillLevel> skillChoice() {
        List<HudChoice.Choice<SkillLevel>> choices = new ArrayList<>();
        for (SkillLevel level : SkillLevelPickerUtility.PICKER_LEVELS) {
            String label = level.isNone() ? SkillLevelPickerUtility.getRandomLabel() : level.toString();
            choices.add(new HudChoice.Choice<>(level, label));
        }
        HudChoice<SkillLevel> choice = new HudChoice<>(0, level -> { });
        choice.setChoices(choices, SkillLevel.NONE);
        return choice;
    }

    private void wireEnablement() {
        bestCommander.addActionListener(event -> refreshEnablement());
        bestOfficers.addActionListener(event -> refreshEnablement());
        autoRanks.addActionListener(event -> refreshEnablement());
        reserve.addActionListener(event -> {
            refreshEnablement();
            changed();
        });
        randomizeOrigin.addActionListener(event -> refreshOrigin());
        aroundPlanet.addActionListener(event -> refreshOrigin());
        implants.addActionListener(event -> refreshEnablement());
        for (HudCheckBox box : new HudCheckBox[] { runSimulation, marriages, procreation }) {
            box.addActionListener(event -> changed());
        }
        numberRegiments.addActionListener(event -> {
            if (namingChangeListener != null) {
                namingChangeListener.run();
            }
        });
    }

    private void refreshEnablement() {
        commanderCombat.setEnabled(bestCommander.isSelected());
        officerCombat.setEnabled(bestOfficers.isSelected());
        reservePercent.setEnabled(reserve.isSelected());
        neuralMode.setEnabled(implants.isSelected());
        boolean simulation = runSimulation.isSelected();
        simulationYears.setEnabled(simulation);
        marriages.setEnabled(simulation);
        procreation.setEnabled(simulation);
    }

    private void refreshAssistants() {
        astechSkillRow.setVisible(astechMode.getSelected() == AssistantMode.PEOPLE);
        medicSkillRow.setVisible(medicMode.getSelected() == AssistantMode.PEOPLE);
        root.revalidate();
    }

    private void refreshOrigin() {
        boolean randomize = randomizeOrigin.isSelected();
        dependents.setEnabled(randomize);
        extraRandom.setEnabled(randomize);
        clanOrigins.setEnabled(randomize);
        aroundPlanet.setEnabled(randomize);
        specifiedSystem.getComponent().setVisible(randomize && aroundPlanet.isSelected());
        radius.setEnabled(randomize);
        distanceScale.setEnabled(randomize);
        radiusNote.removeAll();
        if (isOriginStuckAtHome()) {
            addRows(radiusNote, note(text("Designer.people.origin.radiusZero"), AMBER, WRAP_WIDTH - 20));
        }
        root.revalidate();
        root.repaint();
        changed();
    }

    // endregion Layout

    // region Refresh

    private void changed() {
        refresh();
        onChanged.run();
    }

    /** Recomputes everything this page shows that depends on the rolled command or on other choices here. */
    public void refresh() {
        ForceSummary rolled = summary.get();
        Faction faction = campaign.getPlayerForce().getFaction();
        int vehiclesToCome = (faction == null) ? 0 : SupportUnitGenerator.vehiclesStillToGenerate(campaign, faction);
        SupportDemand demand = SupportPersonnelCalculator.estimate(campaign, rolled.getIncludedEntities(),
              vehiclesToCome);
        int techHires = 0;
        int doctorHires = 0;
        for (PersonnelRole role : SUPPORT_ROLES) {
            int need = needFor(demand, role);
            int target = SupportPersonnelCalculator.applyPercent(need, coverage.get(role).getValue());
            int hire = Math.max(0, target - SupportPersonnelGenerator.countActiveByRole(campaign, role));
            hires.put(role, hire);
            needLabels.get(role).setText(Integer.toString(need));
            hireLabels.get(role).setText(Integer.toString(hire));
            if (role.isTech()) {
                techHires += hire;
            } else if (role == PersonnelRole.DOCTOR) {
                doctorHires += hire;
            }
        }
        astechCaption.setText(upper(formatted("Designer.people.assistants.astechs", ASTECHS_PER_TECH * techHires)));
        medicCaption.setText(upper(formatted("Designer.people.assistants.medics", MEDICS_PER_DOCTOR * doctorHires)));

        hrNote.removeAll();
        if (campaign.getCampaignOptions().get(CampaignOption.USE_HR_STRAIN)) {
            addRows(hrNote, note(text("Designer.people.support.hrStrain"), AMBER, WRAP_WIDTH + 120));
        }

        int reserveSpares = reserve.isSelected()
              ? (int) Math.ceil(rolled.getCombatCrew() * reservePercent.getValue() / 100.0) : 0;
        reserveLine.setText(reserve.isSelected()
              ? formatted("Designer.people.reserve.line", reserveSpares)
              : text("Designer.people.reserve.off"));

        refreshChain(rolled);
        refreshEnablement();
        refreshAssistants();
        root.revalidate();
        root.repaint();
    }

    private static int needFor(SupportDemand demand, PersonnelRole role) {
        return switch (role) {
            case MEK_TECH -> demand.mekTechsNeeded();
            case MECHANIC -> demand.mechanicsNeeded();
            case AERO_TEK -> demand.aeroTeksNeeded();
            case BA_TECH -> demand.baTechsNeeded();
            case DOCTOR -> demand.doctorsNeeded();
            case ADMINISTRATOR -> demand.administratorsNeeded();
            default -> 0;
        };
    }

    private String rankBand(PersonnelRole role) {
        Faction faction = campaign.getPlayerForce().getFaction();
        if ((faction != null) && (faction.isClan() || faction.isComStarOrWoB())) {
            return text("Designer.people.support.rank.flat");
        }
        return switch (role) {
            case DOCTOR -> text("Designer.people.support.rank.officer");
            case ADMINISTRATOR -> text("Designer.people.support.rank.administrator");
            default -> text("Designer.people.support.rank.enlisted");
        };
    }

    /** Lists the officers the roll designates: the command's own, then each formation directly beneath it. */
    private void refreshChain(ForceSummary rolled) {
        chain.removeAll();
        ForceDescriptor top = rolled.getTopCommand();
        if ((top == null) || !rolled.hasRoll()) {
            addRows(chain, wrapped(text("Designer.people.chain.empty"), WRAP_WIDTH));
            return;
        }
        List<JComponent> rows = new ArrayList<>();
        addChainRow(rows, top);
        for (ForceDescriptor child : top.getSubForces()) {
            if (rows.size() >= CHAIN_ROWS) {
                break;
            }
            if (child.getEntity() == null) {
                addChainRow(rows, child);
            }
        }
        addRows(chain, rows.toArray(new JComponent[0]));
        addRows(chain, faint(text("Designer.people.chain.note")));
    }

    private void addChainRow(List<JComponent> rows, ForceDescriptor formation) {
        CrewDescriptor officer = formation.getCo();
        if (officer == null) {
            return;
        }
        String post = formationNamer.apply(formation);
        if ((post == null) || post.isBlank()) {
            post = formation.parseName();
        }
        String title = officer.getTitle();
        String name = ((title == null) || title.isBlank()) ? officer.getName() : title + " " + officer.getName();
        JPanel row = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        JLabel postLabel = keyLabel(upper(post));
        JLabel who = new JLabel(name);
        who.setForeground(TEXT);
        who.setFont(hudFont(Font.BOLD, 0.92f, 0.0f));
        row.add(postLabel, BorderLayout.NORTH);
        row.add(who, BorderLayout.CENTER);
        row.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(4), 0, scaleForGUI(4), 0)));
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, row.getPreferredSize().height));
        rows.add(row);
    }

    // endregion Refresh

    // region Options

    /** @return the formation naming method chosen here, read live by the board's preview names */
    public @Nullable ForceNamingMethod getSelectedNamingMethod() {
        return naming.getSelected();
    }

    /** Sets the formation naming method, as the faction-driven defaults do for a Clan, and tells the preview. */
    public void setSelectedNamingMethod(ForceNamingMethod method) {
        naming.setSelected(method);
        if (namingChangeListener != null) {
            namingChangeListener.run();
        }
    }

    public boolean isAlwaysNumberRegimentsSelected() {
        return numberRegiments.isSelected();
    }

    /** @param listener told when the naming method or regiment numbering changes */
    public void setNamingChangeListener(@Nullable Runnable listener) {
        this.namingChangeListener = listener;
    }

    /**
     * Shows or hides the augmentation choices. Only the factions whose warriors are actually augmented are offered
     * them.
     */
    public void setAugmentationVisible(boolean visible) {
        augmentationVisible = visible;
        augmentationBody.setVisible(visible);
        augmentationLocked.setVisible(!visible);
        root.revalidate();
    }

    public boolean isAugmentationVisible() {
        return augmentationVisible;
    }

    public boolean isRunStartingSimulation() {
        return runSimulation.isSelected();
    }

    public int getSimulationYears() {
        return simulationYears.getValue();
    }

    /** @return {@code true} if origins are randomized within a radius of zero, so everyone comes from home */
    public boolean isOriginStuckAtHome() {
        return randomizeOrigin.isSelected() && !aroundPlanet.isSelected() && (radius.getValue() == 0);
    }

    /** @return the people the support stage is expected to hire as individuals, assistants included */
    public int getEstimatedSupportHires() {
        int total = 0;
        int techs = 0;
        int doctors = 0;
        for (Map.Entry<PersonnelRole, Integer> entry : hires.entrySet()) {
            total += entry.getValue();
            if (entry.getKey().isTech()) {
                techs += entry.getValue();
            } else if (entry.getKey() == PersonnelRole.DOCTOR) {
                doctors += entry.getValue();
            }
        }
        if (astechMode.getSelected() == AssistantMode.PEOPLE) {
            total += ASTECHS_PER_TECH * techs;
        }
        if (medicMode.getSelected() == AssistantMode.PEOPLE) {
            total += MEDICS_PER_DOCTOR * doctors;
        }
        return total;
    }

    /** @return the assistants the support stage is expected to add to the anonymous pools */
    public int getEstimatedPoolHires() {
        int techs = 0;
        int doctors = 0;
        for (Map.Entry<PersonnelRole, Integer> entry : hires.entrySet()) {
            if (entry.getKey().isTech()) {
                techs += entry.getValue();
            } else if (entry.getKey() == PersonnelRole.DOCTOR) {
                doctors += entry.getValue();
            }
        }
        int pool = 0;
        if (astechMode.getSelected() == AssistantMode.POOL) {
            pool += ASTECHS_PER_TECH * techs;
        }
        if (medicMode.getSelected() == AssistantMode.POOL) {
            pool += MEDICS_PER_DOCTOR * doctors;
        }
        return pool;
    }

    /** @return the medical reserve spares the build is expected to hire */
    public int getEstimatedReserve() {
        return reserve.isSelected()
              ? (int) Math.ceil(summary.get().getCombatCrew() * reservePercent.getValue() / 100.0) : 0;
    }

    public void loadValuesFromOptions(@Nullable CommandGenerationOptions options) {
        if (options == null) {
            return;
        }
        naming.setSelected(options.getForceNamingMethod());
        numberRegiments.setSelected(options.isAlwaysNumberRegiments());
        autoRanks.setSelected(options.isAutomaticallyAssignRanks());
        callsigns.setSelected(options.isAssignMekWarriorsCallSigns());
        founders.setSelected(options.isAssignFounderFlag());
        commanderFlag.setSelected(options.isAssignCompanyCommanderFlag());

        bestCommander.setSelected(options.isAssignBestCompanyCommander());
        commanderCombat.setSelected(options.isPrioritizeCompanyCommanderCombatSkills());
        bestOfficers.setSelected(options.isAssignBestOfficers());
        officerCombat.setSelected(options.isPrioritizeOfficerCombatSkills());
        mostSkilled.setSelected(options.isAssignMostSkilledToPrimaryLances());
        captains.setSelected(options.isGenerateCaptains());
        statBonus.setSelected(options.isApplyOfficerStatBonusToWorstSkill());

        for (PersonnelRole role : SUPPORT_ROLES) {
            Integer percent = options.getSupportPersonnelCoveragePercents().get(role);
            coverage.get(role).setValue((percent == null) ? COVERAGE_DEFAULT : percent);
            // NONE selects the Random entry.
            SkillLevel level = options.getSupportPersonnelSkillLevels().get(role);
            skills.get(role).setSelected((level == null) ? SkillLevel.NONE : level);
        }
        astechMode.setSelected(assistantMode(options.isGenerateAstechs(), options.isAstechsAsPersonnel()));
        astechSkill.setSelected(options.getAstechSkillLevel());
        medicMode.setSelected(assistantMode(options.isGenerateMedics(), options.isMedicsAsPersonnel()));
        medicSkill.setSelected(options.getMedicSkillLevel());
        reserve.setSelected(options.isGenerateMedicalReserve());
        reservePercent.setValue(options.getMedicalReservePercent());

        runSimulation.setSelected(options.isRunStartingSimulation());
        // Saved options may hold a value from when the duration was counted in months, or one past the range; the
        // stepper keeps it within range.
        simulationYears.setValue(options.getSimulationDuration());
        marriages.setSelected(options.isSimulateRandomMarriages());
        procreation.setSelected(options.isSimulateRandomProcreation());

        RandomOriginOptions origin = options.getRandomOriginOptions();
        if (origin != null) {
            randomizeOrigin.setSelected(origin.isRandomizeOrigin());
            dependents.setSelected(origin.isRandomizeDependentOrigin());
            extraRandom.setSelected(origin.isExtraRandomOrigin());
            clanOrigins.setSelected(origin.isAllowClanOrigins());
            aroundPlanet.setSelected(origin.isRandomizeAroundSpecifiedPlanet());
            radius.setValue(origin.getOriginSearchRadius());
            distanceScale.setValue((int) Math.round(origin.getOriginDistanceScale() * 10));
            Planet planet = origin.getSpecifiedPlanet();
            if ((planet != null) && (planet.getParentSystem() != null)) {
                specifiedSystem.setSelected(planet.getParentSystem());
            }
            // After the system, whose change listener forgets the planet.
            specifiedPlanet = planet;
        }

        loadAugmentationValues();
        refreshOrigin();
        refresh();
    }

    private static AssistantMode assistantMode(boolean generate, boolean asPersonnel) {
        if (!generate) {
            return AssistantMode.NONE;
        }
        return asPersonnel ? AssistantMode.PEOPLE : AssistantMode.POOL;
    }

    private void loadAugmentationValues() {
        boolean tracksImplants = campaign.getCampaignOptions().get(CampaignOption.USE_IMPLANTS);
        NeuralInterfaceMode mode = NeuralInterfaceMode.from(campaign.getGameOptions());
        // A campaign that has chosen nothing is shown the player's last answer rather than the off defaults.
        boolean campaignHasChosen = tracksImplants || mode.isOn();
        if (!campaignHasChosen) {
            tracksImplants = MekHQ.getMHQOptions().getLastUseImplants();
            mode = MekHQ.getMHQOptions().getLastNeuralInterfaceMode();
        }
        implants.setSelected(tracksImplants);
        neuralMode.setSelected(mode);
        refreshEnablement();
    }

    public void writeValuesToOptions(@Nullable CommandGenerationOptions options) {
        if (options == null) {
            return;
        }
        ForceNamingMethod method = naming.getSelected();
        if (method != null) {
            options.setForceNamingMethod(method);
        }
        options.setAlwaysNumberRegiments(numberRegiments.isSelected());
        options.setAutomaticallyAssignRanks(autoRanks.isSelected());
        options.setAssignMekWarriorsCallSigns(callsigns.isSelected());
        options.setAssignFounderFlag(founders.isSelected());
        options.setAssignCompanyCommanderFlag(commanderFlag.isSelected());

        options.setAssignBestCompanyCommander(bestCommander.isSelected());
        options.setPrioritizeCompanyCommanderCombatSkills(commanderCombat.isSelected());
        options.setAssignBestOfficers(bestOfficers.isSelected());
        options.setPrioritizeOfficerCombatSkills(officerCombat.isSelected());
        options.setAssignMostSkilledToPrimaryLances(mostSkilled.isSelected());
        options.setGenerateCaptains(captains.isSelected());
        options.setApplyOfficerStatBonusToWorstSkill(statBonus.isSelected());

        for (PersonnelRole role : SUPPORT_ROLES) {
            options.getSupportPersonnelCoveragePercents().put(role, coverage.get(role).getValue());
            SkillLevel level = skills.get(role).getSelected();
            // A NONE selection is the Random entry; stored as-is so the generator rolls per person.
            options.getSupportPersonnelSkillLevels().put(role, (level == null) ? SkillLevel.NONE : level);
        }
        AssistantMode astechs = astechMode.getSelected();
        options.setGenerateAstechs(astechs != AssistantMode.NONE);
        options.setAstechsAsPersonnel(astechs == AssistantMode.PEOPLE);
        options.setAstechSkillLevel(skillOrRandom(astechSkill));
        AssistantMode medics = medicMode.getSelected();
        options.setGenerateMedics(medics != AssistantMode.NONE);
        options.setMedicsAsPersonnel(medics == AssistantMode.PEOPLE);
        options.setMedicSkillLevel(skillOrRandom(medicSkill));
        options.setGenerateMedicalReserve(reserve.isSelected());
        options.setMedicalReservePercent(reservePercent.getValue());

        options.setRunStartingSimulation(runSimulation.isSelected());
        options.setSimulationDuration(simulationYears.getValue());
        options.setSimulateRandomMarriages(marriages.isSelected());
        options.setSimulateRandomProcreation(procreation.isSelected());

        RandomOriginOptions origin = new RandomOriginOptions(true);
        origin.setRandomizeOrigin(randomizeOrigin.isSelected());
        origin.setRandomizeDependentOrigin(dependents.isSelected());
        origin.setExtraRandomOrigin(extraRandom.isSelected());
        origin.setAllowClanOrigins(clanOrigins.isSelected());
        origin.setRandomizeAroundSpecifiedPlanet(aroundPlanet.isSelected());
        origin.setOriginSearchRadius(radius.getValue());
        origin.setOriginDistanceScale(distanceScale.getValue() / 10.0);
        PlanetarySystem system = specifiedSystem.getSelected();
        if (specifiedPlanet != null) {
            origin.setSpecifiedPlanet(specifiedPlanet);
        } else if ((system != null) && (system.getPrimaryPlanet() != null)) {
            origin.setSpecifiedPlanet(system.getPrimaryPlanet());
        }
        options.setRandomOriginOptions(origin);

        // Only written while the section is on screen. A player who set implants for a Shadow Division and then
        // switched to a faction that cannot be augmented would otherwise still have that choice written to the
        // campaign from a section they can no longer see.
        options.setApplyAugmentationRules(augmentationVisible);
        if (augmentationVisible) {
            options.setUseImplants(implants.isSelected());
            NeuralInterfaceMode mode = neuralMode.getSelected();
            if (mode == null) {
                mode = NeuralInterfaceMode.OFF;
            }
            options.setNeuralInterfaceMode(mode);
            // Remembered as the player's own answer, so the next new campaign opens on it rather than asking again.
            MekHQ.getMHQOptions().setLastUseImplants(implants.isSelected());
            MekHQ.getMHQOptions().setLastNeuralInterfaceMode(mode);
        }
    }

    private static SkillLevel skillOrRandom(HudChoice<SkillLevel> choice) {
        SkillLevel level = choice.getSelected();
        return (level == null) ? SkillLevel.NONE : level;
    }

    // endregion Options
}
