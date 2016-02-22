package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
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

import megamek.common.GunEmplacement;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.PortraitChoiceDialog;
import mekhq.gui.dialog.TextAreaDialog;
import mekhq.gui.utilities.MenuScroller;
import mekhq.gui.utilities.StaticChecks;

public class OrgTreeMouseAdapter extends MouseInputAdapter implements
        ActionListener {
    private CampaignGUI gui;

    public OrgTreeMouseAdapter(CampaignGUI gui) {
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
                    gui.getCampaign().addForce(new Force(name), singleForce);
                    gui.refreshOrganization();
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
                		for (UUID uuid : singleForce.getAllUnits()) {
                			Unit u = gui.getCampaign().getUnit(uuid);
                			if (u != null) {
                			    if (null != u.getTech()) {
                			        Person oldTech = u.getTech();
                			        oldTech.removeTechUnitId(u.getId());
                			    }
                				u.setTech(tech.getId());
                				tech.addTechUnitID(u.getId());
                			}
                		}
                	}
                	gui.refreshOrganization();
                	gui.refreshScenarioList();
                	gui.refreshPersonnelList();
                	gui.refreshUnitList();
                	gui.refreshServicedUnitList();
                }
          	}
        }
        if (command.contains("ADD_UNIT")) {
            if (null != singleForce) {
                Unit u = gui.getCampaign().getUnit(UUID.fromString(target));
                if (null != u) {
                    gui.getCampaign().addUnitToForce(u, singleForce.getId());
                    gui.refreshOrganization();
                    gui.refreshScenarioList();
                    gui.refreshPersonnelList();
                    gui.refreshUnitList();
                    gui.refreshServicedUnitList();
                    gui.refreshOverview();
                }
            }
        } else if (command.contains("UNDEPLOY_FORCE")) {
            for (Force force : forces) {
                gui.undeployForce(force);
            }
            gui.refreshOrganization();
            gui.refreshPersonnelList();
            gui.refreshUnitList();
            gui.refreshServicedUnitList();
            gui.refreshScenarioList();
            gui.refreshOverview();
        } else if (command.contains("DEPLOY_FORCE")) {
            int sid = Integer.parseInt(target);
            Scenario scenario = gui.getCampaign().getScenario(sid);
            for (Force force : forces) {
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
            }
            gui.refreshScenarioList();
            gui.refreshOrganization();
            gui.refreshPersonnelList();
            gui.refreshUnitList();
            gui.refreshServicedUnitList();
            gui.refreshOverview();
        } else if (command.contains("CHANGE_ICON")) {
            if (null != singleForce) {
                PortraitChoiceDialog pcd = new PortraitChoiceDialog(
                        gui.getFrame(), true, singleForce.getIconCategory(),
                        singleForce.getIconFileName(), gui.getIconPackage()
                                .getForceIcons(), true);
                pcd.setVisible(true);
                singleForce.setIconCategory(pcd.getCategory());
                singleForce.setIconFileName(pcd.getFileName());
                gui.refreshOrganization();
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
                gui.refreshOrganization();
            }
        } else if (command.contains("CHANGE_DESC")) {
            if (null != singleForce) {
                TextAreaDialog tad = new TextAreaDialog(gui.getFrame(), true,
                        "Edit Force Description",
                        singleForce.getDescription());
                tad.setVisible(true);
                if (tad.wasChanged()) {
                    singleForce.setDescription(tad.getText());
                    gui.refreshOrganization();
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
            gui.refreshOrganization();
            gui.refreshPersonnelList();
            gui.refreshScenarioList();
            gui.refreshUnitList();
            gui.refreshOverview();
        } else if (command.contains("REMOVE_LANCE_TECH")) {
           	if (singleForce.getTechID() != null) {
    			Person oldTech = gui.getCampaign().getPerson(singleForce.getTechID());
    			oldTech.clearTechUnitIDs();
    			oldTech.addLogEntry(gui.getCampaign().getDate(), "Removed from " + singleForce.getName());
    			if (singleForce.getAllUnits() !=null) {
           			for (UUID uuid : singleForce.getAllUnits()) {
           				Unit u = gui.getCampaign().getUnit(uuid);
           				if (u != null) {
           					u.setTech((UUID)null);
           				}
           			}
           		}
    			singleForce.setTechID(null);

    			gui.refreshOrganization();
    			gui.refreshPersonnelList();
                gui.refreshScenarioList();
                gui.refreshUnitList();
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
                        }
                    }
                }
            }
            gui.refreshOrganization();
            gui.refreshPersonnelList();
            gui.refreshScenarioList();
            gui.refreshUnitList();
            gui.refreshOverview();
        } else if (command.contains("UNDEPLOY_UNIT")) {
            for (Unit unit : units) {
                gui.undeployUnit(unit);
            }
            gui.refreshScenarioList();
            gui.refreshOrganization();
            gui.refreshPersonnelList();
            gui.refreshUnitList();
            gui.refreshServicedUnitList();
            gui.refreshOverview();
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

                }
            }
            gui.refreshScenarioList();
            gui.refreshOrganization();
            gui.refreshPersonnelList();
            gui.refreshUnitList();
            gui.refreshServicedUnitList();
            gui.refreshOverview();
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
            gui.refreshOrganization();
        } else if (command.contains("REMOVE_NETWORK")) {
            gui.getCampaign().removeUnitsFromNetwork(units);
            gui.refreshOrganization();
        } else if (command.contains("DISBAND_NETWORK")) {
            if (null != singleUnit) {
                gui.getCampaign().disbandNetworkOf(singleUnit);
            }
            gui.refreshOrganization();
        } else if (command.contains("ADD_NETWORK")) {
            gui.getCampaign().addUnitsToNetwork(units, target);
            gui.refreshOrganization();
        } else if (command.contains("ADD_SLAVES")) {
            for (Unit u : units) {
                u.getEntity().setC3MasterIsUUIDAsString(target);
            }
            gui.getCampaign().refreshNetworks();
            gui.refreshOrganization();
        } else if (command.contains("SET_MM")) {
            for (Unit u : units) {
                gui.getCampaign().removeUnitsFromC3Master(u);
                u.getEntity().setC3MasterIsUUIDAsString(
                        u.getEntity().getC3UUIDAsString());
            }
            gui.getCampaign().refreshNetworks();
            gui.refreshOrganization();
        } else if (command.contains("SET_IND_M")) {
            for (Unit u : units) {
                u.getEntity().setC3MasterIsUUIDAsString(null);
                u.getEntity().setC3Master(null, true);
                gui.getCampaign().removeUnitsFromC3Master(u);
            }
            gui.getCampaign().refreshNetworks();
            gui.refreshOrganization();
        }
        if (command.contains("REMOVE_C3")) {
            for (Unit u : units) {
                u.getEntity().setC3MasterIsUUIDAsString(null);
                u.getEntity().setC3Master(null, true);
            }
            gui.getCampaign().refreshNetworks();
            gui.refreshOrganization();
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
            // but
            // not both - if both are selected then default to
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
                        for (Person tech : gui.getCampaign().getTechs()) {
                        	if (tech.getMaintenanceTimeUsing() == 0 && !tech.isEngineer()) {
                        		String skillLvl = "Unknown";
                        		skillLvl = SkillType.getExperienceLevelName(tech.getExperienceLevel(false));
                        		menuItem = new JMenuItem(tech.getFullTitle() + " (" + skillLvl + ", " + tech.getRoleDesc() + ")");
                        		menuItem.setActionCommand("ADD_LANCE_TECH|FORCE|" + tech.getId() + "|" + forceIds);
                        		menuItem.addActionListener(this);
                        		menuItem.setEnabled(true);
                        		menu.add(menuItem);
                        		menu.setEnabled(true);
                        	}
                        }
                        if (menu.getItemCount() > 0) {
                        	popup.add(menu);
                        	if (menu.getItemCount() > 20) {
                        		MenuScroller.setScrollerFor(menu, 20);
                        	}
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
                    // only add units that have commanders
                    // Or Gun Emplacements!
                    for (Unit u : gui.getCampaign().getUnits()) {
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
                                menu.add(menuItem);
                                menu.setEnabled(true);
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
                                menu.add(menuItem);
                                menu.setEnabled(true);
                            }
                        }
                    }
                    if (menu.getItemCount() > 30) {
                        MenuScroller.setScrollerFor(menu, 30);
                    }
                    popup.add(menu);
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
                    popup.add(menu);
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
                    if (availMenu.getItemCount() > 0) {
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
                    if (availMenu.getItemCount() > 0) {
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
                        if (availMenu.getItemCount() > 0) {
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
                if (networkMenu.getItemCount() > 0) {
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
                        if (missionMenu.getItemCount() > 30) {
                            MenuScroller.setScrollerFor(missionMenu, 30);
                        }
                        menu.add(missionMenu);
                    }
                    if (menu.getItemCount() > 30) {
                        MenuScroller.setScrollerFor(menu, 30);
                    }
                    popup.add(menu);
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