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
package mekhq.gui.baseComponents.immersiveDialogs;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.HPGLink;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HPGRating;

/**
 * Resolves immersive-dialog signal quality from authoritative campaign locations and dated HPG links.
 *
 * <p>For visual quality only, dated A/B links form an undirected reachability graph. This does not model canonical
 * HPG routing or latency.</p>
 */
final class TransmissionSignalQualityResolver {
    private TransmissionSignalQualityResolver() {
    }

    static TransmissionSignalQuality resolve(@Nullable Campaign campaign, @Nullable Person leftSpeaker,
          @Nullable Person rightSpeaker) {
        if (campaign == null || (leftSpeaker == null && rightSpeaker == null)) {
            return TransmissionSignalQuality.REMOTE;
        }

        PlanetarySystem campaignSystem = campaign.getCurrentSystem();
        if (campaignSystem == null) {
            return TransmissionSignalQuality.REMOTE;
        }

        TransmissionSignalQuality quality = TransmissionSignalQuality.CLEAR;
        for (Person speaker : new Person[] { leftSpeaker, rightSpeaker }) {
            if (speaker == null) {
                continue;
            }

            TransmissionSignalQuality speakerQuality = resolveSpeaker(campaign, campaignSystem, speaker);
            if (speakerQuality == TransmissionSignalQuality.DEGRADED) {
                return TransmissionSignalQuality.DEGRADED;
            }
            if (speakerQuality == TransmissionSignalQuality.REMOTE) {
                quality = TransmissionSignalQuality.REMOTE;
            }
        }
        return quality;
    }

    private static TransmissionSignalQuality resolveSpeaker(Campaign campaign, PlanetarySystem campaignSystem,
          Person speaker) {
        if (!isCampaignRosterSpeaker(campaign, speaker)) {
            return TransmissionSignalQuality.REMOTE;
        }

        PlanetarySystem speakerSystem = speaker.getCurrentSystem();
        if (speakerSystem == null) {
            return TransmissionSignalQuality.REMOTE;
        }
        if (Objects.equals(campaignSystem, speakerSystem)) {
            return TransmissionSignalQuality.CLEAR;
        }

        return hasUsablePath(campaign, campaignSystem, speakerSystem)
                     ? TransmissionSignalQuality.REMOTE
                     : TransmissionSignalQuality.DEGRADED;
    }

    private static boolean isCampaignRosterSpeaker(Campaign campaign, Person speaker) {
        PlayerForce playerForce = campaign.getPlayerForce();
        if (playerForce == null || playerForce.getHumanResources() == null) {
            return false;
        }

        Collection<Person> personnel = playerForce.getHumanResources().getPersonnel();
        return personnel != null && personnel.contains(speaker);
    }

    private static boolean hasUsablePath(Campaign campaign, PlanetarySystem start, PlanetarySystem destination) {
        LocalDate date = campaign.getLocalDate();
        if (date == null || !hasUsableRating(start, date) || !hasUsableRating(destination, date)) {
            return false;
        }

        Collection<HPGLink> links = campaign.getHPGNetwork();
        if (links == null || links.isEmpty()) {
            return false;
        }

        Map<PlanetarySystem, Set<PlanetarySystem>> graph = new HashMap<>();
        for (HPGLink link : links) {
            if (!isUsableLink(link, date)) {
                continue;
            }
            graph.computeIfAbsent(link.primary(), ignored -> new HashSet<>()).add(link.secondary());
            graph.computeIfAbsent(link.secondary(), ignored -> new HashSet<>()).add(link.primary());
        }

        ArrayDeque<PlanetarySystem> systemsToVisit = new ArrayDeque<>();
        Set<PlanetarySystem> visitedSystems = new HashSet<>();
        systemsToVisit.add(start);
        visitedSystems.add(start);
        while (!systemsToVisit.isEmpty()) {
            PlanetarySystem currentSystem = systemsToVisit.removeFirst();
            for (PlanetarySystem neighbor : graph.getOrDefault(currentSystem, Set.of())) {
                if (Objects.equals(neighbor, destination)) {
                    return true;
                }
                if (visitedSystems.add(neighbor)) {
                    systemsToVisit.addLast(neighbor);
                }
            }
        }
        return false;
    }

    private static boolean isUsableLink(@Nullable HPGLink link, LocalDate date) {
        return link != null && link.primary() != null && link.secondary() != null
                     && isUsableRating(link.rating())
                     && hasUsableRating(link.primary(), date)
                     && hasUsableRating(link.secondary(), date);
    }

    private static boolean hasUsableRating(PlanetarySystem system, LocalDate date) {
        return isUsableRating(system.getHPG(date));
    }

    private static boolean isUsableRating(@Nullable HPGRating rating) {
        return rating == HPGRating.A || rating == HPGRating.B;
    }
}
