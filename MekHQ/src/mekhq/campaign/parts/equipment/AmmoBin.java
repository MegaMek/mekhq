/*
 * AmmoBin.java
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

package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.Jumpship;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.TargetRoll;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Availability;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Era;
import mekhq.campaign.work.IAcquisitionWork;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class AmmoBin extends EquipmentPart implements IAcquisitionWork {
	private static final long serialVersionUID = 2892728320891712304L;

	public static final Integer[] ALLOWED_BY_TYPE_ARRAY = { AmmoType.T_LRM, AmmoType.T_LRM_PRIMITIVE, AmmoType.T_LRM_STREAK, AmmoType.T_LRM_TORPEDO,
	    AmmoType.T_LRM_TORPEDO_COMBO, AmmoType.T_SRM, AmmoType.T_SRM_ADVANCED, AmmoType.T_SRM_PRIMITIVE, AmmoType.T_SRM_STREAK, AmmoType.T_SRM_TORPEDO,
	    AmmoType.T_MRM, AmmoType.T_MRM_STREAK, AmmoType.T_ROCKET_LAUNCHER, AmmoType.T_EXLRM, AmmoType.T_PXLRM, AmmoType.T_HSRM, AmmoType.T_MML,
	    AmmoType.T_NLRM };
	public static final HashSet<Integer> ALLOWED_BY_TYPE = new HashSet<Integer>(Arrays.asList(ALLOWED_BY_TYPE_ARRAY));

	protected long munition;
	protected int shotsNeeded;
	protected boolean checkedToday;
	protected boolean oneShot;

    public AmmoBin() {
    	this(0, null, -1, 0, false, false, null);
    }

    public AmmoBin(int tonnage, EquipmentType et, int equipNum, int shots, boolean singleShot,
            boolean omniPodded, Campaign c) {
        super(tonnage, et, equipNum, omniPodded, c);
        this.shotsNeeded = shots;
        this.oneShot = singleShot;
        this.checkedToday = false;
        if(null != type && type instanceof AmmoType) {
        	this.munition = ((AmmoType)type).getMunitionType();
        }
        if(null != name) {
        	this.name += " Bin";
        }
    }

    public AmmoBin clone() {
    	AmmoBin clone = new AmmoBin(getUnitTonnage(), getType(), getEquipmentNum(), shotsNeeded, oneShot,
    	        omniPodded, campaign);
        clone.copyBaseData(this);
        clone.shotsNeeded = this.shotsNeeded;
        clone.munition = this.munition;
        return clone;
    }
    
    /* Per TM, ammo for fighters is stored in the fuselage. This makes a difference for omnifighter
     * pod space, so we're going to stick them in LOC_NONE where the heat sinks are */ 
    @Override
    public String getLocationName() {
        if (unit.getEntity() instanceof Aero
                && !((unit.getEntity() instanceof SmallCraft) || (unit.getEntity() instanceof Jumpship))){
            return "Fuselage";
        }
        return super.getLocationName();
    }
    
    @Override
    public int getLocation() {
        if (unit.getEntity() instanceof Aero
                && !((unit.getEntity() instanceof SmallCraft) || (unit.getEntity() instanceof Jumpship))){
            return Aero.LOC_NONE;
        }
        return super.getLocation();
    }
    
    @Override
    public boolean isInLocation(String loc) {
        if (unit.getEntity() instanceof Aero
                && !((unit.getEntity() instanceof SmallCraft) || (unit.getEntity() instanceof Jumpship))){
            return loc.equals("FSLG");
        }
        return super.isInLocation(loc);
    }

    @Override
    public double getTonnage() {
    	return (1.0 * getFullShots())/((AmmoType)type).getShots();
    }

    public int getFullShots() {
    	int fullShots = ((AmmoType)type).getShots();
    	if(unit != null) {
			Mounted m = unit.getEntity().getEquipment(equipmentNum);
            if(null != m && m.getOriginalShots() > 0) {
                fullShots = m.getOriginalShots();
            }
		}
    	if(null != unit && unit.getEntity() instanceof Protomech) {
	        //if protomechs are using alternate munitions then cut in half
	        if(((AmmoType)type).getMunitionType() != AmmoType.M_STANDARD) {
	            fullShots = fullShots / 2;
	        }
		}
		if(oneShot) {
			fullShots = 1;
		}
		return fullShots;
    }
    
    protected int getCurrentShots() {
    	int shots = getFullShots() - shotsNeeded;
    	//replace with actual entity values if entity not null because the previous number will not
    	//be correct for ammo swaps
    	if(null != unit && null != unit.getEntity()) {
    		Mounted m = unit.getEntity().getEquipment(equipmentNum);
    		if(null != m) {
    			shots = m.getBaseShotsLeft();
    		}
    	}
    	return shots;
    }

    public long getValueNeeded() {
    	return adjustCostsForCampaignOptions((long)(getPricePerTon() * ((double)shotsNeeded / getShotsPerTon())));
    }

    protected long getPricePerTon() {
    	//if on a unit, then use the ammo type on the existing entity, to avoid getting it wrong due to 
    	//ammo swaps
    	EquipmentType curType = type;
    	if(null != unit && null != unit.getEntity()) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted && (mounted.getType() instanceof AmmoType)) {
				curType = mounted.getType();
			}
    	}
    	return (long)curType.getRawCost();
    }
    
    protected int getShotsPerTon() {
    	AmmoType atype = (AmmoType)type;
    	if(atype.getKgPerShot() > 0) {
    		return (int)Math.floor(1000.0/atype.getKgPerShot());
    	}
    	//if not listed by kg per shot, we assume this is a single ton increment
    	return ((AmmoType)type).getShots();
    }
    
    @Override
    public long getStickerPrice() {
    	return (long)(getPricePerTon() * (1.0 * getCurrentShots()/getShotsPerTon()));
    }

    @Override
    public long getBuyCost() {
        return getNewPart().getStickerPrice();
    }

    public int getShotsNeeded() {
    	return shotsNeeded;
    }

    public void changeMunition(long m) {
    	this.munition = m;
    	for (AmmoType atype : Utilities.getMunitionsFor(unit.getEntity(),(AmmoType)type, CampaignOptions.TECH_EXPERIMENTAL)) {
    		if (atype.getMunitionType() == munition) {
    			type = atype;
    			break;
    		}
    	}
    	updateConditionFromEntity(false);
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<equipmentNum>"
				+equipmentNum
				+"</equipmentNum>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<typeName>"
				+MekHqXmlUtil.escape(type.getInternalName())
				+"</typeName>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<munition>"
				+munition
				+"</munition>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<shotsNeeded>"
				+shotsNeeded
				+"</shotsNeeded>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<checkedToday>"
				+checkedToday
				+"</checkedToday>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<oneShot>"
				+oneShot
				+"</oneShot>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		super.loadFieldsFromXmlNode(wn);
		NodeList nl = wn.getChildNodes();

		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("equipmentNum")) {
				equipmentNum = Integer.parseInt(wn2.getTextContent());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
				typeName = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("munition")) {
				munition = Long.parseLong(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("shotsNeeded")) {
				shotsNeeded = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("checkedToday")) {
				if(wn2.getTextContent().equalsIgnoreCase("true")) {
					checkedToday = true;
				} else {
					checkedToday = false;
				}
			} else if (wn2.getNodeName().equalsIgnoreCase("oneShot")) {
				if(wn2.getTextContent().equalsIgnoreCase("true")) {
					oneShot = true;
				} else {
					oneShot = false;
				}
			}
		}
		restore();
	}

	public void restoreMunitionType() {
		for (AmmoType atype : Utilities.getMunitionsFor(unit.getEntity(),(AmmoType)type, CampaignOptions.TECH_EXPERIMENTAL)) {
    		if (atype.getMunitionType() == munition && atype.getInternalName().equals(type.getInternalName())) {
    			type = atype;
    			break;
    		}
    	}
	}

	public long getMunitionType() {
		return munition;
	}

	@Override
	public int getAvailability(int era) {
		return type.getAvailability(Era.convertEra(era));
	}

	@Override
	public int getTechRating() {
		return type.getTechRating();
	}

	/*
	@Override
	public int getTechBase() {
		return T_BOTH;
	}
	*/

	@Override
	public String getStatus() {
		String toReturn = "Fully Loaded";
		if(shotsNeeded >= getFullShots()) {
			toReturn = "Empty";
		} else if (shotsNeeded > 0) {
			toReturn = "Partially Loaded";
		}
		if(isReservedForRefit()) {
			toReturn += " (Reserved for Refit)";
		}
		return toReturn;
	}

	@Override
	public void fix() {
		loadBin();
	}

	public void loadBin() {
		int shots = Math.min(getAmountAvailable(), shotsNeeded);
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted && mounted.getType() instanceof AmmoType) {
				if(mounted.getType().equals(type)
						&& ((AmmoType)mounted.getType()).getMunitionType() == getMunitionType()) {
					//just a simple reload
					mounted.setShotsLeft(mounted.getBaseShotsLeft() + shots);
				} else {
					//loading a new type of ammo
					unload();
					mounted.changeAmmoType((AmmoType)type);
					mounted.setShotsLeft(shots);
				}
			}
		}
		changeAmountAvailable(-1 * shots, (AmmoType)type);
		shotsNeeded -= shots;
	}

	public void setShotsNeeded(int shots) {
		this.shotsNeeded = shots;
	}

	@Override
	public String find(int transitDays) {
		return "<font color='red'> You shouldn't be here (AmmoBin.find()).</font>";
	}

	@Override
	public String failToFind() {
		return "<font color='red'> You shouldn't be here (AmmoBin.failToFind()).</font>";
	}

	public void unload() {
		//FIXME: the following won't work for proto and Dropper bins if they
		//are not attached to a unit. Currently the only place AmmoBins are loaded
		//off of units is for refits, which neither of those units can do, but we
		//may want to think about not having refits load ammo bins but rather reserve
		//some AmmoStorage instead if we implement customization of these units
		int shots = getFullShots() - shotsNeeded;
		AmmoType curType = (AmmoType)type;
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted && mounted.getType() instanceof AmmoType) {
				shots = mounted.getBaseShotsLeft();
				mounted.setShotsLeft(0);
				curType = (AmmoType)mounted.getType();
			}
		}
		shotsNeeded = getFullShots();
		if(shots > 0) {
			changeAmountAvailable(shots, curType);
		}
	}

	@Override
	public void remove(boolean salvage) {
		if(salvage) {
			unload();
		}
		super.remove(salvage);
	}

	@Override
	public MissingPart getMissingPart() {
		return new MissingAmmoBin(getUnitTonnage(), type, equipmentNum, oneShot, omniPodded, campaign);
	}

	public boolean isOneShot() {
		return oneShot;
	}

	@Override
	public TargetRoll getAllMods(Person tech) {
		if(isSalvaging()) {
			return super.getAllMods(tech);
		}
		return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "ammo loading");
	}

	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				if(mounted.isMissing() || mounted.isDestroyed()) {
					mounted.setShotsLeft(0);
					remove(false);
					return;
				}
				long currentMuniType = 0;
				if(mounted.getType() instanceof AmmoType) {
					currentMuniType = ((AmmoType)mounted.getType()).getMunitionType();
				}
				if(currentMuniType == getMunitionType()) {
					shotsNeeded = getFullShots() - mounted.getBaseShotsLeft();
				} else {
					//we have a change of munitions
					mounted.changeAmmoType((AmmoType) type);
					this.unload();
					shotsNeeded = getFullShots();
				}
			}
		}
	}

	@Override
	public int getBaseTime() {
		if(isSalvaging()) {
			return isOmniPodded()? 30 : 120;
		}
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				long currentMuniType = 0;
				if(mounted.getType() instanceof AmmoType) {
					currentMuniType = ((AmmoType)mounted.getType()).getMunitionType();
				}
				if(getMunitionType() != currentMuniType) {
					return 30;
				}
			}
		}
		return 15;
	}

    @Override
    public int getActualTime() {
        if (isOmniPodded()) {
            return (int)Math.ceil(getBaseTime() * mode.timeMultiplier * 0.5);
        }
        return (int) Math.ceil(getBaseTime() * mode.timeMultiplier);
    }

	@Override
	public int getDifficulty() {
		if(isSalvaging()) {
			return -2;
		}
		return 0;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				mounted.setHit(false);
		        mounted.setDestroyed(false);
		        mounted.setRepairable(true);
		        mounted.changeAmmoType((AmmoType) type);
		        unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, equipmentNum);
				mounted.setShotsLeft(getFullShots() - shotsNeeded);
			}
		}
	}

	@Override
	public boolean isSamePartType(Part part) {
    	return  part instanceof AmmoBin
                        && getType().equals( ((AmmoBin)part).getType() )
                        && ((AmmoBin)part).getFullShots() == getFullShots();
    }

	@Override
	public boolean needsFixing() {
		return shotsNeeded > 0;// && null != unit;
	}

	public String getDesc() {
		if(isSalvaging()) {
			return super.getDesc();
		}
		String toReturn = "<html><font size='2'";
		String scheduled = "";
		if (getTeamId() != null) {
			scheduled = " (scheduled) ";
		}

		toReturn += ">";
		toReturn += "<b>Reload " + getName() + "</b><br/>";
		toReturn += getDetails() + "<br/>";
		toReturn += "" + getTimeLeft() + " minutes" + scheduled;
		toReturn += "</font></html>";
		return toReturn;
	}

    @Override
    public String getDetails() {
    	if(isSalvaging()) {
    		return super.getDetails();
    	}
    	if(null != unit) {
    		String availability = "";
    		int shotsAvailable = getAmountAvailable();
            String[] inventories = campaign.getPartInventory(getNewPart());
    		if(shotsAvailable == 0) {
                availability = "<br><font color='red'>No ammo ("+ inventories[1] + " in transit, " + inventories[2] + " on order)</font>";
    		} else if(shotsAvailable < shotsNeeded) {
                availability = "<br><font color='red'>Only " + shotsAvailable + " available ("+ inventories[1] + " in transit, " + inventories[2] + " on order)</font>";
    		}
			return ((AmmoType)type).getDesc() + ", " + shotsNeeded + " shots needed" + availability;
    	} else {
    		return "";
    	}
    }

	@Override
    public String checkFixable() {
		if(!isSalvaging() && getAmountAvailable() == 0) {
			return "No ammo of this type is available";
		}
		if(null == unit) {
			return "Ammo bins can only be loaded when installed on units";
		}
        return null;
    }

	public void swapAmmoFromCompatible(int needed, AmmoStorage as) {
		AmmoStorage a = null;
		AmmoType aType = null;
		AmmoType curType = ((AmmoType)((AmmoStorage)as).getType());
		int converted = 0;
		for(Part part : campaign.getSpareParts()) {
		    if(!part.isPresent()) {
                continue;
            }
		    if(part instanceof AmmoStorage) {
				a = (AmmoStorage)part;
				aType = ((AmmoType)a.getType());
				if (a.isSamePartType(as)) {
					continue;
				}
				if (!isCompatibleAmmo(aType, curType)) {
					continue;
				}
				if (a.getShots() == 0) {
					continue;
				}
				// Finally, do the conversion. Run until the other ammo type runs out or we have enough
				converted = aType.getRackSize();
				a.changeAmountAvailable(-1, aType);
				while (converted < needed && a.getShots() > 0) {
					converted += aType.getRackSize();
					a.changeAmountAvailable(-1, aType);
				}
				needed -= converted;
				// If we have enough, we're done.
				if (converted >= needed) {
					break;
				}
			}
		}
		converted = Math.round((float)converted/curType.getRackSize());
		as.changeAmountAvailable(converted, curType);
	}

	public void changeAmountAvailable(int amount, AmmoType curType) {
		AmmoStorage a = null;
		AmmoType aType = null;
		for(Part part : campaign.getSpareParts()) {
		    if(!part.isPresent()) {
                continue;
            }
			if(part instanceof AmmoStorage) {
				aType = ((AmmoType)((AmmoStorage)part).getType());
				if(aType.equals(curType) && curType.getMunitionType() == aType.getMunitionType()) {
					a = (AmmoStorage)part;
					if (amount < 0 && campaign.getCampaignOptions().useAmmoByType()
					        && a.getShots() < Math.abs(amount)) {
						swapAmmoFromCompatible(Math.abs(amount) * aType.getRackSize(), a);
					}
					a.changeShots(amount);
					break;
				}
			}
		}
		if(null != a && a.getShots() <= 0) {
			campaign.removePart(a);
		} else if(null == a && amount > 0) {
			campaign.addPart(new AmmoStorage(1, curType, amount, campaign), 0);
		} else if (a == null && amount < 0
		        && campaign.getCampaignOptions().useAmmoByType()
		        && AmmoBin.ALLOWED_BY_TYPE.contains(curType.getAmmoType())) {
			campaign.addPart(new AmmoStorage(1 , curType ,0, campaign), 0);
			changeAmountAvailable(amount, curType);
		}
	}

	public boolean isCompatibleAmmo(AmmoType a1, AmmoType a2) {

		// If the option isn't on, we don't use this!
		if (!(campaign.getCampaignOptions().useAmmoByType())) {
			return false;
		}

		// NPE protection
		if (a1 == null || a2 == null) {
			return false;
		}

        // If it isn't an allowed type, then nope!
        if (!AmmoBin.ALLOWED_BY_TYPE.contains(a1.getAmmoType()) || !AmmoBin.ALLOWED_BY_TYPE.contains(a2.getAmmoType())) {
            return false;
        }

		// Now we begin to compare
		boolean result = false;

		// MML Launchers, ugh.
		if ((a1.getAmmoType() == AmmoType.T_MML || a2.getAmmoType() == AmmoType.T_MML)
				&& a1.getMunitionType() == a2.getMunitionType()) {
			// LRMs...
			if (a1.getAmmoType() == AmmoType.T_MML && a1.hasFlag(AmmoType.F_MML_LRM) && a2.getAmmoType() == AmmoType.T_LRM) {
				result = true;
			} else if (a2.getAmmoType() == AmmoType.T_MML && a2.hasFlag(AmmoType.F_MML_LRM) && a1.getAmmoType() == AmmoType.T_LRM) {
				result = true;
			}
			// SRMs
			if (a1.getAmmoType() == AmmoType.T_MML && !a1.hasFlag(AmmoType.F_MML_LRM) && a2.getAmmoType() == AmmoType.T_SRM) {
				result = true;
			} else if (a2.getAmmoType() == AmmoType.T_MML && !a2.hasFlag(AmmoType.F_MML_LRM) && a1.getAmmoType() == AmmoType.T_SRM) {
				result = true;
			}
		}

		// AR-10 Launchers, ugh.
		/*if (a1.getAmmoType() == AmmoType.T_AR10 || a2.getAmmoType() == AmmoType.T_AR10) {
			// Barracuda
			if (a1.getAmmoType() == AmmoType.T_AR10 && a1.hasFlag(AmmoType.F_AR10_BARRACUDA) && a2.getAmmoType() == AmmoType.T_BARRACUDA) {
				result = true;
			} else if (a2.getAmmoType() == AmmoType.T_AR10 && a2.hasFlag(AmmoType.F_AR10_BARRACUDA) && a1.getAmmoType() == AmmoType.T_BARRACUDA) {
				result = true;
			}
			// Killer Whale
			if (a1.getAmmoType() == AmmoType.T_AR10 && a1.hasFlag(AmmoType.F_AR10_KILLER_WHALE) && a2.getAmmoType() == AmmoType.T_KILLER_WHALE) {
				result = true;
			} else if (a2.getAmmoType() == AmmoType.T_AR10 && a2.hasFlag(AmmoType.F_AR10_KILLER_WHALE) && a1.getAmmoType() == AmmoType.T_KILLER_WHALE) {
				result = true;
			}
			// White Shark
			if (a1.getAmmoType() == AmmoType.T_AR10 && a1.hasFlag(AmmoType.F_AR10_WHITE_SHARK) && a2.getAmmoType() == AmmoType.T_WHITE_SHARK) {
				result = true;
			} else if (a2.getAmmoType() == AmmoType.T_AR10 && a2.hasFlag(AmmoType.F_AR10_WHITE_SHARK) && a1.getAmmoType() == AmmoType.T_WHITE_SHARK) {
				result = true;
			}
		}*/

		// General Launchers
		if (a1.getAmmoType() == a2.getAmmoType() && a1.getMunitionType() == a2.getMunitionType()) {
			result = true;
		}

		return result;
	}

	public int getAmountAvailable() {
		int amount = 0;
		AmmoStorage a = null;
		AmmoType aType = null;
		AmmoType thisType = null;
		for(Part part : campaign.getSpareParts()) {
		    if(!part.isPresent()) {
		        continue;
		    }
			if(part instanceof AmmoStorage) {
				a = (AmmoStorage)part;
				aType = ((AmmoType)((AmmoStorage)a).getType());
				thisType = ((AmmoType)getType());
				if(aType.equals((Object)getType()) && thisType.getMunitionType() == aType.getMunitionType()) {
					amount += a.getShots();
					if (!(campaign.getCampaignOptions().useAmmoByType())) {
						break;
					}
				} else if (isCompatibleAmmo(aType, thisType) && thisType.getRackSize() != 0) {
					amount += a.getShots()*aType.getRackSize()/thisType.getRackSize();
				}
			}
		}
		return amount;
	}

	public boolean isEnoughSpareAmmoAvailable() {
		return getAmountAvailable() >= shotsNeeded;
	}

	@Override
	public String getAcquisitionDesc() {
		String toReturn = "<html><font size='2'";

		toReturn += ">";
		toReturn += "<b>" + getAcquisitionDisplayName() + "</b> " + getAcquisitionBonus() + "<br/>";
		toReturn += getAcquisitionExtraDesc() + "<br/>";
		String[] inventories = campaign.getPartInventory(getAcquisitionPart());
        toReturn += inventories[1] + " in transit, " + inventories[2] + " on order<br>";
		toReturn += Utilities.getCurrencyString(getBuyCost()) + "<br/>";
		toReturn += "</font></html>";
		return toReturn;
	}
	
    @Override
    public String getAcquisitionDisplayName() {
    	return type.getDesc();
    }    

	@Override
	public String getAcquisitionExtraDesc() {
		return ((AmmoType)type).getShots() + " shots (1 ton)";
	}

	@Override
    public String getAcquisitionBonus() {
		String bonus = getAllAcquisitionMods().getValueAsString();
		if(getAllAcquisitionMods().getValue() > -1) {
			bonus = "+" + bonus;
		}

		return "(" + bonus + ")";
    }

	@Override
	public Part getAcquisitionPart() {
		return getNewPart();
	}

	@Override
	public String getAcquisitionName() {
		return type.getDesc();
	}

	@Override
	public TargetRoll getAllAcquisitionMods() {
        TargetRoll target = new TargetRoll();
        // Faction and Tech mod
        if(isClanTechBase() && campaign.getCampaignOptions().getClanAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getClanAcquisitionPenalty(), "clan-tech");
        }
        else if(campaign.getCampaignOptions().getIsAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getIsAcquisitionPenalty(), "Inner Sphere tech");
        }
        //availability mod
        int avail = getAvailability(campaign.getEra());
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + EquipmentType.getRatingName(avail) + ")");
        return target;
    }

	@Override
    public Object getNewEquipment() {
        return getNewPart();
    }

	public Part getNewPart() {
		return new AmmoStorage(1,type,shotsNeeded,campaign);
	}

	@Override
    public IAcquisitionWork getAcquisitionWork() {
		int shots = 1;
		if(type instanceof AmmoType) {
			shots = ((AmmoType)type).getShots();
		}
        return new AmmoStorage(1,type,shots,campaign);
    }

	@Override
	public boolean needsMaintenance() {
        return false;
    }

	@Override
	public boolean isPriceAdustedForAmount() {
        return true;
    }

    @Override
    public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.AMMO;
    }
    
    @Override
    public boolean isOmniPoddable() {
        return true;
    }
    
    /**
     * Since ammo bins aren't real parts they can't be podded in the warehouse, and
     * whether they're podded on the unit depends entirely on the unit they're installed on.
     */
    @Override
    public boolean isOmniPodded() {
        return getUnit() != null && getUnit().getEntity().getEquipment(equipmentNum).isOmniPodMounted();
    }
}
