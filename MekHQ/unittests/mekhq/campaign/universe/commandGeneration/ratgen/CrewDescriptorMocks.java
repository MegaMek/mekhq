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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.client.ratgenerator.CrewDescriptor;

/**
 * Crew descriptors for tests. A real one names itself from the faction tables on construction, which needs
 * the RAT generator loaded; these hold a name, a Bloodname and two skill targets and nothing else.
 */
final class CrewDescriptorMocks {

    private CrewDescriptorMocks() {
    }

    /**
     * @param name     the crew's name
     * @param gunnery  the gunnery target number
     * @param piloting the piloting target number
     *
     * @return a crew descriptor that remembers what is set on it
     */
    static CrewDescriptor crew(String name, int gunnery, int piloting) {
        return crew(name, "", gunnery, piloting);
    }

    /**
     * @param name      the crew's name
     * @param bloodname the Bloodname the roll gave the crew, or blank for none
     * @param gunnery   the gunnery target number
     * @param piloting  the piloting target number
     *
     * @return a crew descriptor that remembers what is set on it
     */
    static CrewDescriptor crew(String name, String bloodname, int gunnery, int piloting) {
        CrewDescriptor crew = mock(CrewDescriptor.class);
        String[] state = { name, bloodname };
        when(crew.getName()).thenAnswer(invocation -> state[0]);
        doAnswer(invocation -> {
            state[0] = invocation.getArgument(0);
            return null;
        }).when(crew).setName(anyString());
        when(crew.getBloodname()).thenAnswer(invocation -> state[1]);
        doAnswer(invocation -> {
            state[1] = invocation.getArgument(0);
            return null;
        }).when(crew).setBloodname(anyString());
        when(crew.getGunnery()).thenReturn(gunnery);
        when(crew.getPiloting()).thenReturn(piloting);
        return crew;
    }
}
