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
package mekhq.campaign.parts;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * The repair site each refit class needs, by Campaign Operations p.211 (issue #3437): Class A and B anywhere, Class C
 * and D at a field workshop or better, Class E and F and refurbishment at a factory.
 */
class RefitFacilityTest {
    private static final String PARTS_BUNDLE = "mekhq.resources.Parts";
    private static final String DIALOG_BUNDLE = "mekhq.resources.ChooseRefitDialog";
    private static final String GUI_BUNDLE = "mekhq.resources.CampaignGUI";

    @ParameterizedTest
    @CsvSource({ Refit.NO_CHANGE + "," + Unit.SITE_IMPROVISED,
                 Refit.CLASS_OMNI + "," + Unit.SITE_IMPROVISED,
                 Refit.CLASS_A + "," + Unit.SITE_IMPROVISED,
                 Refit.CLASS_B + "," + Unit.SITE_IMPROVISED,
                 Refit.CLASS_C + "," + Unit.SITE_FIELD_WORKSHOP,
                 Refit.CLASS_D + "," + Unit.SITE_FIELD_WORKSHOP,
                 Refit.CLASS_E + "," + Unit.SITE_FACTORY_CONDITIONS,
                 Refit.CLASS_F + "," + Unit.SITE_FACTORY_CONDITIONS })
    void eachClassNeedsItsSite(int refitClass, int requiredSite) {
        assertEquals(requiredSite, RefitFacility.getRequiredSite(refitClass), Refit.getRefitClassName(refitClass));
    }

    @Test
    void aRefurbishmentNeedsFactoryConditions() {
        PartsScenario scenario = PartsScenario.create();
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        Refit refurbishment = new Refit(wolverine, wolverine.getEntity(), false, true, false);

        assertEquals(Unit.SITE_FACTORY_CONDITIONS, RefitFacility.getRequiredSite(refurbishment));
        assertFalse(RefitFacility.isSiteGoodEnough(refurbishment, Unit.SITE_FACILITY_MAINTENANCE));
        assertTrue(RefitFacility.isSiteGoodEnough(refurbishment, Unit.SITE_FACTORY_CONDITIONS));
        assertFalse(RefitFacility.isSiteGoodEnough(refurbishment, Unit.SITE_UNKNOWN), "An unknown site is too low");
        assertEquals(Unit.getSiteName(Unit.SITE_FACTORY_CONDITIONS),
              RefitFacility.describeRequiredSite(refurbishment));
    }

    @Test
    void theSiteTextReadsForThePlayer() {
        assertEquals("Field Workshop or better",
              getFormattedTextAt(PARTS_BUNDLE, "RefitFacility.orBetter", Unit.getSiteName(Unit.SITE_FIELD_WORKSHOP)));
        assertEquals("Any site", getTextAt(PARTS_BUNDLE, "RefitFacility.anySite"));

        String tooLow = getFormattedTextAt(DIALOG_BUNDLE, "refitTable.facility.tooltip.tooLow", "Factory Conditions",
              "Field Workshop");
        assertTrue(tooLow.startsWith("Needs Factory Conditions, but the unit is at Field Workshop."), tooLow);

        String question = getFormattedTextAt(GUI_BUNDLE, "refitSite.text", "Captain", "Wolverine WVR-6R",
              "WVR-6M", "Class C (Maintenance)", "Field Workshop or better", "Improvised");
        assertFalse(question.contains("{"), question);
        assertTrue(question.contains("needs Field Workshop or better") && question.contains("at Improvised"),
              question);
        for (String key : new String[] { "refitSite.rules", "refitSite.button.cancel" }) {
            assertFalse(getTextAt(GUI_BUNDLE, key).startsWith("!"), key + " is missing");
        }
        assertEquals("Move to Factory Conditions",
              getFormattedTextAt(GUI_BUNDLE, "refitSite.button.move", "Factory Conditions"));
    }
}
