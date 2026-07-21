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
package mekhq.gui.campaignOptions.contents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.getTestCampaign;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode;
import mekhq.gui.displayWrappers.FactionDisplay;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeneralPageTest {
    private static final LocalDate DATE_3025 = LocalDate.of(3025, 1, 1);
    private static final LocalDate DATE_2750 = LocalDate.of(2750, 1, 1);

    private Factions originalFactions;

    @BeforeEach
    void setUp() {
        originalFactions = Factions.getInstance();
    }

    @AfterEach
    void tearDown() {
        Factions.setInstance(originalFactions);
    }

    @Test
    void repeatedDateChangesReplaceFactionOptions() throws ReflectiveOperationException {
        Faction alwaysValid = createFaction("ALWAYS", date -> true);
        Faction validIn3025 = createFaction("YEAR_3025", date -> date.getYear() == 3025);
        Faction validIn2750 = createFaction("YEAR_2750", date -> date.getYear() == 2750);

        Campaign campaign = getTestCampaign();
        campaign.setLocalDate(LocalDate.of(3151, 1, 1));
        campaign.getPlayerForce().setFaction(alwaysValid);

        Factions factions = mock(Factions.class);
        when(factions.getChoosableFactions()).thenReturn(List.of(alwaysValid, validIn3025, validIn2750));
        when(factions.getFaction("MERC")).thenReturn(alwaysValid);
        Factions.setInstance(factions);

        GeneralPage page = new GeneralPage(campaign, null, CampaignOptionsDialogMode.STARTUP);
        page.createGeneralPage();
        MMComboBox<FactionDisplay> factionCombo = getFactionCombo(page, "comboFaction");
        MMComboBox<FactionDisplay> startingFactionCombo = getFactionCombo(page, "comboStartingLocationFaction");
        Object initialModel = factionCombo.getModel();
        Object initialStartingModel = startingFactionCombo.getModel();

        setDate(page, DATE_3025);
        assertNotSame(initialModel, factionCombo.getModel());
        assertNotSame(initialStartingModel, startingFactionCombo.getModel());
        assertEquals(Set.of("ALWAYS", "YEAR_3025"), getFactionCodes(factionCombo));
        assertEquals(Set.of("ALWAYS", "YEAR_3025"), getFactionCodes(startingFactionCombo));
        assertEquals("ALWAYS 3025 [ALWAYS]", factionCombo.getSelectedItem().toString());
        assertEquals("ALWAYS 3025 [ALWAYS]", startingFactionCombo.getSelectedItem().toString());

        factionCombo.setSelectedItem(getFactionDisplay(factionCombo, "YEAR_3025"));
        startingFactionCombo.setSelectedItem(getFactionDisplay(startingFactionCombo, "YEAR_3025"));
        Object model3025 = factionCombo.getModel();
        Object startingModel3025 = startingFactionCombo.getModel();

        setDate(page, DATE_2750);
        assertNotSame(model3025, factionCombo.getModel());
        assertNotSame(startingModel3025, startingFactionCombo.getModel());
        assertEquals(Set.of("ALWAYS", "YEAR_2750"), getFactionCodes(factionCombo));
        assertEquals(Set.of("ALWAYS", "YEAR_2750"), getFactionCodes(startingFactionCombo));
        assertEquals("ALWAYS 2750 [ALWAYS]", factionCombo.getSelectedItem().toString());
        assertEquals("ALWAYS 2750 [ALWAYS]", startingFactionCombo.getSelectedItem().toString());
    }

    private static Faction createFaction(String shortName, Predicate<LocalDate> validity) {
        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn(shortName);
        when(faction.getFullName(anyInt())).thenAnswer(invocation -> shortName + " " + invocation.getArgument(0));
        when(faction.validIn(any(LocalDate.class))).thenAnswer(invocation -> validity.test(invocation.getArgument(0)));
        return faction;
    }

    private static void setDate(GeneralPage page, LocalDate date) throws ReflectiveOperationException {
        Method setDate = GeneralPage.class.getDeclaredMethod("setDate", LocalDate.class);
        setDate.setAccessible(true);
        setDate.invoke(page, date);
    }

    private static Set<String> getFactionCodes(MMComboBox<FactionDisplay> comboFaction) {
        return IntStream.range(0, comboFaction.getItemCount())
                     .mapToObj(index -> comboFaction.getItemAt(index).getFaction().getShortName())
                     .collect(java.util.stream.Collectors.toSet());
    }

    private static FactionDisplay getFactionDisplay(MMComboBox<FactionDisplay> comboFaction, String factionCode) {
        return IntStream.range(0, comboFaction.getItemCount())
                     .mapToObj(comboFaction::getItemAt)
                     .filter(display -> display.getFaction().getShortName().equals(factionCode))
                     .findFirst()
                     .orElseThrow();
    }

    private static MMComboBox<FactionDisplay> getFactionCombo(GeneralPage page, String fieldName)
          throws ReflectiveOperationException {
        @SuppressWarnings("unchecked")
        Class<MMComboBox<FactionDisplay>> comboBoxClass = (Class<MMComboBox<FactionDisplay>>) (Class<?>) MMComboBox.class;
        return getField(page, fieldName, comboBoxClass);
    }

    private static <T> T getField(Object target, String fieldName, Class<T> fieldType)
          throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return fieldType.cast(field.get(target));
    }
}
