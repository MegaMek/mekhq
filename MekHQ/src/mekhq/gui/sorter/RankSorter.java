/*
 * Copyright (c) 2013-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.sorter;

import java.io.Serializable;
import java.util.Comparator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

/**
 * A comparator for ranks written as strings with "-" sorted to the bottom always
 * @author Jay Lawson
 */
public class RankSorter implements Comparator<String>, Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = -7004206878096279028L;
    private final Campaign campaign;
    private final Pattern pattern = Pattern.compile("id=\"([^\"]+)\"");
    private final NaturalOrderComparator naturalOrderComparator = new NaturalOrderComparator();
    private final PrisonerStatusSorter prisonerStatusSorter = new PrisonerStatusSorter();
    //endregion Variable Declarations

    //region Constructors
    public RankSorter(final Campaign campaign) {
        this.campaign = campaign;
    }
    //endregion Constructors

    //region Getters
    public Campaign getCampaign() {
        return campaign;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public NaturalOrderComparator getNaturalOrderComparator() {
        return naturalOrderComparator;
    }

    public PrisonerStatusSorter getPrisonerStatusSorter() {
        return prisonerStatusSorter;
    }
    //endregion Getters

    @Override
    public int compare(final String s0, final String s1) {
        // get the numbers associated with each rank string, and compare
        Matcher matcher = getPattern().matcher(s0);
        matcher.find();
        final String s00 = matcher.group(1);
        matcher = getPattern().matcher(s1);
        matcher.find();
        final String s11 = matcher.group(1);
        try {
            final Person p0 = getCampaign().getPerson(UUID.fromString(s00));
            final Person p1 = getCampaign().getPerson(UUID.fromString(s11));

            // First we sort based on prisoner status
            final int prisonerStatusComparison = getPrisonerStatusSorter().compare(
                    p0.getPrisonerStatus(), p1.getPrisonerStatus());
            if (prisonerStatusComparison != 0) {
                return prisonerStatusComparison;
            }

            // Both have the same prisoner status, so now we sort based on the ranks
            // This is done in the following way:
            // 1. Rank Numeric
            // 2. Rank Level
            // 3. Manei Domini Rank
            // 4. Rank Name (natural order)
            if (p0.getRankNumeric() == p1.getRankNumeric()) {
                if (p0.getRankLevel() == p1.getRankLevel()) {
                    if (p0.getManeiDominiRank() == p1.getManeiDominiRank()) {
                        return getNaturalOrderComparator().compare(s0, s1);
                    } else {
                        return Integer.compare(p0.getManeiDominiRank().ordinal(), p1.getManeiDominiRank().ordinal());
                    }
                } else {
                    return Integer.compare(p0.getRankLevel(), p1.getRankLevel());
                }
            } else {
                return Integer.compare(p0.getRankNumeric(), p1.getRankNumeric());
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(String.format("RankSorter Exception, s0: %s, s1: %s", s00, s11), e);
            return 0;
        }
    }
}
