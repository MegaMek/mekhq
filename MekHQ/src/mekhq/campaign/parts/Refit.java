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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.Modes;
import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import megamek.common.WeaponType;
import megamek.common.loaders.BLKFile;
import megameklab.com.util.UnitUtil;

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
public class Refit implements IPartWork, IAcquisitionWork {
	
	public static final int NO_CHANGE = 0;
	public static final int CLASS_A = 1;
	public static final int CLASS_B = 2;
	public static final int CLASS_C = 3;
	public static final int CLASS_D = 4;
	public static final int CLASS_E = 5;
	public static final int CLASS_F = 6;
	
	private Unit oldUnit;
	private Entity newEntity;
	
	private int refitClass;
	private int time;
	private int timeSpent;
	private long cost;
	private boolean failedCheck;
	private boolean customJob;
	private boolean kitFound;
	private String fixableString;
	
	private ArrayList<Integer> oldUnitParts;
	private ArrayList<Integer> newUnitParts;
	private ArrayList<Part> shoppingList;
	
	private int armorNeeded;
	private Armor newArmorSupplies;
	private int newArmorSuppliesId;
	
	private int assignedTechId;
	
	public Refit() {
		oldUnitParts = new ArrayList<Integer>();
		newUnitParts = new ArrayList<Integer>();
		shoppingList = new ArrayList<Part>();
		fixableString = null;
	}
	
	public Refit(Unit oUnit, Entity newEn, boolean custom) {
		customJob = custom;
		oldUnit = oUnit;
		newEntity = newEn;
		oldUnitParts = new ArrayList<Integer>();
		newUnitParts = new ArrayList<Integer>();
		shoppingList = new ArrayList<Part>();
		assignedTechId = -1;
		failedCheck = false;
		timeSpent = 0;
		fixableString = null;
		kitFound = false;
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
	
	public int getRefitClass() {
		return refitClass;
	}
	
	public long getCost() {
		return cost;
	}
	
	public int getTime() {
		return time;
	}
	
	public void calculate() {
		Unit newUnit = new Unit(newEntity, oldUnit.campaign);
		newUnit.initializeParts(false);
		refitClass = NO_CHANGE;
		time = 0;
		ArrayList<Part> newPartList = new ArrayList<Part>();
		
		//Step 1: put all of the parts from the current unit into a new arraylist so they can
		//be removed when we find a match.
		for(Part p : oldUnit.getParts()) {
			oldUnitParts.add(p.getId());
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
			Part movedPart = null;
			int moveIndex = 0;
			int i = -1;
			for(int pid : oldUnitParts) {
				Part oPart = oldUnit.campaign.getPart(pid);
				i++;
				if(((oPart instanceof MissingPart && ((MissingPart)oPart).isAcceptableReplacement(part, true)) 
						|| oPart.isSamePartTypeAndStatus(part))
						|| (part instanceof AmmoBin && oPart instanceof AmmoBin && 
								!((AmmoType)((AmmoBin)part).getType()).equals((AmmoType)((AmmoBin)oPart).getType()))
						|| (part instanceof AmmoBin && oPart instanceof MissingAmmoBin && 
								!((AmmoType)((AmmoBin)part).getType()).equals((AmmoType)((MissingAmmoBin)oPart).getType()))) {
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
						} 
					}
					newUnitParts.add(pid);
					partFound = true;
					break;
				} 
			}
			if(partFound) {
				oldUnitParts.remove(i);
			} else if(null != movedPart) {
				newUnitParts.add(movedPart.getId());
				oldUnitParts.remove(moveIndex);
				updateRefitClass(CLASS_C);
				if(movedPart instanceof EquipmentPart) {
					boolean isSalvaging = movedPart.isSalvaging();
					movedPart.setSalvaging(true);
					movedPart.updateConditionFromEntity();
					time += movedPart.getBaseTime();
					movedPart.setSalvaging(isSalvaging);
				}
			} else {
				//its a new part
				//dont actually add the part iself but rather its missing equivalent
				//except in the case of armor
				if(part instanceof Armor) {
					newPartList.add(part);
				} else {
					newPartList.add(part.getMissingPart());
				}
			}		
		}
		
		//Step 3: loop through the newequipment list and determine what class of refit it entails,
		//add time for both installing this part.
		//This may involve taking a look at remaining oldunit parts to determine whether this item
		//replaces another item of the same or fewer crits. Also add cost for new equipment.
		//at the same time, check the parts store for new equipment
		
		//first put oldUnitParts in a new arraylist so they can be removed as we find them
		ArrayList<Integer> tempParts = new ArrayList<Integer>();
		tempParts.addAll(oldUnitParts);
		
		armorNeeded = 0;
		int atype = 0;
		boolean aclan = false;
		
		for(Part nPart : newPartList) {
			nPart.setUnit(oldUnit);
			if(nPart instanceof MissingPart) {
				time += nPart.getBaseTime();
				Part replacement = ((MissingPart)nPart).findReplacement(true);
				if(null != replacement) {
					replacement.setRefitId(oldUnit.getId());
					newUnitParts.add(replacement.getId());
				} else {
					cost += ((MissingPart)nPart).getNewPart().getCurrentValue();
					shoppingList.add(nPart);
				}
			} else if(nPart instanceof Armor) {
				int totalAmount = ((Armor)nPart).getTotalAmount();
				time += totalAmount * ((Armor)nPart).getBaseTimeFor(newEntity);
				armorNeeded += totalAmount;
				atype = ((Armor)nPart).getType();
				aclan = ((Armor)nPart).isClanTechBase();
				//armor always gets added to the shopping list - it will be checked for differently
				shoppingList.add(nPart);
			}
			if(nPart instanceof MissingEnginePart) {
				if(oldUnit.getEntity().getEngine().getRating() != newUnit.getEntity().getEngine().getRating()) {
					updateRefitClass(CLASS_D);
				}
				if(newUnit.getEntity().getEngine().getEngineType() != oldUnit.getEntity().getEngine().getEngineType()) {
					updateRefitClass(CLASS_F);
				}
			} else if(nPart instanceof MissingMekGyro) {
				updateRefitClass(CLASS_F);
			} else if(nPart instanceof MissingMekLocation) {
				if(((Mech)newUnit.getEntity()).hasTSM() != ((Mech)oldUnit.getEntity()).hasTSM()) {
					updateRefitClass(CLASS_E);
				} else {
					updateRefitClass(CLASS_F);
				}
			} else if(nPart instanceof Armor) {
				updateRefitClass(CLASS_C);
			} else { 
				//determine whether this is A, B, or C
				if(nPart instanceof MissingEquipmentPart) {
					nPart.setUnit(newUnit);
					int loc = ((MissingEquipmentPart)nPart).getLocation();
					EquipmentType type = ((MissingEquipmentPart)nPart).getType();
					int crits = type.getCriticals(newUnit.getEntity());
					nPart.setUnit(oldUnit);
					int i = -1;
					boolean matchFound = false;
					int matchIndex = -1;
					int rClass = CLASS_D;
					for(int pid : tempParts) {
						Part oPart = oldUnit.campaign.getPart(pid);
						i++;
						int oLoc = -1;
						int oCrits = -1;
						EquipmentType oType = null;
						if(oPart instanceof EquipmentPart) {
							oLoc = ((EquipmentPart)oPart).getLocation();
							oType = ((EquipmentPart)oPart).getType();
							oCrits = oType.getCriticals(oldUnit.getEntity());
						}
						if(oPart instanceof MissingEquipmentPart) {
							oLoc = ((MissingEquipmentPart)oPart).getLocation();
							oType = ((MissingEquipmentPart)oPart).getType();
							oCrits = oType.getCriticals(oldUnit.getEntity());
						}
						if(loc != oLoc) {
							continue;
						}
						if(crits == oCrits 
								&& (type.hasFlag(WeaponType.F_LASER) == oType.hasFlag(WeaponType.F_LASER))
								&& (type.hasFlag(WeaponType.F_MISSILE) == oType.hasFlag(WeaponType.F_MISSILE))
								&& (type.hasFlag(WeaponType.F_BALLISTIC) == oType.hasFlag(WeaponType.F_BALLISTIC))
								&& (type.hasFlag(WeaponType.F_ARTILLERY) == oType.hasFlag(WeaponType.F_ARTILLERY))) {
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
		if(armorNeeded > 0) {		
			newArmorSupplies = new Armor(0, atype, 0, 0, false, aclan);
			newArmorSupplies.setAmountNeeded(armorNeeded);
			newArmorSupplies.setRefitId(oldUnit.getId());
			//check existing supplies before determining cost
			Armor existingArmorSupplies = getExistingArmorSupplies();
			int amountNeeded = armorNeeded;
			if(null != existingArmorSupplies) {
				amountNeeded = Math.max(0, armorNeeded - existingArmorSupplies.getAmount());
			}
			newArmorSupplies.setUnit(oldUnit);
			cost += (long)(Math.ceil(((double)amountNeeded)/(5*newArmorSupplies.getArmorPointsPerTon())) * 5 * EquipmentType.getArmorCost(atype));
			newArmorSupplies.setUnit(null);
		}
		
		//Step 4: loop through remaining equipment on oldunit parts and add time for removing.
		for(int pid : oldUnitParts) {
			Part oPart = oldUnit.campaign.getPart(pid);
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
		
		//deal with integral heat sinks
		//TODO: compact heat sinks
		//TODO: heat sinks on other units?
		if(newEntity instanceof Mech 
				&& (((Mech)newEntity).hasDoubleHeatSinks() != ((Mech)oldUnit.getEntity()).hasDoubleHeatSinks()
						|| ((Mech)newEntity).hasLaserHeatSinks() != ((Mech)oldUnit.getEntity()).hasLaserHeatSinks())) {
			time += newEntity.getEngine().integralHeatSinkCapacity() * 90;
			time += oldUnit.getEntity().getEngine().integralHeatSinkCapacity() * 90;
			updateRefitClass(CLASS_D);
		}
		
		//check for CASE
		//TODO: we still dont have to order the part, we need to get the CASE issues sorted out
		for(int loc = 0; loc < newEntity.locations(); loc++) {
			if(newEntity.locationHasCase(loc) != oldUnit.getEntity().locationHasCase(loc)
					|| (newEntity instanceof Mech 
							&& ((Mech)newEntity).hasCASEII(loc) != ((Mech)oldUnit.getEntity()).hasCASEII(loc))) {
				time += 60;
				updateRefitClass(CLASS_E);
			}
		}
		
		//multiply time by refit class
		time *= getTimeMultiplier();
		if(!customJob) {
			cost *= 1.1;
		}
	}
	
	public void begin() {
		oldUnit.setRefit(this);
		if(customJob) {
			saveCustomization();
		}
	}
	
	public void resetCheckedToday() {
		for(Part part : shoppingList) {
			if(part instanceof IAcquisitionWork) {
				((IAcquisitionWork)part).setCheckedToday(false);
			}
		}
	}
	
	public boolean acquireParts() {
		if(!customJob) {
			return acquireRefitKit();
		}
		ArrayList<Part> newShoppingList = new ArrayList<Part>();
		Person tech = oldUnit.campaign.getPerson(assignedTechId);
		for(Part part : shoppingList) {
			if(part instanceof Armor) {
				//automatically add armor by location, we will check for the lump sum of
				//armor using newArmorSupplies
				oldUnit.campaign.addPart(part);
				part.setUnit(oldUnit);
				newUnitParts.add(part.getId());
			}
			else if(part instanceof IAcquisitionWork) {
				if(oldUnit.campaign.acquirePart((IAcquisitionWork)part, tech)) {
					Part replacement = ((MissingPart)part).findReplacement(true);
					if(null != replacement) {
						replacement.setRefitId(oldUnit.getId());
						newUnitParts.add(replacement.getId());
					} else {
						//shouldnt happen, but just to be sure
						newShoppingList.add(part);
					}
				} else {
					newShoppingList.add(part);
				}
			}
		}
		if(null != newArmorSupplies) {
			if(newArmorSupplies.getId() <= 0) {
				checkForArmorSupplies();
				oldUnit.campaign.addPart(newArmorSupplies);
			}
			if(newArmorSupplies.getAmount() <= armorNeeded) {
				Armor a = new Armor(0, newArmorSupplies.getType(), 0, 0, false, newArmorSupplies.isClanTechBase());
				a.setRefitId(oldUnit.getId());
				a.setUnit(oldUnit);
				a.setAmountNeeded(armorNeeded);
				oldUnit.campaign.acquirePart((IAcquisitionWork)a, tech);
			}
		}
		shoppingList = newShoppingList;
		return shoppingList.size() == 0 && (null == newArmorSupplies || newArmorSupplies.getAmount() >= armorNeeded);
	}
	
	public void checkForArmorSupplies() {
		Armor existingArmorSupplies = getExistingArmorSupplies();	
		if(null != existingArmorSupplies) {
			if(existingArmorSupplies.getAmount() > armorNeeded) {
				newArmorSupplies.setAmount(armorNeeded);
				existingArmorSupplies.setAmount(existingArmorSupplies.getAmount() - armorNeeded);
			} else {
				newArmorSupplies.setAmount(newArmorSupplies.getAmount() + existingArmorSupplies.getAmount());
				oldUnit.campaign.removePart(existingArmorSupplies);
			}
			oldUnit.campaign.updateAllArmorForNewSpares();
		}
	}
	
	public Armor getExistingArmorSupplies() {
		Armor existingArmorSupplies = null;
		if(null == newArmorSupplies) {
			return null;
		}
		for(Part part : oldUnit.campaign.getSpareParts()) {
			if(part instanceof Armor && ((Armor)part).getType() == newArmorSupplies.getType() 
					&& ((Armor)part).isClanTechBase() == newArmorSupplies.isClanTechBase() 
					&& !part.isReservedForRefit()) {
				existingArmorSupplies = (Armor)part;				
				break;
			}
		}
		return existingArmorSupplies;
	}
	
	public boolean acquireRefitKit() {
		if(kitFound) {
			return true;
		}
		checkForArmorSupplies();
		Person tech = oldUnit.campaign.getPerson(assignedTechId);
		return oldUnit.campaign.acquirePart((IAcquisitionWork)this, tech);
	}
	
	private void updateRefitClass(int rClass) {
		if(rClass > refitClass) {
			refitClass = rClass;
		}
	}
	
	public void cancel() {
		oldUnit.setRefit(null);
		for(int pid : newUnitParts) {
			Part part = oldUnit.campaign.getPart(pid);
			part.setRefitId(-1);
			if(part instanceof Armor) {
				oldUnit.campaign.removePart(part);
			}
		}	
		if(null != newArmorSupplies) {
			newArmorSupplies.setRefitId(-1);
			newArmorSupplies.setUnit(oldUnit);
			oldUnit.campaign.removePart(newArmorSupplies);
			newArmorSupplies.changeAmountAvailable(newArmorSupplies.getAmount());
		}
	}
	
	private void complete() {
		oldUnit.setEntity(newEntity);
		//add old parts to the warehouse
		for(int pid : oldUnitParts) {
			Part part = oldUnit.campaign.getPart(pid);
			if(part instanceof MekLocation && ((MekLocation)part).getLoc() == Mech.LOC_CT) {
				part.setUnit(null);
				oldUnit.campaign.removePart(part);
				continue;
			}
			if(part instanceof Armor) {
				Armor a = (Armor)part;
				a.changeAmountAvailable(a.getAmount());
				oldUnit.campaign.removePart(part);
			}
			part.setUnit(null);
		}
		//set up new parts
		ArrayList<Part> newParts = new ArrayList<Part>();
		for(int pid : newUnitParts) {
			Part part = oldUnit.campaign.getPart(pid);
			if(null == part) {
				MekHQ.logMessage("part with id " + pid + " not found for refit of " + getDesc());
				return;
			}
			part.setUnit(oldUnit);
			part.setRefitId(-1);
			newParts.add(part);
		}
		oldUnit.setParts(newParts);
		unscrambleEquipmentNumbers();	
		if(null != newArmorSupplies) {
			oldUnit.campaign.removePart(newArmorSupplies);
		}
		for(Part part : oldUnit.getParts()) {
			part.updateConditionFromPart();
		}
		oldUnit.resetPilotAndEntity();
		oldUnit.setRefit(null);
	}
	
	private void unscrambleEquipmentNumbers() {
		ArrayList<Integer> equipNums = new ArrayList<Integer>();
		for(Mounted m : oldUnit.getEntity().getEquipment()) {
			equipNums.add(oldUnit.getEntity().getEquipmentNum(m));
		}
		for(Part part : oldUnit.getParts()) {
			if(part instanceof EquipmentPart) {
				EquipmentPart epart = (EquipmentPart)part;
				int i = -1;
				boolean found = false;
				for(int equipNum : equipNums) {
					i++;
					Mounted m = oldUnit.getEntity().getEquipment(equipNum);
					if(m.getType().equals(epart.getType())) {
						epart.setEquipmentNum(equipNum);
						found = true;
						break;
					}
				}
				if(found) {
					equipNums.remove(i);
				}
			}
			else if(part instanceof MissingEquipmentPart) {
				MissingEquipmentPart epart = (MissingEquipmentPart)part;
				int i = -1;
				boolean found = false;
				for(int equipNum : equipNums) {
					i++;
					Mounted m = oldUnit.getEntity().getEquipment(equipNum);
					if(m.getType().equals(epart.getType())) {
						epart.setEquipmentNum(equipNum);
						found = true;
						break;
					}
				}
				if(found) {
					equipNums.remove(i);
				}
			}
		}
	}
	
	public void saveCustomization() {
		UnitUtil.compactCriticals(newEntity);
	    UnitUtil.reIndexCrits(newEntity);
	
		String fileName = newEntity.getChassis() + " " + newEntity.getModel();    
	    String sCustomsDir = "data/mechfiles/customs/";
	    File customsDir = new File(sCustomsDir);
	    if(!customsDir.exists()) {
	    	customsDir.mkdir();
	    }
	    try {
	        if (newEntity instanceof Mech) {
	            FileOutputStream out = new FileOutputStream(sCustomsDir + File.separator + fileName + ".mtf");
	            PrintStream p = new PrintStream(out);
	            p.println(((Mech) newEntity).getMtf());
	            p.close();
	            out.close();
	        } else {
	            BLKFile.encode(sCustomsDir + File.separator + fileName + ".blk", newEntity);
	        }
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }
	    oldUnit.campaign.addCustom(newEntity.getChassis() + " " + newEntity.getModel());
	    MechSummaryCache.getInstance().loadMechData();
	}
	
	private int getTimeMultiplier() {
		int mult = 0;
		switch(refitClass) {
		case NO_CHANGE:
			mult = 0;
			break;
		case CLASS_A:
		case CLASS_B:
			mult = 1;
			break;
		case CLASS_C:
			mult = 2;
			break;
		case CLASS_D:
			mult = 3;
			break;
		case CLASS_E:
			mult = 4;
			break;
		case CLASS_F:
			mult = 5;
			break;
		default:	
			mult = 1;	
		}
		if(customJob) {
			mult *= 2;
		}
		return mult;
	}
	
	public Entity getOriginalEntity() {
		return oldUnit.getEntity();
	}
	
	public Entity getNewEntity() {
		return newEntity;
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
		TargetRoll mods = new TargetRoll(getDifficulty(), "difficulty");
		mods.append(oldUnit.getSiteMod());
		if(oldUnit.getEntity().getQuirks().booleanOption("easy_maintain")) {
			mods.addModifier(-1, "easy to maintain");
		}
		else if(oldUnit.getEntity().getQuirks().booleanOption("difficult_maintain")) {
			mods.addModifier(1, "difficult to maintain");
		}
		if(customJob) {
			mods.addModifier(2, "custom job");
		}
		return mods;
	}

	@Override
	public String succeed() {
		complete();
		return "The customization of "+ oldUnit.getEntity().getShortName() + " is complete.";
	}

	@Override
	public String fail(int rating) {
		timeSpent = 0;
		failedCheck = true;
		return "The customization of " + oldUnit.getEntity().getShortName() + " will take " + getTimeLeft() + " additional minutes to complete.";
	}

	@Override
	public int getMode() {
		return Modes.MODE_NORMAL;
	}
	
	@Override
	public void resetTimeSpent() {
		timeSpent = 0;
	}

	@Override
	public String getPartName() {
		if(customJob) {
			return newEntity.getShortName() + " Customization";
		} else {
			return newEntity.getShortName() + " Refit Kit";
		}
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
		return fixableString;
	}
	
	public void writeToXml(PrintWriter pw1, int indentLvl, int id) {
		pw1.println(MekHqXmlUtil.indentStr(indentLvl) + "<refit>");
		pw1.println(MekHqXmlUtil.writeEntityToXmlString(newEntity, indentLvl+1));
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<time>"
				+ time + "</time>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<timeSpent>" + timeSpent
				+ "</timeSpent>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<cost>" + cost
				+ "</cost>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<failedCheck>" + failedCheck
				+ "</failedCheck>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<customJob>" + customJob
				+ "</customJob>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<kitFound>" + kitFound
				+ "</kitFound>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<armorNeeded>" + armorNeeded
				+ "</armorNeeded>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<assignedTechId>" + assignedTechId
				+ "</assignedTechId>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<oldUnitParts>");
		for(int pid : oldUnitParts) {
			pw1.println(MekHqXmlUtil.indentStr(indentLvl + 2) + "<pid>" + pid
					+ "</pid>");
		}
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "</oldUnitParts>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<newUnitParts>");
		for(int pid : newUnitParts) {
			pw1.println(MekHqXmlUtil.indentStr(indentLvl + 2) + "<pid>" + pid
					+ "</pid>");
		}
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "</newUnitParts>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<shoppingList>");
		for(Part p : shoppingList) {
			p.writeToXml(pw1, indentLvl+2, id);
		}
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "</shoppingList>");
		if(null != newArmorSupplies) {
			if(newArmorSupplies.getId() == 0) {
				pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<newArmorSupplies>");
				newArmorSupplies.writeToXml(pw1, indentLvl+2, id);
				pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "</newArmorSupplies>");
			} else {
				pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<newArmorSuppliesId>" + newArmorSupplies.getId()
						+ "</newArmorSuppliesId>");
			}
		}
		pw1.println(MekHqXmlUtil.indentStr(indentLvl) + "</refit>");
	}
	
	public static Refit generateInstanceFromXML(Node wn, Unit u) {
		Refit retVal = new Refit();
		retVal.oldUnit = u;
		
		NodeList nl = wn.getChildNodes();
		
		try {
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				
				if (wn2.getNodeName().equalsIgnoreCase("time")) {
					retVal.time = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("timeSpent")) {
					retVal.timeSpent = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("cost")) {
					retVal.cost = Long.parseLong(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("newArmorSuppliesId")) {
					retVal.newArmorSuppliesId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("assignedTechId")) {
					retVal.assignedTechId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("failedCheck")) {
					if (wn2.getTextContent().equalsIgnoreCase("true"))
						retVal.failedCheck = true;
					else
						retVal.failedCheck = false;
				} else if (wn2.getNodeName().equalsIgnoreCase("customJob")) {
					if (wn2.getTextContent().equalsIgnoreCase("true"))
						retVal.customJob = true;
					else
						retVal.customJob = false;
				} else if (wn2.getNodeName().equalsIgnoreCase("kitFound")) {
					if (wn2.getTextContent().equalsIgnoreCase("true"))
						retVal.kitFound = true;
					else
						retVal.kitFound = false;
				} else if (wn2.getNodeName().equalsIgnoreCase("armorNeeded")) {
					retVal.armorNeeded = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("entity")) {
					retVal.newEntity = MekHqXmlUtil.getEntityFromXmlString(wn2);
				} else if (wn2.getNodeName().equalsIgnoreCase("oldUnitParts")) {
					NodeList nl2 = wn2.getChildNodes();
					for (int y=0; y<nl2.getLength(); y++) {
						Node wn3 = nl2.item(y);
						if (wn3.getNodeName().equalsIgnoreCase("pid")) {
							retVal.oldUnitParts.add(Integer.parseInt(wn3.getTextContent()));
						}
					}
				} else if (wn2.getNodeName().equalsIgnoreCase("newUnitParts")) {
					NodeList nl2 = wn2.getChildNodes();
					for (int y=0; y<nl2.getLength(); y++) {
						Node wn3 = nl2.item(y);
						if (wn3.getNodeName().equalsIgnoreCase("pid")) {
							retVal.newUnitParts.add(Integer.parseInt(wn3.getTextContent()));
						}
					}
				} else if (wn2.getNodeName().equalsIgnoreCase("shoppingList")) {
					processShoppingList(retVal, wn2, retVal.oldUnit);
				} else if (wn2.getNodeName().equalsIgnoreCase("newArmorSupplies")) {
					processArmorSupplies(retVal, wn2);
				}
			}
		} catch (Exception ex) {
			// Doh!
			MekHQ.logError(ex);
		}
		
		return retVal;
	}
	
	private static void processShoppingList(Refit retVal, Node wn, Unit u) {

		NodeList wList = wn.getChildNodes();
		
		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			if (!wn2.getNodeName().equalsIgnoreCase("part")) {
				// Error condition of sorts!
				// Errr, what should we do here?
				MekHQ.logMessage("Unknown node type not loaded in Part nodes: "+wn2.getNodeName());

				continue;
			}
			Part p = Part.generateInstanceFromXML(wn2);
			p.setUnit(u);
			
			if (p != null) {
				retVal.shoppingList.add(p);
			}
		}
	}
	
	private static void processArmorSupplies(Refit retVal, Node wn) {

		NodeList wList = wn.getChildNodes();
		
		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			if (!wn2.getNodeName().equalsIgnoreCase("part")) {
				// Error condition of sorts!
				// Errr, what should we do here?
				MekHQ.logMessage("Unknown node type not loaded in Part nodes: "+wn2.getNodeName());

				continue;
			}
			Part p = Part.generateInstanceFromXML(wn2);
			
			if (p != null && p instanceof Armor) {
				retVal.newArmorSupplies = (Armor)p;
				break;
			}
		}
	}

	@Override
	public Part getNewPart() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAcquisitionDesc() {
		return "Fill this in";
	}

	@Override
	public long getPurchasePrice() {
		return cost;
	}

	@Override
	public boolean hasCheckedToday() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setCheckedToday(boolean b) {
		//don't do anything
	}

	@Override
	public int getId() {
		return 0;
	}

	@Override
	public String find() {
		for(Part part : shoppingList) {
			if(part instanceof Armor) {
				oldUnit.campaign.addPart(part);
				part.setUnit(oldUnit);
				newUnitParts.add(part.getId());
			} else if(part instanceof MissingPart) {
				oldUnit.campaign.buyPart(((IAcquisitionWork)part).getNewPart(),(long)(((IAcquisitionWork)part).getPurchasePrice() * 1.1));
				Part replacement = ((MissingPart)part).findReplacement(true);
				if(null != replacement) {
					replacement.setRefitId(oldUnit.getId());
					newUnitParts.add(replacement.getId());
				} 
			}
		}
		if(null != newArmorSupplies) {
			newArmorSupplies.setAmount(armorNeeded);
			oldUnit.campaign.buyPart(newArmorSupplies,(long)(newArmorSupplies.getPurchasePrice() * 1.1));
		}
		shoppingList = new ArrayList<Part>();
		kitFound = true;
		return "<font color='green'> refit kit found.</font>";
	}

	@Override
	public String failToFind() {
		return "<font color='red'> refit kit not found.</font>";

	}

	@Override
	public TargetRoll getAllAcquisitionMods() {
		TargetRoll roll = new TargetRoll();
		for(Part part : shoppingList) {
			if(((IAcquisitionWork)part).getAllAcquisitionMods().getValue() > roll.getValue()) {
				roll = ((IAcquisitionWork)part).getAllAcquisitionMods();
			}
		}
		return roll;
	}
	
	public Armor getNewArmorSupplies() {
		return newArmorSupplies;
	}
	
	public void setNewArmorSupplies(Armor a) {
		newArmorSupplies = a;
	}
	
	public int getNewArmorSuppliesId() {
		return newArmorSuppliesId;
	}

	@Override
	public void resetOvertime() {
		// TODO Auto-generated method stub
		
	}
}