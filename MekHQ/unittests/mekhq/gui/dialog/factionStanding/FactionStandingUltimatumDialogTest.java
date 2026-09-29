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
package mekhq.gui.dialog.factionStanding;

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandingUltimatumData;
import mekhq.gui.baseComponents.hud.HudCard.Tag;
import mekhq.gui.baseComponents.hud.HudStyle;
import mekhq.gui.dialog.factionStanding.FactionStandingUltimatumDialog.UltimatumSide;
import mekhq.gui.dialog.factionStanding.UltimatumSidePickerDialog.ChoiceKind;
import mekhq.gui.dialog.factionStanding.UltimatumSidePickerDialog.PickerChoice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the decision logic in {@link FactionStandingUltimatumDialog} and {@link UltimatumSidePickerDialog}.
 *
 * <p>Both dialogs are modal, so only their pure helpers are covered here.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class FactionStandingUltimatumDialogTest {
    private static final int GAME_YEAR = 2784;

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static Faction buildFaction(String code, boolean isAggregate) {
        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn(code);
        when(faction.getFullName(GAME_YEAR)).thenReturn(code + " Full Name");
        when(faction.isAggregate()).thenReturn(isAggregate);
        return faction;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static UltimatumSide buildSide(String id, Faction faction) {
        Person agitator = mock(Person.class);
        when(agitator.getGivenName()).thenReturn(id + " Leader");
        when(agitator.getPrimaryRole()).thenReturn(PersonnelRole.NOBLE);
        return new UltimatumSide(id, agitator, faction);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static Campaign buildCampaign(List<Person> activePersonnel) {
        Campaign campaign = mock(Campaign.class);
        PlayerForce playerForce = mock(PlayerForce.class);
        ForceHumanResources humanResources = mock(ForceHumanResources.class);

        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getHumanResources()).thenReturn(humanResources);
        when(humanResources.getActivePersonnel(false, false)).thenReturn(activePersonnel);

        return campaign;
    }

    // ----- picker cards -----

    @Test
    @DisplayName("The picker offers each side in order, then Mercenary and Pirate")
    void testBuildChoicesOrder() {
        Faction terranHegemony = buildFaction("TH", false);
        List<UltimatumSide> sides = List.of(buildSide("SLIE", buildFaction("SLIE", false)),
              buildSide("CS", buildFaction("CS", false)),
              buildSide("TH", terranHegemony));

        List<PickerChoice> choices = FactionStandingUltimatumDialog.buildChoices(sides, "EXODUS", terranHegemony,
              false, true, "TH", GAME_YEAR, key -> key);

        assertEquals(5, choices.size());
        List<ChoiceKind> kinds = new ArrayList<>();
        for (PickerChoice choice : choices) {
            kinds.add(choice.kind());
        }
        assertEquals(List.of(ChoiceKind.SIDE, ChoiceKind.SIDE, ChoiceKind.SIDE, ChoiceKind.MERCENARY,
              ChoiceKind.PIRATE), kinds);
        assertEquals("CS", choices.get(1).code());
        assertEquals(1, choices.get(1).sideIndex());
        assertEquals("CS Full Name", choices.get(1).title());
        assertEquals("FactionStandingUltimatumDialog.EXODUS.side.CS.pitch", choices.get(1).pitch());
        assertEquals(-1, choices.get(3).sideIndex());
    }

    @Test
    @DisplayName("Rogue cards use the ultimatum's own appeal, or the shared default when it isn't written")
    void testRoguePitchFallsBackToSharedDefault() {
        Faction federatedCommonwealth = buildFaction("FC", false);
        List<UltimatumSide> sides = List.of(buildSide("LA", buildFaction("LA", false)));

        List<PickerChoice> shipped = FactionStandingUltimatumDialog.buildChoices(sides, "FED_COM_CIVIL_WAR",
              federatedCommonwealth, false, false, "LA", GAME_YEAR, key -> key);
        List<PickerChoice> unwritten = FactionStandingUltimatumDialog.buildChoices(sides, "UNWRITTEN_TEST",
              federatedCommonwealth, false, false, "LA", GAME_YEAR, key -> key);

        assertEquals("FactionStandingUltimatumDialog.FED_COM_CIVIL_WAR.rogue.mercenary.pitch",
              shipped.get(1).pitch());
        assertEquals("FactionStandingUltimatumDialog.FED_COM_CIVIL_WAR.rogue.pirate.pitch", shipped.get(2).pitch());
        assertEquals(getTextAt("mekhq.resources.FactionStandingUltimatumDialog",
              "FactionStandingUltimatumDialog.picker.mercenary.pitch"), unwritten.get(1).pitch());
    }

    @Test
    @DisplayName("Only the dissenter's preferred card is marked as keeping them")
    void testBuildChoicesMarksDissenterPreference() {
        Faction draconisCombine = buildFaction("DC", false);
        List<UltimatumSide> sides = List.of(buildSide("FRR", buildFaction("FRR", false)));

        List<PickerChoice> choices = FactionStandingUltimatumDialog.buildChoices(sides, "RASALHAGUE_INDEPENDENCE",
              draconisCombine, false, false, FactionStandingUltimatumData.ROGUE_PREFERENCE, GAME_YEAR, key -> key);

        assertEquals(3, choices.size());
        assertFalse(choices.get(0).dissenterStays());
        assertTrue(choices.get(1).dissenterStays());
        assertTrue(choices.get(2).dissenterStays());
    }

    @Test
    @DisplayName("Standing tags only appear when Faction Standing is tracked")
    void testBuildChoicesStandingTags() {
        Faction federatedCommonwealth = buildFaction("FC", false);
        List<UltimatumSide> sides = List.of(buildSide("LA", buildFaction("LA", false)),
              buildSide("FS", buildFaction("FS", false)));

        List<PickerChoice> untracked = FactionStandingUltimatumDialog.buildChoices(sides, "FED_COM_CIVIL_WAR",
              federatedCommonwealth, false, false, "FS", GAME_YEAR, key -> key);
        List<PickerChoice> tracked = FactionStandingUltimatumDialog.buildChoices(sides, "FED_COM_CIVIL_WAR",
              federatedCommonwealth, false, true, "FS", GAME_YEAR, key -> key);

        // Untracked: one row, "Joins LA". Tracked: that row, then a standing row with "up LA" and "down FC, FS".
        assertEquals(1, untracked.get(0).tagRows().size());
        assertEquals(1, untracked.get(0).tagRows().get(0).size());
        assertEquals(2, tracked.get(0).tagRows().size());
        assertEquals(2, tracked.get(0).tagRows().get(1).size());
    }

    @Test
    @DisplayName("A violent rogue card puts its placement and defection on one row and standing on another")
    void testRogueCardTagRows() {
        Faction arano = buildFaction("ARC", false);
        List<UltimatumSide> sides = List.of(buildSide("ARD", buildFaction("ARD", false)), buildSide("ARC", arano));

        List<PickerChoice> choices = FactionStandingUltimatumDialog.buildChoices(sides, "ESPINOSA_COUP", arano,
              true, true, "ARC", GAME_YEAR, key -> key);
        PickerChoice mercenary = choices.get(2);

        assertEquals(ChoiceKind.MERCENARY, mercenary.kind());
        assertEquals(2, mercenary.tagRows().size());
        assertEquals(2, mercenary.tagRows().get(0).size());
        assertEquals(1, mercenary.tagRows().get(1).size());
    }

    @Test
    @DisplayName("Empty tag rows are left out")
    void testToTagRowsDropsEmptyRows() {
        Tag tag = new Tag("Joins LA", HudStyle.TEXT_MUTED);

        assertEquals(List.of(List.of(tag)), FactionStandingUltimatumDialog.toTagRows(List.of(tag), List.of()));
        assertEquals(List.of(List.of(tag)), FactionStandingUltimatumDialog.toTagRows(List.of(), List.of(tag)));
        assertTrue(FactionStandingUltimatumDialog.toTagRows(List.of(), List.of()).isEmpty());
    }

    // ----- standing losses -----

    @Test
    @DisplayName("Choosing a side lowers standing with the faction left and every other side")
    void testStandingLossCodesForASide() {
        Faction federatedCommonwealth = buildFaction("FC", false);
        Faction lyranAlliance = buildFaction("LA", false);
        Faction federatedSuns = buildFaction("FS", false);
        List<UltimatumSide> sides = List.of(buildSide("LA", lyranAlliance), buildSide("FS", federatedSuns));

        assertEquals(List.of("FC", "FS"),
              FactionStandingUltimatumDialog.getStandingLossCodes(sides, lyranAlliance, federatedCommonwealth));
        assertEquals(List.of(federatedSuns),
              FactionStandingUltimatumDialog.getOtherSideFactions(sides, lyranAlliance, federatedCommonwealth));
    }

    @Test
    @DisplayName("Staying with the current faction doesn't lower standing with it")
    void testStandingLossCodesWhenStaying() {
        Faction comStar = buildFaction("CS", false);
        Faction wordOfBlake = buildFaction("WOB", false);
        List<UltimatumSide> sides = List.of(buildSide("WOB", wordOfBlake), buildSide("CS", comStar));

        assertEquals(List.of("WOB"), FactionStandingUltimatumDialog.getStandingLossCodes(sides, comStar, comStar));
        assertEquals(List.of(wordOfBlake),
              FactionStandingUltimatumDialog.getOtherSideFactions(sides, comStar, comStar));
    }

    @Test
    @DisplayName("Going rogue lowers standing with every side, and aggregate factions are skipped")
    void testStandingLossCodesWhenGoingRogue() {
        Faction terranHegemony = buildFaction("TH", false);
        Faction starLeagueInExile = buildFaction("SLIE", false);
        Faction aggregate = buildFaction("IS", true);
        List<UltimatumSide> sides = List.of(buildSide("SLIE", starLeagueInExile), buildSide("TH", terranHegemony),
              buildSide("IS", aggregate));

        assertEquals(List.of("TH", "SLIE"),
              FactionStandingUltimatumDialog.getStandingLossCodes(sides, null, terranHegemony));
    }

    // ----- dissenter -----

    @Test
    @DisplayName("The dissenter stays only for their preferred side")
    void testDissenterStaysForPreferredSide() {
        assertTrue(FactionStandingUltimatumDialog.dissenterStays("FS", ChoiceKind.SIDE, "FS"));
        assertFalse(FactionStandingUltimatumDialog.dissenterStays("FS", ChoiceKind.SIDE, "LA"));
        assertFalse(FactionStandingUltimatumDialog.dissenterStays("FS", ChoiceKind.MERCENARY, null));
    }

    @Test
    @DisplayName("A dissenter who prefers going rogue stays for either rogue choice")
    void testDissenterStaysWhenPreferringRogue() {
        String rogue = FactionStandingUltimatumData.ROGUE_PREFERENCE;

        assertTrue(FactionStandingUltimatumDialog.dissenterStays(rogue, ChoiceKind.MERCENARY, null));
        assertTrue(FactionStandingUltimatumDialog.dissenterStays(rogue, ChoiceKind.PIRATE, null));
        assertFalse(FactionStandingUltimatumDialog.dissenterStays(rogue, ChoiceKind.SIDE, "FRR"));
    }

    @Test
    @DisplayName("A missing dissenter preference means the dissenter always leaves")
    void testDissenterWithoutPreferenceLeaves() {
        assertFalse(FactionStandingUltimatumDialog.dissenterStays(null, ChoiceKind.SIDE, "FS"));
        assertFalse(FactionStandingUltimatumDialog.dissenterStays(null, ChoiceKind.PIRATE, null));
    }

    // ----- keys and helpers -----

    @Test
    @DisplayName("Every scene, every side's pitch and news, and both rogue appeals are required text")
    void testGetRequiredTextKeys() {
        assertEquals(List.of("FactionStandingUltimatumDialog.EXODUS.initialOffer",
                    "FactionStandingUltimatumDialog.EXODUS.for",
                    "FactionStandingUltimatumDialog.EXODUS.against",
                    "FactionStandingUltimatumDialog.EXODUS.side.SLIE.pitch",
                    "FactionStandingUltimatumDialog.EXODUS.side.SLIE.news",
                    "FactionStandingUltimatumDialog.EXODUS.side.CS.pitch",
                    "FactionStandingUltimatumDialog.EXODUS.side.CS.news",
                    "FactionStandingUltimatumDialog.EXODUS.rogue.mercenary.pitch",
                    "FactionStandingUltimatumDialog.EXODUS.rogue.pirate.pitch"),
              FactionStandingUltimatumDialog.getRequiredTextKeys("EXODUS", List.of("SLIE", "CS")));
    }

    @Test
    @DisplayName("Leaving the current faction in a violent transition is a defection")
    void testIsDefection() {
        Faction currentFaction = mock(Faction.class);
        Faction otherFaction = mock(Faction.class);

        assertTrue(FactionStandingUltimatumDialog.isDefection(true, otherFaction, currentFaction));
        assertFalse(FactionStandingUltimatumDialog.isDefection(true, currentFaction, currentFaction));
        assertFalse(FactionStandingUltimatumDialog.isDefection(false, otherFaction, currentFaction));
    }

    @Test
    @DisplayName("The carousel wraps around at both ends")
    void testWrapIndex() {
        assertEquals(4, UltimatumSidePickerDialog.wrapIndex(-1, 5));
        assertEquals(0, UltimatumSidePickerDialog.wrapIndex(5, 5));
        assertEquals(2, UltimatumSidePickerDialog.wrapIndex(2, 5));
        assertEquals(0, UltimatumSidePickerDialog.wrapIndex(1, 1));
    }

    @Test
    @DisplayName("getThirdInCommand picks the highest-ranked person outside the top two")
    void testGetThirdInCommandPicksHighestRemaining() {
        Person commander = mock(Person.class);
        Person secondInCommand = mock(Person.class);
        Person junior = mock(Person.class);
        Person senior = mock(Person.class);
        Campaign campaign = buildCampaign(List.of(commander, junior, secondInCommand, senior));

        when(senior.outRanksUsingSkillTiebreaker(campaign.getCampaignOptions(), false,
              campaign.getLocalDate(), junior)).thenReturn(true);

        assertSame(senior, FactionStandingUltimatumDialog.getThirdInCommand(campaign, commander, secondInCommand));
    }

    @Test
    @DisplayName("getThirdInCommand returns null when only the command staff remain")
    void testGetThirdInCommandNullWhenNoCandidates() {
        Person commander = mock(Person.class);
        Person secondInCommand = mock(Person.class);
        Campaign campaign = buildCampaign(List.of(commander, secondInCommand));

        assertNull(FactionStandingUltimatumDialog.getThirdInCommand(campaign, commander, secondInCommand));
    }

    @Test
    @DisplayName("Sides sharing a faction only lower standing with it once")
    void testStandingLossCodesDeduplicateFactions() {
        Faction federatedCommonwealth = buildFaction("FC", false);
        Faction lyranAlliance = buildFaction("LA", false);
        Faction federatedSuns = buildFaction("FS", false);
        List<UltimatumSide> sides = List.of(buildSide("LA", lyranAlliance), buildSide("FS_NORTH", federatedSuns),
              buildSide("FS_SOUTH", federatedSuns));

        assertEquals(List.of("FC", "FS"),
              FactionStandingUltimatumDialog.getStandingLossCodes(sides, lyranAlliance, federatedCommonwealth));
    }

    @Test
    @DisplayName("Going rogue from an aggregate faction doesn't list that faction as a standing loss")
    void testStandingLossCodesFromAggregateFaction() {
        Faction mercenaries = buildFaction("MERC", true);
        Faction lyranAlliance = buildFaction("LA", false);
        List<UltimatumSide> sides = List.of(buildSide("LA", lyranAlliance));

        assertEquals(List.of("LA"), FactionStandingUltimatumDialog.getStandingLossCodes(sides, null, mercenaries));
    }

    @Test
    @DisplayName("The dissenter preference is matched exactly, so a differently cased ID means they leave")
    void testDissenterPreferenceIsCaseSensitive() {
        assertFalse(FactionStandingUltimatumDialog.dissenterStays("fs", ChoiceKind.SIDE, "FS"));
        assertFalse(FactionStandingUltimatumDialog.dissenterStays("rogue", ChoiceKind.MERCENARY, null));
    }

    @Test
    @DisplayName("A one-sided ultimatum shows exactly three cards and the dissenter stays only on their choice")
    void testSingleSidedUltimatumCards() {
        Faction draconisCombine = buildFaction("DC", false);
        List<UltimatumSide> sides = List.of(buildSide("FRR", buildFaction("FRR", false)));

        List<PickerChoice> choices = FactionStandingUltimatumDialog.buildChoices(sides, "RASALHAGUE_INDEPENDENCE",
              draconisCombine, true, true, "FRR", GAME_YEAR, key -> key);

        assertEquals(3, choices.size());
        assertTrue(choices.get(0).dissenterStays());
        assertFalse(choices.get(1).dissenterStays());
        assertFalse(choices.get(2).dissenterStays());
        // Violent and leaving DC: the side card is a defection, and its standing row lowers DC
        assertEquals(2, choices.get(0).tagRows().get(0).size());
        assertEquals(List.of("DC"),
              FactionStandingUltimatumDialog.getStandingLossCodes(sides, sides.get(0).faction(), draconisCombine));
    }
}
