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
package mekhq.campaign.universe.factionStanding;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import megamek.common.enums.Gender;
import megamek.common.universe.FactionLeaderData;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.Test;

/**
 * Verifies how the signatory of a head-of-state letter is named and gendered.
 *
 * <p>Faction leaders come from the universe data, where the gender is optional: the command files carry commanding
 * officers whose sourcebooks never state one. Applying such a leader must not clear the gender the speaker was
 * already generated with, because a person without a gender goes on to break anything that reads it.</p>
 */
class FactionAccoladeLeaderIdentityTest {

    private static FactionLeaderData leaderWithGender(Gender gender) {
        return new FactionLeaderData("Colonel", "Natasha", "Kerensky", null, gender, 3020, 3050);
    }

    @Test
    void genderIsAppliedWhenTheLeaderRecordsOne() {
        Person speaker = mock(Person.class);

        FactionAccoladeEvent.applyLeaderIdentity(speaker, leaderWithGender(Gender.FEMALE));

        verify(speaker).setGender(Gender.FEMALE);
        verify(speaker).setGivenName("Colonel Natasha Kerensky");
        verify(speaker).setSurname("");
    }

    @Test
    void generatedGenderIsKeptWhenTheLeaderRecordsNone() {
        Person speaker = mock(Person.class);

        FactionAccoladeEvent.applyLeaderIdentity(speaker, leaderWithGender(null));

        verify(speaker, never()).setGender(any());
        // The full title goes into the given name, so the generated surname must still be cleared.
        // Otherwise the letter is signed by "Colonel Natasha Kerensky" followed by a surname that
        // was never theirs.
        verify(speaker).setGivenName("Colonel Natasha Kerensky");
        verify(speaker).setSurname("");
    }

    @Test
    void nothingIsAppliedWhenTheFactionHasNoLeaderForTheYear() {
        Person speaker = mock(Person.class);

        FactionAccoladeEvent.applyLeaderIdentity(speaker, null);

        verify(speaker, never()).setGender(any());
        verify(speaker, never()).setGivenName(any());
        verify(speaker, never()).setSurname(any());
    }
}
