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
import mekhq.campaign.universe.Planet;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.mockito.Mockito.spy;

/**
 * @author Miguel Azevedo
 * @version %Id%
 * @since 20/07/18 10:23 AM
 */
public class ContractTest {

    private Contract contract = spy(new Contract());
    private Campaign mockCampaign;

    @Test
    public void testGetBaseAmount() {
        initializeTest();
        Assert.assertEquals(130, contract.getBaseAmount());
    }

    @Test
    public void testGetOverheadAmount() {
        initializeTest();
        Assert.assertEquals(10, contract.getOverheadAmount());
    }

    @Test
    public void testGetSupportAmount() {
        initializeTest();
        Assert.assertEquals(10, contract.getSupportAmount());
    }

    @Test
    public void testGetTransportAmount() {
        initializeTest();
        Assert.assertEquals(20, contract.getTransportAmount());
    }

    @Test
    public void testGetTransitAmount() {
        initializeTest();
        Assert.assertEquals(30, contract.getTransitAmount());
    }

    @Test
    public void testSigningBonusAmount() {
        initializeTest();
        Assert.assertEquals(20, contract.getSigningBonusAmount());
    }

    @Test
    public void testGetFeeAmount() {
        initializeTest();
        Assert.assertEquals(10, contract.getFeeAmount());
    }

    @Test
    public void testGetTotalAmount() {
        initializeTest();
        Assert.assertEquals(200, contract.getTotalAmount());
    }

    @Test
    public void testGetTotalAmountPlusFees(){
        initializeTest();
        Assert.assertEquals(190, contract.getTotalAmountPlusFees());
    }

    @Test
    public void testGetAdvanceAmount(){
        initializeTest();
        Assert.assertEquals(19, contract.getAdvanceAmount());
    }

    @Test
    public void testGetTotalAmountPlusFeesAndBonuses() {
        initializeTest();
        Assert.assertEquals(210, contract.getTotalAmountPlusFeesAndBonuses());
    }

    @Test
    public void testGetMonthlyPayout(){
        initializeTest();
        Assert.assertEquals(((190-19) / 10), contract.getMonthlyPayOut());
    }

    private void initializeTest(){
        initCampaign();
        initContract();
        contract.calculateContract(mockCampaign);
    }

    private void initContract(){
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

    private void initCampaign(){
        mockCampaign = Mockito.mock(Campaign.class);

        CampaignOptions mockCampaignOptions = Mockito.mock(CampaignOptions.class);
        Mockito.when(mockCampaignOptions.usePeacetimeCost()).thenReturn(true);
        Mockito.when(mockCampaignOptions.getUnitRatingMethod()).thenReturn(mekhq.campaign.rating.UnitRatingMethod.CAMPAIGN_OPS);

        JumpPath mockJumpPath = Mockito.mock(JumpPath.class);
        Mockito.when(mockJumpPath.getJumps()).thenReturn(2);

        Mockito.when(mockCampaign.calculateJumpPath(Mockito.any(Planet.class), Mockito.any(Planet.class))).thenReturn(mockJumpPath);
        Mockito.when(mockCampaign.calculateCostPerJump(Mockito.anyBoolean(), Mockito.anyBoolean())).thenReturn((long) 5);
        Mockito.when(mockCampaign.getUnitRatingMod()).thenReturn(10);

        Mockito.when(mockCampaign.getOverheadExpenses()).thenReturn((long) 1);
        Mockito.when(mockCampaign.getContractBase()).thenReturn((long) 10);
        Mockito.when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        Mockito.when(mockCampaign.getPeacetimeCost()).thenReturn(1);
        Mockito.when(mockCampaign.getCalendar()).thenReturn(new GregorianCalendar(3067, Calendar.JANUARY, 1));
    }
}