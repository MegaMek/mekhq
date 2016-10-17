package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputAdapter;

import megamek.common.AmmoType;
import megamek.common.TargetRoll;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions.MassRepairOption;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.WorkTime;
import mekhq.gui.CampaignGUI;
import mekhq.gui.utilities.MenuScroller;
import mekhq.gui.utilities.StaticChecks;

public class ServicedUnitsTableMouseAdapter extends MouseInputAdapter
        implements ActionListener {
    private CampaignGUI gui;

    public ServicedUnitsTableMouseAdapter(CampaignGUI gui) {
        super();
        this.gui = gui;
    }

    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        @SuppressWarnings("unused")
        Unit selectedUnit = gui.getServicedUnitModel()
                .getUnit(gui.getServicedUnitTable()
                        .convertRowIndexToModel(gui.getServicedUnitTable()
                                .getSelectedRow()));
        int[] rows = gui.getServicedUnitTable().getSelectedRows();
        Unit[] units = new Unit[rows.length];
        for (int i = 0; i < rows.length; i++) {
            units[i] = gui.getServicedUnitModel().getUnit(gui.getServicedUnitTable()
                    .convertRowIndexToModel(rows[i]));
        }
        if (command.contains("ASSIGN_TECH")) {
            /*
             * String sel = command.split(":")[1]; int selected =
             * Integer.parseInt(sel); if ((selected > -1) && (selected <
             * gui.getCampaign().getTechTeams().size())) { SupportTeam team =
             * gui.getCampaign().getTechTeams().get(selected); if (null != team)
             * { for (WorkItem task : gui.getCampaign()
             * .getTasksForUnit(selectedUnit.getId())) { if
             * (team.getTargetFor(task).getValue() != TargetRoll.IMPOSSIBLE)
             * { gui.getCampaign().processTask(task, team); } } } }
             * gui.refreshServicedUnitList(); gui.refreshUnitList();
             * gui.refreshTaskList(); gui.refreshAcquireList(); gui.refreshTechsList();
             * gui.refreshReport(); gui.refreshPartsList(); gui.refreshOverview();
             */
        } else if (command.contains("SWAP_AMMO")) {
            String sel = command.split(":")[1];
            int selAmmoId = Integer.parseInt(sel);
            Part part = gui.getCampaign().getPart(selAmmoId);
            if (null == part || !(part instanceof AmmoBin)) {
                return;
            }
            AmmoBin ammo = (AmmoBin) part;
            sel = command.split(":")[2];
            long munition = Long.parseLong(sel);
            ammo.changeMunition(munition);
            gui.refreshTaskList();
            gui.refreshAcquireList();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshOverview();
            gui.filterTasks();
        } else if (command.contains("CHANGE_SITE")) {
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    String sel = command.split(":")[1];
                    int selected = Integer.parseInt(sel);
                    if ((selected > -1) && (selected < Unit.SITE_N)) {
                        unit.setSite(selected);
                    }
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshTaskList();
            gui.refreshAcquireList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("SALVAGE")) {
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    unit.setSalvage(true);
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("REPAIR")) {
            for (Unit unit : units) {
                if (!unit.isDeployed() && unit.isRepairable()) {
                    unit.setSalvage(false);
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshOverview();
        } else if (command.contains("MASS_REPAIR_SALVAGE")) {
            boolean isSalvage = Integer.parseInt(command.split(":")[1]) == 1;
            int mroType = Integer.parseInt(command.split(":")[2]);

            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                	performMassRepairOrSalvage(unit, mroType, isSalvage);
                }
            }
            
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("REMOVE")) {
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    if (0 == JOptionPane.showConfirmDialog(
                            null,
                            "Do you really want to remove "
                                    + unit.getName() + "?", "Remove Unit?",
                            JOptionPane.YES_NO_OPTION)) {
                        gui.getCampaign().removeUnit(unit.getId());
                    }
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("UNDEPLOY")) {
            for (Unit unit : units) {
                if (unit.isDeployed()) {
                    gui.undeployUnit(unit);
                }
            }
            gui.refreshPersonnelList();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshOrganization();
            gui.refreshTaskList();
            gui.refreshUnitView();
            gui.refreshPartsList();
            gui.refreshAcquireList();
            gui.refreshReport();
            gui.refreshPatientList();
            gui.refreshScenarioList();
            gui.refreshOverview();
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
            if (gui.getServicedUnitTable().getSelectedRowCount() == 0) {
                return;
            }
            int[] rows = gui.getServicedUnitTable().getSelectedRows();
            int row = gui.getServicedUnitTable().getSelectedRow();
            boolean oneSelected = gui.getServicedUnitTable().getSelectedRowCount() == 1;
            Unit unit = gui.getServicedUnitModel().getUnit(gui.getServicedUnitTable()
                    .convertRowIndexToModel(row));
            Unit[] units = new Unit[rows.length];
            for (int i = 0; i < rows.length; i++) {
                units[i] = gui.getUnitModel().getUnit(gui.getUnitTable()
                        .convertRowIndexToModel(rows[i]));
            }
            JMenuItem menuItem = null;
            JMenu menu = null;
            JCheckBoxMenuItem cbMenuItem = null;
            // **lets fill the pop up menu**//
            // change the location
            menu = new JMenu("Change site");
            int i = 0;
            for (i = 0; i < Unit.SITE_N; i++) {
                cbMenuItem = new JCheckBoxMenuItem(Unit.getSiteName(i));
                if (StaticChecks.areAllSameSite(units) && unit.getSite() == i) {
                    cbMenuItem.setSelected(true);
                } else {
                    cbMenuItem.setActionCommand("CHANGE_SITE:" + i);
                    cbMenuItem.addActionListener(this);
                }
                menu.add(cbMenuItem);
            }
            menu.setEnabled(unit.isAvailable());
            popup.add(menu);
            // assign all tasks to a certain tech
            /*
             * menu = new JMenu("Assign all tasks"); i = 0; for (Person tech
             * : gui.getCampaign().getTechs()) { menuItem = new
             * JMenuItem(tech.getFullName());
             * menuItem.setActionCommand("ASSIGN_TECH:" + i);
             * menuItem.addActionListener(this);
             * menuItem.setEnabled(tech.getMinutesLeft() > 0);
             * menu.add(menuItem); i++; }
             * menu.setEnabled(unit.isAvailable()); if (menu.getItemCount()
             * > 20) { MenuScroller.setScrollerFor(menu, 20); }
             * popup.add(menu);
             */
            // swap ammo
            if (oneSelected) {
                menu = new JMenu("Swap ammo");
                JMenu ammoMenu = null;
                for (AmmoBin ammo : unit.getWorkingAmmoBins()) {
                    ammoMenu = new JMenu(ammo.getType().getDesc());
                    AmmoType curType = (AmmoType) ammo.getType();
                    for (AmmoType atype : Utilities.getMunitionsFor(unit
                            .getEntity(), curType, gui.getCampaign()
                            .getCampaignOptions().getTechLevel())) {
                        cbMenuItem = new JCheckBoxMenuItem(atype.getDesc());
                        if (atype.equals(curType)
                        		&& atype.getMunitionType() == curType.getMunitionType()) {
                            cbMenuItem.setSelected(true);
                        } else {
                            cbMenuItem.setActionCommand("SWAP_AMMO:"
                                    + ammo.getId() + ":"
                                    + atype.getMunitionType());
                            cbMenuItem.addActionListener(this);
                        }
                        ammoMenu.add(cbMenuItem);
                        i++;
                    }
                    if (menu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(menu, 20);
                    }
                    menu.add(ammoMenu);
                }
                menu.setEnabled(unit.isAvailable());
                popup.add(menu);
                // Salvage / Repair
                if (unit.isSalvage()) {
                    menuItem = new JMenuItem("Repair");
                    menuItem.setActionCommand("REPAIR");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(unit.isAvailable()
                            && unit.isRepairable());
                    popup.add(menuItem);
                } else {
                    menuItem = new JMenuItem("Salvage");
                    menuItem.setActionCommand("SALVAGE");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(unit.isAvailable());
                    popup.add(menuItem);
                }
                
                if (!unit.isSelfCrewed() && unit.isAvailable()) {
	                //Mass Repair
	                if (unit.isSalvage()) {
	                    menu = new JMenu("Mass Salvage");
	                    
                    	JCheckBoxMenuItem mroMenuItem = new JCheckBoxMenuItem("Use configuration");
                    	mroMenuItem.setActionCommand("MASS_REPAIR_SALVAGE:1:-1");
                    	mroMenuItem.addActionListener(this);
                    	menu.add(mroMenuItem);
                    	
	                    for (int x = 0; x <= MassRepairOption.OPTION_TYPE.MAX; x++) {
	                    	mroMenuItem = new JCheckBoxMenuItem("Only " + MassRepairOption.getShortName(x));
	                    	mroMenuItem.setActionCommand("MASS_REPAIR_SALVAGE:1:" + x);
	                    	mroMenuItem.addActionListener(this);
	                    	menu.add(mroMenuItem);
	                    }
	                    
	                    popup.add(menu);
	                } else if (unit.isRepairable()) {
	                    menu = new JMenu("Mass Repair");
	                    
                    	JCheckBoxMenuItem mroMenuItem = new JCheckBoxMenuItem("Use configuration");
                    	mroMenuItem.setActionCommand("MASS_REPAIR_SALVAGE:0:-1");
                    	mroMenuItem.addActionListener(this);
                    	menu.add(mroMenuItem);
                    	
	                    for (int x = 0; x <= MassRepairOption.OPTION_TYPE.MAX; x++) {
	                    	mroMenuItem = new JCheckBoxMenuItem("Only " + MassRepairOption.getShortName(x));
	                    	mroMenuItem.setActionCommand("MASS_REPAIR_SALVAGE:0:" + x);
	                    	mroMenuItem.addActionListener(this);
	                    	menu.add(mroMenuItem);
	                    }
	                    
	                    popup.add(menu);
	                }
                }
                
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
    
    private void performMassRepairOrSalvage(Unit unit, int mroType, boolean isSalvage) {
    	if ((unit == null) || (unit.getEntity() == null)) {
    		return;
    	}

    	Campaign c = gui.getCampaign();
    	String actionDescriptor = isSalvage ? "salvage" : "repair";
    	
    	c.addReport(String.format("Beginning mass %s on %s.", actionDescriptor, unit.getName()));
    	
    	ArrayList<Person> techs = c.getTechs(true);
    	
    	if (techs.isEmpty()) {
    		c.addReport(String.format("No available techs to %s parts %s %s.", actionDescriptor, isSalvage ? "from" : "on", unit.getName()));
    	} else {
    		//Filter our tech list to only our techs that can work on this unit
            for (int i = techs.size() - 1; i >= 0; i--) {
            	Person tech = techs.get(i);
            	
                if (!tech.canTech(unit.getEntity())) {
                	techs.remove(i);
                }
            }
            
            //Possibly call this multiple times. 
            //Sometimes some actions are first dependent upon others being finished also, 
            //failed actions can be performed again by a tech with a higher skill.
            int totalActionsPerformed = 0;
            int actionsPerformed = 1;
            
            while (actionsPerformed > 0) {
            	actionsPerformed = performMassTechAction(unit, techs, mroType, isSalvage);
            	totalActionsPerformed += actionsPerformed;
            }
            
            c.addReport(String.format("Mass %s complete on %s. %d total actions performed.", actionDescriptor, unit.getName(), totalActionsPerformed));
    	}
    	
        gui.refreshReport();
    }
    
    private int performMassTechAction(Unit unit, List<Person> techs, int mroType, boolean salvaging) {
    	Campaign c = gui.getCampaign();
    	int totalActionsPerformed = 0;
    	String actionDescriptor = salvaging ? "salvage" : "repair";
    	
    	if (techs.isEmpty()) {
    		c.addReport(String.format("Unable to %s any more parts from %s because there are no available techs.", actionDescriptor, unit.getName()));
    		return totalActionsPerformed;
    	}
    	
    	MassRepairOption[] repairOptions = c.getCampaignOptions().getMassRepairOptions();
    	
        List<Part> parts = gui.getCampaign().getPartsNeedingServiceFor(unit.getId());
        
        //Filter our parts list to only those that aren't being worked on or those that meet our criteria as defined in the campaign configurations
        for (int i = parts.size() - 1; i >= 0; i--) {
        	Part part = parts.get(i);
        	
        	if (part.isBeingWorkedOn()) {
        		parts.remove(i);
        	} else {
	        	if (mroType != -1) {
	            	if (mroType != MassRepairOption.findCorrectOptionType(part)) {
	            		parts.remove(i);        		
	            	}
	        	} else if (!repairOptions[MassRepairOption.findCorrectOptionType(part)].isActive()) {
	        		parts.remove(i);
	        	}
        	}
        }
        
        if (parts.isEmpty()) {
    		c.addReport(String.format("Unable to %s any more parts from %s because there are no valid parts left to %s.", actionDescriptor, unit.getName(), actionDescriptor));
    		return totalActionsPerformed;
        }
        
        for (Part part : parts) {
        	if (techs.isEmpty()) {
        		c.addReport(String.format("Unable to %s any more parts from %s because there are no available techs.", actionDescriptor, unit.getName()));
        		continue;
        	}
        	
        	int modePenalty = part.getMode().expReduction;
        	
        	//Search the list of techs each time for a variety of checks. We'll create a temporary truncated list of techs
        	List<Person> validTechs = new ArrayList<Person>();
        	Map<String, WorkTime> techToWorktimeMap = new HashMap<String, WorkTime>();
        	
            for (int i = techs.size() - 1; i >= 0; i--) {
            	//Reset our WorkTime back to normal so that we can adjust as necessary
            	WorkTime selectedWorktime = WorkTime.NORMAL;
            	part.setMode(WorkTime.of(selectedWorktime.id));
            	
            	Person tech = techs.get(i);
            	Skill skill = tech.getSkillForWorkingOn(part);
            	MassRepairOption mro = null;
            	
            	if (mroType == -1) {
            		mro = repairOptions[MassRepairOption.findCorrectOptionType(part)];
            	} else {
            		mro = repairOptions[mroType];
            	}
            	
            	if (mro.getSkillMin() > skill.getExperienceLevel()) {
            		continue;
            	}
            	
            	if (part.getSkillMin() > (skill.getExperienceLevel() - modePenalty)) {
            		continue;
            	}
            	
            	if (tech.getMinutesLeft() <= 0) {
            		continue;
            	}

            	//Check if we can actually even repair this part
            	TargetRoll targetRoll = c.getTargetFor(part, tech);
            	
	        	if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE) || (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL) || (targetRoll.getValue() == TargetRoll.CHECK_FALSE))  {
	        		continue;
	        	}
	        	
	        	//Check if we need to increase the time to meet the min BTH
	        	if (targetRoll.getValue() > mro.getBthMin()) {
	        		if (!c.getCampaignOptions().massRepairUseExtraTime()) {
	        			continue;
	        		}
	        		
	        		WorkTime newWorkTime = calculateNewMassRepairWorktime(part, tech, mro, c, true);
	        		
	        		if (null == newWorkTime) {
	        			continue;
	        		}
	        		
	        		selectedWorktime = newWorkTime;
	        	} else if (targetRoll.getValue() < mro.getBthMax()) {
	        		//Or decrease the time to meet the max BTH
	        		if (c.getCampaignOptions().massRepairUseRushJob()) {
		        		WorkTime newWorkTime = calculateNewMassRepairWorktime(part, tech, mro, c, false);
		        		
		        		//This should never happen, but...
		        		if (null != newWorkTime) {
		        			selectedWorktime = newWorkTime;
		        		}
	        		}
	        	}
	        	
	        	
            	if ((tech.getMinutesLeft() < part.getActualTime()) && !c.getCampaignOptions().massRepairAllowCarryover()) {
            		continue;
            	}
            
            	validTechs.add(tech);
            	techToWorktimeMap.put(tech.getId().toString(), selectedWorktime);
            	
            	part.setMode(WorkTime.of(WorkTime.NORMAL.id));
            }
            
        	if (!validTechs.isEmpty()) {
	            /*
        		 * Sort the valid techs by applicable skill. 
        		 * Let's start with the least experienced and work our way up until we find someone who can perform the work. 
        		 * If we have two techs with the same skill, put the one with the lesser XP in the front.
        		 */
	            Collections.sort(validTechs, new Comparator<Person>() {
					@Override
					public int compare(Person tech1, Person tech2) {
						Skill skill1 = tech1.getSkillForWorkingOn(part);
						Skill skill2 = tech2.getSkillForWorkingOn(part);
						
						if (skill1.getExperienceLevel() == skill2.getExperienceLevel()) {
							if (tech1.getXp() == tech2.getXp()) {
								return 0;
							}
							
							return tech1.getXp() < tech2.getXp() ? -1 : 1;
						}
						
						return skill1.getExperienceLevel() < skill2.getExperienceLevel() ? -1 : 1;
					}
				});
        		
        		Person tech = validTechs.get(0);
	        	WorkTime wt = techToWorktimeMap.get(tech.getId().toString());
	        	
	        	part.setMode(wt);
        		
	        	c.fixPart(part, tech);
	        	
	        	totalActionsPerformed++;
	        	
	        	//If this tech has no time left, filter them out so we don't spend cycles on them in the future
	        	if (tech.getMinutesLeft() <= 0) {
	        		techs.remove(tech);
	        	}
            	
            	Thread.yield();
        	}
        }        
        
        return totalActionsPerformed;
    }

	private WorkTime calculateNewMassRepairWorktime(Part part, Person tech, MassRepairOption mro, Campaign c, boolean increaseTime) {
		WorkTime newWorkTime = part.getMode();
		WorkTime previousNewWorkTime = newWorkTime;
    	TargetRoll targetRoll = c.getTargetFor(part, tech);
    	    	
		while (null != newWorkTime) {
			previousNewWorkTime = newWorkTime;
    		newWorkTime = newWorkTime.moveTimeToNextLevel(increaseTime);
    		
    		if (null == newWorkTime) {
    			if (!increaseTime) {
    				return previousNewWorkTime;
    			} else {
	    			return null;
    			}
    		}
    		
    		part.setMode(newWorkTime);
    		
        	targetRoll = c.getTargetFor(part, tech);
        	
        	if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE) || (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL) || (targetRoll.getValue() == TargetRoll.CHECK_FALSE))  {
        		continue;
        	}
        	
        	if (increaseTime) {
        		if (targetRoll.getValue() <= mro.getBthMin()) {
            		return newWorkTime;
        		}
         	} else {
         		if (targetRoll.getValue() > mro.getBthMax()) {
         			return previousNewWorkTime;
         		}
         		
         		return newWorkTime;
         	}
		}
		
		return null;
	}
}