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
 */
package mekhq.campaign.personnel.skills;

import static megamek.codeUtilities.MathUtility.clamp;

import java.io.PrintWriter;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
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
    private static final MMLogger logger = MMLogger.create(Attributes.class);

    private int strength;
    private int body;
    private int reflexes;
    private int dexterity;
    private int intelligence;
    private int willpower;
    private int charisma;

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
    }

    /**
     * Constructs a new {@code Attributes} object with the specified attribute values.
     *
     * @param strength     the {@link SkillAttribute#STRENGTH} value of the character, representing physical power.
     * @param body         the {@link SkillAttribute#BODY} value of the character, representing endurance and physical
     *                     resilience.
     * @param reflexes     the {@link SkillAttribute#REFLEXES} value of the character, representing reaction speed and
     *                     agility.
     * @param dexterity    the {@link SkillAttribute#DEXTERITY} value of the character, representing skillfulness and
     *                     precision.
     * @param intelligence the {@link SkillAttribute#INTELLIGENCE} value of the character, representing cognitive
     *                     ability and reasoning.
     * @param willpower    the {@link SkillAttribute#WILLPOWER} value of the character, representing mental strength and
     *                     determination.
     * @param charisma     the {@link SkillAttribute#CHARISMA} value of the character, representing persuasiveness and
     *                     social skills.
     *
     * @author Illiani
     * @since 0.50.5
     */
    public Attributes(int strength, int body, int reflexes, int dexterity, int intelligence, int willpower,
          int charisma) {
        this.strength = strength;
        this.body = body;
        this.reflexes = reflexes;
        this.dexterity = dexterity;
        this.intelligence = intelligence;
        this.willpower = willpower;
        this.charisma = charisma;
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
            case STRENGTH -> strength;
            case BODY -> body;
            case REFLEXES -> reflexes;
            case DEXTERITY -> dexterity;
            case INTELLIGENCE -> intelligence;
            case WILLPOWER -> willpower;
            case CHARISMA -> charisma;
            default -> 0;
        };
    }

    /**
     * @return the current strength value.
     *
     * @since 0.50.5
     */
    public int getStrength() {
        return strength;
    }

    /**
     * Sets the strength attribute, ensuring it is clamped between the defined minimum and maximum values.
     *
     * @param strength the new strength value.
     *
     * @see #MINIMUM_ATTRIBUTE_SCORE
     * @see #MAXIMUM_ATTRIBUTE_SCORE
     * @since 0.50.5
     */
    public void setStrength(int strength) {
        this.strength = clamp(strength, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    /**
     * @return the current body value.
     *
     * @since 0.50.5
     */
    public int getBody() {
        return body;
    }


    /**
     * Sets the body attribute, ensuring it is clamped between the defined minimum and maximum values.
     *
     * @param body the new body value.
     *
     * @see #MINIMUM_ATTRIBUTE_SCORE
     * @see #MAXIMUM_ATTRIBUTE_SCORE
     * @since 0.50.5
     */
    public void setBody(int body) {
        this.body = clamp(body, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    /**
     * @return the current reflexes value.
     *
     * @since 0.50.5
     */
    public int getReflexes() {
        return reflexes;
    }


    /**
     * Sets the reflexes attribute, ensuring it is clamped between the defined minimum and maximum values.
     *
     * @param reflexes the new reflexes value.
     *
     * @see #MINIMUM_ATTRIBUTE_SCORE
     * @see #MAXIMUM_ATTRIBUTE_SCORE
     * @since 0.50.5
     */
    public void setReflexes(int reflexes) {
        this.reflexes = clamp(reflexes, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    /**
     * @return the current dexterity value.
     *
     * @since 0.50.5
     */
    public int getDexterity() {
        return dexterity;
    }


    /**
     * Sets the dexterity attribute, ensuring it is clamped between the defined minimum and maximum values.
     *
     * @param dexterity the new dexterity value.
     *
     * @see #MINIMUM_ATTRIBUTE_SCORE
     * @see #MAXIMUM_ATTRIBUTE_SCORE
     * @since 0.50.5
     */
    public void setDexterity(int dexterity) {
        this.dexterity = clamp(dexterity, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    /**
     * @return the current intelligence value.
     *
     * @since 0.50.5
     */
    public int getIntelligence() {
        return intelligence;
    }

    /**
     * Sets the intelligence attribute, ensuring it is clamped between the defined minimum and maximum values.
     *
     * @param intelligence the new intelligence value.
     *
     * @see #MINIMUM_ATTRIBUTE_SCORE
     * @see #MAXIMUM_ATTRIBUTE_SCORE
     * @since 0.50.5
     */
    public void setIntelligence(int intelligence) {
        this.intelligence = clamp(intelligence, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    /**
     * @return the current willpower value.
     *
     * @since 0.50.5
     */
    public int getWillpower() {
        return willpower;
    }

    /**
     * Sets the willpower attribute, ensuring it is clamped between the defined minimum and maximum values.
     *
     * @param willpower the new willpower value.
     *
     * @see #MINIMUM_ATTRIBUTE_SCORE
     * @see #MAXIMUM_ATTRIBUTE_SCORE
     * @since 0.50.5
     */
    public void setWillpower(int willpower) {
        this.willpower = clamp(willpower, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    /**
     * @return the current charisma value.
     *
     * @since 0.50.5
     */
    public int getCharisma() {
        return charisma;
    }

    /**
     * Sets the charisma attribute, ensuring it is clamped between the defined minimum and maximum values.
     *
     * @param charisma the new charisma value.
     *
     * @see #MINIMUM_ATTRIBUTE_SCORE
     * @see #MAXIMUM_ATTRIBUTE_SCORE
     * @since 0.50.5
     */
    public void setCharisma(int charisma) {
        this.charisma = clamp(charisma, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    // Utility Methods

    /**
     * Applies a delta to all attributes, incrementing or decrementing their values by the specified amount while
     * clamping results within bounds.
     *
     * @param delta the value to add to each attribute. A positive delta will increase the attribute scores, while a
     *              negative delta will decrease them.
     *
     * @since 0.50.5
     */
    public void changeAllAttributes(int delta) {
        changeStrength(delta);
        changeBody(delta);
        changeReflexes(delta);
        changeDexterity(delta);
        changeIntelligence(delta);
        changeWillpower(delta);
        changeCharisma(delta);
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
                }
            }
        } catch (Exception ex) {
            logger.error("Could not parse Attributes: ", ex);
        }

        return this;
    }
}
