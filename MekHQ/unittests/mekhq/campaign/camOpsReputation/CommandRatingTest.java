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
package mekhq.campaign.camOpsReputation;

import static megamek.common.options.OptionsConstants.ATOW_COMBAT_PARALYSIS;
import static megamek.common.options.OptionsConstants.ATOW_COMBAT_SENSE;
import static megamek.common.options.PilotOptions.LVL3_ADVANTAGES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import megamek.common.options.OptionsConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.randomEvents.personalities.enums.Aggression;
import mekhq.campaign.randomEvents.personalities.enums.Ambition;
import mekhq.campaign.randomEvents.personalities.enums.Greed;
import mekhq.campaign.randomEvents.personalities.enums.Social;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CommandRatingTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Campaign campaign;

    @Mock
    private CampaignOptions campaignOptions;

    private static final Set<String> EXPECTED_RESULT_KEYS = Set.of(
          "leadership", "tactics", "strategy", "negotiation", "traits", "personality", "total");

    @BeforeAll
    public static void setupClass() throws IOException {
        SkillType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
    }

    private void assertRating(Person commander, Map<String, Integer> overrides) {
        var expected = new HashMap<>();
        EXPECTED_RESULT_KEYS.forEach(key -> expected.put(key, 0));
        expected.putAll(overrides);
        var actual = CommandRating.calculateCommanderRating(campaign, commander);
        assertEquals(expected, actual);
    }

    private Person commanderWithLeadership(int leadership) {
        Person commander = new Person(campaign, "");
        commander.addSkill(SkillType.S_LEADER, leadership, 0);
        return commander;
    }

    @Test
    void testNullCommander() {
        assertRating(null, Map.of("total", 1));
    }

    @Test
    void testZeroCommander() {
        var commander = commanderWithLeadership(0);
        assertRating(commander, Map.of("total", 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5, 10})
    void testLeadership(int leadership) {
        var commander = commanderWithLeadership(leadership);
        assertRating(commander, Map.of("leadership", leadership, "total", Math.max(1, leadership)));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 5, 10})
    void testTactics(int tactics) {
        var commander = commanderWithLeadership(5);
        commander.addSkill(SkillType.S_TACTICS, tactics, 0);;
        assertRating(commander, Map.of("leadership", 5, "tactics", tactics, "total", 5 + tactics));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 6, 10})
    void testNegotiation(int negotiation) {
        var commander = commanderWithLeadership(5);
        commander.addSkill(SkillType.S_NEGOTIATION, negotiation, 0);;
        assertRating(commander, Map.of("leadership", 5, "negotiation", negotiation, "total", 5 + negotiation));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 7, 10})
    void testStrategy(int strategy) {
        var commander = commanderWithLeadership(5);
        commander.addSkill(SkillType.S_STRATEGY, strategy, 0);;
        assertRating(commander, Map.of("leadership", 5, "strategy", strategy, "total", 5 + strategy));
    }

    @Test
    void testSkillBonuses() {
        var commander = new Person(campaign, "");
        commander.addSkill(SkillType.S_LEADER, 1, 2);
        commander.addSkill(SkillType.S_TACTICS, 2, 3);;
        commander.addSkill(SkillType.S_NEGOTIATION, 3, 4);
        commander.addSkill(SkillType.S_STRATEGY, 4, 5);
        assertRating(commander, Map.of("leadership", 3, "tactics", 5, "negotiation", 7, "strategy", 9, "total", 24));
    }

    @Test
    void testPersonality() {
        var commander = commanderWithLeadership(9);
        commander.setGreed(Greed.CORRUPT); // -2
        commander.setAmbition(Ambition.TYRANNICAL); // -2
        commander.setSocial(Social.NARCISSISTIC); // -2
        commander.setAggression(Aggression.COURAGEOUS); // +1
        assertRating(commander, Map.of("leadership", 9, "personality", 0, "total", 9));

        // ensure that both options are enabled before calculating personality score
        when(campaignOptions.isUseRandomPersonalities()).thenReturn(true);
        assertRating(commander, Map.of("leadership", 9, "personality", 0, "total", 9));
        when(campaignOptions.isUseRandomPersonalityReputation()).thenReturn(true);
        assertRating(commander, Map.of("leadership", 9, "personality", -5, "total", 4));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 5, 10})
    void testConnections(int connections) {
        var commander = commanderWithLeadership(9);
        commander.setConnections(connections);
        assertRating(commander, Map.of("traits", 1, "leadership", 9, "total", 10));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1, 6, 7, 10})
    void testWealth(int wealth) {
        var commander = commanderWithLeadership(5);
        commander.setWealth(wealth);
        int expectedTraits = wealth >= 7 ? 1 : 0;
        assertRating(commander, Map.of("traits", expectedTraits, "leadership", 5, "total", 5 + expectedTraits));
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, -1, 1, 5})
    void testReputation(int reputation) {
        var commander = commanderWithLeadership(5);
        commander.setReputation(reputation);
        int expectedTraits = Integer.compare(reputation, 0);
        assertRating(commander, Map.of("traits", expectedTraits, "leadership", 5, "total", 5 + expectedTraits));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 4, 6, 7, 10})
    void testCharisma(int charisma) {
        var commander = commanderWithLeadership(9);
        commander.setAttributeScore(SkillAttribute.CHARISMA, charisma);
        int expectedTraits = 0;
        if (charisma <= 3) {
            expectedTraits = -1;
        } else if (charisma >= 7) {
            expectedTraits = 1;
        }
        assertRating(commander, Map.of("traits", expectedTraits, "leadership", 9, "total", 9 + expectedTraits));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 5})
    void testUnlucky(int unlucky) {
        var commander = commanderWithLeadership(5);
        commander.setUnlucky(unlucky);
        assertRating(commander, Map.of("traits", -1, "leadership", 5, "total", 4));
    }

    @Test
    void testSPA() {
        var commander = commanderWithLeadership(5);
        PersonnelOptions options = commander.getOptions();
        options.acquireAbility(LVL3_ADVANTAGES, ATOW_COMBAT_SENSE, true);
        assertRating(commander, Map.of("traits", 1, "leadership", 5, "total", 6));

        options.acquireAbility(LVL3_ADVANTAGES, ATOW_COMBAT_SENSE, false);
        options.acquireAbility(LVL3_ADVANTAGES, ATOW_COMBAT_PARALYSIS, true);
        assertRating(commander, Map.of("traits", -1, "leadership", 5, "total", 4));
    }

}
