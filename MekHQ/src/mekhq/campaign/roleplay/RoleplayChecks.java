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
package mekhq.campaign.roleplay;

import static mekhq.campaign.enums.DailyReportType.SKILL_CHECKS;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import megamek.common.annotations.Nullable;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.ActionCheck;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.ActionCheckRoll;
import mekhq.campaign.personnel.skills.ActionCheckRoll.RollType;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;

/**
 * Makes skill, attribute and opposed checks for the Oracle console's Checks page, logs them to the Oracle log and adds
 * them to the daily report. Single and group checks use MekHQ's own {@link ActionCheck}, so they roll exactly as checks
 * elsewhere do.
 */
public class RoleplayChecks {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    /**
     * The campaign settings that affect a check.
     *
     * @param useAgingEffects {@code true} if age changes skill target numbers
     * @param clanCampaign    {@code true} if the player's force is a Clan force
     * @param date            the current in-game date
     * @param edgeAllowed     {@code true} if the campaign uses Edge
     */
    public record Settings(boolean useAgingEffects, boolean clanCampaign, LocalDate date, boolean edgeAllowed) {}

    /**
     * One side of an opposed check: someone in Personnel rolling a skill or attributes, or a cast member with no stats
     * rolling against a rating.
     *
     * @param person     the person rolling, or {@code null} for a rated character
     * @param character  the cast member, or {@code null} if the person is not in the cast
     * @param trait      what the person rolls, or {@code null} for a rated character
     * @param rating     the rated character's rating, or {@code null} for a person
     * @param difficulty how hard the situation makes it for this side
     * @param other      any other change to the target number; positive makes it harder
     * @param useEdge    {@code true} to re-roll a loss with Edge, if this side has any
     */
    public record Opponent(@Nullable Person person, @Nullable OracleCharacter character, @Nullable CheckTrait trait,
          @Nullable NpcRating rating, CheckDifficulty difficulty, int other, boolean useEdge) {
        /**
         * @return someone in Personnel rolling {@code trait}
         */
        public static Opponent person(final Person person, final @Nullable OracleCharacter character,
              final CheckTrait trait, final CheckDifficulty difficulty, final int other, final boolean useEdge) {
            return new Opponent(person, character, trait, null, difficulty, other, useEdge);
        }

        /**
         * @return a cast member with no stats, rolling against {@code rating}
         */
        public static Opponent rated(final @Nullable OracleCharacter character, final NpcRating rating,
              final CheckDifficulty difficulty, final int other) {
            return new Opponent(null, character, null, rating, difficulty, other, false);
        }

        String name() {
            if (person != null) {
                return person.getFullName();
            }
            return (character == null) ? "" : character.getName();
        }
    }

    private final OracleActions actions;
    private final Supplier<Settings> settings;
    private final Consumer<String> report;
    private final Function<RollType, ActionCheckRoll> roller;

    /**
     * @param actions  logs the checks
     * @param settings supplies the campaign settings that affect checks
     * @param report   adds a line to the daily report
     * @param roller   rolls the dice for opposed checks
     */
    public RoleplayChecks(final OracleActions actions, final Supplier<Settings> settings,
          final Consumer<String> report, final Function<RollType, ActionCheckRoll> roller) {
        this.actions = actions;
        this.settings = settings;
        this.report = report;
        this.roller = roller;
    }

    /**
     * @param campaign the campaign
     * @param actions  the campaign's Oracle actions
     *
     * @return checks for the campaign, reported to its Skill Checks daily report
     */
    public static RoleplayChecks forCampaign(final Campaign campaign, final OracleActions actions) {
        return new RoleplayChecks(actions,
              () -> new Settings(campaign.getCampaignOptions().get(CampaignOption.USE_AGE_EFFECTS),
                    campaign.getPlayerForce().isClanForce(), campaign.getLocalDate(),
                    campaign.getCampaignOptions().get(CampaignOption.USE_EDGE)),
              text -> campaign.addReport(SKILL_CHECKS, text.replace("<p>", "<br><br>").replace("</p>", "")),
              ActionCheckRoll::perform);
    }

    // region Targets

    /**
     * @return {@code true} if the campaign uses Edge
     */
    public boolean isEdgeAllowed() {
        return settings.get().edgeAllowed();
    }

    /**
     * @param person someone in Personnel
     *
     * @return {@code true} if the campaign uses Edge and the person has some left
     */
    public boolean canUseEdge(final @Nullable Person person) {
        return person != null && isEdgeAllowed() && person.getCurrentEdge() > 0;
    }

    /**
     * @param person   the person rolling
     * @param trait    what they roll
     * @param modifier the change to the target number; positive makes it harder
     *
     * @return the target they need
     */
    public CheckTarget target(final Person person, final CheckTrait trait, final int modifier) {
        return targetOf(person, trait, build(person, trait).withMiscModifier(modifier));
    }

    /**
     * @param rating   a character's rating
     * @param modifier the change to the target number; positive makes it harder
     *
     * @return the target a character of that rating needs
     */
    public static CheckTarget target(final NpcRating rating, final int modifier) {
        return CheckTarget.of(rating.getTargetNumber() + modifier);
    }

    /**
     * @param person someone in Personnel
     * @param trait  a skill check
     *
     * @return {@code true} if the person has the skill
     */
    public static boolean isTrained(final Person person, final CheckTrait trait) {
        return !trait.isSkill() || person.hasSkill(trait.skillName());
    }

    private ActionCheck<?> build(final Person person, final CheckTrait trait) {
        if (trait.isSkill()) {
            Settings current = settings.get();
            return person.checkSkill(trait.skillName(), current.useAgingEffects(), current.clanCampaign(),
                  current.date());
        }
        return (trait.second() == null) ? person.checkAttribute(Objects.requireNonNull(trait.first()))
                     : person.checkAttributes(Objects.requireNonNull(trait.first()), trait.second());
    }

    private static CheckTarget targetOf(final Person person, final CheckTrait trait, final ActionCheck<?> check) {
        TargetRoll targetRoll = check.getTargetNumber();
        boolean countUp = trait.isSkill() && SkillType.getType(trait.skillName()).isCountUp();
        Skill skill = trait.isSkill() ? person.getSkill(trait.skillName()) : null;
        RollType rollType = (skill != null && skill.getHasNaturalAptitude()) ? RollType.ADVANTAGE : RollType.NORMAL;
        return new CheckTarget(targetRoll.getValue(), countUp, targetRoll.cannotSucceed(), rollType);
    }

    // endregion Targets

    // region Checks

    /**
     * Makes the same check for one or more people, logs it and adds each result to the daily report.
     *
     * @param people     the people rolling
     * @param trait      what they roll
     * @param difficulty how hard the situation makes it
     * @param other      any other change to the target number; positive makes it harder
     * @param useEdge    {@code true} to re-roll a failure with Edge, for those who have any
     * @param reason     what the check is for, or an empty string
     * @param cast       the campaign's cast, to tag the entry with anyone in it
     * @param thread     the thread the check belongs to, or {@code null}
     *
     * @return the check's result
     */
    public CheckRecord check(final List<Person> people, final CheckTrait trait, final CheckDifficulty difficulty,
          final int other, final boolean useEdge, final String reason, final List<OracleCharacter> cast,
          final @Nullable PlotThread thread) {
        final String trimmedReason = (reason == null) ? "" : reason.strip();
        final String action = describeAction(trait.getLabel(), difficulty, other);
        final List<CheckRecord.Side> sides = new ArrayList<>();
        final List<OracleCharacter> tagged = new ArrayList<>();
        for (Person person : people) {
            ActionCheck<?> check = build(person, trait).withMiscModifier(difficulty.getModifier() + other);
            CheckTarget target = targetOf(person, trait, check);
            // The daily report is HTML, so what the player typed is escaped there.
            ActionCheckResult result = check.resolve(useEdge && isEdgeAllowed(),
                  trimmedReason.isEmpty() ? null : JournalText.escapeHtml(trimmedReason));
            report.accept(result.getReport());
            sides.add(new CheckRecord.Side(person.getFullName(), person.getId(), action, target.describe(),
                  result.getRollResult(), result.getRoll().individualDice(), result.getMarginOfSuccess(),
                  result.hasUsedEdge(), result.isSuccess()));
            OracleCharacter character = castMember(cast, person.getId());
            if (character != null) {
                tagged.add(character);
            }
        }
        final CheckRecord record = new CheckRecord(false, trimmedReason, List.copyOf(sides));
        actions.logCheck(record, tagged, thread);
        return record;
    }

    /**
     * Makes an opposed check, logs it and adds it to the daily report. Both sides roll against their own target and
     * the higher margin of success wins; the defender wins a tie. Whoever is losing then re-rolls once with Edge if
     * they chose to and have some. A rated character's rating is remembered on their cast entry.
     *
     * @param acting    the side acting
     * @param defending the side being acted against
     * @param reason    what the check is for, or an empty string
     * @param cast      the campaign's cast, to tag the entry with anyone in it
     * @param thread    the thread the check belongs to, or {@code null}
     *
     * @return the check's result
     */
    public CheckRecord opposed(final Opponent acting, final Opponent defending, final String reason,
          final List<OracleCharacter> cast, final @Nullable PlotThread thread) {
        final String trimmedReason = (reason == null) ? "" : reason.strip();
        final CheckTarget actingTarget = targetOf(acting);
        final CheckTarget defendingTarget = targetOf(defending);
        ActionCheckRoll actingRoll = roller.apply(actingTarget.rollType());
        ActionCheckRoll defendingRoll = roller.apply(defendingTarget.rollType());
        boolean actingWins = actingTarget.opposedMargin(actingRoll.result())
                                   > defendingTarget.opposedMargin(defendingRoll.result());

        boolean actingEdge = false;
        boolean defendingEdge = false;
        // Edge is only spent when a re-roll could change the outcome: the acting side must be able to beat the
        // defender's margin, and the defender to at least tie the acting side's.
        final long actingMargin = actingTarget.opposedMargin(actingRoll.result());
        final long defendingMargin = defendingTarget.opposedMargin(defendingRoll.result());
        if (!actingWins && rerolls(acting, actingTarget) && actingTarget.bestMargin() > defendingMargin) {
            actingRoll = roller.apply(actingTarget.rollType());
            actingEdge = true;
            Objects.requireNonNull(acting.person()).spendEdge();
        } else if (actingWins && rerolls(defending, defendingTarget)
                         && defendingTarget.bestMargin() >= actingMargin) {
            defendingRoll = roller.apply(defendingTarget.rollType());
            defendingEdge = true;
            Objects.requireNonNull(defending.person()).spendEdge();
        }
        actingWins = actingTarget.opposedMargin(actingRoll.result())
                           > defendingTarget.opposedMargin(defendingRoll.result());

        final CheckRecord record = new CheckRecord(true, trimmedReason, List.of(
              side(acting, actingTarget, actingRoll, actingEdge, actingWins),
              side(defending, defendingTarget, defendingRoll, defendingEdge, !actingWins)));

        final List<OracleCharacter> tagged = new ArrayList<>();
        for (Opponent opponent : List.of(acting, defending)) {
            OracleCharacter character = (opponent.character() != null) ? opponent.character()
                                              : (opponent.person() == null) ? null
                                                      : castMember(cast, opponent.person().getId());
            if (character != null && !tagged.contains(character)) {
                tagged.add(character);
            }
            if (opponent.rating() != null && opponent.character() != null) {
                opponent.character().setRating(opponent.rating());
            }
        }
        actions.logCheck(record, tagged, thread);
        report.accept(getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.check.report",
              JournalText.escapeHtml(OracleActions.describeCheck(record)).replace("\n", "<br>")));
        return record;
    }

    private CheckTarget targetOf(final Opponent opponent) {
        final int modifier = opponent.difficulty().getModifier() + opponent.other();
        if (opponent.person() != null && opponent.trait() != null) {
            return target(opponent.person(), opponent.trait(), modifier);
        }
        return target(Objects.requireNonNullElse(opponent.rating(), NpcRating.REGULAR), modifier);
    }

    private boolean rerolls(final Opponent opponent, final CheckTarget target) {
        return opponent.useEdge() && canUseEdge(opponent.person()) && !target.impossible();
    }

    private static CheckRecord.Side side(final Opponent opponent, final CheckTarget target, final ActionCheckRoll roll,
          final boolean usedEdge, final boolean won) {
        String label = (opponent.trait() != null && opponent.person() != null) ? opponent.trait().getLabel()
                             : Objects.requireNonNullElse(opponent.rating(), NpcRating.REGULAR).getLabel();
        UUID personId = (opponent.person() == null) ? null : opponent.person().getId();
        return new CheckRecord.Side(opponent.name(), personId,
              describeAction(label, opponent.difficulty(), opponent.other()), target.describe(), roll.result(),
              roll.individualDice(), recordedMargin(target, roll.result()), usedEdge, won);
    }

    /**
     * @return the margin an opposed side records: uncapped, as it settled the check, unless the check was impossible
     */
    private static int recordedMargin(final CheckTarget target, final int roll) {
        return target.impossible() ? target.margin(roll)
                     : (int) Math.clamp(target.opposedMargin(roll), Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * @param label      what is rolled, such as "Negotiation"
     * @param difficulty how hard the situation makes it
     * @param other      any other modifier
     *
     * @return the action as the log shows it, such as "Negotiation (Hard +2, other -1)"
     */
    static String describeAction(final String label, final CheckDifficulty difficulty, final int other) {
        String conditions = difficulty.getLabel() + (difficulty.getModifier() == 0 ? ""
                                                           : " " + difficulty.getSignedModifier());
        if (other != 0) {
            conditions = getFormattedTextAt(RESOURCE_BUNDLE, "OracleLog.check.other", conditions,
                  CheckDifficulty.signed(other));
        }
        return label + " (" + conditions + ")";
    }

    private static @Nullable OracleCharacter castMember(final List<OracleCharacter> cast, final UUID personId) {
        for (OracleCharacter character : cast) {
            if (personId.equals(character.getPersonId())) {
                return character;
            }
        }
        return null;
    }

    // endregion Checks
}
