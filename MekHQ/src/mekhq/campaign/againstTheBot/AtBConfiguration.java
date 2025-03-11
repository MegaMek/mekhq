/*
 * Copyright (c) 2014 - Carl Spain. All rights reserved.
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.campaign.againstTheBot;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.utilities.MHQXMLUtility;
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
 *         Class that handles configuration options for Against the Bot
 *         campaigns
 *         more extensive than what is handled by CampaignOptions. Most of the
 *         options
 *         fall into one of two categories: they allow users to customize the
 *         various
 *         tables in the rules, or they avoid hard-coding universe details.
 */
public class AtBConfiguration {
    private static final MMLogger logger = MMLogger.create(AtBConfiguration.class);

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

    /* Personnel and unit markets */
    private Money shipSearchCost;
    private int shipSearchLengthWeeks = 4;
    private Integer dropshipSearchTarget;
    private Integer jumpshipSearchTarget;
    private Integer warshipSearchTarget;
    private WeightedTable<String> dsTable;
    private WeightedTable<String> jsTable;

    private final transient ResourceBundle defaultProperties = ResourceBundle.getBundle(
            "mekhq.resources.AtBConfigDefaults",
            MekHQ.getMHQOptions().getLocale());

    private AtBConfiguration() {
        dsTable = new WeightedTable<>();
        jsTable = new WeightedTable<>();
        shipSearchCost = Money.of(100000);
    }

    /**
     * Provide default values in case the file is missing or contains errors.
     */
    private WeightedTable<String> getDefaultForceTable(String key, int index) {
        if (index < 0) {
            logger.error("Default force tables don't support negative weights, limiting to 0");
            index = 0;
        }
        String property = defaultProperties.getString(key);
        String[] fields = property.split("\\|");
        if (index >= fields.length) {
            // Deal with too short field lengths
            logger.error(
                    String.format("Default force tables have %d weight entries; limiting the original value of %d.",
                            fields.length, index));
            index = fields.length - 1;
        }
        return parseDefaultWeightedTable(fields[index]);
    }

    private WeightedTable<String> parseDefaultWeightedTable(String entry) {
        return parseDefaultWeightedTable(entry, Function.identity());
    }

    private <T> WeightedTable<T> parseDefaultWeightedTable(String entry, Function<String, T> fromString) {
        WeightedTable<T> retVal = new WeightedTable<>();
        String[] entries = entry.split(",");
        for (String e : entries) {
            try {
                String[] fields = e.split(":");
                retVal.add(Integer.parseInt(fields[0]), fromString.apply(fields[1]));
            } catch (Exception ex) {
                logger.error("", ex);
            }
        }
        return retVal;
    }

    /**
     * Used if the config file is missing.
     */
    private void setAllValuesToDefaults() {
        for (Enumeration<String> e = defaultProperties.getKeys(); e.hasMoreElements();) {
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
        return switch (wc) {
            case "L", "UL" -> 0;
            case "M" -> 1;
            case "H" -> 2;
            case "A", "C", "SH" -> 3;
            default -> throw new IllegalArgumentException("Could not parse weight class " + wc);
        };
    }

    public @Nullable String selectBotLances(String org, int weightClass) {
        return selectBotLances(org, weightClass, 0f);
    }

    /**
     * Selects a bot lance based on the organization, weight class, and roll
     * modifier.
     *
     * @param org         The organization of the bot force tables.
     * @param weightClass The weight class of the bot.
     * @param rollMod     A modifier to the die roll, expressed as a fraction of the
     *                    total weight.
     * @return The selected bot lance, or null if the organization's bot force
     *         tables are not found or invalid.
     */
    public @Nullable String selectBotLances(String org, int weightClass, float rollMod) {
        // Check if the bot force tables contain the required organization
        if (!botForceTables.containsKey(org)) {
            logger.error(String.format("Bot force tables for organization \"%s\" not found, ignoring", org));
            return null;
        }

        // Retrieve botForceTable for the organization
        final List<WeightedTable<String>> botForceTable = botForceTables.get(org);

        // Weight Class Index
        int weightClassIndex = weightClassIndex(weightClass);

        // Check if the weightClassIndex is within valid range
        if (weightClassIndex < 0 || weightClassIndex >= botForceTable.size()) {
            logger.error(String.format(
                    "Bot force tables for organization \"%s\" don't have an entry for weight class %d, limiting to valid values",
                    org, weightClass));

            // Limit the weightClassIndex within valid range
            weightClassIndex = Math.max(0, Math.min(weightClassIndex, botForceTable.size() - 1));
        }

        // Fetch table for the weight class
        WeightedTable<String> table = botForceTable.get(weightClassIndex);

        // If there isn't relevant table, provide a default one
        if (table == null) {
            table = getDefaultForceTable("botForce." + org, weightClassIndex);
        }

        // Return the selected table
        return table.select(rollMod);
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
     * @param s A String of single-character codes that indicate the weight classes
     *          of the units in a lance (e.g. "LMMH")
     * @param i The index of the code to be translated
     * @return The value used by UnitTableData to find the correct RAT for the
     *         weight class
     */
    public static int decodeWeightStr(String s, int i) {
        return switch (s.charAt(i)) {
            case WEIGHT_ULTRA_LIGHT -> EntityWeightClass.WEIGHT_ULTRA_LIGHT;
            case WEIGHT_LIGHT -> EntityWeightClass.WEIGHT_LIGHT;
            case WEIGHT_MEDIUM -> EntityWeightClass.WEIGHT_MEDIUM;
            case WEIGHT_HEAVY -> EntityWeightClass.WEIGHT_HEAVY;
            case WEIGHT_ASSAULT -> EntityWeightClass.WEIGHT_ASSAULT;
            case WEIGHT_SUPER_HEAVY -> EntityWeightClass.WEIGHT_SUPER_HEAVY;
            default -> 0;
        };
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
        return switch (unitType) {
            case UnitType.DROPSHIP -> dropshipSearchTarget;
            case UnitType.JUMPSHIP -> jumpshipSearchTarget;
            case UnitType.WARSHIP -> warshipSearchTarget;
            default -> null;
        };
    }

    public TargetRoll shipSearchTargetRoll(int unitType, Campaign campaign) {
        final Integer baseShipSearchTarget = shipSearchTargetBase(unitType);
        if (baseShipSearchTarget == null) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, "Base");
        }

        TargetRoll target = new TargetRoll(baseShipSearchTarget, "Base");
        Person adminLog = campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_LOGISTICS, SkillType.S_ADMIN);
        int adminLogExp = (adminLog == null) ? SkillType.EXP_ULTRA_GREEN
                : adminLog.getSkill(SkillType.S_ADMIN).getExperienceLevel();

        target.addModifier(SkillType.EXP_REGULAR - adminLogExp, "Admin/Logistics");
        target.addModifier(IUnitRating.DRAGOON_C - campaign.getAtBUnitRatingMod(),
                "Unit Rating");
        return target;
    }

    public @Nullable MekSummary findShip(int unitType) {
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
        return MekSummaryCache.getInstance().getMek(shipName);
    }

    public static AtBConfiguration loadFromXml() {
        AtBConfiguration retVal = new AtBConfiguration();

        logger.info("Starting load of AtB configuration data from XML...");

        Document xmlDoc;
        try (InputStream is = new FileInputStream("data/universe/atbconfig.xml")) { // TODO : Remove inline file path
            DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

            xmlDoc = db.parse(is);
        } catch (FileNotFoundException ex) {
            logger.info("File data/universe/atbconfig.xml not found. Loading defaults.");
            retVal.setAllValuesToDefaults();
            return retVal;
        } catch (Exception ex) {
            logger.error("Error parsing file data/universe/atbconfig.xml. Loading defaults.", ex);
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
                    logger.error("Could not parse weight class attribute for enemy forces table", ex);
                }
            }
        }
        return retVal;
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

    private <T> WeightedTable<T> loadWeightedTableFromXml(Node node, Function<String, T> fromString) {
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
         *
         * @param rollMod - a modifier to the die roll, expressed as a fraction of the
         *                total weight
         * @return
         */
        public @Nullable T select(float rollMod) {
            int total = weights.stream().mapToInt(Integer::intValue).sum();
            if (total > 0) {
                int roll = Math.min(Compute.randomInt(total) + (int) (total * rollMod + 0.5f),
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
