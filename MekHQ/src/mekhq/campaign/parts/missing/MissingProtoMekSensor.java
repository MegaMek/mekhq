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
package mekhq.campaign.parts.missing;

import megamek.common.CriticalSlot;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.units.ProtoMek;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.protomeks.ProtoMekLocation;
import mekhq.campaign.parts.protomeks.ProtoMekSensor;
import org.w3c.dom.Node;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingProtoMekSensor extends MissingPart {
    public MissingProtoMekSensor() {
        this(0, null);
    }

    public MissingProtoMekSensor(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "ProtoMek Sensors";
    }

    @Override
    public int getBaseTime() {
        return 120;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public double getTonnage() {
        // TODO : how much do actuators weight?
        // apparently nothing
        return 0;
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {

    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_HEAD_CRIT, ProtoMek.LOC_HEAD, 1);
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        if (unit.isLocationBreached(ProtoMek.LOC_HEAD)) {
            return unit.getEntity().getLocationName(ProtoMek.LOC_HEAD) + " is breached.";
        }
        if (unit.isLocationDestroyed(ProtoMek.LOC_HEAD)) {
            return unit.getEntity().getLocationName(ProtoMek.LOC_HEAD) + " is destroyed.";
        }
        return null;
    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if (null != replacement) {
            Part actualReplacement = replacement.clone();
            unit.addPart(actualReplacement);
            campaign.getQuartermaster().addPart(actualReplacement, 0, false);
            replacement.changeQuantity(-1);
            remove(false);
            //assign the replacement part to the unit
            actualReplacement.updateConditionFromPart();
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof ProtoMekSensor
                     && getUnitTonnage() == part.getUnitTonnage();
    }

    @Override
    public Part getNewPart() {
        return new ProtoMekSensor(getUnitTonnage(), campaign);
    }

    @Override
    public String getLocationName() {
        return unit != null ? unit.getEntity().getLocationName(getLocation()) : null;
    }

    @Override
    public int getLocation() {
        return ProtoMek.LOC_HEAD;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return ProtoMekLocation.TECH_ADVANCEMENT;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ELECTRONICS;
    }
}
