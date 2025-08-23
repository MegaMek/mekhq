/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.PrintWriter;
import java.util.StringJoiner;

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.units.Jumpship;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author MKerensky
 */
public class KFHeliumTank extends Part {
    private static final MMLogger logger = MMLogger.create(KFHeliumTank.class);

    public static final TechAdvancement TA_HELIUM_TANK = new TechAdvancement(TechBase.ALL)
                                                               .setAdvancement(2107, 2120, 2300)
                                                               .setPrototypeFactions(Faction.TA)
                                                               .setProductionFactions(Faction.TA)
                                                               .setTechRating(TechRating.D)
                                                               .setAvailability(AvailabilityValue.D,
                                                                     AvailabilityValue.E,
                                                                     AvailabilityValue.D,
                                                                     AvailabilityValue.D)
                                                               .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    // Standard, primitive, compact, subcompact...
    private int coreType;

    public int getCoreType() {
        return coreType;
    }

    // How many docking collars does this drive support?
    private int docks;

    public int getDocks() {
        return docks;
    }

    public KFHeliumTank() {
        this(0, Jumpship.DRIVE_CORE_STANDARD, 0, null);
    }

    public KFHeliumTank(int tonnage, int coreType, int docks, Campaign c) {
        super(tonnage, c);
        this.coreType = coreType;
        this.docks = docks;
        this.name = "K-F Helium Tank";
        this.unitTonnageMatters = true;
    }

    @Override
    public KFHeliumTank clone() {
        KFHeliumTank clone = new KFHeliumTank(0, coreType, docks, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if (null != unit) {
            if (unit.getEntity() instanceof Jumpship) {
                if (((Jumpship) unit.getEntity()).getKFHeliumTankHit()) {
                    hits = 1;
                } else {
                    hits = 0;
                }
            }
            if (checkForDestruction
                      && hits > priorHits
                      && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        int time;
        if (isSalvaging()) {
            // 10x the repair time
            time = 1800;
        } else {
            // BattleSpace, p28
            time = 180;
        }
        return time;
    }

    @Override
    public int getDifficulty() {
        // Battlespace, p28 - pretty easy to fix. Replacing's a pain.
        if (isSalvaging()) {
            return 4;
        }
        return 0;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Jumpship) {
            ((Jumpship) unit.getEntity()).setKFHeliumTankHit(needsFixing());
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit && unit.getEntity() instanceof Jumpship) {
            Jumpship js = ((Jumpship) unit.getEntity());
            js.setKFHeliumTankHit(false);
            // Also repair your KF Drive integrity - up to 2/3 of the total if you have
            // other components to fix
            // Otherwise, fix it all.
            if (js.isKFDriveDamaged()) {
                js.setKFIntegrity(
                      Math.min((js.getKFIntegrity() + js.getKFHeliumTankIntegrity()), js.getOKFIntegrity()));
            } else {
                js.setKFIntegrity(js.getOKFIntegrity());
            }
        }
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            if (unit.getEntity() instanceof Jumpship) {
                Jumpship js = ((Jumpship) unit.getEntity());
                js.setKFIntegrity(Math.max(0, js.getKFIntegrity() - js.getKFHeliumTankIntegrity()));
                js.setKFHeliumTankHit(true);
                // You can transport a helium tank
                // See SO p130 for reference
                Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
                if (!salvage) {
                    campaign.getWarehouse().removePart(this);
                } else if (null != spare) {
                    spare.incrementQuantity();
                    campaign.getWarehouse().removePart(this);
                } else {
                    // Start a new collection
                    campaign.getQuartermaster().addPart(this, 0);
                }
                campaign.getWarehouse().removePart(this);
                unit.removePart(this);
                Part missing = getMissingPart();
                unit.addPart(missing);
                campaign.getQuartermaster().addPart(missing, 0);
            }
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingKFHeliumTank(getUnitTonnage(), coreType, docks, campaign);
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public Money getStickerPrice() {
        if (unit != null && unit.getEntity() instanceof Jumpship) {
            int cost = (50000 * ((Jumpship) unit.getEntity()).getOKFIntegrity());
            if (((Jumpship) unit.getEntity()).getDriveCoreType() == Jumpship.DRIVE_CORE_COMPACT
                      && ((Jumpship) unit.getEntity()).hasLF()) {
                cost *= 15;
            } else if (((Jumpship) unit.getEntity()).hasLF()) {
                cost *= 3;
            } else if (((Jumpship) unit.getEntity()).getDriveCoreType() == Jumpship.DRIVE_CORE_COMPACT) {
                cost *= 5;
            }
            return Money.of(cost);
        }
        return Money.of(50000);
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof KFHeliumTank
                     && coreType == ((KFHeliumTank) part).getCoreType()
                     && docks == ((KFHeliumTank) part).getDocks();
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "coreType", coreType);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "docks", docks);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("coreType")) {
                    coreType = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("docks")) {
                    docks = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        StringJoiner joiner = new StringJoiner(", ");
        String details = super.getDetails(includeRepairDetails);
        if (!details.isEmpty()) {
            joiner.add(details);
        }
        joiner.add(getDocks() + " collars");
        return joiner.toString();
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_VESSEL);
    }

    @Override
    public String getLocationName() {
        return null;
    }

    @Override
    public int getLocation() {
        return Jumpship.LOC_HULL;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TA_HELIUM_TANK;
    }
}
