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
package mekhq.campaign.io;

import java.util.Map;

import jakarta.annotation.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MULParser;
import megamek.logging.MMLogger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Keeps campaigns loading after MegaMek removes equipment they still name.
 *
 * <p>MegaMek removed the playtest Armor-Piercing, Precision and Rotary AC Caseless ammo when their rules became
 * standard. Campaigns saved before then still name the playtest ammo in unit loadouts, warehouse parts and shopping
 * list items; those names are switched to the standard munition that replaced them.</p>
 */
public final class RemovedEquipmentNames {
    private static final MMLogger LOGGER = MMLogger.create(RemovedEquipmentNames.class);

    /**
     * The munition name of each removed playtest AC ammo, mapped to the munition name of the standard ammo that
     * replaced it. The munition name is part of every lookup and display name of the ammo, such as "IS Armor-Piercing
     * Playtest Ammo AC/5", "Autocannon/5 Ammo - Armor-Piercing Playtest" or "ISRotaryAC5 Playtest Caseless Ammo".
     */
    private static final Map<String, String> REMOVED_PLAYTEST_MUNITION_NAMES = Map.of(
          "Armor-Piercing Playtest", "Armor-Piercing",
          "Precision Playtest", "Precision",
          "Playtest Caseless", "Caseless");

    private RemovedEquipmentNames() {
    }

    /**
     * Returns the name to look up for a saved equipment type name. When the saved name no longer finds an equipment
     * type but names equipment MegaMek removed, this is the name of its replacement; otherwise the saved name is
     * returned unchanged.
     *
     * @param typeName the equipment type name from the save
     *
     * @return the name of the replacement equipment, or {@code typeName} when there is nothing to replace
     */
    public static @Nullable String replaceRemovedTypeName(@Nullable String typeName) {
        if ((typeName == null) || typeName.isBlank() || (EquipmentType.get(typeName) != null)) {
            return typeName;
        }

        String replacementName = replaceRemovedMunitionNames(typeName);
        if (replacementName.equals(typeName) || (EquipmentType.get(replacementName) == null)) {
            return typeName;
        }

        LOGGER.info("Replacing removed equipment \"{}\" with \"{}\"", typeName, replacementName);
        return replacementName;
    }

    /**
     * Replaces the munition name of removed playtest ammo in any lookup or display name with the munition name of the
     * standard ammo that replaced it. Names without a removed munition name are returned unchanged.
     *
     * @param name a lookup or display name of equipment
     *
     * @return the name with removed munition names replaced, or {@code null} when {@code name} is {@code null}
     */
    public static @Nullable String replaceRemovedMunitionNames(@Nullable String name) {
        if (name == null) {
            return null;
        }

        String replacedName = name;
        for (Map.Entry<String, String> removedMunition : REMOVED_PLAYTEST_MUNITION_NAMES.entrySet()) {
            replacedName = replacedName.replace(removedMunition.getKey(), removedMunition.getValue());
        }
        return replacedName;
    }

    /**
     * Switches removed equipment named by the slots of a saved entity to its replacement, so MegaMek's
     * {@link MULParser} can load the slot's equipment and ammo instead of dropping it.
     *
     * @param entityElement the saved entity element, before it is passed to {@link MULParser}
     */
    public static void replaceRemovedEquipmentInEntity(Element entityElement) {
        NodeList slots = entityElement.getElementsByTagName(MULParser.ELE_SLOT);
        for (int slotIndex = 0; slotIndex < slots.getLength(); slotIndex++) {
            Element slot = (Element) slots.item(slotIndex);
            replaceRemovedTypeNameAttribute(slot, MULParser.ATTR_TYPE);
            replaceRemovedTypeNameAttribute(slot, MULParser.ATTR_MUNITION);
        }
    }

    private static void replaceRemovedTypeNameAttribute(Element element, String attributeName) {
        String typeName = element.getAttribute(attributeName);
        String replacementName = replaceRemovedTypeName(typeName);
        if ((replacementName != null) && !replacementName.equals(typeName)) {
            element.setAttribute(attributeName, replacementName);
        }
    }
}
