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
package mekhq.campaign.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignLocationManager;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.GroundTransitLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Verifies how {@link LocationDispatch} chooses a travel node based on system and root location. */
class LocationDispatchTest {

    Campaign campaign;
    CampaignLocationManager locationManager;
    PlanetarySystem system;

    @BeforeEach
    void setUp() {
        locationManager = new CampaignLocationManager();
        system = mock(PlanetarySystem.class);
        campaign = mock(Campaign.class);
        when(campaign.getCampaignLocationManager()).thenReturn(locationManager);
        when(campaign.getCurrentSystem()).thenReturn(system);
    }

    private static Person newPerson() {
        return new Person("First", "Last", null, "MERC");
    }

    private GroundTransitLocation registeredGroundTransit() {
        return locationManager.getLocations().stream()
              .filter(GroundTransitLocation.class::isInstance)
              .map(GroundTransitLocation.class::cast)
              .findFirst()
              .orElse(null);
    }

    /** Same planet, different root location → a 2-day overland GroundTransitLocation. */
    @Nested
    class SamePlanetDifferentRoot {
        @Test
        void createsGroundTransitNodeCarryingTheTraveler() {
            FixedLocation origin = new FixedLocation(system);
            FixedLocation destination = new FixedLocation(system);
            Person person = newPerson();
            person.setParent(origin);

            LocationDispatch.dispatchTravelers(List.of(person), destination, campaign);

            GroundTransitLocation groundNode = registeredGroundTransit();
            assertInstanceOf(GroundTransitLocation.class, person.getCurrentLocation());
            assertSame(groundNode, person.getCurrentLocation());
            assertEquals(2.0, groundNode.getTransitTime(), 1e-9);
        }
    }

    /** Already at the destination's root → land directly, no travel node. */
    @Nested
    class SameRoot {
        @Test
        void landsDirectlyWithoutTravelNode() {
            FixedLocation destination = new FixedLocation(system);
            Person person = newPerson();
            person.setParent(destination);

            LocationDispatch.dispatchTravelers(List.of(person), destination, campaign);

            assertNull(registeredGroundTransit());
            assertSame(destination, person.getCurrentLocation());
        }
    }

    /** Different system → an interplanetary CurrentLocation, not a ground node. */
    @Nested
    class DifferentSystem {
        @Test
        void createsInterplanetaryTravelNode() {
            PlanetarySystem destinationSystem = mock(PlanetarySystem.class);
            when(system.getTimeToJumpPoint(1.0)).thenReturn(5.0);
            JumpPath path = mock(JumpPath.class);
            when(path.isEmpty()).thenReturn(false);
            when(campaign.calculateJumpPath(system, destinationSystem)).thenReturn(path);

            FixedLocation origin = new FixedLocation(system);
            FixedLocation destination = new FixedLocation(destinationSystem);
            Person person = newPerson();
            person.setParent(origin);

            LocationDispatch.dispatchTravelers(List.of(person), destination, campaign);

            assertNull(registeredGroundTransit());
            assertInstanceOf(CurrentLocation.class, person.getCurrentLocation());
        }
    }
}
