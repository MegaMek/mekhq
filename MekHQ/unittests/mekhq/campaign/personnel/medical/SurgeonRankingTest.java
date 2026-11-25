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
package mekhq.campaign.personnel.medical;

import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import org.junit.jupiter.api.Test;

/**
 * Tests that the surgeon chosen for Advanced Surgery is the doctor with the best Surgery skill, not the highest-ranked
 * doctor. Previously a higher-ranked Surgery 1 doctor was picked over a Surgery 3 doctor, so the dialog claimed no
 * doctor was skilled enough for level 2-3 surgeries.
 */
class SurgeonRankingTest {
    private static final LocalDate TODAY = LocalDate.of(3050, 6, 1);
    private static final CampaignOptions CAMPAIGN_OPTIONS = new CampaignOptions();

    @Test
    void getSurgerySkillLevelReturnsTotalSkillLevel() {
        Person doctor = doctor(3, false);

        assertEquals(3, SurgeonRanking.getSurgerySkillLevel(doctor));
    }

    @Test
    void getSurgerySkillLevelWithoutSkillReturnsNoSurgerySkill() {
        Person doctor = doctorWithoutSurgery(false);

        assertEquals(SurgeonRanking.NO_SURGERY_SKILL, SurgeonRanking.getSurgerySkillLevel(doctor));
    }

    @Test
    void betterSurgeonIsChosenOverHigherRankedDoctor() {
        Person seniorButWeakSurgeon = doctor(1, true);
        Person juniorButStrongSurgeon = doctor(3, false);

        assertSame(juniorButStrongSurgeon, getBestSurgeon(List.of(seniorButWeakSurgeon, juniorButStrongSurgeon)));
        assertSame(juniorButStrongSurgeon, getBestSurgeon(List.of(juniorButStrongSurgeon, seniorButWeakSurgeon)));
    }

    @Test
    void equalSurgerySkillFallsBackToRank() {
        Person juniorSurgeon = doctor(2, false);
        Person seniorSurgeon = doctor(2, true);

        assertSame(seniorSurgeon, getBestSurgeon(List.of(juniorSurgeon, seniorSurgeon)));
        assertSame(seniorSurgeon, getBestSurgeon(List.of(seniorSurgeon, juniorSurgeon)));
    }

    @Test
    void doctorWithSurgeryIsChosenOverDoctorWithout() {
        Person unskilledDoctor = doctorWithoutSurgery(true);
        Person surgeon = doctor(0, false);

        assertSame(surgeon, getBestSurgeon(List.of(unskilledDoctor, surgeon)));
        assertSame(surgeon, getBestSurgeon(List.of(surgeon, unskilledDoctor)));
    }

    @Test
    void inactiveDoctorsAndNonDoctorsAreIgnored() {
        Person inactiveSurgeon = doctor(5, true);
        when(inactiveSurgeon.isActive()).thenReturn(false);
        Person nonDoctorSurgeon = doctor(5, true);
        when(nonDoctorSurgeon.isDoctor()).thenReturn(false);
        Person activeDoctor = doctor(1, false);

        assertSame(activeDoctor, getBestSurgeon(List.of(inactiveSurgeon, nonDoctorSurgeon, activeDoctor)));
    }

    @Test
    void noDoctorsReturnsNull() {
        Person nonDoctor = doctor(5, true);
        when(nonDoctor.isDoctor()).thenReturn(false);

        assertNull(getBestSurgeon(List.of(nonDoctor)));
        assertNull(getBestSurgeon(List.of()));
    }

    private static Person getBestSurgeon(List<Person> candidates) {
        return SurgeonRanking.getBestSurgeon(candidates, CAMPAIGN_OPTIONS, false, TODAY);
    }

    /**
     * @param surgeryLevel the doctor's total Surgery skill level
     * @param outRanksAll  {@code true} if this doctor outranks everyone they are compared with
     *
     * @return an active doctor with the given Surgery skill
     */
    private static Person doctor(int surgeryLevel, boolean outRanksAll) {
        Person doctor = doctorWithoutSurgery(outRanksAll);
        Skill surgerySkill = mock(Skill.class);
        when(surgerySkill.getTotalSkillLevel(any())).thenReturn(surgeryLevel);
        when(doctor.getSkill(S_SURGERY)).thenReturn(surgerySkill);
        return doctor;
    }

    /**
     * @param outRanksAll {@code true} if this doctor outranks everyone they are compared with
     *
     * @return an active doctor with no Surgery skill
     */
    private static Person doctorWithoutSurgery(boolean outRanksAll) {
        Person doctor = mock(Person.class);
        when(doctor.isActive()).thenReturn(true);
        when(doctor.isDoctor()).thenReturn(true);
        when(doctor.outRanksUsingSkillTiebreaker(any(CampaignOptions.class), anyBoolean(), any(LocalDate.class),
              any(Person.class))).thenReturn(outRanksAll);
        return doctor;
    }
}
