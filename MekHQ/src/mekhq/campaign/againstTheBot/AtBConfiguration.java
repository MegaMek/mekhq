/*
 * AtBPreferences.java
 *
 * Copyright (c) 2014 - Carl Spain. All rights reserved.
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
package mekhq.campaign.againstTheBot;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

/**
 * @author Neoancient
 *
 * Class that handles configuration options for Against the Bot campaigns
 * more extensive than what is handled by CampaignOptions. Most of the options
 * fall into one of two categories: they allow users to customize the various
 * tables in the rules, or they avoid hard-coding universe details.
 */
public class AtBConfiguration {
    /* Used to indicate size of lance or equivalent in OpFor forces */
    public static final String ORG_IS = "IS";
    public static final String ORG_CLAN = "CLAN";
    public static final String ORG_CS = "CS";

    public static final char WEIGHT_ULTRA_LIGHT = 'U';
    public static final char WEIGHT_LIGHT = 'L';
    public static final char WEIGHT_MEDIUM = 'M';
    public static final char WEIGHT_HEAVY = 'H';
    public static final char WEIGHT_ASSAULT = 'A';
    public static final char WEIGHT_SUPER_HEAVY = 'V';

    /* Scenario generation */
    private HashMap<String, List<WeightedTable<String>>> botForceTables = new HashMap<>();
    private HashMap<String, List<WeightedTable<String>>> botLanceTables = new HashMap<>();

    /* Contract generation */
    private ArrayList<DatedRecord<String>> hiringHalls;

    /* Personnel and unit markets */
    private Money shipSearchCost;
    private int shipSearchLengthWeeks = 4;
    private Integer dropshipSearchTarget;
    private Integer jumpshipSearchTarget;
    private Integer warshipSearchTarget;
    private WeightedTable<String> dsTable;
    private WeightedTable<String> jsTable;

    private final transient ResourceBundle defaultProperties = ResourceBundle.getBundle("mekhq.resources.AtBConfigDefaults",
            MekHQ.getMHQOptions().getLocale());

    private AtBConfiguration() {
        hiringHalls = new ArrayList<>();
        dsTable = new WeightedTable<>();
        jsTable = new WeightedTable<>();
        shipSearchCost = Money.of(100000);
    }

    /**
     * Provide default values in case the file is missing or contains errors.
     */
    private WeightedTable<String> getDefaultForceTable(String key, int index) {
        if (index < 0) {
            LogManager.getLogger().error("Default force tables don't support negative weights, limiting to 0");
            index = 0;
        }
        String property = defaultProperties.getString(key);
        String[] fields = property.split("\\|");
        if (index >= fields.length) {
            // Deal with too short field lengths
            LogManager.getLogger().error(String.format("Default force tables have %d weight entries; limiting the original value of %d.", fields.length, index));
            index = fields.length - 1;
        }
        return parseDefaultWeightedTable(fields[index]);
    }

    private WeightedTable<String> parseDefaultWeightedTable(String entry) {
        return parseDefaultWeightedTable(entry, Function.identity());
    }

    private <T>WeightedTable<T> parseDefaultWeightedTable(String entry, Function<String,T> fromString) {
        WeightedTable<T> retVal = new WeightedTable<>();
        String[] entries = entry.split(",");
        for (String e : entries) {
            try {
                String[] fields = e.split(":");
                retVal.add(Integer.parseInt(fields[0]), fromString.apply(fields[1]));
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        }
        return retVal;
    }

    /**
     * Used if the config file is missing.
     */
    private void setAllValuesToDefaults() {
        for (Enumeration<String> e = defaultProperties.getKeys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            String property = defaultProperties.getString(key);
            switch (key) {
                case "botForce.IS":
                case "botForce.CLAN":
                case "botForce.CS":
                    ArrayList<WeightedTable<String>> list = new ArrayList<>();
                    for (String entry : property.split("\\|")) {
                        list.add(parseDefaultWeightedTable(entry));
                    }
                    botForceTables.put(key.replace("botForce.", ""), list);
                    break;
                case "botLance.IS":
                case "botLance.CLAN":
                case "botLance.CS":
                    list = new ArrayList<>();
                    for (String entry : property.split("\\|")) {
                        list.add(parseDefaultWeightedTable(entry));
                    }
                    botLanceTables.put(key.replace("botLance.", ""), list);
                    break;
                case "hiringHalls":
                    for (String entry : property.split("\\|")) {
                        String[] fields = entry.split(",");
                        hiringHalls.add(new DatedRecord<>(
                                !fields[0].isBlank() ? MHQXMLUtility.parseDate(fields[0]) : null,
                                !fields[1].isBlank() ? MHQXMLUtility.parseDate(fields[1]) : null,
                                fields[2]));
                    }
                    break;
                case "shipSearchCost":
                    shipSearchCost = Money.of(Double.parseDouble(property));
                    break;
                case "shipSearchLengthWeeks":
                    shipSearchLengthWeeks = Integer.parseInt(property);
                    break;
                case "shipSearchTarget.Dropship":
                    dropshipSearchTarget = property.matches("\\d+") ? Integer.valueOf(property) : null;
                    break;
                case "shipSearchTarget.Jumpship":
                    jumpshipSearchTarget = property.matches("\\d+") ? Integer.valueOf(property) : null;
                    break;
                case "shipSearchTarget.Warship":
                    warshipSearchTarget = property.matches("\\d+") ? Integer.valueOf(property) : null;
                    break;
                case "ships.Dropship":
                    dsTable = parseDefaultWeightedTable(property);
                    break;
                case "ships.Jumpship":
                    jsTable = parseDefaultWeightedTable(property);
                    break;
            }
        }
    }

    public int weightClassIndex(int entityWeightClass) {
        return entityWeightClass - 1;
    }

    public int weightClassIndex(String wc) {
        switch (wc) {
            case "L":
            case "UL":
                return 0;
            case "M":
                return 1;
            case "H":
                return 2;
            case "A":
            case "C":
            case "SH":
                return 3;
            default:
                throw new IllegalArgumentException("Could not parse weight class " + wc);
        }
    }

    public @Nullable String selectBotLances(String org, int weightClass) {
        return selectBotLances(org, weightClass, 0f);
    }

    public @Nullable String selectBotLances(String org, int weightClass, float rollMod) {
        if (botForceTables.containsKey(org)) {
            final List<WeightedTable<String>> botForceTable = botForceTables.get(org);
            int weightClassIndex = weightClassIndex(weightClass);
            WeightedTable<String> table;
            if ((weightClassIndex < 0) || (weightClassIndex >= botForceTable.size())) {
                LogManager.getLogger().error(String.format("Bot force tables for organization \"%s\" don't have an entry for weight class %d, limiting to valid values", org, weightClass));
                weightClassIndex = Math.max(0, Math.min(weightClassIndex, botForceTable.size() - 1));
            }
            table = botForceTable.get(weightClassIndex);
            if (null == table) {
                table = getDefaultForceTable("botForce." + org, weightClassIndex);
            }
            return table.select(rollMod);
        } else {
            LogManager.getLogger().error(String.format("Bot force tables for organization \"%s\" not found, ignoring", org));
            return null;
        }
    }

    public @Nullable String selectBotUnitWeights(String org, int weightClass) {
        if (botLanceTables.containsKey(org)) {
            WeightedTable<String> table = botLanceTables.get(org).get(weightClassIndex(weightClass));
            if (table == null) {
                table = this.getDefaultForceTable("botLance." + org, weightClassIndex(weightClass));
            }
            return table.select();
        }
        return null;
    }

    /**
     * Translates character code in the indicated position to the appropriate weight
     * class constant.
     *
     * @param s        A String of single-character codes that indicate the weight classes of the units in a lance (e.g. "LMMH")
     * @param i        The index of the code to be translated
     * @return        The value used by UnitTableData to find the correct RAT for the weight class
     */
    public static int decodeWeightStr(String s, int i) {
        switch (s.charAt(i)) {
            case WEIGHT_ULTRA_LIGHT:
                return EntityWeightClass.WEIGHT_ULTRA_LIGHT;
            case WEIGHT_LIGHT:
                return EntityWeightClass.WEIGHT_LIGHT;
            case WEIGHT_MEDIUM:
                return EntityWeightClass.WEIGHT_MEDIUM;
            case WEIGHT_HEAVY:
                return EntityWeightClass.WEIGHT_HEAVY;
            case WEIGHT_ASSAULT:
                return EntityWeightClass.WEIGHT_ASSAULT;
            case WEIGHT_SUPER_HEAVY:
                return EntityWeightClass.WEIGHT_SUPER_HEAVY;
            default:
                return 0;
        }
    }

    public static String getParentFactionType(final Faction faction) {
        if (faction.isComStar()) {
            return AtBConfiguration.ORG_CS;
        } else if (faction.isClan()) {
            return AtBConfiguration.ORG_CLAN;
        } else {
            return AtBConfiguration.ORG_IS;
        }
    }

    public boolean isHiringHall(String planet, LocalDate date) {
        return hiringHalls.stream().anyMatch( rec -> rec.getValue().equals(planet)
                && rec.fitsDate(date));
    }

    public Money getShipSearchCost() {
        return shipSearchCost;
    }

    public int getShipSearchLengthWeeks() {
        return shipSearchLengthWeeks;
    }

    public Money shipSearchCostPerWeek() {
        if (shipSearchLengthWeeks <= 0) {
            return Money.zero();
        }

        return shipSearchCost.dividedBy(shipSearchLengthWeeks);
    }

    public @Nullable Integer shipSearchTargetBase(int unitType) {
        switch (unitType) {
            case UnitType.DROPSHIP:
                return dropshipSearchTarget;
            case UnitType.JUMPSHIP:
                return jumpshipSearchTarget;
            case UnitType.WARSHIP:
                return warshipSearchTarget;
            default:
                return null;
        }
    }

    public TargetRoll shipSearchTargetRoll(int unitType, Campaign campaign) {
        final Integer baseShipSearchTarget = shipSearchTargetBase(unitType);
        if (baseShipSearchTarget == null) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Base");
        }

        TargetRoll target = new TargetRoll(baseShipSearchTarget, "Base");
        Person adminLog = campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_LOGISTICS, SkillType.S_ADMIN);
        int adminLogExp = (adminLog == null) ? SkillType.EXP_ULTRA_GREEN : adminLog.getSkill(SkillType.S_ADMIN).getExperienceLevel();

        target.addModifier(SkillType.EXP_REGULAR - adminLogExp, "Admin/Logistics");
        target.addModifier(IUnitRating.DRAGOON_C - campaign.getUnitRatingMod(),
                "Unit Rating");
        return target;
    }

    public @Nullable MechSummary findShip(int unitType) {
        WeightedTable<String> table = null;
        if (unitType == UnitType.JUMPSHIP) {
            table = jsTable;
        } else if (unitType == UnitType.DROPSHIP) {
            table = dsTable;
        }

        if (table == null) {
            return null;
        }

        String shipName = table.select();
        if (shipName == null) {
            return null;
        }
        return MechSummaryCache.getInstance().getMech(shipName);
    }

    public static AtBConfiguration loadFromXml() {
        AtBConfiguration retVal = new AtBConfiguration();

        LogManager.getLogger().info("Starting load of AtB configuration data from XML...");

        Document xmlDoc;
        try (InputStream is = new FileInputStream("data/universe/atbconfig.xml")) { // TODO : Remove inline file path
            DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

            xmlDoc = db.parse(is);
        } catch (FileNotFoundException ex) {
            LogManager.getLogger().info("File data/universe/atbconfig.xml not found. Loading defaults.");
            retVal.setAllValuesToDefaults();
            return retVal;
        } catch (Exception ex) {
            LogManager.getLogger().error("Error parsing file data/universe/atbconfig.xml. Loading defaults.", ex);
            retVal.setAllValuesToDefaults();
            return retVal;
        }

        Element rootElement = xmlDoc.getDocumentElement();
        NodeList nl = rootElement.getChildNodes();
        rootElement.normalize();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);
            switch (wn.getNodeName()) {
                case "scenarioGeneration":
                    retVal.loadScenarioGenerationNodeFromXml(wn);
                    break;
                case "contractGeneration":
                    retVal.loadContractGenerationNodeFromXml(wn);
                    break;
                case "shipSearch":
                    retVal.loadShipSearchNodeFromXml(wn);
                    break;
            }
        }

        return retVal;
    }

    private void loadScenarioGenerationNodeFromXml(Node node) {
        NodeList nl = node.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node wn = nl.item(i);
            String[] orgs;
            List<WeightedTable<String>> list;
            switch (wn.getNodeName()) {
                case "botForce":
                    if (wn.getAttributes().getNamedItem("org") == null) {
                        orgs = new String[1];
                        orgs[0] = ORG_IS;
                    } else {
                        orgs = wn.getAttributes().getNamedItem("org").getTextContent().split(",");
                    }
                    list = loadForceTableFromXml(wn);
                    for (String org : orgs) {
                        botForceTables.put(org, list);
                    }
                    break;
                case "botLance":
                    if (wn.getAttributes().getNamedItem("org") == null) {
                        orgs = new String[1];
                        orgs[0] = ORG_IS;
                    } else {
                        orgs = wn.getAttributes().getNamedItem("org").getTextContent().split(",");
                    }
                    list = loadForceTableFromXml(wn);
                    for (String org : orgs) {
                        botLanceTables.put(org, list);
                    }
                    break;
            }
        }
    }

    private List<WeightedTable<String>> loadForceTableFromXml(Node node) {
        List<WeightedTable<String>> retVal = new ArrayList<>();
        NodeList nl = node.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node wn = nl.item(i);
            if (wn.getNodeName().equals("weightedTable")) {
                try {
                    int weightClass = weightClassIndex(wn.getAttributes()
                            .getNamedItem("weightClass").getTextContent());
                    while (retVal.size() <= weightClass) {
                        retVal.add(null);
                    }
                    retVal.set(weightClass, loadWeightedTableFromXml(wn));
                } catch (Exception ex) {
                    LogManager.getLogger().error("Could not parse weight class attribute for enemy forces table", ex);
                }
            }
        }
        return retVal;
    }

    private void loadContractGenerationNodeFromXml(Node node) {
        NodeList nl = node.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node wn = nl.item(i);
            if (wn.getNodeName().equals("hiringHalls")) {
                hiringHalls.clear();
                for (int j = 0; j < wn.getChildNodes().getLength(); j++) {
                    Node wn2 = wn.getChildNodes().item(j);
                    if (wn2.getNodeName().equals("hall")) {
                        LocalDate start = null;
                        LocalDate end = null;
                        if (wn2.getAttributes().getNamedItem("start") != null) {
                            start = MHQXMLUtility.parseDate(wn2.getAttributes().getNamedItem("start").getTextContent());
                        }
                        if (wn2.getAttributes().getNamedItem("end") != null) {
                            end = MHQXMLUtility.parseDate(wn2.getAttributes().getNamedItem("end").getTextContent());
                        }
                        hiringHalls.add(new DatedRecord<>(start, end, wn2.getTextContent()));
                    }
                }
            }
        }
    }

    private void loadShipSearchNodeFromXml(Node node) {
        NodeList nl = node.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node wn = nl.item(i);
            switch (wn.getNodeName()) {
                case "shipSearchCost":
                    shipSearchCost = Money.of(Double.parseDouble(wn.getTextContent()));
                    break;
                case "shipSearchLengthWeeks":
                    shipSearchLengthWeeks = Integer.parseInt(wn.getTextContent());
                    break;
                case "target":
                    if (wn.getAttributes().getNamedItem("unitType") != null) {
                        int target = Integer.parseInt(wn.getTextContent());
                        switch (wn.getAttributes().getNamedItem("unitType").getTextContent()) {
                            case "Dropship":
                                dropshipSearchTarget = target;
                                break;
                            case "Jumpship":
                                jumpshipSearchTarget = target;
                                break;
                            case "Warship":
                                warshipSearchTarget = target;
                                break;
                        }
                    }
                    break;
                case "weightedTable":
                    if (wn.getAttributes().getNamedItem("unitType") != null) {
                        WeightedTable<String> map = loadWeightedTableFromXml(wn);
                        switch (wn.getAttributes().getNamedItem("unitType").getTextContent()) {
                            case "Dropship":
                                dsTable = map;
                                break;
                            case "Jumpship":
                                jsTable = map;
                        }
                    }
                    break;
            }
        }
    }

    private WeightedTable<String> loadWeightedTableFromXml(Node node) {
        return loadWeightedTableFromXml(node, Function.identity());
    }

    private <T>WeightedTable<T> loadWeightedTableFromXml(Node node, Function<String,T> fromString) {
        WeightedTable<T> retVal = new WeightedTable<>();
        NodeList nl = node.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node wn = nl.item(i);
            if (wn.getNodeName().equals("entry")) {
                int weight = 1;
                if (wn.getAttributes().getNamedItem("weight") != null) {
                    weight = Integer.parseInt(wn.getAttributes().getNamedItem("weight").getTextContent());
                }
                retVal.add(weight, fromString.apply(wn.getTextContent()));
            }
        }
        return retVal;
    }

    /*
     * Attaches a start and end date to any object.
     * Either the start or end date can be null, indicating that
     * the value should apply to all dates from the beginning
     * or to the end of the epoch, respectively.
     */
    static class DatedRecord<E> {
        private LocalDate start;
        private LocalDate end;
        private E value;

        public DatedRecord(LocalDate start, LocalDate end, E value) {
            this.start = start;
            this.end = end;
            this.value = value;
        }

        public void setStart(LocalDate start) {
            this.start = start;
        }

        public LocalDate getStart() {
            return start;
        }

        public void setEnd(LocalDate end) {
            this.end = end;
        }

        public LocalDate getEnd() {
            return end;
        }

        public void setValue(E v) {
            value = v;
        }

        public E getValue() {
            return value;
        }

        /**
         * @param d date to check
         * @return true if d is between the start and end date, inclusive
         */
        public boolean fitsDate(LocalDate d) {
            return ((start == null) || !start.isAfter(d))
                    && ((end == null) || !end.isBefore(d));
        }
    }

    static class WeightedTable<T> {
        private final List<Integer> weights = new ArrayList<>();
        private final List<T> values = new ArrayList<>();

        public void add(Integer weight, T value) {
            weights.add(weight);
            values.add(value);
        }

        public @Nullable T select() {
            return select(0f);
        }

        /**
         * Select random entry proportionally to the weight values
         * @param rollMod - a modifier to the die roll, expressed as a fraction of the total weight
         * @return
         */
        public @Nullable T select(float rollMod) {
            int total = weights.stream().mapToInt(Integer::intValue).sum();
            if (total > 0) {
                int roll = Math.min(Compute.randomInt(total) + (int)(total * rollMod + 0.5f),
                        total - 1);
                for (int i = 0; i < weights.size(); i++) {
                    if (roll < weights.get(i)) {
                        return values.get(i);
                    }
                    roll -= weights.get(i);
                }
            }
            return null;
        }
    }
}
