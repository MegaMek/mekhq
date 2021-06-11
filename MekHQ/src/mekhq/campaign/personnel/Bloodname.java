/*
 * Bloodname.java
 *
 * Copyright (c) 2014 - Carl Spain. All Rights Reserved.
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
package mekhq.campaign.personnel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;

import mekhq.campaign.personnel.enums.Phenotype;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;

/**
 * @author Neoancient
 */
public class Bloodname implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 3958964485520416824L;

    private static List<Bloodname> bloodnames;

    private String name;
    private String founder;
    private Clan origClan;
    private boolean exclusive;
    private boolean limited;
    private int inactive;
    private int abjured;
    private int reactivated;
    private int startDate;
    private Phenotype phenotype;
    private List<Clan> postReavingClans;
    private List<NameAcquired> acquiringClans;
    private NameAcquired absorbed;
    //endregion Variable Declarations

    public Bloodname() {
        name = "";
        founder = "";
        exclusive = false;
        limited = false;
        inactive = 0;
        abjured = 0;
        reactivated = 0;
        startDate = 2807;
        phenotype = Phenotype.GENERAL;
        postReavingClans = new ArrayList<>();
        acquiringClans = new ArrayList<>();
        absorbed = null;
    }

    public String getName() {
        return name;
    }

    public String getFounder() {
        return founder;
    }

    public Clan getOriginClan() {
        return origClan;
    }

    public String getOrigClan() {
        return origClan.getCode();
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public boolean isLimited() {
        return limited;
    }

    public boolean isInactive(int year) {
        return (year < startDate) || ((inactive > 0) && (inactive < year)
                && !((reactivated > 0) && (reactivated <= year)));
    }

    public boolean isAbjured(int year) {
        return ((abjured > 0) && (abjured < year));
    }

    public Phenotype getPhenotype() {
        return phenotype;
    }

    public List<Clan> getPostReavingClans() {
        return postReavingClans;
    }

    public List<NameAcquired> getAcquiringClans() {
        return acquiringClans;
    }

    public NameAcquired getAbsorbed() {
        return absorbed;
    }

    /**
     * @param warriorType A Phenotype constant
     * @param year The current year of the campaign setting
     * @return An adjustment to the frequency of this name for the phenotype.
     *
     * A warrior is three times as likely to have a Bloodname associated with the
     * same phenotype as a general name (which is split among the three types).
     * Elemental names are treated as general prior to 2870. The names that later
     * became associated with ProtoMech pilots (identified in WoR) are assumed
     * to have been poor performers and have a lower frequency even before the
     * invention of the PM, though have a higher frequency for PM pilots than other
     * aerospace names.
     */
    private int phenotypeMultiplier(Phenotype warriorType, int year) {
        switch (getPhenotype()) {
            case MECHWARRIOR:
                return (warriorType == Phenotype.MECHWARRIOR) ? 3 : 0;
            case AEROSPACE:
                return ((warriorType == Phenotype.AEROSPACE) || (warriorType == Phenotype.PROTOMECH)) ? 3 : 0;
            case ELEMENTAL:
                if (year < 2870) {
                    return 1;
                }
                return (warriorType == Phenotype.ELEMENTAL) ? 3 : 0;
            case PROTOMECH:
                switch (warriorType) {
                    case PROTOMECH:
                        return 9;
                    case AEROSPACE:
                        return 1;
                    default:
                        return 0;
                }
            case NAVAL:
                return (warriorType == Phenotype.NAVAL) ? 3 : 0;
            case VEHICLE:
            case GENERAL:
            default:
                return 1;
        }
    }

    public static Bloodname loadFromXml(Node node) {
        Bloodname retVal = new Bloodname();
        NodeList nl = node.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node wn = nl.item(i);

            if (wn.getNodeName().equalsIgnoreCase("name")) {
                retVal.name = wn.getTextContent().trim();
            } else if (wn.getNodeName().equalsIgnoreCase("founder")) {
                retVal.founder = wn.getTextContent().trim();
            } else if (wn.getNodeName().equalsIgnoreCase("clan")) {
                retVal.origClan = Clan.getClan(wn.getTextContent().trim());
            } else if (wn.getNodeName().equalsIgnoreCase("exclusive")) {
                retVal.exclusive = true;
            } else if (wn.getNodeName().equalsIgnoreCase("reaved")) {
                retVal.inactive = Integer.parseInt(wn.getTextContent().trim());
            } else if (wn.getNodeName().equalsIgnoreCase("dormant")) {
                retVal.inactive = Integer.parseInt(wn.getTextContent().trim()) + 10;
            } else if (wn.getNodeName().equalsIgnoreCase("abjured")) {
                retVal.abjured = Integer.parseInt(wn.getTextContent().trim());
            } else if (wn.getNodeName().equalsIgnoreCase("reactivated")) {
                retVal.reactivated = Integer.parseInt(wn.getTextContent().trim() + 20);
            } else if (wn.getNodeName().equalsIgnoreCase("phenotype")) {
                retVal.phenotype = Phenotype.parseFromString(wn.getTextContent().trim());
            } else if (wn.getNodeName().equalsIgnoreCase("postReaving")) {
                String[] clans = wn.getTextContent().trim().split(",");
                for (String c : clans) {
                    retVal.postReavingClans.add(Clan.getClan(c));
                }
            } else if (wn.getNodeName().equalsIgnoreCase("acquired")) {
                retVal.acquiringClans.add(new NameAcquired(
                        Integer.parseInt(wn.getAttributes().getNamedItem("date").getTextContent()) + 10,
                        wn.getTextContent().trim()));
            } else if (wn.getNodeName().equalsIgnoreCase("shared")) {
                retVal.acquiringClans.add(new NameAcquired(
                        Integer.parseInt(wn.getAttributes().getNamedItem("date").getTextContent()),
                        wn.getTextContent().trim()));
            } else if (wn.getNodeName().equalsIgnoreCase("absorbed")) {
                retVal.absorbed = new NameAcquired(
                        Integer.parseInt(wn.getAttributes().getNamedItem("date").getTextContent()),
                        wn.getTextContent().trim());
            } else if (wn.getNodeName().equalsIgnoreCase("created")) {
                retVal.startDate = Integer.parseInt(wn.getTextContent().trim()) + 20;
            }
        }

        return retVal;
    }

    /**
     * Determines a likely Bloodname based on Clan, phenotype, and year.
     *
     * @param factionCode The faction code for the Clan; must exist in data/names/bloodnames/clans.xml
     * @param phenotype The person's Phenotype
     * @param year The current campaign year
     * @return An object representing the chosen Bloodname
     *
     * Though based as much as possible on official sources, the method employed here involves a
     * considerable amount of speculation.
     */
    public static @Nullable Bloodname randomBloodname(String factionCode, Phenotype phenotype, int year) {
        return randomBloodname(Clan.getClan(factionCode), phenotype, year);
    }

    /**
     * Determines a likely Bloodname based on Clan, phenotype, and year.
     *
     * @param faction The Clan faction; must exist in data/names/bloodnames/clans.xml
     * @param phenotype The person's Phenotype
     * @param year The current campaign year
     * @return An object representing the chosen Bloodname
     *
     * Though based as much as possible on official sources, the method employed here involves a
     * considerable amount of speculation.
     */
    public static @Nullable Bloodname randomBloodname(Clan faction, Phenotype phenotype, int year) {
        if (faction == null) {
            MekHQ.getLogger().error("Random Bloodname attempted for a clan that does not exist."
                    + System.lineSeparator()
                    + "Please ensure that your clan exists in both the clans.xml and bloodnames.xml files as appropriate.");
            return null;
        } else if (phenotype == null) {
            MekHQ.getLogger().error("Random Bloodname attempted for an unknown phenotype. Please open a bug report so this issue may be fixed.");
            return null;
        }

        // This is required because there are currently no bloodnames specifically for vehicle phenotypes
        if (phenotype == Phenotype.VEHICLE) {
            phenotype = Phenotype.GENERAL;
        }

        if (Compute.randomInt(20) == 0) {
            /* 1 in 20 chance that warrior was taken as isorla from another Clan */
            return randomBloodname(faction.getRivalClan(year), phenotype, year);
        }
        if (Compute.randomInt(20) == 0) {
            /* Bloodnames that are predominantly used for a particular phenotype are not
             * exclusively used for that phenotype. A 5% chance of ignoring phenotype will
             * result in a very small chance (around 1%) of a Bloodname usually associated
             * with a different phenotype.
             */
            phenotype = Phenotype.GENERAL;
        }

        /* The relative probability of the various Bloodnames that are original to this Clan */
        Map<Bloodname, Fraction> weights = new HashMap<>();
        /* A list of non-exclusive Bloodnames from other Clans */
        List<Bloodname> nonExclusives = new ArrayList<>();
        /* The relative probability that a warrior in this Clan will have a non-exclusive
         * Bloodname that originally belonged to another Clan; the smaller the number
         * of exclusive Bloodnames of this Clan, the larger this chance.
         */
        double nonExclusivesWeight = 0.0;

        for (Bloodname name : bloodnames) {
            /* Bloodnames exclusive to Clans that have been abjured (NC, WIE) continue
             * to be used by those Clans but not by others.
             */
            if (name.isInactive(year) ||
                    (name.isAbjured(year) && !name.getOrigClan().equals(faction.getGenerationCode()))
                    || (0 == name.phenotypeMultiplier(phenotype, year))) {
                continue;
            }

            Fraction weight = null;

            /* Effects of the Wars of Reaving would take a generation to show up
             * in the breeding programs, so the tables given in the WoR source book
             * are in effect from about 3100 on.
             */
            if (year < 3100) {
                int numClans = 1;
                for (Bloodname.NameAcquired a : name.getAcquiringClans()) {
                    if (a.year < year) {
                        numClans++;
                    }
                }
                /* Non-exclusive names have a weight of 1 (equal to exclusives) up to 2900,
                 * then decline 10% per 50 years to a minimum of 0.6 in 3050+. In the few
                 * cases where the other Clans using the name are known, the weight is
                 * 1/(number of Clans) instead.
                 */
                if (name.getOrigClan().equals(faction.getGenerationCode()) ||
                        (null != name.getAbsorbed() && faction.getGenerationCode().equals(name.getAbsorbed().clan) &&
                        name.getAbsorbed().year > year)) {
                    if (name.isExclusive() || numClans > 1) {
                        weight = new Fraction(1, numClans);
                    } else {
                        weight = eraFraction(year);
                        nonExclusivesWeight += 1 - eraFraction(year).value();
                        /* The fraction is squared to represent the combined effect
                         * of increasing distribution among the Clans and the likelihood
                         * that non-exclusive names would suffer
                         * more reavings and have a lower Bloodcount.
                         */
                        weight.mul(eraFraction(year));
                    }
                } else {
                    /* Most non-exclusives have an unknown distribution and are estimated.
                     * When the actual Clans sharing the Bloodname are known, it is divided
                     * among those Clans.
                     */
                    for (Bloodname.NameAcquired a : name.getAcquiringClans()) {
                        if (faction.getGenerationCode().equals(a.clan)) {
                            weight = new Fraction(1, numClans);
                            break;
                        }
                    }
                    if (null == weight && !name.isExclusive()) {
                        for (int i = 0; i < name.phenotypeMultiplier(phenotype, year); i++) {
                            nonExclusives.add(name);
                        }
                    }
                }
            } else {
                if (name.getPostReavingClans().contains(faction)) {
                    weight = new Fraction(name.phenotypeMultiplier(phenotype, year), name.getPostReavingClans().size());
                    /* Assume that Bloodnames that were exclusive before the Wars of Reaving
                     * are more numerous (higher bloodcount).
                     */
                    if (!name.isLimited()) {
                        if (name.isExclusive()) {
                            weight.mul(4);
                        } else {
                            weight.mul(2);
                        }
                    }
                } else if (name.getPostReavingClans().size() == 0) {
                    for (int i = 0; i < name.phenotypeMultiplier(phenotype, year); i++) {
                        nonExclusives.add(name);
                    }
                }
            }
            if (null != weight) {
                weight.mul(name.phenotypeMultiplier(phenotype, year));
                weights.put(name, weight);
            }
        }

        int lcd = Fraction.lcd(weights.values());
        for (Fraction f : weights.values()) {
            f.mul(lcd);
        }
        List<Bloodname> nameList = new ArrayList<>();
        for (Bloodname b : weights.keySet()) {
            for (int i = 0; i < weights.get(b).value(); i++) {
                nameList.add(b);
            }
        }
        nonExclusivesWeight *= lcd;
        if (year >= 3100) {
            nonExclusivesWeight = nameList.size() / 10.0;
        }
        int roll = Compute.randomInt(nameList.size() + (int) (nonExclusivesWeight + 0.5));
        if (roll > nameList.size() - 1) {
            return nonExclusives.get(Compute.randomInt(nonExclusives.size()));
        }
        return nameList.get(roll);
    }

    /**
     * Represents the decreasing frequency of non-exclusive names within the original Clan
     * due to dispersal throughout the Clans and reavings.
     *
     * @param year The current year of the campaign
     * @return A fraction that decreases by 10%/year
     */
    private static Fraction eraFraction(int year) {
        if (year < 2900) {
            return new Fraction(1);
        } else if (year < 2950) {
            return new Fraction(9, 10);
        } else if (year < 3000) {
            return new Fraction(4, 5);
        } else if (year < 3050) {
            return new Fraction(7, 10);
        } else {
            return new Fraction(3, 5);
        }
    }

    public static void loadBloodnameData() {
        Clan.loadClanData();
        bloodnames = new ArrayList<>();

        File f = new File("data/names/bloodnames/bloodnames.xml");
        FileInputStream fis;
        try {
            fis = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            MekHQ.getLogger().error(Bloodname.class, "Cannot find file bloodnames.xml");
            return;
        }

        Document doc;

        try {
            DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();
            doc = db.parse(fis);
            fis.close();
        } catch (Exception ex) {
            MekHQ.getLogger().error(Bloodname.class, "Could not parse bloodnames.xml");
            MekHQ.getLogger().error(Bloodname.class, ex);
            return;
        }

        Element bloodnameElement = doc.getDocumentElement();
        NodeList nl = bloodnameElement.getChildNodes();
        bloodnameElement.normalize();

        for (int i = 0; i < nl.getLength(); i++) {
            Node wn = nl.item(i);
            if (wn.getNodeType() == Node.ELEMENT_NODE) {
                if (wn.getNodeName().equalsIgnoreCase("bloodname")) {
                    bloodnames.add(Bloodname.loadFromXml(wn));
                }
            }
        }
        MekHQ.getLogger().info(Bloodname.class, "Loaded " + bloodnames.size() + " Bloodname records.");
    }

    private static class NameAcquired {
        public int year;
        public String clan;
        public NameAcquired(int y, String c) {
            year = y;
            clan = c;
        }
    }

    static class Fraction {
        private int numerator;
        private int denominator;

        public Fraction() {
            numerator = 0;
            denominator = 1;
        }

        public Fraction(int n, int d) {
            if (d == 0) {
                throw new IllegalArgumentException("Denominator is zero.");
            }
            if (d < 0) {
                n = -n;
                d = -d;
            }
            numerator = n;
            denominator = d;
        }

        public Fraction(int i) {
            numerator = i;
            denominator = 1;
        }

        public Fraction(Fraction f) {
            numerator = f.numerator;
            denominator = f.denominator;
        }

        @Override
        public Object clone() {
            return new Fraction(this);
        }

        @Override
        public String toString() {
            return numerator + "/" + denominator;
        }

        public boolean equals(Fraction f) {
            return value() == f.value();
        }

        public double value() {
            return (double)numerator / (double)denominator;
        }

        public void reduce() {
            if (denominator > 1) {
                for (int i = denominator - 1; i > 1; i--) {
                    if (numerator % i == 0 && denominator % i == 0) {
                        numerator /= i;
                        denominator /= i;
                        i = denominator - 1;
                    }
                }
            }
        }

        public int getNumerator() {
            return numerator;
        }

        public int getDenominator() {
            return denominator;
        }

        public void add(Fraction f) {
            numerator = numerator * f.denominator + f.numerator * denominator;
            denominator = denominator * f.denominator;
            reduce();
        }

        public void add(int i) {
            numerator += i * denominator;
            reduce();
        }

        public void sub(Fraction f) {
            numerator = numerator * f.denominator - f.numerator * denominator;
            denominator = denominator * f.denominator;
            reduce();
        }

        public void sub(int i) {
            numerator -= i * denominator;
            reduce();
        }

        public void mul(Fraction f) {
            numerator *= f.numerator;
            denominator *= f.denominator;
            reduce();
        }

        public void mul(int i) {
            numerator *= i;
            reduce();
        }

        public void div(Fraction f) {
            numerator *= f.denominator;
            denominator *= f.numerator;
            reduce();
        }

        public void div(int i) {
            denominator *= i;
        }

        public static int lcd(Collection<Fraction> list) {
            Set<Integer> denominators = new HashSet<>();
            for (Fraction f : list) {
                denominators.add(f.denominator);
            }
            boolean done = false;
            int retVal = 1;
            while (!done) {
                done = true;
                for (Integer d : denominators) {
                    if (d / retVal > 1 || retVal % d != 0) {
                        retVal++;
                        done = false;
                        break;
                    }
                }
            }
            return retVal;
        }
    }
}
