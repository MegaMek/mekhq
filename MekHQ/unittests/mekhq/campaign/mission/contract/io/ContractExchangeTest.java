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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import megamek.client.ui.util.PlayerColour;
import megamek.common.enums.SkillLevel;
import megamek.common.equipment.EquipmentType;
import megamek.common.icons.Camouflage;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ChaosActOfPiracy;
import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.contract.contractData.*;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.mission.contract.io.ContractExchange.ExportEntry;
import mekhq.campaign.mission.contract.io.ContractExchange.ImportResult;
import mekhq.campaign.mission.contract.io.ContractExchange.SkipReason;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Tests for {@link ContractExchange}, the JSON export and import of contract offers.
 *
 * <p>A shared file is read by another player's campaign, so these check both that an offer arrives intact and that
 * nothing belonging to the exporting campaign comes with it. The NPC snapshots have their own tests
 * ({@link ContractNpcJsonTest}); the offers here carry no NPCs.</p>
 */
class ContractExchangeTest {
    private static final LocalDate START = LocalDate.of(3051, 3, 4);
    private static final UUID ORIGINAL_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private Campaign campaign;

    @BeforeAll
    static void initSingletons() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        campaign = MHQTestUtilities.getTestCampaign();
    }

    /** An offer with every exported field set away from its default, so a dropped field shows up as a difference. */
    private static AbstractContract fullyPopulatedOffer() {
        AbstractContract contract = new ChaosContract();
        contract.setContractId(ORIGINAL_ID);
        contract.setContractName("Operation Sundowner");
        contract.setNameOperationCodename(true);
        contract.setDescription("A punitive raid into the **Periphery**.");
        contract.setScale(4);
        contract.setSalvageTaperMultiplier(0.75);
        contract.setTrackCount(2);
        contract.setRequiredVictoryPoints(9);
        contract.setScenarioSchedule(List.of(0, 1, 0, 1, 0, 0));
        contract.setNature(ContractNature.PROVING_GROUND);
        contract.setSharesPercent(45);
        contract.setObfuscatedIntel(EnumSet.of(ObfuscatableIntel.THREAT, ObfuscatableIntel.MORALE));
        contract.setCharacteristics(EnumSet.of(ContractCharacteristic.CAREER_MAKER, ContractCharacteristic.CUT_RATE));

        Camouflage employerCamouflage = new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, "Blue");
        employerCamouflage.setRotationAngle(90);
        employerCamouflage.setScale(12);
        contract.setEmployerData(new EmployerData(ChaosEmployerType.LOCAL_PLANETARY_GOVERNMENT, "LA", "LA", "CS",
              "Lyran Alliance", null, null, SkillLevel.VETERAN, 5, employerCamouflage, PlayerColour.GOLD));
        contract.setEnemyData(new EnemyData("DC", "MERC", "Draconis Combine", SkillLevel.ELITE, 7, null,
              new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, "Red"), PlayerColour.RED, true));
        contract.setContractTerms(new ContractTermsData(ChaosContractStepsTable.STEP_NINE,
              ChaosContractStepsTable.STEP_THREE, ChaosContractStepsTable.STEP_FIVE, ChaosContractStepsTable.STEP_SEVEN,
              ChaosContractStepsTable.STEP_ELEVEN));
        contract.setObjectiveData(new ContractObjectiveData(ContractObjectiveType.OBJECTIVE_RAID,
              ContractObjectiveType.GARRISON_DUTY));
        contract.setContractFinanceData(new ContractFinanceData(Money.of(2_000_000), Money.of(500_000.5),
              Money.of(125_000)));
        contract.setScheduleData(new ContractScheduleData(START, START.plusMonths(6), 6));
        contract.setSystemsTargetData(new SystemsTargetData(null, null));
        contract.setRentedFacilitiesData(new RentedFacilitiesData(8, 3, 5));
        contract.setNonNegotiableTermsData(new NonNegotiableTermsData(true, false, true, false, false));
        contract.setMoraleData(new MoraleData(ContractMoraleLevel.ADVANCING));
        return contract;
    }

    private AbstractContract roundTrip(AbstractContract contract, ContractSearchType searchType) throws IOException {
        ImportResult result = ContractExchange.fromJson(
              ContractExchange.toJson(List.of(new ExportEntry(contract, searchType))), campaign);
        assertEquals(1, result.imported().size(), "the offer must import");
        assertTrue(result.skipped().isEmpty());
        return result.imported().getFirst().contract();
    }

    // region Round trip

    @Test
    void anOfferArrivesIntact() throws IOException {
        AbstractContract original = fullyPopulatedOffer();

        AbstractContract imported = roundTrip(original, ContractSearchType.MERCENARY);

        assertInstanceOf(ChaosContract.class, imported);
        assertEquals("Operation Sundowner", imported.getName());
        assertTrue(imported.isNameOperationCodename());
        assertEquals(original.getDescription(), imported.getDescription());
        assertEquals(original.getExactScale(), imported.getExactScale());
        assertEquals(0.75, imported.getSalvageTaperMultiplier());
        assertEquals(2, imported.getTrackCount());
        assertEquals(9, imported.getRequiredVictoryPoints());
        assertEquals(List.of(0, 1, 0, 1, 0, 0), imported.getScenarioSchedule());
        assertEquals(ContractNature.PROVING_GROUND, imported.getNature());
        assertEquals(45, imported.getSharesPercent());
        assertEquals(Set.of(ObfuscatableIntel.THREAT, ObfuscatableIntel.MORALE), imported.getObfuscatedIntel());
        assertEquals(EnumSet.of(ContractCharacteristic.CAREER_MAKER, ContractCharacteristic.CUT_RATE),
              imported.getCharacteristics());
        assertEquals(original.getContractTerms(), imported.getContractTerms());
        assertEquals(original.getObjectiveData(), imported.getObjectiveData());
        assertEquals(original.getScheduleData(), imported.getScheduleData());
        assertEquals(original.getRentedFacilitiesData(), imported.getRentedFacilitiesData());
        assertEquals(original.getNonNegotiableTermsData(), imported.getNonNegotiableTermsData());
        assertEquals(ContractMoraleLevel.ADVANCING, imported.getMoraleLevel());
    }

    @Test
    void theOffersPaymentsArriveToTheCent() throws IOException {
        AbstractContract imported = roundTrip(fullyPopulatedOffer(), ContractSearchType.MERCENARY);

        ContractFinanceData finance = imported.getContractFinanceData();
        assertEquals(0, Money.of(2_000_000).getAmount().compareTo(finance.transport().getAmount()));
        assertEquals(0, Money.of(500_000.5).getAmount().compareTo(finance.monthlyPay().getAmount()));
        assertEquals(0, Money.of(125_000).getAmount().compareTo(finance.combatPay().getAmount()));
    }

    @Test
    void theEmployerAndEnemyArriveIntact() throws IOException {
        AbstractContract original = fullyPopulatedOffer();

        AbstractContract imported = roundTrip(original, ContractSearchType.MERCENARY);

        EmployerData employer = imported.getEmployerData();
        assertEquals(ChaosEmployerType.LOCAL_PLANETARY_GOVERNMENT, employer.type());
        assertEquals("LA", employer.factionCode());
        assertEquals("LA", employer.anchorFactionCode());
        assertEquals("CS", employer.sponsorFactionCode());
        assertEquals("Lyran Alliance", employer.displayName());
        assertEquals(SkillLevel.VETERAN, employer.forceSkill());
        assertEquals(5, employer.equipmentRating());
        assertEquals(PlayerColour.GOLD, employer.color());
        assertEquals(Camouflage.COLOUR_CAMOUFLAGE, employer.camouflage().getCategory());
        assertEquals("Blue", employer.camouflage().getFilename());
        assertEquals(90, employer.camouflage().getRotationAngle());
        assertEquals(12, employer.camouflage().getScale());

        EnemyData enemy = imported.getEnemyData();
        assertEquals("DC", enemy.factionCode());
        assertEquals("MERC", enemy.sponsorFactionCode());
        assertEquals("Draconis Combine", enemy.displayName());
        assertEquals(SkillLevel.ELITE, enemy.forceSkill());
        assertEquals(7, enemy.equipmentRating());
        assertEquals(PlayerColour.RED, enemy.color());
        assertTrue(enemy.batchallAccepted());
    }

    @Test
    void theConcreteContractTypeIsKept() throws IOException {
        AbstractContract piracy = fullyPopulatedOffer();
        AbstractContract original = new ChaosActOfPiracy();
        original.setContractName(piracy.getName());
        original.setEmployerData(piracy.getEmployerData());
        original.setEnemyData(piracy.getEnemyData());
        original.setScheduleData(piracy.getScheduleData());

        assertInstanceOf(ChaosActOfPiracy.class, roundTrip(original, ContractSearchType.PIRATE));
    }

    @Test
    void theBoardTheOfferWasListedOnIsKept() throws IOException {
        ImportResult result = ContractExchange.fromJson(ContractExchange.toJson(List.of(
              new ExportEntry(fullyPopulatedOffer(), ContractSearchType.TOURNAMENT))), campaign);

        assertEquals(ContractSearchType.TOURNAMENT, result.imported().getFirst().searchType());
    }

    @Test
    void severalOffersImportInOrder() throws IOException {
        AbstractContract first = fullyPopulatedOffer();
        AbstractContract second = fullyPopulatedOffer();
        second.setContractName("Operation Daybreak");

        ImportResult result = ContractExchange.fromJson(ContractExchange.toJson(List.of(
              new ExportEntry(first, ContractSearchType.MERCENARY),
              new ExportEntry(second, ContractSearchType.MERCENARY))), campaign);

        assertEquals(2, result.imported().size());
        assertEquals("Operation Sundowner", result.imported().get(0).contract().getName());
        assertEquals("Operation Daybreak", result.imported().get(1).contract().getName());
    }

    // endregion Round trip

    // region Clean offers

    @Test
    void anImportedOfferGetsAFreshId() throws IOException {
        AbstractContract imported = roundTrip(fullyPopulatedOffer(), ContractSearchType.MERCENARY);

        assertNotNull(imported.getId());
        assertNotEquals(ORIGINAL_ID, imported.getId());
    }

    @Test
    void importingTheSameFileTwiceGivesTwoDistinctOffers() throws IOException {
        String json = ContractExchange.toJson(List.of(
              new ExportEntry(fullyPopulatedOffer(), ContractSearchType.MERCENARY)));

        UUID first = ContractExchange.fromJson(json, campaign).imported().getFirst().contract().getId();
        UUID second = ContractExchange.fromJson(json, campaign).imported().getFirst().contract().getId();

        assertNotEquals(first, second, "a second import must not overwrite the first on the board");
    }

    @Test
    void theExportingCampaignsStateIsLeftBehind() throws IOException {
        AbstractContract original = fullyPopulatedOffer();
        original.setStatus(MissionStatus.ACTIVE);
        original.setMandatedCompletionStatus(MissionStatus.PARTIAL);
        original.setSalvagedByUnitValue(Money.of(1_250_000));
        original.setWithheldSupportPayments(Money.of(30_000));
        original.setConsecutiveTrackResultTally(3);
        original.setPendingPlayerNegotiatorId(UUID.randomUUID());
        original.setActiveNegotiationData(ActiveNegotiationData.haggle(1, 2, 0, 0, 0, 0, 0));
        original.setNegotiationData(new NegotiationData(1, 2, 3, 4, 5, 6, 7, 8, List.of()));

        AbstractContract imported = roundTrip(original, ContractSearchType.MERCENARY);

        assertNull(imported.getStatus(), "an imported offer has not been accepted");
        assertNull(imported.getMandatedCompletionStatus());
        assertTrue(imported.getSalvagedByUnitValue().isZero());
        assertTrue(imported.getWithheldSupportPayments().isZero());
        assertEquals(0, imported.getConsecutiveTrackResultTally());
        assertNull(imported.getPendingPlayerNegotiatorId(), "the exporter's negotiator is not in this campaign");
        assertNull(imported.getActiveNegotiationData());
        assertNull(imported.getNegotiationData());
        assertNull(imported.getStratConCampaignState());
        assertTrue(imported.getScenarios().isEmpty());
    }

    @Test
    void theFileCarriesNoIdsFromTheExportingCampaign() throws IOException {
        String json = ContractExchange.toJson(List.of(
              new ExportEntry(fullyPopulatedOffer(), ContractSearchType.MERCENARY)));

        assertFalse(json.contains(ORIGINAL_ID.toString()));
    }

    // endregion Clean offers

    // region Tolerance

    @Test
    void anOfferWhoseTargetSystemIsNotOnTheMapIsSkipped() throws IOException {
        AbstractContract original = fullyPopulatedOffer();
        original.setSystemsTargetData(new SystemsTargetData("Not A Real System 12345", null));

        ImportResult result = ContractExchange.fromJson(ContractExchange.toJson(List.of(
              new ExportEntry(original, ContractSearchType.MERCENARY))), campaign);

        assertTrue(result.imported().isEmpty());
        assertEquals(1, result.skipped().size());
        assertEquals(SkipReason.UNKNOWN_TARGET_SYSTEM, result.skipped().getFirst().reason());
        assertEquals("Operation Sundowner", result.skipped().getFirst().name());
    }

    @Test
    void anOfferOfAnUnknownContractTypeIsSkipped() throws IOException {
        String json = ContractExchange.toJson(List.of(
              new ExportEntry(fullyPopulatedOffer(), ContractSearchType.MERCENARY)));

        ImportResult result = ContractExchange.fromJson(
              json.replace("\"ChaosContract\"", "\"java.lang.Runtime\""), campaign);

        assertTrue(result.imported().isEmpty(), "a file must not be able to construct an arbitrary class");
        assertEquals(SkipReason.UNKNOWN_CONTRACT_TYPE, result.skipped().getFirst().reason());
    }

    @Test
    void oneBadOfferDoesNotCostTheRestOfTheFile() throws IOException {
        AbstractContract bad = fullyPopulatedOffer();
        bad.setContractName("Lost Cause");
        bad.setSystemsTargetData(new SystemsTargetData("Not A Real System 12345", null));

        ImportResult result = ContractExchange.fromJson(ContractExchange.toJson(List.of(
              new ExportEntry(bad, ContractSearchType.MERCENARY),
              new ExportEntry(fullyPopulatedOffer(), ContractSearchType.MERCENARY))), campaign);

        assertEquals(1, result.imported().size());
        assertEquals(1, result.skipped().size());
        assertEquals("Lost Cause", result.skipped().getFirst().name());
    }

    @Test
    void anUnknownEnumValueFallsBackToADefault() throws IOException {
        String json = ContractExchange.toJson(List.of(
              new ExportEntry(fullyPopulatedOffer(), ContractSearchType.MERCENARY)));

        AbstractContract imported = ContractExchange.fromJson(
              json.replace("\"STEP_NINE\"", "\"STEP_FROM_THE_FUTURE\""), campaign).imported().getFirst().contract();

        assertEquals(ChaosContractStepsTable.STEP_SEVEN, imported.getContractTerms().payRate());
        assertEquals(ChaosContractStepsTable.STEP_THREE, imported.getContractTerms().support());
    }

    @Test
    void anUnknownCharacteristicIsDropped() throws IOException {
        String json = ContractExchange.toJson(List.of(
              new ExportEntry(fullyPopulatedOffer(), ContractSearchType.MERCENARY)));

        AbstractContract imported = ContractExchange.fromJson(
              json.replace("\"CUT_RATE\"", "\"NOT_A_CHARACTERISTIC\""), campaign).imported().getFirst().contract();

        assertEquals(EnumSet.of(ContractCharacteristic.CAREER_MAKER), imported.getCharacteristics());
    }

    @Test
    void anUnknownBoardReadsAsNoBoard() throws IOException {
        String json = ContractExchange.toJson(List.of(
              new ExportEntry(fullyPopulatedOffer(), ContractSearchType.MERCENARY)));

        ImportResult result = ContractExchange.fromJson(json.replace("\"MERCENARY\"", "\"SPACE_PIRATES\""),
              campaign);

        assertNull(result.imported().getFirst().searchType(), "the market then puts it on the board on screen");
    }

    @Test
    void fieldsFromANewerVersionAreIgnored() throws IOException {
        String json = ContractExchange.toJson(List.of(
              new ExportEntry(fullyPopulatedOffer(), ContractSearchType.MERCENARY)));

        ImportResult result = ContractExchange.fromJson(
              json.replace("\"name\" :", "\"aFieldFromTheFuture\" : 42, \"name\" :"), campaign);

        assertEquals("Operation Sundowner", result.imported().getFirst().contract().getName());
    }

    @Test
    void textThatIsNotJsonIsRefused() {
        assertThrows(IOException.class, () -> ContractExchange.fromJson("<contract>not json</contract>", campaign));
    }

    @Test
    void jsonThatIsNotAContractFileIsRefused() {
        assertThrows(IOException.class, () -> ContractExchange.fromJson("{\"units\": []}", campaign));
    }

    // endregion Tolerance
}
