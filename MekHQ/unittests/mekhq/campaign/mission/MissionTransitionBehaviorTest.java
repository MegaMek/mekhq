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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.backgrounds.RandomCompanyNameGenerator;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.TestSystems;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Behavioral tests for the shared {@link AbstractMissionTransition} base class created by PR #9417.
 *
 * <p>All of the contract/mission state and arithmetic now lives on the single flattened base class, so the most
 * valuable coverage is (a) the financial helper math that every mission type now inherits, and (b) the null-tolerant
 * paths that protect against the {@code NullPointerException}s the flattening could introduce - chiefly an unset
 * destination system and unset start/end dates. These are exercised here with the lightest possible scaffolding so the
 * assertions stay deterministic.</p>
 *
 * @author Claude (test author for PR #9417 review)
 */
class MissionTransitionBehaviorTest {
    private static final String UNKNOWN_SYSTEM_ID = "NoSuchSystem_ZZZ";

    @BeforeAll
    static void initSingletons() {
        EquipmentType.initializeTypes();
        RandomCallsignGenerator.getInstance(true);
        RandomCompanyNameGenerator.getInstance();
        try {
            Factions.setInstance(Factions.loadDefault(true));
            Systems.setInstance(TestSystems.loadDefault());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    private static Campaign mockCampaignWithOptions() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        return campaign;
    }

    // region isActiveOn date handling (base, date-aware implementation)

    @Test
    void isActiveOnExcludeEndDateCheckIgnoresPastEndDate() {
        Contract contract = new Contract("c", "FS");
        contract.setStatus(MissionStatus.ACTIVE);
        contract.setStartDate(LocalDate.of(3051, 1, 1));
        contract.setEndingDate(LocalDate.of(3051, 12, 31));

        LocalDate afterEnd = LocalDate.of(3052, 3, 1);
        assertTrue(contract.isActiveOn(afterEnd, true),
              "excludeEndDateCheck == true must treat a past-end contract as still active");
        assertFalse(contract.isActiveOn(afterEnd, false),
              "excludeEndDateCheck == false must treat a past-end contract as inactive");
    }

    // endregion isActiveOn date handling

    // region getMonthsLeft / getMonthlyPayOut guards

    @Test
    void getMonthlyPayOutReturnsZeroForNonPositiveLength() {
        Contract contract = new Contract("c", "FS");
        contract.setLengthInMonths(0);
        contract.setBaseAmount(Money.of(1_000_000));
        assertTrue(contract.getMonthlyPayOut().isZero(),
              "A zero-length contract must yield a zero monthly payout rather than dividing by zero");
    }

    // endregion getMonthsLeft / getMonthlyPayOut guards

    // region null-system tolerance (key NPE surface after flattening)

    @Test
    void getSystemReturnsNullForUnknownSystemId() {
        Contract contract = new Contract("c", "FS");
        contract.setSystemId(UNKNOWN_SYSTEM_ID);
        assertNull(contract.getSystem(), "An unknown system id must resolve to a null PlanetarySystem");
    }

    @Test
    void getSystemNameFallsBackToLegacyPlanetNameWhenSystemIsNull() {
        Contract contract = new Contract("c", "FS");
        contract.setSystemId(UNKNOWN_SYSTEM_ID);
        contract.setLegacyPlanetName("Some Lost World");
        assertEquals("Some Lost World", contract.getSystemName(LocalDate.of(3051, 1, 1)),
              "getSystemName must fall back to the stored legacy planet name when the system cannot be resolved");
    }

    @Test
    void travelAndTransportCostsDegradeToZeroWithoutASystem() {
        Campaign campaign = mockCampaignWithOptions();
        Contract contract = new Contract("c", "FS");
        contract.setSystemId(UNKNOWN_SYSTEM_ID);

        assertEquals(0, contract.getTravelDays(campaign),
              "Travel days must be 0 when there is no destination system");
        assertTrue(contract.getTotalTransportationFees(campaign).isZero(),
              "Transportation fees must be zero when there is no destination system");
    }

    // endregion null-system tolerance

    // region faction resolution

    @Test
    void updateEmployerSetsCodeAndFactionDerivedName() {
        Contract contract = new Contract("c", "FS");
        contract.updateEmployer("LA", 3025);

        assertEquals("LA", contract.getEmployerCode(), "updateEmployer must store the supplied faction code");
        assertNotNull(contract.getEmployerFaction(), "A known faction code must resolve to a non-null faction");
        assertEquals(contract.getEmployerFaction().getFullName(3025), contract.getEmployerName(3025),
              "The faction-derived employer name must match the resolved faction's full name");
    }

    @Test
    void enemyFactionResolvesForKnownCode() {
        Contract contract = new Contract("c", "FS");
        contract.setEnemyCode("DC");
        assertNotNull(contract.getEnemy(), "A known enemy faction code must resolve to a non-null faction");
    }

    // endregion faction resolution

    // region financial helper arithmetic (inherited by every mission type)

    @Test
    void getTotalAmountSumsTheComponentAmounts() {
        Contract contract = new Contract("c", "FS");
        contract.setBaseAmount(Money.of(100));
        contract.setSupportAmount(Money.of(10));
        contract.setOverheadAmount(Money.of(5));
        contract.setTransportAmount(Money.of(3));
        contract.setTransitAmount(Money.of(2));

        assertEquals(0, Money.of(120).compareTo(contract.getTotalAmount()),
              "getTotalAmount must equal base + support + overhead + transport + transit");
    }

    @Test
    void getTotalAmountPlusFeesAndBonusesAppliesFeesAndSigningBonus() {
        Contract contract = new Contract("c", "FS");
        contract.setBaseAmount(Money.of(100));
        contract.setFeeAmount(Money.of(5));
        contract.setSigningBonusAmount(Money.of(20));

        assertEquals(0, Money.of(95).compareTo(contract.getTotalAmountPlusFees()),
              "getTotalAmountPlusFees must subtract the MRBC fee from the total");
        assertEquals(0, Money.of(115).compareTo(contract.getTotalAmountPlusFeesAndBonuses()),
              "getTotalAmountPlusFeesAndBonuses must add the signing bonus on top of the post-fee total");
    }

    @Test
    void canSalvageReflectsSalvagePercent() {
        Contract contract = new Contract("c", "FS");
        contract.setSalvagePercent(0);
        assertFalse(contract.canSalvage(), "0% salvage rights must report canSalvage() == false");
        contract.setSalvagePercent(40);
        assertTrue(contract.canSalvage(), "A positive salvage percentage must report canSalvage() == true");
    }

    // endregion financial helper arithmetic

    // region salvage percentage rounding (CEILING)

    /**
     * {@code calculateSalvagePercentage} rounds up by design (a fractional breach of the salvage cap must surface as a
     * breach). The cases below pin the boundary behavior, including the divide-by-nothing guard for an empty split.
     */
    @ParameterizedTest
    @CsvSource({
          "0, 0, 0",      // nothing salvaged -> 0, no divide-by-zero
          "1, 1, 50",     // even split
          "1, 2, 34",     // 33.33% rounds UP to 34
          "2, 1, 67",     // 66.66% rounds UP to 67
          "1, 0, 100",    // player took everything
          "0, 1, 0"       // employer took everything
    })
    void calculateSalvagePercentageRoundsUp(long playerShare, long employerShare, int expectedPercent) {
        assertEquals(expectedPercent,
              AbstractMissionTransition.calculateSalvagePercentage(Money.of(playerShare), Money.of(employerShare)),
              "Salvage percentage must round up (CEILING)");
    }

    @Test
    void getCurrentSalvagePctUsesStoredSalvageTotals() {
        Contract contract = new Contract("c", "FS");
        contract.setSalvagedByUnit(Money.of(1));
        contract.setSalvagedByEmployer(Money.of(2));
        assertEquals(34, contract.getCurrentSalvagePct(),
              "getCurrentSalvagePct must compute from the stored unit/employer salvage totals with CEILING rounding");
    }

    // endregion salvage percentage rounding

    // region setStartAndEndDate

    @Test
    void setStartAndEndDateDerivesEndFromLength() {
        Contract contract = new Contract("c", "FS");
        contract.setLengthInMonths(9);
        contract.setStartAndEndDate(LocalDate.of(3055, 3, 4));

        assertEquals(LocalDate.of(3055, 3, 4), contract.getStartDate(), "start date must be the supplied date");
        assertEquals(LocalDate.of(3055, 12, 4), contract.getEndingDate(),
              "ending date must be the start date plus the contract length in months");
    }

    // endregion setStartAndEndDate
}
