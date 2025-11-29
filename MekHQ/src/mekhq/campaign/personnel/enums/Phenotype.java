/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.enums;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.List;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.skills.Attributes;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

/**
 * The {@link Phenotype} Enum represents various phenotypes a Clan character can have. Each {@link Phenotype} is
 * associated with a name, short name, grouping name and a tooltip text. Each {@link Phenotype} can be classified as
 * either {@code external} or {@code internal}.
 */
public enum Phenotype {
    // region Enum Declarations
    /**
     * Individual external phenotypes.
     */
    MEKWARRIOR(true, true, 0, 0, 1, 1, new Attributes(8, 8, 9, 9, 8, 8, 9, 8, 0), new ArrayList<>()),
    ELEMENTAL(true, true, 2, 1, -1, 0, new Attributes(9, 9, 8, 7, 8, 9, 8, 8, 0), List.of("atow_toughness")),
    AEROSPACE(true, true, -1, -1, +2, +2, new Attributes(7, 7, 9, 9, 9, 8, 8, 8, 0), List.of("flaw_glass_jaw")),
    // ATOW doesn't cover a vehicle phenotype, but as the linked attributes for vehicle skills are also reflexes and
    // dexterity, I copied the MekWarrior phenotype
    VEHICLE(true, true, 0, 0, 1, 1, new Attributes(8, 8, 9, 9, 8, 8, 9, 8, 0), new ArrayList<>()),
    // According to my research, ProtoMek pilots are normally just Aerospace washouts, so I'm assuming they'd have the
    // same phenotype modifiers.
    PROTOMEK(true, true, -1, -1, +2, +2, new Attributes(7, 7, 9, 9, 9, 8, 8, 8, 0), List.of("flaw_glass_jaw")),
    // Copying the MekWarrior phenotype, same reasons as above.
    NAVAL(true, true, 0, 0, 1, 1, new Attributes(8, 8, 9, 9, 8, 8, 9, 8, 0), new ArrayList<>()),

    /**
     * Individual internal phenotypes.
     */
    // Internal Phenotypes
    NONE(false, false, 0, 0, 0, 0, new Attributes(8, 8, 8, 8, 8, 8, 9, 8, 0), new ArrayList<>()),
    GENERAL(false, false, 0, 0, 0, 0, new Attributes(8, 8, 8, 8, 8, 8, 9, 8, 0), new ArrayList<>());
    // endregion Enum Declarations

    // region Variable Declarations
    private static final MMLogger logger = MMLogger.create(Phenotype.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Phenotype";

    private final String shortName;
    private final String label;
    private final String tooltip;
    private final boolean isTrueborn;
    private final boolean external;
    private final int strength;
    private final int body;
    private final int reflexes;
    private final int dexterity;
    private final Attributes attributeCaps;
    private final List<String> bonusTraits;
    // endregion Variable Declarations

    // region Constructors
    Phenotype() {
        this(false, false, 0, 0, 0, 0, new Attributes(8, 8, 8, 8, 8, 8, 9, 8, 0), new ArrayList<>());
    }

    Phenotype(final boolean isTrueborn, final boolean external, final int strength, final int body, final int reflexes,
          final int dexterity, final Attributes attributeCaps, final List<String> bonusTraits) {
        this.shortName = generateShortName();
        this.label = generateLabel();
        this.tooltip = generateTooltip();
        this.isTrueborn = isTrueborn;
        this.external = external;
        this.strength = strength;
        this.body = body;
        this.reflexes = reflexes;
        this.dexterity = dexterity;
        this.attributeCaps = attributeCaps;
        this.bonusTraits = bonusTraits;
    }
    // endregion Constructors

    // region Getters

    public String getShortName() {
        return shortName;
    }

    public String getLabel() {
        return label;
    }

    public String getTooltip() {
        return tooltip;
    }

    /**
     * Retrieves the cap (maximum allowable score) for a specified {@link SkillAttribute}.
     *
     * @param attribute The {@link SkillAttribute} for which the cap is requested.
     *
     * @return The cap value for the specified skill attribute.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public int getAttributeCap(SkillAttribute attribute) {
        return attributeCaps.getBaseAttributeScore(attribute);
    }

    /**
     * Checks whether the phenotype is a Clan Trueborn phenotype.
     *
     * @return a boolean, {@code true} if the phenotype is Trueborn, otherwise {@code false}.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public boolean isTrueborn() {
        return external;
    }

    /**
     * Checks whether the phenotype is marked as external.
     *
     * @return a boolean, {@code true} if the phenotype is external, otherwise {@code false}.
     */
    public boolean isExternal() {
        return external;
    }

    /**
     * Retrieves the modifier value for a given skill attribute.
     *
     * <p>The method determines the corresponding modifier based on the
     * specified {@link SkillAttribute}. The value is fetched from the associated field depending on the attribute:</p>
     * <ul>
     *     <li>For {@code STRENGTH}, the {@code strength} field is returned.</li>
     *     <li>For {@code BODY}, the {@code body} field is returned.</li>
     *     <li>For {@code REFLEXES}, the {@code reflexes} field is returned.</li>
     *     <li>For {@code DEXTERITY}, the {@code dexterity} field is returned.</li>
     *     <li>For {@code NONE}, {@code INTELLIGENCE}, {@code WILLPOWER}, and
     *     {@code CHARISMA}, a default value of {@code 0} is returned.</li>
     * </ul>
     *
     * @param attribute The skill attribute for which the modifier is requested.
     *
     * @return The modifier value associated with the provided attribute. If the attribute is {@code NONE},
     *       {@code INTELLIGENCE}, {@code WILLPOWER}, or {@code CHARISMA}, the method returns {@code 0}.
     *
     * @throws NullPointerException If the {@code attribute} is {@code null}.
     */
    public int getAttributeModifier(final SkillAttribute attribute) {
        return switch (attribute) {
            case STRENGTH -> strength;
            case BODY -> body;
            case REFLEXES -> reflexes;
            case DEXTERITY -> dexterity;
            case NONE, INTELLIGENCE, WILLPOWER, CHARISMA, EDGE -> 0;
        };
    }

    /**
     * Retrieves a list of bonus traits assigned to this phenotype.
     *
     * @return A list of bonus traits as strings.
     */
    public List<String> getBonusTraits() {
        return bonusTraits;
    }

    /**
     * Retrieves the short name of this phenotype based on its Clan status.
     *
     * <p>The method determines the appropriate key by appending the Clan status
     * ("trueborn" or "freeborn") to the base key "shortName." This key is then used to fetch the formatted text from
     * the resource bundle.</p>
     *
     * @return A formatted short name string corresponding to the born type (e.g., "shortName.trueborn" or
     *       "shortName.freeborn") from the resource bundle.
     *
     * @author Illiani
     * @since 0.50.05
     */
    private String generateShortName() {
        String key = "shortName." + (isTrueborn ? "trueborn" : "freeborn");

        return getTextAt(RESOURCE_BUNDLE, key);
    }

    /**
     * Retrieves the label for this phenotype.
     *
     * <p>The method constructs the key by appending ".label" to the name of
     * the current instance (as returned by {@code name()}) and uses it to fetch the formatted text from the resource
     * bundle.</p>
     *
     * @return A formatted label string corresponding to the key "{@code Component Name}.label" from the resource
     *       bundle.
     *
     * @author Illiani
     * @since 0.50.05
     */
    private String generateLabel() {
        return getTextAt(RESOURCE_BUNDLE, name() + ".label");
    }

    /**
     * Retrieves the tooltip text for this phenotype.
     *
     * <p>The method constructs the key by appending ".tooltip" to the name of
     * the current instance (as returned by {@code name()}) and uses it to fetch the formatted text from the resource
     * bundle.</p>
     *
     * @return A formatted tooltip string corresponding to the key "{@code Component Name}.tooltip" from the resource
     *       bundle.
     *
     * @author Illiani
     * @since 0.50.05
     */
    private String generateTooltip() {
        return getTextAt(RESOURCE_BUNDLE, name() + ".tooltip");
    }
    // endregion Getters

    // region Boolean Comparison Methods

    /**
     * Checks if the phenotype is MekWarrior.
     *
     * @return {@code true} if the phenotype is a Mek Warrior, {@code false} otherwise.
     */
    public boolean isMekWarrior() {
        return this == MEKWARRIOR;
    }

    /**
     * Checks if the phenotype is Elemental.
     *
     * @return {@code true} if the phenotype is an Elemental, {@code false} otherwise.
     */
    public boolean isElemental() {
        return this == ELEMENTAL;
    }

    /**
     * Checks if the phenotype is Aerospace.
     *
     * @return {@code true} if the phenotype is Aerospace, {@code false} otherwise.
     */
    public boolean isAerospace() {
        return this == AEROSPACE;
    }

    /**
     * Checks if the phenotype is Vehicle.
     *
     * @return {@code true} if the phenotype is a Vehicle, {@code false} otherwise.
     */
    public boolean isVehicle() {
        return this == VEHICLE;
    }

    /**
     * Checks if the phenotype is ProtoMek.
     *
     * @return {@code true} if the phenotype is a ProtoMek, {@code false} otherwise.
     */
    public boolean isProtoMek() {
        return this == PROTOMEK;
    }

    /**
     * Checks if the phenotype is Naval.
     *
     * @return {@code true} if the phenotype is Naval, {@code false} otherwise.
     */
    public boolean isNaval() {
        return this == NAVAL;
    }

    /**
     * Checks if the phenotype is None.
     *
     * @return {@code true} if the phenotype is None, {@code false} otherwise.
     */
    public boolean isNone() {
        return this == NONE;
    }

    /**
     * Checks if the phenotype is General.
     *
     * @return {@code true} if the phenotype is General, {@code false} otherwise.
     */
    public boolean isGeneral() {
        return this == GENERAL;
    }
    // endregion Boolean Comparison Methods

    /**
     * Retrieves a list of external phenotypes.
     *
     * @return a {@link List} of {@link Phenotype} objects where {@code isExternal()} returns {@code true}.
     */
    public static List<Phenotype> getExternalPhenotypes() {
        List<Phenotype> externalPhenotypes = new ArrayList<>();

        for (Phenotype phenotype : values()) {
            if (phenotype.isExternal()) {
                externalPhenotypes.add(phenotype);
            }
        }

        return externalPhenotypes;
    }

    // region File I/O
    // CHECKSTYLE IGNORE ForbiddenWords FOR 32 LINES

    /**
     * Converts a string representation to a corresponding {@link Phenotype} value.
     *
     * <p>This method attempts to parse the input string and return the appropriate
     * {@link Phenotype} instance using the following approaches:</p>
     * <ol>
     *     <li><b>By name:</b> Converts the input to uppercase, replaces spaces with underscores,
     *         and tries to match it using {@link Phenotype#valueOf(String)}.</li>
     *     <li><b>By label (short name):</b> Matches the input case-insensitively against
     *         the short name of each {@link Phenotype}.</li>
     *     <li><b>By ordinal:</b> Parses the input as an integer and retrieves the
     *         phenotype corresponding to the specified ordinal.</li>
     * </ol>
     *
     * <p>If the input is {@code null} or none of the parsing approaches are successful,
     * an error is logged, and the method defaults to returning {@link Phenotype#NONE}.</p>
     *
     * @param text The string representation to parse. Supported input formats include:
     *             <ul>
     *                 <li>The full name of the phenotype (case-insensitive, spaces allowed).</li>
     *                 <li>The short name (label) of the phenotype (case-insensitive).</li>
     *                 <li>An ordinal value corresponding to the phenotype.</li>
     *             </ul>
     *
     * @return The corresponding {@link Phenotype} instance for the given input, or {@link Phenotype#NONE} if the input
     *       he input is invalid or {@code null}.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static Phenotype fromString(String text) {
        if (text == null || text.isBlank()) {
            logger.error("Unable to parse text into a Phenotype. Returning NONE");
            return NONE;
        }

        // Backwards Compatibility Fix
        text = text.toUpperCase().replace("CH", "K");

        // Parse from name
        try {
            return Phenotype.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {
        }

        // Parse from label
        try {
            for (Phenotype phenotype : Phenotype.values()) {
                if (phenotype.getLabel().equalsIgnoreCase(text)) {
                    return phenotype;
                }
            }
        } catch (Exception ignored) {
        }

        // Parse from ordinal
        try {
            return Phenotype.values()[MathUtility.parseInt(text, NONE.ordinal())];
        } catch (Exception ignored) {
        }

        logger.error("Unable to parse {} into a Phenotype. Returning NONE", text);
        return NONE;
    }
    // endregion File I/O

    /**
     * Returns a string representation of this phenotype.
     *
     * <p>The string is composed of the short name and, if the component is
     * {@code trueborn}, the label is appended with a space separator. If the component is not {@code trueborn}, only
     * the short name is included.
     * </p>
     *
     * @return A string representation consisting of the short name and, if applicable, the label, formatted as:
     *       <ul>
     *           <li>{@code "<shortName> <label>"} if {@code trueborn} is {@code true}</li>
     *           <li>{@code "<shortName>"} if {@code trueborn} is {@code false}</li>
     *       </ul>
     *
     * @author Illiani
     * @since 0.50.05
     */
    @Override
    public String toString() {
        return getShortName() + (isTrueborn ? ' ' + getLabel() : "");
    }
}
