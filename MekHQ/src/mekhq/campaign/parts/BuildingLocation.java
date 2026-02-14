/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekhq.campaign.parts;


import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.BuildingType;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.IArmorState;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.IBuilding;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;

/**
 * Represents a single floor of a building hex in an AbstractBuildingEntity.
 * Each BuildingLocation corresponds to one location in the entity's location array.
 */
public class BuildingLocation extends Part {
    private static final MMLogger LOGGER = MMLogger.create(BuildingLocation.class);

    public static final TechAdvancement TECH_ADVANCEMENT = new TechAdvancement(TechBase.ALL)
            .setAdvancement(2300, 2350, 2400)
            .setApproximate(true, false, false)
            .setPrototypeFactions(Faction.TH)
            .setProductionFactions(Faction.TH)
            .setTechRating(TechRating.C)
            .setAvailability(AvailabilityValue.A,
                    AvailabilityValue.A,
                    AvailabilityValue.A,
                    AvailabilityValue.A)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    protected int loc;
    protected int cfDamage;      // CF (internal structure) damage
    protected int armorDamage;   // Armor damage

    private BuildingType buildingType;
    /**
     * See {@link IBuilding} for building class constants
     */
    private int buildingClass;

    public BuildingLocation(int loc, BuildingType buildingType, int buildingClass, Campaign campaign) {
        super(0, campaign);
        this.loc = loc;
        this.buildingType = buildingType;
        this.buildingClass = buildingClass;
        this.cfDamage = 0;
        this.armorDamage = 0;
        this.name = "Building Floor";
        updateName();
    }

    private void updateName() {
        if (unit != null && unit.getEntity() instanceof AbstractBuildingEntity) {
            this.name = unit.getEntity().getLocationName(loc);
        } else {
            this.name = "Building Floor " + loc;
        }
    }

    public int getLoc() {
        return loc;
    }

    public int getCFDamage() {
        return cfDamage;
    }

    public int getArmorDamage() {
        return armorDamage;
    }

    @Override
    public BuildingLocation clone() {
        BuildingLocation clone = new BuildingLocation(loc, buildingType, buildingClass, campaign);
        clone.copyBaseData(this);
        clone.loc = this.loc;
        clone.cfDamage = this.cfDamage;
        clone.armorDamage = this.armorDamage;
        clone.buildingType = this.buildingType;
        clone.buildingClass = this.buildingClass;
        return clone;
    }

    @Override
    public double getTonnage() {
        // Buildings don't have traditional tonnage - return 0
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof BuildingLocation &&
                getLoc() == ((BuildingLocation) part).getLoc() &&
                buildingType == ((BuildingLocation) part).buildingType &&
                buildingClass == ((BuildingLocation) part).buildingClass;
    }

    @Override
    public boolean isSameStatus(Part part) {
        return super.isSameStatus(part) &&
                this.cfDamage == ((BuildingLocation) part).cfDamage &&
                this.armorDamage == ((BuildingLocation) part).armorDamage;
    }

    @Override
    public void writeToXML(PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "loc", loc);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "cfDamage", cfDamage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "armorDamage", armorDamage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "buildingType", buildingType.name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "buildingClass", buildingClass);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("loc")) {
                    loc = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("cfDamage")) {
                    cfDamage = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("armorDamage")) {
                    armorDamage = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("buildingType")) {
                    buildingType = BuildingType.valueOf(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("buildingClass")) {
                    buildingClass = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }
    }

    @Override
    public void fix() {
        super.fix();
        // Repair armor first if damaged
        if (armorDamage > 0) {
            armorDamage = 0;
            if (unit != null && unit.getEntity() instanceof AbstractBuildingEntity entity) {
                int maxArmor = entity.getOArmor(loc);
                entity.setArmor(maxArmor, loc);
            }
        } else {
            // Then repair CF
            cfDamage = 0;
            if (unit != null) {
                unit.getEntity().setInternal(unit.getEntity().getOInternal(loc), loc);
            }
        }
    }

    @Override
    public @Nullable MissingPart getMissingPart() {
        // Can't replace building locations
        return null;
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if (null != spare) {
                spare.changeQuantity(1);
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if (null != unit) {
            if (IArmorState.ARMOR_DESTROYED == unit.getEntity().getInternal(loc)) {
                remove(false);
            } else {
                // Update CF damage
                int originalInternal = unit.getEntity().getOInternal(loc);
                int internal = unit.getEntity().getInternal(loc);
                cfDamage = originalInternal - Math.min(originalInternal, Math.max(internal, 0));

                // Update armor damage
                int originalArmor = unit.getEntity().getOArmor(loc);
                int armor = unit.getEntity().getArmor(loc);
                armorDamage = originalArmor - Math.min(originalArmor, Math.max(armor, 0));
            }
        }
    }

    @Override
    public void updateConditionFromPart() {
        if (unit != null) {
            // Update CF
            unit.getEntity().setInternal(unit.getEntity().getOInternal(loc) - cfDamage, loc);
            // Update armor
            unit.getEntity().setArmor(unit.getEntity().getOArmor(loc) - armorDamage, loc);
        }
    }

    @Override
    public int getBaseTime() {
        return 120; // Buildings take longer to repair than tanks
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public boolean needsFixing() {
        return cfDamage > 0 || armorDamage > 0;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        StringBuilder toReturn = new StringBuilder();

        toReturn.append(super.getDetails(includeRepairDetails));

        if (includeRepairDetails) {
            if (armorDamage > 0) {
                toReturn.append(", ").append(armorDamage).append(armorDamage == 1 ? " point" : " points")
                        .append(" of armor damage");
            }
            if (cfDamage > 0) {
                toReturn.append(", ").append(cfDamage).append(cfDamage == 1 ? " point" : " points")
                        .append(" of CF damage");
            }
        }

        return toReturn.toString();
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public boolean isSalvaging() {
        return false;
    }

    @Override
    public String checkScrappable() {
        return "Building locations cannot be scrapped";
    }

    @Override
    public boolean canNeverScrap() {
        return true;
    }

    @Override
    public Money getStickerPrice() {
        if (unit == null) {
            return Money.zero();
        }

        // Building construction costs based on CF and building class
        // CF cost = building class modifier * CF
        double buildingClassModifier = switch (buildingClass) {
            case IBuilding.STANDARD -> 10000;
            case IBuilding.HANGAR -> 8000;
            case IBuilding.FORTRESS -> 20000;
            case IBuilding.GUN_EMPLACEMENT -> 20000;
            default -> 10000;
        };

        int cf = unit.getEntity().getOInternal(loc);
        int armor = unit.getEntity().getOArmor(loc);

        double cfCost = buildingClassModifier * cf;

        // Armor cost = (armor points / 16) * cost per ton
        // Standard armor is 16 points per ton, IS armor is 10000 per ton
        double baseArmorPrice = isClan() ? 15000 : 10000;
        double armorPrice = (armor / 16.0) * baseArmorPrice;

        return Money.of(cfCost + armorPrice);
    }

    @Override
    public String getLocationName() {
        return unit != null ? unit.getEntity().getLocationName(loc) : "Building Floor " + loc;
    }

    @Override
    public int getLocation() {
        return loc;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TECH_ADVANCEMENT;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.GENERAL_LOCATION;
    }

    @Override
    public boolean isRightTechType(String skillType) {
        // Buildings could use construction skills, but we'll use mechanic for now
        return skillType.equals(mekhq.campaign.personnel.skills.SkillType.S_TECH_MECHANIC);
    }

    @Override
    public void doMaintenanceDamage(int d) {
        if (null == unit) {
            LOGGER.error("Tried to damage a building location without a unit");
            return;
        }
        // Damage armor first
        if (unit.getEntity().getArmor(loc) > 0) {
            int armor = unit.getEntity().getArmor(loc);
            armor = Math.max(armor - d, 0);
            unit.getEntity().setArmor(armor, loc);
        } else {
            // Then damage CF
            int cf = unit.getEntity().getInternal(loc);
            cf = Math.max(cf - d, 1);
            unit.getEntity().setInternal(cf, loc);
        }
        updateConditionFromEntity(false);
    }
}
