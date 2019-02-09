/*
 * ContractTest.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.mission;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Money;
import mekhq.campaign.universe.Planet;
import org.joda.money.CurrencyUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.mockito.Mockito.spy;

/**
 * @author Miguel Azevedo
 * @version %Id%
 * @since 20/07/18 10:23 AM
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Money.class)
public class ContractTest {

    private Contract contract;
    private Campaign mockCampaign;

    private Money jumpCost;
    private Money contractBase;
    private Money overHeadExpenses;
    private Money peacetimeCost;

    @Before
    public void setUp() {
        // We need to create these objects before we tell PowerMockito to
        // mock the class. After we tell the class to be mocked, all methods not
        // covered with a when call will just return null;
        jumpCost = Money.of(5, CurrencyUnit.USD);
        contractBase = Money.of(10, CurrencyUnit.USD);
        overHeadExpenses = Money.of(1, CurrencyUnit.USD);
        peacetimeCost = Money.of(1, CurrencyUnit.USD);

        PowerMockito.mockStatic(Money.class);
        Money zero = Money.zero(CurrencyUnit.USD);

        Mockito.when(Money.zero()).thenReturn(zero);
    }

    @Test
    public void testGetBaseAmount() {
        initializeTest();
        Assert.assertEquals("USD 130.0", contract.getBaseAmount().toString());
    }

    @Test
    public void testGetOverheadAmount() {
        initializeTest();
        Assert.assertEquals("USD 10", contract.getOverheadAmount().toString());
    }

    @Test
    public void testGetSupportAmount() {
        initializeTest();
        Assert.assertEquals("USD 10", contract.getSupportAmount().toString());
    }

    @Test
    public void testGetTransportAmount() {
        initializeTest();
        Assert.assertEquals("USD 20", contract.getTransportAmount().toString());
    }

    @Test
    public void testGetTransitAmount() {
        initializeTest();
        Assert.assertEquals("USD 30.00", contract.getTransitAmount().toString());
    }

    @Test
    public void testSigningBonusAmount() {
        initializeTest();
        Assert.assertEquals("USD 20.00", contract.getSigningBonusAmount().toString());
    }

    @Test
    public void testGetFeeAmount() {
        initializeTest();
        Assert.assertEquals("USD 10.00", contract.getFeeAmount().toString());
    }

    @Test
    public void testGetTotalAmount() {
        initializeTest();
        Assert.assertEquals("USD 200.00", contract.getTotalAmount().toString());
    }

    @Test
    public void testGetTotalAmountPlusFees(){
        initializeTest();
        Assert.assertEquals("USD 190.00", contract.getTotalAmountPlusFees().toString());
    }

    @Test
    public void testGetAdvanceAmount(){
        initializeTest();
        Assert.assertEquals("USD 19.00", contract.getAdvanceAmount().toString());
    }

    @Test
    public void testGetTotalAmountPlusFeesAndBonuses() {
        initializeTest();
        Assert.assertEquals("USD 210.00", contract.getTotalAmountPlusFeesAndBonuses().toString());
    }

    @Test
    public void testGetMonthlyPayout(){
        initializeTest();
        Assert.assertEquals("USD 17.10", contract.getMonthlyPayOut().toString());
    }

    private void initializeTest() {
        initCampaign();
        initContract();
        contract.calculateContract(mockCampaign);
    }

    private void initContract(){
        contract = spy(new Contract());

        contract.setOverheadComp(2); // Full overhead compensation
        contract.setMultiplier(1.3);

        contract.setLength(10);

        contract.setStraightSupport(100);
        contract.setTransportComp(100);

        contract.setSigningBonusPct(10);
        contract.setMRBCFee(true);
        contract.setAdvancePct(10);

        Mockito.when(contract.getPlanet()).thenReturn(new Planet());
    }

    private void initCampaign() {
        mockCampaign = Mockito.mock(Campaign.class);

        CampaignOptions mockCampaignOptions = Mockito.mock(CampaignOptions.class);
        Mockito.when(mockCampaignOptions.usePeacetimeCost()).thenReturn(true);
        Mockito.when(mockCampaignOptions.getUnitRatingMethod()).thenReturn(mekhq.campaign.rating.UnitRatingMethod.CAMPAIGN_OPS);

        JumpPath mockJumpPath = Mockito.mock(JumpPath.class);
        Mockito.when(mockJumpPath.getJumps()).thenReturn(2);

        Mockito.when(mockCampaign.calculateJumpPath(Mockito.nullable(Planet.class), Mockito.nullable(Planet.class))).thenReturn(mockJumpPath);
        Mockito.when(mockCampaign.calculateCostPerJump(Mockito.anyBoolean(), Mockito.anyBoolean())).thenReturn(jumpCost);
        Mockito.when(mockCampaign.getUnitRatingMod()).thenReturn(10);
        Mockito.when(mockCampaign.getOverheadExpenses()).thenReturn(overHeadExpenses);
        Mockito.when(mockCampaign.getContractBase()).thenReturn(contractBase);
        Mockito.when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Mockito.when(mockCampaign.getPeacetimeCost()).thenReturn(peacetimeCost);
        Mockito.when(mockCampaign.getCalendar()).thenReturn(new GregorianCalendar(3067, Calendar.JANUARY, 1));
    }
}
