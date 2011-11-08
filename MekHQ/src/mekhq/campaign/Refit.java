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

package mekhq.campaign;

import java.util.ArrayList;

import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import megamek.common.Entity;
import megamek.common.Mech;

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
public class Refit {
	
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
	private long cost;
	
	private ArrayList<Part> oldUnitParts;
	private ArrayList<Part> newUnitParts;
	private ArrayList<Part> newEquipment;
	
	public Refit(Unit oUnit, Entity newEn) {
		oldUnit = oUnit;
		newUnit = new Unit(newEn, oldUnit.campaign);
		newUnit.initializeParts(false);
		oldUnitParts = new ArrayList<Part>();
		newUnitParts = new ArrayList<Part>();
		newEquipment = new ArrayList<Part>();
		calculate();
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
		
		/*
		 * Ok I think the best way to handle this will be to create a new unit
		 * from the new entity, and then cycle through its parts. For each part, look for a 
		 * corresponding part in the old units part vector. If the same, then put that part in 
		 * a keeper vector. If different or missing, then put in a salvaged part vector and add a part to 
		 * a parts needed vector. At the same time, refit class and time required can be 
		 * updated
		 */
		
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
		for(Part part : newUnit.getParts()) {
			boolean partFound = false;
			int i = 0;
			for(Part oPart : oldUnitParts) {
				if(oPart instanceof Armor) {
					int bob = 1;
				}
				if((oPart instanceof MissingPart && ((MissingPart)oPart).isAcceptableReplacement(part)) 
						|| oPart.isSamePartTypeAndStatus(part)) {
					//need a special check for location and armor amount for armo
					if(oPart instanceof Armor 
							&& (((Armor)oPart).getLocation() != ((Armor)part).getLocation()
									|| ((Armor)oPart).getTotalAmount() != ((Armor)part).getTotalAmount())) {
						continue;
					}
					//TODO: check location of EquipmentPart and MissingEquipmentPart
					newUnitParts.add(oPart);
					partFound = true;
					break;
				}
				i++;
			}
			if(partFound) {
				oldUnitParts.remove(i);
			} else {
				//its a new part
				newEquipment.add(part);
			}		
		}
		
		//Step 5: loop through the newequipment list and determine what class of refit it entails,
		//add time for both installing this part.
		//This may involve taking a look at remaining oldunit parts to determine whether this item
		//replaces another item of the same or fewer crits. Also add cost for new equipment.
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
				//TODO: determine whether this is A, B, or C
			}
		}
		
		//Step 6: loop through remaining equipment on oldunit parts and add time for removing.
		for(Part oPart : oldUnitParts) {
			if(oPart instanceof MissingPart) {
				continue;
			}
			oPart.setSalvaging(true);
			oPart.updateConditionFromEntity();
			time += oPart.getBaseTime();
			oPart.setUnit(null);
		}
		
		//Class C
		//change armor type or quantity
		//add ammunition
		//move component
		//add heat sink
		//replace equipment with any other
		
		//Class B
		//replace one weapon with an another type of same or fewer crit spaces
		
		//Class A
		//replace weapon with same type and crit spaces

	}
	
	private void updateRefitClass(int rClass) {
		if(rClass > refitClass) {
			refitClass = rClass;
		}
	}
	
	public void complete() {
		for(Part part : newEquipment) {
			newUnitParts.add(part);
			oldUnit.campaign.addPart(part);
		}
		oldUnit.setEntity(newUnit.getEntity());
		oldUnit.setParts(newUnitParts);
		oldUnit.runDiagnostic();
	}
	
}