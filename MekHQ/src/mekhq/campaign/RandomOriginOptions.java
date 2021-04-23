/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Systems;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.time.LocalDate;

public class RandomOriginOptions implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 8347933663775775825L;

    private boolean randomizeOrigin;
    private boolean randomizeDependentOrigin;
    private boolean randomizeAroundCentralPlanet;
    private Planet centralPlanet;
    private int originSearchRadius;
    private boolean extraRandomOrigin;
    private double originDistanceScale;
    //endregion Variable Declarations

    //region Constructors
    public RandomOriginOptions(final boolean campaignOptions) {
        setRandomizeOrigin(!campaignOptions);
        setRandomizeDependentOrigin(!campaignOptions);
        setRandomizeAroundCentralPlanet(!campaignOptions);
        setCentralPlanet(Systems.getInstance().getSystemByName("Terra", LocalDate.ofYearDay(3067, 1))
                .getPrimaryPlanet());
        setOriginSearchRadius(campaignOptions ? 45 : 1000);
        setExtraRandomOrigin(false);
        setOriginDistanceScale(campaignOptions ? 0.6 : 0.2);
    }
    //endregion Constructors

    //region Getters/Setters
    /**
     * Gets a value indicating whether or not to randomize the origin of personnel.
     */
    public boolean isRandomizeOrigin() {
        return randomizeOrigin;
    }

    /**
     * Sets a value indicating whether or not to randomize the origin of personnel.
     * @param randomizeOrigin true for randomize, otherwise false
     */
    public void setRandomizeOrigin(final boolean randomizeOrigin) {
        this.randomizeOrigin = randomizeOrigin;
    }

    /**
     * Gets a value indicating whether or not to randomize the origin of dependents
     */
    public boolean isRandomizeDependentOrigin() {
        return randomizeDependentOrigin;
    }

    /**
     * Sets a value indicating whether or not to randomize the origin of dependents
     * @param randomizeDependentOrigin true for randomize, otherwise false
     */
    public void setRandomizeDependentOrigin(final boolean randomizeDependentOrigin) {
        this.randomizeDependentOrigin = randomizeDependentOrigin;
    }

    /**
     * @return whether to randomize around a central planet of the campaign's current planet
     */
    public boolean isRandomizeAroundCentralPlanet() {
        return randomizeAroundCentralPlanet;
    }

    /**
     * @param randomizeAroundCentralPlanet true to randomize around a central planet, otherwise
     *                                     it will randomize around the campaign's current planet
     */
    public void setRandomizeAroundCentralPlanet(final boolean randomizeAroundCentralPlanet) {
        this.randomizeAroundCentralPlanet = randomizeAroundCentralPlanet;
    }

    /**
     * @return the central planet to randomize around
     */
    public Planet getCentralPlanet() {
        return centralPlanet;
    }

    /**
     * @param centralPlanet the new central planet to randomize around
     */
    public void setCentralPlanet(final Planet centralPlanet) {
        this.centralPlanet = centralPlanet;
    }

    /**
     * Gets the search radius to use for randomizing personnel origins.
     */
    public int getOriginSearchRadius() {
        return originSearchRadius;
    }

    /**
     * Sets the search radius to use for randomizing personnel origins.
     * @param originSearchRadius The search radius.
     */
    public void setOriginSearchRadius(final int originSearchRadius) {
        this.originSearchRadius = originSearchRadius;
    }

    /**
     * Gets a value indicating whether or not to randomize origin to the planetary level, rather
     * than just the system level.
     */
    public boolean isExtraRandomOrigin() {
        return extraRandomOrigin;
    }

    /**
     * Sets a value indicating whether or not to randomize origin to the planetary level, rather
     * than just the system level.
     */
    public void setExtraRandomOrigin(final boolean extraRandomOrigin) {
        this.extraRandomOrigin = extraRandomOrigin;
    }

    /**
     * Gets the distance scale factor to apply when weighting random origin planets.
     */
    public double getOriginDistanceScale() {
        return originDistanceScale;
    }

    /**
     * Sets the distance scale factor to apply when weighting random origin planets (should be
     * between 0.1 and 2.0, with 0.6 being the standard base)
     */
    public void setOriginDistanceScale(final double originDistanceScale) {
        this.originDistanceScale = originDistanceScale;
    }
    //endregion Getters/Setters

    //region File I/O
    public void writeToXML(final PrintWriter pw, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw, indent++, "randomOriginOptions");
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "randomizeOrigin", isRandomizeOrigin());
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "randomizeDependentOrigin", isRandomizeDependentOrigin());
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "randomizeAroundCentralPlanet", isRandomizeAroundCentralPlanet());
        MekHqXmlUtil.writeAttributedTag(pw, indent, "centralPlanet", "systemId",
                getCentralPlanet().getParentSystem().getId(), getCentralPlanet().getId());
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "originSearchRadius", getOriginSearchRadius());
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "extraRandomOrigin", isExtraRandomOrigin());
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "originDistanceScale", getOriginDistanceScale());
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, --indent, "randomOriginOptions");
    }

    /**
     * @param nl the node list to parse the options from
     * @return the parsed random origin options, or null if the parsing fails
     */
    public static @Nullable RandomOriginOptions parseFromXML(final NodeList nl,
                                                             final boolean campaignOptions) {
        final RandomOriginOptions options = new RandomOriginOptions(campaignOptions);
        try {
            for (int i = 0; i < nl.getLength(); i++) {
                final Node wn = nl.item(i);
                switch (wn.getNodeName()) {
                    case "randomizeOrigin":
                        options.setRandomizeOrigin(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "randomizeDependentOrigin":
                        options.setRandomizeDependentOrigin(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "randomizeAroundCentralPlanet":
                        options.setRandomizeAroundCentralPlanet(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "centralPlanet":
                        String centralPlanetSystemId = wn.getAttributes().getNamedItem("systemId").getTextContent().trim();
                        String centralPlanetPlanetId = wn.getTextContent().trim();
                        options.setCentralPlanet(Systems.getInstance().getSystemById(centralPlanetSystemId).getPlanetById(centralPlanetPlanetId));
                        break;
                    case "originSearchRadius":
                        options.setOriginSearchRadius(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    case "extraRandomOrigin":
                        options.setExtraRandomOrigin(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "originDistanceScale":
                        options.setOriginDistanceScale(Double.parseDouble(wn.getTextContent().trim()));
                        break;
                }
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
            return null;
        }

        return options;
    }
    //endregion File I/O
}
