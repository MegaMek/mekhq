/*
 * Copyright (C) 2020 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.utilities;

import megamek.common.EquipmentType;
import megamek.common.WeaponType;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.work.IPartWork;

public class PartWorkImageSelector {

    /**
     * Find the most appropriate image for a given {@see IPartWork}.
     * @param part The {@see PartWork} to select the image for.
     * @return An array containing the path parts pointing to the image.
     */
    public static String[] findPartImage(IPartWork part) {
        String imgBase = null;
        PartRepairType repairType = IPartWork.findCorrectRepairType(part);

        switch (repairType) {
            case ARMOR:
                imgBase = "armor";
                break;
            case AMMO:
                imgBase = "ammo";
                break;
            case ACTUATOR:
                imgBase = "actuator";
                break;
            case ENGINE:
                imgBase = "engine";
                break;
            case ELECTRONICS:
                imgBase = "electronics";
                break;
            case HEAT_SINK:
                imgBase = "heatsink";
                break;
            case WEAPON:
                EquipmentType equipmentType = null;

                if (part instanceof EquipmentPart) {
                    equipmentType = ((EquipmentPart) part).getType();
                } else if (part instanceof MissingEquipmentPart) {
                    equipmentType = ((MissingEquipmentPart) part).getType();
                }

                if (equipmentType != null) {
                    if (equipmentType.hasFlag(WeaponType.F_LASER)) {
                        imgBase = "laser";
                    } else if (equipmentType.hasFlag(WeaponType.F_MISSILE)) {
                        imgBase = "missile";
                    } else if (equipmentType.hasFlag(WeaponType.F_BALLISTIC)) {
                        imgBase = "ballistic";
                    } else if (equipmentType.hasFlag(WeaponType.F_ARTILLERY)) {
                        imgBase = "artillery";
                    }
                }

                break;
            case MEK_LOCATION:
            case POD_SPACE:
                imgBase = "location_mek";
                break;
            case PHYSICAL_WEAPON:
                imgBase = "melee";
                break;
            default:
                break;
        }

        if (imgBase == null) {
            imgBase = "equipment";
        }


        String[] imgData = new String[2];
        imgData[0] = "data/images/misc/repair/";
        imgData[1] = imgBase;

        return imgData;
    }  
}
