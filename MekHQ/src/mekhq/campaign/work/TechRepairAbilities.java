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
package mekhq.campaign.work;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;

/**
 * The special abilities that change a tech's repair, maintenance or fabrication work, so the Repair Bay can show at a
 * glance which tech has which. For example, a tech with Weapon Specialist is worth saving for a weapon repair, and a
 * tech with Gremlins is one to keep away from a delicate one.
 */
public final class TechRepairAbilities {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";

    /** Every ability the repair, maintenance and fabrication rules check, in the order they are listed */
    static final List<String> REPAIR_ABILITIES = List.of(PersonnelOptions.TECH_ENGINEER,
          PersonnelOptions.TECH_MAINTAINER,
          PersonnelOptions.TECH_FIXER,
          PersonnelOptions.TECH_WEAPON_SPECIALIST,
          PersonnelOptions.TECH_ARMOR_SPECIALIST,
          PersonnelOptions.TECH_INTERNAL_SPECIALIST,
          PersonnelOptions.TECH_FABRICATOR,
          PersonnelOptions.TECH_JURY_RIGGER,
          PersonnelOptions.TECH_MACGYVER,
          PersonnelOptions.TECH_MUNITIONEER,
          PersonnelOptions.ATOW_TECH_EMPATHY,
          PersonnelOptions.TECH_WASTEFUL,
          PersonnelOptions.FLAW_GREMLINS);

    private TechRepairAbilities() {}

    /**
     * @param tech a tech
     *
     * @return the tech's abilities that change repair work, in the order they are listed
     */
    public static List<SpecialAbility> find(Person tech) {
        List<SpecialAbility> abilities = new ArrayList<>();
        for (String abilityName : REPAIR_ABILITIES) {
            SpecialAbility ability = SpecialAbility.getAbility(abilityName);
            if ((ability != null) && tech.getOptions().booleanOption(abilityName)) {
                abilities.add(ability);
            }
        }
        return abilities;
    }

    /**
     * @param tech a tech
     *
     * @return one line naming the tech's repair abilities, for example {@code Abilities: Weapon Specialist,
     *       Gremlins}, or an empty string if they have none
     */
    public static String describeNames(Person tech) {
        List<String> names = find(tech).stream().map(SpecialAbility::getDisplayName).toList();
        if (names.isEmpty()) {
            return "";
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "TechRepairAbilities.names", String.join(", ", names));
    }

    /**
     * @param tech a tech
     *
     * @return what each of the tech's repair abilities does, one to a line, as HTML without {@code <html>} tags, or
     *       {@code null} if they have none
     */
    public static @Nullable String describeEffects(Person tech) {
        List<SpecialAbility> abilities = find(tech);
        if (abilities.isEmpty()) {
            return null;
        }
        StringBuilder effects = new StringBuilder();
        for (SpecialAbility ability : abilities) {
            if (!effects.isEmpty()) {
                effects.append("<br>");
            }
            effects.append(getFormattedTextAt(RESOURCE_BUNDLE, "TechRepairAbilities.effect", ability.getDisplayName(),
                  ability.getDescription()));
        }
        return effects.toString();
    }
}
