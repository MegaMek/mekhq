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

package mekhq.campaign;

import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.Campaign.AdministratorSpecialization.TRANSPORT;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.personnel.PersonnelOptions.FLAW_TRANSIT_DISORIENTATION_SYNDROME;
import static mekhq.campaign.personnel.medical.BodyLocation.GENERIC;
import static mekhq.campaign.personnel.medical.BodyLocation.INTERNAL;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.CanonicalDiseaseType.getAllActiveBioweapons;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.CanonicalDiseaseType.getAllActiveDiseases;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.CanonicalDiseaseType.getAllSystemSpecificDiseasesWithCures;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import org.w3c.dom.Node;

/**
 * Abstract implementation of a specific location. An {@code AbstractLocation} is expected as the
 * {@link ILocation ILocation locatable} of the root {@link LocationNode} in a {@code LocationNode} tree.
 */
public abstract class AbstractLocation implements ILocation {
    protected static final MMLogger logger = MMLogger.create(AbstractLocation.class);
    static final String RESOURCE_BUNDLE = "mekhq.resources.CurrentLocation";

    @XmlElement(name = "currentSystemId")
    @XmlJavaTypeAdapter(PlanetarySystemAdapter.class)
    protected PlanetarySystem currentSystem;

    @XmlTransient
    protected LocationNode locationNode;

    public AbstractLocation(PlanetarySystem system) {
        this.currentSystem = system;
        locationNode = new LocationNode(this);
    }

    @Override
    public boolean isOnPlanet() {
        return true;
    }

    @Override
    public boolean isAtJumpPoint() {
        return false;
    }

    @Override
    public boolean isInTransit() {
        return false;
    }

    @Override
    public double getPercentageTransit() {
        return 1.0;
    }

    @Override
    public boolean isJumpZenith() {
        return false;
    }

    @Override
    public double getTransitTime() {
        return 0.0;
    }

    public void setTransitTime(double time) {}

    public boolean isRecharging(Campaign campaign) {
        return false;
    }

    public void chargeFully(Campaign campaign) {}

    @Override
    public JumpPath getJumpPath() {
        return null;
    }

    @Override
    public void setJumpPath(JumpPath path) {}

    @Override
    public PlanetarySystem getCurrentSystem() {
        return currentSystem;
    }

    /**
     * @return the current planet location. This is currently the primary planet of the system, but in the future this
     *       will not be the case.
     */
    @Override
    public Planet getPlanet() {
        return getCurrentSystem().getPrimaryPlanet();
    }

    @Override
    @Nullable
    public LocationNode getLocationNode() {
        return locationNode;
    }

    public boolean computeIsUseCommandCircuit(Campaign campaign) {
        return computeIsUseCommandCircuit(campaign, campaign.getCampaignOptions());
    }

    protected boolean computeIsUseCommandCircuit(Campaign campaign, CampaignOptions campaignOptions) {
        return FactionStandingUtilities.isUseCommandCircuit(
              campaign.isOverridingCommandCircuitRequirements(),
              campaign.isGM(),
              campaignOptions.isUseFactionStandingCommandCircuitSafe(),
              campaign.getFactionStandings(),
              campaign.getFutureAtBContracts());
    }

    public double getRechargeTime() {
        return 0.0;
    }

    protected void setRechargeTime(double t) {}

    /**
     * Applies up to {@code availableHours} of JumpShip recharging at the current system and reports progress.
     *
     * @return the number of hours actually used for recharging
     */
    protected double applyRechargeForHours(Campaign campaign, LocalDate today, boolean isUseCommandCircuit,
          double availableHours, boolean isSilentProcessing) {
        double neededRechargeTime = currentSystem.getRechargeTime(today, isUseCommandCircuit);
        double usedRechargeTime = Math.min(availableHours, neededRechargeTime - getRechargeTime());
        if (usedRechargeTime > 0) {
            if (!isSilentProcessing) {
                campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE, "getReport.recharge.hours",
                                                  Math.round(100.0 * usedRechargeTime) / 100.0));
            }
            setRechargeTime(getRechargeTime() + usedRechargeTime);
            if (getRechargeTime() >= neededRechargeTime && !isSilentProcessing) {
                campaign.addReport(GENERAL, getTextAt(RESOURCE_BUNDLE, "getReport.recharge.complete"));
            }
        }
        return usedRechargeTime;
    }

    // recharge even if there is no jump path because JumpShips don't go anywhere
    public void newDay(Campaign campaign, boolean isSilentProcessing) {
        LocalDate today = campaign.getLocalDate();
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        applyRechargeForHours(campaign, today, computeIsUseCommandCircuit(campaign, campaignOptions), 24.0,
              isSilentProcessing);
    }

    void checkForDiseaseOrBioweaponOutbreaks(Campaign campaign, LocalDate today) {
        Set<InjuryType> availableCures = getAllSystemSpecificDiseasesWithCures(currentSystem.getId(), today, true);

        Set<InjuryType> activeBioweapons = getAllActiveBioweapons(currentSystem.getId(), today, true);
        for (InjuryType bioweapon : activeBioweapons) {
            String centerMessage = getFormattedTextAt(RESOURCE_BUNDLE, "bioweaponAttack.inCharacter",
                  campaign.getCommanderAddress());
            String bottomMessage = getFormattedTextAt(RESOURCE_BUNDLE, "bioweaponAttack.outOfCharacter",
                  currentSystem.getName(today), bioweapon.getSimpleName());
            bottomMessage += availableCures.contains(bioweapon)
                                   ? getTextAt(RESOURCE_BUNDLE, "disease.outOfCharacter.vaccineStatus.available")
                                   : getTextAt(RESOURCE_BUNDLE, "disease.outOfCharacter.vaccineStatus.none");

            new ImmersiveDialogSimple(campaign, campaign.getSeniorMedicalPerson(), null,
                  centerMessage, null, bottomMessage, null, false, ImmersiveDialogWidth.LARGE);
        }

        Set<InjuryType> activeDiseases = getAllActiveDiseases(currentSystem.getId(), today, true);
        for (InjuryType disease : activeDiseases) {
            String centerMessage = getFormattedTextAt(RESOURCE_BUNDLE, "diseaseOutbreak.inCharacter",
                  campaign.getCommanderAddress());
            centerMessage += availableCures.contains(disease)
                                   ? getTextAt(RESOURCE_BUNDLE, "disease.outOfCharacter.vaccineStatus.available")
                                   : getTextAt(RESOURCE_BUNDLE, "disease.outOfCharacter.vaccineStatus.none");

            new ImmersiveDialogNotification(campaign, centerMessage, true);
        }
    }

    /**
     * Tests for whether the campaign arrived at a contract location before it's due to start.
     *
     * <p>The first matching contract in the system ends the loop after handling early arrival notifications.</p>
     *
     * @param campaign The {@link Campaign} instance.
     */
    void testForEarlyArrival(Campaign campaign) {
        for (Contract contract : campaign.getFutureContracts()) {
            if (Objects.equals(currentSystem, contract.getSystem())) {
                int daysTillStart = campaign.getLocalDate().until(contract.getStartDate()).getDays();

                String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
                      "contract.arrivedEarly.ic." + randomInt(10),
                      campaign.getCommanderAddress(),
                      daysTillStart);

                new ImmersiveDialogSimple(campaign, campaign.getSeniorAdminPerson(TRANSPORT), null,
                      inCharacterMessage, null,
                      getFormattedTextAt(RESOURCE_BUNDLE, "contract.arrivedEarly.ooc"),
                      null, false);
                break;
            }
        }
    }

    /**
     * Applies Transit Disorientation Syndrome effects to all personnel who have the corresponding flaw.
     *
     * @param campaign        the current campaign
     * @param campaignOptions the campaign's ruleset and configuration
     */
    static void checkForTransitDisorientationSyndrome(Campaign campaign, CampaignOptions campaignOptions) {
        final boolean useAdvancedMedical = campaignOptions.isUseAdvancedMedical();
        final boolean useAltAdvancedMedical = campaignOptions.isUseAlternativeAdvancedMedical();
        final boolean useFatigue = campaignOptions.isUseFatigue();
        final int fatigueRate = campaignOptions.getFatigueRate();

        for (Person person : campaign.getPersonnelFilteringOutDepartedAndAbsent()) {
            if (!person.getOptions().booleanOption(FLAW_TRANSIT_DISORIENTATION_SYNDROME)) {
                continue;
            }

            if (useAdvancedMedical) {
                person.addInjury(createTransitDisorientationInjury(campaign, person, useAltAdvancedMedical));
            } else {
                person.setHits(person.getHits() + 1);
            }

            if (useFatigue) {
                person.changeFatigue(fatigueRate);
            }
        }
    }

    private static Injury createTransitDisorientationInjury(Campaign campaign, Person person,
          boolean useAltAdvancedMedical) {
        return useAltAdvancedMedical
                     ? AlternateInjuries.TRANSIT_DISORIENTATION_SYNDROME.newInjury(campaign, person, GENERIC, 1)
                     : InjuryTypes.TRANSIT_DISORIENTATION_SYNDROME.newInjury(campaign, person, INTERNAL, 1);
    }

    public abstract void writeToXML(PrintWriter writer, int indent);

    /**
     * Dispatches XML deserialization to the correct {@link AbstractLocation} subclass based on the element name of
     * {@code wn}.
     *
     * @return the deserialized location, or {@code null} if the node name is unrecognized
     */
    public static AbstractLocation generateInstanceFromXML(Node wn, Campaign campaign) {
        return switch (wn.getNodeName().toLowerCase()) {
            case "location" -> CurrentLocation.generateInstanceFromXML(wn, campaign);
            case "fixedlocation" -> FixedLocation.generateInstanceFromXML(wn, campaign);
            default -> {
                logger.warn("Unrecognized location node '{}' — skipping", wn.getNodeName());
                yield null;
            }
        };
    }

    static class PlanetarySystemAdapter extends XmlAdapter<String, PlanetarySystem> {
        private final Campaign campaign;

        @SuppressWarnings("unused")
        public PlanetarySystemAdapter() {
            this.campaign = null;
        }

        public PlanetarySystemAdapter(Campaign campaign) {
            this.campaign = campaign;
        }

        @Override
        public PlanetarySystem unmarshal(String id) {
            PlanetarySystem p = Systems.getInstance().getSystemById(id);
            if (p != null) {
                return p;
            }
            logger.error("Couldn't find system: {}", id);
            if (campaign == null) {
                return null;
            }
            p = campaign.getSystemByName("Terra");
            return p != null ? p : campaign.getSystems().get(0);
        }

        @Override
        public String marshal(PlanetarySystem p) {
            return p != null ? p.getId() : null;
        }
    }
}
