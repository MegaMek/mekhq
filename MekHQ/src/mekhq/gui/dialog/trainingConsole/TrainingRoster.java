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
package mekhq.gui.dialog.trainingConsole;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.Nullable;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * Everyone the Training Console can train, read once when it opens or after a commit: every active person, prisoners
 * included but marked, with what each list row and filter needs worked out up front.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class TrainingRoster {
    /**
     * One person as the console lists them.
     *
     * @param person        the person
     * @param name          their full name, for sorting
     * @param title         their name with rank, for the list
     * @param role          their primary role
     * @param roleName      their primary role's name
     * @param formationName the formation their unit is in, or an empty string
     * @param skillLevel    their experience level
     * @param isOfficer     {@code true} if their rank is an officer rank
     * @param isPrisoner    {@code true} if they are a prisoner
     * @param search        what the search field matches against, in lower case
     */
    record PersonEntry(Person person, String name, String title, PersonnelRole role, String roleName,
          String formationName, SkillLevel skillLevel, boolean isOfficer, boolean isPrisoner, String search) {}

    private final List<PersonEntry> entries = new ArrayList<>();
    private final Map<UUID, PersonEntry> byId = new HashMap<>();

    TrainingRoster(Campaign campaign) {
        CampaignOptions options = campaign.getCampaignOptions();
        boolean isClan = campaign.getPlayerForce().isClanForce();
        LocalDate today = campaign.getLocalDate();
        for (Person person : campaign.getPlayerForce().getHumanResources().getPersonnel()) {
            if (!person.getStatus().isActiveFlexible()) {
                continue;
            }
            PersonnelRole role = person.getPrimaryRole();
            String roleName = role.getLabel(isClan);
            Formation formation = campaign.getPlayerForce().getFormationFor(person);
            String formationName = (formation == null) ? "" : formation.getFullName();
            SkillLevel level = person.getSkillLevel(options, isClan, today, false, true);
            boolean isOfficer = person.getRank().isOfficer();
            boolean isPrisoner = person.getPrisonerStatus().isCurrentPrisoner();
            String title = person.getFullTitle();
            String search = (title + ' ' + roleName + ' ' + formationName).toLowerCase(Locale.ROOT);
            PersonEntry entry = new PersonEntry(person, person.getFullName(), title, role, roleName, formationName,
                  level, isOfficer, isPrisoner, search);
            entries.add(entry);
            byId.put(person.getId(), entry);
        }
        entries.sort(Comparator.comparing(PersonEntry::name));
    }

    /** @return everyone, by name */
    List<PersonEntry> entries() {
        return entries;
    }

    /** @return the person's entry, or {@code null} if the console doesn't list them */
    @Nullable PersonEntry entryFor(Person person) {
        return byId.get(person.getId());
    }
}
