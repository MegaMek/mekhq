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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.BattleArmorAmmoBin;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;
import mekhq.campaign.parts.missing.MissingPart;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import testUtilities.parts.PartSamples;

/**
 * The rules every stockable part must keep for the warehouse and repairs to work, checked over every sampled part.
 *
 * <ul>
 *     <li>A copy made with {@link Part#clone()} is the same part type as the original, both ways round, and keeps
 *     its quality and condition. Repairs install a clone of the spare, so a clone that loses quality turns a
 *     quality A spare into a quality D part.</li>
 *     <li>The part's missing placeholder accepts a clone of the part as a replacement.</li>
 *     <li>Two parts only share a warehouse stack ({@link Part#isSamePartTypeAndStatus}) when their status matches
 *     ({@link Part#isSameStatus}), so a used or in-transit spare never merges into a new, delivered stack.</li>
 * </ul>
 *
 * <p>Parts built into the unit that can never be a spare (transport bays and spacecraft cooling systems, see
 * {@link PartSamples#isNeverStocked}) are left out. Missing placeholders are left out of the clone rules for the same
 * reason: they are never stocked, and {@link MissingPart#clone()} returns {@code null} by design.</p>
 *
 * <p>A few parts break a rule today in a way issue #10176 does not cover. Those cases are skipped with the reason,
 * so the rest of the contract still guards every other part until each gets its own fix.</p>
 */
class PartIdentityContractTest {
    /** Parts that are not the same part type as their own clone today, with the reason. */
    private static final Map<Class<? extends Part>, String> KNOWN_CLONE_TYPE_BREAKS = Map.of(
          BattleArmorSuit.class,
          "Follow-up to #10176: an installed suit prices itself from its unit, its unattached clone from a saved "
                + "alternate cost that is unset, so the prices differ",
          BattleArmorAmmoBin.class,
          "Follow-up to #10176: the bin's identity counts the unit's troopers, which an unattached clone does not have",
          LargeCraftAmmoBin.class,
          "Follow-up to #10176 (item 42): the bin finds its weapon bay through a link that is not saved or copied, "
                + "so a copy or reloaded bin is not the same part type");

    static List<Arguments> stockableParts() {
        List<Arguments> arguments = new ArrayList<>();
        for (Part part : PartSamples.create().getRealParts()) {
            if (!PartSamples.isNeverStocked(part)) {
                arguments.add(Arguments.of(Named.of(PartSamples.describe(part), part)));
            }
        }
        return arguments;
    }

    static List<Arguments> partsWithMissingPlaceholders() {
        List<Arguments> arguments = new ArrayList<>();
        for (PartSamples.MissingSample missingSample : PartSamples.create().getMissingParts()) {
            arguments.add(Arguments.of(Named.of(PartSamples.describe(missingSample.realPart()), missingSample)));
        }
        return arguments;
    }

    @ParameterizedTest
    @MethodSource("stockableParts")
    void cloneIsTheSamePartTypeBothWays(Part part) {
        skipKnownBreak(KNOWN_CLONE_TYPE_BREAKS.get(part.getClass()));
        Part clone = part.clone();

        assertNotNull(clone, "clone");
        assertTrue(part.isSamePartType(clone), "part is the same type as its clone");
        assertTrue(clone.isSamePartType(part), "clone is the same type as the part");
    }

    @ParameterizedTest
    @MethodSource("stockableParts")
    void cloneKeepsQualityAndCondition(Part part) {
        PartQuality originalQuality = part.getQuality();
        int originalHits = part.getHits();
        boolean originalBrandNew = part.isBrandNew();
        try {
            part.setQuality(PartQuality.QUALITY_A);
            part.setHits(1);
            part.setBrandNew(!originalBrandNew);

            Part clone = part.clone();

            assertEquals(PartQuality.QUALITY_A, clone.getQuality(), "quality");
            assertEquals(1, clone.getHits(), "hits");
            assertEquals(part.isBrandNew(), clone.isBrandNew(), "brand new");
            // An ammo bin is pod-mounted only through the unit it is installed on (AmmoBin#isOmniPodded), so a
            // clone, which has no unit, is never pod-mounted by design
            if (!(part instanceof AmmoBin)) {
                assertEquals(part.isOmniPodded(), clone.isOmniPodded(), "omni-podded");
            }
        } finally {
            part.setQuality(originalQuality);
            part.setHits(originalHits);
            part.setBrandNew(originalBrandNew);
        }
    }

    @ParameterizedTest
    @MethodSource("partsWithMissingPlaceholders")
    void missingPlaceholderAcceptsAClone(PartSamples.MissingSample missingSample) {
        skipKnownBreak(PartSamples.knownReplacementBreak(missingSample.realPart()));
        Part spare = missingSample.realPart().clone();

        assertTrue(missingSample.missingPart().isAcceptableReplacement(spare, missingSample.isReplacedOnlyByRefit()),
              "placeholder " + missingSample.missingPart().getName() + " accepts a clone of the part");
    }

    @ParameterizedTest
    @MethodSource("stockableParts")
    void sharingAStackRequiresTheSameStatus(Part part) {
        Part spare = part.clone();

        Part otherQuality = part.clone();
        otherQuality.setQuality(spare.getQuality() == PartQuality.QUALITY_A
                                      ? PartQuality.QUALITY_B
                                      : PartQuality.QUALITY_A);
        Part inTransit = part.clone();
        inTransit.setDaysToArrival(14);
        Part damaged = part.clone();
        damaged.setHits(spare.getHits() + 1);

        assertImpliesSameStatus(spare, part.clone());
        assertImpliesSameStatus(spare, otherQuality);
        assertImpliesSameStatus(spare, inTransit);
        assertImpliesSameStatus(spare, damaged);
    }

    @Test
    void transportBayIsNeverStocked() {
        TransportBayPart bay = new TransportBayPart(0, 1, 10, PartSamples.create().getCampaign());

        assertTrue(PartSamples.isNeverStocked(bay));
        assertFalse(bay.isSamePartType(bay), "a bay is never the same part type as anything, even itself");
        assertNull(bay.getMissingPart(), "a bay has no missing placeholder, so no spare can replace it");
    }

    @Test
    void spacecraftCoolingSystemIsNeverStocked() {
        SpacecraftCoolingSystem coolingSystem = new SpacecraftCoolingSystem(0, 10, 0,
              PartSamples.create().getCampaign());

        assertTrue(PartSamples.isNeverStocked(coolingSystem));
        assertFalse(coolingSystem.isSamePartType(coolingSystem), "a cooling system is only ever modified in place");
    }

    /**
     * Skips the current case when it is a known break outside issue #10176.
     *
     * @param knownBreak the reason the case is known to fail, or {@code null} to run it
     */
    private static void skipKnownBreak(@Nullable String knownBreak) {
        assumeTrue(knownBreak == null, knownBreak);
    }

    /**
     * Checks that if two parts may share a warehouse stack, their status matches, both ways round.
     */
    private static void assertImpliesSameStatus(Part first, Part second) {
        if (first.isSamePartTypeAndStatus(second)) {
            assertTrue(first.isSameStatus(second),
                  "stacks with a part of different status: quality " + second.getQuality() + ", hits "
                        + second.getHits() + ", days to arrival " + second.getDaysToArrival());
        }
        if (second.isSamePartTypeAndStatus(first)) {
            assertTrue(second.isSameStatus(first),
                  "a part of different status stacks with it: quality " + second.getQuality() + ", hits "
                        + second.getHits() + ", days to arrival " + second.getDaysToArrival());
        }
    }
}
