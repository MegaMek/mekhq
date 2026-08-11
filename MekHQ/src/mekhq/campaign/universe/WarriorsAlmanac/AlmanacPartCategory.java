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
package mekhq.campaign.universe.WarriorsAlmanac;

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.util.function.Predicate;

import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.TankLocation;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.parts.meks.MekGyro;
import mekhq.campaign.parts.meks.MekLifeSupport;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.meks.MekSensor;

public enum AlmanacPartCategory {
    ARMOR("WarriorsAlmanacDialog.partCategory.armor", part -> part instanceof Armor),
    WEAPONS("WarriorsAlmanacDialog.partCategory.weapons",
          part -> part instanceof EquipmentPart equipment && equipment.getType() instanceof WeaponType),
    AMMUNITION("WarriorsAlmanacDialog.partCategory.ammunition", part -> part instanceof AmmoStorage),
    MISCELLANEOUS_EQUIPMENT("WarriorsAlmanacDialog.partCategory.miscEquipment",
          part -> part instanceof EquipmentPart equipment && equipment.getType() instanceof MiscType),
    ENGINES("WarriorsAlmanacDialog.partCategory.engines", part -> part instanceof EnginePart),
    GYROS("WarriorsAlmanacDialog.partCategory.gyros", part -> part instanceof MekGyro),
    ACTUATORS("WarriorsAlmanacDialog.partCategory.actuators", part -> part instanceof MekActuator),
    EQUIPMENT("WarriorsAlmanacDialog.partCategory.equipment", part -> part instanceof EquipmentPart),
    SYSTEM_COMPONENTS("WarriorsAlmanacDialog.partCategory.systemComponents",
          part -> part instanceof MekLifeSupport || part instanceof MekSensor),
    LOCATIONS("WarriorsAlmanacDialog.partCategory.locations",
          part -> part instanceof MekLocation || part instanceof TankLocation),
    OTHER("WarriorsAlmanacDialog.partCategory.other", part -> true);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.WarriorsAlmanacDialog";

    private final String labelKey;
    private final Predicate<Part> matcher;

    AlmanacPartCategory(String labelKey, Predicate<Part> matcher) {
        this.labelKey = labelKey;
        this.matcher = matcher;
    }

    /**
     * @return the localized category name, used as the tab title
     */
    public String getLabel() {
        return getTextAt(RESOURCE_BUNDLE, labelKey);
    }

    /**
     * @return the localized introductory blurb for this category, falling back to a generic blurb if none is defined
     */
    public String getIntro() {
        final String text = getTextAt(RESOURCE_BUNDLE, labelKey + ".intro");
        if (isResourceKeyValid(text)) {
            return text;
        }
        return getTextAt(RESOURCE_BUNDLE, "WarriorsAlmanacDialog.partCategory.default.intro");
    }

    /**
     * Resolves the single almanac category for a part, checking categories most-specific-first.
     *
     * @param part the part to categorize
     *
     * @return the first matching category; never {@code null} (falls back to {@link #OTHER})
     */
    public static AlmanacPartCategory categorize(Part part) {
        for (AlmanacPartCategory category : values()) {
            if (category.matcher.test(part)) {
                return category;
            }
        }
        return OTHER;
    }
}
