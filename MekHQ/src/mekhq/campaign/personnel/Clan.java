/*
 * Copyright (c) 2020 The MegaMek Team
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel;

import megamek.common.Compute;
import megamek.common.logging.LogLevel;
import megamek.common.util.StringUtil;
import megameklab.com.util.StringUtils;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * This is used to supply clan data needed to generate bloodnames
 * TODO : Merge me into Faction?  - Ask about this Windchild
 */
public class Clan {
    //region Variable Declarations
    private static Map<String, Clan> allClans;

    private String code;
    private String generationCode; // this is used to enable RA name generation using CSR lists
    private String fullName;
    private int startDate;
    private int endDate;
    private int abjurationDate;
    private List<DatedRecord> rivals;
    private List<DatedRecord> nameChanges;
    private boolean homeClan;
    //endregion Variable Declarations

    public Clan() {
        startDate = 2807;
        endDate = 9999;
        abjurationDate = 0;
        rivals = new ArrayList<>();
        nameChanges = new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Clan) {
            return code.equals(((Clan)o).code);
        } else if (o instanceof String) {
            return code.equals(o);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getCode());
    }

    public static Clan getClan(String code) {
        return allClans.get(code);
    }

    public static Collection<Clan> getClans() {
        return allClans.values();
    }

    public String getCode() {
        return code;
    }

    public String getGenerationCode() {
        if (!StringUtil.isNullOrEmpty(generationCode)) {
            return generationCode;
        } else {
            return code;
        }
    }

    public String getFullName(int year) {
        for (DatedRecord r : nameChanges) {
            if (r.isActive(year)) {
                return r.description;
            }
        }
        return fullName;
    }

    public boolean isActive(int year) {
        return ((startDate < year) && ((endDate == 0) || (endDate > year)));
    }

    public int getStartDate() {
        return startDate;
    }

    public int getEndDate() {
        return endDate;
    }

    public boolean isAbjured(int year) {
        if (abjurationDate == 0) return false;
        return abjurationDate < year;
    }

    public List<Clan> getRivals(int year) {
        List<Clan> retVal = new ArrayList<>();
        for (DatedRecord r : rivals) {
            if (r.isActive(year)) {
                Clan c = allClans.get(r.description);
                if (c.isActive(year)) {
                    retVal.add(c);
                }
            }
        }
        return retVal;
    }

    public boolean isHomeClan() {
        return homeClan;
    }

    public Clan getRivalClan(int year) {
        List<Clan> rivals = getRivals(year);
        int roll = Compute.randomInt(rivals.size() + 1);
        if (roll > rivals.size() - 1) {
            return randomClan(year, homeClan);
        }
        return rivals.get(roll);
    }

    public static Clan randomClan(int year, boolean homeClan) {
        List<Clan> list = new ArrayList<>();
        for (Clan c : allClans.values()) {
            if (year > 3075 && homeClan != c.homeClan) {
                continue;
            }
            if (c.isActive(year)) {
                list.add(c);
            }
        }
        return list.get(Compute.randomInt(list.size()));
    }

    public static void loadClanData() {
        allClans = new HashMap<>();
        File f = new File("data/names/bloodnames/clans.xml");
        FileInputStream fis;
        try {
            fis = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            MekHQ.getLogger().log(Bloodname.class, "loadClanData()", LogLevel.ERROR,
                    "Cannot find file clans.xml"); //$NON-NLS-1$
            return;
        }

        Document doc;

        try {
            DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();
            doc = db.parse(fis);
            fis.close();
        } catch (Exception ex) {
            MekHQ.getLogger().error(Bloodname.class, "loadClanData()", "Could not parse clans.xml", ex);
            return;
        }

        Element clanElement = doc.getDocumentElement();
        NodeList nl = clanElement.getChildNodes();
        clanElement.normalize();

        for (int i = 0; i < nl.getLength(); i++) {
            Node wn = nl.item(i);
            if (wn.getNodeName().equalsIgnoreCase("clan")) {
                Clan c = loadFromXml(wn);
                allClans.put(c.code, c);
            }
        }
    }

    private static Clan loadFromXml(Node node) {
        Clan retVal = new Clan();

        retVal.code = node.getAttributes().getNamedItem("code").getTextContent().trim();
        if (null != node.getAttributes().getNamedItem("start")) {
            retVal.startDate = Integer.parseInt(node.getAttributes().getNamedItem("start").getTextContent().trim());
        }
        if (null != node.getAttributes().getNamedItem("end")) {
            retVal.endDate = Integer.parseInt(node.getAttributes().getNamedItem("end").getTextContent().trim());
        }
        NodeList nl = node.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node wn = nl.item(i);

            if (wn.getNodeName().equalsIgnoreCase("fullName")) {
                retVal.fullName = wn.getTextContent().trim();
            } else if (wn.getNodeName().equalsIgnoreCase("abjured")) {
                retVal.abjurationDate = Integer.parseInt(wn.getTextContent().trim());
            } else if (wn.getNodeName().equalsIgnoreCase("nameChange")) {
                int start = retVal.startDate;
                int end = retVal.endDate;
                if (null != wn.getAttributes().getNamedItem("start")) {
                    start = Integer.parseInt(wn.getAttributes().getNamedItem("start").getTextContent().trim());
                }
                if (null != wn.getAttributes().getNamedItem("end")) {
                    end = Integer.parseInt(wn.getAttributes().getNamedItem("end").getTextContent().trim());
                }
                retVal.nameChanges.add(new DatedRecord(start, end, wn.getTextContent().trim()));
            } else if (wn.getNodeName().equalsIgnoreCase("rivals")) {
                int start = retVal.startDate;
                int end = retVal.endDate;
                if (null != wn.getAttributes().getNamedItem("start")) {
                    start = Integer.parseInt(wn.getAttributes().getNamedItem("start").getTextContent().trim());
                }
                if (null != wn.getAttributes().getNamedItem("end")) {
                    end = Integer.parseInt(wn.getAttributes().getNamedItem("end").getTextContent().trim());
                }
                String[] rivals = wn.getTextContent().trim().split(",");
                for (String r : rivals) {
                    retVal.rivals.add(new DatedRecord(start, end, r));
                }
            } else if (wn.getNodeName().equalsIgnoreCase("homeClan")) {
                retVal.homeClan = true;
            } else if (wn.getNodeName().equalsIgnoreCase("generateAsIf")) {
                retVal.generationCode = wn.getTextContent().trim();
            }
        }

        return retVal;
    }

    private static class DatedRecord {
        public int startDate;
        public int endDate;
        public String description;

        public DatedRecord(int s, int e, String d) {
            startDate = s;
            endDate = e;
            description = d;
        }

        public boolean isActive(int year) {
            return (startDate < year) && ((endDate == 0) || (endDate > year));
        }
    }
}
