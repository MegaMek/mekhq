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
 */

package mekhq.campaign.parts;

import megamek.common.annotations.Nullable;
import org.w3c.dom.Node;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.TechAdvancement;
import megamek.common.VTOL;
import mekhq.campaign.Campaign;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingRotor extends MissingPart {
    public MissingRotor() {
        this(0, null);
    }

    public MissingRotor(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Rotor";
    }

    @Override
    public int getBaseTime() {
        return 300;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // Do nothing - no fields to load.
    }

    @Override
    public int getTechRating() {
        return EquipmentType.RATING_B;
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof Rotor && part.getUnitTonnage() == getUnitTonnage();
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public Part getNewPart() {
        //TODO: how to get second turret location?
        return new Rotor(getUnitTonnage(), campaign);
    }

    @Override
    public double getTonnage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void fix() {
        if ((null != unit) && (unit.getEntity() instanceof VTOL)) {
            int maxIsVal = unit.getEntity().getOInternal(VTOL.LOC_ROTOR);
            unit.getEntity().setInternal(maxIsVal, VTOL.LOC_ROTOR);
        }

        super.fix();
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof VTOL) {
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, VTOL.LOC_ROTOR);
        }
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
        return Rotor.TECH_ADVANCEMENT;
    }

}
