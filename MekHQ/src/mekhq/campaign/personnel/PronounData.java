/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel;

import static mekhq.campaign.personnel.enums.GenderDescriptors.HE_SHE_THEY;
import static mekhq.campaign.personnel.enums.GenderDescriptors.HIM_HER_THEM;
import static mekhq.campaign.personnel.enums.GenderDescriptors.HIS_HER_THEIR;

import megamek.common.enums.Gender;

/**
 * Represents a set of grammatical pronouns associated with a subject, object, and possessive form, along with their
 * lowercased variations, and additional data for pluralization handling.
 *
 * <p>This record encapsulates information related to personal pronouns for use in linguistic or grammatical
 * processing. It includes pronoun forms commonly required for sentence construction and supports pluralization when
 * applicable.</p>
 *
 * @param subjectPronoun             The subject pronoun (e.g., "He", "She", "They").
 * @param subjectPronounLowerCase    The lowercased version of the subject pronoun.
 * @param objectPronoun              The object pronoun (e.g., "Him", "Her", "Them").
 * @param objectPronounLowerCase     The lowercased version of the object pronoun.
 * @param possessivePronoun          The possessive pronoun (e.g., "His", "Hers", "Theirs").
 * @param possessivePronounLowerCase The lowercased version of the possessive pronoun.
 * @param pluralizer                 An integer value to represent singular (1) or plural (0 for gender-neutral).
 */
public record PronounData(
      String subjectPronoun,
      String subjectPronounLowerCase,
      String objectPronoun,
      String objectPronounLowerCase,
      String possessivePronoun,
      String possessivePronounLowerCase,
      int pluralizer
) {
    /**
     * Constructs a new {@code PronounData} record based on the specified gender.
     *
     * @param gender The gender used to determine the pronouns and pluralizer.
     */
    public PronounData(Gender gender) {
        this(
              HE_SHE_THEY.getDescriptorCapitalized(gender),
              HE_SHE_THEY.getDescriptorCapitalized(gender).toLowerCase(),
              HIM_HER_THEM.getDescriptorCapitalized(gender),
              HIM_HER_THEM.getDescriptorCapitalized(gender).toLowerCase(),
              HIS_HER_THEIR.getDescriptorCapitalized(gender),
              HIS_HER_THEIR.getDescriptorCapitalized(gender).toLowerCase(),
              gender.isGenderNeutral() ? 0 : 1 // Used to determine whether to use a plural case
        );
    }
}
