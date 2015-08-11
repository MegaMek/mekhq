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
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
//import java.io.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BipedMech;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.TargetRoll;
import megamek.common.WeaponType;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;
import megameklab.com.util.UnitUtil;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.MhqFileUtil;
import mekhq.Utilities;
import mekhq.Version;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.Modes;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
public class Refit extends Part implements IPartWork, IAcquisitionWork {

	/**
     *
     */
    private static final long serialVersionUID = -1765098410743713570L;
    public static final int NO_CHANGE = 0;
	public static final int CLASS_OMNI = 1;
	public static final int CLASS_A = 2;
	public static final int CLASS_B = 3;
	public static final int CLASS_C = 4;
	public static final int CLASS_D = 5;
	public static final int CLASS_E = 6;
	public static final int CLASS_F = 7;

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
	private boolean sameArmorType;

	private UUID assignedTechId;
	private int oldTechId = -1;

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
        newEntity.setOwner(oldUnit.getEntity().getOwner());
		newEntity.setGame(oldUnit.getEntity().getGame());
		oldUnitParts = new ArrayList<Integer>();
		newUnitParts = new ArrayList<Integer>();
		shoppingList = new ArrayList<Part>();
		failedCheck = false;
		timeSpent = 0;
		fixableString = null;
		kitFound = false;
		campaign = oldUnit.campaign;
		calculate();
		if(customJob) {
			suggestNewName();
		}
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
		case CLASS_OMNI:
			return "Omni Repod";
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

	public ArrayList<Part> getShoppingList() {
		return shoppingList;
	}

	public String[] getShoppingListDescription() {
		Hashtable<String,Integer> tally = new Hashtable<String,Integer>();
	    Hashtable<String,String> desc = new Hashtable<String,String>();
		for(Part p : shoppingList) {
		    if(p instanceof Armor || p instanceof ProtomekArmor || p instanceof BaArmor) {
		        continue;
		    }
			if(null != tally.get(p.getName())) {
				tally.put(p.getName(), tally.get(p.getName()) + 1);
				desc.put(p.getName(), p.getQuantityName(tally.get(p.getName())));
			} else {
				tally.put(p.getName(), 1);
				desc.put(p.getName(), p.getQuantityName(1));
			}
		}
		if(null != newArmorSupplies) {
		    int actualAmountNeeded = armorNeeded;
		    Armor existingSupplies = getExistingArmorSupplies();
		    if(null != existingSupplies) {
		        actualAmountNeeded -= existingSupplies.getAmount();
		    }
		    if(actualAmountNeeded > 0) {
		        Armor a = (Armor)newArmorSupplies.getNewPart();
		        a.setAmount(actualAmountNeeded);
		        desc.put(a.getName(), a.getQuantityName(1));
		    }
		}
		String[] descs = new String[desc.keySet().size()];
		int i = 0;
		for(String name : desc.keySet()) {
			descs[i] = desc.get(name);
			i++;
		}
		return descs;
	}

	public int getTime() {
		return time;
	}

	public void calculate() {
		Unit newUnit = new Unit(newEntity, oldUnit.campaign);
		newUnit.initializeParts(false);
		refitClass = NO_CHANGE;
		boolean isOmni = oldUnit.getEntity().isOmni();
		time = 0;
		sameArmorType = newEntity.getArmorType(0) == oldUnit.getEntity().getArmorType(0);
		int recycledArmorPoints = 0;
		boolean replacingLocations = false;
		boolean[] locationHasNewStuff = new boolean[newEntity.locations()];
		Arrays.fill(locationHasNewStuff, Boolean.FALSE);
		HashMap<EquipmentType,Integer> ammoNeeded = new HashMap<EquipmentType,Integer>();
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
						|| oPart.isSamePartType(part))) {
					//need a special check for location and armor amount for armor
					if(oPart instanceof Armor
							&& (((Armor)oPart).getLocation() != ((Armor)part).getLocation()
									|| ((Armor)oPart).getTotalAmount() != ((Armor)part).getTotalAmount())) {
						continue;
					}
					if(oPart instanceof ProtomekArmor
							&& (((ProtomekArmor)oPart).getLocation() != ((ProtomekArmor)part).getLocation()
									|| ((ProtomekArmor)oPart).getTotalAmount() != ((ProtomekArmor)part).getTotalAmount())) {
						continue;
					}
					if(oPart instanceof BaArmor
							&& (((BaArmor)oPart).getLocation() != ((BaArmor)part).getLocation()
									|| ((BaArmor)oPart).getTotalAmount() != ((BaArmor)part).getTotalAmount())) {
						continue;
					}
					if(part instanceof EquipmentPart) {
						//check the location to see if this moved. If so, then don't break, but
						//save this in case we fail to find equipment in the same location.
						int loc = ((EquipmentPart)part).getLocation();
						boolean rear = ((EquipmentPart)part).isRearFacing();
						if((oPart instanceof EquipmentPart
								&& (((EquipmentPart)oPart).getLocation() != loc || ((EquipmentPart)oPart).isRearFacing() != rear))
								|| (oPart instanceof MissingEquipmentPart
										&& (((MissingEquipmentPart)oPart).getLocation() != loc || ((MissingEquipmentPart)oPart).isRearFacing() != rear))) {
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
				if(isOmni && movedPart.isOmniPoddable()) {
					updateRefitClass(CLASS_OMNI);
				} else {
					updateRefitClass(CLASS_C);
				}
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
				if(part instanceof Armor || part instanceof ProtomekArmor || part instanceof BaArmor || part instanceof AmmoBin) {
					newPartList.add(part);
				} else {
					Part mPart = part.getMissingPart();
					if(null != mPart) {
						newPartList.add(mPart);
					} else {
						MekHQ.logError("null missing part for " + part.getName() + " during refit calculations");
					}
				}
			}
		}

		//Step 3: loop through the newequipment list and determine what class of refit it entails,
		//add time for both installing this part.
		//This may involve taking a look at remaining oldunit parts to determine whether this item
		//replaces another item of the same or fewer crits. Also add cost for new equipment.
		//at the same time, check spare parts for new equipment

		//first put oldUnitParts in a new arraylist so they can be removed as we find them
		ArrayList<Integer> tempParts = new ArrayList<Integer>();
		tempParts.addAll(oldUnitParts);

		armorNeeded = 0;
		int atype = 0;
		boolean aclan = false;
		HashMap<Integer,Integer> partQuantity = new HashMap<Integer,Integer>();
		for(Part nPart : newPartList) {
			nPart.setUnit(oldUnit);
			if(nPart instanceof MissingPart) {
				time += nPart.getBaseTime();
				Part replacement = ((MissingPart)nPart).findReplacement(true);
				//check quantity
				//TODO: the one weakness here is that we will not pick up damaged parts
				if(null != replacement && null == partQuantity.get(replacement.getId())) {
					partQuantity.put(replacement.getId(), replacement.getQuantity());
				}
				if(null != replacement && partQuantity.get(replacement.getId()) > 0) {
					newUnitParts.add(replacement.getId());
					//adjust quantity
					partQuantity.put(replacement.getId(), partQuantity.get(replacement.getId())-1);
				} else {
					replacement = ((MissingPart)nPart).getNewPart();
					//set entity for variable cost items
					replacement.setUnit(newUnit);
					cost += replacement.getActualValue();
					shoppingList.add(nPart);
				}
			}
			else if(nPart instanceof Armor) {
				int totalAmount = ((Armor)nPart).getTotalAmount();
				time += totalAmount * ((Armor)nPart).getBaseTimeFor(newEntity);
				armorNeeded += totalAmount;
				atype = ((Armor)nPart).getType();
				aclan = ((Armor)nPart).isClanTechBase();
				//armor always gets added to the shopping list - it will be checked for differently
				shoppingList.add(nPart);
			}
			else if(nPart instanceof ProtomekArmor) {
				int totalAmount = ((ProtomekArmor)nPart).getTotalAmount();
				time += totalAmount * ((ProtomekArmor)nPart).getBaseTimeFor(newEntity);
				armorNeeded += totalAmount;
				atype = 0; //((ProtomekArmor)nPart).getType();
				aclan = ((ProtomekArmor)nPart).isClanTechBase();
				//armor always gets added to the shopping list - it will be checked for differently
				shoppingList.add(nPart);
			}
			else if(nPart instanceof BaArmor) {
				int totalAmount = ((BaArmor)nPart).getTotalAmount();
				time += totalAmount * ((BaArmor)nPart).getBaseTimeFor(newEntity);
				armorNeeded += totalAmount;
				atype = ((BaArmor)nPart).getType();
				aclan = ((BaArmor)nPart).isClanTechBase();
				//armor always gets added to the shopping list - it will be checked for differently
				shoppingList.add(nPart);
			}
			else if(nPart instanceof AmmoBin) {
				EquipmentType type = ((AmmoBin)nPart).getType();
				if(null == ammoNeeded.get(type)) {
					ammoNeeded.put(type, ((AmmoType)((AmmoBin)nPart).getType()).getShots());
				} else {
					ammoNeeded.put(type,ammoNeeded.get(type) + ((AmmoType)((AmmoBin)nPart).getType()).getShots());
				}
				time += 120;
				shoppingList.add(nPart);
			}
			if(nPart instanceof MissingEnginePart) {
				if(oldUnit.getEntity().getEngine().getRating() != newUnit.getEntity().getEngine().getRating()) {
					updateRefitClass(CLASS_D);
				}
				if(newUnit.getEntity().getEngine().getEngineType() != oldUnit.getEntity().getEngine().getEngineType()) {
					updateRefitClass(CLASS_F);
				}
				if(((MissingEnginePart)nPart).getEngine().getSideTorsoCriticalSlots().length > 0) {
					locationHasNewStuff[Mech.LOC_LT] = true;
					locationHasNewStuff[Mech.LOC_RT] = true;
				}
			} else if(nPart instanceof MissingMekGyro) {
				updateRefitClass(CLASS_F);
			} else if(nPart instanceof MissingMekLocation) {
				replacingLocations = true;
				if(((Mech)newUnit.getEntity()).hasTSM() != ((Mech)oldUnit.getEntity()).hasTSM()) {
					updateRefitClass(CLASS_E);
				} else {
					updateRefitClass(CLASS_F);
				}
			} else if(nPart instanceof Armor || nPart instanceof ProtomekArmor || nPart instanceof BaArmor) {
				updateRefitClass(CLASS_C);
				locationHasNewStuff[((Armor)nPart).getLocation()] = true;
			} else if(nPart instanceof MissingMekCockpit) {
				updateRefitClass(CLASS_F);
				locationHasNewStuff[Mech.LOC_HEAD] = true;
			}else if(nPart instanceof MissingMekActuator) {
				if(isOmni && nPart.isOmniPoddable()) {
					updateRefitClass(CLASS_OMNI);
				} else {
					updateRefitClass(CLASS_D);
				}
				locationHasNewStuff[((MissingMekActuator)nPart).getLocation()] = true;
			} else if(nPart instanceof MissingInfantryMotiveType || nPart instanceof MissingInfantryArmorPart) {
				updateRefitClass(CLASS_A);
			} else {
				//determine whether this is A, B, or C
				if(nPart instanceof MissingEquipmentPart || nPart instanceof AmmoBin) {
					nPart.setUnit(newUnit);
					int loc = -1;
					EquipmentType type = null;
					if(nPart instanceof MissingEquipmentPart) {
						loc = ((MissingEquipmentPart)nPart).getLocation();
						if(loc > -1 && loc < newEntity.locations()) {
							locationHasNewStuff[loc] = true;
						}
						type = ((MissingEquipmentPart)nPart).getType();
					} else {
						loc = ((AmmoBin)nPart).getLocation();
						if(loc > -1 && loc < newEntity.locations()) {
							locationHasNewStuff[loc] = true;
						}
						type = ((AmmoBin)nPart).getType();
					}
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
					if(isOmni && nPart.isOmniPoddable()) {
						rClass = CLASS_OMNI;
					}
					updateRefitClass(rClass);
					if(matchFound) {
						tempParts.remove(matchIndex);
					}
				}
			}
		}

		//Step 4: loop through remaining equipment on oldunit parts and add time for removing.
		for(int pid : oldUnitParts) {
			Part oPart = oldUnit.campaign.getPart(pid);
			if(oPart instanceof MissingPart) {
				continue;
			}
			if(oPart instanceof AmmoBin) {
				if(((AmmoBin)oPart).getShotsNeeded() < ((AmmoBin)oPart).getFullShots()) {
					time += 120;
				}
				continue;
			}
			if(oPart instanceof Armor && sameArmorType) {
				recycledArmorPoints += ((Armor)oPart).getAmount();
				continue;
			}
			if(oPart instanceof ProtomekArmor && sameArmorType) {
				recycledArmorPoints += ((ProtomekArmor)oPart).getAmount();
				continue;
			}
			if(oPart instanceof BaArmor && sameArmorType) {
				recycledArmorPoints += ((BaArmor)oPart).getAmount();
				continue;
			}
			boolean isSalvaging = oPart.isSalvaging();
			oPart.setSalvaging(true);
			oPart.updateConditionFromEntity();
			time += oPart.getBaseTime();
			oPart.setSalvaging(isSalvaging);
			oPart.updateConditionFromEntity();
		}

		if(sameArmorType) {
			//if this is the same armor type then we can recyle armor
			armorNeeded -= recycledArmorPoints;
		}
		if(armorNeeded > 0) {
			newArmorSupplies = new Armor(0, atype, 0, 0, false, aclan, oldUnit.campaign);
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

		for(EquipmentType type : ammoNeeded.keySet()) {
			int shotsNeeded = Math.max(ammoNeeded.get(type) - getAmmoAvailable(type), 0);
			int shotsPerTon = ((AmmoType)type).getShots();
			cost += type.getCost(newEntity, false, -1) * ((double)shotsNeeded/shotsPerTon);
		}

		//deal with integral heat sinks
		//TODO: heat sinks on other units?
		if(newEntity instanceof Mech
				&& (((Mech)newEntity).hasDoubleHeatSinks() != ((Mech)oldUnit.getEntity()).hasDoubleHeatSinks()
						|| ((Mech)newEntity).hasLaserHeatSinks() != ((Mech)oldUnit.getEntity()).hasLaserHeatSinks())) {
			time += newEntity.getEngine().integralHeatSinkCapacity(((Mech)newEntity).hasCompactHeatSinks()) * 90;
			time += oldUnit.getEntity().getEngine().integralHeatSinkCapacity(((Mech)newEntity).hasCompactHeatSinks()) * 90;
			updateRefitClass(CLASS_D);
		}

		//check for CASE
		//TODO: we still dont have to order the part, we need to get the CASE issues sorted out
		for(int loc = 0; loc < newEntity.locations(); loc++) {
			if((newEntity.locationHasCase(loc) != oldUnit.getEntity().locationHasCase(loc)
					&& !(newEntity.isClan() && newEntity instanceof Mech))
					|| (newEntity instanceof Mech
							&& ((Mech)newEntity).hasCASEII(loc) != ((Mech)oldUnit.getEntity()).hasCASEII(loc))) {
				if(isOmni) {
					updateRefitClass(CLASS_OMNI);
				} else {
					time += 60;
					updateRefitClass(CLASS_E);
				}
			}
		}

		//multiply time by refit class
		time *= getTimeMultiplier();
		if(!customJob) {
			cost *= 1.1;
		}

		//TODO: track the number of locations changed so we can get stuff for omnis
		//TODO: some class D stuff is not omnipodable
		if(refitClass == CLASS_OMNI) {
			int nloc = 0;
			for(boolean newStuff : locationHasNewStuff) {
				if(newStuff) {
					nloc++;
				}
			}
			time = 30 * nloc;
		}

		//infantry take zero time to re-organize
		//also check for squad size and number changes
		if(oldUnit.getEntity() instanceof Infantry && !(oldUnit.getEntity() instanceof BattleArmor)) {
			if(((Infantry)oldUnit.getEntity()).getSquadN() != ((Infantry)newEntity).getSquadN()
					||((Infantry)oldUnit.getEntity()).getSquadSize() != ((Infantry)newEntity).getSquadSize()) {
				updateRefitClass(CLASS_A);
			}
			time = 0;
		}

		//figure out if we are putting new stuff on a missing location
		if(!replacingLocations) {
			for(int loc = 0; loc < newEntity.locations(); loc++) {
				if(locationHasNewStuff[loc] && oldUnit.isLocationDestroyed(loc)) {
					String problem = "Can't add new equipment to a missing " + newEntity.getLocationAbbr(loc);
					if(null == fixableString) {
						fixableString = problem;
					} else {
						fixableString += "\n" + problem;
					}
				}
			}
		}
	}

	public void begin() throws EntityLoadingException, IOException {
	    if(customJob) {
            saveCustomization();
        }
		oldUnit.setRefit(this);
		newEntity.setOwner(oldUnit.getEntity().getOwner());
		reserveNewParts();
		if(customJob) {
		    //add the stuff on the shopping list to the master shopping list
		    ArrayList<Part> newShoppingList = new ArrayList<Part>();
    		for(Part part : shoppingList) {
    			if(part instanceof Armor || part instanceof ProtomekArmor || part instanceof BaArmor) {
                    //automatically add armor by location, we will check for the lump sum of
                    //armor using newArmorSupplies
                    oldUnit.campaign.addPart(part, 0);
                    part.setRefitId(oldUnit.getId());
                    newUnitParts.add(part.getId());
                }
                else if(part instanceof AmmoBin) {
                    //ammo bins are free - bleh
                    AmmoBin bin = (AmmoBin)part;
                    bin.setShotsNeeded(bin.getFullShots());
                    oldUnit.campaign.addPart(part, 0);
                    part.setRefitId(oldUnit.getId());
                    newUnitParts.add(part.getId());
                    part.setUnit(oldUnit);
                    bin.loadBin(false);
                    if(bin.needsFixing()) {
                        oldUnit.campaign.getShoppingList().addShoppingItem(bin, 1, oldUnit.campaign);
                        //need to call it a second time to use up if found
                        bin.loadBin(false);
                    }
                    part.setUnit(null);
                }
                else if(part instanceof IAcquisitionWork) {
    		        oldUnit.campaign.getShoppingList().addShoppingItem(((IAcquisitionWork)part), 1, oldUnit.campaign);
    		        newShoppingList.add(part);
    		    }
    		}
    		shoppingList = newShoppingList;
    		if(null != newArmorSupplies) {
                //add enough armor to the shopping list
                int armorSupplied = 0;
                Armor existingArmorSupplies = getExistingArmorSupplies();
                if(null != existingArmorSupplies) {
                    armorSupplied = existingArmorSupplies.getAmount();
                }
                while(armorSupplied < armorNeeded) {
                    armorSupplied += ((Armor)newArmorSupplies.getNewPart()).getAmount();
                    oldUnit.campaign.getShoppingList().addShoppingItem((Armor)newArmorSupplies.getNewPart(),1,oldUnit.campaign);
                }
            }
		} else {
		    checkForArmorSupplies();
		    oldUnit.campaign.getShoppingList().addShoppingItem(this, 1, oldUnit.campaign);
		}

	}

	public void reserveNewParts() {
		//we need to loop through the new parts and
	    //if they are not on a unit already, then we need
	    //to set the refit id. Also, if there is more than one part
	    //then we need to clone a part and reserve that instead
		ArrayList<Integer> newNewUnitParts = new ArrayList<Integer>();
		for(int id : newUnitParts) {
			Part newPart = oldUnit.campaign.getPart(id);
			if(newPart.isSpare()) {
				if(newPart.getQuantity() > 1) {
					newPart.decrementQuantity();
					newPart = newPart.clone();
					newPart.setRefitId(oldUnit.getId());
					oldUnit.campaign.addPart(newPart, 0);
					newNewUnitParts.add(newPart.getId());
				} else {
					newPart.setRefitId(oldUnit.getId());
					newNewUnitParts.add(id);
				}
			} else {
				newNewUnitParts.add(id);
			}
		}
		newUnitParts = newNewUnitParts;
	}

	public boolean partsInTransit() {
		boolean retVal = false;
		for (int pid : newUnitParts) {
			Part part = oldUnit.campaign.getPart(pid);
			if (part == null) {
				if(null == part) {
					MekHQ.logMessage("part with id " + pid + " not found for refit of " + getDesc());
					continue;
				}
			}
			if (!part.isPresent()) {
				retVal = true;
				break;
			}
		}
		return retVal;
	}

	public boolean acquireParts() {
	    if(!customJob && !kitFound) {
	        return false;
	    }
		ArrayList<Part> newShoppingList = new ArrayList<Part>();
		for(Part part : shoppingList) {
			if(part instanceof IAcquisitionWork) {
			    //check to see if we found a replacement
			    Part replacement = ((MissingPart)part).findReplacement(true);
			    if(null != replacement) {
			        if(replacement.getQuantity() > 1) {
			            Part actualReplacement = replacement.clone();
			            actualReplacement.setRefitId(oldUnit.getId());
			            oldUnit.campaign.addPart(actualReplacement, 0);
			            newUnitParts.add(actualReplacement.getId());
			            replacement.decrementQuantity();
			        } else {
			            replacement.setRefitId(oldUnit.getId());
			            newUnitParts.add(replacement.getId());
			        }
			    } else {
			        newShoppingList.add(part);
			    }
			}
		}
		// Cycle through newUnitParts, find any ammo bins and if they need loading, try to load them
		boolean missingAmmo = false;
		for(int pid : newUnitParts) {
			Part part = oldUnit.campaign.getPart(pid);
			if(part instanceof AmmoBin && null == part.getUnit()) {
				AmmoBin bin = (AmmoBin)part;
				bin.setUnit(oldUnit);
				bin.loadBin(false);
				if(bin.needsFixing()) {
					missingAmmo = true;
				}
                bin.setUnit(null);
			}
		}

		checkForArmorSupplies();
		shoppingList = newShoppingList;

		// Also, check to make sure that they're not still in transit! - ralgith 2013/07/09
		if (partsInTransit()) {
			return false;
		}

		return shoppingList.size() == 0 && !missingAmmo && (null == newArmorSupplies || (armorNeeded - newArmorSupplies.getAmount()) <= 0);
	}

	public void checkForArmorSupplies() {
	    if(null == newArmorSupplies) {
	        return;
	    }
		Armor existingArmorSupplies = getExistingArmorSupplies();
		int actualNeed = armorNeeded - newArmorSupplies.getAmount();
		if(null != existingArmorSupplies && actualNeed > 0) {
			if(existingArmorSupplies.getAmount() > actualNeed) {
				newArmorSupplies.setAmount(armorNeeded);
				newArmorSupplies.setAmountNeeded(0);
				existingArmorSupplies.setAmount(existingArmorSupplies.getAmount() - actualNeed);
			} else {
				newArmorSupplies.setAmount(newArmorSupplies.getAmount() + existingArmorSupplies.getAmount());
				newArmorSupplies.setAmountNeeded(newArmorSupplies.getAmountNeeded() - existingArmorSupplies.getAmount());
				oldUnit.campaign.removePart(existingArmorSupplies);
			}
			if(newArmorSupplies.getId() <= 0) {
	             oldUnit.campaign.addPart(newArmorSupplies, 0);
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
					&& !part.isReservedForRefit()
					&& part.isPresent()) {
				existingArmorSupplies = (Armor)part;
				break;
			}
		}
		return existingArmorSupplies;
	}

	public int getAmmoAvailable(EquipmentType type) {
		for(Part part : oldUnit.campaign.getSpareParts()) {
			if(part instanceof AmmoStorage) {
				AmmoStorage a = (AmmoStorage)part;
				if(a.getType() == type) {
					return a.getShots();
				}
			}
		}
		return 0;
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
			part.setRefitId(null);
			if(part instanceof Armor || part instanceof ProtomekArmor || part instanceof BaArmor) {
				oldUnit.campaign.removePart(part);
			}
			else if(part instanceof AmmoBin) {
				part.setUnit(oldUnit);
				((AmmoBin) part).unload(false);
				part.setUnit(null);
				oldUnit.campaign.removePart(part);
			} else {
				Part spare = oldUnit.campaign.checkForExistingSparePart(part);
				if(null != spare) {
					spare.incrementQuantity();
					oldUnit.campaign.removePart(part);
				}
			}
		}
		if(null != newArmorSupplies) {
			newArmorSupplies.setRefitId(null);
			newArmorSupplies.setUnit(oldUnit);
			oldUnit.campaign.removePart(newArmorSupplies);
			newArmorSupplies.changeAmountAvailable(newArmorSupplies.getAmount());
		}
	}

	private void complete() {
		int atype = -1;
		boolean aclan = false;
	    oldUnit.setRefit(null);
        Entity oldEntity = oldUnit.getEntity();
        ArrayList<Person> soldiers = new ArrayList<Person>();
        //unload any soldiers to reload later, because troop size may have changed
        if(oldEntity instanceof Infantry) {
			soldiers = oldUnit.getCrew();
			for(Person soldier : soldiers) {
				oldUnit.remove(soldier, true);
			}
		}
		oldUnit.setEntity(newEntity);
		//add old parts to the warehouse
		for(int pid : oldUnitParts) {
			Part part = oldUnit.campaign.getPart(pid);
			if(null == part) {
				continue;
			}
			if(part instanceof MekLocation && ((MekLocation)part).getLoc() == Mech.LOC_CT) {
				part.setUnit(null);
				oldUnit.campaign.removePart(part);
				continue;
			}
			// SI Should never be "kept" for the Warehouse
			else if(part instanceof StructuralIntegrity) {
				part.setUnit(null);
				oldUnit.campaign.removePart(part);
				continue;
			}
			else if(part instanceof Armor) {
				Armor a = (Armor)part;
				if(!sameArmorType) {
					a.changeAmountAvailable(a.getAmount());
				} else {
					atype = a.getType();
					aclan = a.isClanTechBase();
				}
				oldUnit.campaign.removePart(part);
			}
			else if(part instanceof ProtomekArmor) {
				ProtomekArmor a = (ProtomekArmor)part;
				if(!sameArmorType) {
					a.changeAmountAvailable(a.getAmount());
				} else {
					atype = 0; //a.getType();
					aclan = a.isClanTechBase();
				}
				oldUnit.campaign.removePart(part);
			}
			else if(part instanceof BaArmor) {
				BaArmor a = (BaArmor)part;
				if(!sameArmorType) {
					a.changeAmountAvailable(a.getAmount());
				} else {
					atype = a.getType();
					aclan = a.isClanTechBase();
				}
				oldUnit.campaign.removePart(part);
			}
			else if(part instanceof AmmoBin) {
				((AmmoBin) part).unload(false);
			}
			else {
				Part spare = oldUnit.campaign.checkForExistingSparePart(part);
				if(null != spare) {
					spare.incrementQuantity();
					oldUnit.campaign.removePart(part);
				}
			}
			part.setUnit(null);
		}
		//set up new parts
		ArrayList<Part> newParts = new ArrayList<Part>();
		for(int pid : newUnitParts) {
			Part part = oldUnit.campaign.getPart(pid);
			if(null == part) {
				MekHQ.logMessage("part with id " + pid + " not found for refit of " + getDesc());
				continue;
			}
			part.setUnit(oldUnit);
			part.setRefitId(null);
			newParts.add(part);
		}
		oldUnit.setParts(newParts);
		Utilities.unscrambleEquipmentNumbers(oldUnit);
		assignArmActuators();
		if(null != newArmorSupplies) {
			oldUnit.campaign.removePart(newArmorSupplies);
		}
		//in some cases we may have had more armor on the original unit and so we may add more
		//back then we received
		if(sameArmorType && armorNeeded < 0) {
			Armor a = new Armor(0, atype, -1 * armorNeeded, -1, false, aclan, oldUnit.campaign);
			a.setUnit(oldUnit);
			a.changeAmountAvailable(a.getAmount());
		}
		for(Part part : oldUnit.getParts()) {
			part.updateConditionFromPart();
		}
        oldUnit.getEntity().setC3UUIDAsString(oldEntity.getC3UUIDAsString());
        oldUnit.getEntity().setExternalIdAsString(oldUnit.getId().toString());
		oldUnit.campaign.clearGameData(oldUnit.getEntity());
		oldUnit.campaign.reloadGameEntities();
		//reload any soldiers
		for(Person soldier : soldiers) {
			if(!oldUnit.canTakeMoreGunners()) {
				break;
			}
			oldUnit.addPilotOrSoldier(soldier);
		}
		oldUnit.resetPilotAndEntity();
	}

	public void saveCustomization() throws EntityLoadingException, IOException {
		UnitUtil.compactCriticals(newEntity);
	    //UnitUtil.reIndexCrits(newEntity); Method is gone?

		String fileName = MhqFileUtil.escapeReservedCharacters(newEntity.getChassis() + " " + newEntity.getModel());
	    String sCustomsDir = "data"+File.separator+"mechfiles"+File.separator+"customs";
		String sCustomsDirCampaign = sCustomsDir+File.separator+oldUnit.campaign.getName();
	    File customsDir = new File(sCustomsDir);
	    if(!customsDir.exists()) {
	    	customsDir.mkdir();
	    }
	    File customsDirCampaign = new File(sCustomsDirCampaign);
	    if(!customsDirCampaign.exists()) {
	    	customsDirCampaign.mkdir();
	    }

	    try {
	        if (newEntity instanceof Mech) {
			    //if this file already exists then don't overwrite it or we will end up with a bunch of copies
				String fileOutName = sCustomsDir + File.separator + fileName + ".mtf";
                String fileNameCampaign = sCustomsDirCampaign + File.separator + fileName + ".mtf";
                if((new File(fileOutName)).exists() || (new File(fileNameCampaign)).exists()) {
                    throw new IOException("A file already exists with the custom name "+fileNameCampaign+". Please choose a different name. (Unit name and/or model)");
                }
	            FileOutputStream out = new FileOutputStream(fileNameCampaign);
	            PrintStream p = new PrintStream(out);
	            p.println(((Mech) newEntity).getMtf());
	            p.close();
	            out.close();
	        } else {
			    //if this file already exists then don't overwrite it or we will end up with a bunch of copies
				String fileOutName = sCustomsDir + File.separator + fileName + ".blk";
                String fileNameCampaign = sCustomsDirCampaign + File.separator + fileName + ".blk";
                if((new File(fileOutName)).exists() || (new File(fileNameCampaign)).exists()) {
                	throw new IOException("A file already exists with the custom name "+fileNameCampaign+". Please choose a different name. (Unit name and/or model)");
                }
	            BLKFile.encode(fileNameCampaign, newEntity);
	        }
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }
	    oldUnit.campaign.addCustom(newEntity.getChassis() + " " + newEntity.getModel());
	    MechSummaryCache.getInstance().loadMechData();
	    //I need to change the new entity to the one from the mtf file now, so that equip
	    //nums will match
	    MechSummary summary = MechSummaryCache.getInstance().getMech(newEntity.getChassis() + " " + newEntity.getModel());
		if(null == summary) {
		    throw(new EntityLoadingException());
		}
	    //try {
            newEntity = new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
		/*} catch (EntityLoadingException ex) {
			Logger.getLogger(CampaignGUI.class.getName())
					.log(Level.SEVERE, null, ex);
		}*/

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
		case CLASS_OMNI:
			return -2;
		default:
			return 1;
		}
	}

	@Override
	public TargetRoll getAllMods() {
		TargetRoll mods = new TargetRoll(getDifficulty(), "difficulty");
		mods.append(oldUnit.getSiteMod());
		if(oldUnit.getEntity().hasQuirk("easy_maintain")) {
			mods.addModifier(-1, "easy to maintain");
		}
		else if(oldUnit.getEntity().hasQuirk("difficult_maintain")) {
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
	public String getAcquisitionName() {
		return getPartName();
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
	public UUID getAssignedTeamId() {
		return assignedTechId;
	}

	@Override
	public void setTeamId(UUID id) {
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
	public MissingPart getMissingPart() {
		//not applicable
		return null;
	}

	@Override
	public String getDesc() {
		return newEntity.getModel() + " " + getDetails();
	}

	@Override
	public String getDetails() {
		return "(" + getRefitClassName() + "/" + getTimeLeft() + " minutes/" + Utilities.getCurrencyString(getCost()) + ")";
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

	@Override
	public void writeToXml(PrintWriter pw1, int indentLvl) {
		pw1.println(MekHqXmlUtil.indentStr(indentLvl) + "<refit>");
		pw1.println(MekHqXmlUtil.writeEntityToXmlString(newEntity, indentLvl+1, oldUnit.campaign.getEntities()));
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<time>"
				+ time + "</time>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<timeSpent>" + timeSpent
				+ "</timeSpent>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<refitClass>" + refitClass
				+ "</refitClass>");
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
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<sameArmorType>" + sameArmorType
				+ "</sameArmorType>");
		if(null != assignedTechId) {
			pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<assignedTechId>" + assignedTechId.toString()
					+ "</assignedTechId>");
		}
		pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                +"<quantity>"
                +quantity
                +"</quantity>");
        pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                +"<daysToWait>"
                +daysToWait
                +"</daysToWait>");
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
			p.writeToXml(pw1, indentLvl+2);
		}
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "</shoppingList>");
		if(null != newArmorSupplies) {
			if(newArmorSupplies.getId() == 0) {
				pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<newArmorSupplies>");
				newArmorSupplies.writeToXml(pw1, indentLvl+2);
				pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "</newArmorSupplies>");
			} else {
				pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<newArmorSuppliesId>" + newArmorSupplies.getId()
						+ "</newArmorSuppliesId>");
			}
		}
		pw1.println(MekHqXmlUtil.indentStr(indentLvl) + "</refit>");
	}

	public static Refit generateInstanceFromXML(Node wn, Unit u, Version version) {
		Refit retVal = new Refit();
		retVal.oldUnit = u;

		NodeList nl = wn.getChildNodes();

		try {
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);

				if (wn2.getNodeName().equalsIgnoreCase("time")) {
					retVal.time = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("refitClass")) {
					retVal.refitClass = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("timeSpent")) {
					retVal.timeSpent = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("quantity")) {
                    retVal.quantity = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("daysToWait")) {
                    retVal.daysToWait = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("cost")) {
					retVal.cost = Long.parseLong(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("newArmorSuppliesId")) {
					retVal.newArmorSuppliesId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("assignedTechId")) {
					if(version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
						retVal.oldTechId = Integer.parseInt(wn2.getTextContent());
					} else {
						retVal.assignedTechId = UUID.fromString(wn2.getTextContent());
					}
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
				} else if (wn2.getNodeName().equalsIgnoreCase("sameArmorType")) {
					if (wn2.getTextContent().equalsIgnoreCase("true"))
						retVal.sameArmorType = true;
					else
						retVal.sameArmorType = false;
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
					processShoppingList(retVal, wn2, retVal.oldUnit, version);
				} else if (wn2.getNodeName().equalsIgnoreCase("newArmorSupplies")) {
					processArmorSupplies(retVal, wn2, version);
				}
			}
		} catch (Exception ex) {
			// Doh!
			MekHQ.logError(ex);
		}

		return retVal;
	}

	private static void processShoppingList(Refit retVal, Node wn, Unit u, Version version) {

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
			Part p = Part.generateInstanceFromXML(wn2, version);
			p.setUnit(u);

			if (p != null) {
				retVal.shoppingList.add(p);
			}
		}
	}

	private static void processArmorSupplies(Refit retVal, Node wn, Version version) {

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
			Part p = Part.generateInstanceFromXML(wn2, version);

			if (p != null && p instanceof Armor) {
				retVal.newArmorSupplies = (Armor)p;
				break;
			}
		}
	}

	public void reCalc() {
	    setCampaign(oldUnit.campaign);
		for(Part p : shoppingList) {
			p.setCampaign(oldUnit.campaign);
		}
		if(null != newArmorSupplies) {
			newArmorSupplies.setCampaign(oldUnit.campaign);
		}
	}

	@Override
	public Part getNewEquipment() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAcquisitionDesc() {
		return "Fill this in";
	}

	public long getStickerPrice() {
		return cost;
	}

	@Override
	public long getBuyCost() {
	    return getStickerPrice();
	}

	@Override
	public String find(int transitDays) {
	    //TODO: check somewhere before this if they can afford it
        ArrayList<Part> newShoppingList = new ArrayList<Part>();
		for(Part part : shoppingList) {
			if(part instanceof Armor || part instanceof ProtomekArmor || part instanceof BaArmor) {
				oldUnit.campaign.addPart(part, transitDays);
				part.setUnit(oldUnit);
				part.setRefitId(oldUnit.getId());
				newUnitParts.add(part.getId());
			}
			else if(part instanceof AmmoBin) {
				oldUnit.campaign.addPart(part, 0);
				newUnitParts.add(part.getId());
				AmmoBin bin = (AmmoBin)part;
                part.setRefitId(oldUnit.getId());
				part.setUnit(oldUnit);
				bin.setShotsNeeded(bin.getFullShots());
				bin.loadBin(false);
				if(bin.needsFixing()) {
	                oldUnit.campaign.buyPart(bin.getNewPart(), 1.1, transitDays);
					bin.loadBin(false);
				}
				part.setUnit(null);
			}
			else if(part instanceof MissingPart) {
				oldUnit.campaign.buyPart((Part)((IAcquisitionWork)part).getNewEquipment(), 1.1, transitDays);
				newShoppingList.add((MissingPart)part);
			}
		}
		shoppingList = newShoppingList;
		if(null != newArmorSupplies) {
		    int amount = armorNeeded - newArmorSupplies.getAmount();
		    if(amount > 0) {
    		    Armor a = (Armor)newArmorSupplies.getNewPart();
    		    a.setAmount(amount);
    			oldUnit.campaign.buyPart(a, 1.1, transitDays);
		    }
		}
		shoppingList = new ArrayList<Part>();
		kitFound = true;
		return "<font color='green'><b> refit kit found.</b> Kit will arrive in " + transitDays + " days.</font>";
	}

	@Override
	public String failToFind() {
	    resetDaysToWait();
		return "<font color='red'> refit kit not found.</font>";

	}

	@Override
	public TargetRoll getAllAcquisitionMods() {
        TargetRoll roll = new TargetRoll();
		int avail = EquipmentType.RATING_A;
		int techBaseMod = 0;
		for(Part part : shoppingList) {
		    if(getTechBase() == T_CLAN && campaign.getCampaignOptions().getClanAcquisitionPenalty() > techBaseMod) {
	            techBaseMod = campaign.getCampaignOptions().getClanAcquisitionPenalty();
	        }
	        else if(getTechBase() == T_IS && campaign.getCampaignOptions().getIsAcquisitionPenalty() > techBaseMod) {
                techBaseMod = campaign.getCampaignOptions().getIsAcquisitionPenalty();
	        }
	        else if(getTechBase() == T_BOTH) {
	            int penalty = Math.min(campaign.getCampaignOptions().getClanAcquisitionPenalty(), campaign.getCampaignOptions().getIsAcquisitionPenalty());
	            if(penalty > techBaseMod) {
	                techBaseMod = penalty;
	            }
	        }
		    avail = Math.max(avail, part.getAvailability(campaign.getEra()));
		}
		if(techBaseMod > 0) {
            roll.addModifier(techBaseMod, "tech limit");
		}
		int availabilityMod = Availability.getAvailabilityModifier(avail);
        roll.addModifier(availabilityMod, "availability (" + EquipmentType.getRatingName(avail) + ")");
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

	@Override
	public int getTechLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTechBase() {
	    return Part.T_BOTH;
	}

	public void fixIdReferences(Hashtable<Integer, UUID> uHash, Hashtable<Integer, UUID> pHash) {
		assignedTechId = pHash.get(oldTechId);
		if(null != newArmorSupplies) {
			newArmorSupplies.fixIdReferences(uHash, pHash);
		}
		for(Part p : shoppingList) {
			p.fixIdReferences(uHash, pHash);
		}
	}

	@Override
	public void resetRepairStatus() {
		// do nothing

	}

	@Override
	public boolean isRightTechType(String skillType) {
		// TODO Auto-generated method stub
		return true;
	}

	public void suggestNewName() {
		if(newEntity instanceof Infantry && !(newEntity instanceof BattleArmor)) {
			Infantry infantry = (Infantry)newEntity;
			String chassis = "?";
			switch (infantry.getMovementMode()) {
	        case INF_UMU:
	            chassis = "Scuba ";
	            break;
	        case INF_MOTORIZED:
	        	chassis = "Motorized ";
	            break;
	        case INF_JUMP:
	        	chassis = "Jump ";
	            break;
	        case HOVER:
	        	chassis = "Mechanized Hover ";
	            break;
	        case WHEELED:
	        	chassis = "Mechanized Wheeled ";
	            break;
	        case TRACKED:
	        	chassis = "Mechanized Tracked ";
	            break;
	        default:
	        	chassis = "Foot ";
			}
			if(infantry.isSquad()) {
				chassis += "Squad";
			} else {
				chassis += "Platoon";
			}
			newEntity.setChassis(chassis);
			String model = "?";
			if(infantry.getSecondaryN() > 1 && null != infantry.getSecondaryWeapon()) {
				model = "(" + infantry.getSecondaryWeapon().getInternalName() + ")";
			} else if(null != infantry.getPrimaryWeapon()) {
				model = "(" + infantry.getPrimaryWeapon().getInternalName() + ")";
			}
			newEntity.setModel(model);
		} else {
			//newEntity.setModel(oldUnit.getEntity().getModel() + " Mk II");
		}
	}

	private void assignArmActuators() {
	    if(!(oldUnit.getEntity() instanceof BipedMech)) {
	        return;
	    }
	    BipedMech m = (BipedMech)oldUnit.getEntity();
	    //we only need to worry about lower arm actuators and hands
	    Part rightLowerArm = null;
	    Part leftLowerArm = null;
	    Part rightHand = null;
	    Part leftHand = null;
	    MekActuator missingHand1 = null;
	    MekActuator missingHand2 = null;
	    MekActuator missingArm1 = null;
	    MekActuator missingArm2 = null;
	    for(Part part : oldUnit.getParts()) {
	        if(part instanceof MekActuator || part instanceof MissingMekActuator) {
	            int type = -1;
                int loc = -1;
                if(part instanceof MekActuator) {
                    type = ((MekActuator)part).getType();
                    loc = ((MekActuator)part).getLocation();
                } else {
                    type = ((MissingMekActuator)part).getType();
                    loc = ((MissingMekActuator)part).getLocation();
                }
	            if(type == Mech.ACTUATOR_LOWER_ARM) {
                    if(loc == Mech.LOC_RARM) {
                        rightLowerArm = part;
                    } else if(loc == Mech.LOC_LARM) {
                        leftLowerArm = part;
                    } else if(null == missingArm1 && part instanceof MekActuator) {
                        missingArm1 = (MekActuator)part;
                    } else if(part instanceof MekActuator) {
                        missingArm2 = (MekActuator)part;
                    }
                } else if(type == Mech.ACTUATOR_HAND) {
                    if(loc == Mech.LOC_RARM) {
                        rightHand = part;
                    } else if(loc == Mech.LOC_LARM) {
                        leftHand = part;
                    } else if(null == missingHand1 && part instanceof MekActuator) {
                        missingHand1 = (MekActuator)part;
                    } else if(part instanceof MekActuator) {
                        missingHand2 = (MekActuator)part;
                    }
                }
	        }
	    }
	    //ok now check all the conditions, assign right hand stuff first
	    if(null == rightHand && m.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)) {
	        MekActuator part = missingHand1;
	        if(null == part || part.getLocation() != Entity.LOC_NONE) {
	            part = missingHand2;
	        }
	        if(null != part) {
	            part.setLocation(Mech.LOC_RARM);
	        }
	    }
	    if(null == leftHand && m.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)) {
            MekActuator part = missingHand1;
            if(null == part || part.getLocation() != Entity.LOC_NONE) {
                part = missingHand2;
            }
            if(null != part) {
                part.setLocation(Mech.LOC_LARM);
            }
        }
	    if(null == rightLowerArm && m.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM)) {
            MekActuator part = missingArm1;
            if(null == part || part.getLocation() != Entity.LOC_NONE) {
                part = missingArm2;
            }
            if(null != part) {
                part.setLocation(Mech.LOC_RARM);
            }
        }
        if(null == leftLowerArm && m.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)) {
            MekActuator part = missingArm1;
            if(null == part || part.getLocation() != Entity.LOC_NONE) {
                part = missingArm2;
            }
            if(null != part) {
                part.setLocation(Mech.LOC_LARM);
            }
        }
	}

    @Override
    public String getShoppingListReport(int quantity) {
        return getAcquisitionName() + " has been added to the procurement list.";
    }

    @Override
    public double getTonnage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getTechRating() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getAvailability(int era) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // TODO Auto-generated method stub

    }

    @Override
    public Part clone() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isCustomJob() {
        return customJob;
    }

    public boolean kitFound() {
        return kitFound;
    }

	@Override
	public String getLocationName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLocation() {
		return Entity.LOC_NONE;
	}
	
	@Override
	public int getIntroDate() {
		return EquipmentType.DATE_NONE;
	}

	@Override
	public int getExtinctDate() {
		return EquipmentType.DATE_NONE;
	}

	@Override
	public int getReIntroDate() {
		return EquipmentType.DATE_NONE;
	}
	
}