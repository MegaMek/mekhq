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
 */
package mekhq.campaign.work;

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

    public int getTechBase();

    public int getTechLevel();

    public int getQuantity();

    public String getQuantityName(int quantity);

    public void incrementQuantity();

    public void decrementQuantity();

    public Money getBuyCost();

    public boolean isIntroducedBy(int year, boolean clan, int techFaction);

    public boolean isExtinctIn(int year, boolean clan, int techFaction);

    public int getAvailability();

    public String getShoppingListReport(int quantity);

}
