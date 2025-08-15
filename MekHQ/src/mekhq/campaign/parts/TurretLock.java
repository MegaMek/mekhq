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

import megamek.common.Entity;
import megamek.common.Tank;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class TurretLock extends Part {
    public TurretLock() {
        // Needed for loading from save
        this(null);
    }

    public TurretLock(Campaign c) {
        super(0, c);
        this.name = "Turret Lock";
    }

    @Override
    public int getBaseTime() {
        return 90;
    }

    @Override
    public int getDifficulty() {
        return -1;
    }

    @Override
    public TurretLock clone() {
        TurretLock clone = new TurretLock(campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public Money getStickerPrice() {
        return Money.zero();
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof TurretLock;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        // Just use Part's writer
        indent = writeToXMLBegin(pw, indent);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // Since we're not adding any bits above Part, think this can be empty
    }

    @Override
    public @Nullable String checkFixable() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit && unit.getEntity() instanceof Tank) {
            ((Tank) unit.getEntity()).unlockTurret();
        }
    }

    @Override
    public MissingPart getMissingPart() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void remove(boolean salvage) {
        //nothing to do here
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        //nothing to do here because we are just going to check directly in needsFixing()
        //since this "part" can never be removed
    }

    @Override
    public void updateConditionFromPart() {
        //nothing to do here
    }

    @Override
    public boolean needsFixing() {
        if (null != unit && unit.getEntity() instanceof Tank) {
            return ((Tank) unit.getEntity()).isTurretLocked(Tank.LOC_TURRET);
        }
        return false;
    }

    @Override
    public boolean isSalvaging() {
        return false;
    }

    @Override
    public String checkScrappable() {
        return "Turret Lock is not scrappable";
    }

    @Override
    public boolean canNeverScrap() {
        return true;
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
        return TA_GENERIC;
    }
}
