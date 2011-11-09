/*
 * Refit.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.parts;

import java.util.ArrayList;
import java.util.HashMap;

import mekhq.campaign.Unit;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.Modes;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.TargetRoll;
import megamek.common.WeaponType;

/**
 * This object tracks the refit of a given unit into a new unit.
 * It has fields for the current entity and the new entity and it
 * uses these to calculate various characteristics of the refit.
 * 
 * It can then also be used to track the actual refit process, by
 * attaching it to a Unit.
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Refit implements IPartWork {
	
	public static final int NO_CHANGE = 0;
	public static final int CLASS_A = 1;
	public static final int CLASS_B = 2;
	public static final int CLASS_C = 3;
	public static final int CLASS_D = 4;
	public static final int CLASS_E = 5;
	public static final int CLASS_F = 6;
	
	private Unit oldUnit;
	private Unit newUnit;
	
	private int refitClass;
	private int time;
	private int timeSpent;
	private long cost;
	private boolean failedCheck;
	
	private ArrayList<Part> oldUnitParts;
	private ArrayList<Part> newUnitParts;
	private ArrayList<Part> newEquipment;
		
	private HashMap<Integer, Integer> equipNumTracker;
	
	private int assignedTechId;
	
	public Refit(Unit oUnit, Entity newEn) {
		oldUnit = oUnit;
		newUnit = new Unit(newEn, oldUnit.campaign);
		newUnit.initializeParts(false);
		oldUnitParts = new ArrayList<Part>();
		newUnitParts = new ArrayList<Part>();
		newEquipment = new ArrayList<Part>();
		equipNumTracker = new HashMap<Integer, Integer>();
		calculate();
		assignedTechId = -1;
		failedCheck = false;
		timeSpent = 0;
	}
	
	public static String getRefitClassName(int refitClass) {
		switch(refitClass) {
		case NO_CHANGE:
			return "No Change";
		case CLASS_A:
			return "Class A (Field)";
		case CLASS_B:
			return "Class B (Field)";
		case CLASS_C:
			return "Class C (Maintenance)";
		case CLASS_D:
			return "Class D (Maintenance)";
		case CLASS_E:
			return "Class E (Factory)";
		case CLASS_F:
			return "Class F (Factory)";
		default:	
			return "Unknown";
				
		}
	}
	
	public String getRefitClassName() {
		return getRefitClassName(refitClass);
	}
	
	public long getCost() {
		return cost;
	}
	
	public int getTime() {
		return time;
	}
	
	public void calculate() {
		refitClass = NO_CHANGE;
		time = 0;
		
		
		//Step 1: put all of the parts from the current unit into a new arraylist so they can
		//be removed when we find a match.
		for(Part p : oldUnit.getParts()) {
			oldUnitParts.add(p);
		}
		
		//Step 2: loop through the parts arraylist in the newUnit and attempt to find the 
		//corresponding part of missing part in the parts arraylist we just created. Depending on 
		//what we find, we may have:
		//a) An exact copy in the same location - we move the part from the oldunit parts to the 
		//newunit parts. Nothing needs to be changed in terms of refit class, time, or anything.
		//b) An exact copy in a different location - move this part to the newunit part list, but 
		//change its location id. Change refit class to C and add time for removing and reinstalling
		//part.
		//c) We dont find the part in the oldunit part list.  That means this is a new part.  Add
		//this to the newequipment arraylist from step 3.  Don't change anything in terms of refit 
		//stats yet, that will happen later.
		//TODO: need to keep track of equipment numbers
		for(Part part : newUnit.getParts()) {
			boolean partFound = false;
			Part movedPart = null;
			int moveIndex = 0;
			int i = -1;
			for(Part oPart : oldUnitParts) {
				i++;
				if((oPart instanceof MissingPart && ((MissingPart)oPart).isAcceptableReplacement(part)) 
						|| oPart.isSamePartTypeAndStatus(part)) {
					//need a special check for location and armor amount for armor
					if(oPart instanceof Armor 
							&& (((Armor)oPart).getLocation() != ((Armor)part).getLocation()
									|| ((Armor)oPart).getTotalAmount() != ((Armor)part).getTotalAmount())) {
						continue;
					}
					if(part instanceof EquipmentPart) {
						//check the location to see if this moved. If so, then don't break, but 
						//save this in case we fail to find equipment in the same location.
						int loc = ((EquipmentPart)part).getLocation();
						if((oPart instanceof EquipmentPart && ((EquipmentPart)oPart).getLocation() != loc)
								|| (oPart instanceof MissingEquipmentPart && ((MissingEquipmentPart)oPart).getLocation() != loc)) {
							movedPart = oPart;
							moveIndex = i;
							continue;
						} else {
							equipNumTracker.put(oPart.getId(), ((EquipmentPart)part).getEquipmentNum());
						}
					}
					newUnitParts.add(oPart);
					partFound = true;
					break;
				}
			}
			if(partFound) {
				oldUnitParts.remove(i);
			} else if(null != movedPart) {
				newUnitParts.add(movedPart);
				oldUnitParts.remove(moveIndex);
				updateRefitClass(CLASS_C);
				if(movedPart instanceof EquipmentPart) {
					equipNumTracker.put(movedPart.getId(), ((EquipmentPart)part).getEquipmentNum());
					boolean isSalvaging = movedPart.isSalvaging();
					movedPart.setSalvaging(true);
					movedPart.updateConditionFromEntity();
					time += movedPart.getBaseTime();
					movedPart.setSalvaging(isSalvaging);
				}
			} else {
				//its a new part
				newEquipment.add(part);
			}		
		}
		
		//Step 3: loop through the newequipment list and determine what class of refit it entails,
		//add time for both installing this part.
		//This may involve taking a look at remaining oldunit parts to determine whether this item
		//replaces another item of the same or fewer crits. Also add cost for new equipment.
		//TODO: check the parts store for new equipment
		
		//first put oldUnitParts in a new arraylist so they can be removed as we find them
		ArrayList<Part> tempParts = new ArrayList<Part>();
		tempParts.addAll(oldUnitParts);
		
		for(Part nPart : newEquipment) {
			cost += nPart.getCurrentValue();
			if(nPart instanceof Armor) {
				time += nPart.getBaseTime();
			} else {
				time += nPart.getMissingPart().getBaseTime();
			}
			if(nPart instanceof EnginePart) {
				if(oldUnit.getEntity().getEngine().getRating() != newUnit.getEntity().getEngine().getRating()) {
					updateRefitClass(CLASS_D);
				}
				if(newUnit.getEntity().getEngine().getEngineType() != oldUnit.getEntity().getEngine().getEngineType()) {
					updateRefitClass(CLASS_F);
				}
			} else if(nPart instanceof MekGyro) {
				updateRefitClass(CLASS_F);
			} else if(nPart instanceof MekLocation) {
				if(((Mech)newUnit.getEntity()).hasTSM() != ((Mech)oldUnit.getEntity()).hasTSM()) {
					updateRefitClass(CLASS_E);
				} else {
					updateRefitClass(CLASS_F);
				}
			} else if(nPart instanceof Armor) {
				updateRefitClass(CLASS_C);
			} else { 
				//determine whether this is A, B, or C
				if(nPart instanceof EquipmentPart) {
					int loc = ((EquipmentPart)nPart).getLocation();
					EquipmentType type = ((EquipmentPart)nPart).getType();
					int crits = type.getCriticals(newUnit.getEntity());
					int i = -1;
					boolean matchFound = false;
					int matchIndex = -1;
					int rClass = CLASS_D;
					for(Part oPart : tempParts) {
						i++;
						int oLoc = -1;
						int oCrits = -1;
						EquipmentType oType = null;
						if(oPart instanceof EquipmentPart) {
							oLoc = ((EquipmentPart)oPart).getLocation();
							oType = ((EquipmentPart)nPart).getType();
							oCrits = oType.getCriticals(oldUnit.getEntity());
						}
						if(oPart instanceof MissingEquipmentPart) {
							oLoc = ((MissingEquipmentPart)oPart).getLocation();
							oType = ((MissingEquipmentPart)nPart).getType();
							oCrits = oType.getCriticals(oldUnit.getEntity());
						}
						if(loc != oLoc) {
							continue;
						}
						if(crits == oCrits 
								&& (type.hasFlag(WeaponType.F_LASER) && oType.hasFlag(WeaponType.F_LASER))
								&& (type.hasFlag(WeaponType.F_MISSILE) && oType.hasFlag(WeaponType.F_MISSILE))
								&& (type.hasFlag(WeaponType.F_BALLISTIC) && oType.hasFlag(WeaponType.F_BALLISTIC))
								&& (type.hasFlag(WeaponType.F_ARTILLERY) && oType.hasFlag(WeaponType.F_ARTILLERY))) {
							rClass = CLASS_A;
							matchFound = true;
							matchIndex = i;
							break;
						} else if (crits <= oCrits) {
							rClass = CLASS_B;
							matchFound = true;
							matchIndex = i;
							//don't break because we may find something better
						} else {
							rClass = CLASS_C;
							matchFound = true;
							matchIndex = i;
							//don't break because we may find something better
						}
					}
					updateRefitClass(rClass);
					if(matchFound) {
						tempParts.remove(matchIndex);
					}
				}
			}
		}
		
		//Step 4: loop through remaining equipment on oldunit parts and add time for removing.
		for(Part oPart : oldUnitParts) {
			if(oPart instanceof MissingPart) {
				continue;
			}
			boolean isSalvaging = oPart.isSalvaging();
			oPart.setSalvaging(true);
			oPart.updateConditionFromEntity();
			time += oPart.getBaseTime();
			oPart.setSalvaging(isSalvaging);
			oPart.updateConditionFromEntity();
		}
		
		//TODO: heat sink type change - Class D
		//TODO: install CASE - Class E
		//TODO: double time for custom jobs
		
		//multiply time by refit class
		time *= getTimeMultiplier();
	}
	
	public void begin() {
		oldUnit.setRefit(this);
		orderParts();
	}
	
	private void orderParts() {
		//TODO: should have to make an acquisition roll
		for(Part part : newEquipment) {
			oldUnit.campaign.buyPart(part, part.getCurrentValue());
		}
	}
	
	private void updateRefitClass(int rClass) {
		if(rClass > refitClass) {
			refitClass = rClass;
		}
	}
	
	public void cancel() {
		oldUnit.setRefit(null);
	}
	
	private void complete() {
		oldUnit.setEntity(newUnit.getEntity());
		//add old parts to the warehouse
		for(Part part : oldUnitParts) {
			part.setUnit(null);
		}
		//change equipment number of equipment part new unit parts
		for(Part part : newUnitParts) {
			if(part instanceof EquipmentPart) {
				((EquipmentPart)part).setEquipmentNum(equipNumTracker.get(part.getId()));
			}
			else if(part instanceof EquipmentPart) {
				((MissingEquipmentPart)part).setEquipmentNum(equipNumTracker.get(part.getId()));
			}
		}
		//add new parts to the unit
		for(Part part : newEquipment) {
			part.setUnit(oldUnit);
			newUnitParts.add(part);
			oldUnit.campaign.addPart(part);
		}
		oldUnit.setParts(newUnitParts);
		oldUnit.runDiagnostic();
		oldUnit.setRefit(null);
		
		//TODO: I should save a copy of this unit to the mechfiles and I should
		//also decide on a way to save this to the XML save file, so that MekHQ save files
		//are fully transportable.
	}
	
	private int getTimeMultiplier() {
		switch(refitClass) {
		case NO_CHANGE:
			return 0;
		case CLASS_A:
			return 1;
		case CLASS_B:
			return 1;
		case CLASS_C:
			return 2;
		case CLASS_D:
			return 3;
		case CLASS_E:
			return 4;
		case CLASS_F:
			return 5;
		default:	
			return 1;	
		}
	}
	
	public void reduceTime(int minutes) {
		time -= minutes;
	}
	
	public Entity getOriginalEntity() {
		return oldUnit.getEntity();
	}
	
	public Entity getNewEntity() {
		return newUnit.getEntity();
	}
	
	public Unit getOriginalUnit() {
		return oldUnit;
	}
	
	public boolean hasFailedCheck() {
		return failedCheck;
	}

	@Override
	public boolean needsFixing() {
		return true;
	}

	@Override
	public int getDifficulty() {
		switch(refitClass) {
		case NO_CHANGE:
			return 0;
		case CLASS_A:
			return 1;
		case CLASS_B:
			return 1;
		case CLASS_C:
			return 2;
		case CLASS_D:
			return 2;
		case CLASS_E:
			return 3;
		case CLASS_F:
			return 4;
		default:	
			return 1;
				
		}
	}

	@Override
	public TargetRoll getAllMods() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String succeed() {
		complete();
		return "The customization of "+ oldUnit.getEntity().getDisplayName() + " is complete.";
	}

	@Override
	public String fail(int rating) {
		timeSpent = 0;
		failedCheck = true;
		return "The customization of " + oldUnit.getEntity().getDisplayName() + " will take " + getTimeLeft() + " additional minutes to complete.";
	}

	@Override
	public int getMode() {
		return Modes.MODE_NORMAL;
	}

	@Override
	public String getPartName() {
		return newUnit.getEntity().getDisplayName() + " Customization";
	}

	@Override
	public int getSkillMin() {
		return SkillType.EXP_GREEN;
	}

	@Override
	public int getBaseTime() {
		return time;
	}

	@Override
	public int getActualTime() {
		return time;
	}

	@Override
	public int getTimeSpent() {
		return timeSpent;
	}

	@Override
	public int getTimeLeft() {
		return time - timeSpent;
	}

	@Override
	public void addTimeSpent(int time) {
		timeSpent += time;
	}

	@Override
	public int getAssignedTeamId() {
		return assignedTechId;
	}

	@Override
	public void setTeamId(int id) {
		assignedTechId = id;
	}

	@Override
	public boolean hasWorkedOvertime() {
		return false;
	}

	@Override
	public void setWorkedOvertime(boolean b) {
		//do nothing
	}

	@Override
	public int getShorthandedMod() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setShorthandedMod(int i) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateConditionFromEntity() {
		//do nothing
	}

	@Override
	public void updateConditionFromPart() {
		//do nothing
	}

	@Override
	public void fix() {
		//do nothing
	}

	@Override
	public void remove(boolean salvage) {
		//do nothing
	}

	@Override
	public Part getMissingPart() {
		//not applicable
		return null;
	}

	@Override
	public String getDesc() {
		return "Fill this in";
	}

	@Override
	public String getDetails() {
		return "Fill this in";
	}

	@Override
	public Unit getUnit() {
		return oldUnit;
	}

	@Override
	public boolean isSalvaging() {
		return false;
	}

	@Override
	public String checkFixable() {
		return null;
	}
}