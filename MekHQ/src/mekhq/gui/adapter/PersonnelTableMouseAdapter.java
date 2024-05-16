/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.adapter;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.dialogs.PortraitChooserDialog;
import megamek.common.Crew;
import megamek.common.Mounted;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Kill;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.event.PersonLogEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.*;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.personnel.generator.SingleSpecialAbilityGenerator;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.gui.CampaignGUI;
import mekhq.gui.PersonnelTab;
import mekhq.gui.dialog.*;
import mekhq.gui.displayWrappers.RankDisplay;
import mekhq.gui.menus.AssignPersonToUnitMenu;
import mekhq.gui.model.PersonnelTableModel;
import mekhq.gui.utilities.JMenuHelpers;
import mekhq.gui.utilities.MultiLineTooltip;
import mekhq.gui.utilities.StaticChecks;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PersonnelTableMouseAdapter extends JPopupMenuAdapter {
    //region Variable Declarations
    private static final String CMD_RANKSYSTEM = "RANKSYSTEM";
    private static final String CMD_RANK = "RANK";
    private static final String CMD_MANEI_DOMINI_RANK = "MD_RANK";
    private static final String CMD_MANEI_DOMINI_CLASS = "MD_CLASS";
    private static final String CMD_PRIMARY_ROLE = "PROLE";
    private static final String CMD_SECONDARY_ROLE = "SROLE";
    private static final String CMD_PRIMARY_DESIGNATOR = "DESIG_PRI";
    private static final String CMD_SECONDARY_DESIGNATOR = "DESIG_SEC";
    private static final String CMD_ADD_AWARD = "ADD_AWARD";
    private static final String CMD_RMV_AWARD = "RMV_AWARD";

    private static final String CMD_EDIT_SALARY = "SALARY";
    private static final String CMD_GIVE_PAYMENT = "GIVE_PAYMENT";
    private static final String CMD_EDIT_INJURIES = "EDIT_INJURIES";
    private static final String CMD_REMOVE_INJURY = "REMOVE_INJURY";
    private static final String CMD_CLEAR_INJURIES = "CLEAR_INJURIES";
    private static final String CMD_CALLSIGN = "CALLSIGN";
    private static final String CMD_EDIT_PERSONNEL_LOG = "LOG";
    private static final String CMD_ADD_LOG_ENTRY = "ADD_PERSONNEL_LOG_SINGLE";
    private static final String CMD_EDIT_SCENARIO_LOG = "SCENARIO_LOG";
    private static final String CMD_ADD_SCENARIO_ENTRY = "ADD_SCENARIO_ENTRY";
    private static final String CMD_EDIT_KILL_LOG = "KILL_LOG";
    private static final String CMD_ADD_KILL = "ADD_KILL";
    private static final String CMD_BUY_EDGE = "EDGE_BUY";
    private static final String CMD_SET_EDGE = "EDGE_SET";
    private static final String CMD_SET_XP = "XP_SET";
    private static final String CMD_ADD_1_XP = "XP_ADD_1";
    private static final String CMD_ADD_XP = "XP_ADD";
    private static final String CMD_EDIT_BIOGRAPHY = "BIOGRAPHY";
    private static final String CMD_EDIT_PORTRAIT = "PORTRAIT";
    private static final String CMD_EDIT_HITS = "EDIT_HITS";
    private static final String CMD_EDIT = "EDIT";
    private static final String CMD_SACK = "SACK";
    private static final String CMD_REMOVE = "REMOVE";
    private static final String CMD_EDGE_TRIGGER = "EDGE";
    private static final String CMD_CHANGE_PRISONER_STATUS = "PRISONER_STATUS";
    private static final String CMD_CHANGE_STATUS = "STATUS";
    private static final String CMD_ACQUIRE_SPECIALIST = "SPECIALIST";
    private static final String CMD_ACQUIRE_WEAPON_SPECIALIST = "WSPECIALIST";
    private static final String CMD_ACQUIRE_SANDBLASTER = "SANDBLASTER";
    private static final String CMD_ACQUIRE_RANGEMASTER = "RANGEMASTER";
    private static final String CMD_ACQUIRE_ENVSPEC = "ENVSPEC";
    private static final String CMD_ACQUIRE_HUMANTRO = "HUMANTRO";
    private static final String CMD_ACQUIRE_ABILITY = "ABILITY";
    private static final String CMD_ACQUIRE_CUSTOM_CHOICE = "CUSTOM_CHOICE";
    private static final String CMD_IMPROVE = "IMPROVE";
    private static final String CMD_ADD_SPOUSE = "SPOUSE";
    private static final String CMD_REMOVE_SPOUSE = "REMOVE_SPOUSE";
    private static final String CMD_ADD_PREGNANCY = "ADD_PREGNANCY";
    private static final String CMD_REMOVE_PREGNANCY = "PREGNANCY_SPOUSE";

    private static final String CMD_IMPRISON = "IMPRISON";
    private static final String CMD_FREE = "FREE";
    private static final String CMD_RECRUIT = "RECRUIT";
    private static final String CMD_RANSOM = "RANSOM";

    // MechWarrior Edge Options
    private static final String OPT_EDGE_MASC_FAILURE = "edge_when_masc_fails";
    private static final String OPT_EDGE_EXPLOSION = "edge_when_explosion";
    private static final String OPT_EDGE_KO = "edge_when_ko";
    private static final String OPT_EDGE_TAC = "edge_when_tac";
    private static final String OPT_EDGE_HEADHIT = "edge_when_headhit";

    // Aero Edge Options
    private static final String OPT_EDGE_WHEN_AERO_ALT_LOSS= "edge_when_aero_alt_loss";
    private static final String OPT_EDGE_WHEN_AERO_EXPLOSION= "edge_when_aero_explosion";
    private static final String OPT_EDGE_WHEN_AERO_KO= "edge_when_aero_ko";
    private static final String OPT_EDGE_WHEN_AERO_LUCKY_CRIT= "edge_when_aero_lucky_crit";
    private static final String OPT_EDGE_WHEN_AERO_NUKE_CRIT= "edge_when_aero_nuke_crit";
    private static final String OPT_EDGE_WHEN_AERO_UNIT_CARGO_LOST= "edge_when_aero_unit_cargo_lost";

    //region Randomization Menu
    private static final String CMD_RANDOM_NAME = "RANDOM_NAME";
    private static final String CMD_RANDOM_BLOODNAME = "RANDOM_BLOODNAME";
    private static final String CMD_RANDOM_CALLSIGN = "RANDOM_CALLSIGN";
    private static final String CMD_RANDOM_PORTRAIT = "RANDOM_PORTRAIT";
    private static final String CMD_RANDOM_ORIGIN = "RANDOM_ORIGIN";
    private static final String CMD_RANDOM_ORIGIN_FACTION = "RANDOM_ORIGIN_FACTION";
    private static final String CMD_RANDOM_ORIGIN_PLANET = "RANDOM_ORIGIN_PLANET";
    //endregion Randomization Menu

    private static final String SEPARATOR = "@";
    private static final String TRUE = String.valueOf(true);
    private static final String FALSE = String.valueOf(false);

    private final CampaignGUI gui;
    private final JTable personnelTable;
    private final PersonnelTableModel personnelModel;

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    protected PersonnelTableMouseAdapter(CampaignGUI gui, JTable personnelTable,
                                         PersonnelTableModel personnelModel) {
        this.gui = gui;
        this.personnelTable = personnelTable;
        this.personnelModel = personnelModel;
    }

    public static void connect(CampaignGUI gui, JTable personnelTable,
            PersonnelTableModel personnelModel, JSplitPane splitPersonnel) {
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

    private String makeCommand(String ... parts) {
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
            case CMD_RANKSYSTEM: {
                final RankSystem rankSystem = Ranks.getRankSystemFromCode(data[1]);
                final RankValidator rankValidator = new RankValidator();
                for (final Person person : people) {
                    person.setRankSystem(rankValidator, rankSystem);
                }
                break;
            }
            case CMD_RANK: {
                try {
                    final int rank = Integer.parseInt(data[1]);
                    final int level = (data.length > 2) ? Integer.parseInt(data[2]) : 0;
                    for (final Person person : people) {
                        person.changeRank(gui.getCampaign(), rank, level, true);
                    }
                } catch (Exception e) {
                    LogManager.getLogger().error("", e);
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
                    LogManager.getLogger().error("Failed to assign Manei Domini Class", e);
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
                    LogManager.getLogger().error("Failed to assign ROM designator", e);
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
                    LogManager.getLogger().error("Failed to assign ROM secondary designator", e);
                }
                break;
            }
            case CMD_PRIMARY_ROLE: {
                PersonnelRole role = PersonnelRole.valueOf(data[1]);
                for (final Person person : people) {
                    person.setPrimaryRole(gui.getCampaign(), role);
                    gui.getCampaign().personUpdated(person);
                    if (gui.getCampaign().getCampaignOptions().isUsePortraitForRole(role)
                            && gui.getCampaign().getCampaignOptions().isAssignPortraitOnRoleChange()
                            && person.getPortrait().hasDefaultFilename()) {
                        gui.getCampaign().assignRandomPortraitFor(person);
                    }
                }
                break;
            }
            case CMD_SECONDARY_ROLE: {
                PersonnelRole role = PersonnelRole.valueOf(data[1]);
                for (final Person person : people) {
                    person.setSecondaryRole(role);
                    gui.getCampaign().personUpdated(person);
                }
                break;
            }
            case CMD_ADD_PREGNANCY: {
                Stream.of(people)
                        .filter(person -> (gui.getCampaign().getProcreation().canProcreate(
                                gui.getCampaign().getLocalDate(), person, false) == null))
                        .forEach(person -> {
                            gui.getCampaign().getProcreation().addPregnancy(
                                    gui.getCampaign(), gui.getCampaign().getLocalDate(), person);
                            MekHQ.triggerEvent(new PersonChangedEvent(person));
                });
                break;
            }
            case CMD_REMOVE_PREGNANCY: {
                Stream.of(people).filter(Person::isPregnant).forEach(person -> {
                    gui.getCampaign().getProcreation().removePregnancy(person);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                });
                break;
            }
            case CMD_REMOVE_SPOUSE: {
                Stream.of(people)
                        .filter(person -> gui.getCampaign().getDivorce().canDivorce(person, false) == null)
                        .forEach(person -> gui.getCampaign().getDivorce().divorce(gui.getCampaign(),
                                gui.getCampaign().getLocalDate(), person,
                                SplittingSurnameStyle.valueOf(data[1])));
            }
            case CMD_ADD_SPOUSE: {
                gui.getCampaign().getMarriage().marry(gui.getCampaign(),
                        gui.getCampaign().getLocalDate(), selectedPerson,
                        gui.getCampaign().getPerson(UUID.fromString(data[1])),
                        MergingSurnameStyle.valueOf(data[2]));
                break;
            }
            case CMD_ADD_AWARD: {
                for (Person person : people) {
                    person.getAwardController().addAndLogAward(gui.getCampaign(), data[1], data[2],
                            gui.getCampaign().getLocalDate());
                }
                break;
            }
            case CMD_RMV_AWARD: {
                for (Person person : people) {
                    try {
                        if (person.getAwardController().hasAward(data[1], data[2])) {
                            person.getAwardController().removeAward(data[1], data[2],
                                    (data.length > 3)
                                            ? MekHQ.getMHQOptions().parseDisplayFormattedDate(data[3])
                                            : null,
                                    gui.getCampaign().getLocalDate());
                        }
                    } catch (Exception e) {
                        LogManager.getLogger().error("Could not remove award.", e);
                    }
                }
                break;
            }
            case CMD_IMPROVE: {
                String type = data[1];
                int cost = Integer.parseInt(data[2]);
                int oldExpLevel = selectedPerson.getExperienceLevel(gui.getCampaign(), false);
                selectedPerson.improveSkill(type);
                selectedPerson.spendXP(cost);

                PersonalLogger.improvedSkill(gui.getCampaign(), selectedPerson,
                        gui.getCampaign().getLocalDate(), selectedPerson.getSkill(type).getType().getName(),
                        selectedPerson.getSkill(type).toString());
                gui.getCampaign().addReport(String.format(resources.getString("improved.format"),
                        selectedPerson.getHyperlinkedName(), type));

                if (gui.getCampaign().getCampaignOptions().isUseAtB()
                        && gui.getCampaign().getCampaignOptions().isUseAbilities()) {
                    if (selectedPerson.getPrimaryRole().isCombat()
                            && (selectedPerson.getExperienceLevel(gui.getCampaign(), false) > oldExpLevel)
                            && (oldExpLevel >= SkillType.EXP_REGULAR)) {
                        SingleSpecialAbilityGenerator spaGenerator = new SingleSpecialAbilityGenerator();
                        String spa = spaGenerator.rollSPA(gui.getCampaign(), selectedPerson);
                        if (spa == null) {
                            if (gui.getCampaign().getCampaignOptions().isUseEdge()) {
                                selectedPerson.changeEdge(1);
                                selectedPerson.changeCurrentEdge(1);
                                PersonalLogger.gainedEdge(gui.getCampaign(), selectedPerson,
                                        gui.getCampaign().getLocalDate());
                                gui.getCampaign().addReport(String.format(resources.getString("gainedEdge.format"),
                                        selectedPerson.getHyperlinkedName()));
                            }
                        } else {
                            PersonalLogger.gainedSPA(gui.getCampaign(), selectedPerson,
                                    gui.getCampaign().getLocalDate(), spa);
                            gui.getCampaign().addReport(String.format(resources.getString("gained.format"),
                                    selectedPerson.getHyperlinkedName(), spa));
                        }
                    }
                }
                gui.getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_ABILITY: {
                String selected = data[1];
                int cost = Integer.parseInt(data[2]);
                selectedPerson.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES,
                        selected, true);
                selectedPerson.spendXP(cost);
                final String displayName = SpecialAbility.getDisplayName(selected);
                PersonalLogger.gainedSPA(gui.getCampaign(), selectedPerson,
                        gui.getCampaign().getLocalDate(), displayName);
                gui.getCampaign().addReport(String.format(resources.getString("gained.format"),
                        selectedPerson.getHyperlinkedName(), displayName));
                gui.getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_WEAPON_SPECIALIST: {
                String selected = data[1];
                int cost = Integer.parseInt(data[2]);
                selectedPerson.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES,
                        OptionsConstants.GUNNERY_WEAPON_SPECIALIST, selected);
                selectedPerson.spendXP(cost);
                final String displayName = String.format("%s %s",
                        SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_WEAPON_SPECIALIST), selected);
                PersonalLogger.gainedSPA(gui.getCampaign(), selectedPerson,
                        gui.getCampaign().getLocalDate(), displayName);
                gui.getCampaign().addReport(String.format(resources.getString("gained.format"),
                        selectedPerson.getHyperlinkedName(), displayName));
                gui.getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_SANDBLASTER: {
                String selected = data[1];
                int cost = Integer.parseInt(data[2]);
                selectedPerson.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES,
                        OptionsConstants.GUNNERY_SANDBLASTER, selected);
                selectedPerson.spendXP(cost);
                final String displayName = String.format("%s %s",
                        SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_SANDBLASTER), selected);
                PersonalLogger.gainedSPA(gui.getCampaign(), selectedPerson,
                        gui.getCampaign().getLocalDate(), displayName);
                gui.getCampaign().addReport(String.format(resources.getString("gained.format"),
                        selectedPerson.getHyperlinkedName(), displayName));
                gui.getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_SPECIALIST: {
                String selected = data[1];
                int cost = Integer.parseInt(data[2]);
                selectedPerson.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES,
                        OptionsConstants.GUNNERY_SPECIALIST, selected);
                selectedPerson.spendXP(cost);
                final String displayName = String.format("%s %s",
                        SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_SPECIALIST), selected);
                PersonalLogger.gainedSPA(gui.getCampaign(), selectedPerson,
                        gui.getCampaign().getLocalDate(), displayName);
                gui.getCampaign().addReport(String.format(resources.getString("gained.format"),
                        selectedPerson.getHyperlinkedName(), displayName));
                gui.getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_RANGEMASTER: {
                String selected = data[1];
                int cost = Integer.parseInt(data[2]);
                selectedPerson.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES,
                        OptionsConstants.GUNNERY_RANGE_MASTER, selected);
                selectedPerson.spendXP(cost);
                final String displayName = String.format("%s %s",
                        SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_RANGE_MASTER), selected);
                PersonalLogger.gainedSPA(gui.getCampaign(), selectedPerson,
                        gui.getCampaign().getLocalDate(), displayName);
                gui.getCampaign().addReport(String.format(resources.getString("gained.format"),
                        selectedPerson.getHyperlinkedName(), displayName));
                gui.getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_ENVSPEC: {
                String selected = data[1];
                int cost = Integer.parseInt(data[2]);
                selectedPerson.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES,
                        OptionsConstants.MISC_ENV_SPECIALIST, selected);
                selectedPerson.spendXP(cost);
                final String displayName = String.format("%s %s",
                        SpecialAbility.getDisplayName(OptionsConstants.MISC_ENV_SPECIALIST), selected);
                PersonalLogger.gainedSPA(gui.getCampaign(), selectedPerson,
                        gui.getCampaign().getLocalDate(), displayName);
                gui.getCampaign().addReport(String.format(resources.getString("gained.format"),
                        selectedPerson.getHyperlinkedName(), displayName));
                gui.getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_HUMANTRO: {
                String selected = data[1];
                int cost = Integer.parseInt(data[2]);
                selectedPerson.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES,
                        OptionsConstants.MISC_HUMAN_TRO, selected);
                selectedPerson.spendXP(cost);
                final String displayName = String.format("%s %s",
                        SpecialAbility.getDisplayName(OptionsConstants.MISC_HUMAN_TRO), selected);
                PersonalLogger.gainedSPA(gui.getCampaign(), selectedPerson,
                        gui.getCampaign().getLocalDate(), displayName);
                gui.getCampaign().addReport(String.format(resources.getString("gained.format"),
                        selectedPerson.getHyperlinkedName(), displayName));
                gui.getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_ACQUIRE_CUSTOM_CHOICE: {
                String selected = data[1];
                int cost = Integer.parseInt(data[2]);
                String ability = data[3];
                selectedPerson.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES,
                        ability, selected);
                selectedPerson.spendXP(cost);
                final String displayName = String.format("%s %s",
                        SpecialAbility.getDisplayName(ability), selected);
                PersonalLogger.gainedSPA(gui.getCampaign(), selectedPerson,
                        gui.getCampaign().getLocalDate(), displayName);
                gui.getCampaign().addReport(String.format(resources.getString("spaGainedChoices.format"),
                        selectedPerson.getHyperlinkedName(), displayName));
                gui.getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_CHANGE_STATUS: {
                PersonnelStatus status = PersonnelStatus.valueOf(data[1]);
                for (Person person : people) {
                    if (status.isActive() || (JOptionPane.showConfirmDialog(null,
                            String.format(resources.getString("confirmRetireQ.format"), person.getFullTitle()),
                            status.toString(), JOptionPane.YES_NO_OPTION) == 0)) {
                        person.changeStatus(gui.getCampaign(), gui.getCampaign().getLocalDate(), status);
                    }
                }
                break;
            }
            case CMD_CHANGE_PRISONER_STATUS: {
                try {
                    PrisonerStatus status = PrisonerStatus.valueOf(data[1]);
                    for (Person person : people) {
                        if (person.getPrisonerStatus() != status) {
                            person.setPrisonerStatus(gui.getCampaign(), status, true);
                        }
                    }
                } catch (Exception e) {
                    LogManager.getLogger().error("Unknown PrisonerStatus Option. No changes will be made.", e);
                }
                break;
            }
            case CMD_IMPRISON: {
                for (Person person : people) {
                    if (!person.getPrisonerStatus().isCurrentPrisoner()) {
                        person.setPrisonerStatus(gui.getCampaign(), PrisonerStatus.PRISONER, true);
                    }
                }
                break;
            }
            case CMD_FREE: {
                // TODO: Warn in particular for "freeing" in deep space, leading to Geneva Conventions violation (#1400 adding Crime to MekHQ)
                // TODO: Record the people into some NPC pool, if still alive
                String title = (people.length == 1) ? people[0].getFullTitle()
                        : String.format(resources.getString("numPrisoners.text"), people.length);
                if (0 == JOptionPane.showConfirmDialog(null,
                        String.format(resources.getString("confirmFree.format"), title),
                        resources.getString("freeQ.text"),
                        JOptionPane.YES_NO_OPTION)) {
                    for (Person person : people) {
                        gui.getCampaign().removePerson(person);
                    }
                }
                break;
            }
            case CMD_RECRUIT: {
                for (Person person : people) {
                    if (person.getPrisonerStatus().isPrisonerDefector()) {
                        person.setPrisonerStatus(gui.getCampaign(), PrisonerStatus.FREE, true);
                    }
                }
                break;
            }
            case CMD_RANSOM: {
                // ask the user if they want to sell off their prisoners. If yes, then add a daily report entry, add the money and remove them all.
                Money total = Money.zero();
                total = total.plus(Arrays.stream(people)
                        .map(person -> person.getRansomValue(gui.getCampaign()))
                        .collect(Collectors.toList()));

                if (0 == JOptionPane.showConfirmDialog(
                        null,
                        String.format(resources.getString("ransomQ.format"), people.length, total.toAmountAndSymbolString()),
                        resources.getString("ransom.text"),
                        JOptionPane.YES_NO_OPTION)) {
                    gui.getCampaign().addReport(String.format(resources.getString("ransomReport.format"),
                            people.length, total.toAmountAndSymbolString()));
                    gui.getCampaign().addFunds(TransactionType.RANSOM, total, resources.getString("ransom.text"));
                    for (Person person : people) {
                        gui.getCampaign().removePerson(person, false);
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
                        gui.getCampaign().personUpdated(person);
                    }
                } else {
                    selectedPerson.changeEdgeTrigger(trigger);
                    gui.getCampaign().personUpdated(selectedPerson);
                }
                break;
            }
            case CMD_REMOVE: {
                String title = (people.length == 1) ? people[0].getFullTitle()
                        : String.format(resources.getString("numPersonnel.text"), people.length);
                if (0 == JOptionPane.showConfirmDialog(null,
                        String.format(resources.getString("confirmRemove.format"), title),
                        resources.getString("removeQ.text"),
                        JOptionPane.YES_NO_OPTION)) {
                    for (Person person : people) {
                        gui.getCampaign().removePerson(person);
                    }
                }
                break;
            }
            case CMD_SACK: {
                boolean showDialog = false;
                List<Person> toRemove = new ArrayList<>();
                for (Person person : people) {
                    if (gui.getCampaign().getRetirementDefectionTracker().removeFromCampaign(
                            person, false, gui.getCampaign(), null)) {
                        showDialog = true;
                    } else {
                        toRemove.add(person);
                    }
                }
                if (showDialog) {
                    RetirementDefectionDialog rdd = new RetirementDefectionDialog(
                            gui, null, false);
                    rdd.setVisible(true);
                    if (rdd.wasAborted()
                            || !gui.getCampaign().applyRetirement(rdd.totalPayout(),
                            rdd.getUnitAssignments())) {
                        for (Person person : people) {
                            gui.getCampaign().getRetirementDefectionTracker()
                                    .removePayout(person);
                        }
                    } else {
                        for (final Person person : toRemove) {
                            gui.getCampaign().removePerson(person);
                        }
                    }
                } else {
                    String question;
                    if (people.length > 1) {
                        question = resources.getString("confirmRemoveMultiple.text");
                    } else {
                        question = String.format(resources.getString("confirmRemove.format"), people[0].getFullTitle());
                    }
                    if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                            null, question, resources.getString("removeQ.text"),
                            JOptionPane.YES_NO_OPTION)) {
                        for (Person person : people) {
                            gui.getCampaign().removePerson(person);
                        }
                    }
                }
                break;
            }
            case CMD_EDIT: {
                CustomizePersonDialog npd = new CustomizePersonDialog(
                        gui.getFrame(), true, selectedPerson, gui.getCampaign());
                npd.setVisible(true);
                gui.getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_EDIT_HITS: {
                EditPersonnelHitsDialog ephd = new EditPersonnelHitsDialog(gui.getFrame(), true, selectedPerson);
                ephd.setVisible(true);
                if (0 == selectedPerson.getHits()) {
                    selectedPerson.setDoctorId(null, gui.getCampaign().getCampaignOptions()
                            .getNaturalHealingWaitingPeriod());
                }
                gui.getCampaign().personUpdated(selectedPerson);
                break;
            }
            case CMD_EDIT_PORTRAIT: {
                final PortraitChooserDialog portraitDialog = new PortraitChooserDialog(
                        gui.getFrame(), selectedPerson.getPortrait());
                if (portraitDialog.showDialog().isConfirmed()) {
                    for (Person person : people) {
                        if (!person.getPortrait().equals(portraitDialog.getSelectedItem())) {
                            person.setPortrait(portraitDialog.getSelectedItem());
                            gui.getCampaign().personUpdated(person);
                        }
                    }
                }
                break;
            }
            case CMD_EDIT_BIOGRAPHY: {
                MarkdownEditorDialog tad = new MarkdownEditorDialog(gui.getFrame(), true,
                        resources.getString("editBiography.text"), selectedPerson.getBiography());
                tad.setVisible(true);
                if (tad.wasChanged()) {
                    selectedPerson.setBiography(tad.getText());
                    MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                }
                break;
            }
            case CMD_ADD_1_XP: {
                for (Person person : people) {
                    person.awardXP(gui.getCampaign(), 1);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_ADD_XP: {
                PopupValueChoiceDialog pvcda = new PopupValueChoiceDialog(
                        gui.getFrame(), true, resources.getString("xp.text"), 1, 0);
                pvcda.setVisible(true);

                int ia = pvcda.getValue();
                if (ia <= 0) {
                    // <0 indicates Cancellation
                    // =0 is a No-Op
                    return;
                }

                for (Person person : people) {
                    person.awardXP(gui.getCampaign(), ia);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_SET_XP: {
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                        gui.getFrame(), true, resources.getString("xp.text"), selectedPerson.getXP(), 0);
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                int i = pvcd.getValue();
                for (Person person : people) {
                    person.setXP(gui.getCampaign(), i);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_BUY_EDGE: {
                final int cost = gui.getCampaign().getCampaignOptions().getEdgeCost();
                for (Person person : people) {
                    selectedPerson.spendXP(cost);
                    person.changeEdge(1);
                    // Make the new edge point available to support personnel, but don't reset until
                    // the week ends
                    person.changeCurrentEdge(1);
                    PersonalLogger.gainedEdge(gui.getCampaign(), person, gui.getCampaign().getLocalDate());
                    gui.getCampaign().addReport(String.format(resources.getString("gainedEdge.format"), selectedPerson.getHyperlinkedName()));
                    gui.getCampaign().personUpdated(person);
                }
                break;
            }
            case CMD_SET_EDGE: {
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                        gui.getFrame(), true, resources.getString("edge.text"), selectedPerson.getEdge(), 0,
                        10);
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                int i = pvcd.getValue();
                for (Person person : people) {
                    person.setEdge(i);
                    //Reset currentEdge for support people
                    person.resetCurrentEdge();
                    PersonalLogger.changedEdge(gui.getCampaign(), person, gui.getCampaign().getLocalDate());
                    gui.getCampaign().personUpdated(person);
                }
                break;
            }
            case CMD_ADD_KILL: {
                AddOrEditKillEntryDialog nkd;
                Unit unit = selectedPerson.getUnit();
                if (people.length > 1) {
                    nkd = new AddOrEditKillEntryDialog(gui.getFrame(), true, null,
                            (unit != null) ? unit.getName() : resources.getString("bareHands.text"),
                            gui.getCampaign().getLocalDate(), gui.getCampaign());
                } else {
                    nkd = new AddOrEditKillEntryDialog(gui.getFrame(), true, selectedPerson.getId(),
                            (unit != null) ? unit.getName() : resources.getString("bareHands.text"),
                            gui.getCampaign().getLocalDate(), gui.getCampaign());
                }
                nkd.setVisible(true);
                if (nkd.getKill().isPresent()) {
                    Kill kill = nkd.getKill().get();
                    if (people.length > 1) {
                        for (Person person : people) {
                            Kill k = kill.clone();
                            k.setPilotId(person.getId());
                            gui.getCampaign().addKill(k);
                            MekHQ.triggerEvent(new PersonLogEvent(person));
                        }
                    } else {
                        gui.getCampaign().addKill(kill);
                        MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                    }
                }
                break;
            }
            case CMD_EDIT_KILL_LOG: {
                EditKillLogDialog ekld = new EditKillLogDialog(gui.getFrame(), true, gui.getCampaign(), selectedPerson);
                ekld.setVisible(true);
                MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                break;
            }
            case CMD_EDIT_PERSONNEL_LOG: {
                EditPersonnelLogDialog epld = new EditPersonnelLogDialog(gui.getFrame(), true, gui.getCampaign(), selectedPerson);
                epld.setVisible(true);
                MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                break;
            }
            case CMD_ADD_LOG_ENTRY: {
                final AddOrEditPersonnelEntryDialog addPersonnelLogDialog = new AddOrEditPersonnelEntryDialog(
                        gui.getFrame(), null, gui.getCampaign().getLocalDate());
                if (addPersonnelLogDialog.showDialog().isConfirmed()) {
                    for (Person person : people) {
                        person.addLogEntry(addPersonnelLogDialog.getEntry().clone());
                        MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                    }
                }
                break;
            }
            case CMD_EDIT_SCENARIO_LOG: {
                EditScenarioLogDialog emld = new EditScenarioLogDialog(gui.getFrame(), true, gui.getCampaign(), selectedPerson);
                emld.setVisible(true);
                MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                break;
            }
            case CMD_ADD_SCENARIO_ENTRY: {
                AddOrEditScenarioEntryDialog addScenarioDialog = new AddOrEditScenarioEntryDialog(
                        gui.getFrame(), true, gui.getCampaign().getLocalDate());
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
                String s = (String) JOptionPane.showInputDialog(gui.getFrame(),
                        resources.getString("enterNewCallsign.text"), resources.getString("editCallsign.text"),
                        JOptionPane.PLAIN_MESSAGE, null, null,
                        selectedPerson.getCallsign());
                if (null != s) {
                    selectedPerson.setCallsign(s);
                    gui.getCampaign().personUpdated(selectedPerson);
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
            case CMD_EDIT_INJURIES: {
                EditPersonnelInjuriesDialog epid = new EditPersonnelInjuriesDialog(
                        gui.getFrame(), true, gui.getCampaign(), selectedPerson);
                epid.setVisible(true);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                break;
            }
            case CMD_EDIT_SALARY: {
                PopupValueChoiceDialog pcvd = new PopupValueChoiceDialog(gui.getFrame(), true,
                        resources.getString("changeSalary.text"),
                        selectedPerson.getSalary(gui.getCampaign()).getAmount().intValue(),
                        -1, 100000);
                pcvd.setVisible(true);
                int salary = pcvd.getValue();
                if (salary < -1) {
                    return;
                }
                for (Person person : people) {
                    person.setSalary(Money.of(salary));
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_GIVE_PAYMENT: {
                PopupValueChoiceDialog pcvd = new PopupValueChoiceDialog(gui.getFrame(), true,
                        resources.getString("givePayment.title"),
                        1000,
                        1,
                        1000000);
                pcvd.setVisible(true);

                int payment = pcvd.getValue();
                if (payment <= 0) {
                    // <0 indicates Cancellation
                    // =0 is a No-Op
                    return;
                }

                // pay person
                for (Person person : people) {
                    person.payPerson(Money.of(payment));
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }

                // add expense
                gui.getCampaign().removeFunds(TransactionType.MISCELLANEOUS, Money.of(payment),
                        String.format(resources.getString("givePayment.format"),
                                selectedPerson.getFullName()));

                break;
            }

            //region Randomization Menu
            case CMD_RANDOM_NAME: {
                for (final Person person : people) {
                    final String[] name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplit(
                            person.getGender(), person.isClanPersonnel(), person.getOriginFaction().getShortName());
                    person.setGivenName(name[0]);
                    person.setSurname(name[1]);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_RANDOM_BLOODNAME: {
                final boolean ignoreDice = (data.length > 1) && Boolean.parseBoolean(data[1]);
                for (final Person person : people) {
                    gui.getCampaign().checkBloodnameAdd(person, ignoreDice);
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
                    gui.getCampaign().assignRandomPortraitFor(person);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_RANDOM_ORIGIN: {
                for (final Person person : people) {
                    gui.getCampaign().assignRandomOriginFor(person);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_RANDOM_ORIGIN_FACTION: {
                for (final Person person : people) {
                    final Faction faction = gui.getCampaign().getFactionSelector()
                            .selectFaction(gui.getCampaign());
                    if (faction != null) {
                        person.setOriginFaction(faction);
                        MekHQ.triggerEvent(new PersonChangedEvent(person));
                    }
                }
                break;
            }
            case CMD_RANDOM_ORIGIN_PLANET: {
                for (final Person person : people) {
                    final Planet planet = gui.getCampaign().getPlanetSelector().selectPlanet(
                            gui.getCampaign(), person.getOriginFaction());
                    if (planet != null) {
                        person.setOriginPlanet(planet);
                        MekHQ.triggerEvent(new PersonChangedEvent(person));
                    }
                }
                break;
            }
            //endregion Randomization Menu

            default: {
                break;
            }
        }
    }

    private void loadGMToolsForPerson(Person person) {
        GMToolsDialog gmToolsDialog = new GMToolsDialog(gui.getFrame(), gui, person);
        gmToolsDialog.setVisible(true);
        gui.getCampaign().personUpdated(person);
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
        if (StaticChecks.areAllEligible(true, selected)) {
            menu = new JMenu(resources.getString("changeRank.text"));
            final Profession initialProfession = Profession.getProfessionFromPersonnelRole(person.getPrimaryRole());
            for (final RankDisplay rankDisplay : RankDisplay.getRankDisplaysForSystem(
                    person.getRankSystem(), initialProfession)) {
                final Rank rank = person.getRankSystem().getRank(rankDisplay.getRankNumeric());
                final Profession profession = initialProfession.getProfession(person.getRankSystem(), rank);
                final int rankLevels = rank.getRankLevels().get(profession);

                if (rankLevels > 1) {
                    submenu = new JMenu(rankDisplay.toString());
                    for (int level = 0; level <= rankLevels; level++) {
                        cbMenuItem = new JCheckBoxMenuItem(rank.getName(profession)
                                + Utilities.getRomanNumeralsFromArabicNumber(level, true));
                        cbMenuItem.setSelected((person.getRankNumeric() == rankDisplay.getRankNumeric())
                                && (person.getRankLevel() == level));
                        cbMenuItem.setActionCommand(makeCommand(CMD_RANK,
                                String.valueOf(rankDisplay.getRankNumeric()), String.valueOf(level)));
                        cbMenuItem.addActionListener(this);
                        submenu.add(cbMenuItem);
                    }
                    JMenuHelpers.addMenuIfNonEmpty(menu, submenu);
                } else {
                    cbMenuItem = new JCheckBoxMenuItem(rankDisplay.toString());
                    cbMenuItem.setSelected(person.getRankNumeric() == rankDisplay.getRankNumeric());
                    cbMenuItem.setActionCommand(makeCommand(CMD_RANK, String.valueOf(rankDisplay.getRankNumeric())));
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);
                }
            }
            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        }

        menu = new JMenu(resources.getString("changeRankSystem.text"));
        final RankSystem campaignRankSystem = gui.getCampaign().getRankSystem();
        // First allow them to revert to the campaign system
        cbMenuItem = new JCheckBoxMenuItem(resources.getString("useCampaignRankSystem.text"));
        cbMenuItem.setSelected(campaignRankSystem.equals(person.getRankSystem()));
        cbMenuItem.setActionCommand(makeCommand(CMD_RANKSYSTEM, campaignRankSystem.getCode()));
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
            cbMenuItem.setActionCommand(makeCommand(CMD_RANKSYSTEM, rankSystem.getCode()));
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
        for (final PersonnelStatus status : PersonnelStatus.getImplementedStatuses()) {
            cbMenuItem = new JCheckBoxMenuItem(status.toString());
            cbMenuItem.setToolTipText(status.getToolTipText());
            cbMenuItem.setSelected(person.getStatus() == status);
            cbMenuItem.setActionCommand(makeCommand(CMD_CHANGE_STATUS, status.name()));
            cbMenuItem.addActionListener(this);
            menu.add(cbMenuItem);
        }
        popup.add(menu);

        if (StaticChecks.areAnyFree(selected)) {
            popup.add(newMenuItem(resources.getString("imprison.text"), CMD_IMPRISON));
        } else {
            // If none are free, then we can put the Free option
            popup.add(newMenuItem(resources.getString("free.text"), CMD_FREE));
        }

        if (gui.getCampaign().getCampaignOptions().isUseAtBPrisonerRansom()
                && StaticChecks.areAllPrisoners(selected)) {
            popup.add(newMenuItem(resources.getString("ransom.text"), CMD_RANSOM));
        }

        if (StaticChecks.areAnyWillingToDefect(selected)) {
            popup.add(newMenuItem(resources.getString("recruit.text"), CMD_RECRUIT));
        }

        final PersonnelRole[] roles = PersonnelRole.values();
        menu = new JMenu(resources.getString("changePrimaryRole.text"));
        for (final PersonnelRole role : roles) {
            if (person.canPerformRole(role, true)) {
                cbMenuItem = new JCheckBoxMenuItem(role.getName(person.isClanPersonnel()));
                cbMenuItem.setActionCommand(makeCommand(CMD_PRIMARY_ROLE, role.name()));
                cbMenuItem.setSelected(person.getPrimaryRole() == role);
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);
            }
        }
        JMenuHelpers.addMenuIfNonEmpty(popup, menu);

        menu = new JMenu(resources.getString("changeSecondaryRole.text"));
        for (final PersonnelRole role : roles) {
            if (person.canPerformRole(role, false)) {
                cbMenuItem = new JCheckBoxMenuItem(role.getName(person.isClanPersonnel()));
                cbMenuItem.setActionCommand(makeCommand(CMD_SECONDARY_ROLE, role.name()));
                cbMenuItem.setSelected(person.getSecondaryRole() == role);
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);
            }
        }
        JMenuHelpers.addMenuIfNonEmpty(popup, menu);

        // change salary
        if (gui.getCampaign().getCampaignOptions().isPayForSalaries() && StaticChecks.areAllActive(selected)) {
            menuItem = new JMenuItem(resources.getString("setSalary.text"));
            menuItem.setActionCommand(CMD_EDIT_SALARY);
            menuItem.addActionListener(this);
            popup.add(menuItem);
        }

        // give C-Bill payment
        if (oneSelected && person.getStatus().isActive()) {
            menuItem = new JMenuItem(resources.getString("givePayment.text"));
            menuItem.setActionCommand(CMD_GIVE_PAYMENT);
            menuItem.addActionListener(this);
            popup.add(menuItem);
        }

        JMenuHelpers.addMenuIfNonEmpty(popup, new AssignPersonToUnitMenu(gui.getCampaign(), selected));

        if (oneSelected && person.getStatus().isActive()) {
            if (gui.getCampaign().getCampaignOptions().isUseManualMarriages()
                    && (gui.getCampaign().getMarriage().canMarry(gui.getCampaign(),
                            gui.getCampaign().getLocalDate(), person, false) == null)) {
                menu = new JMenu(resources.getString("chooseSpouse.text"));
                JMenu maleMenu = new JMenu(resources.getString("spouseMenuMale.text"));
                JMenu femaleMenu = new JMenu(resources.getString("spouseMenuFemale.text"));
                JMenu spouseMenu;

                LocalDate today = gui.getCampaign().getLocalDate();

                // Get all safe potential spouses sorted by age and then by surname
                final List<Person> personnel = gui.getCampaign().getPersonnel().stream()
                        .filter(potentialSpouse -> gui.getCampaign().getMarriage().safeSpouse(
                                gui.getCampaign(), gui.getCampaign().getLocalDate(), person,
                                potentialSpouse, false))
                        .sorted(Comparator.comparing((Person p) -> p.getAge(today))
                                .thenComparing(Person::getSurname))
                        .collect(Collectors.toList());

                for (final Person potentialSpouse : personnel) {
                    final String status;
                    final String founder = potentialSpouse.isFounder() ? resources.getString("spouseFounder.text") : "";
                    if (potentialSpouse.getPrisonerStatus().isBondsman()) {
                        status = String.format(resources.getString("marriageBondsmanDesc.format"),
                                potentialSpouse.getFullName(), potentialSpouse.getAge(today),
                                potentialSpouse.getRoleDesc(), founder);
                    } else if (potentialSpouse.getPrisonerStatus().isCurrentPrisoner()) {
                        status = String.format(resources.getString("marriagePrisonerDesc.format"),
                                potentialSpouse.getFullName(), potentialSpouse.getAge(today),
                                potentialSpouse.getRoleDesc(), founder);
                    } else {
                        status = String.format(resources.getString("marriagePartnerDesc.format"),
                                potentialSpouse.getFullName(), potentialSpouse.getAge(today),
                                potentialSpouse.getRoleDesc(), founder);
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

        if (gui.getCampaign().getCampaignOptions().isUseManualDivorce()
                && Stream.of(selected).anyMatch(p -> gui.getCampaign().getDivorce().canDivorce(person, false) == null)) {
            menu = new JMenu(resources.getString("removeSpouse.text"));

            for (final SplittingSurnameStyle style : SplittingSurnameStyle.values()) {
                JMenuItem divorceMenu = new JMenuItem(style.getDropDownText());
                divorceMenu.setActionCommand(makeCommand(CMD_REMOVE_SPOUSE, style.name()));
                divorceMenu.addActionListener(this);
                menu.add(divorceMenu);
            }

            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        }

        //region Awards Menu
        JMenu awardMenu = new JMenu(resources.getString("award.text"));
        List<String> setNames = AwardsFactory.getInstance().getAllSetNames();
        Collections.sort(setNames);
        for (String setName : setNames) {
            JMenu setAwardMenu = new JMenu(setName);

            List<Award> awardsOfSet = AwardsFactory.getInstance().getAllAwardsForSet(setName);
            Collections.sort(awardsOfSet);

            for (Award award : awardsOfSet) {
                if (oneSelected && !award.canBeAwarded(selected)) {
                    continue;
                }

                StringBuilder awardMenuItem = new StringBuilder();
                awardMenuItem.append(String.format("%s", award.getName()));

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

                    awardMenuItem.append(")");
                }

                menuItem = new JMenuItem(awardMenuItem.toString());
                menuItem.setToolTipText(MultiLineTooltip.splitToolTip(award.getDescription()));
                menuItem.setActionCommand(makeCommand(CMD_ADD_AWARD, award.getSet(), award.getName()));
                menuItem.addActionListener(this);
                setAwardMenu.add(menuItem);
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
                        specificAwardMenu.setActionCommand(makeCommand(CMD_RMV_AWARD, award.getSet(), award.getName(), date));
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
        //endregion Awards Menu

        //region Spend XP Menu
        if (oneSelected && person.getStatus().isActive()) {
            menu = new JMenu(resources.getString("spendXP.text"));
            if (gui.getCampaign().getCampaignOptions().isUseAbilities()) {
                JMenu abMenu = new JMenu(resources.getString("spendOnSpecialAbilities.text"));
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
                    cost = spa.getCost();
                    String costDesc;
                    if (cost < 0) {
                        costDesc = resources.getString("costNotPossible.text");
                    } else {
                        costDesc = String.format(resources.getString("costValue.format"), cost);
                    }
                    boolean available = (cost >= 0) && (person.getXP() >= cost);
                    if (spa.getName().equals(OptionsConstants.GUNNERY_WEAPON_SPECIALIST)) {
                        Unit u = person.getUnit();
                        if (null != u) {
                            JMenu specialistMenu = new JMenu(SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_WEAPON_SPECIALIST));
                            TreeSet<String> uniqueWeapons = new TreeSet<>();
                            for (int j = 0; j < u.getEntity().getWeaponList().size(); j++) {
                                Mounted m = u.getEntity().getWeaponList().get(j);
                                uniqueWeapons.add(m.getName());
                            }
                            boolean isSpecialist = person.getOptions().booleanOption(spa.getName());
                            for (String name : uniqueWeapons) {
                                if (!(isSpecialist && person.getOptions().getOption(spa.getName()).stringValue().equals(name))) {
                                    menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), name, costDesc));
                                    menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_WEAPON_SPECIALIST, name, String.valueOf(cost)));
                                    menuItem.addActionListener(this);
                                    menuItem.setEnabled(available);
                                    specialistMenu.add(menuItem);
                                }
                            }
                            if (specialistMenu.getMenuComponentCount() > 0) {
                                abMenu.add(specialistMenu);
                            }
                        }
                    } else if (spa.getName().equals(OptionsConstants.GUNNERY_SANDBLASTER)) {
                        Unit u = person.getUnit();
                        if (null != u) {
                            JMenu specialistMenu = new JMenu(SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_SANDBLASTER));
                            TreeSet<String> uniqueWeapons = new TreeSet<>();
                            for (int j = 0; j < u.getEntity().getWeaponList().size(); j++) {
                                Mounted m = u.getEntity().getWeaponList().get(j);
                                if (SpecialAbility.isWeaponEligibleForSPA(m.getType(), person.getPrimaryRole(), true)) {
                                    uniqueWeapons.add(m.getName());
                                }
                            }
                            boolean isSpecialist = person.getOptions().booleanOption(spa.getName());
                            for (String name : uniqueWeapons) {
                                if (!(isSpecialist && person.getOptions().getOption(spa.getName()).stringValue().equals(name))) {
                                    menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), name, costDesc));
                                    menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_SANDBLASTER, name, String.valueOf(cost)));
                                    menuItem.addActionListener(this);
                                    menuItem.setEnabled(available);
                                    specialistMenu.add(menuItem);
                                }
                            }
                            if (specialistMenu.getMenuComponentCount() > 0) {
                                abMenu.add(specialistMenu);
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
                        menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("envspec_fog.text"), costDesc));
                        if (!tros.contains(Crew.ENVSPC_FOG)) {
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_ENVSPEC, Crew.ENVSPC_FOG, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!tros.contains(Crew.ENVSPC_LIGHT)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("envspec_light.text"), costDesc));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_ENVSPEC, Crew.ENVSPC_LIGHT, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!tros.contains(Crew.ENVSPC_RAIN)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("envspec_rain.text"), costDesc));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_ENVSPEC, Crew.ENVSPC_RAIN, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!tros.contains(Crew.ENVSPC_SNOW)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("envspec_snow.text"), costDesc));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_ENVSPEC, Crew.ENVSPC_SNOW, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!tros.contains(Crew.ENVSPC_WIND)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("envspec_wind.text"), costDesc));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_ENVSPEC, Crew.ENVSPC_WIND, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        JMenuHelpers.addMenuIfNonEmpty(abMenu, specialistMenu);
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
                        menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("humantro_mek.text"), costDesc));
                        if (!tros.contains(Crew.HUMANTRO_MECH)) {
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_HUMANTRO, Crew.HUMANTRO_MECH, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!tros.contains(Crew.HUMANTRO_AERO)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("humantro_aero.text"), costDesc));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_HUMANTRO, Crew.HUMANTRO_AERO, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!tros.contains(Crew.HUMANTRO_VEE)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("humantro_vee.text"), costDesc));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_HUMANTRO, Crew.HUMANTRO_VEE, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!tros.contains(Crew.HUMANTRO_BA)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("humantro_ba.text"), costDesc));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_HUMANTRO, Crew.HUMANTRO_BA, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (specialistMenu.getMenuComponentCount() > 0) {
                            abMenu.add(specialistMenu);
                        }
                    } else if (spa.getName().equals(OptionsConstants.GUNNERY_SPECIALIST)
                            && !person.getOptions().booleanOption(OptionsConstants.GUNNERY_SPECIALIST)) {
                        JMenu specialistMenu = new JMenu(SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_SPECIALIST));
                        menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("laserSpecialist.text"), costDesc));
                        menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_SPECIALIST, Crew.SPECIAL_ENERGY, String.valueOf(cost)));
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(available);
                        specialistMenu.add(menuItem);
                        menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("missileSpecialist.text"), costDesc));
                        menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_SPECIALIST, Crew.SPECIAL_MISSILE, String.valueOf(cost)));
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(available);
                        specialistMenu.add(menuItem);
                        menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("ballisticSpecialist.text"), costDesc));
                        menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_SPECIALIST, Crew.SPECIAL_BALLISTIC, String.valueOf(cost)));
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(available);
                        specialistMenu.add(menuItem);
                        abMenu.add(specialistMenu);
                    } else if (spa.getName().equals(OptionsConstants.GUNNERY_RANGE_MASTER)) {
                        JMenu specialistMenu = new JMenu(SpecialAbility.getDisplayName(OptionsConstants.GUNNERY_RANGE_MASTER));
                        List<Object> ranges = new ArrayList<>();
                        if (person.getOptions().getOption(OptionsConstants.GUNNERY_RANGE_MASTER).booleanValue()) {
                            Object val = person.getOptions().getOption(OptionsConstants.GUNNERY_RANGE_MASTER).getValue();
                            if (val instanceof Collection<?>) {
                                ranges.addAll((Collection<?>) val);
                            } else {
                                ranges.add(val);
                            }
                        }

                        if (!ranges.contains(Crew.RANGEMASTER_MEDIUM)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("rangemaster_med.text"), costDesc));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_RANGEMASTER, Crew.RANGEMASTER_MEDIUM, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!ranges.contains(Crew.RANGEMASTER_LONG)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("rangemaster_lng.text"), costDesc));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_RANGEMASTER, Crew.RANGEMASTER_LONG, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (!ranges.contains(Crew.RANGEMASTER_EXTREME)) {
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), resources.getString("rangemaster_xtm.text"), costDesc));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_RANGEMASTER, Crew.RANGEMASTER_EXTREME, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }

                        if (specialistMenu.getMenuComponentCount() > 0) {
                            abMenu.add(specialistMenu);
                        }
                    } else if ((person.getOptions().getOption(spa.getName()).getType() == IOption.CHOICE)
                            && !(person.getOptions().getOption(spa.getName()).booleanValue())) {
                        JMenu specialistMenu = new JMenu(spa.getDisplayName());
                        List<String> choices = spa.getChoiceValues();
                        for (String s : choices) {
                            if (s.equalsIgnoreCase("none")) {
                                continue;
                            }
                            menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"),
                                    s, costDesc));
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_CUSTOM_CHOICE,
                                    s, String.valueOf(cost), spa.getName()));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                        }
                        if (specialistMenu.getMenuComponentCount() > 0) {
                            abMenu.add(specialistMenu);
                        }
                    } else if (!person.getOptions().booleanOption(spa.getName())) {
                        menuItem = new JMenuItem(String.format(resources.getString("abilityDesc.format"), spa.getDisplayName(), costDesc));
                        menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_ABILITY, spa.getName(), String.valueOf(cost)));
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(available);
                        abMenu.add(menuItem);
                    }
                }
                JMenuHelpers.addMenuIfNonEmpty(menu, abMenu);
            }

            JMenu currentMenu = new JMenu(resources.getString("spendOnCurrentSkills.text"));
            JMenu newMenu = new JMenu(resources.getString("spendOnNewSkills.text"));
            for (int i = 0; i < SkillType.getSkillList().length; i++) {
                String type = SkillType.getSkillList()[i];
                int cost = person.hasSkill(type) ? person.getSkill(type).getCostToImprove() : SkillType.getType(type).getCost(0);
                if (cost >= 0) {
                    String desc = String.format(resources.getString("skillDesc.format"), type, cost);
                    menuItem = new JMenuItem(desc);
                    menuItem.setActionCommand(makeCommand(CMD_IMPROVE, type, String.valueOf(cost)));
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(person.getXP() >= cost);
                    if (person.hasSkill(type)) {
                        currentMenu.add(menuItem);
                    } else {
                        newMenu.add(menuItem);
                    }
                }
            }
            JMenuHelpers.addMenuIfNonEmpty(menu, currentMenu);
            JMenuHelpers.addMenuIfNonEmpty(menu, newMenu);

            // Edge Purchasing
            if (gui.getCampaign().getCampaignOptions().isUseEdge()) {
                JMenu edgeMenu = new JMenu(resources.getString("edge.text"));
                int cost = gui.getCampaign().getCampaignOptions().getEdgeCost();

                if ((cost >= 0) && (person.getXP() >= cost)) {
                    menuItem = new JMenuItem(String.format(resources.getString("spendOnEdge.text"), cost));
                    menuItem.setActionCommand(makeCommand(CMD_BUY_EDGE, String.valueOf(cost)));
                    menuItem.addActionListener(this);
                    edgeMenu.add(menuItem);
                }
                JMenuHelpers.addMenuIfNonEmpty(menu, edgeMenu);
            }
            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
            //endregion Spend XP Menu

            //region Edge Triggers
            if (gui.getCampaign().getCampaignOptions().isUseEdge()) {
                menu = new JMenu(resources.getString("setEdgeTriggers.text"));

                //Start of Edge reroll options
                //MechWarriors
                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerHeadHits.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_HEADHIT));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_HEADHIT));
                if (!person.getPrimaryRole().isMechWarriorGrouping()) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerTAC.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_TAC));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_TAC));
                if (!person.getPrimaryRole().isMechWarriorGrouping()) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerKO.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_KO));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_KO));
                if (!person.getPrimaryRole().isMechWarriorGrouping()) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerExplosion.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_EXPLOSION));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_EXPLOSION));
                if (!person.getPrimaryRole().isMechWarriorGrouping()) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerMASCFailure.text"));
                cbMenuItem.setSelected(person.getOptions().booleanOption(OPT_EDGE_MASC_FAILURE));
                cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_MASC_FAILURE));
                if (!person.getPrimaryRole().isMechWarriorGrouping()) {
                    cbMenuItem.setForeground(new Color(150, 150, 150));
                }
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);

                // Aerospace pilots and gunners
                final boolean isNotAeroOrConventional = !(person.getPrimaryRole().isAerospacePilot()
                        || person.getPrimaryRole().isConventionalAircraftPilot()
                        || person.getPrimaryRole().isLAMPilot());
                final boolean isNotVessel = !person.getPrimaryRole().isVesselCrewmember();
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
                if (gui.getCampaign().getCampaignOptions().isUseSupportEdge()) {
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
                    cbMenuItem.setSelected(person.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_FAILED_REFIT));
                    cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_REPAIR_FAILED_REFIT));
                    if (!person.getPrimaryRole().isTech()) {
                        cbMenuItem.setForeground(new Color(150, 150, 150));
                    }
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);

                    // Admins
                    cbMenuItem = new JCheckBoxMenuItem(resources.getString("edgeTriggerAcquireCheck.text"));
                    cbMenuItem.setSelected(person.getOptions().booleanOption(PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL));
                    cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL));
                    if (!person.getPrimaryRole().isAdministrator()) {
                        cbMenuItem.setForeground(new Color(150, 150, 150));
                    }
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);
                }
                JMenuHelpers.addMenuIfNonEmpty(popup, menu);
            }
            //endregion Edge Triggers

            popup.add(menu);
        } else if (StaticChecks.areAllActive(selected)) {
            if (gui.getCampaign().getCampaignOptions().isUseEdge()) {
                menu = new JMenu(resources.getString("setEdgeTriggers.text"));
                submenu = new JMenu(resources.getString("On.text"));

                menuItem = new JMenuItem(resources.getString("edgeTriggerHeadHits.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_HEADHIT, TRUE));
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

                if (gui.getCampaign().getCampaignOptions().isUseSupportEdge()) {
                    menuItem = new JMenuItem(resources.getString("edgeTriggerHealCheck.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_MEDICAL, TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("edgeTriggerBreakPart.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_REPAIR_BREAK_PART, TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("edgeTriggerFailedRefit.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_REPAIR_FAILED_REFIT, TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("edgeTriggerAcquireCheck.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL, TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                }
                JMenuHelpers.addMenuIfNonEmpty(menu, submenu);

                submenu = new JMenu(resources.getString("Off.text"));

                menuItem = new JMenuItem(resources.getString("edgeTriggerHeadHits.text"));
                menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_HEADHIT, FALSE));
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

                if (gui.getCampaign().getCampaignOptions().isUseSupportEdge()) {
                    menuItem = new JMenuItem(resources.getString("edgeTriggerHealCheck.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_MEDICAL, FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("edgeTriggerBreakPart.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_REPAIR_BREAK_PART, FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("edgeTriggerFailedRefit.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_REPAIR_FAILED_REFIT, FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("edgeTriggerAcquireCheck.text"));
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL, FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                }
                JMenuHelpers.addMenuIfNonEmpty(menu, submenu);
                JMenuHelpers.addMenuIfNonEmpty(popup, menu);
            }

            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        }

        if(!oneSelected) {
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

        if(oneSelected) {
            menuItem = new JMenuItem(resources.getString("editPersonnelLog.text"));
            menuItem.setActionCommand(CMD_EDIT_PERSONNEL_LOG);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("editScenarioLog.text"));
            menuItem.setActionCommand(CMD_EDIT_SCENARIO_LOG);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("editKillLog.text"));
            menuItem.setActionCommand(CMD_EDIT_KILL_LOG);
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

            if (StaticChecks.allHaveSameUnit(selected)) {
                menuItem = new JMenuItem(resources.getString("assignKill.text"));
                menuItem.setActionCommand(CMD_ADD_KILL);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                menu.add(menuItem);
            }
        }

        JMenuHelpers.addMenuIfNonEmpty(popup, menu);

        menuItem = new JMenuItem(resources.getString("exportPersonnel.text"));
        menuItem.addActionListener(evt -> gui.savePersonFile());
        menuItem.setEnabled(true);
        popup.add(menuItem);

        if (gui.getCampaign().getCampaignOptions().isUseAtB() && StaticChecks.areAllActive(selected)) {
            menuItem = new JMenuItem(resources.getString("sack.text"));
            menuItem.setActionCommand(CMD_SACK);
            menuItem.addActionListener(this);
            popup.add(menuItem);
        }

        //region Flags Menu
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
                gui.getCampaign().getPersonnel().stream()
                        .filter(Person::isCommander)
                        .forEach(commander -> {
                            commander.setCommander(false);
                            gui.getCampaign().addReport(String.format(resources.getString("removedCommander.format"),
                                    commander.getHyperlinkedFullTitle()));
                            gui.getCampaign().personUpdated(commander);
                        });
                if (miCommander.isSelected()) {
                    person.setCommander(true);
                    gui.getCampaign().addReport(String.format(resources.getString("setAsCommander.format"), person.getHyperlinkedFullTitle()));
                    gui.getCampaign().personUpdated(person);
                }
            });
            menu.add(miCommander);
        }

        if ((gui.getCampaign().getCampaignOptions().isUseManualDivorce()
                || !gui.getCampaign().getCampaignOptions().getRandomDivorceMethod().isNone())
                && Stream.of(selected).allMatch(p -> p.getGenealogy().hasSpouse())
                && Stream.of(selected).allMatch(p -> p.isDivorceable() == person.isDivorceable())) {
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

        if (!gui.getCampaign().getCampaignOptions().getRandomDeathMethod().isNone()
                && Stream.of(selected).noneMatch(p -> p.getStatus().isDead())
                && Stream.of(selected).allMatch(p -> p.isImmortal() == person.isImmortal())) {
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

        if ((gui.getCampaign().getCampaignOptions().isUseManualMarriages()
                || !gui.getCampaign().getCampaignOptions().getRandomMarriageMethod().isNone())
                && Stream.of(selected).allMatch(p -> p.isMarriageable() == person.isMarriageable())) {
            cbMenuItem = new JCheckBoxMenuItem(resources.getString("miMarriageable.text"));
            cbMenuItem.setToolTipText(MultiLineTooltip.splitToolTip(String.format(resources.getString("miMarriageable.toolTipText"),
                    gui.getCampaign().getCampaignOptions().getMinimumMarriageAge()), 100));
            cbMenuItem.setName("miMarriageable");
            cbMenuItem.setSelected(person.isMarriageable());
            cbMenuItem.addActionListener(evt -> {
                final boolean marriageable = !person.isMarriageable();
                Stream.of(selected).forEach(p -> p.setMarriageable(marriageable));
            });
            menu.add(cbMenuItem);
        }

        if ((gui.getCampaign().getCampaignOptions().isUseManualProcreation()
                || !gui.getCampaign().getCampaignOptions().getRandomProcreationMethod().isNone())
                && Stream.of(selected).allMatch(p -> p.getGender().isFemale())
                && Stream.of(selected).allMatch(p -> p.isTryingToConceive() == person.isTryingToConceive())) {
            cbMenuItem = new JCheckBoxMenuItem(resources.getString("miTryingToConceive.text"));
            cbMenuItem.setToolTipText(MultiLineTooltip.splitToolTip(resources.getString("miTryingToConceive.toolTipText"), 100));
            cbMenuItem.setName("miTryingToConceive");
            cbMenuItem.setSelected(person.isTryingToConceive());
            cbMenuItem.addActionListener(evt -> {
                final boolean tryingToConceive = !person.isTryingToConceive();
                Stream.of(selected).forEach(p -> p.setTryingToConceive(tryingToConceive));
            });
            menu.add(cbMenuItem);
        }

        JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        //endregion Flags Menu

        //region Randomization Menu
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

        menuItem = new JMenuItem(resources.getString(oneSelected ? "miRandomName.single.text" : "miRandomName.bulk.text"));
        menuItem.setName("miRandomName");
        menuItem.setActionCommand(CMD_RANDOM_NAME);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        if (StaticChecks.areAllClanEligible(selected)) {
            menuItem = new JMenuItem(resources.getString(oneSelected ? "miRandomBloodnameCheck.single.text" : "miRandomBloodnameCheck.bulk.text"));
            menuItem.setName("miRandomBloodnameCheck");
            menuItem.setActionCommand(makeCommand(CMD_RANDOM_BLOODNAME, String.valueOf(false)));
            menuItem.addActionListener(this);
            menu.add(menuItem);

            if (gui.getCampaign().isGM()) {
                menuItem = new JMenuItem(resources.getString(oneSelected ? "miRandomBloodname.single.text" : "miRandomBloodname.bulk.text"));
                menuItem.setName("miRandomBloodname");
                menuItem.setActionCommand(makeCommand(CMD_RANDOM_BLOODNAME, String.valueOf(true)));
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }
        }

        menuItem = new JMenuItem(resources.getString(oneSelected ? "miRandomCallsign.single.text" : "miRandomCallsign.bulk.text"));
        menuItem.setName("miRandomCallsign");
        menuItem.setActionCommand(CMD_RANDOM_CALLSIGN);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem(resources.getString(oneSelected ? "miRandomPortrait.single.text" : "miRandomPortrait.bulk.text"));
        menuItem.setName("miRandomPortrait");
        menuItem.setActionCommand(CMD_RANDOM_PORTRAIT);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        if (gui.getCampaign().getCampaignOptions().getRandomOriginOptions().isRandomizeOrigin()) {
            menuItem = new JMenuItem(resources.getString(oneSelected ? "miRandomOrigin.single.text" : "miRandomOrigin.bulk.text"));
            menuItem.setName("miRandomOrigin");
            menuItem.setActionCommand(CMD_RANDOM_ORIGIN);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString(oneSelected ? "miRandomOriginFaction.single.text" : "miRandomOriginFaction.bulk.text"));
            menuItem.setName("miRandomOriginFaction");
            menuItem.setActionCommand(CMD_RANDOM_ORIGIN_FACTION);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString(oneSelected ? "miRandomOriginPlanet.single.text" : "miRandomOriginPlanet.bulk.text"));
            menuItem.setName("miRandomOriginPlanet");
            menuItem.setActionCommand(CMD_RANDOM_ORIGIN_PLANET);
            menuItem.addActionListener(this);
            menu.add(menuItem);
        }

        JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        //endregion Randomization Menu

        //region GM Menu
        if (gui.getCampaign().isGM()) {
            popup.addSeparator();

            menu = new JMenu(resources.getString("GMMode.text"));

            menuItem = new JMenu(resources.getString("changePrisonerStatus.text"));
            menuItem.add(newCheckboxMenu(
                    PrisonerStatus.FREE.toString(),
                    makeCommand(CMD_CHANGE_PRISONER_STATUS, PrisonerStatus.FREE.name()),
                    (person.getPrisonerStatus() == PrisonerStatus.FREE)));
            menuItem.add(newCheckboxMenu(
                    PrisonerStatus.PRISONER.toString(),
                    makeCommand(CMD_CHANGE_PRISONER_STATUS, PrisonerStatus.PRISONER.name()),
                    (person.getPrisonerStatus() == PrisonerStatus.PRISONER)));
            menuItem.add(newCheckboxMenu(
                    PrisonerStatus.PRISONER_DEFECTOR.toString(),
                    makeCommand(CMD_CHANGE_PRISONER_STATUS, PrisonerStatus.PRISONER_DEFECTOR.name()),
                    (person.getPrisonerStatus() == PrisonerStatus.PRISONER_DEFECTOR)));
            menuItem.add(newCheckboxMenu(
                    PrisonerStatus.BONDSMAN.toString(),
                    makeCommand(CMD_CHANGE_PRISONER_STATUS, PrisonerStatus.BONDSMAN.name()),
                    (person.getPrisonerStatus() == PrisonerStatus.BONDSMAN)));
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("removePerson.text"));
            menuItem.setActionCommand(CMD_REMOVE);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            if (!gui.getCampaign().getCampaignOptions().isUseAdvancedMedical()) {
                menuItem = new JMenuItem(resources.getString("editHits.text"));
                menuItem.setActionCommand(CMD_EDIT_HITS);
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }

            menuItem = new JMenuItem(resources.getString("add1XP.text"));
            menuItem.setActionCommand(CMD_ADD_1_XP);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("addXP.text"));
            menuItem.setActionCommand(CMD_ADD_XP);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem(resources.getString("setXP.text"));
            menuItem.setActionCommand(CMD_SET_XP);
            menuItem.addActionListener(this);
            menu.add(menuItem);

            if (gui.getCampaign().getCampaignOptions().isUseEdge()) {
                menuItem = new JMenuItem(resources.getString("setEdge.text"));
                menuItem.setActionCommand(CMD_SET_EDGE);
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }

            if (oneSelected) {
                menuItem = new JMenuItem(resources.getString("edit.text"));
                menuItem.setActionCommand(CMD_EDIT);
                menuItem.addActionListener(this);
                menu.add(menuItem);

                menuItem = new JMenuItem(resources.getString("loadGMTools.text"));
                menuItem.addActionListener(evt -> loadGMToolsForPerson(person));
                menu.add(menuItem);
            }

            if (gui.getCampaign().getCampaignOptions().isUseAdvancedMedical()) {
                menuItem = new JMenuItem(resources.getString("removeAllInjuries.text"));
                menuItem.setActionCommand(CMD_CLEAR_INJURIES);
                menuItem.addActionListener(this);
                menu.add(menuItem);

                if (oneSelected) {
                    for (Injury i : person.getInjuries()) {
                        menuItem = new JMenuItem(String.format(resources.getString("removeInjury.format"), i.getName()));
                        menuItem.setActionCommand(makeCommand(CMD_REMOVE_INJURY, i.getUUID().toString()));
                        menuItem.addActionListener(this);
                        menu.add(menuItem);
                    }

                    menuItem = new JMenuItem(resources.getString("editInjuries.text"));
                    menuItem.setActionCommand(CMD_EDIT_INJURIES);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                }
            }

            if (gui.getCampaign().getCampaignOptions().isUseManualProcreation()) {
                if (Stream.of(selected).anyMatch(p -> gui.getCampaign().getProcreation()
                        .canProcreate(gui.getCampaign().getLocalDate(), p, false) == null)) {
                    menuItem = new JMenuItem(resources.getString(oneSelected ? "addPregnancy.text" : "addPregnancies.text"));
                    menuItem.setActionCommand(CMD_ADD_PREGNANCY);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                }

                if (Stream.of(selected).anyMatch(Person::isPregnant)) {
                    menuItem = new JMenuItem(resources.getString(oneSelected ? "removePregnancy.text" : "removePregnancies.text"));
                    menuItem.setActionCommand(CMD_REMOVE_PREGNANCY);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                }
            }

            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        }
        //endregion GM Menu

        return Optional.of(popup);
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
