/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.view;

import static java.awt.Color.BLACK;
import static java.awt.Color.RED;
import static java.lang.Math.ceil;
import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.common.options.PilotOptions.LVL3_ADVANTAGES;
import static megamek.common.options.PilotOptions.MD_ADVANTAGES;
import static megamek.common.units.EntityWeightClass.WEIGHT_ULTRA_LIGHT;
import static megamek.utilities.ImageUtilities.addTintToImageIcon;
import static mekhq.campaign.personnel.Person.getLoyaltyName;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_AMBIDEXTROUS;
import static mekhq.campaign.personnel.enums.PersonnelStatus.ACTIVE;
import static mekhq.campaign.personnel.skills.Skill.getIndividualAttributeModifier;
import static mekhq.campaign.personnel.skills.Skill.getTotalAttributeModifier;
import static mekhq.campaign.personnel.skills.SkillType.RP_ONLY_TAG;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.*;
import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.getEffectiveFatigue;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getAmazingColor;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.messageSurroundedBySpanWithColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;
import static org.jfree.chart.ChartColor.DARK_BLUE;
import static org.jfree.chart.ChartColor.DARK_RED;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.accessibility.AccessibleRelation;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.table.TableColumn;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Portrait;
import megamek.common.options.IOption;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import megamek.utilities.ImageUtilities;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonAwardController;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.education.Academy;
import mekhq.campaign.personnel.education.EducationController;
import mekhq.campaign.personnel.enums.BloodmarkLevel;
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.enums.education.EducationStage;
import mekhq.campaign.personnel.familyTree.FormerSpouse;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternate;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjuryEffect;
import mekhq.campaign.personnel.skills.Attributes;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.model.PersonnelEventLogModel;
import mekhq.gui.model.PersonnelKillLogModel;
import mekhq.gui.utilities.MarkdownRenderer;
import mekhq.gui.utilities.WrapLayout;
import mekhq.utilities.ReportingUtilities;

/**
 * A custom panel that gets filled in with goodies from a Person record
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class PersonViewPanel extends JScrollablePanel {
    private static final MMLogger LOGGER = MMLogger.create(PersonViewPanel.class);

    private static final int MAX_NUMBER_OF_RIBBON_AWARDS_PER_ROW = 5;
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(
          PersonViewPanel.class);

    private final CampaignGUI gui;

    private Person person;
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PersonViewPanel",
          MekHQ.getMHQOptions().getLocale());

    public PersonViewPanel(@Nullable Person person, Campaign campaign, CampaignGUI gui) {
        super();
        this.person = person;
        this.campaign = campaign;
        campaignOptions = campaign.getCampaignOptions();
        this.gui = gui;
        if (person == null) {
            fillInfoEmpty();
        } else {
            initComponents();
        }
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        getAccessibleContext().setAccessibleName("Details for " + person.getFullName());

        JPanel pnlPortrait = setPortrait();
        GridBagConstraints gbc_pnlPortrait = new GridBagConstraints();
        gbc_pnlPortrait.gridx = 0;
        gbc_pnlPortrait.gridy = 0;
        gbc_pnlPortrait.fill = GridBagConstraints.NONE;
        gbc_pnlPortrait.anchor = GridBagConstraints.NORTHWEST;
        gbc_pnlPortrait.insets = new Insets(10, 0, 0, 0);
        add(pnlPortrait, gbc_pnlPortrait);

        JPanel pnlInfo = fillInfo();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 0, 10, 0);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlInfo, gridBagConstraints);

        int gridY = 1;

        PersonAwardController awardController = person.getAwardController();
        if (awardController.hasAwards()) {
            gridY = applyAndDisplayAwards(awardController, pnlPortrait, gridY);
        }

        JPanel pnlAttributes = null;
        if (campaignOptions.isDisplayAllAttributes()) {
            pnlAttributes = fillAttributeScores();
        } else {
            Map<SkillAttribute, Integer> relevantAttributes = getRelevantAttributes();
            if (!relevantAttributes.isEmpty()) {
                pnlAttributes = fillAttributeModifiers(relevantAttributes);
            }
        }

        if (pnlAttributes != null) {
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlAttributes, gridBagConstraints);
            gridY++;
        }

        List<String> relevantSkills = person.getKnownSkillsBySkillSubType(List.of(COMBAT_GUNNERY, COMBAT_PILOTING,
              SUPPORT, SUPPORT_TECHNICIAN));
        if (!relevantSkills.isEmpty()) {
            JPanel pnlCombatSkills = fillSkills(relevantSkills, "pnlSkills.profession");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlCombatSkills, gridBagConstraints);
            gridY++;
        }

        relevantSkills = person.getKnownSkillsBySkillSubType(List.of(UTILITY, UTILITY_COMMAND));
        if (!relevantSkills.isEmpty()) {
            JPanel pnlSupportSkills = fillSkills(relevantSkills, "pnlSkills.utility");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlSupportSkills, gridBagConstraints);
            gridY++;
        }

        relevantSkills = person.getKnownSkillsBySkillSubType(List.of(ROLEPLAY_GENERAL,
              ROLEPLAY_ART,
              ROLEPLAY_INTEREST,
              ROLEPLAY_SCIENCE,
              ROLEPLAY_SECURITY));
        if (!relevantSkills.isEmpty()) {
            JPanel pnlRoleplaySkills = fillSkills(relevantSkills, "pnlSkills.roleplay");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlRoleplaySkills, gridBagConstraints);
            gridY++;
        }

        Map<IOption, String> relevantAbilities = getRelevantAbilities();
        if (!relevantAbilities.isEmpty()) {
            JPanel pnlAbilities = fillAbilitiesAndImplants(relevantAbilities);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlAbilities, gridBagConstraints);
            gridY++;
        }

        JPanel pnlOther = fillOther();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridY;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 0, 10, 0);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlOther, gridBagConstraints);
        gridY++;

        List<Injury> injuries = person.getInjuries();
        if (campaignOptions.isUseAdvancedMedical() && !injuries.isEmpty()) {
            JPanel pnlInjuries = fillInjuries(injuries);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlInjuries, gridBagConstraints);
            gridY++;
        }

        if ((!person.getPersonalityDescription().isBlank()) &&
                  (campaignOptions.isUseRandomPersonalities()) &&
                  (!person.isHidePersonality()) &&
                  (!person.isChild(campaign.getLocalDate()))) { // we don't display for children, as most of the
            // descriptions won't fit
            JTextPane txtDesc = new JTextPane();
            txtDesc.setName("personalityDescription");
            txtDesc.setEditable(false);
            txtDesc.setContentType("text/html");

            String borderTitleKey = "pnlPersonality.normal";
            if (person.getJoinedCampaign() == null) {
                borderTitleKey = "pnlPersonality.interview";
                txtDesc.setText(person.getPersonalityInterviewNotes());
            } else {
                txtDesc.setText(person.getPersonalityDescription());
            }
            txtDesc.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(borderTitleKey)));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(txtDesc, gridBagConstraints);
            gridY++;
        }

        if (!person.getBiography().isBlank()) {
            JTextPane txtDesc = new JTextPane();
            txtDesc.setName("txtDesc");
            txtDesc.setEditable(false);
            txtDesc.setContentType("text/html");
            txtDesc.setText(MarkdownRenderer.getRenderedHtml(person.getBiography()));
            txtDesc.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("pnlDescription.title")));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(txtDesc, gridBagConstraints);
            gridY++;
        }

        if (!person.getPersonalLog().isEmpty()) {
            JPanel pnlPersonalLogHeader = new JPanel();
            pnlPersonalLogHeader.setName("pnlLogHeader");
            pnlPersonalLogHeader.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
                  "pnlLogHeader.title")));
            pnlPersonalLogHeader.setVisible(!campaignOptions.isDisplayPersonnelLog());

            JPanel pnlPersonalLog = fillPersonalLog();
            pnlPersonalLog.setName("pnlLog");
            pnlPersonalLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("pnlLog.title")));
            pnlPersonalLog.setVisible(campaignOptions.isDisplayPersonnelLog());

            pnlPersonalLogHeader.addMouseListener(getSwitchListener(pnlPersonalLogHeader, pnlPersonalLog));
            pnlPersonalLog.addMouseListener(getSwitchListener(pnlPersonalLog, pnlPersonalLogHeader));

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlPersonalLogHeader, gridBagConstraints);
            add(pnlPersonalLog, gridBagConstraints);
            gridY++;
        }

        if (!person.getPerformanceLog().isEmpty()) {
            JPanel pnlPerformanceLogHeader = new JPanel();
            pnlPerformanceLogHeader.setName("pnlPerformanceLogHeader");
            pnlPerformanceLogHeader.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
                  "pnlPerformanceLogHeader.title")));
            pnlPerformanceLogHeader.setVisible(!campaignOptions.isDisplayPerformanceRecord());

            JPanel pnlPerformanceLog = fillPerformanceLog();
            pnlPerformanceLog.setName("pnlPerformanceLog");
            pnlPerformanceLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
                  "pnlPerformanceLog.title")));
            pnlPerformanceLog.setVisible(campaignOptions.isDisplayPerformanceRecord());

            pnlPerformanceLogHeader.addMouseListener(getSwitchListener(pnlPerformanceLogHeader, pnlPerformanceLog));
            pnlPerformanceLog.addMouseListener(getSwitchListener(pnlPerformanceLog, pnlPerformanceLogHeader));

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlPerformanceLogHeader, gridBagConstraints);
            add(pnlPerformanceLog, gridBagConstraints);
            gridY++;
        }

        if (!person.getMedicalLog().isEmpty()) {
            JPanel pnlMedicalLogHeader = new JPanel();
            pnlMedicalLogHeader.setName("pnlMedicalLogHeader");
            pnlMedicalLogHeader.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
                  "pnlMedicalLogHeader.title")));
            pnlMedicalLogHeader.setVisible(!campaignOptions.isDisplayMedicalRecord());

            JPanel pnlMedicalLog = fillMedicalLog();
            pnlMedicalLog.setName("pnlMedicalLog");
            pnlMedicalLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
                  "pnlMedicalLog.title")));
            pnlMedicalLog.setVisible(campaignOptions.isDisplayMedicalRecord());

            pnlMedicalLogHeader.addMouseListener(getSwitchListener(pnlMedicalLogHeader, pnlMedicalLog));
            pnlMedicalLog.addMouseListener(getSwitchListener(pnlMedicalLog, pnlMedicalLogHeader));

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlMedicalLogHeader, gridBagConstraints);
            add(pnlMedicalLog, gridBagConstraints);
            gridY++;
        }

        if (!person.getPatientLog().isEmpty()) {
            JPanel pnlPatientLogHeader = new JPanel();
            pnlPatientLogHeader.setName("pnlPatientLogHeader");
            pnlPatientLogHeader.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
                  "pnlPatientLogHeader.title")));
            pnlPatientLogHeader.setVisible(!campaignOptions.isDisplayPatientRecord());

            JPanel pnlPatientLog = fillPatientLog();
            pnlPatientLog.setName("pnlPatientLog");
            pnlPatientLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
                  "pnlPatientLog.title")));
            pnlPatientLog.setVisible(campaignOptions.isDisplayPatientRecord());

            pnlPatientLogHeader.addMouseListener(getSwitchListener(pnlPatientLogHeader, pnlPatientLog));
            pnlPatientLog.addMouseListener(getSwitchListener(pnlPatientLog, pnlPatientLogHeader));

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlPatientLogHeader, gridBagConstraints);
            add(pnlPatientLog, gridBagConstraints);
            gridY++;
        }

        if (!person.getAssignmentLog().isEmpty()) {
            JPanel pnlAssignmentsLogHeader = new JPanel();
            pnlAssignmentsLogHeader.setName("assignmentLogHeader");
            pnlAssignmentsLogHeader.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
                  "assignmentLogHeader.title")));
            pnlAssignmentsLogHeader.setVisible(!campaignOptions.isDisplayAssignmentRecord());

            JPanel pnlAssignmentsLog = fillAssignmentLog();

            pnlAssignmentsLog.setName("assignmentLog");
            pnlAssignmentsLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
                  "assignmentLog.title")));
            pnlAssignmentsLog.setVisible(campaignOptions.isDisplayAssignmentRecord());

            pnlAssignmentsLogHeader.addMouseListener(getSwitchListener(pnlAssignmentsLogHeader, pnlAssignmentsLog));
            pnlAssignmentsLog.addMouseListener(getSwitchListener(pnlAssignmentsLog, pnlAssignmentsLogHeader));

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlAssignmentsLogHeader, gridBagConstraints);
            add(pnlAssignmentsLog, gridBagConstraints);
            gridY++;
        }

        if (!campaign.getKillsFor(person.getId()).isEmpty()) {
            JPanel pnlKillsHeader = new JPanel();
            pnlKillsHeader.setName("killsHeader");
            pnlKillsHeader.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
                  "pnlKillsHeader.title")));
            pnlKillsHeader.setVisible(!campaignOptions.isDisplayKillRecord());

            JPanel pnlKills = fillKillRecord();

            pnlKills.setName("txtKills");
            pnlKills.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("pnlKills.title")));
            pnlKills.setVisible(campaignOptions.isDisplayKillRecord());

            pnlKillsHeader.addMouseListener(getSwitchListener(pnlKillsHeader, pnlKills));
            pnlKills.addMouseListener(getSwitchListener(pnlKills, pnlKillsHeader));

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlKillsHeader, gridBagConstraints);
            add(pnlKills, gridBagConstraints);
            gridY++;
        }

        if (!person.getScenarioLog().isEmpty()) {
            JPanel pnlScenariosLogHeader = new JPanel();
            pnlScenariosLogHeader.setName("scenarioLogHeader");
            pnlScenariosLogHeader.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
                  "scenarioLogHeader.title")));
            pnlScenariosLogHeader.setVisible(!campaignOptions.isDisplayScenarioLog());

            JPanel pnlScenariosLog = fillScenarioLog();

            pnlScenariosLog.setName("scenarioLog");
            pnlScenariosLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
                  "scenarioLog.title")));
            pnlScenariosLog.setVisible(campaignOptions.isDisplayScenarioLog());

            pnlScenariosLogHeader.addMouseListener(getSwitchListener(pnlScenariosLogHeader, pnlScenariosLog));
            pnlScenariosLog.addMouseListener(getSwitchListener(pnlScenariosLog, pnlScenariosLogHeader));

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlScenariosLogHeader, gridBagConstraints);
            add(pnlScenariosLog, gridBagConstraints);
            gridY++;
        }

        if (!person.getGenealogy().isEmpty()) {
            JPanel pnlFamily = fillFamily();
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlFamily, gridBagConstraints);
            gridY++;
        }

        // use glue to fill up the remaining space so everything is aligned to the top
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridY;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(Box.createGlue(), gridBagConstraints);
    }

    /**
     * Initializes and lays out the award sections within a portrait panel for a person.
     *
     * <p>If ribbon awards are present, they are added above the awards panel. If medal or miscellaneous awards exist,
     * they are displayed in their respective panels using a {@link WrapLayout}, all contained within a titled section.
     * This method adds the constructed awards panel to the layout using {@link GridBagConstraints} and updates the grid
     * Y position accordingly.</p>
     *
     * @param awardController the {@link PersonAwardController} providing award data and state
     * @param pnlPortrait     the portrait {@link JPanel} to which award ribbons may be added
     * @param gridY           the starting Y grid position for layout
     *
     * @return the next available grid Y position after inserting any new panels
     *
     * @author Illiani
     * @since 0.50.06
     */
    private int applyAndDisplayAwards(PersonAwardController awardController, JPanel pnlPortrait, int gridY) {
        GridBagConstraints gridBagConstraints;
        if (awardController.hasAwardsWithRibbons()) {
            Box boxRibbons = drawRibbons();

            GridBagConstraints gbc_pnlAllRibbons = new GridBagConstraints();
            gbc_pnlAllRibbons.gridx = 0;
            gbc_pnlAllRibbons.gridy = 1;
            gbc_pnlAllRibbons.fill = GridBagConstraints.NONE;
            gbc_pnlAllRibbons.anchor = GridBagConstraints.NORTHWEST;
            gbc_pnlAllRibbons.insets = new Insets(-10, 10, 0, 5);
            pnlPortrait.add(boxRibbons, gbc_pnlAllRibbons);
        }

        JPanel pnlAllAwards = new JPanel();
        pnlAllAwards.setLayout(new BoxLayout(pnlAllAwards, BoxLayout.PAGE_AXIS));
        pnlAllAwards.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("pnlAwards.title")));

        if (awardController.hasAwardsWithMedals()) {
            JPanel pnlMedals = drawMedals();
            pnlMedals.setName("pnlMedals");
            pnlMedals.setLayout(new WrapLayout(FlowLayout.LEFT));
            pnlAllAwards.add(pnlMedals);
        }

        if (awardController.hasAwardsWithMiscs()) {
            JPanel pnlMiscAwards = drawMiscAwards();
            pnlMiscAwards.setName("pnlMiscAwards");
            pnlMiscAwards.setLayout(new WrapLayout(FlowLayout.LEFT));
            pnlAllAwards.add(pnlMiscAwards);
        }

        if (awardController.hasAwardsWithMedals() || awardController.hasAwardsWithMiscs()) {
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridY;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlAllAwards, gridBagConstraints);
            gridY++;
        }
        return gridY;
    }

    /**
     * Retrieves a map of relevant special abilities and implants for the person based on the current campaign options.
     *
     * <p>This method checks whether abilities and/or implants are enabled in the campaign options.</p>
     *
     * <p>For each enabled category, it iterates over the person's corresponding options. If an option is selected
     * (its boolean value is {@code true}), it retrieves the corresponding {@link IOption} instance and adds it to the
     * result map, associating it with a string indicating its category (e.g., {@code LVL3_ADVANTAGES} for abilities or
     * {@code MD_ADVANTAGES} for implants).</p>
     *
     * @return a {@link Map} where the key is a relevant {@link IOption} (representing a special ability or implant) and
     *       the value is a {@link String} indicating the ability or implant category
     *
     * @author Illiani
     * @since 0.50.06
     */
    private Map<IOption, String> getRelevantAbilities() {
        Map<IOption, String> relevantAbilities = new HashMap<>();

        PersonnelOptions options = person.getOptions();
        if (campaignOptions.isUseAbilities() && (person.countOptions(LVL3_ADVANTAGES) > 0)) {
            for (Enumeration<IOption> i = person.getOptions(LVL3_ADVANTAGES); i.hasMoreElements(); ) {
                IOption option = i.nextElement();
                if (option.booleanValue()) {
                    IOption ability = options.getOption(option.getName());
                    relevantAbilities.put(ability, LVL3_ADVANTAGES);
                }
            }
        }

        if (campaignOptions.isUseImplants() && (person.countOptions(MD_ADVANTAGES) > 0)) {
            for (Enumeration<IOption> i = person.getOptions(MD_ADVANTAGES); i.hasMoreElements(); ) {
                IOption option = i.nextElement();
                if (option.booleanValue()) {
                    IOption ability = options.getOption(option.getName());
                    relevantAbilities.put(ability, MD_ADVANTAGES);
                }
            }
        }
        return relevantAbilities;
    }

    /**
     * Returns a map of relevant skill attributes and their corresponding modifiers for the person.
     *
     * <p>This method iterates over all possible {@link SkillAttribute} values (excluding {@link SkillAttribute#NONE}),
     * retrieves each attribute's score for the person, and computes the associated modifier using
     * {@link Skill#getIndividualAttributeModifier(int)}. Only attributes with a non-zero modifier are included in the
     * result map.</p>
     *
     * @return a {@link Map} mapping each relevant {@link SkillAttribute} to its computed modifier for the person
     *
     * @author Illiani
     * @since 0.50.06
     */
    private Map<SkillAttribute, Integer> getRelevantAttributes() {
        Map<SkillAttribute, Integer> relevantAttributes = new HashMap<>();
        for (SkillAttribute attribute : SkillAttribute.values()) {
            if (attribute == SkillAttribute.NONE) {
                continue;
            }

            if (attribute == SkillAttribute.EDGE) {
                relevantAttributes.put(attribute, 0); // modifier is irrelevant for Edge
                continue;
            }

            int attributeScore = person.getAttributeScore(attribute);
            int modifier = getIndividualAttributeModifier(attributeScore);
            if (modifier != 0) {
                relevantAttributes.put(attribute, modifier);
            }
        }

        if (!campaignOptions.isUseEdge()) {
            relevantAttributes.remove(SkillAttribute.EDGE);
        }

        return relevantAttributes;
    }

    private MouseListener getSwitchListener(JPanel current, JPanel switchTo) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (current.isVisible()) {
                    current.setVisible(false);
                    switchTo.setVisible(true);
                }
            }
        };
    }

    /**
     * Draws the ribbons below the person portrait.
     */
    private Box drawRibbons() {
        Box boxRibbons = Box.createVerticalBox();
        boxRibbons.add(Box.createRigidArea(new Dimension(100, 0)));

        List<Award> awards = person.getAwardController()
                                   .getAwards()
                                   .stream()
                                   .filter(a -> a.getNumberOfRibbonFiles() > 0)
                                   .sorted()
                                   .collect(Collectors.toList());
        Collections.reverse(awards);

        int i = 0;
        Box rowRibbonsBox = Box.createHorizontalBox();
        ArrayList<Box> rowRibbonsBoxes = new ArrayList<>();

        for (Award award : awards) {
            JLabel ribbonLabel = new JLabel();
            Image ribbon;

            if (i % MAX_NUMBER_OF_RIBBON_AWARDS_PER_ROW == 0) {
                rowRibbonsBox = Box.createHorizontalBox();
                rowRibbonsBox.setBackground(RED);
            }
            try {
                int maximumTiers = award.getNumberOfRibbonFiles();
                int awardTierCount = getAwardTierCount(award, maximumTiers);

                String ribbonFileName = award.getRibbonFileName(awardTierCount);
                String directory = award.getSet() + "/ribbons/";

                ribbon = (Image) MHQStaticDirectoryManager.getAwardIcons().getItem(directory, ribbonFileName);
                if (ribbon == null) {
                    LOGGER.warn("No ribbon icon found for award: {}", directory + ribbonFileName);
                    continue;
                }

                ImageIcon ribbonAsImageIcon = new ImageIcon(ribbon);
                ribbonAsImageIcon = ImageUtilities.scaleImageIcon(ribbonAsImageIcon, 8, false);
                ribbon = ribbonAsImageIcon.getImage();

                ribbonLabel.setIcon(new ImageIcon(ribbon));
                ribbonLabel.setToolTipText(award.getTooltip(campaignOptions, person));
                rowRibbonsBox.add(ribbonLabel, 0);
            } catch (Exception e) {
                LOGGER.error("", e);
            }

            i++;
            if (i % MAX_NUMBER_OF_RIBBON_AWARDS_PER_ROW == 0) {
                rowRibbonsBoxes.add(rowRibbonsBox);
            }
        }
        if (i % MAX_NUMBER_OF_RIBBON_AWARDS_PER_ROW != 0) {
            rowRibbonsBoxes.add(rowRibbonsBox);
        }

        for (Box box : rowRibbonsBoxes) {
            boxRibbons.add(box);
        }

        return boxRibbons;
    }

    /**
     * Returns the number of image tiers for an award based on the maximum number of tiers and the number of awards
     * received.
     *
     * @param award        The award for which to calculate the number of tiers.
     * @param maximumTiers The maximum number of tiers allowed for the award.
     *
     * @return The number of tiers for the award. The value is clamped between 1 and the maximum number of tiers.
     */
    private int getAwardTierCount(Award award, int maximumTiers) {
        int numAwards = person.getAwardController().getNumberOfAwards(award);
        int tierSize = campaignOptions.getAwardTierSize();

        int divisionResult = numAwards / tierSize;
        int addition = (tierSize == 1) ? 0 : 1;

        return MathUtility.clamp(divisionResult + addition, 1, maximumTiers);
    }

    /**
     * Draws the medals above the personal log.
     */
    private JPanel drawMedals() {
        JPanel pnlMedals = new JPanel();

        List<Award> awards = new ArrayList<>();
        for (Award award : person.getAwardController().getAwards()) {
            if (award.getNumberOfMedalFiles() > 0) {
                awards.add(award);
            }
        }
        Collections.sort(awards);

        for (Award award : awards) {
            JLabel medalLabel = new JLabel();

            Image medal;
            try {
                int maximumTiers = award.getNumberOfMedalFiles();
                int awardTierCount = getAwardTierCount(award, maximumTiers);

                String medalFileName = award.getMedalFileName(awardTierCount);
                String directory = award.getSet() + "/medals/";

                medal = (Image) MHQStaticDirectoryManager.getAwardIcons().getItem(directory, medalFileName);
                if (medal == null) {
                    LOGGER.warn("No medal icon found for award: {}", directory + medalFileName);
                    continue;
                }

                ImageIcon medalAsImageIcon = new ImageIcon(medal);
                medalAsImageIcon = ImageUtilities.scaleImageIcon(medalAsImageIcon, 40, false);
                medal = medalAsImageIcon.getImage();

                medalLabel.setIcon(new ImageIcon(medal));
                medalLabel.setToolTipText(award.getTooltip(campaignOptions, person));
                pnlMedals.add(medalLabel);
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }

        return pnlMedals;
    }

    /**
     * Draws the misc awards below the medals.
     */
    private JPanel drawMiscAwards() {
        JPanel pnlMisc = new JPanel();

        List<Award> awards = new ArrayList<>();
        for (Award award : person.getAwardController().getAwards()) {
            if (award.getNumberOfMiscFiles() > 0) {
                awards.add(award);
            }
        }
        Collections.sort(awards);

        for (Award award : awards) {
            JLabel miscLabel = new JLabel();

            Image misc;
            try {
                int maximumTiers = award.getNumberOfMiscFiles();
                int awardTierCount = getAwardTierCount(award, maximumTiers);

                String miscFileName = award.getMiscFileName(awardTierCount);
                String directory = award.getSet() + "/misc/";

                misc = (Image) MHQStaticDirectoryManager.getAwardIcons().getItem(directory, miscFileName);
                if (misc == null) {
                    LOGGER.warn("No misc icon found for award: {}", directory + miscFileName);
                    continue;
                }

                ImageIcon miscAsImageIcon = new ImageIcon(misc);
                miscAsImageIcon = ImageUtilities.scaleImageIcon(miscAsImageIcon, 40, false);
                misc = miscAsImageIcon.getImage();

                miscLabel.setIcon(new ImageIcon(misc));
                miscLabel.setToolTipText(award.getTooltip(campaignOptions, person));
                pnlMisc.add(miscLabel);
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }

        return pnlMisc;
    }

    /**
     * set the portrait for the given person.
     *
     * @return The <code>Image</code> of the pilot's portrait. This value will be
     *       <code>null</code> if no portrait was selected or if there was an
     *       error loading it.
     */
    public JPanel setPortrait() {
        JPanel pnlPortrait = new JPanel();

        // Panel portrait will include the person picture and the ribbons
        pnlPortrait.setName("pnlPortrait");
        pnlPortrait.setLayout(new GridBagLayout());
        pnlPortrait.getAccessibleContext().setAccessibleName("Portrait for: " + person.getFullName());

        JLabel lblPortrait = new JLabel();
        lblPortrait.setName("lblPortrait");

        ImageIcon portraitImageIcon = getPortraitImageIcon();

        lblPortrait.setIcon(portraitImageIcon);
        GridBagConstraints gbc_lblPortrait = new GridBagConstraints();
        gbc_lblPortrait.gridx = 0;
        gbc_lblPortrait.gridy = 0;
        gbc_lblPortrait.fill = GridBagConstraints.NONE;
        gbc_lblPortrait.anchor = GridBagConstraints.NORTHWEST;
        gbc_lblPortrait.insets = new Insets(0, 0, 10, 0);
        pnlPortrait.add(lblPortrait, gbc_lblPortrait);

        return pnlPortrait;
    }

    /**
     * Retrieves a tinted {@link ImageIcon} representation of the person's portrait based on their current status.
     *
     * <ul>
     *     <li>If the person is deceased, a dark red tint is applied.</li>
     *     <li>If the person is retired, a dark blue tint is applied.</li>
     *     <li>If the person has departed the campaign, a black tint is applied.</li>
     * </ul>
     * <p>
     * If the person's status does not meet any of the above conditions, their portrait will be
     * returned without any modifications.
     *
     * @return a tinted {@link ImageIcon} representing the person's portrait.
     */
    private ImageIcon getPortraitImageIcon() {
        Portrait portrait = person.getPortrait();
        ImageIcon portraitImageIcon = portrait.getImageIcon(175);

        PersonnelStatus status = person.getStatus();
        if (status.isDead()) {
            portraitImageIcon = addTintToImageIcon(portraitImageIcon.getImage(), DARK_RED);
        } else if (status.isRetired()) {
            portraitImageIcon = addTintToImageIcon(portrait.getImage(100), DARK_BLUE);
        } else if (status.isDepartedUnit()) {
            portraitImageIcon = addTintToImageIcon(portrait.getImage(100), BLACK);
        }

        return portraitImageIcon;
    }

    /**
     * Constructs and returns a {@link JPanel} with empty or placeholder information fields.
     *
     * <p>The panel uses a {@link GridBagLayout} and is intended to display default or empty details for when no
     * person is selected. Rows are added for various labels, including status, origin, age, gender, and blood type,
     * with placeholder values.</p>
     *
     * <p>Origin information is conditionally added depending on campaign options.</p>
     *
     * @author Illiani
     * @since 0.50.06
     */
    private void fillInfoEmpty() {
        // TODO Update layout for new person view (needs that PR to be merged) - Illiani, 50.06

        JPanel pnlInfo = new JPanel(new GridBagLayout());
        pnlInfo.setBorder(RoundedLineBorder.createRoundedLineBorder("-"));

        // Helper to simplify row addition (text, value, isPair, gridwidth)
        BiConsumer<String[], Integer> addRow = (arr, gridWidth) -> {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.NONE;
            if (arr.length == 1) {
                gbc.gridx = 0;
                gbc.gridwidth = gridWidth;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(0, 0, 5, 0);
                pnlInfo.add(new JLabel(arr[0]), gbc);
            } else {
                gbc.gridx = 0;
                gbc.gridwidth = 1;
                pnlInfo.add(new JLabel(arr[0]), gbc);

                gbc.gridx = 1;
                gbc.gridwidth = 3;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(0, 10, 0, 0);
                pnlInfo.add(new JLabel(arr[1]), gbc);
            }
        };

        addRow.accept(new String[] { String.format(resourceMap.getString("format.italic"), '-') }, 4);
        addRow.accept(new String[] { resourceMap.getString("lblStatus1.text"), ACTIVE.toString() }, 4);

        if (campaign.getCampaignOptions().isShowOriginFaction()) {
            addRow.accept(new String[] { resourceMap.getString("lblOrigin1.text"),
                                         "<html><a href='#'>-</a> (-)</html>" }, 4);
        }
        addRow.accept(new String[] { resourceMap.getString("lblAge1.text"), "-" }, 4);
        addRow.accept(new String[] { resourceMap.getString("lblGender1.text"), "-" }, 4);
        addRow.accept(new String[] { resourceMap.getString("lblBloodType1.text"), "-" }, 4);

    }

    private JPanel fillInfo() {
        JPanel pnlInfo = new JPanel(new GridBagLayout());
        pnlInfo.setBorder(RoundedLineBorder.createRoundedLineBorder(person.getFullTitle()));
        JLabel lblBounty = new JLabel();
        JLabel lblType = new JLabel();
        JLabel lblUnitNotResponsibleForSalary = new JLabel();
        JLabel lblStatus1 = new JLabel();
        JLabel lblStatus2 = new JLabel();
        JLabel lblOrigin1 = new JLabel();
        JLabel lblOrigin2 = new JLabel();
        JLabel lblCall1 = new JLabel();
        JLabel lblCall2 = new JLabel();
        JLabel lblAge1 = new JLabel();
        JLabel lblAge2 = new JLabel();
        JLabel lblGender1 = new JLabel();
        JLabel lblGender2 = new JLabel();
        JLabel lblBloodType1 = new JLabel();
        JLabel lblBloodType2 = new JLabel();
        JLabel lblOriginalUnit1 = new JLabel();
        JLabel lblOriginalUnit2 = new JLabel();
        JLabel lblDueDate1 = new JLabel();
        JLabel lblDueDate2 = new JLabel();
        JLabel lblRecruited1 = new JLabel();
        JLabel lblRecruited2 = new JLabel();
        JLabel lblTimeServed1 = new JLabel();
        JLabel lblTimeServed2 = new JLabel();

        int y = 0;

        GridBagConstraints gridBagConstraints;

        LocalDate today = campaign.getLocalDate();

        int bloodmarkLevel = person.getBloodmark();
        boolean isChild = person.isChild(today, true);
        if (!isChild && (bloodmarkLevel > BloodmarkLevel.BLOODMARK_ZERO.getLevel())) {
            BloodmarkLevel bloodmark = BloodmarkLevel.parseBloodmarkLevelFromInt(bloodmarkLevel);
            Money bounty = bloodmark.getBounty();
            String bountyText = String.format(resourceMap.getString("lblBounty.text"),
                  spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG, bounty.toAmountString());

            lblBounty.setName("lblBounty");
            lblBounty.setText(bountyText);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 4;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 0.0;
            gridBagConstraints.insets = new Insets(0, 0, 5, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblBounty, gridBagConstraints);
            y++;
        }

        if (!person.isEmployed()) {
            lblUnitNotResponsibleForSalary.setName("lblNotResponsibleForSalary");
            lblUnitNotResponsibleForSalary.setText(resourceMap.getString("lblNotEmployedByUnit.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblUnitNotResponsibleForSalary, gridBagConstraints);
            y++;
        }

        lblType.setName("lblType");
        lblType.setText(String.format(resourceMap.getString("format.italic"), person.getRoleDesc()));
        lblType.getAccessibleContext().setAccessibleName("Role: " + person.getRoleDesc());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblType, gridBagConstraints);
        y++;

        lblStatus1.setName("lblStatus1");
        lblStatus1.setText(resourceMap.getString("lblStatus1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblStatus1, gridBagConstraints);

        lblStatus2.setName("lblStatus2");
        lblStatus2.setText(person.getStatus().toString() + person.pregnancyStatus());
        lblStatus1.setLabelFor(lblStatus2);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblStatus2, gridBagConstraints);
        y++;

        if (campaignOptions.isShowOriginFaction()) {
            lblOrigin1.setName("lblOrigin1");
            lblOrigin1.setText(resourceMap.getString("lblOrigin1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblOrigin1, gridBagConstraints);

            lblOrigin2.setName("lblOrigin2");
            lblOrigin1.setLabelFor(lblOrigin2);
            String factionName = person.getOriginFaction().getFullName(campaign.getGameYear());
            if (person.getOriginPlanet() != null) {
                String planetName = person.getOriginPlanet().getName(today);
                lblOrigin2.setText(String.format("<html><a href='#'>%s</a> (%s)</html>", planetName, factionName));
                lblOrigin2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                lblOrigin2.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        PlanetarySystem system = person.getOriginPlanet().getParentSystem();
                        // Stay on the interstellar map if their origin planet is the primary planet...
                        if (system.getPrimaryPlanet().equals(person.getOriginPlanet())) {
                            gui.getMapTab().switchSystemsMap(system);
                        } else {
                            // ...otherwise, dive on in to the system view!
                            gui.getMapTab().switchPlanetaryMap(person.getOriginPlanet());
                        }
                        gui.setSelectedTab(MHQTabType.INTERSTELLAR_MAP);
                    }
                });
            } else {
                lblOrigin2.setText(factionName);
            }
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblOrigin2, gridBagConstraints);
            y++;
        }

        if (!person.getCallsign().equals("-") && !person.getCallsign().isBlank()) {
            lblCall1.setName("lblCall1");
            lblCall1.setText(resourceMap.getString("lblCall1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblCall1, gridBagConstraints);

            lblCall2.setName("lblCall2");
            lblCall2.setText(person.getCallsign());
            lblCall1.setLabelFor(lblCall2);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblCall2, gridBagConstraints);
            y++;
        }

        lblAge1.setName("lblAge1");
        lblAge1.setText(resourceMap.getString("lblAge1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblAge1, gridBagConstraints);

        lblAge2.setName("lblAge2");
        lblAge2.setText(Integer.toString(person.getAge(today)));
        lblAge1.setLabelFor(lblAge2);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblAge2, gridBagConstraints);
        y++;

        lblGender1.setName("lblGender1");
        lblGender1.setText(resourceMap.getString("lblGender1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblGender1, gridBagConstraints);

        lblGender2.setName("lblGender2");
        lblGender2.setText(GenderDescriptors.MALE_FEMALE_OTHER.getDescriptorCapitalized(person.getGender()));
        lblGender1.setLabelFor(lblGender2);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblGender2, gridBagConstraints);
        y++;

        lblBloodType1.setName("lblBloodType1");
        lblBloodType1.setText(resourceMap.getString("lblBloodType1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblBloodType1, gridBagConstraints);

        lblBloodType2.setName("lblBloodType2");
        lblBloodType2.setText(person.getBloodGroup().getLabel());
        lblBloodType1.setLabelFor(lblBloodType2);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblBloodType2, gridBagConstraints);
        y++;

        boolean displayOriginalUnit = person.getOriginalUnitId() != null ||
                                            person.getOriginalUnitWeight() != WEIGHT_ULTRA_LIGHT;

        if (displayOriginalUnit) {
            lblOriginalUnit1.setName("lblOriginalUnit1");
            lblOriginalUnit1.setText(resourceMap.getString("lblOriginalUnit1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblOriginalUnit1, gridBagConstraints);

            lblOriginalUnit2.setName("lblOriginalUnit2");

            if (campaign.getUnit(person.getOriginalUnitId()) != null) {
                lblOriginalUnit2.setText(campaign.getUnit(person.getOriginalUnitId()).getName());
            } else {
                List<String> originalUnitWeight = List.of("None", "Light", "Medium", "Heavy", "Assault");
                int originalUnitWeightIndex = person.getOriginalUnitWeight();

                List<String> originalUnitTech = List.of("IS1", "IS2", "Clan");
                int originalUnitTechIndex = person.getOriginalUnitTech();

                lblOriginalUnit2.setText(originalUnitWeight.get(originalUnitWeightIndex) +
                                               " (" +
                                               originalUnitTech.get(originalUnitTechIndex) +
                                               ')');
            }
            lblOriginalUnit1.setLabelFor(lblOriginalUnit2);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblOriginalUnit2, gridBagConstraints);
            y++;
        }

        if (person.isPregnant()) {
            lblDueDate1.setName("lblDueDate1");
            lblDueDate1.setText(resourceMap.getString("lblDueDate1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblDueDate1, gridBagConstraints);

            lblDueDate2.setName("lblDueDate2");
            lblDueDate2.setText(person.getDueDateAsString(campaign));
            lblDueDate1.setLabelFor(lblDueDate2);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblDueDate2, gridBagConstraints);
            y++;
        }

        if (person.getRetirement() != null) {
            JLabel lblRetirement1 = new JLabel(resourceMap.getString("lblRetirement1.text"));
            lblRetirement1.setName("lblRetirement1");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblRetirement1, gridBagConstraints);

            JLabel lblRetirement2 = new JLabel(MekHQ.getMHQOptions().getDisplayFormattedDate(person.getRetirement()));
            lblRetirement2.setName("lblRetirement2");
            lblRetirement1.setLabelFor(lblRetirement2);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblRetirement2, gridBagConstraints);
            y++;
        }

        // We show the following if track total earnings is on for a free person or if
        // the
        // person has previously tracked total earnings
        if (campaignOptions.isTrackTotalEarnings() &&
                  (person.getPrisonerStatus().isFree() || person.getTotalEarnings().isGreaterThan(Money.zero()))) {
            JLabel lblTotalEarnings1 = new JLabel(resourceMap.getString("lblTotalEarnings1.text"));
            lblTotalEarnings1.setName("lblTotalEarnings1");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblTotalEarnings1, gridBagConstraints);

            JLabel lblTotalEarnings2 = new JLabel(person.getTotalEarnings().toAmountAndSymbolString());
            lblTotalEarnings2.setName("lblTotalEarnings2");
            lblTotalEarnings1.setLabelFor(lblTotalEarnings2);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblTotalEarnings2, gridBagConstraints);
            y++;
        }

        // We show the following if track total xp earnings is on for a free person or
        // if the
        // person has previously tracked total xp earnings
        if (campaignOptions.isTrackTotalXPEarnings() &&
                  (person.getPrisonerStatus().isFree() || (person.getTotalXPEarnings() != 0))) {
            JLabel lblTotalXPEarnings1 = new JLabel(resourceMap.getString("lblTotalXPEarnings1.text"));
            lblTotalXPEarnings1.setName("lblTotalXPEarnings1");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblTotalXPEarnings1, gridBagConstraints);

            JLabel lblTotalXPEarnings2 = new JLabel(Integer.toString(person.getTotalXPEarnings()));
            lblTotalXPEarnings2.setName("lblTotalXPEarnings2");
            lblTotalXPEarnings1.setLabelFor(lblTotalXPEarnings2);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblTotalXPEarnings2, gridBagConstraints);
            y++;
        }

        if (person.getRecruitment() != null) {
            lblRecruited1.setName("lblRecruited1");
            lblRecruited1.setText(resourceMap.getString("lblRecruited1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblRecruited1, gridBagConstraints);

            lblRecruited2.setName("lblRecruited2");
            lblRecruited2.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(person.getRecruitment()));
            lblRecruited1.setLabelFor(lblRecruited2);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblRecruited2, gridBagConstraints);
            y++;

            lblTimeServed1.setName("lblTimeServed1");
            lblTimeServed1.setText(resourceMap.getString("lblTimeServed1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblTimeServed1, gridBagConstraints);

            lblTimeServed2.setName("lblTimeServed2");
            lblTimeServed2.setText(person.getTimeInService(campaign));
            lblTimeServed1.setLabelFor(lblTimeServed2);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblTimeServed2, gridBagConstraints);
            y++;
        }

        if (person.getLastRankChangeDate() != null) {
            JLabel lblLastRankChangeDate1 = new JLabel(resourceMap.getString("lblLastRankChangeDate1.text"));
            lblLastRankChangeDate1.setName("lblLastRankChangeDate1");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblLastRankChangeDate1, gridBagConstraints);

            JLabel lblLastRankChangeDate2 = new JLabel(MekHQ.getMHQOptions()
                                                             .getDisplayFormattedDate(person.getLastRankChangeDate()));
            lblLastRankChangeDate2.setName("lblLastRankChangeDate2");
            lblLastRankChangeDate1.setLabelFor(lblLastRankChangeDate2);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblLastRankChangeDate2, gridBagConstraints);
            y++;

            JLabel lblTimeInRank1 = new JLabel(resourceMap.getString("lblTimeInRank1.text"));
            lblTimeInRank1.setName("lblTimeInRank1");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblTimeInRank1, gridBagConstraints);

            JLabel lblTimeInRank2 = new JLabel(person.getTimeInRank(campaign));
            lblTimeInRank2.setName("lblTimeInRank2");
            lblTimeInRank1.setLabelFor(lblTimeInRank2);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblTimeInRank2, gridBagConstraints);
        }

        return pnlInfo;
    }

    private JPanel fillFamily() {
        JPanel pnlFamily = new JPanel(new GridBagLayout());
        pnlFamily.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("pnlFamily.title")));

        // family panel
        JLabel lblSpouse2 = new JLabel();
        JLabel lblFormerSpouses1 = new JLabel();
        JLabel lblFormerSpouses2;
        JLabel lblChildren1 = new JLabel();
        JLabel lblChildren2;
        JLabel lblGrandchildren1 = new JLabel();
        JLabel lblGrandchildren2;
        JLabel lblSiblings1 = new JLabel();
        JLabel lblSiblings2;
        JLabel lblGrandparents1 = new JLabel();
        JLabel lblGrandparents2;
        JLabel lblAuntsOrUncles1 = new JLabel();
        JLabel lblAuntsOrUncles2;
        JLabel lblCousins1 = new JLabel();
        JLabel lblCousins2;

        GridBagConstraints gridBagConstraints;

        int firstY = 0;

        final Person spouse = person.getGenealogy().getSpouse();
        if (spouse != null) {
            JLabel lblSpouse1 = new JLabel(resourceMap.getString("lblSpouse1.text"));
            lblSpouse1.setName("lblSpouse1");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firstY;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlFamily.add(lblSpouse1, gridBagConstraints);

            lblSpouse2.setName("lblSpouse2");
            lblSpouse1.setLabelFor(lblSpouse2);
            lblSpouse2.setText(String.format("<html>%s</html>", spouse.getHyperlinkedFullTitle()));
            lblSpouse2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lblSpouse2.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    gui.focusOnPerson(spouse);
                }
            });
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = firstY;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            pnlFamily.add(lblSpouse2, gridBagConstraints);
            firstY++;
        }

        if (person.getGenealogy().hasFormerSpouse()) {
            lblFormerSpouses1.setName("lblFormerSpouses1");
            lblFormerSpouses1.setText(resourceMap.getString("lblFormerSpouses1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firstY;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlFamily.add(lblFormerSpouses1, gridBagConstraints);

            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);

            List<FormerSpouse> formerSpouses = person.getGenealogy().getFormerSpouses();
            Collections.reverse(person.getGenealogy().getFormerSpouses());

            for (FormerSpouse formerSpouse : formerSpouses) {
                Person ex = formerSpouse.getFormerSpouse();
                String name = getRelativeName(ex);

                gridBagConstraints.gridy = firstY;
                lblFormerSpouses2 = new JLabel();
                lblFormerSpouses2.setName("lblFormerSpouses2");
                lblFormerSpouses2.getAccessibleContext()
                      .getAccessibleRelationSet()
                      .add(new AccessibleRelation(AccessibleRelation.LABELED_BY, lblFormerSpouses1));
                lblFormerSpouses2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                lblFormerSpouses2.setText(String.format("<html>%s, %s, %s</html>",
                      name,
                      formerSpouse.getReason(),
                      MekHQ.getMHQOptions().getDisplayFormattedDate(formerSpouse.getDate())));
                lblFormerSpouses2.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        gui.focusOnPerson(ex);
                    }
                });
                pnlFamily.add(lblFormerSpouses2, gridBagConstraints);
                firstY++;
            }
        }

        if (campaignOptions.getFamilyDisplayLevel().displayParentsChildrenSiblings()) {
            final List<Person> children = person.getGenealogy().getChildren();
            if (!children.isEmpty()) {
                lblChildren1.setName("lblChildren1");
                lblChildren1.setText(resourceMap.getString("lblChildren1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firstY;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlFamily.add(lblChildren1, gridBagConstraints);

                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);

                for (Person child : children) {
                    String name = getRelativeName(child);

                    gridBagConstraints.gridy = firstY;
                    lblChildren2 = new JLabel();
                    lblChildren2.setName("lblChildren2");
                    lblChildren2.getAccessibleContext()
                          .getAccessibleRelationSet()
                          .add(new AccessibleRelation(AccessibleRelation.LABELED_BY, lblChildren1));
                    lblChildren2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    lblChildren2.setText(String.format("<html>%s</html>", name));
                    lblChildren2.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            gui.focusOnPerson(child);
                        }
                    });
                    pnlFamily.add(lblChildren2, gridBagConstraints);
                    firstY++;
                }
            }

            final List<Person> grandchildren = person.getGenealogy().getGrandchildren();
            if (!grandchildren.isEmpty() &&
                      campaignOptions.getFamilyDisplayLevel().displayGrandparentsGrandchildren()) {
                lblGrandchildren1.setName("lblGrandchildren1");
                lblGrandchildren1.setText(resourceMap.getString("lblGrandchildren1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firstY;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlFamily.add(lblGrandchildren1, gridBagConstraints);

                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

                for (Person grandchild : grandchildren) {
                    String name = getRelativeName(grandchild);

                    gridBagConstraints.gridy = firstY;
                    lblGrandchildren2 = new JLabel();
                    lblGrandchildren2.setName("lblGrandchildren2");
                    lblGrandchildren2.getAccessibleContext()
                          .getAccessibleRelationSet()
                          .add(new AccessibleRelation(AccessibleRelation.LABELED_BY, lblGrandchildren1));
                    lblGrandchildren2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    lblGrandchildren2.setText(String.format("<html>%s</html>", name));
                    lblGrandchildren2.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            gui.focusOnPerson(grandchild);
                        }
                    });
                    pnlFamily.add(lblGrandchildren2, gridBagConstraints);
                    firstY++;
                }
            }

            for (Person parent : person.getGenealogy().getParents()) {
                JLabel labelParent = new JLabel(resourceMap.getString(parent.getGender().isMale() ?
                                                                            "lblFather1.text" :
                                                                            "lblMother1.text"));
                labelParent.setName("lblParent");
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firstY;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlFamily.add(labelParent, gridBagConstraints);

                JLabel labelParentName = new JLabel(String.format("<html>%s</html>", parent.getHyperlinkedName()));
                labelParentName.setName("lblParentName");
                labelParent.setLabelFor(labelParentName);
                labelParentName.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                labelParentName.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        gui.focusOnPerson(parent);
                    }
                });
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                pnlFamily.add(labelParentName, gridBagConstraints);
                firstY++;
            }

            final List<Person> siblings = person.getGenealogy().getSiblings();
            if (!siblings.isEmpty()) {
                lblSiblings1.setName("lblSiblings1");
                lblSiblings1.setText(resourceMap.getString("lblSiblings1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firstY;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlFamily.add(lblSiblings1, gridBagConstraints);

                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

                for (Person sibling : siblings) {
                    String name = getRelativeName(sibling);

                    gridBagConstraints.gridy = firstY;
                    lblSiblings2 = new JLabel(String.format("<html>%s</html>", name));
                    lblSiblings2.setName("lblSiblings2");
                    lblSiblings2.getAccessibleContext()
                          .getAccessibleRelationSet()
                          .add(new AccessibleRelation(AccessibleRelation.LABELED_BY, lblSiblings1));

                    lblSiblings2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    lblSiblings2.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            gui.focusOnPerson(sibling);
                        }
                    });
                    pnlFamily.add(lblSiblings2, gridBagConstraints);
                    firstY++;
                }
            }

            final List<Person> grandparents = person.getGenealogy().getGrandparents();
            if (!grandparents.isEmpty() && campaignOptions.getFamilyDisplayLevel().displayGrandparentsGrandchildren()) {
                lblGrandparents1.setName("lblGrandparents1");
                lblGrandparents1.setText(resourceMap.getString("lblGrandparents1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firstY;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlFamily.add(lblGrandparents1, gridBagConstraints);

                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

                for (Person grandparent : grandparents) {
                    String name = getRelativeName(grandparent);

                    gridBagConstraints.gridy = firstY;
                    lblGrandparents2 = new JLabel(String.format("<html>%s</html>", name));
                    lblGrandparents2.setName("lblGrandparents2");
                    lblGrandparents2.getAccessibleContext()
                          .getAccessibleRelationSet()
                          .add(new AccessibleRelation(AccessibleRelation.LABELED_BY, lblGrandparents1));
                    lblGrandparents2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    lblGrandparents2.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            gui.focusOnPerson(grandparent);
                        }
                    });
                    pnlFamily.add(lblGrandparents2, gridBagConstraints);
                    firstY++;
                }
            }

            final List<Person> auntsAndUncles = person.getGenealogy().getsAuntsAndUncles();
            if (!auntsAndUncles.isEmpty() && campaignOptions.getFamilyDisplayLevel().isAuntsUnclesCousins()) {
                lblAuntsOrUncles1.setName("lblAuntsOrUncles1");
                lblAuntsOrUncles1.setText(resourceMap.getString("lblAuntsOrUncles1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firstY;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlFamily.add(lblAuntsOrUncles1, gridBagConstraints);

                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

                for (Person auntOrUncle : auntsAndUncles) {
                    String name = getRelativeName(auntOrUncle);

                    gridBagConstraints.gridy = firstY;
                    lblAuntsOrUncles2 = new JLabel(String.format("<html>%s</html>", name));
                    lblAuntsOrUncles2.setName("lblAuntsOrUncles2");
                    lblAuntsOrUncles2.getAccessibleContext()
                          .getAccessibleRelationSet()
                          .add(new AccessibleRelation(AccessibleRelation.LABELED_BY, lblAuntsOrUncles1));

                    lblAuntsOrUncles2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    lblAuntsOrUncles2.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            gui.focusOnPerson(auntOrUncle);
                        }
                    });
                    pnlFamily.add(lblAuntsOrUncles2, gridBagConstraints);
                    firstY++;
                }
            }

            final List<Person> cousins = person.getGenealogy().getCousins();
            if (!cousins.isEmpty() && campaignOptions.getFamilyDisplayLevel().isAuntsUnclesCousins()) {
                lblCousins1.setName("lblCousins1");
                lblCousins1.setText(resourceMap.getString("lblCousins1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firstY;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlFamily.add(lblCousins1, gridBagConstraints);

                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

                for (Person cousin : cousins) {
                    String name = getRelativeName(cousin);

                    gridBagConstraints.gridy = firstY;
                    lblCousins2 = new JLabel();
                    lblCousins2.setName("lblCousins2");
                    lblCousins2.getAccessibleContext()
                          .getAccessibleRelationSet()
                          .add(new AccessibleRelation(AccessibleRelation.LABELED_BY, lblCousins1));
                    lblCousins2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    lblCousins2.setText(String.format("<html>%s</html>", name));
                    lblCousins2.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            gui.focusOnPerson(cousin);
                        }
                    });
                    pnlFamily.add(lblCousins2, gridBagConstraints);
                    firstY++;
                }
            }
        }

        return pnlFamily;
    }

    /**
     * If the relative has joined the campaign, the hyperlinked full title is returned. Otherwise, the full name of the
     * relative is returned.
     *
     * @param relative The relative.
     *
     * @return The relative's name.
     */
    private static String getRelativeName(Person relative) {
        if (relative.getJoinedCampaign() == null) {
            return relative.getFirstName();
        } else {
            return "<a href='#'>" + relative.getHyperlinkedFullTitle() + "</a>";
        }
    }

    /**
     * Creates and returns a JPanel displaying a sorted list of skills arranged in columns, with each skill's name and
     * corresponding value shown side-by-side. The panel's title is determined by the provided resource key. Skills are
     * distributed evenly across a fixed number of columns (default is three).
     *
     * @param relevantSkills the list of skill names to display; will be sorted alphabetically
     * @param titleKey       the resource key for the panel's titled border
     *
     * @return a {@link JPanel} containing the skill names and values in a grid layout
     *
     * @author Illiani
     * @since 0.50.06
     */
    private JPanel fillSkills(List<String> relevantSkills, String titleKey) {
        Collections.sort(relevantSkills);
        JPanel pnlSkills = new JPanel(new GridBagLayout());
        pnlSkills.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(titleKey)));

        Attributes attributes = person.getATOWAttributes();
        PersonnelOptions options = person.getOptions();
        int adjustedReputation = person.getAdjustedReputation(campaignOptions.isUseAgeEffects(),
              campaign.isClanCampaign(),
              campaign.getLocalDate(),
              person.getRankNumeric());

        boolean adminsHaveNegotiation = campaignOptions.isAdminsHaveNegotiation();
        boolean doctorsUseAdmin = campaignOptions.isDoctorsUseAdministration();
        boolean techsUseAdmin = campaignOptions.isTechsUseAdministration();
        boolean isUseArtillery = campaignOptions.isUseArtillery();
        PersonnelRole primaryProfession = person.getPrimaryRole();
        List<String> primaryProfessionSkills = primaryProfession.getSkillsForProfession(adminsHaveNegotiation,
              doctorsUseAdmin,
              techsUseAdmin,
              isUseArtillery);

        PersonnelRole secondaryProfession = person.getSecondaryRole();
        List<String> secondaryProfessionSkills = new ArrayList<>(secondaryProfession.getSkillsForProfession(
              adminsHaveNegotiation,
              doctorsUseAdmin,
              techsUseAdmin,
              isUseArtillery));

        // Calculate how many rows per column for even distribution
        double numColumns = 3.0;
        int skillsPerColumn = (int) ceil(relevantSkills.size() / numColumns);
        for (int i = 0; i < relevantSkills.size(); i++) {
            int column = i / skillsPerColumn; // 0, 1, 2
            int row = i % skillsPerColumn;
            int gridX = column * 2; // Each column takes 2 grid positions: name + value

            String skillName = relevantSkills.get(i);
            Skill skill = person.getSkill(skillName);
            String formattedSkillName = skillName.replaceAll(Pattern.quote(RP_ONLY_TAG), "");

            String label;
            if (primaryProfessionSkills.contains(skillName)) {
                label = String.format(resourceMap.getString("format.itemHeader.profession"),
                      ReportingUtilities.spanOpeningWithCustomColor(getAmazingColor()), CLOSING_SPAN_TAG,
                      formattedSkillName);
            } else if (secondaryProfessionSkills.contains(skillName)) {
                label = String.format(resourceMap.getString("format.itemHeader.profession"),
                      ReportingUtilities.spanOpeningWithCustomColor(getPositiveColor()), CLOSING_SPAN_TAG,
                      formattedSkillName);
            } else {
                label = formattedSkillName;
            }
            JLabel lblName = new JLabel(label);
            boolean isAmbidextrous = person.getOptions().booleanOption(PersonnelOptions.ATOW_AMBIDEXTROUS);
            List<InjuryEffect> injuryEffects = AdvancedMedicalAlternate.getAllActiveInjuryEffects(isAmbidextrous,
                  person.getInjuries());
            SkillModifierData skillModifierData = new SkillModifierData(options, attributes, adjustedReputation,
                  injuryEffects);
            int attributeModifier = getTotalAttributeModifier(new TargetRoll(), attributes, skill.getType());
            int spaModifier = skill.getSPAModifiers(options, adjustedReputation);
            int injuryModifier = Skill.getTotalInjuryModifier(skillModifierData, skill.getType());
            String adjustment = getAdjustment(skill, attributeModifier, spaModifier, injuryModifier);

            JLabel lblValue = new JLabel(String.format("<html>%s%s</html>",
                  skill.toString(skillModifierData),
                  adjustment));
            lblName.setLabelFor(lblValue);
            String tooltip = wordWrap(skill.getTooltip(skillModifierData));
            lblName.setToolTipText(tooltip);
            lblValue.setToolTipText(tooltip);

            // Name label constraints
            GridBagConstraints nameConstraints = new GridBagConstraints();
            nameConstraints.gridx = gridX;
            nameConstraints.gridy = row;
            nameConstraints.anchor = GridBagConstraints.NORTHWEST;

            // Value label constraints
            GridBagConstraints valueConstraints = new GridBagConstraints();
            valueConstraints.gridx = gridX + 1;
            valueConstraints.gridy = row;
            valueConstraints.anchor = GridBagConstraints.NORTHWEST;
            valueConstraints.insets = new Insets(0, 5, 0, 10);
            valueConstraints.weightx = 1;

            pnlSkills.add(lblName, nameConstraints);
            pnlSkills.add(lblValue, valueConstraints);
        }

        return pnlSkills;
    }

    private static String getAdjustment(Skill skill, int attributeModifier, int spaModifier, int injuryModifier) {
        int ageModifier = skill.getAgingModifier();
        int totalModifier = attributeModifier + spaModifier + ageModifier + injuryModifier;

        String color = "";
        String icon = "";
        if (totalModifier != 0) {
            color = totalModifier < 0 ?
                          ReportingUtilities.getNegativeColor() :
                          ReportingUtilities.getPositiveColor();
            icon = totalModifier < 0 ? "&#x25BC" : "&#x25B2";
        }

        String adjustment = "";
        if (!color.isBlank()) {
            adjustment = String.format(" %s%s%s", spanOpeningWithCustomColor(color), icon, CLOSING_SPAN_TAG);
        }
        return adjustment;
    }

    /**
     * Creates and returns a JPanel displaying attribute modifiers arranged in columns, with each attribute's name and
     * its corresponding modifier value shown side-by-side. The attributes are distributed evenly across a fixed number
     * of columns (default is three). The panel's title is set using a localized resource key.
     *
     * @param relevantAttributes a map of {@link SkillAttribute} to their integer modifier values; each entry will be
     *                           displayed as a name/value pair
     *
     * @return a {@link JPanel} containing the attribute names and values in a grid layout
     */
    private JPanel fillAttributeModifiers(Map<SkillAttribute, Integer> relevantAttributes) {
        JPanel pnlAttributes = new JPanel(new GridBagLayout());
        pnlAttributes.setBorder(RoundedLineBorder.createRoundedLineBorder(
              resourceMap.getString("pnlSkills.attributes.modifiers")));

        // Calculate how many rows per column for even distribution
        double numColumns = 3.0;
        int skillsPerColumn = (int) ceil(relevantAttributes.size() / numColumns);

        int i = 0;
        for (SkillAttribute attribute : SkillAttribute.values()) {
            if (!relevantAttributes.containsKey(attribute)) {
                continue;
            }

            int column = i / skillsPerColumn; // 0, 1, 2
            int row = i % skillsPerColumn;
            int gridX = column * 2; // Each column takes 2 grid positions: name + value

            int baseEdge = person.getEdge();
            int adjustedEdge = person.getAdjustedEdge();
            int currentEdge = person.getCurrentEdge();

            JLabel lblName = new JLabel();
            JLabel lblValue = new JLabel();
            if (attribute != SkillAttribute.EDGE) {
                String attributeName = attribute.getLabel();
                int attributeModifier = relevantAttributes.get(attribute);

                lblName.setText(attributeName);
                lblValue.setText((attributeModifier > 0 ? "+" : "") + attributeModifier);
                lblName.setLabelFor(lblValue);

                String tooltip = wordWrap(attribute.getDescription());
                lblName.setToolTipText(tooltip);
                lblValue.setToolTipText(tooltip);
            } else {
                String attributeName = attribute.getLabel();
                String adjustment = getTraitAdjustmentIcon(baseEdge, adjustedEdge);
                String value = "<html>" + currentEdge + "/" + adjustedEdge + adjustment + "</html>";

                lblName.setText(attributeName);
                lblValue.setText(value);

                String tooltip = wordWrap(attribute.getDescription());
                lblName.setToolTipText(tooltip);
                lblValue.setToolTipText(tooltip);
            }

            // Name label constraints
            GridBagConstraints nameConstraints = new GridBagConstraints();
            nameConstraints.gridx = gridX;
            nameConstraints.gridy = row;
            nameConstraints.anchor = GridBagConstraints.NORTHWEST;

            // Value label constraints
            GridBagConstraints valueConstraints = new GridBagConstraints();
            valueConstraints.gridx = gridX + 1;
            valueConstraints.gridy = row;
            valueConstraints.anchor = GridBagConstraints.NORTHWEST;
            valueConstraints.insets = new Insets(0, 5, 0, 10);
            valueConstraints.weightx = 1;

            pnlAttributes.add(lblName, nameConstraints);
            pnlAttributes.add(lblValue, valueConstraints);

            i++;
        }

        return pnlAttributes;
    }

    /**
     * Constructs and returns a JPanel displaying the attribute scores and modifiers for a {@link Person}'s ATOW (A Time
     * of War) attributes.
     *
     * <p>The attributes are displayed in three columns for even distribution, with each attribute's label and
     * corresponding score (including modifier, if applicable). Tooltips are added to each label and value to provide
     * attribute descriptions.</p>
     *
     * @return a {@link JPanel} arranged in a GridBagLayout, showing each attribute's name and value (with modifier, if
     *       any), each with appropriate tooltips.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel fillAttributeScores() {
        JPanel pnlAttributes = new JPanel(new GridBagLayout());
        pnlAttributes.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
              "pnlSkills.attributes.scores")));

        Attributes attributes = person.getATOWAttributes();

        // Calculate how many rows per column for even distribution
        double numColumns = 3.0;
        SkillAttribute[] allAttributes = SkillAttribute.values();
        int numAttributes = allAttributes.length - 1; // -1 to exclude NONE
        int skillsPerColumn = (int) ceil(numAttributes / numColumns);

        int i = 0;
        for (SkillAttribute attribute : allAttributes) {
            if (attribute == SkillAttribute.NONE) {
                continue;
            }

            int column = i / skillsPerColumn; // 0, 1, 2
            int row = i % skillsPerColumn;
            int gridX = column * 2; // Each column takes 2 grid positions: name + value

            int baseEdge = person.getEdge();
            int adjustedEdge = person.getAdjustedEdge();
            int currentEdge = person.getCurrentEdge();

            JLabel lblName = new JLabel();
            JLabel lblValue = new JLabel();
            if (attribute != SkillAttribute.EDGE) {
                String attributeName = attribute.getLabel();
                int attributeScore = attributes.getAttributeScore(attribute);
                int attributeModifier = attributes.getAttributeModifier(attribute);

                lblName.setText(attributeName);
                String value = String.valueOf(attributeScore);
                if (attributeModifier != 0) {
                    value += " (" + (attributeModifier > 0 ? "+" : "") + attributeModifier + ")";
                }

                lblValue.setText(value);
                lblName.setLabelFor(lblValue);

                String tooltip = wordWrap(attribute.getDescription());
                lblName.setToolTipText(tooltip);
                lblValue.setToolTipText(tooltip);
            } else if (campaignOptions.isUseEdge() && (baseEdge != 0 || adjustedEdge != 0)) {
                String attributeName = attribute.getLabel();
                String adjustment = getTraitAdjustmentIcon(baseEdge, adjustedEdge);
                String value = "<html>" + currentEdge + "/" + adjustedEdge + adjustment + "</html>";

                lblName.setText(attributeName);
                lblValue.setText(value);

                String tooltip = wordWrap(attribute.getDescription());
                lblName.setToolTipText(tooltip);
                lblValue.setToolTipText(tooltip);
            }

            // Name label constraints
            GridBagConstraints nameConstraints = new GridBagConstraints();
            nameConstraints.gridx = gridX;
            nameConstraints.gridy = row;
            nameConstraints.anchor = GridBagConstraints.NORTHWEST;

            // Value label constraints
            GridBagConstraints valueConstraints = new GridBagConstraints();
            valueConstraints.gridx = gridX + 1;
            valueConstraints.gridy = row;
            valueConstraints.anchor = GridBagConstraints.NORTHWEST;
            valueConstraints.insets = new Insets(0, 5, 0, 10);
            valueConstraints.weightx = 1;

            pnlAttributes.add(lblName, nameConstraints);
            pnlAttributes.add(lblValue, valueConstraints);

            i++;
        }

        return pnlAttributes;
    }

    /**
     * Creates and returns a {@link JPanel} displaying abilities and implants in a grid layout.
     *
     * <p>Each ability/implant (represented by the keys of {@code relevantAbilities}) is shown as a {@link JLabel}
     * with its name and a tooltip description. Optionally, special abilities recognized as "flaws" are highlighted
     * using a colored icon. The items are laid out in columns to distribute them evenly based on the total number of
     * abilities.</p>
     *
     * @param relevantAbilities A map where the key is an {@link IOption} representing an ability or implant, and the
     *                          value is an associated string (such as a type or category).
     *
     * @return a {@link JPanel} containing the visual representation of the abilities and implants.
     *
     * @author Illiani
     * @since 0.50.06
     */
    private JPanel fillAbilitiesAndImplants(Map<IOption, String> relevantAbilities) {
        JPanel pnlAbilitiesAndImplants = new JPanel(new GridBagLayout());
        pnlAbilitiesAndImplants.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
              "pnlSkills.abilities")));

        // Calculate how many rows per column for even distribution
        double numColumns = 3.0;
        int skillsPerColumn = (int) ceil(relevantAbilities.size() / numColumns);

        int counter = 0;
        for (IOption option : relevantAbilities.keySet()) {
            int column = counter / skillsPerColumn; // 0, 1
            int row = counter % skillsPerColumn;

            String name = option.getDisplayableNameWithValue();
            String description = option.getDescription();

            boolean isFlaw = false;
            if (Objects.equals(relevantAbilities.get(option), LVL3_ADVANTAGES)) {
                SpecialAbility ability = SpecialAbility.getOption(option.getName());
                if (ability != null) {
                    isFlaw = ability.getCost() < -1; // -1 is currently used to designate an origin only SPA
                }
            }

            String adjustment = "";
            if (isFlaw) {
                String color = ReportingUtilities.getNegativeColor();
                String icon = "&#x25BC;";
                adjustment = String.format(" %s%s%s", spanOpeningWithCustomColor(color), icon, CLOSING_SPAN_TAG);
            }

            JLabel lblName = new JLabel(String.format("<html>%s%s</html>",
                  name.replaceAll("\\s*\\([^)]*\\)", ""),
                  adjustment));
            String tooltip = wordWrap(description);
            lblName.setToolTipText(tooltip);

            // Name label constraints
            GridBagConstraints nameConstraints = new GridBagConstraints();
            nameConstraints.gridx = column;
            nameConstraints.gridy = row;
            nameConstraints.anchor = GridBagConstraints.NORTHWEST;
            nameConstraints.insets = new Insets(0, 5, 0, 10);
            nameConstraints.weightx = 1;

            pnlAbilitiesAndImplants.add(lblName, nameConstraints);

            counter++;
        }

        return pnlAbilitiesAndImplants;
    }

    private JPanel fillOther() {
        JPanel pnlOther = new JPanel(new GridBagLayout());
        pnlOther.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("pnlSkills.traits")));

        JLabel lblConnections = null;
        int connections = person.getAdjustedConnections(true);
        if (connections != 0) {
            String connectionsDisplayValue = Integer.toString(connections);
            if (person.getBurnedConnectionsEndDate() != null) {
                connectionsDisplayValue = "<html><b><font color='gray'>" + connections + "</font></b></html>";
            }

            String connectionsLabel = String.format(resourceMap.getString("format.traitValue"),
                  resourceMap.getString("lblConnections.text"),
                  connectionsDisplayValue,
                  "");
            lblConnections = new JLabel(connectionsLabel);
            lblConnections.setToolTipText(wordWrap(resourceMap.getString("lblConnections.tooltip")));
        }

        JLabel lblWealth = null;
        int wealth = person.getWealth();
        if (wealth != 0) {
            String wealthLabel = String.format(resourceMap.getString("format.traitValue"),
                  resourceMap.getString("lblWealth.text"),
                  wealth,
                  "");
            lblWealth = new JLabel(wealthLabel);
            lblWealth.setToolTipText(wordWrap(resourceMap.getString("lblWealth.tooltip")));
        }

        JLabel lblReputation = null;
        int baseReputation = person.getReputation();
        int adjustedReputation = person.getAdjustedReputation(campaignOptions.isUseAgeEffects(),
              campaign.isClanCampaign(),
              campaign.getLocalDate(),
              person.getRankNumeric());
        if (baseReputation != 0 || adjustedReputation != 0) {
            String adjustment = getTraitAdjustmentIcon(baseReputation, adjustedReputation);
            String reputationLabel = String.format(resourceMap.getString("format.traitValue"),
                  resourceMap.getString("lblReputation.text"),
                  adjustedReputation,
                  adjustment);
            lblReputation = new JLabel(reputationLabel);
            lblReputation.setToolTipText(wordWrap(String.format(resourceMap.getString("lblReputation.tooltip"),
                  baseReputation,
                  adjustedReputation)));
        }

        JLabel lblToughness = null;
        int totalToughness = person.getToughness();
        if ((campaignOptions.isUseToughness()) && (totalToughness != 0)) {
            String toughnessLabel = String.format(resourceMap.getString("format.traitValue"),
                  resourceMap.getString("lblToughness.text"),
                  totalToughness,
                  "");
            lblToughness = new JLabel(toughnessLabel);
            lblToughness.setToolTipText(wordWrap(resourceMap.getString("lblToughness.tooltip")));
        }

        JLabel lblLoyalty = null;
        int loyaltyModifier = person.getLoyaltyModifier(person.getAdjustedLoyalty(campaign.getFaction(),
              campaignOptions.isUseAlternativeAdvancedMedical()));
        if ((campaignOptions.isUseLoyaltyModifiers()) &&
                  (!campaignOptions.isUseHideLoyalty()) &&
                  (loyaltyModifier != 0)) {
            String loyaltyLabel = String.format(resourceMap.getString("format.traitValue"),
                  resourceMap.getString("lblLoyalty.text"),
                  loyaltyModifier + " (" + getLoyaltyName(loyaltyModifier) + ')',
                  "");
            lblLoyalty = new JLabel(loyaltyLabel);
            lblLoyalty.setToolTipText(wordWrap(resourceMap.getString("lblLoyalty.tooltip")));
        }

        JLabel lblFatigue = null;
        int baseFatigue = person.getFatigue();
        int effectiveFatigue = getEffectiveFatigue(person.getFatigue(), person.getPermanentFatigue(),
              person.isClanPersonnel(),
              person.getSkillLevel(campaign, false, true));
        if (campaignOptions.isUseFatigue() && (baseFatigue != 0 || effectiveFatigue != 0)) {
            StringBuilder fatigueDisplay = new StringBuilder("<html>");
            int fatigueTurnoverModifier = MathUtility.clamp(((effectiveFatigue - 1) / 4) - 1, 0, 3);
            if (effectiveFatigue != baseFatigue) {
                fatigueDisplay.append("<s><font color='gray'>")
                      .append(baseFatigue)
                      .append("</font></s> ")
                      .append(effectiveFatigue);
            } else {
                fatigueDisplay.append("<font color='gray'>").append(effectiveFatigue).append("</font>");
            }
            if (fatigueTurnoverModifier > 0) {
                fatigueDisplay.append(" (-").append(fatigueTurnoverModifier).append(')');
            }
            fatigueDisplay.append("</html>");

            String adjustment = getTraitAdjustmentIcon(baseFatigue, effectiveFatigue);
            String fatigueLabel = String.format(resourceMap.getString("format.traitValue"),
                  resourceMap.getString("lblFatigue.text"),
                  fatigueDisplay,
                  adjustment);
            lblFatigue = new JLabel(fatigueLabel);
            lblFatigue.setToolTipText(wordWrap(resourceMap.getString("lblFatigue.tooltip")));
        }

        JLabel lblHighestEducation = null;
        JLabel lblEducationStage = null;
        if (campaignOptions.isUseEducationModule()) {
            EducationLevel highestEducation = person.getEduHighestEducation();
            String highestEducationLabel = String.format(resourceMap.getString("format.traitValue"),
                  resourceMap.getString("lblHighestEducation.text"),
                  highestEducation,
                  "");
            lblHighestEducation = new JLabel(highestEducationLabel);
            lblHighestEducation.setToolTipText(wordWrap(highestEducation.getToolTipText()));

            EducationStage educationStage = person.getEduEducationStage();
            Academy academy = EducationController.getAcademy(person.getEduAcademySet(),
                  person.getEduAcademyNameInSet());

            if (academy != null && educationStage != EducationStage.NONE) {
                String educationLabel;
                String educationValue;
                switch (educationStage) {
                    case EDUCATION -> {
                        educationLabel = resourceMap.getString("lblEducationStage.educationTime");

                        if (academy.isPrepSchool()) {
                            educationValue = String.format(resourceMap.getString("lblEducationDurationAge.text"),
                                  academy.getAgeMax());
                        } else {
                            educationValue = String.format(resourceMap.getString("lblEducationDurationDays.text"),
                                  person.getEduEducationTime());
                        }
                    }
                    case JOURNEY_TO_CAMPUS, JOURNEY_FROM_CAMPUS -> {
                        educationLabel = resourceMap.getString("lblEducationStage.journeyTime");

                        if (educationStage.isJourneyToCampus()) {
                            educationValue = String.format(resourceMap.getString("lblEducationTravelTo.text"),
                                  person.getEduDaysOfTravel(),
                                  person.getEduJourneyTime(),
                                  campaign.getSystemById(person.getEduAcademySystem())
                                        .getName(campaign.getLocalDate()));
                        } else {
                            educationValue = String.format(resourceMap.getString("lblEducationTravelFrom.text"),
                                  person.getEduDaysOfTravel(),
                                  person.getEduJourneyTime(),
                                  campaign.getSystemById(person.getEduAcademySystem())
                                        .getName(campaign.getLocalDate()));

                        }
                    }
                    default -> {
                        educationLabel = educationStage.toString();
                        educationValue = "-";
                    }
                }

                String educationStageLabel = String.format(resourceMap.getString("format.traitValue"),
                      educationLabel,
                      educationValue,
                      "");
                lblEducationStage = new JLabel(educationStageLabel);
                lblEducationStage.setToolTipText(wordWrap(educationStage.getToolTipText()));
            }
        }

        List<JLabel> components = new ArrayList<>();
        if (lblConnections != null) {
            components.add(lblConnections);
        }
        if (lblWealth != null) {
            components.add(lblWealth);
        }
        if (lblReputation != null) {
            components.add(lblReputation);
        }
        if (lblToughness != null) {
            components.add(lblToughness);
        }
        if (lblLoyalty != null) {
            components.add(lblLoyalty);
        }
        if (lblFatigue != null) {
            components.add(lblFatigue);
        }
        if (lblHighestEducation != null) {
            components.add(lblHighestEducation);
        }
        if (lblEducationStage != null) {
            components.add(lblEducationStage);
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int total = components.size();
        int numRows = (int) Math.ceil(total / 2.0);

        for (int i = 0; i < total; i++) {
            int col = i / numRows;
            int row = i % numRows;

            gbc.gridx = col;
            gbc.gridy = row;
            pnlOther.add(components.get(i), gbc);
        }

        return pnlOther;
    }

    /**
     * Returns an HTML-formatted icon indicating whether the adjusted trait value is higher or lower than the base
     * value.
     *
     * <p>If the adjusted value is lower than the base value, a downward arrow is returned,
     * colored based on the configured negative color. If the adjusted value is higher, an upward arrow is returned,
     * colored according to the configured positive color. When there is no adjustment (base and adjusted values are
     * equal), an empty string is returned.</p>
     *
     * @param baseValue     the original or unmodified value of the trait
     * @param adjustedValue the value of the trait after adjustment
     *
     * @return an HTML string containing a colored up or down arrow, or an empty string if there is no adjustment
     *
     * @author Illiani
     * @since 0.50.06
     */
    private static String getTraitAdjustmentIcon(int baseValue, int adjustedValue) {
        String adjustment = "";
        if (baseValue > adjustedValue) {
            String color = ReportingUtilities.getNegativeColor();
            adjustment = String.format(" %s%s%s", spanOpeningWithCustomColor(color), "&#x25BC", CLOSING_SPAN_TAG);
        } else if (baseValue < adjustedValue) {
            String color = ReportingUtilities.getPositiveColor();
            adjustment = String.format(" %s%s%s", spanOpeningWithCustomColor(color), "&#x25B2", CLOSING_SPAN_TAG);
        }
        return adjustment;
    }

    private JPanel fillPersonalLog() {
        List<LogEntry> logs = person.getPersonalLog();
        Collections.reverse(logs);

        return getLogPanel(logs, "Event log for ", person.getFullName());
    }

    private JPanel fillPerformanceLog() {
        List<LogEntry> logs = person.getPerformanceLog();
        Collections.reverse(logs);

        return getLogPanel(logs, "Performance report for ", person.getFullName());
    }

    private JPanel getLogPanel(List<LogEntry> logs, String accessibleName, String person) {
        JPanel pnlLog = new JPanel(new GridBagLayout());

        PersonnelEventLogModel eventModel = new PersonnelEventLogModel();
        eventModel.setData(logs);
        JTable eventTable = new JTable(eventModel);
        eventTable.getAccessibleContext().setAccessibleName(accessibleName + person);
        eventTable.setRowSelectionAllowed(false);
        eventTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumn column;
        for (int i = 0; i < eventModel.getColumnCount(); ++i) {
            column = eventTable.getColumnModel().getColumn(i);
            column.setCellRenderer(eventModel.getRenderer());
            column.setPreferredWidth(eventModel.getPreferredWidth(i));
            if (eventModel.hasConstantWidth(i)) {
                column.setMinWidth(eventModel.getPreferredWidth(i));
                column.setMaxWidth(eventModel.getPreferredWidth(i));
            }
        }
        eventTable.setIntercellSpacing(new Dimension(0, 0));
        eventTable.setShowGrid(false);
        eventTable.setTableHeader(null);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;

        pnlLog.add(eventTable, gridBagConstraints);

        return pnlLog;
    }

    private JPanel fillMedicalLog() {
        List<LogEntry> logs = person.getMedicalLog();
        Collections.reverse(logs);

        return getLogPanel(logs, "Medical log for ", person.getFullName());
    }

    private JPanel fillPatientLog() {
        List<LogEntry> logs = person.getPatientLog();
        Collections.reverse(logs);

        return getLogPanel(logs, "Patient log for ", person.getFullName());
    }

    private JPanel fillAssignmentLog() {
        List<LogEntry> logs = person.getAssignmentLog();
        Collections.reverse(logs);

        return getLogPanel(logs, "Assignment log for ", person.getFullTitle());
    }

    private JPanel fillScenarioLog() {
        List<LogEntry> scenarioLog = person.getScenarioLog();
        Collections.reverse(scenarioLog);

        JPanel pnlScenariosLog = new JPanel(new GridBagLayout());

        JLabel lblScenarios = new JLabel(String.format(resourceMap.getString("format.scenarios"), scenarioLog.size()));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlScenariosLog.add(lblScenarios, gridBagConstraints);

        PersonnelEventLogModel eventModel = new PersonnelEventLogModel();
        eventModel.setData(scenarioLog);
        JTable scenariosTable = new JTable(eventModel);
        lblScenarios.setLabelFor(scenariosTable);
        scenariosTable.getAccessibleContext().setAccessibleName("Scenario log for " + person.getFullName());
        scenariosTable.setRowSelectionAllowed(false);
        scenariosTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumn column;
        for (int i = 0; i < eventModel.getColumnCount(); ++i) {
            column = scenariosTable.getColumnModel().getColumn(i);
            column.setCellRenderer(eventModel.getRenderer());
            column.setPreferredWidth(eventModel.getPreferredWidth(i));
            if (eventModel.hasConstantWidth(i)) {
                column.setMinWidth(eventModel.getPreferredWidth(i));
                column.setMaxWidth(eventModel.getPreferredWidth(i));
            }
        }
        scenariosTable.setIntercellSpacing(new Dimension(0, 0));
        scenariosTable.setShowGrid(false);
        scenariosTable.setTableHeader(null);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlScenariosLog.add(scenariosTable, gridBagConstraints);

        return pnlScenariosLog;
    }

    private JPanel fillInjuries(List<Injury> injuries) {
        final String WARNING_ICON = "\u26A0";

        JPanel pnlInjuries = new JPanel(new BorderLayout());
        pnlInjuries.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("pnlInjuries.title")));

        JPanel pnlInjuryDetails = new JPanel(new GridBagLayout());
        pnlInjuryDetails.getAccessibleContext().setAccessibleName("Injury Details for " + person.getFullName());
        pnlInjuryDetails.setAlignmentY(Component.TOP_ALIGNMENT);

        JLabel lblAdvancedMedical1 = new JLabel();
        JLabel lblAdvancedMedical2 = new JLabel();

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

        double vWeight = 1.0;
        if (person.hasInjuries(true)) {
            vWeight = 0.0;
        }

        if (campaignOptions.isUseAlternativeAdvancedMedical()) {
            getAlternativeAdvancedMedicalDisplay(injuries,
                  lblAdvancedMedical2,
                  lblAdvancedMedical1,
                  gridBagConstraints,
                  vWeight,
                  pnlInjuryDetails);
        } else {
            getAdvancedMedicalDisplay(lblAdvancedMedical1,
                  pnlInjuryDetails,
                  gridBagConstraints,
                  lblAdvancedMedical2,
                  vWeight);
        }

        // This adds a dummy/invisible label to column 2, row 0 to prevent column 3 from being pushed away
        JLabel dummy = new JLabel();
        dummy.setPreferredSize(new Dimension(0, 0));
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInjuryDetails.add(dummy, gridBagConstraints);

        JLabel lblInjury;
        int columns = 3;
        int rowsPerColumn = (int) Math.ceil((double) injuries.size() / columns);
        for (int i = 0; i < injuries.size(); ++i) {
            Injury injury = injuries.get(i);

            int col = i / rowsPerColumn;
            int displayRow = (i % rowsPerColumn) + 1; // Start rows at 1 as we have a header

            String durationValue = injury.isPermanent() ? WARNING_ICON : String.valueOf(injury.getTime());
            String durationColor = injury.isPermanent() ? getNegativeColor() : getWarningColor();
            String durationText = messageSurroundedBySpanWithColor(durationColor, durationValue);
            String label = String.format(resourceMap.getString("format.injuryLabel"), injury.getName(), durationText);

            lblInjury = new JLabel(label);
            gridBagConstraints.gridx = col;
            gridBagConstraints.gridy = displayRow;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInjuryDetails.add(lblInjury, gridBagConstraints);
        }

        pnlInjuries.add(pnlInjuryDetails, BorderLayout.CENTER);

        return pnlInjuries;
    }

    private void getAlternativeAdvancedMedicalDisplay(List<Injury> injuries, JLabel lblAdvancedMedical2,
          JLabel lblAdvancedMedical1,
          GridBagConstraints gridBagConstraints, double vWeight, JPanel pnlInjuryDetails) {
        lblAdvancedMedical2.setName("lblAdvancedMedical2");
        boolean isAmbidextrous = person.getOptions().booleanOption(ATOW_AMBIDEXTROUS);
        List<InjuryEffect> injuryEffects = AdvancedMedicalAlternate.getAllActiveInjuryEffects(isAmbidextrous, injuries);
        lblAdvancedMedical2.setText(InjuryEffect.getEffectsLabel(injuryEffects));
        String injuryEffectTooltip = wordWrap(InjuryEffect.getTooltip(injuryEffects));
        lblAdvancedMedical2.setToolTipText(injuryEffectTooltip);
        lblAdvancedMedical1.setLabelFor(lblAdvancedMedical2);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = vWeight;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInjuryDetails.add(lblAdvancedMedical2, gridBagConstraints);
    }

    private void getAdvancedMedicalDisplay(JLabel lblAdvancedMedical1, JPanel pnlInjuryDetails,
          GridBagConstraints gridBagConstraints,
          JLabel lblAdvancedMedical2, double vWeight) {
        lblAdvancedMedical1.setName("lblAdvancedMedical1");
        lblAdvancedMedical1.setText(resourceMap.getString("lblAdvancedMedical1.text"));
        pnlInjuryDetails.add(lblAdvancedMedical1, gridBagConstraints);

        lblAdvancedMedical2.setName("lblAdvancedMedical2");
        lblAdvancedMedical2.setText(getAdvancedMedalEffectString(person));
        lblAdvancedMedical1.setLabelFor(lblAdvancedMedical2);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = vWeight;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInjuryDetails.add(lblAdvancedMedical2, gridBagConstraints);
    }

    /**
     * Gets the advanced medical effects active for the person.
     *
     * @return an HTML encoded string of effects
     */
    private String getAdvancedMedalEffectString(Person person) {
        StringBuilder medicalEffects = new StringBuilder();
        final int pilotingMod = person.getInjuryModifiers(true);
        final int gunneryMod = person.getInjuryModifiers(false);
        boolean hadEffect = false;

        if ((pilotingMod != 0) && (pilotingMod < Integer.MAX_VALUE)) {
            medicalEffects.append(String.format("Piloting %+d", pilotingMod));
            hadEffect = true;
        } else if (pilotingMod == Integer.MAX_VALUE) {
            medicalEffects.append("Piloting: Impossible");
            hadEffect = true;
        }

        if ((gunneryMod != 0) && (gunneryMod < Integer.MAX_VALUE)) {
            if (hadEffect) {medicalEffects.append(", ");}
            medicalEffects.append(String.format("Gunnery %+d", gunneryMod));
            hadEffect = true;
        } else if (gunneryMod == Integer.MAX_VALUE) {
            if (hadEffect) {medicalEffects.append(", ");}
            medicalEffects.append("Gunnery: Impossible");
            hadEffect = true;
        }

        if (!hadEffect) {
            medicalEffects.append("None");
        }
        return medicalEffects.toString();
    }

    private JPanel fillKillRecord() {
        List<Kill> kills = campaign.getKillsFor(person.getId());
        Collections.reverse(kills);

        JPanel pnlKills = new JPanel(new GridBagLayout());

        JLabel lblRecord = new JLabel(String.format(resourceMap.getString("format.kills"), kills.size()));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlKills.add(lblRecord, gridBagConstraints);

        PersonnelKillLogModel killModel = new PersonnelKillLogModel();
        killModel.setData(kills);
        JTable killTable = new JTable(killModel);
        lblRecord.setLabelFor(killTable);
        killTable.getAccessibleContext().setAccessibleName("Personnel Kill Log");
        killTable.setRowSelectionAllowed(false);
        killTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumn column;
        for (int i = 0; i < killModel.getColumnCount(); ++i) {
            column = killTable.getColumnModel().getColumn(i);
            column.setCellRenderer(killModel.getRenderer());
            column.setPreferredWidth(killModel.getPreferredWidth(i));
            if (killModel.hasConstantWidth(i)) {
                column.setMinWidth(killModel.getPreferredWidth(i));
                column.setMaxWidth(killModel.getPreferredWidth(i));
            }
        }
        killTable.setIntercellSpacing(new Dimension(0, 0));
        killTable.setShowGrid(false);
        killTable.setTableHeader(null);
        gridBagConstraints = new GridBagConstraints();

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;

        pnlKills.add(killTable, gridBagConstraints);

        return pnlKills;
    }

    /**
     * Sets the current {@link Person} object to be displayed by this panel.
     *
     * <p>If the provided {@code person} is {@code null}, the panel is initialized to show default (empty) content.
     * Otherwise, it configures the panel to display the details of the specified {@link Person}.</p>
     *
     * <p>After updating, the panel is revalidated and repainted to reflect the changes.</p>
     *
     * @param person the {@link Person} to display, or {@code null} for empty content
     *
     * @author Illiani
     * @since 0.50.06
     */
    public void setPerson(@Nullable Person person) {
        this.person = person;
        removeAll();
        if (person == null) {
            fillInfoEmpty();
        } else {
            initComponents();
        }
        revalidate();
        repaint();
    }
}
