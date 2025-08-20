/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.work;

import megamek.common.ITechnology;
import megamek.common.ITechnology.AvailabilityValue;
import megamek.common.ITechnology.TechBase;
import megamek.common.TargetRoll;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

public interface IAcquisitionWork extends IWork {
    public String getAcquisitionName();

    public String getAcquisitionDisplayName();

    public Object getNewEquipment();

    public String getAcquisitionDesc();

    public String getAcquisitionExtraDesc();

    public String getAcquisitionBonus();

    public Part getAcquisitionPart();

    public Unit getUnit();

    public int getDaysToWait();

    public void resetDaysToWait();

    public void decrementDaysToWait();

    public String find(int transitDays);

    public String failToFind();

    public TargetRoll getAllAcquisitionMods();

    public TechBase getTechBase();

    public int getTechLevel();

    public int getQuantity();

    /**
     * Gets the true number of parts represented by this AcquisitionWork. An ammo part that
     * contains six shots should return six, not one.
     */
    public default int getTotalQuantity() {
        return getQuantity();
    }

    public String getQuantityName(int quantity);

    public void incrementQuantity();

    public void decrementQuantity();

    public Money getBuyCost();

    public default Money getTotalBuyCost() {
        return getBuyCost().multipliedBy(getQuantity());
    }

    public boolean isIntroducedBy(int year, boolean clan, ITechnology.Faction techFaction);

    public boolean isExtinctIn(int year, boolean clan, ITechnology.Faction techFaction);

    public AvailabilityValue getAvailability();

    public String getShoppingListReport(int quantity);

}
