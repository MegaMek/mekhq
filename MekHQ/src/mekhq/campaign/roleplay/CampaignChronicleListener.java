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
package mekhq.campaign.roleplay;

import megamek.common.annotations.Nullable;
import megamek.common.event.Subscribe;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.events.LocationAddedEvent;
import mekhq.campaign.events.LocationRemovedEvent;
import mekhq.campaign.events.NewDayEvent;
import mekhq.campaign.events.TransitCompleteEvent;
import mekhq.campaign.events.missions.MissionCompletedEvent;
import mekhq.campaign.events.missions.MissionNewEvent;
import mekhq.campaign.events.persons.PersonNewEvent;
import mekhq.campaign.events.persons.PersonStatusChangedEvent;
import mekhq.campaign.events.scenarios.ScenarioResolvedEvent;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.roleplay.CampaignChronicle.PersonChange;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * Passes MekHQ's campaign events to a {@link CampaignChronicle} and keeps the {@link TravelLog} up to date. Register
 * it on the event bus while a campaign is open and unregister it when the campaign closes. Events about anything that
 * is not in this campaign are ignored, so a campaign being loaded never writes into another's journal.
 */
public class CampaignChronicleListener {
    private static final MMLogger LOGGER = MMLogger.create(CampaignChronicleListener.class);

    private final Campaign campaign;
    private final CampaignChronicle chronicle;

    /**
     * @param campaign the campaign whose journal the chronicle writes to
     */
    public CampaignChronicleListener(final Campaign campaign) {
        this.campaign = campaign;
        this.chronicle = new CampaignChronicle(campaign.getRoleplay(), campaign::getLocalDate,
              () -> campaign.getCampaignOptions().get(CampaignOption.MAXIMUM_ORACLE_LOG_ENTRIES),
              () -> campaign.getCampaignOptions().get(CampaignOption.USE_ORACLE_CHRONICLE));
        updateTravelLog();
    }

    /**
     * Rebuilds the travel history from past contracts the first time a campaign is opened, then records any travel
     * since the last look.
     */
    public void updateTravelLog() {
        guard(() -> {
            TravelLog log = campaign.getRoleplay().getTravelLog();
            if (!log.isHistoryFilled()) {
                log.fillHistory(TravelSnapshots.contracts(campaign), campaign.getLocalDate());
            }
            log.observe(campaign.getLocalDate(), TravelSnapshots.capture(campaign));
        });
    }

    /** Records travel at the start of each day. */
    @Subscribe
    public void handle(final NewDayEvent event) {
        if (event.getCampaign() == campaign) {
            updateTravelLog();
        }
    }

    /** Records a new base. */
    @Subscribe
    public void handle(final LocationAddedEvent event) {
        updateTravelLog();
    }

    /** Records a base closing. */
    @Subscribe
    public void handle(final LocationRemovedEvent event) {
        updateTravelLog();
    }

    /** Records a contract being accepted. */
    @Subscribe
    public void handle(final MissionNewEvent event) {
        guard(() -> {
            AbstractContract contract = ourContract(event.getMission());
            if (contract != null) {
                chronicle.contractStarted(contract.getId(), contract.getName(), contract.getEmployerDisplayName(),
                      (contract.getTargetSystem() == null) ? null
                            : contract.getTargetSystemName(campaign.getLocalDate()));
            }
        });
    }

    /** Records a contract ending, and the day it really ended for the travel log. */
    @Subscribe
    public void handle(final MissionCompletedEvent event) {
        guard(() -> {
            AbstractContract contract = ourContract(event.getMission());
            if (contract != null && contract.getStatus() != null) {
                campaign.getRoleplay().getTravelLog().recordContractEnd(contract.getId(), campaign.getLocalDate());
                chronicle.contractEnded(contract.getId(), contract.getName(), contract.getStatus().toString());
            }
        });
    }

    /** Records a battle being resolved. */
    @Subscribe
    public void handle(final ScenarioResolvedEvent event) {
        guard(() -> {
            Scenario scenario = event.getScenario();
            if (scenario != null && campaign.getScenario(scenario.getId()) == scenario) {
                chronicle.battleResolved(scenario.getId(), scenario.getName(), scenario.getStatus().toString());
            }
        });
    }

    /** Records the company arriving at the end of a journey. */
    @Subscribe
    public void handle(final TransitCompleteEvent event) {
        guard(() -> {
            // Bases and convoys finish journeys too; only the main force's arrival is the company arriving.
            if (event.getLocation() != campaign.getPlayerForce().getForceDetachment().getCurrentLocation()) {
                return;
            }
            PlanetarySystem system = campaign.getCurrentSystem();
            if (system != null) {
                chronicle.arrived(system.getName(campaign.getLocalDate()));
            }
        });
        updateTravelLog();
    }

    /** Records someone joining or being born; prisoners are left out. */
    @Subscribe
    public void handle(final PersonNewEvent event) {
        guard(() -> {
            Person person = ourPerson(event.getPerson());
            if (person == null || person.getPrisonerStatus().isCurrentPrisoner()) {
                return;
            }
            boolean born = campaign.getLocalDate().equals(person.getDateOfBirth());
            chronicle.personChanged(born ? PersonChange.BORN : PersonChange.JOINED, person.getId(),
                  person.getFullName(), born ? null : person.getPrimaryRoleDesc());
        });
    }

    /** Records someone dying, going missing, being captured or leaving. */
    @Subscribe
    public void handle(final PersonStatusChangedEvent event) {
        guard(() -> {
            Person person = ourPerson(event.getPerson());
            if (person == null) {
                return;
            }
            PersonChange change = changeFor(person.getStatus());
            if (change != null) {
                chronicle.personChanged(change, person.getId(), person.getFullName(), person.getStatus().getLabel());
            }
        });
    }

    /**
     * @param status someone's new status
     *
     * @return the chronicle entry it belongs in, or {@code null} for statuses the chronicle does not record
     */
    static @Nullable PersonChange changeFor(final PersonnelStatus status) {
        if (status.isDead()) {
            return PersonChange.KILLED;
        }
        if (status.isMIA()) {
            return PersonChange.MISSING;
        }
        if (status.isPoW()) {
            return PersonChange.CAPTURED;
        }
        if (status.isRetired() || status.isResigned() || status.isSacked() || status.isDeserted()
                  || status.isDefected() || status.isLeft() || status.isDishonorablyDischarged()) {
            return PersonChange.DEPARTED;
        }
        return null;
    }

    /**
     * Matching the object, not just the id, keeps out a copy of this campaign being loaded alongside it.
     *
     * @return the contract if it is this campaign's, otherwise {@code null}
     */
    private @Nullable AbstractContract ourContract(final @Nullable AbstractContract contract) {
        return (contract != null && campaign.getContractHistoryAsMap().get(contract.getId()) == contract)
                     ? contract : null;
    }

    private @Nullable Person ourPerson(final @Nullable Person person) {
        return (person != null && campaign.getPlayerForce().getHumanResources().getPerson(person.getId()) == person)
                     ? person : null;
    }

    /** The chronicle is a nicety: a problem writing it must never interrupt the campaign. */
    private static void guard(final Runnable action) {
        try {
            action.run();
        } catch (Exception exception) {
            LOGGER.error("Failed to record a campaign event in the Oracle journal", exception);
        }
    }
}
