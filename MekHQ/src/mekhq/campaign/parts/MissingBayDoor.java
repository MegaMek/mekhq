/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import org.w3c.dom.Node;

/**
 * @author Neoancient
 */
public class MissingBayDoor extends MissingPart {
    public MissingBayDoor() {
        this(0, null);
    }

    public MissingBayDoor(int tonnage, Campaign c) {
        super(tonnage, false, c);
        name = "Bay Door";
    }

    @Override
    public String getName() {
        if (null != parentPart) {
            return parentPart.getName() + " Door";
        }
        return super.getName();
    }

    @Override
    public int getBaseTime() {
        return 600;
    }

    @Override
    public void updateConditionFromPart() {

    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if (null != replacement) {
            Part actualReplacement = replacement.clone();
            unit.addPart(actualReplacement);
            campaign.getQuartermaster().addPart(actualReplacement, 0);
            replacement.decrementQuantity();

            // Calling 'remove()' has the side effect of setting this.parentPart to null.
            // Issue #2878 - Missing Bay Door on reload.
            Part parentReference = parentPart;
            remove(false);

            if (null != parentReference) {
                parentReference.addChildPart(actualReplacement);
                parentReference.updateConditionFromPart();
            }
        }
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public int getDifficulty() {
        return -1;
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof BayDoor;
    }

    @Override
    public Part getNewPart() {
        return new BayDoor(getUnitTonnage(), campaign);
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
    }

    @Override
    public String getLocationName() {
        return null;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return new TechAdvancement(TechBase.ALL).setAdvancement(DATE_PS, DATE_PS, DATE_PS)
                     .setTechRating(TechRating.A)
                     .setAvailability(AvailabilityValue.A,
                           AvailabilityValue.A,
                           AvailabilityValue.A,
                           AvailabilityValue.A)
                     .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
