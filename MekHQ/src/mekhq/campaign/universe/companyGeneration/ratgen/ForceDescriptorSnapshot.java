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
package mekhq.campaign.universe.companyGeneration.ratgen;

import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.MissionRole;
import mekhq.utilities.MHQXMLUtility;

/**
 * User-facing inputs for {@code Ruleset.processRoot}, serialized alongside the rest of
 * {@link mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions} so a preset that targets the
 * ratgen pipeline survives a save/load round-trip.
 *
 * <p>Each field maps to a control on {@link megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView}.
 * Phase 1 carries the minimum the engine needs (faction, year, echelon, unit type, rating); subsequent
 * phases extend the snapshot to cover roles, weight class, flags, augmented / size modifier, transport
 * percentages. Default-on-missing semantics keep old presets parsing cleanly.</p>
 */
public final class ForceDescriptorSnapshot {

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
    private double cargo;

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

    public Integer getEchelon() {
        return echelon;
    }

    public void setEchelon(Integer echelon) {
        this.echelon = echelon;
    }

    public Integer getUnitType() {
        return unitType;
    }

    public void setUnitType(Integer unitType) {
        this.unitType = unitType;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public Integer getExperience() {
        return experience;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    public Integer getWeightClass() {
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

    public Integer getSizeMod() {
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

    public double getCargo() {
        return cargo;
    }

    public void setCargo(double cargo) {
        this.cargo = cargo;
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
        // ForceDescriptor.getJumpshipPct() doesn't exist on the engine type; the panel writes the value
        // back into its own text field but the engine has no setter. Leave snapshot's jumpshipPct alone.
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
        if (cargo != 0d) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, "cargo", cargo);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, indent, "forceDescriptorSnapshot");
    }

    /**
     * Parses a {@code <forceDescriptorSnapshot>} element. Missing children leave defaults intact, so this
     * is safe to call on old presets that lack the block.
     */
    public static ForceDescriptorSnapshot parseFromXML(Node element) {
        ForceDescriptorSnapshot snap = new ForceDescriptorSnapshot();
        if (element == null) {
            return snap;
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String tag = child.getNodeName();
            String text = child.getTextContent();
            if (text == null) {
                continue;
            }
            text = text.trim();
            try {
                switch (tag) {
                    case "faction" -> snap.faction = text;
                    case "year" -> snap.year = Integer.parseInt(text);
                    case "echelon" -> snap.echelon = Integer.parseInt(text);
                    case "unitType" -> snap.unitType = Integer.parseInt(text);
                    case "rating" -> snap.rating = text;
                    case "experience" -> snap.experience = Integer.parseInt(text);
                    case "weightClass" -> snap.weightClass = Integer.parseInt(text);
                    case "flags" -> {
                        for (String token : text.split(",")) {
                            if (!token.isBlank()) {
                                snap.flags.add(token.trim());
                            }
                        }
                    }
                    case "augmented" -> snap.augmented = Boolean.parseBoolean(text);
                    case "sizeMod" -> snap.sizeMod = Integer.parseInt(text);
                    case "roles" -> {
                        for (String token : text.split(",")) {
                            if (!token.isBlank()) {
                                snap.roles.add(token.trim());
                            }
                        }
                    }
                    case "dropshipPct" -> snap.dropshipPct = Double.parseDouble(text);
                    case "jumpshipPct" -> snap.jumpshipPct = Double.parseDouble(text);
                    case "cargo" -> snap.cargo = Double.parseDouble(text);
                    default -> {
                        // forward-compatible: unknown tags are ignored
                    }
                }
            } catch (NumberFormatException nfe) {
                // best-effort parse: leave the default
            }
        }
        return snap;
    }
}
