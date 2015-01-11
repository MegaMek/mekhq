package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.UUID;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputAdapter;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Crew;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.options.IOption;
import megamek.common.options.PilotOptions;
import mekhq.Utilities;
import mekhq.campaign.Kill;
import mekhq.campaign.LogEntry;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Rank;
import mekhq.campaign.personnel.Ranks;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.MenuScroller;
import mekhq.gui.dialog.CustomizePersonDialog;
import mekhq.gui.dialog.EditKillLogDialog;
import mekhq.gui.dialog.EditLogEntryDialog;
import mekhq.gui.dialog.EditPersonnelInjuriesDialog;
import mekhq.gui.dialog.EditPersonnelLogDialog;
import mekhq.gui.dialog.KillDialog;
import mekhq.gui.dialog.PopupValueChoiceDialog;
import mekhq.gui.dialog.PortraitChoiceDialog;
import mekhq.gui.dialog.RetirementDefectionDialog;
import mekhq.gui.dialog.TextAreaDialog;
import mekhq.gui.utilities.StaticChecks;

public class PersonnelTableMouseAdapter extends MouseInputAdapter implements
        ActionListener {
    private CampaignGUI gui;

    public PersonnelTableMouseAdapter(CampaignGUI gui) {
        super();
        this.gui = gui;
    }

    public PersonnelTableMouseAdapter() {
        super();
    }

    public void actionPerformed(ActionEvent action) {
        StringTokenizer st = new StringTokenizer(action.getActionCommand(),
                "|");
        String command = st.nextToken();
        int row = gui.getPersonnelTable().getSelectedRow();
        if (row < 0) {
            return;
        }
        Person selectedPerson = gui.getPersonnelModel().getPerson(gui.getPersonnelTable()
                .convertRowIndexToModel(row));
        int[] rows = gui.getPersonnelTable().getSelectedRows();
        Person[] people = new Person[rows.length];
        for (int i = 0; i < rows.length; i++) {
            people[i] = gui.getPersonnelModel().getPerson(gui.getPersonnelTable()
                    .convertRowIndexToModel(rows[i]));
        }
        if (command.startsWith("RANKSYSTEM")) {
            int system = Integer.parseInt(st.nextToken());
            for (Person person : people) {
                person.setRankSystem(system);
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshOrganization();
        } else if (command.startsWith("RANK")) {
            int rank = Integer.parseInt(st.nextToken());
            int level = 0;
            // Check to see if we added a rank level...
            if (st.hasMoreTokens()) {
                level = Integer.parseInt(st.nextToken());
            }

            for (Person person : people) {
                gui.getCampaign().changeRank(person, rank, level, true);
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.startsWith("MD_RANK")) {
            int md_rank = Integer.parseInt(st.nextToken());
            for (Person person : people) {
                person.setManeiDominiRank(md_rank);
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.startsWith("MD_CLASS")) {
            int md_class = Integer.parseInt(st.nextToken());
            for (Person person : people) {
                person.setManeiDominiClass(md_class);
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.startsWith("DESIG_PRI")) {
            int designation = Integer.parseInt(st.nextToken());
            for (Person person : people) {
                person.setPrimaryDesignator(designation);
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.startsWith("DESIG_SEC")) {
            int designation = Integer.parseInt(st.nextToken());
            for (Person person : people) {
                person.setSecondaryDesignator(designation);
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.contains("PROLE")) {
            int role = Integer.parseInt(st.nextToken());
            for (Person person : people) {
                person.setPrimaryRole(role);
                gui.getCampaign().personUpdated(person);
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.contains("SROLE")) {
            int role = Integer.parseInt(st.nextToken());
            for (Person person : people) {
                person.setSecondaryRole(role);
                gui.getCampaign().personUpdated(person);
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.contains("REMOVE_UNIT")) {
            for (Person person : people) {
                Unit u = gui.getCampaign().getUnit(person.getUnitId());
                if (null != u) {
                    u.remove(person, true);
                    u.resetEngineerOrTech();
                    u.runDiagnostic();
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
                            u.resetEngineerOrTech();
                            u.runDiagnostic();
                        }
                    }
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.contains("ADD_PILOT")) {
            UUID selected = UUID.fromString(st.nextToken());
            Unit u = gui.getCampaign().getUnit(selected);
            Unit oldUnit = gui.getCampaign()
                    .getUnit(selectedPerson.getUnitId());
            if (null != oldUnit) {
                oldUnit.remove(selectedPerson, true);
            }
            if (null != u) {
                u.addPilotOrSoldier(selectedPerson);
            }
            u.resetPilotAndEntity();
            u.runDiagnostic();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.contains("ADD_SOLDIER")) {
            UUID selected = UUID.fromString(st.nextToken());
            Unit u = gui.getCampaign().getUnit(selected);
            if (null != u) {
                for (Person p : people) {
                    if (u.canTakeMoreGunners()) {
                        Unit oldUnit = gui.getCampaign().getUnit(p.getUnitId());
                        if (null != oldUnit) {
                            oldUnit.remove(p, true);
                        }
                        u.addPilotOrSoldier(p);
                    }
                }
            }
            u.resetPilotAndEntity();
            u.runDiagnostic();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.contains("ADD_DRIVER")) {
            UUID selected = UUID.fromString(st.nextToken());
            Unit u = gui.getCampaign().getUnit(selected);
            Unit oldUnit = gui.getCampaign()
                    .getUnit(selectedPerson.getUnitId());
            if (null != oldUnit) {
                oldUnit.remove(selectedPerson, true);
            }
            if (null != u) {
                u.addDriver(selectedPerson);
            }
            u.resetPilotAndEntity();
            u.runDiagnostic();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.contains("ADD_VESSEL_PILOT")) {
            UUID selected = UUID.fromString(st.nextToken());
            Unit u = gui.getCampaign().getUnit(selected);
            if (null != u) {
                for (Person p : people) {
                    if (u.canTakeMoreDrivers()) {
                        Unit oldUnit = gui.getCampaign().getUnit(p.getUnitId());
                        if (null != oldUnit) {
                            oldUnit.remove(p, true);
                        }
                        u.addDriver(p);
                    }
                }
            }
            u.resetPilotAndEntity();
            u.runDiagnostic();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.contains("ADD_GUNNER")) {
            UUID selected = UUID.fromString(st.nextToken());
            Unit u = gui.getCampaign().getUnit(selected);
            if (null != u) {
                for (Person p : people) {
                    if (u.canTakeMoreGunners()) {
                        Unit oldUnit = gui.getCampaign().getUnit(p.getUnitId());
                        if (null != oldUnit) {
                            oldUnit.remove(p, true);
                        }
                        u.addGunner(p);
                    }
                }
            }
            u.resetPilotAndEntity();
            u.runDiagnostic();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.contains("ADD_CREW")) {
            UUID selected = UUID.fromString(st.nextToken());
            Unit u = gui.getCampaign().getUnit(selected);
            if (null != u) {
                for (Person p : people) {
                    if (u.canTakeMoreVesselCrew()) {
                        Unit oldUnit = gui.getCampaign().getUnit(p.getUnitId());
                        if (null != oldUnit) {
                            oldUnit.remove(p, true);
                        }
                        u.addVesselCrew(p);
                    }
                }
            }
            u.resetPilotAndEntity();
            u.runDiagnostic();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.contains("ADD_NAV")) {
            UUID selected = UUID.fromString(st.nextToken());
            Unit u = gui.getCampaign().getUnit(selected);
            if (null != u) {
                for (Person p : people) {
                    if (u.canTakeNavigator()) {
                        Unit oldUnit = gui.getCampaign().getUnit(p.getUnitId());
                        if (null != oldUnit) {
                            oldUnit.remove(p, true);
                        }
                        u.setNavigator(p);
                    }
                }
            }
            u.resetPilotAndEntity();
            u.runDiagnostic();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.contains("ADD_TECH")) {
            UUID selected = UUID.fromString(st.nextToken());
            Unit u = gui.getCampaign().getUnit(selected);
            if (null != u) {
                if (u.canTakeTech()) {
                    u.setTech(selectedPerson);
                }
            }
            u.resetPilotAndEntity();
            u.runDiagnostic();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.contains("REMOVE_SPOUSE")) {
            selectedPerson.getSpouse().addLogEntry(gui.getCampaign().getDate(),
                    "Divorced from " + selectedPerson.getFullName());
            selectedPerson.addLogEntry(gui.getCampaign().getDate(),
                    "Divorced from "
                            + selectedPerson.getSpouse().getFullName());
            selectedPerson.getSpouse().setSpouseID(null);
            selectedPerson.setSpouseID(null);
            gui.refreshPersonnelList();
        } else if (command.contains("SPOUSE")) {
            Person spouse = gui.getCampaign().getPerson(
                    UUID.fromString(st.nextToken()));
            spouse.setSpouseID(selectedPerson.getId());
            spouse.addLogEntry(gui.getCampaign().getDate(), "Marries "
                    + selectedPerson.getFullName());
            selectedPerson.setSpouseID(spouse.getId());
            selectedPerson.addLogEntry(gui.getCampaign().getDate(), "Marries "
                    + spouse.getFullName());
            gui.refreshPersonnelList();
        } else if (command.contains("IMPROVE")) {
            String type = st.nextToken();
            int cost = Integer.parseInt(st.nextToken());
            int oldExpLevel = selectedPerson.getExperienceLevel(false);
            selectedPerson.improveSkill(type);
            gui.getCampaign().personUpdated(selectedPerson);
            selectedPerson.setXp(selectedPerson.getXp() - cost);
            gui.getCampaign().addReport(
                    selectedPerson.getHyperlinkedName() + " improved "
                            + type + "!");
            if (gui.getCampaign().getCampaignOptions().getUseAtB()) {
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
                                    PilotOptions.EDGE_ADVANTAGES, "edge",
                                    selectedPerson.getEdge() + 1);
                            gui.getCampaign().addReport(
                                    selectedPerson.getHyperlinkedName()
                                            + " gained edge point!");
                        }
                    } else {
                        gui.getCampaign().addReport(
                                selectedPerson.getHyperlinkedName()
                                        + " gained "
                                        + SpecialAbility
                                                .getDisplayName(spa) + "!");
                    }
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshReport();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.contains("ABILITY")) {
            String selected = st.nextToken();
            int cost = Integer.parseInt(st.nextToken());
            selectedPerson.acquireAbility(PilotOptions.LVL3_ADVANTAGES,
                    selected, true);
            gui.getCampaign().personUpdated(selectedPerson);
            selectedPerson.setXp(selectedPerson.getXp() - cost);
            // TODO: add gui.getCampaign() report
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.contains("WSPECIALIST")) {
            String selected = st.nextToken();
            int cost = Integer.parseInt(st.nextToken());
            selectedPerson.acquireAbility(PilotOptions.LVL3_ADVANTAGES,
                    "weapon_specialist", selected);
            gui.getCampaign().personUpdated(selectedPerson);
            selectedPerson.setXp(selectedPerson.getXp() - cost);
            // TODO: add campaign report
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.contains("SPECIALIST")) {
            String selected = st.nextToken();
            int cost = Integer.parseInt(st.nextToken());
            selectedPerson.acquireAbility(PilotOptions.LVL3_ADVANTAGES,
                    "specialist", selected);
            gui.getCampaign().personUpdated(selectedPerson);
            selectedPerson.setXp(selectedPerson.getXp() - cost);
            // TODO: add campaign report
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("STATUS")) {
            int selected = Integer.parseInt(st.nextToken());
            for (Person person : people) {
                if (selected == Person.S_ACTIVE
                        || (0 == JOptionPane.showConfirmDialog(null,
                                "Do you really want to change the status of "
                                        + person.getFullTitle()
                                        + " to a non-active status?",
                                "KIA?", JOptionPane.YES_NO_OPTION))) {
                    gui.getCampaign().changeStatus(person, selected);
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.filterPersonnel();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshReport();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("PRISONER_STATUS")) {
            int selected = Integer.parseInt(st.nextToken());
            for (Person person : people) {
                gui.getCampaign().changePrisonerStatus(person, selected);
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.filterPersonnel();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshReport();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("EDGE")) {
            String trigger = st.nextToken();
            if (people.length > 1) {
                boolean status = Boolean.parseBoolean(st.nextToken());
                for (Person person : people) {
                    person.setEdgeTrigger(trigger, status);
                    gui.getCampaign().personUpdated(person);
                }
            } else {
                selectedPerson.changeEdgeTrigger(trigger);
                gui.getCampaign().personUpdated(selectedPerson);
            }
            gui.refreshPersonnelList();
            gui.refreshPersonnelView();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("REMOVE")) {
            for (Person person : people) {
                if (0 == JOptionPane.showConfirmDialog(
                        null,
                        "Do you really want to remove "
                                + person.getFullTitle() + "?", "Remove?",
                        JOptionPane.YES_NO_OPTION)) {
                    gui.getCampaign().removePerson(person.getId());
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshOrganization();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("SACK")) {
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
                if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                        null,
                        "Do you really want to remove " +
                        		((people.length > 1)?"personnel?":
                                people[0].getFullTitle() + "?"),
                        "Remove?",
                        JOptionPane.YES_NO_OPTION)) {
                	for (Person person : people) {
                		gui.getCampaign().removePerson(person.getId());
                	}
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshTechsList();
            gui.refreshDoctorsList();
            gui.refreshOrganization();
            gui.refreshReport();
        } else if (command.equalsIgnoreCase("EDIT")) {
            CustomizePersonDialog npd = new CustomizePersonDialog(
                    gui.getFrame(), true, selectedPerson, gui.getCampaign());
            npd.setVisible(true);
            gui.getCampaign().personUpdated(selectedPerson);
            gui.refreshPatientList();
            gui.refreshDoctorsList();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("HEAL")) {
            for (Person person : people) {
                person.setHits(0);
                person.setDoctorId(null, gui.getCampaign().getCampaignOptions()
                        .getNaturalHealingWaitingPeriod());
            }
            gui.getCampaign().personUpdated(selectedPerson);
            gui.refreshPatientList();
            gui.refreshDoctorsList();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("PORTRAIT")) {
            PortraitChoiceDialog pcd = new PortraitChoiceDialog(gui.getFrame(),
                    true, selectedPerson.getPortraitCategory(),
                    selectedPerson.getPortraitFileName(), gui.getIconPackage()
                            .getPortraits());
            pcd.setVisible(true);
            selectedPerson.setPortraitCategory(pcd.getCategory());
            selectedPerson.setPortraitFileName(pcd.getFileName());
            gui.getCampaign().personUpdated(selectedPerson);
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("BIOGRAPHY")) {
            TextAreaDialog tad = new TextAreaDialog(gui.getFrame(), true,
                    "Edit Biography", selectedPerson.getBiography());
            tad.setVisible(true);
            if (tad.wasChanged()) {
                selectedPerson.setBiography(tad.getText());
            }
            gui.refreshPersonnelList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("XP_ADD")) {
            for (Person person : people) {
                person.setXp(person.getXp() + 1);
            }
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("XP_SET")) {
            PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                    gui.getFrame(), true, "XP", selectedPerson.getXp(), 0,
                    Math.max(selectedPerson.getXp() + 10, 100));
            pvcd.setVisible(true);
            if (pvcd.getValue() < 0) {
                return;
            }
            int i = pvcd.getValue();
            for (Person person : people) {
                person.setXp(i);
            }
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("EDGE_SET")) {
            PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                    gui.getFrame(), true, "Edge", selectedPerson.getEdge(), 0,
                    10);
            pvcd.setVisible(true);
            if (pvcd.getValue() < 0) {
                return;
            }
            int i = pvcd.getValue();
            for (Person person : people) {
                person.setEdge(i);
                gui.getCampaign().personUpdated(person);
            }
            gui.refreshPersonnelList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("KILL")) {
            KillDialog nkd;
            if (people.length > 1) {
                nkd = new KillDialog(
                        gui.getFrame(),
                        true,
                        new Kill(
                                null,
                                "?",
                                gui.getCampaign().getUnit(
                                        selectedPerson.getUnitId()) != null ? gui.getCampaign()
                                        .getUnit(selectedPerson.getUnitId())
                                        .getName()
                                        : "Bare Hands", gui.getCampaign()
                                        .getDate()), "Crew");
            } else {
                nkd = new KillDialog(
                        gui.getFrame(),
                        true,
                        new Kill(
                                selectedPerson.getId(),
                                "?",
                                gui.getCampaign().getUnit(
                                        selectedPerson.getUnitId()) != null ? gui.getCampaign()
                                        .getUnit(selectedPerson.getUnitId())
                                        .getName()
                                        : "Bare Hands", gui.getCampaign()
                                        .getDate()),
                        selectedPerson.getFullName());
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
            gui.refreshPersonnelList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("KILL_LOG")) {
            EditKillLogDialog ekld = new EditKillLogDialog(gui.getFrame(),
                    true, gui.getCampaign(), selectedPerson);
            ekld.setVisible(true);
            gui.refreshPersonnelList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("LOG")) {
            EditPersonnelLogDialog epld = new EditPersonnelLogDialog(
                    gui.getFrame(), true, gui.getCampaign(), selectedPerson);
            epld.setVisible(true);
            gui.refreshPersonnelList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("LOG_SINGLE")) {
            EditLogEntryDialog eeld = new EditLogEntryDialog(gui.getFrame(), true,
                    new LogEntry(gui.getCampaign().getDate(), ""));
            eeld.setVisible(true);
            LogEntry entry = eeld.getEntry();
            if (null != entry) {
                for (Person person : people) {
                    person.addLogEntry(entry.clone());
                }
            }
            gui.refreshPersonnelList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("COMMANDER")) {
            selectedPerson.setCommander(!selectedPerson.isCommander());
            if (selectedPerson.isCommander()) {
                for (Person p : gui.getCampaign().getPersonnel()) {
                    if (p.isCommander()
                            && !p.getId().equals(selectedPerson.getId())) {
                        p.setCommander(false);
                        gui.getCampaign()
                                .addReport(
                                        p.getHyperlinkedFullTitle()
                                                + " has been removed as the overall unit commander.");
                        gui.getCampaign().personUpdated(p);
                    }
                }
                gui.getCampaign()
                        .addReport(
                                selectedPerson.getHyperlinkedFullTitle()
                                        + " has been set as the overall unit commander.");
                gui.getCampaign().personUpdated(selectedPerson);
            }
            gui.refreshReport();
        } else if (command.equalsIgnoreCase("DEPENDENT")) {
            if (people.length > 1) {
                boolean status = Boolean.parseBoolean(st.nextToken());
                for (Person person : people) {
                    person.setDependent(status);
                    gui.getCampaign().personUpdated(person);
                }
            } else {
                selectedPerson.setDependent(!selectedPerson.isDependent());
                gui.getCampaign().personUpdated(selectedPerson);
            }
        } else if (command.equalsIgnoreCase("CALLSIGN")) {
            String s = (String) JOptionPane.showInputDialog(gui.getFrame(),
                    "Enter new callsign", "Edit Callsign",
                    JOptionPane.PLAIN_MESSAGE, null, null,
                    selectedPerson.getCallsign());
            if (null != s) {
                selectedPerson.setCallsign(s);
            }
            gui.getCampaign().personUpdated(selectedPerson);
            gui.refreshPersonnelList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("CLEAR_INJURIES")) {
            for (Person person : people) {
                person.clearInjuries();
                Unit u = gui.getCampaign().getUnit(person.getUnitId());
                if (null != u) {
                    u.resetPilotAndEntity();
                }
            }
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshOverview();
        } else if (command.contains("REMOVE_INJURY")) {
            String sel = command.split(":")[1];
            Injury toRemove = null;
            for (Injury i : selectedPerson.getInjuries()) {
                if (i.getUUIDAsString().equals(sel)) {
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
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("EDIT_INJURIES")) {
            EditPersonnelInjuriesDialog epid = new EditPersonnelInjuriesDialog(
                    gui.getFrame(), true, gui.getCampaign(), selectedPerson);
            epid.setVisible(true);
            gui.refreshPatientList();
            gui.refreshPersonnelList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("BLOODNAME")) {
            for (Person p : people) {
                if (!p.isClanner()) {
                    continue;
                }
                gui.getCampaign()
                        .checkBloodnameAdd(p, p.getPrimaryRole(), true);
            }
            gui.getCampaign().personUpdated(selectedPerson);
            gui.refreshPatientList();
            gui.refreshDoctorsList();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
        } else if (command.equalsIgnoreCase("SALARY")) {
            PopupValueChoiceDialog pcvd = new PopupValueChoiceDialog(gui.getFrame(),
                    true, "Change Salary (-1 to remove custom salary)",
                    selectedPerson.getSalary(), -1, 100000);
            pcvd.setVisible(true);
            int salary = pcvd.getValue();
            if (salary < -1) {
                return;
            }
            for (Person person : people) {
                person.setSalary(salary);
            }
            gui.refreshPersonnelList();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            if ((gui.getSplitPersonnel().getSize().width
                    - gui.getSplitPersonnel().getDividerLocation() + gui.getSplitPersonnel()
                        .getDividerSize()) < CampaignGUI.PERSONNEL_VIEW_WIDTH) {
                // expand
                gui.getSplitPersonnel().resetToPreferredSizes();
            } else {
                // collapse
                gui.getSplitPersonnel().setDividerLocation(1.0);
            }

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

    private void maybeShowPopup(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();

        if (e.isPopupTrigger()) {
            if (gui.getPersonnelTable().getSelectedRowCount() == 0) {
                return;
            }
            int row = gui.getPersonnelTable().getSelectedRow();
            boolean oneSelected = gui.getPersonnelTable().getSelectedRowCount() == 1;
            Person person = gui.getPersonnelModel().getPerson(gui.getPersonnelTable()
                    .convertRowIndexToModel(row));
            JMenuItem menuItem = null;
            JMenu menu = null;
            JMenu submenu = null;
            JCheckBoxMenuItem cbMenuItem = null;
            Person[] selected = gui.getSelectedPeople();
            // **lets fill the pop up menu**//
            if (StaticChecks.areAllEligible(selected)) {
                menu = new JMenu("Change Rank");
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

                    if (rank.getName(profession).equals("-")) {
                        continue;
                    }

                    // re-route through any profession redirections,
                    // starting with the empty profession check
                    while (rank.getName(profession).startsWith("--")
                            && profession != Ranks.RPROF_MW) {
                        if (rank.getName(profession).equals("--")) {
                            profession = ranks
                                    .getAlternateProfession(profession);
                        } else if (rank.getName(profession)
                                .startsWith("--")) {
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
                                            + Utilities
                                                    .getRomanNumeralsFromArabicNumber(
                                                            level, true));
                            cbMenuItem.setActionCommand("RANK|" + rankOrder
                                    + "|" + level);
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
                        cbMenuItem.setActionCommand("RANK|" + rankOrder);
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
            menu = new JMenu("Change Rank System");
            // First allow them to revert to the campaign system
            cbMenuItem = new JCheckBoxMenuItem("Use Campaign Rank System");
            cbMenuItem.setActionCommand("RANKSYSTEM|" + "-1");
            cbMenuItem.addActionListener(this);
            cbMenuItem.setEnabled(true);
            menu.add(cbMenuItem);
            for (int system = 0; system < Ranks.RS_NUM; system++) {
                if (system == Ranks.RS_CUSTOM) {
                    continue;
                }
                cbMenuItem = new JCheckBoxMenuItem(
                        Ranks.getRankSystemName(system));
                cbMenuItem.setActionCommand("RANKSYSTEM|" + system);
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
                menu = new JMenu("Change Manei Domini Rank");
                for (int i = Rank.MD_RANK_NONE; i < Rank.MD_RANK_NUM; i++) {
                    cbMenuItem = new JCheckBoxMenuItem(
                            Rank.getManeiDominiRankName(i));
                    cbMenuItem.setActionCommand("MD_RANK|" + i);
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
                menu = new JMenu("Change Manei Domini Class");
                for (int i = Person.MD_NONE; i < Person.MD_NUM; i++) {
                    cbMenuItem = new JCheckBoxMenuItem(
                            Person.getManeiDominiClassNames(i, Ranks.RS_WOB));
                    cbMenuItem.setActionCommand("MD_CLASS|" + i);
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
                menu = new JMenu("Change Primary Designation");
                for (int i = Person.DESIG_NONE; i < Person.DESIG_NUM; i++) {
                    cbMenuItem = new JCheckBoxMenuItem(
                            Person.parseDesignator(i));
                    cbMenuItem.setActionCommand("DESIG_PRI|" + i);
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

                menu = new JMenu("Change Secondary Designation");
                for (int i = Person.DESIG_NONE; i < Person.DESIG_NUM; i++) {
                    cbMenuItem = new JCheckBoxMenuItem(
                            Person.parseDesignator(i));
                    cbMenuItem.setActionCommand("DESIG_SEC|" + i);
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
            menu = new JMenu("Change Status");
            for (int s = 0; s < Person.S_NUM; s++) {
                cbMenuItem = new JCheckBoxMenuItem(Person.getStatusName(s));
                if (person.getStatus() == s) {
                    cbMenuItem.setSelected(true);
                }
                cbMenuItem.setActionCommand("STATUS|" + s);
                cbMenuItem.addActionListener(this);
                cbMenuItem.setEnabled(true);
                menu.add(cbMenuItem);
            }
            popup.add(menu);
            menu = new JMenu("Change Prisoner Status");
            for (int s = 0; s < Person.PRISONER_NUM; s++) {
                cbMenuItem = new JCheckBoxMenuItem(
                        Person.getPrisonerStatusName(s));
                if (person.getPrisonerStatus() == s) {
                    cbMenuItem.setSelected(true);
                }
                cbMenuItem.setActionCommand("PRISONER_STATUS|" + s);
                cbMenuItem.addActionListener(this);
                cbMenuItem.setEnabled(true);
                menu.add(cbMenuItem);
            }
            popup.add(menu);
            menu = new JMenu("Change Primary Role");
            for (int i = Person.T_MECHWARRIOR; i < Person.T_NUM; i++) {
                if (person.canPerformRole(i)
                        && person.getSecondaryRole() != i) {
                    cbMenuItem = new JCheckBoxMenuItem(Person.getRoleDesc(
                            i, gui.getCampaign().getFaction().isClan()));
                    cbMenuItem.setActionCommand("PROLE|" + i);
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
            menu = new JMenu("Change Secondary Role");
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
                    cbMenuItem.setActionCommand("SROLE|" + i);
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
                menuItem = new JMenuItem("Give Random Bloodname");
                menuItem.setActionCommand("BLOODNAME");
                menuItem.addActionListener(this);
                menuItem.setEnabled(StaticChecks.areAllActive(selected));
                popup.add(menuItem);
            }
            // change salary
            if (gui.getCampaign().getCampaignOptions().payForSalaries()) {
                menuItem = new JMenuItem("Set Salary...");
                menuItem.setActionCommand("SALARY");
                menuItem.addActionListener(this);
                menuItem.setEnabled(StaticChecks.areAllActive(selected));
                popup.add(menuItem);
            }
            // switch pilot
            menu = new JMenu("Assign to Unit");
            JMenu pilotMenu = new JMenu("As Pilot");
            JMenu crewMenu = new JMenu("As Crewmember");
            JMenu driverMenu = new JMenu("As Driver");
            JMenu gunnerMenu = new JMenu("As Gunner");
            JMenu soldierMenu = new JMenu("As Soldier");
            JMenu techMenu = new JMenu("As Tech");
            JMenu navMenu = new JMenu("As Navigator");
            cbMenuItem = new JCheckBoxMenuItem("None");
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
                            cbMenuItem.setActionCommand("ADD_PILOT|"
                                    + unit.getId());
                            cbMenuItem.addActionListener(this);
                            pilotMenu.add(cbMenuItem);
                        }
                    } else if (unit.usesSoldiers()) {
                        if (unit.canTakeMoreGunners()
                                && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand("ADD_SOLDIER|"
                                    + unit.getId());
                            cbMenuItem.addActionListener(this);
                            soldierMenu.add(cbMenuItem);
                        }
                    } else {
                        if (unit.canTakeMoreDrivers()
                                && person.canDrive(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand("ADD_DRIVER|"
                                    + unit.getId());
                            cbMenuItem.addActionListener(this);
                            if (unit.getEntity() instanceof Aero) {
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
                            cbMenuItem.setActionCommand("ADD_GUNNER|"
                                    + unit.getId());
                            cbMenuItem.addActionListener(this);
                            gunnerMenu.add(cbMenuItem);
                        }
                        if (unit.canTakeMoreVesselCrew()
                                && person.hasSkill(SkillType.S_TECH_VESSEL)) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand("ADD_CREW|"
                                    + unit.getId());
                            cbMenuItem.addActionListener(this);
                            crewMenu.add(cbMenuItem);
                        }
                        if (unit.canTakeNavigator()
                                && person.hasSkill(SkillType.S_NAV)) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand("ADD_NAV|"
                                    + unit.getId());
                            cbMenuItem.addActionListener(this);
                            navMenu.add(cbMenuItem);
                        }
                    }
                    if (unit.canTakeTech()
                            && person.canTech(unit.getEntity())
                            && (person.getMaintenanceTimeUsing() + unit
                                    .getMaintenanceTime()) <= 480) {
                        cbMenuItem = new JCheckBoxMenuItem(unit.getName()
                                + " (" + unit.getMaintenanceTime()
                                + " minutes/day)");
                        // TODO: check the box
                        cbMenuItem.setActionCommand("ADD_TECH|"
                                + unit.getId());
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
                        if (!(unit.getEntity() instanceof Infantry)
                                || unit.getEntity() instanceof BattleArmor) {
                            continue;
                        }
                        if (unit.canTakeMoreGunners()
                                && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand("ADD_SOLDIER|"
                                    + unit.getId());
                            cbMenuItem.addActionListener(this);
                            soldierMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllBattleArmor(selected)) {
                        if (!(unit.getEntity() instanceof BattleArmor)) {
                            continue;
                        }
                        if (unit.canTakeMoreGunners()
                                && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand("ADD_SOLDIER|"
                                    + unit.getId());
                            cbMenuItem.addActionListener(this);
                            soldierMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllVeeGunners(selected)) {
                        if (!(unit.getEntity() instanceof Tank)) {
                            continue;
                        }
                        if (unit.canTakeMoreGunners()
                                && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand("ADD_GUNNER|"
                                    + unit.getId());
                            cbMenuItem.addActionListener(this);
                            gunnerMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllVesselGunners(selected)) {
                        if (!(unit.getEntity() instanceof Aero)) {
                            continue;
                        }
                        if (unit.canTakeMoreGunners()
                                && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(
                                    unit.getName());
                            // TODO: check the box
                            cbMenuItem.setActionCommand("ADD_GUNNER|"
                                    + unit.getId());
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
                            cbMenuItem.setActionCommand("ADD_CREW|"
                                    + unit.getId());
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
                            cbMenuItem.setActionCommand("ADD_VESSEL_PILOT|"
                                    + unit.getId());
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
                            cbMenuItem.setActionCommand("ADD_NAV|"
                                    + unit.getId());
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
            cbMenuItem.setActionCommand("REMOVE_UNIT|" + -1);
            cbMenuItem.addActionListener(this);
            menu.add(cbMenuItem);
            if (oneSelected && person.isActive()) {
                if (person.getAge(gui.getCampaign().getCalendar()) > 13
                        && person.getSpouseID() == null) {
                    menu = new JMenu("Choose Spouse (Mate)");
                    for (Person ps : gui.getCampaign().getPersonnel()) {
                        if (person.safeSpouse(ps)) {
                            menuItem = new JMenuItem(
                                    ps.getFullName()
                                            + ", "
                                            + ps.getAge(gui.getCampaign()
                                                    .getCalendar()) + ", "
                                            + ps.getRoleDesc());
                            menuItem.setActionCommand("SPOUSE|"
                                    + ps.getId().toString());
                            menuItem.addActionListener(this);
                            menu.add(menuItem);
                        }
                    }
                    if (menu.getItemCount() > 30) {
                        MenuScroller.setScrollerFor(menu, 20);
                    }
                    popup.add(menu);
                }
                if (person.getSpouseID() != null) {
                    menuItem = new JMenuItem("Remove Spouse");
                    menuItem.setActionCommand("REMOVE_SPOUSE");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }
                menu = new JMenu("Spend XP");
                JMenu currentMenu = new JMenu("Current Skills");
                JMenu newMenu = new JMenu("New Skills");
                for (int i = 0; i < SkillType.getSkillList().length; i++) {
                    String type = SkillType.getSkillList()[i];
                    if (person.hasSkill(type)) {
                        int cost = person.getSkill(type).getCostToImprove();
                        if (cost >= 0) {
                            String costDesc = " (" + cost + "XP)";
                            menuItem = new JMenuItem(type + costDesc);
                            menuItem.setActionCommand("IMPROVE|" + type
                                    + "|" + cost);
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(person.getXp() >= cost);
                            currentMenu.add(menuItem);
                        }
                    } else {
                        int cost = SkillType.getType(type).getCost(0);
                        if (cost >= 0) {
                            String costDesc = " (" + cost + "XP)";
                            menuItem = new JMenuItem(type + costDesc);
                            menuItem.setActionCommand("IMPROVE|" + type
                                    + "|" + cost);
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(person.getXp() >= cost);
                            newMenu.add(menuItem);
                        }
                    }
                }
                menu.add(currentMenu);
                menu.add(newMenu);
                if (gui.getCampaign().getCampaignOptions().useAbilities()) {
                    JMenu abMenu = new JMenu("Special Abilities");
                    int cost = -1;
                    String costDesc = "";
                    for (Enumeration<IOption> i = person
                            .getOptions(PilotOptions.LVL3_ADVANTAGES); i
                            .hasMoreElements();) {
                        IOption ability = i.nextElement();
                        if (!ability.booleanValue()) {
                            SpecialAbility spa = SpecialAbility
                                    .getAbility(ability.getName());
                            if (null == spa) {
                                continue;
                            }
                            if (!spa.isEligible(person)) {
                                continue;
                            }
                            cost = spa.getCost();
                            costDesc = " (" + cost + "XP)";
                            if (cost < 0) {
                                costDesc = " (Not Possible)";
                            }
                            if (ability.getName().equals(
                                    "weapon_specialist")) {
                                Unit u = gui.getCampaign().getUnit(
                                        person.getUnitId());
                                if (null != u) {
                                    JMenu specialistMenu = new JMenu(
                                            "Weapon Specialist");
                                    TreeSet<String> uniqueWeapons = new TreeSet<String>();
                                    for (int j = 0; j < u.getEntity()
                                            .getWeaponList().size(); j++) {
                                        Mounted m = u.getEntity()
                                                .getWeaponList().get(j);
                                        uniqueWeapons.add(m.getName());
                                    }
                                    for (String name : uniqueWeapons) {
                                        menuItem = new JMenuItem(name
                                                + costDesc);
                                        menuItem.setActionCommand("WSPECIALIST|"
                                                + name + "|" + cost);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(cost >= 0
                                                && person.getXp() >= cost);
                                        specialistMenu.add(menuItem);
                                    }
                                    abMenu.add(specialistMenu);
                                }
                            } else if (ability.getName().equals(
                                    "specialist")) {
                                JMenu specialistMenu = new JMenu(
                                        "Specialist");
                                menuItem = new JMenuItem("Laser Specialist"
                                        + costDesc);
                                menuItem.setActionCommand("SPECIALIST|"
                                        + Crew.SPECIAL_LASER + "|" + cost);
                                menuItem.addActionListener(this);
                                menuItem.setEnabled(cost >= 0
                                        && person.getXp() >= cost);
                                specialistMenu.add(menuItem);
                                menuItem = new JMenuItem(
                                        "Missile Specialist" + costDesc);
                                menuItem.setActionCommand("SPECIALIST|"
                                        + Crew.SPECIAL_MISSILE + "|" + cost);
                                menuItem.addActionListener(this);
                                menuItem.setEnabled(cost >= 0
                                        && person.getXp() >= cost);
                                specialistMenu.add(menuItem);
                                menuItem = new JMenuItem(
                                        "Ballistic Specialist" + costDesc);
                                menuItem.setActionCommand("SPECIALIST|"
                                        + Crew.SPECIAL_BALLISTIC + "|"
                                        + cost);
                                menuItem.addActionListener(this);
                                menuItem.setEnabled(cost >= 0
                                        && person.getXp() >= cost);
                                specialistMenu.add(menuItem);
                                abMenu.add(specialistMenu);
                            } else {
                                menuItem = new JMenuItem(
                                        ability.getDisplayableName()
                                                + costDesc);
                                menuItem.setActionCommand("ABILITY|"
                                        + ability.getName() + "|" + cost);
                                menuItem.addActionListener(this);
                                menuItem.setEnabled(cost >= 0
                                        && person.getXp() >= cost);
                                abMenu.add(menuItem);
                            }
                        }
                    }
                    if (abMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(abMenu, 20);
                    }
                    menu.add(abMenu);
                }
                popup.add(menu);
            }
            if (oneSelected && person.isActive()) {
                if (gui.getCampaign().getCampaignOptions().useEdge()) {
                    menu = new JMenu("Set Edge Triggers");
                    cbMenuItem = new JCheckBoxMenuItem("Head Hits");
                    cbMenuItem.setSelected(person.getOptions()
                            .booleanOption("edge_when_headhit"));
                    cbMenuItem.setActionCommand("EDGE|edge_when_headhit");
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);
                    cbMenuItem = new JCheckBoxMenuItem(
                            "Through Armor Crits");
                    cbMenuItem.setSelected(person.getOptions()
                            .booleanOption("edge_when_tac"));
                    cbMenuItem.setActionCommand("EDGE|edge_when_tac");
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);
                    cbMenuItem = new JCheckBoxMenuItem("Fail KO check");
                    cbMenuItem.setSelected(person.getOptions()
                            .booleanOption("edge_when_ko"));
                    cbMenuItem.setActionCommand("EDGE|edge_when_ko");
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);
                    cbMenuItem = new JCheckBoxMenuItem("Ammo Explosion");
                    cbMenuItem.setSelected(person.getOptions()
                            .booleanOption("edge_when_explosion"));
                    cbMenuItem.setActionCommand("EDGE|edge_when_explosion");
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);
                    cbMenuItem = new JCheckBoxMenuItem("MASC Failures");
                    cbMenuItem.setSelected(person.getOptions()
                            .booleanOption("edge_when_masc_fails"));
                    cbMenuItem
                            .setActionCommand("EDGE|edge_when_masc_fails");
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);
                    popup.add(menu);
                }
                menu = new JMenu("Special Flags...");
                cbMenuItem = new JCheckBoxMenuItem("Dependent");
                cbMenuItem.setSelected(person.isDependent());
                cbMenuItem.setActionCommand("DEPENDENT");
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);
                cbMenuItem = new JCheckBoxMenuItem("Commander");
                cbMenuItem.setSelected(person.isCommander());
                cbMenuItem.setActionCommand("COMMANDER");
                cbMenuItem.addActionListener(this);
                menu.add(cbMenuItem);
                popup.add(menu);
            } else if (StaticChecks.areAllActive(selected)) {
                if (gui.getCampaign().getCampaignOptions().useEdge()) {
                    menu = new JMenu("Set Edge Triggers");
                    submenu = new JMenu("On");
                    menuItem = new JMenuItem("Head Hits");
                    menuItem.setActionCommand("EDGE|edge_when_headhit|true");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem("Through Armor Crits");
                    menuItem.setActionCommand("EDGE|edge_when_tac|true");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem("Fail KO check");
                    menuItem.setActionCommand("EDGE|edge_when_ko|true");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem("Ammo Explosion");
                    menuItem.setActionCommand("EDGE|edge_when_explosion|true");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem("MASC Failures");
                    menuItem.setActionCommand("EDGE|edge_when_masc_fails|true");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menu.add(submenu);
                    submenu = new JMenu("Off");
                    menuItem = new JMenuItem("Head Hits");
                    menuItem.setActionCommand("EDGE|edge_when_headhit|false");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem("Through Armor Crits");
                    menuItem.setActionCommand("EDGE|edge_when_tac|false");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem("Fail KO check");
                    menuItem.setActionCommand("EDGE|edge_when_ko|false");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem("Ammo Explosion");
                    menuItem.setActionCommand("EDGE|edge_when_explosion|false");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menuItem = new JMenuItem("MASC Failures");
                    menuItem.setActionCommand("EDGE|edge_when_masc_fails|false");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    menu.add(submenu);
                    popup.add(menu);
                }
                menu = new JMenu("Special Flags...");
                submenu = new JMenu("Dependent");
                menuItem = new JMenuItem("Yes");
                menuItem.setActionCommand("DEPENDENT|true");
                menuItem.addActionListener(this);
                submenu.add(menuItem);
                menuItem = new JMenuItem("No");
                menuItem.setActionCommand("DEPENDENT|false");
                menuItem.addActionListener(this);
                submenu.add(menuItem);
                menu.add(submenu);
                popup.add(menu);
            }
            if (oneSelected) {
                // change portrait
                menuItem = new JMenuItem("Change Portrait...");
                menuItem.setActionCommand("PORTRAIT");
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
                // change Biography
                menuItem = new JMenuItem("Change Biography...");
                menuItem.setActionCommand("BIOGRAPHY");
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
                menuItem = new JMenuItem("Change Callsign...");
                menuItem.setActionCommand("CALLSIGN");
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
                menuItem = new JMenuItem("Edit Personnel Log...");
                menuItem.setActionCommand("LOG");
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);

            }
            menuItem = new JMenuItem("Add Single Log Entry...");
            menuItem.setActionCommand("LOG_SINGLE");
            menuItem.addActionListener(this);
            menuItem.setEnabled(true);
            popup.add(menuItem);
            if (oneSelected || StaticChecks.allHaveSameUnit(selected)) {
                menuItem = new JMenuItem("Assign Kill...");
                menuItem.setActionCommand("KILL");
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
            }
            if (oneSelected) {
                menuItem = new JMenuItem("Edit Kill Log...");
                menuItem.setActionCommand("KILL_LOG");
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
            }
            menuItem = new JMenuItem("Export Personnel");
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    gui.miExportPersonActionPerformed(evt);
                }
            });
            menuItem.setEnabled(true);
            popup.add(menuItem);
            if (gui.getCampaign().getCampaignOptions().getUseAtB()
                    && StaticChecks.areAllActive(selected)) {
                menuItem = new JMenuItem("Sack...");
                menuItem.setActionCommand("SACK");
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }
            menu = new JMenu("GM Mode");
            menuItem = new JMenuItem("Remove Person");
            menuItem.setActionCommand("REMOVE");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            if (!gui.getCampaign().getCampaignOptions().useAdvancedMedical()) {
                menuItem = new JMenuItem("Heal Person");
                menuItem.setActionCommand("HEAL");
                menuItem.addActionListener(this);
                menuItem.setEnabled(gui.getCampaign().isGM());
                menu.add(menuItem);
            }
            menuItem = new JMenuItem("Add XP");
            menuItem.setActionCommand("XP_ADD");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            menuItem = new JMenuItem("Set XP");
            menuItem.setActionCommand("XP_SET");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            if (gui.getCampaign().getCampaignOptions().useEdge()) {
                menuItem = new JMenuItem("Set Edge");
                menuItem.setActionCommand("EDGE_SET");
                menuItem.addActionListener(this);
                menuItem.setEnabled(gui.getCampaign().isGM());
                menu.add(menuItem);
            }
            if (oneSelected) {
                menuItem = new JMenuItem("Edit...");
                menuItem.setActionCommand("EDIT");
                menuItem.addActionListener(this);
                menuItem.setEnabled(gui.getCampaign().isGM());
                menu.add(menuItem);
            }
            if (gui.getCampaign().getCampaignOptions().useAdvancedMedical()) {
                menuItem = new JMenuItem("Remove All Injuries");
                menuItem.setActionCommand("CLEAR_INJURIES");
                menuItem.addActionListener(this);
                menuItem.setEnabled(gui.getCampaign().isGM());
                menu.add(menuItem);
                if (oneSelected) {
                    for (Injury i : person.getInjuries()) {
                        menuItem = new JMenuItem("Remove Injury: "
                                + i.getName());
                        menuItem.setActionCommand("REMOVE_INJURY:"
                                + i.getUUIDAsString());
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(gui.getCampaign().isGM());
                        menu.add(menuItem);
                    }

                    menuItem = new JMenuItem("Edit Injuries");
                    menuItem.setActionCommand("EDIT_INJURIES");
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
}