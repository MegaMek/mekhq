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

import java.util.StringJoiner;

import megamek.common.CriticalSlot;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.enums.TechRating;
import megamek.common.units.Aero;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Avionics;
import mekhq.campaign.parts.Part;
import org.w3c.dom.Node;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingAvionics extends MissingPart {
    public MissingAvionics() {
        this(0, null);
    }

    public MissingAvionics(int tonnage, Campaign c) {
        super(0, c);
        this.name = "Avionics";
    }

    @Override
    public int getBaseTime() {
        if (campaign.getCampaignOptions().isUseAeroSystemHits()) {
            int time;
            // Test of proposed errata for repair times
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                time = 2400;
            } else {
                time = 600;
            }
            return time;
            // CamOps, 3rd printing, page 207: 80 hours for Large Craft, 8 hours otherwise
        } else if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
            return 4800;
        } else {
            return 480;
        }
    }

    @Override
    public int getDifficulty() {
        return 1;
    }

    @Override
    public @Nullable String checkFixable() {
        if ((unit != null) && (unit.getEntity() instanceof LandAirMek)) {
            // Avionics are installed in the Head and both Torsos,
            // make sure they're not missing.
            StringJoiner missingLocs = new StringJoiner(", ");
            for (Part part : unit.getParts()) {
                if (part instanceof MissingMekLocation) {
                    switch (part.getLocation()) {
                        case Mek.LOC_HEAD:
                        case Mek.LOC_LEFT_TORSO:
                        case Mek.LOC_RIGHT_TORSO:
                            missingLocs.add(unit.getEntity().getLocationName(part.getLocation()));
                            break;
                        default:
                            break;
                    }
                }
            }

            return missingLocs.length() == 0
                         ? null
                         : "Cannot reinstall avionics when missing: " + missingLocs;
        }

        return null;
    }

    @Override
    public Part getNewPart() {
        return new Avionics(getUnitTonnage(), campaign);
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof Avionics;
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public TechRating getTechRating() {
        // go with conventional fighter avionics
        return TechRating.B;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setAvionicsHits(3);
        } else if (null != unit && unit.getEntity() instanceof LandAirMek) {
            unit.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_AVIONICS, 3);
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
        return Part.TA_GENERIC;
    }
}
