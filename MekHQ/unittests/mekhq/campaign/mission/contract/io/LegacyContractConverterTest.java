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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import megamek.Version;
import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.ui.util.PlayerColour;
import megamek.common.enums.SkillLevel;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.personnel.backgrounds.RandomCompanyNameGenerator;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.TestSystems;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import testUtilities.MHQTestUtilities;

/**
 * Tests {@link LegacyContractConverter} against the real legacy save fragments in
 * {@code testresources/data/missions/}.
 *
 * <p>Conversion is deliberately lossy, so the value here is pinning what must <em>not</em> be lost: the fields with a
 * clean equivalent, the two NPCs the legacy format stored in full, and the close-out rule that force-completes a
 * contract that was still running while leaving a concluded one with the outcome it finished with. The placeholder
 * filling is asserted too, since the new model treats those fields as never-null and a gap there produces a contract
 * that breaks somewhere far from the loader.</p>
 */
class LegacyContractConverterTest {
    private static final Path MISSIONS_DIR = Path.of("testresources", "data", "missions");

    /** Any version at or above the current release; keeps the version-gated compatibility branches dormant. */
    private static final Version VERSION = new Version(999, 0, 0);

    private Campaign campaign;

    @BeforeAll
    static void initSingletons() {
        EquipmentType.initializeTypes();
        // Required when the converter has to synthesize placeholder NPCs for the contract.
        RandomCallsignGenerator.getInstance(true);
        RandomCompanyNameGenerator.getInstance();
        try {
            Factions.setInstance(Factions.loadDefault(true));
            Systems.setInstance(TestSystems.loadDefault());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    @BeforeEach
    void setUp() {
        campaign = MHQTestUtilities.getTestCampaign();
    }

    // region AtBContract sample

    @Test
    void legacyAtBContractCarriesOverItsHeadlineFields() throws Exception {
        AbstractContract contract = convertFirstMissionIn("AtBContract.cpnx");

        assertEquals("3151 - FWL - Avellaneda Objective Raid", contract.getName());
        assertEquals("FWL", contract.getEmployerFactionCode());
        assertEquals("Free Worlds League", contract.getEmployerDisplayName());
        assertEquals("CP", contract.getEnemyFactionCode());
        assertEquals("Clan Protectorate", contract.getEnemyDisplayName());
        assertEquals("Avellaneda", contract.getTargetSystemId());
        assertEquals(ContractObjectiveType.OBJECTIVE_RAID, contract.getObjectiveType());
        assertEquals(SkillLevel.ELITE, contract.getEmployerForceSkill());
        assertEquals(SkillLevel.REGULAR, contract.getEnemyForceSkill());
        assertEquals(2, contract.getEmployerEquipmentRating());
        assertEquals(0, contract.getEnemyEquipmentRating());
        assertEquals(ContractMoraleLevel.STALEMATE, contract.getMoraleLevel());
        assertTrue(contract.isBatchallAccepted());
    }

    @Test
    void legacyAtBContractCarriesOverItsSchedule() throws Exception {
        AbstractContract contract = convertFirstMissionIn("AtBContract.cpnx");

        assertEquals(LocalDate.of(3151, 2, 14), contract.getStartDate());
        assertEquals(LocalDate.of(3151, 5, 14), contract.getEndingDate());
        assertEquals(3, contract.getLengthInMonths());
    }

    /**
     * The liaison and the opposing commander are the only two NPCs the legacy format stored in full. An earlier
     * conversion dropped them and substituted placeholders, which is why they are pinned by name here.
     */
    @Test
    void legacyAtBContractKeepsTheNpcsTheSaveRecorded() throws Exception {
        AbstractContract contract = convertFirstMissionIn("AtBContract.cpnx");

        assertNotNull(contract.getEmployerLiaison(), "the save records a liaison; it must not be replaced");
        assertEquals("Wamika Seshan", contract.getEmployerLiaison().getFullName());

        assertNotNull(contract.getEnemyData().opposingCommander(), "the save records a clan opponent");
        assertEquals("Nsen", contract.getEnemyData().opposingCommander().getFullName());
    }

    /**
     * The employer's negotiator has no legacy equivalent, so it is always invented - but it must still be filled, since
     * the new model treats the NPC slots as never-null.
     */
    @Test
    void legacyAtBContractInventsTheEmployerNegotiatorItNeverHad() throws Exception {
        AbstractContract contract = convertFirstMissionIn("AtBContract.cpnx");

        assertNotNull(contract.getEmployerNegotiator(),
              "the negotiator slot is not nullable, so conversion must supply a placeholder");
    }

    @Test
    void legacyAtBContractRestoresItsStratConState() throws Exception {
        AbstractContract contract = convertFirstMissionIn("AtBContract.cpnx");

        assertNotNull(contract.getStratConCampaignState(), "the save carries StratCon state");
        assertEquals(contract, contract.getStratConCampaignState().getContract(),
              "restored StratCon state must point back at its new contract");
    }

    // endregion AtBContract sample

    // region close-out rule

    @Test
    void legacyContractThatWasStillRunningIsClosedOutAsASuccess() throws Exception {
        AbstractContract contract = convertFirstMissionIn("AtBContract.cpnx");

        assertEquals(MissionStatus.SUCCESS, contract.getStatus(),
              "an old-format contract cannot run under the new system, so an active one is force-completed");
    }

    @Test
    void legacyContractThatHadAlreadyConcludedKeepsItsOutcome() throws Exception {
        AbstractContract contract = convert(legacyMission("<status>FAILED</status>"));

        assertEquals(MissionStatus.FAILED, contract.getStatus(),
              "a contract that had already ended has nothing to close out and keeps the outcome it finished with");
    }

    @Test
    void outstandingRoutedPayoutOfAnActiveContractIsSettledToThePlayer() throws Exception {
        Money before = campaign.getPlayerForce().getFinances().getBalance();

        AbstractContract contract = convert(legacyMission("<status>ACTIVE</status><routedPayout>500000 CSB"
                                                                + "</routedPayout>"));

        assertEquals(MissionStatus.SUCCESS, contract.getStatus());
        assertEquals(before.plus(Money.of(500_000)), campaign.getPlayerForce().getFinances().getBalance(),
              "closing out a running contract must settle its outstanding payout");
    }

    @Test
    void concludedContractIsNotSettledAgain() throws Exception {
        Money before = campaign.getPlayerForce().getFinances().getBalance();

        convert(legacyMission("<status>BREACH</status><routedPayout>500000 CSB</routedPayout>"));

        assertEquals(before, campaign.getPlayerForce().getFinances().getBalance(),
              "a contract that had already ended was settled when it ended; it must not be paid twice");
    }

    // endregion close-out rule

    // region placeholders and defaults

    @Test
    void missingFactionCodesFallBackToThePlayersFactionAndPirates() throws Exception {
        AbstractContract contract = convert(legacyMission(""));

        assertEquals(campaign.getPlayerForce().getFaction().getShortName(), contract.getEmployerFactionCode(),
              "an employer-less legacy record falls back to the player's own faction");
        assertEquals("PIR", contract.getEnemyFactionCode(), "an enemy-less legacy record falls back to pirates");
    }

    @Test
    void missingDisplayNamesAreResolvedFromTheFactionCodes() throws Exception {
        AbstractContract contract = convert(legacyMission("<employerCode>LA</employerCode>"
                                                                + "<enemyCode>DC</enemyCode>"));

        assertEquals(Factions.getInstance().getFaction("LA").getFullName(campaign.getGameYear()),
              contract.getEmployerDisplayName());
        assertEquals(Factions.getInstance().getFaction("DC").getFullName(campaign.getGameYear()),
              contract.getEnemyDisplayName());
    }

    @Test
    void missingNameIsReplacedWithAGeneratedOne() throws Exception {
        AbstractContract contract = convert(legacyMission("<employerCode>LA</employerCode>"));

        assertFalse(contract.getName().isBlank(), "an unnamed legacy contract must still get a display name");
    }

    @Test
    void negotiableTermsAreSetToANeutralStep() throws Exception {
        AbstractContract contract = convert(legacyMission(""));

        assertEquals(ChaosContractStepsTable.STEP_SEVEN, contract.getBasePayRateStep(),
              "the legacy percentage model has no step-table equivalent, so terms convert to a neutral step");
        assertEquals(ChaosContractStepsTable.STEP_SEVEN, contract.getSupportStep());
        assertEquals(ChaosContractStepsTable.STEP_SEVEN, contract.getTransportStep());
        assertEquals(ChaosContractStepsTable.STEP_SEVEN, contract.getSalvageRightsStep());
        assertEquals(ChaosContractStepsTable.STEP_SEVEN, contract.getCommandRightsStep());
    }

    @Test
    void theOpposingObjectiveHasNoLegacyEquivalentAndConvertsAsUndefined() throws Exception {
        AbstractContract contract = convert(legacyMission("<contractType>PLANETARY_ASSAULT</contractType>"));

        assertEquals(ContractObjectiveType.PLANETARY_ASSAULT, contract.getObjectiveType());
        assertEquals(ContractObjectiveType.UNDEFINED, contract.getOpposingObjectiveType(),
              "the legacy format records only the player's side of the engagement");
    }

    @Test
    void everyConvertedContractGetsAFreshIdentity() throws Exception {
        AbstractContract first = convert(legacyMission("<id>2</id>"));
        AbstractContract second = convert(legacyMission("<id>2</id>"));

        assertNotNull(first.getId(), "the legacy integer id is replaced with a UUID");
        assertNotNull(second.getId());
        assertNotEquals(first.getId(), second.getId(),
              "two contracts sharing a legacy id must not collide on their new identities");
    }

    @Test
    void scaleIsClampedToAtLeastOne() throws Exception {
        AbstractContract contract = convert(legacyMission("<requiredCombatTeams>0</requiredCombatTeams>"));

        assertEquals(1, contract.getScale(), "a zero-scale contract would report no support-point reserve at all");
    }

    // endregion placeholders and defaults

    // region other legacy shapes

    /**
     * The oldest saves record the target as {@code planetId}; slightly newer ones as {@code systemId}. Newer still is a
     * save that has neither and only carries the system's display name.
     */
    @Test
    void oldestSavesRecordTheTargetSystemUnderPlanetId() throws Exception {
        AbstractContract contract = convert(legacyMission("<planetId>Galatea</planetId>"));

        assertEquals("Galatea", contract.getTargetSystemId());
    }

    @Test
    void legacyContractNamesNoPlanetWithinItsSystem() throws Exception {
        AbstractContract contract = convert(legacyMission("<systemId>Galatea</systemId>"));

        assertEquals("Galatea", contract.getTargetSystemId());
        assertNull(contract.getTargetPlanetId(), "the legacy format targets a system, never a world within it");
    }

    @Test
    void plainLegacyMissionConvertsToAWellFormedContract() throws Exception {
        AbstractContract contract = convertFirstMissionIn("Mission.cpnx");

        assertEquals("New Mission", contract.getName());
        assertNotNull(contract.getEmployerData(), "a plain mission has no employer, so one must be filled in");
        assertNotNull(contract.getEnemyData());
        assertNotNull(contract.getContractTerms());
        assertNotNull(contract.getMoraleData());
    }

    @Test
    void plainLegacyContractConvertsToAWellFormedContract() throws Exception {
        AbstractContract contract = convertFirstMissionIn("Contract.cpnx");

        assertEquals("New Contract", contract.getName());
        assertEquals("Terra", contract.getTargetSystemId());
        assertEquals(LocalDate.of(3151, 2, 6), contract.getStartDate());
        assertEquals(12, contract.getLengthInMonths());
    }

    @Test
    void unrecognizedLegacyElementIsSkippedAndTheRestStillConverts() throws Exception {
        AbstractContract contract = convert(legacyMission("<somethingLegacyOnly>x</somethingLegacyOnly>"
                                                                + "<employerCode>LA</employerCode>"));

        assertEquals("LA", contract.getEmployerFactionCode(),
              "a legacy-only field with no new-model equivalent must not abort the conversion");
    }

    @Test
    void malformedLegacyFieldIsSkippedAndTheRestStillConverts() throws Exception {
        AbstractContract contract = convert(legacyMission("<allySkill>NOT_A_SKILL</allySkill>"
                                                                + "<employerCode>LA</employerCode>"));

        assertEquals("LA", contract.getEmployerFactionCode());
        assertEquals(SkillLevel.REGULAR, contract.getEmployerForceSkill(),
              "the unparseable field keeps the default the accumulator was seeded with");
    }

    @Test
    void legacyColoursAreCarriedOver() throws Exception {
        AbstractContract contract = convert(legacyMission("<allyColour>GREEN</allyColour>"
                                                                + "<enemyColour>YELLOW</enemyColour>"));

        assertEquals(PlayerColour.GREEN, contract.getEmployerColor());
        assertEquals(PlayerColour.YELLOW, contract.getEnemyColour());
    }

    // endregion other legacy shapes

    // region helpers

    /** Wraps {@code body} in a legacy {@code <mission>} element of the retired {@code AtBContract} type. */
    private static String legacyMission(String body) {
        return "<mission id=\"1\" type=\"mekhq.campaign.mission.AtBContract\">" + body + "</mission>";
    }

    private AbstractContract convert(String missionXml) throws Exception {
        try (InputStream inputStream = new ByteArrayInputStream(missionXml.getBytes(StandardCharsets.UTF_8))) {
            Document document = MHQXMLUtility.newSafeDocumentBuilder().parse(inputStream);
            return LegacyContractConverter.convert(document.getDocumentElement(), campaign, VERSION);
        }
    }

    private AbstractContract convertFirstMissionIn(String fileName) throws Exception {
        byte[] bytes = Files.readAllBytes(MISSIONS_DIR.resolve(fileName));
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            Document document = MHQXMLUtility.newSafeDocumentBuilder().parse(inputStream);
            Node missionNode = firstMissionElement(document.getDocumentElement());
            assertNotNull(missionNode, "sample " + fileName + " must contain a <mission> element");
            return LegacyContractConverter.convert(missionNode, campaign, VERSION);
        }
    }

    private static Node firstMissionElement(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if ((child.getNodeType() == Node.ELEMENT_NODE) && child.getNodeName().equalsIgnoreCase("mission")) {
                return child;
            }
        }
        return null;
    }

    // endregion helpers
}
