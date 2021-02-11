/*
 * Copyright (C) 2020 MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.mission;

import mekhq.campaign.Campaign;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AtBContractTest {

    @Test
    public void atbContractRestoreDoesNothingWithoutParent() {
        Campaign mockCampaign = mock(Campaign.class);

        int childId = 2;
        AtBContract child = new AtBContract();
        child.setId(childId);
        child.setParentContract(null);

        // Restore the AtBContract
        child.restore(mockCampaign);

        verify(mockCampaign, times(0)).getMission(anyInt());

        // Ensure the parent is not set
        assertNull(child.getParentContract());
    }
    
    @Test
    public void atbContractRestoresRefs() {
        Campaign mockCampaign = mock(Campaign.class);

        int parentId = 1;
        AtBContract parent = mock(AtBContract.class);
        when(parent.getId()).thenReturn(parentId);
        doReturn(parent).when(mockCampaign).getMission(eq(parentId));

        int childId = 2;
        AtBContract child = new AtBContract();
        child.setId(childId);
        child.setParentContract(new AtBContract.AtBContractRef(parentId));
        doReturn(child).when(mockCampaign).getMission(eq(childId));

        int otherId = 3;
        AtBContract other = mock(AtBContract.class);
        when(other.getId()).thenReturn(otherId);
        doReturn(other).when(mockCampaign).getMission(eq(otherId));

        // Restore the AtBContract
        child.restore(mockCampaign);

        // Ensure the parent is set properly
        assertEquals(parent, child.getParentContract());
    }

    @Test
    public void atbContractRestoreClearsParentIfMissing() {
        Campaign mockCampaign = mock(Campaign.class);

        int parentId = 1;
        doReturn(null).when(mockCampaign).getMission(eq(parentId));

        int childId = 2;
        AtBContract child = new AtBContract();
        child.setId(childId);
        child.setParentContract(new AtBContract.AtBContractRef(parentId));
        doReturn(child).when(mockCampaign).getMission(eq(childId));

        int otherId = 3;
        AtBContract other = mock(AtBContract.class);
        when(other.getId()).thenReturn(otherId);
        doReturn(other).when(mockCampaign).getMission(eq(otherId));

        // Restore the AtBContract
        child.restore(mockCampaign);

        // Ensure the parent is null because it is missing
        assertNull(child.getParentContract());
    }

    @Test
    public void atbContractRestoreClearsParentIfWrongType() {
        Campaign mockCampaign = mock(Campaign.class);

        int parentId = 1;
        Contract parent = mock(Contract.class);
        when(parent.getId()).thenReturn(parentId);
        doReturn(parent).when(mockCampaign).getMission(eq(parentId));

        int childId = 2;
        AtBContract child = new AtBContract();
        child.setId(childId);
        child.setParentContract(new AtBContract.AtBContractRef(parentId));
        doReturn(child).when(mockCampaign).getMission(eq(childId));

        int otherId = 3;
        AtBContract other = mock(AtBContract.class);
        when(other.getId()).thenReturn(otherId);
        doReturn(other).when(mockCampaign).getMission(eq(otherId));

        // Restore the AtBContract
        child.restore(mockCampaign);

        // Ensure the parent is null because it is not the correct type of contract
        assertNull(child.getParentContract());
    }
}
