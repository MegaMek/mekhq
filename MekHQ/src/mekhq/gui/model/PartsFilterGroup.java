/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.model;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.kfs.KFBoom;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.parts.meks.MekCockpit;
import mekhq.campaign.parts.meks.MekGyro;
import mekhq.campaign.parts.meks.MekLifeSupport;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.meks.MekSensor;
import mekhq.campaign.parts.protomeks.ProtoMekArmActuator;
import mekhq.campaign.parts.protomeks.ProtoMekJumpJet;
import mekhq.campaign.parts.protomeks.ProtoMekLegActuator;
import mekhq.campaign.parts.protomeks.ProtoMekLocation;
import mekhq.campaign.parts.protomeks.ProtoMekSensor;

/**
 * The set of category filters shared by the Warehouse tab and the Parts Store dialog. Each group pairs a human-readable
 * name with the predicate that decides whether a given {@link Part} belongs to it. Keeping both concerns in one place
 * ensures the two screens always offer the same filters and classify parts identically.
 *
 * <p>The declaration order is the order the groups are presented to the player, so append new groups at the end to
 * keep
 * remembered filter selections stable.</p>
 */
public enum PartsFilterGroup {
    ALL("ALL") {
        @Override
        public boolean matches(Part part) {
            return true;
        }
    },
    ARMOR("ARMOR") {
        @Override
        public boolean matches(Part part) {
            // ProtoMekArmor and BAArmor are derived from Armor
            return part instanceof Armor;
        }
    },
    SYSTEM("SYSTEM") {
        @Override
        public boolean matches(Part part) {
            return (part instanceof MekLifeSupport) ||
                         (part instanceof MekSensor) ||
                         (part instanceof LandingGear) ||
                         (part instanceof Avionics) ||
                         (part instanceof FireControlSystem) ||
                         (part instanceof AeroSensor) ||
                         (part instanceof KFBoom) ||
                         (part instanceof DropshipDockingCollar) ||
                         (part instanceof JumpshipDockingCollar) ||
                         (part instanceof BayDoor) ||
                         (part instanceof Cubicle) ||
                         (part instanceof GravDeck) ||
                         (part instanceof VeeSensor) ||
                         (part instanceof VeeStabilizer) ||
                         (part instanceof ProtoMekSensor);
        }
    },
    EQUIPMENT("EQUIPMENT") {
        @Override
        public boolean matches(Part part) {
            return (part instanceof EquipmentPart) || (part instanceof ProtoMekJumpJet);
        }
    },
    LOCATIONS("LOCATIONS") {
        @Override
        public boolean matches(Part part) {
            return (part instanceof MekLocation) ||
                         (part instanceof TankLocation) ||
                         (part instanceof ProtoMekLocation);
        }
    },
    WEAPON("WEAPON") {
        @Override
        public boolean matches(Part part) {
            return (part instanceof EquipmentPart) && (((EquipmentPart) part).getType() instanceof WeaponType);
        }
    },
    AMMO("AMMO") {
        @Override
        public boolean matches(Part part) {
            return part instanceof AmmoStorage;
        }
    },
    MISC("MISC") {
        @Override
        public boolean matches(Part part) {
            return ((part instanceof EquipmentPart) && (((EquipmentPart) part).getType() instanceof MiscType)) ||
                         (part instanceof ProtoMekJumpJet);
        }
    },
    ENGINE("ENGINE") {
        @Override
        public boolean matches(Part part) {
            return part instanceof EnginePart;
        }
    },
    GYRO("GYRO") {
        @Override
        public boolean matches(Part part) {
            return part instanceof MekGyro;
        }
    },
    ACTUATORS("ACTUATORS") {
        @Override
        public boolean matches(Part part) {
            return (part instanceof MekActuator) ||
                         (part instanceof ProtoMekArmActuator) ||
                         (part instanceof ProtoMekLegActuator);
        }
    },
    COCKPIT("COCKPIT") {
        @Override
        public boolean matches(Part part) {
            return part instanceof MekCockpit;
        }
    },
    BA_SUIT("BA_SUIT") {
        @Override
        public boolean matches(Part part) {
            return part instanceof BattleArmorSuit;
        }
    },
    OMNI_POD("OMNI_POD") {
        @Override
        public boolean matches(Part part) {
            return part instanceof OmniPod;
        }
    },
    ARMOR_KIT("ARMOR_KIT") {
        @Override
        public boolean matches(Part part) {
            return (part instanceof EquipmentPart) &&
                         ((EquipmentPart) part).getType().hasFlag(MiscType.F_ARMOR_KIT);
        }
    };

    private final static String RESOURCE_BUNDLE = "mekhq.resources.PartsFilterGroup";

    private final String groupName;

    PartsFilterGroup(String lookupName) {
        this.groupName = getTextAt(RESOURCE_BUNDLE, lookupName + ".label");
    }

    /**
     * @return the human-readable name of this filter group, as shown in the group selector.
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * @param part the part to test
     *
     * @return {@code true} if the part belongs to this filter group.
     */
    public abstract boolean matches(Part part);

    /**
     * @param index the ordinal of the group to look up
     *
     * @return the display name of the group at the given ordinal, or {@code "?"} if the ordinal is out of range.
     */
    public static String getGroupName(int index) {
        PartsFilterGroup[] groups = values();
        return ((index >= 0) && (index < groups.length)) ? groups[index].getGroupName() : "?";
    }

    /**
     * @param index the ordinal of the group
     * @param part  the part to test
     *
     * @return {@code true} if the part belongs to the group at the given ordinal.
     */
    public static boolean matches(int index, Part part) {
        PartsFilterGroup[] groups = values();
        return ((index >= 0) && (index < groups.length)) && groups[index].matches(part);
    }
}
