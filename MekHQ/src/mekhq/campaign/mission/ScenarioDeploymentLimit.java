/*
 * Copyright (c) 2021 The Megamek Team. All rights reserved.
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
package mekhq.campaign.mission;

import megamek.Version;
import megamek.common.UnitType;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

/**
 * This class is optionally used by Scenario to determine any limits on the type and quantity of units that the player
 * can field for a scenario. It can be used to limit the types of units that are deployed and to limit the quantity
 * of units. Quantity can be limited as a percent of player's valid force or as an absolute number. This quantity
 * can also be a BV limit or a raw unit count limit. The structure here allows for easy extension to other quantity
 * types.
 *
 * Additionally, this class also can specify UUIDs for required personnel and units. The method Scenario#canStartScenario
 * will return false if these personnel or units are not present in the deployed force.
 */
public class ScenarioDeploymentLimit implements Serializable, MekHqXmlSerializable {

    //region Variable Declarations

    /**
     * The QuantityType enum tells this class how to interpret the meaning quantityLimit variable
     */
    private enum QuantityType {
        /**
         * the PERCENT QuantityType will treat the quantityLimit variable as a percent of the player's valid forces
         */
        PERCENT,
        /**
         * The ABSOLUTE QuantityType will treat the quantityLimit variable as the raw amount of CountType
         */
        ABSOLUTE;
    }

    /**
     * The CountType enum tells this class what the units of the quantity limits should be
     */
    private enum CountType {
        /**
         * The BV CountType will limit deployed forces by BV
         */
        BV,
        /**
         * The UNIT CountType will limit deployed forces by unit count
         */
        UNIT;

        @Override
        public String toString() {
            if (this == BV) {
                return "BV";
            } else if (this == UNIT) {
                return "unit(s)";
            } else {
                return name();
            }
        }
    }

    /** list of personnel ids that are required in the scenario **/
    private List<UUID> requiredPersonnel;

    /** list of unit ids that are required in the scenario **/
    private List<UUID> requiredUnits;

    /** list of UnitType integers that are allowed in the scenario **/
    private List<Integer> allowedUnitTypes;

    /** an integer governing how many units can be deployed **/
    int quantityLimit;

    /** an enum indicating whether the quantity variable indicates percent or absolute number of units **/
    QuantityType quantityType;

    /** an enum indicating whether the quantity variable is a unit or BV count **/
    CountType countType;

    //endregion Variable Declarations

    //region Constructors
    public ScenarioDeploymentLimit() {
        requiredPersonnel = new ArrayList<>();
        requiredUnits = new ArrayList<>();
        allowedUnitTypes = new ArrayList<>();

        // default will be 100% of all valid units
        quantityLimit = 100;
        quantityType = QuantityType.PERCENT;
        countType = CountType.UNIT;
    }
    //endregion Constructors

    //region Unit type methods

    /**
     * Add a UnitType integer to the allowed unit types list
     * @param type an integer giving a unit type
     */
    private void addAllowedUnitType(int type) {
        allowedUnitTypes.add(type);
    }

    /**
     * Determines whether a given unit type is allowed in the scenario. If no unit type limitations have been specified,
     * then this method will return true.
     * @param unitType - an integer giving the UnitType
     * @return a boolean indicating whether the UnitType is allowed.
     */
    public boolean isAllowedType(int unitType) {
        return allowedUnitTypes.isEmpty() || allowedUnitTypes.contains(unitType);
    }

    /**
     * This method returns a description of what unit types are allowed for graphical presentation
     * @return a String of comma separated unit type descriptions
     */
    public String getAllowedUnitTypeDesc() {
        if (allowedUnitTypes.isEmpty()) {
            return "All";
        }
        ArrayList<String> allowedTypes = new ArrayList<>();
        for (int allowed: allowedUnitTypes) {
            allowedTypes.add(UnitType.getTypeDisplayableName(allowed));
        }
        return String.join(", ", allowedTypes);
    }
    //endregion Unit type checks

    //region Unit quantity methods
    /**
     * Determine how much unit quantity this unit counts toward, using the appropriate measurement provided by
     * CountType. Units that are not allowed in this scenario will return zero.
     * @param u - the Unit to be evaluated
     * @return an integer giving the quantity that this unit counts toward in the appropriate units of CountType
     */
    public int getUnitQuantity(Unit u) {
        if ((null != u) && (null != u.getEntity()) && isAllowedType(u.getEntity().getUnitType())) {
            if (countType == CountType.BV) {
                return u.getEntity().calculateBattleValue();
            } else if (countType == CountType.UNIT) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * The total quantity measured in the units of CountType that this force has.
     * @param f - a Force to be evaluated
     * @param c - a pointer to the Campaign
     * @returnthe an integer giving the quantity that this force counts toward in the appropriate units of CountType
     */
    public int getForceQuantity(Force f, Campaign c) {
        int quantity = 0;

        Vector<UUID> unitIds = f.getAllUnits(true);
        for(UUID id : unitIds) {
            Unit u = c.getUnit(id);
            if ((null != u) && (null != u.getEntity()) && isAllowedType(u.getEntity().getUnitType())) {
                if (countType == CountType.BV) {
                    quantity += u.getEntity().calculateBattleValue();
                } else if (countType == CountType.UNIT) {
                    quantity += 1;
                }
            }
        }

        return quantity;
    }

    /**
     * This method calculates the maximum quantity, in appropriate CountType units, that this scenario allows. If
     * quantityType is PERCENT it will do this by looping through all the forces in the TO&E and calculating force
     * quantity.
     * @param c - a pointer to the campaign
     * @return an integer giving the maximum quantity allowed in this scenario
     */
    public int getQuantityCap(Campaign c) {
        if (quantityType == QuantityType.ABSOLUTE) {
            return quantityLimit;
        } else if (quantityType == QuantityType.PERCENT) {
            int totalValue = getForceQuantity(c.getForces(), c);
            return (int) Math.ceil(totalValue * ((double) quantityLimit / 100.0));
        } else {
            // should not get here
            LogManager.getLogger().error("Unable to set quantity cap in ScenarioDeploymentLimit because of unknown quantityType.");
            return 0;
        }
    }

    /**
     * Calculate the quantity, measured in CountType units, of forces currently deployed in the scenario
     * @param s - a Scenario to be evaluated
     * @param c - a pointer to the campaign
     * @return an integer giving the current quantity of deployed forces in this scenario
     */
    public int getCurrentQuantity(Scenario s, Campaign c) {
        return getForceQuantity(s.getForces(c), c);
    }

    /**
     * Provides a String description of the quantity limits of the scenario for grqphical display
     * @param s - a Scenario to get the description of
     * @param c - a point to the Campaign
     * @return a String describing the quantity limits
     */
    public String getQuantityLimitDesc(Scenario s, Campaign c) {
        StringBuilder sb = new StringBuilder();
        sb.append(getCurrentQuantity(s, c));
        sb.append("/");
        sb.append(getQuantityCap(c));
        sb.append(" ");
        sb.append(countType.toString());

        if (quantityType == QuantityType.PERCENT) {
            sb.append(" (");
            sb.append(quantityLimit);
            sb.append("% of eligible combat forces");

            sb.append(")");
        }

        return sb.toString();
    }
    //endregion Unit quantity methods

    //region Required personnel methods

    /**
     * Checks whether any required personnel are currently deployed in the scenario
     * @param s - a Scenario to evaluate
     * @param c - a pointer to the campaign
     * @return a boolean that evaluates to true if all required personnel are currently deployed in the scenario
     */
    public boolean checkRequiredPersonnel(Scenario s, Campaign c) {
        Force f = s.getForces(c);
        if (null == f) {
            return false;
        }
        for (UUID personId : requiredPersonnel) {
            Person p = c.getPerson(personId);
            if ((null == p) || !p.getStatus().isActive()) {
                // skip personnel who are not active or not present
                continue;
            }
            if (!isPersonInForce(personId, f, c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether a given Person is part of a Force
     * @param personId - the id of a Person
     * @param force - a Force
     * @param c - a pointer to the campaign
     * @return a boolean that evaluates to true if the person identified by personId is part of the force
     */
    public boolean isPersonInForce(UUID personId, Force force, Campaign c) {
        Vector<UUID> unitIds = force.getAllUnits(true);
        for (UUID unitId : unitIds) {
            Unit u = c.getUnit(unitId);
            for (Person p : u.getActiveCrew()) {
                if (p.getId().equals(personId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a String giving a description of required personnel for graphical display
     * @param c - a pointer to the campaign
     * @return a String that is a comma separated list of required personnel names
     */
    public String getRequiredPersonnelDesc(Campaign c) {
        ArrayList<String> personNames = new ArrayList<>();
        for (UUID personId: requiredPersonnel) {
            Person p = c.getPerson(personId);
            if ((null != p) && p.getStatus().isActive()) {
                personNames.add(p.getFullName());
            }
        }
        return String.join(", ", personNames);
    }
    //endregion Required personnel methods

    //region Required unit methods
    /**
     * Checks whether any required units are currently deployed in the scenario
     * @param s - a Scenario to evaluate
     * @param c - a pointer to the campaign
     * @return a boolean that evaluates to true if all required units are currently deployed in the scenario
     */
    public boolean checkRequiredUnits(Scenario s, Campaign c) {
        Force f = s.getForces(c);
        if (null == f) {
            return false;
        }
        for (UUID unitId : requiredUnits) {
            Unit u = c.getUnit(unitId);
            if (null == u) {
                // skip units that do not exist
                continue;
            }
            if (!isUnitInForce(unitId, f, c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether a given unit is part of a Force
     * @param unitId - the id of a Person
     * @param force - a Force
     * @param c - a pointer to the campaign
     * @return a boolean that evaluates to true if the unit identified by unitId is part of the force
     */
    public boolean isUnitInForce(UUID unitId, Force force, Campaign c) {
        Vector<UUID> unitIds = force.getAllUnits(true);
        for (UUID id : unitIds) {
            if (id.equals(unitId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a String giving a description of required units for graphical display
     * @param c - a pointer to the campaign
     * @return a String that is a comma separated list of required unit names
     */
    public String getRequiredUnitDesc(Campaign c) {
        ArrayList<String> unitNames = new ArrayList<>();
        for (UUID unitId: requiredUnits) {
            Unit u = c.getUnit(unitId);
            if (null != u) {
                unitNames.add(u.getName());
            }
        }
        return String.join(", ", unitNames);
    }
    //endregion Required unit methods

    //region File I/O
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenTag(pw1, indent++, "scenarioDeploymentLimit");
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "quantityLimit", quantityLimit);
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "quantityType", quantityType.name());
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "countType", countType.name());
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "countType", countType.name());
        for (UUID id : requiredPersonnel) {
            MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "requiredPersonnel", id);
        }
        for (UUID id : requiredUnits) {
            MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "requiredUnit", id);
        }
        if (!allowedUnitTypes.isEmpty()) {
            MekHqXmlUtil.writeSimpleXMLOpenTag(pw1, indent++, "allowedUnitTypes");
            for (int type : allowedUnitTypes) {
                MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "allowedUnitType", type);
            }
            MekHqXmlUtil.writeSimpleXMLCloseTag(pw1, --indent, "allowedUnitTypes");
        }
        MekHqXmlUtil.writeSimpleXMLCloseTag(pw1, --indent, "scenarioDeploymentLimit");
    }

    public static ScenarioDeploymentLimit generateInstanceFromXML(Node wn, Campaign c, Version version) {
        ScenarioDeploymentLimit retVal = new ScenarioDeploymentLimit();

        try {
            // Okay, now load Part-specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("allowedUnitTypes")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int i = 0; i < nl2.getLength(); i++) {
                        Node wn3 = nl2.item(i);
                        if (wn3.getNodeName().equalsIgnoreCase("allowedUnitType")) {
                            retVal.addAllowedUnitType(Integer.parseInt(wn3.getTextContent().trim()));
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("quantityLimit")) {
                    retVal.quantityLimit = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("quantityType")) {
                    retVal.quantityType = QuantityType.valueOf(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("countType")) {
                    retVal.countType = CountType.valueOf(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("requiredPersonnel")) {
                    retVal.requiredPersonnel.add(UUID.fromString(wn2.getTextContent().trim()));
                }  else if (wn2.getNodeName().equalsIgnoreCase("requiredUnit")) {
                    retVal.requiredUnits.add(UUID.fromString(wn2.getTextContent().trim()));
                }

            }
        }  catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

        return retVal;
    }
    //endregion File I/O
}
