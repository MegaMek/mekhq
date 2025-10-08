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
import megamek.common.units.Entity;
import megamek.common.units.ProtoMek;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.protomeks.ProtoMekJumpJet;
import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingProtoMekJumpJet extends MissingPart {
    public MissingProtoMekJumpJet() {
        this(0, null);
    }

    public MissingProtoMekJumpJet(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "ProtoMek Jump Jet";
    }

    @Override
    public int getBaseTime() {
        return 60;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public double getTonnage() {
        if (getUnitTonnage() <= 5) {
            return 0.05;
        } else {
            return 0.1;
        }
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {

    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            int damageJJ = getOtherDamagedJumpJets() + 1;
            if (damageJJ < (int) Math.ceil(unit.getEntity().getOriginalJumpMP() / 2.0)) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSO_CRIT, ProtoMek.LOC_TORSO);
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSO_CRIT, ProtoMek.LOC_TORSO, 1);
            } else {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSO_CRIT, ProtoMek.LOC_TORSO);
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSO_CRIT, ProtoMek.LOC_TORSO, 2);
            }
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        if (unit.isLocationBreached(ProtoMek.LOC_TORSO)) {
            return unit.getEntity().getLocationName(ProtoMek.LOC_TORSO) + " is breached.";
        }
        if (unit.isLocationDestroyed(ProtoMek.LOC_TORSO)) {
            return unit.getEntity().getLocationName(ProtoMek.LOC_TORSO) + " is destroyed.";
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
        return part instanceof ProtoMekJumpJet && getUnitTonnage() == part.getUnitTonnage();
    }

    @Override
    public Part getNewPart() {
        return new ProtoMekJumpJet(getUnitTonnage(), campaign);
    }

    private int getOtherDamagedJumpJets() {
        int damagedJJ = 0;
        if (null != unit) {
            for (Part p : unit.getParts()) {
                if (p.getId() == this.getId()) {
                    continue;
                }
                if (p instanceof MissingProtoMekJumpJet
                          || (p instanceof ProtoMekJumpJet && p.needsFixing())) {
                    damagedJJ++;
                }
            }
        }
        return damagedJJ;
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
        return ProtoMekJumpJet.TECH_ADVANCEMENT;
    }

}
