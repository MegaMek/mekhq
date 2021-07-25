/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel;

import megamek.common.Compute;
import megamek.common.util.StringUtil;
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
 * TODO : I should be part of faction, and hence I am deprecated
 */
@Deprecated
public class Clan {
    //region Variable Declarations
    private static Map<String, Clan> allClans;

    private String code;
    private String generationCode; // this is used to enable RA name generation using CSR lists, for example
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

    /**
     * @param o the object to compare to
     * @return true if they are equal, otherwise false
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Clan) {
            return code.equals(((Clan) o).code);
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

    /**
     * @param code The code to get a Clan for
     * @return the Clan object for the code, or null if there isn't a Clan with that code
     */
    public static Clan getClan(String code) {
        return allClans.get(code);
    }

    /**
     * @return a Collection of all Clan objects
     */
    public static Collection<Clan> getClans() {
        return allClans.values();
    }

    /**
     * @return the Clan's code
     */
    public String getCode() {
        return code;
    }

    /**
     * @return the code that the Clan uses to generate Bloodnames
     */
    public String getGenerationCode() {
        if (!StringUtil.isNullOrEmpty(generationCode)) {
            return generationCode;
        } else {
            return code;
        }
    }

    /**
     * @param year the year to get the Clan's name for
     * @return the full name of the Clan at the specified year
     */
    public String getFullName(int year) {
        for (DatedRecord r : nameChanges) {
            if (r.isActive(year)) {
                return r.getDescription();
            }
        }
        return fullName;
    }

    /**
     * @param year the year to determine if the Clan is active in
     * @return whether the Clan is active or not in the specified year
     */
    public boolean isActive(int year) {
        return ((startDate < year) && ((endDate == 0) || (endDate > year)));
    }

    /**
     * @return the date the Clan starts
     */
    public int getStartDate() {
        return startDate;
    }

    /**
     * @return the date the Clan ends
     */
    public int getEndDate() {
        return endDate;
    }

    /**
     * @param year the year to determine if the Clan has been abjured by
     * @return whether or not the Clan has been abjured
     */
    public boolean isAbjured(int year) {
        if (abjurationDate == 0) {
            return false;
        } else {
            return abjurationDate < year;
        }
    }

    /**
     * @param year the year to get the Clan's rivals in
     * @return a list of all of the Clan's rivals in the specified year
     */
    public List<Clan> getRivals(int year) {
        List<Clan> retVal = new ArrayList<>();
        for (DatedRecord r : rivals) {
            if (r.isActive(year)) {
                Clan c = allClans.get(r.getDescription());
                if (c.isActive(year)) {
                    retVal.add(c);
                }
            }
        }
        return retVal;
    }

    /**
     * @return whether or not the Clan is a Home Clan
     */
    public boolean isHomeClan() {
        return homeClan;
    }

    /**
     * @param year the year to get a single rival clan in
     * @return a rival clan to the current Clan
     */
    public Clan getRivalClan(int year) {
        List<Clan> rivals = getRivals(year);
        int roll = Compute.randomInt(rivals.size() + 1);
        if (roll > rivals.size() - 1) {
            return randomClan(year, isHomeClan());
        }
        return rivals.get(roll);
    }

    /**
     * @param year the year to get a random Clan for
     * @param homeClan whether or not the Clan is a Home Clan
     * @return a random Clan
     */
    public static Clan randomClan(int year, boolean homeClan) {
        List<Clan> list = new ArrayList<>();
        for (Clan c : getClans()) {
            if ((year > 3075) && (homeClan != c.isHomeClan())) {
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
        File f = new File("data/names/bloodnames/clans.xml"); // TODO : Remove inline file path
        FileInputStream fis;
        try {
            fis = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            MekHQ.getLogger().error("Cannot find file clans.xml");
            return;
        }

        Document doc;

        try {
            DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();
            doc = db.parse(fis);
            fis.close();
        } catch (Exception ex) {
            MekHQ.getLogger().error("Could not parse clans.xml", ex);
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

            try {
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
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }

        return retVal;
    }

    /**
     * This holds dated records for Clan events
     */
    private static class DatedRecord {
        private int startDate;
        private int endDate;
        private String description;

        public DatedRecord(int s, int e, String d) {
            setStartDate(s);
            setEndDate(e);
            setDescription(d);
        }

        public boolean isActive(int year) {
            return (getStartDate() < year) && ((getEndDate() == 0) || (getEndDate() > year));
        }

        public int getStartDate() {
            return startDate;
        }

        public void setStartDate(int startDate) {
            this.startDate = startDate;
        }

        public int getEndDate() {
            return endDate;
        }

        public void setEndDate(int endDate) {
            this.endDate = endDate;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
