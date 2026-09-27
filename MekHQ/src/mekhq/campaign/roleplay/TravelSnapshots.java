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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.AbstractMobileLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.roleplay.TravelLog.ContractInfo;
import mekhq.campaign.roleplay.TravelLog.Observation;
import mekhq.campaign.roleplay.TravelLog.Snapshot;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * Reads where the main force, the bases and the linked cast members are, for the {@link TravelLog}.
 */
public final class TravelSnapshots {
    private TravelSnapshots() {
    }

    /**
     * @param campaign the campaign
     *
     * @return where everyone is now
     */
    public static Snapshot capture(final Campaign campaign) {
        final LocalDate today = campaign.getLocalDate();
        final AbstractLocation forceLocation = campaign.getPlayerForce().getForceDetachment().getCurrentLocation();
        final Observation force = observeForce(forceLocation, today);

        final List<Observation> bases = new ArrayList<>();
        for (PlayerBase base : campaign.getCampaignLocationManager().getPlayerBases()) {
            final Observation observation = observe(base.getId(), base.getDisplayName(), base.getCurrentLocation(),
                  today);
            if (observation != null) {
                bases.add(observation);
            }
        }

        final List<Observation> castAway = new ArrayList<>();
        final Set<UUID> castWithForce = new LinkedHashSet<>();
        for (OracleCharacter character : campaign.getRoleplay().getActiveCharacters()) {
            if (!character.isLinked()) {
                continue;
            }
            final Person person = campaign.getPlayerForce().getHumanResources().getPerson(character.getPersonId());
            if (person == null || !person.getStatus().isActive()) {
                continue;
            }
            final AbstractLocation location = person.getCurrentLocation();
            if (location == null || location == forceLocation) {
                castWithForce.add(character.getId());
            } else {
                final Observation observation = observe(character.getId(), character.getName(), location, today);
                if (observation != null) {
                    castAway.add(observation);
                }
            }
        }
        return new Snapshot(force, bases, castAway, castWithForce);
    }

    /**
     * @param campaign the campaign
     *
     * @return every contract, for the travel log's history and views
     */
    public static List<ContractInfo> contracts(final Campaign campaign) {
        final LocalDate today = campaign.getLocalDate();
        final List<ContractInfo> contracts = new ArrayList<>();
        final TravelLog log = campaign.getRoleplay().getTravelLog();
        for (AbstractContract contract : campaign.getContractHistoryAsMap().values()) {
            final boolean ended = contract.getStatus() != null && !contract.getStatus().isActive();
            // A contract that ended early (breached or cancelled) ended on the day that was recorded, not its
            // scheduled end; contracts that ended before that was recorded fall back to the schedule.
            final LocalDate recordedEnd = log.getContractEnd(contract.getId());
            final LocalDate end = !ended ? null : (recordedEnd != null) ? recordedEnd : contract.getEndingDate();
            contracts.add(new ContractInfo(contract.getId(), contract.getName(), contract.getTargetSystemId(),
                  contract.getTargetSystemName(today), contract.getStartDate(),
                  (end != null && end.isAfter(today)) ? today : end));
        }
        return contracts;
    }

    private static @Nullable Observation observeForce(final @Nullable AbstractLocation location,
          final LocalDate today) {
        if (location == null || location.getCurrentSystem() == null) {
            return null;
        }
        final PlanetarySystem system = location.getCurrentSystem();
        final JumpPath path = location.getJumpPath();
        final PlanetarySystem destination = (path == null || path.isEmpty()) ? null : path.getLastSystem();
        return new Observation(null, "", system.getId(), system.getName(today), destination != null,
              destination == null ? null : destination.getId(),
              destination == null ? null : destination.getName(today));
    }

    private static @Nullable Observation observe(final UUID id, final String name,
          final @Nullable AbstractLocation location, final LocalDate today) {
        if (location == null || location.getCurrentSystem() == null) {
            return null;
        }
        final PlanetarySystem system = location.getCurrentSystem();
        final boolean travelling = location instanceof AbstractMobileLocation mobile && mobile.isInTransit();
        PlanetarySystem destination = null;
        if (travelling) {
            final JumpPath path = location.getJumpPath();
            destination = (path == null || path.isEmpty()) ? system : path.getLastSystem();
        }
        return new Observation(id, name, system.getId(), system.getName(today), travelling,
              destination == null ? null : destination.getId(),
              destination == null ? null : destination.getName(today));
    }
}
