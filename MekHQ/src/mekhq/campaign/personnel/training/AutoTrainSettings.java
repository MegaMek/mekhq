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
package mekhq.campaign.personnel.training;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.io.PrintWriter;

import jakarta.annotation.Nullable;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.QuickTrain.QuickTrainOptions;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * How auto-train levels people's skills: the Training Console's replacement for Quick Train and Mass Training, saved
 * per campaign as named presets. A preset can also run by itself at the start of each month.
 *
 * @param name                    the preset's name
 * @param targetLevel             the level to train skills up to
 * @param isContinuous            {@code true} to keep training each skill until it reaches the target or the person
 *                                runs out of XP; {@code false} for one level per skill
 * @param isLevelArtillery        also train Artillery, for those who have it
 * @param isLevelScouting         also train each person's best scouting skill
 * @param isLevelEscape           also train each person's best escape skill
 * @param isLevelLeadership       also train Leadership, for those who have it
 * @param isLevelTraining         also train Training, for those who have it
 * @param isLevelOtherCommand     also train Tactics and Strategy, for those who have them
 * @param specificSkill           train only this skill, for everyone who has it, as Mass Training did; {@code null} to
 *                                train role skills, as Quick Train did
 * @param reserveXp               XP each person keeps back
 * @param isSkipTrainingFormations leave out people in a formation whose combat role is training
 * @param isRespectExclusions     leave out people marked to be excluded from automatic training
 * @param isSkipFeeImprovements   leave out improvements that cost C-bills
 * @param order                   which skill each person trains first
 * @param isRunMonthly            run this preset by itself at the start of each month
 *
 * @author Illiani
 * @since 0.51.01
 */
public record AutoTrainSettings(String name, int targetLevel, boolean isContinuous, boolean isLevelArtillery,
      boolean isLevelScouting, boolean isLevelEscape, boolean isLevelLeadership, boolean isLevelTraining,
      boolean isLevelOtherCommand, @Nullable String specificSkill, int reserveXp, boolean isSkipTrainingFormations,
      boolean isRespectExclusions, boolean isSkipFeeImprovements, Order order, boolean isRunMonthly) {
    private static final MMLogger LOGGER = MMLogger.create(AutoTrainSettings.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.TrainingConsole";
    /** Quick Train's default target. */
    public static final int DEFAULT_TARGET_LEVEL = 5;

    /** Which skill each person trains first. */
    public enum Order {
        /** The weakest skill first, as Quick Train did. */
        WEAKEST_FIRST,
        /** The cheapest improvement first, to make the most of each person's XP. */
        CHEAPEST_FIRST;

        /** @return the order's name for the player */
        public String getLabel() {
            return getTextAt(RESOURCE_BUNDLE, "AutoTrainSettings.Order." + name());
        }
    }

    /**
     * @param name            the preset's name
     * @param campaignOptions the campaign's options, which decide which optional skills Quick Train offers
     *
     * @return settings that train as Quick Train did by default
     */
    public static AutoTrainSettings defaults(String name, CampaignOptions campaignOptions) {
        QuickTrainOptions options = QuickTrainOptions.buildQuickTrainOptions(campaignOptions);
        return new AutoTrainSettings(name, DEFAULT_TARGET_LEVEL, false, options.isLevelArtillery(),
              options.isLevelScoutingSkills(), options.isLevelEscapeSkills(), options.isLevelLeadership(),
              options.isLevelTraining(), options.isLevelOtherCommandSkills(), null, 0, true, true, false,
              Order.WEAKEST_FIRST, false);
    }

    /** @return the optional skills to train, in Quick Train's terms */
    public QuickTrainOptions toQuickTrainOptions() {
        return new QuickTrainOptions(isLevelArtillery, isLevelScouting, isLevelEscape, isLevelLeadership,
              isLevelTraining, isLevelOtherCommand, isSkipTrainingFormations);
    }

    /** @return these settings under another name */
    public AutoTrainSettings withName(String newName) {
        return new AutoTrainSettings(newName, targetLevel, isContinuous, isLevelArtillery, isLevelScouting,
              isLevelEscape, isLevelLeadership, isLevelTraining, isLevelOtherCommand, specificSkill, reserveXp,
              isSkipTrainingFormations, isRespectExclusions, isSkipFeeImprovements, order, isRunMonthly);
    }

    /** @return these settings, running monthly or not */
    public AutoTrainSettings withRunMonthly(boolean runMonthly) {
        return new AutoTrainSettings(name, targetLevel, isContinuous, isLevelArtillery, isLevelScouting,
              isLevelEscape, isLevelLeadership, isLevelTraining, isLevelOtherCommand, specificSkill, reserveXp,
              isSkipTrainingFormations, isRespectExclusions, isSkipFeeImprovements, order, runMonthly);
    }

    // region XML

    /**
     * Writes these settings as a {@code <preset>} element.
     *
     * @param writer the writer
     * @param indent the indent
     */
    public void writeToXML(PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "preset");
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "name", name);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "targetLevel", targetLevel);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "continuous", isContinuous);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "levelArtillery", isLevelArtillery);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "levelScouting", isLevelScouting);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "levelEscape", isLevelEscape);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "levelLeadership", isLevelLeadership);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "levelTraining", isLevelTraining);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "levelOtherCommand", isLevelOtherCommand);
        if (specificSkill != null) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "specificSkill", specificSkill);
        }
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "reserveXp", reserveXp);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "skipTrainingFormations", isSkipTrainingFormations);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "respectExclusions", isRespectExclusions);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "skipFeeImprovements", isSkipFeeImprovements);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "order", order.name());
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "runMonthly", isRunMonthly);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "preset");
    }

    /**
     * Reads settings from a {@code <preset>} element. Missing or unreadable values keep Quick Train's defaults.
     *
     * @param node            the element
     * @param campaignOptions the campaign's options, for the defaults
     *
     * @return the settings
     */
    public static AutoTrainSettings generateInstanceFromXML(Node node, CampaignOptions campaignOptions) {
        AutoTrainSettings defaults = defaults("", campaignOptions);
        String name = defaults.name();
        int targetLevel = defaults.targetLevel();
        boolean isContinuous = defaults.isContinuous();
        boolean isLevelArtillery = defaults.isLevelArtillery();
        boolean isLevelScouting = defaults.isLevelScouting();
        boolean isLevelEscape = defaults.isLevelEscape();
        boolean isLevelLeadership = defaults.isLevelLeadership();
        boolean isLevelTraining = defaults.isLevelTraining();
        boolean isLevelOtherCommand = defaults.isLevelOtherCommand();
        String specificSkill = null;
        int reserveXp = defaults.reserveXp();
        boolean isSkipTrainingFormations = defaults.isSkipTrainingFormations();
        boolean isRespectExclusions = defaults.isRespectExclusions();
        boolean isSkipFeeImprovements = defaults.isSkipFeeImprovements();
        Order order = defaults.order();
        boolean isRunMonthly = defaults.isRunMonthly();

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String text = child.getTextContent().trim();
            try {
                switch (child.getNodeName()) {
                    case "name" -> name = text;
                    case "targetLevel" -> targetLevel = MathUtility.parseInt(text, targetLevel);
                    case "continuous" -> isContinuous = Boolean.parseBoolean(text);
                    case "levelArtillery" -> isLevelArtillery = Boolean.parseBoolean(text);
                    case "levelScouting" -> isLevelScouting = Boolean.parseBoolean(text);
                    case "levelEscape" -> isLevelEscape = Boolean.parseBoolean(text);
                    case "levelLeadership" -> isLevelLeadership = Boolean.parseBoolean(text);
                    case "levelTraining" -> isLevelTraining = Boolean.parseBoolean(text);
                    case "levelOtherCommand" -> isLevelOtherCommand = Boolean.parseBoolean(text);
                    case "specificSkill" -> specificSkill = text.isBlank() ? null : text;
                    case "reserveXp" -> reserveXp = MathUtility.parseInt(text, reserveXp);
                    case "skipTrainingFormations" -> isSkipTrainingFormations = Boolean.parseBoolean(text);
                    case "respectExclusions" -> isRespectExclusions = Boolean.parseBoolean(text);
                    case "skipFeeImprovements" -> isSkipFeeImprovements = Boolean.parseBoolean(text);
                    case "order" -> order = Order.valueOf(text);
                    case "runMonthly" -> isRunMonthly = Boolean.parseBoolean(text);
                    default -> LOGGER.debug("Ignoring unknown auto-train preset element {}", child.getNodeName());
                }
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("Ignoring unreadable auto-train preset value {} for {}", text, child.getNodeName());
            }
        }
        return new AutoTrainSettings(name, targetLevel, isContinuous, isLevelArtillery, isLevelScouting,
              isLevelEscape, isLevelLeadership, isLevelTraining, isLevelOtherCommand, specificSkill, reserveXp,
              isSkipTrainingFormations, isRespectExclusions, isSkipFeeImprovements, order, isRunMonthly);
    }

    // endregion XML
}
