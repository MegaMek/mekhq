/*
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.io;

import megamek.Version;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.Entity;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoMounted;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;
import megamek.common.weapons.bayweapons.BayWeapon;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.Utilities;
import mekhq.campaign.*;
import mekhq.campaign.againstTheBot.AtBConfiguration;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.icons.UnitIcon;
import mekhq.campaign.market.ContractMarket;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.ShoppingList;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mod.am.InjuryTypes;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.equipment.*;
import mekhq.campaign.personnel.*;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.cleanup.EquipmentUnscrambler;
import mekhq.campaign.unit.cleanup.EquipmentUnscramblerResult;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planet.PlanetaryEvent;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.io.idReferenceClasses.PersonIdReference;
import mekhq.io.migration.*;
import mekhq.module.atb.AtBEventProcessor;
import mekhq.utilities.MHQXMLUtility;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import java.io.*;
import java.util.*;

public class CampaignXmlParser {
    private InputStream is;
    private MekHQ app;

    public CampaignXmlParser(InputStream is, MekHQ app) {
        this.is = is;
        this.app = app;
    }

    public void close() throws IOException {
        this.is.close();
    }

    /**
     * Designed to create a campaign object from an input stream containing an XML structure.
     *
     * @return The created Campaign object, or null if there was a problem.
     * @throws CampaignXmlParseException Thrown when there was a problem parsing the CPNX file
     * @throws NullEntityException Thrown when an entity is referenced but cannot be loaded or found
     */
    public Campaign parse() throws CampaignXmlParseException, NullEntityException {
        LogManager.getLogger().info("Starting load of campaign file from XML...");
        // Initialize variables.
        Campaign retVal = new Campaign();
        retVal.setApp(app);

        Document xmlDoc;

        try {
            // Using factory get an instance of document builder
            DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(is);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            throw new CampaignXmlParseException(ex);
        }

        Element campaignEle = xmlDoc.getDocumentElement();
        NodeList nl = campaignEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML. At least this cleans it up.
        campaignEle.normalize();

        final Version version = new Version(campaignEle.getAttribute("version"));
        if (version.is("0.0.0")) {
            throw new CampaignXmlParseException(String.format("Illegal version of %s failed to parse",
                    campaignEle.getAttribute("version")));
        }

        // Indicates whether or not new units were written to disk while
        // loading the Campaign file. If so, we need to kick back off loading
        // all of the unit data from disk.
        boolean reloadUnitData = false;

        // we need to iterate through three times, the first time to collect
        // any custom units that might not be written yet
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            if (!wn.getParentNode().equals(campaignEle)) {
                continue;
            }

            int xc = wn.getNodeType();

            if (xc == Node.ELEMENT_NODE) {
                // This is what we really care about.
                // All the meat of our document is in this node type, at this
                // level.
                // Okay, so what element is it?
                String xn = wn.getNodeName();

                if (xn.equalsIgnoreCase("info")) { // This is needed so that the campaign name gets set in retVal
                    try {
                        processInfoNode(retVal, wn, version);
                    } catch (DOMException e) {
                        throw new CampaignXmlParseException(e);
                    }
                } else if (xn.equalsIgnoreCase("custom")) {
                    reloadUnitData |= processCustom(retVal, wn);
                }
            } else {
                // If it's a text node or attribute or whatever at this level,
                // it's probably white-space.
                // We can safely ignore it even if it isn't, for now.
                continue;
            }
        }

        // Only reload unit data if we updated files on disk
        if (reloadUnitData) {
            MechSummaryCache.getInstance().loadMechData();
        }

        // the second time to check for any null entities
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            if (!wn.getParentNode().equals(campaignEle)) {
                continue;
            }

            int xc = wn.getNodeType();

            if (xc == Node.ELEMENT_NODE) {
                // This is what we really care about.
                // All the meat of our document is in this node type, at this
                // level.
                // Okay, so what element is it?
                String xn = wn.getNodeName();

                if (xn.equalsIgnoreCase("units")) {
                    String missingList = checkUnits(wn);
                    if (null != missingList) {
                        throw new NullEntityException(missingList);
                    }
                }
            } else {
                // If it's a text node or attribute or whatever at this level,
                // it's probably white-space.
                // We can safely ignore it even if it isn't, for now.
                continue;
            }
        }

        boolean foundPersonnelMarket = false;
        boolean foundContractMarket = false;
        boolean foundUnitMarket = false;

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            if (!wn.getParentNode().equals(campaignEle)) {
                continue;
            }

            int xc = wn.getNodeType();

            if (xc == Node.ELEMENT_NODE) {
                // This is what we really care about.
                // All the meat of our document is in this node type, at this
                // level.
                // Okay, so what element is it?
                String xn = wn.getNodeName();

                if (xn.equalsIgnoreCase("campaignOptions")) {
                    retVal.setCampaignOptions(CampaignOptions.generateCampaignOptionsFromXml(wn, version));
                } else if (xn.equalsIgnoreCase("randomSkillPreferences")) {
                    retVal.setRandomSkillPreferences(RandomSkillPreferences.generateRandomSkillPreferencesFromXml(wn, version));
                } else if (xn.equalsIgnoreCase("parts")) {
                    processPartNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("personnel")) {
                    // TODO: Make this depending on campaign options
                    // TODO: hoist registerAll out of this
                    InjuryTypes.registerAll();
                    processPersonnelNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("ancestors")) { // Legacy
                    migrateAncestorNodes(retVal, wn);
                } else if (xn.equalsIgnoreCase("units")) {
                    processUnitNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("missions")) {
                    processMissionNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("forces")) {
                    processForces(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("finances")) {
                    processFinances(retVal, wn);
                } else if (xn.equalsIgnoreCase("location")) {
                    retVal.setLocation(CurrentLocation.generateInstanceFromXML(wn, retVal));
                } else if (xn.equalsIgnoreCase("skillTypes")) {
                    processSkillTypeNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("specialAbilities")) {
                    processSpecialAbilityNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("storyArc")) {
                    processStoryArcNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("gameOptions")) {
                    retVal.getGameOptions().fillFromXML(wn.getChildNodes());
                } else if (xn.equalsIgnoreCase("kills")) {
                    processKillNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("shoppingList")) {
                    retVal.setShoppingList(ShoppingList.generateInstanceFromXML(wn, retVal, version));
                } else if (xn.equalsIgnoreCase("personnelMarket")) {
                    retVal.setPersonnelMarket(PersonnelMarket.generateInstanceFromXML(wn, retVal, version));
                    foundPersonnelMarket = true;
                } else if (xn.equalsIgnoreCase("contractMarket")) {
                    // CAW: implicit DEPENDS-ON to the <missions> node
                    retVal.setContractMarket(ContractMarket.generateInstanceFromXML(wn, retVal, version));
                    foundContractMarket = true;
                } else if (xn.equalsIgnoreCase("unitMarket")) {
                    // Windchild: implicit DEPENDS ON to the <campaignOptions> nodes
                    retVal.setUnitMarket(retVal.getCampaignOptions().getUnitMarketMethod().getUnitMarket());
                    retVal.getUnitMarket().fillFromXML(wn, retVal, version);
                    foundUnitMarket = true;
                } else if (xn.equalsIgnoreCase("lances")) {
                    processLanceNodes(retVal, wn);
                } else if (xn.equalsIgnoreCase("retirementDefectionTracker")) {
                    retVal.setRetirementDefectionTracker(RetirementDefectionTracker.generateInstanceFromXML(wn, retVal));
                } else if (xn.equalsIgnoreCase("shipSearchStart")) {
                    retVal.setShipSearchStart(MHQXMLUtility.parseDate(wn.getTextContent().trim()));
                } else if (xn.equalsIgnoreCase("shipSearchType")) {
                    retVal.setShipSearchType(Integer.parseInt(wn.getTextContent()));
                } else if (xn.equalsIgnoreCase("shipSearchResult")) {
                    retVal.setShipSearchResult(wn.getTextContent());
                } else if (xn.equalsIgnoreCase("shipSearchExpiration")) {
                    retVal.setShipSearchExpiration(MHQXMLUtility.parseDate(wn.getTextContent().trim()));
                } else if (xn.equalsIgnoreCase("customPlanetaryEvents")) {
                    updatePlanetaryEventsFromXML(wn);
                }
            } else {
                // If it's a text node or attribute or whatever at this level,
                // it's probably white-space.
                // We can safely ignore it even if it isn't, for now.
            }
        }

        // Okay, after we've gone through all the nodes and constructed the
        // Campaign object...
        final CampaignOptions options = retVal.getCampaignOptions();

        // Apply Migration
        if (version.isLowerThan("0.49.3")) {
            CamouflageMigrator.migrateCamouflage(version, retVal.getCamouflage());
        }

        if (version.isLowerThan("0.49.6")) {
            retVal.setUnitIcon(ForceIconMigrator.migrateForceIconToKailans(retVal.getUnitIcon()));
        } else if (version.isLowerThan("0.49.7")) {
            retVal.setUnitIcon(ForceIconMigrator.migrateForceIcon0496To0497(retVal.getUnitIcon()));
        }

        if (version.isLowerThan("0.49.7")) {
            FactionMigrator.migrateFactionCode(retVal);
        }

        GameOptionsMigrator.migrate(version, retVal.getGameOptions());

        // We need to do a post-process pass to restore a number of references.
        // Fix any Person Id References
        PersonIdReference.fixPersonIdReferences(retVal);

        // Fixup any ghost kills
        cleanupGhostKills(retVal);

        // Update the Personnel Modules
        retVal.setDeath(options.getRandomDeathMethod().getMethod(options));
        retVal.setDivorce(options.getRandomDivorceMethod().getMethod(options));
        retVal.setMarriage(options.getRandomMarriageMethod().getMethod(options));
        retVal.setProcreation(options.getRandomProcreationMethod().getMethod(options));

        long timestamp = System.currentTimeMillis();

        // loop through forces to set force id
        for (Force f : retVal.getAllForces()) {
            Scenario s = retVal.getScenario(f.getScenarioId());
            if (null != s
                    && (null == f.getParentForce() || !f.getParentForce().isDeployed())) {
                s.addForces(f.getId());
            }
            // some units may need force id set for backwards compatibility
            // some may also need scenario id set
            for (UUID uid : f.getUnits()) {
                Unit u = retVal.getUnit(uid);
                if (null != u) {
                    u.setForceId(f.getId());
                    if (f.isDeployed()) {
                        u.setScenarioId(f.getScenarioId());
                    }
                }
            }
        }

        // determine if we've missed any lances and add those back into the campaign
        if (options.isUseAtB()) {
            Hashtable<Integer, Lance> lances = retVal.getLances();
            for (Force f : retVal.getAllForces()) {
                if (!f.getUnits().isEmpty() && (null == lances.get(f.getId()))) {
                    lances.put(f.getId(), new Lance(f.getId(), retVal));
                    LogManager.getLogger().warn(String.format("Added missing Lance %s to AtB list", f.getName()));
                }
            }
        }

        LogManager.getLogger().info(String.format("[Campaign Load] Force IDs set in %dms",
                System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        // Process parts...
        postProcessParts(retVal, version);

        LogManager.getLogger().info(String.format("[Campaign Load] Parts processed in %dms",
                System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        for (Person psn : retVal.getPersonnel()) {
            // skill types might need resetting
            psn.resetSkillTypes();
        }

        LogManager.getLogger().info(String.format("[Campaign Load] Rank references fixed in %dms",
                System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        // Okay, Units, need their pilot references fixed.
        retVal.getHangar().forEachUnit(unit -> {
            // Also, the unit should have its campaign set.
            unit.setCampaign(retVal);
            unit.fixReferences(retVal);

            // reset the pilot and entity, to reflect newly assigned personnel
            unit.resetPilotAndEntity();

            if (null != unit.getRefit()) {
                unit.getRefit().fixReferences(retVal);

                unit.getRefit().reCalc();
                if (!unit.getRefit().isCustomJob() && !unit.getRefit().kitFound()) {
                    retVal.getShoppingList().addShoppingItemWithoutChecking(unit.getRefit());
                }
            }

            // lets make sure the force id set actually corresponds to a force
            // TODO: we have some reports of force id relics - need to fix
            if ((unit.getForceId() > 0) && (retVal.getForce(unit.getForceId()) == null)) {
                unit.setForceId(-1);
            }

            // It's annoying to have to do this, but this helps to ensure
            // that equipment numbers correspond to the right parts - its
            // possible that these might have changed if changes were made to
            // the ordering of equipment in the underlying data file for the unit.
            // We're not checking for refit here.
            final EquipmentUnscrambler unscrambler = EquipmentUnscrambler.create(unit);
            final EquipmentUnscramblerResult result = unscrambler.unscramble();
            if (!result.succeeded()) {
                LogManager.getLogger().warn(result.getMessage());
            }

            // some units might need to be assigned to scenarios
            Scenario s = retVal.getScenario(unit.getScenarioId());
            if (null != s) {
                // most units will be properly assigned through their
                // force, so check to make sure they aren't already here
                if (!s.isAssigned(unit, retVal)) {
                    s.addUnit(unit.getId());
                }
            }
        });

        LogManager.getLogger().info(String.format("[Campaign Load] Pilot references fixed in %dms",
                System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        retVal.getHangar().forEachUnit(unit -> {
            // Some units have been incorrectly assigned a null C3UUID as a string. This should
            // correct that by setting a new C3UUID
            if ((unit.getEntity().hasC3() || unit.getEntity().hasC3i() || unit.getEntity().hasNavalC3())
                    && (unit.getEntity().getC3UUIDAsString() == null || unit.getEntity().getC3UUIDAsString().equals("null"))) {
                unit.getEntity().setC3UUID();
                unit.getEntity().setC3NetIdSelf();
            }
        });
        retVal.refreshNetworks();

        LogManager.getLogger().info(String.format("[Campaign Load] C3 networks refreshed in %dms",
                System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        // ok, once we are sure that campaign has been set for all units, we can
        // now go through and initializeParts and run diagnostics
        List<Unit> removeUnits = new ArrayList<>();
        retVal.getHangar().forEachUnit(unit -> {
            // just in case parts are missing (i.e. because they weren't tracked
            // in previous versions)
            unit.initializeParts(true);
            unit.runDiagnostic(false);
            if (!unit.isRepairable()) {
                if (!unit.hasSalvageableParts()) {
                    // we shouldnt get here but some units seem to stick around
                    // for some reason
                    removeUnits.add(unit);
                } else {
                    unit.setSalvage(true);
                }
            }
        });

        for (Unit unit : removeUnits) {
            retVal.removeUnit(unit.getId());
        }

        LogManager.getLogger().info(String.format("[Campaign Load] Units initialized in %dms",
                System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        for (Person person : retVal.getPersonnel()) {
            person.fixReferences(retVal);
        }

        LogManager.getLogger().info(String.format("[Campaign Load] Personnel initialized in %dms",
                System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        retVal.reloadNews();

        LogManager.getLogger().info(String.format("[Campaign Load] News loaded in %dms",
                System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        // If we don't have a personnel market, create one.
        if (!foundPersonnelMarket) {
            retVal.setPersonnelMarket(new PersonnelMarket(retVal));
        }

        if (!foundContractMarket) {
            retVal.setContractMarket(new ContractMarket());
        }

        if (!foundUnitMarket) {
            retVal.setUnitMarket(retVal.getCampaignOptions().getUnitMarketMethod().getUnitMarket());
        }

        if (null == retVal.getRetirementDefectionTracker()) {
            retVal.setRetirementDefectionTracker(new RetirementDefectionTracker());
        }

        if (retVal.getCampaignOptions().isUseAtB()) {
            retVal.setHasActiveContract();
            retVal.setAtBConfig(AtBConfiguration.loadFromXml());
            retVal.setAtBEventProcessor(new AtBEventProcessor(retVal));
        }

        // Load Completed. Time for final migration and sanity checks.
        // Final migration
        PersonMigrator.finalPersonMigration(version, retVal.getPersonnel());

        // Sanity Checks
        fixupUnitTechProblems(retVal);

        // unload any ammo bins in the warehouse
        List<AmmoBin> binsToUnload = new ArrayList<>();
        retVal.getWarehouse().forEachSparePart(prt -> {
            if (prt instanceof AmmoBin && !prt.isReservedForRefit() && ((AmmoBin) prt).getShotsNeeded() == 0) {
                binsToUnload.add((AmmoBin) prt);
            }
        });
        for (AmmoBin bin : binsToUnload) {
            bin.unload();
        }

        LogManager.getLogger().info(String.format("[Campaign Load] Ammo bins cleared in %dms",
                System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();


        // Check all parts that are reserved for refit and if the refit id unit
        // is not refitting or is gone then unreserve
        for (Part part : retVal.getWarehouse().getParts()) {
            if (part.isReservedForRefit()) {
                Unit u = part.getRefitUnit();
                if ((null == u) || !u.isRefitting()) {
                    part.setRefitUnit(null);
                }
            }
        }

        LogManager.getLogger().info(String.format("[Campaign Load] Reserved refit parts fixed in %dms",
                System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        // Build a new, clean warehouse from the current parts
        Warehouse warehouse = new Warehouse();
        for (Part part : retVal.getWarehouse().getParts()) {
            warehouse.addPart(part, true);
        }

        // This will have aggregated all of the possible spare parts together
        retVal.setWarehouse(warehouse);

        LogManager.getLogger().info(String.format("[Campaign Load] Warehouse cleaned up in %dms",
                System.currentTimeMillis() - timestamp));

        retVal.setUnitRating(null);

        LogManager.getLogger().info("Load of campaign file complete!");

        return retVal;
    }

    /**
     * This will fixup unit-tech problems seen in some save games, such as techs
     * having been double-assigned or being assigned to mothballed units.
     */
    private void fixupUnitTechProblems(Campaign retVal) {
        // Cleanup problems with techs and units
        for (Person tech : retVal.getTechs()) {
            for (Unit u : new ArrayList<>(tech.getTechUnits())) {
                String reason = null;
                String unitDesc = u.getId().toString();
                if (null == u.getTech()) {
                    reason = "was not referenced by unit";
                    u.setTech(tech);
                } else if (u.isMothballed()) {
                    reason = "referenced mothballed unit";
                    unitDesc = u.getName();
                    tech.removeTechUnit(u);
                } else if (u.getTech() != null && !tech.getId().equals(u.getTech().getId())) {
                    reason = String.format("referenced tech %s's maintained unit", u.getTech().getFullName());
                    unitDesc = u.getName();
                    tech.removeTechUnit(u);
                }
                if (null != reason) {
                    LogManager.getLogger().warn(String.format("Tech %s %s %s (fixed)", tech.getFullName(), reason, unitDesc));
                }
            }
        }
    }

    /**
     * Pulled out purely for encapsulation. Makes the code neater and easier to read.
     *
     * @param retVal The Campaign object that is being populated.
     * @param wni    The XML node we're working from.
     * @throws DOMException
     */
    private static void processInfoNode(Campaign retVal, Node wni, Version version) throws DOMException {
        NodeList nl = wni.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);
            int xc = wn.getNodeType();

            // If it's not an element, again, we're ignoring it.
            if (xc != Node.ELEMENT_NODE) {
                continue;
            }
            String xn = wn.getNodeName();

            try {
                if (xn.equalsIgnoreCase("calendar")) {
                    retVal.setLocalDate(MHQXMLUtility.parseDate(wn.getTextContent().trim()));
                } else if (xn.equalsIgnoreCase(Camouflage.XML_TAG)) {
                    retVal.setCamouflage(Camouflage.parseFromXML(wn));
                } else if (xn.equalsIgnoreCase("camoCategory")) {
                    String val = wn.getTextContent().trim();

                    if (!val.equals("null")) {
                        retVal.getCamouflage().setCategory(val);
                    }
                } else if (xn.equalsIgnoreCase("camoFileName")) {
                    String val = wn.getTextContent().trim();

                    if (!val.equals("null")) {
                        retVal.getCamouflage().setFilename(val);
                    }
                } else if (xn.equalsIgnoreCase("colour")) {
                    retVal.setColour(PlayerColour.parseFromString(wn.getTextContent().trim()));
                } else if (xn.equalsIgnoreCase("colorIndex")) { // Legacy - 0.47.15 removal
                    retVal.setColour(PlayerColour.parseFromString(wn.getTextContent().trim()));
                    if (Camouflage.NO_CAMOUFLAGE.equals(retVal.getCamouflage().getCategory())) {
                        retVal.getCamouflage().setCategory(Camouflage.COLOUR_CAMOUFLAGE);
                        retVal.getCamouflage().setFilename(retVal.getColour().name());
                    }
                } else if (xn.equalsIgnoreCase(UnitIcon.XML_TAG)) {
                    retVal.setUnitIcon(UnitIcon.parseFromXML(wn));
                } else if (xn.equalsIgnoreCase("iconCategory")) { // Legacy - 0.49.6 removal
                    final String value = wn.getTextContent().trim();
                    retVal.getUnitIcon().setCategory(value.equals("null") ? null : value);
                } else if (xn.equalsIgnoreCase("iconFileName")) { // Legacy - 0.49.6 removal
                    final String value = wn.getTextContent().trim();
                    if (value.equals("null") || value.equals(AbstractIcon.DEFAULT_ICON_FILENAME)) {
                        retVal.getUnitIcon().setFilename(null);
                    } else {
                        retVal.getUnitIcon().setFilename(value);
                    }
                } else if (xn.equalsIgnoreCase("nameGen")) {
                    // First, get all the child nodes;
                    NodeList nl2 = wn.getChildNodes();
                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);
                        if (wn2.getParentNode() != wn) {
                            continue;
                        }
                        if (wn2.getNodeName().equalsIgnoreCase("faction")) {
                            RandomNameGenerator.getInstance().setChosenFaction(wn2.getTextContent().trim());
                        } else if (wn2.getNodeName().equalsIgnoreCase("percentFemale")) {
                            RandomGenderGenerator.setPercentFemale(Integer.parseInt(wn2.getTextContent().trim()));
                        }
                    }
                } else if (xn.equalsIgnoreCase("currentReport")) {
                    // First, get all the child nodes;
                    NodeList nl2 = wn.getChildNodes();

                    // Then, make sure the report is empty. *just* in case.
                    // ...That is, creating a new campaign throws in a date line
                    // for us...
                    // So make sure it's cleared out.
                    retVal.getCurrentReport().clear();

                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);

                        if (wn2.getParentNode() != wn) {
                            continue;
                        }

                        if (wn2.getNodeName().equalsIgnoreCase("reportLine")) {
                            retVal.getCurrentReport().add(wn2.getTextContent());
                        }
                    }
                } else if (xn.equalsIgnoreCase("faction")) {
                    retVal.setFactionCode(wn.getTextContent());
                } else if (xn.equalsIgnoreCase("retainerEmployerCode")) {
                    retVal.setRetainerEmployerCode(wn.getTextContent());
                } else if (xn.equalsIgnoreCase("rankSystem")) {
                    if (!wn.hasChildNodes()) { // we need there to be child nodes to parse from
                        continue;
                    }
                    final RankSystem rankSystem = RankSystem.generateInstanceFromXML(wn.getChildNodes(), version);
                    // If the system is valid (either not campaign or validates), set it. Otherwise,
                    // keep the default
                    if (!rankSystem.getType().isCampaign() || new RankValidator().validate(rankSystem, true)) {
                        retVal.setRankSystemDirect(rankSystem);
                    }
                } else if (xn.equalsIgnoreCase("gmMode")) {
                    retVal.setGMMode(Boolean.parseBoolean(wn.getTextContent().trim()));
                } else if (xn.equalsIgnoreCase("showOverview")) {
                    retVal.setOverviewLoadingValue(Boolean.parseBoolean(wn.getTextContent().trim()));
                } else if (xn.equalsIgnoreCase("name")) {
                    String val = wn.getTextContent().trim();

                    if (val.equals("null")) {
                        retVal.setName(null);
                    } else {
                        retVal.setName(val);
                    }
                } else if (xn.equalsIgnoreCase("overtime")) {
                    retVal.setOvertime(Boolean.parseBoolean(wn.getTextContent().trim()));
                } else if (xn.equalsIgnoreCase("astechPool")) {
                    retVal.setAstechPool(Integer.parseInt(wn.getTextContent().trim()));
                } else if (xn.equalsIgnoreCase("astechPoolMinutes")) {
                    retVal.setAstechPoolMinutes(Integer.parseInt(wn.getTextContent().trim()));
                } else if (xn.equalsIgnoreCase("astechPoolOvertime")) {
                    retVal.setAstechPoolOvertime(Integer.parseInt(wn.getTextContent().trim()));
                } else if (xn.equalsIgnoreCase("medicPool")) {
                    retVal.setMedicPool(Integer.parseInt(wn.getTextContent().trim()));
                } else if (xn.equalsIgnoreCase("fatigueLevel")) {
                    retVal.setFatigueLevel(Integer.parseInt(wn.getTextContent().trim()));
                } else if (xn.equalsIgnoreCase("id")) {
                    retVal.setId(UUID.fromString(wn.getTextContent().trim()));
                }
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        }

        // TODO: this could probably be better
        retVal.setCurrentReportHTML(Utilities.combineString(retVal.getCurrentReport(), Campaign.REPORT_LINEBREAK));

        // Everything's new
        List<String> newReports = new ArrayList<>(retVal.getCurrentReport().size() * 2);
        boolean firstReport = true;
        for (String report : retVal.getCurrentReport()) {
            if (firstReport) {
                firstReport = false;
            } else {
                newReports.add(Campaign.REPORT_LINEBREAK);
            }
            newReports.add(report);
        }
        retVal.setNewReports(newReports);
    }

    private static void processLanceNodes(Campaign retVal, Node wn) {
        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("lance")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                LogManager.getLogger().error("Unknown node type not loaded in Lance nodes: " + wn2.getNodeName());
                continue;
            }

            Lance l = Lance.generateInstanceFromXML(wn2);

            if (l != null) {
                retVal.importLance(l);
            }
        }
    }

    private static void cleanupGhostKills(Campaign retVal) {
        // check for kills with missing person references
        List<Kill> ghostKills = new ArrayList<>();
        for (Kill k : retVal.getKills()) {
            if (null == k.getPilotId()) {
                ghostKills.add(k);
            }
        }

        for (Kill k : ghostKills) {
            if (null == k.getPilotId()) {
                retVal.removeKill(k);
            }
        }
    }

    private static void processFinances(Campaign retVal, Node wn) {
        LogManager.getLogger().info("Loading Finances from XML...");
        retVal.setFinances(Finances.generateInstanceFromXML(wn));
        LogManager.getLogger().info("Load of Finances complete!");
    }

    private static void processForces(Campaign retVal, Node wn, Version version) {
        LogManager.getLogger().info("Loading Force Organization from XML...");

        NodeList wList = wn.getChildNodes();

        boolean foundForceAlready = false;
        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("force")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                LogManager.getLogger().error("Unknown node type not loaded in Forces nodes: " + wn2.getNodeName());

                continue;
            }

            if (!foundForceAlready) {
                Force f = Force.generateInstanceFromXML(wn2, retVal, version);
                if (null != f) {
                    retVal.setForces(f);
                    foundForceAlready = true;
                }
            } else {
                LogManager.getLogger().error("More than one type-level force found");
            }
        }

        LogManager.getLogger().info("Load of Force Organization complete!");
    }

    private static void processPersonnelNodes(Campaign retVal, Node wn, Version version) {
        LogManager.getLogger().info("Loading Personnel Nodes from XML...");

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("person")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                LogManager.getLogger().error("Unknown node type not loaded in Personnel nodes: " + wn2.getNodeName());

                continue;
            }

            Person p = Person.generateInstanceFromXML(wn2, retVal, version);

            if (p != null) {
                retVal.importPerson(p);
            }
        }

        LogManager.getLogger().info("Load Personnel Nodes Complete!");
    }

    private static void processSkillTypeNodes(Campaign retVal, Node wn, Version version) {
        LogManager.getLogger().info("Loading Skill Type Nodes from XML...");

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (wn2.getNodeName().startsWith("ability-")) {
                continue;
            } else if (!wn2.getNodeName().equalsIgnoreCase("skillType")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                LogManager.getLogger().error("Unknown node type not loaded in Skill Type nodes: " + wn2.getNodeName());
                continue;
            }

            // TODO: make SkillType a Campaign instance
            SkillType.generateInstanceFromXML(wn2, version);
        }

        LogManager.getLogger().info("Load Skill Type Nodes Complete!");
    }

    private static void processStoryArcNodes(Campaign retVal, Node wn, Version version) {
        LogManager.getLogger().info("Loading Story Arc Nodes from XML...");

        StoryArc storyArc = StoryArc.parseFromXML(wn.getChildNodes(), retVal, version);
        MekHQ.registerHandler(storyArc);
        retVal.useStoryArc(storyArc, false);
    }

    private static void processSpecialAbilityNodes(Campaign retVal, Node wn, Version version) {
        LogManager.getLogger().info("Loading Special Ability Nodes from XML...");

        PersonnelOptions options = new PersonnelOptions();

        // TODO: make SpecialAbility a Campaign instance
        SpecialAbility.clearSPA();

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("ability")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                LogManager.getLogger().error("Unknown node type not loaded in Special Ability nodes: " + wn2.getNodeName());
                continue;
            }
            SpecialAbility.generateInstanceFromXML(wn2, options, version);
        }

        LogManager.getLogger().info("Load Special Ability Nodes Complete!");
    }

    private static void processKillNodes(Campaign retVal, Node wn, Version version) {
        LogManager.getLogger().info("Loading Kill Nodes from XML...");

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            } else if (!wn2.getNodeName().equalsIgnoreCase("kill")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                LogManager.getLogger().error("Unknown node type not loaded in Kill nodes: " + wn2.getNodeName());
                continue;
            }

            Kill kill = Kill.generateInstanceFromXML(wn2, version);
            if (kill != null) {
                retVal.importKill(kill);
            }
        }

        LogManager.getLogger().info("Load Kill Nodes Complete!");
    }

    /**
     * Processes a custom unit in a campaign.
     * @param retVal The {@see Campaign} being parsed.
     * @param wn The current XML element representing a custom unit.
     * @return A value indicating whether or not a new custom unit
     *         file was added to disk.
     */
    private static boolean processCustom(Campaign retVal, Node wn) {
        String sCustomsDir = "data" + File.separator + "mechfiles"
                + File.separator + "customs";  // TODO : Remove inline file path
        String sCustomsDirCampaign = sCustomsDir + File.separator + retVal.getName();
        File customsDir = new File(sCustomsDir);
        if (!customsDir.exists()) {
            if (!customsDir.mkdir()) {
                LogManager.getLogger().error("Failed to create directory " + sCustomsDir + ", and therefore cannot save the unit.");
                return false;
            }
        }
        File customsDirCampaign = new File(sCustomsDirCampaign);
        if (!customsDirCampaign.exists()) {
            if (!customsDirCampaign.mkdir()) {
                LogManager.getLogger().error("Failed to create directory " + sCustomsDirCampaign + ", and therefore cannot save the unit.");
                return false;
            }
        }

        NodeList wList = wn.getChildNodes();

        String name = null;
        String mtf = null;
        String blk = null;

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (wn2.getNodeName().equalsIgnoreCase("name")) {
                name = wn2.getTextContent().trim();
            } else if (wn2.getNodeName().equalsIgnoreCase("mtf")) {
                mtf = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("blk")) {
                blk = wn2.getTextContent();
            }
        }

        if (StringUtils.isNotBlank(name)) {
            String ext;
            String contents;

            if (StringUtils.isNotBlank(mtf)) {
                ext = ".mtf";
                contents = mtf;
            } else if (StringUtils.isNotBlank(blk)) {
                ext = ".blk";
                contents = blk;
            } else {
                return false;
            }

            // If this file already exists then don't overwrite it or we will end up with a bunch of copies
            String safeName = MHQXMLUtility.escape(name);
            String fileName = sCustomsDir + File.separator + safeName + ext;
            String fileNameCampaign = sCustomsDirCampaign + File.separator + safeName + ext;

            // TODO : get a hash or something to validate and overwrite if we updated this
            if ((new File(fileName)).exists() || (new File(fileNameCampaign)).exists()) {
                return false;
            }

            if (tryWriteCustomToFile(fileNameCampaign, contents)) {
                retVal.addCustom(name);
                return true;
            }
        }

        return false;
    }

    private static boolean tryWriteCustomToFile(String fileName, String contents) {
        LogManager.getLogger().info("Writing custom unit from inline data to " + fileName);

        try (OutputStream out = new FileOutputStream(fileName);
            PrintStream p = new PrintStream(out)) {

            p.println(contents);

            LogManager.getLogger().info("Wrote custom unit from inline data to: " + fileName);

            return true;
        } catch (Exception ex) {
            LogManager.getLogger().error("Error writing custom unit from inline data to: " + fileName, ex);
            return false;
        }
    }

    private static void processMissionNodes(Campaign retVal, Node wn, Version version) {
        LogManager.getLogger().info("Loading Mission Nodes from XML...");

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("mission")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                LogManager.getLogger().warn("Unknown node type not loaded in Mission nodes: " + wn2.getNodeName());
                continue;
            }

            Mission m = Mission.generateInstanceFromXML(wn2, retVal, version);

            if (m != null) {
                retVal.importMission(m);
            }
        }

        // Restore references on AtBContracts
        for (AtBContract contract : retVal.getAtBContracts()) {
            contract.restore(retVal);
        }

        LogManager.getLogger().info("Load Mission Nodes Complete!");
    }

    private static @Nullable String checkUnits(final Node wn) {
        LogManager.getLogger().info("Checking for missing entities...");

        List<String> unitList = new ArrayList<>();
        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("unit")) {
                continue;
            }

            NodeList nl = wn2.getChildNodes();

            for (int y = 0; y < nl.getLength(); y++) {
                Node wn3 = nl.item(y);
                if (wn3.getNodeName().equalsIgnoreCase("entity")) {
                    try {
                        final Entity entity = MHQXMLUtility.parseSingleEntityMul((Element) wn3, null);
                        if (entity == null) {
                            String name = MHQXMLUtility.getEntityNameFromXmlString(wn3);
                            if (!unitList.contains(name)) {
                                unitList.add(name);
                            }
                        }
                    } catch (Exception ex) {
                        LogManager.getLogger().error("Could not read entity from XML", ex);
                    }
                }
            }
        }
        LogManager.getLogger().info("Finished checking for missing entities!");

        if (unitList.isEmpty()) {
            return null;
        } else {
            StringBuilder unitListString = new StringBuilder();
            for (String s : unitList) {
                unitListString.append("\n").append(s);
            }
            LogManager.getLogger().error(String.format("Could not load the following units: %s", unitListString.toString()));
            return unitListString.toString();
        }
    }

    private static void processUnitNodes(Campaign retVal, Node wn, Version version) {
        LogManager.getLogger().info("Loading Unit Nodes from XML...");

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("unit")) {
                LogManager.getLogger().error("Unknown node type not loaded in Unit nodes: " + wn2.getNodeName());
                continue;
            }

            Unit u = Unit.generateInstanceFromXML(wn2, version, retVal);

            if (u != null) {
                retVal.importUnit(u);
            }
        }

        LogManager.getLogger().info("Load Unit Nodes Complete!");
    }

    private static void processPartNodes(Campaign retVal, Node wn, Version version) {
        LogManager.getLogger().info("Loading Part Nodes from XML...");

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        List<Part> parts = new ArrayList<>();
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("part")) {
                LogManager.getLogger().error("Unknown node type not loaded in Part nodes: " + wn2.getNodeName());
                continue;
            }

            Part p = Part.generateInstanceFromXML(wn2, version);

            if (p != null) {
                parts.add(p);
            }
        }

        retVal.importParts(parts);

        LogManager.getLogger().info("Load Part Nodes Complete!");
    }

    private static void postProcessParts(Campaign retVal, Version version) {
        Map<Integer, Part> replaceParts = new HashMap<>();
        List<Part> removeParts = new ArrayList<>();
        for (Part prt : retVal.getWarehouse().getParts()) {
            prt.fixReferences(retVal);

            // Remove fundamentally broken equipment parts
            if (((prt instanceof EquipmentPart) && ((EquipmentPart) prt).getType() == null)
                    || ((prt instanceof MissingEquipmentPart) && ((MissingEquipmentPart) prt).getType() == null)) {
                LogManager.getLogger().warn("Could not find matching EquipmentType for part " + prt.getName());
                removeParts.add(prt);
                continue;
            }

            // deal with equipmentparts that are now subtyped
            if (isLegacyMASC(prt)) {
                Part replacement = new MASC(prt.getUnitTonnage(), ((EquipmentPart) prt).getType(),
                        ((EquipmentPart) prt).getEquipmentNum(), retVal, 0, prt.isOmniPodded());
                replacement.setId(prt.getId());
                replacement.setUnit(prt.getUnit());
                replaceParts.put(prt.getId(), replacement);
            }

            if (isLegacyMissingMASC(prt)) {
                Part replacement = new MissingMASC(prt.getUnitTonnage(),
                        ((MissingEquipmentPart) prt).getType(), ((MissingEquipmentPart) prt).getEquipmentNum(), retVal,
                        prt.getTonnage(), 0, prt.isOmniPodded());
                replacement.setId(prt.getId());
                replacement.setUnit(prt.getUnit());
                replaceParts.put(prt.getId(), replacement);
            }

            // Fixup LargeCraftAmmoBins from old versions
            if ((prt.getUnit() != null) && (prt.getUnit().getEntity() != null)
                    && version.isLowerThan("0.43.5")
                    && ((prt instanceof AmmoBin) || (prt instanceof MissingAmmoBin))) {
                if (prt.getUnit().getEntity().usesWeaponBays()) {
                    AmmoMounted ammo;
                    if (prt instanceof EquipmentPart) {
                        ammo = (AmmoMounted) prt.getUnit().getEntity().getEquipment(((EquipmentPart) prt).getEquipmentNum());
                    } else {
                        ammo = (AmmoMounted) prt.getUnit().getEntity().getEquipment(((MissingEquipmentPart) prt).getEquipmentNum());
                    }
                    if (null != ammo) {
                        if (prt instanceof AmmoBin) {
                            LargeCraftAmmoBin replacement = new LargeCraftAmmoBin(prt.getUnitTonnage(),
                                    ((AmmoBin) prt).getType(),
                                    ((AmmoBin) prt).getEquipmentNum(),
                                    ((AmmoBin) prt).getShotsNeeded(),
                                    ammo.getSize(), retVal);
                            replacement.setId(prt.getId());
                            replacement.setUnit(prt.getUnit());
                            replacement.setBay(prt.getUnit().getEntity().getBayByAmmo(ammo));
                            replaceParts.put(prt.getId(), replacement);
                        } else {
                            MissingLargeCraftAmmoBin replacement = new MissingLargeCraftAmmoBin(prt.getUnitTonnage(),
                                    ((MissingAmmoBin) prt).getType(),
                                    ((MissingAmmoBin) prt).getEquipmentNum(),
                                    ammo.getSize(), retVal);
                            replacement.setId(prt.getId());
                            replacement.setUnit(prt.getUnit());
                            replacement.setBay(prt.getUnit().getEntity().getBayByAmmo(ammo));
                            replaceParts.put(prt.getId(), replacement);
                        }
                    }
                }
            }
        }

        // Replace parts that need to be replaced
        for (Map.Entry<Integer, Part> entry : replaceParts.entrySet()) {
            int partId = entry.getKey();
            Part oldPart = retVal.getWarehouse().getPart(partId);
            if (oldPart != null) {
                retVal.getWarehouse().removePart(oldPart);
            }

            retVal.getWarehouse().addPart(entry.getValue());
        }

        // After replacing parts, go back through and remove more broken parts
        for (Part prt : retVal.getWarehouse().getParts()) {
            // deal with the Weapon as Heat Sink problem from earlier versions
            if ((prt instanceof HeatSink) && !prt.getName().contains("Heat Sink")) {
                removeParts.add(prt);
                continue;
            }

            Unit u = prt.getUnit();
            if (u != null) {
                // get rid of any equipmentparts without types, locations or mounteds
                if (prt instanceof EquipmentPart) {
                    final EquipmentPart ePart = (EquipmentPart) prt;

                    // Null Type... parsing failure
                    if (ePart.getType() == null) {
                        removeParts.add(prt);
                        continue;
                    }

                    Mounted m = u.getEntity().getEquipment(ePart.getEquipmentNum());

                    // Remove equipment parts missing mounts
                    if (m == null) {
                        removeParts.add(prt);
                        continue;
                    }

                    // Remove equipment parts without a valid location, unless they're an ammo bin
                    // as they may not have a location
                    if ((m.getLocation() == Entity.LOC_NONE) && !(prt instanceof AmmoBin)) {
                        removeParts.add(prt);
                        continue;
                    }

                    // Remove existing duplicate parts.
                    Part duplicatePart = u.getPartForEquipmentNum(ePart.getEquipmentNum(), prt.getLocation());
                    if ((duplicatePart instanceof EquipmentPart)
                            && ePart.getType().equals(((EquipmentPart) duplicatePart).getType())) {
                        removeParts.add(prt);
                        continue;
                    }
                }
                if (prt instanceof MissingEquipmentPart) {
                    Mounted m = u.getEntity().getEquipment(((MissingEquipmentPart) prt).getEquipmentNum());

                    // Remove equipment parts missing mounts
                    if (m == null) {
                        removeParts.add(prt);
                        continue;
                    }

                    // Remove missing equipment parts without a valid location, unless they're an ammo bin as they may not have a location
                    if ((m.getLocation() == Entity.LOC_NONE) && !(prt instanceof MissingAmmoBin)) {
                        removeParts.add(prt);
                        continue;
                    }
                }

                // if the type is a BayWeapon, remove
                if ((prt instanceof EquipmentPart)
                        && (((EquipmentPart) prt).getType() instanceof BayWeapon)) {
                    removeParts.add(prt);
                    continue;
                }

                if ((prt instanceof MissingEquipmentPart)
                        && (((MissingEquipmentPart) prt).getType() instanceof BayWeapon)) {
                    removeParts.add(prt);
                    continue;
                }

                // if actuators on units have no location (on version 1.23 and
                // earlier) then remove them and let initializeParts (called
                // later) create new ones
                if ((prt instanceof MekActuator) && (prt.getLocation() == Entity.LOC_NONE)) {
                    removeParts.add(prt);
                } else if ((prt instanceof MissingMekActuator) && (prt.getLocation() == Entity.LOC_NONE)) {
                    removeParts.add(prt);
                } else if (((u.getEntity() instanceof SmallCraft) || (u.getEntity() instanceof Jumpship))
                        && ((prt instanceof EnginePart) || (prt instanceof MissingEnginePart))) {
                    //units from earlier versions might have the wrong kind of engine
                    removeParts.add(prt);
                } else {
                    u.addPart(prt);
                }
            }

            // deal with true values for sensor and life support on non-Mech heads
            if ((prt instanceof MekLocation)
                    && (((MekLocation) prt).getLoc() != Mech.LOC_HEAD)) {
                ((MekLocation) prt).setSensors(false);
                ((MekLocation) prt).setLifeSupport(false);
            }

            if (prt instanceof MissingPart) {
                // Missing Parts should only exist on units, but there have
                // been cases where they continue to float around outside of units
                // so this should clean that up
                if (null == u) {
                    removeParts.add(prt);
                } else {
                    // run this to make sure that slots for missing parts are set as
                    // unrepairable
                    // because they will not be in missing locations
                    prt.updateConditionFromPart();
                }
            }

            // old versions didn't distinguish tank engines
            if ((prt instanceof EnginePart) && prt.getName().contains("Vehicle")) {
                boolean isHover = null != u
                        && u.getEntity().getMovementMode() == EntityMovementMode.HOVER && u.getEntity() instanceof Tank;
                ((EnginePart) prt).fixTankFlag(isHover);
            }

            // clan flag might not have been properly set in early versions
            if ((prt instanceof EnginePart) && prt.getName().contains("(Clan")
                    && (prt.getTechBase() != Part.T_CLAN)) {
                ((EnginePart) prt).fixClanFlag();
            }
            if ((prt instanceof MissingEnginePart) && (null != u)
                    && (u.getEntity() instanceof Tank)) {
                boolean isHover = u.getEntity().getMovementMode() == EntityMovementMode.HOVER;
                ((MissingEnginePart) prt).fixTankFlag(isHover);
            }
            if ((prt instanceof MissingEnginePart)
                    && prt.getName().contains("(Clan") && (prt.getTechBase() != Part.T_CLAN)) {
                ((MissingEnginePart) prt).fixClanFlag();
            }

            if (version.isLowerThan("0.44.0")) {
                if ((prt instanceof MekLocation)
                        && (((MekLocation) prt).getStructureType() == EquipmentType.T_STRUCTURE_ENDO_STEEL)) {
                    if (null != u) {
                        ((MekLocation) prt).setClan(TechConstants.isClan(u.getEntity().getStructureTechLevel()));
                    } else {
                        ((MekLocation) prt).setClan(retVal.getFaction().isClan());
                    }
                } else if ((prt instanceof MissingMekLocation)
                        && (((MissingMekLocation) prt).getStructureType() == EquipmentType.T_STRUCTURE_ENDO_STEEL)) {
                    if (null != u) {
                        ((MissingMekLocation) prt).setClan(TechConstants.isClan(u.getEntity().getStructureTechLevel()));
                    }
                }
            }

            // Spare Ammo bins are useless
            if ((prt instanceof AmmoBin) && prt.isSpare()) {
                removeParts.add(prt);
            }
        }
        for (Part prt : removeParts) {
            LogManager.getLogger().debug("Removing part #" + prt.getId() + " " + prt.getName());
            retVal.getWarehouse().removePart(prt);
        }
    }

    /**
     * Determines if the supplied part is a MASC from an older save. This means that it needs to be converted
     * to an actual MASC part.
     * @param p The part to check.
     * @return Whether it's an old MASC.
     */
    private static boolean isLegacyMASC(Part p) {
        return (p instanceof EquipmentPart) &&
                !(p instanceof MASC) &&
                ((EquipmentPart) p).getType().hasFlag(MiscType.F_MASC) &&
                (((EquipmentPart) p).getType() instanceof MiscType);
    }

    /**
     * Determines if the supplied part is a "missing" MASC from an older save. This means that it needs to be converted
     * to an actual "missing" MASC part.
     * @param p The part to check.
     * @return Whether it's an old "missing" MASC.
     */
    private static boolean isLegacyMissingMASC(Part p) {
        return (p instanceof MissingEquipmentPart) &&
                !(p instanceof MissingMASC) &&
                ((MissingEquipmentPart) p).getType().hasFlag(MiscType.F_MASC) &&
                (((MissingEquipmentPart) p).getType() instanceof MiscType);
    }

    private static void updatePlanetaryEventsFromXML(Node wn) {
        List<Planet.PlanetaryEvent> events;
        Map<Integer, List<Planet.PlanetaryEvent>> eventsMap = new HashMap<>();

        NodeList wList = wn.getChildNodes();
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (wn2.getNodeName().equalsIgnoreCase("system")) {
                NodeList systemNodes = wn2.getChildNodes();
                String systemId = null;
                List<PlanetarySystem.PlanetarySystemEvent> sysEvents = new ArrayList<>();
                eventsMap.clear();
                for (int n = 0; n < systemNodes.getLength(); ++n) {
                    Node systemNode = systemNodes.item(n);
                    if (systemNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if (systemNode.getNodeName().equalsIgnoreCase("id")) {
                        systemId = systemNode.getTextContent();
                    } else if (systemNode.getNodeName().equalsIgnoreCase("event")) {
                        PlanetarySystem.PlanetarySystemEvent event = Systems.getInstance().readPlanetarySystemEvent(systemNode);
                        if (null != event) {
                            event.custom = true;
                            sysEvents.add(event);
                        }
                    } else if (systemNode.getNodeName().equalsIgnoreCase("planet")) {
                        NodeList planetNodes = systemNode.getChildNodes();
                        int sysPos = 0;
                        events = new ArrayList<>();
                        for (int j = 0; j < planetNodes.getLength(); ++j) {
                            Node planetNode = planetNodes.item(j);
                            if (planetNode.getNodeType() != Node.ELEMENT_NODE) {
                                continue;
                            }
                            if (planetNode.getNodeName().equalsIgnoreCase("sysPos")) {
                                sysPos = Integer.parseInt(planetNode.getTextContent());
                            } else if (planetNode.getNodeName().equalsIgnoreCase("event")) {
                                Planet.PlanetaryEvent event = Systems.getInstance().readPlanetaryEvent(planetNode);
                                if (null != event) {
                                    event.custom = true;
                                    events.add(event);
                                }
                            }
                        }
                        if (sysPos > 0 && !events.isEmpty()) {
                            eventsMap.put(sysPos, events);
                        }
                    }
                }
                if (null != systemId) {
                    //iterate through events hash and assign events to planets
                    Iterator<Map.Entry<Integer, List<PlanetaryEvent>>> it = eventsMap.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<Integer, List<PlanetaryEvent>> pair = it.next();
                        Systems.getInstance().updatePlanetaryEvents(systemId, pair.getValue(), true, pair.getKey());
                    }
                    //check for system-wide events
                    if (!sysEvents.isEmpty()) {
                        Systems.getInstance().updatePlanetarySystemEvents(systemId, sysEvents, true);
                    }
                }
            }

            //legacy code for before switch to planetary systems if planet is at top level
            if (wn2.getNodeName().equalsIgnoreCase("planet")) {
                NodeList planetNodes = wn2.getChildNodes();
                String planetId = null;
                events = new ArrayList<>();
                for (int n = 0; n < planetNodes.getLength(); ++n) {
                    Node planetNode = planetNodes.item(n);
                    if (planetNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    if (planetNode.getNodeName().equalsIgnoreCase("id")) {
                        planetId = planetNode.getTextContent();
                    } else if (planetNode.getNodeName().equalsIgnoreCase("event")) {
                        Planet.PlanetaryEvent event = Systems.getInstance().readPlanetaryEvent(planetNode);
                        if (null != event) {
                            event.custom = true;
                            events.add(event);
                        }
                    }
                }
                if (null != planetId) {
                    Systems.getInstance().updatePlanetaryEvents(planetId, events, true);
                }
            }
        }
    }


    //region Migration Methods
    //region Ancestry Migration
    private static Map<UUID, List<Person>> ancestryMigrationMap = new HashMap<>();

    /**
     * This method is used to add people to the ancestry migration map that is used to migrate
     * from the old Ancestors setup to {@link mekhq.campaign.personnel.familyTree.Genealogy} starting
     * from 0.47.8
     * @param ancestorsId the Person's Ancestor Id
     * @param person the person to add the the above HashMap
     */
    public static void addToAncestryMigrationMap(UUID ancestorsId, Person person) {
        ancestryMigrationMap.putIfAbsent(ancestorsId, new ArrayList<>());
        ancestryMigrationMap.get(ancestorsId).add(person);
    }

    /**
     * This method is used to migrate from Ancestry nodes to
     * {@link mekhq.campaign.personnel.familyTree.Genealogy} since the swap-over in 0.47.8
     * @param wn the node containing the saved ancestry
     */
    private static void migrateAncestorNodes(Campaign campaign, Node wn) {
        NodeList wList = wn.getChildNodes();

        for (int x = 0; x < wList.getLength(); x++) {
            // First, we determine the node values
            UUID id = null;
            Person father = null;
            Person mother = null;
            Node wn2 = wList.item(x);

            if ((wn2.getNodeType() != Node.ELEMENT_NODE)
                    || !wn2.getNodeName().equalsIgnoreCase("ancestor")) {
                continue;
            }

            NodeList nl = wn2.getChildNodes();
            for (int y = 0; y < nl.getLength(); y++) {
                Node wn3 = nl.item(y);
                if (wn3.getNodeName().equalsIgnoreCase("id")) {
                    id = UUID.fromString(wn3.getTextContent().trim());
                } else if (wn3.getNodeName().equalsIgnoreCase("fatherId")) {
                    father = campaign.getPerson(UUID.fromString(wn3.getTextContent().trim()));
                } else if (wn3.getNodeName().equalsIgnoreCase("motherId")) {
                    mother = campaign.getPerson(UUID.fromString(wn3.getTextContent().trim()));
                }
            }

            // We skip the Person if they are null or cannot be migrated
            if ((id == null) || !ancestryMigrationMap.containsKey(id)) {
                continue;
            }

            // Finally, we migrate the individual person data
            Iterator<Person> people = ancestryMigrationMap.get(id).iterator();
            while (people.hasNext()) {
                Person person = people.next();
                people.remove();

                if (father == null) {
                    LogManager.getLogger().warn("Unknown father does not exist, skipping adding Genealogy for them.");
                } else if (father.getId() != null) {
                    person.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, father);
                    father.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, person);
                } else {
                    LogManager.getLogger().warn("Person with id " + father.getId() + "does not exist, skipping adding Genealogy for them.");
                }

                if (mother == null) {
                    LogManager.getLogger().warn("Unknown mother does not exist, skipping adding Genealogy for them.");
                } else if (mother.getId() != null) {
                    person.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, mother);
                    mother.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, person);
                } else {
                    LogManager.getLogger().warn("Person with id " + mother.getId() + " does not exist, skipping adding Genealogy for them.");
                }
            }
        }
    }
    //endregion Ancestry Migration
    //endregion Migration Methods
}
