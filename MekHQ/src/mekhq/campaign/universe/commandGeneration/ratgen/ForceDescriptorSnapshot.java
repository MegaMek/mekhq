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
package mekhq.campaign.universe.commandGeneration.ratgen;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.MissionRole;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * User-facing inputs for {@code Ruleset.processRoot}, serialized alongside the rest of
 * {@link mekhq.campaign.universe.commandGeneration.CommandGenerationOptions} so a preset that targets the
 * ratgen pipeline survives a save/load round-trip.
 *
 * <p>Each field maps to a control on {@link megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView}.
 * Phase 1 carries the minimum the engine needs (faction, year, echelon, unit type, rating); subsequent
 * phases extend the snapshot to cover roles, weight class, flags, augmented / size modifier, transport
 * percentages. Default-on-missing semantics keep old presets parsing cleanly.</p>
 */
public final class ForceDescriptorSnapshot {

    private static final MMLogger LOGGER = MMLogger.create(ForceDescriptorSnapshot.class);

    private String faction;
    private int year;
    /** ratgen echelon int from {@code data/forcegenerator/faction_rules/constants.txt}. */
    private Integer echelon;
    /** {@link megamek.common.units.UnitType} integer, or {@code null} to let the ruleset decide. */
    private Integer unitType;
    /** "A" / "B" / "C" / "D" / "F" or {@code null} for ruleset default. */
    private String rating;
    /** experience level integer (Green/Regular/Veteran/Elite), or {@code null} to randomize. */
    private Integer experience;
    /** weight class integer, or {@code null} for ruleset default. */
    private Integer weightClass;
    /** flag tokens (e.g. "c3", "omni", "novacews"). */
    private final Set<String> flags = new LinkedHashSet<>();
    private boolean augmented;
    private Integer sizeMod;
    /** mission-role tokens (e.g. "RECON", "FIRE_SUPPORT"). */
    private final Set<String> roles = new LinkedHashSet<>();
    private double dropshipPct;
    private double jumpshipPct;
    /** Percentage of the command's cargo requirement to provision hauling for; 100 covers it all. */
    private double cargoPct = 100.0;

    public ForceDescriptorSnapshot() {
        // Defaults are deliberately conservative; the dialog populates real values before generate.
        this.faction = "IS";
        this.year = 3025;
    }

    // ---- Accessors ---------------------------------------------------------

    public String getFaction() {
        return faction;
    }

    public void setFaction(String faction) {
        this.faction = faction;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    /**
     * @return the ratgen echelon, or {@code null} to leave the ruleset's own choice
     */
    public @Nullable Integer getEchelon() {
        return echelon;
    }

    public void setEchelon(Integer echelon) {
        this.echelon = echelon;
    }

    /**
     * @return the {@link megamek.common.units.UnitType} integer, or {@code null} to leave the ruleset's own choice
     */
    public @Nullable Integer getUnitType() {
        return unitType;
    }

    public void setUnitType(Integer unitType) {
        this.unitType = unitType;
    }

    /**
     * @return the equipment rating, or {@code null} to leave the ruleset's own choice
     */
    public @Nullable String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    /**
     * @return the experience level, or {@code null} to have it randomized
     */
    public @Nullable Integer getExperience() {
        return experience;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    /**
     * @return the weight class, or {@code null} to leave the ruleset's own choice
     */
    public @Nullable Integer getWeightClass() {
        return weightClass;
    }

    public void setWeightClass(Integer weightClass) {
        this.weightClass = weightClass;
    }

    public Set<String> getFlags() {
        return flags;
    }

    public boolean isAugmented() {
        return augmented;
    }

    public void setAugmented(boolean augmented) {
        this.augmented = augmented;
    }

    /**
     * @return the size modifier, or {@code null} to leave the ruleset's own choice
     */
    public @Nullable Integer getSizeMod() {
        return sizeMod;
    }

    public void setSizeMod(Integer sizeMod) {
        this.sizeMod = sizeMod;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public double getDropshipPct() {
        return dropshipPct;
    }

    public void setDropshipPct(double dropshipPct) {
        this.dropshipPct = dropshipPct;
    }

    public double getJumpshipPct() {
        return jumpshipPct;
    }

    public void setJumpshipPct(double jumpshipPct) {
        this.jumpshipPct = jumpshipPct;
    }

    public double getCargoPct() {
        return cargoPct;
    }

    public void setCargoPct(double cargoPct) {
        this.cargoPct = cargoPct;
    }

    // ---- Population from a built ForceDescriptor ---------------------------

    /**
     * Copies every snapshot-relevant field out of a {@link ForceDescriptor} produced by
     * {@link megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView#buildForceDescriptor()}. This is
     * how the embedded options panel hands its user-selected values to the persistent snapshot when the
     * dialog's OK button is clicked.
     *
     * <p>Values that the panel left {@code null} (i.e. "ruleset default") overwrite the snapshot field with
     * {@code null}; the engine treats {@code null} on the {@code ForceDescriptor} as "let the ruleset
     * decide" and we mirror that semantics rather than retaining stale snapshot state from a previous run.</p>
     */
    public void populateFromForceDescriptor(ForceDescriptor fd) {
        if (fd == null) {
            return;
        }
        if (fd.getFaction() != null && !fd.getFaction().isBlank()) {
            this.faction = fd.getFaction();
        }
        if (fd.getYear() != null) {
            this.year = fd.getYear();
        }
        this.echelon = fd.getEchelon();
        this.unitType = fd.getUnitType();
        this.rating = fd.getRating();
        this.experience = fd.getExperience();
        this.weightClass = fd.getWeightClass();
        this.augmented = fd.isAugmented();
        // getSizeMod() returns int; the snapshot stores Integer so the engine's "no preference" is null.
        // Treat 0 as "not specified" here, matching how the panel leaves the value when the user doesn't touch it.
        int rawSizeMod = fd.getSizeMod();
        this.sizeMod = rawSizeMod == 0 ? null : rawSizeMod;
        this.dropshipPct = fd.getDropshipPct();
        this.cargoPct = fd.getCargoPct();
        this.jumpshipPct = fd.getJumpshipPct();
        this.flags.clear();
        if (fd.getFlags() != null) {
            this.flags.addAll(fd.getFlags());
        }
        this.roles.clear();
        if (fd.getRoles() != null) {
            for (MissionRole role : fd.getRoles()) {
                this.roles.add(role.name());
            }
        }
    }

    // ---- XML round-trip ----------------------------------------------------

    /**
     * Writes a {@code <forceDescriptorSnapshot>} element with all set fields as child tags.
     */
    public void writeToXML(PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent, "forceDescriptorSnapshot");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "faction", faction);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "year", year);
        if (echelon != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "echelon", echelon);
        }
        if (unitType != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "unitType", unitType);
        }
        if (rating != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "rating", rating);
        }
        if (experience != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "experience", experience);
        }
        if (weightClass != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "weightClass", weightClass);
        }
        if (!flags.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "flags", String.join(",", flags));
        }
        if (augmented) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "augmented", true);
        }
        if (sizeMod != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "sizeMod", sizeMod);
        }
        if (!roles.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "roles", String.join(",", roles));
        }
        if (dropshipPct != 0d) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "dropshipPct", dropshipPct);
        }
        if (jumpshipPct != 0d) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "jumpshipPct", jumpshipPct);
        }
        if (cargoPct != 100d) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "cargoPct", cargoPct);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, indent, "forceDescriptorSnapshot");
    }

    /**
     * Parses a {@code <forceDescriptorSnapshot>} element.
     *
     * <p>Anything missing keeps its default, so a preset written before a setting existed still loads,
     * and so does one written by a later version that carries settings this one has no field for.</p>
     *
     * @param element the element to read, or {@code null} for an all-defaults snapshot
     *
     * @return the snapshot the element describes
     */
    public static ForceDescriptorSnapshot parseFromXML(@Nullable Node element) {
        ForceDescriptorSnapshot snapshot = new ForceDescriptorSnapshot();
        if (element == null) {
            return snapshot;
        }
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String tag = child.getNodeName();
            String text = child.getTextContent();
            if (text == null) {
                continue;
            }
            text = text.trim();
            SnapshotElementReader reader = READERS.get(tag);
            if (reader == null) {
                LOGGER.debug("[CommandGen] preset carries an element this version does not know, '{}';"
                            + " ignored", tag);
                continue;
            }
            reader.read(snapshot, text);
        }
        return snapshot;
    }

    /** Reads one element's text into the snapshot it belongs to. */
    @FunctionalInterface
    private interface SnapshotElementReader {
        void read(ForceDescriptorSnapshot snapshot, String text);
    }

    /**
     * One reader per element name, built once.
     *
     * <p>Looking the element up by name and running its reader keeps each setting's parsing in one place,
     * and lets an element that fails to parse be reported and skipped without costing the rest of the
     * preset. This is the shape the contract loader in {@code mekhq.campaign.mission.contract.io} uses.</p>
     */
    private static final Map<String, SnapshotElementReader> READERS = createReaderMap();

    private static Map<String, SnapshotElementReader> createReaderMap() {
        Map<String, SnapshotElementReader> readers = new HashMap<>();
        readers.put("faction", (snapshot, text) -> snapshot.faction = text);
        readers.put("year", (snapshot, text) -> snapshot.year = parseInteger("year", text, snapshot.year));
        readers.put("echelon", (snapshot, text) -> snapshot.echelon = parseOptionalInteger("echelon", text, snapshot.echelon));
        readers.put("unitType", (snapshot, text) -> snapshot.unitType = parseOptionalInteger("unitType", text, snapshot.unitType));
        readers.put("rating", (snapshot, text) -> snapshot.rating = text);
        readers.put("experience", (snapshot, text) -> snapshot.experience = parseOptionalInteger("experience", text,
              snapshot.experience));
        readers.put("weightClass", (snapshot, text) -> snapshot.weightClass = parseOptionalInteger("weightClass", text,
              snapshot.weightClass));
        readers.put("flags", (snapshot, text) -> addTokens(text, snapshot.flags));
        readers.put("augmented", (snapshot, text) -> snapshot.augmented = Boolean.parseBoolean(text));
        readers.put("sizeMod", (snapshot, text) -> snapshot.sizeMod = parseOptionalInteger("sizeMod", text, snapshot.sizeMod));
        readers.put("roles", (snapshot, text) -> addTokens(text, snapshot.roles));
        readers.put("dropshipPct", (snapshot, text) -> snapshot.dropshipPct = parseDouble("dropshipPct", text,
              snapshot.dropshipPct));
        readers.put("jumpshipPct", (snapshot, text) -> snapshot.jumpshipPct = parseDouble("jumpshipPct", text,
              snapshot.jumpshipPct));
        readers.put("cargoPct", (snapshot, text) -> snapshot.cargoPct = parseDouble("cargoPct", text, snapshot.cargoPct));
        return Map.copyOf(readers);
    }

    /**
     * Reads a whole number into a setting that may be unset, keeping the current value where the text is
     * not a number.
     *
     * <p>Each parse catches its own failure, so one bad setting costs only that setting and is named in
     * the log.</p>
     *
     * @param elementName  the element being read, named in the log on failure
     * @param text         the element's text
     * @param currentValue the value to keep on failure, or {@code null} where the setting is unset
     *
     * @return the parsed number, or {@code currentValue} where the text is not a number
     */
    private static @Nullable Integer parseOptionalInteger(String elementName, String text, @Nullable Integer currentValue) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            logUnreadableNumber(elementName, text);
            return currentValue;
        }
    }

    /**
     * Reads a whole number into a setting that always holds one, keeping the current value where the text
     * is not a number.
     *
     * @param elementName  the element being read, named in the log on failure
     * @param text         the element's text
     * @param currentValue the value to keep on failure
     *
     * @return the parsed number, or {@code currentValue} where the text is not a number
     */
    private static int parseInteger(String elementName, String text, int currentValue) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            logUnreadableNumber(elementName, text);
            return currentValue;
        }
    }

    /**
     * Reads a decimal number, keeping the current value where the text is not one.
     *
     * @param elementName  the element being read, named in the log on failure
     * @param text         the element's text
     * @param currentValue the value to keep on failure
     *
     * @return the parsed number, or {@code currentValue} where the text is not a number
     */
    private static double parseDouble(String elementName, String text, double currentValue) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException exception) {
            logUnreadableNumber(elementName, text);
            return currentValue;
        }
    }

    private static void logUnreadableNumber(String elementName, String text) {
        LOGGER.warn("[CommandGen] preset element '{}' held '{}', which is not a number; the setting keeps its"
              + " default", elementName, text);
    }

    /**
     * Splits a comma-separated element into its tokens and adds them to the given set.
     *
     * @param text        the element's text
     * @param destination the set to add the tokens to
     */
    private static void addTokens(String text, Set<String> destination) {
        for (String token : text.split(",")) {
            if (!token.isBlank()) {
                destination.add(token.trim());
            }
        }
    }

    /**
     * Two ForceDescriptorSnapshots are equal when every setting matches, which is how the dialog tells whether the
     * settings have moved since the model was last generated.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ForceDescriptorSnapshot other)) {
            return false;
        }
        return Objects.equals(faction, other.faction)
              && (year == other.year)
              && Objects.equals(echelon, other.echelon)
              && Objects.equals(unitType, other.unitType)
              && Objects.equals(rating, other.rating)
              && Objects.equals(experience, other.experience)
              && Objects.equals(weightClass, other.weightClass)
              && Objects.equals(flags, other.flags)
              && (augmented == other.augmented)
              && Objects.equals(sizeMod, other.sizeMod)
              && Objects.equals(roles, other.roles)
              && (Double.compare(dropshipPct, other.dropshipPct) == 0)
              && (Double.compare(jumpshipPct, other.jumpshipPct) == 0)
              && (Double.compare(cargoPct, other.cargoPct) == 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(faction,
              year,
              echelon,
              unitType,
              rating,
              experience,
              weightClass,
              flags,
              augmented,
              sizeMod,
              roles,
              dropshipPct,
              jumpshipPct,
              cargoPct);
    }
}
