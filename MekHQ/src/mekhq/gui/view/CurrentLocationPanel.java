/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package mekhq.gui.view;

import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.PERSONNEL_MARKET_DISABLED;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.time.LocalDate;
import javax.swing.JLabel;
import javax.swing.UIManager;

import megamek.common.Configuration;
import megamek.common.event.Subscribe;
import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.LocationChangedEvent;
import mekhq.campaign.events.TransitStatusChangedEvent;
import mekhq.campaign.events.missions.MissionEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.TransportCostCalculations;
import mekhq.campaign.universe.Atmosphere;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.campaign.universe.SocioIndustrialData;
import mekhq.campaign.universe.StarUtil;
import mekhq.campaign.universe.enums.HiringHallLevel;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.ScalingVerticalFillImage;
import mekhq.gui.baseComponents.ScalingWidthConstrainedPanel;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.utilities.MHQInternationalization;
import mekhq.utilities.ReportingUtilities;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

/**
 * A UI panel that displays the current location details.
 * <p>
 * Visually represents whether the player is currently on a planet, in transit, or at a jump point, provides relevant
 * planetary statistics (e.g. atmosphere, gravity, socio-industrial data) or travel progress, and includes an entry
 * point to the personnel market.
 * </p>
 * <p>
 * This panel subscribes to the global event bus updates to stay synchronized with changes in the campaign's current
 * location and transit status.
 * </p>
 */
public class CurrentLocationPanel extends ScalingWidthConstrainedPanel {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CurrentLocation";

    private static final File JUMP_SHIP_IMAGE = new File(Configuration.unitImagesDir(), "jumpships/invader.png");

    private final Campaign campaign;

    // UI components
    private final ScalingVerticalFillImage imgLocation = new ScalingVerticalFillImage();
    private final JLabel lblLocationPrimaryInfo = new JLabel();
    private final JLabel lblLocationSecondaryInfo = new JLabel();
    private final RoundedJButton btnRecruitment = new RoundedJButton();

    /**
     * Constructs a new {@code CurrentLocationPanel}.
     *
     * @param minWidth              the minimum enforced width of the panel in pixels
     * @param maxWidth              the maximum enforced width of the panel in pixels
     * @param iconMaxHeight         the maximum enforced height of the location image in pixels
     * @param campaign              the active {@link Campaign} instance
     * @param openRecruitmentDialog a {@link Runnable} that triggers the opening of the Recruitment Dialog UI
     */
    public CurrentLocationPanel(int minWidth, int maxWidth, int iconMaxHeight,
          Campaign campaign, Runnable openRecruitmentDialog) {
        super(minWidth, maxWidth);
        this.campaign = campaign;

        setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(0, CampaignGUI.THIN_GAP, 0, CampaignGUI.MEDIUM_GAP);

        imgLocation.setMaxHeight(iconMaxHeight);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        add(imgLocation, gridBagConstraints);

        gridBagConstraints.gridheight = 1;
        gridBagConstraints.gridx++;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(0, 0, 0, CampaignGUI.SMALL_GAP);
        add(lblLocationPrimaryInfo, gridBagConstraints);

        gridBagConstraints.gridy++;
        add(lblLocationSecondaryInfo, gridBagConstraints);

        // vertical filler
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1;
        add(new JLabel(), gridBagConstraints);

        btnRecruitment.addActionListener(e -> openRecruitmentDialog.run());
        btnRecruitment.setToolTipText(getTextAt("recruitment.tooltip"));
        gridBagConstraints.gridy++;
        gridBagConstraints.weighty = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(btnRecruitment, gridBagConstraints);

        refresh();
        MekHQ.registerHandler(this);
    }

    /**
     * Updates the location title, dynamic images, planetary/transit info text, and the visibility and state of the
     * Hiring Hall button based on the current location context.
     */
    private void refresh() {
        CampaignOptions options = campaign.getCampaignOptions();
        AbstractLocation location = campaign.getCurrentLocation();
        PlanetarySystem system = location.getCurrentSystem();
        LocalDate date = campaign.getLocalDate();

        setBorder(RoundedLineBorder.createRoundedLineBorder(getTitle()));

        File locationImage;
        if (location.isAtJumpPoint()) {
            locationImage = JUMP_SHIP_IMAGE;
        } else {
            locationImage = new File(Configuration.dataDir(), StarUtil.getIconImage(location.getPlanet()));
        }
        float scale = location.isAtJumpPoint() ? 1 : (float) (location.getPercentageTransit() * 1.3 + 0.1);
        imgLocation.setImage(locationImage, scale);

        if (options.getPersonnelMarketStyle() == PERSONNEL_MARKET_DISABLED) {
            // keep the legacy recruitment always available
            btnRecruitment.setEnabled(true);
            btnRecruitment.setText(getTextAt("recruitment.legacy"));
        } else {
            String availabilityMessage = campaign.getNewPersonnelMarket().getAvailabilityMessage();
            btnRecruitment.setEnabled(availabilityMessage.isBlank());

            HiringHallLevel hiringHallLevel = system.getHiringHallLevel(date);
            if (!availabilityMessage.isBlank()) {
                btnRecruitment.setText(availabilityMessage);
            } else if (hiringHallLevel.isNone()) {
                btnRecruitment.setText(getTextAt("recruitment.hiringHall.none"));
            } else {
                btnRecruitment.setText(getFormattedTextAt("recruitment.hiringHall.some",
                      StringUtils.capitalize(hiringHallLevel.name().toLowerCase())));
            }
        }
        if (location.isOnPlanet()) {
            lblLocationPrimaryInfo.setText(getPlanetaryConditionsInfo());
            lblLocationPrimaryInfo.setToolTipText(getTextAt("info.atmosphere.tooltip"));
            lblLocationSecondaryInfo.setText(getSocioIndustrialInfo());
            lblLocationSecondaryInfo.setToolTipText(getTextAt("info.socioIndustrial.tooltip"));
        } else {
            lblLocationPrimaryInfo.setText(getCourseInfo());
            lblLocationPrimaryInfo.setToolTipText(Strings.EMPTY);
            lblLocationSecondaryInfo.setText(getJumpCostInfo());
            lblLocationSecondaryInfo.setToolTipText(Strings.EMPTY);
        }
    }

    /**
     * Generates a detailed title for the current location
     *
     * <p>The result includes:</p>
     * <ul>
     *   <li>The current planet if on a planet, or system name otherwise.</li>
     *   <li>Jumpship charge progress if at a jump point.</li>
     *   <li>Remaining travel time to planet or jump point.</li>
     * </ul>
     *
     * @return a plain text string representing the formatted location title
     */
    public String getTitle() {
        LocalDate date = campaign.getLocalDate();
        AbstractLocation location = campaign.getCurrentLocation();
        PlanetarySystem currentSystem = location.getCurrentSystem();
        if (location.isOnPlanet()) {
            return getFormattedTextAt("title.onPlanet", location.getPlanet().getPrintableName(date));
        }
        String systemName = currentSystem.getPrintableName(date);
        if (location.isAtJumpPoint()) {
            boolean isUseCommandCircuit = campaign.isUseCommandCircuit();
            double neededRechargeTime = currentSystem.getRechargeTime(date, isUseCommandCircuit);
            if (Double.isInfinite(neededRechargeTime)) {
                return getFormattedTextAt("title.chargingImpossible",
                      systemName, currentSystem.getRechargeTimeText(date, isUseCommandCircuit));
            }
            return getFormattedTextAt("title.jumpshipCharging",
                  systemName, location.getRechargeTime() / neededRechargeTime);
        }
        if ((null != location.getJumpPath()) && (currentSystem == location.getJumpPath().getLastSystem())) {
            int daysRemaining = (int) Math.ceil(location.getTransitTime());
            return getFormattedTextAt("title.travelingToPlanet",
                  systemName, daysRemaining, location.getPlanet().getPrintableName(date));
        }
        int daysToJP = (int) Math.ceil(currentSystem.getTimeToJumpPoint(1.0) - location.getTransitTime());
        if (location.isJumpZenith()) {
            return getFormattedTextAt("title.travelingToJumpPoint.zenith", systemName, daysToJP);
        } else {
            return getFormattedTextAt("title.travelingToJumpPoint.nadir", systemName, daysToJP);
        }
    }


    /**
     * Generates a status line for planetary conditions.
     *
     * <p>The description includes:</p>
     * <ul>
     *   <li>Atmosphere type.</li>
     *   <li>Atmospheric pressure.</li>
     *   <li>Temperature.</li>
     *   <li>Gravity.</li>
     * </ul>
     * <p>
     * Each characteristic is colored dynamically based on the severity of the penalties it causes.
     *
     * @return a formatted HTML string representing the planetary conditions
     */
    public String getPlanetaryConditionsInfo() {
        Planet planet = campaign.getCurrentLocation().getPlanet();

        Atmosphere atmosphere = planet.getAtmosphere(campaign.getLocalDate());
        megamek.common.planetaryConditions.Atmosphere pressure = planet.getPressure(campaign.getLocalDate());

        String atmosphereColor = getDefaultFontHexColor();
        if (atmosphere.isTainted() || atmosphere.isToxic()) {
            atmosphereColor = ReportingUtilities.getNegativeColor();
        }
        String atmosphereLabel = atmosphere == Atmosphere.BREATHABLE ? "" : atmosphere.name;

        String pressureColor = getDefaultFontHexColor();
        if (pressure.isTrace() || pressure.isVeryHigh()) {
            pressureColor = ReportingUtilities.getWarningColor();
        } else if (pressure.isVacuum()) {
            pressureColor = ReportingUtilities.getNegativeColor();
        }
        String pressureLabel = pressure.isStandard() ? "" : pressure.name().toLowerCase();
        if (pressure.isHigh() || pressure.isVeryHigh()) {
            pressureLabel += " pressure";
        }

        String gravityColor = getDefaultFontHexColor();
        double gravity = ObjectUtils.firstNonNull(planet.getGravity(), 1.0);
        if (gravity >= 2.0) {
            gravityColor = ReportingUtilities.getNegativeColor();
        } else if (gravity < 0.8 || gravity > 1.2) {
            gravityColor = ReportingUtilities.getWarningColor();
        }

        String temperatureColor = getDefaultFontHexColor();
        // 25 based on StratConContractInitializer's and Scenario's defaults
        double temperature = ObjectUtils.firstNonNull(planet.getTemperature(campaign.getLocalDate()), 25);
        if (temperature < -30 || temperature > 50) {
            temperatureColor = ReportingUtilities.getWarningColor();
        }

        if (atmosphereLabel.isEmpty()) {
            if (pressureLabel.isEmpty()) {
                atmosphereLabel = "Normal";
            } else {
                pressureLabel = StringUtils.capitalize(pressureLabel);
            }
        }
        return getFormattedTextAt("info.atmosphere",
              atmosphereColor, atmosphereLabel, pressureColor, pressureLabel,
              temperatureColor, temperature, gravityColor, gravity);
    }

    /**
     * Generates a description for the current travel course.
     *
     * @return a formatted HTML string detailing the final destination system and remaining jumps, or an empty/fallback
     *       string if the player is not currently traveling.
     */
    public String getCourseInfo() {
        JumpPath jumpPath = campaign.getCurrentLocation().getJumpPath();
        if ((jumpPath == null) || jumpPath.isEmpty()) {
            return getTextAt("info.course.notTraveling");
        } else if (jumpPath.getJumps() == 0) {
            return getTextAt("info.course.inDestinationSystem");
        }
        return getFormattedTextAt("info.course",
              jumpPath.getLastSystem().getPrintableName(campaign.getLocalDate()), jumpPath.getJumps());
    }

    /**
     * Generates jump cost summary line.
     *
     * @return a formatted HTML string with remaining (or weekly average in case of traveling to a planet) jump cost,
     *       information, or an empty string if not traveling
     */
    public String getJumpCostInfo() {
        AbstractLocation location = campaign.getCurrentLocation();
        JumpPath jumpPath = location.getJumpPath();
        if ((jumpPath == null) || jumpPath.isEmpty()) {
            return "";
        }
        TransportCostCalculations calculation = campaign.getTransportCostCalculation(EXP_REGULAR);
        if (jumpPath.getJumps() > 0) {
            int duration = (int) Math.ceil(jumpPath.getTotalTime(campaign.getLocalDate(), location.getTransitTime(),
                  campaign.isUseCommandCircuit()));
            Money jumpCost = calculation.calculateJumpCostForEntireJourney(duration, jumpPath.getJumps());
            return getFormattedTextAt("info.jumpCost.remaining", jumpCost.toAmountString());
        } else {
            Money jumpCost = calculation.calculateJumpCostForEntireJourney(7, 0);
            return getFormattedTextAt("info.jumpCost.average", jumpCost.toAmountString());
        }
    }

    // TODO: reuse the calculation logic from Maintenance::getTargetForMaintenance

    /**
     * Generates a status line detailing the socio-industrial conditions of the current planet.
     *
     * <p>The description includes:</p>
     * <ul>
     *   <li>Technological sophistication.</li>
     *   <li>Industrial development.</li>
     *   <li>Raw material output.</li>
     *   <li>Total population.</li>
     * </ul>
     * <p>
     * Each metric is color-coded based on its rating.
     *
     * @return a formatted HTML string containing the socio-industrial status
     */
    public String getSocioIndustrialInfo() {
        LocalDate date = campaign.getLocalDate();
        Planet planet = campaign.getCurrentLocation().getPlanet();
        SocioIndustrialData status = planet.getSocioIndustrial(date);

        long population = ObjectUtils.firstNonNull(planet.getPopulation(date), 0L);
        String populationLabel;
        if (population > 1e9) {
            populationLabel = String.format("%.0fB", population / 1e9);
        } else if (population > 1e6) {
            populationLabel = String.format("%.0fM", population / 1e6);
        } else if (population > 1e3) {
            populationLabel = String.format("%.0fK", population / 1e3);
        } else {
            populationLabel = Long.toString(population);
        }

        return getFormattedTextAt("info.socioIndustrial",
              getFontColor(status.tech), status.tech.getName(),
              getFontColor(status.industry), status.industry.getName(),
              getFontColor(status.output), status.output.getName(),
              getPopulationFontColor(population), populationLabel);
    }

    private static String getFontColor(PlanetarySophistication rating) {
        return switch (rating) {
            case PlanetarySophistication.ADVANCED -> ReportingUtilities.getAmazingColor();
            case PlanetarySophistication.A -> MekHQ.getMHQOptions().getFontColorPositiveHexColor();
            case PlanetarySophistication.B -> getDefaultFontHexColor();
            case PlanetarySophistication.C -> MekHQ.getMHQOptions().getFontColorWarningHexColor();
            case PlanetarySophistication.D -> ReportingUtilities.getWarningColor();
            case PlanetarySophistication.F -> ReportingUtilities.getNegativeColor();
            case PlanetarySophistication.REGRESSED -> ReportingUtilities.getNegativeColor();
        };
    }

    private static String getFontColor(PlanetaryRating rating) {
        return switch (rating) {
            case PlanetaryRating.A -> MekHQ.getMHQOptions().getFontColorPositiveHexColor();
            case PlanetaryRating.B -> getDefaultFontHexColor();
            case PlanetaryRating.C -> MekHQ.getMHQOptions().getFontColorWarningHexColor();
            case PlanetaryRating.D -> MekHQ.getMHQOptions().getFontColorWarningHexColor();
            case PlanetaryRating.F -> ReportingUtilities.getNegativeColor();
        };
    }

    private static String getPopulationFontColor(long population) {
        if (population > 1e7) {
            return getDefaultFontHexColor();
        } else if (population > 0) {
            return ReportingUtilities.getWarningColor();
        } else {
            return ReportingUtilities.getNegativeColor();
        }
    }

    private static String getDefaultFontHexColor() {
        Color color = UIManager.getColor("Label.foreground");
        return MHQOptions.convertFontColorToHexColor(color == null ? Color.WHITE : color);
    }

    private static String getFormattedTextAt(String key, Object... args) {
        return MHQInternationalization.getFormattedTextAt(RESOURCE_BUNDLE, key, args);
    }

    private static String getTextAt(String key) {
        return MHQInternationalization.getTextAt(RESOURCE_BUNDLE, key);
    }

    // ======================================
    // Event handlers for UI synchronization
    // ======================================

    @Subscribe
    public void handle(LocationChangedEvent event) {
        refresh();
    }

    @Subscribe
    public void handle(TransitStatusChangedEvent event) {
        refresh();
    }

    @Subscribe
    public void handle(MissionEvent event) {
        refresh();
    }

}
