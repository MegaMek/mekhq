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
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.parts.missing.MissingVeeSensor;
import mekhq.campaign.personnel.skills.SkillType;
import org.w3c.dom.Node;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class VeeSensor extends Part {
    public VeeSensor() {
        this(0, null);
    }

    public VeeSensor(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Vehicle Sensors";
    }

    @Override
    public VeeSensor clone() {
        VeeSensor clone = new VeeSensor(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof VeeSensor;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // Do nothing.
    }

    @Override
    public void fix() {
        super.fix();
        if ((null != unit) && (unit.getEntity() instanceof Tank)) {
            ((Tank) unit.getEntity()).setSensorHits(0);
        }
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingVeeSensor(getUnitTonnage(), campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if ((null != unit) && (unit.getEntity() instanceof Tank)) {
            ((Tank) unit.getEntity()).setSensorHits(4);
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
        if ((null != unit) && (unit.getEntity() instanceof Tank)) {
            int priorHits = hits;
            hits = ((Tank) unit.getEntity()).getSensorHits();
            if (checkForDestruction && (hits > priorHits)
                      && (Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget())) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        return isSalvaging() ? 260 : 75;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public void updateConditionFromPart() {
        if ((null != unit) && (unit.getEntity() instanceof Tank)) {
            ((Tank) unit.getEntity()).setSensorHits(hits);
        }
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public Money getStickerPrice() {
        return Money.zero();
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MECHANIC);
    }

    @Override
    public String getLocationName() {
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

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ELECTRONICS;
    }
}
