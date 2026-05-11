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
package mekhq.campaign.universe.companyGeneration.ratgen;

import megamek.client.ratgenerator.CrewDescriptor;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Person;

/**
 * Seeds a freshly-generated MekHQ {@link Person} from a MegaMek {@link CrewDescriptor} produced by the
 * Force Generator engine.
 *
 * <p>The descriptor carries name, gunnery skill, piloting skill, and a rank index. MekHQ's personnel
 * generator already produces a fully-formed {@link Person} with skills, callsign, advantages, education
 * etc. via {@code campaign.newPerson(role, generator)}; this adapter overrides the descriptor-supplied
 * name and rank on top of that. Skill values are not transferred in Phase 1: MekHQ's generator decides
 * skills based on the campaign's selected skill setting, and the descriptor's gunnery/piloting numbers
 * are coarser. Phase 2 may revisit if alignment drifts noticeably.</p>
 */
public final class CrewDescriptorAdapter {

    private static final MMLogger LOGGER = MMLogger.create(CrewDescriptorAdapter.class);

    private CrewDescriptorAdapter() {
        // utility class
    }

    /**
     * Applies the descriptor's name (and any other lightweight metadata) to the given Person. Skills,
     * callsign, advantages, etc. remain whatever {@code campaign.newPerson} set up.
     *
     * @param descriptor the source descriptor from {@code ForceDescriptor.getCo()}; if {@code null}, this
     *                   method is a no-op
     * @param person     the target Person; must be non-null
     * @param overrideName when {@code true}, the descriptor's name replaces MekHQ's randomly assigned
     *                     name; when {@code false}, MekHQ's name is kept
     */
    public static void apply(CrewDescriptor descriptor, Person person, boolean overrideName) {
        if (descriptor == null || person == null) {
            LOGGER.info("[CompanyGen]         CrewDescriptorAdapter.apply skipped (descriptor or person null)");
            return;
        }
        if (overrideName) {
            String fullName = descriptor.getName();
            if (fullName != null && !fullName.isBlank()) {
                int firstSpace = fullName.indexOf(' ');
                if (firstSpace > 0 && firstSpace < fullName.length() - 1) {
                    person.setGivenName(fullName.substring(0, firstSpace));
                    person.setSurname(fullName.substring(firstSpace + 1));
                } else {
                    person.setGivenName(fullName);
                    person.setSurname("");
                }
                person.setFullName();
                LOGGER.info("[CompanyGen]         CrewDescriptorAdapter.apply renamed person to '{}' (gunnery={} piloting={} rank={})",
                      fullName, descriptor.getGunnery(), descriptor.getPiloting(), descriptor.getRank());
            } else {
                LOGGER.info("[CompanyGen]         CrewDescriptorAdapter.apply: descriptor has blank name, keeping random");
            }
        }
    }
}
