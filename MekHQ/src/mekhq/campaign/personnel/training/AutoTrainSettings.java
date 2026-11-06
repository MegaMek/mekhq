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
        return defaultsBuilder(name, campaignOptions).build();
    }

    private static Builder defaultsBuilder(String name, CampaignOptions campaignOptions) {
        QuickTrainOptions options = QuickTrainOptions.buildQuickTrainOptions(campaignOptions);
        return new Builder()
                     .setName(name)
                     .setTargetLevel(DEFAULT_TARGET_LEVEL)
                     .setLevelArtillery(options.isLevelArtillery())
                     .setLevelScouting(options.isLevelScoutingSkills())
                     .setLevelEscape(options.isLevelEscapeSkills())
                     .setLevelLeadership(options.isLevelLeadership())
                     .setLevelTraining(options.isLevelTraining())
                     .setLevelOtherCommand(options.isLevelOtherCommandSkills())
                     .setSkipTrainingFormations(true)
                     .setRespectExclusions(true);
    }

    /** @return the optional skills to train, in Quick Train's terms */
    public QuickTrainOptions toQuickTrainOptions() {
        return new QuickTrainOptions(isLevelArtillery, isLevelScouting, isLevelEscape, isLevelLeadership,
              isLevelTraining, isLevelOtherCommand, isSkipTrainingFormations);
    }

    /** @return a builder holding these settings, to change some of them */
    public Builder toBuilder() {
        return new Builder()
                     .setName(name)
                     .setTargetLevel(targetLevel)
                     .setContinuous(isContinuous)
                     .setLevelArtillery(isLevelArtillery)
                     .setLevelScouting(isLevelScouting)
                     .setLevelEscape(isLevelEscape)
                     .setLevelLeadership(isLevelLeadership)
                     .setLevelTraining(isLevelTraining)
                     .setLevelOtherCommand(isLevelOtherCommand)
                     .setSpecificSkill(specificSkill)
                     .setReserveXp(reserveXp)
                     .setSkipTrainingFormations(isSkipTrainingFormations)
                     .setRespectExclusions(isRespectExclusions)
                     .setSkipFeeImprovements(isSkipFeeImprovements)
                     .setOrder(order)
                     .setRunMonthly(isRunMonthly);
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
        Builder builder = defaultsBuilder("", campaignOptions);
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String text = child.getTextContent().trim();
            try {
                switch (child.getNodeName()) {
                    case "name" -> builder.setName(text);
                    case "targetLevel" -> builder.setTargetLevel(MathUtility.parseInt(text, builder.getTargetLevel()));
                    case "continuous" -> builder.setContinuous(Boolean.parseBoolean(text));
                    case "levelArtillery" -> builder.setLevelArtillery(Boolean.parseBoolean(text));
                    case "levelScouting" -> builder.setLevelScouting(Boolean.parseBoolean(text));
                    case "levelEscape" -> builder.setLevelEscape(Boolean.parseBoolean(text));
                    case "levelLeadership" -> builder.setLevelLeadership(Boolean.parseBoolean(text));
                    case "levelTraining" -> builder.setLevelTraining(Boolean.parseBoolean(text));
                    case "levelOtherCommand" -> builder.setLevelOtherCommand(Boolean.parseBoolean(text));
                    case "specificSkill" -> builder.setSpecificSkill(text.isBlank() ? null : text);
                    case "reserveXp" -> builder.setReserveXp(MathUtility.parseInt(text, builder.getReserveXp()));
                    case "skipTrainingFormations" -> builder.setSkipTrainingFormations(Boolean.parseBoolean(text));
                    case "respectExclusions" -> builder.setRespectExclusions(Boolean.parseBoolean(text));
                    case "skipFeeImprovements" -> builder.setSkipFeeImprovements(Boolean.parseBoolean(text));
                    case "order" -> builder.setOrder(Order.valueOf(text));
                    case "runMonthly" -> builder.setRunMonthly(Boolean.parseBoolean(text));
                    default -> LOGGER.debug("Ignoring unknown auto-train preset element {}", child.getNodeName());
                }
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("Ignoring unreadable auto-train preset value {} for {}", text, child.getNodeName());
            }
        }
        return builder.build();
    }

    // endregion XML

    /**
     * Settings being put together or edited, such as on the Training Console's Auto-train page. Everything starts
     * off, at level 0, with no name, training the weakest skill first.
     */
    public static final class Builder {
        private String name = "";
        private int targetLevel;
        private boolean isContinuous;
        private boolean isLevelArtillery;
        private boolean isLevelScouting;
        private boolean isLevelEscape;
        private boolean isLevelLeadership;
        private boolean isLevelTraining;
        private boolean isLevelOtherCommand;
        private @Nullable String specificSkill;
        private int reserveXp;
        private boolean isSkipTrainingFormations;
        private boolean isRespectExclusions;
        private boolean isSkipFeeImprovements;
        private Order order = Order.WEAKEST_FIRST;
        private boolean isRunMonthly;

        /** @return the settings */
        public AutoTrainSettings build() {
            return new AutoTrainSettings(name, targetLevel, isContinuous, isLevelArtillery, isLevelScouting,
                  isLevelEscape, isLevelLeadership, isLevelTraining, isLevelOtherCommand, specificSkill, reserveXp,
                  isSkipTrainingFormations, isRespectExclusions, isSkipFeeImprovements, order, isRunMonthly);
        }

        public String getName() {
            return name;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public int getTargetLevel() {
            return targetLevel;
        }

        public Builder setTargetLevel(int targetLevel) {
            this.targetLevel = targetLevel;
            return this;
        }

        public boolean isContinuous() {
            return isContinuous;
        }

        public Builder setContinuous(boolean isContinuous) {
            this.isContinuous = isContinuous;
            return this;
        }

        public boolean isLevelArtillery() {
            return isLevelArtillery;
        }

        public Builder setLevelArtillery(boolean isLevelArtillery) {
            this.isLevelArtillery = isLevelArtillery;
            return this;
        }

        public boolean isLevelScouting() {
            return isLevelScouting;
        }

        public Builder setLevelScouting(boolean isLevelScouting) {
            this.isLevelScouting = isLevelScouting;
            return this;
        }

        public boolean isLevelEscape() {
            return isLevelEscape;
        }

        public Builder setLevelEscape(boolean isLevelEscape) {
            this.isLevelEscape = isLevelEscape;
            return this;
        }

        public boolean isLevelLeadership() {
            return isLevelLeadership;
        }

        public Builder setLevelLeadership(boolean isLevelLeadership) {
            this.isLevelLeadership = isLevelLeadership;
            return this;
        }

        public boolean isLevelTraining() {
            return isLevelTraining;
        }

        public Builder setLevelTraining(boolean isLevelTraining) {
            this.isLevelTraining = isLevelTraining;
            return this;
        }

        public boolean isLevelOtherCommand() {
            return isLevelOtherCommand;
        }

        public Builder setLevelOtherCommand(boolean isLevelOtherCommand) {
            this.isLevelOtherCommand = isLevelOtherCommand;
            return this;
        }

        public @Nullable String getSpecificSkill() {
            return specificSkill;
        }

        public Builder setSpecificSkill(@Nullable String specificSkill) {
            this.specificSkill = specificSkill;
            return this;
        }

        public int getReserveXp() {
            return reserveXp;
        }

        public Builder setReserveXp(int reserveXp) {
            this.reserveXp = reserveXp;
            return this;
        }

        public boolean isSkipTrainingFormations() {
            return isSkipTrainingFormations;
        }

        public Builder setSkipTrainingFormations(boolean isSkipTrainingFormations) {
            this.isSkipTrainingFormations = isSkipTrainingFormations;
            return this;
        }

        public boolean isRespectExclusions() {
            return isRespectExclusions;
        }

        public Builder setRespectExclusions(boolean isRespectExclusions) {
            this.isRespectExclusions = isRespectExclusions;
            return this;
        }

        public boolean isSkipFeeImprovements() {
            return isSkipFeeImprovements;
        }

        public Builder setSkipFeeImprovements(boolean isSkipFeeImprovements) {
            this.isSkipFeeImprovements = isSkipFeeImprovements;
            return this;
        }

        public Order getOrder() {
            return order;
        }

        public Builder setOrder(Order order) {
            this.order = order;
            return this;
        }

        public boolean isRunMonthly() {
            return isRunMonthly;
        }

        public Builder setRunMonthly(boolean isRunMonthly) {
            this.isRunMonthly = isRunMonthly;
            return this;
        }
    }
}
