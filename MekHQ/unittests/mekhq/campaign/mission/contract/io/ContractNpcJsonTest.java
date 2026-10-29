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
package mekhq.campaign.mission.contract.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import megamek.common.enums.Gender;
import megamek.common.equipment.EquipmentType;
import megamek.common.icons.Camouflage;
import megamek.common.icons.Portrait;
import megamek.common.options.OptionsConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.contract.contractData.EmployerData;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.mission.contract.io.ContractExchange.ExportEntry;
import mekhq.campaign.mission.contract.io.ContractExchange.ImportResult;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Tests for {@link ContractNpcJson}, the snapshot that carries a contract's NPCs between campaigns. A negotiator's
 * skills and abilities decide how a negotiation goes, so those must arrive exactly; the rest of the person is
 * regenerated.
 */
class ContractNpcJsonTest {
    private Campaign campaign;

    @BeforeAll
    static void initSingletons() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        campaign = MHQTestUtilities.getTestCampaign();
    }

    private Person negotiator() {
        Person person = campaign.getPlayerForce()
                              .getHumanResources()
                              .newPerson(campaign, PersonnelRole.LAWYER, null, Gender.FEMALE);
        person.setGivenName("Katrina");
        person.setSurname("Steiner");
        person.setPortrait(new Portrait("Female/Command", "steiner.png"));
        person.setRank(12);
        person.getSkills().clear();
        person.addSkill(SkillType.S_NEGOTIATION, 6, 1);
        person.getSkill(SkillType.S_NEGOTIATION).setHasNaturalAptitude(true);
        person.getOptions().getOption(OptionsConstants.ATOW_COMBAT_SENSE).setValue(true);
        person.setAttributeScore(SkillAttribute.CHARISMA, 7);
        person.setAttributeScore(SkillAttribute.WILLPOWER, 6);
        return person;
    }

    private static ContractNpcJson snapshot(Person person) {
        ContractNpcJson snapshot = ContractNpcJson.from(person);
        assertNotNull(snapshot);
        return snapshot;
    }

    @Test
    void aMissingNpcHasNoSnapshot() {
        assertNull(ContractNpcJson.from(null));
    }

    @Test
    void theNpcsIdentityArrives() {
        Person rebuilt = snapshot(negotiator()).toPerson(campaign);

        assertEquals("Katrina", rebuilt.getGivenName());
        assertEquals("Steiner", rebuilt.getSurname());
        assertEquals(Gender.FEMALE, rebuilt.getGender());
        assertEquals(PersonnelRole.LAWYER, rebuilt.getPrimaryRole());
        assertEquals(12, rebuilt.getRankNumeric());
        assertEquals("Female/Command", rebuilt.getPortrait().getCategory());
        assertEquals("steiner.png", rebuilt.getPortrait().getFilename());
    }

    @Test
    void theNpcsSkillsArriveExactly() {
        Person rebuilt = snapshot(negotiator()).toPerson(campaign);

        Skill negotiation = rebuilt.getSkill(SkillType.S_NEGOTIATION);
        assertNotNull(negotiation);
        assertEquals(6, negotiation.getLevel());
        assertEquals(1, negotiation.getBonus());
        assertTrue(negotiation.getHasNaturalAptitude());
        assertEquals(1, rebuilt.getSkills().size(), "a regenerated person must not keep skills of their own");
    }

    @Test
    void theNpcsAttributesArrive() {
        Person original = negotiator();

        Person rebuilt = snapshot(original).toPerson(campaign);

        for (SkillAttribute attribute : SkillAttribute.values()) {
            if (attribute != SkillAttribute.NO_ATTRIBUTE) {
                assertEquals(original.getATOWAttributes().getAttribute(attribute),
                      rebuilt.getATOWAttributes().getAttribute(attribute), attribute.name());
            }
        }
    }

    @Test
    void theNpcsAbilitiesArriveAndNoOthers() {
        Person original = negotiator();
        ContractNpcJson snapshot = snapshot(original);

        Person rebuilt = snapshot.toPerson(campaign);

        assertTrue(rebuilt.getOptions().booleanOption(OptionsConstants.ATOW_COMBAT_SENSE));
        assertEquals(original.getOptionList("::", "lvl3Advantages"), rebuilt.getOptionList("::", "lvl3Advantages"));
    }

    @Test
    void anOffersNegotiatorArrivesThroughAContractFile() throws Exception {
        AbstractContract offer = new ChaosContract();
        offer.setContractName("Operation Handshake");
        offer.setEmployerData(new EmployerData(ChaosEmployerType.LOCAL_SYSTEM_OWNER, "LA", "LA", null,
              "Lyran Alliance", negotiator(), null, new Camouflage()));

        ImportResult result = ContractExchange.fromJson(ContractExchange.toJson(List.of(
              new ExportEntry(offer, ContractSearchType.MERCENARY))), campaign);

        Person rebuilt = result.imported().getFirst().contract().getEmployerNegotiator();
        assertNotNull(rebuilt);
        assertEquals("Katrina", rebuilt.getGivenName());
        assertEquals(6, rebuilt.getSkill(SkillType.S_NEGOTIATION).getLevel());
        assertNull(result.imported().getFirst().contract().getEmployerLiaison());
    }

    @Test
    void anUnknownAbilityOrAttributeIsIgnored() {
        ContractNpcJson snapshot = new ContractNpcJson("Ada", "Lovelace", Gender.FEMALE.name(),
              PersonnelRole.LAWYER.name(), null, "NONE", false, null, 0, 0, null,
              Map.of("NOT_AN_ATTRIBUTE", 9), Map.of(), List.of("not_a_real_ability"), List.of());

        Person rebuilt = snapshot.toPerson(campaign);

        assertEquals("Ada", rebuilt.getGivenName());
        assertFalse(rebuilt.getOptionList("::", "lvl3Advantages").contains("not_a_real_ability"));
    }
}
