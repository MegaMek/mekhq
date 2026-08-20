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

import static mekhq.campaign.mission.contract.io.ContractXmlCodec.CONTRACT_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import megamek.Version;
import megamek.client.ui.util.PlayerColour;
import megamek.common.enums.SkillLevel;
import megamek.common.equipment.EquipmentType;
import megamek.common.icons.Camouflage;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ChaosActOfPiracy;
import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.contract.ChaosGameWorlds;
import mekhq.campaign.mission.contract.contractData.*;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.TermFunding;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import testUtilities.MHQTestUtilities;

/**
 * Save/load tests for {@link ContractXmlCodec}.
 *
 * <p>A corrupted contract silently breaks a player's campaign, so this exercises the codec in both directions: every
 * populated field must survive a write, and a second write of the reloaded contract must be byte-identical to the
 * first. That idempotency check is what catches a field the writer emits but the reader ignores - the failure mode a
 * per-field assertion list misses whenever someone adds a field and forgets the test.</p>
 *
 * <p>The nested {@code Person} negotiators and the StratCon campaign state marshal themselves and are covered by
 * their own tests; the concern here is the contract's own state and the reader's tolerance of malformed input.</p>
 */
class ContractXmlCodecTest {
    /** Any version at or above the current release; keeps the version-gated compatibility branches dormant. */
    private static final Version VERSION = new Version(999, 0, 0);

    private static final LocalDate START = LocalDate.of(3051, 3, 4);

    private Campaign campaign;

    @BeforeAll
    static void initSingletons() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        campaign = MHQTestUtilities.getTestCampaign();
    }

    /**
     * A contract with every codec-visible field set to a non-default value, so a field the writer or reader drops shows
     * up as a difference rather than coinciding with the default.
     */
    private static AbstractContract fullyPopulatedContract() {
        AbstractContract contract = new ChaosContract();
        contract.setContractId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
        contract.setContractName("Operation Sundowner");
        contract.setDescription("A punitive raid into the Periphery.");
        contract.setScale(4);
        contract.setTrackCount(2);
        contract.setProvingGround(true);
        contract.setSharesPercent(45);
        contract.setStatus(MissionStatus.ACTIVE);
        contract.setSalvagedByUnitValue(Money.of(1_250_000));
        contract.setSalvagedByEmployerValue(Money.of(750_000));
        contract.setRequiredCombatElements(12);
        contract.setRequiredVictoryPoints(9);

        contract.setEmployerData(new EmployerData(ChaosEmployerType.LOCAL_PLANETARY_GOVERNMENT,
              "LA",
              "LA",
              "CS",
              "Lyran Alliance",
              null,
              null,
              SkillLevel.VETERAN,
              5,
              new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, "Blue"),
              PlayerColour.BLUE));
        contract.setEnemyData(new EnemyData("DC",
              "MERC",
              "Draconis Combine",
              SkillLevel.ELITE,
              7,
              null,
              new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, "Red"),
              PlayerColour.RED,
              false));
        contract.setContractTerms(new ContractTermsData(ChaosContractStepsTable.STEP_NINE,
              ChaosContractStepsTable.STEP_THREE,
              ChaosContractStepsTable.STEP_FIVE,
              ChaosContractStepsTable.STEP_SEVEN,
              ChaosContractStepsTable.STEP_ELEVEN));
        contract.setObjectiveData(new ContractObjectiveData(ContractObjectiveType.OBJECTIVE_RAID,
              ContractObjectiveType.GARRISON_DUTY));
        contract.setContractFinanceData(new ContractFinanceData(Money.of(2_000_000),
              Money.of(500_000),
              Money.of(125_000)));
        contract.setScheduleData(new ContractScheduleData(START, START.plusMonths(6), 6));
        contract.setSystemsTargetData(new SystemsTargetData("Galatea", "Galatea 3"));
        contract.setRentedFacilitiesData(new RentedFacilitiesData(8, 3, 5));
        contract.setMoraleData(new MoraleData(ContractMoraleLevel.ADVANCING,
              START.plusDays(20),
              Money.of(90_000)));
        contract.setNegotiationData(new NegotiationData(3,
              1,
              2,
              4,
              5,
              6,
              7,
              8,
              // Canonical clause order - pay, support, transport, salvage, command - with empty clauses kept in
              // place so their positions survive the round trip.
              List.of(List.of(TermFunding.REPUTATION),
                    List.of(),
                    List.of(TermFunding.REPUTATION, TermFunding.REPUTATION),
                    List.of(),
                    List.of())));
        return contract;
    }

    // region round trip

    @Test
    void fullyPopulatedContractSurvivesARoundTrip() throws Exception {
        AbstractContract original = fullyPopulatedContract();

        AbstractContract reloaded = reparse(write(original));

        assertNotNull(reloaded);
        assertCoreFieldsEqual(original, reloaded);
    }

    /**
     * Writing the reloaded contract must reproduce the original document exactly. Any field the writer emits but the
     * reader drops disappears on the second pass, so this catches gaps no hand-maintained assertion list would.
     */
    @Test
    void writingAReloadedContractReproducesTheOriginalDocument() throws Exception {
        String firstPass = write(fullyPopulatedContract());

        String secondPass = write(reparse(firstPass));

        assertEquals(firstPass, secondPass, "load -> save must be idempotent; a differing tag is a dropped field");
    }

    @Test
    void freshlyConstructedContractSurvivesARoundTrip() throws Exception {
        AbstractContract contract = new ChaosContract();
        contract.setScheduleData(new ContractScheduleData(null, null, 0));

        AbstractContract reloaded = reparse(write(contract));

        assertNotNull(reloaded, "a contract carrying almost nothing must still parse");
        assertNull(reloaded.getStartDate());
        assertEquals(ContractMoraleLevel.STALEMATE, reloaded.getMoraleLevel());
    }

    // endregion round trip

    // region concrete types

    @ParameterizedTest
    @ValueSource(classes = { ChaosContract.class, ChaosGameWorlds.class, ChaosActOfPiracy.class })
    void concreteContractTypeIsPreservedAcrossARoundTrip(Class<? extends AbstractContract> contractClass)
          throws Exception {
        AbstractContract contract = contractClass.getDeclaredConstructor().newInstance();
        contract.setScheduleData(new ContractScheduleData(START, START.plusMonths(3), 3));
        contract.setContractName("Typed");

        AbstractContract reloaded = reparse(write(contract));

        assertInstanceOf(contractClass, reloaded, "the concrete contract type must be restored from the save");
    }

    // endregion concrete types

    // region negotiator resolution

    /**
     * Contracts are written inside {@code <info>}, which the loader parses before the personnel roster exists, so the
     * player's negotiator cannot be looked up while the contract is being read. The codec stashes the raw id for the
     * loader to resolve in a later pass.
     */
    @Test
    void playerNegotiatorIsStashedAsAPendingIdRatherThanResolvedDuringTheRead() throws Exception {
        UUID negotiatorId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        String xml = "<contract type=\"" + ChaosContract.class.getName() + "\">"
                           + "<playerNegotiatorId>" + negotiatorId + "</playerNegotiatorId>"
                           + "</contract>";

        AbstractContract reloaded = parse(xml);

        assertNotNull(reloaded);
        assertEquals(negotiatorId, reloaded.getPendingPlayerNegotiatorId());
        assertNull(reloaded.getPlayerNegotiator(), "the roster does not exist yet, so nothing can be resolved");
    }

    // endregion negotiator resolution

    // region robustness

    @Test
    void contractWithoutATypeAttributeFailsSoftRatherThanThrowing() throws Exception {
        assertNull(parse("<contract><contractName>Nameless</contractName></contract>"),
              "a contract element with no type attribute cannot be instantiated and must return null");
    }

    @Test
    void unknownContractTypeFailsSoftRatherThanThrowing() throws Exception {
        assertNull(parse("<contract type=\"mekhq.campaign.mission.NoSuchContract\"/>"),
              "an unknown class must be logged and skipped, never thrown into the loader");
    }

    @Test
    void unknownElementIsIgnoredAndTheRemainingFieldsStillLoad() throws Exception {
        String xml = "<contract type=\"" + ChaosContract.class.getName() + "\">"
                           + "<somethingFromTheFuture>42</somethingFromTheFuture>"
                           + "<contractName>Still Here</contractName>"
                           + "</contract>";

        AbstractContract reloaded = parse(xml);

        assertNotNull(reloaded);
        assertEquals("Still Here", reloaded.getName(), "an unrecognized tag must not abort the rest of the contract");
    }

    @Test
    void malformedFieldIsSkippedAndTheRemainingFieldsStillLoad() throws Exception {
        String xml = "<contract type=\"" + ChaosContract.class.getName() + "\">"
                           + "<scale>not-a-number</scale>"
                           + "<contractName>Still Here</contractName>"
                           + "</contract>";

        AbstractContract reloaded = parse(xml);

        assertNotNull(reloaded);
        assertEquals("Still Here", reloaded.getName(), "one unparseable field must not lose the whole contract");
        assertEquals(0, reloaded.getScale(), "the unparseable field keeps its default");
    }

    @Test
    void malformedNestedRecordFieldIsSkippedAndTheRecordStillLoads() throws Exception {
        String xml = "<contract type=\"" + ChaosContract.class.getName() + "\">"
                           + "<scheduleData>"
                           + "<startDate>not-a-date</startDate>"
                           + "<lengthInMonths>6</lengthInMonths>"
                           + "</scheduleData>"
                           + "</contract>";

        AbstractContract reloaded = parse(xml);

        assertNotNull(reloaded);
        assertEquals(6, reloaded.getLengthInMonths(), "a bad field inside a record must not lose its siblings");
        assertNull(reloaded.getStartDate());
    }

    // endregion robustness

    // region helpers

    private String write(AbstractContract contract) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            ContractXmlCodec.writeContract(printWriter, 0, contract, campaign);
        }
        return stringWriter.toString();
    }

    private AbstractContract reparse(String contractXml) throws Exception {
        AbstractContract contract = parse(contractXml);
        assertNotNull(contract, "re-parse of a written contract returned null");
        return contract;
    }

    private AbstractContract parse(String contractXml) throws Exception {
        try (InputStream inputStream = new ByteArrayInputStream(contractXml.getBytes(StandardCharsets.UTF_8))) {
            Document document = MHQXMLUtility.newSafeDocumentBuilder().parse(inputStream);
            assertEquals(CONTRACT_TAG, document.getDocumentElement().getNodeName());
            return ContractXmlCodec.readContract(document.getDocumentElement(), campaign, VERSION);
        }
    }

    private static void assertCoreFieldsEqual(AbstractContract expected, AbstractContract actual) {
        assertEquals(expected.getId(), actual.getId(), "contractId");
        assertEquals(expected.getName(), actual.getName(), "contractName");
        assertEquals(expected.getDescription(), actual.getDescription(), "description");
        assertEquals(expected.getScale(), actual.getScale(), "scale");
        assertEquals(expected.getTrackCount(), actual.getTrackCount(), "trackCount");
        assertTrue(actual.isProvingGround(), "provingGround");
        assertEquals(expected.getSharesPercent(), actual.getSharesPercent(), "sharesPercent");
        assertEquals(expected.getStatus(), actual.getStatus(), "missionStatus");
        assertEquals(expected.getSalvagedByUnitValue(), actual.getSalvagedByUnitValue(), "salvagedByUnitValue");
        assertEquals(expected.getSalvagedByEmployerValue(), actual.getSalvagedByEmployerValue(),
              "salvagedByEmployerValue");

        assertEquals(expected.getEmployerType(), actual.getEmployerType(), "employer type");
        assertEquals(expected.getEmployerFactionCode(), actual.getEmployerFactionCode(), "employer factionCode");
        assertEquals(expected.getEmployerDisplayName(), actual.getEmployerDisplayName(), "employer displayName");
        assertEquals(expected.getEmployerForceSkill(), actual.getEmployerForceSkill(), "employer forceSkill");
        assertEquals(expected.getEmployerEquipmentRating(), actual.getEmployerEquipmentRating(),
              "employer equipmentRating");
        assertEquals(expected.getEmployerColor(), actual.getEmployerColor(), "employer color");

        assertEquals(expected.getEnemyFactionCode(), actual.getEnemyFactionCode(), "enemy factionCode");
        assertEquals(expected.getEnemySponsorFactionCode(), actual.getEnemySponsorFactionCode(),
              "enemy sponsorFactionCode");
        assertEquals(expected.getEnemyDisplayName(), actual.getEnemyDisplayName(), "enemy displayName");
        assertEquals(expected.getEnemyForceSkill(), actual.getEnemyForceSkill(), "enemy forceSkill");
        assertEquals(expected.getEnemyEquipmentRating(), actual.getEnemyEquipmentRating(), "enemy equipmentRating");
        assertEquals(expected.getEnemyColour(), actual.getEnemyColour(), "enemy color");
        assertFalse(actual.isBatchallAccepted(), "batchallAccepted");

        assertEquals(expected.getContractTerms(), actual.getContractTerms(), "contractTerms");
        assertEquals(expected.getObjectiveData(), actual.getObjectiveData(), "objectiveData");
        assertEquals(expected.getContractFinanceData(), actual.getContractFinanceData(), "contractFinanceData");
        assertEquals(expected.getScheduleData(), actual.getScheduleData(), "scheduleData");
        assertEquals(expected.getSystemsTargetData(), actual.getSystemsTargetData(), "systemsTargetData");
        assertEquals(expected.getRentedFacilitiesData(), actual.getRentedFacilitiesData(), "rentedFacilitiesData");
        assertEquals(expected.getMoraleData(), actual.getMoraleData(), "moraleData");
        assertEquals(expected.getNegotiationData(), actual.getNegotiationData(), "negotiationData");
    }

    // endregion helpers
}
