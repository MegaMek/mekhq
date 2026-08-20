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
package mekhq.campaign.mission.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import megamek.Version;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.contractData.ContractScheduleData;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.w3c.dom.Document;
import testUtilities.MHQTestUtilities;

/**
 * Tests {@link ContractMarket} - the four buckets of offers the player can browse, and their save/load.
 *
 * <p>The buckets must stay genuinely separate: an offer added to one search type appearing under another would put
 * mercenary work in front of a pirate band. Offers are keyed by contract id, so re-adding a contract must replace
 * rather than duplicate it, and a load must fully replace the current market rather than merge into it.</p>
 */
class ContractMarketTest {
    /** Any version at or above the current release; keeps the version-gated compatibility branches dormant. */
    private static final Version VERSION = new Version(999, 0, 0);

    private static final LocalDate START = LocalDate.of(3051, 1, 1);

    private ContractMarket market;
    private Campaign campaign;

    @BeforeAll
    static void initSingletons() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        market = new ContractMarket();
        campaign = MHQTestUtilities.getTestCampaign();
    }

    private static AbstractContract offer(String name) {
        AbstractContract contract = new ChaosContract();
        contract.setContractId(UUID.randomUUID());
        contract.setContractName(name);
        contract.setScheduleData(new ContractScheduleData(START, START.plusMonths(6), 6));
        return contract;
    }

    // region buckets

    @ParameterizedTest
    @EnumSource(ContractSearchType.class)
    void anOfferLandsOnlyInTheBucketItWasAddedTo(ContractSearchType searchType) {
        AbstractContract contract = offer("Offer");

        market.addContract(searchType, contract);

        assertEquals(1, market.getContracts(searchType).size());
        for (ContractSearchType other : ContractSearchType.values()) {
            if (other != searchType) {
                assertTrue(market.isEmpty(other), other + " must not see an offer added to " + searchType);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ContractSearchType.class)
    void eachSearchTypeResolvesToItsOwnNamedAccessor(ContractSearchType searchType) {
        assertSame(switch (searchType) {
            case MERCENARY -> market.getMercenaryWork();
            case PIRATE -> market.getActsOfPiracy();
            case GOVERNMENT -> market.getGovernmentOrders();
            case TOURNAMENT -> market.getTournament();
        }, market.getContracts(searchType), "the named accessor and the switch must reach the same map");
    }

    @Test
    void offersAreKeyedByContractIdSoReAddingReplacesRatherThanDuplicates() {
        AbstractContract contract = offer("Offer");

        market.addContract(ContractSearchType.MERCENARY, contract);
        market.addContract(ContractSearchType.MERCENARY, contract);

        assertEquals(1, market.getMercenaryWork().size());
        assertSame(contract, market.getMercenaryWork().get(contract.getId()));
    }

    @Test
    void removingAnOfferTakesItOutOfItsBucket() {
        AbstractContract contract = offer("Offer");
        market.addContract(ContractSearchType.PIRATE, contract);

        market.removeContract(ContractSearchType.PIRATE, contract);

        assertTrue(market.isEmpty(ContractSearchType.PIRATE));
    }

    @Test
    void clearingOneBucketLeavesTheOthersAlone() {
        market.addContract(ContractSearchType.MERCENARY, offer("Merc"));
        market.addContract(ContractSearchType.GOVERNMENT, offer("Gov"));

        market.clear(ContractSearchType.MERCENARY);

        assertTrue(market.isEmpty(ContractSearchType.MERCENARY));
        assertFalse(market.isEmpty(ContractSearchType.GOVERNMENT));
    }

    @Test
    void aFreshMarketIsEmptyInEveryBucket() {
        for (ContractSearchType searchType : ContractSearchType.values()) {
            assertTrue(market.isEmpty(searchType), searchType + " must start empty");
        }
    }

    // endregion buckets

    // region save/load

    @Test
    void everyBucketSurvivesARoundTrip() throws Exception {
        for (ContractSearchType searchType : ContractSearchType.values()) {
            market.addContract(searchType, offer(searchType.name() + " Offer"));
        }

        ContractMarket reloaded = reload(write());

        for (ContractSearchType searchType : ContractSearchType.values()) {
            assertEquals(1, reloaded.getContracts(searchType).size(), searchType + " lost its offer");
            assertEquals(searchType.name() + " Offer",
                  reloaded.getContracts(searchType).values().iterator().next().getName());
        }
    }

    @Test
    void anEmptyMarketWritesNothingAtAll() {
        assertTrue(write().isEmpty(), "a market with no offers must not emit an empty block into the save");
    }

    @Test
    void anEmptyBucketIsOmittedRatherThanWrittenEmpty() {
        market.addContract(ContractSearchType.MERCENARY, offer("Merc"));

        String xml = write();

        assertTrue(xml.contains("mercenaryWork"));
        assertFalse(xml.contains("actsOfPiracy"), "buckets with no offers are skipped");
    }

    @Test
    void loadingFullyReplacesTheCurrentMarket() throws Exception {
        market.addContract(ContractSearchType.MERCENARY, offer("Saved"));
        String saved = write();

        ContractMarket target = new ContractMarket();
        target.addContract(ContractSearchType.GOVERNMENT, offer("Stale"));
        load(target, saved);

        assertTrue(target.isEmpty(ContractSearchType.GOVERNMENT), "a load must not leave the previous market behind");
        assertEquals(1, target.getMercenaryWork().size());
    }

    @Test
    void unknownSectionIsIgnoredAndTheKnownOnesStillLoad() throws Exception {
        market.addContract(ContractSearchType.MERCENARY, offer("Merc"));
        String xml = write().replace("<contractMarket>", "<contractMarket><somethingNew><x/></somethingNew>");

        ContractMarket reloaded = reload(xml);

        assertEquals(1, reloaded.getMercenaryWork().size(),
              "an unrecognized section must not abort the rest of the market");
    }

    /**
     * Offers are stored by contract id, so a contract that failed to parse - or parsed without one - has no key to file
     * it under and is dropped rather than keyed on {@code null}.
     */
    @Test
    void anOfferWithoutAnIdIsDroppedRatherThanStoredUnderANullKey() throws Exception {
        String xml = "<contractMarket><mercenaryWork>"
                           + "<contract type=\"" + ChaosContract.class.getName() + "\">"
                           + "<contractName>Idless</contractName>"
                           + "</contract>"
                           + "</mercenaryWork></contractMarket>";

        ContractMarket reloaded = reload(xml);

        assertTrue(reloaded.isEmpty(ContractSearchType.MERCENARY));
    }

    // endregion save/load

    // region helpers

    private String write() {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            market.writeToXML(printWriter, 0, campaign);
        }
        return stringWriter.toString();
    }

    private ContractMarket reload(String marketXml) throws Exception {
        ContractMarket reloaded = new ContractMarket();
        load(reloaded, marketXml);
        return reloaded;
    }

    private void load(ContractMarket target, String marketXml) throws Exception {
        try (InputStream inputStream = new ByteArrayInputStream(marketXml.getBytes(StandardCharsets.UTF_8))) {
            Document document = MHQXMLUtility.newSafeDocumentBuilder().parse(inputStream);
            assertNotNull(document.getDocumentElement());
            target.loadFromXML(document.getDocumentElement(), campaign, VERSION);
        }
    }

    // endregion helpers
}
