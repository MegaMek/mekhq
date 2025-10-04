/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Era;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.parts.missing.MissingVeeStabilizer;
import mekhq.campaign.personnel.skills.enums.SkillTypeNew;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class VeeStabilizer extends Part {
    private static final MMLogger LOGGER = MMLogger.create(VeeStabilizer.class);

    private int loc;

    public VeeStabilizer() {
        this(0, 0, null);
    }

    public VeeStabilizer(int tonnage, int loc, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.name = "Vehicle Stabilizer";
    }

    @Override
    public VeeStabilizer clone() {
        VeeStabilizer clone = new VeeStabilizer(getUnitTonnage(), 0, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof VeeStabilizer;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "loc", loc);
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
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }
    }

    @Override
    public AvailabilityValue getBaseAvailability(Era era) {
        return AvailabilityValue.B;
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit && unit.getEntity() instanceof Tank) {
            ((Tank) unit.getEntity()).clearStabiliserHit(loc);
        }
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingVeeStabilizer(getUnitTonnage(), loc, campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit && unit.getEntity() instanceof Tank) {
            ((Tank) unit.getEntity()).setStabiliserHit(loc);
            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if (null != spare) {
                spare.changeQuantity(1);
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.getQuartermaster().addPart(missing, 0, false);
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if (null != unit && unit.getEntity() instanceof Tank) {
            int priorHits = hits;
            if (((Tank) unit.getEntity()).isStabiliserHit(loc)) {
                hits = 1;
            } else {
                hits = 0;
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
        return 60;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return 0;
        }
        return 1;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Tank) {
            if (hits > 0 && !((Tank) unit.getEntity()).isStabiliserHit(loc)) {
                ((Tank) unit.getEntity()).setStabiliserHit(loc);
            } else if (hits == 0 && ((Tank) unit.getEntity()).isStabiliserHit(loc)) {
                ((Tank) unit.getEntity()).clearStabiliserHit(loc);
            }
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (!isSalvaging() && (null != unit) && unit.isLocationBreached(loc)) {
            return unit.getEntity().getLocationName(loc) + " is breached.";
        }
        return null;
    }

    @Override
    public double getTonnage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Money getStickerPrice() {
        // TODO Auto-generated method stub
        return Money.zero();
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if (null != unit) {
            return unit.getEntity().getLocationName(loc);
        }
        return "";
    }

    @Override
    public int getLocation() {
        return loc;
    }

    public void setLocation(int l) {
        this.loc = l;
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillTypeNew.S_TECH_MECHANIC.name());
    }

    @Override
    public String getLocationName() {
        return unit.getEntity().getLocationName(loc);
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TankLocation.TECH_ADVANCEMENT;
    }

}
