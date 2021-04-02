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
import mekhq.campaign.personnel.enums.RankSystemType;
import mekhq.gui.model.RankTableModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class RankSystem implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = -6037712487121208137L;

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

    // Rank Profession Codes - TODO : Enum swapover
    public static final int RPROF_MW    = 0;
    public static final int RPROF_ASF   = 1;
    public static final int RPROF_VEE   = 2;
    public static final int RPROF_NAVAL = 3;
    public static final int RPROF_INF   = 4;
    public static final int RPROF_TECH  = 5;

    private String rankSystemCode; // Primary Key, must be unique
    private String rankSystemName;
    private transient RankSystemType type; // no need to serialize
    private List<Rank> ranks;
    //endregion Variable Declarations

    //region Constructors
    private RankSystem(final RankSystemType type) {
        this("UNK", "Unknown", type);
    }

    public RankSystem(final String rankSystemCode, final String rankSystemName, final RankSystemType type) {
        setRankSystemCode(rankSystemCode);
        setRankSystemName(rankSystemName);
        setType(type);
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

    public RankSystemType getType() {
        return type;
    }

    public void setType(RankSystemType type) {
        this.type = type;
    }

    public List<Rank> getRanks() {
        return ranks;
    }

    public void setRanks(final List<Rank> ranks) {
        // TODO : Windchild : I need to ensure the size is proper, and fix it if it is not... for now
        setRanksDirect(ranks);
    }

    public void setRanksDirect(final List<Rank> ranks) {
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
    // TODO : Windchild : Move me into an enum
    /**
     * This takes the initial profession, converts it into a base profession, and then calls
     * getProfessionFromBase to determine the profession to use for the provided rank.
     *
     * @param rank the rank to determine the profession for
     * @param initialProfession the initial profession to determine the profession for
     * @return the determined profession
     */
    public int getProfession(final Rank rank, final int initialProfession) {
        return getProfessionFromBase(rank, getBaseProfession(initialProfession));
    }

    /**
     * This takes the base profession and uses it to determine the determine the profession to use
     * for the provided rank.
     *
     * @param rank the rank to determine the profession for
     * @param baseProfession the base profession to determine the profession for
     * @return the determined profession
     */
    public int getProfessionFromBase(final Rank rank, final int baseProfession) {
        int profession = baseProfession;

        // This runs if the rank is empty or indicates an alternative system
        for (boolean empty = rank.isEmpty(profession); empty || rank.indicatesAlternativeSystem(profession);
             empty = rank.isEmpty(profession)) {
            if (empty) {
                profession = getAlternateProfession(profession);
            } else {
                profession = getAlternateProfession(rank, profession);
            }
        }

        return profession;
    }

    /**
     * This is used to get the base profession for the rank column following any required redirects
     * based on the provided initial profession.
     *
     * @param initialProfession the initial profession (normally a person's individual profession)
     * @return the final base profession for this rank system based on the initial profession
     */
    public int getBaseProfession(final int initialProfession) {
        int baseProfession = initialProfession;
        while (isEmptyProfession(baseProfession)) {
            baseProfession = getAlternateProfession(baseProfession);
        }
        return baseProfession;
    }

    /**
     * This is used to determine if a profession is empty, which means the first rank is an
     * alternative system while every other rank tier is empty.
     * @param profession the profession to determine the emptiness status
     * @return whether the profession is empty or not
     */
    public boolean isEmptyProfession(final int profession) {
        // MechWarrior profession cannot be empty
        // TODO : I should be allowed to be empty, and have my default replaced by another column,
        // TODO : albeit with the validator properly run before to ensure the rank system is valid.
        // TODO : The default return for getAlternativeProfession would not need to change in this case
        if (profession == RPROF_MW) {
            return false;
        }

        // Check the first rank to ensure it indicates an alternative rank system, as otherwise the
        // rank system is not empty.
        final Rank rank = getRanks().get(0);
        if (!rank.indicatesAlternativeSystem(profession)) {
            return false;
        }

        // Return true if all ranks except the first are empty
        return getRanks().subList(1, getRanks().size()).stream().allMatch(r -> r.isEmpty(profession));
    }

    /**
     * Determines the alternative profession to use based on the initial rank value
     * @param profession the profession to determine the alternative for
     * @return the alternative profession determined
     */
    public int getAlternateProfession(final int profession) {
        return getAlternateProfession(getRanks().get(0), profession);
    }

    /**
     * Determines the alternative profession to use based on the provided rank
     * @param rank the rank to determine the alternative profession for
     * @param profession the profession to determine the alternative for
     * @return the alternative profession determined
     */
    public int getAlternateProfession(final Rank rank, final int profession) {
        return getAlternateProfession(rank.getName(profession));
    }

    /**
     * Determines the alternative profession to use based on the name of a rank
     * @param name the name of the rank to use in determining the alternative profession
     * @return the alternative profession determined
     */
    public int getAlternateProfession(final String name) {
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
    @Deprecated
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
    //endregion Table Model

    //region File IO
    public void writeToFile(File file) {
        if (file == null) {
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".xml")) {
            path += ".xml";
            file = new File(path);
        }

        try (OutputStream fos = new FileOutputStream(file);
             OutputStream bos = new BufferedOutputStream(fos);
             OutputStreamWriter osw = new OutputStreamWriter(bos, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(osw)) {
            // Then save it out to that file.
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<individualRankSystem version=\"" + ResourceBundle.getBundle("mekhq.resources.MekHQ").getString("Application.version") + "\">");
            writeToXML(pw, 1, true);
            MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, 0, "individualRankSystem");
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }
    }

    public void writeToXML(final PrintWriter pw, int indent, final boolean export) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw, indent++, "rankSystem");
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "systemCode", getRankSystemCode());

        // Only write out any other information if we are exporting the system or we are using a
        // campaign-specific custom system
        if (export || getType().isCampaign()) {
            MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "systemName", getRankSystemName());
            for (int i = 0; i < getRanks().size(); i++) {
                getRanks().get(i).writeToXML(pw, indent);
                pw.println(getRankPostTag(i));
            }
        }

        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, --indent, "rankSystem");
    }

    private String getRankPostTag(final int rankNum) {
        if (rankNum == 0) {
            return " <!-- E0 \"None\" -->";
        } else if (rankNum < RE_NUM) {
            return " <!-- E" + rankNum + " -->";
        } else if (rankNum < RWO_NUM) {
            return " <!-- WO" + (rankNum - RE_MAX) + " -->";
        } else if (rankNum < RO_NUM) {
            return " <!-- O" + (rankNum - RWO_MAX) + " -->";
        } else {
            return "";
        }
    }

    /**
     * This generates a single Rank System from an XML file
     * @param file the file to load, or null if none are to be loaded
     * @return the single (or first) rank system located within the file, or null if no file is
     * provided or there is an error
     */
    public static @Nullable RankSystem generateIndividualInstanceFromXML(final @Nullable File file) {
        if (file == null) {
            return null;
        }

        final Element element;

        // Open up the file.
        try (InputStream is = new FileInputStream(file)) {
            element = MekHqXmlUtil.newSafeDocumentBuilder().parse(is).getDocumentElement();
        } catch (Exception e) {
            MekHQ.getLogger().error("Failed to open file, returning null", e);
            return null;
        }
        element.normalize();
        final Version version = new Version(element.getAttribute("version"));
        final NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            final Node wn = nl.item(i);
            if ("rankSystem".equals(wn.getNodeName()) && wn.hasChildNodes()) {
                // We can assume a RankSystemType of Campaign, as any other would be returned with
                // the proper type through the already loaded rank systems
                return generateInstanceFromXML(wn.getChildNodes(), version);
            }
        }
        MekHQ.getLogger().error("Failed to parse file, returning null");
        return null;
    }

    /**
     * This loads a Rank System after the initial load of the rank system data.
     * @param nl the node list to parse the rank system from
     * @param version the version to parse the rank system at
     * @return the unvalidated parsed rank system, or null if there is an issue in parsing
     */
    public static @Nullable RankSystem generateInstanceFromXML(final NodeList nl,
                                                               final @Nullable Version version) {
        return generateInstanceFromXML(nl, version, false, RankSystemType.CAMPAIGN);
    }

    /**
     * @param nl the node list to parse the rank system from
     * @param version the version to parse the rank system at, or null to not check the version
     * @param initialLoad whether this is the initial load or a later load
     * @param type the type of rank system being loaded
     * @return the unvalidated parsed rank system, or null if there is an issue in parsing
     */
    public static @Nullable RankSystem generateInstanceFromXML(final NodeList nl,
                                                               final @Nullable Version version,
                                                               final boolean initialLoad,
                                                               final RankSystemType type) {
        final RankSystem rankSystem = new RankSystem(type);
        // Dump the ranks ArrayList so we can re-use it.
        rankSystem.setRanksDirect(new ArrayList<>());

        try {
            int rankSystemId = -1; // migration, 0.49.X

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn = nl.item(x);

                if (Stream.of("system", "rankSystem", "systemId").anyMatch(s -> wn.getNodeName().equalsIgnoreCase(s))) { // Legacy, 0.49.0 removal
                    rankSystemId = Integer.parseInt(wn.getTextContent().trim());
                    if (!initialLoad && (rankSystemId != 12)) {
                        return Ranks.getRankSystemFromCode(PersonMigrator.migrateRankSystemCode(rankSystemId));
                    }
                } else if (wn.getNodeName().equalsIgnoreCase("systemCode")) {
                    final String systemCode = wn.getTextContent().trim();
                    // If this isn't the initial load and we already have a loaded system with the
                    // provided key, just return the rank system saved by the key in question.
                    // This does not need to be validated to ensure it is a proper rank system
                    if (!initialLoad && Ranks.getRankSystems().containsKey(systemCode)) {
                        return Ranks.getRankSystemFromCode(systemCode);
                    }
                    rankSystem.setRankSystemCode(systemCode);
                } else if (wn.getNodeName().equalsIgnoreCase("systemName")) {
                    rankSystem.setRankSystemName(wn.getTextContent().trim());
                } else if (wn.getNodeName().equalsIgnoreCase("rank")) {
                    rankSystem.getRanks().add(Rank.generateInstanceFromXML(wn));
                }
            }

            if ((version != null) && (rankSystemId != -1) && version.isLowerThan("0.49.0")) {
                rankSystem.setRankSystemCode(PersonMigrator.migrateRankSystemCode(rankSystemId));
                rankSystem.setRankSystemName(PersonMigrator.migrateRankSystemName(rankSystemId));
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
            return null;
        }
        return rankSystem;
    }
    //endregion File IO

    @Override
    public String toString() {
        return rankSystemName;
    }

    @Override
    public boolean equals(final @Nullable Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof RankSystem)) {
            return false;
        } else {
            return getRankSystemCode().equals(((RankSystem) object).getRankSystemCode());
        }
    }

    @Override
    public int hashCode() {
        return getRankSystemCode().hashCode();
    }
}
