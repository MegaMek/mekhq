/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.adapter;

import static java.lang.Math.min;
import static java.lang.Math.round;
import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.finances.enums.TransactionType.MEDICAL_EXPENSES;
import static mekhq.campaign.personnel.DiscretionarySpending.getExpenditure;
import static mekhq.campaign.personnel.DiscretionarySpending.getExpenditureExhaustedReportMessage;
import static mekhq.campaign.personnel.DiscretionarySpending.performExtremeExpenditure;
import static mekhq.campaign.personnel.Person.*;
import static mekhq.campaign.personnel.education.Academy.skillParser;
import static mekhq.campaign.personnel.education.EducationController.getAcademy;
import static mekhq.campaign.personnel.education.EducationController.makeEnrollmentCheck;
import static mekhq.campaign.personnel.enums.PersonnelStatus.statusValidator;
import static mekhq.campaign.personnel.enums.education.EducationLevel.DOCTORATE;
import static mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes.REPLACEMENT_LIMB_COST_ARM_TYPE_5;
import static mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes.REPLACEMENT_LIMB_COST_FOOT_TYPE_5;
import static mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes.REPLACEMENT_LIMB_COST_HAND_TYPE_5;
import static mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes.REPLACEMENT_LIMB_COST_LEG_TYPE_5;
import static mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes.REPLACEMENT_LIMB_MINIMUM_SKILL_REQUIRED_TYPES_3_4_5;
import static mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes.REPLACEMENT_LIMB_RECOVERY;
import static mekhq.campaign.personnel.skills.Attributes.MAXIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MINIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.SkillType.S_ARTILLERY;
import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static mekhq.campaign.personnel.skills.SkillType.getType;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.WILLPOWER;
import static mekhq.campaign.randomEvents.personalities.PersonalityController.writeInterviewersNotes;
import static mekhq.campaign.randomEvents.personalities.PersonalityController.writePersonalityDescription;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.processAdHocExecution;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getAmazingColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.spaUtilities.SpaUtilities.getSpaCategory;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ratgenerator.CrewDescriptor;
import megamek.client.ui.dialogs.iconChooser.PortraitChooserDialog;
import megamek.codeUtilities.MathUtility;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.equipment.Mounted;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.common.units.EntityWeightClass;
import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.events.persons.PersonLogEvent;
import mekhq.campaign.events.persons.PersonStatusChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.PerformanceLogger;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.AwardsFactory;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.education.Academy;
import mekhq.campaign.personnel.education.AcademyFactory;
import mekhq.campaign.personnel.education.EducationController;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.enums.education.EducationStage;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.campaign.personnel.generator.AbstractSkillGenerator;
import mekhq.campaign.personnel.generator.DefaultSkillGenerator;
import mekhq.campaign.personnel.generator.SingleSpecialAbilityGenerator;
import mekhq.campaign.personnel.marriage.AbstractMarriage;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.Aging;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillDeprecationTool;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillSubType;
import mekhq.campaign.randomEvents.personalities.PersonalityController;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.CampaignGUI;
import mekhq.gui.PersonnelTab;
import mekhq.gui.control.EditLogControl.LogType;
import mekhq.gui.dialog.*;
import mekhq.gui.displayWrappers.RankDisplay;
import mekhq.gui.menus.AssignPersonToUnitMenu;
import mekhq.gui.model.PersonnelTableModel;
import mekhq.gui.utilities.JMenuHelpers;
import mekhq.gui.utilities.MultiLineTooltip;
import mekhq.gui.utilities.StaticChecks;
import mekhq.utilities.ReportingUtilities;
import mekhq.utilities.spaUtilities.enums.AbilityCategory;

public class PersonnelTableMouseAdapter extends JPopupMenuAdapter {
    private static final MMLogger LOGGER = MMLogger.create(PersonnelTableMouseAdapter.class);

    // region Variable Declarations
    private static final String CMD_SKILL_CHECK = "SKILL_CHECK";
    private static final String CMD_ATTRIBUTE_CHECK = "ATTRIBUTE_CHECK";
    private static final String CMD_MEDICAL_RECORDS = "MEDICAL_RECORDS";
    private static final String CMD_RANK_SYSTEM = "RANK_SYSTEM";
    private static final String CMD_RANK = "RANK";
    private static final String CMD_MANEI_DOMINI_RANK = "MD_RANK";
    private static final String CMD_MANEI_DOMINI_CLASS = "MD_CLASS";
    private static final String CMD_PRIMARY_ROLE = "PRIMARY_ROLL";
    private static final String CMD_SECONDARY_ROLE = "SECONDARY_ROLL";
    private static final String CMD_PRIMARY_DESIGNATOR = "PRIMARY_DESIGNATOR";
    private static final String CMD_SECONDARY_DESIGNATOR = "SECONDARY_DESIGNATOR";
    private static final String CMD_ADD_AWARD = "ADD_AWARD";
    private static final String CMD_RMV_AWARD = "RMV_AWARD";
    private static final String CMD_BEGIN_EDUCATION_ENROLLMENT = "BEGIN_EDUCATION_ENROLLMENT";
    private static final String CMD_BEGIN_EDUCATION_RE_ENROLLMENT = "BEGIN_EDUCATION_RE_ENROLLMENT";
    private static final String CMD_COMPLETE_STAGE = "COMPLETE_STAGE";
    private static final String CMD_DROP_OUT = "DROP_OUT";
    private static final String CMD_CHANGE_EDUCATION_LEVEL = "CHANGE_EDUCATION_LEVEL";

    private static final String CMD_EDIT_SALARY = "SALARY";
    private static final String CMD_GIVE_PAYMENT = "GIVE_PAYMENT";
    private static final String CMD_EDIT_INJURIES = "EDIT_INJURIES";
    private static final String CMD_ADD_RANDOM_INJURY = "ADD_RANDOM_INJURY";
    private static final String CMD_ADD_RANDOM_INJURIES = "ADD_RANDOM_INJURIES";
    private static final String CMD_REMOVE_INJURY = "REMOVE_INJURY";
    private static final String CMD_REPLACE_MISSING_LIMB = "REPLACE_MISSING_LIMB";
    private static final String CMD_CLEAR_INJURIES = "CLEAR_INJURIES";
    private static final String CMD_CALLSIGN = "CALLSIGN";
    private static final String CMD_EDIT_PERSONNEL_LOG = "LOG";
    private static final String CMD_ADD_LOG_ENTRY = "ADD_PERSONNEL_LOG_SINGLE";
    private static final String CMD_EDIT_MEDICAL_LOG = "MEDICAL_LOG";
    private static final String CMD_ADD_MEDICAL_LOG_ENTRY = "ADD_ADD_MEDICAL_LOG_ENTRY";
    private static final String CMD_EDIT_ASSIGNMENT_LOG = "ASSIGNMENT_LOG";
    private static final String CMD_ADD_ASSIGNMENT_LOG_ENTRY = "ADD_ADD_ASSIGNMENT_LOG_ENTRY";
    private static final String CMD_EDIT_PERFORMANCE_LOG = "PERFORMANCE_LOG";
    private static final String CMD_ADD_PERFORMANCE_LOG_ENTRY = "ADD_ADD_PERFORMANCE_LOG_ENTRY";
    private static final String CMD_EDIT_SCENARIO_LOG = "SCENARIO_LOG";
    private static final String CMD_ADD_SCENARIO_ENTRY = "ADD_SCENARIO_ENTRY";
    private static final String CMD_EDIT_KILL_LOG = "KILL_LOG";
    private static final String CMD_ADD_KILL = "ADD_KILL";
    private static final String CMD_SET_XP = "XP_SET";
    private static final String CMD_ADD_XP = "XP_ADD";
    private static final String CMD_EDIT_BIOGRAPHY = "BIOGRAPHY";
    private static final String CMD_EDIT_PORTRAIT = "PORTRAIT";
    private static final String CMD_EDIT_HITS = "EDIT_HITS";
    private static final String CMD_EDIT = "EDIT";
    private static final String CMD_SACK = "SACK";
    private static final String CMD_EMPLOY = "EMPLOY";
    private static final String CMD_SPENDING_SPREE = "SPENDING_SPREE";
    private static final String CMD_CLAIM_BOUNTY = "CLAIM_BOUNTY";
    private static final String CMD_FAMILY_TREE = "CMD_FAMILY_TREE";
    private static final String CMD_REMOVE = "REMOVE";
    private static final String CMD_EDGE_TRIGGER = "EDGE";
    private static final String CMD_CHANGE_PRISONER_STATUS = "PRISONER_STATUS";
    private static final String CMD_CHANGE_STATUS = "STATUS";
    private static final String CMD_ACQUIRE_SPECIALIST = "SPECIALIST";
    private static final String CMD_ACQUIRE_WEAPON_SPECIALIST = "WEAPON_SPECIALIST";
    private static final String CMD_ACQUIRE_SANDBLASTER = "SANDBLASTER";
    private static final String CMD_ACQUIRE_RANGEMASTER = "RANGEMASTER";
    private static final String CMD_ACQUIRE_ENVIRONMENT_SPECIALIST = "ENVIRONMENT_SPECIALIST";
    private static final String CMD_ACQUIRE_HUMAN_TRO = "HUMAN_TRO";
    private static final String CMD_ACQUIRE_ABILITY = "ABILITY";
    private static final String CMD_ACQUIRE_CUSTOM_CHOICE = "CUSTOM_CHOICE";
    private static final String CMD_REFUND_SKILL = "REFUND_SKILL";
    private static final String CMD_IMPROVE = "IMPROVE";
    private static final String CMD_BUY_TRAIT = "BUY_TRAIT";
    private static final String CMD_CHANGE_ATTRIBUTE = "CHANGE_ATTRIBUTE";
    private static final String CMD_SET_ATTRIBUTE = "SET_ATTRIBUTE";
    private static final String CMD_RANDOM_PROFESSION = "RANDOM_PROFESSION";
    private static final String CMD_ADD_SPOUSE = "SPOUSE";
    private static final String CMD_REMOVE_SPOUSE = "REMOVE_SPOUSE";
    private static final String CMD_ADD_PREGNANCY = "ADD_PREGNANCY";
    private static final String CMD_REMOVE_PREGNANCY = "PREGNANCY_SPOUSE";
    private static final String CMD_LOYALTY = "LOYALTY";
    private static final String CMD_PERSONALITY = "PERSONALITY";
    private static final String CMD_ADD_RANDOM_ABILITY = "ADD_RANDOM_ABILITY";
    private static final String CMD_GENERATE_ROLEPLAY_SKILLS = "GENERATE_ROLEPLAY_SKILLS";
    private static final String CMD_REMOVE_ROLEPLAY_SKILLS = "REMOVE_ROLEPLAY_SKILLS";
    private static final String CMD_GENERATE_ROLEPLAY_ATTRIBUTES = "GENERATE_ROLEPLAY_ATTRIBUTES";
    private static final String CMD_GENERATE_ROLEPLAY_TRAITS = "GENERATE_ROLEPLAY_TRAITS";

    private static final String CMD_FREE = "FREE";
    private static final String CMD_EXECUTE = "EXECUTE";
    private static final String CMD_JETTISON = "JETTISON";
    private static final String CMD_RECRUIT = "RECRUIT";
    private static final String CMD_ABTAKHA = "ABTAKHA";
    private static final String CMD_ADOPTION = "ADOPTION";
    private static final String CMD_ADD_PARENT = "CMD_ADD_PARENT";
    private static final String CMD_REMOVE_PARENT = "CMD_REMOVE_PARENT";
    private static final String CMD_ADD_CHILD = "CMD_ADD_CHILD";
    private static final String CMD_REMOVE_CHILD = "CMD_REMOVE_CHILD";
    private static final String CMD_RANSOM = "RANSOM";
    private static final String CMD_RANSOM_FRIENDLY = "RANSOM_FRIENDLY";

    // MekWarrior Edge Options
    private static final String OPT_EDGE_MASC_FAILURE = "edge_when_masc_fails";
    private static final String OPT_EDGE_EXPLOSION = "edge_when_explosion";
    private static final String OPT_EDGE_KO = "edge_when_ko";
    private static final String OPT_EDGE_TAC = "edge_when_tac";
    private static final String OPT_EDGE_HEAD_HIT = "edge_when_headhit";

    // Aero Edge Options
    private static final String OPT_EDGE_WHEN_AERO_ALT_LOSS = "edge_when_aero_alt_loss";
    private static final String OPT_EDGE_WHEN_AERO_EXPLOSION = "edge_when_aero_explosion";
    private static final String OPT_EDGE_WHEN_AERO_KO = "edge_when_aero_ko";
    private static final String OPT_EDGE_WHEN_AERO_LUCKY_CRIT = "edge_when_aero_lucky_crit";
    private static final String OPT_EDGE_WHEN_AERO_NUKE_CRIT = "edge_when_aero_nuke_crit";
    private static final String OPT_EDGE_WHEN_AERO_UNIT_CARGO_LOST = "edge_when_aero_unit_cargo_lost";

    // region Randomization Menu
    private static final String CMD_RANDOM_NAME = "RANDOM_NAME";
    private static final String CMD_RANDOM_BLOODNAME = "RANDOM_BLOODNAME";
    private static final String CMD_RANDOM_CALLSIGN = "RANDOM_CALLSIGN";
    private static final String CMD_RANDOM_PORTRAIT = "RANDOM_PORTRAIT";
    private static final String CMD_RANDOM_ORIGIN = "RANDOM_ORIGIN";
    private static final String CMD_RANDOM_ORIGIN_FACTION = "RANDOM_ORIGIN_FACTION";
    private static final String CMD_RANDOM_ORIGIN_PLANET = "RANDOM_ORIGIN_PLANET";
    // endregion Randomization Menu

    // region Original Unit
    private static final String CMD_ORIGINAL_TO_CURRENT = "ORIGINAL_TO_CURRENT";
    private static final String CMD_WIPE_ORIGINAL = "WIPE_ORIGINAL";

    // endregion Original Unit

    private static final String SEPARATOR = "@";
    private static final String TRUE = String.valueOf(true);
    private static final String FALSE = String.valueOf(false);

    private final CampaignGUI gui;
    private final JTable personnelTable;
    private final PersonnelTableModel personnelModel;

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    protected PersonnelTableMouseAdapter(CampaignGUI gui, JTable personnelTable, PersonnelTableModel personnelModel) {
        this.gui = gui;
        this.personnelTable = personnelTable;
        this.personnelModel = personnelModel;
    }

    private JFrame getFrame() {
        return gui.getFrame();
    }

    private Campaign getCampaign() {
        return gui.getCampaign();
    }

    private CampaignOptions getCampaignOptions() {
        return getCampaign().getCampaignOptions();
    }

    public static void connect(CampaignGUI gui, JTable personnelTable, PersonnelTableModel personnelModel,
          JSplitPane splitPersonnel) {
        new PersonnelTableMouseAdapter(gui, personnelTable, personnelModel) {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 2)) {
                    int width = splitPersonnel.getSize().width;
                    int location = splitPersonnel.getDividerLocation();
                    int size = splitPersonnel.getDividerSize();
                    if ((width - location + size) < PersonnelTab.PERSONNEL_VIEW_WIDTH) {
                        // expand
                        splitPersonnel.resetToPreferredSizes();
                    } else {
                        // collapse
                        splitPersonnel.setDividerLocation(1.0);
                    }
                }
            }
        }.connect(personnelTable);
    }

    private String makeCommand(String... parts) {
        return Utilities.combineString(Arrays.asList(parts), SEPARATOR);
    }

    @Override
    public void actionPerformed(ActionEvent action) {
        int row = personnelTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Person selectedPerson = personnelModel.getPerson(personnelTable.convertRowIndexToModel(row));
        int[] rows = personnelTable.getSelectedRows();
        Person[] people = new Person[rows.length];
        for (int i = 0; i < rows.length; i++) {
            people[i] = personnelModel.getPerson(personnelTable.convertRowIndexToModel(rows[i]));
        }

        String[] data = action.getActionCommand().split(SEPARATOR, -1);

        switch (data[0]) {
            case CMD_SKILL_CHECK: {
                for (final Person person : people) {
                    new SkillCheckDialog(getCampaign(), person);
                }
                break;
            }
            case CMD_ATTRIBUTE_CHECK: {
                for (final Person person : people) {
                    new AttributeCheckDialog(getCampaign(), person);
                }
                break;
            }
            case CMD_MEDICAL_RECORDS: {
                MedicalViewDialog medDialog = new MedicalViewDialog(null, getCampaign(), selectedPerson);
                medDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
                medDialog.setVisible(true);
                break;
            }
            case CMD_RANK_SYSTEM: {
                final RankSystem rankSystem = Ranks.getRankSystemFromCode(data[1]);
                final RankValidator rankValidator = new RankValidator();
                for (final Person person : people) {
                    person.setRankSystem(rankValidator, rankSystem);
                }
                break;
            }
            case CMD_RANK: {
                List<Person> promotedPersonnel = new ArrayList<>();
                try {
                    final int rank = MathUtility.parseInt(data[1]);
                    final int level = (data.length > 2) ? MathUtility.parseInt(data[2]) : 0;
                    for (final Person person : people) {
                        person.changeRank(getCampaign(), rank, level, true);

                        promotedPersonnel.add(person);
                    }

                    if ((getCampaignOptions().isEnableAutoAwards()) && (!promotedPersonnel.isEmpty())) {
                        AutoAwardsController autoAwardsController = new AutoAwardsController();
                        autoAwardsController.PromotionController(getCampaign(), false);
                    }
                } catch (Exception e) {
                    LOGGER.error("", e);
                }
                break;
            }
            case CMD_MANEI_DOMINI_CLASS: {
                try {
                    final ManeiDominiClass mdClass = ManeiDominiClass.valueOf(data[1]);
                    for (final Person person : people) {
                        person.setManeiDominiClass(mdClass);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to assign Manei Domini Class", e);
                }
                break;
            }
            case CMD_MANEI_DOMINI_RANK: {
                final ManeiDominiRank maneiDominiRank = ManeiDominiRank.valueOf(data[1]);
                for (final Person person : people) {
                    person.setManeiDominiRank(maneiDominiRank);
                }
                break;
            }
            case CMD_PRIMARY_DESIGNATOR: {
                try {
                    ROMDesignation romDesignation = ROMDesignation.valueOf(data[1]);
                    for (Person person : people) {
                        person.setPrimaryDesignator(romDesignation);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to assign ROM designator", e);
                }
                break;
            }
            case CMD_SECONDARY_DESIGNATOR: {
                try {
                    ROMDesignation romDesignation = ROMDesignation.valueOf(data[1]);
                    for (Person person : people) {
                        person.setSecondaryDesignator(romDesignation);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to assign ROM secondary designator", e);
                }
                break;
            }
            case CMD_PRIMARY_ROLE: {
                PersonnelRole role = PersonnelRole.valueOf(data[1]);
                for (final Person person : people) {
                    person.setPrimaryRole(getCampaign(), role);
                    writePersonalityDescription(person);
                    writeInterviewersNotes(person);
                    getCampaign().personUpdated(person);
                    if (getCampaignOptions().isUsePortraitForRole(role) &&
                              getCampaignOptions().isAssignPortraitOnRoleChange() &&
                              person.getPortrait().hasDefaultFilename()) {
                        getCampaign().assignRandomPortraitFor(person);
                    }
                }
                break;
            }
            case CMD_SECONDARY_ROLE: {
                PersonnelRole role = PersonnelRole.valueOf(data[1]);
                for (final Person person : people) {
                    person.setSecondaryRole(role);
                    getCampaign().personUpdated(person);
                }
                break;
            }
            case CMD_ADD_PREGNANCY: {
                Stream.of(people)
                      .filter(person -> (getCampaign().getProcreation()
                                               .canProcreate(getCampaign().getLocalDate(), person, false) == null))
                      .forEach(person -> {
                          getCampaign().getProcreation()
                                .addPregnancy(getCampaign(), getCampaign().getLocalDate(), person, false);
                          MekHQ.triggerEvent(new PersonChangedEvent(person));
                      });
                break;
            }
            case CMD_REMOVE_PREGNANCY: {
                Stream.of(people).filter(Person::isPregnant).forEach(person -> {
                    getCampaign().getProcreation().removePregnancy(person);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                });
                break;
            }
            case CMD_REMOVE_SPOUSE: {
                Stream.of(people)
                      .filter(person -> getCampaign().getDivorce().canDivorce(person, false) == null)
                      .forEach(person -> getCampaign().getDivorce()
                                               .divorce(getCampaign(),
                                                     getCampaign().getLocalDate(),
                                                     person,
                                                     SplittingSurnameStyle.valueOf(data[1])));
                break;
            }
            case CMD_ADD_SPOUSE: {
                getCampaign().getMarriage()
                      .marry(getCampaign(),
                            getCampaign().getLocalDate(),
                            selectedPerson,
                            getCampaign().getPerson(UUID.fromString(data[1])),
                            MergingSurnameStyle.valueOf(data[2]),
                            false);
                break;
            }
            case CMD_ADD_AWARD: {
                for (Person person : people) {
                    person.getAwardController()
                          .addAndLogAward(getCampaign(), data[1], data[2], getCampaign().getLocalDate());
                }
                break;
            }
            case CMD_RMV_AWARD: {
                for (Person person : people) {
                    try {
                        if (person.getAwardController().hasAward(data[1], data[2])) {
                            person.getAwardController()
                                  .removeAward(data[1],
                                        data[2],
                                        (data.length > 3) ?
                                              MekHQ.getMHQOptions().parseDisplayFormattedDate(data[3]) :
                                              null,
                                        getCampaign().getLocalDate());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Could not remove award.", e);
                    }
                }
                break;
            }
            case CMD_BEGIN_EDUCATION_ENROLLMENT: {
                processApplication(people, data, false);
                break;
            }
            case CMD_BEGIN_EDUCATION_RE_ENROLLMENT: {
                processApplication(people, data, true);
                break;
            }
            case CMD_COMPLETE_STAGE: {
                List<UUID> graduatingPersonnel = new ArrayList<>();
                HashMap<UUID, List<Object>> academyAttributesMap = new HashMap<>();

                for (Person person : people) {
                    Academy academy = getAcademy(person.getEduAcademySet(), person.getEduAcademyNameInSet());

                    if (academy == null) {
                        LOGGER.debug("Found null academy for {} skipping", person.getFullTitle());
                        continue;
                    }

                    EducationStage educationStage = person.getEduEducationStage();

                    switch (educationStage) {
                        case JOURNEY_TO_CAMPUS:
                        case JOURNEY_FROM_CAMPUS:
                            // this should be enough to ensure even the most distant academy is
                            // reached/returned from
                            person.setEduDaysOfTravel(9999);
                            break;
                        case EDUCATION:
                            if (!academy.isPrepSchool()) {
                                person.setEduEducationTime(1);
                            }
                            break;
                        default:
                            break;
                    }

                    List<Object> individualAcademyAttributes = new ArrayList<>();

                    // if graduating, process autoAwards component for this person
                    if (EducationController.processNewDay(getCampaign(), person, true)) {
                        graduatingPersonnel.add(person.getId());

                        individualAcademyAttributes.add(academy.getEducationLevel(person));
                        individualAcademyAttributes.add(academy.getType());
                        individualAcademyAttributes.add(academy.getName());

                        academyAttributesMap.put(person.getId(), individualAcademyAttributes);
                    }

                    MekHQ.triggerEvent(new PersonStatusChangedEvent(person));
                }

                if (!graduatingPersonnel.isEmpty()) {
                    AutoAwardsController autoAwardsController = new AutoAwardsController();
                    autoAwardsController.PostGraduationController(getCampaign(),
                          graduatingPersonnel,
                          academyAttributesMap);
                }
                break;
            }
            case CMD_CHANGE_EDUCATION_LEVEL: {
                EducationLevel educationLevel = EducationLevel.fromString(data[1]);

                for (Person person : people) {
                    person.setEduHighestEducation(educationLevel);

                    if (educationLevel == DOCTORATE) {
                        if (person.getPreNominal() == null || person.getPreNominal().isBlank()) {
                            person.setPreNominal(resources.getString("eduDoctorPrenominal.text"));
                        }
                    } else {
                        if (person.getPreNominal().equals(resources.getString("eduDoctorPrenominal.text"))) {
                            person.setPreNominal("");
                        }
                    }

                    MekHQ.triggerEvent(new PersonStatusChangedEvent(person));
                }
                break;
            }
            case CMD_DROP_OUT: {
                for (Person person : people) {
                    Academy academy = getAcademy(person.getEduAcademySet(), person.getEduAcademyNameInSet());

                    if (academy == null) {
                        LOGGER.debug("Found null academy for {} skipping", person.getFullTitle());
                        continue;
                    }

                    EducationStage educationStage = person.getEduEducationStage();

                    switch (educationStage) {
                        case JOURNEY_TO_CAMPUS, JOURNEY_FROM_CAMPUS ->
                              person.changeStatus(getCampaign(), getCampaign().getLocalDate(), PersonnelStatus.ACTIVE);
                        case EDUCATION -> EducationController.processForcedDropOut(getCampaign(), person, academy);
                        default -> {
                        }
                    }

                    MekHQ.triggerEvent(new PersonStatusChangedEvent(person));
                }

                break;
            }
            case CMD_IMPROVE: {
                String type = data[1];
                int cost = MathUtility.parseInt(data[2]);
                selectedPerson.improveSkill(type);
                selectedPerson.spendXPOnSkills(getCampaign(), cost);

                Skill skill = selectedPerson.getSkill(type);
                SkillType skillType = skill.getType();

                PerformanceLogger.improvedSkill(getCampaignOptions().isPersonnelLogSkillGain(),
                      selectedPerson,
                      getCampaign().getLocalDate(),
                      skillType.getName(),
                      skill.getLevel());
                getCampaign().addReport(String.format(resources.getString("improved.format"),
                      selectedPerson.getHyperlinkedName(),
                      type));

                getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_REFUND_SKILL: {
                String typeLabel = data[1];
                SkillType skillType = SkillType.getType(typeLabel);
                Skills skills = selectedPerson.getSkills();
                int refundValue = SkillDeprecationTool.getRefundValue(skills, skillType, skillType.getName());

                selectedPerson.removeSkill(skillType.getName());
                selectedPerson.awardXP(getCampaign(), refundValue);

                getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_BUY_TRAIT: {
                String type = data[1];
                int cost = MathUtility.parseInt(data[2]);
                int target = MathUtility.parseInt(data[3]);

                switch (type) {
                    case CONNECTIONS_LABEL -> selectedPerson.setConnections(target);
                    case REPUTATION_LABEL -> selectedPerson.setReputation(target);
                    case WEALTH_LABEL -> selectedPerson.setWealth(target);
                    case UNLUCKY_LABEL -> selectedPerson.setUnlucky(target);
                    case BLOODMARK_LABEL -> selectedPerson.setBloodmark(target);
                    case EXTRA_INCOME_LABEL -> selectedPerson.setExtraIncomeFromTraitLevel(target);
                    default -> LOGGER.error("Invalid trait type: {}", type);
                }

                selectedPerson.spendXP(cost);

                getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_CHANGE_ATTRIBUTE: {
                SkillAttribute attribute = SkillAttribute.fromString(data[1]);

                selectedPerson.changeAttributeScore(attribute, 1);

                int cost = MathUtility.parseInt(data[2]);
                selectedPerson.spendXP(cost);

                getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_SET_ATTRIBUTE: {
                SkillAttribute attribute = SkillAttribute.fromString(data[1]);

                PopupValueChoiceDialog choiceDialog = new PopupValueChoiceDialog(getFrame(),
                      true,
                      resources.getString("spendOnAttributes.score"),
                      selectedPerson.getAttributeScore(attribute),
                      MINIMUM_ATTRIBUTE_SCORE);
                choiceDialog.setVisible(true);

                int choice = choiceDialog.getValue();
                if (choice < 0) {
                    // <0 indicates Cancellation
                    return;
                }

                for (Person person : people) {
                    person.setAttributeScore(attribute, choice);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                    getCampaign().personUpdated(person);
                }

                break;
            }
            case CMD_RANDOM_PROFESSION: {
                List<PersonnelRole> possibleRoles = PersonnelRole.getCivilianRolesExceptNone();
                LocalDate today = getCampaign().getLocalDate();

                for (Person person : people) {
                    // Under 16s are considered children and unable to be assigned a profession
                    if (person.isChild(today)) {
                        continue;
                    }

                    List<PersonnelRole> eligibleRoles = new ArrayList<>(possibleRoles);

                    int personAge = person.getAge(today);

                    if (personAge < 18) {
                        // Characters under 18 years old are strictly unable to be assigned these roles.
                        // This is project policy.
                        eligibleRoles.remove(PersonnelRole.ADULT_ENTERTAINER);
                        eligibleRoles.remove(PersonnelRole.LUXURY_COMPANION);
                    }

                    PersonnelRole randomProfession = ObjectUtility.getRandomItem(eligibleRoles);
                    int experienceLevel = CrewDescriptor.randomExperienceLevel();

                    for (String skillName : randomProfession.getSkillsForProfession()) {
                        if (person.getSkill(skillName) != null) { // They already have this skill
                            continue;
                        }

                        SkillType skillType = SkillType.getType(skillName);
                        int targetLevel = skillType.getExperienceLevel(experienceLevel);
                        person.addSkill(skillName, targetLevel, 0);
                    }

                    Aging.updateAllSkillAgeModifiers(today, person);
                    person.setPrimaryRole(getCampaign(), randomProfession);

                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                    getCampaign().personUpdated(person);
                }

                break;
            }
            case CMD_ACQUIRE_ABILITY: {
                String selected = data[1];
                int cost = MathUtility.parseInt(data[2]);
                selectedPerson.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, selected, true);
                selectedPerson.spendXP(cost);
                final String displayName = SpecialAbility.getDisplayName(selected);
                PerformanceLogger.gainedSPA(getCampaign(), selectedPerson, getCampaign().getLocalDate(), displayName);
                getCampaign().addReport(String.format(resources.getString("gained.format"),
                      selectedPerson.getHyperlinkedName(),
                      displayName));
                getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_WEAPON_SPECIALIST: {
                String selected = data[1];
                int cost = MathUtility.parseInt(data[2]);
                selectedPerson.getOptions()
                      .acquireAbility(PersonnelOptions.LVL3_ADVANTAGES,
                            OptionsConstants.GUNNERY_WEAPON_SPECIALIST,
                            selected);
                selectedPerson.spendXP(cost);
                final String displayName = String.format("%s %s",
                      SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_WEAPON_SPECIALIST),
                      selected);
                PerformanceLogger.gainedSPA(getCampaign(), selectedPerson, getCampaign().getLocalDate(), displayName);
                getCampaign().addReport(String.format(resources.getString("gained.format"),
                      selectedPerson.getHyperlinkedName(),
                      displayName));
                getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_SANDBLASTER: {
                String selected = data[1];
                int cost = MathUtility.parseInt(data[2]);
                selectedPerson.getOptions()
                      .acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, OptionsConstants.GUNNERY_SANDBLASTER, selected);
                selectedPerson.spendXP(cost);
                final String displayName = String.format("%s %s",
                      SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_SANDBLASTER),
                      selected);
                PerformanceLogger.gainedSPA(getCampaign(), selectedPerson, getCampaign().getLocalDate(), displayName);
                getCampaign().addReport(String.format(resources.getString("gained.format"),
                      selectedPerson.getHyperlinkedName(),
                      displayName));
                getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_SPECIALIST: {
                String selected = data[1];
                int cost = MathUtility.parseInt(data[2]);
                selectedPerson.getOptions()
                      .acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, OptionsConstants.GUNNERY_SPECIALIST, selected);
                selectedPerson.spendXP(cost);
                final String displayName = String.format("%s %s",
                      SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_SPECIALIST),
                      selected);
                PerformanceLogger.gainedSPA(getCampaign(), selectedPerson, getCampaign().getLocalDate(), displayName);
                getCampaign().addReport(String.format(resources.getString("gained.format"),
                      selectedPerson.getHyperlinkedName(),
                      displayName));
                getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_RANGEMASTER: {
                String selected = data[1];
                int cost = MathUtility.parseInt(data[2]);
                selectedPerson.getOptions()
                      .acquireAbility(PersonnelOptions.LVL3_ADVANTAGES,
                            OptionsConstants.GUNNERY_RANGE_MASTER,
                            selected);
                selectedPerson.spendXP(cost);
                final String displayName = String.format("%s %s",
                      SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_RANGE_MASTER),
                      selected);
                PerformanceLogger.gainedSPA(getCampaign(), selectedPerson, getCampaign().getLocalDate(), displayName);
                getCampaign().addReport(String.format(resources.getString("gained.format"),
                      selectedPerson.getHyperlinkedName(),
                      displayName));
                getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_ENVIRONMENT_SPECIALIST: {
                String selected = data[1];
                int cost = MathUtility.parseInt(data[2]);
                selectedPerson.getOptions()
                      .acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, OptionsConstants.MISC_ENV_SPECIALIST, selected);
                selectedPerson.spendXP(cost);
                final String displayName = String.format("%s %s",
                      SpecialAbility.getDisplayName(OptionsConstants.MISC_ENV_SPECIALIST),
                      selected);
                PerformanceLogger.gainedSPA(getCampaign(), selectedPerson, getCampaign().getLocalDate(), displayName);
                getCampaign().addReport(String.format(resources.getString("gained.format"),
                      selectedPerson.getHyperlinkedName(),
                      displayName));
                getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_HUMAN_TRO: {
                String selected = data[1];
                int cost = MathUtility.parseInt(data[2]);
                selectedPerson.getOptions()
                      .acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, OptionsConstants.MISC_HUMAN_TRO, selected);
                selectedPerson.spendXP(cost);
                final String displayName = String.format("%s %s",
                      SpecialAbility.getDisplayName(OptionsConstants.MISC_HUMAN_TRO),
                      selected);
                PerformanceLogger.gainedSPA(getCampaign(), selectedPerson, getCampaign().getLocalDate(), displayName);
                getCampaign().addReport(String.format(resources.getString("gained.format"),
                      selectedPerson.getHyperlinkedName(),
                      displayName));
                getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_CUSTOM_CHOICE: {
                String selected = data[1];
                int cost = MathUtility.parseInt(data[2]);
                String ability = data[3];
                selectedPerson.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, ability, selected);
                selectedPerson.spendXP(cost);
                final String displayName = String.format("%s %s", SpecialAbility.getDisplayName(ability), selected);
                PerformanceLogger.gainedSPA(getCampaign(), selectedPerson, getCampaign().getLocalDate(), displayName);
                getCampaign().addReport(String.format(resources.getString("spaGainedChoices.format"),
                      selectedPerson.getHyperlinkedName(),
                      displayName));
                getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_CHANGE_STATUS: {
                PersonnelStatus status = PersonnelStatus.valueOf(data[1]);
                for (Person person : people) {
                    if (status.isActive() ||
                              (JOptionPane.showConfirmDialog(null,
                                    String.format(resources.getString("confirmRetireQ.format"), person.getFullTitle()),
                                    status.toString(),
                                    JOptionPane.YES_NO_OPTION) == 0)) {
                        person.changeStatus(getCampaign(), getCampaign().getLocalDate(), status);
                    }
                }
                break;
            }
            case CMD_CHANGE_PRISONER_STATUS: {
                try {
                    PrisonerStatus status = PrisonerStatus.valueOf(data[1]);
                    for (Person person : people) {
                        if (person.getPrisonerStatus() != status) {
                            person.setPrisonerStatus(getCampaign(), status, true);

                            if (status.isCurrentPrisoner()) {
                                statusValidator(getCampaign(), person, true);
                            }

                            MekHQ.triggerEvent(new PersonStatusChangedEvent(person));
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Unknown PrisonerStatus Option. No changes will be made.", e);
                }
                break;
            }
            case CMD_FREE: {
                processPrisonerResolutionCommand(people, "confirmFree.format", "freeQ.text", false);

                break;
            }
            case CMD_EXECUTE: {
                processPrisonerResolutionCommand(people, "confirmExecute.format", "executeQ.text", true);

                break;
            }
            case CMD_JETTISON: {
                processPrisonerResolutionCommand(people, "confirmJettison.format", "jettisonQ.text", true);

                break;
            }
            case CMD_RECRUIT: {
                for (Person person : people) {
                    if (person.getPrisonerStatus().isPrisonerDefector()) {
                        person.setPrisonerStatus(getCampaign(), PrisonerStatus.FREE, true);
                    }
                }
                break;
            }
            case CMD_ABTAKHA: {
                for (Person person : people) {
                    if (person.getPrisonerStatus().isBondsman()) {
                        person.setPrisonerStatus(getCampaign(), PrisonerStatus.FREE, true);
                    }
                }
                break;
            }
            case CMD_ADOPTION: {
                Person orphan = getCampaign().getPerson(UUID.fromString(data[1]));
                if (orphan == null) {
                    LOGGER.error("Could not find orphaned person with UUID {}. No changes will be made.", data[1]);
                    return;
                }

                Genealogy orphanGenealogy = orphan.getGenealogy();
                if (orphanGenealogy == null) {
                    LOGGER.error("Could not find orphaned person's genealogy. No changes will be made.");
                    return;
                }

                // clear the old parents
                List<Person> originalParents = new ArrayList<>(orphanGenealogy.getParents());
                for (Person parent : originalParents) {
                    orphanGenealogy.removeFamilyMember(FamilialRelationshipType.PARENT, parent);
                }

                // add the new
                for (Person person : people) {
                    Genealogy personGenealogy = person.getGenealogy();
                    if (personGenealogy == null) {
                        LOGGER.error("Could not find {}'s genealogy. No changes will be made.", person.getFullTitle());
                        continue;
                    }

                    personGenealogy.addFamilyMember(FamilialRelationshipType.CHILD, orphan);
                    orphanGenealogy.addFamilyMember(FamilialRelationshipType.PARENT, person);

                    MekHQ.triggerEvent(new PersonChangedEvent(person));

                    if (personGenealogy.hasSpouse()) {
                        Person spouse = personGenealogy.getSpouse();
                        Genealogy spouseGenealogy = spouse.getGenealogy();
                        if (spouseGenealogy == null) {
                            LOGGER.error("Could not find {}'s (spouse) genealogy. No changes will be made.",
                                  spouse.getFullTitle());
                            continue;
                        }

                        spouse.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, orphan);
                        orphanGenealogy.addFamilyMember(FamilialRelationshipType.PARENT, spouse);

                        MekHQ.triggerEvent(new PersonChangedEvent(spouse));
                    }
                }

                MekHQ.triggerEvent(new PersonChangedEvent(orphan));
                break;
            }
            case CMD_ADD_PARENT: {
                Person newParent = getCampaign().getPerson(UUID.fromString(data[1]));
                if (newParent == null) {
                    LOGGER.warn("Could not find new parent with UUID {}. No changes will be made.", data[1]);
                    return;
                }

                Genealogy newParentGenealogy = newParent.getGenealogy();
                newParentGenealogy.addFamilyMember(FamilialRelationshipType.CHILD, selectedPerson);
                MekHQ.triggerEvent(new PersonChangedEvent(newParent));

                Genealogy selectedPersonGenealogy = selectedPerson.getGenealogy();
                selectedPersonGenealogy.addFamilyMember(FamilialRelationshipType.PARENT, newParent);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                break;
            }
            case CMD_REMOVE_PARENT: {
                Person oldParent = getCampaign().getPerson(UUID.fromString(data[1]));
                if (oldParent == null) {
                    LOGGER.warn("Could not find old parent with UUID {}. No changes will be made.", data[1]);
                    return;
                }

                Genealogy oldParentGenealogy = oldParent.getGenealogy();
                oldParentGenealogy.removeFamilyMember(FamilialRelationshipType.CHILD, selectedPerson);
                MekHQ.triggerEvent(new PersonChangedEvent(oldParent));

                Genealogy selectedPersonGenealogy = selectedPerson.getGenealogy();
                selectedPersonGenealogy.removeFamilyMember(FamilialRelationshipType.PARENT, oldParent);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                break;
            }
            case CMD_ADD_CHILD: {
                Person newChild = getCampaign().getPerson(UUID.fromString(data[1]));
                if (newChild == null) {
                    LOGGER.warn("Could not find new child with UUID {}. No changes will be made.", data[1]);
                    return;
                }

                Genealogy newChildGenealogy = newChild.getGenealogy();
                newChildGenealogy.addFamilyMember(FamilialRelationshipType.PARENT, selectedPerson);
                MekHQ.triggerEvent(new PersonChangedEvent(newChild));

                Genealogy selectedPersonGenealogy = selectedPerson.getGenealogy();
                selectedPersonGenealogy.addFamilyMember(FamilialRelationshipType.CHILD, newChild);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                break;
            }
            case CMD_REMOVE_CHILD: {
                Person oldChild = getCampaign().getPerson(UUID.fromString(data[1]));
                if (oldChild == null) {
                    LOGGER.warn("Could not find old child with UUID {}. No changes will be made.", data[1]);
                    return;
                }

                Genealogy oldChildGenealogy = oldChild.getGenealogy();
                oldChildGenealogy.removeFamilyMember(FamilialRelationshipType.PARENT, selectedPerson);
                MekHQ.triggerEvent(new PersonChangedEvent(oldChild));

                Genealogy selectedPersonGenealogy = selectedPerson.getGenealogy();
                selectedPersonGenealogy.removeFamilyMember(FamilialRelationshipType.CHILD, oldChild);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                break;
            }
            case CMD_RANSOM: {
                // ask the user if they want to sell off their prisoners. If yes, then add a
                // daily report entry, add the money and remove them all.
                Money total = Money.zero();
                total = total.plus(Arrays.stream(people)
                                         .map(person -> person.getRansomValue(getCampaign()))
                                         .collect(Collectors.toList()));

                if (0 ==
                          JOptionPane.showConfirmDialog(null,
                                String.format(resources.getString("ransomQ.format"),
                                      people.length,
                                      total.toAmountAndSymbolString()),
                                resources.getString("ransom.text"),
                                JOptionPane.YES_NO_OPTION)) {
                    getCampaign().addReport(String.format(resources.getString("ransomReport.format"),
                          people.length,
                          total.toAmountAndSymbolString()));
                    getCampaign().addFunds(TransactionType.RANSOM, total, resources.getString("ransom.text"));
                    for (Person person : people) {
                        getCampaign().removePerson(person, false);
                    }
                }
                break;
            }
            case CMD_RANSOM_FRIENDLY: {
                Money total = Money.zero();
                total = total.plus(Arrays.stream(people)
                                         .map(person -> person.getRansomValue(getCampaign()))
                                         .collect(Collectors.toList()));

                if (getCampaign().getFunds().isLessThan(total)) {
                    getCampaign().addReport(String.format(resources.getString("unableToRansom.format"),
                          people.length,
                          total.toAmountAndSymbolString()));
                    break;
                }

                if (0 ==
                          JOptionPane.showConfirmDialog(null,
                                String.format(resources.getString("ransomFriendlyQ.format"),
                                      people.length,
                                      total.toAmountAndSymbolString()),
                                resources.getString("ransom.text"),
                                JOptionPane.YES_NO_OPTION)) {
                    getCampaign().addReport(String.format(resources.getString("ransomReport.format"),
                          people.length,
                          total.toAmountAndSymbolString()));
                    getCampaign().removeFunds(TransactionType.RANSOM, total, resources.getString("ransom.text"));
                    for (Person person : people) {
                        person.changeStatus(getCampaign(), getCampaign().getLocalDate(), PersonnelStatus.ACTIVE);
                    }
                }
                break;
            }
            case CMD_EDGE_TRIGGER: {
                String trigger = data[1];
                if (people.length > 1) {
                    boolean status = Boolean.parseBoolean(data[2]);
                    for (Person person : people) {
                        person.setEdgeTrigger(trigger, status);
                        getCampaign().personUpdated(person);
                    }
                } else {
                    selectedPerson.changeEdgeTrigger(trigger);
                    getCampaign().personUpdated(selectedPerson);
                }
                break;
            }
            case CMD_REMOVE: {
                String title = (people.length == 1) ?
                                     people[0].getFullTitle() :
                                     String.format(resources.getString("numPersonnel.text"), people.length);
                if (0 ==
                          JOptionPane.showConfirmDialog(null,
                                String.format(resources.getString("confirmRemove.format"), title),
                                resources.getString("removeQ.text"),
                                JOptionPane.YES_NO_OPTION)) {
                    for (Person person : people) {
                        getCampaign().removePerson(person);
                    }
                }
                break;
            }
            case CMD_SACK: {
                boolean showDialog = false;
                List<Person> toRemove = new ArrayList<>();
                if (getCampaignOptions().isUseAtB()) {
                    for (Person person : people) {
                        if (!person.getPrimaryRole().isCivilian()) {
                            if (getCampaign().getRetirementDefectionTracker()
                                      .removeFromCampaign(person, false, true, getCampaign(), null)) {
                                showDialog = true;
                            } else {
                                toRemove.add(person);
                            }
                        } else {
                            toRemove.add(person);
                        }
                    }
                }

                if (showDialog) {
                    RetirementDefectionDialog rdd = new RetirementDefectionDialog(gui, null, false);

                    if (rdd.wasAborted() ||
                              !getCampaign().applyRetirement(rdd.totalPayout(), rdd.getUnitAssignments())) {
                        for (Person person : people) {
                            getCampaign().getRetirementDefectionTracker().removePayout(person);
                        }
                    } else {
                        for (final Person person : toRemove) {
                            getCampaign().removePerson(person);
                        }
                    }
                } else {
                    String question;
                    if (people.length > 1) {
                        question = resources.getString("confirmRemoveMultiple.text");
                    } else {
                        question = String.format(resources.getString("confirmRemove.format"), people[0].getFullTitle());
                    }
                    if (JOptionPane.YES_OPTION ==
                              JOptionPane.showConfirmDialog(null,
                                    question,
                                    resources.getString("removeQ.text"),
                                    JOptionPane.YES_NO_OPTION)) {
                        for (Person person : people) {
                            getCampaign().removePerson(person);
                        }
                    }
                }
                break;
            }
            case CMD_EMPLOY: {
                for (Person person : people) {
                    getCampaign().recruitPerson(person);
                }

                break;
            }
            case CMD_SPENDING_SPREE: {
                for (Person person : people) {
                    if (!person.getPrisonerStatus().isFreeOrBondsman()) {
                        continue;
                    }

                    if (!person.getStatus().isActive()) {
                        continue;
                    }

                    if (person.getWealth() > MINIMUM_WEALTH) {
                        if (person.isHasPerformedExtremeExpenditure()) {
                            String report = getExpenditureExhaustedReportMessage(person.getHyperlinkedFullTitle());
                            getCampaign().addReport(report);
                            continue;
                        }

                        String report = performExtremeExpenditure(person,
                              getCampaign().getFinances(),
                              getCampaign().getLocalDate());
                        getCampaign().addReport(report);

                        if (!person.isFounder()) {
                            person.performForcedDirectionLoyaltyChange(getCampaign(), false, true, true);
                        }

                        MekHQ.triggerEvent(new PersonChangedEvent(person));
                    }
                }
                break;
            }
            case CMD_CLAIM_BOUNTY: {
                String question = resources.getString("bloodmark.confirmation");

                if (JOptionPane.NO_OPTION ==
                          JOptionPane.showConfirmDialog(null,
                                question,
                                resources.getString("bloodmark.claimBounty"),
                                JOptionPane.YES_NO_OPTION)) {
                    return;
                }

                LocalDate today = getCampaign().getLocalDate();
                boolean validBounty = false;
                for (Person person : people) {
                    if (person.getStatus().isDead()) {
                        continue;
                    }

                    int level = person.getBloodmark();
                    if (level <= BloodmarkLevel.BLOODMARK_ZERO.getLevel()) {
                        continue;
                    }

                    BloodmarkLevel bloodmark = BloodmarkLevel.parseBloodmarkLevelFromInt(level);
                    Money bounty = bloodmark.getBounty();
                    String bountyReport = String.format(resources.getString("bloodmark.transaction"),
                          person.getFullName());
                    getCampaign().getFinances().credit(TransactionType.RANSOM, today, bounty, bountyReport);
                    person.changeStatus(getCampaign(), today, PersonnelStatus.HOMICIDE);
                    validBounty = true;
                }

                if (validBounty) {
                    performMassForcedDirectionLoyaltyChange(getCampaign(), false, false);
                }
                break;
            }
            case CMD_FAMILY_TREE: {
                new FamilyTreeDialog(gui.getFrame(), selectedPerson.getGenealogy(), getCampaign().getPersonnel());
                break;
            }
            case CMD_EDIT: {
                for (Person person : people) {
                    CustomizePersonDialog npd = new CustomizePersonDialog(getFrame(), true, person, getCampaign());
                    npd.setVisible(true);
                    getCampaign().personUpdated(selectedPerson);
                }
                break;
            }
            case CMD_EDIT_HITS: {
                EditPersonnelHitsDialog editPersonnelHitsDialog = new EditPersonnelHitsDialog(getFrame(),
                      true,
                      selectedPerson);
                editPersonnelHitsDialog.setVisible(true);
                if (0 == selectedPerson.getHits()) {
                    selectedPerson.setDoctorId(null, getCampaignOptions().getNaturalHealingWaitingPeriod());
                }
                getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_EDIT_PORTRAIT: {
                final PortraitChooserDialog portraitDialog = new PortraitChooserDialog(getFrame(),
                      selectedPerson.getPortrait());
                if (portraitDialog.showDialog().isConfirmed()) {
                    for (Person person : people) {
                        if (!person.getPortrait().equals(portraitDialog.getSelectedItem())) {
                            person.setPortrait(portraitDialog.getSelectedItem());
                            getCampaign().personUpdated(person);
                        }
                    }
                }
                break;
            }
            case CMD_EDIT_BIOGRAPHY: {
                MarkdownEditorDialog tad = new MarkdownEditorDialog(getFrame(),
                      true,
                      resources.getString("editBiography.text"),
                      selectedPerson.getBiography());
                tad.setVisible(true);
                if (tad.wasChanged()) {
                    selectedPerson.setBiography(tad.getText());
                    MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                }
                break;
            }
            case CMD_ADD_XP: {
                PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                      true,
                      resources.getString("xp.text"),
                      1,
                      0);
                popupValueChoiceDialog.setVisible(true);

                int ia = popupValueChoiceDialog.getValue();
                if (ia <= 0) {
                    // <0 indicates Cancellation
                    // =0 is a No-Op
                    return;
                }

                for (Person person : people) {
                    person.awardXP(getCampaign(), ia);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_SET_XP: {
                PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                      true,
                      resources.getString("xp.text"),
                      selectedPerson.getXP(),
                      0);
                popupValueChoiceDialog.setVisible(true);
                if (popupValueChoiceDialog.getValue() < 0) {
                    return;
                }
                int i = popupValueChoiceDialog.getValue();
                for (Person person : people) {
                    person.setXP(getCampaign(), i);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_ADD_KILL: {
                AddOrEditKillEntryDialog nkd;
                Unit unit = selectedPerson.getUnit();
                if (people.length > 1) {
                    nkd = new AddOrEditKillEntryDialog(getFrame(),
                          true,
                          null,
                          (unit != null) ? unit.getName() : resources.getString("bareHands.text"),
                          getCampaign().getLocalDate(),
                          getCampaign());
                } else {
                    nkd = new AddOrEditKillEntryDialog(getFrame(),
                          true,
                          selectedPerson.getId(),
                          (unit != null) ? unit.getName() : resources.getString("bareHands.text"),
                          getCampaign().getLocalDate(),
                          getCampaign());
                }
                nkd.setVisible(true);
                if (nkd.getKill().isPresent()) {
                    Kill kill = nkd.getKill().get();
                    if (people.length > 1) {
                        for (Person person : people) {
                            Kill k = kill.clone();
                            k.setPilotId(person.getId());
                            getCampaign().addKill(k);
                            MekHQ.triggerEvent(new PersonLogEvent(person));
                        }
                    } else {
                        getCampaign().addKill(kill);
                        MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                    }
                }
                break;
            }
            case CMD_EDIT_KILL_LOG: {
                EditKillLogDialog editKillLogDialog = new EditKillLogDialog(getFrame(),
                      true,
                      getCampaign(),
                      selectedPerson);
                editKillLogDialog.setVisible(true);
                MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                break;
            }
            case CMD_EDIT_PERSONNEL_LOG: {
                EditLogDialog editLogDialog = new EditLogDialog(getFrame(),
                      getCampaign().getLocalDate(),
                      selectedPerson,
                      LogType.PERSONAL_LOG);
                editLogDialog.setVisible(true);
                MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                break;
            }
            case CMD_ADD_LOG_ENTRY: {
                final AddOrEditLogEntryDialog addLogDialog = new AddOrEditLogEntryDialog(getFrame(),
                      null,
                      getCampaign().getLocalDate());
                if (addLogDialog.showDialog().isConfirmed()) {
                    for (Person person : people) {
                        person.addPersonalLogEntry(addLogDialog.getEntry().clone());
                        MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                    }
                }
                break;
            }
            case CMD_ADD_MEDICAL_LOG_ENTRY: {
                final AddOrEditLogEntryDialog addLogDialog = new AddOrEditLogEntryDialog(getFrame(),
                      null,
                      getCampaign().getLocalDate());
                if (addLogDialog.showDialog().isConfirmed()) {
                    for (Person person : people) {
                        person.addMedicalLogEntry(addLogDialog.getEntry().clone());
                        MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                    }
                }
                break;
            }
            case CMD_EDIT_MEDICAL_LOG: {
                EditLogDialog editLogDialog = new EditLogDialog(getFrame(),
                      getCampaign().getLocalDate(),
                      selectedPerson,
                      LogType.MEDICAL_LOG);
                editLogDialog.setVisible(true);
                MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                break;
            }
            case CMD_ADD_ASSIGNMENT_LOG_ENTRY: {
                final AddOrEditLogEntryDialog addLogDialog = new AddOrEditLogEntryDialog(getFrame(),
                      null,
                      getCampaign().getLocalDate());
                if (addLogDialog.showDialog().isConfirmed()) {
                    for (Person person : people) {
                        person.addAssignmentLogEntry(addLogDialog.getEntry().clone());
                        MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                    }
                }
                break;
            }
            case CMD_EDIT_ASSIGNMENT_LOG: {
                EditLogDialog editLogDialog = new EditLogDialog(getFrame(),
                      getCampaign().getLocalDate(),
                      selectedPerson,
                      LogType.ASSIGNMENT_LOG);
                editLogDialog.setVisible(true);
                MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                break;
            }
            case CMD_ADD_PERFORMANCE_LOG_ENTRY: {
                final AddOrEditLogEntryDialog addLogDialog = new AddOrEditLogEntryDialog(getFrame(),
                      null,
                      getCampaign().getLocalDate());
                if (addLogDialog.showDialog().isConfirmed()) {
                    for (Person person : people) {
                        person.addPerformanceLogEntry(addLogDialog.getEntry().clone());
                        MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                    }
                }
                break;
            }
            case CMD_EDIT_PERFORMANCE_LOG: {
                EditLogDialog editLogDialog = new EditLogDialog(getFrame(),
                      getCampaign().getLocalDate(),
                      selectedPerson,
                      LogType.PERFORMANCE_LOG);
                editLogDialog.setVisible(true);
                MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                break;
            }
            case CMD_EDIT_SCENARIO_LOG: {
                EditScenarioLogDialog editScenarioLogDialog = new EditScenarioLogDialog(getFrame(),
                      true,
                      getCampaign(),
                      selectedPerson);
                editScenarioLogDialog.setVisible(true);
                MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                break;
            }
            case CMD_ADD_SCENARIO_ENTRY: {
                AddOrEditScenarioEntryDialog addScenarioDialog = new AddOrEditScenarioEntryDialog(getFrame(),
                      true,
                      getCampaign().getLocalDate());
                addScenarioDialog.setVisible(true);
                Optional<LogEntry> scenarioEntry = addScenarioDialog.getEntry();
                if (scenarioEntry.isPresent()) {
                    for (Person person : people) {
                        person.addScenarioLogEntry(scenarioEntry.get().clone());
                        MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                    }
                }
                break;
            }
            case CMD_CALLSIGN: {
                String s = (String) JOptionPane.showInputDialog(getFrame(),
                      resources.getString("enterNewCallsign.text"),
                      resources.getString("editCallsign.text"),
                      JOptionPane.PLAIN_MESSAGE,
                      null,
                      null,
                      selectedPerson.getCallsign());
                if (null != s) {
                    selectedPerson.setCallsign(s);
                    getCampaign().personUpdated(selectedPerson);
                }
                break;
            }
            case CMD_CLEAR_INJURIES: {
                for (Person person : people) {
                    person.clearInjuries();
                    Unit u = person.getUnit();
                    if (null != u) {
                        u.resetPilotAndEntity();
                    }
                }
                break;
            }
            case CMD_REMOVE_INJURY: {
                String sel = data[1];
                Injury toRemove = null;
                for (Injury i : selectedPerson.getInjuries()) {
                    if (i.getUUID().toString().equals(sel)) {
                        toRemove = i;
                        break;
                    }
                }
                if (toRemove != null) {
                    selectedPerson.removeInjury(toRemove);
                }
                Unit u = selectedPerson.getUnit();
                if (null != u) {
                    u.resetPilotAndEntity();
                }
                break;
            }
            case CMD_REPLACE_MISSING_LIMB: {
                replaceLimb(data[1], selectedPerson, getCampaign());
                break;
            }
            case CMD_EDIT_INJURIES: {
                EditPersonnelInjuriesDialog editPersonnelInjuriesDialog = new EditPersonnelInjuriesDialog(getFrame(),
                      true,
                      getCampaign(),
                      selectedPerson);
                editPersonnelInjuriesDialog.setVisible(true);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                break;
            }
            case CMD_ADD_RANDOM_INJURY: {
                for (Person person : people) {
                    InjuryUtil.resolveCombatDamage(getCampaign(), person, 1);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_ADD_RANDOM_INJURIES: {
                for (Person person : people) {
                    // We want an injury count between 1 and 5 (inclusive), so use randomInt instead of d6.
                    // At 6 injuries, the character should be dead, and we don't want to kill anyone.
                    InjuryUtil.resolveCombatDamage(getCampaign(), person, randomInt(5) + 1);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_EDIT_SALARY: {
                int originalSalary = selectedPerson.getSalary(getCampaign()).getAmount().intValue();

                PopupValueChoiceDialog salaryDialog = new PopupValueChoiceDialog(getFrame(),
                      true,
                      resources.getString("changeSalary.text"),
                      MathUtility.clamp(originalSalary, -1, 1000000000),
                      -1,
                      1000000000);

                salaryDialog.setVisible(true);

                int newSalary = salaryDialog.getValue();

                if (newSalary < -1) {
                    return;
                }

                for (Person person : people) {
                    person.setSalary(Money.of(newSalary));
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }

                break;
            }
            case CMD_GIVE_PAYMENT: {
                PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                      true,
                      resources.getString("givePayment.title"),
                      1000,
                      1,
                      1000000);
                popupValueChoiceDialog.setVisible(true);

                int payment = popupValueChoiceDialog.getValue();
                if (payment <= 0) {
                    // <0 indicates Cancellation
                    // =0 is a No-Op
                    return;
                }

                // pay person & add expense
                Map<Person, Money> personMoneyMap = new HashMap<>();
                personMoneyMap.put(selectedPerson, Money.of(payment));
                getCampaign().payPersonnel(TransactionType.MISCELLANEOUS,
                      Money.of(payment),
                      String.format(resources.getString("givePayment.format"), selectedPerson.getFullName()),
                      personMoneyMap);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));


                break;
            }
            case CMD_LOYALTY: {
                for (Person person : people) {
                    person.setLoyalty(d6(3));
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_PERSONALITY: {
                for (Person person : people) {
                    PersonalityController.generatePersonality(person);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_ADD_RANDOM_ABILITY: {
                SingleSpecialAbilityGenerator singleSpecialAbilityGenerator = new SingleSpecialAbilityGenerator();
                for (Person person : people) {
                    singleSpecialAbilityGenerator.rollSPA(getCampaign(), person);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_GENERATE_ROLEPLAY_SKILLS: {
                RandomSkillPreferences skillPreferences = getCampaign().getRandomSkillPreferences();
                AbstractSkillGenerator skillGenerator = new DefaultSkillGenerator(skillPreferences);
                for (Person person : people) {
                    skillGenerator.generateRoleplaySkills(person);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_REMOVE_ROLEPLAY_SKILLS: {
                for (Person person : people) {
                    person.removeAllSkillsOfSubType(SkillSubType.ROLEPLAY_GENERAL);
                    person.removeAllSkillsOfSubType(SkillSubType.ROLEPLAY_ART);
                    person.removeAllSkillsOfSubType(SkillSubType.ROLEPLAY_INTEREST);
                    person.removeAllSkillsOfSubType(SkillSubType.ROLEPLAY_SCIENCE);
                    person.removeAllSkillsOfSubType(SkillSubType.ROLEPLAY_SECURITY);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_GENERATE_ROLEPLAY_ATTRIBUTES: {
                RandomSkillPreferences skillPreferences = getCampaign().getRandomSkillPreferences();
                AbstractSkillGenerator skillGenerator = new DefaultSkillGenerator(skillPreferences);
                for (Person person : people) {
                    skillGenerator.generateAttributes(person, getCampaign().getCampaignOptions().isUseEdge());
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_GENERATE_ROLEPLAY_TRAITS: {
                RandomSkillPreferences skillPreferences = getCampaign().getRandomSkillPreferences();
                AbstractSkillGenerator skillGenerator = new DefaultSkillGenerator(skillPreferences);
                for (Person person : people) {
                    skillGenerator.generateTraits(person);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }

            // region Randomization Menu
            case CMD_RANDOM_NAME: {
                for (final Person person : people) {
                    final String[] name = RandomNameGenerator.getInstance()
                                                .generateGivenNameSurnameSplit(person.getGender(),
                                                      person.isClanPersonnel(),
                                                      person.getOriginFaction().getShortName());
                    person.setGivenName(name[0]);
                    person.setSurname(name[1]);
                    writePersonalityDescription(person);
                    writeInterviewersNotes(person);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_RANDOM_BLOODNAME: {
                final boolean ignoreDice = (data.length > 1) && Boolean.parseBoolean(data[1]);
                for (final Person person : people) {
                    getCampaign().checkBloodnameAdd(person, ignoreDice);
                }
                break;
            }
            case CMD_RANDOM_CALLSIGN: {
                for (final Person person : people) {
                    person.setCallsign(RandomCallsignGenerator.getInstance().generate());
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_RANDOM_PORTRAIT: {
                for (final Person person : people) {
                    getCampaign().assignRandomPortraitFor(person);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_RANDOM_ORIGIN: {
                for (final Person person : people) {
                    getCampaign().assignRandomOriginFor(person);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_RANDOM_ORIGIN_FACTION: {
                for (final Person person : people) {
                    final Faction faction = getCampaign().getFactionSelector().selectFaction(getCampaign());
                    if (faction != null) {
                        person.setOriginFaction(faction);
                        MekHQ.triggerEvent(new PersonChangedEvent(person));
                    }
                }
                break;
            }
            case CMD_RANDOM_ORIGIN_PLANET: {
                for (final Person person : people) {
                    final Planet planet = getCampaign().getPlanetSelector()
                                                .selectPlanet(getCampaign(), person.getOriginFaction());
                    if (planet != null) {
                        person.setOriginPlanet(planet);
                        MekHQ.triggerEvent(new PersonChangedEvent(person));
                    }
                }
                break;
            }
            case CMD_ORIGINAL_TO_CURRENT:
                for (final Person person : people) {
                    Unit unit = person.getUnit();

                    if (unit != null) {
                        person.setOriginalUnit(unit);
                    }
                }
                break;
            case CMD_WIPE_ORIGINAL:
                for (final Person person : people) {
                    if (person.getOriginalUnitId() != null) {
                        person.setOriginalUnitId(null);
                        person.setOriginalUnitTech(TECH_IS1);
                        person.setOriginalUnitWeight(EntityWeightClass.WEIGHT_ULTRA_LIGHT);
                    }
                }
                break;
            // endregion Randomization Menu

            default: {
                break;
            }
        }
    }

    /**
     * Handles the limb replacement procedure for the selected person. This method determines the suitable doctors,
     * calculates the cost of the procedure, and processes the surgery if the user accepts it.
     *
     * <p>If no doctors are available with sufficient skill levels, the cost of the operation
     * is significantly increased. Once the surgery is performed, the person's injury is updated, and associated
     * financial and unit adjustments are made.</p>
     *
     * @param selectedInjury The {@link UUID} of the injury being fixed.
     * @param selectedPerson The {@link Person} undergoing the limb replacement procedure.
     * @param campaign       The {@link Campaign} instance representing the current campaign, containing active
     *                       personnel, finances, and other relevant details.
     */
    private void replaceLimb(String selectedInjury, Person selectedPerson, Campaign campaign) {
        List<Person> suitableDoctors = new ArrayList<>();

        for (Person person : campaign.getActivePersonnel(false, false)) {
            if (person.isDoctor()) {
                SkillModifierData skillModifierData = person.getSkillModifierData();
                Skill skill = person.getSkill(S_SURGERY);

                if (skill != null &&
                          skill.getTotalSkillLevel(skillModifierData) >=
                                REPLACEMENT_LIMB_MINIMUM_SKILL_REQUIRED_TYPES_3_4_5) {
                    suitableDoctors.add(person);
                }
            }
        }

        for (Injury injury : selectedPerson.getInjuries()) {
            if (injury.getUUID().toString().equals(selectedInjury)) {
                BodyLocation location = injury.getLocation();

                Money cost = switch (location) {
                    case RIGHT_ARM, LEFT_ARM -> REPLACEMENT_LIMB_COST_ARM_TYPE_5;
                    case RIGHT_HAND, LEFT_HAND -> REPLACEMENT_LIMB_COST_HAND_TYPE_5;
                    case RIGHT_LEG, LEFT_LEG -> REPLACEMENT_LIMB_COST_LEG_TYPE_5;
                    case RIGHT_FOOT, LEFT_FOOT -> REPLACEMENT_LIMB_COST_FOOT_TYPE_5;
                    default -> Money.zero();
                };

                // Failsafe for if we have a missing location that hasn't been accounted for
                if (Objects.equals(cost, Money.zero())) {
                    return;
                }

                if (suitableDoctors.isEmpty()) {
                    cost = cost.multipliedBy(10);
                }

                ReplacementLimbDialog replacementLimbDialog = new ReplacementLimbDialog(campaign,
                      suitableDoctors,
                      selectedPerson,
                      cost);
                int choice = replacementLimbDialog.getChoiceIndex();

                // If the user chose to decline the surgery
                if (choice == 0) {
                    return;
                }

                campaign.getFinances()
                      .debit(MEDICAL_EXPENSES,
                            campaign.getLocalDate(),
                            cost,
                            String.format(resources.getString("replaceMissingLimb.surgery"),
                                  selectedPerson.getFullTitle()));

                int hitCount = injury.getHits();
                Injury newInjury = REPLACEMENT_LIMB_RECOVERY.newInjury(campaign, selectedPerson, location, hitCount);
                newInjury.setWorkedOn(true);

                selectedPerson.removeInjury(injury);
                selectedPerson.addInjury(newInjury);
                break;
            }
        }

        Unit unit = selectedPerson.getUnit();
        if (unit != null) {
            unit.resetPilotAndEntity();
        }
    }

    /**
     * Private method to process a list of applications to education academies.
     *
     * @param people         an array of Person objects representing the applicants
     * @param data           an array of String objects representing the command data
     * @param isReEnrollment a boolean indicating if the application is for re-enrollment
     */
    private void processApplication(Person[] people, String[] data, boolean isReEnrollment) {
        boolean applicationFailed = false;

        for (Person person : people) {
            if (makeEnrollmentCheck(getCampaign(), person, data[1], data[2])) {
                EducationController.performEducationPreEnrollmentActions(getCampaign(),
                      person,
                      data[1],
                      data[2],
                      MathUtility.parseInt(data[3]),
                      data[4],
                      data[5],
                      isReEnrollment);
            } else {
                applicationFailed = true;
            }
        }

        if (applicationFailed) {
            JOptionPane.showMessageDialog(null,
                  wordWrap(resources.getString("eduFailedApplication.text")),
                  resources.getString("eduFailedApplication.title"),
                  JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Processes a prisoner resolution command, allowing a user to confirm a specific action (e.g., releasing or
     * executing prisoners) via a dialog box. If confirmed, this method removes the selected prisoners from the campaign
     * and optionally triggers additional execution-specific logic.
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *   <li>Displays a confirmation dialog with a message and title for the given prisoners.</li>
     *   <li>If the user confirms, the specified prisoners are removed from the campaign.</li>
     *   <li>Handles single prisoners by showing their full title in the dialog, while grouping
     *   multiple prisoners by count.</li>
     *   <li>If the action involves execution, an additional execution-handling method is called,
     *   which processes execution-specific logic.</li>
     * </ul>
     *
     * @param prisoners   an array of {@link Person} objects representing the prisoners to process. If the array
     *                    contains only one prisoner, their full title is displayed; otherwise, the count of prisoners
     *                    is shown.
     * @param message     a {@link String} key representing the resource for the dialog message. This resource key
     *                    should be able to handle placeholders for details, such as prisoner count or name.
     * @param title       a {@link String} key representing the resource for the dialog title.
     * @param isExecution a {@code boolean} indicating whether the command is related to execution. If {@code true},
     *                    additional logic is executed to handle executions.
     */
    private void processPrisonerResolutionCommand(Person[] prisoners, String message, String title,
          boolean isExecution) {
        String label;

        if (prisoners.length == 1) {
            label = prisoners[0].getFullTitle();
        } else {
            label = String.format(resources.getString("numPrisoners.text"), prisoners.length);
        }

        if (0 ==
                  JOptionPane.showConfirmDialog(null,
                        String.format(resources.getString(message), label),
                        resources.getString(title),
                        JOptionPane.YES_NO_OPTION)) {
            for (Person prisoner : prisoners) {
                getCampaign().removePerson(prisoner);
            }
        }

        if (isExecution) {
            if (getCampaign().getCampaignOptions().isTrackFactionStanding()) {
                FactionStandings factionStandings = getCampaign().getFactionStandings();

                List<Person> listOfPrisoners = Arrays.asList(prisoners);
                List<String> reports =
                      factionStandings.executePrisonersOfWar(getCampaign().getFaction().getShortName(), listOfPrisoners,
                            getCampaign().getGameYear(), getCampaign().getCampaignOptions().getRegardMultiplier());

                for (String report : reports) {
                    getCampaign().addReport(report);
                }
            }

            processAdHocExecution(getCampaign(), prisoners.length);
        }
    }

    private void loadGMToolsForPerson(Person person) {
        GMToolsDialog gmToolsDialog = new GMToolsDialog(getFrame(), gui, person);
        gmToolsDialog.setVisible(true);
        getCampaign().personUpdated(person);
    }

    private Person[] getSelectedPeople() {
        Person[] selected = new Person[personnelTable.getSelectedRowCount()];
        int[] rows = personnelTable.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            Person person = personnelModel.getPerson(personnelTable.convertRowIndexToModel(rows[i]));
            selected[i] = person;
        }
        return selected;
    }

    @Override
    protected Optional<JPopupMenu> createPopupMenu() {
        if (personnelTable.getSelectedRowCount() == 0) {
            return Optional.empty();
        }

        JPopupMenu popup = new JPopupMenu();

        int row = personnelTable.getSelectedRow();
        boolean oneSelected = personnelTable.getSelectedRowCount() == 1;
        Person person = personnelModel.getPerson(personnelTable.convertRowIndexToModel(row));
        JMenuItem menuItem;
        JMenu menu;
        JMenu submenu;
        JCheckBoxMenuItem cbMenuItem;
        Person[] selected = getSelectedPeople();

        // lets fill the pop up menu
        menuItem = new JMenuItem(resources.getString("makeSkillCheck.text"));
        menuItem.setActionCommand(makeCommand(CMD_SKILL_CHECK));
        menuItem.addActionListener(this);
        popup.add(menuItem);

        menuItem = new JMenuItem(resources.getString("makeAttributeCheck.text"));
        menuItem.setActionCommand(makeCommand(CMD_ATTRIBUTE_CHECK));
        menuItem.addActionListener(this);
        popup.add(menuItem);

        if (getCampaignOptions().isUseAdvancedMedical() && oneSelected) {
            menuItem = new JMenuItem(resources.getString("viewMedicalRecords.text"));
            menuItem.setActionCommand(makeCommand(CMD_MEDICAL_RECORDS));
            menuItem.addActionListener(this);
            popup.add(menuItem);
        }

        if (StaticChecks.areAllEligible(true, selected)) {
            menu = new JMenu(resources.getString("changeRank.text"));
            final Profession initialProfession = Profession.getProfessionFromPersonnelRole(person.getPrimaryRole());
            for (final RankDisplay rankDisplay : RankDisplay.getRankDisplaysForSystem(person.getRankSystem(),
                  initialProfession)) {
                final Rank rank = person.getRankSystem().getRank(rankDisplay.rankNumeric());
                final Profession profession = initialProfession.getProfession(person.getRankSystem(), rank);
                final int rankLevels = rank.getRankLevels().get(profession);

                if (rankLevels > 1) {
                    submenu = new JMenu(rankDisplay.toString());
                    for (int level = 0; level <= rankLevels; level++) {
                        cbMenuItem = new JCheckBoxMenuItem(rank.getName(profession) +
                                                                 Utilities.getRomanNumeralsFromArabicNumber(level,
                                                                       true));
                        cbMenuItem.setSelected((person.getRankNumeric() == rankDisplay.rankNumeric()) &&
                                                     (person.getRankLevel() == level));
                        cbMenuItem.setActionCommand(makeCommand(CMD_RANK,
                              String.valueOf(rankDisplay.rankNumeric()),
                              String.valueOf(level)));
                        cbMenuItem.addActionListener(this);
                        submenu.add(cbMenuItem);
                    }
                    JMenuHelpers.addMenuIfNonEmpty(menu, submenu);
                } else {
                    cbMenuItem = new JCheckBoxMenuItem(rankDisplay.toString());
                    cbMenuItem.setSelected(person.getRankNumeric() == rankDisplay.rankNumeric());
                    cbMenuItem.setActionCommand(makeCommand(CMD_RANK, String.valueOf(rankDisplay.rankNumeric())));
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);
                }
            }
            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        }

        menu = new JMenu(resources.getString("changeRankSystem.text"));
        final RankSystem campaignRankSystem = getCampaign().getRankSystem();
        // First allow them to revert to the campaign system
        cbMenuItem = new JCheckBoxMenuItem(resources.getString("useCampaignRankSystem.text"));
        cbMenuItem.setSelected(campaignRankSystem.equals(person.getRankSystem()));
        cbMenuItem.setActionCommand(makeCommand(CMD_RANK_SYSTEM, campaignRankSystem.getCode()));
        cbMenuItem.addActionListener(this);
        menu.add(cbMenuItem);

        final List<RankSystem> rankSystems = new ArrayList<>(Ranks.getRankSystems().values());
        final NaturalOrderComparator naturalOrderComparator = new NaturalOrderComparator();
        rankSystems.sort((r1, r2) -> naturalOrderComparator.compare(r1.toString(), r2.toString()));
        for (final RankSystem rankSystem : rankSystems) {
            if (rankSystem.equals(campaignRankSystem)) {
                continue;
            }
            cbMenuItem = new JCheckBoxMenuItem(rankSystem.toString());
            cbMenuItem.setSelected(rankSystem.equals(person.getRankSystem()));
            cbMenuItem.setActionCommand(makeCommand(CMD_RANK_SYSTEM, rankSystem.getCode()));
            cbMenuItem.addActionListener(this);
            menu.add(cbMenuItem);
        }
        JMenuHelpers.addMenuIfNonEmpty(popup, menu);

        if (Stream.of(selected).allMatch(p -> p.getRankSystem().isUseManeiDomini())) {
            // MD Classes
            menu = new JMenu(resources.getString("changeMDClass.text"));
            for (ManeiDominiClass maneiDominiClass : ManeiDominiClass.values()) {
                cbMenuItem = new JCheckBoxMenuItem(maneiDominiClass.toString());
                cbMenuItem.setActionCommand(makeCommand(CMD_MANEI_DOMINI_CLASS, maneiDominiClass.name()));
                cbMenuItem.addActionListener(this);
                if (maneiDominiClass == person.getManeiDominiClass()) {
                    cbMenuItem.setSelected(true);
                }
                menu.add(cbMenuItem);
            }
            JMenuHelpers.addMenuIfNonEmpty(popup, menu);

            // MD Ranks
            menu = new JMenu(resources.getString("changeMDRank.text"));
            for (ManeiDominiRank maneiDominiRank : ManeiDominiRank.values()) {
                cbMenuItem = new JCheckBoxMenuItem(maneiDominiRank.toString());
                cbMenuItem.setActionCommand(makeCommand(CMD_MANEI_DOMINI_RANK, maneiDominiRank.name()));
                cbMenuItem.addActionListener(this);
                if (person.getManeiDominiRank() == maneiDominiRank) {
                    cbMenuItem.setSelected(true);
                }
                menu.add(cbMenuItem);
            }
            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        }

        if (Stream.of(selected).allMatch(p -> p.getRankSystem().isUseROMDesignation())) {
            menu = new JMenu(resources.getString("changePrimaryDesignation.text"));
            for (ROMDesignation romDesignation : ROMDesignation.values()) {
                cbMenuItem = new JCheckBoxMenuItem(romDesignation.toString());
                cbMenuItem.setActionCommand(makeCommand(CMD_PRIMARY_DESIGNATOR, romDesignation.name()));
                cbMenuItem.addActionListener(this);
                if (romDesignation == person.getPrimaryDesignator()) {
                    cbMenuItem.setSelected(true);
                }
                menu.add(cbMenuItem);
            }
            JMenuHelpers.addMenuIfNonEmpty(popup, menu);

            menu = new JMenu(resources.getString("changeSecondaryDesignation.text"));
            for (ROMDesignation romDesignation : ROMDesignation.values()) {
                cbMenuItem = new JCheckBoxMenuItem(romDesignation.toString());
                cbMenuItem.setActionCommand(makeCommand(CMD_SECONDARY_DESIGNATOR, romDesignation.name()));
                cbMenuItem.addActionListener(this);
                if (romDesignation == person.getSecondaryDesignator()) {
                    cbMenuItem.setSelected(true);
                }
                menu.add(cbMenuItem);
            }
            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        }

        menu = new JMenu(resources.getString("changeStatus.text"));
        boolean areAllFree = Stream.of(selected).allMatch(p -> p.getPrisonerStatus().isFreeOrBondsman());
        for (final PersonnelStatus status : PersonnelStatus.getImplementedStatuses(areAllFree, false)) {
            cbMenuItem = new JCheckBoxMenuItem(status.toString());
            cbMenuItem.setToolTipText(status.getToolTipText());
            cbMenuItem.setSelected(person.getStatus() == status);
            cbMenuItem.setActionCommand(makeCommand(CMD_CHANGE_STATUS, status.name()));
            cbMenuItem.addActionListener(this);
            menu.add(cbMenuItem);
        }

        JMenu cbMenu = new JMenu(resources.getString("changeStatus.causesOfDeath.text"));
        for (final PersonnelStatus status : PersonnelStatus.getCauseOfDeathStatuses(areAllFree)) {
            cbMenuItem = new JCheckBoxMenuItem(status.toString());
            cbMenuItem.setToolTipText(status.getToolTipText());
            cbMenuItem.setSelected(person.getStatus() == status);
            cbMenuItem.setActionCommand(makeCommand(CMD_CHANGE_STATUS, status.name()));
            cbMenuItem.addActionListener(this);
            cbMenu.add(cbMenuItem);
        }

        menu.add(cbMenu);
        popup.add(menu);

        if (!StaticChecks.areAnyFree(selected)) {
            if (getCampaign().getLocation().isOnPlanet()) {
                popup.add(newMenuItem(resources.getString("free.text"), CMD_FREE));
                popup.add(newMenuItem(resources.getString("execute.text"), CMD_EXECUTE));
            } else {
                popup.add(newMenuItem(resources.getString("jettison.text"), CMD_JETTISON));
            }

            if (StaticChecks.areAnyWillingToDefect(selected)) {
                popup.add(newMenuItem(resources.getString("recruit.text"), CMD_RECRUIT));
            }

            if ((getCampaign().isClanCampaign()) && (StaticChecks.areAnyBondsmen(selected))) {
                popup.add(newMenuItem(resources.getString("abtakha.text"), CMD_ABTAKHA));
            }
        }

        if ((oneSelected) && (!person.isChild(getCampaign().getLocalDate()))) {
            List<Person> orphans = getCampaign().getActivePersonnel(true, true)
                                         .stream()
                                         .filter(child -> (child.isChild(getCampaign().getLocalDate())) &&
                                                                (!child.getGenealogy().hasLivingParents()))
                                         .toList();

            if (!orphans.isEmpty()) {
                JMenu orphanMenu = new JMenu(resources.getString("adopt.text"));

                for (final Person orphan : orphans) {
                    String status = getPersonOptionString(orphan);

                    JMenuItem orphanItem = new JMenuItem(status);
                    orphanItem.setActionCommand(makeCommand(CMD_ADOPTION, String.valueOf(orphan.getId())));
                    orphanItem.addActionListener(this);
                    orphanMenu.add(orphanItem);
                }

                JMenuHelpers.addMenuIfNonEmpty(popup, orphanMenu);
            }
        }

        final PersonnelRole[] roles = PersonnelRole.values();

        menu = new JMenu(resources.getString("changePrimaryRole.text"));
        JMenu menuCombatPrimary = new JMenu(resources.getString("changeRole.combat"));
        JMenu menuSupportPrimary = new JMenu(resources.getString("changeRole.support"));
        JMenu menuCivilianPrimary = new JMenu(resources.getString("changeRole.civilian"));


        for (final PersonnelRole role : roles) {
            boolean allCanPerform = true;

            for (Person selectedPerson : getSelectedPeople()) {
                if (!selectedPerson.canPerformRole(getCampaign().getLocalDate(), role, true)) {
                    allCanPerform = false;
                    break;
                }
            }

            if (allCanPerform) {
                cbMenuItem = new JCheckBoxMenuItem(role.getLabel(getCampaign().isClanCampaign()));
                cbMenuItem.setToolTipText(wordWrap(role.getTooltip(getCampaign().isClanCampaign()), 50));
                cbMenuItem.setActionCommand(makeCommand(CMD_PRIMARY_ROLE, role.name()));
                cbMenuItem.addActionListener(this);
                if (oneSelected && role == person.getPrimaryRole()) {
                    cbMenuItem.setSelected(true);
                }

                if (role.isCombat()) {
                    menuCombatPrimary.add(cbMenuItem);
                } else if (role.isSupport(true)) {
                    menuSupportPrimary.add(cbMenuItem);
                } else {
                    menuCivilianPrimary.add(cbMenuItem);
                }
            }
        }
        if (menuCombatPrimary.getItemCount() > 0) {
            menu.add(menuCombatPrimary);
        }
        if (menuSupportPrimary.getItemCount() > 0) {
            menu.add(menuSupportPrimary);
        }
        if (menuCivilianPrimary.getItemCount() > 0) {
            menu.add(menuCivilianPrimary);
        }

        JMenuHelpers.addMenuIfNonEmpty(popup, menu);

        menu = new JMenu(resources.getString("changeSecondaryRole.text"));
        JMenu menuCombatSecondary = new JMenu(resources.getString("changeRole.combat"));
        JMenu menuSupportSecondary = new JMenu(resources.getString("changeRole.support"));
        JMenu menuCivilianSecondary = new JMenu(resources.getString("changeRole.civilian"));
        for (final PersonnelRole role : roles) {
            boolean allCanPerform = true;

            for (Person selectedPerson : getSelectedPeople()) {
                if (!selectedPerson.canPerformRole(getCampaign().getLocalDate(), role, false)) {
                    allCanPerform = false;
                    break;
                }
            }

            if (allCanPerform) {
                cbMenuItem = new JCheckBoxMenuItem(role.getLabel(getCampaign().isClanCampaign()));
                cbMenuItem.setToolTipText(wordWrap(role.getTooltip(getCampaign().isClanCampaign())));
                cbMenuItem.setActionCommand(makeCommand(CMD_SECONDARY_ROLE, role.name()));
                cbMenuItem.addActionListener(this);
                if (oneSelected && role == person.getSecondaryRole()) {
                    cbMenuItem.setSelected(true);
                }

                if (role.isCombat()) {
                    menuCombatSecondary.add(cbMenuItem);
                } else if (role.isSupport(true)) {
                    menuSupportSecondary.add(cbMenuItem);
                } else {
                    menuCivilianSecondary.add(cbMenuItem);
                }
            }
        }

        if (menuCombatSecondary.getItemCount() > 0) {
            menu.add(menuCombatSecondary);
        }
        if (menuSupportSecondary.getItemCount() > 0) {
            menu.add(menuSupportSecondary);
        }
        if (menuCivilianSecondary.getItemCount() > 0) {
            menu.add(menuCivilianSecondary);
        }

        JMenuHelpers.addMenuIfNonEmpty(popup, menu);

        // change salary
        if (getCampaignOptions().isPayForSalaries() && StaticChecks.areAllActive(selected)) {
            menuItem = new JMenuItem(resources.getString("setSalary.text"));
            menuItem.setActionCommand(CMD_EDIT_SALARY);
            menuItem.addActionListener(this);
            popup.add(menuItem);
        }

        // give C-Bill payment
        if (oneSelected && person.getStatus().isActiveFlexible()) {
            menuItem = new JMenuItem(resources.getString("givePayment.text"));
            menuItem.setActionCommand(CMD_GIVE_PAYMENT);
            menuItem.addActionListener(this);
            popup.add(menuItem);
        }

        if (oneSelected && getCampaignOptions().isUseAdvancedMedical()) {
            List<Injury> missingLimbInjuries = new ArrayList<>();

            for (Injury injury : person.getInjuries()) {
                InjuryType injuryType = injury.getType();
                if (injuryType.impliesMissingLocation() && injury.getType().isPermanent()) {
                    missingLimbInjuries.add(injury);
                }
            }

            if (!missingLimbInjuries.isEmpty()) {
                JMenu subMenu = new JMenu(resources.getString("replaceMissingLimb.text"));

                for (Injury injury : missingLimbInjuries) {
                    menuItem = new JMenuItem(String.format(resources.getString("replaceMissingLimb.format"),
                          injury.getName()));
                    menuItem.setActionCommand(makeCommand(CMD_REPLACE_MISSING_LIMB, injury.getUUID().toString()));
                    menuItem.addActionListener(this);
                    subMenu.add(menuItem);
                }

                popup.add(subMenu);
            }
        }

        JMenuHelpers.addMenuIfNonEmpty(popup, new AssignPersonToUnitMenu(getCampaign(), selected));

        if (oneSelected && person.getStatus().isActiveFlexible()) {
            if (getCampaignOptions().isUseManualMarriages() &&
                      (getCampaign().getMarriage().canMarry(getCampaign().getLocalDate(), person, false) == null)) {
                menu = new JMenu(resources.getString("chooseSpouse.text"));
                JMenu maleMenu = new JMenu(resources.getString("spouseMenuMale.text"));
                JMenu femaleMenu = new JMenu(resources.getString("spouseMenuFemale.text"));
                JMenu spouseMenu;

                LocalDate today = getCampaign().getLocalDate();

                // Get all safe potential spouses sorted by age and then by surname
                final Campaign campaign = getCampaign();
                final AbstractMarriage marriage = campaign.getMarriage();

                final List<Person> personnel = campaign.getPersonnel()
                                                     .stream()
                                                     .filter(potentialSpouse -> marriage.safeSpouse(campaign,
                                                           today,
                                                           person,
                                                           potentialSpouse,
                                                           false))
                                                     .filter(potentialSpouse -> AbstractMarriage.isGenderCompatible(
                                                           person,
                                                           potentialSpouse))
                                                     .sorted(Comparator.comparing((Person p) -> p.getAge(today))
                                                                   .thenComparing(Person::getSurname))
                                                     .toList();

                for (final Person potentialSpouse : personnel) {
                    final String status;
                    final String founder = potentialSpouse.isFounder() ? resources.getString("spouseFounder.text") : "";
                    if (potentialSpouse.getPrisonerStatus().isBondsman()) {
                        status = String.format(resources.getString("marriageBondsmanDesc.format"),
                              potentialSpouse.getFullName(),
                              potentialSpouse.getAge(today),
                              potentialSpouse.getRoleDesc(),
                              founder);
                    } else if (potentialSpouse.getPrisonerStatus().isCurrentPrisoner()) {
                        status = String.format(resources.getString("marriagePrisonerDesc.format"),
                              potentialSpouse.getFullName(),
                              potentialSpouse.getAge(today),
                              potentialSpouse.getRoleDesc(),
                              founder);
                    } else {
                        status = String.format(resources.getString("marriagePartnerDesc.format"),
                              potentialSpouse.getFullName(),
                              potentialSpouse.getAge(today),
                              potentialSpouse.getRoleDesc(),
                              founder);
                    }

                    spouseMenu = new JMenu(status);

                    for (final MergingSurnameStyle style : MergingSurnameStyle.values()) {
                        spouseMenu.add(newMenuItem(style.getDropDownText(),
                              makeCommand(CMD_ADD_SPOUSE, potentialSpouse.getId().toString(), style.name())));
                    }

                    if (potentialSpouse.getGender().isMale()) {
                        maleMenu.add(spouseMenu);
                    } else {
                        femaleMenu.add(spouseMenu);
                    }
                }

                if (person.getGender().isMale()) {
                    JMenuHelpers.addMenuIfNonEmpty(menu, femaleMenu);
                    JMenuHelpers.addMenuIfNonEmpty(menu, maleMenu);
                } else {
                    JMenuHelpers.addMenuIfNonEmpty(menu, maleMenu);
                    JMenuHelpers.addMenuIfNonEmpty(menu, femaleMenu);
                }

                JMenuHelpers.addMenuIfNonEmpty(popup, menu);
            }
        }

        if (getCampaignOptions().isUseManualDivorce() &&
                  (Stream.of(selected).anyMatch(p -> getCampaign().getDivorce().canDivorce(person, false) == null))) {
            menu = new JMenu(resources.getString("removeSpouse.text"));

            for (final SplittingSurnameStyle style : SplittingSurnameStyle.values()) {
                JMenuItem divorceMenu = new JMenuItem(style.getDropDownText());
                divorceMenu.setActionCommand(makeCommand(CMD_REMOVE_SPOUSE, style.name()));
                divorceMenu.addActionListener(this);
                menu.add(divorceMenu);
            }

            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        }

        if (oneSelected) {
            menuItem = new JMenuItem(resources.getString("familyTree.text"));
            menuItem.setActionCommand(CMD_FAMILY_TREE);
            menuItem.addActionListener(this);
            popup.add(menuItem);
        }

        // region Awards Menu
        JMenu awardMenu = new JMenu(resources.getString("award.text"));
        List<String> setNames = AwardsFactory.getInstance().getAllSetNames();
        Collections.sort(setNames);

        for (String setName : setNames) {
            if ((setName.equals("standard")) && (getCampaignOptions().isIgnoreStandardSet())) {
                continue;
            }

            // we can't capitalize the set filename without breaking compatibility with
            // older saves,
            // so we have a special handler here.
            String setNameProcessed;
            if ((setName.equals("standard")) && (!setNames.contains("Standard"))) {
                setNameProcessed = "Standard";
            } else {
                setNameProcessed = setName;
            }

            JMenu setAwardMenu = new JMenu(setNameProcessed);

            List<Award> awardsOfSet = AwardsFactory.getInstance().getAllAwardsForSet(setName);
            Collections.sort(awardsOfSet);

            List<String> awardGroups = new ArrayList<>();
            List<String> awardGroupDescriptions = new ArrayList<>();

            for (Award award : awardsOfSet) {
                if ("group".equalsIgnoreCase(award.getItem())) {
                    awardGroups.add(award.getName());
                    awardGroupDescriptions.add(award.getDescription());
                }
            }

            if (awardGroups.isEmpty()) {
                for (Award award : awardsOfSet) {
                    if (oneSelected && !award.canBeAwarded(selected)) {
                        continue;
                    }

                    menuItem = getAwardMenuItem(award);
                    setAwardMenu.add(menuItem);
                }
            } else {
                for (int index = 0; index < awardGroups.size(); index++) {
                    JMenu awardGroupMenu = new JMenu(awardGroups.get(index));
                    awardGroupMenu.setToolTipText(MultiLineTooltip.splitToolTip(awardGroupDescriptions.get(index)));
                    setAwardMenu.add(awardGroupMenu);

                    for (Award award : awardsOfSet) {
                        if (oneSelected && !award.canBeAwarded(selected)) {
                            continue;
                        } else if (award.getItem().equalsIgnoreCase("group")) {
                            continue;
                        }

                        if (award.getGroup().equalsIgnoreCase(awardGroups.get(index))) {
                            menuItem = getAwardMenuItem(award);
                            awardGroupMenu.add(menuItem);
                        } else if ((!awardGroups.contains(award.getGroup())) && (index == 0)) {
                            menuItem = getAwardMenuItem(award);
                            awardMenu.add(menuItem);
                        }
                    }
                }
            }

            JMenuHelpers.addMenuIfNonEmpty(awardMenu, setAwardMenu);
        }

        if (StaticChecks.doAnyHaveAnAward(selected)) {
            if (awardMenu.getItemCount() > 0) {
                awardMenu.addSeparator();
            }

            JMenu removeAwardMenu = new JMenu(resources.getString("removeAward.text"));

            if (oneSelected) {
                for (Award award : person.getAwardController().getAwards()) {
                    JMenu singleAwardMenu = new JMenu(award.getName());
                    for (String date : award.getFormattedDates()) {
                        JMenuItem specificAwardMenu = new JMenuItem(date);
                        specificAwardMenu.setActionCommand(makeCommand(CMD_RMV_AWARD,
                              award.getSet(),
                              award.getName(),
                              date));
                        specificAwardMenu.addActionListener(this);
                        singleAwardMenu.add(specificAwardMenu);
                    }
                    JMenuHelpers.addMenuIfNonEmpty(removeAwardMenu, singleAwardMenu);
                }
            } else {
                Set<Award> awards = new TreeSet<>((a1, a2) -> {
                    if (a1.getSet().equalsIgnoreCase(a2.getSet())) {
                        return a1.getName().compareToIgnoreCase(a2.getName());
                    } else {
                        return a1.getSet().compareToIgnoreCase(a2.getSet());
                    }
                });
                for (Person p : selected) {
                    awards.addAll(p.getAwardController().getAwards());
                }

                for (Award award : awards) {
                    JMenuItem singleAwardMenu = new JMenuItem(award.getName());
                    singleAwardMenu.setActionCommand(makeCommand(CMD_RMV_AWARD, award.getSet(), award.getName()));
                    singleAwardMenu.addActionListener(this);
                    removeAwardMenu.add(singleAwardMenu);
                }
            }
            JMenuHelpers.addMenuIfNonEmpty(awardMenu, removeAwardMenu);
        }
        popup.add(awardMenu);
        // endregion Awards Menu

        // region Education Menu
        if (getCampaignOptions().isUseEducationModule()) {
            JMenu academyMenu = new JMenu(resources.getString("eduEducation.text"));

            // we use 'campaign' a lot here, so let's store it, so we don't have to re-call
            // it every time
            Campaign campaign = getCampaign();

            if (StaticChecks.areAllActiveFlexible(selected)) {
                if (Arrays.stream(selected).noneMatch(prospectiveStudent -> person.needsFixing())) {
                    // this next block preps variables for use by the menu & tooltip
                    List<String> academySetNames = AcademyFactory.getInstance().getAllSetNames();
                    Collections.sort(academySetNames);

                    // this filters out any academy sets that are disabled in Campaign Options,
                    // or not applicable for the current campaign faction
                    if (academySetNames.contains("Local Academies")) {
                        if (!campaign.getCampaignOptions().isEnableLocalAcademies()) {
                            academySetNames.remove("Local Academies");
                        }
                    }

                    if (academySetNames.contains("Prestigious Academies")) {
                        if (!campaign.getCampaignOptions().isEnablePrestigiousAcademies()) {
                            academySetNames.remove("Prestigious Academies");
                        }
                    }

                    if (academySetNames.contains("Unit Education")) {
                        if (!campaign.getCampaignOptions().isEnableUnitEducation()) {
                            academySetNames.remove("Unit Education");
                        }
                    }

                    // We then start processing the remaining academy sets
                    for (String setName : academySetNames) {
                        JMenu setAcademyMenu = new JMenu(setName);

                        // we filter each academy into one of these three categories
                        JMenu civilianMenu = new JMenu(resources.getString("eduCivilian.text"));
                        JMenu militaryMenu = new JMenu(resources.getString("eduMilitary.text"));

                        setAcademyMenu.add(civilianMenu);
                        setAcademyMenu.add(militaryMenu);

                        List<Academy> academiesOfSet = AcademyFactory.getInstance().getAllAcademiesForSet(setName);
                        Collections.sort(academiesOfSet);

                        for (Academy academy : academiesOfSet) {
                            // time to start filtering the academies
                            if (oneSelected) {
                                buildEducationMenusSingleton(campaign, person, academy, militaryMenu, civilianMenu);
                            } else {
                                buildEducationMenusMassEnroll(campaign,
                                      Arrays.asList(selected),
                                      academy,
                                      militaryMenu,
                                      civilianMenu);
                            }
                        }
                        academyMenu.add(setAcademyMenu);
                    }
                }
            }

            if (StaticChecks.areAllStudents(selected)) {
                JMenuItem completeStage = new JMenuItem(resources.getString("eduDropOut.text"));
                completeStage.setToolTipText(resources.getString("eduDropOut.toolTip"));
                completeStage.setActionCommand(makeCommand(CMD_DROP_OUT));
                completeStage.addActionListener(this);
                academyMenu.add(completeStage);
            }

            if ((oneSelected) && (StaticChecks.areAllStudents(selected))) {
                Academy academy = getAcademy(person.getEduAcademySet(), person.getEduAcademyNameInSet());

                if (academy == null) {
                    LOGGER.debug("Found null academy for {} skipping", person.getFullTitle());
                } else {
                    // this pile of if-statements just checks that the individual is eligible for
                    // re-enrollment
                    // has the person finished their education, but not yet returned to the unit?
                    if ((!person.getEduEducationStage().isJourneyToCampus()) &&
                              (!person.getEduEducationStage().isEducation())) {
                        // is the academy still standing?
                        if ((campaign.getGameYear() < academy.getDestructionYear()) &&
                                  (campaign.getGameYear() < academy.getClosureYear())) {
                            // if the academy is local, is the system still populated?
                            if ((!academy.isLocal()) ||
                                      (campaign.getCurrentSystem().getPopulation(campaign.getLocalDate()) > 0)) {
                                // is the person still within the correct age band?
                                if ((person.getAge(campaign.getLocalDate()) < academy.getAgeMax()) &&
                                          (person.getAge(campaign.getLocalDate()) >= academy.getAgeMin())) {
                                    // has the person been edited at some point and is no longer qualified?
                                    if (academy.isQualified(person)) {
                                        // here we check that the person will benefit from re-enrollment
                                        int improvementPossible = 0;

                                        String filteredFaction = academy.getFilteredFaction(campaign,
                                              person,
                                              List.of(person.getEduAcademyFaction()));

                                        if (filteredFaction != null) {
                                            int educationLevel = academy.getEducationLevel(person);

                                            String[] skillNames = academy.getCurriculums()
                                                                        .get(person.getEduCourseIndex())
                                                                        .split(",");

                                            skillNames = Arrays.stream(skillNames)
                                                               .map(String::trim)
                                                               .toArray(String[]::new);

                                            for (String skillName : skillNames) {
                                                if (skillName.equalsIgnoreCase("none")) {
                                                    continue;
                                                }

                                                if (skillName.equalsIgnoreCase("xp")) {
                                                    if (EducationLevel.parseToInt(person.getEduHighestEducation()) <
                                                              educationLevel) {
                                                        improvementPossible++;
                                                    }
                                                } else {
                                                    String skillParsed = skillParser(skillName);
                                                    Skill skill = person.getSkill(skillParsed);

                                                    if (skill != null) {
                                                        int skillLevel = skill.getLevel();
                                                        int experienceLevel = skill.getType()
                                                                                    .getExperienceLevel(skillLevel);
                                                        if (experienceLevel < educationLevel) {
                                                            improvementPossible++;
                                                        }
                                                    } else {
                                                        improvementPossible++;
                                                    }
                                                }
                                            }

                                            JMenuItem reEnroll;

                                            if (improvementPossible > 0) {
                                                reEnroll = new JMenuItem(resources.getString("eduReEnroll.text"));
                                                reEnroll.setToolTipText(resources.getString("eduReEnroll.toolTip"));
                                                reEnroll.setActionCommand(makeCommand(CMD_BEGIN_EDUCATION_RE_ENROLLMENT,
                                                      academy.getSet(),
                                                      academy.getName(),
                                                      String.valueOf(person.getEduCourseIndex()),
                                                      person.getEduAcademySystem(),
                                                      person.getEduAcademyFaction()));
                                                reEnroll.addActionListener(this);
                                            } else {
                                                reEnroll = new JMenuItem(resources.getString(
                                                      "eduReEnrollImpossible.text"));
                                                reEnroll.setToolTipText(resources.getString(
                                                      "eduReEnrollImpossible.toolTip"));
                                            }

                                            academyMenu.add(reEnroll);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if ((StaticChecks.areAllStudents(selected)) && (campaign.isGM())) {
                JMenuItem completeStage = new JMenuItem(resources.getString("eduCompleteStage.text"));
                completeStage.setToolTipText(resources.getString("eduCompleteStage.toolTip"));
                completeStage.setActionCommand(makeCommand(CMD_COMPLETE_STAGE));
                completeStage.addActionListener(this);
                academyMenu.add(completeStage);
            }

            if (campaign.isGM()) {
                JMenu changeEducation = new JMenu(resources.getString("eduChangeEducation.text"));
                changeEducation.setToolTipText(resources.getString("eduChangeEducation.toolTip"));
                academyMenu.add(changeEducation);

                for (EducationLevel level : EducationLevel.values()) {
                    JMenuItem educationLevel = new JMenuItem(level.toString());
                    educationLevel.setToolTipText(level.getToolTipText());
                    educationLevel.setActionCommand(makeCommand(CMD_CHANGE_EDUCATION_LEVEL + '@' + level.name()));
                    educationLevel.addActionListener(this);
                    changeEducation.add(educationLevel);
                }
            }

            popup.add(academyMenu);
        }
        // endregion Education Menu

        // region Spend XP Menu
        if (oneSelected && person.getStatus().isActiveFlexible()) {
            final boolean isUseReasoningMultiplier = getCampaignOptions().isUseReasoningXpMultiplier();
            final double reasoningXpCostMultiplier = person.getReasoningXpCostMultiplier(isUseReasoningMultiplier);
            final double xpCostMultiplier = getCampaignOptions().getXpCostMultiplier();

            menu = new JMenu(resources.getString("spendXP.text"));
            if (getCampaignOptions().isUseAbilities()) {
                JMenu combatAbilityMenu = new JMenu(resources.getString("combatAbilityMenu.text"));
                menu.add(combatAbilityMenu);

                JMenu maneuveringAbilityMenu = new JMenu(resources.getString("maneuveringAbilityMenu.text"));
                menu.add(maneuveringAbilityMenu);

                JMenu utilityAbilityMenu = new JMenu(resources.getString("utilityAbilityMenu.text"));
                menu.add(utilityAbilityMenu);

                JMenu characterFlawMenu = new JMenu(resources.getString("characterFlawMenu.text"));
                menu.add(characterFlawMenu);

                JMenu characterOriginMenu = new JMenu(resources.getString("characterOriginMenu.text"));
                menu.add(characterOriginMenu);

                int cost;

                List<SpecialAbility> specialAbilities = new ArrayList<>(SpecialAbility.getSpecialAbilities().values());
                specialAbilities.sort(Comparator.comparing(SpecialAbility::getName));

                for (SpecialAbility spa : specialAbilities) {
                    if (null == spa) {
                        continue;
                    }
                    if (!spa.isEligible(person)) {
                        continue;
                    }

                    // Reasoning cost changes should always take place before global changes
                    int baseCost = spa.getCost();
                    cost = (int) round(baseCost > 0 ? baseCost * reasoningXpCostMultiplier : baseCost);
                    cost = (int) round(cost * xpCostMultiplier);

                    String costDesc = String.format(resources.getString("costValue.format"), cost);
                    boolean available = person.getXP() >= cost;
                    if (spa.getName().equals(OptionsConstants.GUNNERY_WEAPON_SPECIALIST)) {
                        Unit unit = person.getUnit();
                        if (null != unit) {
                            JMenu specialistMenu = new JMenu(SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_WEAPON_SPECIALIST));
                            TreeSet<String> uniqueWeapons = new TreeSet<>();
                            for (int j = 0; j < unit.getEntity().getWeaponList().size(); j++) {
                                Mounted<?> m = unit.getEntity().getWeaponList().get(j);
                                uniqueWeapons.add(m.getName());
                            }
                            boolean isSpecialist = person.getOptions().booleanOption(spa.getName());
                            for (String name : uniqueWeapons) {
                                if (!(isSpecialist &&
                                            person.getOptions().getOption(spa.getName()).stringValue().equals(name))) {
                                    menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                                          name,
                                          costDesc));
                                    menuItem.setToolTipText(wordWrap(spa.getDescription() +
                                                                           "<br><br>" +
                                                                           spa.getAllPrereqDesc()));
                                    menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_WEAPON_SPECIALIST,
                                          name,
                                          String.valueOf(cost)));
                                    menuItem.addActionListener(this);
                                    menuItem.setEnabled(available);
                                    specialistMenu.add(menuItem);
                                }
                            }

                            if (specialistMenu.getMenuComponentCount() > 0) {
                                placeInAppropriateSPASubMenu(spa,
                                      specialistMenu,
                                      combatAbilityMenu,
                                      maneuveringAbilityMenu,
                                      utilityAbilityMenu,
                                      characterFlawMenu,
                                      characterOriginMenu);
                            }
                        }
                    } else if (spa.getName().equals(OptionsConstants.GUNNERY_SANDBLASTER)) {
                        Unit u = person.getUnit();
                        if (null != u) {
                            JMenu specialistMenu = new JMenu(SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_SANDBLASTER));
                            TreeSet<String> uniqueWeapons = new TreeSet<>();
                            for (int j = 0; j < u.getEntity().getWeaponList().size(); j++) {
                                Mounted<?> m = u.getEntity().getWeaponList().get(j);
                                if (SpecialAbility.isWeaponEligibleForSPA(m.getType(), person.getPrimaryRole(), true)) {
                                    uniqueWeapons.add(m.getName());
                                }
                            }
                            boolean isSpecialist = person.getOptions().booleanOption(spa.getName());
                            for (String name : uniqueWeapons) {
                                if (!(isSpecialist &&
                                            person.getOptions().getOption(spa.getName()).stringValue().equals(name))) {
                                    menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                                          name,
                                          costDesc));
                                    menuItem.setToolTipText(wordWrap(spa.getDescription() +
                                                                           "<br><br>" +
                                                                           spa.getAllPrereqDesc()));
                                    menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_SANDBLASTER,
                                          name,
                                          String.valueOf(cost)));
                                    menuItem.addActionListener(this);
                                    menuItem.setEnabled(available);
                                    specialistMenu.add(menuItem);
                                }
                            }
                            if (specialistMenu.getMenuComponentCount() > 0) {
                                placeInAppropriateSPASubMenu(spa,
                                      specialistMenu,
                                      combatAbilityMenu,
                                      maneuveringAbilityMenu,
                                      characterFlawMenu, utilityAbilityMenu,
                                      characterOriginMenu);
                            }
                        }
                    } else if (spa.getName().equals(OptionsConstants.MISC_ENV_SPECIALIST)) {
                        JMenu specialistMenu = new JMenu(SpecialAbility.getDisplayName(OptionsConstants.MISC_ENV_SPECIALIST));
                        List<Object> tros = new ArrayList<>();
                        if (person.getOptions().getOption(OptionsConstants.MISC_ENV_SPECIALIST).booleanValue()) {
                            Object val = person.getOptions().getOption(OptionsConstants.MISC_ENV_SPECIALIST).getValue();
                            if (val instanceof Collection<?>) {
                                tros.addAll((Collection<?>) val);
                            } else {
                                tros.add(val);
                            }
                        }
                        menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                              resources.getString("envspec_fog.text"),
                              costDesc));
                        menuItem.setToolTipText(wordWrap(spa.getDescription() + "<br><br>" + spa.getAllPrereqDesc()));
                        if (!tros.contains(Crew.ENVIRONMENT_SPECIALIST_FOG)) {
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_ENVIRONMENT_SPECIALIST,
                                  Crew.ENVIRONMENT_SPECIALIST_FOG,
                                  String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!tros.contains(Crew.ENVIRONMENT_SPECIALIST_LIGHT)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                                  resources.getString("envspec_light.text"),
                                  costDesc));
                            menuItem.setToolTipText(wordWrap(spa.getDescription() +
                                                                   "<br><br>" +
                                                                   spa.getAllPrereqDesc()));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_ENVIRONMENT_SPECIALIST,
                                  Crew.ENVIRONMENT_SPECIALIST_LIGHT,
                                  String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!tros.contains(Crew.ENVIRONMENT_SPECIALIST_RAIN)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                                  resources.getString("envspec_rain.text"),
                                  costDesc));
                            menuItem.setToolTipText(wordWrap(spa.getDescription() +
                                                                   "<br><br>" +
                                                                   spa.getAllPrereqDesc()));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_ENVIRONMENT_SPECIALIST,
                                  Crew.ENVIRONMENT_SPECIALIST_RAIN,
                                  String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!tros.contains(Crew.ENVIRONMENT_SPECIALIST_SNOW)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                                  resources.getString("envspec_snow.text"),
                                  costDesc));
                            menuItem.setToolTipText(wordWrap(spa.getDescription() +
                                                                   "<br><br>" +
                                                                   spa.getAllPrereqDesc()));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_ENVIRONMENT_SPECIALIST,
                                  Crew.ENVIRONMENT_SPECIALIST_SNOW,
                                  String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!tros.contains(Crew.ENVIRONMENT_SPECIALIST_WIND)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                                  resources.getString("envspec_wind.text"),
                                  costDesc));
                            menuItem.setToolTipText(wordWrap(spa.getDescription() +
                                                                   "<br><br>" +
                                                                   spa.getAllPrereqDesc()));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_ENVIRONMENT_SPECIALIST,
                                  Crew.ENVIRONMENT_SPECIALIST_WIND,
                                  String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (specialistMenu.getMenuComponentCount() > 0) {
                            placeInAppropriateSPASubMenu(spa,
                                  specialistMenu,
                                  combatAbilityMenu,
                                  maneuveringAbilityMenu,
                                  characterFlawMenu, utilityAbilityMenu,
                                  characterOriginMenu);
                        }
                    } else if (spa.getName().equals(OptionsConstants.MISC_HUMAN_TRO)) {
                        JMenu specialistMenu = new JMenu(SpecialAbility.getDisplayName(OptionsConstants.MISC_HUMAN_TRO));
                        List<Object> tros = new ArrayList<>();
                        if (person.getOptions().getOption(OptionsConstants.MISC_HUMAN_TRO).booleanValue()) {
                            Object val = person.getOptions().getOption(OptionsConstants.MISC_HUMAN_TRO).getValue();
                            if (val instanceof Collection<?>) {
                                tros.addAll((Collection<?>) val);
                            } else {
                                tros.add(val);
                            }
                        }
                        menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                              resources.getString("humantro_mek.text"),
                              costDesc));
                        menuItem.setToolTipText(wordWrap(spa.getDescription() + "<br><br>" + spa.getAllPrereqDesc()));
                        if (!tros.contains(Crew.HUMAN_TRO_MEK)) {
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_HUMAN_TRO,
                                  Crew.HUMAN_TRO_MEK,
                                  String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!tros.contains(Crew.HUMAN_TRO_AERO)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                                  resources.getString("humantro_aero.text"),
                                  costDesc));
                            menuItem.setToolTipText(wordWrap(spa.getDescription() +
                                                                   "<br><br>" +
                                                                   spa.getAllPrereqDesc()));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_HUMAN_TRO,
                                  Crew.HUMAN_TRO_AERO,
                                  String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!tros.contains(Crew.HUMAN_TRO_VEE)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                                  resources.getString("humantro_vee.text"),
                                  costDesc));
                            menuItem.setToolTipText(wordWrap(spa.getDescription() +
                                                                   "<br><br>" +
                                                                   spa.getAllPrereqDesc()));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_HUMAN_TRO,
                                  Crew.HUMAN_TRO_VEE,
                                  String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!tros.contains(Crew.HUMAN_TRO_BA)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                                  resources.getString("humantro_ba.text"),
                                  costDesc));
                            menuItem.setToolTipText(wordWrap(spa.getDescription() +
                                                                   "<br><br>" +
                                                                   spa.getAllPrereqDesc()));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_HUMAN_TRO,
                                  Crew.HUMAN_TRO_BA,
                                  String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (specialistMenu.getMenuComponentCount() > 0) {
                            placeInAppropriateSPASubMenu(spa,
                                  specialistMenu,
                                  combatAbilityMenu,
                                  maneuveringAbilityMenu,
                                  characterFlawMenu, utilityAbilityMenu,
                                  characterOriginMenu);
                        }
                    } else if (spa.getName().equals(OptionsConstants.GUNNERY_SPECIALIST) &&
                                     !person.getOptions().booleanOption(OptionsConstants.GUNNERY_SPECIALIST)) {
                        JMenu specialistMenu = new JMenu(SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_SPECIALIST));
                        menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                              resources.getString("laserSpecialist.text"),
                              costDesc));
                        menuItem.setToolTipText(wordWrap(spa.getDescription() + "<br><br>" + spa.getAllPrereqDesc()));
                        menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_SPECIALIST,
                              Crew.SPECIAL_ENERGY,
                              String.valueOf(cost)));
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(available);
                        specialistMenu.add(menuItem);
                        menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                              resources.getString("missileSpecialist.text"),
                              costDesc));
                        menuItem.setToolTipText(wordWrap(spa.getDescription() + "<br><br>" + spa.getAllPrereqDesc()));
                        menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_SPECIALIST,
                              Crew.SPECIAL_MISSILE,
                              String.valueOf(cost)));
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(available);
                        specialistMenu.add(menuItem);
                        menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                              resources.getString("ballisticSpecialist.text"),
                              costDesc));
                        menuItem.setToolTipText(wordWrap(spa.getDescription() + "<br><br>" + spa.getAllPrereqDesc()));
                        menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_SPECIALIST,
                              Crew.SPECIAL_BALLISTIC,
                              String.valueOf(cost)));
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(available);
                        specialistMenu.add(menuItem);

                        if (specialistMenu.getMenuComponentCount() > 0) {
                            placeInAppropriateSPASubMenu(spa,
                                  specialistMenu,
                                  combatAbilityMenu,
                                  maneuveringAbilityMenu,
                                  characterFlawMenu, utilityAbilityMenu,
                                  characterOriginMenu);
                        }
                    } else if (spa.getName().equals(OptionsConstants.GUNNERY_RANGE_MASTER)) {
                        JMenu specialistMenu = new JMenu(SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_RANGE_MASTER));
                        List<Object> ranges = new ArrayList<>();
                        if (person.getOptions().getOption(OptionsConstants.GUNNERY_RANGE_MASTER).booleanValue()) {
                            Object val = person.getOptions()
                                               .getOption(OptionsConstants.GUNNERY_RANGE_MASTER)
                                               .getValue();
                            if (val instanceof Collection<?>) {
                                ranges.addAll((Collection<?>) val);
                            } else {
                                ranges.add(val);
                            }
                        }

                        if (!ranges.contains(Crew.RANGEMASTER_MEDIUM)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                                  resources.getString("rangemaster_med.text"),
                                  costDesc));
                            menuItem.setToolTipText(wordWrap(spa.getDescription() +
                                                                   "<br><br>" +
                                                                   spa.getAllPrereqDesc()));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_RANGEMASTER,
                                  Crew.RANGEMASTER_MEDIUM,
                                  String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!ranges.contains(Crew.RANGEMASTER_LONG)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                                  resources.getString("rangemaster_lng.text"),
                                  costDesc));
                            menuItem.setToolTipText(wordWrap(spa.getDescription() +
                                                                   "<br><br>" +
                                                                   spa.getAllPrereqDesc()));
                            menuItem.setToolTipText(wordWrap(spa.getDescription() +
                                                                   "<br><br>" +
                                                                   spa.getAllPrereqDesc()));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_RANGEMASTER,
                                  Crew.RANGEMASTER_LONG,
                                  String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!ranges.contains(Crew.RANGEMASTER_EXTREME)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                                  resources.getString("rangemaster_xtm.text"),
                                  costDesc));
                            menuItem.setToolTipText(wordWrap(spa.getDescription() +
                                                                   "<br><br>" +
                                                                   spa.getAllPrereqDesc()));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_RANGEMASTER,
                                  Crew.RANGEMASTER_EXTREME,
                                  String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (specialistMenu.getMenuComponentCount() > 0) {
                            placeInAppropriateSPASubMenu(spa,
                                  specialistMenu,
                                  combatAbilityMenu,
                                  maneuveringAbilityMenu,
                                  characterFlawMenu, utilityAbilityMenu,
                                  characterOriginMenu);
                        }
                    } else if (Optional.ofNullable((person.getOptions().getOption(spa.getName()))).isPresent() &&
                                     (person.getOptions().getOption(spa.getName()).getType() == IOption.CHOICE) &&
                                     !(person.getOptions().getOption(spa.getName()).booleanValue())) {
                        JMenu specialistMenu = new JMenu(spa.getDisplayName());
                        List<String> choices = spa.getChoiceValues();
                        for (String s : choices) {
                            if (s.equalsIgnoreCase("none")) {
                                continue;
                            }
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                                  s,
                                  costDesc));
                            menuItem.setToolTipText(wordWrap(spa.getDescription() +
                                                                   "<br><br>" +
                                                                   spa.getAllPrereqDesc()));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_CUSTOM_CHOICE,
                                  s,
                                  String.valueOf(cost),
                                  spa.getName()));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }
                        if (specialistMenu.getMenuComponentCount() > 0) {
                            placeInAppropriateSPASubMenu(spa,
                                  specialistMenu,
                                  combatAbilityMenu,
                                  maneuveringAbilityMenu,
                                  characterFlawMenu, utilityAbilityMenu,
                                  characterOriginMenu);
                        }
                    } else if (!person.getOptions().booleanOption(spa.getName())) {
                        menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                              spa.getDisplayName(),
                              costDesc));
                        menuItem.setToolTipText(wordWrap(spa.getDescription() + "<br><br>" + spa.getAllPrereqDesc()));

                        menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_ABILITY,
                              spa.getName(),
                              String.valueOf(cost)));
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(available);

                        AbilityCategory category = getSpaCategory(spa);

                        switch (category) {
                            case COMBAT_ABILITY -> combatAbilityMenu.add(menuItem);
                            case MANEUVERING_ABILITY -> maneuveringAbilityMenu.add(menuItem);
                            case UTILITY_ABILITY -> utilityAbilityMenu.add(menuItem);
                            case CHARACTER_FLAW -> characterFlawMenu.add(menuItem);
                            case CHARACTER_CREATION_ONLY -> {
                            }
                        }
                    }
                }
            }

            JMenu currentMenu = new JMenu(resources.getString("spendOnCurrentSkills.text"));
            JMenu combatGunnerySkillsCurrent = new JMenu(resources.getString("combatGunnerySkills.text"));
            JMenu combatPilotingSkillsCurrent = new JMenu(resources.getString("combatPilotingSkills.text"));
            JMenu supportSkillsCurrent = new JMenu(resources.getString("supportSkills.text"));
            JMenu utilitySkillsCurrent = new JMenu(resources.getString("utilitySkills.text"));
            JMenu roleplaySkillsCurrent = new JMenu(resources.getString("roleplaySkills.text"));
            JMenu roleplaySkillsArtCurrent = new JMenu(resources.getString("roleplaySkills.art"));
            JMenu roleplaySkillsInterestCurrent = new JMenu(resources.getString("roleplaySkills.interest"));
            JMenu roleplaySkillsScienceCurrent = new JMenu(resources.getString("roleplaySkills.science"));

            JMenu newSkillsMenu = new JMenu(resources.getString("spendOnNewSkills.text"));
            JMenu combatGunnerySkillsNew = new JMenu(resources.getString("combatGunnerySkills.text"));
            JMenu combatPilotingSkillsNew = new JMenu(resources.getString("combatPilotingSkills.text"));
            JMenu supportSkillsNew = new JMenu(resources.getString("supportSkills.text"));
            JMenu utilitySkillsNew = new JMenu(resources.getString("utilitySkills.text"));
            JMenu roleplaySkillsNew = new JMenu(resources.getString("roleplaySkills.text"));
            JMenu roleplaySkillsArtNew = new JMenu(resources.getString("roleplaySkills.art"));
            JMenu roleplaySkillsInterestNew = new JMenu(resources.getString("roleplaySkills.interest"));
            JMenu roleplaySkillsScienceNew = new JMenu(resources.getString("roleplaySkills.science"));

            int adjustedReputation = person.getAdjustedReputation(getCampaignOptions().isUseAgeEffects(),
                  getCampaign().isClanCampaign(),
                  getCampaign().getLocalDate(),
                  person.getRankNumeric());

            boolean adminsHaveNegotiation = getCampaignOptions().isAdminsHaveNegotiation();
            boolean doctorsUseAdmin = getCampaignOptions().isDoctorsUseAdministration();
            boolean techsUseAdmin = getCampaignOptions().isTechsUseAdministration();
            boolean isUseArtillery = getCampaignOptions().isUseArtillery();
            PersonnelRole primaryProfession = person.getPrimaryRole();
            List<String> primaryProfessionSkills = primaryProfession.getSkillsForProfession(adminsHaveNegotiation,
                  doctorsUseAdmin,
                  techsUseAdmin,
                  isUseArtillery,
                  true);

            PersonnelRole secondaryProfession = person.getSecondaryRole();
            List<String> secondaryProfessionSkills = new ArrayList<>(secondaryProfession.getSkillsForProfession(
                  adminsHaveNegotiation,
                  doctorsUseAdmin,
                  techsUseAdmin,
                  isUseArtillery,
                  true));
            secondaryProfessionSkills.removeAll(primaryProfessionSkills);

            for (int i = 0; i < SkillType.getSkillList().length; i++) {
                String typeName = SkillType.getSkillList()[i];

                int cost = person.getCostToImprove(typeName, isUseReasoningMultiplier);
                cost = (int) round(cost * xpCostMultiplier);

                if (cost >= 0) {
                    if (Objects.equals(typeName, S_ARTILLERY)) {
                        if (!getCampaignOptions().isUseArtillery()) {
                            continue;
                        }
                    }

                    String description;
                    if (primaryProfessionSkills.contains(typeName)) {
                        description = String.format(resources.getString("skillDesc.format.profession"),
                              ReportingUtilities.spanOpeningWithCustomColor(getAmazingColor()), CLOSING_SPAN_TAG,
                              typeName, cost);
                    } else if (secondaryProfessionSkills.contains(typeName)) {
                        description = String.format(resources.getString("skillDesc.format.profession"),
                              ReportingUtilities.spanOpeningWithCustomColor(getPositiveColor()), CLOSING_SPAN_TAG,
                              typeName, cost);
                    } else {
                        description = String.format(resources.getString("skillDesc.format"), typeName, cost);
                    }

                    SkillModifierData skillModifierData =
                          person.getSkillModifierData(getCampaignOptions().isUseAgeEffects(),
                                getCampaign().isClanCampaign(), getCampaign().getLocalDate());

                    menuItem = new JMenuItem(description);
                    menuItem.setActionCommand(makeCommand(CMD_IMPROVE, typeName, String.valueOf(cost)));
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(person.getXP() >= cost);
                    if (person.hasSkill(typeName)) {
                        Skill skill = person.getSkill(typeName);
                        if (skill.isImprovementLegal()) {
                            SkillType skillType = getType(typeName);
                            if (skillType == null) {
                                LOGGER.error("(Current Skills) Unknown skill type: {}", typeName);
                                continue;
                            }

                            String tooltip = wordWrap(skill.getTooltip(skillModifierData));
                            menuItem.setToolTipText(tooltip);

                            SkillSubType subType = skillType.getSubType();
                            switch (subType) {
                                case NONE -> currentMenu.add(menuItem);
                                case COMBAT_GUNNERY -> combatGunnerySkillsCurrent.add(menuItem);
                                case COMBAT_PILOTING -> combatPilotingSkillsCurrent.add(menuItem);
                                case SUPPORT -> supportSkillsCurrent.add(menuItem);
                                case UTILITY, UTILITY_COMMAND -> utilitySkillsCurrent.add(menuItem);
                                case ROLEPLAY_GENERAL -> roleplaySkillsCurrent.add(menuItem);
                                case ROLEPLAY_ART -> roleplaySkillsArtCurrent.add(menuItem);
                                case ROLEPLAY_INTEREST -> roleplaySkillsInterestCurrent.add(menuItem);
                                case ROLEPLAY_SCIENCE, ROLEPLAY_SECURITY -> roleplaySkillsScienceCurrent.add(menuItem);
                                default -> LOGGER.error("(Current Skills) Unknown skill sub type: {}", subType);
                            }
                        }
                    } else {
                        SkillType skillType = getType(typeName);
                        if (skillType == null) {
                            LOGGER.error("(New Skills) Unknown skill type: {}", typeName);
                            continue;
                        }

                        String tooltip = wordWrap(skillType.getFlavorText(false, true));
                        menuItem.setToolTipText(tooltip);

                        SkillSubType subType = skillType.getSubType();
                        switch (subType) {
                            case NONE -> newSkillsMenu.add(menuItem);
                            case COMBAT_GUNNERY -> combatGunnerySkillsNew.add(menuItem);
                            case COMBAT_PILOTING -> combatPilotingSkillsNew.add(menuItem);
                            case SUPPORT -> supportSkillsNew.add(menuItem);
                            case UTILITY, UTILITY_COMMAND -> utilitySkillsNew.add(menuItem);
                            case ROLEPLAY_GENERAL -> roleplaySkillsNew.add(menuItem);
                            case ROLEPLAY_ART -> roleplaySkillsArtNew.add(menuItem);
                            case ROLEPLAY_INTEREST -> roleplaySkillsInterestNew.add(menuItem);
                            case ROLEPLAY_SCIENCE, ROLEPLAY_SECURITY -> roleplaySkillsScienceNew.add(menuItem);
                            default -> LOGGER.error("(New Skills) Unknown skill sub type: {}", subType);
                        }
                    }
                }
            }


            if (combatGunnerySkillsCurrent.getMenuComponentCount() > 0) {
                currentMenu.add(combatGunnerySkillsCurrent);
            }
            if (combatPilotingSkillsCurrent.getMenuComponentCount() > 0) {
                currentMenu.add(combatPilotingSkillsCurrent);
            }
            if (supportSkillsCurrent.getMenuComponentCount() > 0) {
                currentMenu.add(supportSkillsCurrent);
            }
            if (utilitySkillsCurrent.getMenuComponentCount() > 0) {
                currentMenu.add(utilitySkillsCurrent);
            }
            if (roleplaySkillsArtCurrent.getMenuComponentCount() > 0) {
                roleplaySkillsCurrent.add(roleplaySkillsArtCurrent);
            }
            if (roleplaySkillsInterestCurrent.getMenuComponentCount() > 0) {
                roleplaySkillsCurrent.add(roleplaySkillsInterestCurrent);
            }
            if (roleplaySkillsScienceCurrent.getMenuComponentCount() > 0) {
                roleplaySkillsCurrent.add(roleplaySkillsScienceCurrent);
            }
            if (roleplaySkillsCurrent.getMenuComponentCount() > 0) {
                currentMenu.add(roleplaySkillsCurrent);
            }

            if (currentMenu.getMenuComponentCount() > 0) {
                menu.add(currentMenu);
            }

            if (combatGunnerySkillsNew.getMenuComponentCount() > 0) {
                newSkillsMenu.add(combatGunnerySkillsNew);
            }
            if (combatPilotingSkillsNew.getMenuComponentCount() > 0) {
                newSkillsMenu.add(combatPilotingSkillsNew);
            }
            if (supportSkillsNew.getMenuComponentCount() > 0) {
                newSkillsMenu.add(supportSkillsNew);
            }
            if (utilitySkillsNew.getMenuComponentCount() > 0) {
                newSkillsMenu.add(utilitySkillsNew);
            }
            if (roleplaySkillsArtNew.getMenuComponentCount() > 0) {
                roleplaySkillsNew.add(roleplaySkillsArtNew);
            }
            if (roleplaySkillsInterestNew.getMenuComponentCount() > 0) {
                roleplaySkillsNew.add(roleplaySkillsInterestNew);
            }
            if (roleplaySkillsScienceNew.getMenuComponentCount() > 0) {
                roleplaySkillsNew.add(roleplaySkillsScienceNew);
            }
            if (roleplaySkillsNew.getMenuComponentCount() > 0) {
                newSkillsMenu.add(roleplaySkillsNew);
            }

            if (newSkillsMenu.getMenuComponentCount() > 0) {
                menu.add(newSkillsMenu);
            }

            JMenu traitsMenu = new JMenu(resources.getString("spendOnTraits.text"));
            double costMultiplier = getCampaignOptions().getXpCostMultiplier();
            int traitCost = (int) round(TRAIT_MODIFICATION_COST * costMultiplier);

            // Connections
            int connections = person.getConnections();
            int target = connections + 1;
            menuItem = new JMenuItem(String.format(resources.getString("spendOnConnections.text"), target, traitCost));
            menuItem.setToolTipText(wordWrap(String.format(resources.getString("spendOnConnections.tooltip"),
                  ((target > 0 ? "+" : "-") + target))));
            menuItem.setActionCommand(makeCommand(CMD_BUY_TRAIT,
                  CONNECTIONS_LABEL,
                  String.valueOf(traitCost),
                  String.valueOf(target)));
            menuItem.addActionListener(this);
            menuItem.setEnabled(target <= MAXIMUM_CONNECTIONS && person.getXP() >= traitCost);
            traitsMenu.add(menuItem);

            // Reputation
            int reputation = person.getReputation();
            target = reputation + 1;
            menuItem = new JMenuItem(String.format(resources.getString("spendOnReputation.text"), target, traitCost));
            menuItem.setToolTipText(wordWrap(String.format(resources.getString("spendOnReputation.tooltip"),
                  (target == 0 ? 0 : (target > 0 ? "+" : "-") + target),
                  target)));
            menuItem.setActionCommand(makeCommand(CMD_BUY_TRAIT,
                  REPUTATION_LABEL,
                  String.valueOf(traitCost),
                  String.valueOf(target)));
            menuItem.addActionListener(this);
            menuItem.setEnabled(target <= MAXIMUM_REPUTATION && person.getXP() >= traitCost);
            traitsMenu.add(menuItem);

            target = reputation - 1;
            menuItem = new JMenuItem(String.format(resources.getString("spendOnReputation.text"), target, -traitCost));
            menuItem.setToolTipText(wordWrap(String.format(resources.getString("spendOnReputation.tooltip"),
                  (target == 0 ? 0 : (target > 0 ? "+" : "-") + target),
                  target)));
            menuItem.setActionCommand(makeCommand(CMD_BUY_TRAIT,
                  REPUTATION_LABEL,
                  String.valueOf(-traitCost),
                  String.valueOf(target)));
            menuItem.addActionListener(this);
            menuItem.setEnabled(target >= MINIMUM_REPUTATION);
            traitsMenu.add(menuItem);

            // Wealth
            int wealth = person.getWealth();
            target = wealth + 1;
            menuItem = new JMenuItem(String.format(resources.getString("spendOnWealth.text"), target, traitCost));
            menuItem.setToolTipText(resources.getString("spendOnWealth.tooltip"));
            menuItem.setActionCommand(makeCommand(CMD_BUY_TRAIT,
                  WEALTH_LABEL,
                  String.valueOf(traitCost),
                  String.valueOf(target)));
            menuItem.addActionListener(this);
            menuItem.setEnabled(target <= MAXIMUM_WEALTH && person.getXP() >= traitCost);
            traitsMenu.add(menuItem);

            target = wealth - 1;
            menuItem = new JMenuItem(String.format(resources.getString("spendOnWealth.text"), target, -traitCost));
            menuItem.setToolTipText(resources.getString("spendOnWealth.tooltip"));
            menuItem.setActionCommand(makeCommand(CMD_BUY_TRAIT,
                  WEALTH_LABEL,
                  String.valueOf(-traitCost),
                  String.valueOf(target)));
            menuItem.addActionListener(this);
            menuItem.setEnabled(target >= MINIMUM_WEALTH);
            traitsMenu.add(menuItem);

            // Unlucky
            int unlucky = person.getUnlucky();
            target = unlucky + 1;
            menuItem = new JMenuItem(String.format(resources.getString("spendOnUnlucky.text"), target, -traitCost));
            menuItem.setToolTipText(String.format(resources.getString("spendOnUnlucky.tooltip"), target));
            menuItem.setActionCommand(makeCommand(CMD_BUY_TRAIT,
                  UNLUCKY_LABEL,
                  String.valueOf(-traitCost),
                  String.valueOf(target)));
            menuItem.addActionListener(this);
            menuItem.setEnabled(target <= MAXIMUM_UNLUCKY);
            traitsMenu.add(menuItem);

            target = unlucky - 1;
            menuItem = new JMenuItem(String.format(resources.getString("spendOnUnlucky.text"), target, traitCost));
            menuItem.setToolTipText(String.format(resources.getString("spendOnUnlucky.tooltip"), target));
            menuItem.setActionCommand(makeCommand(CMD_BUY_TRAIT,
                  UNLUCKY_LABEL,
                  String.valueOf(traitCost),
                  String.valueOf(target)));
            menuItem.addActionListener(this);
            menuItem.setEnabled(target >= MINIMUM_UNLUCKY && person.getXP() >= traitCost);
            traitsMenu.add(menuItem);

            // Bloodmark
            int bloodmark = person.getBloodmark();

            target = bloodmark + 1;
            menuItem = new JMenuItem(String.format(resources.getString("spendOnBloodmark.text"), target, -traitCost));
            menuItem.setToolTipText(String.format(resources.getString("spendOnBloodmark.tooltip"), target));
            menuItem.setActionCommand(makeCommand(CMD_BUY_TRAIT,
                  BLOODMARK_LABEL,
                  String.valueOf(-traitCost),
                  String.valueOf(target)));
            menuItem.addActionListener(this);
            menuItem.setEnabled(target <= MAXIMUM_BLOODMARK);
            traitsMenu.add(menuItem);

            target = bloodmark - 1;
            menuItem = new JMenuItem(String.format(resources.getString("spendOnBloodmark.text"), target, traitCost));
            menuItem.setToolTipText(String.format(resources.getString("spendOnBloodmark.tooltip"), target));
            menuItem.setActionCommand(makeCommand(CMD_BUY_TRAIT,
                  BLOODMARK_LABEL,
                  String.valueOf(traitCost),
                  String.valueOf(target)));
            menuItem.addActionListener(this);
            menuItem.setEnabled(target >= MINIMUM_BLOODMARK && person.getXP() >= traitCost);
            traitsMenu.add(menuItem);

            // Extra Income
            int extraIncome = person.getExtraIncomeTraitLevel();

            target = extraIncome + 1;
            menuItem = new JMenuItem(String.format(resources.getString("spendOnExtraIncome.text"), target, -traitCost));
            menuItem.setToolTipText(String.format(resources.getString("spendOnExtraIncome.tooltip"), target));
            menuItem.setActionCommand(makeCommand(CMD_BUY_TRAIT,
                  EXTRA_INCOME_LABEL,
                  String.valueOf(traitCost),
                  String.valueOf(target)));
            menuItem.addActionListener(this);
            menuItem.setEnabled(target <= MAXIMUM_EXTRA_INCOME);
            traitsMenu.add(menuItem);

            target = extraIncome - 1;
            menuItem = new JMenuItem(String.format(resources.getString("spendOnExtraIncome.text"), target, traitCost));
            menuItem.setToolTipText(String.format(resources.getString("spendOnExtraIncome.tooltip"), target));
            menuItem.setActionCommand(makeCommand(CMD_BUY_TRAIT,
                  EXTRA_INCOME_LABEL,
                  String.valueOf(-traitCost),
                  String.valueOf(target)));
            menuItem.addActionListener(this);
            menuItem.setEnabled(target >= MINIMUM_EXTRA_INCOME && person.getXP() >= traitCost);
            traitsMenu.add(menuItem);

            menu.add(traitsMenu);

            JMenu attributesMenuIncrease = new JMenu(resources.getString("spendOnAttributes.increase"));
            int attributeImprovementCost = (int) round(getCampaignOptions().getAttributeCost() * costMultiplier);
            int edgeCost = (int) round(getCampaignOptions().getEdgeCost() * costMultiplier);
            for (SkillAttribute attribute : SkillAttribute.values()) {
                if (attribute.isNone()) {
                    continue;
                }

                boolean isEdge = attribute == SkillAttribute.EDGE;
                if (isEdge && !getCampaignOptions().isUseEdge()) {
                    continue;
                }

                int attributeCost = (int) round((isEdge ? edgeCost : attributeImprovementCost)
                                                      * reasoningXpCostMultiplier);

                int current = person.getAttributeScore(attribute);
                // Improve
                target = current + 1;
                menuItem = new JMenuItem(String.format(resources.getString("spendOnAttributes.format"),
                      attribute.getLabel(),
                      current,
                      target,
                      traitCost));
                menuItem.setToolTipText(wordWrap(String.format(resources.getString("spendOnAttributes.tooltip"))));
                menuItem.setActionCommand(makeCommand(CMD_CHANGE_ATTRIBUTE,
                      String.valueOf(attribute),
                      String.valueOf(attributeCost)));
                menuItem.addActionListener(this);
                int attributeCap = min(person.getPhenotype().getAttributeCap(attribute), MAXIMUM_ATTRIBUTE_SCORE);
                menuItem.setEnabled(target <= attributeCap && person.getXP() >= attributeCost);
                attributesMenuIncrease.add(menuItem);
            }
            menu.add(attributesMenuIncrease);
            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
            // endregion Spend XP Menu

            // region Edge Triggers
            if (getCampaignOptions().isUseEdge()) {
                menu = new JMenu(resources.getString("setEdgeTriggers.text"));

                // Start of Edge reroll options
                // MekWarriors
                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerHeadHits.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_HEAD_HIT));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_HEAD_HIT));
                if (!person.getPrimaryRole().isMekWarriorGrouping()) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerTAC.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_TAC));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_TAC));
                if (!person.getPrimaryRole().isMekWarriorGrouping()) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerKO.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_KO));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_KO));
                if (!person.getPrimaryRole().isMekWarriorGrouping()) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerExplosion.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_EXPLOSION));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_EXPLOSION));
                if (!person.getPrimaryRole().isMekWarriorGrouping()) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerMASCFailure.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_MASC_FAILURE));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_MASC_FAILURE));
                if (!person.getPrimaryRole().isMekWarriorGrouping()) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                // Aerospace pilots and gunners
                final boolean isNotAeroOrConventional = !(person.getPrimaryRole().isAerospacePilot() ||
                                                                person.getPrimaryRole().isConventionalAircraftPilot() ||
                                                                person.getPrimaryRole().isLAMPilot());
                final boolean isNotVessel = !person.getPrimaryRole().isVesselCrewMember();
                final boolean isNotAeroConvOrVessel = isNotAeroOrConventional || isNotVessel;

                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerAeroAltLoss.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_WHEN_AERO_ALT_LOSS));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_ALT_LOSS));
                if (isNotAeroConvOrVessel) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerAeroExplosion.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_WHEN_AERO_EXPLOSION));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_EXPLOSION));
                if (isNotAeroConvOrVessel) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerAeroKO.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_WHEN_AERO_KO));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_KO));
                if (isNotAeroOrConventional) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerAeroLuckyCrit.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_WHEN_AERO_LUCKY_CRIT));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_LUCKY_CRIT));
                if (isNotAeroConvOrVessel) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerAeroNukeCrit.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_WHEN_AERO_NUKE_CRIT));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_NUKE_CRIT));
                if (isNotVessel) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerAeroTrnBayCrit.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_WHEN_AERO_UNIT_CARGO_LOST));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_UNIT_CARGO_LOST));
                if (isNotVessel) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                // Support Edge
                if (getCampaignOptions().isUseSupportEdge()) {
                    // Doctors
                    cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerHealCheck.text"));
                    cbMenuItem.setSelected(person.getOptions().booleanOption(PersonnelOptions.EDGE_MEDICAL));
                    cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_MEDICAL));
                    if (!person.getPrimaryRole().isDoctor()) {
                        cbMenuItem.setForeground(new Color(150, 150, 150));
                    }
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);

                    // Techs
                    cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerBreakPart.text"));
                    cbMenuItem.setSelected(person.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_BREAK_PART));
                    cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_REPAIR_BREAK_PART));
                    if (!person.getPrimaryRole().isTech()) {
                        cbMenuItem.setForeground(new Color(150, 150, 150));
                    }
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);

                    cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerFailedRefit.text"));
                    cbMenuItem.setSelected(person.getOptions()
                                                 .booleanOption(PersonnelOptions.EDGE_REPAIR_FAILED_REFIT));
                    cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER,
                          PersonnelOptions.EDGE_REPAIR_FAILED_REFIT));
                    if (!person.getPrimaryRole().isTech()) {
                        cbMenuItem.setForeground(new Color(150, 150, 150));
                    }
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);

                    // Admins
                    cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerAcquireCheck.text"));
                    cbMenuItem.setSelected(person.getOptions().booleanOption(PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL));
                    cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER,
                          PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL));
                    if (!person.getPrimaryRole().isAdministrator()) {
                        cbMenuItem.setForeground(new Color(150, 150, 150));
                    }
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);
                }
                JMenuHelpers.addMenuIfNonEmpty(popup, menu);
            }
            // endregion Edge Triggers

            popup.add(menu);
        } else if (StaticChecks.areAllActiveFlexible(selected)) {
            if (getCampaignOptions().isUseEdge()) {
                menu = new JMenu(resources.getString("setEdgeTriggers.text"));
                submenu = new JMenu(resources.getString("On.text"));

                menuItem = new JMenuItem(resources.getString("edgeTriggerHeadHits.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_HEAD_HIT, TRUE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerTAC.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_TAC, TRUE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerKO.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_KO, TRUE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerExplosion.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_EXPLOSION, TRUE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerMASCFailure.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_MASC_FAILURE, TRUE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerAeroAltLoss.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_ALT_LOSS, TRUE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerAeroExplosion.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_EXPLOSION, TRUE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerAeroKO.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_KO, TRUE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerAeroLuckyCrit.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_LUCKY_CRIT, TRUE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerAeroNukeCrit.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_NUKE_CRIT, TRUE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerAeroTrnBayCrit.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_UNIT_CARGO_LOST, TRUE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                if (getCampaignOptions().isUseSupportEdge()) {
                    menuItem = new JMenuItem(resources.getString("edgeTriggerHealCheck.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_MEDICAL, TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("edgeTriggerBreakPart.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER,
                          PersonnelOptions.EDGE_REPAIR_BREAK_PART,
                          TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("edgeTriggerFailedRefit.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER,
                          PersonnelOptions.EDGE_REPAIR_FAILED_REFIT,
                          TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("edgeTriggerAcquireCheck.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER,
                          PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL,
                          TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                }
                JMenuHelpers.addMenuIfNonEmpty(menu, submenu);

                submenu = new JMenu(resources.getString("Off.text"));

                menuItem = new JMenuItem(resources.getString("edgeTriggerHeadHits.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_HEAD_HIT, FALSE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerTAC.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_TAC, FALSE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerKO.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_KO, FALSE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerExplosion.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_EXPLOSION, FALSE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerMASCFailure.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_MASC_FAILURE, FALSE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerAeroAltLoss.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_ALT_LOSS, FALSE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerAeroExplosion.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_EXPLOSION, FALSE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerAeroKO.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_KO, FALSE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerAeroLuckyCrit.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_LUCKY_CRIT, FALSE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerAeroNukeCrit.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_NUKE_CRIT, FALSE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("edgeTriggerAeroTrnBayCrit.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_WHEN_AERO_UNIT_CARGO_LOST, FALSE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);

                if (getCampaignOptions().isUseSupportEdge()) {
                    menuItem = new JMenuItem(resources.getString("edgeTriggerHealCheck.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_MEDICAL, FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("edgeTriggerBreakPart.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER,
                          PersonnelOptions.EDGE_REPAIR_BREAK_PART,
                          FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("edgeTriggerFailedRefit.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER,
                          PersonnelOptions.EDGE_REPAIR_FAILED_REFIT,
                          FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("edgeTriggerAcquireCheck.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER,
                          PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL,
                          FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                }
                JMenuHelpers.addMenuIfNonEmpty(menu, submenu);
                JMenuHelpers.addMenuIfNonEmpty(popup, menu);
            }

            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        }

        if (!oneSelected) {
            menuItem = new JMenuItem(resources.getString("bulkAssignSinglePortrait.text"));
            menuItem.setActionCommand(CMD_EDIT_PORTRAIT);
            menuItem.addActionListener(this);
            popup.add(menuItem);
        }

        if (oneSelected) {
            menu = new JMenu(resources.getString("changeProfile.text"));

            menuItem = new JMenuItem(resources.getString("changePortrait.text"));
            menuItem.setActionCommand(CMD_EDIT_PORTRAIT);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("changeBiography.text"));
            menuItem.setActionCommand(CMD_EDIT_BIOGRAPHY);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("changeCallsign.text"));
            menuItem.setActionCommand(CMD_CALLSIGN);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        }

        menu = new JMenu(resources.getString("editLogs.text"));

        if (oneSelected) {
            menuItem = new JMenuItem(resources.getString("editPersonnelLog.text"));
            menuItem.setActionCommand(CMD_EDIT_PERSONNEL_LOG);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("editScenarioLog.text"));
            menuItem.setActionCommand(CMD_EDIT_SCENARIO_LOG);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("editMedicalLog.text"));
            menuItem.setActionCommand(CMD_EDIT_MEDICAL_LOG);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("editKillLog.text"));
            menuItem.setActionCommand(CMD_EDIT_KILL_LOG);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("editAssignmentLog.text"));
            menuItem.setActionCommand(CMD_ADD_ASSIGNMENT_LOG_ENTRY);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("editPerformanceLog.text"));
            menuItem.setActionCommand(CMD_ADD_PERFORMANCE_LOG_ENTRY);
            menuItem.addActionListener(this);
            menu.add(menuItem);
        } else {
            menuItem = new JMenuItem(resources.getString("addSingleLogEntry.text"));
            menuItem.setActionCommand(CMD_ADD_LOG_ENTRY);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("addScenarioEntry.text"));
            menuItem.setActionCommand(CMD_ADD_SCENARIO_ENTRY);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("addSingleMedicalLogEntry.text"));
            menuItem.setActionCommand(CMD_ADD_MEDICAL_LOG_ENTRY);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            if (StaticChecks.allHaveSameUnit(selected)) {
                menuItem = new JMenuItem(resources.getString("assignKill.text"));
                menuItem.setActionCommand(CMD_ADD_KILL);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                menu.add(menuItem);
            }

            menuItem = new JMenuItem(resources.getString("addSingleAssignmentLogEntry.text"));
            menuItem.setActionCommand(CMD_ADD_ASSIGNMENT_LOG_ENTRY);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("addSinglePerformanceLogEntry.text"));
        }
        menuItem.setActionCommand(CMD_ADD_PERFORMANCE_LOG_ENTRY);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        JMenuHelpers.addMenuIfNonEmpty(popup, menu);

        menuItem = new JMenuItem(resources.getString("exportPersonnel.text"));
        menuItem.addActionListener(evt -> gui.savePersonFile());
        menuItem.setEnabled(true);
        popup.add(menuItem);

        if (StaticChecks.areAllEmployed(selected)) {
            menuItem = new JMenuItem(resources.getString("sack.text"));
            menuItem.setActionCommand(CMD_SACK);
            menuItem.addActionListener(this);
            popup.add(menuItem);
        }

        if (!StaticChecks.areAllEmployed(selected)) {
            menuItem = new JMenuItem(resources.getString("employ.text"));
            menuItem.setActionCommand(CMD_EMPLOY);
            menuItem.addActionListener(this);
            popup.add(menuItem);
        }

        if (oneSelected) {
            int wealth = person.getWealth();
            int willpower = person.getAttributeScore(WILLPOWER);
            int spending = getExpenditure(willpower, wealth);
            String spendingString = Money.of(spending).toAmountString();
            menuItem = new JMenuItem(String.format(resources.getString("wealth.extreme.single"), spendingString));
            menuItem.setEnabled(!person.isHasPerformedExtremeExpenditure() &&
                                      person.getStatus().isActive() &&
                                      person.getPrisonerStatus().isFreeOrBondsman());
        } else {
            menuItem = new JMenuItem(resources.getString("wealth.extreme.multiple"));
            menuItem.setEnabled(StaticChecks.areAnyActive(selected) && StaticChecks.areAnyFreeOrBondsman(selected));
        }
        menuItem.setActionCommand(CMD_SPENDING_SPREE);
        menuItem.addActionListener(this);
        popup.add(menuItem);

        menuItem = new JMenuItem(resources.getString("bloodmark.claimBounty"));
        menuItem.setActionCommand(CMD_CLAIM_BOUNTY);
        menuItem.addActionListener(this);
        popup.add(menuItem);

        // region Flags Menu
        // This Menu contains the following flags, in the specified order:
        // 1) Clan Personnel
        // 2) Commander
        // 3) Divorceable
        // 4) Founder
        // 5) Immortal
        // 6) Marriageable
        // 7) Trying To Marry
        menu = new JMenu(resources.getString("specialFlagsMenu.text"));

        if (Stream.of(selected).allMatch(p -> p.isClanPersonnel() == person.isClanPersonnel())) {
            cbMenuItem = new JCheckBoxMenuItem(resources.getString("miClanPersonnel.text"));
            cbMenuItem.setToolTipText(resources.getString("miClanPersonnel.toolTipText"));
            cbMenuItem.setName("miClanPersonnel");
            cbMenuItem.setSelected(person.isClanPersonnel());
            cbMenuItem.addActionListener(evt -> {
                final boolean clanPersonnel = !person.isClanPersonnel();
                Stream.of(selected).forEach(p -> p.setClanPersonnel(clanPersonnel));
            });
            menu.add(cbMenuItem);
        }

        if (oneSelected) {
            final JCheckBoxMenuItem miCommander = new JCheckBoxMenuItem(resources.getString("miCommander.text"));
            miCommander.setToolTipText(resources.getString("miCommander.toolTipText"));
            miCommander.setName("miCommander");
            miCommander.setSelected(person.isCommander());
            miCommander.addActionListener(evt -> {
                getCampaign().getPersonnel().stream().filter(Person::isCommander).forEach(commander -> {
                    commander.setCommander(false);
                    getCampaign().addReport(String.format(resources.getString("removedCommander.format"),
                          commander.getHyperlinkedFullTitle()));
                    getCampaign().personUpdated(commander);
                });
                if (miCommander.isSelected()) {
                    person.setCommander(true);
                    getCampaign().addReport(String.format(resources.getString("setAsCommander.format"),
                          person.getHyperlinkedFullTitle()));
                    getCampaign().personUpdated(person);
                }
            });
            menu.add(miCommander);
        }

        if ((getCampaignOptions().isUseManualDivorce() || !getCampaignOptions().getRandomDivorceMethod().isNone()) &&
                  Stream.of(selected).allMatch(p -> p.getGenealogy().hasSpouse()) &&
                  Stream.of(selected).allMatch(p -> p.isDivorceable() == person.isDivorceable())) {
            cbMenuItem = new JCheckBoxMenuItem(resources.getString("miDivorceable.text"));
            cbMenuItem.setToolTipText(resources.getString("miDivorceable.toolTipText"));
            cbMenuItem.setName("miDivorceable");
            cbMenuItem.setSelected(person.isDivorceable());
            cbMenuItem.addActionListener(evt -> {
                final boolean divorceable = !person.isDivorceable();
                Stream.of(selected).forEach(p -> p.setDivorceable(divorceable));
            });
            menu.add(cbMenuItem);
        }

        if (Stream.of(selected).allMatch(p -> p.isFounder() == person.isFounder())) {
            cbMenuItem = new JCheckBoxMenuItem(resources.getString("miFounder.text"));
            cbMenuItem.setToolTipText(resources.getString("miFounder.toolTipText"));
            cbMenuItem.setName("miFounder");
            cbMenuItem.setSelected(person.isFounder());
            cbMenuItem.addActionListener(evt -> {
                final boolean founder = !person.isFounder();
                Stream.of(selected).forEach(p -> p.setFounder(founder));
            });
            menu.add(cbMenuItem);
        }

        if (Stream.of(selected).noneMatch(p -> p.getStatus().isDead()) &&
                  Stream.of(selected).allMatch(p -> p.isImmortal() == person.isImmortal())) {
            cbMenuItem = new JCheckBoxMenuItem(resources.getString("miImmortal.text"));
            cbMenuItem.setToolTipText(resources.getString("miImmortal.toolTipText"));
            cbMenuItem.setName("miImmortal");
            cbMenuItem.setSelected(person.isImmortal());
            cbMenuItem.addActionListener(evt -> {
                final boolean immortal = !person.isImmortal();
                Stream.of(selected).forEach(p -> p.setImmortal(immortal));
            });
            menu.add(cbMenuItem);
        }

        if (Stream.of(selected).allMatch(p -> p.isQuickTrainIgnore() == person.isQuickTrainIgnore())) {
            cbMenuItem = new JCheckBoxMenuItem(resources.getString("miQuickTrainIgnore.text"));
            cbMenuItem.setToolTipText(resources.getString("miQuickTrainIgnore.toolTipText"));
            cbMenuItem.setName("miQuickTrainIgnore");
            cbMenuItem.setSelected(person.isQuickTrainIgnore());
            cbMenuItem.addActionListener(evt -> {
                final boolean quickTrainIgnore = !person.isQuickTrainIgnore();
                Stream.of(selected).forEach(p -> p.setQuickTrainIgnore(quickTrainIgnore));
            });
            menu.add(cbMenuItem);
        }

        if (getCampaignOptions().isUseManualMarriages() || !getCampaignOptions().getRandomMarriageMethod().isNone()) {
            cbMenuItem = new JCheckBoxMenuItem(resources.getString("miPrefersMen.text"));
            cbMenuItem.setToolTipText(wordWrap(resources.getString("miPrefersMen.toolTipText")));
            cbMenuItem.setName("miPrefersMen");
            cbMenuItem.setSelected(selected.length == 1 && person.isPrefersMen());
            cbMenuItem.addActionListener(evt -> {
                Stream.of(selected).forEach(p -> p.setPrefersMen(!p.isPrefersMen()));
            });
            menu.add(cbMenuItem);

            cbMenuItem = new JCheckBoxMenuItem(resources.getString("miPrefersWomen.text"));
            cbMenuItem.setToolTipText(wordWrap(resources.getString("miPrefersWomen.toolTipText")));
            cbMenuItem.setName("miPrefersWomen");
            cbMenuItem.setSelected(selected.length == 1 && person.isPrefersWomen());
            cbMenuItem.addActionListener(evt -> {
                Stream.of(selected).forEach(p -> p.setPrefersWomen(!p.isPrefersWomen()));
            });
            menu.add(cbMenuItem);
        }

        if ((getCampaignOptions().isUseManualProcreation() ||
                   !getCampaignOptions().getRandomProcreationMethod().isNone()) &&
                  Stream.of(selected).allMatch(p -> p.getGender().isFemale()) &&
                  Stream.of(selected).allMatch(p -> p.isTryingToConceive() == person.isTryingToConceive())) {
            cbMenuItem = new JCheckBoxMenuItem(resources.getString("miTryingToConceive.text"));
            cbMenuItem.setToolTipText(MultiLineTooltip.splitToolTip(resources.getString("miTryingToConceive.toolTipText"),
                  100));
            cbMenuItem.setName("miTryingToConceive");
            cbMenuItem.setSelected(person.isTryingToConceive());
            cbMenuItem.addActionListener(evt -> {
                final boolean tryingToConceive = !person.isTryingToConceive();
                Stream.of(selected).forEach(p -> p.setTryingToConceive(tryingToConceive));
            });
            menu.add(cbMenuItem);
        }

        if (getCampaignOptions().isUseRandomPersonalities()) {
            cbMenuItem = new JCheckBoxMenuItem(resources.getString("miHidePersonality.text"));
            cbMenuItem.setToolTipText(MultiLineTooltip.splitToolTip(resources.getString("miHidePersonality.toolTipText"),
                  100));
            cbMenuItem.setName("miHidePersonality");
            cbMenuItem.setSelected(person.isHidePersonality());
            cbMenuItem.addActionListener(evt -> {
                final boolean hidePersonality = !person.isHidePersonality();
                Stream.of(selected).forEach(selectedPerson -> {
                    selectedPerson.setHidePersonality(hidePersonality);
                    MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                });
            });
            menu.add(cbMenuItem);
        }

        JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        // endregion Flags Menu

        // region Randomization Menu
        // This Menu contains the following options, in the specified order:
        // 1) Random Name
        // 2) Random Bloodname Check
        // 3) Random Bloodname Assignment
        // 4) Random Callsign
        // 5) Random Portrait
        // 6) Random Origin
        // 7) Random Origin Faction
        // 8) Random Origin Planet
        menu = new JMenu(resources.getString("randomizationMenu.text"));

        menuItem = new JMenuItem(resources.getString(oneSelected ?
                                                           "miRandomName.single.text" :
                                                           "miRandomName.bulk.text"));
        menuItem.setName("miRandomName");
        menuItem.setActionCommand(CMD_RANDOM_NAME);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        if (StaticChecks.areAllClanEligible(selected)) {
            menuItem = new JMenuItem(resources.getString(oneSelected ?
                                                               "miRandomBloodnameCheck.single.text" :
                                                               "miRandomBloodnameCheck.bulk.text"));
            menuItem.setName("miRandomBloodnameCheck");
            menuItem.setActionCommand(makeCommand(CMD_RANDOM_BLOODNAME, String.valueOf(false)));
            menuItem.addActionListener(this);
            menu.add(menuItem);

            if (getCampaign().isGM()) {
                menuItem = new JMenuItem(resources.getString(oneSelected ?
                                                                   "miRandomBloodname.single.text" :
                                                                   "miRandomBloodname.bulk.text"));
                menuItem.setName("miRandomBloodname");
                menuItem.setActionCommand(makeCommand(CMD_RANDOM_BLOODNAME, String.valueOf(true)));
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }
        }

        menuItem = new JMenuItem(resources.getString(oneSelected ?
                                                           "miRandomCallsign.single.text" :
                                                           "miRandomCallsign.bulk.text"));
        menuItem.setName("miRandomCallsign");
        menuItem.setActionCommand(CMD_RANDOM_CALLSIGN);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem(resources.getString(oneSelected ?
                                                           "miRandomPortrait.single.text" :
                                                           "miRandomPortrait.bulk.text"));
        menuItem.setName("miRandomPortrait");
        menuItem.setActionCommand(CMD_RANDOM_PORTRAIT);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        if (getCampaignOptions().getRandomOriginOptions().isRandomizeOrigin()) {
            menuItem = new JMenuItem(resources.getString(oneSelected ?
                                                               "miRandomOrigin.single.text" :
                                                               "miRandomOrigin.bulk.text"));
            menuItem.setName("miRandomOrigin");
            menuItem.setActionCommand(CMD_RANDOM_ORIGIN);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString(oneSelected ?
                                                               "miRandomOriginFaction.single.text" :
                                                               "miRandomOriginFaction.bulk.text"));
            menuItem.setName("miRandomOriginFaction");
            menuItem.setActionCommand(CMD_RANDOM_ORIGIN_FACTION);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString(oneSelected ?
                                                               "miRandomOriginPlanet.single.text" :
                                                               "miRandomOriginPlanet.bulk.text"));
            menuItem.setName("miRandomOriginPlanet");
            menuItem.setActionCommand(CMD_RANDOM_ORIGIN_PLANET);
            menuItem.addActionListener(this);
            menu.add(menuItem);
        }

        JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        // endregion Randomization Menu

        // region Original Unit
        menu = new JMenu(resources.getString("originalUnitMenu.text"));

        menuItem = new JMenuItem(resources.getString("originalUnitToCurrent.text"));
        menuItem.setName("originalUnitToCurrent");
        menuItem.setActionCommand(CMD_ORIGINAL_TO_CURRENT);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem(resources.getString("removeOriginalUnit.text"));
        menuItem.setName("removeOriginalUnit");
        menuItem.setActionCommand(CMD_WIPE_ORIGINAL);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        // endregion Original Unit

        // region GM Menu
        if (getCampaign().isGM()) {
            popup.addSeparator();

            menu = new JMenu(resources.getString("GMMode.text"));

            menuItem = new JMenu(resources.getString("changePrisonerStatus.text"));
            menuItem.add(newCheckboxMenu(PrisonerStatus.FREE.toString(),
                  makeCommand(CMD_CHANGE_PRISONER_STATUS, PrisonerStatus.FREE.name()),
                  (person.getPrisonerStatus() == PrisonerStatus.FREE)));
            menuItem.add(newCheckboxMenu(PrisonerStatus.PRISONER.toString(),
                  makeCommand(CMD_CHANGE_PRISONER_STATUS, PrisonerStatus.PRISONER.name()),
                  (person.getPrisonerStatus() == PrisonerStatus.PRISONER)));
            menuItem.add(newCheckboxMenu(PrisonerStatus.PRISONER_DEFECTOR.toString(),
                  makeCommand(CMD_CHANGE_PRISONER_STATUS, PrisonerStatus.PRISONER_DEFECTOR.name()),
                  (person.getPrisonerStatus() == PrisonerStatus.PRISONER_DEFECTOR)));
            menuItem.add(newCheckboxMenu(PrisonerStatus.BONDSMAN.toString(),
                  makeCommand(CMD_CHANGE_PRISONER_STATUS, PrisonerStatus.BONDSMAN.name()),
                  (person.getPrisonerStatus() == PrisonerStatus.BONDSMAN)));
            menu.add(menuItem);

            if (StaticChecks.areAllPrisoners(selected)) {
                menu.add(newMenuItem(resources.getString("ransom.text"), CMD_RANSOM));
            }

            if (Stream.of(selected).allMatch(p -> p.getStatus().isPoW())) {
                menu.add(newMenuItem(resources.getString("ransom.text"), CMD_RANSOM_FRIENDLY));
            }

            menuItem = new JMenuItem(resources.getString("removePerson.text"));
            menuItem.setActionCommand(CMD_REMOVE);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            if (oneSelected) {
                JMenu subMenu = new JMenu(resources.getString("refundSkill.text"));
                for (Skill skill : person.getSkills().getSkills()) {
                    String label = skill.getType().getName();
                    JMenuItem menuSkill = new JMenuItem(label);
                    menuSkill.setActionCommand(makeCommand(CMD_REFUND_SKILL, label));
                    menuSkill.addActionListener(this);
                    subMenu.add(menuSkill);
                }
                menu.add(subMenu);
            }

            if (!getCampaignOptions().isUseAdvancedMedical()) {
                menuItem = new JMenuItem(resources.getString("editHits.text"));
                menuItem.setActionCommand(CMD_EDIT_HITS);
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }

            menuItem = new JMenuItem(resources.getString("addXP.text"));
            menuItem.setActionCommand(CMD_ADD_XP);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("setXP.text"));
            menuItem.setActionCommand(CMD_SET_XP);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("editPerson.text"));
            menuItem.setActionCommand(CMD_EDIT);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            if (oneSelected) {
                menuItem = new JMenuItem(resources.getString("loadGMTools.text"));
                menuItem.addActionListener(evt -> loadGMToolsForPerson(person));
                menu.add(menuItem);
            }

            if (getCampaignOptions().isUseAdvancedMedical()) {
                menuItem = new JMenuItem(resources.getString("removeAllInjuries.text"));
                menuItem.setActionCommand(CMD_CLEAR_INJURIES);
                menuItem.addActionListener(this);
                menu.add(menuItem);

                if (oneSelected) {
                    for (Injury i : person.getInjuries()) {
                        menuItem = new JMenuItem(String.format(resources.getString("removeInjury.format"),
                              i.getName()));
                        menuItem.setActionCommand(makeCommand(CMD_REMOVE_INJURY, i.getUUID().toString()));
                        menuItem.addActionListener(this);
                        menu.add(menuItem);
                    }

                    menuItem = new JMenuItem(resources.getString("editInjuries.text"));
                    menuItem.setActionCommand(CMD_EDIT_INJURIES);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                }

                menuItem = new JMenuItem(resources.getString("addRandomInjury.format"));
                menuItem.setActionCommand(CMD_ADD_RANDOM_INJURY);
                menuItem.addActionListener(this);
                menu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("addRandomInjuries.format"));
                menuItem.setActionCommand(CMD_ADD_RANDOM_INJURIES);
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }

            if (getCampaignOptions().isUseManualProcreation()) {
                if (Stream.of(selected)
                          .anyMatch(p -> getCampaign().getProcreation()
                                               .canProcreate(getCampaign().getLocalDate(), p, false) == null)) {
                    menuItem = new JMenuItem(resources.getString(oneSelected ?
                                                                       "addPregnancy.text" :
                                                                       "addPregnancies.text"));
                    menuItem.setActionCommand(CMD_ADD_PREGNANCY);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                }

                if (Stream.of(selected).anyMatch(Person::isPregnant)) {
                    menuItem = new JMenuItem(resources.getString(oneSelected ?
                                                                       "removePregnancy.text" :
                                                                       "removePregnancies.text"));
                    menuItem.setActionCommand(CMD_REMOVE_PREGNANCY);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                }
            }

            if (getCampaignOptions().isUseLoyaltyModifiers()) {
                menuItem = new JMenuItem(resources.getString("regenerateLoyalty.text"));
                menuItem.setActionCommand(CMD_LOYALTY);
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }

            if (getCampaignOptions().isUseRandomPersonalities()) {
                menuItem = new JMenuItem(resources.getString("regeneratePersonality.text"));
                menuItem.setActionCommand(CMD_PERSONALITY);
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }

            if (getCampaignOptions().isUseAbilities()) {
                menuItem = new JMenuItem(resources.getString("addRandomSPA.text"));
                menuItem.setActionCommand(CMD_ADD_RANDOM_ABILITY);
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }

            menuItem = new JMenuItem(resources.getString("generateRoleplaySkills.text"));
            menuItem.setActionCommand(CMD_GENERATE_ROLEPLAY_SKILLS);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("removeRoleplaySkills.text"));
            menuItem.setActionCommand(CMD_REMOVE_ROLEPLAY_SKILLS);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            RandomSkillPreferences randomSkillPreferences = getCampaign().getRandomSkillPreferences();
            boolean isRandomizeAttributes = randomSkillPreferences.isRandomizeAttributes();

            menuItem = new JMenuItem(resources.getString("generateRoleplayAttributes." + (isRandomizeAttributes ?
                                                                                                "random" : "reset")));
            menuItem.setActionCommand(CMD_GENERATE_ROLEPLAY_ATTRIBUTES);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            boolean isRandomizeTraits = randomSkillPreferences.isRandomizeTraits();
            menuItem = new JMenuItem(resources.getString("generateRoleplayTraits." + (isRandomizeTraits ?
                                                                                            "random" : "reset")));
            menuItem.setActionCommand(CMD_GENERATE_ROLEPLAY_TRAITS);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            JMenu attributesMenu = new JMenu(resources.getString("spendOnAttributes.set"));

            for (SkillAttribute attribute : SkillAttribute.values()) {
                if (attribute.isNone()) {
                    continue;
                }

                // Set
                menuItem = new JMenuItem(attribute.getLabel());
                menuItem.setToolTipText(wordWrap(String.format(resources.getString("spendOnAttributes.tooltip"))));
                menuItem.setActionCommand(makeCommand(CMD_SET_ATTRIBUTE, String.valueOf(attribute)));
                menuItem.addActionListener(this);
                menuItem.setEnabled(getCampaign().isGM());
                attributesMenu.add(menuItem);
            }
            menu.add(attributesMenu);

            menuItem = new JMenuItem(resources.getString("generateRandomCivilianProfession.text"));
            menuItem.setToolTipText(wordWrap(String.format(resources.getString(
                  "generateRandomCivilianProfession.tooltip"))));
            menuItem.setActionCommand(makeCommand(CMD_RANDOM_PROFESSION));
            menuItem.addActionListener(this);
            menuItem.setEnabled(getCampaign().isGM());
            menu.add(menuItem);

            if (oneSelected) {
                Genealogy personGenealogy = person.getGenealogy();
                List<Person> personParents = personGenealogy.getParents();

                if (personParents.size() < 2) {
                    JMenu newParentMenu = new JMenu(resources.getString("parent.add"));
                    List<Person> potentialParents = new ArrayList<>(getCampaign().getActivePersonnel(false, true)
                                                                          .stream()
                                                                          .filter(p -> (!p.isChild(getCampaign().getLocalDate())))
                                                                          .toList());
                    potentialParents.removeAll(personParents);
                    potentialParents.remove(person);

                    for (final Person newParent : potentialParents) {
                        String status = getPersonOptionString(newParent);

                        JMenuItem newParentItem = new JMenuItem(status);
                        newParentItem.setActionCommand(makeCommand(CMD_ADD_PARENT, String.valueOf(newParent.getId())));
                        newParentItem.addActionListener(this);
                        newParentMenu.add(newParentItem);
                    }
                    if (newParentMenu.getItemCount() > 0) {
                        menu.add(newParentMenu);
                    }
                }

                JMenu removeParentMenu = new JMenu(resources.getString("parent.remove"));
                for (final Person oldParent : personParents) {
                    String status = getPersonOptionString(oldParent);

                    JMenuItem removeParentItem = new JMenuItem(status);
                    removeParentItem.setActionCommand(makeCommand(CMD_REMOVE_PARENT,
                          String.valueOf(oldParent.getId())));
                    removeParentItem.addActionListener(this);
                    removeParentMenu.add(removeParentItem);
                }
                if (removeParentMenu.getItemCount() > 0) {
                    menu.add(removeParentMenu);
                }

                JMenu newChildMenu = new JMenu(resources.getString("child.add"));
                List<Person> potentialChildren = new ArrayList<>(getCampaign().getActivePersonnel(false, true)
                                                                       .stream()
                                                                       .filter(p -> (p.getGenealogy()
                                                                                           .getParents()
                                                                                           .size() < 2))
                                                                       .toList());
                potentialChildren.removeAll(personParents);
                potentialChildren.removeAll(personGenealogy.getChildren());
                potentialChildren.remove(person);

                for (final Person newChild : potentialChildren) {
                    String status = getPersonOptionString(newChild);

                    JMenuItem newChildItem = new JMenuItem(status);
                    newChildItem.setActionCommand(makeCommand(CMD_ADD_CHILD, String.valueOf(newChild.getId())));
                    newChildItem.addActionListener(this);
                    newChildMenu.add(newChildItem);
                }
                if (newChildMenu.getItemCount() > 0) {
                    menu.add(newChildMenu);
                }

                JMenu removeChildMenu = new JMenu(resources.getString("child.remove"));
                for (final Person oldChild : personGenealogy.getChildren()) {
                    String status = getPersonOptionString(oldChild);

                    JMenuItem removeChildItem = new JMenuItem(status);
                    removeChildItem.setActionCommand(makeCommand(CMD_REMOVE_CHILD, String.valueOf(oldChild.getId())));
                    removeChildItem.addActionListener(this);
                    removeChildMenu.add(removeChildItem);
                }
                if (removeChildMenu.getItemCount() > 0) {
                    menu.add(removeChildMenu);
                }
            }

            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        }
        // endregion GM Menu

        return Optional.of(popup);
    }

    /**
     * Generates a formatted description string for a person, typically used in adoption scenarios.
     *
     * <p>This method creates a localized string containing the person's full name, gender, and current age.</p>
     *
     * @param person The {@link Person} to generate the description for
     *
     * @return A formatted string describing the person with their name, gender, and age
     *
     * @author Illiani
     * @since 0.50.10
     */
    private String getPersonOptionString(Person person) {
        return String.format(resources.getString("personOption.description"),
              person.getFullName(),
              person.getGender(),
              person.getAge(getCampaign().getLocalDate()));
    }

    private static void placeInAppropriateSPASubMenu(SpecialAbility spa, JMenu specialistMenu, JMenu combatAbilityMenu,
          JMenu maneuveringAbilityMenu, JMenu characterFlawMenu, JMenu utilityAbilityMenu, JMenu characterOriginMenu) {
        AbilityCategory category = getSpaCategory(spa);

        switch (category) {
            case COMBAT_ABILITY -> combatAbilityMenu.add(specialistMenu);
            case MANEUVERING_ABILITY -> maneuveringAbilityMenu.add(specialistMenu);
            case UTILITY_ABILITY -> utilityAbilityMenu.add(specialistMenu);
            case CHARACTER_FLAW -> characterFlawMenu.add(specialistMenu);
            case CHARACTER_CREATION_ONLY -> characterOriginMenu.add(specialistMenu);
        }
    }

    /**
     * Builds the education menu when only one person is selected
     *
     * @param campaign     the campaign to check parameters against
     * @param person       the person to check parameters against
     * @param academy      the academy to build menus for
     * @param militaryMenu the military menu object
     * @param civilianMenu the civilian menu object
     */
    private void buildEducationMenusSingleton(Campaign campaign, Person person, Academy academy, JMenu militaryMenu,
          JMenu civilianMenu) {
        boolean showIneligibleAcademies = campaign.getCampaignOptions().isEnableShowIneligibleAcademies();
        if (campaign.getCampaignOptions().isEnableOverrideRequirements()) {
            JMenu academyOption = new JMenu(academy.getName());

            String campus;

            if ((academy.isLocal()) || (academy.isHomeSchool())) {
                campus = campaign.getCurrentSystem().getId();
            } else {
                campus = academy.getLocationSystems().get(0);
            }

            educationJMenuAdder(academy, militaryMenu, civilianMenu, academyOption);

            List<String> academyFactions = campaign.getSystemById(campus).getFactions(campaign.getLocalDate());

            // in the event the location has no faction, we use the campaign faction.
            // this is only relevant if we're overriding the academy restrictions, as we
            // won't reach this point during normal play.
            if (academyFactions.isEmpty()) {
                buildEducationSubMenus(campaign,
                      academy,
                      List.of(person),
                      academyOption,
                      campus,
                      campaign.getFaction().getShortName());
            } else {
                buildEducationSubMenus(campaign,
                      academy,
                      List.of(person),
                      academyOption,
                      campus,
                      campaign.getSystemById(campus).getFactions(campaign.getLocalDate()).get(0));
            }
            return;
        }

        // has the academy been constructed, is still standing, & has not closed?
        if ((campaign.getGameYear() >= academy.getConstructionYear()) &&
                  (campaign.getGameYear() < academy.getDestructionYear()) &&
                  (campaign.getGameYear() < academy.getClosureYear())) {
            // is the planet populated?
            if ((academy.isLocal()) && (campaign.getCurrentSystem().getPopulation(campaign.getLocalDate()) == 0)) {
                if (showIneligibleAcademies) {
                    JMenuItem academyOption = new JMenuItem("<html>" +
                                                                  academy.getName() +
                                                                  resources.getString("eduPopulationConflict.text") +
                                                                  "</html>");

                    educationJMenuItemAdder(academy, militaryMenu, civilianMenu, academyOption);
                }
                // is the applicant within the right age bracket?
            } else if ((person.getAge(campaign.getLocalDate()) >= academy.getAgeMax()) ||
                             (person.getAge(campaign.getLocalDate()) < academy.getAgeMin())) {
                if (showIneligibleAcademies) {
                    JMenuItem academyOption;

                    if (academy.getAgeMax() != 9999) {
                        academyOption = new JMenuItem("<html>" +
                                                            academy.getName() +
                                                            String.format(resources.getString("eduAgeConflictRange.text"),
                                                                  academy.getAgeMin(),
                                                                  academy.getAgeMax()) +
                                                            "</html>");
                    } else {
                        academyOption = new JMenuItem("<html>" +
                                                            academy.getName() +
                                                            String.format(resources.getString("eduAgeConflictPlus.text"),
                                                                  academy.getAgeMin()));
                    }

                    educationJMenuItemAdder(academy, militaryMenu, civilianMenu, academyOption);
                }
                // is the applicant qualified?
            } else if (!academy.isQualified(person)) {
                if (showIneligibleAcademies) {
                    JMenuItem academyOption = new JMenuItem("<html>" +
                                                                  academy.getName() +
                                                                  String.format(resources.getString(
                                                                              "eduUnqualified.text"),
                                                                        academy.getEducationLevelMin()));
                    educationJMenuItemAdder(academy, militaryMenu, civilianMenu, academyOption);
                }
            } else if (academy.hasRejectedApplication(person)) {
                if (showIneligibleAcademies) {
                    JMenuItem academyOption = new JMenuItem("<html>" +
                                                                  academy.getName() +
                                                                  String.format(resources.getString("eduRejected.text"),
                                                                        academy.getEducationLevelMin()));
                    educationJMenuItemAdder(academy, militaryMenu, civilianMenu, academyOption);
                }
            } else if (academy.isLocal()) {
                // are any of the local academies accepting applicants from person's Faction or
                // campaign's Faction?
                String faction = academy.getFilteredFaction(campaign,
                      person,
                      campaign.getSystemById(campaign.getCurrentSystem().getId()).getFactions(campaign.getLocalDate()));

                if (faction == null) {
                    if (showIneligibleAcademies) {
                        JMenuItem academyOption = new JMenuItem("<html>" +
                                                                      academy.getName() +
                                                                      resources.getString("eduFactionConflict.text"));

                        educationJMenuItemAdder(academy, militaryMenu, civilianMenu, academyOption);
                    }
                } else {
                    JMenu academyOption = new JMenu(academy.getName());
                    educationJMenuAdder(academy, militaryMenu, civilianMenu, academyOption);

                    buildEducationSubMenus(campaign,
                          academy,
                          List.of(person),
                          academyOption,
                          campaign.getCurrentSystem().getId(),
                          faction);
                }
            } else if (academy.isHomeSchool()) {
                JMenu academyOption = new JMenu(academy.getName());
                educationJMenuAdder(academy, militaryMenu, civilianMenu, academyOption);

                buildEducationSubMenus(campaign,
                      academy,
                      List.of(person),
                      academyOption,
                      campaign.getCurrentSystem().getId(),
                      campaign.getFaction().getShortName());
            } else {
                // what campuses are accepting applicants?
                List<String> campuses = new ArrayList<>();

                for (String campusId : academy.getLocationSystems()) {
                    PlanetarySystem system = campaign.getSystemById(campusId);

                    if (academy.getFilteredFaction(campaign, person, system.getFactions(campaign.getLocalDate())) !=
                              null) {
                        campuses.add(campusId);
                    }
                }

                if (campuses.isEmpty()) {
                    if (showIneligibleAcademies) {
                        JMenuItem academyOption = new JMenuItem("<html>" +
                                                                      academy.getName() +
                                                                      resources.getString("eduFactionConflict.text"));
                        educationJMenuItemAdder(academy, militaryMenu, civilianMenu, academyOption);
                    }
                    // which is the nearest campus and is it in range?
                } else {
                    String nearestCampus = Academy.getNearestCampus(campaign, campuses);

                    if ((campaign.getSimplifiedTravelTime(campaign.getSystemById(nearestCampus)) / 7) >
                              campaign.getCampaignOptions().getMaximumJumpCount()) {
                        if (showIneligibleAcademies) {
                            JMenuItem academyOption = new JMenuItem("<html>" +
                                                                          academy.getName() +
                                                                          resources.getString("eduRangeConflict.text"));
                            educationJMenuItemAdder(academy, militaryMenu, civilianMenu, academyOption);
                        }
                    } else {
                        String faction = academy.getFilteredFaction(campaign,
                              person,
                              campaign.getSystemById(nearestCampus).getFactions(campaign.getLocalDate()));

                        if (faction != null) {
                            JMenu academyOption = new JMenu(academy.getName());

                            educationJMenuAdder(academy, militaryMenu, civilianMenu, academyOption);

                            buildEducationSubMenus(campaign,
                                  academy,
                                  List.of(person),
                                  academyOption,
                                  nearestCampus,
                                  faction);
                        }
                    }
                }
            }
        }
    }

    /**
     * Builds the education menu when multiple people are
     *
     * @param campaign     the campaign to check parameters against
     * @param personnel    the people to check parameters against
     * @param academy      the academy to build menus for
     * @param militaryMenu the military menu object
     * @param civilianMenu the civilian menu object
     */
    private void buildEducationMenusMassEnroll(Campaign campaign, List<Person> personnel, Academy academy,
          JMenu militaryMenu, JMenu civilianMenu) {
        if (campaign.getCampaignOptions().isEnableOverrideRequirements()) {
            JMenu academyOption = new JMenu(academy.getName());

            String campus;

            if ((academy.isLocal()) || (academy.isHomeSchool())) {
                campus = campaign.getCurrentSystem().getId();
            } else {
                campus = academy.getLocationSystems().get(0);
            }

            educationJMenuAdder(academy, militaryMenu, civilianMenu, academyOption);

            List<String> academyFactions = campaign.getSystemById(campus).getFactions(campaign.getLocalDate());

            if (academyFactions.isEmpty()) {
                buildEducationSubMenus(campaign,
                      academy,
                      personnel,
                      academyOption,
                      campus,
                      campaign.getFaction().getShortName());
            } else {
                buildEducationSubMenus(campaign,
                      academy,
                      personnel,
                      academyOption,
                      campus,
                      campaign.getSystemById(campus).getFactions(campaign.getLocalDate()).get(0));
            }
            return;
        }

        // has the academy been constructed, is still standing, & has not closed?
        if ((campaign.getGameYear() >= academy.getConstructionYear()) &&
                  (campaign.getGameYear() < academy.getDestructionYear()) &&
                  (campaign.getGameYear() < academy.getClosureYear())) {

            // is the planet populated?
            if ((campaign.getCurrentSystem().getPopulation(campaign.getLocalDate()) == 0) &&
                      (!academy.isHomeSchool())) {
                return;
            }

            // are all the applicants within the right age bracket?
            // are all the applicants qualified?
            boolean arePersonnelEligible = personnel.stream()
                                                 .allMatch(person -> person.getAge(campaign.getLocalDate()) <
                                                                           academy.getAgeMax() &&
                                                                           person.getAge(campaign.getLocalDate()) >=
                                                                                 academy.getAgeMin() &&
                                                                           academy.isQualified(person) &&
                                                                           !academy.hasRejectedApplication(person));

            // if one or more people are not eligible to attend the academy,
            // there is no point doing any further processes
            if (!arePersonnelEligible) {
                return;
            }

            if (academy.isLocal()) {
                // find the first faction that accepts applications from all persons in
                // personnel
                Optional<String> suitableFaction = personnel.stream()
                                                         .map(person -> academy.getFilteredFaction(campaign,
                                                               person,
                                                               campaign.getCurrentSystem()
                                                                     .getFactions(campaign.getLocalDate())))
                                                         .filter(faction -> personnel.stream()
                                                                                  .allMatch(person -> Objects.equals(
                                                                                        faction,
                                                                                        academy.getFilteredFaction(
                                                                                              campaign,
                                                                                              person,
                                                                                              campaign.getCurrentSystem()
                                                                                                    .getFactions(
                                                                                                          campaign.getLocalDate())))))
                                                         .distinct()
                                                         .filter(Objects::nonNull)
                                                         .findFirst();

                if (suitableFaction.isPresent()) {
                    JMenu academyOption = new JMenu(academy.getName());
                    educationJMenuAdder(academy, militaryMenu, civilianMenu, academyOption);

                    buildEducationSubMenus(campaign,
                          academy,
                          personnel,
                          academyOption,
                          campaign.getCurrentSystem().getId(),
                          suitableFaction.get());
                }
            } else if (academy.isHomeSchool()) {
                JMenu academyOption = new JMenu(academy.getName());
                educationJMenuAdder(academy, militaryMenu, civilianMenu, academyOption);

                buildEducationSubMenus(campaign,
                      academy,
                      personnel,
                      academyOption,
                      campaign.getCurrentSystem().getId(),
                      campaign.getFaction().getShortName());
            } else {
                // find the campuses that accept applications from all members of the group
                List<String> suitableCampuses = personnel.stream()
                                                      .flatMap(person -> academy.getLocationSystems()
                                                                               .stream()
                                                                               .filter(campus -> academy.getFilteredFaction(
                                                                                     campaign,
                                                                                     person,
                                                                                     campaign.getSystemById(campus)
                                                                                           .getFactions(campaign.getLocalDate())) !=
                                                                                                       null))
                                                      .distinct()
                                                      .filter(campus -> personnel.stream()
                                                                              .allMatch(person -> academy.getFilteredFaction(
                                                                                    campaign,
                                                                                    person,
                                                                                    campaign.getSystemById(campus)
                                                                                          .getFactions(campaign.getLocalDate())) !=
                                                                                                        null))
                                                      .collect(Collectors.toList());

                if (!suitableCampuses.isEmpty()) {
                    String nearestCampus = Academy.getNearestCampus(campaign, suitableCampuses);

                    // find what factions accept an application from all members of the group
                    Optional<String> suitableFaction = personnel.stream()
                                                             .map(person -> academy.getFilteredFaction(campaign,
                                                                   person,
                                                                   campaign.getSystemById(nearestCampus)
                                                                         .getFactions(campaign.getLocalDate())))
                                                             .distinct()
                                                             .filter(faction -> personnel.stream()
                                                                                      .allMatch(person -> faction.equals(
                                                                                            academy.getFilteredFaction(
                                                                                                  campaign,
                                                                                                  person,
                                                                                                  campaign.getSystemById(
                                                                                                              nearestCampus)
                                                                                                        .getFactions(
                                                                                                              campaign.getLocalDate())))))
                                                             .findFirst();

                    if (suitableFaction.isPresent()) {
                        if ((campaign.getSimplifiedTravelTime(campaign.getSystemById(nearestCampus)) / 7) <=
                                  campaign.getCampaignOptions().getMaximumJumpCount()) {
                            JMenu academyOption = new JMenu(academy.getName());
                            educationJMenuAdder(academy, militaryMenu, civilianMenu, academyOption);

                            buildEducationSubMenus(campaign,
                                  academy,
                                  personnel,
                                  academyOption,
                                  nearestCampus,
                                  suitableFaction.get());
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds an education option to the appropriate JMenu based on the type of Academy. This version accepts JMenu
     * objects.
     *
     * @param academy      the Academy
     * @param militaryMenu the JMenu for military education options
     * @param civilianMenu the JMenu for civilian education options
     * @param option       the option to be added to the appropriate JMenu
     */
    private static void educationJMenuAdder(Academy academy, JMenu militaryMenu, JMenu civilianMenu, JMenu option) {
        if (academy.isMilitary()) {
            militaryMenu.add(option);
        } else {
            civilianMenu.add(option);
        }
    }

    /**
     * Adds an education option to the appropriate JMenu based on the type of Academy. This version accepts JMenuItem
     * objects.
     *
     * @param academy      the Academy
     * @param militaryMenu the JMenu for military education options
     * @param civilianMenu the JMenu for civilian education options
     * @param option       the option to be added to the appropriate JMenu
     */
    private static void educationJMenuItemAdder(Academy academy, JMenu militaryMenu, JMenu civilianMenu,
          JMenuItem option) {
        if (academy.isMilitary()) {
            militaryMenu.add(option);
        } else {
            civilianMenu.add(option);
        }
    }

    private void buildEducationSubMenus(Campaign campaign, Academy academy, List<Person> personnel, JMenu academyOption,
          String campus, String faction) {
        JMenuItem courses;
        int courseCount = academy.getQualifications().size();

        if (courseCount > 0) {
            for (int courseIndex = 0; courseIndex < (courseCount); courseIndex++) {
                // we also need to make sure the course is being offered
                if ((campaign.getCampaignOptions().isEnableOverrideRequirements()) ||
                          (campaign.getGameYear() >= academy.getQualificationStartYears().get(courseIndex))) {
                    String course = academy.getQualifications().get(courseIndex);
                    courses = new JMenuItem(course);

                    if ((academy.isLocal()) || (academy.isHomeSchool())) {
                        courses.setToolTipText(academy.getTooltip(campaign,
                              personnel,
                              courseIndex,
                              campaign.getCurrentSystem()));
                        courses.setActionCommand(makeCommand(CMD_BEGIN_EDUCATION_ENROLLMENT,
                              academy.getSet(),
                              academy.getName(),
                              String.valueOf(courseIndex),
                              campaign.getCurrentSystem().getId(),
                              faction));
                    } else {
                        courses.setToolTipText(academy.getTooltip(campaign,
                              personnel,
                              courseIndex,
                              campaign.getSystemById(campus)));
                        courses.setActionCommand(makeCommand(CMD_BEGIN_EDUCATION_ENROLLMENT,
                              academy.getSet(),
                              academy.getName(),
                              String.valueOf(courseIndex),
                              campus,
                              faction));
                    }
                    courses.addActionListener(this);
                    academyOption.add(courses);
                }
            }
        } else {
            courses = new JMenuItem(resources.getString("eduNoQualificationsOffered.text"));
            academyOption.add(courses);
        }
    }

    /**
     * Returns a JMenuItem for a given Award.
     *
     * @param award The Award object for which the JMenuItem is to be created.
     *
     * @return A JMenuItem representing the given Award.
     */
    private JMenuItem getAwardMenuItem(Award award) {
        StringBuilder awardMenuItem = new StringBuilder();
        awardMenuItem.append(String.format("%s", award.getName()));

        if (getCampaignOptions().getAwardBonusStyle().isBoth()) {
            if ((award.getXPReward() != 0) || (award.getEdgeReward() != 0)) {
                awardMenuItem.append(" (");

                if (award.getXPReward() != 0) {
                    awardMenuItem.append(award.getXPReward()).append(" XP");
                    if (award.getEdgeReward() != 0) {
                        awardMenuItem.append(" & ");
                    }
                }

                if (award.getEdgeReward() != 0) {
                    awardMenuItem.append(award.getEdgeReward()).append(" Edge");
                }

                awardMenuItem.append(')');
            }
        } else if (getCampaignOptions().getAwardBonusStyle().isXP()) {
            if (award.getXPReward() != 0) {
                awardMenuItem.append(" (");

                awardMenuItem.append(award.getXPReward()).append(" XP)");
            }
        } else if (getCampaignOptions().getAwardBonusStyle().isEdge()) {

            if (award.getEdgeReward() != 0) {
                awardMenuItem.append(" (");

                awardMenuItem.append(award.getEdgeReward()).append(" Edge)");
            }
        }

        JMenuItem menuItem = new JMenuItem(awardMenuItem.toString());
        menuItem.setToolTipText(MultiLineTooltip.splitToolTip(award.getDescription()));
        menuItem.setActionCommand(makeCommand(CMD_ADD_AWARD, award.getSet(), award.getName()));
        menuItem.addActionListener(this);
        return menuItem;
    }

    private JMenuItem newMenuItem(String text, String command) {
        JMenuItem result = new JMenuItem(text);
        result.setActionCommand(command);
        result.addActionListener(this);
        return result;
    }

    private JCheckBoxMenuItem newCheckboxMenu(String text, String command, boolean selected) {
        JCheckBoxMenuItem result = new JCheckBoxMenuItem(text);
        result.setSelected(selected);
        result.setActionCommand(command);
        result.addActionListener(this);
        result.setEnabled(true);
        return result;
    }
}
