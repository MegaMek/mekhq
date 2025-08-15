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

import java.util.StringJoiner;

import megamek.common.Aero;
import megamek.common.CriticalSlot;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.LandAirMek;
import megamek.common.Mek;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import org.w3c.dom.Node;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingLandingGear extends MissingPart {
    public MissingLandingGear() {
        this(0, null);
    }

    public MissingLandingGear(int tonnage, Campaign c) {
        super(0, c);
        this.name = "Landing Gear";
    }

    @Override
    public int getBaseTime() {
        if (campaign.getCampaignOptions().isUseAeroSystemHits()) {
            int time = 0;
            // Test of proposed errata for repair times
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                time = 1200;
            } else {
                time = 600;
            }
            return time;
        }
        return 1200;
    }

    @Override
    public int getDifficulty() {
        return 2;
    }

    @Override
    public @Nullable String checkFixable() {
        if ((unit != null) && (unit.getEntity() instanceof LandAirMek)) {
            // Landing Gear is installed in the CT and both Side Torsos,
            // make sure they're not missing.
            StringJoiner missingLocs = new StringJoiner(", ");
            for (Part part : unit.getParts()) {
                if (part instanceof MissingMekLocation) {
                    // The CT cannot be scrapped, so that check is elided.
                    switch (part.getLocation()) {
                        case Mek.LOC_LT:
                        case Mek.LOC_RT:
                            missingLocs.add(unit.getEntity().getLocationName(part.getLocation()));
                            break;
                        default:
                            break;
                    }
                }
            }

            return missingLocs.length() == 0
                         ? null
                         : "Cannot reinstall landing gear when missing: " + missingLocs;
        }
        return null;
    }

    @Override
    public Part getNewPart() {
        return new LandingGear(getUnitTonnage(), campaign);
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof LandingGear;
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public TechRating getTechRating() {
        //go with conventional fighter avionics
        return TechRating.B;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setGearHit(true);
        } else if (null != unit && unit.getEntity() instanceof LandAirMek) {
            unit.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR, 3);
        }
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        //nothing to load
    }

    @Override
    public String getLocationName() {
        if (null != unit) {
            return unit.getEntity().getLocationName(unit.getEntity().getBodyLocation());
        }
        return null;
    }

    @Override
    public int getLocation() {
        if (null != unit) {
            return unit.getEntity().getBodyLocation();
        }
        return Entity.LOC_NONE;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TA_GENERIC;
    }
}
