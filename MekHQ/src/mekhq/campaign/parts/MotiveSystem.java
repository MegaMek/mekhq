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
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Era;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MotiveSystem extends Part {
    private static final MMLogger LOGGER = MMLogger.create(MotiveSystem.class);

    int damage;
    int penalty;

    public MotiveSystem() {
        this(0, null);
    }

    public MotiveSystem(int ton, Campaign c) {
        super(ton, c);
        this.name = "Motive System";
        this.damage = 0;
        this.penalty = 0;
    }

    @Override
    public int getBaseTime() {
        return 60;
    }

    @Override
    public int getDifficulty() {
        return -1;
    }

    @Override
    public MotiveSystem clone() {
        MotiveSystem clone = new MotiveSystem(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public AvailabilityValue getBaseAvailability(Era era) {
        return AvailabilityValue.B;
    }

    @Override
    public Money getStickerPrice() {
        // TODO Auto-generated method stub
        return Money.zero();
    }

    @Override
    public double getTonnage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof MotiveSystem;
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("damage")) {
                    damage = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("penalty")) {
                    penalty = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }

    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "damage", damage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "penalty", penalty);
        writeToXMLEnd(pw, indent);
    }

    @Override
    public @Nullable String checkFixable() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void fix() {
        super.fix();
        damage = 0;
        penalty = 0;
        if (null != unit && unit.getEntity() instanceof Tank) {
            ((Tank) unit.getEntity()).resetMovementDamage();
        }
    }

    @Override
    public MissingPart getMissingPart() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void remove(boolean salvage) {
        // you can't do this so nothing here

    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        // motive systems don't have to check for destruction since they
        // cannot be removed
        if (null != unit && unit.getEntity() instanceof Tank tank) {
            damage = tank.getMotiveDamage();
            penalty = tank.getMotivePenalty();
        }
    }

    @Override
    public void updateConditionFromPart() {
        // TODO Auto-generated method stub
        // you can't get here so, dont worry about it
    }

    @Override
    public boolean needsFixing() {
        return damage > 0 || penalty > 0;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if (includeRepairDetails) {
            return "-" + damage + " MP/-" + penalty + " Piloting";
        } else {
            return super.getDetails(false);
        }
    }

    @Override
    public String checkScrappable() {
        return "Motive type cannot be scrapped";
    }

    @Override
    public boolean canNeverScrap() {
        return true;
    }

    @Override
    public boolean isSalvaging() {
        return false;
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
        return TankLocation.TECH_ADVANCEMENT;
    }
}
