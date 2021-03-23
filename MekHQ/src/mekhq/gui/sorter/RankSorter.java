/*
 * Copyright (c) 2013, 2020 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

/**
 * A comparator for ranks written as strings with "-" sorted to the bottom always
 * @author Jay Lawson
 */
public class RankSorter implements Comparator<String>, Serializable {
    private static final long serialVersionUID = -7004206878096279028L;
    private PrisonerStatusSorter prisonerStatusSorter = new PrisonerStatusSorter();
    private Campaign campaign;
    private Pattern pattern;

    public RankSorter(Campaign c) {
        campaign = c;
        pattern = Pattern.compile("id=\"([^\"]+)\"");
    }

    @Override
    public int compare(String s0, String s1) {
        // get the numbers associated with each rank string, and compare
        Matcher matcher = pattern.matcher(s0);
        matcher.find();
        String s00 = matcher.group(1);
        matcher = pattern.matcher(s1);
        matcher.find();
        String s11 = matcher.group(1);
        try {
            Person p0 = campaign.getPerson(UUID.fromString(s00));
            Person p1 = campaign.getPerson(UUID.fromString(s11));

            // First we sort based on prisoner status
            int prisonerStatusComparison = prisonerStatusSorter.compare(p0.getPrisonerStatus(), p1.getPrisonerStatus());
            if (prisonerStatusComparison != 0) {
                return prisonerStatusComparison;
            }

            // Both have the same prisoner status, so now we sort based on the ranks
            // the rank orders match, try comparing the levels
            if (p0.getRankNumeric() == p1.getRankNumeric()) {
                // the levels match too, try comparing MD rank
                if (p0.getRankLevel() == p1.getRankLevel()) {
                    if (p0.getManeiDominiRank() == p1.getManeiDominiRank()) {
                        return s0.compareTo(s1);
                    }
                    return Integer.compare(p0.getManeiDominiRank().ordinal(), p1.getManeiDominiRank().ordinal());
                }
                return Integer.compare(p0.getRankLevel(), p1.getRankLevel());
            }
            return Integer.compare(p0.getRankNumeric(), p1.getRankNumeric());
        } catch (Exception e) {
            MekHQ.getLogger().error(String.format("RankSorter Exception, s0: %s, s1: %s", s00, s11), e);
            return 0;
        }
    }
}
