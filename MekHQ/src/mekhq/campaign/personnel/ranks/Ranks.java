/*
 * Ranks.java
 *
 * Copyright (c) 2009 - Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;

import megamek.common.annotations.Nullable;
import mekhq.campaign.io.Migration.PersonMigrator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.gui.model.RankTableModel;

/**
 * This object will keep track of rank information. It will keep information
 * on a set of pre-fab rank structures and will hold info on the one chosen by the user.
 * It will also allow for the input of a user-created rank structure from a comma-delimited
 * set of names
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Ranks {
    // Rank Faction Codes
    public static final int RS_SL       = 0;
    public static final int RS_FS       = 1;
    public static final int RS_FC       = 2;
    public static final int RS_LC       = 3;
    public static final int RS_LA       = 4;
    public static final int RS_FWL      = 5;
    public static final int RS_CC       = 6;
    public static final int RS_CCWH     = 7;
    public static final int RS_DC       = 8;
    public static final int RS_CL       = 9;
    public static final int RS_COM      = 10;
    public static final int RS_WOB      = 11;
    public static final int RS_CUSTOM   = 12;
    public static final int RS_MOC      = 13;
    public static final int RS_TC       = 14;
    public static final int RS_MH       = 15;
    public static final int RS_OA       = 16;
    public static final int RS_FRR      = 17;
    public static final int RS_ARC      = 18;
    public static final int RS_FSL      = 19;
    public static final int RS_NUM      = 20;

    public static final int[] translateFactions = { /* 0 */ 0, /* 1 */ 1, /* 2 */ 4, /* 3 */ 5, /* 4 */ 6, /* 5 */ 8, /* 6 */ 9, /* 7 */ 12 };

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

    private static Hashtable<Integer, Ranks> rankSystems;
    @SuppressWarnings("unused") // FIXME
    private static final int[] officerCut = {/*SLDF*/7,/*AFFS*/6,/*AFFC*/8,/*LCAF*/14,/*LAAF*/11,/*FWLM*/9,/*CCAF*/5,/*CCWH*/2,/*DCMD*/9,/*Clan*/3,/*COM*/2,/*WOB*/2};

    private int rankSystem;
    private String rankSystemName;
    private int oldRankSystem = -1;
    private List<Rank> ranks;

    public Ranks() {
        this(RS_SL, "Second Star League");
    }

    public Ranks(int system, String rankSystemName) {
        rankSystem = system;
        this.rankSystemName = rankSystemName;
        useRankSystem(rankSystem);
    }

    public static void initializeRankSystems() {
        rankSystems = new Hashtable<>();
        MekHQ.getLogger().info("Starting load of Rank Systems from XML...");
        // Initialize variables.
        Document xmlDoc;

        try (InputStream is = new FileInputStream("data/universe/ranks.xml")) {
            // Using factory get an instance of document builder
            DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(is);
        } catch (Exception ex) {
            MekHQ.getLogger().error(ex);
            return;
        }

        Element ranksEle = xmlDoc.getDocumentElement();
        NodeList nl = ranksEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML. At least this cleans it up.
        ranksEle.normalize();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);
            Ranks value;

            if (!wn.getParentNode().equals(ranksEle)) {
                continue;
            }

            int xc = wn.getNodeType();

            if (xc == Node.ELEMENT_NODE) {
                // This is what we really care about.
                // All the meat of our document is in this node type, at this level.
                // Okay, so what element is it?
                if (wn.getNodeName().equalsIgnoreCase("rankSystem")) {
                    // Starting to generate rankSystem
                    value = generateInstanceFromXML(wn, null, true);
                    rankSystems.put(value.getRankSystem(), value);
                }
            }
        }
        MekHQ.getLogger().info("Done loading Rank Systems");
    }

    public static @Nullable Ranks getRanksFromSystem(int system) {
        final Ranks ranks = rankSystems.get(system);
        return (ranks == null) ? rankSystems.get(Ranks.RS_SL) : ranks;
    }

    public int getRankSystem() {
        return rankSystem;
    }

    public String getRankSystemName() {
        return rankSystemName;
    }

    public void setRankSystemName(String rankSystemName) {
        this.rankSystemName = rankSystemName;
    }

    public boolean isCustom() {
        return getRankSystem() == Ranks.RS_CUSTOM;
    }

    public int getRankNumericFromNameAndProfession(int profession, String name) {
        while ((profession != RPROF_MW) && isEmptyProfession(profession)) {
            profession = getAlternateProfession(profession);
        }

        for (int i = 0; i < RC_NUM; i++) {
            Rank rank = getAllRanks().get(i);
            int p = profession;

            // Grab rank from correct profession as needed
            while (rank.getName(p).startsWith("--") && (p != Ranks.RPROF_MW)) {
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
        if (profession == RPROF_MW)
            return false;

        // Check the profession
        for (int i = 0; i < RC_NUM; i++) {
            // If our first Rank is an indicator of an alternate system, skip it.
            if (i == 0 && ranks.get(0).getName(profession).startsWith("--")) {
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

    public List<Rank> getAllRanks() {
        return ranks;
    }

    private void useRankSystem(int system) {
        // If we've got an invalid rank system, default to Star League
        if (system >= rankSystems.size()) {
            if (rankSystems.isEmpty()) {
                ranks = new ArrayList<>();
            } else {
                ranks = rankSystems.get(RS_SL).getAllRanks();
            }
            rankSystem = RS_SL;
            return;
        }
        ranks = rankSystems.get(system).getAllRanks();
        rankSystem = system;
    }

    public void setCustomRanks(ArrayList<ArrayList<String>> customRanks, int offCut) {
        ranks = new ArrayList<>();
        for (int i = 0; i < customRanks.size(); i++) {
            ranks.add(new Rank(customRanks.get(i), offCut <= i,  1.0));
        }
        rankSystem = RS_CUSTOM;
    }

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

    public int getRankOrder(String rank, int profession) {
        for (int i = 0; i < ranks.size(); i++) {
            if (ranks.get(i).getName(profession).equals(rank)) {
                return i;
            }
        }
        return 0;
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

    public void writeToXml(PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent++, "rankSystem");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "systemId", getRankSystem());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "systemName", getRankSystemName());

        // Only write out the ranks if we're using a custom system
        if (rankSystem == RS_CUSTOM) {
            for (int i = 0; i < ranks.size(); i++) {
                Rank r = ranks.get(i);
                r.writeToXml(pw1, indent);
                pw1.println(getRankPostTag(i));
            }
        }

        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "rankSystem");
    }

    public static Ranks generateInstanceFromXML(final Node wn, final @Nullable Version version,
                                                final boolean initialLoad) {
        final Ranks retVal = new Ranks();
        // Dump the ranks ArrayList so we can re-use it.
        retVal.ranks = new ArrayList<>();

        try {
            NodeList nl = wn.getChildNodes();
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("system") // Legacy, 0.47.15 removal
                        || wn2.getNodeName().equalsIgnoreCase("rankSystem") // Legacy, 0.47.15 removal
                        || wn2.getNodeName().equalsIgnoreCase("systemId")) {
                    final int rankSystem = Integer.parseInt(wn2.getTextContent().trim());
                    if (!initialLoad && (retVal.rankSystem != RS_CUSTOM)) {
                        return Ranks.getRanksFromSystem(retVal.rankSystem);
                    }
                    retVal.rankSystem = rankSystem;
                } else if (wn2.getNodeName().equalsIgnoreCase("systemName")) {
                    retVal.setRankSystemName(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("rank")) {
                    retVal.ranks.add(Rank.generateInstanceFromXML(wn2));
                }
            }

            if ((version != null) && version.isLowerThan("0.48.0")) {
                retVal.setRankSystemName(PersonMigrator.migrateRankSystemName(retVal.getRankSystem()));
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }
        return retVal;
    }

    public Object[][] getRanksForModel() {
        Object[][] array = new Object[ranks.size()][RankTableModel.COL_NUM];
        int i = 0;
        for (Rank rank : ranks) {
            String rating = "E"+i;
            if (i > RWO_MAX) {
                rating = "O"+(i-RWO_MAX);
            } else if (i > RE_MAX) {
                rating = "WO"+(i-RE_MAX);
            }
            array[i][RankTableModel.COL_NAME_RATE]	= rating;
            array[i][RankTableModel.COL_NAME_MW]	= rank.getNameWithLevels(RPROF_MW);
            array[i][RankTableModel.COL_NAME_ASF]	= rank.getNameWithLevels(RPROF_ASF);
            array[i][RankTableModel.COL_NAME_VEE]	= rank.getNameWithLevels(RPROF_VEE);
            array[i][RankTableModel.COL_NAME_NAVAL]	= rank.getNameWithLevels(RPROF_NAVAL);
            array[i][RankTableModel.COL_NAME_INF]	= rank.getNameWithLevels(RPROF_INF);
            array[i][RankTableModel.COL_NAME_TECH]	= rank.getNameWithLevels(RPROF_TECH);
            array[i][RankTableModel.COL_OFFICER] = rank.isOfficer();
            array[i][RankTableModel.COL_PAYMULT] = rank.getPayMultiplier();
            i++;
        }
        return array;
    }

    public void setRanksFromModel(RankTableModel model) {
        ranks = new ArrayList<>();
        @SuppressWarnings("rawtypes") // Broken java doesn't have typed vectors in the DefaultTableModel
        Vector<Vector> vectors = model.getDataVector();
        for (@SuppressWarnings("rawtypes") Vector row : vectors) {
            String[] names = { (String)row.get(RankTableModel.COL_NAME_MW), (String)row.get(RankTableModel.COL_NAME_ASF),
                    (String)row.get(RankTableModel.COL_NAME_VEE), (String)row.get(RankTableModel.COL_NAME_NAVAL),
                    (String)row.get(RankTableModel.COL_NAME_INF), (String)row.get(RankTableModel.COL_NAME_TECH) };
            Boolean officer = (Boolean)row.get(RankTableModel.COL_OFFICER);
            double payMult = (Double)row.get(RankTableModel.COL_PAYMULT);
            ranks.add(new Rank(names, officer, payMult));
        }
    }

    @Override
    public String toString() {
        return getRankSystemName();
    }
}
