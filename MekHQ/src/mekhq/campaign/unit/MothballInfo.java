/*
 * MothballInfo.java
 *
 * Copyright (c) 2018 - The Megamek Team. All rights reserved.
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
package mekhq.campaign.unit;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.utilities.MHQXMLUtility;

/**
 * This class is used to store information about a particular unit that is
 * lost when a unit is mothballed, so that it may be restored to as close to
 * its prior state as possible when the unit is reactivated.
 *
 * @author NickAragua
 */
public class MothballInfo {
    private static final MMLogger logger = MMLogger.create(MothballInfo.class);

    private Person tech;
    private int forceID;
    private List<Person> drivers;
    private List<Person> gunners;
    private List<Person> vesselCrew;
    private Person techOfficer;
    private Person navigator;

    /**
     * Parameterless constructor, used for deserialization.
     */
    private MothballInfo() {
        drivers = new ArrayList<>();
        gunners = new ArrayList<>();
        vesselCrew = new ArrayList<>();
    }

    /**
     * Who was the original tech of this vessel?
     * @return The original tech
     */
    public Person getTech() {
        return tech;
    }

    /**
     * Creates a set of mothball info for a given unit
     *
     * @param unit The unit to work with
     */
    public MothballInfo(Unit unit) {
        tech = unit.getTech();
        forceID = unit.getForceId();
        drivers = new ArrayList<>(unit.getDrivers());
        gunners = new ArrayList<>(unit.getGunners());
        vesselCrew = new ArrayList<>(unit.getVesselCrew());
        techOfficer = unit.getTechOfficer();
        navigator = unit.getNavigator();
    }

    /**
     * Restore a unit's pilot, assigned tech and force, to the best of our ability
     *
     * @param unit     The unit to restore
     * @param campaign The campaign in which this is happening
     */
    public void restorePreMothballInfo(Unit unit, Campaign campaign) {
        if (tech != null) {
            unit.setTech(tech);
        }

        for (Person driver : drivers) {
            if (driver.getStatus().isActive() && (driver.getUnit() == null)) {
                unit.addDriver(driver);
            }
        }

        for (Person gunner : gunners) {
            // add the gunner if they exist, aren't dead/retired/etc and aren't already
            // assigned to some
            // other unit. Caveat: single-person units have the same driver and gunner.
            if (gunner.getStatus().isActive() &&
                    ((gunner.getUnit() == null) || (gunner.getUnit() == unit))) {
                unit.addGunner(gunner);
            }
        }

        for (Person crew : vesselCrew) {
            if (crew.getStatus().isActive() && (crew.getUnit() == null)) {
                unit.addVesselCrew(crew);
            }
        }

        if ((techOfficer != null) && (techOfficer.getStatus().isActive()) && (techOfficer.getUnit() == null)) {
            unit.setTechOfficer(techOfficer);
        }

        if ((navigator != null) && (navigator.getStatus().isActive()) && (navigator.getUnit() == null)) {
            unit.setNavigator(navigator);
        }

        if (campaign.getForce(forceID) != null) {
            campaign.addUnitToForce(unit, forceID);
        }

        unit.resetEngineer();
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "mothballInfo");
        if (tech != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "techId", tech.getId());
        }

        if (forceID > 0) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forceID", forceID);
        }

        for (Person driver : drivers) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "driverId", driver.getId());
        }

        for (Person gunner : gunners) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "gunnerId", gunner.getId());
        }

        for (Person crew : vesselCrew) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "vesselCrewId", crew.getId());
        }

        if (navigator != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "navigatorId", navigator.getId());
        }

        if (techOfficer != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "techOfficerId", techOfficer.getId());
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
                    retVal.tech = new MothballInfoPersonRef(UUID.fromString(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("forceID")) {
                    retVal.forceID = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("driverID")) {
                    retVal.drivers.add(new MothballInfoPersonRef(UUID.fromString(wn2.getTextContent())));
                } else if (wn2.getNodeName().equalsIgnoreCase("gunnerID")) {
                    retVal.gunners.add(new MothballInfoPersonRef(UUID.fromString(wn2.getTextContent())));
                } else if (wn2.getNodeName().equalsIgnoreCase("vesselCrewID")) {
                    retVal.vesselCrew.add(new MothballInfoPersonRef(UUID.fromString(wn2.getTextContent())));
                } else if (wn2.getNodeName().equalsIgnoreCase("techOfficerID")) {
                    retVal.techOfficer = new MothballInfoPersonRef(UUID.fromString(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("navigatorID")) {
                    retVal.navigator = new MothballInfoPersonRef(UUID.fromString(wn2.getTextContent()));
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }

        return retVal;
    }

    /**
     * Represents an unresolved reference to a Person from a MothballInfo instance.
     */
    public static class MothballInfoPersonRef extends Person {
        public MothballInfoPersonRef(UUID id) {
            super(id);
        }
    }

    public void fixReferences(Campaign campaign) {
        if (tech instanceof MothballInfoPersonRef) {
            UUID id = tech.getId();
            tech = campaign.getPerson(id);
            if (tech == null) {
                logger.error(
                        String.format("Mothball info references missing tech %s", id));
            }
        }
        for (int ii = drivers.size() - 1; ii >= 0; --ii) {
            Person driver = drivers.get(ii);
            if (driver instanceof MothballInfoPersonRef) {
                drivers.set(ii, campaign.getPerson(driver.getId()));
                if (drivers.get(ii) == null) {
                    logger.error(
                            String.format("Mothball info references missing driver %s",
                                    driver.getId()));
                    drivers.remove(ii);
                }
            }
        }
        for (int ii = gunners.size() - 1; ii >= 0; --ii) {
            Person gunner = gunners.get(ii);
            if (gunner instanceof MothballInfoPersonRef) {
                gunners.set(ii, campaign.getPerson(gunner.getId()));
                if (gunners.get(ii) == null) {
                    logger.error(
                            String.format("Mothball info references missing gunner %s",
                                    gunner.getId()));
                    gunners.remove(ii);
                }
            }
        }
        for (int ii = vesselCrew.size() - 1; ii >= 0; --ii) {
            Person crew = vesselCrew.get(ii);
            if (crew instanceof MothballInfoPersonRef) {
                vesselCrew.set(ii, campaign.getPerson(crew.getId()));
                if (vesselCrew.get(ii) == null) {
                    logger.error(
                            String.format("Mothball info references missing vessel crew %s",
                                    crew.getId()));
                    vesselCrew.remove(ii);
                }
            }
        }
        if (navigator instanceof MothballInfoPersonRef) {
            UUID id = navigator.getId();
            navigator = campaign.getPerson(id);
            if (navigator == null) {
                logger.error(
                        String.format("Mothball info references missing navigator %s", id));
            }
        }
    }
}
