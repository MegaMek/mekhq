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
package mekhq.campaign.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import megamek.Version;
import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.ui.util.PlayerColour;
import megamek.common.enums.SkillLevel;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.backgrounds.RandomCompanyNameGenerator;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.stratCon.StratConCampaignState;
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
 * Save/load (XML serialization) regression tests for the {@link AbstractMission} hierarchy.
 *
 * <p>These tests guard the refactor that moved the shared mission/contract state onto {@link AbstractMission}, leaving
 * {@link Mission}, {@link Contract}, and {@link AtBContract} as thin subclasses. Because loading and saving of
 * contracts is a sensitive code path (a corrupted contract silently breaks a player's campaign), each sample file is
 * checked in two ways:</p>
 *
 * <ol>
 *     <li>The on-disk sample parses into the correct concrete type with the correct field values.</li>
 *     <li>A full load -&gt; save -&gt; load round-trip preserves both the headline field values and the complete
 *     serialized form (idempotency), proving the writer and reader agree on every tag.</li>
 * </ol>
 *
 * <p>The sample files live in {@code testresources/data/missions/} and mirror real {@code .cpnx} mission blocks for a
 * plain {@link Mission}, a {@link Contract}, and a fully-populated {@link AtBContract} (scenarios, StratCon state and
 * NPCs included).</p>
 */
public class AbstractMissionXmlIOTest {
    private static final Path MISSIONS_DIR = Path.of("testresources", "data", "missions");

    /** Any version at or above the current release; keeps the version-gated compatibility branches dormant. */
    private static final Version VERSION = new Version(999, 0, 0);

    private Campaign campaign;

    @BeforeAll
    public static void initSingletons() {
        EquipmentType.initializeTypes();
        // Required by the AtBContract load path when it has to synthesize the contract's NPCs / merc company names.
        // IntelliJ will tell you it's not being used, IntelliJ is lying
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

    // region Mission

    @Test
    void plainMissionLoadsAsMissionWithExpectedFields() throws Exception {
        AbstractMission mission = loadFirstMissionFromFile("Mission.cpnx");

        // A plain mission must NOT be promoted to a Contract / AtBContract.
        assertEquals(Mission.class, mission.getClass(), "plain mission must load as exactly Mission");
        assertEquals("New Mission", mission.getName());
        assertEquals("affdsf", mission.getContractTypeName());
        assertEquals(MissionStatus.ACTIVE, mission.getStatus());
        assertEquals("afdfadefadefd", mission.getDescription());
        assertEquals(1, mission.getId());
        assertTrue(mission.getScenarios().isEmpty(), "sample mission has no scenarios");
        // The sample uses the legacy <planetName> form for an unknown system, which is preserved verbatim.
        assertEquals("afadfadf", mission.getSystemName(campaign.getLocalDate()));
    }

    /**
     * Previously {@link AbstractMission#writeToXMLBegin} unconditionally wrote
     * {@code <contractType>UNDEFINED</contractType>} for a plain mission. On reload, the {@code contractType} handler
     * called {@code setContractTypeAndName(UNDEFINED)}, whose side effect overwrote the {@code contractTypeName} that
     * was just read from {@code <type>}; the free-text type (here {@code "affdsf"}) became {@code "Undefined"}. A plain
     * mission's type is therefore corrupted by a single save/load cycle. On {@code main} a plain mission never
     * serialized {@code <contractType>} at all.
     *
     * <p>This bug has been since fixed. The test remains to protect against regression.</p>
     */
    @Test
    void plainMissionSurvivesRoundTrip() throws Exception {
        assertRoundTripStable("Mission.cpnx");
    }

    // endregion Mission

    // region Contract

    @Test
    void contractLoadsAsContractWithExpectedFields() throws Exception {
        AbstractMission mission = loadFirstMissionFromFile("Contract.cpnx");

        assertEquals(Contract.class, mission.getClass(), "sample must load as exactly Contract");
        Contract contract = (Contract) mission;

        assertEquals("New Contract", contract.getName());
        assertEquals("Deception", contract.getContractTypeName());
        assertEquals("Terra", contract.getSystemId());
        assertEquals(MissionStatus.ACTIVE, contract.getStatus());
        assertEquals(1, contract.getId());

        // Terms and dates.
        assertEquals(12, contract.getLengthInMonths());
        assertEquals(LocalDate.of(3151, 2, 6), contract.getStartDate());
        assertEquals(LocalDate.of(3152, 2, 6), contract.getEndingDate());
        assertEquals("New Employer", contract.getEmployerName());
        assertEquals(2.0, contract.getPaymentMultiplier());
        assertEquals(ContractCommandRights.HOUSE, contract.getCommandRights());

        // Compensation terms.
        assertEquals(0, contract.getOverheadCompensation());
        assertEquals(50, contract.getSalvagePercent());
        assertFalse(contract.isSalvageExchange());
        assertEquals(50, contract.getStraightSupport());
        assertEquals(50, contract.getBattleLossCompensation());
        assertEquals(50, contract.getTransportCompensation());
        assertEquals(25, contract.getAdvancePercent());
        assertEquals(0, contract.getSigningBonus());

        // <mrbcFee>true</mrbcFee> must load (and later survive a save) as paid.
        assertTrue(contract.isPaidMRBCFee(), "contract sample has mrbcFee=true");
    }

    /**
     * Previously {@link AbstractMission#writeToXMLBegin} unconditionally wrote
     * {@code <contractType>UNDEFINED</contractType>} for a plain mission. On reload, the {@code contractType} handler
     * called {@code setContractTypeAndName(UNDEFINED)}, whose side effect overwrote the {@code contractTypeName} that
     * was just read from {@code <type>}; the free-text type (here {@code "affdsf"}) became {@code "Undefined"}. A plain
     * mission's type is therefore corrupted by a single save/load cycle. On {@code main} a plain mission never
     * serialized {@code <contractType>} at all.
     *
     * <p>This bug has been since fixed. The test remains to protect against regression.</p>
     */
    @Test
    void contractSurvivesRoundTrip() throws Exception {
        assertRoundTripStable("Contract.cpnx");
    }

    // endregion Contract

    // region AtBContract

    @Test
    void atbContractLoadsAsAtBContractWithExpectedFields() throws Exception {
        AbstractMission mission = loadFirstMissionFromFile("AtBContract.cpnx");

        assertEquals(AtBContract.class, mission.getClass(), "sample must load as exactly AtBContract");
        AtBContract contract = (AtBContract) mission;

        // Shared identity / terms.
        assertEquals("3151 - FWL - Avellaneda Objective Raid", contract.getName());
        assertEquals(MissionStatus.ACTIVE, contract.getStatus());
        assertEquals(2, contract.getId());
        assertEquals("Avellaneda", contract.getSystemId());
        assertEquals(3, contract.getLengthInMonths());
        assertEquals(LocalDate.of(3151, 2, 14), contract.getStartDate());
        assertEquals(LocalDate.of(3151, 5, 14), contract.getEndingDate());
        assertEquals(0.96, contract.getPaymentMultiplier());
        assertEquals(ContractCommandRights.HOUSE, contract.getCommandRights());

        // AtB faction / force data.
        assertEquals("FWL", contract.getEmployerCode());
        assertEquals("CP", contract.getEnemyCode());
        assertEquals(AtBContractType.OBJECTIVE_RAID, contract.getContractType());
        assertEquals("Objective Raid", contract.getContractTypeName());
        assertEquals(SkillLevel.ELITE, contract.getAllySkill());
        assertEquals(SkillLevel.REGULAR, contract.getEnemySkill());
        assertEquals(2, contract.getAllyQuality());
        assertEquals(0, contract.getEnemyQuality());
        assertEquals(5, contract.getContractDifficulty());
        assertEquals(1, contract.getRequiredCombatTeams());
        assertEquals(1, contract.getRequiredCombatElements());
        assertEquals(1, contract.getPartsAvailabilityLevel());
        assertEquals(0, contract.getSharesPercent());
        assertTrue(contract.isBatchallAccepted());

        // AtB-private state (no public getters, but accessible in-package).
        assertEquals(0, contract.extensionLength);
        assertEquals(0, contract.playerMinorBreaches);
        assertEquals(0, contract.employerMinorBreaches);
        assertEquals(0, contract.getContractScoreArbitraryModifier());
        assertFalse(contract.priorLogisticsFailure);

        // The heavy object graph (one StratCon scenario, StratCon campaign state, both NPCs) must load.
        assertEquals(1, contract.getScenarios().size(), "sample has a single AtBDynamicScenario");
        assertNotNull(contract.getStratConCampaignState(), "StratCon campaign state must load");
        assertNotNull(contract.getEmployerLiaison(), "employer liaison NPC must load");
        assertNotNull(contract.getClanOpponent(), "clan opponent NPC must load");
    }

    @Test
    void atbContractSurvivesRoundTrip() throws Exception {
        assertRoundTripStable("AtBContract.cpnx");
    }

    /**
     * Beyond "did the contract load", confirms the heavy nested objects deserialize with their real content: the
     * scenario, the StratCon campaign state (wired back to this contract), and both NPCs with their identities.
     */
    @Test
    void atbContractLoadsNestedObjectGraph() throws Exception {
        AtBContract contract = (AtBContract) loadFirstMissionFromFile("AtBContract.cpnx");

        // Scenario.
        assertEquals(1, contract.getScenarios().size());
        Scenario scenario = contract.getScenarios().getFirst();
        assertEquals("Assassination", scenario.getName());
        assertEquals(ScenarioStatus.CURRENT, scenario.getStatus());

        // StratCon campaign state, rewired to this contract during load.
        StratConCampaignState stratConCampaignState = contract.getStratConCampaignState();
        assertNotNull(stratConCampaignState);
        assertEquals(contract, stratConCampaignState.getContract(), "StratCon state must be wired to its contract");
        assertEquals(1, stratConCampaignState.getTracks().size());
        assertEquals(0, stratConCampaignState.getSupportPoints());
        assertEquals(0, stratConCampaignState.getVictoryPoints());

        // NPCs.
        Person employerLiaison = contract.getEmployerLiaison();
        assertNotNull(employerLiaison);
        assertEquals("Wamika", employerLiaison.getGivenName());
        assertEquals("Seshan", employerLiaison.getSurname());
        assertEquals(PersonnelRole.MILITARY_LIAISON, employerLiaison.getPrimaryRole());

        Person clanOpponent = contract.getClanOpponent();
        assertNotNull(clanOpponent);
        assertEquals("Nsen", clanOpponent.getGivenName());
        assertEquals(PersonnelRole.MEKWARRIOR, clanOpponent.getPrimaryRole());
    }

    // endregion AtBContract

    // region Programmatic round-trip (every field)

    /**
     * The file samples only populate the fields that happen to appear in them. This builds a contract in code with a
     * non-default value in <em>every</em> serialized field - including those the samples leave at defaults
     * (enemyMercenaryEmployerCode, the negotiation rolls, routEnd/routedPayout, rented facilities, ally/enemy colours,
     * etc.) - then writes and reads it back and asserts each value survives. This is the test that most directly guards
     * against the "written-but-not-read" / "read-but-not-written" tag mismatch that corrupts contracts.
     *
     * <p>A real {@code contractType} is set (rather than {@code UNDEFINED}) so that {@code type}/{@code contractType}
     * round-trip consistently here; the {@code UNDEFINED}-specific clobber is covered by
     * {@link #contractSurvivesRoundTrip()}.</p>
     */
    @Test
    void contractWithEveryFieldPopulatedSurvivesRoundTrip() throws Exception {
        Contract contract = new Contract();
        contract.setId(7);
        contract.setName("Fully Populated Contract");
        contract.setSystemId("Terra");
        contract.setStatus(MissionStatus.SUCCESS);
        contract.setDescription("a description");
        contract.setContractTypeAndName(AtBContractType.PIRATE_HUNTING);

        contract.setLengthInMonths(9);
        contract.setStartDate(LocalDate.of(3055, 3, 4));
        contract.setEndingDate(LocalDate.of(3055, 12, 4));
        contract.setEmployerName("Employer X");
        contract.setEmployerCode("FS");
        contract.setEnemyCode("DC");
        contract.setEnemyMercenaryEmployerCode("LA");
        contract.setPaymentMultiplier(1.75);
        contract.setCommandRights(ContractCommandRights.LIAISON);
        contract.setOverheadCompensation(2);
        contract.setSalvagePercent(40);
        contract.setSalvageExchange(true);
        contract.setStraightSupport(60);
        contract.setBattleLossCompensation(70);
        contract.setTransportCompensation(80);
        contract.setPaidMRBCFee(false);
        contract.setAdvancePercent(15);
        contract.setSigningBonus(3);
        contract.setHospitalBedsRented(4);
        contract.setKitchensRented(5);
        contract.setHoldingCellsRented(6);

        contract.setAdvanceAmount(Money.of(111));
        contract.setSigningBonusAmount(Money.of(222));
        contract.setTransportAmount(Money.of(333));
        contract.setTransitAmount(Money.of(444));
        contract.setOverheadAmount(Money.of(555));
        contract.setSupportAmount(Money.of(666));
        contract.setBaseAmount(Money.of(777));
        contract.setFeeAmount(Money.of(888));
        contract.setSalvagedByUnit(Money.of(999));
        contract.setSalvagedByEmployer(Money.of(1010));

        contract.setAllySkill(SkillLevel.VETERAN);
        contract.setAllyQuality(3);
        contract.setEnemySkill(SkillLevel.GREEN);
        contract.setEnemyQuality(1);
        contract.setContractDifficulty(7);
        contract.setAllyBotName("Ally Bot");
        contract.setEnemyBotName("Enemy Bot");
        contract.setAllyColour(PlayerColour.GREEN);
        contract.setEnemyColour(PlayerColour.YELLOW);

        contract.setRequiredCombatTeams(2);
        contract.setRequiredCombatElements(8);
        contract.setMoraleLevel(AtBMoraleLevel.ADVANCING);
        contract.setPartsAvailabilityLevel(4);
        contract.setSharesPercent(45);
        contract.setBatchallAccepted(false);

        contract.setContractNegotiationCommandRoll(1);
        contract.setContractNegotiationSalvageRoll(2);
        contract.setContractNegotiationSupportRoll(3);
        contract.setContractNegotiationTransportRoll(4);

        contract.setRoutEndDate(LocalDate.of(3055, 6, 1));
        contract.setRoutedPayout(Money.of(1212));

        Contract reloaded = (Contract) parseMission(writeMission(contract));

        assertEquals(contract.getClass(), reloaded.getClass());
        assertCoreFieldsEqual(contract, reloaded);

        // Fields not covered by assertCoreFieldsEqual.
        assertEquals(contract.getContractType(), reloaded.getContractType(), "contractType");
        assertEquals(contract.getEnemyMercenaryEmployerCode(), reloaded.getEnemyMercenaryEmployerCode(),
              "enemyMercenaryEmployerCode");
        assertEquals(contract.getAllyQuality(), reloaded.getAllyQuality(), "allyQuality");
        assertEquals(contract.getEnemyQuality(), reloaded.getEnemyQuality(), "enemyQuality");
        assertEquals(contract.getAllyBotName(), reloaded.getAllyBotName(), "allyBotName");
        assertEquals(contract.getEnemyBotName(), reloaded.getEnemyBotName(), "enemyBotName");
        assertEquals(contract.getAllyColour(), reloaded.getAllyColour(), "allyColour");
        assertEquals(contract.getEnemyColour(), reloaded.getEnemyColour(), "enemyColour");
        assertEquals(contract.getHospitalBedsRented(), reloaded.getHospitalBedsRented(), "hospitalBedsRented");
        assertEquals(contract.getKitchensRented(), reloaded.getKitchensRented(), "kitchensRented");
        assertEquals(contract.getHoldingCellsRented(), reloaded.getHoldingCellsRented(), "holdingCellsRented");
        assertEquals(contract.getRequiredCombatTeams(), reloaded.getRequiredCombatTeams(), "requiredCombatTeams");
        assertEquals(contract.getRequiredCombatElements(), reloaded.getRequiredCombatElements(),
              "requiredCombatElements");
        assertEquals(contract.getContractNegotiationCommandRoll(), reloaded.getContractNegotiationCommandRoll(),
              "commandRoll");
        assertEquals(contract.getContractNegotiationSalvageRoll(), reloaded.getContractNegotiationSalvageRoll(),
              "salvageRoll");
        assertEquals(contract.getContractNegotiationSupportRoll(), reloaded.getContractNegotiationSupportRoll(),
              "supportRoll");
        assertEquals(contract.getContractNegotiationTransportRoll(), reloaded.getContractNegotiationTransportRoll(),
              "transportRoll");
        assertEquals(contract.getRoutEndDate(), reloaded.getRoutEndDate(), "routEndDate");

        assertMoneyEquals(contract.getAdvanceAmount(), reloaded.getAdvanceAmount(), "advanceAmount");
        assertMoneyEquals(contract.getSigningBonusAmount(), reloaded.getSigningBonusAmount(), "signingBonusAmount");
        assertMoneyEquals(contract.getTransitAmount(), reloaded.getTransitAmount(), "transitAmount");
        assertMoneyEquals(contract.getOverheadAmount(), reloaded.getOverheadAmount(), "overheadAmount");
        assertMoneyEquals(contract.getSupportAmount(), reloaded.getSupportAmount(), "supportAmount");
        assertMoneyEquals(contract.getFeeAmount(), reloaded.getFeeAmount(), "feeAmount");

        assertNotNull(contract.getRoutedPayout());
        assertNotNull(reloaded.getRoutedPayout());
        assertMoneyEquals(contract.getRoutedPayout(), reloaded.getRoutedPayout(), "routedPayout");
    }

    /**
     * A freshly-constructed mission of each concrete type must serialize and deserialize without error and round-trip
     * its core identity. This guards against null-field NPEs in the writer (e.g. unset dates or camouflage) and against
     * a default object failing to reload as the same type.
     */
    @Test
    void freshlyConstructedMissionsSerializeWithoutError() throws Exception {
        Mission plainMission = new Mission();
        plainMission.setName("New Mission");
        Contract contract = new Contract();
        contract.setName("New Contract");
        AtBContract atbContract = new AtBContract();
        atbContract.setName("New AtB Contract");

        List<AbstractMission> missions = List.of(plainMission, contract, atbContract);

        for (AbstractMission mission : missions) {
            AbstractMission reloaded = parseMission(writeMission(mission));
            assertEquals(mission.getClass(), reloaded.getClass(), "type must survive for " + mission.getClass());
            assertEquals(mission.getName(), reloaded.getName(), "name must survive for " + mission.getClass());
            assertEquals(mission.getStatus(), reloaded.getStatus(), "status must survive for " + mission.getClass());
        }
    }

    // endregion Programmatic round-trip (every field)

    // region Save compatibility (legacy versions)

    /**
     * Pre-0.50.12 saves stored the player's out-of-pocket transport cost in {@code transportAmount}; newer saves store
     * the employer's reimbursement. {@link Contract#loadFieldsFromXmlNode} recomputes the value for old saves. This
     * verifies that version gate: an old save's stored amount is recomputed (to zero here, because the destination
     * system is unknown so there is no employer reimbursement), while a current save keeps the stored amount verbatim.
     */
    @Test
    void preTransportRewriteSaveRecomputesTransportAmount() throws Exception {
        String missionXml = """
              <mission id="1" type="mekhq.campaign.mission.Contract">
                  <name>Legacy Save</name>
                  <type>Garrison Duty</type>
                  <systemId>NoSuchSystem_ZZZ</systemId>
                  <status>ACTIVE</status>
                  <id>1</id>
                  <transportAmount>100 CSB</transportAmount>
              </mission>""";

        // Current-version save keeps the stored value untouched.
        Contract current = (Contract) generateFromXml(missionXml, new Version(999, 0, 0));
        assertNotNull(current);
        assertMoneyEquals(Money.of(100), current.getTransportAmount(), "transportAmount (current version)");

        // Pre-0.50.12 save triggers the recompute; unknown system -> zero employer reimbursement.
        Contract legacy = (Contract) generateFromXml(missionXml, new Version("0.50.11"));
        assertNotNull(legacy);
        assertMoneyEquals(Money.zero(), legacy.getTransportAmount(), "transportAmount (legacy migration)");
    }

    // endregion Save compatibility (legacy versions)

    // region Robustness (malformed input)

    /** An unknown {@code type} class must fail soft - logged and {@code null} - never throwing into the loader. */
    @Test
    void unknownMissionTypeReturnsNull() throws Exception {
        String missionXml = "<mission id=\"1\" type=\"mekhq.campaign.mission.NoSuchMissionClass\">"
                                  + "<name>x</name><id>1</id></mission>";
        assertNull(generateFromXml(missionXml, VERSION));
    }

    /**
     * A single corrupt field must not abort the whole contract load - the bad tag is skipped and the remaining tags
     * still parse. This is what prevents one malformed value from rendering an entire campaign unloadable.
     */
    @Test
    void malformedFieldIsSkippedAndRemainingFieldsStillLoad() throws Exception {
        String missionXml = """
              <mission id="1" type="mekhq.campaign.mission.Contract">
                  <name>Resilient Contract</name>
                  <id>1</id>
                  <advancePct>not-a-number</advancePct>
                  <salvagePct>42</salvagePct>
              </mission>""";

        int defaultAdvancePercent = new Contract().getAdvancePercent();

        Contract contract = (Contract) parseMission(missionXml);
        assertEquals("Resilient Contract", contract.getName());
        assertEquals(defaultAdvancePercent, contract.getAdvancePercent(),
              "malformed advancePct must be skipped, leaving the default");
        assertEquals(42, contract.getSalvagePercent(), "a field after the malformed one must still load");
    }

    // endregion Robustness (malformed input)

    // region Non-AtB scenarios

    /** Scenarios attached to a plain (non-AtB) contract must also deserialize. */
    @Test
    void nonAtBContractLoadsItsScenarios() throws Exception {
        String missionXml = """
              <mission id="3" type="mekhq.campaign.mission.Contract">
                  <name>Scenario Carrier</name>
                  <type>Garrison Duty</type>
                  <systemId>Terra</systemId>
                  <status>ACTIVE</status>
                  <id>3</id>
                  <scenarios>
                      <scenario id="1" type="mekhq.campaign.mission.Scenario">
                          <name>Border Skirmish</name>
                          <status>CURRENT</status>
                          <id>1</id>
                      </scenario>
                  </scenarios>
              </mission>""";

        Contract contract = (Contract) parseMission(missionXml);
        assertEquals(1, contract.getScenarios().size(), "plain contract must load its scenarios");
        assertEquals("Border Skirmish", contract.getScenarios().getFirst().getName());
    }

    // endregion Non-AtB scenarios

    // region Helpers

    /**
     * Loads a sample file, writes it back out, reloads the written form, and asserts that (a) the headline contract
     * fields are identical before and after and (b) the serialized XML is byte-for-byte stable across writes. The
     * second write onward must be idempotent; any writer/reader disagreement on a tag shows up here as a mismatch.
     */
    private void assertRoundTripStable(String fileName) throws Exception {
        AbstractMission original = loadFirstMissionFromFile(fileName);

        String firstWrite = writeMission(original);
        AbstractMission reloaded = parseMission(firstWrite);
        String secondWrite = writeMission(reloaded);

        assertEquals(original.getClass(), reloaded.getClass(), "concrete type must survive a save/load cycle");
        assertCoreFieldsEqual(original, reloaded);
        assertEquals(firstWrite, secondWrite, "serialized form must be stable across a save/load/save cycle");
    }

    private void assertCoreFieldsEqual(AbstractMission expected, AbstractMission actual) {
        assertEquals(expected.getName(), actual.getName(), "name");
        assertEquals(expected.getContractTypeName(), actual.getContractTypeName(), "contractTypeName");
        assertEquals(expected.getStatus(), actual.getStatus(), "status");
        assertEquals(expected.getId(), actual.getId(), "id");
        assertEquals(expected.getSystemId(), actual.getSystemId(), "systemId");
        assertEquals(expected.getDescription(), actual.getDescription(), "description");
        assertEquals(expected.getLengthInMonths(), actual.getLengthInMonths(), "lengthInMonths");
        assertEquals(expected.getStartDate(), actual.getStartDate(), "startDate");
        assertEquals(expected.getEndingDate(), actual.getEndingDate(), "endingDate");
        assertEquals(expected.getEmployerName(), actual.getEmployerName(), "employerName");
        assertEquals(expected.getPaymentMultiplier(), actual.getPaymentMultiplier(), "paymentMultiplier");
        assertEquals(expected.getCommandRights(), actual.getCommandRights(), "commandRights");
        assertEquals(expected.getSalvagePercent(), actual.getSalvagePercent(), "salvagePercent");
        assertEquals(expected.isSalvageExchange(), actual.isSalvageExchange(), "salvageExchange");
        assertEquals(expected.getStraightSupport(), actual.getStraightSupport(), "straightSupport");
        assertEquals(expected.getBattleLossCompensation(), actual.getBattleLossCompensation(), "battleLossComp");
        assertEquals(expected.getTransportCompensation(), actual.getTransportCompensation(), "transportComp");
        assertEquals(expected.getOverheadCompensation(), actual.getOverheadCompensation(), "overheadComp");
        assertEquals(expected.getAdvancePercent(), actual.getAdvancePercent(), "advancePercent");
        assertEquals(expected.getSigningBonus(), actual.getSigningBonus(), "signingBonus");
        assertEquals(expected.isPaidMRBCFee(), actual.isPaidMRBCFee(), "paidMRBCFee");
        assertEquals(expected.getEmployerCode(), actual.getEmployerCode(), "employerCode");
        assertEquals(expected.getEnemyCode(), actual.getEnemyCode(), "enemyCode");
        assertEquals(expected.getContractType(), actual.getContractType(), "contractType");
        assertEquals(expected.getAllySkill(), actual.getAllySkill(), "allySkill");
        assertEquals(expected.getEnemySkill(), actual.getEnemySkill(), "enemySkill");
        assertEquals(expected.getContractDifficulty(), actual.getContractDifficulty(), "contractDifficulty");
        assertEquals(expected.getMoraleLevel(), actual.getMoraleLevel(), "moraleLevel");
        assertEquals(expected.getSharesPercent(), actual.getSharesPercent(), "sharesPercent");
        assertEquals(expected.getPartsAvailabilityLevel(), actual.getPartsAvailabilityLevel(),
              "partsAvailabilityLevel");
        assertEquals(expected.getScenarios().size(), actual.getScenarios().size(), "scenario count");

        // Money fields.
        assertMoneyEquals(expected.getBaseAmount(), actual.getBaseAmount(), "baseAmount");
        assertMoneyEquals(expected.getTransportAmount(), actual.getTransportAmount(), "transportAmount");
        assertMoneyEquals(expected.getSalvagedByUnit(), actual.getSalvagedByUnit(), "salvagedByUnit");
        assertMoneyEquals(expected.getSalvagedByEmployer(), actual.getSalvagedByEmployer(), "salvagedByEmployer");
    }

    private static void assertMoneyEquals(Money expected, Money actual, String field) {
        // compareTo is scale-insensitive (1212 vs 1212.00), which the routedPayout reader can produce.
        assertEquals(0, expected.compareTo(actual), field + ": expected " + expected + " but was " + actual);
    }

    /**
     * Reads a {@code <missions>} sample file and instantiates the first {@code <mission>} element through the real
     * production entry point, {@link AbstractMission#generateInstanceFromXML}.
     */
    private AbstractMission loadFirstMissionFromFile(String fileName) throws Exception {
        byte[] bytes = Files.readAllBytes(MISSIONS_DIR.resolve(fileName));
        Document document = parseDocument(bytes);
        Node missionNode = firstChildElement(document.getDocumentElement());
        assertNotNull(missionNode, "sample " + fileName + " must contain a <mission> element");

        AbstractMission mission = AbstractMission.generateInstanceFromXML(missionNode, campaign, VERSION);
        assertNotNull(mission, "generateInstanceFromXML returned null for " + fileName);
        return mission;
    }

    /** Parses a single {@code <mission>} element (as produced by {@link #writeMission}) back into an object. */
    private AbstractMission parseMission(String missionXml) throws Exception {
        AbstractMission mission = generateFromXml(missionXml, VERSION);
        assertNotNull(mission, "re-parse of a written mission returned null");
        return mission;
    }

    /**
     * Instantiates a mission from a single {@code <mission>} XML string at the given save {@link Version}. Unlike
     * {@link #parseMission}, this returns whatever {@link AbstractMission#generateInstanceFromXML} produces - including
     * {@code null} for malformed input - so robustness and version-compatibility tests can assert on it.
     */
    private AbstractMission generateFromXml(String missionXml, Version version) throws Exception {
        Document document = parseDocument(missionXml.getBytes(StandardCharsets.UTF_8));
        return AbstractMission.generateInstanceFromXML(document.getDocumentElement(), campaign, version);
    }

    private String writeMission(AbstractMission mission) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            mission.writeToXML(campaign, printWriter, 0);
        }
        return stringWriter.toString();
    }

    private static Document parseDocument(byte[] bytes) throws Exception {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return MHQXMLUtility.newSafeDocumentBuilder().parse(inputStream);
        }
    }

    private static Node firstChildElement(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if ((child.getNodeType() == Node.ELEMENT_NODE) && child.getNodeName().equalsIgnoreCase("mission")) {
                return child;
            }
        }
        return null;
    }

    // endregion Helpers
}
