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
 * of The Topps Company Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.TechConstants;
import megamek.common.enums.TechBase;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.campaignOptions.AcquisitionsType;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.market.ShoppingList;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.ActionCheck;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.work.IAcquisitionWork;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;


public class SkillCheckRulesTest {

    @Nested
    class AcquisitionSkillChecksTest {

        @BeforeAll
        static void beforeAll() {
            SkillType.initializeTypes();
        }

        private Campaign createCampaignMock(CampaignOptions options) {
            Campaign campaign = mock(Campaign.class);
            // force calling the actual implementation
            doCallRealMethod().when(campaign).checkAcquisition(any(), any(), anyBoolean());
            Faction faction = new Faction();
            when(campaign.getCampaignOptions()).thenReturn(options);
            when(campaign.getFaction()).thenReturn(faction);
            ShoppingList shoppingList = mock(ShoppingList.class);
            when(campaign.getShoppingList()).thenReturn(shoppingList);
            return campaign;
        }

        @Test
        public void testAutomaticAcquisition() {
            CampaignOptions options = mock(CampaignOptions.class);
            Campaign campaign = createCampaignMock(options);
            Person person = new Person(campaign);
            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.AUTOMATIC);

            SkillCheck result = campaign.checkAcquisition(mock(IAcquisitionWork.class), person, false);

            assertEquals(SkillType.S_ADMIN, result.getSkillType().getName());
            assertTrue(result.getTargetNumber().isAutomaticSuccess());
            assertEquals("Automatic Success", result.getTargetNumber().getDesc());
        }

        @Test
        public void testNullPerson() throws NoSuchFieldException, IllegalAccessException {
            CampaignOptions options = mock(CampaignOptions.class);
            Campaign campaign = createCampaignMock(options);
            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.ADMINISTRATION);

            SkillCheck result = campaign.checkAcquisition(mock(IAcquisitionWork.class), null, false);

            // check that the person was initialized; the field is protected and we want to keep that
            Field personField = ActionCheck.class.getDeclaredField("person");
            personField.setAccessible(true);
            Person extractedPerson = (Person) personField.get(result);
            assertEquals("Unnamed", extractedPerson.getFirstName());

            assertTrue(result.getTargetNumber().isAutomaticFail());
            assertTrue(result.getTargetNumber().getDesc().contains("used up all their acquisition attempts"));
        }

        @Test
        public void testNullPerson_SkillTypes() {
            CampaignOptions options = mock(CampaignOptions.class);
            Campaign campaign = createCampaignMock(options);

            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.ADMINISTRATION);
            SkillCheck result = campaign.checkAcquisition(mock(IAcquisitionWork.class), null, false);
            assertEquals(SkillType.S_ADMIN, result.getSkillType().getName());

            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.ANY_TECH);
            result = campaign.checkAcquisition(mock(IAcquisitionWork.class), null, false);
            assertEquals(SkillType.S_TECH_MECHANIC, result.getSkillType().getName());

            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.NEGOTIATION);
            result = campaign.checkAcquisition(mock(IAcquisitionWork.class), null, false);
            assertEquals(SkillType.S_NEGOTIATION, result.getSkillType().getName());

            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.AUTOMATIC);
            result = campaign.checkAcquisition(mock(IAcquisitionWork.class), null, false);
            assertEquals(SkillType.S_ADMIN, result.getSkillType().getName());
        }

        @Test
        public void testAutomaticAcquisitionWithNullPerson() {
            CampaignOptions options = mock(CampaignOptions.class);
            Campaign campaign = createCampaignMock(options);
            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.AUTOMATIC);

            SkillCheck result = campaign.checkAcquisition(mock(IAcquisitionWork.class), null, false);

            assertTrue(result.getTargetNumber().isAutomaticSuccess());
        }

        @Test
        public void testItemOnShoppingList_WaitDaysTrue() {
            CampaignOptions options = mock(CampaignOptions.class);
            Campaign campaign = createCampaignMock(options);
            IAcquisitionWork acquisition = mock(IAcquisitionWork.class);
            ShoppingList shoppingList = mock(ShoppingList.class);
            when(campaign.getShoppingList()).thenReturn(shoppingList);
            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.ADMINISTRATION);
            when(shoppingList.getShoppingItem(any())).thenReturn(mock(IAcquisitionWork.class));

            SkillCheck result = campaign.checkAcquisition(acquisition, new Person(campaign), true);

            assertTrue(result.getTargetNumber().isAutomaticFail());
            assertTrue(result.getTargetNumber().getDesc().contains("wait until the new cycle"));
        }

        @Test
        public void testClanTechDisallowed() {
            CampaignOptions options = mock(CampaignOptions.class);
            Campaign campaign = createCampaignMock(options);
            IAcquisitionWork acquisition = mock(IAcquisitionWork.class);
            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.ADMINISTRATION);
            when(acquisition.getTechBase()).thenReturn(TechBase.CLAN);
            when(options.isAllowClanPurchases()).thenReturn(false);

            SkillCheck result = campaign.checkAcquisition(acquisition, new Person(campaign), false);

            assertTrue(result.getTargetNumber().isImpossible());
            assertEquals("You cannot acquire clan parts", result.getTargetNumber().getDesc());
        }

        @Test
        public void testISTechDisallowed() {
            CampaignOptions options = mock(CampaignOptions.class);
            Campaign campaign = createCampaignMock(options);
            IAcquisitionWork acquisition = mock(IAcquisitionWork.class);
            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.ADMINISTRATION);
            when(acquisition.getTechBase()).thenReturn(TechBase.IS);
            when(options.isAllowISPurchases()).thenReturn(false);

            SkillCheck result = campaign.checkAcquisition(acquisition, new Person(campaign), false);

            assertTrue(result.getTargetNumber().isImpossible());
            assertEquals("You cannot acquire inner sphere parts", result.getTargetNumber().getDesc());
        }

        @Test
        public void testTechLevelExceedsLimit() {
            CampaignOptions options = mock(CampaignOptions.class);
            Campaign campaign = createCampaignMock(options);
            IAcquisitionWork acquisition = mock(IAcquisitionWork.class);
            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.ADMINISTRATION);
            when(options.isAllowISPurchases()).thenReturn(true);
            when(acquisition.getTechBase()).thenReturn(TechBase.IS);
            when(acquisition.getTechLevel()).thenReturn(TechConstants.T_IS_ADVANCED);
            when(options.getTechLevel()).thenReturn(TechConstants.T_INTRO_BOX_SET);

            SkillCheck result = campaign.checkAcquisition(acquisition, new Person(campaign), false);

            assertTrue(result.getTargetNumber().isImpossible());
            assertEquals("You cannot acquire parts of this tech level", result.getTargetNumber().getDesc());
        }

        @Test
        public void testPartNotInvented() {
            CampaignOptions options = mock(CampaignOptions.class);
            Campaign campaign = createCampaignMock(options);
            IAcquisitionWork acquisition = mock(IAcquisitionWork.class);
            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.ADMINISTRATION);
            when(options.isLimitByYear()).thenReturn(true);
            when(acquisition.isIntroducedBy(anyInt(), anyBoolean(), any())).thenReturn(false);

            SkillCheck result = campaign.checkAcquisition(acquisition, new Person(campaign), false);

            assertTrue(result.getTargetNumber().isImpossible());
            assertEquals("It has not been invented yet!", result.getTargetNumber().getDesc());
        }

        @Test
        public void testPartIsExtinct() {
            CampaignOptions options = mock(CampaignOptions.class);
            Campaign campaign = createCampaignMock(options);
            IAcquisitionWork acquisition = mock(IAcquisitionWork.class);
            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.ADMINISTRATION);
            when(options.isDisallowExtinctStuff()).thenReturn(true);
            when(acquisition.isExtinctIn(anyInt(), anyBoolean(), any())).thenReturn(true);

            SkillCheck result = campaign.checkAcquisition(acquisition, new Person(campaign), false);

            assertTrue(result.getTargetNumber().isImpossible());
            assertEquals("It is extinct!", result.getTargetNumber().getDesc());
        }

        @Test
        public void testMissingRequiredSkill() {
            CampaignOptions options = mock(CampaignOptions.class);
            Campaign campaign = createCampaignMock(options);
            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.ADMINISTRATION);

            SkillCheck result = campaign.checkAcquisition(mock(IAcquisitionWork.class), new Person(campaign), false);

            assertTrue(result.getTargetNumber().isAutomaticFail());
            assertEquals("No skill", result.getTargetNumber().getDesc());
        }

        @Test
        public void testAnyTech_WithBestSkill() {
            CampaignOptions options = mock(CampaignOptions.class);
            Campaign campaign = createCampaignMock(options);
            IAcquisitionWork acquisition = mock(IAcquisitionWork.class);
            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.ANY_TECH);
            when(acquisition.getAllAcquisitionMods()).thenReturn(new TargetRoll());

            Person person = new Person(campaign);
            person.addSkill(SkillType.S_TECH_VESSEL, 6, 0);
            person.addSkill(SkillType.S_TECH_BA, 5, 0);
            person.addSkill(SkillType.S_GUN_MEK, 3, 0);

            SkillCheck result = campaign.checkAcquisition(acquisition, person, false);

            assertEquals(SkillType.S_TECH_BA, result.getSkillType().getName());
        }

        @Test
        public void testAnyTech_NoBestSkill_UsesMechanicFallback() {
            CampaignOptions options = mock(CampaignOptions.class);
            Campaign campaign = createCampaignMock(options);
            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.ANY_TECH);

            SkillCheck result = campaign.checkAcquisition(mock(IAcquisitionWork.class), new Person(campaign), false);

            // Fallback to S_TECH_MECHANIC chec even though the person does not have it
            assertEquals(SkillType.S_TECH_MECHANIC, result.getSkillType().getName());
        }

        @ParameterizedTest
        @CsvSource({ "NEGOTIATION, Negotiation", "ANY_TECH, Tech/Mek", "ADMINISTRATION, Administration" })
        public void testTargetNumber(String acquisitionType, String skillName) {
            CampaignOptions options = mock(CampaignOptions.class);
            Campaign campaign = createCampaignMock(options);
            IAcquisitionWork acquisition = mock(IAcquisitionWork.class);
            when(options.getAcquisitionType()).thenReturn(AcquisitionsType.valueOf(acquisitionType));

            Person person = new Person(campaign);
            person.addSkill(skillName, 5, 0);
            when(acquisition.getAllAcquisitionMods()).thenReturn(new TargetRoll());

            SkillCheck result = campaign.checkAcquisition(acquisition, person, false);
            assertEquals(5, result.getTargetNumber().getValue());
            assertEquals(skillName, result.getSkillType().getName());

            when(acquisition.getAllAcquisitionMods()).thenReturn(new TargetRoll(2, ""));
            result = campaign.checkAcquisition(acquisition, person, false);
            assertEquals(7, result.getTargetNumber().getValue());
        }

    }
}
