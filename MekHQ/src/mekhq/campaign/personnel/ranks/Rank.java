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
import megamek.Version;
import mekhq.campaign.personnel.enums.Profession;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
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

    private Map<Profession, String> rankNames;
    private Map<Profession, Integer> rankLevels;
    private boolean officer;
    private double payMultiplier;
    //endregion Variable Declarations

    //region Constructors
    public Rank() {
        this(new String[0], false, 1.0);
    }

    public Rank(final Rank rank) {
        setRankNames(new HashMap<>());
        setRankLevels(new HashMap<>());
        for (final Profession profession : Profession.values()) {
            getRankNames().put(profession, rank.getRankNames().getOrDefault(profession, "-"));
            getRankLevels().put(profession, rank.getRankLevels().getOrDefault(profession, 1));
        }
        setOfficer(rank.isOfficer());
        setPayMultiplier(rank.getPayMultiplier());
    }

    public Rank(final String[] names, final boolean officer, final double payMultiplier) {
        initializeRank(names);
        setOfficer(officer);
        setPayMultiplier(payMultiplier);
    }
    //endregion Constructors

    //region Getters/Setters
    public Map<Profession, String> getRankNames() {
        return rankNames;
    }

    public void setRankNames(final Map<Profession, String> rankNames) {
        this.rankNames = rankNames;
    }

    public Map<Profession, Integer> getRankLevels() {
        return rankLevels;
    }

    public void setRankLevels(final Map<Profession, Integer> rankLevels) {
        this.rankLevels = rankLevels;
    }

    public boolean isOfficer() {
        return officer;
    }

    public void setOfficer(final boolean officer) {
        this.officer = officer;
    }

    public double getPayMultiplier() {
        return payMultiplier;
    }

    public void setPayMultiplier(final double payMultiplier) {
        this.payMultiplier = payMultiplier;
    }
    //endregion Getters/Setters

    //region Initialization
    private void initializeRank(final String... names) {
        setRankNames(new HashMap<>());
        setRankLevels(new HashMap<>());
        for (final Profession profession : Profession.values()) {
            String name = (names.length > profession.ordinal()) ? names[profession.ordinal()] : "-";
            int level = 1;
            if (name.matches(".+:\\d+\\s*$")) {
                final String[] split = name.split(":");
                name = split[0];
                try {
                    level = Integer.parseInt(split[1].trim());
                } catch (Exception e) {
                    MekHQ.getLogger().error(e);
                }
            }
            getRankNames().put(profession, name);
            getRankLevels().put(profession, level);
        }
    }
    //endregion Initialization

    public String getName(final Profession profession) {
        return getRankNames().get(profession);
    }

    public String getNameWithLevels(final Profession profession) {
        return getRankNames().get(profession) + ((getRankLevels().get(profession) > 1) ? ":" + getRankLevels().get(profession) : "");
    }

    public String getRankNamesAsString(final String delimiter) {
        StringJoiner joiner = new StringJoiner(delimiter);
        for (final Profession profession : Profession.values()) {
            String name = getRankNames().get(profession);
            name = (name == null) ? "-" : name;
            if (getRankLevels().containsKey(profession) && (getRankLevels().get(profession) > 1)) {
                joiner.add(name + getRankLevels().get(profession));
            } else {
                joiner.add(name);
            }
        }
        return joiner.toString();
    }

    //region Boolean Comparison Methods
    public boolean isEmpty(final Profession profession) {
        return !getRankNames().containsKey(profession) || getRankNames().get(profession).isBlank()
                || getRankNames().get(profession).equals("-");
    }

    public boolean indicatesAlternativeSystem(final Profession profession) {
        return getRankNames().containsKey(profession) && getRankNames().get(profession).startsWith("--");
    }
    //endregion Boolean Comparison Methods

    //region File I/O
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

    public static @Nullable Rank generateInstanceFromXML(final Node wn, final Version version,
                                                         final boolean e0) {
        final Rank rank = new Rank();
        try {
            final NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("rankNames")) {
                    String names = wn2.getTextContent();
                    if (version.isLowerThan("0.49.0")) {
                        names += e0 ? ",--TECH,--TECH,--ADMIN" : ",-,-,-";
                    }
                    rank.initializeRank(names.split(",", -1));
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
    //endregion File I/O
}
