/*
 * Copyright (c) 2013 - Jay Lawson <jaylawson39 at yahoo.com>. All Rights Reserved.
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.ranks;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

/**
 * A specific rank with information about officer status and payment multipliers
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Rank implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 1677999967776587426L;

    // Rank Size Codes
    // Enlisted
    public static final int RE_MIN	= 0; // Rank "None"
    public static final int RE_MAX	= 20;
    public static final int RE_NUM	= 21;
    // Warrant Officers
    public static final int RWO_MIN	= 21;
    public static final int RWO_MAX	= 30;
    public static final int RWO_NUM	= 31; // Number that comes after RWO_MAX
    // Officers
    public static final int RO_MIN	= 31;
    public static final int RO_MAX	= 50;
    public static final int RO_NUM	= 51; // Number that comes after RO_MAX
    // Total
    public static final int RC_NUM	= 51; // Same as RO_MAX+1

    private List<String> rankNames;
    private boolean officer;
    private double payMultiplier;
    private List<Integer> rankLevels;
    //endregion Variable Declarations

    //region Constructors
    public Rank() {
        this(new ArrayList<>(), false, 1.0);
    }

    public Rank(final String[] names) {
        this(names, false, 1.0);
    }

    public Rank(final List<String> names) {
        this(names, false, 1.0);
    }

    public Rank(final String[] name, final boolean officer, final double payMultiplier) {
        this(Arrays.asList(name), officer, payMultiplier);
    }

    public Rank(final List<String> names, final boolean officer, final double payMultiplier) {
        setRankNames(names);
        setOfficer(officer);
        setPayMultiplier(payMultiplier);
        rankLevels = new ArrayList<>();
        for (int i = 0; i < rankNames.size(); i++) {
            rankLevels.add(0);
            if (rankNames.get(i).matches(".+:\\d+\\s*$")) {
                String[] temp = getRankNames().get(i).split(":");
                getRankNames().set(i, temp[0].trim());
                rankLevels.set(i, Integer.parseInt(temp[1].trim()));
            }
        }
    }
    //endregion Constructors

    //region Getters/Setters
    public List<String> getRankNames() {
        return rankNames;
    }

    public void setRankNames(final List<String> rankNames) {
        this.rankNames = rankNames;
    }

    public boolean isOfficer() {
        return officer;
    }

    public void setOfficer(boolean b) {
        officer = b;
    }

    public double getPayMultiplier() {
        return payMultiplier;
    }

    public void setPayMultiplier(double d) {
        payMultiplier = d;
    }
    //endregion Getters/Setters

    public String getName(int profession) {
        if (profession >= getRankNames().size()) {
            return "Profession Out of Bounds";
        }
        return getRankNames().get(profession);
    }

    public String getNameWithLevels(int profession) {
        if (profession >= getRankNames().size()) {
            return "Profession Out of Bounds";
        }
        return getRankNames().get(profession) + ((rankLevels.get(profession) > 0) ? ":" + rankLevels.get(profession) : "");
    }

    public int getRankLevels(int profession) {
        return rankLevels.get(profession);
    }

    public String getRankNamesAsString(final String delimiter) {
        StringJoiner joiner = new StringJoiner(delimiter);
        for (final String name : getRankNames()) {
            if (!rankLevels.isEmpty() && (rankLevels.get(getRankNames().indexOf(name)) > 0)) {
                joiner.add(name + rankLevels.get(getRankNames().indexOf(name)).toString());
            } else {
                joiner.add(name);
            }
        }
        return joiner.toString();
    }

    //region Boolean Comparison Methods
    public boolean isEmpty(final int profession) {
        return getName(profession).equals("-");
    }

    public boolean indicatesAlternativeSystem(final int profession) {
        return getName(profession).startsWith("--");
    }
    //endregion Boolean Comparison Methods

    //region File IO
    public void writeToXML(final PrintWriter pw, int indent, final int index) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw, indent++, "rank");
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "rankNames", getRankNamesAsString(","));
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "officer", isOfficer());
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "payMultiplier", getPayMultiplier());
        writeCloseTag(pw, --indent, index);
    }

    private void writeCloseTag(final PrintWriter pw, final int indent, final int rankIndex) {
        final String tag;
        if (rankIndex == 0) {
            tag = "</rank> <!-- E0 \"None\" -->\n";
        } else if (rankIndex < RE_NUM) {
            tag = "</rank> <!-- E" + rankIndex + " -->\n";
        } else if (rankIndex < RWO_NUM) {
            tag = "</rank> <!-- WO" + (rankIndex - RE_MAX) + " -->\n";
        } else if (rankIndex < RO_NUM) {
            tag = "</rank> <!-- O" + (rankIndex - RWO_MAX) + " -->\n";
        } else {
            tag = "</rank>\n";
        }
        pw.print(MekHqXmlUtil.indentStr(indent) + tag);
    }

    public static @Nullable Rank generateInstanceFromXML(final Node wn) {
        final Rank rank = new Rank();
        try {
            final NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("rankNames")) {
                    rank.setRankNames(Arrays.asList(wn2.getTextContent().split(",", -1)));
                    for (int i = 0; i < rank.getRankNames().size(); i++) {
                        rank.rankLevels.add(0);
                        if (rank.getRankNames().get(i).matches(".+:\\d+\\s*$")) {
                            String[] temp = rank.getRankNames().get(i).split(":");
                            rank.getRankNames().set(i, temp[0].trim());
                            rank.rankLevels.set(i, Integer.parseInt(temp[1].trim()));
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("officer")) {
                    rank.setOfficer(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("payMultiplier")) {
                    rank.setPayMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
                }
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
            return null;
        }

        return rank;
    }
    //endregion File IO
}
