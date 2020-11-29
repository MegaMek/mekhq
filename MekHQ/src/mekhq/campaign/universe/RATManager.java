/*
 * RATManager.java
 *
 * Copyright (c) 2016 Carl Spain. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.universe;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.client.generator.RandomUnitGenerator;
import megamek.common.Compute;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.MechSummary;
import megamek.common.UnitType;
import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.event.OptionsChangedEvent;

/**
 * Provides a front end to RandomUnitGenerator that allows the user to generate units
 * based on criteria such as faction, unit type, and weight class. May be restricted to
 * a certain subset of all available RATs.
 *
 * @author Neoancient
 *
 */
public class RATManager extends AbstractUnitGenerator implements IUnitGenerator {

    private static final String ALT_FACTION = "data/universe/altfactions.xml";
    private static final String RATINFO_DIR = "data/universe/ratdata";
    // allRATs.get(collectionName).get(era); eras are sorted from latest to earliest
    private Map<String,LinkedHashMap<Integer,List<RAT>>> allRATs;
    private ArrayList<String> selectedCollections;
    private Map<String,List<String>> altFactions;

    private static Map<String,List<Integer>> allCollections = null;
    private static Map<String,String> fileNames = new HashMap<>();

    private boolean canIgnoreEra = false;

    public RATManager() {
        allRATs = new HashMap<>();
        selectedCollections = new ArrayList<>();
        loadAltFactions();
        MekHQ.registerHandler(this);
    }

    @Subscribe
    public void updateRATconfig(OptionsChangedEvent ev) {
        canIgnoreEra = ev.getOptions().canIgnoreRatEra();
        setSelectedRATs(ev.getOptions().getRATs());
    }

    /**
     * Replaces selected RAT collections with new list
     * @param selected List of RAT collection names
     */
    public void setSelectedRATs(List<String> selected) {
        selectedCollections.clear();
        for (String col : selected) {
            addRAT(col);
        }
    }

    /**
     * Replaces selected RAT collections with new list
     * @param selected Array of RAT collection names
     */
    public void setSelectedRATs(String[] selected) {
        selectedCollections.clear();
        for (String col : selected) {
            addRAT(col);
        }
    }

    /**
     * Append RAT collection to list of selected RATs
     * @param collection Name of RAT collection to add
     */
    private void addRAT(String collection) {
        if (allRATs.containsKey(collection) || loadCollection(collection)) {
            selectedCollections.add(collection);
        }
    }

    /**
     * Remove RAT collection from list of selected RATs
     * @param collection Name of RAT collection to remove
     */
    public void removeRAT(String collection) {
        selectedCollections.remove(collection);
    }

    public void setIgnoreRatEra(boolean ignore) {
        canIgnoreEra = ignore;
    }

    private boolean loadCollection(String name) {
        if (!fileNames.containsKey(name)) {
            MekHQ.getLogger().error(this, "RAT collection " + name + " not found in " + RATINFO_DIR);
            return false;
        }
        /* Need RUG to be loaded for validation */
        while (!RandomUnitGenerator.getInstance().isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                MekHQ.getLogger().error(this, e);
            }
        }
        File f = new File(RATINFO_DIR, fileNames.get(name));

        Document xmlDoc;
        DocumentBuilder db;

        try (FileInputStream fis = new FileInputStream(f)) {
            db = MekHqXmlUtil.newSafeDocumentBuilder();
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            MekHQ.getLogger().error(this, "While loading RAT info from " + f.getName() + ": ", ex);
            return false;
        }

        Element elem = xmlDoc.getDocumentElement();
        NodeList nl = elem.getChildNodes();
        elem.normalize();

        if (elem.getAttributes().getNamedItem("source") != null) {
            name = elem.getAttributes().getNamedItem("source").getTextContent();
            allRATs.put(name, new LinkedHashMap<>());
            List<Integer> eras = allCollections.get(name);
            for (int e = eras.size() - 1; e >= 0; e--) {
                allRATs.get(name).put(eras.get(e), new ArrayList<>());
            }
            for (int i = 0; i < nl.getLength(); i++) {
                Node eraNode = nl.item(i);
                if (eraNode.getNodeName().equalsIgnoreCase("era")) {
                    String year = eraNode.getAttributes().getNamedItem("year").getTextContent();
                    if (year != null) {
                        try {
                            int era = Integer.parseInt(year);
                            allRATs.get(name).put(era, new ArrayList<>());
                            parseEraNode(eraNode, name, era);
                        } catch (NumberFormatException ex) {
                            MekHQ.getLogger().error(this, "Could not parse year " + year + " in " + name);
                        }
                    } else {
                        MekHQ.getLogger().error(this, "year attribute not found for era in RAT collection " + name);
                    }
                }
            }
            return allRATs.get(name).size() > 0;
        } else {
            MekHQ.getLogger().error(this, "source attribute not found for RAT data in " + f.getName());
            return false;
        }
    }

    private void parseEraNode(Node eraNode, String name, int era) {
        Set<String> allRatNames = RandomUnitGenerator.getInstance().getRatMap().keySet();
        NodeList nl = eraNode.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node ratNode = nl.item(i);
            if (ratNode.getNodeName().equals("rat")) {
                RAT rat = RAT.createFromXml(ratNode);
                if (rat != null && allRatNames.contains(rat.ratName)) {
                    allRATs.get(name).get(era).add(rat);
                }
            }
        }
    }

    /**
     * Loads a list of alternate factions to check when the desired one cannot be found in a given
     * RAT before checking generic ones.
     */
    private void loadAltFactions() {
        altFactions = new HashMap<>();

        File f = new File(ALT_FACTION);

        Document xmlDoc;
        DocumentBuilder db;

        try (FileInputStream fis = new FileInputStream(f)) {
            db = MekHqXmlUtil.newSafeDocumentBuilder();
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            MekHQ.getLogger().error(this, "While loading altFactions: ", ex);
            return;
        }

        Element elem = xmlDoc.getDocumentElement();
        NodeList nl = elem.getChildNodes();
        elem.normalize();

        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node.getNodeName().equals("faction") && node.getAttributes().getNamedItem("key") != null) {
                String key = node.getAttributes().getNamedItem("key").getTextContent();
                altFactions.putIfAbsent(key, new ArrayList<>());
                for (String alt : node.getTextContent().split(",")) {
                    altFactions.get(key).add(alt);
                }
            }
        }
    }

    /**
     *
     * @return A map of all collections available with a list of eras included
     */
    public static Map<String,List<Integer>> getAllRATCollections() {
        if (allCollections == null) {
            populateCollectionNames();
        }
        return allCollections;
    }

    /**
     * Scans ratdata directory for list of available RATs that can be used by CampaignOptions
     * to provide a list.
     */
    public static void populateCollectionNames() {
        allCollections = new HashMap<>();

        Document xmlDoc;
        DocumentBuilder db;

        File dir = new File(RATINFO_DIR);
        FileInputStream fis;

        if (!dir.isDirectory()) {
            MekHQ.getLogger().error(RATManager.class, "Ratinfo directory not found");
            return;
        }
        for (File f : dir.listFiles()) {
            if (f.getName().endsWith(".xml")) {
                try {
                    fis = new FileInputStream(f);
                    db = MekHqXmlUtil.newSafeDocumentBuilder();
                    xmlDoc = db.parse(fis);
                    fis.close();
                } catch (Exception ex) {
                    MekHQ.getLogger().error(RATManager.class, "While loading RAT info from " + f.getName() + ": ");
                    MekHQ.getLogger().error(RATManager.class, ex);
                    continue;
                }
                Element elem = xmlDoc.getDocumentElement();
                NodeList nl = elem.getChildNodes();
                elem.normalize();

                String name;
                List<Integer> eras = new ArrayList<>();

                if (elem.getAttributes().getNamedItem("source") != null) {
                    name = elem.getAttributes().getNamedItem("source").getTextContent();
                    for (int j = 0; j < nl.getLength(); j++) {
                        Node eraNode = nl.item(j);
                        if (eraNode.getNodeName().equalsIgnoreCase("era")) {
                            String year = eraNode.getAttributes().getNamedItem("year").getTextContent();
                            if (year != null) {
                                try {
                                    eras.add(Integer.parseInt(year));
                                } catch (NumberFormatException ex) {
                                    MekHQ.getLogger().error(RATManager.class, "Could not parse year " + year + " in " + f.getName());
                                }
                            } else {
                                MekHQ.getLogger().error(RATManager.class, "year attribute not found for era in " + f.getName());
                            }
                        }
                    }
                    fileNames.put(name, f.getName());
                    Collections.sort(eras);
                    allCollections.put(name, eras);
                } else {
                    MekHQ.getLogger().error(RATManager.class, "source attribute not found for RAT data in " + f.getName());
                }
            }
        }
    }

    private RAT findRAT(String faction, int unitType, int weightClass, int year, int quality) {
        List<String> factionList = factionTree(faction);
        for (String collectionName : selectedCollections) {
            Map<Integer,List<RAT>> collection = allRATs.get(collectionName);
            if (collection == null) {
                continue;
            }
            for (int era : collection.keySet()) {
                if (era > year) {
                    continue;
                }
                for (String f : factionList) {
                    Optional<RAT> match = collection.get(era).stream()
                            .filter(rat -> rat.matches(f, unitType, weightClass, quality))
                            .findFirst();
                    if (match.isPresent()) {
                        return match.get();
                    }
                }
            }
        }
        if (canIgnoreEra) {
            for (String collectionName : selectedCollections) {
                Map<Integer,List<RAT>> collection = allRATs.get(collectionName);
                if (collection == null) {
                    continue;
                }
                List<Integer> eras = new ArrayList<>(collection.keySet());
                Collections.reverse(eras);

                for (int era : eras) {
                    for (String f : factionList) {
                        Optional<RAT> match = collection.get(era).stream()
                                .filter(rat -> rat.matches(f, unitType, weightClass, quality))
                                .findFirst();
                        if (match.isPresent()) {
                            return match.get();
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<String> factionTree(String faction) {
        List<String> retVal = new ArrayList<>();
        retVal.add(faction);
        if (faction.contains(".")) {
            faction = faction.split("\\.")[0];
            retVal.add(faction);
        }
        if (altFactions.containsKey(faction)) {
            List<String> alts = new ArrayList<>(altFactions.get(faction));
            while (alts.size() > 0) {
                int index = Compute.randomInt(alts.size());
                retVal.add(alts.get(index));
                alts.remove(index);
            }
        }
        Faction f = Faction.getFaction(faction);
        if (f.isPeriphery()) {
            retVal.add("Periphery");
        }
        retVal.add(f.isClan()? "Clan" : "General");
        return retVal;
    }

    /* (non-Javadoc)
     * @see mekhq.campaign.universe.IUnitGenerator#isSupportedUnitType(int)
     */
    @Override
    public boolean isSupportedUnitType(int unitType) {
        return unitType == UnitType.MEK
                || unitType == UnitType.TANK
                || unitType == UnitType.AERO
                || unitType == UnitType.DROPSHIP
                || unitType == UnitType.INFANTRY
                || unitType == UnitType.BATTLE_ARMOR
                || unitType == UnitType.PROTOMEK;
    }

    /* (non-Javadoc)
     * @see mekhq.campaign.universe.IUnitGenerator#generate(java.lang.String, int, int, int, int)
     */
    @Override
    public MechSummary generate(String faction, int unitType, int weightClass, int year, int quality) {
        return generate(faction, unitType, weightClass, year, quality, null);
    }

    /* (non-Javadoc)
     * @see mekhq.campaign.universe.IUnitGenerator#generate(java.lang.String, int, int, int, int, java.util.function.Predicate)
     */
    @Override
    public MechSummary generate(String faction, int unitType, int weightClass, int year, int quality, Predicate<MechSummary> filter) {
        List<MechSummary> list = generate(1, faction, unitType, weightClass, year, quality, filter);
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    @Override
    public MechSummary generate(String faction, int unitType, int weightClass, int year, int quality, Collection<EntityMovementMode> movementModes, Predicate<MechSummary> filter) {
        List<MechSummary> list = generate(1, faction, unitType, weightClass, year, quality, movementModes, filter);
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see mekhq.campaign.universe.IUnitGenerator#generate(int, java.lang.String, int, int, int, int)
     */
    @Override
    public List<MechSummary> generate(int count, String faction, int unitType, int weightClass, int year, int quality) {
        return generate(count, faction, unitType, weightClass, year, quality, null);
    }

    /* (non-Javadoc)
     * @see mekhq.campaign.universe.IUnitGenerator#generate(int, java.lang.String, int, int, int, int, java.util.function.Predicate)
     */
    @Override
    public List<MechSummary> generate(int count, String faction, int unitType, int weightClass, int year, int quality, Predicate<MechSummary> filter) {
        RAT rat = findRAT(faction, unitType, weightClass, year, quality);
        if (rat != null) {
            if (unitType == UnitType.TANK) {
                filter = filter != null ? filter.and(RATManager::isTank) : RATManager::isTank;
            } else if (unitType == UnitType.VTOL) {
                filter = filter != null ? filter.and(RATManager::isVTOL) : RATManager::isVTOL;
            }
            return RandomUnitGenerator.getInstance().generate(count, rat.ratName, filter);
        }
        return new ArrayList<>();
    }

    private static boolean isTank(MechSummary summary) {
        return summary.getUnitType().equals("Tank");
    }

    private static boolean isVTOL(MechSummary summary) {
        return summary.getUnitType().equals("VTOL");
    }

    @Override
    public List<MechSummary> generate(int count, String faction, int unitType,
            int weightClass, int year, int quality, Collection<EntityMovementMode> movementModes,
            Predicate<MechSummary> filter) {
        RAT rat = findRAT(faction, unitType, weightClass, year, quality);
        if (rat != null) {
            if (!movementModes.isEmpty()) {
                Predicate<MechSummary> moveFilter = ms ->
                    movementModes.contains(EntityMovementMode.getMode(ms.getUnitSubType()));
                if (filter == null) {
                    filter = moveFilter;
                } else {
                    filter = filter.and(moveFilter);
                }
            }
            return RandomUnitGenerator.getInstance().generate(count, rat.ratName, filter);
        }
        return new ArrayList<MechSummary>();
    }

    /**
     * Generates a list of mech summaries from a RAT determined by the given faction, quality and other parameters.
     * Note that for the purposes of this implementation, the only properties of "parameters" used are
     * unit type, year, weight classes and movement modes. We also expect the rating to be a number 1-5, rather than A-F.
     * @param count How many units to generate
     * @param parameters RATGenerator parameters (some are ignored)
     */
    @Override
    public List<MechSummary> generate(int count, UnitGeneratorParameters parameters) {
        return generate(count, parameters.getFaction(), parameters.getUnitType(), parameters.getWeightClass(),
                parameters.getYear(), parameters.getQuality(), parameters.getMovementModes(), parameters.getFilter());
    }

    /**
     * Generates a single unit, for the given faction, using the given set of parameters.
     * Note that some of the properties of the parameters may be ignored for generation mechanisms that aren't the RAT Generator
     * @param parameters data structure containing unit generation parameters
     * @return Generated units. Null if none generated.
     */
    @Override
    public MechSummary generate(UnitGeneratorParameters parameters) {
        return generate(parameters.getFaction(), parameters.getUnitType(), parameters.getWeightClass(),
                parameters.getYear(), parameters.getQuality(), parameters.getMovementModes(), parameters.getFilter());
    }

    private static class RAT {
        String ratName = null;
        HashSet<String> factions = new HashSet<>();
        HashSet<Integer> unitTypes = new HashSet<>();
        HashSet<Integer> weightClasses = new HashSet<>();
        HashSet<Integer> ratings = new HashSet<>();

        public boolean matches(String faction, int unitType, int weightClass, int quality) {
            return (factions.contains(faction) || factions.isEmpty())
                    && (unitTypes.contains(unitType) || unitTypes.isEmpty())
                    && (weightClasses.contains(weightClass) || weightClasses.isEmpty() || weightClass < 0)
                    && (ratings.contains(quality) || ratings.isEmpty());
        }

        public static RAT createFromXml(Node node) {
            RAT retVal = new RAT();
            if (node.getAttributes().getNamedItem("name") == null) {
                MekHQ.getLogger().error(RATManager.class, "name attribute missing");
                return null;
            }
            retVal.ratName = node.getAttributes().getNamedItem("name").getTextContent();
            NodeList nl = node.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node wn = nl.item(i);
                switch (wn.getNodeName()) {
                case "factions":
                    if (wn.getTextContent().length() > 0) {
                        retVal.factions.addAll(Arrays.asList(wn.getTextContent().split(",")));
                    }
                    break;
                case "unitTypes":
                    for (String ut : wn.getTextContent().split(",")) {
                        switch(ut) {
                        case "Mek":
                            retVal.unitTypes.add(UnitType.MEK);
                            break;
                        case "Tank":
                            retVal.unitTypes.add(UnitType.TANK);
                            break;
                        case "BattleArmor":
                            retVal.unitTypes.add(UnitType.BATTLE_ARMOR);
                            break;
                        case "Infantry":
                            retVal.unitTypes.add(UnitType.INFANTRY);
                            break;
                        case "ProtoMek":
                            retVal.unitTypes.add(UnitType.PROTOMEK);
                            break;
                        case "VTOL":
                            retVal.unitTypes.add(UnitType.VTOL);
                            break;
                        case "Naval":
                            retVal.unitTypes.add(UnitType.NAVAL);
                            break;
                        case "Gun Emplacement":
                            retVal.unitTypes.add(UnitType.GUN_EMPLACEMENT);
                            break;
                        case "Conventional Fighter":
                            retVal.unitTypes.add(UnitType.CONV_FIGHTER);
                            break;
                        case "Aero":
                            retVal.unitTypes.add(UnitType.AERO);
                            break;
                        case "Small Craft":
                            retVal.unitTypes.add(UnitType.SMALL_CRAFT);
                            break;
                        case "Dropship":
                            retVal.unitTypes.add(UnitType.DROPSHIP);
                            break;
                        case "Jumpship":
                            retVal.unitTypes.add(UnitType.JUMPSHIP);
                            break;
                        case "Warship":
                            retVal.unitTypes.add(UnitType.WARSHIP);
                            break;
                        case "Space Station":
                            retVal.unitTypes.add(UnitType.SPACE_STATION);
                            break;
                        }
                    }
                    break;
                case "weightClasses":
                    for (String wc : wn.getTextContent().split(",")) {
                        switch(wc) {
                        case "UL":
                            retVal.weightClasses.add(EntityWeightClass.WEIGHT_ULTRA_LIGHT);
                            break;
                        case "L":
                            retVal.weightClasses.add(EntityWeightClass.WEIGHT_LIGHT);
                            break;
                        case "M":
                            retVal.weightClasses.add(EntityWeightClass.WEIGHT_MEDIUM);
                            break;
                        case "H":
                            retVal.weightClasses.add(EntityWeightClass.WEIGHT_HEAVY);
                            break;
                        case "A":
                            retVal.weightClasses.add(EntityWeightClass.WEIGHT_ASSAULT);
                            break;
                        case "SH":
                        case "C":
                            retVal.weightClasses.add(EntityWeightClass.WEIGHT_SUPER_HEAVY);
                            break;
                        }
                    }
                    break;
                case "ratings":
                    for (String r : wn.getTextContent().split(",")) {
                        switch(r) {
                        case "A":
                        case "Keshik":
                        case "K":
                            retVal.ratings.add(4);
                            break;
                        case "B":
                        case "FL":
                            retVal.ratings.add(3);
                            break;
                        case "C":
                        case "SL":
                        case "2L":
                            retVal.ratings.add(2);
                            break;
                        case "D":
                        case "Sol":
                        case "Solahma":
                            retVal.ratings.add(1);
                            break;
                        case "F":
                        case "PG":
                        case "PGC":
                            retVal.ratings.add(0);
                            break;
                        }
                    }
                    break;
                }
            }
            return retVal;
        }
    }
}
