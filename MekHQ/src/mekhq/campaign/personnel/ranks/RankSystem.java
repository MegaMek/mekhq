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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import mekhq.MHQConstants;
import mekhq.campaign.personnel.enums.RankSystemType;
import mekhq.utilities.MHQXMLUtility;

public class RankSystem {
    private static final MMLogger logger = MMLogger.create(RankSystem.class);

    // region Variable Declarations
    private String code; // Primary Key, must be unique
    private transient RankSystemType type; // no need to serialize
    private String name;
    private String description;
    private boolean useROMDesignation;
    private boolean useManeiDomini;
    private List<Rank> ranks;
    // endregion Variable Declarations

    // region Constructors
    private RankSystem(final RankSystemType type) {
        this("UNK", "Unknown", "", type);
    }

    public RankSystem(final RankSystem rankSystem) {
        setCode(rankSystem.getCode());
        setType(rankSystem.getType());
        setName(rankSystem.toString());
        setDescription(rankSystem.getDescription());
        setUseROMDesignation(rankSystem.isUseROMDesignation());
        setUseManeiDomini(rankSystem.isUseManeiDomini());
        setRanks(new ArrayList<>(rankSystem.getRanks()));
    }

    public RankSystem(final String code, final String name,
            final String description, final RankSystemType type) {
        setCode(code);
        setType(type);
        setName(name);
        setDescription(description);
        setUseROMDesignation(false);
        setUseManeiDomini(false);
        final RankSystem system = Ranks.getRankSystemFromCode(code);
        setRanks((system == null) ? new ArrayList<>() : new ArrayList<>(system.getRanks()));
    }
    // endregion Constructors

    // region Getters/Setters
    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public RankSystemType getType() {
        return type;
    }

    public void setType(final RankSystemType type) {
        this.type = type;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public boolean isUseROMDesignation() {
        return useROMDesignation;
    }

    public void setUseROMDesignation(final boolean useROMDesignation) {
        this.useROMDesignation = useROMDesignation;
    }

    public boolean isUseManeiDomini() {
        return useManeiDomini;
    }

    public void setUseManeiDomini(final boolean useManeiDomini) {
        this.useManeiDomini = useManeiDomini;
    }

    public List<Rank> getRanks() {
        return ranks;
    }

    public void setRanks(final List<Rank> ranks) {
        this.ranks = ranks;
    }
    // endregion Getters/Setters

    public Rank getRank(int index) {
        if (index >= getRanks().size()) {
            // assign the highest rank
            index = getRanks().size() - 1;
        }
        return getRanks().get(index);
    }

    /**
     * @return the index of the first officer
     */
    public int getOfficerCut() {
        for (int i = 0; i < getRanks().size(); i++) {
            if (getRanks().get(i).isOfficer()) {
                return i;
            }
        }
        return getRanks().size() - 1;
    }

    // region File I/O
    public void writeToFile(File file) {
        if (file == null) {
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".xml")) {
            path += ".xml";
            file = new File(path);
        }
        int indent = 0;
        try (OutputStream fos = new FileOutputStream(file);
                OutputStream bos = new BufferedOutputStream(fos);
                OutputStreamWriter osw = new OutputStreamWriter(bos, StandardCharsets.UTF_8);
                PrintWriter pw = new PrintWriter(osw)) {
            // Then save it out to that file.
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "individualRankSystem", "version", MHQConstants.VERSION);
            writeToXML(pw, indent, true);
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "individualRankSystem");
        } catch (Exception ex) {
            logger.error("", ex);
        }
    }

    public void writeToXML(final PrintWriter pw, int indent, final boolean export) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "rankSystem");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "code", getCode());

        // Only write out any other information if we are exporting the system or we are
        // using a
        // campaign-specific custom system
        if (export || getType().isCampaign()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "name", toString());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "description", getDescription());

            if (isUseROMDesignation()) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useROMDesignation", isUseROMDesignation());
            }

            if (isUseManeiDomini()) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useManeiDomini", isUseManeiDomini());
            }

            for (int i = 0; i < getRanks().size(); i++) {
                getRanks().get(i).writeToXML(pw, indent, i);
            }
        }

        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "rankSystem");
    }

    /**
     * This generates a single Rank System from an XML file
     *
     * @param file the file to load, or null if none are to be loaded
     * @return the single (or first) rank system located within the file, or null if
     *         no file is
     *         provided or there is an error
     */
    public static @Nullable RankSystem generateIndividualInstanceFromXML(final @Nullable File file) {
        if (file == null) {
            return null;
        }

        final Element element;

        // Open up the file.
        try (InputStream is = new FileInputStream(file)) {
            element = MHQXMLUtility.newSafeDocumentBuilder().parse(is).getDocumentElement();
        } catch (Exception ex) {
            logger.error("Failed to open file, returning null", ex);
            return null;
        }
        element.normalize();
        final Version version = new Version(element.getAttribute("version"));
        final NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            final Node wn = nl.item(i);
            if ("rankSystem".equals(wn.getNodeName()) && wn.hasChildNodes()) {
                // We can assume a RankSystemType of Campaign, as any other would be returned
                // with
                // the proper type through the already loaded rank systems
                return generateInstanceFromXML(wn.getChildNodes(), version);
            }
        }
        logger.error("Failed to parse file, returning null");
        return null;
    }

    /**
     * This loads a Rank System after the initial load of the rank system data.
     *
     * @param nl      the node list to parse the rank system from
     * @param version the version to parse the rank system at
     * @return the unvalidated parsed rank system, or null if there is an issue in
     *         parsing
     */
    public static @Nullable RankSystem generateInstanceFromXML(final NodeList nl,
            final Version version) {
        return generateInstanceFromXML(nl, version, false, RankSystemType.CAMPAIGN);
    }

    /**
     * @param nl          the node list to parse the rank system from
     * @param version     the version to parse the rank system at
     * @param initialLoad whether this is the initial load or a later load
     * @param type        the type of rank system being loaded
     * @return the un-validated parsed rank system, or null if there is an issue in
     *         parsing
     */
    public static @Nullable RankSystem generateInstanceFromXML(final NodeList nl,
            final Version version,
            final boolean initialLoad,
            final RankSystemType type) {
        final RankSystem rankSystem = new RankSystem(type);
        // Dump the ranks ArrayList so we can re-use it.
        rankSystem.setRanks(new ArrayList<>());

        try {
            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn = nl.item(x);

                if (wn.getNodeName().equalsIgnoreCase("code")) {
                    final String systemCode = MMXMLUtility.unEscape(wn.getTextContent().trim());
                    // If this isn't the initial load and we already have a loaded system with the
                    // provided key, just return the rank system saved by the key in question.
                    // This does not need to be validated to ensure it is a proper rank system
                    if (!initialLoad && Ranks.getRankSystems().containsKey(systemCode)) {
                        return Ranks.getRankSystemFromCode(systemCode);
                    }
                    rankSystem.setCode(systemCode);
                } else if (wn.getNodeName().equalsIgnoreCase("name")) {
                    rankSystem.setName(MMXMLUtility.unEscape(wn.getTextContent().trim()));
                } else if (wn.getNodeName().equalsIgnoreCase("description")) {
                    rankSystem.setDescription(MMXMLUtility.unEscape(wn.getTextContent().trim()));
                } else if (wn.getNodeName().equalsIgnoreCase("useROMDesignation")) {
                    rankSystem.setUseROMDesignation(Boolean.parseBoolean(wn.getTextContent().trim()));
                } else if (wn.getNodeName().equalsIgnoreCase("useManeiDomini")) {
                    rankSystem.setUseManeiDomini(Boolean.parseBoolean(wn.getTextContent().trim()));
                } else if (wn.getNodeName().equalsIgnoreCase("rank")) {
                    rankSystem.getRanks().add(Rank.generateInstanceFromXML(wn, version, true));
                }
            }
        } catch (Exception e) {
            logger.error("", e);
            return null;
        }
        return rankSystem;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(final @Nullable Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof RankSystem)) {
            return false;
        } else {
            return getCode().equals(((RankSystem) object).getCode());
        }
    }

    @Override
    public int hashCode() {
        return getCode().hashCode();
    }
}
