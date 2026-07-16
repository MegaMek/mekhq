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
package mekhq.gui.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JTable;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.ITechWorkPanel;
import mekhq.gui.model.TaskTableModel;
import org.junit.jupiter.api.Test;

class TaskTableMouseAdapterTest {

    private TaskTableMouseAdapter newAdapter(CampaignGUI gui) {
        return new TaskTableMouseAdapter(gui, mock(JTable.class), mock(TaskTableModel.class),
              mock(ITechWorkPanel.class));
    }

    /**
     * Returns a mock {@link Unit} whose salvage flag is backed by the supplied {@link AtomicBoolean}, so that
     * {@code setSalvage}/{@code isSalvage} behave like the real (mutable) field. This lets tests observe the temporary
     * salvage toggle the strip helpers perform.
     */
    private Unit salvageTrackingUnit(AtomicBoolean salvage) {
        Unit unit = mock(Unit.class);
        when(unit.isSalvage()).thenAnswer(inv -> salvage.get());
        doAnswer(inv -> {
            salvage.set(inv.getArgument(0));
            return null;
        }).when(unit).setSalvage(anyBoolean());
        return unit;
    }

    // region isStripCandidate

    @Test
    void isStripCandidate_installedScrappableComponent_true() {
        Unit unit = mock(Unit.class);
        when(unit.isSalvage()).thenReturn(false);
        Part part = mock(Part.class);
        when(part.getUnit()).thenReturn(unit);
        when(part.canNeverScrap()).thenReturn(false);

        assertTrue(TaskTableMouseAdapter.isStripCandidate(part));
    }

    @Test
    void isStripCandidate_sparePartWithoutUnit_false() {
        Part part = mock(Part.class);
        when(part.getUnit()).thenReturn(null);

        assertFalse(TaskTableMouseAdapter.isStripCandidate(part));
    }

    @Test
    void isStripCandidate_unitInSalvageMode_false() {
        Unit unit = mock(Unit.class);
        when(unit.isSalvage()).thenReturn(true);
        Part part = mock(Part.class);
        when(part.getUnit()).thenReturn(unit);

        assertFalse(TaskTableMouseAdapter.isStripCandidate(part));
    }

    @Test
    void isStripCandidate_missingPart_false() {
        Unit unit = mock(Unit.class);
        when(unit.isSalvage()).thenReturn(false);
        MissingPart part = mock(MissingPart.class);
        when(part.getUnit()).thenReturn(unit);

        assertFalse(TaskTableMouseAdapter.isStripCandidate(part));
    }

    @Test
    void isStripCandidate_partThatCanNeverScrap_false() {
        Unit unit = mock(Unit.class);
        when(unit.isSalvage()).thenReturn(false);
        Part part = mock(Part.class);
        when(part.getUnit()).thenReturn(unit);
        when(part.canNeverScrap()).thenReturn(true);

        assertFalse(TaskTableMouseAdapter.isStripCandidate(part));
    }

    @Test
    void isStripCandidate_mekLocationShownEvenWhenNotScrappable_true() {
        // Mek locations are surfaced (so the menu can explain, disabled, why they can't be stripped) even if they
        // report that they can never be scrapped.
        Unit unit = mock(Unit.class);
        when(unit.isSalvage()).thenReturn(false);
        MekLocation part = mock(MekLocation.class);
        when(part.getUnit()).thenReturn(unit);
        when(part.canNeverScrap()).thenReturn(true);

        assertTrue(TaskTableMouseAdapter.isStripCandidate(part));
    }

    // endregion isStripCandidate

    // region getStripTarget / getStripTime

    @Test
    void getStripTarget_evaluatesWithSalvageForcedThenRestoresFlag() {
        AtomicBoolean salvage = new AtomicBoolean(false);
        Unit unit = salvageTrackingUnit(salvage);
        Part part = mock(Part.class);
        when(part.getUnit()).thenReturn(unit);
        when(part.getTech()).thenReturn(null);
        Person tech = mock(Person.class);

        Campaign campaign = mock(Campaign.class);
        CampaignGUI gui = mock(CampaignGUI.class);
        when(gui.getCampaign()).thenReturn(campaign);

        TargetRoll expected = new TargetRoll(5, "salvage");
        AtomicBoolean salvageDuringCall = new AtomicBoolean(false);
        when(campaign.getTargetFor(part, tech)).thenAnswer(inv -> {
            salvageDuringCall.set(salvage.get());
            return expected;
        });

        TargetRoll result = newAdapter(gui).getStripTarget(part, tech);

        assertSame(expected, result);
        assertTrue(salvageDuringCall.get(), "salvage must be forced on while the target number is computed");
        assertFalse(salvage.get(), "salvage flag must be restored afterwards");
        // The tech is set temporarily (for the Clan-tech flag) and cleared again.
        verify(part).setTech(tech);
        verify(part).setTech(null);
    }

    @Test
    void getStripTarget_sparePart_returnsNull() {
        Part part = mock(Part.class);
        when(part.getUnit()).thenReturn(null);

        assertSame(null, newAdapter(mock(CampaignGUI.class)).getStripTarget(part, mock(Person.class)));
    }

    @Test
    void getStripTime_evaluatesWithSalvageForcedThenRestoresFlag() {
        AtomicBoolean salvage = new AtomicBoolean(false);
        Unit unit = salvageTrackingUnit(salvage);
        Part part = mock(Part.class);
        when(part.getUnit()).thenReturn(unit);

        AtomicBoolean salvageDuringCall = new AtomicBoolean(false);
        when(part.getTimeLeft()).thenAnswer(inv -> {
            salvageDuringCall.set(salvage.get());
            return 180;
        });

        int minutes = newAdapter(mock(CampaignGUI.class)).getStripTime(part);

        assertEquals(180, minutes);
        assertTrue(salvageDuringCall.get(), "salvage must be forced on while the time is computed");
        assertFalse(salvage.get(), "salvage flag must be restored afterwards");
    }

    @Test
    void getStripTime_sparePart_returnsZero() {
        Part part = mock(Part.class);
        when(part.getUnit()).thenReturn(null);

        assertEquals(0, newAdapter(mock(CampaignGUI.class)).getStripTime(part));
    }

    // endregion getStripTarget / getStripTime

    // region stripPart

    @Test
    void stripPart_forcesSalvageDuringFixPartAndRestoresAfterwards() {
        AtomicBoolean salvage = new AtomicBoolean(false);
        Unit unit = salvageTrackingUnit(salvage);
        when(unit.isRepairable()).thenReturn(true);
        Part part = mock(Part.class);
        when(part.getUnit()).thenReturn(unit);
        Person tech = mock(Person.class);

        Campaign campaign = mock(Campaign.class);
        AtomicBoolean salvageDuringCall = new AtomicBoolean(false);
        when(campaign.fixPart(part, tech)).thenAnswer(inv -> {
            salvageDuringCall.set(salvage.get());
            return "done";
        });

        newAdapter(mock(CampaignGUI.class)).stripPart(campaign, part, tech);

        assertTrue(salvageDuringCall.get(), "salvage must be forced on while the work is resolved");
        assertFalse(salvage.get(), "salvage flag must be restored afterwards");
        verify(campaign).fixPart(part, tech);
        verify(campaign, never()).removeUnit(any());
        verify(unit).refreshPodSpace();
    }

    @Test
    void stripPart_removesUnitWhenNoLongerRepairableOrSalvageable() {
        AtomicBoolean salvage = new AtomicBoolean(false);
        Unit unit = salvageTrackingUnit(salvage);
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);
        when(unit.isRepairable()).thenReturn(false);
        when(unit.hasSalvageableParts()).thenReturn(false);
        Part part = mock(Part.class);
        when(part.getUnit()).thenReturn(unit);
        Person tech = mock(Person.class);

        Campaign campaign = mock(Campaign.class);
        when(campaign.fixPart(part, tech)).thenReturn("done");

        newAdapter(mock(CampaignGUI.class)).stripPart(campaign, part, tech);

        verify(campaign).removeUnit(unitId);
    }

    @Test
    void stripPart_sparePart_doesNothing() {
        Part part = mock(Part.class);
        when(part.getUnit()).thenReturn(null);
        Campaign campaign = mock(Campaign.class);

        newAdapter(mock(CampaignGUI.class)).stripPart(campaign, part, mock(Person.class));

        verify(campaign, never()).fixPart(any(), any());
    }

    // endregion stripPart
}
