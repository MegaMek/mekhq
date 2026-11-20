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
package mekhq.campaign.campaignOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mekhq.campaign.campaignOptions.CampaignOptionsSystemChanges.SystemChange;
import mekhq.campaign.campaignOptions.CampaignOptionsSystemChanges.TrackedSystem;
import org.junit.jupiter.api.Test;

class CampaignOptionsSystemChangesTest {
    /** Builds a tracker with only the named record components switched on. */
    private static CampaignOptionsFreebieTracker tracker(String... enabled) throws Exception {
        Set<String> on = new HashSet<>(Arrays.asList(enabled));
        RecordComponent[] components = CampaignOptionsFreebieTracker.class.getRecordComponents();
        Class<?>[] types = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
        Object[] values = Arrays.stream(components).map(component -> on.contains(component.getName())).toArray();
        Constructor<CampaignOptionsFreebieTracker> constructor =
              CampaignOptionsFreebieTracker.class.getDeclaredConstructor(types);
        return constructor.newInstance(values);
    }

    @Test
    void identicalOptionsHaveNoChanges() throws Exception {
        CampaignOptionsFreebieTracker options = tracker("useStratCon", "useFatigue");
        assertTrue(CampaignOptionsSystemChanges.between(options, options).isEmpty());
    }

    @Test
    void turningOnASystemWithADialogIsAFollowUp() throws Exception {
        List<SystemChange> changes = CampaignOptionsSystemChanges.between(tracker(), tracker("useFatigue"));
        assertEquals(List.of(new SystemChange(TrackedSystem.FATIGUE, true, true)), changes);
    }

    @Test
    void turningOffASystemIsNotAFollowUp() throws Exception {
        List<SystemChange> changes = CampaignOptionsSystemChanges.between(tracker("useFatigue"), tracker());
        assertEquals(List.of(new SystemChange(TrackedSystem.FATIGUE, false, false)), changes);
    }

    @Test
    void factionStandingAsksBothWays() throws Exception {
        assertTrue(CampaignOptionsSystemChanges.between(tracker("trackFactionStanding"), tracker()).get(0)
                         .hasFollowUp());
        assertTrue(CampaignOptionsSystemChanges.between(tracker(), tracker("trackFactionStanding")).get(0)
                         .hasFollowUp());
    }

    @Test
    void diseasesNeverAsk() throws Exception {
        List<SystemChange> changes = CampaignOptionsSystemChanges.between(tracker("useAltAdvancedMedical"),
              tracker("useAltAdvancedMedical", "useDiseases"));
        assertEquals(List.of(new SystemChange(TrackedSystem.DISEASES, true, false)), changes);
    }

    @Test
    void advancedScoutingOnlyCountsWithStratCon() throws Exception {
        assertTrue(CampaignOptionsSystemChanges.between(tracker(), tracker("useAdvancedScouting")).isEmpty());
        List<SystemChange> changes = CampaignOptionsSystemChanges.between(tracker("useStratCon"),
              tracker("useStratCon", "useAdvancedScouting"));
        assertEquals(List.of(new SystemChange(TrackedSystem.ADVANCED_SCOUTING, true, true)), changes);
    }

    @Test
    void contractPayModelsOnlyCountWithLegacyContracts() throws Exception {
        assertTrue(CampaignOptionsSystemChanges.between(tracker(), tracker("useNormalizedContractPayModel"))
                         .isEmpty());
        List<SystemChange> changes = CampaignOptionsSystemChanges.between(tracker("useLegacyContractOptions"),
              tracker("useLegacyContractOptions", "useNormalizedContractPayModel"));
        assertEquals(List.of(new SystemChange(TrackedSystem.NORMALIZED_CONTRACT_PAY, true, true)), changes);
    }

    @Test
    void everyTrackerFieldIsCovered() throws Exception {
        for (RecordComponent component : CampaignOptionsFreebieTracker.class.getRecordComponents()) {
            CampaignOptionsFreebieTracker withOnlyThis = tracker(component.getName(), "useStratCon",
                  "useAltAdvancedMedical", "useLegacyContractOptions");
            CampaignOptionsFreebieTracker without = tracker("useStratCon", "useAltAdvancedMedical",
                  "useLegacyContractOptions");
            boolean isPrerequisite = Set.of("useStratCon", "useAltAdvancedMedical", "useLegacyContractOptions")
                                           .contains(component.getName());
            if (!isPrerequisite) {
                assertFalse(CampaignOptionsSystemChanges.between(without, withOnlyThis).isEmpty(),
                      component.getName() + " is not tracked");
            }
        }
    }
}
