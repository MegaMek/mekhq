/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.io;

import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.force.CombatTeam.recalculateCombatTeams;
import static mekhq.campaign.force.Formation.FORCE_NONE;
import static mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket.generatePersonnelMarketDataFromXML;
import static mekhq.campaign.personnel.enums.PersonnelStatus.statusValidator;
import static mekhq.campaign.personnel.skills.SkillDeprecationTool.DEPRECATED_SKILLS;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;

import megamek.Version;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.util.PlayerColour;
import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.enums.TechBase;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.icons.Camouflage;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Jumpship;
import megamek.common.units.Mek;
import megamek.common.units.SmallCraft;
import megamek.common.units.Tank;
import megamek.common.weapons.bayWeapons.BayWeapon;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignFactory;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.Kill;
import mekhq.campaign.Warehouse;
import mekhq.campaign.againstTheBot.AtBConfiguration;
import mekhq.campaign.camOpsReputation.ReputationController;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.campaignOptions.CampaignOptionsUnmarshaller;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.icons.UnitIcon;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.ShoppingList;
import mekhq.campaign.market.contractMarket.AbstractContractMarket;
import mekhq.campaign.market.contractMarket.AtbMonthlyContractMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.SVArmor;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.MASC;
import mekhq.campaign.parts.equipment.MissingAmmoBin;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.parts.equipment.MissingMASC;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.missing.MissingEnginePart;
import mekhq.campaign.parts.missing.MissingMekActuator;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.education.EducationController;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.SkillDeprecationTool;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker;
import mekhq.campaign.storyArc.StoryArc;
import mekhq.campaign.stratCon.StratConPlayType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.cleanup.EquipmentUnscrambler;
import mekhq.campaign.unit.cleanup.EquipmentUnscramblerResult;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.campaignOptions.optionChangeDialogs.StratConMaplessCampaignOptionsChangedConfirmationDialog;
import mekhq.gui.dialog.MilestoneUpgradePathDialog;
import mekhq.io.idReferenceClasses.PersonIdReference;
import mekhq.module.atb.AtBEventProcessor;
import mekhq.utilities.MHQXMLUtility;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public record CampaignXmlParser(InputStream is, MekHQ app) {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignXmlParser";
    private static final MMLogger LOGGER = MMLogger.create(CampaignXmlParser.class);

    public void close() throws IOException {
        this.is.close();
    }

    /**
     * Designed to create a campaign object from an input stream containing an XML structure.
     *
     * @return The created Campaign object, or null if there was a problem.
     *
     * @throws CampaignXmlParseException Thrown when there was a problem parsing the CPNX file
     * @throws NullEntityException       Thrown when an entity is referenced but cannot be loaded or found
     */
    public Campaign parse() throws CampaignXmlParseException, NullEntityException {
        LOGGER.info("Starting load of campaign file from XML...");
        // Initialize variables.
        Campaign campaign = CampaignFactory.createCampaign();
        campaign.setApp(app);

        Document xmlDoc;

        try {
            // Using factory get an instance of document builder
            DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(is);
        } catch (Exception ex) {
            LOGGER.error("", ex);
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
        // Confirm the campaign version is compatible with the current MekHQ version. This function lives here so that
        // we don't attempt to load incompatible campaigns and risk running into errors that might prevent the player
        // from viewing this dialog
        new MilestoneUpgradePathDialog(campaign, version);

        // Assuming there is no upgrade path, we set version and continue parsing the campaign.
        campaign.setVersion(version);

        // Indicates whether new units were written to disk while
        // loading the Campaign file. If so, we need to kick back off loading
        // all the unit data from disk.
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

                if (xn.equalsIgnoreCase("info")) { // This is needed so that the campaign name gets set in campaign
                    try {
                        processInfoNode(campaign, wn, version);
                    } catch (DOMException e) {
                        throw new CampaignXmlParseException(e);
                    }
                } else if (xn.equalsIgnoreCase("custom")) {
                    reloadUnitData |= processCustom(campaign, wn);
                } else if (xn.equalsIgnoreCase("campaignOptions")) {
                    campaign.setCampaignOptions(CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(wn,
                          version));

                    //  < 50.10 compatibility handler
                    CampaignOptions campaignOptions = campaign.getCampaignOptions();
                    if (campaignOptions.isHadAtBEnabledMarker() && !campaignOptions.isUseStratCon()) {
                        // Mapless StratCon replaced AtB in 50.10
                        campaignOptions.setStratConPlayType(StratConPlayType.MAPLESS);
                        new StratConMaplessCampaignOptionsChangedConfirmationDialog(campaign);
                    }
                } else if (xn.equalsIgnoreCase("gameOptions")) {
                    campaign.getGameOptions().fillFromXML(wn.getChildNodes());
                }
            }
            // If it's a text node or attribute or whatever at this level,
            // it's probably white-space.
            // We can safely ignore it even if it isn't, for now.
        }

        // Only reload unit data if we updated files on disk
        if (reloadUnitData) {
            MekSummaryCache.getInstance().loadMekData();
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
            }
            // If it's a text node or attribute or whatever at this level,
            // it's probably white-space.
            // We can safely ignore it even if it isn't, for now.
        }

        boolean foundPersonnelMarket = false;
        boolean foundContractMarket = false;
        boolean foundUnitMarket = false;

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node workingNode = nl.item(x);

            if (!workingNode.getParentNode().equals(campaignEle)) {
                continue;
            }

            int xc = workingNode.getNodeType();

            if (xc == Node.ELEMENT_NODE) {
                // This is what we really care about.
                // All the meat of our document is in this node type, at this level.
                // Okay, so what element is it?
                String nodeName = workingNode.getNodeName();

                if (nodeName.equalsIgnoreCase("pastVersions")) {
                    processPastVersionNodes(campaign, workingNode);
                } else if (nodeName.equalsIgnoreCase("randomSkillPreferences")) {
                    campaign.setRandomSkillPreferences(RandomSkillPreferences.generateRandomSkillPreferencesFromXml(
                          workingNode,
                          version));
                } else if (nodeName.equalsIgnoreCase("parts")) {
                    processPartNodes(campaign, workingNode, version);
                } else if (nodeName.equalsIgnoreCase("personnel")) {
                    // TODO: Make this depending on campaign options
                    // TODO: hoist registerAll out of this
                    InjuryTypes.registerAll();
                    processPersonnelNodes(campaign, workingNode, version);
                } else if (nodeName.equalsIgnoreCase("units")) {
                    processUnitNodes(campaign, workingNode, version);
                } else if (nodeName.equalsIgnoreCase("missions")) {
                    processMissionNodes(campaign, workingNode, version);
                } else if (nodeName.equalsIgnoreCase("forces")) {
                    processForces(campaign, workingNode, version);
                } else if (nodeName.equalsIgnoreCase("finances")) {
                    processFinances(campaign, workingNode);
                } else if (nodeName.equalsIgnoreCase("location")) {
                    campaign.setLocation(CurrentLocation.generateInstanceFromXML(workingNode, campaign));
                } else if (nodeName.equalsIgnoreCase("isAvoidingEmptySystems")) {
                    campaign.setIsAvoidingEmptySystems(Boolean.parseBoolean(workingNode.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("skillTypes")) {
                    processSkillTypeNodes(workingNode, version);
                } else if (nodeName.equalsIgnoreCase("specialAbilities")) {
                    processSpecialAbilityNodes(campaign, workingNode, version);
                } else if (nodeName.equalsIgnoreCase("storyArc")) {
                    processStoryArcNodes(campaign, workingNode, version);
                } else if (nodeName.equalsIgnoreCase("kills")) {
                    processKillNodes(campaign, workingNode, version);
                } else if (nodeName.equalsIgnoreCase("shoppingList")) {
                    campaign.setShoppingList(ShoppingList.generateInstanceFromXML(workingNode, campaign, version));
                } else if (nodeName.equalsIgnoreCase("personnelMarket")) {
                    campaign.setPersonnelMarket(PersonnelMarket.generateInstanceFromXML(workingNode,
                          campaign,
                          version));
                    foundPersonnelMarket = true;
                } else if (nodeName.equalsIgnoreCase("contractMarket")) {
                    // CAW: implicit DEPENDS-ON to the <missions> node
                    campaign.setContractMarket(AbstractContractMarket.generateInstanceFromXML(workingNode,
                          campaign,
                          version));
                    foundContractMarket = true;
                } else if (nodeName.equalsIgnoreCase("unitMarket")) {
                    // Windchild: implicit DEPENDS ON to the <campaignOptions> nodes
                    campaign.setUnitMarket(campaign.getCampaignOptions().getUnitMarketMethod().getUnitMarket());
                    campaign.getUnitMarket().fillFromXML(workingNode, campaign, version);
                    foundUnitMarket = true;
                } else if (nodeName.equalsIgnoreCase("lances") || nodeName.equalsIgnoreCase("combatTeams")) {
                    processCombatTeamNodes(campaign, workingNode);
                } else if (nodeName.equalsIgnoreCase("retirementDefectionTracker")) {
                    campaign.setRetirementDefectionTracker(RetirementDefectionTracker.generateInstanceFromXML(
                          workingNode,
                          campaign));
                } else if (nodeName.equalsIgnoreCase("personnelWhoAdvancedInXP")) {
                    campaign.setPersonnelWhoAdvancedInXP(processPersonnelWhoAdvancedInXP(workingNode, campaign));
                } else if (nodeName.equalsIgnoreCase("automatedMothballUnits")) {
                    campaign.setAutomatedMothballUnits(processAutomatedMothballNodes(workingNode));
                } else if (nodeName.equalsIgnoreCase("autoResolveBehaviorSettings")) {
                    campaign.setAutoResolveBehaviorSettings(firstNonNull(BehaviorSettingsFactory.getInstance()
                                                                               .getBehavior(workingNode.getTextContent()),
                          BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR));
                } else if (nodeName.equalsIgnoreCase("customPlanetaryEvents")) {
                    //TODO: deal with this
                    updatePlanetaryEventsFromXML(workingNode);
                } else if (nodeName.equalsIgnoreCase("partsInUse")) {
                    processPartsInUse(campaign, workingNode, version);
                } else if (nodeName.equalsIgnoreCase("temporaryPrisonerCapacity")) {
                    campaign.setTemporaryPrisonerCapacity(MathUtility.parseInt(workingNode.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("processProcurement")) {
                    campaign.setProcessProcurement(Boolean.parseBoolean(workingNode.getTextContent().trim()));
                }
            }
            // If it's a text node or attribute or whatever at this level,
            // it's probably white-space.
            // We can safely ignore it even if it isn't, for now.
        }

        // Okay, after we've gone through all the nodes and constructed the
        // Campaign object...
        final CampaignOptions options = campaign.getCampaignOptions();

        // We need to do a post-process pass to restore a number of references.
        // Fix any Person ID References
        PersonIdReference.fixPersonIdReferences(campaign);

        // Fixup any ghost kills
        cleanupGhostKills(campaign);

        // Update the Personnel Modules
        campaign.setDivorce(options.getRandomDivorceMethod().getMethod(options));
        campaign.setMarriage(options.getRandomMarriageMethod().getMethod(options));
        campaign.setProcreation(options.getRandomProcreationMethod().getMethod(options));

        long timestamp = System.currentTimeMillis();

        // loop through forces to set force id
        for (Formation f : campaign.getAllForces()) {
            Scenario s = campaign.getScenario(f.getScenarioId());
            if (null != s && (null == f.getParentForce() || !f.getParentForce().isDeployed())) {
                s.addForces(f.getId());
            }
            // some units may need force id set for backwards compatibility
            // some may also need scenario id set
            for (UUID uid : f.getUnits()) {
                Unit u = campaign.getUnit(uid);
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
            Hashtable<Integer, CombatTeam> lances = campaign.getCombatTeamsAsMap();
            for (Formation f : campaign.getAllForces()) {
                if (!f.getUnits().isEmpty() && (null == lances.get(f.getId()))) {
                    lances.put(f.getId(), new CombatTeam(f.getId(), campaign));
                    LOGGER.warn("Added missing Lance {} to AtB list", f.getName());
                }
            }
        }

        LOGGER.info("[Campaign Load] Force IDs set in {}ms", System.currentTimeMillis() - timestamp);
        timestamp = System.currentTimeMillis();

        // Process parts...
        // Note: Units must have their Entities set prior to reaching this point!
        postProcessParts(campaign, version);

        LOGGER.info("[Campaign Load] Parts processed in {}ms", System.currentTimeMillis() - timestamp);
        timestamp = System.currentTimeMillis();

        LOGGER.info("[Campaign Load] Rank references fixed in {}ms", System.currentTimeMillis() - timestamp);
        timestamp = System.currentTimeMillis();

        // Okay, Units, need their pilot references fixed.
        campaign.getHangar().forEachUnit(unit -> {
            // Also, the unit should have its campaign set.
            unit.setCampaign(campaign);
            unit.fixReferences(campaign);

            if (null != unit.getRefit()) {
                unit.getRefit().fixReferences(campaign);

                unit.getRefit().reCalc();
                if (!unit.getRefit().isCustomJob() && !unit.getRefit().kitFound()) {
                    campaign.getShoppingList().addShoppingItemWithoutChecking(unit.getRefit());
                }
            }

            // lets make sure the force id set actually corresponds to a force
            // TODO: we have some reports of force id relics - need to fix
            if ((unit.getForceId() > 0) && (campaign.getForce(unit.getForceId()) == null)) {
                unit.setForceId(FORCE_NONE);
            }

            // It's annoying to have to do this, but this helps to ensure
            // that equipment numbers correspond to the right parts - its
            // possible that these might have changed if changes were made to
            // the ordering of equipment in the underlying data file for the unit.
            // We're not checking for refit here.
            final EquipmentUnscrambler unscrambler = EquipmentUnscrambler.create(unit);
            final EquipmentUnscramblerResult result = unscrambler.unscramble();
            if (!result.succeeded()) {
                LOGGER.warn(result.getMessage());
            }

            // some units might need to be assigned to scenarios
            Scenario s = campaign.getScenario(unit.getScenarioId());
            if (null != s) {
                // most units will be properly assigned through their
                // force, so check to make sure they aren't already here
                if (!s.isAssigned(unit, campaign)) {
                    s.addUnit(unit.getId());
                }
            }

            //Update the campaign transport availability if this is transport.
            //If it's empty we should be able to just ignore it
            for (CampaignTransportType campaignTransportType : CampaignTransportType.values()) {
                if (unit.hasTransportedUnits(campaignTransportType)) {
                    campaign.updateTransportInTransports(campaignTransportType, unit);
                }
            }
        });

        LOGGER.info("[Campaign Load] Pilot references fixed in {}ms", System.currentTimeMillis() - timestamp);
        timestamp = System.currentTimeMillis();

        boolean skipAllDeprecationChecks = false;
        boolean refundAllDeprecatedSkills = false;
        for (Person person : campaign.getPersonnel()) {
            // skill types might need resetting
            person.resetSkillTypes();

            // Seeing as we're already looping through all personnel, we might as well have the deprecation checks
            // here, too.
            if (!DEPRECATED_SKILLS.isEmpty() && !skipAllDeprecationChecks) {
                // This checks to ensure the character doesn't have any Deprecated skills.
                SkillDeprecationTool deprecationTool = new SkillDeprecationTool(campaign,
                      person,
                      refundAllDeprecatedSkills);
                skipAllDeprecationChecks = deprecationTool.isSkipAll();
                refundAllDeprecatedSkills = deprecationTool.isRefundAll();
            }

            // Self-correct any invalid personnel statuses (handles <50.05 campaigns)
            // Any characters with invalid statuses will have their status set to 'Active'
            if (person.getPrisonerStatus().isCurrentPrisoner()) {
                statusValidator(campaign, person, true);
            }

            // <50.10 compatibility handler
            LocalDate today = campaign.getLocalDate();
            if (Person.updateSkillsForVehicleProfessions(today, person, person.getPrimaryRole(), true) ||
                      Person.updateSkillsForVehicleProfessions(today, person, person.getSecondaryRole(), false)) {
                String report = getFormattedTextAt(RESOURCE_BUNDLE, "vehicleProfessionSkillChange",
                      spanOpeningWithCustomColor(getWarningColor()),
                      CLOSING_SPAN_TAG,
                      person.getHyperlinkedFullTitle());
                campaign.addReport(GENERAL, report);
            }

            // This resolves a bug squashed in 2025 (50.03) but lurked in our codebase
            // potentially as far back as 2014. The next two handlers should never be removed.
            if (!person.canPerformRole(today, person.getSecondaryRole(), false)) {
                person.setSecondaryRole(PersonnelRole.NONE);

                campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE, "ineligibleForSecondaryRole",
                      spanOpeningWithCustomColor(getWarningColor()),
                      CLOSING_SPAN_TAG,
                      person.getHyperlinkedFullTitle()));
            }

            if (!person.canPerformRole(today, person.getPrimaryRole(), true)) {
                person.setPrimaryRole(campaign.getLocalDate(), PersonnelRole.DEPENDENT);

                campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE, "ineligibleForPrimaryRole",
                      spanOpeningWithCustomColor(getNegativeColor()),
                      CLOSING_SPAN_TAG,
                      person.getHyperlinkedFullTitle()));
            }
        }

        campaign.getHangar().forEachUnit(unit -> {
            // Some units have been incorrectly assigned a null C3UUID as a string. This
            // should
            // correct that by setting a new C3UUID
            if ((unit.getEntity().hasC3() || unit.getEntity().hasC3i() || unit.getEntity().hasNavalC3()) &&
                      (unit.getEntity().getC3UUIDAsString() == null ||
                             unit.getEntity().getC3UUIDAsString().equals("null"))) {
                unit.getEntity().setC3UUID();
                unit.getEntity().setC3NetIdSelf();
            }

            // This needs to be down here so that it can factor in any changes made to personnel prior to this point.
            unit.resetPilotAndEntity();
        });
        campaign.refreshNetworks();

        LOGGER.info("[Campaign Load] C3 networks refreshed in {}ms", System.currentTimeMillis() - timestamp);
        timestamp = System.currentTimeMillis();

        // This removes the risk of having forces with invalid leadership getting locked in
        for (Formation formation : campaign.getAllForces()) {
            formation.updateCommander(campaign);
        }

        // ok, once we are sure that campaign has been set for all units, we can
        // now go through and initializeParts and run diagnostics
        List<Unit> removeUnits = new ArrayList<>();
        campaign.getHangar().forEachUnit(unit -> {
            // just in case parts are missing (i.e. because they weren't tracked
            // in previous versions)
            unit.initializeParts(true);
            unit.runDiagnostic(false);
            if (!unit.isRepairable()) {
                if (!unit.hasSalvageableParts()) {
                    // we shouldn't get here but some units seem to stick around
                    // for some reason
                    removeUnits.add(unit);
                } else {
                    unit.setSalvage(true);
                }
            }

            List<String> reports = unit.checkForOverCrewing();
            for (String report : reports) {
                campaign.addReport(GENERAL, report);
            }
        });

        for (Unit unit : removeUnits) {
            campaign.removeUnit(unit.getId());
        }

        LOGGER.info("[Campaign Load] Units initialized in {}ms", System.currentTimeMillis() - timestamp);
        timestamp = System.currentTimeMillis();

        for (Person person : campaign.getPersonnel()) {
            person.fixReferences(campaign);
        }

        LOGGER.info("[Campaign Load] Personnel initialized in {}ms", System.currentTimeMillis() - timestamp);
        timestamp = System.currentTimeMillis();

        campaign.reloadNews();

        LOGGER.info("[Campaign Load] News loaded in {}ms", System.currentTimeMillis() - timestamp);
        timestamp = System.currentTimeMillis();

        // If we don't have a personnel market, create one.
        if (!foundPersonnelMarket) {
            campaign.setPersonnelMarket(new PersonnelMarket(campaign));
        }

        if (!foundContractMarket) {
            campaign.setContractMarket(new AtbMonthlyContractMarket());
        }

        if (!foundUnitMarket) {
            campaign.setUnitMarket(campaign.getCampaignOptions().getUnitMarketMethod().getUnitMarket());
        }

        if (null == campaign.getRetirementDefectionTracker()) {
            campaign.setRetirementDefectionTracker(new RetirementDefectionTracker());
        }

        if (campaign.getCampaignOptions().isUseAtB()) {
            campaign.setHasActiveContract();
            campaign.setAtBConfig(AtBConfiguration.loadFromXml());
            campaign.setAtBEventProcessor(new AtBEventProcessor(campaign));
        }

        // Sanity Checks
        fixupUnitTechProblems(campaign);

        // unload any ammo bins in the warehouse
        List<AmmoBin> binsToUnload = new ArrayList<>();
        campaign.getWarehouse().forEachSparePart(prt -> {
            if (prt instanceof AmmoBin && !prt.isReservedForRefit() && ((AmmoBin) prt).getShotsNeeded() == 0) {
                binsToUnload.add((AmmoBin) prt);
            }
        });
        for (AmmoBin bin : binsToUnload) {
            bin.unload();
        }

        LOGGER.info("[Campaign Load] Ammo bins cleared in {}ms", System.currentTimeMillis() - timestamp);
        timestamp = System.currentTimeMillis();

        // Check all parts that are reserved for refit and if the refit id unit
        // is not refitting or is gone then un-reserve
        for (Part part : campaign.getWarehouse().getParts()) {
            if (part.isReservedForRefit()) {
                Unit u = part.getRefitUnit();
                if ((null == u) || !u.isRefitting()) {
                    part.setRefitUnit(null);
                }
            }
        }

        LOGGER.info("[Campaign Load] Reserved refit parts fixed in {}ms", System.currentTimeMillis() - timestamp);
        timestamp = System.currentTimeMillis();

        // Build a new, clean warehouse from the current parts
        Warehouse warehouse = new Warehouse();
        for (Part part : campaign.getWarehouse().getParts()) {
            // < 50.08 compatibility handler
            if (part instanceof SVArmor svArmor) {
                final int PROHIBITED_BAR_RATING = 0;

                int bar = svArmor.getBAR();
                if (bar == PROHIBITED_BAR_RATING) {
                    LOGGER.info("Discarding untracked BAR 0 armor");
                    continue;
                }
            }

            warehouse.addPart(part, true);
        }

        // This will have aggregated all the possible spare parts together
        campaign.setWarehouse(warehouse);

        LOGGER.info("[Campaign Load] Warehouse cleaned up in {}ms", System.currentTimeMillis() - timestamp);

        campaign.setUnitRating(null);

        // this is used to handle characters from pre-50.01 campaigns
        campaign.getPersonnel().stream().filter(person -> person.getJoinedCampaign() == null).forEach(person -> {
            if (person.getRecruitment() != null) {
                person.setJoinedCampaign(person.getRecruitment());
                LOGGER.info(
                      "{} doesn't have a date recorded showing when they joined the campaign. Using recruitment date.",
                      person.getFullTitle());
            } else {
                person.setJoinedCampaign(campaign.getLocalDate());
                LOGGER.info("{} doesn't have a date recorded showing when they joined the campaign. Using current date.",
                      person.getFullTitle());
            }
        });

        // Reset Random Death to match current campaign options
        campaign.resetRandomDeath();

        // Fix sexual preferences
        if (version.isLowerThan(new Version("0.50.10"))) {
            correctSexualPreferencesForCurrentSpouse(campaign.getPersonnel());
        }

        LOGGER.info("Load of campaign file complete!");

        return campaign;
    }

    /**
     * This will fixup unit-tech problems seen in some save games, such as techs having been double-assigned or being
     * assigned to mothballed units.
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
                    LOGGER.warn("Tech {} {} {} (fixed)", tech.getFullName(), reason, unitDesc);
                }
            }
        }
    }

    /**
     * Pulled out purely for encapsulation. Makes the code neater and easier to read.
     *
     * @param campaign   The Campaign object that is being populated.
     * @param parentNode The XML node we're working from.
     *
     */
    private static void processInfoNode(Campaign campaign, Node parentNode, Version version) throws DOMException {
        NodeList childNodes = parentNode.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < childNodes.getLength(); x++) {
            Node childNode = childNodes.item(x);
            int nodeType = childNode.getNodeType();

            // If it's not an element, again, we're ignoring it.
            if (nodeType != Node.ELEMENT_NODE) {
                continue;
            }
            String nodeName = childNode.getNodeName();

            try {
                if (nodeName.equalsIgnoreCase("calendar")) {
                    campaign.setLocalDate(MHQXMLUtility.parseDate(childNode.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase(Camouflage.XML_TAG)) {
                    campaign.setCamouflage(Camouflage.parseFromXML(childNode));
                } else if (nodeName.equalsIgnoreCase("camoCategory")) {
                    String val = childNode.getTextContent().trim();

                    if (!val.equals("null")) {
                        campaign.getCamouflage().setCategory(val);
                    }
                } else if (nodeName.equalsIgnoreCase("camoFileName")) {
                    String val = childNode.getTextContent().trim();

                    if (!val.equals("null")) {
                        campaign.getCamouflage().setFilename(val);
                    }
                } else if (nodeName.equalsIgnoreCase("colour")) {
                    campaign.setColour(PlayerColour.parseFromString(childNode.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase(UnitIcon.XML_TAG)) {
                    campaign.setUnitIcon(UnitIcon.parseFromXML(childNode));
                } else if (nodeName.equalsIgnoreCase("nameGen")) {
                    // First, get all the child nodes;
                    NodeList nl2 = childNode.getChildNodes();
                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);
                        if (wn2.getParentNode() != childNode) {
                            continue;
                        }
                        if (wn2.getNodeName().equalsIgnoreCase("faction")) {
                            RandomNameGenerator.getInstance().setChosenFaction(wn2.getTextContent().trim());
                        } else if (wn2.getNodeName().equalsIgnoreCase("percentFemale")) {
                            RandomGenderGenerator.setPercentFemale(MathUtility.parseInt(wn2.getTextContent().trim(),
                                  50));
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("currentReport")) {
                    // First, get all the child nodes;
                    NodeList nl2 = childNode.getChildNodes();

                    // Then, make sure the report is empty. *just* in case.
                    // ...That is, creating a new campaign throws in a date line
                    // for us...
                    // So make sure it's cleared out.
                    campaign.getCurrentReport().clear();

                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);

                        if (wn2.getParentNode() != childNode) {
                            continue;
                        }

                        if (wn2.getNodeName().equalsIgnoreCase("reportLine")) {
                            campaign.getCurrentReport().add(wn2.getTextContent());
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("skillReport")) {
                    // First, get all the child nodes;
                    NodeList nl2 = childNode.getChildNodes();

                    // Then, make sure the report is empty. *just* in case.
                    // ...That is, creating a new campaign throws in a date line
                    // for us...
                    // So make sure it's cleared out.
                    campaign.getSkillReport().clear();

                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);

                        if (wn2.getParentNode() != childNode) {
                            continue;
                        }

                        if (wn2.getNodeName().equalsIgnoreCase("reportLine")) {
                            campaign.getSkillReport().add(wn2.getTextContent());
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("battleReport")) {
                    // First, get all the child nodes;
                    NodeList nl2 = childNode.getChildNodes();

                    // Then, make sure the report is empty. *just* in case.
                    // ...That is, creating a new campaign throws in a date line
                    // for us...
                    // So make sure it's cleared out.
                    campaign.getBattleReport().clear();

                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);

                        if (wn2.getParentNode() != childNode) {
                            continue;
                        }

                        if (wn2.getNodeName().equalsIgnoreCase("reportLine")) {
                            campaign.getBattleReport().add(wn2.getTextContent());
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("politicsReport")) {
                    // First, get all the child nodes;
                    NodeList nl2 = childNode.getChildNodes();

                    // Then, make sure the report is empty. *just* in case.
                    // ...That is, creating a new campaign throws in a date line
                    // for us...
                    // So make sure it's cleared out.
                    campaign.getPoliticsReport().clear();

                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);

                        if (wn2.getParentNode() != childNode) {
                            continue;
                        }

                        if (wn2.getNodeName().equalsIgnoreCase("reportLine")) {
                            campaign.getPoliticsReport().add(wn2.getTextContent());
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("personnelReport")) {
                    // First, get all the child nodes;
                    NodeList nl2 = childNode.getChildNodes();

                    // Then, make sure the report is empty. *just* in case.
                    // ...That is, creating a new campaign throws in a date line
                    // for us...
                    // So make sure it's cleared out.
                    campaign.getPersonnelReport().clear();

                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);

                        if (wn2.getParentNode() != childNode) {
                            continue;
                        }

                        if (wn2.getNodeName().equalsIgnoreCase("reportLine")) {
                            campaign.getPersonnelReport().add(wn2.getTextContent());
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("medicalReport")) {
                    // First, get all the child nodes;
                    NodeList nl2 = childNode.getChildNodes();

                    // Then, make sure the report is empty. *just* in case.
                    // ...That is, creating a new campaign throws in a date line
                    // for us...
                    // So make sure it's cleared out.
                    campaign.getMedicalReport().clear();

                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);

                        if (wn2.getParentNode() != childNode) {
                            continue;
                        }

                        if (wn2.getNodeName().equalsIgnoreCase("reportLine")) {
                            campaign.getMedicalReport().add(wn2.getTextContent());
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("acquisitionsReport")) {
                    // First, get all the child nodes;
                    NodeList nl2 = childNode.getChildNodes();

                    // Then, make sure the report is empty. *just* in case.
                    // ...That is, creating a new campaign throws in a date line
                    // for us...
                    // So make sure it's cleared out.
                    campaign.getAcquisitionsReport().clear();

                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);

                        if (wn2.getParentNode() != childNode) {
                            continue;
                        }

                        if (wn2.getNodeName().equalsIgnoreCase("reportLine")) {
                            campaign.getAcquisitionsReport().add(wn2.getTextContent());
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("financesReport")) {
                    // First, get all the child nodes;
                    NodeList nl2 = childNode.getChildNodes();

                    // Then, make sure the report is empty. *just* in case.
                    // ...That is, creating a new campaign throws in a date line
                    // for us...
                    // So make sure it's cleared out.
                    campaign.getFinancesReport().clear();

                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);

                        if (wn2.getParentNode() != childNode) {
                            continue;
                        }

                        if (wn2.getNodeName().equalsIgnoreCase("reportLine")) {
                            campaign.getFinancesReport().add(wn2.getTextContent());
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("technicalReport")) {
                    // First, get all the child nodes;
                    NodeList nl2 = childNode.getChildNodes();

                    // Then, make sure the report is empty. *just* in case.
                    // ...That is, creating a new campaign throws in a date line
                    // for us...
                    // So make sure it's cleared out.
                    campaign.getTechnicalReport().clear();

                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);

                        if (wn2.getParentNode() != childNode) {
                            continue;
                        }

                        if (wn2.getNodeName().equalsIgnoreCase("reportLine")) {
                            campaign.getTechnicalReport().add(wn2.getTextContent());
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("faction")) {
                    Faction faction = Factions.getInstance().getFaction(childNode.getTextContent());
                    campaign.setFaction(faction);
                } else if (nodeName.equalsIgnoreCase("retainerEmployerCode")) {
                    campaign.setRetainerEmployerCode(childNode.getTextContent());
                } else if (nodeName.equalsIgnoreCase("retainerStartDate")) {
                    campaign.setRetainerStartDate(LocalDate.parse(childNode.getTextContent()));
                } else if (nodeName.equalsIgnoreCase("crimeRating")) {
                    campaign.setCrimeRating(MathUtility.parseInt(childNode.getTextContent()));
                } else if (nodeName.equalsIgnoreCase("initiativeBonus")) {
                    campaign.setInitiativeBonus(MathUtility.parseInt(childNode.getTextContent()));
                } else if (nodeName.equalsIgnoreCase("initiativeMaxBonus")) {
                    campaign.setInitiativeMaxBonus(MathUtility.parseInt(childNode.getTextContent(), 1));
                } else if (nodeName.equalsIgnoreCase("crimePirateModifier")) {
                    campaign.setCrimePirateModifier(MathUtility.parseInt(childNode.getTextContent()));
                } else if (nodeName.equalsIgnoreCase("dateOfLastCrime")) {
                    campaign.setDateOfLastCrime(LocalDate.parse(childNode.getTextContent()));
                } else if (nodeName.equalsIgnoreCase("reputation")) {
                    campaign.setReputation(new ReputationController().generateInstanceFromXML(childNode));
                } else if (nodeName.equalsIgnoreCase("newPersonnelMarket")) {
                    campaign.setNewPersonnelMarket(generatePersonnelMarketDataFromXML(campaign, childNode, version));
                } else if (nodeName.equalsIgnoreCase("factionStandings")) {
                    campaign.setFactionStandings(FactionStandings.generateInstanceFromXML(childNode));
                } else if (nodeName.equalsIgnoreCase("rankSystem")) {
                    if (!childNode.hasChildNodes()) { // we need there to be child nodes to parse from
                        continue;
                    }
                    final RankSystem rankSystem = RankSystem.generateInstanceFromXML(childNode.getChildNodes(),
                          version);
                    // If the system is valid (either not campaign or validates), set it. Otherwise,
                    // keep the default
                    if (!rankSystem.getType().isCampaign() || new RankValidator().validate(rankSystem, true)) {
                        campaign.setRankSystemDirect(rankSystem);
                    }
                } else if (nodeName.equalsIgnoreCase("gmMode")) {
                    campaign.setGMMode(Boolean.parseBoolean(childNode.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("showOverview")) {
                    campaign.setOverviewLoadingValue(Boolean.parseBoolean(childNode.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("name")) {
                    String val = childNode.getTextContent().trim();

                    if (val.equals("null")) {
                        campaign.setName(null);
                    } else {
                        campaign.setName(val);
                    }
                } else if (nodeName.equalsIgnoreCase("campaignStartDate")) {
                    String campaignStartDate = childNode.getTextContent().trim();

                    if (campaignStartDate.equals("null")) {
                        campaign.setCampaignStartDate(null);
                    } else {
                        campaign.setCampaignStartDate(LocalDate.parse(campaignStartDate));
                    }
                } else if (nodeName.equalsIgnoreCase("overtime")) {
                    campaign.setOvertime(Boolean.parseBoolean(childNode.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("astechPool")) {
                    campaign.setAsTechPool(MathUtility.parseInt(childNode.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("astechPoolMinutes")) {
                    campaign.setAsTechPoolMinutes(MathUtility.parseInt(childNode.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("astechPoolOvertime")) {
                    campaign.setAsTechPoolOvertime(MathUtility.parseInt(childNode.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("medicPool")) {
                    campaign.setMedicPool(MathUtility.parseInt(childNode.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("fieldKitchenWithinCapacity")) {
                    campaign.setFieldKitchenWithinCapacity(Boolean.parseBoolean(childNode.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("mashTheatreCapacity")) {
                    campaign.setMashTheatreCapacity(MathUtility.parseInt(childNode.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("repairBaysRented")) {
                    campaign.setRepairBaysRented(MathUtility.parseInt(childNode.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("id")) {
                    campaign.setId(UUID.fromString(childNode.getTextContent().trim()));
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }

        // Update daily reports
        campaign.setCurrentReportHTML(Utilities.combineString(campaign.getCurrentReport(), Campaign.REPORT_LINEBREAK));
        List<String> newReports = new ArrayList<>(campaign.getCurrentReport().size() * 2);
        boolean firstGeneralReport = true;
        for (String report : campaign.getCurrentReport()) {
            if (firstGeneralReport) {
                firstGeneralReport = false;
            } else {
                newReports.add(Campaign.REPORT_LINEBREAK);
            }
            newReports.add(report);
        }
        campaign.setNewReports(newReports);

        campaign.setSkillReportHTML(Utilities.combineString(campaign.getSkillReport(), Campaign.REPORT_LINEBREAK));
        List<String> newSkillReports = new ArrayList<>(campaign.getSkillReport().size() * 2);
        boolean firstSkillReport = true;
        for (String report : campaign.getSkillReport()) {
            if (firstSkillReport) {
                firstSkillReport = false;
            } else {
                newSkillReports.add(Campaign.REPORT_LINEBREAK);
            }
            newSkillReports.add(report);
        }
        campaign.setNewSkillReports(newSkillReports);

        campaign.setBattleReportHTML(Utilities.combineString(campaign.getBattleReport(), Campaign.REPORT_LINEBREAK));
        List<String> newBattleReports = new ArrayList<>(campaign.getBattleReport().size() * 2);
        boolean firstBattleReport = true;
        for (String report : campaign.getBattleReport()) {
            if (firstBattleReport) {
                firstBattleReport = false;
            } else {
                newBattleReports.add(Campaign.REPORT_LINEBREAK);
            }
            newBattleReports.add(report);
        }
        campaign.setNewBattleReports(newBattleReports);

        campaign.setPoliticsReportHTML(Utilities.combineString(campaign.getPoliticsReport(),
              Campaign.REPORT_LINEBREAK));
        List<String> newPoliticsReports = new ArrayList<>(campaign.getPoliticsReport().size() * 2);
        boolean firstPoliticsReport = true;
        for (String report : campaign.getPoliticsReport()) {
            if (firstPoliticsReport) {
                firstPoliticsReport = false;
            } else {
                newPoliticsReports.add(Campaign.REPORT_LINEBREAK);
            }
            newPoliticsReports.add(report);
        }
        campaign.setNewPoliticsReports(newPoliticsReports);

        campaign.setPersonnelReportHTML(Utilities.combineString(campaign.getPersonnelReport(),
              Campaign.REPORT_LINEBREAK));
        List<String> newPersonnelReports = new ArrayList<>(campaign.getPersonnelReport().size() * 2);
        boolean firstPersonnelReport = true;
        for (String report : campaign.getPersonnelReport()) {
            if (firstPersonnelReport) {
                firstPersonnelReport = false;
            } else {
                newPersonnelReports.add(Campaign.REPORT_LINEBREAK);
            }
            newPersonnelReports.add(report);
        }
        campaign.setNewPersonnelReports(newPersonnelReports);

        campaign.setMedicalReportHTML(Utilities.combineString(campaign.getMedicalReport(), Campaign.REPORT_LINEBREAK));
        List<String> newMedicalReports = new ArrayList<>(campaign.getMedicalReport().size() * 2);
        boolean firstMedicalReport = true;
        for (String report : campaign.getMedicalReport()) {
            if (firstMedicalReport) {
                firstMedicalReport = false;
            } else {
                newMedicalReports.add(Campaign.REPORT_LINEBREAK);
            }
            newMedicalReports.add(report);
        }
        campaign.setNewMedicalReports(newMedicalReports);

        campaign.setFinancesReportHTML(Utilities.combineString(campaign.getFinancesReport(),
              Campaign.REPORT_LINEBREAK));
        List<String> newFinancesReports = new ArrayList<>(campaign.getFinancesReport().size() * 2);
        boolean firstFinancesReport = true;
        for (String report : campaign.getFinancesReport()) {
            if (firstFinancesReport) {
                firstFinancesReport = false;
            } else {
                newFinancesReports.add(Campaign.REPORT_LINEBREAK);
            }
            newFinancesReports.add(report);
        }
        campaign.setNewFinancesReports(newFinancesReports);

        campaign.setAcquisitionsReportHTML(Utilities.combineString(campaign.getAcquisitionsReport(),
              Campaign.REPORT_LINEBREAK));
        List<String> newAcquisitionsReports = new ArrayList<>(campaign.getAcquisitionsReport().size() * 2);
        boolean firstAcquisitionsReport = true;
        for (String report : campaign.getAcquisitionsReport()) {
            if (firstAcquisitionsReport) {
                firstAcquisitionsReport = false;
            } else {
                newAcquisitionsReports.add(Campaign.REPORT_LINEBREAK);
            }
            newAcquisitionsReports.add(report);
        }
        campaign.setNewAcquisitionsReports(newAcquisitionsReports);

        campaign.setTechnicalReportHTML(Utilities.combineString(campaign.getTechnicalReport(),
              Campaign.REPORT_LINEBREAK));
        List<String> newTechnicalReports = new ArrayList<>(campaign.getTechnicalReport().size() * 2);
        boolean firstTechnicalReport = true;
        for (String report : campaign.getTechnicalReport()) {
            if (firstTechnicalReport) {
                firstTechnicalReport = false;
            } else {
                newTechnicalReports.add(Campaign.REPORT_LINEBREAK);
            }
            newTechnicalReports.add(report);
        }
        campaign.setNewTechnicalReports(newTechnicalReports);
    }

    private static void processCombatTeamNodes(Campaign campaign, Node workingNode) {
        NodeList workingNodes = workingNode.getChildNodes();

        // Okay, let's iterate through the children, eh?
        for (int x = 0; x < workingNodes.getLength(); x++) {
            Node wn2 = workingNodes.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("lance") && !wn2.getNodeName().equalsIgnoreCase("combatTeam")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                LOGGER.error("Unknown node type not loaded in combatTeam nodes: {}", wn2.getNodeName());
                continue;
            }

            CombatTeam combatTeam = CombatTeam.generateInstanceFromXML(wn2);

            if (combatTeam != null) {
                campaign.addCombatTeam(combatTeam);
            }
        }
    }

    /**
     * Processes the child nodes of a given XML node to extract and register past version information in the specified
     * campaign.
     * <p>
     * This method iterates through all child nodes of the supplied {@code workingNode}, identifies elements named
     * "pastVersion", and creates {@link Version} objects from their text content. Each parsed version is added to the
     * campaign's list of past versions if it is not already present. Unknown node types encountered in this context are
     * logged as errors.
     * <p>
     * After processing, if the campaign's list of past versions is empty, a warning is logged. The method also ensures
     * the current application version is included in the list if it was not already present.
     *
     * @param campaign    the {@link Campaign} instance to be updated with past version information
     * @param workingNode the XML {@link Node} whose child nodes contain past version data to be processed
     */
    private static void processPastVersionNodes(Campaign campaign, Node workingNode) {
        NodeList childNodes = workingNode.getChildNodes();

        // Iterate through the children (past versions)
        for (int x = 0; x < childNodes.getLength(); x++) {
            Node childNode = childNodes.item(x);

            // If it's not an element node, we ignore it.
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            // If the node name isn't correct, we ignore it.
            if (!childNode.getNodeName().equalsIgnoreCase("pastVersion")) {
                LOGGER.error("Incorrect node loaded in Past Version nodes: {}", childNode.getNodeName());
                continue;
            }

            // Otherwise, we add it to the list of past versions
            Version pastVersion = new Version(childNode.getTextContent());
            if (!campaign.getPastVersions().contains(pastVersion)) {
                campaign.addPastVersion(pastVersion);
            }
        }
        List<Version> pastVersions = campaign.getPastVersions();
        if (pastVersions.isEmpty()) {
            LOGGER.info("No past versions found in campaign file.");
        }

        // Add the current version (if it's missing)
        if (!pastVersions.contains(MHQConstants.VERSION)) {
            LOGGER.info("Current version {} not found in past versions list. Adding it.", MHQConstants.VERSION);
            campaign.addPastVersion(MHQConstants.VERSION);
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
        LOGGER.info("Loading Finances from XML...");
        retVal.setFinances(Finances.generateInstanceFromXML(wn));
        LOGGER.info("Load of Finances complete!");
    }

    private static void processForces(Campaign retVal, Node wn, Version version) {
        LOGGER.info("Loading Force Organization from XML...");

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
                LOGGER.error("Unknown node type not loaded in Forces nodes: {}", wn2.getNodeName());

                continue;
            }

            if (!foundForceAlready) {
                Formation f = Formation.generateInstanceFromXML(wn2, retVal, version);
                if (null != f) {
                    retVal.setForces(f);
                    foundForceAlready = true;
                }
            } else {
                LOGGER.error("More than one type-level force found");
            }
        }

        recalculateCombatTeams(retVal);
        LOGGER.info("Load of Force Organization complete!");
    }

    private static void processPersonnelNodes(Campaign campaign, Node wn, Version version) {
        LOGGER.info("Loading Personnel Nodes from XML...");

        NodeList wList = wn.getChildNodes();

        // Okay, let's iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("person")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                LOGGER.error("Unknown node type not loaded in Personnel nodes: {}", wn2.getNodeName());

                continue;
            }

            Person p = Person.generateInstanceFromXML(wn2, campaign, version);

            if (p != null) {
                campaign.importPerson(p);

                // <50.10 compatibility handler (moves old SPA-based Edge to current Attribute-based
                performEdgeConversion(campaign, p);
            }
        }

        // this block verifies all in-use academies are valid
        List<String> missingList = new ArrayList<>();

        for (Person person : campaign.getPersonnel()) {
            String academySet = person.getEduAcademySet();
            String academyNameInSet = person.getEduAcademyNameInSet();

            if ((academyNameInSet != null) && (EducationController.getAcademy(academySet, academyNameInSet) == null)) {
                String message = academyNameInSet + " from set " + academySet;
                if ((!missingList.contains(message)) && (!missingList.contains('\n' + message))) {
                    missingList.add((missingList.isEmpty() ? "" : "\n") + message);
                }
            }
        }

        if (!missingList.isEmpty()) {
            throw new NullPointerException(missingList.toString());
        }

        LOGGER.info("Load Personnel Nodes Complete!");
    }

    private static void performEdgeConversion(Campaign campaign, Person person) {
        for (Enumeration<IOption> i = person.getOptions().getOptions(); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (OptionsConstants.EDGE.equals(ability.getName())) {
                Object object = ability.getValue();
                if (object instanceof Integer oldEdge) {
                    // Either we've already converted, or there is nothing to convert. Regardless, we're done here.
                    if (oldEdge == 0) {
                        return;
                    }

                    person.setAttributeScore(SkillAttribute.EDGE, oldEdge);
                    int newEdge = person.getAttributeScore(SkillAttribute.EDGE);
                    int difference = oldEdge - newEdge;
                    if (difference > 0) { // We were unable to convert some over
                        int edgeCost = campaign.getCampaignOptions().getEdgeCost();
                        int rebate = edgeCost * difference;
                        person.awardXP(campaign, rebate);
                        campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE,
                              "CampaignXmlParser.compatibility.edge",
                              spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG,
                              person.getHyperlinkedFullTitle(), difference, rebate));
                    }

                    person.setCurrentEdge(person.getEdge()); // We're resetting everyone's Edge as a kindness
                    ability.setValue(0); // This is our marker that conversion has been done.
                } else {
                    LOGGER.error("Unknown Object type {} loaded into Edge compatibility handler from {}",
                          object.getClass().getSimpleName(), ability.getName());
                }
                return;
            }
        }
    }

    /**
     * Ensures that married personnel have sexual preferences compatible with their current spouse.
     *
     * <p>This method iterates through all personnel and, for those who are married, updates their romantic
     * preferences to include their spouse's gender. This is used to bring campaigns older than 0.50.10 up to date with
     * the new sexuality tracking.</p>
     *
     * <p><b>Note A:</b> This method adds to existing preferences rather than replacing them, allowing characters
     * to remain bisexual if they were previously attracted to multiple genders.</p>
     *
     * <p><b>Note B:</b> This approach has to be used, rather than self-correcting during person-load, as the spouse
     * may not have been substantiated when person is loaded.</p>
     * </p>
     *
     * @param personnel the collection of {@link Person} objects to process
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void correctSexualPreferencesForCurrentSpouse(Collection<Person> personnel) {
        for (Person person : personnel) {
            Person spouse = person.getGenealogy().getSpouse();

            if (spouse != null) {
                Gender spouseGender = spouse.getGender();

                if (spouseGender.isMale()) { // the Male/Female checks include n.b. persons
                    person.setPrefersMen(true);
                } else if (spouseGender.isFemale()) {
                    person.setPrefersWomen(true);
                }
            }
        }
    }

    private static void processSkillTypeNodes(Node wn, Version version) {
        LOGGER.info("Loading Skill Type Nodes from XML...");

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
                LOGGER.error("Unknown node type not loaded in Skill Type nodes: {}", wn2.getNodeName());
                continue;
            }

            // TODO: make SkillType a Campaign instance
            SkillType.generateInstanceFromXML(wn2, version);
        }

        LOGGER.info("Load Skill Type Nodes Complete!");
    }

    private static void processStoryArcNodes(Campaign retVal, Node wn, Version version) {
        LOGGER.info("Loading Story Arc Nodes from XML...");

        StoryArc storyArc = StoryArc.parseFromXML(wn.getChildNodes(), retVal, version);
        if (storyArc != null) {
            MekHQ.registerHandler(storyArc);
            retVal.useStoryArc(storyArc, false);
        }
    }

    /**
     * Processes a list of personnel who advanced in experience points (XP) from a given XML node.
     * <p>
     * This method reads the child nodes of the provided XML {@code workingNode} and extracts the personnel listed under
     * the "personWhoAdvancedInXP" nodes. It retrieves the corresponding {@link Person} objects from the provided
     * {@link Campaign} using their unique UUIDs. If a person cannot be found, an error is logged. The method returns a
     * list of processed {@link Person} objects.
     * </p>
     *
     * @param workingNode The XML node containing the "personWhoAdvancedInXP" elements to be processed.
     * @param campaign    The {@link Campaign} instance used to fetch the {@link Person} objects based on UUIDs.
     *
     * @return A {@link List} of {@link Person} objects representing the personnel who advanced in XP. If no valid
     *       personnel are found, an empty list is returned.
     */
    private static List<Person> processPersonnelWhoAdvancedInXP(Node workingNode, Campaign campaign) {
        LOGGER.info("Loading personnelWhoAdvancedInXP Nodes from XML...");

        List<Person> personWhoAdvancedInXP = new ArrayList<>();

        NodeList workingList = workingNode.getChildNodes();
        for (int x = 0; x < workingList.getLength(); x++) {
            Node childNode = workingList.item(x);

            // If it's not an element node, we ignore it.
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!childNode.getNodeName().equalsIgnoreCase("personWhoAdvancedInXP")) {
                LOGGER.error("Unknown node type not loaded in personnelWhoAdvancedInXP nodes: {}",
                      childNode.getNodeName());
                continue;
            }

            Person person = campaign.getPerson(UUID.fromString(childNode.getTextContent()));

            if (person == null) {
                LOGGER.error("Unknown UUID: {}", childNode.getTextContent());
            }

            personWhoAdvancedInXP.add(person);
        }

        LOGGER.info("Load personWhoAdvancedInXP Nodes Complete!");
        return personWhoAdvancedInXP;
    }

    private static List<UUID> processAutomatedMothballNodes(Node workingNode) {
        LOGGER.info("Loading Automated Mothball Nodes from XML...");

        List<UUID> mothballedUnits = new ArrayList<>();

        NodeList workingList = workingNode.getChildNodes();
        for (int x = 0; x < workingList.getLength(); x++) {
            Node childNode = workingList.item(x);

            // If it's not an element node, we ignore it.
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!childNode.getNodeName().equalsIgnoreCase("mothballedUnit")) {
                LOGGER.error("Unknown node type not loaded in Automated Mothball nodes: {}", childNode.getNodeName());
                continue;
            }

            try {
                UUID unitId = UUID.fromString(childNode.getTextContent());
                mothballedUnits.add(unitId);
            } catch (IllegalArgumentException iae) {
                LOGGER.error("Invalid UUID: {}", childNode.getTextContent());
            }
        }

        LOGGER.info("Load Automated Mothball Nodes Complete!");
        return mothballedUnits;
    }

    private static void processSpecialAbilityNodes(Campaign retVal, Node wn, Version version) {
        LOGGER.info("Loading Special Ability Nodes from XML...");

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
                LOGGER.error("Unknown node type not loaded in Special Ability nodes: {}", wn2.getNodeName());
                continue;
            }
            SpecialAbility.generateInstanceFromCampaignXML(wn2, options, version);
        }

        LOGGER.info("Load Special Ability Nodes Complete!");
    }

    private static void processKillNodes(Campaign retVal, Node wn, Version version) {
        LOGGER.info("Loading Kill Nodes from XML...");

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
                LOGGER.error("Unknown node type not loaded in Kill nodes: {}", wn2.getNodeName());
                continue;
            }

            Kill kill = Kill.generateInstanceFromXML(wn2, version);
            if (kill != null) {
                retVal.importKill(kill);
            }
        }

        LOGGER.info("Load Kill Nodes Complete!");
    }

    /**
     * Processes a custom unit in a campaign.
     *
     * @param retVal The {@see Campaign} being parsed.
     * @param wn     The current XML element representing a custom unit.
     *
     * @return A value indicating whether a new custom unit file was added to disk.
     */
    private static boolean processCustom(Campaign retVal, Node wn) {
        String sCustomsDir = "data" +
                                   File.separator +
                                   "mekfiles" +
                                   File.separator +
                                   "customs"; // TODO : Remove inline file path
        String sCustomsDirCampaign = sCustomsDir + File.separator + retVal.getName();
        File customsDir = new File(sCustomsDir);
        if (!customsDir.exists()) {
            if (!customsDir.mkdir()) {
                LOGGER.error("Failed to create directory {}, and therefore cannot save the unit.", sCustomsDir);
                return false;
            }
        }
        File customsDirCampaign = new File(sCustomsDirCampaign);
        if (!customsDirCampaign.exists()) {
            if (!customsDirCampaign.mkdir()) {
                LOGGER.error("Failed to create directory {}, and therefore cannot save the unit.", sCustomsDirCampaign);
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

            // If this file already exists then don't overwrite it, or we will end up with a
            // bunch of copies
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
        LOGGER.info("Writing custom unit from inline data to {}", fileName);

        try (OutputStream out = new FileOutputStream(fileName); PrintStream p = new PrintStream(out)) {

            p.println(contents);

            LOGGER.info("Wrote custom unit from inline data to: {}", fileName);

            return true;
        } catch (Exception ex) {
            LOGGER.error(ex, "Error writing custom unit from inline data to: {}", fileName);
            return false;
        }
    }

    private static void processMissionNodes(Campaign retVal, Node wn, Version version) {
        LOGGER.info("Loading Mission Nodes from XML...");

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
                LOGGER.warn("Unknown node type not loaded in Mission nodes: {}", wn2.getNodeName());
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

        LOGGER.info("Load Mission Nodes Complete!");
    }

    private static @Nullable String checkUnits(final Node wn) {
        LOGGER.info("Checking for missing entities...");

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
                        LOGGER.error("Could not read entity from XML", ex);
                    }
                }
            }
        }
        LOGGER.info("Finished checking for missing entities!");

        if (unitList.isEmpty()) {
            return null;
        } else {
            StringBuilder unitListString = new StringBuilder();
            for (String s : unitList) {
                unitListString.append('\n').append(s);
            }
            LOGGER.error("Could not load the following units: {}", unitListString);
            return unitListString.toString();
        }
    }

    private static void processUnitNodes(Campaign retVal, Node wn, Version version) {
        LOGGER.info("Loading Unit Nodes from XML...");

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("unit")) {
                LOGGER.error("Unknown node type not loaded in Unit nodes: {}", wn2.getNodeName());
                continue;
            }

            Unit u = Unit.generateInstanceFromXML(wn2, version, retVal);

            if (u != null) {
                retVal.importUnit(u);
            }
        }

        LOGGER.info("Load Unit Nodes Complete!");
    }

    private static void processPartNodes(Campaign retVal, Node wn, Version version) {
        LOGGER.info("Loading Part Nodes from XML...");

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
                LOGGER.error("Unknown node type not loaded in Part nodes: {} ", wn2.getNodeName());
                continue;
            }

            Part p = Part.generateInstanceFromXML(wn2, version);

            if (p != null) {
                parts.add(p);
            }
        }

        retVal.importParts(parts);

        LOGGER.info("Load Part Nodes Complete!");
    }

    private static void postProcessParts(Campaign retVal, Version version) {
        Map<Integer, Part> replaceParts = new HashMap<>();
        List<Part> removeParts = new ArrayList<>();
        for (Part prt : retVal.getWarehouse().getParts()) {
            prt.fixReferences(retVal);

            // Remove fundamentally broken equipment parts
            if (((prt instanceof EquipmentPart) && ((EquipmentPart) prt).getType() == null) ||
                      ((prt instanceof MissingEquipmentPart) && ((MissingEquipmentPart) prt).getType() == null)) {
                LOGGER.warn("Could not find matching EquipmentType for part {}", prt.getName());
                removeParts.add(prt);
                continue;
            }

            // deal with equipment parts that are now sub typed
            if (isLegacyMASC(prt) && prt instanceof EquipmentPart equipmentPart) {
                Part replacement = new MASC(prt.getUnitTonnage(),
                      equipmentPart.getType(),
                      equipmentPart.getEquipmentNum(),
                      retVal,
                      0,
                      prt.isOmniPodded());
                replacement.setId(prt.getId());
                replacement.setUnit(prt.getUnit());
                replaceParts.put(prt.getId(), replacement);
            }

            if (isLegacyMissingMASC(prt) && prt instanceof MissingEquipmentPart equipmentPart) {
                Part replacement = new MissingMASC(prt.getUnitTonnage(),
                      equipmentPart.getType(),
                      equipmentPart.getEquipmentNum(),
                      retVal,
                      prt.getTonnage(),
                      0,
                      prt.isOmniPodded());
                replacement.setId(prt.getId());
                replacement.setUnit(prt.getUnit());
                replaceParts.put(prt.getId(), replacement);
            }
        }

        // Replace parts that need to be replaced
        for (Entry<Integer, Part> entry : replaceParts.entrySet()) {
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
                // get rid of any equipment parts without types, locations or mounted
                if (prt instanceof EquipmentPart ePart) {

                    // Null Type... parsing failure
                    if (ePart.getType() == null) {
                        removeParts.add(prt);
                        continue;
                    }

                    Mounted<?> m = u.getEntity().getEquipment(ePart.getEquipmentNum());

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
                    if ((duplicatePart instanceof EquipmentPart) &&
                              ePart.getType().equals(((EquipmentPart) duplicatePart).getType())) {
                        removeParts.add(prt);
                        continue;
                    }
                }
                if (prt instanceof MissingEquipmentPart) {
                    Mounted<?> m = u.getEntity().getEquipment(((MissingEquipmentPart) prt).getEquipmentNum());

                    // Remove equipment parts missing mounts
                    if (m == null) {
                        removeParts.add(prt);
                        continue;
                    }

                    // Remove missing equipment parts without a valid location, unless they're an
                    // ammo bin as they may not have a location
                    if ((m.getLocation() == Entity.LOC_NONE) && !(prt instanceof MissingAmmoBin)) {
                        removeParts.add(prt);
                        continue;
                    }
                }

                // if the type is a BayWeapon, remove
                if ((prt instanceof EquipmentPart) && (((EquipmentPart) prt).getType() instanceof BayWeapon)) {
                    removeParts.add(prt);
                    continue;
                }

                if ((prt instanceof MissingEquipmentPart) &&
                          (((MissingEquipmentPart) prt).getType() instanceof BayWeapon)) {
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
                } else if (((u.getEntity() instanceof SmallCraft) || (u.getEntity() instanceof Jumpship)) &&
                                 ((prt instanceof EnginePart) || (prt instanceof MissingEnginePart))) {
                    // units from earlier versions might have the wrong kind of engine
                    removeParts.add(prt);
                } else {
                    u.addPart(prt);
                }
            }

            // deal with true values for sensor and life support on non-Mek heads
            if ((prt instanceof MekLocation) && (((MekLocation) prt).getLoc() != Mek.LOC_HEAD)) {
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
                boolean isHover = null != u &&
                                        u.getEntity().getMovementMode() == EntityMovementMode.HOVER &&
                                        u.getEntity() instanceof Tank;
                ((EnginePart) prt).fixTankFlag(isHover);
            }

            // clan flag might not have been properly set in early versions
            if ((prt instanceof EnginePart) &&
                      prt.getName().contains("(Clan") &&
                      (prt.getTechBase() != TechBase.CLAN)) {
                ((EnginePart) prt).fixClanFlag();
            }
            if ((prt instanceof MissingEnginePart) && (null != u) && (u.getEntity() instanceof Tank)) {
                boolean isHover = u.getEntity().getMovementMode() == EntityMovementMode.HOVER;
                ((MissingEnginePart) prt).fixTankFlag(isHover);
            }
            if ((prt instanceof MissingEnginePart) &&
                      prt.getName().contains("(Clan") &&
                      (prt.getTechBase() != TechBase.CLAN)) {
                ((MissingEnginePart) prt).fixClanFlag();
            }

            // Spare Ammo bins are useless
            if ((prt instanceof AmmoBin) && prt.isSpare()) {
                removeParts.add(prt);
            }
        }
        for (Part prt : removeParts) {
            LOGGER.debug("Removing part #{} {}", prt.getId(), prt.getName());
            retVal.getWarehouse().removePart(prt);
        }
    }

    /**
     * Determines if the supplied part is a MASC from an older save. This means that it needs to be converted to an
     * actual MASC part.
     *
     * @param p The part to check.
     *
     * @return Whether it's an old MASC.
     */
    private static boolean isLegacyMASC(Part p) {
        return (p instanceof EquipmentPart equipmentPart) &&
                     !(p instanceof MASC) &&
                     (equipmentPart.getType() instanceof MiscType miscType) &&
                     miscType.hasFlag(MiscType.F_MASC);
    }

    /**
     * Determines if the supplied part is a "missing" MASC from an older save. This means that it needs to be converted
     * to an actual "missing" MASC part.
     *
     * @param p The part to check.
     *
     * @return Whether it's an old "missing" MASC.
     */
    private static boolean isLegacyMissingMASC(Part p) {
        return (p instanceof MissingEquipmentPart missingPart) &&
                     !(p instanceof MissingMASC) &&
                     (missingPart.getType() instanceof MiscType miscType) &&
                     miscType.hasFlag(MiscType.F_MASC);
    }

    private static void updatePlanetaryEventsFromXML(Node wn) {
        //TODO: we are no longer tracking planetary events from XML. We weren't allowing this
        // except by hand-editing the original code anyway since the planetary system XML reboot
        // so I think its time to retire this code. A future feature will allow players to add
        // planetary events that can be saved to the campaign file. But until that happens nothing
        // will actually happen here.
    }

    private static void processPartsInUse(Campaign retVal, Node wn, Version version) {
        NodeList wList = wn.getChildNodes();

        for (int i = 0; i < wList.getLength(); i++) {
            Node wn2 = wList.item(i);

            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (wn2.getNodeName().equalsIgnoreCase("ignoreMothBalled")) {
                retVal.setIgnoreMothballed(Boolean.parseBoolean(wn2.getTextContent()));
            } else if (wn2.getNodeName().equalsIgnoreCase("topUpWeekly")) {
                retVal.setTopUpWeekly(Boolean.parseBoolean(wn2.getTextContent()));
            } else if (wn2.getNodeName().equalsIgnoreCase("ignoreSparesUnderQuality")) {
                PartQuality ignoreQuality = PartQuality.valueOf(wn2.getTextContent());
                retVal.setIgnoreSparesUnderQuality(ignoreQuality);
            } else if (wn2.getNodeName().equalsIgnoreCase("partInUseMap")) {
                if (version.isHigherThan(new Version("0.50.07"))) { // <50.10 compatibility handler
                    processPartsInUseRequestedStockMap(retVal, wn2);
                }
            } else {
                LOGGER.error("Unknown node type not loaded in PartInUse nodes: {}", wn2.getNodeName());
            }
        }
    }

    private static void processPartsInUseRequestedStockMap(Campaign retVal, Node wn) {
        NodeList wList = wn.getChildNodes();

        Map<String, Double> partInUseStockMap = new LinkedHashMap<>();

        for (int i = 0; i < wList.getLength(); i++) {
            Node wn2 = wList.item(i);

            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("partInUseMapEntry")) {
                LOGGER.error("Unknown node type not loaded in PartInUseStockMap nodes: {}", wn2.getNodeName());
            }

            processPartsInUseRequestedStockMapVal(retVal, wn2, partInUseStockMap);

        }

        retVal.setPartsInUseRequestedStockMap(partInUseStockMap);
    }

    private static void processPartsInUseRequestedStockMapVal(Campaign retVal, Node wn,
          Map<String, Double> partsInUseRequestedStockMap) {
        NodeList wList = wn.getChildNodes();

        String key = null;
        double val = 0;

        for (int i = 0; i < wList.getLength(); i++) {
            Node wn2 = wList.item(i);

            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (wn2.getNodeName().equalsIgnoreCase("partInUseMapKey")) {
                key = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("partInUseMapVal")) {
                val = Double.parseDouble(wn2.getTextContent());
            }
        }
        if (key != null) {
            partsInUseRequestedStockMap.put(key, val);
        }
    }

    //region Migration Methods
    //region Ancestry Migration
    private static final Map<UUID, List<Person>> ancestryMigrationMap = new HashMap<>();

    // endregion Ancestry Migration
    // endregion Migration Methods
}
