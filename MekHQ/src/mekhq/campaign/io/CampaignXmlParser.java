/*
 * Copyright (c) 2018 - The MegaMek Team
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
package mekhq.campaign.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.MechSummaryCache;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.logging.LogLevel;
import megamek.common.options.IOption;
import megamek.common.options.PilotOptions;
import megamek.common.weapons.bayweapons.BayWeapon;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.NullEntityException;
import mekhq.Utilities;
import mekhq.Version;
import mekhq.campaign.AtBConfiguration;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.Kill;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.market.ContractMarket;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.ShoppingList;
import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mod.am.InjuryTypes;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MissingEnginePart;
import mekhq.campaign.parts.MissingMekActuator;
import mekhq.campaign.parts.MissingMekLocation;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;
import mekhq.campaign.parts.equipment.MASC;
import mekhq.campaign.parts.equipment.MissingAmmoBin;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.parts.equipment.MissingLargeCraftAmmoBin;
import mekhq.campaign.parts.equipment.MissingMASC;
import mekhq.campaign.personnel.Ancestors;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.RankTranslator;
import mekhq.campaign.personnel.Ranks;
import mekhq.campaign.personnel.RetirementDefectionTracker;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planets;
import mekhq.module.atb.AtBEventProcessor;

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
     * @param is The file holding the XML, in InputStream form.
     * @return The created Campaign object, or null if there was a problem.
     * @throws ParseException
     * @throws DOMException
     * @throws NullEntityException
     */
    public Campaign parse() throws CampaignXmlParseException, NullEntityException {
        final String METHOD_NAME = "parse()"; //$NON-NLS-1$

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Starting load of campaign file from XML..."); //$NON-NLS-1$
        // Initialize variables.
        Campaign retVal = new Campaign();
        retVal.setApp(app);

        Document xmlDoc = null;

        try {
            // Using factory get an instance of document builder
            DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(is);
        } catch (Exception ex) {
            MekHQ.getLogger().error(CampaignXmlParser.class, METHOD_NAME, ex);

            throw new CampaignXmlParseException(ex);
        }

        Element campaignEle = xmlDoc.getDocumentElement();
        NodeList nl = campaignEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML. At least this cleans it up.
        campaignEle.normalize();

        Version version = new Version(campaignEle.getAttribute("version"));

        // we need to iterate through three times, the first time to collect
        // any custom units that might not be written yet
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            if (wn.getParentNode() != campaignEle) {
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
                    processCustom(retVal, wn);
                }
            } else {
                // If it's a text node or attribute or whatever at this level,
                // it's probably white-space.
                // We can safely ignore it even if it isn't, for now.
                continue;
            }
        }
        MechSummaryCache.getInstance().loadMechData();

        // the second time to check for any null entities
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            if (wn.getParentNode() != campaignEle) {
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

            if (wn.getParentNode() != campaignEle) {
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
                    retVal.setCampaignOptions(CampaignOptions
                            .generateCampaignOptionsFromXml(wn));
                } else if (xn.equalsIgnoreCase("randomSkillPreferences")) {
                    retVal.setRandomSkillPreferences(RandomSkillPreferences
                            .generateRandomSkillPreferencesFromXml(wn));
                } /* We don't need this since info is processed above in the first iteration...
                else if (xn.equalsIgnoreCase("info")) {
                    processInfoNode(retVal, wn, version);
                }*/ else if (xn.equalsIgnoreCase("parts")) {
                    processPartNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("personnel")) {
                    // TODO: Make this depending on campaign options
                    // TODO: hoist registerAll out of this
                    InjuryTypes.registerAll();
                    processPersonnelNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("ancestors")) {
                    processAncestorNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("units")) {
                    processUnitNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("missions")) {
                    processMissionNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("forces")) {
                    processForces(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("finances")) {
                    processFinances(retVal, wn);
                } else if (xn.equalsIgnoreCase("location")) {
                    retVal.setLocation(CurrentLocation.generateInstanceFromXML(
                            wn, retVal));
                } else if (xn.equalsIgnoreCase("skillTypes")) {
                    processSkillTypeNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("specialAbilities")) {
                    processSpecialAbilityNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("gameOptions")) {
                    processGameOptionNodes(retVal, wn);
                } else if (xn.equalsIgnoreCase("kills")) {
                    processKillNodes(retVal, wn, version);
                } else if (xn.equalsIgnoreCase("shoppingList")) {
                    retVal.setShoppingList(ShoppingList.generateInstanceFromXML(
                            wn, retVal, version));
                } else if (xn.equalsIgnoreCase("personnelMarket")) {
                    retVal.setPersonnelMarket(PersonnelMarket.generateInstanceFromXML(
                            wn, retVal, version));
                    foundPersonnelMarket = true;
                } else if (xn.equalsIgnoreCase("contractMarket")) {
                    retVal.setContractMarket(ContractMarket.generateInstanceFromXML(
                            wn, retVal, version));
                    foundContractMarket = true;
                } else if (xn.equalsIgnoreCase("unitMarket")) {
                    retVal.setUnitMarket(UnitMarket.generateInstanceFromXML(
                            wn, retVal, version));
                    foundUnitMarket = true;
                } else if (xn.equalsIgnoreCase("lances")) {
                    processLanceNodes(retVal, wn);
                } else if (xn.equalsIgnoreCase("retirementDefectionTracker")) {
                    retVal.setRetirementDefectionTracker(RetirementDefectionTracker.generateInstanceFromXML(wn, retVal));
                } else if (xn.equalsIgnoreCase("shipSearchStart")) {
                    Calendar c = new GregorianCalendar();
                    c.setTime(parseDate(retVal.getShortDateFormatter(), wn.getTextContent()));
                    retVal.setShipSearchStart(c);
                } else if (xn.equalsIgnoreCase("shipSearchType")) {
                    retVal.setShipSearchType(Integer.parseInt(wn.getTextContent()));
                } else if (xn.equalsIgnoreCase("shipSearchResult")) {
                    retVal.setShipSearchResult(wn.getTextContent());
                } else if (xn.equalsIgnoreCase("shipSearchExpiration")) {
                    Calendar c = new GregorianCalendar();
                    c.setTime(parseDate(retVal.getShortDateFormatter(), wn.getTextContent()));
                    retVal.setShipSearchExpiration(c);
                } else if (xn.equalsIgnoreCase("customPlanetaryEvents")) {
                    updatePlanetaryEventsFromXML(wn);
                }

            } else {
                // If it's a text node or attribute or whatever at this level,
                // it's probably white-space.
                // We can safely ignore it even if it isn't, for now.
                continue;
            }
        }

        // Okay, after we've gone through all the nodes and constructed the
        // Campaign object...
        // We need to do a post-process pass to restore a number of references.

        // If the version is earlier than 0.3.4 r1782, then we need to translate
        // the rank system.
        if (Version.versionCompare(version, "0.3.4-r1782")) {
            retVal.setRankSystem(
                    RankTranslator.translateRankSystem(retVal.getRanks().getOldRankSystem(), retVal.getFactionCode()));
            if (retVal.getRanks() == null) {

            }
        }

        // if the version is earlier than 0.1.14, then we need to replace all
        // the old integer
        // ids of units and personnel with their UUIDs where they are
        // referenced.
        if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2
                && version.getSnapshot() < 14) {
            fixIdReferences(retVal);
        }
        // if the version is earlier than 0.1.16, then we need to run another
        // fix to update
        // the externalIds to match the Unit IDs.
        if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2
                && version.getSnapshot() < 16) {
            fixIdReferencesB(retVal);
        }

        // adjust tech levels for version before 0.1.21
        if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2
                && version.getSnapshot() < 21) {
            retVal.getCampaignOptions().setTechLevel(retVal.getCampaignOptions()
                    .getTechLevel() + 1);
        }

        long timestamp = System.currentTimeMillis();

        // loop through forces to set force id
        for (Force f : retVal.getAllForces()) {
            Scenario s = retVal.getScenario(f.getScenarioId());
            if (null != s
                    && (null == f.getParentForce() || !f.getParentForce().isDeployed())) {
                s.addForces(f.getId());
            }
            // some units may need force id set for backwards compatability
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

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Force IDs set in %dms", System.currentTimeMillis() - timestamp)); //$NON-NLS-1$
        timestamp = System.currentTimeMillis();

        // Process parts...
        List<Part> spareParts = new ArrayList<>();
        List<Part> removeParts = new ArrayList<>();
        for (Part prt : retVal.getParts()) {
            Unit u = retVal.getUnit(prt.getUnitId());
            prt.setUnit(u);
            if (null != u) {
                // get rid of any equipmentparts without locations or mounteds
                if (prt instanceof EquipmentPart) {
                    Mounted m = u.getEntity().getEquipment(
                            ((EquipmentPart) prt).getEquipmentNum());
                    if (null == m || m.getLocation() == Entity.LOC_NONE) {
                        removeParts.add(prt);
                        continue;
                    }
                    // Remove existing duplicate parts.
                    if (u.getPartForEquipmentNum(((EquipmentPart) prt).getEquipmentNum(), ((EquipmentPart) prt).getLocation()) != null) {
                        removeParts.add(prt);
                        continue;
                    }
                }
                if (prt instanceof MissingEquipmentPart) {
                    Mounted m = u.getEntity().getEquipment(
                            ((MissingEquipmentPart) prt).getEquipmentNum());
                    if (null == m || m.getLocation() == Entity.LOC_NONE) {
                        removeParts.add(prt);
                        continue;
                    }
                    // Remove existing duplicate parts.
                    if (u.getPartForEquipmentNum(((MissingEquipmentPart) prt).getEquipmentNum(), ((MissingEquipmentPart) prt).getLocation()) != null) {
                        removeParts.add(prt);
                        continue;
                    }
                }
                //if the type is a BayWeapon, remove
                if(prt instanceof EquipmentPart
                        && ((EquipmentPart) prt).getType() instanceof BayWeapon) {
                    removeParts.add(prt);
                    continue;
                }

                if(prt instanceof MissingEquipmentPart
                        && ((MissingEquipmentPart) prt).getType() instanceof BayWeapon) {
                    removeParts.add(prt);
                    continue;
                }

                // if actuators on units have no location (on version 1.23 and
                // earlier) then remove them and let initializeParts (called
                // later) create new ones
                if (prt instanceof MekActuator
                        && ((MekActuator) prt).getLocation() == Entity.LOC_NONE) {
                    removeParts.add(prt);
                } else if (prt instanceof MissingMekActuator
                        && ((MissingMekActuator) prt).getLocation() == Entity.LOC_NONE) {
                    removeParts.add(prt);
                } else if ((u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship) && (prt instanceof EnginePart || prt instanceof MissingEnginePart)) {
                    //units from earlier versions might have the wrong kind of engine
                    removeParts.add(prt);
                } else {
                    u.addPart(prt);
                    if (prt instanceof AmmoBin) {
                        ((AmmoBin) prt).restoreMunitionType();
                    }
                }

            } else if (version.getMajorVersion() == 0
                    && version.getMinorVersion() < 2 && version.getSnapshot() < 16) {
                boolean found = false;
                for (Part spare : spareParts) {
                    if (spare.isSamePartTypeAndStatus(prt)) {
                        spare.incrementQuantity();
                        removeParts.add(prt);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    spareParts.add(prt);
                }
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
            // old versions didnt distinguish tank engines
            if (prt instanceof EnginePart && prt.getName().contains("Vehicle")) {
                boolean isHover = null != u
                        && u.getEntity().getMovementMode() == EntityMovementMode.HOVER && u.getEntity() instanceof Tank;
                ((EnginePart) prt).fixTankFlag(isHover);
            }
            // clan flag might not have been properly set in early versions
            if (prt instanceof EnginePart && prt.getName().contains("(Clan")
                    && prt.getTechBase() != Part.T_CLAN) {
                ((EnginePart) prt).fixClanFlag();
            }
            if (prt instanceof MissingEnginePart && null != u
                    && u.getEntity() instanceof Tank) {
                boolean isHover = null != u
                        && u.getEntity().getMovementMode() == EntityMovementMode.HOVER && u.getEntity() instanceof Tank;
                ((MissingEnginePart) prt).fixTankFlag(isHover);
            }
            if (prt instanceof MissingEnginePart
                    && prt.getName().contains("(Clan") && prt.getTechBase() != Part.T_CLAN) {
                ((MissingEnginePart) prt).fixClanFlag();
            }

            if ((version.getMajorVersion() == 0)
                    && ((version.getMinorVersion() < 44)
                            || ((version.getMinorVersion() == 43) && (version.getSnapshot() < 7)))) {
                if ((prt instanceof MekLocation)
                        && (((MekLocation)prt).getStructureType() == EquipmentType.T_STRUCTURE_ENDO_STEEL)) {
                    if (null != u) {
                        ((MekLocation)prt).setClan(TechConstants.isClan(u.getEntity().getStructureTechLevel()));
                    } else {
                        ((MekLocation)prt).setClan(retVal.getFaction().isClan());
                    }
                } else if ((prt instanceof MissingMekLocation)
                        && (((MissingMekLocation)prt).getStructureType() == EquipmentType.T_STRUCTURE_ENDO_STEEL)) {
                    if (null != u) {
                        ((MissingMekLocation)prt).setClan(TechConstants.isClan(u.getEntity().getStructureTechLevel()));
                    }
                }
            }

        }
        for (Part prt : removeParts) {
            retVal.removePart(prt);
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Parts processed in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        // All personnel need the rank reference fixed
        for (Person psn : retVal.getPersonnel()) {
            // skill types might need resetting
            psn.resetSkillTypes();

            //versions before 0.3.4 did not have proper clan phenotypes
            if (version.getMajorVersion() == 0
                    && (version.getMinorVersion() <= 2 ||
                            (version.getMinorVersion() <= 3 && version.getSnapshot() < 4))
                    && retVal.getFaction().isClan()) {
                //assume personnel are clan and trueborn if the right role
                psn.setClanner(true);
                switch (psn.getPrimaryRole()) {
                    case Person.T_MECHWARRIOR:
                        psn.setPhenotype(Person.PHENOTYPE_MW);
                        break;
                    case Person.T_AERO_PILOT:
                    case Person.T_CONV_PILOT:
                        psn.setPhenotype(Person.PHENOTYPE_AERO);
                    case Person.T_BA:
                        psn.setPhenotype(Person.PHENOTYPE_BA);
                    case Person.T_VEE_GUNNER:
                    case Person.T_GVEE_DRIVER:
                    case Person.T_NVEE_DRIVER:
                    case Person.T_VTOL_PILOT:
                        psn.setPhenotype(Person.PHENOTYPE_VEE);
                    default:
                        psn.setPhenotype(Person.PHENOTYPE_NONE);
                }
            }
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Rank references fixed in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        // Okay, Units, need their pilot references fixed.
        for(Unit unit : retVal.getUnits()) {
            // Also, the unit should have its campaign set.
            unit.campaign = retVal;

            // reset the pilot and entity, to reflect newly assigned personnel
            unit.resetPilotAndEntity();

            if (null != unit.getRefit()) {
                unit.getRefit().reCalc();
                if (null == unit.getRefit().getNewArmorSupplies()
                        && unit.getRefit().getNewArmorSuppliesId() > 0) {
                    Armor armorSupplies = (Armor) retVal.getPart(
                            unit.getRefit().getNewArmorSuppliesId());
                    unit.getRefit().setNewArmorSupplies(armorSupplies);
                    if (null == armorSupplies.getUnit()) {
                        armorSupplies.setUnit(unit);
                    }
                }
                if (!unit.getRefit().isCustomJob()
                        && !unit.getRefit().kitFound()) {
                    retVal.getShoppingList().addShoppingItemWithoutChecking(unit
                            .getRefit());
                }
            }

            // lets make sure the force id set actually corresponds to a force
            // TODO: we have some reports of force id relics - need to fix
            if (unit.getForceId() > 0
                    && null == retVal.getForce(unit.getForceId())) {
                unit.setForceId(-1);
            }

            // Its annoying to have to do this, but this helps to ensure
            // that equipment numbers correspond to the right parts - its
            // possible that these might have changed if changes were made to
            // the
            // ordering of equipment in the underlying data file for the unit
            Utilities.unscrambleEquipmentNumbers(unit);

            // some units might need to be assigned to scenarios
            Scenario s = retVal.getScenario(unit.getScenarioId());
            if (null != s) {
                // most units will be properly assigned through their
                // force, so check to make sure they aren't already here
                if (!s.isAssigned(unit, retVal)) {
                    s.addUnit(unit.getId());
                }
            }

            //get rid of BA parts before 0.3.4
            if(unit.getEntity() instanceof BattleArmor
                    && version.getMajorVersion() == 0
                    && (version.getMinorVersion() <= 2 ||
                            (version.getMinorVersion() <= 3 && version.getSnapshot() < 16))) {
                for(Part p : unit.getParts()) {
                    retVal.removePart(p);
                }
                unit.resetParts();
                if(version.getSnapshot() < 4) {
                    for (int loc = 0; loc < unit.getEntity().locations(); loc++) {
                        unit.getEntity().setInternal(0, loc);
                    }
                }
            }
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Pilot references fixed in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        for(Unit unit : retVal.getUnits()) {
            // Some units have been incorrectly assigned a null C3UUID as a string. This should correct that by setting a new C3UUID
            if ((unit.getEntity().hasC3() || unit.getEntity().hasC3i())
                    && (unit.getEntity().getC3UUIDAsString() == null || unit.getEntity().getC3UUIDAsString().equals("null"))) {
                unit.getEntity().setC3UUID();
                unit.getEntity().setC3NetIdSelf();
            }
        }
        retVal.refreshNetworks();

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] C3 networks refreshed in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        // ok, once we are sure that campaign has been set for all units, we can
        // now go through and initializeParts and run diagnostics
        List<Unit> removeUnits = new ArrayList<>();
        for (Unit unit : retVal.getUnits()) {
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
        }
        for (Unit unit : removeUnits) {
            retVal.removeUnit(unit.getId());
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Units initialized in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        retVal.reloadNews();

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] News loaded in %dms", //$NON-NLS-1$
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
            retVal.setUnitMarket(new UnitMarket());
        }
        if (null == retVal.getRetirementDefectionTracker()) {
            retVal.setRetirementDefectionTracker(new RetirementDefectionTracker());
        }
        if (retVal.getCampaignOptions().getUseAtB()) {
            retVal.setAtBConfig(AtBConfiguration.loadFromXml());
            retVal.setAtBEventProcessor(new AtBEventProcessor(retVal));
        }

        //**EVERYTHING HAS BEEN LOADED. NOW FOR SANITY CHECKS**//

        //unload any ammo bins in the warehouse
        ArrayList<AmmoBin> binsToUnload = new ArrayList<AmmoBin>();
        for(Part prt : retVal.getSpareParts()) {
            if (prt instanceof AmmoBin && !prt.isReservedForRefit() && ((AmmoBin) prt).getShotsNeeded() == 0) {
                binsToUnload.add((AmmoBin) prt);
            }
        }
        for(AmmoBin bin : binsToUnload) {
            bin.unload();
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Ammo bins cleared in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();


        //Check all parts that are reserved for refit and if the refit id unit
        //is not refitting or is gone then unreserve
        for(Part part : retVal.getParts()) {
            if (part.isReservedForRefit()) {
                Unit u = retVal.getUnit(part.getRefitId());
                if (null == u || !u.isRefitting()) {
                    part.setRefitId(null);
                }
            }
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Reserved refit parts fixed in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        //try to stack as much as possible the parts in the warehouse that may be unstacked
        //for a variety of reasons
        List<Part> partsToRemove = new ArrayList<>();
        List<Part> partsToKeep = new ArrayList<>();
        for(Part part : retVal.getParts()) {
            if (part.isSpare() && part.isPresent()) {
                for (Part oPart : partsToKeep) {
                    if (part.isSamePartTypeAndStatus(oPart)) {
                        if (part instanceof Armor) {
                            if (oPart instanceof Armor) {
                                ((Armor) oPart).setAmount(((Armor) oPart).getAmount() + ((Armor) part).getAmount());
                                partsToRemove.add(part);
                                break;
                            }
                        } else if (part instanceof AmmoStorage) {
                            if (oPart instanceof AmmoStorage) {
                                ((AmmoStorage) oPart).changeShots(((AmmoStorage) part).getShots());
                                partsToRemove.add(part);
                                break;
                            }
                        } else {
                            int q = part.getQuantity();
                            while (q > 0) {
                                oPart.incrementQuantity();
                                q--;
                            }
                            partsToRemove.add(part);
                            break;
                        }
                    }
                }
                partsToKeep.add(part);
            }
        }
        for(Part toRemove : partsToRemove) {
            retVal.removePart(toRemove);
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                String.format("[Campaign Load] Warehouse cleaned up in %dms", //$NON-NLS-1$
                        System.currentTimeMillis() - timestamp));
        timestamp = System.currentTimeMillis();

        retVal.setUnitRating(null);

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Load of campaign file complete!"); //$NON-NLS-1$

        return retVal;
    }
    
    /**
     * Pulled out purely for encapsulation. Makes the code neater and easier to read.
     *
     * @param retVal The Campaign object that is being populated.
     * @param wni    The XML node we're working from.
     * @throws ParseException
     * @throws DOMException
     */
    private static void processInfoNode(Campaign retVal, Node wni,
            Version version) throws DOMException, CampaignXmlParseException {
        NodeList nl = wni.getChildNodes();

        String rankNames = null;
        int officerCut = 0;
        int rankSystem = -1;

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);
            int xc = wn.getNodeType();

            // If it's not an element, again, we're ignoring it.
            if (xc == Node.ELEMENT_NODE) {
                String xn = wn.getNodeName();

                // Yeah, long if/then clauses suck.
                // I really couldn't think of a significantly better way to
                // handle it.
                // They're all primitives anyway...
                if (xn.equalsIgnoreCase("calendar")) {
                    SimpleDateFormat df = new SimpleDateFormat(
                            "yyyy-MM-dd hh:mm:ss");
                    GregorianCalendar c = (GregorianCalendar) GregorianCalendar
                            .getInstance();
                    c.setTime(parseDate(df, wn.getTextContent().trim()));
                    retVal.setCalendar(c);
                } else if (xn.equalsIgnoreCase("camoCategory")) {
                    String val = wn.getTextContent().trim();

                    if (val.equals("null")) {
                        retVal.setCamoCategory(null);
                    } else {
                        retVal.setCamoCategory(val);
                    }
                } else if (xn.equalsIgnoreCase("camoFileName")) {
                    String val = wn.getTextContent().trim();

                    if (val.equals("null")) {
                        retVal.setCamoFileName(null);
                    } else {
                        retVal.setCamoFileName(val);
                    }
                } else if (xn.equalsIgnoreCase("colorIndex")) {
                    retVal.setColorIndex(Integer.parseInt(wn.getTextContent()
                            .trim()));
                } else if (xn.equalsIgnoreCase("nameGen")) {
                    // First, get all the child nodes;
                    NodeList nl2 = wn.getChildNodes();
                    for (int x2 = 0; x2 < nl2.getLength(); x2++) {
                        Node wn2 = nl2.item(x2);
                        if (wn2.getParentNode() != wn) {
                            continue;
                        }
                        if (wn2.getNodeName().equalsIgnoreCase("faction")) {
                            retVal.getRNG().setChosenFaction(wn2.getTextContent().trim());
                        } else if (wn2.getNodeName().equalsIgnoreCase(
                                "percentFemale")) {
                            retVal.getRNG().setPerentFemale(Integer.parseInt(wn2.getTextContent().trim()));
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
                    if (version.getMajorVersion() == 0
                            && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
                        retVal.setFactionCode(Faction.getFactionCode(Integer.parseInt(wn.getTextContent())));
                    } else {
                        retVal.setFactionCode(wn.getTextContent());
                    }
                    retVal.updateTechFactionCode();
                } else if (xn.equalsIgnoreCase("retainerEmployerCode")) {
                    retVal.setRetainerEmployerCode(wn.getTextContent());
                } else if (xn.equalsIgnoreCase("officerCut")) {
                    officerCut = Integer.parseInt(wn.getTextContent().trim());
                } else if (xn.equalsIgnoreCase("rankNames")) {
                    rankNames = wn.getTextContent().trim();
                } else if (xn.equalsIgnoreCase("ranks") || xn.equalsIgnoreCase("rankSystem")) {
                    if (Version.versionCompare(version, "0.3.4-r1645")) {
                        rankSystem = Integer.parseInt(wn.getTextContent().trim());
                    } else {
                        Ranks r = Ranks.generateInstanceFromXML(wn, version);
                        if (r != null) {
                            retVal.setRanks(r);
                        }
                    }
                } else if (xn.equalsIgnoreCase("gmMode")) {
                    retVal.setGMMode(Boolean.parseBoolean(wn.getTextContent().trim()));
                } else if (xn.equalsIgnoreCase("showOverview")) {
                    retVal.setOverviewLoadingValue(Boolean.parseBoolean(wn.getTextContent().trim()));
                /* CAW: Not used anymore as the Campaign tracks this internally via TreeMap's.
                } else if (xn.equalsIgnoreCase("lastPartId")) {
                    retVal.setLastPartId(Integer.parseInt(wn.getTextContent()
                            .trim()));
                } else if (xn.equalsIgnoreCase("lastForceId")) {
                    retVal.setLastForceId(Integer.parseInt(wn.getTextContent()
                            .trim()));
                } else if (xn.equalsIgnoreCase("lastTeamId")) {
                    retVal.setLastTeamId(Integer.parseInt(wn.getTextContent()
                            .trim()));
                } else if (xn.equalsIgnoreCase("lastMissionId")) {
                    retVal.setLastMissionId(Integer.parseInt(wn.getTextContent()
                            .trim()));
                } else if (xn.equalsIgnoreCase("lastScenarioId")) {
                    retVal.setLastScenarioId(Integer.parseInt(wn.getTextContent().trim()));*/
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
                    retVal.setAstechPool(Integer.parseInt(wn.getTextContent()
                            .trim()));
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
            }
        }
        if (null != rankNames) {
            //backwards compatibility
            retVal.getRanks().setRanksFromList(rankNames, officerCut);
        }
        if (rankSystem != -1) {
            retVal.setRanks(new Ranks(rankSystem));
            retVal.getRanks().setOldRankSystem(rankSystem);
        }

        // TODO: this could probably be better
        retVal.setCurrentReportHTML(Utilities.combineString(retVal.getCurrentReport(), Campaign.REPORT_LINEBREAK));

        // Everything's new
        List<String> newReports = new ArrayList<String>(retVal.getCurrentReport().size() * 2);
        boolean firstReport = true;
        for(String report : retVal.getCurrentReport()) {
            if (firstReport) {
                firstReport = false;
            } else {
                newReports.add(Campaign.REPORT_LINEBREAK);
            }
            newReports.add(report);
        }
        retVal.setNewReports(newReports);
    }

    private static Date parseDate(DateFormat df, String value) throws CampaignXmlParseException {
        try {
            return df.parse(value);
        } catch (ParseException e) {
            throw new CampaignXmlParseException("Could not parse date: " + value, e);
        }
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
                MekHQ.getLogger().log(CampaignXmlParser.class, "processLanceNodes(Campaign,Node)", LogLevel.ERROR, //$NON-NLS-1$
                        "Unknown node type not loaded in Lance nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            Lance l = Lance.generateInstanceFromXML(wn2);

            if (l != null) {
                retVal.importLance(l);
            }
        }
    }

    private static void fixIdReferences(Campaign retVal) {
        // set up translation hashes
        Map<Integer, UUID> uHash = new HashMap<>();
        Map<Integer, UUID> pHash = new HashMap<>();
        for (Unit u : retVal.getUnits()) {
            uHash.put(u.getOldId(), u.getId());
        }
        for (Person p : retVal.getPersonnel()) {
            pHash.put(p.getOldId(), p.getId());
        }
        // ok now go through and fix
        for (Unit u : retVal.getUnits()) {
            u.fixIdReferences(uHash, pHash);
        }
        for (Person p : retVal.getPersonnel()) {
            p.fixIdReferences(uHash, pHash);
        }
        retVal.getForces().fixIdReferences(uHash);
        for (Part p : retVal.getParts()) {
            p.fixIdReferences(uHash, pHash);
        }
        List<Kill> ghostKills = new ArrayList<Kill>();
        for (Kill k : retVal.getKills()) {
            k.fixIdReferences(pHash);
            if (null == k.getPilotId()) {
                ghostKills.add(k);
            }
        }
        // check for kills with missing person references
        for (Kill k : ghostKills) {
            if (null == k.getPilotId()) {
                retVal.removeKill(k);
            }
        }
    }

    private static void fixIdReferencesB(Campaign retVal) {
        for (Unit u : retVal.getUnits()) {
            Entity en = u.getEntity();
            en.setExternalIdAsString(u.getId().toString());

            // If they have C3 or C3i we need to set their ID
            if (en.hasC3() || en.hasC3i()) {
                en.setC3UUID();
                en.setC3NetIdSelf();
            }
        }
    }

    private static void processFinances(Campaign retVal, Node wn) {
        MekHQ.getLogger().log(CampaignXmlParser.class, "processFinances(Campaign,Node)", LogLevel.INFO, //$NON-NLS-1$
                "Loading Finances from XML..."); //$NON-NLS-1$
        retVal.setFinances(Finances.generateInstanceFromXML(wn));
        MekHQ.getLogger().log(CampaignXmlParser.class, "processFinances(Campaign,Node)", LogLevel.INFO, //$NON-NLS-1$
                "Load of Finances complete!"); //$NON-NLS-1$
    }

    private static void processForces(Campaign retVal, Node wn, Version version) {
        final String METHOD_NAME = "processForces(Campaign,Node,Version)"; //$NON-NLS-1$
        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Loading Force Organization from XML..."); //$NON-NLS-1$

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
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Forces nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            if (!foundForceAlready) {
                Force f = Force.generateInstanceFromXML(wn2, retVal, version);
                if (null != f) {
                    retVal.setForces(f);
                    foundForceAlready = true;
                }
            } else {
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.ERROR,
                        "More than one type-level force found"); //$NON-NLS-1$
            }
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Load of Force Organization complete!");
    }

    private static void processPersonnelNodes(Campaign retVal, Node wn,
            Version version) {
        final String METHOD_NAME = "processPersonnelNodes(Campaign,Node,Version)"; //$NON-NLS-1$
        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Loading Personnel Nodes from XML..."); //$NON-NLS-1$

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
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Personnel nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            Person p = Person.generateInstanceFromXML(wn2, retVal, version);

            if (p != null) {
                retVal.importPerson(p);
            }
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Load Personnel Nodes Complete!"); //$NON-NLS-1$
    }

    private static void processAncestorNodes(Campaign retVal, Node wn,
            Version version) {
        final String METHOD_NAME = "processAncestorNodes(Campaign,Node,Version)"; //$NON-NLS-1$

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Loading Ancestor Nodes from XML..."); //$NON-NLS-1$

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("ancestor")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Ancestor nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            Ancestors a = Ancestors.generateInstanceFromXML(wn2, retVal, version);

            if (a != null) {
                retVal.importAncestors(a);
            }
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Load Ancestor Nodes Complete!"); //$NON-NLS-1$
    }

    private static void processSkillTypeNodes(Campaign retVal, Node wn,
            Version version) {
        final String METHOD_NAME = "processSkillTypeNodes(Campaign,Node,Version)"; //$NON-NLS-1$

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Loading Skill Type Nodes from XML..."); //$NON-NLS-1$

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
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Skill Type nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            // TODO: make SkillType a Campaign instance
            SkillType.generateInstanceFromXML(wn2, version);
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Load Skill Type Nodes Complete!"); //$NON-NLS-1$
    }

    private static void processSpecialAbilityNodes(Campaign retVal, Node wn,
            Version version) {
        final String METHOD_NAME = "processSpecialAbilityNodes(Campaign,Node,Version)"; //$NON-NLS-1$

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Loading Special Ability Nodes from XML..."); //$NON-NLS-1$

        PilotOptions options = new PilotOptions();

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
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Special Ability nodes: " //$NON-NLS-1$
                        + wn2.getNodeName());

                continue;
            }

            SpecialAbility.generateInstanceFromXML(wn2, options, version);
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Load Special Ability Nodes Complete!"); //$NON-NLS-1$
    }

    private static void processKillNodes(Campaign retVal, Node wn,
            Version version) {
        final String METHOD_NAME = "processKillNodes(Campaign,Node,Version"; //$NON-NLS-1$
        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Loading Kill Nodes from XML..."); //$NON-NLS-1$

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
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Kill nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            Kill kill = Kill.generateInstanceFromXML(wn2, version);
            if (kill != null) {
                retVal.importKill(kill);
            }
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Load Kill Nodes Complete!"); //$NON-NLS-1$
    }

    private static void processGameOptionNodes(Campaign retVal, Node wn) {
        final String METHOD_NAME = "processGameOptionNodes(Campaign,Node)"; //$NON-NLS-1$
        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Loading GameOption Nodes from XML..."); //$NON-NLS-1$

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            } else if (!wn2.getNodeName().equalsIgnoreCase("gameoption")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Game Option nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }
            NodeList nl = wn2.getChildNodes();

            String name = null;
            Object value = null;
            for (int y = 0; y < nl.getLength(); y++) {
                Node wn3 = nl.item(y);
                if (wn3.getNodeName().equalsIgnoreCase("name")) {
                    name = wn3.getTextContent();
                } else if (wn3.getNodeName().equalsIgnoreCase("value")) {
                    value = wn3.getTextContent();
                }
            }
            if ((null != name) && (null != value)) {
                IOption option = retVal.getGameOptions().getOption(name);
                if (null != option) {
                    if (!option.getValue().toString().equals(value.toString())) {
                        try {
                            switch (option.getType()) {
                                case IOption.STRING:
                                case IOption.CHOICE:
                                    option.setValue((String) value);
                                    break;

                                case IOption.BOOLEAN:
                                    option.setValue(new Boolean(value.toString()));
                                    break;

                                case IOption.INTEGER:
                                    option.setValue(new Integer(value.toString()));
                                    break;

                                case IOption.FLOAT:
                                    option.setValue(new Float(value.toString()));
                                    break;

                            }
                        } catch (IllegalArgumentException iaEx) {
                            MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.ERROR,
                                    "Error trying to load option '" + name + "' with a value of '" //$NON-NLS-1$
                                    + value + "'.");
                        }
                    }
                } else {
                    MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.ERROR,
                            "Invalid option '" + name + "' when trying to load options file."); //$NON-NLS-1$
                }
            }
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Load Game Option Nodes Complete!"); //$NON-NLS-1$
    }

    private static void processCustom(Campaign retVal, Node wn) {
        final String METHOD_NAME = "processCustom(Campaign,Node)"; //$NON-NLS-1$

        String sCustomsDir = "data" + File.separator + "mechfiles"
                + File.separator + "customs";
        String sCustomsDirCampaign = sCustomsDir + File.separator + retVal.getName();
        File customsDir = new File(sCustomsDir);
        if (!customsDir.exists()) {
            customsDir.mkdir();
        }
        File customsDirCampaign = new File(sCustomsDirCampaign);
        if (!customsDirCampaign.exists()) {
            customsDirCampaign.mkdir();
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
                name = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("mtf")) {
                mtf = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("blk")) {
                blk = wn2.getTextContent();
            }
        }
        retVal.addCustom(name);
        if (null != name && null != mtf) {
            try {
                // if this file already exists then don't overwrite it or we
                // will end up with a bunch of copies
                String fileName = sCustomsDir + File.separator + name + ".mtf";
                String fileNameCampaign = sCustomsDirCampaign + File.separator
                        + name + ".mtf";
                if ((new File(fileName)).exists()
                        || (new File(fileNameCampaign)).exists()) {
                    return;
                }
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                        "Loading Custom unit from XML..."); //$NON-NLS-1$
                OutputStream out = new FileOutputStream(fileNameCampaign);
                PrintStream p = new PrintStream(out);
                p.println(mtf);
                p.close();
                out.close();
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                        "Loaded Custom unit!"); //$NON-NLS-1$
            } catch (Exception ex) {
                MekHQ.getLogger().error(CampaignXmlParser.class, METHOD_NAME, ex);
            }
        }
        if (null != name && null != blk) {
            try {
                // if this file already exists then don't overwrite it or we
                // will end up with a bunch of copies
                String fileName = sCustomsDir + File.separator + name + ".blk";
                String fileNameCampaign = sCustomsDirCampaign + File.separator
                        + name + ".blk";
                if ((new File(fileName)).exists()
                        || (new File(fileNameCampaign)).exists()) {
                    return;
                }
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                        "Loading Custom unit from XML..."); //$NON-NLS-1$
                FileOutputStream out = new FileOutputStream(fileNameCampaign);
                PrintStream p = new PrintStream(out);
                p.println(blk);
                p.close();
                out.close();
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                        "Loaded Custom unit!"); //$NON-NLS-1$
            } catch (Exception ex) {
                MekHQ.getLogger().error(CampaignXmlParser.class, METHOD_NAME, ex);
            }
        }
    }

    private static void processMissionNodes(Campaign retVal, Node wn, Version version) {
        final String METHOD_NAME = "processMissionNodes(Campaign,Node,Version)"; //$NON-NLS-1$

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Loading Mission Nodes from XML..."); //$NON-NLS-1$

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
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                        "Unknown node type not loaded in Mission nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            Mission m = Mission.generateInstanceFromXML(wn2, retVal, version);

            if (m != null) {
                retVal.importMission(m);
            }
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Load Mission Nodes Complete!"); //$NON-NLS-1$
    }

    private static String checkUnits(Node wn) {
        final String METHOD_NAME = "checkUnits(Node)"; //$NON-NLS-1$

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                "Checking for missing entities..."); //$NON-NLS-1$

        ArrayList<String> unitList = new ArrayList<String>();
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
                        if (null == MekHqXmlUtil.getEntityFromXmlString(wn3)) {
                            String name = MekHqXmlUtil
                                    .getEntityNameFromXmlString(wn3);
                            if (!unitList.contains(name)) {
                                unitList.add(name);
                            }
                        }
                    } catch (Exception e) {
                        MekHQ.getLogger().error(CampaignXmlParser.class, METHOD_NAME,
                            "Could not read entity from XML", e); //$NON-NLS-1$
                    }
                }
            }
        }
        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Finished checking for missing entities!"); //$NON-NLS-1$

        if (unitList.isEmpty()) {
            return null;
        } else {
            String unitListString = "";
            for (String s : unitList) {
                unitListString += "\n" + s;
            }
            return unitListString;
        }
    }

    private static void processUnitNodes(Campaign retVal, Node wn,
            Version version) {
        final String METHOD_NAME = "processUnitNodes(Campaign,Node,Version)"; //$NON-NLS-1$

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Loading Unit Nodes from XML..."); //$NON-NLS-1$

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("unit")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Unit nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            Unit u = Unit.generateInstanceFromXML(wn2, version);

            if (u != null) {
                retVal.importUnit(u);
            }
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Load Unit Nodes Complete!"); //$NON-NLS-1$
    }

    private static void processPartNodes(Campaign retVal, Node wn,
            Version version) {
        final String METHOD_NAME = "processPartNodes(Campaign,Node,Version)"; //$NON-NLS-1$

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Loading Part Nodes from XML..."); //$NON-NLS-1$

        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("part")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.ERROR,
                        "Unknown node type not loaded in Part nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            Part p = Part.generateInstanceFromXML(wn2, version);

            // deal with the Weapon as Heat Sink problem from earlier versions
            if (p instanceof HeatSink && !p.getName().contains("Heat Sink")) {
                continue;
            }

            if (((p instanceof EquipmentPart) && ((EquipmentPart) p).getType() == null)
                    || ((p instanceof MissingEquipmentPart) && ((MissingEquipmentPart) p).getType() == null)) {
                MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.WARNING,
                        "Could not find matching EquipmentType for part " + p.getName());
                continue;
            }

            // deal with equipmentparts that are now subtyped
            int pid = p.getId();
            if (p instanceof EquipmentPart
                    && ((EquipmentPart) p).getType() instanceof MiscType
                    && ((EquipmentPart) p).getType().hasFlag(MiscType.F_MASC) && !(p instanceof MASC)) {
                p = new MASC(p.getUnitTonnage(), ((EquipmentPart) p).getType(),
                        ((EquipmentPart) p).getEquipmentNum(), retVal, 0, p.isOmniPodded());
                p.setId(pid);
            }
            if (p instanceof MissingEquipmentPart
                    && ((MissingEquipmentPart) p).getType().hasFlag(MiscType.F_MASC) && !(p instanceof MASC)) {
                p = new MissingMASC(p.getUnitTonnage(),
                        ((MissingEquipmentPart) p).getType(), ((MissingEquipmentPart) p).getEquipmentNum(), retVal,
                        ((MissingEquipmentPart) p).getTonnage(), 0, p.isOmniPodded());
                p.setId(pid);
            }
            // deal with true values for sensor and life support on non-Mech
            // heads
            if (p instanceof MekLocation
                    && ((MekLocation) p).getLoc() != Mech.LOC_HEAD) {
                ((MekLocation) p).setSensors(false);
                ((MekLocation) p).setLifeSupport(false);
            }

            if (version.getMinorVersion() < 3 && !p.needsFixing()
                    && !p.isSalvaging()) {
                // repaired parts were not getting experience properly reset
                p.setSkillMin(SkillType.EXP_GREEN);
            }

            //if for some reason we couldn't find a type for equipment part, then remove it
            if((p instanceof EquipmentPart && null == ((EquipmentPart)p).getType())
                    || (p instanceof MissingEquipmentPart && null == ((MissingEquipmentPart) p).getType())) {
                p = null;
            }

            if ((null != p) && (p.getUnitId() != null)
                    && ((version.getMinorVersion() < 43)
                            || ((version.getMinorVersion() == 43) && (version.getSnapshot() < 5)))
                    && ((p instanceof AmmoBin) || (p instanceof MissingAmmoBin))) {
                Unit u = retVal.getUnit(p.getUnitId());
                if ((null != u) && (u.getEntity().usesWeaponBays())) {
                    Mounted ammo;
                    if (p instanceof EquipmentPart) {
                        ammo = u.getEntity().getEquipment(((EquipmentPart) p).getEquipmentNum());
                    } else {
                        ammo = u.getEntity().getEquipment(((MissingEquipmentPart) p).getEquipmentNum());
                    }
                    if (null != ammo) {
                        if (p instanceof AmmoBin) {
                            p = new LargeCraftAmmoBin(p.getUnitTonnage(),
                                    ((AmmoBin) p).getType(),
                                    ((AmmoBin) p).getEquipmentNum(),
                                    ((AmmoBin) p).getShotsNeeded(),
                                    ammo.getAmmoCapacity(), retVal);
                            ((LargeCraftAmmoBin) p).setBay(u.getEntity().getBayByAmmo(ammo));
                        } else {
                            p = new MissingLargeCraftAmmoBin(p.getUnitTonnage(),
                                    ((MissingAmmoBin) p).getType(),
                                    ((MissingAmmoBin) p).getEquipmentNum(),
                                    ammo.getAmmoCapacity(), retVal);
                            ((MissingLargeCraftAmmoBin) p).setBay(u.getEntity().getBayByAmmo(ammo));
                        }
                    }
                }
            }

            if (p != null) {
                retVal.importPart(p);
            }
        }

        MekHQ.getLogger().log(CampaignXmlParser.class, METHOD_NAME, LogLevel.INFO,
                "Load Part Nodes Complete!"); //$NON-NLS-1$
    }
    
    private static void updatePlanetaryEventsFromXML(Node wn) {
        // TODO: make Planets a member of Campaign
        Planets.reload(true);

        NodeList wList = wn.getChildNodes();
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (wn2.getNodeName().equalsIgnoreCase("planet")) {
                NodeList planetNodes = wn2.getChildNodes();
                String planetId = null;
                List<Planet.PlanetaryEvent> events = new ArrayList<>();
                for(int n = 0; n < planetNodes.getLength(); ++n) {
                    Node planetNode = planetNodes.item(n);
                    if(planetNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if(planetNode.getNodeName().equalsIgnoreCase("id")) {
                        planetId = planetNode.getTextContent();
                    } else if(planetNode.getNodeName().equalsIgnoreCase("event")) {
                        Planet.PlanetaryEvent event = Planets.getInstance().readPlanetaryEvent(planetNode);
                        if(null != event) {
                            event.custom = true;
                            events.add(event);
                        }
                    }
                }
                if(null != planetId) {
                    Planets.getInstance().updatePlanetaryEvents(planetId, events, true);
                }
            }
        }
    }
}
