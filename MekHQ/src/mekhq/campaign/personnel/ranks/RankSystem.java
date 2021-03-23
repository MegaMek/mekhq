/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
import mekhq.Version;
import mekhq.campaign.io.Migration.PersonMigrator;
import mekhq.gui.model.RankTableModel;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Stream;

public class RankSystem implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = -6037712487121208137L;

    private String rankSystemCode;
    private String rankSystemName;
    private List<Rank> ranks;

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

    // Rank Profession Codes
    public static final int RPROF_MW    = 0;
    public static final int RPROF_ASF   = 1;
    public static final int RPROF_VEE   = 2;
    public static final int RPROF_NAVAL = 3;
    public static final int RPROF_INF   = 4;
    public static final int RPROF_TECH  = 5;
    public static final int RPROF_NUM   = 6;
    //endregion Variable Declarations

    //region Constructors
    public RankSystem() {
        this("UNK", "Unknown");
    }

    public RankSystem(final String rankSystemCode, final String rankSystemName) {
        this.rankSystemCode = rankSystemCode;
        this.rankSystemName = rankSystemName;

        final RankSystem system = Ranks.getRankSystemFromCode(rankSystemCode);
        setRanks((system == null) ? new ArrayList<>() : new ArrayList<>(system.getRanks()));
    }
    //endregion Constructors

    //region Getters/Setters
    public String getRankSystemCode() {
        return rankSystemCode;
    }

    public void setRankSystemCode(final String rankSystemCode) {
        this.rankSystemCode = rankSystemCode;
    }

    public String getRankSystemName() {
        return rankSystemName;
    }

    public void setRankSystemName(final String rankSystemName) {
        this.rankSystemName = rankSystemName;
    }

    public List<Rank> getRanks() {
        return ranks;
    }

    public void setRanks(final List<Rank> ranks) {
        this.ranks = ranks;
    }
    //endregion Getters/Setters

    //region Boolean Comparison Methods
    public boolean isWoBMilitia() {
        return "WOBM".equals(getRankSystemCode());
    }

    public boolean isComGuard() {
        return "CG".equals(getRankSystemCode());
    }

    public boolean isCGOrWoBM() {
        return isComGuard() || isWoBMilitia();
    }
    //endregion Boolean Comparison Methods

    public Rank getRank(int r) {
        if (r >= ranks.size()) {
            //assign the highest rank
            r = ranks.size() - 1;
        }
        return ranks.get(r);
    }

    public int getOfficerCut() {
        for (int i = 0; i < ranks.size(); i++) {
            if (ranks.get(i).isOfficer()) {
                return i;
            }
        }
        return ranks.size() - 1;
    }

    //region Professions
    public int getRankNumericFromNameAndProfession(int profession, String name) {
        while ((profession != RPROF_MW) && isEmptyProfession(profession)) {
            profession = getAlternateProfession(profession);
        }

        for (int i = 0; i < RC_NUM; i++) {
            Rank rank = getRanks().get(i);
            int p = profession;

            // Grab rank from correct profession as needed
            while (rank.getName(p).startsWith("--") && (p != RPROF_MW)) {
                if (rank.getName(p).equals("--")) {
                    p = getAlternateProfession(p);
                } else if (rank.getName(p).startsWith("--")) {
                    p = getAlternateProfession(rank.getName(p));
                }
            }

            // Check it...
            if (rank.getName(p).equals(name)) {
                return i;
            }
        }

        return 0;
    }

    public boolean isEmptyProfession(int profession) {
        // MechWarrior profession cannot be empty
        if (profession == RPROF_MW) {
            return false;
        }

        // Check the profession
        for (int i = 0; i < RC_NUM; i++) {
            // If our first Rank is an indicator of an alternate system, skip it.
            if ((i == 0) && ranks.get(0).getName(profession).startsWith("--")) {
                continue;
            }

            if (!ranks.get(i).getName(profession).equals("-")) {
                return false;
            }
        }

        // It's empty...
        return true;
    }

    public int getAlternateProfession(int profession) {
        return getAlternateProfession(ranks.get(0).getName(profession));
    }

    public int getAlternateProfession(String name) {
        switch (name.replaceAll("--", "")) {
            case "ASF":
                return RPROF_ASF;
            case "VEE":
                return RPROF_VEE;
            case "NAVAL":
                return RPROF_NAVAL;
            case "INF":
                return RPROF_INF;
            case "TECH":
                return RPROF_TECH;
            case "MW":
            default:
                return RPROF_MW;
        }
    }
    //endregion Professions

    //region Table Model
    // TODO : Move this region into the Table Model, having it here is odd
    public Object[][] getRanksForModel() {
        Object[][] array = new Object[ranks.size()][RankTableModel.COL_NUM];
        int i = 0;
        for (Rank rank : ranks) {
            String rating = "E" + i;
            if (i > RWO_MAX) {
                rating = "O" + (i - RWO_MAX);
            } else if (i > RE_MAX) {
                rating = "WO" + (i - RE_MAX);
            }
            array[i][RankTableModel.COL_NAME_RATE] = rating;
            array[i][RankTableModel.COL_NAME_MW] = rank.getNameWithLevels(RPROF_MW);
            array[i][RankTableModel.COL_NAME_ASF] = rank.getNameWithLevels(RPROF_ASF);
            array[i][RankTableModel.COL_NAME_VEE] = rank.getNameWithLevels(RPROF_VEE);
            array[i][RankTableModel.COL_NAME_NAVAL] = rank.getNameWithLevels(RPROF_NAVAL);
            array[i][RankTableModel.COL_NAME_INF] = rank.getNameWithLevels(RPROF_INF);
            array[i][RankTableModel.COL_NAME_TECH] = rank.getNameWithLevels(RPROF_TECH);
            array[i][RankTableModel.COL_OFFICER] = rank.isOfficer();
            array[i][RankTableModel.COL_PAYMULT] = rank.getPayMultiplier();
            i++;
        }
        return array;
    }

    public void setRanksFromModel(final RankTableModel model) {
        setRanks(new ArrayList<>());
        @SuppressWarnings("rawtypes") // Broken java doesn't have typed vectors in the DefaultTableModel
                Vector<Vector> vectors = model.getDataVector();
        for (@SuppressWarnings("rawtypes") Vector row : vectors) {
            String[] names = { (String) row.get(RankTableModel.COL_NAME_MW), (String) row.get(RankTableModel.COL_NAME_ASF),
                    (String) row.get(RankTableModel.COL_NAME_VEE), (String) row.get(RankTableModel.COL_NAME_NAVAL),
                    (String) row.get(RankTableModel.COL_NAME_INF), (String) row.get(RankTableModel.COL_NAME_TECH) };
            Boolean officer = (Boolean) row.get(RankTableModel.COL_OFFICER);
            double payMult = (Double) row.get(RankTableModel.COL_PAYMULT);
            getRanks().add(new Rank(names, officer, payMult));
        }
    }
    //endregion Table Model

    //region File IO
    public void writeToXML(final PrintWriter pw, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw, indent++, "rankSystem");
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "systemCode", getRankSystemCode());
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "systemName", getRankSystemName());

        // Only write out the ranks if we're using a custom system
        if (!Ranks.getBaseRankSystems().containsKey(getRankSystemCode())) {
            for (int i = 0; i < getRanks().size(); i++) {
                getRanks().get(i).writeToXML(pw, indent);
                pw.println(getRankPostTag(i));
            }
        }

        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, --indent, "rankSystem");
    }

    private String getRankPostTag(int rankNum) {
        if (rankNum == 0) {
            return " <!-- E0 \"None\" -->";
        }
        if (rankNum < RE_NUM) {
            return " <!-- E" + rankNum + " -->";
        }
        if (rankNum < RWO_NUM) {
            return " <!-- WO" + (rankNum - RE_MAX) + " -->";
        }
        if (rankNum < RO_NUM) {
            return " <!-- O" + (rankNum - RWO_MAX) + " -->";
        }

        // Yuck, we've got nada!
        return "";
    }

    public static @Nullable RankSystem generateInstanceFromXML(final NodeList nl,
                                                               final @Nullable Version version,
                                                               final boolean initialLoad) {
        final RankSystem rankSystem = new RankSystem();
        // Dump the ranks ArrayList so we can re-use it.
        rankSystem.setRanks(new ArrayList<>());

        try {
            int rankSystemId = -1; // migration
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn = nl.item(x);

                if (Stream.of("system", "rankSystem", "systemId").anyMatch(s -> wn.getNodeName().equalsIgnoreCase(s))) { // Legacy, 0.49.0 removal
                    rankSystemId = Integer.parseInt(wn.getTextContent().trim());
                    if (!initialLoad && (rankSystemId != 12) && (rankSystemId != -1)) {
                        return Ranks.getRankSystemFromCode(PersonMigrator.migrateRankSystemCode(rankSystemId));
                    }
                } else if (wn.getNodeName().equalsIgnoreCase("systemCode")) {
                    rankSystem.setRankSystemCode(wn.getTextContent().trim());
                } else if (wn.getNodeName().equalsIgnoreCase("systemName")) {
                    rankSystem.setRankSystemName(wn.getTextContent().trim());
                } else if (wn.getNodeName().equalsIgnoreCase("rank")) {
                    rankSystem.ranks.add(Rank.generateInstanceFromXML(wn));
                }
            }

            if ((version != null) && (rankSystemId != -1) && version.isLowerThan("0.49.0")) {
                rankSystem.setRankSystemCode(PersonMigrator.migrateRankSystemCode(rankSystemId));
                rankSystem.setRankSystemName(PersonMigrator.migrateRankSystemName(rankSystemId));
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }
        return rankSystem;
    }
    //endregion File IO

    @Override
    public String toString() {
        return rankSystemName;
    }
}
