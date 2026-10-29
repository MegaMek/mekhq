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
package mekhq.campaign.mission.contract.io;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.universe.Systems;

/**
 * Shares contract offers between campaigns as JSON files, so a game master can hand the offers they have made to their
 * players, or players can pass offers around between themselves.
 *
 * <p>Exporting writes each chosen offer as a {@link ContractOfferJson}, along with the market board it was listed on.
 * Importing reads them back as brand-new offers for the importing campaign: each gets a fresh id (so importing a file
 * twice adds two copies rather than one overwriting the other), and its NPCs are rebuilt in the importing campaign.
 * Offers that cannot be placed - a contract type this build lacks, or a target system the importing campaign's map
 * does not have - are skipped and reported rather than failing the whole file.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ContractExchange {
    private static final MMLogger LOGGER = MMLogger.create(ContractExchange.class);

    /** The file format's version. Bump it when the format changes in a way older builds cannot read. */
    public static final int FORMAT_VERSION = 1;

    private static final ObjectMapper MAPPER = buildMapper();

    private ContractExchange() {}

    /**
     * An offer chosen for export, and the market board it is listed on.
     *
     * @param contract   the offer
     * @param searchType the board it is listed on
     */
    public record ExportEntry(AbstractContract contract, ContractSearchType searchType) {}

    /**
     * An imported offer, and the board the exporting campaign listed it on.
     *
     * @param contract   the rebuilt offer
     * @param searchType the board it was listed on, or {@code null} if the file did not say
     */
    public record ImportedOffer(AbstractContract contract, @Nullable ContractSearchType searchType) {}

    /** Why an offer in a file could not be imported. */
    public enum SkipReason {
        /** The offer is of a contract type this build does not have. */
        UNKNOWN_CONTRACT_TYPE,
        /** The offer's target system is not on the importing campaign's map. */
        UNKNOWN_TARGET_SYSTEM,
        /** The offer could not be rebuilt at all. */
        UNREADABLE
    }

    /**
     * An offer that could not be imported.
     *
     * @param name   the offer's name, as written in the file
     * @param reason why it was skipped
     */
    public record SkippedOffer(String name, SkipReason reason) {}

    /**
     * The outcome of an import.
     *
     * @param imported the offers that were rebuilt
     * @param skipped  the offers that could not be
     */
    public record ImportResult(List<ImportedOffer> imported, List<SkippedOffer> skipped) {}

    /**
     * The on-disk shape of an exported file.
     *
     * @param formatVersion the {@link #FORMAT_VERSION} the file was written with
     * @param mekhqVersion  the MekHQ version that wrote the file, for diagnostics
     * @param offers        the exported offers
     */
    record ContractOfferFile(int formatVersion, String mekhqVersion, List<ContractOfferJson> offers) {}

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Tolerate fields and enum values a newer build added rather than failing the whole file.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        return mapper;
    }

    // region Export

    /**
     * Writes the given offers to a JSON file.
     *
     * @param outputFile the destination file
     * @param entries    the offers to export, with their boards
     *
     * @throws IOException if the file cannot be written
     * @author Illiani
     * @since 0.51.01
     */
    public static void exportOffers(File outputFile, Collection<ExportEntry> entries) throws IOException {
        Files.writeString(outputFile.toPath(), toJson(entries), StandardCharsets.UTF_8);
    }

    /**
     * Serializes the given offers to the exported file's JSON.
     *
     * @param entries the offers to export, with their boards
     *
     * @return the JSON text
     *
     * @throws IOException if the offers cannot be serialized
     * @author Illiani
     * @since 0.51.01
     */
    public static String toJson(Collection<ExportEntry> entries) throws IOException {
        List<ContractOfferJson> offers = new ArrayList<>();
        for (ExportEntry entry : entries) {
            offers.add(ContractOfferJson.from(entry.contract(), entry.searchType()));
        }
        ContractOfferFile file = new ContractOfferFile(FORMAT_VERSION, MHQConstants.VERSION.toString(), offers);
        return MAPPER.writeValueAsString(file) + '\n';
    }

    // endregion Export

    // region Import

    /**
     * Reads the offers in a JSON file as new offers for the given campaign.
     *
     * @param inputFile the file to read
     * @param campaign  the campaign importing the offers
     *
     * @return the imported offers, and any that had to be skipped
     *
     * @throws IOException if the file cannot be read or is not an exported contract file
     * @author Illiani
     * @since 0.51.01
     */
    public static ImportResult importOffers(File inputFile, Campaign campaign) throws IOException {
        return fromJson(Files.readString(inputFile.toPath(), StandardCharsets.UTF_8), campaign);
    }

    /**
     * Reads exported JSON as new offers for the given campaign.
     *
     * @param json     the exported file's JSON text
     * @param campaign the campaign importing the offers
     *
     * @return the imported offers, and any that had to be skipped
     *
     * @throws IOException if the text is not an exported contract file
     * @author Illiani
     * @since 0.51.01
     */
    public static ImportResult fromJson(String json, Campaign campaign) throws IOException {
        ContractOfferFile file = MAPPER.readValue(json, ContractOfferFile.class);
        if ((file == null) || (file.offers() == null)) {
            throw new IOException("Not an exported contract offers file.");
        }
        if (file.formatVersion() > FORMAT_VERSION) {
            LOGGER.warn("Contract offers file format {} is newer than this build's ({}); reading what it can.",
                  file.formatVersion(), FORMAT_VERSION);
        }

        List<ImportedOffer> imported = new ArrayList<>();
        List<SkippedOffer> skipped = new ArrayList<>();
        for (ContractOfferJson offer : file.offers()) {
            if (offer == null) {
                continue;
            }
            String name = offer.name() == null ? "" : offer.name();

            if ((offer.targetSystemId() != null)
                      && (Systems.getInstance().getSystemById(offer.targetSystemId()) == null)) {
                skipped.add(new SkippedOffer(name, SkipReason.UNKNOWN_TARGET_SYSTEM));
                continue;
            }

            AbstractContract contract;
            try {
                contract = offer.toContract(campaign);
            } catch (Exception ex) {
                LOGGER.error(ex, "Unable to import contract offer '{}'", name);
                skipped.add(new SkippedOffer(name, SkipReason.UNREADABLE));
                continue;
            }
            if (contract == null) {
                skipped.add(new SkippedOffer(name, SkipReason.UNKNOWN_CONTRACT_TYPE));
                continue;
            }
            imported.add(new ImportedOffer(contract, offer.searchType()));
        }
        return new ImportResult(imported, skipped);
    }

    // endregion Import
}
