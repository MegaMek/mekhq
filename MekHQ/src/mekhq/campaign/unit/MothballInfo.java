/*
 * Copyright (C) 2018-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is used to store information about a particular unit that is lost when a unit is mothballed, so that it
 * may be restored to as close to its prior state as possible when the unit is reactivated.
 *
 * @author NickAragua
 */
public class MothballInfo {
    private static final MMLogger LOGGER = MMLogger.create(MothballInfo.class);

    private UUID techId;
    private int forceId;
    private final List<UUID> driverIds = new ArrayList<>();
    private final List<UUID> gunnerIds = new ArrayList<>();
    private final List<UUID> vesselCrewIds = new ArrayList<>();
    private UUID techOfficerId;
    private UUID navigatorId;
    private final Map<PersonnelRole, Integer> tempCrewMap = new HashMap<>();

    /**
     * Parameterless constructor, used for deserialization.
     */
    private MothballInfo() {
        forceId = Formation.FORMATION_NONE;
    }

    /**
     * Who was the original tech of this vessel?
     *
     * @return The original tech's ID
     */
    public UUID getTechId() {
        return techId;
    }

    /**
     * Creates a set of mothball info for a given unit
     *
     * @param unit The unit to work with
     */
    public MothballInfo(Unit unit) {
        Person tech = unit.getTech();
        if (tech != null) {
            techId = tech.getId();
        }

        forceId = unit.getFormationId();

        List<Person> drivers = new ArrayList<>(unit.getDrivers());
        for (Person driver : drivers) {
            if (driver != null) {
                driverIds.add(driver.getId());
            }
        }

        List<Person> gunners = new ArrayList<>(unit.getGunners());
        for (Person gunner : gunners) {
            if (gunner != null) {
                gunnerIds.add(gunner.getId());
            }
        }

        List<Person> vesselCrews = new ArrayList<>(unit.getVesselCrew());
        for (Person vesselCrew : vesselCrews) {
            if (vesselCrew != null) {
                vesselCrewIds.add(vesselCrew.getId());
            }
        }

        Person techOfficer = unit.getTechOfficer();
        if (techOfficer != null) {
            techOfficerId = techOfficer.getId();
        }

        Person navigator = unit.getNavigator();
        if (navigator != null) {
            navigatorId = navigator.getId();
        }

        for (PersonnelRole role : unit.getTempCrewRoles()) {
            int count = unit.getTempCrewByPersonnelRole(role);
            if (count > 0) {
                tempCrewMap.put(role, count);
            }
        }
    }

    /**
     * Restore a unit's pilot, assigned tech and force, to the best of our ability
     *
     * @param unit     The unit to restore
     * @param campaign The campaign in which this is happening
     */
    public void restorePreMothballInfo(Unit unit, Campaign campaign) {
        Person tech = campaign.getPerson(techId);
        if (tech != null && tech.getStatus().isActive()) {
            unit.setTech(tech);
        }

        for (UUID driverId : driverIds) {
            Person driver = campaign.getPerson(driverId);
            if (driver != null && driver.getStatus().isActive() && (driver.getUnit() == null)) {
                unit.addDriver(driver);
            }
        }

        for (UUID gunnerId : gunnerIds) {
            // add the gunner if they exist, aren't dead/retired/etc and aren't already
            // assigned to some
            // other unit. Caveat: single-person units have the same driver and gunner.
            Person gunner = campaign.getPerson(gunnerId);
            if (gunner != null &&
                      gunner.getStatus().isActive() &&
                      ((gunner.getUnit() == null) || (gunner.getUnit() == unit))) {
                unit.addGunner(gunner);
            }
        }

        for (UUID crewId : vesselCrewIds) {
            Person crew = campaign.getPerson(crewId);
            if (crew != null && crew.getStatus().isActive() && (crew.getUnit() == null)) {
                unit.addVesselCrew(crew);
            }
        }

        Person techOfficer = campaign.getPerson(techOfficerId);
        if ((techOfficer != null) && (techOfficer.getStatus().isActive()) && (techOfficer.getUnit() == null)) {
            unit.setTechOfficer(techOfficer);
        }

        Person navigator = campaign.getPerson(navigatorId);
        if ((navigator != null) && (navigator.getStatus().isActive()) && (navigator.getUnit() == null)) {
            unit.setNavigator(navigator);
        }

        // Restore temp crew from the pool, up to the saved amounts.
        for (Map.Entry<PersonnelRole, Integer> entry : tempCrewMap.entrySet()) {
            PersonnelRole role = entry.getKey();
            int saved = entry.getValue();
            int available = campaign.getAvailableTempCrewPool(role);
            int toRestore = Math.min(saved, available);
            if (toRestore > 0) {
                unit.setTempCrew(role, toRestore);
            }
        }

        // Attempt to return the unit to its last force assignment.
        Formation formation = campaign.getFormation(forceId);
        if (formation != null) {
            // If the force is deployed to a scenario, back out. We don't want to restore the unit to the original
            // force as that would cause them to teleport into the scenario. This will likely cause issues, so it's
            // prohibited.
            if (formation.isDeployed()) {
                return;
            }

            // If StratCon is enabled, we need to perform an additional check to ensure the original force isn't
            // currently deployed to the Area of Operations.
            boolean isUseStratCon = campaign.getCampaignOptions().isUseStratCon();
            if (isUseStratCon) {
                for (AtBContract contract : campaign.getActiveAtBContracts()) {
                    StratConCampaignState campaignState = contract.getStratConCampaignState();

                    if (campaignState != null) {
                        if (campaignState.isForceDeployedHere(forceId)) {
                            return; // If the force is deployed to the AO return without restoring force assignment.
                        }
                    }
                }
            }

            // If all the checks have passed, restore the unit to its last force
            campaign.addUnitToFormation(unit, forceId);
        }

        unit.resetEngineer();
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "mothballInfo");
        if (techId != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "techId", techId);
        }

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forceID", forceId);

        for (UUID driver : driverIds) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "driverId", driver);
        }

        for (UUID gunner : gunnerIds) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "gunnerId", gunner);
        }

        for (UUID crew : vesselCrewIds) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "vesselCrewId", crew);
        }

        if (navigatorId != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "navigatorId", navigatorId);
        }

        if (techOfficerId != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "techOfficerId", techOfficerId);
        }

        if (!tempCrewMap.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "tempCrewMap");
            for (Map.Entry<PersonnelRole, Integer> entry : tempCrewMap.entrySet()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "tempCrew");
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "role", entry.getKey().name());
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "count", entry.getValue());
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "tempCrew");
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "tempCrewMap");
        }

        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "mothballInfo");
    }

    /**
     * Deserializer method implemented in standard MekHQ pattern.
     *
     * @return Instance of MothballInfo
     */
    public static MothballInfo generateInstanceFromXML(Node wn, Version version) {
        MothballInfo retVal = new MothballInfo();

        NodeList nl = wn.getChildNodes();

        try {
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("techID")) {
                    retVal.techId = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("forceID")) {
                    retVal.forceId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("driverID")) {
                    retVal.driverIds.add(UUID.fromString(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("gunnerID")) {
                    retVal.gunnerIds.add(UUID.fromString(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("vesselCrewID")) {
                    retVal.vesselCrewIds.add(UUID.fromString(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("techOfficerID")) {
                    retVal.techOfficerId = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("navigatorID")) {
                    retVal.navigatorId = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("tempCrewMap")) {
                    NodeList tempCrewNodes = wn2.getChildNodes();
                    for (int y = 0; y < tempCrewNodes.getLength(); y++) {
                        Node tempCrewNode = tempCrewNodes.item(y);
                        if (tempCrewNode.getNodeName().equalsIgnoreCase("tempCrew")) {
                            PersonnelRole role = null;
                            int count = 0;
                            NodeList entryNodes = tempCrewNode.getChildNodes();
                            for (int z = 0; z < entryNodes.getLength(); z++) {
                                Node entryNode = entryNodes.item(z);
                                if (entryNode.getNodeName().equalsIgnoreCase("role")) {
                                    role = PersonnelRole.valueOf(entryNode.getTextContent());
                                } else if (entryNode.getNodeName().equalsIgnoreCase("count")) {
                                    count = Integer.parseInt(entryNode.getTextContent());
                                }
                            }
                            if (role != null && count > 0) {
                                retVal.tempCrewMap.put(role, count);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("", ex);
        }

        return retVal;
    }
}
