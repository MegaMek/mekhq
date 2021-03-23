/*
 * Ranks.java
 *
 * Copyright (c) 2009 - Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * This object will keep track of rank information. It will keep information
 * on a set of pre-fab rank structures and will hold info on the one chosen by the user.
 * It will also allow for the input of a user-created rank structure from a comma-delimited
 * set of names
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Ranks {
    //region Variable Declarations
    // Rank System Codes
    public static final String DEFAULT_SYSTEM_CODE = "SSLDF";

    private static Map<String, RankSystem> baseRankSystems;
    //endregion Variable Declarations

    //region Constructors
    private Ranks() {
        // This Class should never be constructed
    }
    //endregion Constructors

    //region Getters/Setters
    public static Map<String, RankSystem> getBaseRankSystems() {
        return baseRankSystems;
    }

    protected static void setBaseRankSystems(final Map<String, RankSystem> baseRankSystems) {
        Ranks.baseRankSystems = baseRankSystems;
    }

    public static @Nullable RankSystem getRankSystemFromCode(final String code) {
        final RankSystem ranks = getBaseRankSystems().get(code);
        return (ranks == null) ? getBaseRankSystems().get(DEFAULT_SYSTEM_CODE) : ranks;
    }
    //endregion Getters/Setters

    //region File IO
    public static void initializeRankSystems() {
        MekHQ.getLogger().info("Starting load of Rank Systems from XML...");

        // Initialize variables
        setBaseRankSystems(new HashMap<>());

        final Document xmlDoc;

        // TODO : Remove inline file path
        try (InputStream is = new FileInputStream("data/universe/ranks.xml")) {
            xmlDoc = MekHqXmlUtil.newSafeDocumentBuilder().parse(is);
        } catch (Exception ex) {
            MekHQ.getLogger().error(ex);
            return;
        }

        final Element ranksEle = xmlDoc.getDocumentElement();
        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML. At least this cleans it up.
        ranksEle.normalize();

        final NodeList nl = ranksEle.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn = nl.item(x);

            if (!wn.getParentNode().equals(ranksEle) || (wn.getNodeType() != Node.ELEMENT_NODE)) {
                continue;
            }

            if (wn.getNodeName().equalsIgnoreCase("rankSystem") && wn.hasChildNodes()) {
                final RankSystem value = RankSystem.generateInstanceFromXML(wn.getChildNodes(), null, true);
                getBaseRankSystems().put(value.getRankSystemCode(), value);
            }
        }
        MekHQ.getLogger().info("Done loading Rank Systems");
    }
    //endregion File IO
}
