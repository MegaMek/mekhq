/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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

import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.annotations.Nullable;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Systems;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author Justin "Windchild" Bowen
 */
public class RandomOriginOptions {
    // region Variable Declarations
    private boolean randomizeOrigin;
    private boolean randomizeDependentOrigin;
    private boolean randomizeAroundSpecifiedPlanet;
    private Planet specifiedPlanet;
    private int originSearchRadius;
    private double originDistanceScale;
    private boolean allowClanOrigins;
    private boolean extraRandomOrigin;
    // endregion Variable Declarations

    // region Constructors
    public RandomOriginOptions(final boolean campaignOptions) {
        setRandomizeOrigin(!campaignOptions);
        setRandomizeDependentOrigin(!campaignOptions);
        setRandomizeAroundSpecifiedPlanet(!campaignOptions);
        setSpecifiedPlanet(Systems.getInstance().getSystemById("Terra").getPrimaryPlanet());
        setOriginSearchRadius(campaignOptions ? 45 : 1000);
        setOriginDistanceScale(campaignOptions ? 0.6 : 0.2);
        setAllowClanOrigins(false);
        setExtraRandomOrigin(false);
    }
    // endregion Constructors

    // region Getters/Setters
    /**
     * Gets a value indicating whether to randomize the origin of personnel.
     */
    public boolean isRandomizeOrigin() {
        return randomizeOrigin;
    }

    /**
     * Sets a value indicating whether to randomize the origin of personnel.
     *
     * @param randomizeOrigin true for randomize, otherwise false
     */
    public void setRandomizeOrigin(final boolean randomizeOrigin) {
        this.randomizeOrigin = randomizeOrigin;
    }

    /**
     * Gets a value indicating whether to randomize the origin of dependents
     */
    public boolean isRandomizeDependentOrigin() {
        return randomizeDependentOrigin;
    }

    /**
     * Sets a value indicating whether to randomize the origin of dependents
     *
     * @param randomizeDependentOrigin true for randomize, otherwise false
     */
    public void setRandomizeDependentOrigin(final boolean randomizeDependentOrigin) {
        this.randomizeDependentOrigin = randomizeDependentOrigin;
    }

    /**
     * @return whether to randomize around a specified planet of the campaign's
     *         current planet
     */
    public boolean isRandomizeAroundSpecifiedPlanet() {
        return randomizeAroundSpecifiedPlanet;
    }

    /**
     * @param randomizeAroundSpecifiedPlanet true to randomize around a specified
     *                                       planet, otherwise
     *                                       it will randomize around the campaign's
     *                                       current planet
     */
    public void setRandomizeAroundSpecifiedPlanet(final boolean randomizeAroundSpecifiedPlanet) {
        this.randomizeAroundSpecifiedPlanet = randomizeAroundSpecifiedPlanet;
    }

    /**
     * @return the specified planet to randomize around
     */
    public Planet getSpecifiedPlanet() {
        return specifiedPlanet;
    }

    /**
     * @param specifiedPlanet the new specified planet to randomize around
     */
    public void setSpecifiedPlanet(final Planet specifiedPlanet) {
        this.specifiedPlanet = specifiedPlanet;
    }

    /**
     * Gets the search radius to use for randomizing personnel origins.
     */
    public int getOriginSearchRadius() {
        return originSearchRadius;
    }

    /**
     * Sets the search radius to use for randomizing personnel origins.
     *
     * @param originSearchRadius The search radius.
     */
    public void setOriginSearchRadius(final int originSearchRadius) {
        this.originSearchRadius = originSearchRadius;
    }

    /**
     * Gets the distance scale factor to apply when weighting random origin planets.
     */
    public double getOriginDistanceScale() {
        return originDistanceScale;
    }

    /**
     * Sets the distance scale factor to apply when weighting random origin planets
     * (should be
     * between 0.1 and 2.0, with 0.6 being the standard base). Values above 1.0
     * prefer the current
     * location, while values closer to 0.1 spread out the selection.
     */
    public void setOriginDistanceScale(final double originDistanceScale) {
        this.originDistanceScale = originDistanceScale;
    }

    /**
     * @return if clan origins are allowed to be generated for non-Clan Factions
     */
    public boolean isAllowClanOrigins() {
        return allowClanOrigins;
    }

    public void setAllowClanOrigins(final boolean allowClanOrigins) {
        this.allowClanOrigins = allowClanOrigins;
    }

    /**
     * Gets a value indicating whether to randomize origin to the planetary level,
     * rather than just
     * the system level.
     */
    public boolean isExtraRandomOrigin() {
        return extraRandomOrigin;
    }

    /**
     * Sets a value indicating whether to randomize origin to the planetary level,
     * rather than just
     * the system level.
     */
    public void setExtraRandomOrigin(final boolean extraRandomOrigin) {
        this.extraRandomOrigin = extraRandomOrigin;
    }
    // endregion Getters/Setters

    public Planet determinePlanet(final Planet planet) {
        return isRandomizeAroundSpecifiedPlanet() ? getSpecifiedPlanet() : planet;
    }

    // region File I/O
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "randomOriginOptions");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomizeOrigin", isRandomizeOrigin());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomizeDependentOrigin", isRandomizeDependentOrigin());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomizeAroundSpecifiedPlanet",
                isRandomizeAroundSpecifiedPlanet());
        if (isRandomizeAroundSpecifiedPlanet()) {
            MHQXMLUtility.writeSimpleXMLAttributedTag(pw, indent, "specifiedPlanet", "systemId",
                    getSpecifiedPlanet().getParentSystem().getId(), getSpecifiedPlanet().getId());
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "originSearchRadius", getOriginSearchRadius());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "originDistanceScale", getOriginDistanceScale());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowClanOrigins", isAllowClanOrigins());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "extraRandomOrigin", isExtraRandomOrigin());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "randomOriginOptions");
    }

    /**
     * @param nl              the node list to parse the options from
     * @param campaignOptions if the parser is for a Campaign Options parsing or not
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
                    case "randomizeAroundSpecifiedPlanet":
                        options.setRandomizeAroundSpecifiedPlanet(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "specifiedPlanet":
                        final String specifiedPlanetSystemId = wn.getAttributes().getNamedItem("systemId")
                                .getTextContent().trim();
                        final String specifiedPlanetPlanetId = wn.getTextContent().trim();
                        options.setSpecifiedPlanet(Systems.getInstance().getSystemById(specifiedPlanetSystemId)
                                .getPlanetById(specifiedPlanetPlanetId));
                        break;
                    case "originSearchRadius":
                        options.setOriginSearchRadius(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    case "originDistanceScale":
                        options.setOriginDistanceScale(Double.parseDouble(wn.getTextContent().trim()));
                        break;
                    case "allowClanOrigins":
                        options.setAllowClanOrigins(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "extraRandomOrigin":
                        options.setExtraRandomOrigin(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return null;
        }

        return options;
    }
    // endregion File I/O
}
