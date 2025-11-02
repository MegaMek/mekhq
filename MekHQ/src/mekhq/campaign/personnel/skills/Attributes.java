/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.skills;

import static megamek.codeUtilities.MathUtility.clamp;
import static mekhq.campaign.personnel.PersonnelOptions.EXCEPTIONAL_ATTRIBUTE_BODY;
import static mekhq.campaign.personnel.PersonnelOptions.EXCEPTIONAL_ATTRIBUTE_CHARISMA;
import static mekhq.campaign.personnel.PersonnelOptions.EXCEPTIONAL_ATTRIBUTE_DEXTERITY;
import static mekhq.campaign.personnel.PersonnelOptions.EXCEPTIONAL_ATTRIBUTE_EDGE;
import static mekhq.campaign.personnel.PersonnelOptions.EXCEPTIONAL_ATTRIBUTE_INTELLIGENCE;
import static mekhq.campaign.personnel.PersonnelOptions.EXCEPTIONAL_ATTRIBUTE_REFLEXES;
import static mekhq.campaign.personnel.PersonnelOptions.EXCEPTIONAL_ATTRIBUTE_STRENGTH;
import static mekhq.campaign.personnel.PersonnelOptions.EXCEPTIONAL_ATTRIBUTE_WILLPOWER;
import static mekhq.campaign.personnel.PersonnelOptions.MUTATION_FREAKISH_STRENGTH;
import static mekhq.campaign.personnel.skills.Skill.getIndividualAttributeModifier;

import java.io.PrintWriter;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The {@code Attributes} class represents a set of core attributes for a character.
 *
 * <p>Each attribute has a defined minimum, maximum, and default score. Helper methods provide functionality to get,
 * set, adjust, and serialize these attributes.</p>
 *
 * <p>The attributes and their meanings are as follows:</p>
 * <ul>
 *     <li><b>Strength:</b> Physical strength of the entity.</li>
 *     <li><b>Body:</b> Overall fitness and physical endurance.</li>
 *     <li><b>Reflexes:</b> Reaction speed and agility.</li>
 *     <li><b>Dexterity:</b> Fine motor skills and coordination.</li>
 *     <li><b>Intelligence:</b> Cognitive abilities and reasoning capacity.</li>
 *     <li><b>Willpower:</b> Mental determination and perseverance.</li>
 *     <li><b>Charisma:</b> Social skills and personal magnetism.</li>
 * </ul>
 *
 * <p>The class also provides XML serialization and deserialization methods to read/write attributes from/to external
 * storage.</p>
 *
 * @since 0.50.5
 */
public class Attributes {
    private static final MMLogger LOGGER = MMLogger.create(Attributes.class);

    private int strength;
    private int body;
    private int reflexes;
    private int dexterity;
    private int intelligence;
    private int willpower;
    private int charisma;
    private int edge;

    /**
     * The default score assigned to all attributes during initialization.
     */
    public static int DEFAULT_ATTRIBUTE_SCORE = 5;

    /**
     * The minimum allowable score for any attribute.
     *
     * <p>Attribute values cannot be set below this limit, and any attempts to do so will result in clamping to this
     * value.</p>
     *
     * <p><b>Note:</b> ATOW allows attribute scores of 0. However, at that point the character is effectively dead
     * and outside the scope of MekHQ tracking (for now).</p>
     */
    public static int MINIMUM_ATTRIBUTE_SCORE = 1;

    /**
     * The maximum allowable score for any attribute.
     *
     * <p>Attribute values cannot be set above this limit, and any attempts to do so will result in clamping to this
     * value.</p>
     */
    public static int MAXIMUM_ATTRIBUTE_SCORE = 10;

    /**
     * Represents the cost required to improve an attribute. This is taken from ATOW pg 333.
     */
    public static int ATTRIBUTE_IMPROVEMENT_COST = 100;

    // Constructor

    /**
     * Constructs an {@code Attributes} object with all attributes initialized to their default value
     * {@link #DEFAULT_ATTRIBUTE_SCORE}.
     *
     * @since 0.50.5
     */
    public Attributes() {
        strength = DEFAULT_ATTRIBUTE_SCORE;
        body = DEFAULT_ATTRIBUTE_SCORE;
        reflexes = DEFAULT_ATTRIBUTE_SCORE;
        dexterity = DEFAULT_ATTRIBUTE_SCORE;
        intelligence = DEFAULT_ATTRIBUTE_SCORE;
        willpower = DEFAULT_ATTRIBUTE_SCORE;
        charisma = DEFAULT_ATTRIBUTE_SCORE;
        edge = 0;
    }


    /**
     * Creates an instance of {@code Attributes} with specified values for each {@link SkillAttribute}.
     *
     * @param strength     The initial value for the strength {@link SkillAttribute}.
     * @param body         The initial value for the body {@link SkillAttribute}.
     * @param reflexes     The initial value for the reflexes {@link SkillAttribute}.
     * @param dexterity    The initial value for the dexterity {@link SkillAttribute}.
     * @param intelligence The initial value for the intelligence {@link SkillAttribute}.
     * @param willpower    The initial value for the willpower {@link SkillAttribute}.
     * @param charisma     The initial value for the charisma {@link SkillAttribute}.
     * @param edge         The initial value for the edge {@link SkillAttribute}.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public Attributes(int strength, int body, int reflexes, int dexterity, int intelligence, int willpower,
          int charisma, int edge) {
        this.strength = strength;
        this.body = body;
        this.reflexes = reflexes;
        this.dexterity = dexterity;
        this.intelligence = intelligence;
        this.willpower = willpower;
        this.charisma = charisma;
        this.edge = edge;
    }

    /**
     * Initializes all attributes with the same specified value.
     *
     * <p>This constructor is primarily intended to facilitate testing by allowing all attribute fields to be set
     * to the same value with a single argument.</p>
     *
     * @param singleValue The value to be assigned to all attribute fields, such as strength, body, reflexes, dexterity,
     *                    intelligence, willpower, charisma, and edge.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public Attributes(int singleValue) {
        this.strength = singleValue;
        this.body = singleValue;
        this.reflexes = singleValue;
        this.dexterity = singleValue;
        this.intelligence = singleValue;
        this.willpower = singleValue;
        this.charisma = singleValue;
        this.edge = singleValue;
    }

    // Getters and Setters

    /**
     * Retrieves the value of a specified attribute.
     *
     * <p>This method returns the score of the requested {@link SkillAttribute}.
     * If the attribute does not match any of the defined attributes, the method returns {@code 0} as the default
     * value.</p>
     *
     * @param attribute the {@link SkillAttribute} to retrieve the value for.
     *
     * @return the value of the specified attribute, or {@code 0} if the attribute is not valid or not recognized.
     *
     * @since 0.50.05
     */
    public int getAttribute(SkillAttribute attribute) {
        return switch (attribute) {
            case STRENGTH -> clamp(strength, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
            case BODY -> clamp(body, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
            case REFLEXES -> clamp(reflexes, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
            case DEXTERITY -> clamp(dexterity, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
            case INTELLIGENCE -> clamp(intelligence, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
            case WILLPOWER -> clamp(willpower, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
            case CHARISMA -> clamp(charisma, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
            case EDGE -> clamp(edge, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
            default -> 0;
        };
    }

    /**
     * Calculates and returns the modifier for the specified skill attribute.
     *
     * <p>This method first retrieves the score for the given {@code attribute} by calling
     * {@code getAttribute(attribute)}, and then computes the associated modifier using
     * {@code getIndividualAttributeModifier(int attributeScore)}.</p>
     *
     * @param attribute the skill attribute for which the modifier is to be calculated
     *
     * @return the modifier value corresponding to the specified attribute
     *
     * @author Illiani
     * @since 0.50.06
     */
    public int getAttributeModifier(SkillAttribute attribute) {
        int attributeScore = getAttribute(attribute);
        return getIndividualAttributeModifier(attributeScore);
    }

    /**
     * Retrieves the score for a given attribute.
     *
     * <p>If an invalid or unsupported {@link SkillAttribute} is provided, this method logs an error and returns a
     * default value of {@link Attributes#DEFAULT_ATTRIBUTE_SCORE}.</p>
     *
     * @param attribute The {@link SkillAttribute} whose score should be retrieved.
     *
     * @return The score corresponding to the specified skill attribute, or {@link Attributes#DEFAULT_ATTRIBUTE_SCORE}
     *       if the attribute is invalid.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public int getAttributeScore(SkillAttribute attribute) {
        if (attribute == null || attribute.isNone()) {
            LOGGER.warn("(getAttributeScore) attribute is null or NONE.");
            return DEFAULT_ATTRIBUTE_SCORE;
        }

        return switch (attribute) {
            case STRENGTH -> strength;
            case BODY -> body;
            case REFLEXES -> reflexes;
            case DEXTERITY -> dexterity;
            case INTELLIGENCE -> intelligence;
            case WILLPOWER -> willpower;
            case CHARISMA -> charisma;
            case EDGE -> edge;
            default -> {
                LOGGER.error("(getAttributeScore) Invalid attribute requested: {}", attribute);
                yield DEFAULT_ATTRIBUTE_SCORE;
            }
        };
    }

    /**
     * Sets the score for a specific skill attribute while respecting its attribute cap.
     *
     * <p>This method updates the score of the provided {@link SkillAttribute} to the specified value. The score is
     * clamped to ensure it remains within the range of {@link Attributes#MINIMUM_ATTRIBUTE_SCORE} and the calculated
     * cap for the attribute.</p>
     *
     * <p>The attribute cap is determined using the provided {@link Phenotype} and may be further influenced by
     * {@link PersonnelOptions}, such as special abilities or conditions modifying the cap.</p>
     *
     * <p>If the provided {@link SkillAttribute} is <code>null</code>, represents "NONE", or is unrecognized,
     * the method logs a warning or error and exits without making any changes.</p>
     *
     * @param phenotype The {@link Phenotype} object used to derive the attribute cap for the skill.
     * @param options   The {@link PersonnelOptions} containing context-specific modifiers (e.g., special abilities).
     * @param attribute The {@link SkillAttribute} representing the attribute to update. Must not be <code>null</code>
     *                  or "NONE".
     * @param score     The new score to set for the specified attribute. This value will be clamped within
     *                  {@link Attributes#MINIMUM_ATTRIBUTE_SCORE} and the calculated cap.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public void setAttributeScore(Phenotype phenotype, PersonnelOptions options, SkillAttribute attribute, int score) {
        if (attribute == null || attribute.isNone()) {
            LOGGER.warn("(setAttributeScore) attribute is null or NONE.");
            return;
        }

        int cap = getAttributeCap(phenotype, options, attribute);

        // This ensures we never fall outside the hard boundaries, no matter how many SPAs or other weirdness the
        // player piles on.
        cap = clamp(cap, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);

        switch (attribute) {
            case STRENGTH -> strength = clamp(score, MINIMUM_ATTRIBUTE_SCORE, cap);
            case BODY -> body = clamp(score, MINIMUM_ATTRIBUTE_SCORE, cap);
            case REFLEXES -> reflexes = clamp(score, MINIMUM_ATTRIBUTE_SCORE, cap);
            case DEXTERITY -> dexterity = clamp(score, MINIMUM_ATTRIBUTE_SCORE, cap);
            case INTELLIGENCE -> intelligence = clamp(score, MINIMUM_ATTRIBUTE_SCORE, cap);
            case WILLPOWER -> willpower = clamp(score, MINIMUM_ATTRIBUTE_SCORE, cap);
            case CHARISMA -> charisma = clamp(score, MINIMUM_ATTRIBUTE_SCORE, cap);
            case EDGE -> edge = clamp(score, MINIMUM_ATTRIBUTE_SCORE, cap);
            default -> LOGGER.error("(setAttributeScore) Invalid attribute requested: {}", attribute);
        }
    }

    /**
     * Determines the maximum allowable value (cap) for the specified skill attribute.
     *
     * <p>This method calculates the cap for the given {@link SkillAttribute} based on the provided {@link Phenotype}
     * and {@link PersonnelOptions}. The base cap is retrieved from the {@code phenotype}, and adjustments are applied
     * if the character has specific traits (flags in {@code options}) that raise the cap for certain attributes.</p>
     *
     * <p>If the attribute is invalid or unrecognized, an error message is logged, and a default value of {@code 0} is
     * used.</p>
     *
     * @param phenotype The {@link Phenotype} that provides the base cap for the given attribute. Must not be
     *                  <code>null</code>.
     * @param options   The {@link PersonnelOptions} that may modify the attribute cap based on specific traits. Must
     *                  not be <code>null</code>.
     * @param attribute The {@link SkillAttribute} whose maximum value is being determined. Must not be
     *                  <code>null</code>.
     *
     * @return The maximum allowable value (cap) for the given attribute, based on the phenotype and applicable trait
     *       modifiers.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public int getAttributeCap(Phenotype phenotype, PersonnelOptions options, SkillAttribute attribute) {
        // This determines the base cap
        int cap = phenotype.getAttributeCap(attribute);

        // This is where you'd use options to verify if a character has SPAs that modify their maximum attribute score
        boolean hasExceptionalStrength = options.booleanOption(EXCEPTIONAL_ATTRIBUTE_STRENGTH);
        boolean hasFreakishStrength = options.booleanOption(MUTATION_FREAKISH_STRENGTH);
        boolean hasExceptionalBody = options.booleanOption(EXCEPTIONAL_ATTRIBUTE_BODY);
        boolean hasExceptionalReflexes = options.booleanOption(EXCEPTIONAL_ATTRIBUTE_REFLEXES);
        boolean hasExceptionalDexterity = options.booleanOption(EXCEPTIONAL_ATTRIBUTE_DEXTERITY);
        boolean hasExceptionalIntelligence = options.booleanOption(EXCEPTIONAL_ATTRIBUTE_INTELLIGENCE);
        boolean hasExceptionalWillpower = options.booleanOption(EXCEPTIONAL_ATTRIBUTE_WILLPOWER);
        boolean hasExceptionalCharisma = options.booleanOption(EXCEPTIONAL_ATTRIBUTE_CHARISMA);
        boolean hasExceptionalEdge = options.booleanOption(EXCEPTIONAL_ATTRIBUTE_EDGE);

        cap += switch (attribute) {
            case STRENGTH -> {
                int modifier = hasExceptionalStrength ? 1 : 0;
                modifier += hasFreakishStrength ? 1 : 0;
                yield modifier;
            }
            case BODY -> hasExceptionalBody ? 1 : 0;
            case DEXTERITY -> hasExceptionalReflexes ? 1 : 0;
            case REFLEXES -> hasExceptionalDexterity ? 1 : 0;
            case INTELLIGENCE -> hasExceptionalIntelligence ? 1 : 0;
            case WILLPOWER -> hasExceptionalWillpower ? 1 : 0;
            case CHARISMA -> hasExceptionalCharisma ? 1 : 0;
            case EDGE -> hasExceptionalEdge ? 1 : 0;
            default -> {
                LOGGER.error("(setAttributeScore) Invalid attribute requested for cap modifier: {}", attribute);
                yield 0;
            }
        };
        return cap;
    }

    /**
     * @deprecated use {@link #getAttributeScore(SkillAttribute)}  instead
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public int getStrength() {
        return strength;
    }

    /**
     * @deprecated use {@link #setAttributeScore(Phenotype, PersonnelOptions, SkillAttribute, int)} instead.
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public void setStrength(int strength) {
        this.strength = clamp(strength, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    /**
     * @deprecated use {@link #getAttributeScore(SkillAttribute)}  instead
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public int getBody() {
        return body;
    }

    /**
     * @deprecated use {@link #setAttributeScore(Phenotype, PersonnelOptions, SkillAttribute, int)} instead.
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public void setBody(int body) {
        this.body = clamp(body, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    /**
     * @deprecated use {@link #getAttributeScore(SkillAttribute)}  instead
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public int getReflexes() {
        return reflexes;
    }

    /**
     * @deprecated use {@link #setAttributeScore(Phenotype, PersonnelOptions, SkillAttribute, int)} instead.
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public void setReflexes(int reflexes) {
        this.reflexes = clamp(reflexes, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    /**
     * @deprecated use {@link #getAttributeScore(SkillAttribute)}  instead
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public int getDexterity() {
        return dexterity;
    }

    /**
     * @deprecated use {@link #setAttributeScore(Phenotype, PersonnelOptions, SkillAttribute, int)} instead.
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public void setDexterity(int dexterity) {
        this.dexterity = clamp(dexterity, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    /**
     * @deprecated use {@link #getAttributeScore(SkillAttribute)}  instead
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public int getIntelligence() {
        return intelligence;
    }

    /**
     * @deprecated use {@link #setAttributeScore(Phenotype, PersonnelOptions, SkillAttribute, int)} instead.
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public void setIntelligence(int intelligence) {
        this.intelligence = clamp(intelligence, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    /**
     * @deprecated use {@link #getAttributeScore(SkillAttribute)}  instead
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public int getWillpower() {
        return willpower;
    }

    /**
     * @deprecated use {@link #setAttributeScore(Phenotype, PersonnelOptions, SkillAttribute, int)} instead.
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public void setWillpower(int willpower) {
        this.willpower = clamp(willpower, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    /**
     * @deprecated use {@link #getAttributeScore(SkillAttribute)}  instead
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public int getCharisma() {
        return charisma;
    }

    /**
     * @deprecated use {@link #setAttributeScore(Phenotype, PersonnelOptions, SkillAttribute, int)} instead.
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public void setCharisma(int charisma) {
        this.charisma = clamp(charisma, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    // Utility Methods

    /**
     * Adjusts the scores of all skill attributes by a specified delta.
     *
     * <p>This method iterates through all available {@link SkillAttribute} values and applies the given delta to each,
     * modifying their scores accordingly. Attribute values are clamped within valid bounds as defined by the
     * {@code changeAttribute} and {@code setAttributeScore} methods, ensuring no invalid scores are set.</p>
     *
     * <p>Attributes marked as {@link SkillAttribute#NONE} are skipped during the iteration and are not modified.</p>
     *
     * @param phenotype The {@link Phenotype} used to determine the caps for all skill attributes.
     * @param options   The {@link PersonnelOptions} containing context-specific modifiers that may affect attribute
     *                  caps.
     * @param delta     The value to adjust each attribute's score. A positive delta increases the scores, while a
     *                  negative delta decreases them.
     *
     * @author Illiani
     * @since 0.50.5
     */
    public void changeAllAttributes(Phenotype phenotype, PersonnelOptions options, int delta) {
        if (phenotype == null) {
            LOGGER.warn("(changeAllAttributes) phenotype is null.");
            return;
        }

        if (options == null) {
            LOGGER.warn("(changeAllAttributes) options is null.");
            return;
        }

        for (SkillAttribute attribute : SkillAttribute.values()) {
            if (attribute.isNone()) {
                continue;
            }

            changeAttribute(phenotype, options, attribute, delta);
        }
    }

    /**
     * Adjusts the score of a specified skill attribute by a given delta.
     *
     * <p>This method modifies the score of the provided {@link SkillAttribute} by adding the given delta to its
     * current score. The updated score is passed to
     * {@link #setAttributeScore(Phenotype, PersonnelOptions, SkillAttribute, int)} to ensure it complies with the
     * applicable constraints and caps defined by the provided {@link Phenotype} and {@link PersonnelOptions}.</p>
     *
     * <p>If the {@code phenotype}, {@code options}, or {@code attribute} is <code>null</code>, or if the attribute
     * represents "NONE", the method logs a warning and exits without making any changes.</p>
     *
     * @param phenotype The {@link Phenotype} used to determine the cap for the skill attribute. Must not be
     *                  <code>null</code>.
     * @param options   The {@link PersonnelOptions} containing context-specific modifiers that may influence the cap
     *                  for the attribute. Must not be <code>null</code>.
     * @param attribute The {@link SkillAttribute} whose score is to be modified. Must not be <code>null</code> or
     *                  "NONE".
     * @param delta     The value to adjust the current score of the attribute, where a positive value increases the
     *                  score and a negative value decreases it.
     */
    public void changeAttribute(Phenotype phenotype, PersonnelOptions options, SkillAttribute attribute, int delta) {
        if (phenotype == null) {
            LOGGER.warn("(changeAttribute) phenotype is null.");
            return;
        }
        if (options == null) {
            LOGGER.warn("(changeAttribute) options is null.");
            return;
        }

        if (attribute == null || attribute.isNone()) {
            LOGGER.warn("(changeAttribute) attribute is null or NONE.");
            return;
        }

        int currentScore = getAttributeScore(attribute);
        // We defer ensuring this falls within permissible values to setAttributeScore
        int newScore = currentScore + delta;
        setAttributeScore(phenotype, options, attribute, newScore);
    }

    /**
     * Changes the strength attribute by a delta.
     *
     * <p>The result is clamped between {@link #MINIMUM_ATTRIBUTE_SCORE} and {@link #MAXIMUM_ATTRIBUTE_SCORE}.</p>
     *
     * @param delta the value to add to the current strength. A positive delta will increase the attribute score, while
     *              a negative delta will decrease it.
     *
     * @since 0.50.5
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public void changeStrength(int delta) {
        strength += delta;
        strength = clamp(strength, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }


    /**
     * Changes the body attribute by a delta.
     *
     * <p>The result is clamped between {@link #MINIMUM_ATTRIBUTE_SCORE} and {@link #MAXIMUM_ATTRIBUTE_SCORE}.</p>
     *
     * @param delta the value to add to the current body. A positive delta will increase the attribute score, while a
     *              negative delta will decrease it.
     *
     * @since 0.50.5
     */
    public void changeBody(int delta) {
        body += delta;
        body = clamp(body, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }


    /**
     * Changes the reflexes attribute by a delta.
     *
     * <p>The result is clamped between {@link #MINIMUM_ATTRIBUTE_SCORE} and {@link #MAXIMUM_ATTRIBUTE_SCORE}.</p>
     *
     * @param delta the value to add to the current reflexes. A positive delta will increase the attribute score, while
     *              a negative delta will decrease it.
     *
     * @since 0.50.5
     */
    public void changeReflexes(int delta) {
        reflexes += delta;
        reflexes = clamp(reflexes, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }


    /**
     * Changes the dexterity attribute by a delta.
     *
     * <p>The result is clamped between {@link #MINIMUM_ATTRIBUTE_SCORE} and {@link #MAXIMUM_ATTRIBUTE_SCORE}.</p>
     *
     * @param delta the value to add to the current dexterity. A positive delta will increase the attribute score, while
     *              a negative delta will decrease it.
     *
     * @since 0.50.5
     */
    public void changeDexterity(int delta) {
        dexterity += delta;
        dexterity = clamp(dexterity, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }


    /**
     * Changes the intelligence attribute by a delta.
     *
     * <p>The result is clamped between {@link #MINIMUM_ATTRIBUTE_SCORE} and {@link #MAXIMUM_ATTRIBUTE_SCORE}.</p>
     *
     * @param delta the value to add to the current intelligence. A positive delta will increase the attribute score,
     *              while a negative delta will decrease it.
     *
     * @since 0.50.5
     */
    public void changeIntelligence(int delta) {
        intelligence += delta;
        intelligence = clamp(intelligence, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }


    /**
     * Changes the willpower attribute by a delta.
     *
     * <p>The result is clamped between {@link #MINIMUM_ATTRIBUTE_SCORE} and {@link #MAXIMUM_ATTRIBUTE_SCORE}.</p>
     *
     * @param delta the value to add to the current willpower. A positive delta will increase the attribute score, while
     *              a negative delta will decrease it.
     *
     * @since 0.50.5
     */
    public void changeWillpower(int delta) {
        willpower += delta;
        willpower = clamp(willpower, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }


    /**
     * Changes the charisma attribute by a delta.
     *
     * <p>The result is clamped between {@link #MINIMUM_ATTRIBUTE_SCORE} and {@link #MAXIMUM_ATTRIBUTE_SCORE}.</p>
     *
     * @param delta the value to add to the current charisma. A positive delta will increase the attribute score, while
     *              a negative delta will decrease it.
     *
     * @since 0.50.5
     */
    public void changeCharisma(int delta) {
        charisma += delta;
        charisma = clamp(charisma, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }


    /**
     * Changes the edge attribute by a delta.
     *
     * <p>The result is clamped between {@link #MINIMUM_ATTRIBUTE_SCORE} and {@link #MAXIMUM_ATTRIBUTE_SCORE}.</p>
     *
     * @param delta the value to add to the current edge. A positive delta will increase the attribute score, while a
     *              negative delta will decrease it.
     *
     * @since 0.50.5
     */
    public void changeEdge(int delta) {
        edge += delta;
        edge = clamp(edge, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    // Reading and Writing

    /**
     * Writes the current attributes to an XML format.
     *
     * <p>The method generates simple XML tags, writing each attribute as a named element containing its current
     * value.</p>
     *
     * <pre>{@code
     * <Attributes>
     *     <strength>5</strength>
     *     <body>5</body>
     *     <reflexes>5</reflexes>
     *     ...
     * </Attributes>
     * }</pre>
     *
     * @param printWriter the writer used to write the XML.
     * @param indent      the number of spaces to indent each XML tag.
     *
     * @since 0.50.5
     */
    public void writeAttributesToXML(final PrintWriter printWriter, int indent) {
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "strength", strength);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "body", body);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "reflexes", reflexes);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "dexterity", dexterity);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "intelligence", intelligence);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "willpower", willpower);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "charisma", charisma);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "edge", edge);
    }

    /**
     * Reads attributes from an XML {@link Node}.
     *
     * <p>The method parses child nodes of the given XML node to set the values of attributes. If parsing fails for
     * any reason, default attribute values are used.</p>
     *
     * @param workingNode the XML node containing attribute data.
     *
     * @return the current {@code Attributes} object with updated values.
     *
     * @author Illiani
     * @since 0.50.5
     */
    public Attributes generateAttributesFromXML(final Node workingNode) {
        NodeList newLine = workingNode.getChildNodes();

        try {
            for (int i = 0; i < newLine.getLength(); i++) {
                Node workingNode2 = newLine.item(i);

                if (workingNode2.getNodeName().equalsIgnoreCase("strength")) {
                    this.strength = MathUtility.parseInt(workingNode2.getTextContent(), DEFAULT_ATTRIBUTE_SCORE);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("body")) {
                    this.body = MathUtility.parseInt(workingNode2.getTextContent(), DEFAULT_ATTRIBUTE_SCORE);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("reflexes")) {
                    this.reflexes = MathUtility.parseInt(workingNode2.getTextContent(), DEFAULT_ATTRIBUTE_SCORE);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("dexterity")) {
                    this.dexterity = MathUtility.parseInt(workingNode2.getTextContent(), DEFAULT_ATTRIBUTE_SCORE);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("intelligence")) {
                    this.intelligence = MathUtility.parseInt(workingNode2.getTextContent(), DEFAULT_ATTRIBUTE_SCORE);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("willpower")) {
                    this.willpower = MathUtility.parseInt(workingNode2.getTextContent(), DEFAULT_ATTRIBUTE_SCORE);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("charisma")) {
                    this.charisma = MathUtility.parseInt(workingNode2.getTextContent(), DEFAULT_ATTRIBUTE_SCORE);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("edge")) {
                    this.edge = MathUtility.parseInt(workingNode2.getTextContent());
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Could not parse Attributes: ", ex);
        }

        return this;
    }
}
