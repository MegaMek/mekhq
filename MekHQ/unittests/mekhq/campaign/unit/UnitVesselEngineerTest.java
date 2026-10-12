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

package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.units.Dropship;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A large vessel's engineer is the crew member with the best Tech/Vessel skill, with rank breaking ties and as the
 * fallback when nobody has the skill. The chosen engineer is flagged as such, and the flag is cleared when they stop
 * being the engineer.
 */
class UnitVesselEngineerTest {
    private final Map<Person, Integer> ranks = new HashMap<>();

    private Unit unit;

    @BeforeEach
    void setUp() {
        Campaign campaign = mockCampaign();
        when(campaign.getCampaignOptions()).thenReturn(mock(CampaignOptions.class));
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3151, 3, 5));
        unit = spy(new Unit(mock(Dropship.class), campaign));
    }

    @Test
    void bestVesselTechIsEngineerEvenWhenOutranked() {
        Person captain = crewMember(10, 3);
        Person bestTech = crewMember(2, 7);
        Person crewman = crewMember(1, 5);

        Person engineer = resetEngineerWith(captain, bestTech, crewman);

        assertSame(bestTech, engineer);
    }

    @Test
    void rankBreaksTiesBetweenEquallySkilledTechs() {
        Person juniorTech = crewMember(2, 6);
        Person seniorTech = crewMember(5, 6);

        Person engineer = resetEngineerWith(juniorTech, seniorTech);

        assertSame(seniorTech, engineer);
    }

    @Test
    void highestRankIsEngineerWhenNobodyHasVesselTech() {
        Person pilot = crewMember(8, null);
        Person gunner = crewMember(3, null);

        Person engineer = resetEngineerWith(gunner, pilot);

        assertSame(pilot, engineer);
    }

    @Test
    void crewMemberWithoutVesselTechIsNotPickedOverOneWithIt() {
        Person captain = crewMember(10, null);
        Person crewman = crewMember(1, 0);

        Person engineer = resetEngineerWith(captain, crewman);

        assertSame(crewman, engineer);
    }

    @Test
    void vesselWithNoCrewHasNoEngineer() {
        assertNull(resetEngineerWith());
    }

    @Test
    void engineerIsFlaggedAndFormerEngineerIsCleared() {
        Person firstTech = crewMember(5, 7);
        Person secondTech = crewMember(5, 4);
        resetEngineerWith(firstTech, secondTech);
        verify(firstTech).setEngineer(true);

        resetEngineerWith(secondTech);

        verify(firstTech).setEngineer(false);
        verify(secondTech).setEngineer(true);
    }

    private Person resetEngineerWith(Person... crew) {
        doReturn(List.of(crew)).when(unit).getVesselCrew();
        unit.resetEngineer();
        return unit.getEngineer();
    }

    /**
     * @param rank            the crew member's rank; a higher rank outranks a lower one
     * @param vesselTechLevel the total Tech/Vessel skill level, or {@code null} if they do not have the skill
     */
    private Person crewMember(int rank, Integer vesselTechLevel) {
        Person person = mock(Person.class);
        ranks.put(person, rank);
        when(person.outRanksUsingSkillTiebreaker(any(), anyBoolean(), any(), any())).thenAnswer(invocation -> {
            Person otherPerson = invocation.getArgument(3);
            return (otherPerson == null) || (rank > ranks.get(otherPerson));
        });

        if (vesselTechLevel != null) {
            Skill vesselTechSkill = mock(Skill.class);
            when(vesselTechSkill.getTotalSkillLevel(any())).thenReturn(vesselTechLevel);
            when(person.getSkill(SkillType.S_TECH_VESSEL)).thenReturn(vesselTechSkill);
        }
        return person;
    }
}
