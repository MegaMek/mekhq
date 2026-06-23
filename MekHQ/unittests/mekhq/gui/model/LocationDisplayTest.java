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

package mekhq.gui.model;

import static mekhq.utilities.MHQInternationalization.getFormattedText;
import static mekhq.utilities.MHQInternationalization.getText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.location.AcademyCampusLocation;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class LocationDisplayTest {

    @Nested
    class GetLocationName {

        private Person buildTravelingPerson(List<PlanetarySystem> jumpPath, double transitTime) {
            CurrentLocation cl = new CurrentLocation(jumpPath.getFirst(), transitTime);
            cl.setJumpPath(new JumpPath(new ArrayList<>(jumpPath)));
            LocationNode.LocationManager.setLocation(cl, new AcademyCampusLocation("Set", "Name"));
            Person person = new Person("GivenName", "Surname", null, "Faction");
            LocationNode.LocationManager.setLocation(person, cl);
            return person;
        }

        @Test
        void locationName_toJumpPoint() {
            PlanetarySystem originSys = mock(PlanetarySystem.class);
            when(originSys.getTimeToJumpPoint(1.0)).thenReturn(10.0);
            Person person = buildTravelingPerson(List.of(originSys, mock(PlanetarySystem.class)), 3);

            assertEquals(getFormattedText("LocationDisplay.inTransit.toJumpPoint.text", 7),
                  LocationDisplay.getLocationName(person, mock(Campaign.class, RETURNS_DEEP_STUBS), LocalDate.EPOCH));
        }

        @Test
        void locationName_recharging() {
            PlanetarySystem originSys = mock(PlanetarySystem.class);
            when(originSys.getTimeToJumpPoint(1.0)).thenReturn(10.0);
            when(originSys.getRechargeTime(any(), anyBoolean())).thenReturn(168.0);
            Person person = buildTravelingPerson(List.of(originSys, mock(PlanetarySystem.class)), 10);

            assertEquals(getFormattedText("LocationDisplay.inTransit.recharging.text", 7),
                  LocationDisplay.getLocationName(person, mock(Campaign.class, RETURNS_DEEP_STUBS), LocalDate.EPOCH));
        }

        @Test
        void locationName_readyToJump() {
            PlanetarySystem originSys = mock(PlanetarySystem.class);
            when(originSys.getTimeToJumpPoint(1.0)).thenReturn(10.0);
            when(originSys.getRechargeTime(any(), anyBoolean())).thenReturn(0.0);
            Person person = buildTravelingPerson(List.of(originSys, mock(PlanetarySystem.class)), 10);

            assertEquals(getText("LocationDisplay.inTransit.readyToJump.text"),
                  LocationDisplay.getLocationName(person, mock(Campaign.class, RETURNS_DEEP_STUBS), LocalDate.EPOCH));
        }

        @Test
        void locationName_toPlanet() {
            PlanetarySystem academySys = mock(PlanetarySystem.class);
            Person person = buildTravelingPerson(List.of(academySys), 3.5);

            assertEquals(getFormattedText("LocationDisplay.inTransit.toPlanet.text", 4),
                  LocationDisplay.getLocationName(person, mock(Campaign.class, RETURNS_DEEP_STUBS), LocalDate.EPOCH));
        }
    }

}
