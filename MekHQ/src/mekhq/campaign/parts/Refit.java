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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Bay;
import megamek.common.BayType;
import megamek.common.BipedMech;
import megamek.common.ConvFighter;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechAdvancement;
import megamek.common.WeaponType;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.logging.LogLevel;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestAero;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestTank;
import megamek.common.weapons.InfantryAttack;
import megameklab.com.util.UnitUtil;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.MhqFileUtil;
import mekhq.Utilities;
import mekhq.Version;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.UnitRefitEvent;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;
import mekhq.campaign.parts.equipment.MissingAmmoBin;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;

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
	private boolean isRefurbishing;
	private boolean kitFound;
	private String fixableString;

	private List<Integer> oldUnitParts;
	private List<Integer> newUnitParts;
	private List<Part> shoppingList;
	private List<Part> oldIntegratedHS;
	private List<Part> newIntegratedHS;

	private int armorNeeded;
	private Armor newArmorSupplies;
	private int newArmorSuppliesId;
	private boolean sameArmorType;

	private UUID assignedTechId;
	private int oldTechId = -1;

	public Refit() {
		oldUnitParts = new ArrayList<>();
		newUnitParts = new ArrayList<>();
		shoppingList = new ArrayList<>();
		oldIntegratedHS = new ArrayList<>();
		newIntegratedHS = new ArrayList<>();
		fixableString = null;
	}

	public Refit(Unit oUnit, Entity newEn, boolean custom, boolean refurbish) {
	    this();
	    isRefurbishing = refurbish;
		customJob = custom;
		oldUnit = oUnit;
		newEntity = newEn;
        newEntity.setOwner(oldUnit.getEntity().getOwner());
		newEntity.setGame(oldUnit.getEntity().getGame());
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

	public List<Part> getShoppingList() {
		return shoppingList;
	}

	public String[] getShoppingListDescription() {
		Hashtable<String,Integer> tally = new Hashtable<String,Integer>();
	    Hashtable<String,String> desc = new Hashtable<String,String>();
		for(Part p : shoppingList) {
		    if(p instanceof Armor) {
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
        final String METHOD_NAME = "calculate()"; //$NON-NLS-1$
	    
		Unit newUnit = new Unit(newEntity, oldUnit.campaign);
		newUnit.initializeParts(false);
		refitClass = NO_CHANGE;
		boolean isOmniRefit = oldUnit.getEntity().isOmni() && newEntity.isOmni();
		if (isOmniRefit && !Utilities.isOmniVariant(oldUnit.getEntity(), newEntity)) {
            fixableString = "A unit loses omni capabilities if any fixed equipment is modified.";
            return;
		}
		time = 0;
		sameArmorType = newEntity.getArmorType(0) == oldUnit.getEntity().getArmorType(0);
		int recycledArmorPoints = 0;
		boolean replacingLocations = false;
		boolean[] locationHasNewStuff = new boolean[Math.max(newEntity.locations(), oldUnit.getEntity().locations())];
		boolean[] locationLostOldStuff = new boolean[Math.max(newEntity.locations(), oldUnit.getEntity().locations())];
		HashMap<AmmoType,Integer> ammoNeeded = new HashMap<AmmoType,Integer>();
		HashMap<AmmoType,Integer> ammoRemoved = new HashMap<AmmoType,Integer>();
		ArrayList<Part> newPartList = new ArrayList<Part>();

		//Step 1: put all of the parts from the current unit into a new arraylist so they can
		//be removed when we find a match.
		for(Part p : oldUnit.getParts()) {
		    if ((!isOmniRefit || p.isOmniPodded())
		            || (p instanceof TransportBayPart)) {
		        oldUnitParts.add(p.getId());
		    }
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
		    if (isOmniRefit && !part.isOmniPodded()) {
		        continue;
		    }
			boolean partFound = false;
			Part movedPart = null;
			int moveIndex = 0;
			int i = -1;
			for(int pid : oldUnitParts) {
				Part oPart = oldUnit.campaign.getPart(pid);
				i++;
				if (isOmniRefit && !oPart.isOmniPodded()) {
				    continue;
				}
				//FIXME: There have been instances of null oParts here. Save/load will fix these, but
				//I would like to figure out the source. From experimentation, I think it has to do with
				//cancelling a prior refit.
				if ((oPart instanceof MissingPart && ((MissingPart)oPart).isAcceptableReplacement(part, true))
						|| oPart.isSamePartType(part)
						// We're not going to require replacing the life support system just because the
						// number of bay personnel changes.
						|| ((oPart instanceof AeroLifeSupport)
						        && (part instanceof AeroLifeSupport)
						        && (!crewSizeChanged()))) {
					//need a special check for location and armor amount for armor
					if(oPart instanceof Armor
							&& (((Armor)oPart).getLocation() != ((Armor)part).getLocation()
									|| ((Armor)oPart).getTotalAmount() != ((Armor)part).getTotalAmount())) {
						continue;
					}
					if ((oPart instanceof VeeStabiliser)
					        && (oPart.getLocation() != part.getLocation())) {
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
				if (movedPart.getLocation() >= 0) {
				    locationLostOldStuff[movedPart.getLocation()] = true;
				}
				if(isOmniRefit && movedPart.isOmniPodded()) {
					updateRefitClass(CLASS_OMNI);
				} else {
					updateRefitClass(CLASS_C);
				}
				if(movedPart instanceof EquipmentPart) {
					//TODO: set this as salvaging
					//boolean isSalvaging = movedPart.isSalvaging();
					//movedPart.setSalvaging(true);
					//movedPart.updateConditionFromEntity(false);
					time += movedPart.getBaseTime();
					//movedPart.setSalvaging(isSalvaging);
				}
			} else {
				//its a new part
				//dont actually add the part iself but rather its missing equivalent
				//except in the case of armor
				if(part instanceof Armor || part instanceof AmmoBin) {
					newPartList.add(part);
				} else {
					Part mPart = part.getMissingPart();
					if(null != mPart) {
						newPartList.add(mPart);
					} else {
					    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
					            "null missing part for " + part.getName() + " during refit calculations"); //$NON-NLS-1$
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
			//TODO: I don't think we need this here anymore
			nPart.setUnit(oldUnit);
			
			//We don't actually want to order new BA suits; we're just pretending that we're altering the
			//existing suits.
			if (nPart instanceof MissingBattleArmorSuit) {
			    continue;
			}

			/*ADD TIMES AND COSTS*/
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
			} else if(nPart instanceof Armor) {
				int totalAmount = ((Armor)nPart).getTotalAmount();
				time += totalAmount * ((Armor)nPart).getBaseTimeFor(newEntity);
				armorNeeded += totalAmount;
				atype = ((Armor)nPart).getType();
				aclan = ((Armor)nPart).isClanTechBase();
				//armor always gets added to the shopping list - it will be checked for differently
				//NOT ANYMORE - I think this is overkill, lets just reuse existing armor parts
				//shoppingList.add(nPart);
			} else if (nPart instanceof AmmoBin) {
				AmmoType type = (AmmoType)((AmmoBin)nPart).getType();
				ammoNeeded.merge(type, type.getShots(), Integer::sum);
				if (nPart instanceof LargeCraftAmmoBin) {
				    // Adding ammo requires base 15 minutes per ton of ammo. Putting in a new
				    // capital missile bay can take weeks.
				    time += 15 * Math.max(1, nPart.getTonnage());
				    shoppingList.add(nPart);
				} else {
				    time += 120;
    				//check for ammo bins in storage to avoid the proliferation of infinite ammo bins
    				MissingAmmoBin mab = (MissingAmmoBin)nPart.getMissingPart();
    				Part replacement = mab.findReplacement(true);
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
    					shoppingList.add(nPart);
    				}
				}
			}

			/*CHECK REFIT CLASS*/
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
			} else if(nPart instanceof Armor) {
				updateRefitClass(CLASS_C);
				locationHasNewStuff[((Armor)nPart).getLocation()] = true;
			} else if(nPart instanceof MissingMekCockpit) {
				updateRefitClass(CLASS_F);
				locationHasNewStuff[Mech.LOC_HEAD] = true;
			}else if(nPart instanceof MissingMekActuator) {
				if(isOmniRefit && nPart.isOmniPoddable()) {
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
					if(isOmniRefit && nPart.isOmniPoddable()) {
						rClass = CLASS_OMNI;
					}
					updateRefitClass(rClass);
					if(matchFound) {
						tempParts.remove(matchIndex);
					}
				}
			}
		}
		
		//if oldUnitParts is not empty we are removing some stuff and so this should
		//be at least a Class A refit
		if(!oldUnitParts.isEmpty()) {
		    if (isOmniRefit) {
		        updateRefitClass(CLASS_OMNI);
		    } else {
		        updateRefitClass(CLASS_A);
		    }
		}
		
		/*
		 * Cargo and transport bays are essentially just open space and while it may take time and materials
		 * to change the cubicles or the number of doors, the bay itself does not require any refit work
		 * unless the size changes. First we create a list of all bays on each unit, then we attempt to
		 * match them by size and number of doors. Any remaining are matched on size, and difference in
		 * number of doors is noted as moving doors has to be accounted for in the time calculation.
		 */
		List<Bay> oldUnitBays = oldUnit.getEntity().getTransportBays().stream()
		        .filter(b -> !b.isQuarters()).collect(Collectors.toList());
		List<Bay> newUnitBays = newEntity.getTransportBays().stream()
                .filter(b -> !b.isQuarters()).collect(Collectors.toList());
		// If any bays keep the same size but have any doors added or removed, we need to note that separately
		// since removing a door from one bay and adding it to another requires time even if the number
		// of parts hasn't changed. We track them separately so that we don't charge time for changing the
		// overall number of doors twice.
		int doorsRemoved = 0;
		int doorsAdded = 0;
		if (oldUnitBays.size() + newUnitBays.size() > 0) {
    		for (Iterator<Bay> oldbays = oldUnitBays.iterator(); oldbays.hasNext(); ) {
                final Bay oldbay = oldbays.next();
                for (Iterator<Bay> newbays = newUnitBays.iterator(); newbays.hasNext(); ) {
                    final Bay newbay = newbays.next();
                    if ((oldbay.getCapacity() == newbay.getCapacity())
                            && (oldbay.getDoors() == newbay.getDoors())) {
                        oldbays.remove();
                        newbays.remove();
                        break;
                    }
                }
    		}
            for (Iterator<Bay> oldbays = oldUnitBays.iterator(); oldbays.hasNext(); ) {
                final Bay oldbay = oldbays.next();
                for (Iterator<Bay> newbays = newUnitBays.iterator(); newbays.hasNext(); ) {
                    final Bay newbay = newbays.next();
                    if (oldbay.getCapacity() == newbay.getCapacity()) {
                        if (oldbay.getDoors() > newbay.getDoors()) {
                            doorsRemoved += oldbay.getDoors() - newbay.getDoors();
                        } else {
                            doorsAdded += newbay.getDoors() - oldbay.getDoors();
                        }
                        oldbays.remove();
                        newbays.remove();
                        break;
                    }
                }
    		}
            // Use bay replacement time of 1 month (30 days) for each bay to be resized,
            // plus another month for any bays to be added or removed.
            time += Math.max(oldUnitBays.size(), newUnitBays.size()) * 14400;
            int deltaDoors = oldUnitBays.stream().mapToInt(Bay::getDoors).sum()
                    - newUnitBays.stream().mapToInt(Bay::getDoors).sum();
            if (deltaDoors < 0) {
                doorsAdded = Math.max(0, doorsAdded - deltaDoors);
            } else {
                doorsRemoved = Math.max(0, doorsRemoved + deltaDoors);
            }
            time += (doorsAdded + doorsRemoved) * 600;
		}
		
		//Step 4: loop through remaining equipment on oldunit parts and add time for removing.
		for(int pid : oldUnitParts) {
			Part oPart = oldUnit.campaign.getPart(pid);
			//We're pretending we're changing the old suit rather than removing it.
			//We also want to avoid accounting for legacy InfantryAttack parts.
			if ((oPart instanceof BattleArmorSuit)
			        || (oPart instanceof TransportBayPart)
			        || ((oPart instanceof EquipmentPart
			                && ((EquipmentPart)oPart).getType() instanceof InfantryAttack))) {
			    continue;
			}
			if (oPart.getLocation() >= 0) {
			    locationLostOldStuff[oPart.getLocation()] = true;
			}
			if(oPart instanceof MissingPart) {
				continue;
			}
			if(oPart instanceof AmmoBin) {
			    int remainingShots = ((AmmoBin)oPart).getFullShots() - ((AmmoBin)oPart).getShotsNeeded();
				if(remainingShots > 0) {
					time += 120;
	                ammoRemoved.merge((AmmoType)((AmmoBin)oPart).getType(), remainingShots,
	                        (a, b) -> a + b);
				}
				continue;
			}
			if(oPart instanceof Armor && sameArmorType) {
				recycledArmorPoints += ((Armor)oPart).getAmount();
				continue;
			}
			boolean isSalvaging = oldUnit.isSalvage();
			oldUnit.setSalvage(true);
			time += oPart.getBaseTime();
			oldUnit.setSalvage(isSalvaging);
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
			double tonnageNeeded = newArmorSupplies.getTonnageNeeded();
			if(null != existingArmorSupplies) {
				tonnageNeeded = Math.max(0, tonnageNeeded - existingArmorSupplies.getTonnage());
			}
			newArmorSupplies.setUnit(oldUnit);
			cost += newArmorSupplies.getStickerPrice() * (tonnageNeeded / 5.0);
			newArmorSupplies.setUnit(null);
		}

		//TODO: use ammo removed from the old unit in the case of changing between full ton and half
		//ton MG or OS/regular.
		for(AmmoType type : ammoNeeded.keySet()) {
			int shotsNeeded = Math.max(ammoNeeded.get(type) - getAmmoAvailable(type), 0);
			int shotsPerTon = type.getShots();
			cost += type.getCost(newEntity, false, -1) * ((double)shotsNeeded/shotsPerTon);
		}
		
		/*
		 * Figure out how many untracked heat sinks are needed to complete the refit or will
		 * be removed. These are engine integrated heat sinks for Mechs or ASFs that change
		 * the heat sink type or heat sinks required for energy weapons for vehicles and
		 * conventional fighters.
		 */
		if ((newEntity instanceof Mech)
		        || ((newEntity instanceof Aero) && !(newEntity instanceof ConvFighter))) {
		    Part oldHS = heatSinkPart(oldUnit.getEntity());
		    Part newHS = heatSinkPart(newEntity);
		    int oldCount = untrackedHeatSinkCount(oldUnit.getEntity());
		    int newCount = untrackedHeatSinkCount(newEntity);
		    if (oldHS.isSamePartType(newHS)) {
		        // If the number changes we need to add them to either the warehouse at the end of
		        // refit or the shopping list at the beginning.
                for (int i = 0; i < oldCount - newCount; i++) {
                    oldIntegratedHS.add(oldHS.clone());
                }
                for (int i = 0; i < newCount - oldCount; i++) {
                    newIntegratedHS.add(oldHS.clone());
                }
		    } else {
                for (int i = 0; i < oldCount; i++) {
                    oldIntegratedHS.add(oldHS.clone());
                }
                for (int i = 0; i < newCount; i++) {
                    newIntegratedHS.add(newHS.clone());
                }
                updateRefitClass(CLASS_D);
		    }
		} else if ((newEntity instanceof Tank)
		        || (newEntity instanceof ConvFighter)) {
		    int oldHS = untrackedHeatSinkCount(oldUnit.getEntity());
		    int newHS = untrackedHeatSinkCount(newEntity);
		    // We're only concerned with heat sinks that have to be installed in excess of what
		    // may be provided by the engine.
		    if (oldUnit.getEntity().hasEngine()) {
		        oldHS = Math.max(0, oldHS - oldUnit.getEntity().getEngine().integralHeatSinkCapacity(false));
		    }
		    if (newEntity.hasEngine()) {
		        newHS = Math.max(0, newHS - newEntity.getEngine().integralHeatSinkCapacity(false));
		    }
		    if (oldHS != newHS) {
		        Part hsPart = heatSinkPart(newEntity); // only single HS allowed, so they have to be of the same type
                for (int i = oldHS; i < newHS; i++) {
                    newIntegratedHS.add(hsPart.clone());
                }
                for (int i = newHS; i < oldHS; i++) {
                    oldIntegratedHS.add(hsPart.clone());
                }
		    }
		}
		time += (oldIntegratedHS.size() + newIntegratedHS.size()) * 90;
		shoppingList.addAll(newIntegratedHS);
		
		//check for CASE
		//TODO: we still dont have to order the part, we need to get the CASE issues sorted out
		for(int loc = 0; loc < newEntity.locations(); loc++) {
			if((newEntity.locationHasCase(loc) != oldUnit.getEntity().locationHasCase(loc)
					&& !(newEntity.isClan() && newEntity instanceof Mech))
					|| (newEntity instanceof Mech
							&& ((Mech)newEntity).hasCASEII(loc) != ((Mech)oldUnit.getEntity()).hasCASEII(loc))) {
				if(isOmniRefit) {
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
			for(int loc = 0; loc < newEntity.locations(); loc++) {
				if(locationHasNewStuff[loc] || locationLostOldStuff[loc]) {
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

		// Now we set the refurbishment values
        if (isRefurbishing) {
            refitClass = CLASS_E;

            if (newEntity instanceof megamek.common.Warship || newEntity instanceof megamek.common.SpaceStation) {
                time = 40320;
            } else if (newEntity instanceof megamek.common.Dropship || newEntity instanceof megamek.common.Jumpship) {
                time = 13440;
            } else if (newEntity instanceof Mech || newEntity instanceof megamek.common.Aero || newEntity instanceof megamek.common.ConvFighter || newEntity instanceof megamek.common.SmallCraft) {
                time = 6720;
            } else if (newEntity instanceof BattleArmor || newEntity instanceof megamek.common.Tank || newEntity instanceof megamek.common.Protomech) {
                time = 3360;
            } else {
                time = 1111;
                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                        "Unit " + newEntity.getModel() + " did not set its time correctly."); //$NON-NLS-1$
            }

            // The cost is equal to 10 percent of the units base value (not modified for quality).
            cost = (long) (oldUnit.getBuyCost() * .1);
        }
	}

	public void begin() throws EntityLoadingException, IOException {
	    if(customJob) {
            saveCustomization();
        }
		oldUnit.setRefit(this);
		newEntity.setOwner(oldUnit.getEntity().getOwner());
		// We don't want to require waiting for a refit kit if all that is missing is ammo or ammo bins.
		Map<AmmoType,Integer> shotsNeeded = new HashMap<>();
		for (Iterator<Part> iter = shoppingList.iterator(); iter.hasNext(); ) {
		    final Part part = iter.next();
		    if (part instanceof AmmoBin) {
                part.setRefitId(oldUnit.getId());
                part.setUnit(null);
                campaign.addPart(part, 0);
		        newUnitParts.add(part.getId());
		        AmmoBin bin = (AmmoBin) part;
		        bin.setShotsNeeded(bin.getFullShots());
		        bin.loadBin();
		        if (bin.getShotsNeeded() > 0) {
		            shotsNeeded.merge((AmmoType) bin.getType(), bin.getShotsNeeded(), Integer::sum);
		        }
		        iter.remove();
		    }
		}
        for (AmmoType atype : shotsNeeded.keySet()) {
            int tons = (int) Math.ceil((double) shotsNeeded.get(atype) / atype.getShots());
            AmmoStorage ammo = new AmmoStorage(0, atype, atype.getShots() * tons, campaign);
            shoppingList.add(ammo);
        }
		reserveNewParts();
		if(customJob) {
		    //add the stuff on the shopping list to the master shopping list
		    ArrayList<Part> newShoppingList = new ArrayList<Part>();
    		for(Part part : shoppingList) {
    			part.setUnit(null);
    			if(part instanceof Armor) {
                    //Taharqa: WE shouldn't be here anymore, given that I am no longer adding
    				//armor by location to the shopping list but instead changing it all via
    				//the newArmorSupplies object, but commented out for completeness
                    //oldUnit.campaign.addPart(part, 0);
                    //part.setRefitId(oldUnit.getId());
                    //newUnitParts.add(part.getId());
                }
                else if(part instanceof AmmoBin) {
                    //ammo bins are free - bleh
                    AmmoBin bin = (AmmoBin)part;
                    bin.setShotsNeeded(bin.getFullShots());
                    part.setRefitId(oldUnit.getId());
                    oldUnit.campaign.addPart(part, 0);
                    newUnitParts.add(part.getId());
                    bin.loadBin();
                    if(bin.needsFixing()) {
                        oldUnit.campaign.getShoppingList().addShoppingItem(bin, 1, oldUnit.campaign);
                        //need to call it a second time to use up if found
                        bin.loadBin();
                    }
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
			for(Part part : shoppingList) {
    			part.setUnit(null);
    			MekHQ.triggerEvent(new PartChangedEvent(part));
			}
		    checkForArmorSupplies();
		    if(shoppingList.isEmpty() && (null == newArmorSupplies || newArmorSupplies.getAmountNeeded() == 0)) {
		    	kitFound = true;
		    } else {
		    	oldUnit.campaign.getShoppingList().addShoppingItem(this, 1, oldUnit.campaign);
		    }
		}

		if (isRefurbishing) {
	        if (campaign.buyRefurbishment(this)) {
	            campaign.addReport("<font color='green'><b>Refurbishment ready to begin</b></font>");
	        } else {
	            campaign.addReport("You cannot afford to refurbish " + oldUnit.getEntity().getShortName() + ". Transaction cancelled");
	        }
	    }
        MekHQ.triggerEvent(new UnitRefitEvent(oldUnit));
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
		for (int pid : newUnitParts) {
			Part part = oldUnit.campaign.getPart(pid);
			if (part == null) {
				if(null == part) {
			        MekHQ.getLogger().log(getClass(), "partsInTransit()", LogLevel.ERROR, //$NON-NLS-1$
			                "part with id " + pid + " not found for refit of " + getDesc()); //$NON-NLS-1$
					continue;
				}
			}
			if (!part.isPresent()) {
				return true;
			}
		}
		if(null != newArmorSupplies && !newArmorSupplies.isPresent()) {
			return true;
		}
		return false;
	}

	public boolean acquireParts() {
	    if(!customJob) {
        	checkForArmorSupplies();
        	return kitFound && !partsInTransit() && (null == newArmorSupplies || (armorNeeded - newArmorSupplies.getAmount()) <= 0);
	    }
		ArrayList<Part> newShoppingList = new ArrayList<Part>();
		for(Part part : shoppingList) {
			if(part instanceof IAcquisitionWork) {
			    //check to see if we found a replacement
			    Part replacement = part;
			    if (part instanceof MissingPart) {
			        replacement = ((MissingPart)part).findReplacement(true);
			    }
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
				bin.loadBin();
				if(bin.needsFixing()) {
					missingAmmo = true;
				}
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

	public int getAmmoAvailable(AmmoType type) {
		for(Part part : oldUnit.campaign.getSpareParts()) {
			if(part instanceof AmmoStorage) {
				AmmoStorage a = (AmmoStorage)part;
				if(a.getType().equals(type)) {
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
        final String METHOD_NAME = "cancel()"; //$NON-NLS-1$

        oldUnit.setRefit(null);
		for(int pid : newUnitParts) {
			Part part = oldUnit.campaign.getPart(pid);
			if(null == part) {
		        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
		                "part with id " + pid + " not found for refit of " + getDesc()); //$NON-NLS-1$
				continue;
			}
			part.setRefitId(null);
			// If the part was not part of the old unit we need to consolidate it with others of its type
			// in the warehouse. Ammo Bins just get unloaded and removed; no reason to keep them around.
			if (part.getUnitId() == null) {
    			if(part instanceof AmmoBin) {
    				((AmmoBin) part).unload();
    				oldUnit.campaign.removePart(part);
    			} else {
    				Part spare = oldUnit.campaign.checkForExistingSparePart(part);
    				if(null != spare) {
    					spare.incrementQuantity();
    					oldUnit.campaign.removePart(part);
    				}
    			}
			}
		}
		/*
		if(null != newArmorSupplies) {
			newArmorSupplies.setRefitId(null);
			newArmorSupplies.setUnit(oldUnit);
			oldUnit.campaign.removePart(newArmorSupplies);
			newArmorSupplies.changeAmountAvailable(newArmorSupplies.getAmount());
		}
		*/
		
		// Remove refit parts from the procurement list. Those which have already been purchased and
		// are in transit are left as is.
		List<IAcquisitionWork> toRemove = new ArrayList<>();
        toRemove.add(this);
		for (IAcquisitionWork part : campaign.getShoppingList().getPartList()) {
		    if ((part instanceof Part) && ((Part) part).getRefitId() == this.getRefitId()) {
		        toRemove.add(part);
		    }
		}
		for (IAcquisitionWork work : toRemove) {
		    campaign.getShoppingList().removeItem(work);
		}
		MekHQ.triggerEvent(new UnitRefitEvent(oldUnit));
	}

	private void complete() {
        final String METHOD_NAME = "complete()"; //$NON-NLS-1$

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
		//add old parts to the warehouse
		for(int pid : oldUnitParts) {
			Part part = oldUnit.campaign.getPart(pid);
			if (part instanceof TransportBayPart) {
			    part.removeAllChildParts();
			}
			if(null == part) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                        "old part with id " + pid + " not found for refit of " + getDesc()); //$NON-NLS-1$
				continue;
			}
			if(part instanceof MekLocation && ((MekLocation)part).getLoc() == Mech.LOC_CT) {
				part.setUnit(null);
				oldUnit.campaign.removePart(part);
				continue;
			}
			// SI Should never be "kept" for the Warehouse
			// We also don't want to generate new BA suits that have been replaced
			// or allow legacy InfantryAttack BA parts to show up in the warehouse.
			else if(part instanceof StructuralIntegrity || part instanceof BattleArmorSuit
			        || (part instanceof TransportBayPart)
			        || (part instanceof EquipmentPart && ((EquipmentPart)part).getType() instanceof InfantryAttack)) {
				part.setUnit(null);
				oldUnit.campaign.removePart(part);
				continue;
			}
			else if(part instanceof Armor) {
				Armor a = (Armor)part;
				//lets just re-use this armor part
				if(!sameArmorType) {
					//give the amount back to the warehouse since we are switching types
					a.changeAmountAvailable(a.getAmount());
					if(null != newArmorSupplies) {
						a.changeType(newArmorSupplies.getType(), newArmorSupplies.isClanTechBase());
					}
				}
				// Removing vehicle turrets or changing BA squad size can reduce the number of armor locations.
				if (part.getLocation() < newEntity.locations()) {
				    newUnitParts.add(pid);
				} else {
				    part.setUnit(null);
				    oldUnit.campaign.removePart(part);
				}
			}
			else {
				if(part instanceof AmmoBin) {
					((AmmoBin) part).unload();
				}
				Part spare = oldUnit.campaign.checkForExistingSparePart(part);
				if(null != spare) {
					spare.incrementQuantity();
					oldUnit.campaign.removePart(part);
				}
			}
			part.setUnit(null);
		}
        // add leftover untracked heat sinks to the warehouse
        for(Part part : oldIntegratedHS) {
            campaign.addPart(part, 0);
        }

		//dont forget to switch entities!
		oldUnit.setEntity(newEntity);
	
		//set up new parts
		ArrayList<Part> newParts = new ArrayList<Part>();
		//We've already made the old suits go *poof*; now we materialize new ones.
		if (newEntity instanceof BattleArmor) {
		    for (int t = BattleArmor.LOC_TROOPER_1; t < newEntity.locations(); t++) {
		        Part suit = new BattleArmorSuit((BattleArmor)newEntity, t, oldUnit.campaign);
		        newParts.add(suit);
		        suit.setUnit(oldUnit);
		    }
		}
		for(int pid : newUnitParts) {
			Part part = oldUnit.campaign.getPart(pid);
			if(null == part) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                        "part with id " + pid + " not found for refit of " + getDesc()); //$NON-NLS-1$
				continue;
			}
			part.setUnit(oldUnit);
			part.setRefitId(null);
			newParts.add(part);
			if(part instanceof Armor) {
				//get amounts correct for armor
				part.updateConditionFromEntity(false);
			}
		}
		oldUnit.setParts(newParts);
		Utilities.unscrambleEquipmentNumbers(oldUnit);
		assignArmActuators();
		assignBayParts();
		for (Part p : newParts) {
		    if (p instanceof AmmoBin) {
		        ((AmmoBin) p).loadBin();
		    }
		}
		if(null != newArmorSupplies) {
			oldUnit.campaign.removePart(newArmorSupplies);
		}
		//in some cases we may have had more armor on the original unit and so we may add more
		//back then we received
		
		//FIXME: This doesn't deal properly with patchwork armor.
		if(sameArmorType && armorNeeded < 0) {
			Armor a = new Armor(0, oldUnit.getEntity().getArmorType(1),
			        -1 * armorNeeded, -1, false, aclan, oldUnit.campaign);
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

		if (isRefurbishing) {
	        for (Part p : oldUnit.getParts()) {
	            if (p.getQuality() != QUALITY_F) {
	                p.improveQuality();
	            }
	        }
	    }
        MekHQ.triggerEvent(new UnitRefitEvent(oldUnit));
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
	public TargetRoll getAllMods(Person tech) {
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
		if (isRefurbishing) {
            return "Refurbishment of " + oldUnit.getEntity().getShortName() + " is complete.";
        } else {
            return "The customization of "+ oldUnit.getEntity().getShortName() + " is complete.";
        }
	}

	@Override
	public String fail(int rating) {
		timeSpent = 0;
		failedCheck = true;
		// Refurbishment doesn't get extra time like standard refits.
        if (isRefurbishing) {
            oldUnit.setRefit(null); // Failed roll results in lost time and money
            return "Refurbishment of " + oldUnit.getEntity().getShortName() + " was unsuccessful";
        }
        else {
            return "The customization of " + oldUnit.getEntity().getShortName() + " will take " + getTimeLeft() + " additional minutes to complete.";
        }
    }

	@Override
	public void resetTimeSpent() {
		timeSpent = 0;
	}

	@Override
	public String getPartName() {
		if(customJob) {
			return newEntity.getShortName() + " Customization";
		} else if (isRefurbishing) {
		    return newEntity.getShortName() + " Refurbishment";
		} else {
			return newEntity.getShortName() + " Refit Kit";
		}
	}

	@Override
	public String getAcquisitionName() {
		return getPartName();
	}

	@Override
	public String getName() {
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
	public UUID getTeamId() {
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
	
	/**
	 * Requiring the life support system to be changed just because the number of bay personnel changes
	 * is a bit much. Instead we'll limit it to changes in crew size, measured by quarters. 
	 * @return true if the crew quarters capacity changed.
	 */
	private boolean crewSizeChanged() {
        int oldCrew = oldUnit.getEntity().getTransportBays()
                .stream().filter(b -> b.isQuarters())
                .mapToInt(b -> (int) b.getCapacity())
                .sum();
        int newCrew = newEntity.getTransportBays()
                .stream().filter(b -> b.isQuarters())
                .mapToInt(b -> (int) b.getCapacity())
                .sum();
        return oldCrew != newCrew;
	}

	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
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
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<isRefurbishing>" + isRefurbishing
		        + "</isRefurbishing>");
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
        final String METHOD_NAME = "generateInstanceFromXML(Node,Unit,Version)"; //$NON-NLS-1$

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
				} else if (wn2.getNodeName().equalsIgnoreCase("isRefurbishing")) {
                    if (wn2.getTextContent().equalsIgnoreCase("true"))
                        retVal.isRefurbishing = true;
                    else 
                        retVal.isRefurbishing = false;
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
            MekHQ.getLogger().error(Refit.class, METHOD_NAME, ex);
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
                MekHQ.getLogger().log(Refit.class, "processShoppingList(Refit,Node,Unit,Version)", LogLevel.ERROR, //$NON-NLS-1$
                        "Unknown node type not loaded in Part nodes: " + wn2.getNodeName()); //$NON-NLS-1$
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
                MekHQ.getLogger().log(Refit.class, "processArmorSupplies(Refit,Node,Version)", LogLevel.ERROR, //$NON-NLS-1$
                        "Unknown node type not loaded in Part nodes: " + wn2.getNodeName()); //$NON-NLS-1$

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
		return this;
	}

	@Override
	public String getAcquisitionDesc() {
		return "Fill this in";
	}
	
    @Override
    public String getAcquisitionDisplayName() {
    	return null;
    }    

	@Override
	public String getAcquisitionExtraDesc() {
    	return null;
	}

	@Override
    public String getAcquisitionBonus() {
    	return null;
    }

	@Override
	public Part getAcquisitionPart() {
		return null;
	}

	public long getStickerPrice() {
		return cost;
	}

	@Override
	public long getBuyCost() {
	    return getStickerPrice();
	}

	public void addRefitKitParts(int transitDays) {
		for (Part part : shoppingList) {
			if (part instanceof AmmoBin) {
                part.setRefitId(oldUnit.getId());
				oldUnit.campaign.addPart(part, 0);
				newUnitParts.add(part.getId());
				AmmoBin bin = (AmmoBin)part;
				bin.setShotsNeeded(bin.getFullShots());
				bin.loadBin();
				if(bin.needsFixing()) {
	                oldUnit.campaign.addPart(bin.getNewPart(), transitDays);
					bin.loadBin();
				}
			} else if (part instanceof MissingPart) {
				Part newPart = (Part)((IAcquisitionWork)part).getNewEquipment();
				newPart.setRefitId(oldUnit.getId());
				oldUnit.campaign.addPart(newPart, transitDays);
				newUnitParts.add(newPart.getId());
			} else if (part instanceof AmmoStorage) {
			    part.setUnit(null);
                part.setRefitId(oldUnit.getId());
                campaign.addPart(part, transitDays);
			}
		}
		if(null != newArmorSupplies) {
		    int amount = armorNeeded - newArmorSupplies.getAmount();
		    if(amount > 0) {
    		    Armor a = (Armor)newArmorSupplies.getNewPart();
    		    a.setAmount(amount);
    			oldUnit.campaign.addPart(a, transitDays);
		    }
		    checkForArmorSupplies();
		}
		shoppingList = new ArrayList<Part>();
		kitFound = true;
	}

	@Override
	public String find(int transitDays) {
		if(campaign.buyPart(this, transitDays)) {
			return "<font color='green'><b> refit kit found.</b> Kit will arrive in " + transitDays + " days.</font>";
		} else {
		    return "<font color='red'><b> You cannot afford this refit kit. Transaction cancelled</b>.</font>";
		}
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
		    avail = Math.max(avail, part.getAvailability());
		}
		if(techBaseMod > 0) {
            roll.addModifier(techBaseMod, "tech limit");
		}
		int availabilityMod = Availability.getAvailabilityModifier(avail);
        roll.addModifier(availabilityMod, "availability (" + ITechnology.getRatingName(avail) + ")");
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

	public void fixIdReferences(Map<Integer, UUID> uHash, Map<Integer, UUID> pHash) {
		assignedTechId = pHash.get(oldTechId);
		if(null != newArmorSupplies) {
			newArmorSupplies.fixIdReferences(uHash, pHash);
		}
		for(Part p : shoppingList) {
			p.fixIdReferences(uHash, pHash);
		}
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
	
	/**
	 * Assigns bay doors and cubicles as child parts of the bay part. We also need to make sure the
	 * bay number of the parts match up to the Entity. The easiest way to do that is to remove all
	 * the bay parts and create new ones from scratch. Then we assign doors and cubicles.
	 */
	private void assignBayParts() {
	    final Entity entity = oldUnit.getEntity();

	    List<Part> doors = new ArrayList<>();
	    Map<BayType, List<Part>> cubicles = new HashMap<>();
	    List<Part> oldBays = new ArrayList<>();
	    for (Part p : oldUnit.getParts()) {
	        if (p instanceof BayDoor) {
	            doors.add(p);
	        } else if (p instanceof Cubicle) {
	            cubicles.putIfAbsent(((Cubicle) p).getBayType(), new ArrayList<>());
	            cubicles.get(((Cubicle) p).getBayType()).add(p);
	        } else if (p instanceof TransportBayPart) {
	            oldBays.add(p);
	        }
	    }
	    oldBays.forEach(p -> p.remove(false));
	    for (Bay bay : entity.getTransportBays()) {
	        if (bay.isQuarters()) {
	            continue;
	        }
	        BayType btype = BayType.getTypeForBay(bay);
	        Part bayPart = new TransportBayPart((int) oldUnit.getEntity().getWeight(),
	                bay.getBayNumber(), bay.getCapacity(), campaign);
	        oldUnit.addPart(bayPart);
	        oldUnit.campaign.addPart(bayPart, 0);
	        for (int i = 0; i < bay.getDoors(); i++) {
	            Part door;
	            if (doors.size() > 0) {
	                door = doors.remove(0);
	            } else {
	                // This shouldn't ever happen
	                door = new MissingBayDoor((int) entity.getWeight(), campaign);
	                oldUnit.addPart(door);
	                oldUnit.campaign.addPart(door, 0);
	            }
	            bayPart.addChildPart(door);
	        }
	        if (btype.getCategory() == BayType.CATEGORY_NON_INFANTRY) {
	            for (int i = 0; i < bay.getCapacity(); i++) {
	                Part cubicle;
	                if (cubicles.containsKey(btype) && (cubicles.get(btype).size() > 0)) {
	                    cubicle = cubicles.get(btype).remove(0);
	                } else {
	                    cubicle = new MissingCubicle((int) entity.getWeight(), btype, campaign);
	                    oldUnit.addPart(cubicle);
	                    oldUnit.campaign.addPart(cubicle, 0);
	                }
	                bayPart.addChildPart(cubicle);
	            }
	        }
	    }
	}
	
	/**
	 * Refits may require adding or removing heat sinks that are not tracked as parts. For Mechs and
	 * ASFs this would be engine-integrated heat sinks if the heat sink type is changed. For vehicles and
	 * conventional fighters this would be heat sinks required by energy weapons.
	 * 
	 * @param entity Either the starting or the ending unit of the refit.
	 * @return       The number of heat sinks the unit mounts that are not tracked as parts.
	 */
	private int untrackedHeatSinkCount(Entity entity) {
	    if (entity instanceof Mech) {
	        return Math.min(((Mech) entity).heatSinks(), entity.getEngine().integralHeatSinkCapacity(((Mech) entity).hasCompactHeatSinks()));
	    } else if ((entity instanceof Aero)
	            && (entity.getEntityType() & (Entity.ETYPE_CONV_FIGHTER | Entity.ETYPE_SMALL_CRAFT | Entity.ETYPE_JUMPSHIP)) == 0) {
	        return entity.getEngine().integralHeatSinkCapacity(false);
	    } else {
	        EntityVerifier verifier = EntityVerifier.getInstance(new File(
                    "data/mechfiles/UnitVerifierOptions.xml"));
	        TestEntity te = null;
	        if (entity instanceof Tank) {
	            te = new TestTank((Tank) entity, verifier.tankOption, null);
	            return te.getCountHeatSinks();
	        } else if (entity instanceof ConvFighter) {
	            te = new TestAero((Aero) entity, verifier.aeroOption, null);
                return te.getCountHeatSinks();
	        } else {
	            return 0;
	        }
	    }
	}
	
	/**
	 * Creates an independent heat sink part appropriate to the unit that can be used to track
	 * needed and leftover parts for heat sinks that are not actually tracked by the unit.
	 * 
	 * @param entity Either the original or the new unit.
	 * @return       The part corresponding to the type of heat sink for the unit.
	 */
	private Part heatSinkPart(Entity entity) {
	    if (entity instanceof Aero) {
	        return new AeroHeatSink(0, ((Aero) entity).getHeatType(), false, campaign);
	    } else if (entity instanceof Mech) {
	        Optional<Mounted> mount = entity.getMisc().stream()
	                .filter(m -> m.getType().hasFlag(MiscType.F_HEAT_SINK)
	                        || m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK))
	                .findAny();
	        if (mount.isPresent()) {
	            return new HeatSink(0, mount.get().getType(), -1, false, campaign);
	        }
	    }
	    return new HeatSink(0, EquipmentType.get("Heat Sink"), -1, false, campaign);
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
	public TechAdvancement getTechAdvancement() {
	    return TA_GENERIC;
	}

	public boolean isBeingRefurbished() {
        return isRefurbishing;
    }

    @Override
    public boolean isIntroducedBy(int year, boolean clan, int techFaction) {
        return getIntroductionDate(clan, techFaction) <= year;
    }

    @Override
    public boolean isExtinctIn(int year, boolean clan, int techFaction) {
        return isExtinct(year, clan, techFaction);
    }
}