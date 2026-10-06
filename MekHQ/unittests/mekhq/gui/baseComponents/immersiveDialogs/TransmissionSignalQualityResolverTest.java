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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.HPGLink;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HPGRating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TransmissionSignalQualityResolverTest {
    private static final LocalDate DATE = LocalDate.of(3152, 6, 1);

    private Campaign campaign;
    private PlayerForce playerForce;
    private ForceHumanResources humanResources;
    private PlanetarySystem campaignSystem;

    @BeforeEach
    void setUp() {
        campaign = mock(Campaign.class);
        playerForce = mock(PlayerForce.class);
        humanResources = mock(ForceHumanResources.class);
        campaignSystem = systemWithRating(HPGRating.A);

        when(campaign.getCurrentSystem()).thenReturn(campaignSystem);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getHumanResources()).thenReturn(humanResources);
        when(humanResources.getPersonnel()).thenReturn(List.of());
        when(campaign.getLocalDate()).thenReturn(DATE);
        when(campaign.getHPGNetwork()).thenReturn(List.of());
    }

    @Test
    void nullCampaignOrNoSpeakersIsRemote() {
        Person speaker = mock(Person.class);

        assertEquals(TransmissionSignalQuality.REMOTE,
              TransmissionSignalQualityResolver.resolve(null, speaker, null));
        assertEquals(TransmissionSignalQuality.REMOTE,
              TransmissionSignalQualityResolver.resolve(campaign, null, null));
    }

    @Test
    void nullCampaignSystemIsRemote() {
        Person speaker = rosterSpeakerAt(campaignSystem);
        when(campaign.getCurrentSystem()).thenReturn(null);

        assertEquals(TransmissionSignalQuality.REMOTE,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));
    }

    @Test
    void externalContactWithKnownLocationIsRemote() {
        PlanetarySystem contactSystem = systemWithRating(HPGRating.A);
        Person externalContact = speakerAt(contactSystem);
        network(new HPGLink(campaignSystem, contactSystem, HPGRating.A));

        assertEquals(TransmissionSignalQuality.REMOTE,
              TransmissionSignalQualityResolver.resolve(campaign, externalContact, null));
    }

    @Test
    void partialRosterMocksAreUntrusted() {
        Person speaker = speakerAt(campaignSystem);

        when(campaign.getPlayerForce()).thenReturn(null);
        assertEquals(TransmissionSignalQuality.REMOTE,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));

        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getHumanResources()).thenReturn(null);
        assertEquals(TransmissionSignalQuality.REMOTE,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));

        when(playerForce.getHumanResources()).thenReturn(humanResources);
        when(humanResources.getPersonnel()).thenReturn(null);
        assertEquals(TransmissionSignalQuality.REMOTE,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));
    }

    @Test
    void rosterSpeakerWithoutSystemIsRemote() {
        Person speaker = rosterSpeakerAt(null);

        assertEquals(TransmissionSignalQuality.REMOTE,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));
    }

    @Test
    void rosterSpeakerInCampaignSystemIsClearEvenWhenRepeated() {
        Person speaker = rosterSpeakerAt(campaignSystem);

        assertEquals(TransmissionSignalQuality.CLEAR,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, speaker));
    }

    @Test
    void directIndirectAndReverseUsablePathsAreRemote() {
        PlanetarySystem relay = systemWithRating(HPGRating.B);
        PlanetarySystem speakerSystem = systemWithRating(HPGRating.A);
        Person speaker = rosterSpeakerAt(speakerSystem);

        network(new HPGLink(campaignSystem, speakerSystem, HPGRating.B));
        assertEquals(TransmissionSignalQuality.REMOTE,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));

        network(new HPGLink(campaignSystem, relay, HPGRating.A),
              new HPGLink(relay, speakerSystem, HPGRating.B));
        assertEquals(TransmissionSignalQuality.REMOTE,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));

        network(new HPGLink(speakerSystem, relay, HPGRating.A),
              new HPGLink(relay, campaignSystem, HPGRating.B));
        assertEquals(TransmissionSignalQuality.REMOTE,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));
    }

    @Test
    void knownDifferentSystemWithoutPathIsDegraded() {
        Person speaker = rosterSpeakerAt(systemWithRating(HPGRating.A));

        assertEquals(TransmissionSignalQuality.DEGRADED,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));
    }

    @ParameterizedTest
    @EnumSource(value = HPGRating.class, names = { "C", "D", "X" })
    void secondaryLinkRatingsAreRejected(HPGRating rating) {
        PlanetarySystem speakerSystem = systemWithRating(HPGRating.A);
        Person speaker = rosterSpeakerAt(speakerSystem);
        network(new HPGLink(campaignSystem, speakerSystem, rating));

        assertEquals(TransmissionSignalQuality.DEGRADED,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));
    }

    @ParameterizedTest
    @EnumSource(value = HPGRating.class, names = { "C", "D", "X" })
    void datedEndpointRatingsAreRejected(HPGRating rating) {
        PlanetarySystem relay = systemWithRating(rating);
        PlanetarySystem speakerSystem = systemWithRating(HPGRating.A);
        Person speaker = rosterSpeakerAt(speakerSystem);
        network(new HPGLink(campaignSystem, relay, HPGRating.A),
              new HPGLink(relay, speakerSystem, HPGRating.A));

        assertEquals(TransmissionSignalQuality.DEGRADED,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));
    }

    @Test
    void graphTraversalIsCycleSafe() {
        PlanetarySystem firstRelay = systemWithRating(HPGRating.A);
        PlanetarySystem secondRelay = systemWithRating(HPGRating.B);
        PlanetarySystem speakerSystem = systemWithRating(HPGRating.A);
        Person speaker = rosterSpeakerAt(speakerSystem);
        network(new HPGLink(campaignSystem, firstRelay, HPGRating.A),
              new HPGLink(firstRelay, secondRelay, HPGRating.B),
              new HPGLink(secondRelay, firstRelay, HPGRating.A),
              new HPGLink(secondRelay, speakerSystem, HPGRating.B));

        assertEquals(TransmissionSignalQuality.REMOTE,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));
    }

    @Test
    void nullGraphInputsAreTreatedAsNoPath() {
        PlanetarySystem speakerSystem = systemWithRating(HPGRating.A);
        Person speaker = rosterSpeakerAt(speakerSystem);

        when(campaign.getHPGNetwork()).thenReturn(null);
        assertEquals(TransmissionSignalQuality.DEGRADED,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));

        when(campaign.getHPGNetwork()).thenReturn(Arrays.asList(
              null,
              new HPGLink(campaignSystem, null, HPGRating.A),
              new HPGLink(null, speakerSystem, HPGRating.A)));
        assertEquals(TransmissionSignalQuality.DEGRADED,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));
    }

    @Test
    void nullCampaignDateIsTreatedAsNoPath() {
        Person speaker = rosterSpeakerAt(systemWithRating(HPGRating.A));
        when(campaign.getLocalDate()).thenReturn(null);

        assertEquals(TransmissionSignalQuality.DEGRADED,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));
    }

    @Test
    void multipleSpeakersUseWorstQuality() {
        Person clearSpeaker = rosterSpeakerAt(campaignSystem);
        Person remoteSpeaker = mock(Person.class);

        assertEquals(TransmissionSignalQuality.REMOTE,
              TransmissionSignalQualityResolver.resolve(campaign, clearSpeaker, remoteSpeaker));

        PlanetarySystem degradedSystem = systemWithRating(HPGRating.A);
        Person degradedSpeaker = speakerAt(degradedSystem);
        when(humanResources.getPersonnel()).thenReturn(List.of(clearSpeaker, degradedSpeaker));

        assertEquals(TransmissionSignalQuality.DEGRADED,
              TransmissionSignalQualityResolver.resolve(campaign, remoteSpeaker, degradedSpeaker));
    }

    @Test
    void campaignProvidesNetwork() {
        PlanetarySystem speakerSystem = systemWithRating(HPGRating.A);
        Person speaker = rosterSpeakerAt(speakerSystem);
        network(new HPGLink(campaignSystem, speakerSystem, HPGRating.A));

        assertEquals(TransmissionSignalQuality.REMOTE,
              TransmissionSignalQualityResolver.resolve(campaign, speaker, null));
        verify(campaign).getHPGNetwork();
    }

    private Person rosterSpeakerAt(PlanetarySystem system) {
        Person speaker = speakerAt(system);
        when(humanResources.getPersonnel()).thenReturn(List.of(speaker));
        return speaker;
    }

    private static Person speakerAt(PlanetarySystem system) {
        Person speaker = mock(Person.class);
        when(speaker.getCurrentSystem()).thenReturn(system);
        return speaker;
    }

    private static PlanetarySystem systemWithRating(HPGRating rating) {
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getHPG(DATE)).thenReturn(rating);
        return system;
    }

    private void network(HPGLink... links) {
        when(campaign.getHPGNetwork()).thenReturn(List.of(links));
    }
}
