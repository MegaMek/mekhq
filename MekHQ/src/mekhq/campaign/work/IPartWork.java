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

import megamek.common.MiscType;
import megamek.common.TargetRoll;
import megamek.common.WeaponType;
import megamek.common.annotations.Nullable;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public interface IPartWork extends IWork {

    String getPartName();

    int getSkillMin();

    int getBaseTime();
    int getActualTime();
    int getTimeSpent();
    int getTimeLeft();
    void addTimeSpent(int time);
    void resetTimeSpent();
    void resetOvertime();
    boolean isRightTechType(String skillType);
    default boolean canChangeWorkMode() {
        return false;
    }

    TargetRoll getAllModsForMaintenance();

    void setTech(@Nullable Person tech);

    boolean hasWorkedOvertime();
    void setWorkedOvertime(boolean b);
    int getShorthandedMod();
    void setShorthandedMod(int i);

    void updateConditionFromEntity(boolean checkForDestruction);
    void updateConditionFromPart();
    void fix();
    void remove(boolean salvage);
    MissingPart getMissingPart();

    String getDesc();

    /**
     * Gets a string containing details regarding the part,
     * e.g. OmniPod or how many hits it has taken and its
     * repair cost.
     * @return A string containing details regarding the part.
     */
    String getDetails();

    /**
     * Gets a string containing details regarding the part,
     * and optionally include information on its repair
     * status.
     * @param includeRepairDetails {@code true} if the details
     *        should include information such as the number of
     *        hits or how much it would cost to repair the
     *        part.
     * @return A string containing details regarding the part.
     */
    String getDetails(boolean includeRepairDetails);

    int getLocation();

    @Nullable Unit getUnit();

    boolean isSalvaging();

    @Nullable String checkFixable();

    void reservePart();

    void cancelReservation();

    boolean isBeingWorkedOn();

    PartRepairType getMRMSOptionType();

    PartRepairType getRepairPartType();

    static PartRepairType findCorrectMRMSType(IPartWork part) {
        if ((part instanceof EquipmentPart) && (((EquipmentPart) part).getType() instanceof WeaponType)) {
            return PartRepairType.WEAPON;
        } else {
            return part.getMRMSOptionType();
        }
    }

    static PartRepairType findCorrectRepairType(IPartWork part) {
        if (((part instanceof EquipmentPart) && (((EquipmentPart) part).getType() instanceof WeaponType))
                || ((part instanceof MissingEquipmentPart) && (((MissingEquipmentPart) part).getType() instanceof WeaponType))) {
            return PartRepairType.WEAPON;
        } else if ((part instanceof EquipmentPart) && (((EquipmentPart) part).getType().hasFlag(MiscType.F_CLUB))) {
            return PartRepairType.PHYSICAL_WEAPON;
        } else {
            return part.getRepairPartType();
        }
    }


    /**
     * Sticker price is the value of the part according to the rulebooks
     * @return the part's sticker price
     */
    public abstract Money getStickerPrice();

    /**
     * This is the value of the part that may be affected by characteristics and campaign options
     * @return the part's actual value
     */
    public abstract Money getActualValue();

    /**
     * This is the value of the part that may be affected by characteristics and campaign options
     * but not affected by part damage
     * @return the part's actual value if it wasn't damaged
     */
    public abstract Money getUndamagedValue();


    public abstract boolean isPriceAdjustedForAmount();
}
