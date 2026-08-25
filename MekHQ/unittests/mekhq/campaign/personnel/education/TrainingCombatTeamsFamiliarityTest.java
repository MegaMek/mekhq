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
package mekhq.campaign.personnel.education;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.familiarity.Familiarity;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Coverage for
 * {@link TrainingCombatTeams#improveFamiliarityForTrainees(Campaign, Person, Person, int, int, Familiarity)}.
 *
 * <p>The caller runs once per trainee, so both of this method's grants must be person-scoped: the trainee earns the
 * full gain for their own chassis and half of it for the educator's chassis. Awarding a whole unit's crew instead would
 * multiply the session's grants by crew size and would send the cross-chassis reward to the educator's crew rather than
 * to the trainee.</p>
 */
class TrainingCombatTeamsFamiliarityTest {
    private static final String TRAINEE_CHASSIS = "Locust";
    private static final String EDUCATOR_CHASSIS = "Atlas";

    /** Speed x margin of success = 20 for the trainee's own chassis, and half of that for the educator's. */
    private static final int FAMILIARITY_SPEED = 4;
    private static final int MARGIN_OF_SUCCESS = 5;
    private static final int EXPECTED_OWN_CHASSIS_GAIN = 20;
    private static final int EXPECTED_EDUCATOR_CHASSIS_GAIN = 10;

    private Campaign campaign;
    private Person educator;
    private Person educatorGunner;
    private Person trainee;
    private Person traineeGunner;

    @BeforeEach
    void beforeEach() {
        campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.CHASSIS_FAMILIARITY_MODE, Familiarity.NORMAL);

        educator = new Person(campaign);
        educatorGunner = new Person(campaign);
        trainee = new Person(campaign);
        traineeGunner = new Person(campaign);

        // Both units are multi-crew, and the educator rides a different chassis than the trainee.
        educator.setUnit(mockUnit(EDUCATOR_CHASSIS, educator, educatorGunner));
        trainee.setUnit(mockUnit(TRAINEE_CHASSIS, trainee, traineeGunner));
    }

    private static Unit mockUnit(String chassis, Person... crew) {
        Entity entity = mock(Entity.class);
        when(entity.getChassis()).thenReturn(chassis);
        when(entity.isChassisFamiliarityEligible()).thenReturn(true);

        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.getCrew()).thenReturn(List.of(crew));
        when(unit.getActiveCrew()).thenReturn(List.of(crew));

        return unit;
    }

    private void trainOnce() {
        TrainingCombatTeams.improveFamiliarityForTrainees(campaign,
              educator,
              trainee,
              FAMILIARITY_SPEED,
              MARGIN_OF_SUCCESS,
              Familiarity.NORMAL);
    }

    @Test
    void testTraineeGainsOwnChassis() {
        trainOnce();

        assertEquals(EXPECTED_OWN_CHASSIS_GAIN, trainee.getChassisFamiliarity(TRAINEE_CHASSIS));
    }

    /**
     * Regression: the own-chassis grant went to the trainee's whole unit, so every occupant of a K-person unit received
     * the session's gain once per trainee processed - K identical grants each.
     */
    @Test
    void testTraineeCrewmatesGainNothing() {
        trainOnce();

        assertEquals(0, traineeGunner.getChassisFamiliarity(TRAINEE_CHASSIS),
              "only the trainee being processed is awarded, not the rest of their crew");
    }

    /**
     * Regression: the cross-chassis grant went to the educator's unit, so the educator's crew climbed the educator's
     * own chassis track and the trainee never received the documented reward.
     */
    @Test
    void testTraineeGainsEducatorChassisAndTheEducatorGainsNothing() {
        trainOnce();

        assertEquals(EXPECTED_EDUCATOR_CHASSIS_GAIN, trainee.getChassisFamiliarity(EDUCATOR_CHASSIS),
              "the trainee earns familiarity with the chassis they were taught from");
        assertEquals(0, educator.getChassisFamiliarity(EDUCATOR_CHASSIS),
              "the educator is not rewarded by the trainee's cross-chassis grant");
        assertEquals(0, educatorGunner.getChassisFamiliarity(EDUCATOR_CHASSIS),
              "nor is the educator's crew");
        assertEquals(0, educator.getChassisFamiliarity(TRAINEE_CHASSIS));
    }

    @Test
    void testOwnChassisGainIsCappedAtTheTrainingCap() {
        int trainingCap = Familiarity.NORMAL.getTrainingCap();
        trainee.setChassisFamiliarity(TRAINEE_CHASSIS, trainingCap - 5);

        trainOnce();

        assertEquals(trainingCap, trainee.getChassisFamiliarity(TRAINEE_CHASSIS));
    }

    @Test
    void testEducatorChassisGainIsCappedAtHalfTheTrainingCap() {
        int educatorChassisCap = Familiarity.NORMAL.getTrainingCap() / 2;
        trainee.setChassisFamiliarity(EDUCATOR_CHASSIS, educatorChassisCap - 2);

        trainOnce();

        assertEquals(educatorChassisCap, trainee.getChassisFamiliarity(EDUCATOR_CHASSIS));
    }

    @Test
    void testRepeatedSessionsAccumulateOnceEach() {
        trainOnce();
        trainOnce();

        assertEquals(EXPECTED_OWN_CHASSIS_GAIN * 2, trainee.getChassisFamiliarity(TRAINEE_CHASSIS));
        assertEquals(EXPECTED_EDUCATOR_CHASSIS_GAIN * 2, trainee.getChassisFamiliarity(EDUCATOR_CHASSIS));
        assertEquals(0, traineeGunner.getChassisFamiliarity(TRAINEE_CHASSIS));
    }

    @Test
    void testUnassignedTraineeStillLearnsTheEducatorChassis() {
        trainee.setUnit(null);

        trainOnce();

        assertEquals(0, trainee.getChassisFamiliarity(TRAINEE_CHASSIS));
        assertEquals(EXPECTED_EDUCATOR_CHASSIS_GAIN, trainee.getChassisFamiliarity(EDUCATOR_CHASSIS));
    }

    @Test
    void testIneligibleChassisIsSkipped() {
        when(trainee.getUnit().getEntity().isChassisFamiliarityEligible()).thenReturn(false);

        trainOnce();

        assertEquals(0, trainee.getChassisFamiliarity(TRAINEE_CHASSIS));
        assertEquals(EXPECTED_EDUCATOR_CHASSIS_GAIN, trainee.getChassisFamiliarity(EDUCATOR_CHASSIS));
    }

    @Test
    void testDisabledModeAwardsNothing() {
        campaign.getCampaignOptions().set(CampaignOption.CHASSIS_FAMILIARITY_MODE, Familiarity.DISABLED);

        trainOnce();

        assertEquals(0, trainee.getChassisFamiliarity(TRAINEE_CHASSIS));
        assertEquals(0, trainee.getChassisFamiliarity(EDUCATOR_CHASSIS));
    }
}
