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

import java.io.PrintWriter;

import megamek.Version;
import org.w3c.dom.Node;

/**
 * Serializes and deserializes a single {@link CampaignOption} value to and from the campaign XML, keyed by the option's
 * {@link CampaignOption#xmlTag()}. This is the per-option strategy that lets {@link CampaignOptionsMarshaller} and
 * {@link CampaignOptionsUnmarshaller} be data-driven loops over {@link CampaignOption#values()} rather than parallel
 * hand-maintained tables.
 *
 * <p>Both methods operate directly on the {@link CampaignOptions} instance (not just the raw value) so that a codec
 * can reuse the option's existing getter/setter — preserving side effects (client-preference updates), value clamps,
 * and merge-into-defaults behavior exactly as the previous hand-written code did.</p>
 *
 * @param <T> the option's value type
 */
interface CampaignOptionCodec<T> {
    /** Builds a read/write codec from two lambdas. */
    static <T> CampaignOptionCodec<T> of(final Writer<T> writer, final Reader<T> reader) {
        return new CampaignOptionCodec<>() {
            @Override
            public void write(PrintWriter pw, int indent, CampaignOption<T> option, CampaignOptions options) {
                writer.write(pw, indent, option, options);
            }

            @Override
            public void read(Node node, String text, Version version, CampaignOption<T> option,
                  CampaignOptions options) {
                reader.read(node, text, version, option, options);
            }
        };
    }

    /** Builds a read-only codec (never written on save) — e.g. legacy presence markers. */
    static <T> CampaignOptionCodec<T> readOnly(final Reader<T> reader) {
        return new CampaignOptionCodec<>() {
            @Override
            public void write(PrintWriter pw, int indent, CampaignOption<T> option, CampaignOptions options) {
                // intentionally not written
            }

            @Override
            public void read(Node node, String text, Version version, CampaignOption<T> option,
                  CampaignOptions options) {
                reader.read(node, text, version, option, options);
            }

            @Override
            public boolean writesOutput() {
                return false;
            }
        };
    }

    /** Writes the option's current value as an XML tag. */
    void write(PrintWriter pw, int indent, CampaignOption<T> option, CampaignOptions options);

    /**
     * Reads the option's value from XML and applies it to {@code options}.
     *
     * @param node    the option's XML element (for nested/child parsing)
     * @param text    the element's trimmed text content (for scalar values)
     * @param version the save's version (only needed by a few codecs, e.g. {@code mrmsOptions})
     * @param option  the option being read
     * @param options the campaign options to apply the parsed value to
     */
    void read(Node node, String text, Version version, CampaignOption<T> option, CampaignOptions options);

    /** @return {@code true} if this option is emitted on save; {@code false} for read-only legacy markers. */
    default boolean writesOutput() {
        return true;
    }

    @FunctionalInterface
    interface Writer<T> {
        void write(PrintWriter pw, int indent, CampaignOption<T> option, CampaignOptions options);
    }

    @FunctionalInterface
    interface Reader<T> {
        void read(Node node, String text, Version version, CampaignOption<T> option, CampaignOptions options);
    }
}
