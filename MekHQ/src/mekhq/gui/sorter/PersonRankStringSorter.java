/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.sorter;

import java.util.Comparator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;

/**
 * A comparator for ranks written as strings with "-" sorted to the bottom
 * always
 *
 * @author Jay Lawson
 */
public class PersonRankStringSorter implements Comparator<String> {
    private static final MMLogger logger = MMLogger.create(PersonRankStringSorter.class);

    // region Variable Declarations
    private final Campaign campaign;
    private final Pattern pattern = Pattern.compile("id=\"([^\"]+)\"");
    private final PersonRankSorter personRankSorter;
    // endregion Variable Declarations

    // region Constructors
    public PersonRankStringSorter(final Campaign campaign) {
        this.campaign = campaign;
        this.personRankSorter = new PersonRankSorter(new NaturalOrderComparator());
    }
    // endregion Constructors

    // region Getters
    public Campaign getCampaign() {
        return campaign;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public PersonRankSorter getPersonRankSorter() {
        return personRankSorter;
    }
    // endregion Getters

    @Override
    public int compare(final String s0, final String s1) {
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

            return getPersonRankSorter().compare(getCampaign().getPerson(UUID.fromString(id0)),
                    getCampaign().getPerson(UUID.fromString(id1)));
        } catch (Exception e) {
            logger.error(String.format("s0: %s, s1: %s", s0, s1), e);
            return 0;
        }
    }
}
