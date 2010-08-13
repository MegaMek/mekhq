/*
 * EquipmentPart.java
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

import components.abPlaceable;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.TechConstants;
import megamek.common.weapons.Weapon;
import mekhq.campaign.Campaign;
import mekhq.campaign.Faction;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.SSWLibHelper;
import mekhq.campaign.Unit;
import mekhq.campaign.work.EquipmentReplacement;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class EquipmentPart extends Part {
	private static final long serialVersionUID = 2892728320891712304L;

	private Unit tmpUnit = null; // This is *not* serialized in XML.  Should only be necessary for calculations.
	private int tmpFaction = -1; // This is *not* serialized in XML.  Should only be necessary for calculations.
	
	//crap equipmenttype is not serialized!
    protected transient EquipmentType type;

    protected String typeName;

    public EquipmentType getType() {
        return type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }
    
    public EquipmentPart() {
    	this(false, 0, 0, null, null);
    }
    
    public EquipmentPart(boolean salvage, int tonnage, int faction, EquipmentType et, Unit unit) {
        // TODO Memorize all entity attributes needed to calculate cost
        // As it is a part bought with one entity can be used on another entity
        // on which it would have a different price (only tonnage is taken into
        // account for compatibility)
        super(salvage, tonnage);
        this.type = et;
        this.tmpUnit = unit;
        this.tmpFaction = faction;
        reCalc();
    }
    
    @Override
    public void reCalc() {
        if (type != null) {
        	this.name = type.getDesc();
        	this.typeName = type.getInternalName();

            if (type.getCost(null, false) == EquipmentType.COST_VARIABLE)
                this.name += " (" + getTonnage() + ")";
        }

        // The cost is calculated here.
        // Upon initial construction, a temporary unit and faction are passed in.
        // They are *only* used to calculate the cost, and therefore correctly are not necessarily here.
        //TODO: Convert to static cost calc function for construction?
        if (tmpUnit == null)
        	return;
        
        computeCost(tmpUnit.getEntity());

        if (tmpFaction < 0)
        	return;
        
        // Increase cost for Clan parts when player is IS faction
        if (isClanTechBase() && !Faction.isClanFaction(tmpFaction))
            this.cost *= tmpUnit.campaign.getCampaignOptions().getClanPriceModifier();
    }

    /**
     * Copied from megamek.common.Entity.getWeaponsAndEquipmentCost(StringBuffer detail, boolean ignoreAmmo)
     *
     * @param entity The entity the Equipment comes from / is added to
     */
    private void computeCost (Entity entity) {
        if (entity == null)
            return;

        EquipmentType type = getType();
        float weight = entity.getWeight();
        int cost = 0;

        if (type instanceof MiscType
                && type.hasFlag(MiscType.F_LASER_HEAT_SINK)) {
            // TODO Laser heat sink cost ?
            cost = 6000;
        } else if (type instanceof MiscType
                && type.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
            cost = 6000;
        } else if (type instanceof MiscType
                && type.hasFlag(MiscType.F_HEAT_SINK)) {
            cost = 2000;
        } else if (entity instanceof megamek.common.Mech
                && type instanceof MiscType
                && type.hasFlag(MiscType.F_JUMP_JET)) {
            megamek.common.Mech mech = (megamek.common.Mech) entity;

            double jumpBaseCost = 200;
            // You cannot have JJ's and UMU's on the same unit.
            double c = 0;
            
            if (mech.hasUMU()) {
                c = Math.pow(mech.getAllUMUCount(), 2.0) * weight * jumpBaseCost;
            } else {
                if (mech.getJumpType() == megamek.common.Mech.JUMP_BOOSTER) {
                    jumpBaseCost = 150;
                } else if (mech.getJumpType() == megamek.common.Mech.JUMP_IMPROVED) {
                    jumpBaseCost = 500;
                }
                c = Math.pow(mech.getOriginalJumpMP(), 2.0) * weight * jumpBaseCost;
            }

            cost = (int) c;
        } else {
            // TODO take isArmored into account
            boolean isArmored = false;

            // TODO set isWeaponGroup correctly
            boolean isWeaponGroup = false;

            if (isWeaponGroup) {
                this.cost = 2;
                return ;
            }

            int itemCost = (int) type.getCost(entity, isArmored);
            if (itemCost == EquipmentType.COST_VARIABLE) {
                itemCost = type.resolveVariableCost(entity, isArmored);
            }

            cost = itemCost;
        }

        if (cost > 100000000 || cost < 0) {
            cost = 0;
        }

        if (cost == 0) {
            // Some equipments do not have a price set in megamek
            // Check if ssw has the price
            abPlaceable placeable = null;
            
            for (String sswName : getPotentialSSWNames(Faction.F_FEDSUN)) {
                placeable = SSWLibHelper.getAbPlaceableByName(Campaign.getSswEquipmentFactory(), Campaign.getSswMech(), sswName);
                if (placeable != null)
                    break;
            }
            
            if (placeable != null)
                cost = (int) placeable.GetCost();
        }

        if (cost > 100000000 || cost < 0) {
            cost = 0;
        }

        this.cost = cost;
    }
    
    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        if(task instanceof EquipmentReplacement) {
            EquipmentType et = ((EquipmentReplacement)task).getMounted().getType();
            if (et.getCost(null, false) == EquipmentType.COST_VARIABLE) {
                // In this case tonnage matters (ex. : hartchet, sword, ...
                return type.equals(et) && getTonnage() == ((EquipmentReplacement)task).getUnit().getEntity().getWeight();
            } else {
                return type.equals(et);
            }
        }
        return false;
    }
    
    /**
     * Restores the equipment from the name
     */
    public void restore() {
        if (typeName == null) {
            typeName = type.getName();
        } else {
            type = EquipmentType.get(typeName);
        }

        if (type == null) {
            System.err
            .println("Mounted.restore: could not restore equipment type \""
                    + typeName + "\"");
        }
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        boolean b =  part instanceof EquipmentPart
                        && getName().equals(part.getName())
                        && getStatus().equals(part.getStatus())
                        && getType().equals( ((EquipmentPart)part).getType() );
        if (getType().getCost(null, false) == EquipmentType.COST_VARIABLE)
            return b && getTonnage() == part.getTonnage();
        else
            return b;
    }

    @Override
    public int getPartType() {
        if (getType() instanceof Weapon)
            return PART_TYPE_WEAPON;
        else if (getType() instanceof AmmoType)
            return PART_TYPE_AMMO;
        else
            return PART_TYPE_EQUIPMENT_PART;
    }

    @Override
    public ArrayList<String> getPotentialSSWNames (int faction) {
        ArrayList<String> sswNames = new ArrayList<String>();

        // The tech base matters for equipments (ie. you can't use a IS medium pulse laser to replace a Clan medium pulse laser
        String techBase = (isClanTechBase() ? "(CL)" : "(IS)");
        
        if (getPartType()==PART_TYPE_AMMO || getPartType()==PART_TYPE_WEAPON) {
            String sswName = getName();

            sswName = sswName.replace("SRM ", "SRM-");
            sswName = sswName.replace("MRM ", "MRM-");
            sswName = sswName.replace("LRM ", "LRM-");
            sswName = sswName.replace("ATM ", "ATM-");

            sswName = sswName.replace("X Pulse", "X-Pulse");

            sswName = sswName.replace("LAC/2", "Light AC/2");
            sswName = sswName.replace("LAC/5", "Light AC/5");

            sswName = sswName.replace("AMS", "Anti-Missile System");

            sswName = sswName.replace("HAG/20", "Hyper Assault Gauss 20");
            sswName = sswName.replace("HAG/30", "Hyper Assault Gauss 30");
            sswName = sswName.replace("HAG/40", "Hyper Assault Gauss 40");

            if (getPartType()==PART_TYPE_AMMO) {
                sswName = sswName.replace(" Ammo","");
                sswName = "@ " + sswName;

                sswName = sswName.replace("Cluster", "(Cluster)");

                if (sswName.contains("-X") && !sswName.contains("AC"))
                    sswName = sswName.replace("-X","-X AC");

                if (sswName.contains("-X AC") && !sswName.contains("Cluster"))
                    sswName = sswName.replace("-X AC", "-X AC (Slug)");

                sswName = sswName.replace("Artemis-capable", "(Artemis IV Capable)");
                sswName = sswName.replace("Narc-capable", "(Narc Capable)");

                if (sswName.contains("Gauss") && !sswName.contains("Gauss Rifle") && !sswName.contains("Hyper Assault"))
                    sswName = sswName.replace("Gauss", "Gauss Rifle");

                sswName = sswName.replace(" (Clan)", "");

                sswName = sswName.replace("Arrow IV Homing", "Arrow IV (Homing)");
                sswName = sswName.replace("Arrow IV FASCAM", "Arrow IV (FASCAM)");
                if (sswName.contains("Arrow IV") && !sswName.contains("Homing") && !sswName.contains("FASCAM"))
                    sswName = sswName.replace("Arrow IV", "Arrow IV (Non-Homing)");

                if (sswName.contains("MML") && sswName.contains("-Ammo")) {
                    sswName = sswName.replace("LRM-Ammo", "(LRM)");
                    sswName = sswName.replace("MRM-Ammo", "(MRM)");
                    sswName = sswName.replace("SRM-Ammo", "(SRM)");
                    sswName = sswName.replace("MML ", "MML-");
                }
            } else if (getPartType()==PART_TYPE_WEAPON) {

                sswName = sswName.replace("Narc", "Narc Missile Beacon");
                sswName = sswName.replace("Arrow IV", "Arrow IV Missile");

                sswName = sswName.replace("Machine Gun Array", "Machine Gun");
                sswName = sswName.replace("MML ", "MML-");
            }

            sswNames.add(techBase + " " + sswName);
            sswNames.add(sswName);
        } else if (getPartType() == Part.PART_TYPE_EQUIPMENT_PART) {
            String sswName = getName();

            sswName = sswName.replace("C3 Slave", "C3 Computer (Slave)");
            sswName = sswName.replace("C3i Computer", "Improved C3 Computer");
            sswName = sswName.replace("Angel ECM Suite", "Angel ECM");

            sswNames.add(techBase + " " + sswName);
            sswNames.add(sswName);
        }
        
        return sswNames;
    }

    @Override
    public boolean isClanTechBase() {
        String techBase = TechConstants.getTechName(getType().getTechLevel());

        if (techBase.equals("Clan"))
            return true;
        else if (techBase.equals("Inner Sphere"))
            return false;
        else
            return false;
    }

    @Override
    public int getTech () {
        if (getType().getTechLevel() < 0 || getType().getTechLevel() >= TechConstants.SIZE)
            return TechConstants.T_IS_TW_NON_BOX;
        else
            return getType().getTechLevel();
    }

    @Override
    public String getDesc() {
        return (isClanTechBase() ? "Clan " : "") + super.getDesc();
    }

    @Override
    public String getSaveString () {
        return "EquipmentPart" 
                    + ";" + getTonnage()
                    + ";" + getTypeName()
                    + ";" + getCost();
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		// We should be able to regenerate the EquipmentType from its name,
		// using the "restore()" function.
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<typeName>"
				+(typeName==null?type.getName():typeName)
				+"</typeName>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
				typeName = wn2.getTextContent();
			} 
		}
	}
}
