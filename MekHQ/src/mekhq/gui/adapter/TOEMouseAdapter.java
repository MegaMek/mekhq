package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.MouseInputAdapter;
import javax.swing.tree.TreePath;

import megamek.client.ui.swing.util.MenuScroller;
import megamek.common.EntityWeightClass;
import megamek.common.GunEmplacement;
import megamek.common.UnitType;
import mekhq.MekHQ;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.event.NetworkChangedEvent;
import mekhq.campaign.event.OrganizationChangedEvent;
import mekhq.campaign.event.PersonTechAssignmentEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.ImageChoiceDialog;
import mekhq.gui.dialog.TextAreaDialog;
import mekhq.gui.utilities.StaticChecks;

public class TOEMouseAdapter extends MouseInputAdapter implements
ActionListener {
    private CampaignGUI gui;

    public TOEMouseAdapter(CampaignGUI gui) {
        super();
        this.gui = gui;
    }

    @Override
    public void actionPerformed(ActionEvent action) {
        StringTokenizer st = new StringTokenizer(action.getActionCommand(),
                "|");
        String command = st.nextToken();
        String type = st.nextToken();
        String target = st.nextToken();
        Vector<Force> forces = new Vector<Force>();
        Vector<Unit> units = new Vector<Unit>();
        while (st.hasMoreTokens()) {
            String id = st.nextToken();
            if (type.equals("FORCE")) {
                Force force = gui.getCampaign().getForce(Integer.parseInt(id));
                if (null != force) {
                    forces.add(force);
                }
            }
            if (type.equals("UNIT")) {
                Unit unit = gui.getCampaign().getUnit(UUID.fromString(id));
                if (null != unit) {
                    units.add(unit);
                }
            }
        }
        if (type.equals("FORCE")) {
            Vector<Force> newForces = new Vector<Force>();
            for (Force force : forces) {
                boolean duplicate = false;
                for (Force otherForce : forces) {
                    if (otherForce.getId() == force.getId()) {
                        continue;
                    }
                    if (otherForce.isAncestorOf(force)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    newForces.add(force);
                }
            }
            forces = newForces;
        }
        // TODO: eliminate any forces that are descendants of other forces
        // in the vector
        Force singleForce = null;
        if (!forces.isEmpty()) {
            singleForce = forces.get(0);
        }
        Unit singleUnit = null;
        if (!units.isEmpty()) {
            singleUnit = units.get(0);
        }
        if (command.contains("ADD_FORCE")) {
            if (null != singleForce) {
                String name = (String) JOptionPane.showInputDialog(null,
                        "Enter the force name", "Force Name",
                        JOptionPane.PLAIN_MESSAGE, null, null, "My Lance");
                if (null != name) {
                    Force f = new Force(name);
                    gui.getCampaign().addForce(f, singleForce);
                    MekHQ.triggerEvent(new OrganizationChangedEvent(f));
                }
            }
        }
        if (command.contains("ADD_LANCE_TECH")) {
            if (null != singleForce) {
                Person tech = gui.getCampaign().getPerson(UUID.fromString(target));
                if (null != tech) {
                    if (singleForce.getTechID() != null) {
                        Person oldTech = gui.getCampaign().getPerson(singleForce.getTechID());
                        oldTech.clearTechUnitIDs();
                        oldTech.addLogEntry(gui.getCampaign().getDate(), "Removed from " + singleForce.getName());
                    }
                    singleForce.setTechID(tech.getId());
                    tech.addLogEntry(gui.getCampaign().getDate(), "Assigned to " + singleForce.getFullName());
                    if (singleForce.getAllUnits() !=null) {
                        String cantTech = "";
                        for (UUID uuid : singleForce.getAllUnits()) {
                            Unit u = gui.getCampaign().getUnit(uuid);
                            if (u != null) {
                                if (null != u.getTech()) {
                                    Person oldTech = u.getTech();
                                    oldTech.removeTechUnitId(u.getId());
                                    u.removeTech();
                                }
                                if (tech.canTech(u.getEntity())) {
                                    u.setTech(tech.getId());
                                    tech.addTechUnitID(u.getId());
                                    MekHQ.triggerEvent(new PersonTechAssignmentEvent(tech, u));
                                } else {
                                    cantTech += tech.getName() + " cannot maintain " + u.getName() + "\n";
                                }
                            }
                        }
                        if (cantTech != "") {
                            cantTech += "You will need to assign a tech manually.";
                            JOptionPane.showMessageDialog(null, cantTech, "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }
            }
        }
        if (command.contains("ADD_UNIT")) {
            if (null != singleForce) {
                Unit u = gui.getCampaign().getUnit(UUID.fromString(target));
                if (null != u) {
                    gui.getCampaign().addUnitToForce(u, singleForce.getId());
                }
            }
        } else if (command.contains("UNDEPLOY_FORCE")) {
            for (Force force : forces) {
                gui.undeployForce(force);
                //Event triggered from undeployForce
            }
        } else if (command.contains("DEPLOY_FORCE")) {
            int sid = Integer.parseInt(target);
            Scenario scenario = gui.getCampaign().getScenario(sid);
            for (Force force : forces) {
                gui.undeployForce(force);
                force.clearScenarioIds(gui.getCampaign(), true);
                if (null != force && null != scenario) {
                    scenario.addForces(force.getId());
                    force.setScenarioId(scenario.getId());
                    for (UUID uid : force.getAllUnits()) {
                        Unit u = gui.getCampaign().getUnit(uid);
                        if (null != u) {
                            u.setScenarioId(scenario.getId());
                        }
                    }
                }
                MekHQ.triggerEvent(new DeploymentChangedEvent(force, scenario));
            }
        } else if (command.contains("CHANGE_ICON")) {
            if (null != singleForce) {
                ImageChoiceDialog pcd = new ImageChoiceDialog(
                        gui.getFrame(), true, singleForce.getIconCategory(),
                        singleForce.getIconFileName(), gui.getIconPackage()
                        .getForceIcons(), true);
                pcd.setVisible(true);
                if (pcd.isChanged()) {
                    singleForce.setIconCategory(pcd.getCategory());
                    singleForce.setIconFileName(pcd.getFileName());
                    singleForce.setIconMap(pcd.getIconMap());
                    MekHQ.triggerEvent(new OrganizationChangedEvent(singleForce));
                }
            }
        } else if (command.contains("CHANGE_NAME")) {
            if (null != singleForce) {
                String name = (String) JOptionPane.showInputDialog(null,
                        "Enter the force name", "Force Name",
                        JOptionPane.PLAIN_MESSAGE, null, null,
                        singleForce.getName());
                if (name != null) {
                    singleForce.setName(name);
                }
                MekHQ.triggerEvent(new OrganizationChangedEvent(singleForce));
            }
        } else if (command.contains("CHANGE_DESC")) {
            if (null != singleForce) {
                TextAreaDialog tad = new TextAreaDialog(gui.getFrame(), true,
                        "Edit Force Description",
                        singleForce.getDescription());
                tad.setVisible(true);
                if (tad.wasChanged()) {
                    singleForce.setDescription(tad.getText());
                    MekHQ.triggerEvent(new OrganizationChangedEvent(singleForce));
                }
            }
        } else if (command.contains("REMOVE_FORCE")) {
            for (Force force : forces) {
                if (null != force && null != force.getParentForce()) {
                    if (0 != JOptionPane.showConfirmDialog(
                            null,
                            "Are you sure you want to delete "
                                    + force.getFullName() + "?",
                                    "Delete Force?", JOptionPane.YES_NO_OPTION)) {
                        return;
                    }
                    gui.getCampaign().removeForce(force);
                }
            }
        } else if (command.contains("REMOVE_LANCE_TECH")) {
            if (singleForce.getTechID() != null) {
                Person oldTech = gui.getCampaign().getPerson(singleForce.getTechID());
                oldTech.clearTechUnitIDs();
                oldTech.addLogEntry(gui.getCampaign().getDate(), "Removed from " + singleForce.getName());
                if (singleForce.getAllUnits() !=null) {
                    for (UUID uuid : singleForce.getAllUnits()) {
                        Unit u = gui.getCampaign().getUnit(uuid);
                        if (null != u.getTech()) {
                            oldTech = u.getTech();
                            oldTech.removeTechUnitId(u.getId());
                            MekHQ.triggerEvent(new PersonTechAssignmentEvent(oldTech, u));
                        }
                        if (u != null) {
                            u.setTech((UUID)null);
                        }
                    }
                }
                singleForce.setTechID(null);
            }
        } else if (command.contains("REMOVE_UNIT")) {
            for (Unit unit : units) {
                if (null != unit) {
                    Force parentForce = gui.getCampaign().getForceFor(unit);
                    if (null != parentForce) {
                        gui.getCampaign().removeUnitFromForce(unit);
                        if (null != parentForce.getTechID()) {
                            unit.removeTech();
                            Person forceTech = gui.getCampaign().getPerson(parentForce.getTechID());
                            forceTech.removeTechUnitId(unit.getId());
                            MekHQ.triggerEvent(new PersonTechAssignmentEvent(forceTech, unit));
                        }
                    }
                    MekHQ.triggerEvent(new OrganizationChangedEvent(parentForce, unit));
                }
            }
        } else if (command.contains("UNDEPLOY_UNIT")) {
            for (Unit unit : units) {
                gui.undeployUnit(unit);
                //Event triggered from undeployUnit
            }
        } else if (command.contains("GOTO_UNIT")) {
            if (null != singleUnit) {
                gui.focusOnUnit(singleUnit.getId());
            }
        } else if (command.contains("GOTO_PILOT")) {
            if (null != singleUnit && null != singleUnit.getCommander()) {
                gui.focusOnPerson(singleUnit.getCommander().getId());
            }
        } else if (command.contains("DEPLOY_UNIT")) {
            int sid = Integer.parseInt(target);
            Scenario scenario = gui.getCampaign().getScenario(sid);
            for (Unit unit : units) {
                if (null != unit && null != scenario) {
                    scenario.addUnit(unit.getId());
                    unit.setScenarioId(scenario.getId());
                    MekHQ.triggerEvent(new DeploymentChangedEvent(unit, scenario));
                }
            }
        } else if (command.contains("C3I")) {
            // don't set them directly, set the C3i UUIDs and then
            // run gui.refreshNetworks on the campaign
            // TODO: is that too costly?
            Vector<String> uuids = new Vector<String>();
            for (Unit unit : units) {
                if (null == unit.getEntity()) {
                    continue;
                }
                uuids.add(unit.getEntity().getC3UUIDAsString());
            }
            for (int pos = 0; pos < uuids.size(); pos++) {
                for (Unit unit : units) {
                    if (null == unit.getEntity()) {
                        continue;
                    }
                    unit.getEntity().setC3iNextUUIDAsString(pos,
                            uuids.get(pos));
                }
            }
            gui.getCampaign().refreshNetworks();
            MekHQ.triggerEvent(new NetworkChangedEvent(units));
        } else if (command.contains("REMOVE_NETWORK")) {
            gui.getCampaign().removeUnitsFromNetwork(units);
            MekHQ.triggerEvent(new NetworkChangedEvent(units));
        } else if (command.contains("DISBAND_NETWORK")) {
            if (null != singleUnit) {
                gui.getCampaign().disbandNetworkOf(singleUnit);
            }
        } else if (command.contains("ADD_NETWORK")) {
            gui.getCampaign().addUnitsToNetwork(units, target);
        } else if (command.contains("ADD_SLAVES")) {
            for (Unit u : units) {
                u.getEntity().setC3MasterIsUUIDAsString(target);
            }
            gui.getCampaign().refreshNetworks();
            MekHQ.triggerEvent(new NetworkChangedEvent(units));
        } else if (command.contains("SET_MM")) {
            for (Unit u : units) {
                gui.getCampaign().removeUnitsFromC3Master(u);
                u.getEntity().setC3MasterIsUUIDAsString(
                        u.getEntity().getC3UUIDAsString());
            }
            gui.getCampaign().refreshNetworks();
            MekHQ.triggerEvent(new NetworkChangedEvent(units));
        } else if (command.contains("SET_IND_M")) {
            for (Unit u : units) {
                u.getEntity().setC3MasterIsUUIDAsString(null);
                u.getEntity().setC3Master(null, true);
                gui.getCampaign().removeUnitsFromC3Master(u);
            }
            gui.getCampaign().refreshNetworks();
            MekHQ.triggerEvent(new NetworkChangedEvent(units));
        }
        if (command.contains("REMOVE_C3")) {
            for (Unit u : units) {
                u.getEntity().setC3MasterIsUUIDAsString(null);
                u.getEntity().setC3Master(null, true);
            }
            gui.getCampaign().refreshNetworks();
            MekHQ.triggerEvent(new NetworkChangedEvent(units));
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

        if (e.isPopupTrigger()) {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem menuItem;
            JMenu menu;
            JTree tree = (JTree) e.getSource();
            if (tree == null) {
                return;
            }
            // this is a little tricky because we want to
            // distinguish forces and units, but the user can
            // select multiple items of both types
            // we will allow multiple selection of either units or forces
            // but not both - if both are selected then default to
            // unit and deselect all forces
            Vector<Force> forces = new Vector<Force>();
            Vector<Unit> unitsInForces = new Vector<Unit>();
            Vector<Unit> units = new Vector<Unit>();
            Vector<TreePath> uPath = new Vector<TreePath>();
            for (TreePath path : tree.getSelectionPaths()) {
                Object node = path.getLastPathComponent();
                if (node instanceof Force) {
                    forces.add((Force) node);
                }
                if (node instanceof Unit) {
                    units.add((Unit) node);
                    uPath.add(path);
                }
            }
            for(Force force: forces) {
                for(UUID id : force.getAllUnits()) {
                    Unit u = gui.getCampaign().getUnit(id);
                    if(null != u) {
                        unitsInForces.add(u);
                    }
                }
            }
            boolean forcesSelected = !forces.isEmpty();
            boolean unitsSelected = !units.isEmpty();
            // if both are selected then we prefer units
            // and will deselect forces
            if (forcesSelected & unitsSelected) {
                forcesSelected = false;
                TreePath[] paths = new TreePath[uPath.size()];
                int i = 0;
                for (TreePath p : uPath) {
                    paths[i] = p;
                    i++;
                }
                tree.setSelectionPaths(paths);
            }
            boolean multipleSelection = (forcesSelected && forces.size() > 1)
                    || (unitsSelected && units.size() > 1);
            if (forcesSelected) {
                Force force = forces.get(0);
                String forceIds = "" + force.getId();
                for (int i = 1; i < forces.size(); i++) {
                    forceIds += "|" + forces.get(i).getId();
                }
                if (!multipleSelection) {
                    menuItem = new JMenuItem("Change Name...");
                    menuItem.setActionCommand("CHANGE_NAME|FORCE|empty|"
                            + forceIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    popup.add(menuItem);
                    menuItem = new JMenuItem("Change Description...");
                    menuItem.setActionCommand("CHANGE_DESC|FORCE|empty|"
                            + forceIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    popup.add(menuItem);
                    menuItem = new JMenuItem("Add New Force...");
                    menuItem.setActionCommand("ADD_FORCE|FORCE|empty|"
                            + forceIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    popup.add(menuItem);
                    if (force.getTechID() == null) {
                        menu = new JMenu("Add Tech to Force");
                        menu.setEnabled(false);
                        // Mech Techs
                        JMenu mech_techs = new JMenu("Mech Techs");
                        JMenu m_ug_menu = new JMenu(SkillType.ULTRA_GREEN_NM);
                        JMenu m_g_menu = new JMenu(SkillType.GREEN_NM);
                        JMenu m_r_menu = new JMenu(SkillType.REGULAR_NM);
                        JMenu m_v_menu = new JMenu(SkillType.VETERAN_NM);
                        JMenu m_e_menu = new JMenu(SkillType.ELITE_NM);

                        // Aero Techs
                        JMenu aero_techs = new JMenu("Aero Techs");
                        JMenu a_ug_menu = new JMenu(SkillType.ULTRA_GREEN_NM);
                        JMenu a_g_menu = new JMenu(SkillType.GREEN_NM);
                        JMenu a_r_menu = new JMenu(SkillType.REGULAR_NM);
                        JMenu a_v_menu = new JMenu(SkillType.VETERAN_NM);
                        JMenu a_e_menu = new JMenu(SkillType.ELITE_NM);

                        // Mechanics
                        JMenu mechanics = new JMenu("Mechanics");
                        JMenu v_ug_menu = new JMenu(SkillType.ULTRA_GREEN_NM);
                        JMenu v_g_menu = new JMenu(SkillType.GREEN_NM);
                        JMenu v_r_menu = new JMenu(SkillType.REGULAR_NM);
                        JMenu v_v_menu = new JMenu(SkillType.VETERAN_NM);
                        JMenu v_e_menu = new JMenu(SkillType.ELITE_NM);

                        // BA Techs
                        JMenu ba_techs = new JMenu("BA Techs");
                        JMenu ba_ug_menu = new JMenu(SkillType.ULTRA_GREEN_NM);
                        JMenu ba_g_menu = new JMenu(SkillType.GREEN_NM);
                        JMenu ba_r_menu = new JMenu(SkillType.REGULAR_NM);
                        JMenu ba_v_menu = new JMenu(SkillType.VETERAN_NM);
                        JMenu ba_e_menu = new JMenu(SkillType.ELITE_NM);

                        for (Person tech : gui.getCampaign().getTechs()) {
                            if (tech.getMaintenanceTimeUsing() == 0 && !tech.isEngineer()) {
                                String skillLvl = "Unknown";
                                skillLvl = SkillType.getExperienceLevelName(tech.getExperienceLevel(tech.isTechPrimary() ? false : true));
                                int role = tech.isTechPrimary() ? tech.getPrimaryRole() : tech.getSecondaryRole();

                                // We're a MechTech!
                                if (role == Person.T_MECH_TECH) {
                                    if (skillLvl.equals(SkillType.ULTRA_GREEN_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        m_ug_menu.add(menuItem);
                                        m_ug_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.GREEN_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        m_g_menu.add(menuItem);
                                        m_g_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.REGULAR_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        m_r_menu.add(menuItem);
                                        m_r_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.VETERAN_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        m_v_menu.add(menuItem);
                                        m_v_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.ELITE_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        m_e_menu.add(menuItem);
                                        m_e_menu.setEnabled(true);
                                    }
                                }

                                // We're a AerpTech!
                                if (role == Person.T_AERO_TECH) {
                                    if (skillLvl.equals(SkillType.ULTRA_GREEN_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        a_ug_menu.add(menuItem);
                                        a_ug_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.GREEN_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        a_g_menu.add(menuItem);
                                        a_g_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.REGULAR_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        a_r_menu.add(menuItem);
                                        a_r_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.VETERAN_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        a_v_menu.add(menuItem);
                                        a_v_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.ELITE_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        a_e_menu.add(menuItem);
                                        a_e_menu.setEnabled(true);
                                    }
                                }

                                // We're a Mechanic!
                                if (role == Person.T_MECHANIC) {
                                    if (skillLvl.equals(SkillType.ULTRA_GREEN_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        v_ug_menu.add(menuItem);
                                        v_ug_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.GREEN_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        v_g_menu.add(menuItem);
                                        v_g_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.REGULAR_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        v_r_menu.add(menuItem);
                                        v_r_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.VETERAN_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        v_v_menu.add(menuItem);
                                        v_v_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.ELITE_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        v_e_menu.add(menuItem);
                                        v_e_menu.setEnabled(true);
                                    }
                                }

                                // We're a BATech!
                                if (role == Person.T_BA_TECH) {
                                    if (skillLvl.equals(SkillType.ULTRA_GREEN_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        ba_ug_menu.add(menuItem);
                                        ba_ug_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.GREEN_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        ba_g_menu.add(menuItem);
                                        ba_g_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.REGULAR_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        ba_r_menu.add(menuItem);
                                        ba_r_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.VETERAN_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        ba_v_menu.add(menuItem);
                                        ba_v_menu.setEnabled(true);
                                    }
                                    if (skillLvl.equals(SkillType.ELITE_NM)) {
                                        menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ")");
                                        menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                                        menuItem.addActionListener(this);
                                        menuItem.setEnabled(true);
                                        ba_e_menu.add(menuItem);
                                        ba_e_menu.setEnabled(true);
                                    }
                                }
                            }

                            // Set up 'Mech Menus
                            if (m_ug_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                mech_techs.add(m_ug_menu);
                            }
                            if (m_g_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                mech_techs.add(m_g_menu);
                            }
                            if (m_r_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                mech_techs.add(m_r_menu);
                            }
                            if (m_v_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                mech_techs.add(m_v_menu);
                            }
                            if (m_e_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                mech_techs.add(m_e_menu);
                            }
                            if (mech_techs.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                menu.add(mech_techs);
                            }

                            // Set up Aero Menus
                            if (a_ug_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                aero_techs.add(a_ug_menu);
                            }
                            if (a_g_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                aero_techs.add(a_g_menu);
                            }
                            if (a_r_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                aero_techs.add(a_r_menu);
                            }
                            if (a_v_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                aero_techs.add(a_v_menu);
                            }
                            if (a_e_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                aero_techs.add(a_e_menu);
                            }
                            if (aero_techs.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                menu.add(aero_techs);
                            }

                            // Set up Mechanic Menus
                            if (v_ug_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                aero_techs.add(v_ug_menu);
                            }
                            if (v_g_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                aero_techs.add(v_g_menu);
                            }
                            if (v_r_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                aero_techs.add(v_r_menu);
                            }
                            if (v_v_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                aero_techs.add(v_v_menu);
                            }
                            if (v_e_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                aero_techs.add(v_e_menu);
                            }
                            if (aero_techs.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                menu.add(mechanics);
                            }

                            // Set up BA Menus
                            if (ba_ug_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                ba_techs.add(ba_ug_menu);
                            }
                            if (ba_g_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                ba_techs.add(ba_g_menu);
                            }
                            if (ba_r_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                ba_techs.add(ba_r_menu);
                            }
                            if (ba_v_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                ba_techs.add(ba_v_menu);
                            }
                            if (ba_e_menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                ba_techs.add(ba_e_menu);
                            }
                            if (ba_techs.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                                menu.add(ba_techs);
                            }
                        }

                        if (menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                            menu.setEnabled(true);
                            MenuScroller.createScrollBarsOnMenus(menu);
                            popup.add(menu);
                        }
                    }
                    if (force.getTechID() != null) {
                        menuItem = new JMenuItem("Remove Tech from Force");
                        menuItem.setActionCommand("REMOVE_LANCE_TECH|FORCE|" + force.getTechID() + "|" + forceIds);
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(true);
                        popup.add(menuItem);
                    }
                    menu = new JMenu("Add Unit");
                    menu.setEnabled(false);
                    HashMap<String, JMenu> unitTypeMenus = new HashMap<String, JMenu>();
                    HashMap<String, JMenu> weightClassForUnitType = new HashMap<String, JMenu>();
                    
                    // TODO: Doesn't currently account for Support Vees of any type
                    for (int i = 0; i < UnitType.SIZE; i++)
                    {
                        String unittype = UnitType.getTypeName(i);
                        String displayname = UnitType.getTypeDisplayableName(i);
                        unitTypeMenus.put(unittype, new JMenu(displayname));
                        unitTypeMenus.get(unittype).setName(unittype);
                        unitTypeMenus.get(unittype).setEnabled(false);
                        for (int j = 0; j < EntityWeightClass.getWeightLimitByType(unittype).length; j++) {
                            double tonnage = EntityWeightClass.getWeightLimitByType(unittype)[j];
                            // Skip over the padding 0s
                            if (tonnage == 0) {
                                continue;
                            }
                            
                            int weightClass = EntityWeightClass.getWeightClass(tonnage, unittype);
                            String displayname2 = EntityWeightClass.getClassName(weightClass, unittype, false);
                            String weightClassMenuName = unittype + "_"
                                    + EntityWeightClass.getClassName(weightClass, unittype, false);
                            weightClassForUnitType.put(weightClassMenuName, new JMenu(displayname2));
                            weightClassForUnitType.get(weightClassMenuName).setName(weightClassMenuName);
                            weightClassForUnitType.get(weightClassMenuName).setEnabled(false);
                        }
                    }
                    
                    // Only add units that have commanders
                    // Or Gun Emplacements!
                    // TODO: Or Robotic Systems!
                    JMenu unsorted = new JMenu("Unsorted");
                    for (Unit u : gui.getCampaign().getUnits(true, true)) {
                        String type = UnitType.determineUnitType(u.getEntity());
                        String className = u.getEntity().getWeightClassName();
                        if (null != u.getCommander()) {
                            Person p = u.getCommander();
                            if (p.isActive() && u.getForceId() < 1
                                    && u.isPresent()) {
                                menuItem = new JMenuItem(p.getFullTitle()
                                        + ", " + u.getName());
                                menuItem.setActionCommand("ADD_UNIT|FORCE|"
                                        + u.getId() + "|" + forceIds);
                                menuItem.addActionListener(this);
                                menuItem.setEnabled(u.isAvailable());
                                if (null != weightClassForUnitType.get(type + "_" + className)) {
                                    weightClassForUnitType.get(type + "_" + className).add(menuItem);
                                    weightClassForUnitType.get(type + "_" + className).setEnabled(true);
                                } else {
                                    unsorted.add(menuItem);
                                }
                                unitTypeMenus.get(type).setEnabled(true);
                            }
                        }
                        if (u.getEntity() instanceof GunEmplacement) {
                            if (u.getForceId() < 1 && u.isPresent()) {
                                menuItem = new JMenuItem("AutoTurret, "
                                        + u.getName());
                                menuItem.setActionCommand("ADD_UNIT|FORCE|"
                                        + u.getId() + "|" + forceIds);
                                menuItem.addActionListener(this);
                                menuItem.setEnabled(u.isAvailable());
                                if (null != weightClassForUnitType.get(type + "_" + className)) {
                                    weightClassForUnitType.get(type + "_" + className).add(menuItem);
                                    weightClassForUnitType.get(type + "_" + className).setEnabled(true);
                                } else {
                                    unsorted.add(menuItem);
                                }
                                unitTypeMenus.get(type).setEnabled(true);
                            }
                        }
                    }

                    for (int i = 0; i < UnitType.SIZE; i++)
                    {
                        String unittype = UnitType.getTypeName(i);
                        JMenu tmp = unitTypeMenus.get(UnitType.getTypeName(i));
                        if (tmp.isEnabled()) {
                            for (int j = 0; j < EntityWeightClass.getWeightLimitByType(unittype).length; j++) {
                                double tonnage = EntityWeightClass.getWeightLimitByType(unittype)[j];
                                // Skip over the padding 0s
                                if (tonnage == 0) {
                                    continue;
                                }

                                int weightClass = EntityWeightClass.getWeightClass(tonnage, unittype);
                                JMenu tmp2 = weightClassForUnitType
                                        .get(unittype + "_"
                                                + EntityWeightClass.getClassName(weightClass, unittype, false));
                                if (tmp2.isEnabled()) {
                                    tmp.add(tmp2);
                                }
                            }
                            menu.add(tmp);
                            menu.setEnabled(true);
                        }
                    }

                    if (unsorted.getComponentCount() > 0 || unsorted.getItemCount() > 0) {
                        menu.add(unsorted);
                        menu.setEnabled(true);
                    }

                    if (menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                        MenuScroller.createScrollBarsOnMenus(menu);
                        popup.add(menu);
                    }
                    menuItem = new JMenuItem("Change Force Icon...");
                    menuItem.setActionCommand("CHANGE_ICON|FORCE|empty|"
                            + forceIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    popup.add(menuItem);
                }
                if (StaticChecks.areAllForcesUndeployed(forces)) {
                    menu = new JMenu("Deploy Force");
                    menu.setEnabled(false);
                    JMenu missionMenu;
                    for (Mission m : gui.getCampaign().getMissions()) {
                        if (!m.isActive()) {
                            continue;
                        }
                        missionMenu = new JMenu(m.getName());
                        for (Scenario s : m.getScenarios()) {
                            if (s.isCurrent()) {
                                if (gui.getCampaign().getCampaignOptions()
                                        .getUseAtB()
                                        && s instanceof AtBScenario
                                        && !((AtBScenario) s)
                                        .canDeployForces(forces,
                                                gui.getCampaign())) {
                                    continue;
                                }
                                menuItem = new JMenuItem(s.getName());
                                menuItem.setActionCommand("DEPLOY_FORCE|FORCE|"
                                        + s.getId() + "|" + forceIds);
                                menuItem.addActionListener(this);
                                menuItem.setEnabled(true);
                                missionMenu.add(menuItem);
                                menu.setEnabled(true);
                            }
                        }
                        menu.add(missionMenu);
                    }


                    if (menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                        MenuScroller.createScrollBarsOnMenus(menu);
                        popup.add(menu);
                    }
                }
                if (StaticChecks.areAllForcesDeployed(forces)) {
                    menuItem = new JMenuItem("Undeploy Force");
                    menuItem.setActionCommand("UNDEPLOY_FORCE|FORCE|empty|"
                            + forceIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    popup.add(menuItem);
                }
                menuItem = new JMenuItem("Remove Force");
                menuItem.setActionCommand("REMOVE_FORCE|FORCE|empty|"
                        + forceIds);
                menuItem.addActionListener(this);
                menuItem.setEnabled(!StaticChecks.areAnyForcesDeployed(forces) && !StaticChecks.areAnyUnitsDeployed(unitsInForces));
                popup.add(menuItem);
            } else if (unitsSelected) {
                Unit unit = units.get(0);
                String unitIds = "" + unit.getId().toString();
                for (int i = 1; i < units.size(); i++) {
                    unitIds += "|" + units.get(i).getId().toString();
                }
                JMenu networkMenu = new JMenu("Network");
                JMenu availMenu;
                if (StaticChecks.areAllUnitsC3Slaves(units)) {
                    availMenu = new JMenu("Slave to");
                    for (String[] network : gui.getCampaign()
                            .getAvailableC3MastersForSlaves()) {
                        int nodesFree = Integer.parseInt(network[1]);
                        if (nodesFree >= units.size()) {
                            menuItem = new JMenuItem(network[2] + ": "
                                    + network[1] + " nodes free");
                            menuItem.setActionCommand("ADD_SLAVES|UNIT|"
                                    + network[0] + "|" + unitIds);
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(true);
                            availMenu.add(menuItem);
                        }
                    }
                    if (availMenu.getMenuComponentCount() > 0 || availMenu.getItemCount() > 0) {
                        networkMenu.add(availMenu);
                    }
                }
                if (StaticChecks.areAllUnitsIndependentC3Masters(units)) {
                    menuItem = new JMenuItem("Set as Company Level Master");
                    menuItem.setActionCommand("SET_MM|UNIT|empty|"
                            + unitIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    networkMenu.add(menuItem);
                    availMenu = new JMenu("Slave to");
                    for (String[] network : gui.getCampaign()
                            .getAvailableC3MastersForMasters()) {
                        int nodesFree = Integer.parseInt(network[1]);
                        if (nodesFree >= units.size()) {
                            menuItem = new JMenuItem(network[2] + ": "
                                    + network[1] + " nodes free");
                            menuItem.setActionCommand("ADD_SLAVES|UNIT|"
                                    + network[0] + "|" + unitIds);
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(true);
                            availMenu.add(menuItem);
                        }
                    }
                    if (availMenu.getMenuComponentCount() > 0 || availMenu.getItemCount() > 0) {
                        networkMenu.add(availMenu);
                    }
                }
                if (StaticChecks.areAllUnitsCompanyLevelMasters(units)) {
                    menuItem = new JMenuItem("Set as Independent Master");
                    menuItem.setActionCommand("SET_IND_M|UNIT|empty|"
                            + unitIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    networkMenu.add(menuItem);
                }
                if (StaticChecks.doAllUnitsHaveC3Master(units)) {
                    menuItem = new JMenuItem("Remove from network");
                    menuItem.setActionCommand("REMOVE_C3|UNIT|empty|"
                            + unitIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    networkMenu.add(menuItem);
                }
                if (StaticChecks.doAllUnitsHaveC3i(units)) {

                    if (multipleSelection
                            && StaticChecks.areAllUnitsNotC3iNetworked(units)
                            && units.size() < 7) {
                        menuItem = new JMenuItem("Create new C3i network");
                        menuItem.setActionCommand("C3I|UNIT|empty|"
                                + unitIds);
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(true);
                        networkMenu.add(menuItem);
                    }
                    if (StaticChecks.areAllUnitsNotC3iNetworked(units)) {
                        availMenu = new JMenu("Add to network");
                        for (String[] network : gui.getCampaign()
                                .getAvailableC3iNetworks()) {
                            int nodesFree = Integer.parseInt(network[1]);
                            if (nodesFree >= units.size()) {
                                menuItem = new JMenuItem(network[0] + ": "
                                        + network[1] + " nodes free");
                                menuItem.setActionCommand("ADD_NETWORK|UNIT|"
                                        + network[0] + "|" + unitIds);
                                menuItem.addActionListener(this);
                                menuItem.setEnabled(true);
                                availMenu.add(menuItem);
                            }
                        }
                        if (availMenu.getMenuComponentCount() > 0 || availMenu.getItemCount() > 0) {
                            networkMenu.add(availMenu);
                        }
                    }
                    if (StaticChecks.areAllUnitsC3iNetworked(units)) {
                        menuItem = new JMenuItem("Remove from network");
                        menuItem.setActionCommand("REMOVE_NETWORK|UNIT|empty|"
                                + unitIds);
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(true);
                        networkMenu.add(menuItem);
                        if (StaticChecks.areAllUnitsOnSameC3iNetwork(units)) {
                            menuItem = new JMenuItem("Disband this network");
                            menuItem.setActionCommand("DISBAND_NETWORK|UNIT|empty|"
                                    + unitIds);
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(true);
                            networkMenu.add(menuItem);
                        }
                    }
                }
                if (networkMenu.getMenuComponentCount() > 0 || networkMenu.getItemCount() > 0) {
                    MenuScroller.createScrollBarsOnMenus(networkMenu);
                    popup.add(networkMenu);
                }
                menuItem = new JMenuItem("Remove Unit from TO&E");
                menuItem.setActionCommand("REMOVE_UNIT|UNIT|empty|"
                        + unitIds);
                menuItem.addActionListener(this);
                menuItem.setEnabled(!StaticChecks.areAnyUnitsDeployed(units));
                popup.add(menuItem);
                if (StaticChecks.areAllUnitsAvailable(units)) {
                    menu = new JMenu("Deploy Unit");
                    JMenu missionMenu;
                    for (Mission m : gui.getCampaign().getMissions()) {
                        if (!m.isActive()) {
                            continue;
                        }
                        missionMenu = new JMenu(m.getName());
                        for (Scenario s : m.getScenarios()) {
                            if (s.isCurrent()) {
                                if (gui.getCampaign().getCampaignOptions()
                                        .getUseAtB()
                                        && s instanceof AtBScenario
                                        && !((AtBScenario) s)
                                        .canDeployUnits(units,
                                                gui.getCampaign())) {
                                    continue;
                                }
                                menuItem = new JMenuItem(s.getName());
                                menuItem.setActionCommand("DEPLOY_UNIT|UNIT|"
                                        + s.getId() + "|" + unitIds);
                                menuItem.addActionListener(this);
                                menuItem.setEnabled(true);
                                missionMenu.add(menuItem);
                            }
                        }
                        if (missionMenu.getMenuComponentCount() > 30) {
                            MenuScroller.setScrollerFor(missionMenu, 30);
                        }
                        menu.add(missionMenu);
                    }

                    if (menu.getMenuComponentCount() > 0 || menu.getItemCount() > 0) {
                        MenuScroller.createScrollBarsOnMenus(menu);
                        popup.add(menu);
                    }
                }
                if (StaticChecks.areAllUnitsDeployed(units)) {
                    menuItem = new JMenuItem("Undeploy Unit");
                    menuItem.setActionCommand("UNDEPLOY_UNIT|UNIT|empty|"
                            + unitIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    popup.add(menuItem);
                }
                if (!multipleSelection) {
                    menuItem = new JMenuItem("Go to Unit in Hangar");
                    menuItem.setActionCommand("GOTO_UNIT|UNIT|empty|"
                            + unitIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    popup.add(menuItem);
                    menuItem = new JMenuItem(
                            "Go to Pilot/Commander in Personnel");
                    menuItem.setActionCommand("GOTO_PILOT|UNIT|empty|"
                            + unitIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    popup.add(menuItem);
                }
            }
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}