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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission;

import megamek.Version;
import megamek.common.UnitType;
import mekhq.MekHqXmlSerializable;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
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

public class ScenarioDeploymentLimit implements Serializable, MekHqXmlSerializable {

    //region Variable Declarations
    private enum QuantityType {
        PERCENT,
        ABSOLUTE;
    }

    private enum CountType {
        BV,
        UNIT;

        @Override
        public String toString() {
            if(this == BV) {
                return "BV";
            } else if(this == UNIT) {
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
        requiredPersonnel = new ArrayList<UUID>();
        requiredUnits = new ArrayList<UUID>();
        allowedUnitTypes = new ArrayList<Integer>();

        // default will be 100% of all valid units
        quantityLimit = 100;
        quantityType = QuantityType.PERCENT;
        countType = CountType.UNIT;
    }
    //endregion Constructiosn

    //region getter/setter methods

    //endregion getter/setter methods

    public boolean isAllowedType(int unitType) {
        for(int validType : allowedUnitTypes) {
            if(validType == unitType) {
                return true;
            }
        }
        return false;
    }

    private void addAllowedUnitType(int type) {
        allowedUnitTypes.add(type);
    }

    public String getAllowedUnitTypeDesc() {
        ArrayList<String> allowedTypes = new ArrayList<String>();
        for(int allowed: allowedUnitTypes) {
            allowedTypes.add(UnitType.getTypeDisplayableName(allowed));
        }
        return String.join(", ", allowedTypes);
    }

    public String getQuantityLimitDesc(Scenario s, Campaign c) {
        StringBuilder sb = new StringBuilder();
        sb.append(getCurrentQuantity(s, c));
        sb.append("/");
        sb.append(getQuantityCap(c));
        sb.append(" ");
        sb.append(countType.toString());

        if(quantityType == QuantityType.PERCENT) {
            sb.append(" (");
            sb.append(quantityLimit);
            sb.append("% of eligible combat forces");

            sb.append(")");
        }

        return sb.toString();
    }

    public int getUnitQuantity(Unit u, Campaign c) {
        if(null != u && null != u.getEntity() && isAllowedType(u.getEntity().getUnitType())) {
            if(countType == CountType.BV) {
                return u.getEntity().calculateBattleValue();
            } else if(countType == CountType.UNIT) {
                return 1;
            }
        }
        return 0;
    }

    public int getForceQuantity(Force f, Campaign c) {
        int quantity = 0;

        Vector<UUID> unitIds = f.getAllUnits(true);
        for(UUID id : unitIds) {
            Unit u = c.getUnit(id);
            if(null != u && null != u.getEntity() && isAllowedType(u.getEntity().getUnitType())) {
                if(countType == CountType.BV) {
                    quantity += u.getEntity().calculateBattleValue();
                } else if(countType == CountType.UNIT) {
                    quantity += 1;
                }
            }
        }

        return quantity;
    }

    public int getQuantityCap(Campaign c) {
        // TODO: should this be calculated dynamically every time this is called or stored in memory
        // and updated when the force organization is changed?
        if(quantityType == QuantityType.ABSOLUTE) {
            return quantityLimit;
        } else if(quantityType == QuantityType.PERCENT) {
            int totalValue = getForceQuantity(c.getForces(), c);
            return (int) Math.ceil(totalValue * ((double) quantityLimit/100.0));
        } else {
            // should not get here
            return 0;
        }
    }

    public int getCurrentQuantity(Scenario s, Campaign c) {
        // TODO: should this be calculated dynamically every time this is called or stored in memory
        // and updated any time scenario deployment is changed?
        return getForceQuantity(s.getForces(c), c);
    }


    @Override
    public void writeToXml(PrintWriter pw1, int indent) {

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
                }

            }
        }  catch (Exception ex) {
            LogManager.getLogger().error(ex);
        }

        return retVal;
    }
}
