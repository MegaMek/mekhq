/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.sorter;

import java.util.Comparator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;

public class PersonTitleStringSorter implements Comparator<String> {
    private static final MMLogger logger = MMLogger.create(PersonTitleStringSorter.class);

    // region Variable Declarations
    private final Campaign campaign;
    private final Pattern pattern = Pattern.compile("id=\"([^\"]+)\"");
    private final PersonTitleSorter personTitleSorter;
    // endregion Variable Declarations

    // region Constructors
    public PersonTitleStringSorter(final Campaign campaign) {
        this.campaign = campaign;
        this.personTitleSorter = new PersonTitleSorter();
    }
    // endregion Constructors

    // region Getters
    public Campaign getCampaign() {
        return campaign;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public PersonTitleSorter getPersonTitleSorter() {
        return personTitleSorter;
    }
    // endregion Getters

    @Override
    public int compare(final @Nullable String s0, final @Nullable String s1) {
        // First we need to compare for null or "-" values, as those are used for absent
        // values
        // on the front-end
        if ((s0 == null) && (s1 == null)) {
            return 0;
        } else if (s0 == null) {
            return -1;
        } else if (s1 == null) {
            return 1;
        } else if (s0.equals(s1)) {
            return 0;
        } else if ("-".equals(s0)) {
            return -1;
        } else if ("-".equals(s1)) {
            return 1;
        }

        try {
            // get the numbers associated with each rank string, and compare
            Matcher matcher = getPattern().matcher(s0);
            matcher.find();
            final String id0 = matcher.group(1);
            matcher = getPattern().matcher(s1);
            matcher.find();
            final String id1 = matcher.group(1);

            return getPersonTitleSorter().compare(getCampaign().getPerson(UUID.fromString(id0)),
                  getCampaign().getPerson(UUID.fromString(id1)));
        } catch (Exception e) {
            logger.error(String.format("s0: %s, s1: %s", s0, s1), e);
            return 0;
        }
    }
}
