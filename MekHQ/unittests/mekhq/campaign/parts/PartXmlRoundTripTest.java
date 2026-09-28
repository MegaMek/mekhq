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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import testUtilities.parts.PartSamples;
import testUtilities.parts.PartSamples.MissingSample;

/**
 * Saves every sampled part to XML the way the campaign save does, loads it back the way the campaign load does, and
 * checks the part that comes back is the part that went in.
 *
 * <p>The samples come from {@link PartSamples}: every distinct part installed on the canon fixture units, a few parts
 * no fixture carries, and the missing-part placeholder of each. A failure names the part class and part name.</p>
 */
class PartXmlRoundTripTest {
    static List<Arguments> realParts() {
        List<Arguments> arguments = new ArrayList<>();
        for (Part part : PartSamples.create().getRealParts()) {
            arguments.add(Arguments.of(Named.of(PartSamples.describe(part), part)));
        }
        return arguments;
    }

    static List<Arguments> missingParts() {
        List<Arguments> arguments = new ArrayList<>();
        for (MissingSample missingSample : PartSamples.create().getMissingParts()) {
            arguments.add(Arguments.of(Named.of(PartSamples.describe(missingSample.missingPart()), missingSample)));
        }
        return arguments;
    }

    @ParameterizedTest
    @MethodSource("realParts")
    void realPartSurvivesSaveAndLoad(Part part) throws Exception {
        PartQuality originalQuality = part.getQuality();
        int originalHits = part.getHits();
        boolean originalBrandNew = part.isBrandNew();
        try {
            // Move the part away from its defaults so a field the load forgets cannot pass by luck.
            part.setQuality(PartQuality.QUALITY_A);
            part.setHits(1);
            part.setBrandNew(!originalBrandNew);

            Part loaded = saveAndLoad(part, part.getCampaign());

            assertBaseFieldsSurvive(part, loaded);
            assertSame(part.getUnit(), loaded.getUnit(), "unit");
            if (!PartSamples.isNeverStocked(part)) {
                assertTrue(part.isSamePartType(loaded), "original is the same part type as the loaded part");
                assertTrue(loaded.isSamePartType(part), "loaded part is the same part type as the original");
            }
        } finally {
            part.setQuality(originalQuality);
            part.setHits(originalHits);
            part.setBrandNew(originalBrandNew);
        }
    }

    @ParameterizedTest
    @MethodSource("missingParts")
    void missingPartSurvivesSaveAndLoad(MissingSample missingSample) throws Exception {
        MissingPart missingPart = missingSample.missingPart();

        Part loaded = saveAndLoad(missingPart, missingSample.realPart().getCampaign());

        assertBaseFieldsSurvive(missingPart, loaded);
        String knownBreak = PartSamples.knownReplacementBreak(missingSample.realPart());
        assumeTrue(knownBreak == null, knownBreak);
        MissingPart loadedMissingPart = (MissingPart) loaded;
        assertTrue(loadedMissingPart.isAcceptableReplacement(missingSample.realPart().clone(),
                    missingSample.isReplacedOnlyByRefit()),
              "the loaded placeholder still accepts a spare " + missingSample.realPart().getName());
    }

    /**
     * Checks the fields every part saves in {@link Part#writeToXMLBegin} came back unchanged.
     */
    private static void assertBaseFieldsSurvive(Part original, Part loaded) {
        assertSame(original.getClass(), loaded.getClass(), "class");
        assertEquals(original.getName(), loaded.getName(), "name");
        assertEquals(original.getQuality(), loaded.getQuality(), "quality");
        assertEquals(original.getHits(), loaded.getHits(), "hits");
        assertEquals(original.getQuantity(), loaded.getQuantity(), "quantity");
        assertEquals(original.isOmniPodded(), loaded.isOmniPodded(), "omni-podded");
        assertEquals(original.getUnitTonnage(), loaded.getUnitTonnage(), "unit tonnage");
        assertEquals(original.isBrandNew(), loaded.isBrandNew(), "brand new");
        assertEquals(original.getDaysToArrival(), loaded.getDaysToArrival(), "days to arrival");
        assertEquals(original.getTonnage(), loaded.getTonnage(), "tonnage");
    }

    /**
     * Writes a part as the campaign save does and reads it back as the campaign load does: parse the XML, rebuild the
     * part, give it the campaign and resolve its references to units, parts and people.
     *
     * @param part     the part to save
     * @param campaign the campaign to load the part into; the part's unit must belong to it
     *
     * @return the loaded part, never {@code null}
     */
    static Part saveAndLoad(Part part, Campaign campaign) throws Exception {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            part.writeToXML(printWriter, 0);
        }

        Document document = MHQXMLUtility.newSafeDocumentBuilder()
                                  .parse(new ByteArrayInputStream(stringWriter.toString()
                                                                        .getBytes(StandardCharsets.UTF_8)));
        Part loaded = Part.generateInstanceFromXML(document.getDocumentElement(), new Version());
        assertNotNull(loaded, "the saved part did not load back: " + stringWriter);

        loaded.setCampaign(campaign);
        loaded.fixReferences(campaign);
        return loaded;
    }
}
