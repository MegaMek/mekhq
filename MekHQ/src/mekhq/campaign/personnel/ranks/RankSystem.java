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

    private String rankSystemCode; // Primary Key, must be unique
    private String rankSystemName;
    private String description;
    private transient RankSystemType type; // no need to serialize
    private List<Rank> ranks;
    //endregion Variable Declarations

    //region Constructors
    private RankSystem(final RankSystemType type) {
        this("UNK", "Unknown", type);
    }

    public RankSystem(final RankSystem rankSystem) {
        setRankSystemCode(rankSystem.getRankSystemCode());
        setRankSystemName(rankSystem.toString());
        setDescription(rankSystem.getDescription());
        setType(rankSystem.getType());
        setRanks(new ArrayList<>(rankSystem.getRanks()));
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

    public void setRankSystemName(final String rankSystemName) {
        this.rankSystemName = rankSystemName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public RankSystemType getType() {
        return type;
    }

    public void setType(final RankSystemType type) {
        this.type = type;
    }

    public List<Rank> getRanks() {
        return ranks;
    }

    public void setRanks(final List<Rank> ranks) {
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

    //region File I/O
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
            MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "systemName", toString());
            for (int i = 0; i < getRanks().size(); i++) {
                getRanks().get(i).writeToXML(pw, indent, i);
            }
        }

        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, --indent, "rankSystem");
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
                final Node wn = nl.item(x);

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
    //endregion File I/O

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
