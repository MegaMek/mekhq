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

import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME;
import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.KIT_AEROSPACE_PILOT;
import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.KIT_MEKWARRIOR_ADVANCED;
import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.KIT_MEKWARRIOR_BASIC;
import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.KIT_MEKWARRIOR_CLAN;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvFighter;
import megamek.common.units.Entity;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Coverage for the armor-kit deployment requirements in {@link Unit#checkCrewArmorKits}: Meks need every crew member
 * in a MekWarrior kit, aerospace fighters need every crew member in the Aerospace Fighter Pilot Kit, and each rule
 * only applies to its own unit type and only when its campaign option is on.
 */
class UnitCrewArmorKitsTest {
    private CampaignOptions options;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        options = new CampaignOptions();
    }

    private static Person wearing(String kit) {
        Person person = mock(Person.class);
        when(person.getArmorKitName()).thenReturn(kit);
        return person;
    }

    // region MekWarrior requirement
    @Test
    void meksDeployFreelyWhenTheMekWarriorRequirementIsOff() {
        assertNull(Unit.checkCrewArmorKits(new BipedMek(), List.of(wearing(DEFAULT_ARMOR_KIT_NAME)), options));
    }

    @Test
    void aMekDeploysWhenEveryCrewMemberWearsAnyMekWarriorKit() {
        options.set(CampaignOption.REQUIRE_MEKWARRIOR_KIT_TO_DEPLOY, true);
        List<Person> crew = List.of(wearing(KIT_MEKWARRIOR_BASIC), wearing(KIT_MEKWARRIOR_ADVANCED),
              wearing(KIT_MEKWARRIOR_CLAN));

        assertNull(Unit.checkCrewArmorKits(new BipedMek(), crew, options));
    }

    @Test
    void aMekIsBlockedWhenAnyCrewMemberLacksAMekWarriorKit() {
        options.set(CampaignOption.REQUIRE_MEKWARRIOR_KIT_TO_DEPLOY, true);
        List<Person> crew = List.of(wearing(KIT_MEKWARRIOR_BASIC), wearing(KIT_AEROSPACE_PILOT));

        assertNotNull(Unit.checkCrewArmorKits(new BipedMek(), crew, options));
    }

    @Test
    void theMekWarriorRequirementDoesNotApplyToFighters() {
        options.set(CampaignOption.REQUIRE_MEKWARRIOR_KIT_TO_DEPLOY, true);
        assertNull(Unit.checkCrewArmorKits(new AeroSpaceFighter(), List.of(wearing(DEFAULT_ARMOR_KIT_NAME)), options));
    }
    // endregion MekWarrior requirement

    // region aerospace requirement
    @Test
    void fightersDeployFreelyWhenTheAerospaceRequirementIsOff() {
        assertNull(Unit.checkCrewArmorKits(new AeroSpaceFighter(), List.of(wearing(DEFAULT_ARMOR_KIT_NAME)),
              options));
    }

    @Test
    void anAerospaceFighterDeploysWhenEveryCrewMemberWearsThePilotKit() {
        options.set(CampaignOption.REQUIRE_AEROSPACE_KIT_TO_DEPLOY, true);
        assertNull(Unit.checkCrewArmorKits(new AeroSpaceFighter(), List.of(wearing(KIT_AEROSPACE_PILOT)), options));
    }

    @Test
    void anAerospaceFighterIsBlockedWhenACrewMemberLacksThePilotKit() {
        options.set(CampaignOption.REQUIRE_AEROSPACE_KIT_TO_DEPLOY, true);
        List<Person> crew = List.of(wearing(KIT_AEROSPACE_PILOT), wearing(KIT_MEKWARRIOR_BASIC));

        assertNotNull(Unit.checkCrewArmorKits(new AeroSpaceFighter(), crew, options));
    }

    @Test
    void conventionalFightersAreNotAffectedByTheAerospaceRequirement() {
        options.set(CampaignOption.REQUIRE_AEROSPACE_KIT_TO_DEPLOY, true);
        assertNull(Unit.checkCrewArmorKits(new ConvFighter(), List.of(wearing(DEFAULT_ARMOR_KIT_NAME)), options));
    }

    @Test
    void theAerospaceRequirementDoesNotApplyToMeks() {
        options.set(CampaignOption.REQUIRE_AEROSPACE_KIT_TO_DEPLOY, true);
        assertNull(Unit.checkCrewArmorKits(new BipedMek(), List.of(wearing(DEFAULT_ARMOR_KIT_NAME)), options));
    }
    // endregion aerospace requirement

    @Test
    void aNullEntityIsNeverBlocked() {
        options.set(CampaignOption.REQUIRE_MEKWARRIOR_KIT_TO_DEPLOY, true);
        options.set(CampaignOption.REQUIRE_AEROSPACE_KIT_TO_DEPLOY, true);
        Entity noEntity = null;
        assertNull(Unit.checkCrewArmorKits(noEntity, List.of(wearing(DEFAULT_ARMOR_KIT_NAME)), options));
    }
}
