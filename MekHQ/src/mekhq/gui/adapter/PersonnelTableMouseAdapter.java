package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.UUID;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Crew;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.logging.LogLevel;
import megamek.common.options.PilotOptions;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Kill;
import mekhq.campaign.LogEntry;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.event.PersonLogEvent;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.Rank;
import mekhq.campaign.personnel.Ranks;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.CustomizePersonDialog;
import mekhq.gui.dialog.EditKillLogDialog;
import mekhq.gui.dialog.EditLogEntryDialog;
import mekhq.gui.dialog.EditPersonnelInjuriesDialog;
import mekhq.gui.dialog.EditPersonnelLogDialog;
import mekhq.gui.dialog.ImageChoiceDialog;
import mekhq.gui.dialog.KillDialog;
import mekhq.gui.dialog.PopupValueChoiceDialog;
import mekhq.gui.dialog.RetirementDefectionDialog;
import mekhq.gui.dialog.TextAreaDialog;
import mekhq.gui.model.PersonnelTableModel;
import mekhq.gui.utilities.MenuScroller;
import mekhq.gui.utilities.StaticChecks;

public class PersonnelTableMouseAdapter extends MouseInputAdapter implements
        ActionListener {
    private static final String CMD_RANKSYSTEM = "RANKSYSTEM"; //$NON-NLS-1$
    private static final String CMD_RANK = "RANK"; //$NON-NLS-1$
    private static final String CMD_MANEI_DOMINI_RANK = "MD_RANK"; //$NON-NLS-1$
    private static final String CMD_MANEI_DOMINI_CLASS = "MD_CLASS"; //$NON-NLS-1$
    private static final String CMD_PRIMARY_ROLE = "PROLE"; //$NON-NLS-1$
    private static final String CMD_SECONDARY_ROLE = "SROLE"; //$NON-NLS-1$
    private static final String CMD_PRIMARY_DESIGNATOR = "DESIG_PRI"; //$NON-NLS-1$
    private static final String CMD_SECONDARY_DESIGNATOR = "DESIG_SEC"; //$NON-NLS-1$
    private static final String CMD_REMOVE_UNIT = "REMOVE_UNIT"; //$NON-NLS-1$
    private static final String CMD_ADD_PILOT = "ADD_PILOT"; //$NON-NLS-1$
    private static final String CMD_ADD_SOLDIER = "ADD_SOLDIER"; //$NON-NLS-1$
    private static final String CMD_ADD_DRIVER = "ADD_DRIVER"; //$NON-NLS-1$
    private static final String CMD_ADD_VESSEL_PILOT = "ADD_VESSEL_PILOT"; //$NON-NLS-1$
    private static final String CMD_ADD_GUNNER = "ADD_GUNNER"; //$NON-NLS-1$
    private static final String CMD_ADD_CREW = "ADD_CREW"; //$NON-NLS-1$
    private static final String CMD_ADD_NAVIGATOR = "ADD_NAV"; //$NON-NLS-1$
    private static final String CMD_ADD_TECH_OFFICER = "ADD_TECH_OFFICER"; //$NON-NLS-1$

    private static final String CMD_EDIT_SALARY = "SALARY"; //$NON-NLS-1$
    private static final String CMD_BLOODNAME = "BLOODNAME"; //$NON-NLS-1$
    private static final String CMD_EDIT_INJURIES = "EDIT_INJURIES"; //$NON-NLS-1$
    private static final String CMD_REMOVE_INJURY = "REMOVE_INJURY"; //$NON-NLS-1$
    private static final String CMD_CLEAR_INJURIES = "CLEAR_INJURIES"; //$NON-NLS-1$
    private static final String CMD_CALLSIGN = "CALLSIGN"; //$NON-NLS-1$
    private static final String CMD_DEPENDENT = "DEPENDENT"; //$NON-NLS-1$
    private static final String CMD_COMMANDER = "COMMANDER"; //$NON-NLS-1$
    private static final String CMD_EDIT_LOG_ENTRY = "LOG_SINGLE"; //$NON-NLS-1$
    private static final String CMD_EDIT_PERSONNEL_LOG = "LOG"; //$NON-NLS-1$
    private static final String CMD_EDIT_KILL_LOG = "KILL_LOG"; //$NON-NLS-1$
    private static final String CMD_KILL = "KILL"; //$NON-NLS-1$
    private static final String CMD_BUY_EDGE = "EDGE_BUY"; //$NON-NLS-1$
    private static final String CMD_SET_EDGE = "EDGE_SET"; //$NON-NLS-1$
    private static final String CMD_SET_XP = "XP_SET"; //$NON-NLS-1$
    private static final String CMD_ADD_XP = "XP_ADD"; //$NON-NLS-1$
    private static final String CMD_EDIT_BIOGRAPHY = "BIOGRAPHY"; //$NON-NLS-1$
    private static final String CMD_EDIT_PORTRAIT = "PORTRAIT"; //$NON-NLS-1$
    private static final String CMD_HEAL = "HEAL"; //$NON-NLS-1$
    private static final String CMD_EDIT = "EDIT"; //$NON-NLS-1$
    private static final String CMD_SACK = "SACK"; //$NON-NLS-1$
    private static final String CMD_REMOVE = "REMOVE"; //$NON-NLS-1$
    private static final String CMD_EDGE_TRIGGER = "EDGE"; //$NON-NLS-1$
    private static final String CMD_CHANGE_PRISONER_STATUS = "PRISONER_STATUS"; //$NON-NLS-1$
    private static final String CMD_CHANGE_STATUS = "STATUS"; //$NON-NLS-1$
    private static final String CMD_ACQUIRE_SPECIALIST = "SPECIALIST"; //$NON-NLS-1$
    private static final String CMD_ACQUIRE_WEAPON_SPECIALIST = "WSPECIALIST"; //$NON-NLS-1$
    private static final String CMD_ACQUIRE_RANGEMASTER = "RANGEMASTER"; //$NON-NLS-1$
    private static final String CMD_ACQUIRE_HUMANTRO = "HUMANTRO"; //$NON-NLS-1$
    private static final String CMD_ACQUIRE_ABILITY = "ABILITY"; //$NON-NLS-1$
    private static final String CMD_IMPROVE = "IMPROVE"; //$NON-NLS-1$
    private static final String CMD_ADD_SPOUSE = "SPOUSE"; //$NON-NLS-1$
    private static final String CMD_REMOVE_SPOUSE = "REMOVE_SPOUSE"; //$NON-NLS-1$
    private static final String CMD_ADD_PREGNANCY = "ADD_PREGNANCY"; //$NON-NLS-1$
    private static final String CMD_REMOVE_PREGNANCY = "PREGNANCY_SPOUSE"; //$NON-NLS-1$
    private static final String CMD_ADD_TECH = "ADD_TECH"; //$NON-NLS-1$

    private static final String CMD_IMPRISON = "IMPRISON"; //$NON-NLS-1$
    private static final String CMD_FREE = "FREE"; //$NON-NLS-1$
    private static final String CMD_RECRUIT = "RECRUIT"; //$NON-NLS-1$
    private static final String CMD_RANSOM = "RANSOM";
    
    private static final String SEPARATOR = "@"; //$NON-NLS-1$
    private static final String SPACE = " "; //$NON-NLS-1$
    private static final String HYPHEN = "-"; //$NON-NLS-1$
    private static final String QUESTION_MARK = "?"; //$NON-NLS-1$
    private static final String TRUE = String.valueOf(true);
    private static final String FALSE = String.valueOf(false);

    private CampaignGUI gui;
    private JTable personnelTable;
    private PersonnelTableModel personnelModel;
    private ResourceBundle resourceMap = null;

    public PersonnelTableMouseAdapter(CampaignGUI gui, JTable personnelTable,
            PersonnelTableModel personnelModel) {
        super();
        this.gui = gui;
        this.personnelTable = personnelTable;
        this.personnelModel = personnelModel;
        resourceMap = ResourceBundle.getBundle("mekhq.resources.PersonnelTableMouseAdapter", new EncodeControl()); //$NON-NLS-1$
    }

    private static final String OPT_SURNAME_NO_CHANGE = "no_change"; //$NON-NLS-1$
    private static final String OPT_SURNAME_YOURS = "yours"; //$NON-NLS-1$
    private static final String OPT_SURNAME_SPOUSE = "spouse"; //$NON-NLS-1$
    private static final String OPT_SURNAME_HYP_YOURS = "hyp_yours"; //$NON-NLS-1$
    private static final String OPT_SURNAME_HYP_SPOUSE = "hyp_spouse"; //$NON-NLS-1$

    private static final String OPT_EDGE_MASC_FAILURE = "edge_when_masc_fails"; //$NON-NLS-1$
    private static final String OPT_EDGE_EXPLOSION = "edge_when_explosion"; //$NON-NLS-1$
    private static final String OPT_EDGE_KO = "edge_when_ko"; //$NON-NLS-1$
    private static final String OPT_EDGE_TAC = "edge_when_tac"; //$NON-NLS-1$
    private static final String OPT_EDGE_HEADHIT = "edge_when_headhit"; //$NON-NLS-1$
    
    private static final String OPT_PRISONER_FREE = "free"; //$NON-NLS-1$
    private static final String OPT_PRISONER_IMPRISONED = "imprisoned"; //$NON-NLS-1$
    private static final String OPT_PRISONER_IMPRISONED_DEFECTING = "imprisoned_defecting"; //$NON-NLS-1$
    private static final String OPT_PRISONER_BONDSMAN = "bondsman"; //$NON-NLS-1$

    private String makeCommand(String ... parts) {
        return Utilities.combineString(Arrays.asList(parts), SEPARATOR);
    }

    @Override
    public void actionPerformed(ActionEvent action) {
        final String METHOD_NAME = "actionPerformed(ActionEvent)"; //$NON-NLS-1$
        
        int row = personnelTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Person selectedPerson = personnelModel.getPerson(personnelTable
                .convertRowIndexToModel(row));
        int[] rows = personnelTable.getSelectedRows();
        Person[] people = new Person[rows.length];
        for (int i = 0; i < rows.length; i++) {
            people[i] = personnelModel.getPerson(personnelTable
                    .convertRowIndexToModel(rows[i]));
        }

        String[] data = action.getActionCommand().split(SEPARATOR, -1);
        String command = data[0];

        switch(command) {
            case CMD_RANKSYSTEM:
                int system = Integer.parseInt(data[1]);
                for (Person person : people) {
                    person.setRankSystem(system);
                }
                break;
            case CMD_RANK:
                int rank = Integer.parseInt(data[1]);
                int level = 0;
                // Check to see if we added a rank level...
                if (data.length > 2) {
                    level = Integer.parseInt(data[2]);
                }

                for (Person person : people) {
                    gui.getCampaign().changeRank(person, rank, level, true);
                }
                break;
            case CMD_MANEI_DOMINI_RANK:
                int md_rank = Integer.parseInt(data[1]);
                for (Person person : people) {
                    person.setManeiDominiRank(md_rank);
                }
                break;
            case CMD_MANEI_DOMINI_CLASS:
                int md_class = Integer.parseInt(data[1]);
                for (Person person : people) {
                    person.setManeiDominiClass(md_class);
                }
                break;
            case CMD_PRIMARY_DESIGNATOR:
                int designation = Integer.parseInt(data[1]);
                for (Person person : people) {
                    person.setPrimaryDesignator(designation);
                }
                break;
            case CMD_SECONDARY_DESIGNATOR:
                int secDesignation = Integer.parseInt(data[1]);
                for (Person person : people) {
                    person.setSecondaryDesignator(secDesignation);
                }
                break;
            case CMD_PRIMARY_ROLE:
                int role = Integer.parseInt(data[1]);
                for (Person person : people) {
                    person.setPrimaryRole(role);
                    gui.getCampaign().personUpdated(person);
                }
                break;
            case CMD_SECONDARY_ROLE:
                int secRole = Integer.parseInt(data[1]);
                for (Person person : people) {
                    person.setSecondaryRole(secRole);
                    gui.getCampaign().personUpdated(person);
                }
                break;
            case CMD_REMOVE_UNIT:
                for (Person person : people) {
                    Unit u = gui.getCampaign().getUnit(person.getUnitId());
                    if (null != u) {
                        u.remove(person, true);
                        u.resetEngineer();
                        u.runDiagnostic(false);
                    }
                    // check for tech unit assignments
                    if (!person.getTechUnitIDs().isEmpty()) {
                        // I need to create a new array list to avoid concurrent
                        // problems
                        ArrayList<UUID> temp = new ArrayList<UUID>();
                        for (UUID i : person.getTechUnitIDs()) {
                            temp.add(i);
                        }
                        for (UUID i : temp) {
                            u = gui.getCampaign().getUnit(i);
                            if (null != u) {
                                u.remove(person, true);
                                u.resetEngineer();
                                u.runDiagnostic(false);
                            }
                        }
                        /*
                         * Incase there's still some assignments for this tech,
                         * clear them out. This can happen if the target unit
                         * above is null. The tech will still have the pointer
                         * but to a null unit and it will never go away 
                         * otherwise.
                         */
                        person.clearTechUnitIDs();
                    }
                }
                break;
            case CMD_ADD_PILOT:
            {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                Unit oldUnit = gui.getCampaign().getUnit(selectedPerson.getUnitId());
                boolean useTransfers = false;
                boolean transferLog = !gui.getCampaign().getCampaignOptions().useTransfers();
                if (null != oldUnit) {
                    oldUnit.remove(selectedPerson, transferLog);
                    useTransfers = gui.getCampaign().getCampaignOptions().useTransfers();
                }
                if (null != u) {
                    u.addPilotOrSoldier(selectedPerson, useTransfers);
                }
                u.resetPilotAndEntity();
                u.runDiagnostic(false);
                break;
            }
            case CMD_ADD_SOLDIER:
            {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                if (null != u) {
                    for (Person p : people) {
                        if (u.canTakeMoreGunners()) {
                            Unit oldUnit = gui.getCampaign().getUnit(p.getUnitId());
                            boolean useTransfers = false;
                            boolean transferLog = !gui.getCampaign().getCampaignOptions().useTransfers();
                            if (null != oldUnit) {
                                oldUnit.remove(p, transferLog);
                                useTransfers = gui.getCampaign().getCampaignOptions().useTransfers();
                            }
                            u.addPilotOrSoldier(p, useTransfers);
                        }
                    }
                }
                u.resetPilotAndEntity();
                u.runDiagnostic(false);
                break;
            }
            case CMD_ADD_DRIVER:
            {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                Unit oldUnit = gui.getCampaign().getUnit(selectedPerson.getUnitId());
                boolean useTransfers = false;
                boolean transferLog = !gui.getCampaign().getCampaignOptions().useTransfers();
                if (null != oldUnit) {
                    oldUnit.remove(selectedPerson, transferLog);
                    useTransfers = gui.getCampaign().getCampaignOptions().useTransfers();
                }
                if (null != u) {
                    u.addDriver(selectedPerson, useTransfers);
                }
                u.resetPilotAndEntity();
                u.runDiagnostic(false);
                break;
            }
            case CMD_ADD_VESSEL_PILOT:
            {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                if (null != u) {
                    for (Person p : people) {
                        if (u.canTakeMoreDrivers()) {
                            Unit oldUnit = gui.getCampaign().getUnit(p.getUnitId());
                            boolean useTransfers = false;
                            boolean transferLog = !gui.getCampaign().getCampaignOptions().useTransfers();
                            if (null != oldUnit) {
                                oldUnit.remove(p, transferLog);
                                useTransfers = gui.getCampaign().getCampaignOptions().useTransfers();
                            }
                            u.addDriver(p, useTransfers);
                        }
                    }
                }
                u.resetPilotAndEntity();
                u.runDiagnostic(false);
                break;
            }
            case CMD_ADD_GUNNER:
            {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                if (null != u) {
                    for (Person p : people) {
                        if (u.canTakeMoreGunners()) {
                            Unit oldUnit = gui.getCampaign().getUnit(p.getUnitId());
                            boolean useTransfers = false;
                            boolean transferLog = !gui.getCampaign().getCampaignOptions().useTransfers();
                            if (null != oldUnit) {
                                oldUnit.remove(p, transferLog);
                                useTransfers = gui.getCampaign().getCampaignOptions().useTransfers();
                            }
                            u.addGunner(p, useTransfers);
                        }
                    }
                }
                u.resetPilotAndEntity();
                u.runDiagnostic(false);
                break;
            }
            case CMD_ADD_CREW:
            {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                if (null != u) {
                    for (Person p : people) {
                        if (u.canTakeMoreVesselCrew()) {
                            Unit oldUnit = gui.getCampaign().getUnit(p.getUnitId());
                            boolean useTransfers = false;
                            boolean transferLog = !gui.getCampaign().getCampaignOptions().useTransfers();
                            if (null != oldUnit) {
                                oldUnit.remove(p, transferLog);
                                useTransfers = gui.getCampaign().getCampaignOptions().useTransfers();
                            }
                            u.addVesselCrew(p, useTransfers);
                        }
                    }
                }
                u.resetPilotAndEntity();
                u.runDiagnostic(false);
                break;
            }
            case CMD_ADD_NAVIGATOR:
            {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                if (null != u) {
                    for (Person p : people) {
                        if (u.canTakeNavigator()) {
                            Unit oldUnit = gui.getCampaign().getUnit(p.getUnitId());
                            boolean useTransfers = false;
                            boolean transferLog = !gui.getCampaign().getCampaignOptions().useTransfers();
                            if (null != oldUnit) {
                                oldUnit.remove(p, transferLog);
                                useTransfers = gui.getCampaign().getCampaignOptions().useTransfers();
                            }
                            u.setNavigator(p, useTransfers);
                        }
                    }
                }
                u.resetPilotAndEntity();
                u.runDiagnostic(false);
                break;
            }
            case CMD_ADD_TECH_OFFICER:
            {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                if (null != u) {
                    for (Person p : people) {
                        if (u.canTakeTechOfficer()) {
                            Unit oldUnit = gui.getCampaign().getUnit(p.getUnitId());
                            boolean useTransfers = false;
                            boolean transferLog = !gui.getCampaign().getCampaignOptions().useTransfers();
                            if (null != oldUnit) {
                                oldUnit.remove(p, transferLog);
                                useTransfers = gui.getCampaign().getCampaignOptions().useTransfers();
                            }
                            u.setTechOfficer(p, useTransfers);
                        }
                    }
                }
                u.resetPilotAndEntity();
                u.runDiagnostic(false);
                break;
            }
            case CMD_ADD_TECH:
            {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                if (null != u) {
                    if (u.canTakeTech()) {
                        u.setTech(selectedPerson);
                    }
                }
                u.resetPilotAndEntity();
                u.runDiagnostic(false);
                break;
            }
            case CMD_ADD_PREGNANCY:
            {
                if (selectedPerson.isFemale()) {
                    selectedPerson.addPregnancy();
                    MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                }
                break;
            }
            case CMD_REMOVE_PREGNANCY:
            {
                if (selectedPerson.isPregnant()) {
                    selectedPerson.removePregnancy();
                    MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                }
                break;
            }
            case CMD_REMOVE_SPOUSE:
            {
                if (!selectedPerson.getSpouse().isDeadOrMIA()) {
                    selectedPerson.getSpouse().addLogEntry(gui.getCampaign().getDate(),
                        String.format(resourceMap.getString("divorcedFrom.format"), selectedPerson.getFullName())); //$NON-NLS-1$
                    selectedPerson.addLogEntry(gui.getCampaign().getDate(),
                        String.format(resourceMap.getString("divorcedFrom.format"), selectedPerson.getSpouse().getFullName())); //$NON-NLS-1$
                    if (selectedPerson.getMaidenName() != null) {
                        selectedPerson.setName(selectedPerson.getName().split(SPACE, 2)[0]
                                + SPACE
                                + selectedPerson.getMaidenName());
                    }
                    if (selectedPerson.getSpouse().getMaidenName() != null) {
                        Person spouse = selectedPerson.getSpouse();
                        spouse.setName(spouse.getName().split(SPACE, 2)[0]
                                + SPACE
                                + spouse.getMaidenName());
                    }
                }
                selectedPerson.getSpouse().setSpouseID(null);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson.getSpouse()));
                selectedPerson.setSpouseID(null);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                break;
            }
            case CMD_ADD_SPOUSE:
            {
                Person spouse = gui.getCampaign().getPerson(UUID.fromString(data[1]));
                String surnameOption = data[2];

                String selectedSurname = selectedPerson.getName().split(SPACE, 2)[1];
                String spouseSurname = spouse.getName().split(SPACE, 2)[1];
                String selectedGivenname = selectedPerson.getName().split(SPACE, 2)[0];
                String spouseGivenname = spouse.getName().split(SPACE, 2)[0];

                switch (surnameOption) {
                    case OPT_SURNAME_NO_CHANGE:
                        break;
                    case OPT_SURNAME_YOURS:
                        spouse.setName(spouseGivenname + SPACE + selectedSurname);
                        spouse.setMaidenName(spouseSurname);
                        break;
                    case OPT_SURNAME_SPOUSE:
                        selectedPerson.setName(selectedGivenname + SPACE + spouseSurname);
                        selectedPerson.setMaidenName(selectedSurname);
                        break;
                    case OPT_SURNAME_HYP_YOURS:
                        selectedPerson.setName(selectedGivenname + SPACE + selectedSurname + HYPHEN + spouseSurname);
                        selectedPerson.setMaidenName(selectedSurname);
                        break;
                    case OPT_SURNAME_HYP_SPOUSE:
                        spouse.setName(spouseGivenname + SPACE + spouseSurname+ HYPHEN + selectedSurname);
                        spouse.setMaidenName(spouseSurname);
                        break;
                    default:
                        spouse.setName(spouseGivenname + SPACE + "ImaError"); //$NON-NLS-1$
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                                String.format("Unknown error in Surname chooser between \"%s\" and \"%s\"", //$NON-NLS-1$
                            selectedPerson.getFullName(), spouse.getFullName()));
                        break;
                }

                spouse.setSpouseID(selectedPerson.getId());
                spouse.addLogEntry(gui.getCampaign().getDate(), String.format(resourceMap.getString("marries.format"), selectedPerson.getFullName())); //$NON-NLS-1$
                selectedPerson.setSpouseID(spouse.getId());
                selectedPerson.addLogEntry(gui.getCampaign().getDate(), String.format(resourceMap.getString("marries.format"), spouse.getFullName())); //$NON-NLS-1$
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                MekHQ.triggerEvent(new PersonChangedEvent(spouse));
                break;
            }
            case CMD_IMPROVE:
            {
                String type = data[1];
                int cost = Integer.parseInt(data[2]);
                int oldExpLevel = selectedPerson.getExperienceLevel(false);
                selectedPerson.improveSkill(type);
                gui.getCampaign().personUpdated(selectedPerson);
                selectedPerson.setXp(selectedPerson.getXp() - cost);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                gui.getCampaign().addReport(String.format(resourceMap.getString("improved.format"), selectedPerson.getHyperlinkedName(), type)); //$NON-NLS-1$
                if (gui.getCampaign().getCampaignOptions().getUseAtB()
                		&& gui.getCampaign().getCampaignOptions().useAbilities()) {
                    if (selectedPerson.getPrimaryRole() > Person.T_NONE
                            && selectedPerson.getPrimaryRole() <= Person.T_CONV_PILOT
                            && selectedPerson.getExperienceLevel(false) > oldExpLevel
                            && oldExpLevel >= SkillType.EXP_REGULAR) {
                        String spa = gui.getCampaign()
                                .rollSPA(selectedPerson.getPrimaryRole(),
                                        selectedPerson);
                        if (null == spa) {
                            if (gui.getCampaign().getCampaignOptions().useEdge()) {
                                selectedPerson.acquireAbility(
                                        PilotOptions.EDGE_ADVANTAGES, "edge", //$NON-NLS-1$
                                        selectedPerson.getEdge() + 1);
                                gui.getCampaign().addReport(String.format(resourceMap.getString("gainedEdge.format"), selectedPerson.getHyperlinkedName())); //$NON-NLS-1$
                            }
                        } else {
                            gui.getCampaign().addReport(String.format(resourceMap.getString("gained.format"), //$NON-NLS-1$
                                selectedPerson.getHyperlinkedName(), SpecialAbility.getDisplayName(spa)));
                        }
                    }
                }
                break;
            }
            case CMD_ACQUIRE_ABILITY:
            {
                String selected = data[1];
                int cost = Integer.parseInt(data[2]);
                selectedPerson.acquireAbility(PilotOptions.LVL3_ADVANTAGES,
                        selected, true);
                gui.getCampaign().personUpdated(selectedPerson);
                selectedPerson.setXp(selectedPerson.getXp() - cost);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                // TODO: add personnelTab.getCampaign() report
                break;
            }
            case CMD_ACQUIRE_WEAPON_SPECIALIST:
            {
                String selected = data[1];
                int cost = Integer.parseInt(data[2]);
                selectedPerson.acquireAbility(PilotOptions.LVL3_ADVANTAGES,
                        "weapon_specialist", selected); //$NON-NLS-1$
                gui.getCampaign().personUpdated(selectedPerson);
                selectedPerson.setXp(selectedPerson.getXp() - cost);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                // TODO: add campaign report
                break;
            }
            case CMD_ACQUIRE_SPECIALIST:
            {
                String selected = data[1];
                int cost = Integer.parseInt(data[2]);
                selectedPerson.acquireAbility(PilotOptions.LVL3_ADVANTAGES,
                        "specialist", selected); //$NON-NLS-1$
                gui.getCampaign().personUpdated(selectedPerson);
                selectedPerson.setXp(selectedPerson.getXp() - cost);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                // TODO: add campaign report
                break;
            }
            case CMD_ACQUIRE_RANGEMASTER:
            {
                String selected = data[1];
                int cost = Integer.parseInt(data[2]);
                selectedPerson.acquireAbility(PilotOptions.LVL3_ADVANTAGES,
                        "range_master", selected); //$NON-NLS-1$
                gui.getCampaign().personUpdated(selectedPerson);
                selectedPerson.setXp(selectedPerson.getXp() - cost);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                // TODO: add campaign report
                break;
            }
            case CMD_ACQUIRE_HUMANTRO:
            {
                String selected = data[1];
                int cost = Integer.parseInt(data[2]);
                selectedPerson.acquireAbility(PilotOptions.LVL3_ADVANTAGES,
                        "human_tro", selected); //$NON-NLS-1$
                gui.getCampaign().personUpdated(selectedPerson);
                selectedPerson.setXp(selectedPerson.getXp() - cost);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                // TODO: add campaign report
                break;
            }
            case CMD_CHANGE_STATUS:
            {
                int selected = Integer.parseInt(data[1]);
                for (Person person : people) {
                    if ((selected == Person.S_ACTIVE) || (0 == JOptionPane.showConfirmDialog(null,
                        String.format(resourceMap.getString("confirmRetireQ.format"), person.getFullTitle()), //$NON-NLS-1$
                        resourceMap.getString("kiaQ.text"), JOptionPane.YES_NO_OPTION))) { //$NON-NLS-1$
                        gui.getCampaign().changeStatus(person, selected);
                    }
                }
                break;
            }
            case CMD_CHANGE_PRISONER_STATUS:
                String selected = data[1];
                for (Person person : people) {
                switch(selected) {
                    case OPT_PRISONER_FREE:
                        gui.getCampaign().changePrisonerStatus(person, Person.PRISONER_NOT);
                        break;
                    case OPT_PRISONER_IMPRISONED:
                        gui.getCampaign().changePrisonerStatus(person, Person.PRISONER_YES);
                        break;
                    case OPT_PRISONER_IMPRISONED_DEFECTING:
                        gui.getCampaign().changePrisonerStatus(person, Person.PRISONER_YES);
                        person.setWillingToDefect(true);
                        break;
                    case OPT_PRISONER_BONDSMAN:
                        gui.getCampaign().changePrisonerStatus(person, Person.PRISONER_BONDSMAN);
                        break;
                    default:
                        // U WOT M8?
                        break;
                    }
                }
                break;
            case CMD_IMPRISON:
                gui.getCampaign().changePrisonerStatus(selectedPerson, Person.PRISONER_YES);
                break;
            case CMD_FREE:
                // TODO: Warn in particular for "freeing" in deep space, leading to Geneva Conventions violation
                // TODO: Record the people into some NPC pool, if still alive
                if(0 == JOptionPane.showConfirmDialog(
                        null,
                        String.format(resourceMap.getString("confirmFree.format"), selectedPerson.getFullTitle()), //$NON-NLS-1$
                        resourceMap.getString("freeQ.text"), //$NON-NLS-1$
                        JOptionPane.YES_NO_OPTION)) {
                    gui.getCampaign().removePerson(selectedPerson.getId());
                }
                break;
            case CMD_RECRUIT:
                gui.getCampaign().changePrisonerStatus(selectedPerson, Person.PRISONER_NOT);
                break;
            case CMD_RANSOM:
                // ask the user if they want to sell off their prisoners. If yes, then add a daily report entry, add the money and remove them all.
                int total = 0;
                for(Person person : people) {
                    total += person.getRansomValue();
                }
                
                if (0 == JOptionPane.showConfirmDialog(
                        null,
                        String.format(resourceMap.getString("ransomQ.format"), people.length, total), //$NON-NLS-1$
                        resourceMap.getString("ransom.text"), //$NON-NLS-1$
                        JOptionPane.YES_NO_OPTION)) {
                    
                    gui.getCampaign().addReport(String.format(resourceMap.getString("ransomReport.format"), people.length, total));
                    gui.getCampaign().addFunds(total, resourceMap.getString("ransom.text"), Transaction.C_MISC);
                    for (Person person : people) {
                        gui.getCampaign().removePerson(person.getId(), false);
                    }
                }
                break;
            case CMD_EDGE_TRIGGER:
            {
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
            case CMD_REMOVE:
            {
                String title = String.format(resourceMap.getString("numPersonnel.text"), people.length); //$NON-NLS-1$
                if(people.length == 1) {
                    title = people[0].getFullTitle();
                }
                if (0 == JOptionPane.showConfirmDialog(
                        null,
                        String.format(resourceMap.getString("confirmRemove.format"), title), //$NON-NLS-1$
                        resourceMap.getString("removeQ.text"), //$NON-NLS-1$
                        JOptionPane.YES_NO_OPTION)) {
                    for (Person person : people) {
                        gui.getCampaign().removePerson(person.getId());
                    }
                }
                break;
            }
            case CMD_SACK:
            {
                boolean showDialog = false;
                ArrayList<UUID> toRemove = new ArrayList<UUID>();
                for (Person person : people) {
                    if (gui.getCampaign().getRetirementDefectionTracker()
                            .removeFromCampaign(
                                    person,
                                    false,
                                    gui.getCampaign().getCampaignOptions()
                                            .getUseShareSystem() ? person
                                            .getNumShares(gui.getCampaign()
                                                    .getCampaignOptions()
                                                    .getSharesForAll()) : 0,
                                    gui.getCampaign(), null)) {
                        showDialog = true;
                    } else {
                        toRemove.add(person.getId());
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
                        for (UUID id : toRemove) {
                            gui.getCampaign().removePerson(id);
                        }
                    }
                } else {
                    String question;
                    if(people.length > 1) {
                        question = resourceMap.getString("confirmRemoveMultiple.text"); //$NON-NLS-1$
                    } else {
                        question = String.format(resourceMap.getString("confirmRemove.format"), people[0].getFullTitle()); //$NON-NLS-1$
                    }
                    if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                            null, question, resourceMap.getString("removeQ.text"), //$NON-NLS-1$
                            JOptionPane.YES_NO_OPTION)) {
                        for (Person person : people) {
                            gui.getCampaign().removePerson(person.getId());
                        }
                    }
                }
                break;
            }
            case CMD_EDIT:
                CustomizePersonDialog npd = new CustomizePersonDialog(
                        gui.getFrame(), true, selectedPerson, gui.getCampaign());
                npd.setVisible(true);
                gui.getCampaign().personUpdated(selectedPerson);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                break;
            case CMD_HEAL:
                for (Person person : people) {
                    person.setHits(0);
                    person.setDoctorId(null, gui.getCampaign().getCampaignOptions()
                            .getNaturalHealingWaitingPeriod());
                }
                gui.getCampaign().personUpdated(selectedPerson);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                break;
            case CMD_EDIT_PORTRAIT:
                ImageChoiceDialog pcd = new ImageChoiceDialog(gui.getFrame(),
                        true, selectedPerson.getPortraitCategory(),
                        selectedPerson.getPortraitFileName(), gui.getIconPackage()
                                .getPortraits());
                pcd.setVisible(true);
                if (pcd.isChanged()) {
                    selectedPerson.setPortraitCategory(pcd.getCategory());
                    selectedPerson.setPortraitFileName(pcd.getFileName());
                    gui.getCampaign().personUpdated(selectedPerson);
                    MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                }
                break;
            case CMD_EDIT_BIOGRAPHY:
                TextAreaDialog tad = new TextAreaDialog(gui.getFrame(), true,
                        resourceMap.getString("editBiography.text"), selectedPerson.getBiography()); //$NON-NLS-1$
                tad.setVisible(true);
                if (tad.wasChanged()) {
                    selectedPerson.setBiography(tad.getText());
                    MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                }
                break;
            case CMD_ADD_XP:
                for (Person person : people) {
                    person.setXp(person.getXp() + 1);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            case CMD_SET_XP:
            {
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                        gui.getFrame(), true, resourceMap.getString("xp.text"), selectedPerson.getXp(), 0); //$NON-NLS-1$
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                int i = pvcd.getValue();
                for (Person person : people) {
                    person.setXp(i);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_BUY_EDGE:
            {
                int cost = gui.getCampaign().getCampaignOptions().getEdgeCost();
                for (Person person : people) {
                    selectedPerson.setXp(selectedPerson.getXp() - cost);
                    person.setEdge(person.getEdge() + 1);
                    gui.getCampaign().personUpdated(person);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_SET_EDGE:
            {
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                        gui.getFrame(), true, resourceMap.getString("edge.text"), selectedPerson.getEdge(), 0, //$NON-NLS-1$
                        10);
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                int i = pvcd.getValue();
                for (Person person : people) {
                    person.setEdge(i);
                    gui.getCampaign().personUpdated(person);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
            case CMD_KILL:
            {
                KillDialog nkd;
                Unit unit = gui.getCampaign().getUnit(selectedPerson.getUnitId());
                if (people.length > 1) {
                    nkd = new KillDialog(gui.getFrame(), true, new Kill(null, QUESTION_MARK,
                        unit != null ? unit.getName() : resourceMap.getString("bareHands.text"), gui.getCampaign().getDate()), resourceMap.getString("crew.text")); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    nkd = new KillDialog(gui.getFrame(), true, new Kill(selectedPerson.getId(), QUESTION_MARK,
                        unit != null ? unit.getName() : resourceMap.getString("bareHands.text"), gui.getCampaign().getDate()), selectedPerson.getFullName()); //$NON-NLS-1$
                }
                nkd.setVisible(true);
                if (!nkd.wasCancelled()) {
                    Kill kill = nkd.getKill();
                    if (people.length > 1) {
                        for (Person person : people) {
                            Kill k = kill.clone();
                            k.setPilotId(person.getId());
                            gui.getCampaign().addKill(k);
                        }
                    } else {
                        gui.getCampaign().addKill(kill);
                    }
                }
                break;
            }
            case CMD_EDIT_KILL_LOG:
                EditKillLogDialog ekld = new EditKillLogDialog(gui.getFrame(), true, gui.getCampaign(), selectedPerson);
                ekld.setVisible(true);
                MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                break;
            case CMD_EDIT_PERSONNEL_LOG:
                EditPersonnelLogDialog epld = new EditPersonnelLogDialog(gui.getFrame(), true, gui.getCampaign(), selectedPerson);
                epld.setVisible(true);
                MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                break;
            case CMD_EDIT_LOG_ENTRY:
                EditLogEntryDialog eeld = new EditLogEntryDialog(gui.getFrame(), true, new LogEntry(gui.getCampaign().getDate(), "")); //$NON-NLS-1$
                eeld.setVisible(true);
                LogEntry entry = eeld.getEntry();
                if (null != entry) {
                    for (Person person : people) {
                        person.addLogEntry(entry.clone());
                        MekHQ.triggerEvent(new PersonLogEvent(selectedPerson));
                    }
                }
                break;
            case CMD_COMMANDER:
            {
                selectedPerson.setCommander(!selectedPerson.isCommander());
                if (selectedPerson.isCommander()) {
                    for (Person p : gui.getCampaign().getPersonnel()) {
                        if (p.isCommander() && !p.getId().equals(selectedPerson.getId())) {
                            p.setCommander(false);
                            gui.getCampaign().addReport(String.format(resourceMap.getString("removedCommander.format"), p.getHyperlinkedFullTitle())); //$NON-NLS-1$
                            gui.getCampaign().personUpdated(p);
                            MekHQ.triggerEvent(new PersonChangedEvent(p));
                        }
                    }
                    gui.getCampaign().addReport(String.format(resourceMap.getString("setAsCommander.format"), selectedPerson.getHyperlinkedFullTitle())); //$NON-NLS-1$
                    gui.getCampaign().personUpdated(selectedPerson);
                    MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                }
                break;
            }
            case CMD_DEPENDENT:
            {
                if (people.length > 1) {
                    boolean status = Boolean.parseBoolean(data[1]);
                    for (Person person : people) {
                        person.setDependent(status);
                        gui.getCampaign().personUpdated(person);
                        MekHQ.triggerEvent(new PersonChangedEvent(person));
                    }
                } else {
                    selectedPerson.setDependent(!selectedPerson.isDependent());
                    gui.getCampaign().personUpdated(selectedPerson);
                    MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                }
                break;
            }
            case CMD_CALLSIGN:
                String s = (String) JOptionPane.showInputDialog(gui.getFrame(),
                        resourceMap.getString("enterNewCallsign.text"), resourceMap.getString("editCallsign.text"), //$NON-NLS-1$ //$NON-NLS-2$
                        JOptionPane.PLAIN_MESSAGE, null, null,
                        selectedPerson.getCallsign());
                if (null != s) {
                    selectedPerson.setCallsign(s);
                    MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                    gui.getCampaign().personUpdated(selectedPerson);
                }
                break;
            case CMD_CLEAR_INJURIES:
                for (Person person : people) {
                    person.clearInjuries();
                    Unit u = gui.getCampaign().getUnit(person.getUnitId());
                    if (null != u) {
                        u.resetPilotAndEntity();
                    }
                }
                break;
            case CMD_REMOVE_INJURY:
            {
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
                Unit u = gui.getCampaign().getUnit(selectedPerson.getUnitId());
                if (null != u) {
                    u.resetPilotAndEntity();
                }
                break;
            }
            case CMD_EDIT_INJURIES:
                EditPersonnelInjuriesDialog epid = new EditPersonnelInjuriesDialog(
                        gui.getFrame(), true, gui.getCampaign(), selectedPerson);
                epid.setVisible(true);
                MekHQ.triggerEvent(new PersonChangedEvent(selectedPerson));
                break;
            case CMD_BLOODNAME:
                for (Person p : people) {
                    if (!p.isClanner()) {
                        continue;
                    }
                    gui.getCampaign()
                            .checkBloodnameAdd(p, p.getPrimaryRole(), true);
                }
                gui.getCampaign().personUpdated(selectedPerson);
                break;
            case CMD_EDIT_SALARY:
            {
                PopupValueChoiceDialog pcvd = new PopupValueChoiceDialog(gui.getFrame(),
                        true, resourceMap.getString("changeSalary.text"), //$NON-NLS-1$
                        selectedPerson.getSalary(), -1, 100000);
                pcvd.setVisible(true);
                int salary = pcvd.getValue();
                if (salary < -1) {
                    return;
                }
                for (Person person : people) {
                    person.setSalary(salary);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
                break;
            }
        default:
            break;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
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

    private void maybeShowPopup(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();

        if (e.isPopupTrigger()) {
            if (personnelTable.getSelectedRowCount() == 0) {
                return;
            }
            int row = personnelTable.getSelectedRow();
            boolean oneSelected = personnelTable.getSelectedRowCount() == 1;
            Person person = personnelModel.getPerson(personnelTable
                    .convertRowIndexToModel(row));
            JMenuItem menuItem = null;
            JMenu menu = null;
            JMenu submenu = null;
            JCheckBoxMenuItem cbMenuItem = null;
            Person[] selected = getSelectedPeople();
            // **lets fill the pop up menu**//
            if (StaticChecks.areAllEligible(selected)) {
                menu = new JMenu(resourceMap.getString("changeRank.text")); //$NON-NLS-1$
                Ranks ranks = person.getRanks();
                for (int rankOrder = 0; rankOrder < Ranks.RC_NUM; rankOrder++) {
                    Rank rank = ranks.getAllRanks().get(rankOrder);
                    int profession = person.getProfession();

                    // Empty professions need swapped before the
                    // continuation
                    while (ranks.isEmptyProfession(profession)
                            && profession != Ranks.RPROF_MW) {
                        profession = ranks
                                .getAlternateProfession(profession);
                    }

                    if (rank.getName(profession).equals(HYPHEN)) {
                        continue;
                    }

                    // re-route through any profession redirections,
                    // starting with the empty profession check
                    while (rank.getName(profession).startsWith("--") //$NON-NLS-1$
                            && profession != Ranks.RPROF_MW) {
                        if (rank.getName(profession).equals("--")) { //$NON-NLS-1$
                            profession = ranks
                                    .getAlternateProfession(profession);
                        } else if (rank.getName(profession)
                                .startsWith("--")) { //$NON-NLS-1$
                            profession = ranks.getAlternateProfession(rank
                                    .getName(profession));
                        }
                    }

                    if (rank.getRankLevels(profession) > 0) {
                        submenu = new JMenu(rank.getName(profession));
                        for (int level = 0; level <= rank
                                .getRankLevels(profession); level++) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    rank.getName(profession)
                                            + Utilities.getRomanNumeralsFromArabicNumber(level, true));
                            cbMenuItem.setActionCommand(makeCommand(CMD_RANK, String.valueOf(rankOrder), String.valueOf(level)));
                            if (person.getRankNumeric() == rankOrder
                                    && person.getRankLevel() == level) {
                                cbMenuItem.setSelected(true);
                            }
                            cbMenuItem.addActionListener(this);
                            cbMenuItem.setEnabled(true);
                            submenu.add(cbMenuItem);
                        }
                        if (submenu.getItemCount() > 20) {
                            MenuScroller.setScrollerFor(submenu, 20);
                        }
                        menu.add(submenu);
                    } else {
                        cbMenuItem = new JCheckBoxMenuItem(
                                rank.getName(profession));
                        cbMenuItem.setActionCommand(makeCommand(CMD_RANK, String.valueOf(rankOrder)));
                        if (person.getRankNumeric() == rankOrder) {
                            cbMenuItem.setSelected(true);
                        }
                        cbMenuItem.addActionListener(this);
                        cbMenuItem.setEnabled(true);
                        menu.add(cbMenuItem);
                    }
                }
                if (menu.getItemCount() > 20) {
                    MenuScroller.setScrollerFor(menu, 20);
                }
                popup.add(menu);
            }
            menu = new JMenu(resourceMap.getString("changeRankSystem.text")); //$NON-NLS-1$
            // First allow them to revert to the campaign system
            cbMenuItem = new JCheckBoxMenuItem(resourceMap.getString("useCampaignRankSystem.text")); //$NON-NLS-1$
            cbMenuItem.setActionCommand(makeCommand(CMD_RANKSYSTEM, "-1")); //$NON-NLS-1$
            cbMenuItem.addActionListener(this);
            cbMenuItem.setEnabled(true);
            menu.add(cbMenuItem);
            for (int system = 0; system < Ranks.RS_NUM; system++) {
                if (system == Ranks.RS_CUSTOM) {
                    continue;
                }
                cbMenuItem = new JCheckBoxMenuItem(
                        Ranks.getRankSystemName(system));
                cbMenuItem.setActionCommand(makeCommand(CMD_RANKSYSTEM, String.valueOf(system)));
                cbMenuItem.addActionListener(this);
                cbMenuItem.setEnabled(true);
                if (system == person.getRanks().getRankSystem()) {
                    cbMenuItem.setSelected(true);
                }
                menu.add(cbMenuItem);
            }
            if (menu.getItemCount() > 20) {
                MenuScroller.setScrollerFor(menu, 20);
            }
            popup.add(menu);
            if (StaticChecks.areAllWoB(selected)) {
                // MD Ranks
                menu = new JMenu(resourceMap.getString("changeMDRank.text")); //$NON-NLS-1$
                for (int i = Rank.MD_RANK_NONE; i < Rank.MD_RANK_NUM; i++) {
                    cbMenuItem = new JCheckBoxMenuItem(
                            Rank.getManeiDominiRankName(i));
                    cbMenuItem.setActionCommand(makeCommand(CMD_MANEI_DOMINI_RANK, String.valueOf(i)));
                    cbMenuItem.addActionListener(this);
                    cbMenuItem.setEnabled(true);
                    if (i == person.getManeiDominiRank()) {
                        cbMenuItem.setSelected(true);
                    }
                    menu.add(cbMenuItem);
                }
                if (menu.getItemCount() > 20) {
                    MenuScroller.setScrollerFor(menu, 20);
                }
                popup.add(menu);

                // MD Classes
                menu = new JMenu(resourceMap.getString("changeMDClass.text")); //$NON-NLS-1$
                for (int i = Person.MD_NONE; i < Person.MD_NUM; i++) {
                    cbMenuItem = new JCheckBoxMenuItem(
                            Person.getManeiDominiClassNames(i, Ranks.RS_WOB));
                    cbMenuItem.setActionCommand(makeCommand(CMD_MANEI_DOMINI_CLASS, String.valueOf(i)));
                    cbMenuItem.addActionListener(this);
                    cbMenuItem.setEnabled(true);
                    if (i == person.getManeiDominiClass()) {
                        cbMenuItem.setSelected(true);
                    }
                    menu.add(cbMenuItem);
                }
                if (menu.getItemCount() > 20) {
                    MenuScroller.setScrollerFor(menu, 20);
                }
                popup.add(menu);
            }
            if (StaticChecks.areAllWoBOrComstar(selected)) {
                menu = new JMenu(resourceMap.getString("changePrimaryDesignation.text")); //$NON-NLS-1$
                for (int i = Person.DESIG_NONE; i < Person.DESIG_NUM; i++) {
                    cbMenuItem = new JCheckBoxMenuItem(
                            Person.parseDesignator(i));
                    cbMenuItem.setActionCommand(makeCommand(CMD_PRIMARY_DESIGNATOR, String.valueOf(i)));
                    cbMenuItem.addActionListener(this);
                    cbMenuItem.setEnabled(true);
                    if (i == person.getPrimaryDesignator()) {
                        cbMenuItem.setSelected(true);
                    }
                    menu.add(cbMenuItem);
                }
                if (menu.getItemCount() > 20) {
                    MenuScroller.setScrollerFor(menu, 20);
                }
                popup.add(menu);

                menu = new JMenu(resourceMap.getString("changeSecondaryDesignation.text")); //$NON-NLS-1$
                for (int i = Person.DESIG_NONE; i < Person.DESIG_NUM; i++) {
                    cbMenuItem = new JCheckBoxMenuItem(
                            Person.parseDesignator(i));
                    cbMenuItem.setActionCommand(makeCommand(CMD_SECONDARY_DESIGNATOR, String.valueOf(i)));
                    cbMenuItem.addActionListener(this);
                    cbMenuItem.setEnabled(true);
                    if (i == person.getSecondaryDesignator()) {
                        cbMenuItem.setSelected(true);
                    }
                    menu.add(cbMenuItem);
                }
                if (menu.getItemCount() > 20) {
                    MenuScroller.setScrollerFor(menu, 20);
                }
                popup.add(menu);
            }
            menu = new JMenu(resourceMap.getString("changeStatus.text")); //$NON-NLS-1$
            for (int s = 0; s < Person.S_NUM; s++) {
                cbMenuItem = new JCheckBoxMenuItem(Person.getStatusName(s));
                if (person.getStatus() == s) {
                    cbMenuItem.setSelected(true);
                }
                cbMenuItem.setActionCommand(makeCommand(CMD_CHANGE_STATUS, String.valueOf(s)));
                cbMenuItem.addActionListener(this);
                cbMenuItem.setEnabled(true);
                menu.add(cbMenuItem);
            }
            popup.add(menu);

            if(oneSelected) {
                popup.add(newMenuItem(resourceMap.getString("imprison.text"), CMD_IMPRISON, person.isFree())); //$NON-NLS-1$
                popup.add(newMenuItem(resourceMap.getString("free.text"), CMD_FREE, !person.isFree())); //$NON-NLS-1$
                popup.add(newMenuItem(resourceMap.getString("recruit.text"), CMD_RECRUIT, //$NON-NLS-1$
                        person.isBondsman() || person.isWillingToDefect()));
            }
            
            if(gui.getCampaign().getCampaignOptions().getUseAtB() &&
               gui.getCampaign().getCampaignOptions().getUseAtBCapture() &&
               StaticChecks.areAllPrisoners(selected)) {
                popup.add(newMenuItem(resourceMap.getString("ransom.text"), CMD_RANSOM));
            }

            menu = new JMenu(resourceMap.getString("changePrimaryRole.text")); //$NON-NLS-1$
            for (int i = Person.T_MECHWARRIOR; i < Person.T_NUM; i++) {
                if (person.canPerformRole(i)
                        && person.getSecondaryRole() != i) {
                    cbMenuItem = new JCheckBoxMenuItem(Person.getRoleDesc(
                            i, gui.getCampaign().getFaction().isClan()));
                    cbMenuItem.setActionCommand(makeCommand(CMD_PRIMARY_ROLE, String.valueOf(i)));
                    if (person.getPrimaryRole() == i) {
                        cbMenuItem.setSelected(true);
                    }
                    cbMenuItem.addActionListener(this);
                    cbMenuItem.setEnabled(true);
                    menu.add(cbMenuItem);
                }
            }
            if (menu.getItemCount() > 20) {
                MenuScroller.setScrollerFor(menu, 20);
            }
            popup.add(menu);
            menu = new JMenu(resourceMap.getString("changeSecondaryRole.text")); //$NON-NLS-1$
            for (int i = 0; i < Person.T_NUM; i++) {
                if (i == Person.T_NONE
                        || (person.canPerformRole(i) && person
                                .getPrimaryRole() != i)) {
                    // you cant be an astech if you are a tech, or a medic
                    // if you are a doctor
                    if (person.isTechPrimary() && i == Person.T_ASTECH) {
                        continue;
                    }
                    if (person.getPrimaryRole() == Person.T_DOCTOR
                            && i == Person.T_MEDIC) {
                        continue;
                    }
                    cbMenuItem = new JCheckBoxMenuItem(Person.getRoleDesc(
                            i, gui.getCampaign().getFaction().isClan()));
                    cbMenuItem.setActionCommand(makeCommand(CMD_SECONDARY_ROLE, String.valueOf(i)));
                    if (person.getSecondaryRole() == i) {
                        cbMenuItem.setSelected(true);
                    }
                    cbMenuItem.addActionListener(this);
                    cbMenuItem.setEnabled(true);
                    menu.add(cbMenuItem);
                }
            }
            if (menu.getItemCount() > 20) {
                MenuScroller.setScrollerFor(menu, 20);
            }
            popup.add(menu);
            // Bloodnames
            if (StaticChecks.areAllClanEligible(selected)) {
                menuItem = new JMenuItem(resourceMap.getString("giveRandomBloodname.text")); //$NON-NLS-1$
                menuItem.setActionCommand(CMD_BLOODNAME);
                menuItem.addActionListener(this);
                menuItem.setEnabled(StaticChecks.areAllActive(selected));
                popup.add(menuItem);
            }
            // change salary
            if (gui.getCampaign().getCampaignOptions().payForSalaries()) {
                menuItem = new JMenuItem(resourceMap.getString("setSalary.text")); //$NON-NLS-1$
                menuItem.setActionCommand(CMD_EDIT_SALARY);
                menuItem.addActionListener(this);
                menuItem.setEnabled(StaticChecks.areAllActive(selected));
                popup.add(menuItem);
            }
            // switch pilot
            menu = new JMenu(resourceMap.getString("assignToUnit.text")); //$NON-NLS-1$
            JMenu pilotMenu = new JMenu(resourceMap.getString("assignAsPilot.text")); //$NON-NLS-1$
            JMenu crewMenu = new JMenu(resourceMap.getString("assignAsCrewmember.text")); //$NON-NLS-1$
            JMenu driverMenu = new JMenu(resourceMap.getString("assignAsDriver.text")); //$NON-NLS-1$
            JMenu gunnerMenu = new JMenu(resourceMap.getString("assignAsGunner.text")); //$NON-NLS-1$
            JMenu soldierMenu = new JMenu(resourceMap.getString("assignAsSoldier.text")); //$NON-NLS-1$
            JMenu techMenu = new JMenu(resourceMap.getString("assignAsTech.text")); //$NON-NLS-1$
            JMenu navMenu = new JMenu(resourceMap.getString("assignAsNavigator.text")); //$NON-NLS-1$
            JMenu techOfficerMenu = new JMenu(resourceMap.getString("assignAsTechOfficer.text")); //$NON-NLS-1$
            JMenu consoleCmdrMenu = new JMenu(resourceMap.getString("assignAsConsoleCmdr.text")); //$NON-NLS-1$
            /*
             * if(!person.isAssigned()) { cbMenuItem.setSelected(true); }
             */
            if (oneSelected && person.isActive()
                    && !(person.isPrisoner() || person.isBondsman())) {
                for (Unit unit : gui.getCampaign().getUnits()) {
                    if (!unit.isAvailable()) {
                        continue;
                    }
                    if (unit.usesSoloPilot()) {
                        if (unit.canTakeMoreDrivers()
                                && person.canDrive(unit.getEntity())
                                && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_PILOT, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            pilotMenu.add(cbMenuItem);
                        }
                    } else if (unit.usesSoldiers()) {
                        if (unit.canTakeMoreGunners()
                                && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_SOLDIER, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            soldierMenu.add(cbMenuItem);
                        }
                    } else {
                        if (unit.canTakeMoreDrivers()
                                && person.canDrive(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_DRIVER, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            if (unit.getEntity() instanceof Aero || unit.getEntity() instanceof Mech) {
                                pilotMenu.add(cbMenuItem);
                            } else {
                                driverMenu.add(cbMenuItem);
                            }
                        }
                        if (unit.canTakeMoreGunners()
                                && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_GUNNER, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            gunnerMenu.add(cbMenuItem);
                        }
                        if (unit.canTakeMoreVesselCrew()
                                && person.hasSkill(SkillType.S_TECH_VESSEL)) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_CREW, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            crewMenu.add(cbMenuItem);
                        }
                        if (unit.canTakeNavigator()
                                && person.hasSkill(SkillType.S_NAV)) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_NAVIGATOR, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            navMenu.add(cbMenuItem);
                        }
                        if (unit.canTakeTechOfficer()) {
                            //For a vehicle command console we will require the commander to be a driver or a gunner, but not necessarily both
                            if (unit.getEntity() instanceof Tank) {
                                if (person.canDrive(unit.getEntity()) || person.canGun(unit.getEntity())) {
                                    cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                                    cbMenuItem.setActionCommand(makeCommand(CMD_ADD_TECH_OFFICER, unit.getId().toString()));
                                    cbMenuItem.addActionListener(this);
                                    consoleCmdrMenu.add(cbMenuItem);
                                }
                            } else if (person.canDrive(unit.getEntity())
                                    && person.canGun(unit.getEntity())) {
                                cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                                cbMenuItem.setActionCommand(makeCommand(CMD_ADD_TECH_OFFICER, unit.getId().toString()));
                                cbMenuItem.addActionListener(this);
                                techOfficerMenu.add(cbMenuItem);
                            }
                        }
                    }
                    if (unit.canTakeTech() && person.canTech(unit.getEntity())
                            && (person.getMaintenanceTimeUsing() + unit.getMaintenanceTime() <= 480)) {
                        cbMenuItem = new JCheckBoxMenuItem(String.format(resourceMap.getString("maintenanceTimeDesc.format"), //$NON-NLS-1$
                            unit.getName(), unit.getMaintenanceTime()));
                        // TODO: check the box
                        cbMenuItem.setActionCommand(makeCommand(CMD_ADD_TECH, unit.getId().toString()));
                        cbMenuItem.addActionListener(this);
                        techMenu.add(cbMenuItem);
                    }
                }
                if (pilotMenu.getItemCount() > 0) {
                    menu.add(pilotMenu);
                    if (pilotMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(pilotMenu, 20);
                    }
                }
                if (driverMenu.getItemCount() > 0) {
                    menu.add(driverMenu);
                    if (driverMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(driverMenu, 20);
                    }
                }
                if (crewMenu.getItemCount() > 0) {
                    menu.add(crewMenu);
                    if (crewMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(crewMenu, 20);
                    }
                }
                if (navMenu.getItemCount() > 0) {
                    menu.add(navMenu);
                    if (navMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(navMenu, 20);
                    }
                }
                if (gunnerMenu.getItemCount() > 0) {
                    menu.add(gunnerMenu);
                    if (gunnerMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(gunnerMenu, 20);
                    }
                }
                if (soldierMenu.getItemCount() > 0) {
                    menu.add(soldierMenu);
                    if (soldierMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(soldierMenu, 20);
                    }
                }
                if (techOfficerMenu.getItemCount() > 0) {
                    menu.add(techOfficerMenu);
                    if (techOfficerMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(techOfficerMenu, 20);
                    }
                }
                if (consoleCmdrMenu.getItemCount() > 0) {
                    menu.add(consoleCmdrMenu);
                    if (consoleCmdrMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(consoleCmdrMenu, 20);
                    }
                }
                if (techMenu.getItemCount() > 0) {
                    menu.add(techMenu);
                    if (techMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(techMenu, 20);
                    }
                }
                menu.setEnabled(!person.isDeployed());
                popup.add(menu);
            } else if (StaticChecks.areAllActive(selected) && StaticChecks.areAllEligible(selected)) {
                for (Unit unit : gui.getCampaign().getUnits()) {
                    if (!unit.isAvailable()) {
                        continue;
                    }
                    if (StaticChecks.areAllInfantry(selected)) {
                        if (!(unit.getEntity() instanceof Infantry) || unit.getEntity() instanceof BattleArmor) {
                            continue;
                        }
                        if (unit.canTakeMoreGunners() && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_SOLDIER, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            soldierMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllBattleArmor(selected)) {
                        if (!(unit.getEntity() instanceof BattleArmor)) {
                            continue;
                        }
                        if (unit.canTakeMoreGunners() && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_SOLDIER, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            soldierMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllVeeGunners(selected)) {
                        if (!(unit.getEntity() instanceof Tank)) {
                            continue;
                        }
                        if (unit.canTakeMoreGunners() && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_GUNNER, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            gunnerMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllVesselGunners(selected)) {
                        if (!(unit.getEntity() instanceof Aero)) {
                            continue;
                        }
                        if (unit.canTakeMoreGunners() && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_GUNNER, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            gunnerMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllVesselCrew(selected)) {
                        if (!(unit.getEntity() instanceof Aero)) {
                            continue;
                        }
                        if (unit.canTakeMoreVesselCrew()
                                && person.hasSkill(SkillType.S_TECH_VESSEL)) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_CREW, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            crewMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllVesselPilots(selected)) {
                        if (!(unit.getEntity() instanceof Aero)) {
                            continue;
                        }
                        if (unit.canTakeMoreDrivers()
                                && person.canDrive(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_VESSEL_PILOT, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            pilotMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllVesselNavigators(selected)) {
                        if (!(unit.getEntity() instanceof Aero)) {
                            continue;
                        }
                        if (unit.canTakeNavigator()
                                && person.hasSkill(SkillType.S_NAV)) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_NAVIGATOR, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            navMenu.add(cbMenuItem);
                        }
                    }
                }
                if (soldierMenu.getItemCount() > 0) {
                    menu.add(soldierMenu);
                    if (soldierMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(soldierMenu, 20);
                    }
                }
                if (pilotMenu.getItemCount() > 0) {
                    menu.add(pilotMenu);
                    if (pilotMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(pilotMenu, 20);
                    }
                }
                if (driverMenu.getItemCount() > 0) {
                    menu.add(driverMenu);
                    if (driverMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(driverMenu, 20);
                    }
                }
                if (crewMenu.getItemCount() > 0) {
                    menu.add(crewMenu);
                    if (crewMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(crewMenu, 20);
                    }
                }
                if (navMenu.getItemCount() > 0) {
                    menu.add(navMenu);
                    if (navMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(navMenu, 20);
                    }
                }
                if (gunnerMenu.getItemCount() > 0) {
                    menu.add(gunnerMenu);
                    if (gunnerMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(gunnerMenu, 20);
                    }
                }
                if (soldierMenu.getItemCount() > 0) {
                    menu.add(soldierMenu);
                    if (soldierMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(soldierMenu, 20);
                    }
                }
                menu.setEnabled(!person.isDeployed());
                popup.add(menu);
            }
            cbMenuItem = new JCheckBoxMenuItem(resourceMap.getString("none.text")); //$NON-NLS-1$
            cbMenuItem.setActionCommand(makeCommand(CMD_REMOVE_UNIT, "-1")); //$NON-NLS-1$
            cbMenuItem.addActionListener(this);
            menu.add(cbMenuItem);
            if (oneSelected) {
                if ((person.getAge(gui.getCampaign().getCalendar()) > 13) && (person.isFemale()) && !person.isPregnant()) {
                    menuItem = new JMenuItem(resourceMap.getString("addPregnancy.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(CMD_ADD_PREGNANCY);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(gui.getCampaign().isGM());
                    popup.add(menuItem);
                }
                if (person.isPregnant()) {
                    menuItem = new JMenuItem(resourceMap.getString("removePregnancy.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(CMD_REMOVE_PREGNANCY);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(gui.getCampaign().isGM());
                    popup.add(menuItem);
                }
            }
            if (oneSelected && person.isActive()) {
                if ((person.getAge(gui.getCampaign().getCalendar()) > 13) && (person.getSpouseID() == null)) {
                    menu = new JMenu(resourceMap.getString("changeSpouse.text")); //$NON-NLS-1$
                    JMenuItem surnameMenu;
                    JMenu spouseMenu;
                    String type;
                    for (Person ps : gui.getCampaign().getPersonnel()) {
                        if (person.safeSpouse(ps) && !ps.isDeadOrMIA()) {
                            String pStatus;
                            if (ps.isBondsman()) {
                                pStatus = String.format(resourceMap.getString("marriageBondsmanDesc.format"), //$NON-NLS-1$
                                    ps.getFullName(), ps.getAge(gui.getCampaign().getCalendar()), ps.getRoleDesc());
                            } else if (ps.isPrisoner()) {
                                pStatus = String.format(resourceMap.getString("marriagePrisonerDesc.format"), //$NON-NLS-1$
                                    ps.getFullName(), ps.getAge(gui.getCampaign().getCalendar()), ps.getRoleDesc());
                            } else {
                                pStatus = String.format(resourceMap.getString("marriagePartnerDesc.format"), //$NON-NLS-1$
                                    ps.getFullName(), ps.getAge(gui.getCampaign().getCalendar()), ps.getRoleDesc());
                            }
                            spouseMenu = new JMenu(pStatus);
                            type = resourceMap.getString("marriageNoNameChange.text"); //$NON-NLS-1$
                            surnameMenu = new JMenuItem(type);
                            surnameMenu.setActionCommand(
                                makeCommand(CMD_ADD_SPOUSE, ps.getId().toString(), OPT_SURNAME_NO_CHANGE));
                            surnameMenu.addActionListener(this);
                            spouseMenu.add(surnameMenu);
                            if (!ps.isClanner() && !person.isClanner()) {
                                type = resourceMap.getString("marriageRenameSpouse.text"); //$NON-NLS-1$
                                surnameMenu = new JMenuItem(type);
                                surnameMenu.setActionCommand(
                                       makeCommand(CMD_ADD_SPOUSE, ps.getId().toString(), OPT_SURNAME_YOURS));
                                surnameMenu.addActionListener(this);
                                spouseMenu.add(surnameMenu);
                                type = resourceMap.getString("marriageRenameYourself.text"); //$NON-NLS-1$
                                surnameMenu = new JMenuItem(type);
                                surnameMenu.setActionCommand(
                                      makeCommand(CMD_ADD_SPOUSE, ps.getId().toString(), OPT_SURNAME_SPOUSE));
                                surnameMenu.addActionListener(this);
                                spouseMenu.add(surnameMenu);
                                type = resourceMap.getString("marriageHyphenateYourself.text"); //$NON-NLS-1$
                                surnameMenu = new JMenuItem(type);
                                surnameMenu.setActionCommand(
                                    makeCommand(CMD_ADD_SPOUSE, ps.getId().toString(), OPT_SURNAME_HYP_YOURS));
                                surnameMenu.addActionListener(this);
                                spouseMenu.add(surnameMenu);
                                type = resourceMap.getString("marriageHyphenateSpouse.text"); //$NON-NLS-1$
                                surnameMenu = new JMenuItem(type);
                                surnameMenu.setActionCommand(
                                    makeCommand(CMD_ADD_SPOUSE, ps.getId().toString(), OPT_SURNAME_HYP_SPOUSE));
                                surnameMenu.addActionListener(this);
                                spouseMenu.add(surnameMenu);
                            }
                            menu.add(spouseMenu);
                        }
                    }
                    if (menu.getItemCount() > 30) {
                        MenuScroller.setScrollerFor(menu, 20);
                    }
                    popup.add(menu);
                }
                if (person.getSpouseID() != null) {
                    menuItem = new JMenuItem(resourceMap.getString("removeSpouse.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(CMD_REMOVE_SPOUSE);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }
                menu = new JMenu(resourceMap.getString("spendXP.text")); //$NON-NLS-1$
                JMenu currentMenu = new JMenu(resourceMap.getString("spendOnCurrentSkills.text")); //$NON-NLS-1$
                JMenu newMenu = new JMenu(resourceMap.getString("spendOnNewSkills.text")); //$NON-NLS-1$
                for (int i = 0; i < SkillType.getSkillList().length; i++) {
                    String type = SkillType.getSkillList()[i];
                    int cost = person.hasSkill(type) ? person.getSkill(type).getCostToImprove() : SkillType.getType(type).getCost(0);
                    if( cost >= 0 ) {
                        String desc = String.format(resourceMap.getString("skillDesc.format"), type, cost); //$NON-NLS-1$
                        menuItem = new JMenuItem(desc);
                        menuItem.setActionCommand(makeCommand(CMD_IMPROVE, type, String.valueOf(cost)));
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(person.getXp() >= cost);
                        if(person.hasSkill(type)) {
                            currentMenu.add(menuItem);
                        } else {
                            newMenu.add(menuItem);
                        }
                    }
                }
                menu.add(currentMenu);
                menu.add(newMenu);
                if (gui.getCampaign().getCampaignOptions().useAbilities()) {
                    JMenu abMenu = new JMenu(resourceMap.getString("spendOnSpecialAbilities.text")); //$NON-NLS-1$
                    int cost = -1;
                    for (SpecialAbility spa : SpecialAbility.getAllSpecialAbilities().values()) {
                        if (null == spa) {
                            continue;
                        }
                        if (!spa.isEligible(person)) {
                            continue;
                        }
                        cost = spa.getCost();
                        String costDesc;
                        if(cost < 0) {
                            costDesc = resourceMap.getString("costNotPossible.text"); //$NON-NLS-1$
                        } else {
                            costDesc = String.format(resourceMap.getString("costValue.format"), cost); //$NON-NLS-1$
                        }
                        boolean available = (cost >= 0) && (person.getXp() >= cost);
                        if (spa.getName().equals("weapon_specialist")) { //$NON-NLS-1$
                            Unit u = gui.getCampaign().getUnit(person.getUnitId());
                            if (null != u) {
                                JMenu specialistMenu = new JMenu(resourceMap.getString("weaponSpecialist.text")); //$NON-NLS-1$
                                TreeSet<String> uniqueWeapons = new TreeSet<String>();
                                for (int j = 0; j < u.getEntity().getWeaponList().size(); j++) {
                                    Mounted m = u.getEntity().getWeaponList().get(j);
                                    uniqueWeapons.add(m.getName());
                                }
                                for (String name : uniqueWeapons) {
                                    menuItem = new JMenuItem(String.format(resourceMap.getString("abilityDesc.format"), name, costDesc)); //$NON-NLS-1$
                                    menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_WEAPON_SPECIALIST, name, String.valueOf(cost)));
                                    menuItem.addActionListener(this);
                                    menuItem.setEnabled(available);
                                    specialistMenu.add(menuItem);
                                }
                                abMenu.add(specialistMenu);
                            }
                        } else if (spa.getName().equals("human_tro")) { //$NON-NLS-1$
                            JMenu specialistMenu = new JMenu(resourceMap.getString("humantro.text")); //$NON-NLS-1$
                            menuItem = new JMenuItem(String.format(resourceMap.getString("abilityDesc.format"), resourceMap.getString("humantro_mek.text"), costDesc)); //$NON-NLS-1$ //$NON-NLS-2$
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_HUMANTRO, Crew.HUMANTRO_MECH, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                            menuItem = new JMenuItem(String.format(resourceMap.getString("abilityDesc.format"), resourceMap.getString("humantro_aero.text"), costDesc)); //$NON-NLS-1$ //$NON-NLS-2$
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_HUMANTRO, Crew.HUMANTRO_AERO, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                            menuItem = new JMenuItem(String.format(resourceMap.getString("abilityDesc.format"), resourceMap.getString("humantro_vee.text"), costDesc)); //$NON-NLS-1$ //$NON-NLS-2$
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_HUMANTRO, Crew.HUMANTRO_VEE, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                            menuItem = new JMenuItem(String.format(resourceMap.getString("abilityDesc.format"), resourceMap.getString("humantro_ba.text"), costDesc)); //$NON-NLS-1$ //$NON-NLS-2$
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_HUMANTRO, Crew.HUMANTRO_BA, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                            abMenu.add(specialistMenu);
                        } else if (spa.getName().equals("specialist")) { //$NON-NLS-1$
                            JMenu specialistMenu = new JMenu(resourceMap.getString("specialist.text")); //$NON-NLS-1$
                            menuItem = new JMenuItem(String.format(resourceMap.getString("abilityDesc.format"), resourceMap.getString("laserSpecialist.text"), costDesc)); //$NON-NLS-1$ //$NON-NLS-2$
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_SPECIALIST, Crew.SPECIAL_ENERGY, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                            menuItem = new JMenuItem(String.format(resourceMap.getString("abilityDesc.format"), resourceMap.getString("missileSpecialist.text"), costDesc)); //$NON-NLS-1$ //$NON-NLS-2$
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_SPECIALIST, Crew.SPECIAL_MISSILE, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                            menuItem = new JMenuItem(String.format(resourceMap.getString("abilityDesc.format"), resourceMap.getString("ballisticSpecialist.text"), costDesc)); //$NON-NLS-1$ //$NON-NLS-2$
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_SPECIALIST, Crew.SPECIAL_BALLISTIC, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                            abMenu.add(specialistMenu);
                        } else if (spa.getName().equals("range_master")) { //$NON-NLS-1$
                            JMenu specialistMenu = new JMenu(resourceMap.getString("rangemaster.text")); //$NON-NLS-1$
                            menuItem = new JMenuItem(String.format(resourceMap.getString("abilityDesc.format"), resourceMap.getString("rangemaster_med.text"), costDesc)); //$NON-NLS-1$ //$NON-NLS-2$
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_RANGEMASTER, Crew.RANGEMASTER_MEDIUM, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                            menuItem = new JMenuItem(String.format(resourceMap.getString("abilityDesc.format"), resourceMap.getString("rangemaster_lng.text"), costDesc)); //$NON-NLS-1$ //$NON-NLS-2$
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_RANGEMASTER, Crew.RANGEMASTER_LONG, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                            menuItem = new JMenuItem(String.format(resourceMap.getString("abilityDesc.format"), resourceMap.getString("rangemaster_xtm.text"), costDesc)); //$NON-NLS-1$ //$NON-NLS-2$
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_RANGEMASTER, Crew.RANGEMASTER_EXTREME, String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            specialistMenu.add(menuItem);
                            abMenu.add(specialistMenu);
                        } else {
                            menuItem = new JMenuItem(String.format(resourceMap.getString("abilityDesc.format"), spa.getDisplayName(), costDesc)); //$NON-NLS-1$
                            menuItem.setActionCommand(makeCommand(CMD_ACQUIRE_ABILITY, spa.getName(), String.valueOf(cost)));
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(available);
                            abMenu.add(menuItem);
                        }
                    }
                    if (abMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(abMenu, 20);
                    }
                    menu.add(abMenu);
                }
                menu.add(currentMenu);
                menu.add(newMenu);
                if (gui.getCampaign().getCampaignOptions().useEdge()) {
                    JMenu edgeMenu = new JMenu(resourceMap.getString("edge.text")); //$NON-NLS-1$
                    int cost = gui.getCampaign().getCampaignOptions().getEdgeCost();
                    boolean available = (cost >= 0) && (person.getXp() >= cost);
                                        
                    menuItem = new JMenuItem(String.format(resourceMap.getString("spendOnEdge.text"), cost)); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_BUY_EDGE, String.valueOf(cost)));
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(available);
                    edgeMenu.add(menuItem);
                    menu.add(edgeMenu); 
                }        
                popup.add(menu);
            }
            if (oneSelected && person.isActive()) {
                if (gui.getCampaign().getCampaignOptions().useEdge()) {
                    menu = new JMenu(resourceMap.getString("setEdgeTriggers.text")); //$NON-NLS-1$
                    //Start of role-specific Edge reroll options
                    //Mechwarriors
                    if (person.getPrimaryRole() == 1) {
                        cbMenuItem = new JCheckBoxMenuItem(resourceMap.getString("edgeTriggerHeadHits.text")); //$NON-NLS-1$
                        cbMenuItem.setSelected(person.getOptions()
                                .booleanOption(OPT_EDGE_HEADHIT));
                        cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_HEADHIT));
                        cbMenuItem.addActionListener(this);
                        menu.add(cbMenuItem);
                        cbMenuItem = new JCheckBoxMenuItem(
                                resourceMap.getString("edgeTriggerTAC.text")); //$NON-NLS-1$
                        cbMenuItem.setSelected(person.getOptions()
                                .booleanOption(OPT_EDGE_TAC));
                        cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_TAC));
                        cbMenuItem.addActionListener(this);
                        menu.add(cbMenuItem);
                        cbMenuItem = new JCheckBoxMenuItem(resourceMap.getString("edgeTriggerKO.text")); //$NON-NLS-1$
                        cbMenuItem.setSelected(person.getOptions()
                                .booleanOption(OPT_EDGE_KO));
                        cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_KO));
                        cbMenuItem.addActionListener(this);
                        menu.add(cbMenuItem);
                        cbMenuItem = new JCheckBoxMenuItem(resourceMap.getString("edgeTriggerExplosion.text")); //$NON-NLS-1$
                        cbMenuItem.setSelected(person.getOptions()
                                .booleanOption(OPT_EDGE_EXPLOSION));
                        cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_EXPLOSION));
                        cbMenuItem.addActionListener(this);
                        menu.add(cbMenuItem);
                        cbMenuItem = new JCheckBoxMenuItem(resourceMap.getString("edgeTriggerMASCFailure.text")); //$NON-NLS-1$
                        cbMenuItem.setSelected(person.getOptions()
                                .booleanOption(OPT_EDGE_MASC_FAILURE));
                        cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_MASC_FAILURE));
                        cbMenuItem.addActionListener(this);
                        menu.add(cbMenuItem);
                    //Doctors    
                    } else if (person.getPrimaryRole() == 20 && CampaignOptions.useRemfEdge()) {
                        cbMenuItem = new JCheckBoxMenuItem(
                                resourceMap.getString("edgeTriggerHealCheck.text")); //$NON-NLS-1$
                        cbMenuItem.setSelected(person.getOptions()
                                .booleanOption(PersonnelOptions.EDGE_MEDICAL));
                        cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_MEDICAL));
                        cbMenuItem.addActionListener(this);
                        menu.add(cbMenuItem);
                    //Techs
                    } else if ((person.getPrimaryRole() == 12
                                || person.getPrimaryRole() == 15
                                || person.getPrimaryRole() == 16
                                || person.getPrimaryRole() == 17
                                || person.getPrimaryRole() == 18)
                                && CampaignOptions.useRemfEdge()) {
                        cbMenuItem = new JCheckBoxMenuItem(
                                resourceMap.getString("edgeTriggerBreakPart.text")); //$NON-NLS-1$
                        cbMenuItem.setSelected(person.getOptions()
                                .booleanOption(PersonnelOptions.EDGE_REPAIR_BREAK_PART));
                        cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_REPAIR_BREAK_PART));
                        cbMenuItem.addActionListener(this);
                        menu.add(cbMenuItem);
                        cbMenuItem = new JCheckBoxMenuItem(
                                resourceMap.getString("edgeTriggerFailedRefit.text")); //$NON-NLS-1$
                        cbMenuItem.setSelected(person.getOptions()
                                .booleanOption(PersonnelOptions.EDGE_REPAIR_FAILED_REFIT));
                        cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_REPAIR_FAILED_REFIT));
                        cbMenuItem.addActionListener(this);
                        menu.add(cbMenuItem);
                    //Admins
                    } else if ((person.getPrimaryRole() == 22
                            || person.getPrimaryRole() == 23
                            || person.getPrimaryRole() == 24
                            || person.getPrimaryRole() == 25) 
                            && CampaignOptions.useRemfEdge()) {
                        cbMenuItem = new JCheckBoxMenuItem(
                                resourceMap.getString("edgeTriggerAcquireCheck.text")); //$NON-NLS-1$
                        cbMenuItem.setSelected(person.getOptions()
                                .booleanOption(PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL));
                        cbMenuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL));
                        cbMenuItem.addActionListener(this);
                        menu.add(cbMenuItem);                        
                    } 
                    
                    popup.add(menu);
                }
                menu = new JMenu(resourceMap.getString("specialFlags.text")); //$NON-NLS-1$
                cbMenuItem = new JCheckBoxMenuItem(resourceMap.getString("dependent.text")); //$NON-NLS-1$
                cbMenuItem.setSelected(person.isDependent());
                cbMenuItem.setActionCommand(CMD_DEPENDENT);
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);
                cbMenuItem = new JCheckBoxMenuItem(resourceMap.getString("commander.text")); //$NON-NLS-1$
                cbMenuItem.setSelected(person.isCommander());
                cbMenuItem.setActionCommand(CMD_COMMANDER);
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);
                popup.add(menu);
            } else if (StaticChecks.areAllActive(selected)) {
                if (gui.getCampaign().getCampaignOptions().useEdge()) {
                    menu = new JMenu(resourceMap.getString("setEdgeTriggers.text")); //$NON-NLS-1$
                    submenu = new JMenu(resourceMap.getString("on.text")); //$NON-NLS-1$
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerHeadHits.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_HEADHIT, TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerTAC.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_TAC, TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerKO.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_KO, TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerExplosion.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_EXPLOSION, TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerMASCFailure.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_MASC_FAILURE, TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerHealCheck.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_MEDICAL, TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerBreakPart.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_REPAIR_BREAK_PART, TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerFailedRefit.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_REPAIR_FAILED_REFIT, TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerAcquireCheck.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL, TRUE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    submenu = new JMenu(resourceMap.getString("off.text")); //$NON-NLS-1$
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerHeadHits.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_HEADHIT, FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerTAC.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_TAC, FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerKO.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_KO, FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerExplosion.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_EXPLOSION, FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerMASCFailure.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, OPT_EDGE_MASC_FAILURE, FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menu.add(submenu);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerHealCheck.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_MEDICAL, FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerBreakPart.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_REPAIR_BREAK_PART, FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerFailedRefit.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_REPAIR_FAILED_REFIT, FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem(resourceMap.getString("edgeTriggerAcquireCheck.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(makeCommand(CMD_EDGE_TRIGGER, PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL, FALSE));
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    popup.add(menu);
                }
                menu = new JMenu(resourceMap.getString("specialFlags.text")); //$NON-NLS-1$
                submenu = new JMenu(resourceMap.getString("dependent.text")); //$NON-NLS-1$
                menuItem = new JMenuItem(resourceMap.getString("yes.text")); //$NON-NLS-1$
                menuItem.setActionCommand(makeCommand(CMD_DEPENDENT, TRUE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);
                menuItem = new JMenuItem(resourceMap.getString("no.text")); //$NON-NLS-1$
                menuItem.setActionCommand(makeCommand(CMD_DEPENDENT, FALSE));
                menuItem.addActionListener(this);
                submenu.add(menuItem);
                menu.add(submenu);
                popup.add(menu);
            }
            if (oneSelected) {
                // change portrait
                menuItem = new JMenuItem(resourceMap.getString("changePortrait.text")); //$NON-NLS-1$
                menuItem.setActionCommand(CMD_EDIT_PORTRAIT);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
                // change Biography
                menuItem = new JMenuItem(resourceMap.getString("changeBiography.text")); //$NON-NLS-1$
                menuItem.setActionCommand(CMD_EDIT_BIOGRAPHY);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
                menuItem = new JMenuItem(resourceMap.getString("changeCallsign.text")); //$NON-NLS-1$
                menuItem.setActionCommand(CMD_CALLSIGN);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
                menuItem = new JMenuItem(resourceMap.getString("editPersonnelLog.text")); //$NON-NLS-1$
                menuItem.setActionCommand(CMD_EDIT_PERSONNEL_LOG);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);

            }
            menuItem = new JMenuItem(resourceMap.getString("addSingleLogEntry.text")); //$NON-NLS-1$
            menuItem.setActionCommand(CMD_EDIT_LOG_ENTRY);
            menuItem.addActionListener(this);
            menuItem.setEnabled(true);
            popup.add(menuItem);
            if (oneSelected || StaticChecks.allHaveSameUnit(selected)) {
                menuItem = new JMenuItem(resourceMap.getString("assignKill.text")); //$NON-NLS-1$
                menuItem.setActionCommand(CMD_KILL);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
            }
            if (oneSelected) {
                menuItem = new JMenuItem(resourceMap.getString("editKillLog.text")); //$NON-NLS-1$
                menuItem.setActionCommand(CMD_EDIT_KILL_LOG);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
            }
            menuItem = new JMenuItem(resourceMap.getString("exportPersonnel.text")); //$NON-NLS-1$
            menuItem.addActionListener(ev -> gui.miExportPersonActionPerformed(ev));
            menuItem.setEnabled(true);
            popup.add(menuItem);
            if (gui.getCampaign().getCampaignOptions().getUseAtB()
                    && StaticChecks.areAllActive(selected)) {
                menuItem = new JMenuItem(resourceMap.getString("sack.text")); //$NON-NLS-1$
                menuItem.setActionCommand(CMD_SACK);
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }
            menu = new JMenu(resourceMap.getString("gmMode.text")); //$NON-NLS-1$

            menuItem = new JMenu(resourceMap.getString("changePrisonerStatus.text")); //$NON-NLS-1$
            menuItem.add(newCheckboxMenu(
                    Person.getPrisonerStatusName(Person.PRISONER_NOT),
                    makeCommand(CMD_CHANGE_PRISONER_STATUS, OPT_PRISONER_FREE),
                    person.getPrisonerStatus() == Person.PRISONER_NOT));
            menuItem.add(newCheckboxMenu(
                    Person.getPrisonerStatusName(Person.PRISONER_YES),
                    makeCommand(CMD_CHANGE_PRISONER_STATUS, OPT_PRISONER_IMPRISONED),
                    (person.getPrisonerStatus() == Person.PRISONER_YES) && !person.isWillingToDefect()));
            menuItem.add(newCheckboxMenu(
                    resourceMap.getString("prisonerWillingToDefect.text"), //$NON-NLS-1$
                    makeCommand(CMD_CHANGE_PRISONER_STATUS, OPT_PRISONER_IMPRISONED_DEFECTING),
                    (person.getPrisonerStatus() == Person.PRISONER_YES) && person.isWillingToDefect()));
            menuItem.add(newCheckboxMenu(
                    Person.getPrisonerStatusName(Person.PRISONER_BONDSMAN),
                    makeCommand(CMD_CHANGE_PRISONER_STATUS, OPT_PRISONER_BONDSMAN),
                    person.getPrisonerStatus() == Person.PRISONER_BONDSMAN));
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);

            menuItem = new JMenuItem(resourceMap.getString("removePerson.text")); //$NON-NLS-1$
            menuItem.setActionCommand(CMD_REMOVE);
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            if (!gui.getCampaign().getCampaignOptions().useAdvancedMedical()) {
                menuItem = new JMenuItem(resourceMap.getString("healPerson.text")); //$NON-NLS-1$
                menuItem.setActionCommand(CMD_HEAL);
                menuItem.addActionListener(this);
                menuItem.setEnabled(gui.getCampaign().isGM());
                menu.add(menuItem);
            }
            menuItem = new JMenuItem(resourceMap.getString("addXP.text")); //$NON-NLS-1$
            menuItem.setActionCommand(CMD_ADD_XP);
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            menuItem = new JMenuItem(resourceMap.getString("setXP.text")); //$NON-NLS-1$
            menuItem.setActionCommand(CMD_SET_XP);
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            if (gui.getCampaign().getCampaignOptions().useEdge()) {
                menuItem = new JMenuItem(resourceMap.getString("setEdge.text")); //$NON-NLS-1$
                menuItem.setActionCommand(CMD_SET_EDGE);
                menuItem.addActionListener(this);
                menuItem.setEnabled(gui.getCampaign().isGM());
                menu.add(menuItem);
            }
            if (oneSelected) {
                menuItem = new JMenuItem(resourceMap.getString("edit.text")); //$NON-NLS-1$
                menuItem.setActionCommand(CMD_EDIT);
                menuItem.addActionListener(this);
                menuItem.setEnabled(gui.getCampaign().isGM());
                menu.add(menuItem);
            }
            if (gui.getCampaign().getCampaignOptions().useAdvancedMedical()) {
                menuItem = new JMenuItem(resourceMap.getString("removeAllInjuries.text")); //$NON-NLS-1$
                menuItem.setActionCommand(CMD_CLEAR_INJURIES);
                menuItem.addActionListener(this);
                menuItem.setEnabled(gui.getCampaign().isGM());
                menu.add(menuItem);
                if (oneSelected) {
                    for (Injury i : person.getInjuries()) {
                        menuItem = new JMenuItem(String.format(resourceMap.getString("removeInjury.format"), i.getName())); //$NON-NLS-1$
                        menuItem.setActionCommand(makeCommand(CMD_REMOVE_INJURY, i.getUUID().toString()));
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(gui.getCampaign().isGM());
                        menu.add(menuItem);
                    }

                    menuItem = new JMenuItem(resourceMap.getString("editInjuries.text")); //$NON-NLS-1$
                    menuItem.setActionCommand(CMD_EDIT_INJURIES);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(gui.getCampaign().isGM());
                    menu.add(menuItem);
                }
            }
            popup.addSeparator();
            popup.add(menu);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    private JMenuItem newMenuItem(String text, String command) {
        return newMenuItem(text, command, true);
    }

    private JMenuItem newMenuItem(String text, String command, boolean enabled) {
        JMenuItem result = new JMenuItem(text);
        result.setActionCommand(command);
        result.addActionListener(this);
        result.setEnabled(enabled);
        return result;
    }

    private JCheckBoxMenuItem newCheckboxMenu(String text, String command, boolean selected) {
        return newCheckboxMenu(text, command, selected, true);
    }

    private JCheckBoxMenuItem newCheckboxMenu(String text, String command, boolean selected, boolean enabled) {
        JCheckBoxMenuItem result = new JCheckBoxMenuItem(text);
        result.setSelected(selected);
        result.setActionCommand(command);
        result.addActionListener(this);
        result.setEnabled(true);
        return result;
    }
}