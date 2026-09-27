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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.IntUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;

/**
 * A dice expression such as {@code 2d6+1}, {@code d100}, {@code 4d6kh3} or {@code 1d20+1d4-2}, for rules from other
 * games. Each term is a number, or dice written {@code NdS}: N dice (1 if left out) of S sides, where {@code d%} means
 * {@code d100}. Dice may keep only their highest or lowest few with {@code khK} or {@code klK}. Terms are joined with
 * {@code +} or {@code -}.
 */
public final class DiceExpression {
    public static final int MAXIMUM_DICE = 100;
    public static final int MAXIMUM_SIDES = 1000;
    public static final int MAXIMUM_TERMS = 10;
    private static final int MAXIMUM_LENGTH = 100;

    private static final Pattern TERM = Pattern.compile(
          "([+-])?(?:(\\d*)d(\\d+|%)(?:k([hl])(\\d+))?|(\\d+))");

    /**
     * One part of an expression.
     *
     * @param sign  {@code 1} to add the term, {@code -1} to subtract it
     * @param count how many dice, or {@code 0} for a plain number
     * @param sides how many sides each die has, or the number itself for a plain number
     * @param keep  how many dice count, or {@code 0} to count them all
     * @param high  {@code true} to keep the highest dice, {@code false} the lowest
     */
    public record Term(int sign, int count, int sides, int keep, boolean high) {
        boolean isDice() {
            return count > 0;
        }

        String describe(final boolean first) {
            String text = isDice() ? (count == 1 ? "" : count) + "d" + sides
                                           + (keep > 0 ? "k" + (high ? "h" : "l") + keep : "")
                                : Integer.toString(sides);
            return (first ? (sign < 0 ? "-" : "") : (sign < 0 ? " - " : " + ")) + text;
        }
    }

    /**
     * The dice thrown for one term.
     *
     * @param term  the term
     * @param dice  every die thrown, in the order thrown; empty for a plain number
     * @param kept  the dice that counted
     * @param total the term's value before its sign
     */
    public record TermRoll(Term term, List<Integer> dice, List<Integer> kept, int total) {}

    /**
     * A rolled expression.
     *
     * @param expression the expression as written tidily, such as "2d6 + 1"
     * @param terms      each term's dice
     * @param total      the result
     */
    public record Roll(String expression, List<TermRoll> terms, int total) {
        /**
         * @return the working, such as "[3, 5] + 1"; dropped dice are shown struck with a tilde
         */
        public String describeWorking() {
            StringBuilder working = new StringBuilder();
            for (int index = 0; index < terms.size(); index++) {
                TermRoll roll = terms.get(index);
                if (index > 0 || roll.term().sign() < 0) {
                    working.append(roll.term().sign() < 0 ? (index == 0 ? "-" : " - ") : " + ");
                }
                if (!roll.term().isDice()) {
                    working.append(roll.total());
                    continue;
                }
                List<Integer> unused = new ArrayList<>(roll.kept());
                List<String> faces = new ArrayList<>();
                for (int die : roll.dice()) {
                    faces.add(unused.remove(Integer.valueOf(die)) ? Integer.toString(die) : "~" + die);
                }
                working.append('[').append(String.join(", ", faces)).append(']');
            }
            return working.toString();
        }
    }

    private final List<Term> terms;

    private DiceExpression(final List<Term> terms) {
        this.terms = List.copyOf(terms);
    }

    /**
     * @param text an expression such as "2d6+1"; spaces and letter case do not matter
     *
     * @return the expression
     *
     * @throws IllegalArgumentException if the text is not a valid expression or asks for too many dice
     */
    public static DiceExpression parse(final @Nullable String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("empty");
        }
        // Spaces may surround + and -, but not split a term: "2d6 3" must not be read as 2d63.
        if (text.matches("(?s).*[0-9a-zA-Z%]\\s+[0-9a-zA-Z%].*")) {
            throw new IllegalArgumentException("space inside a term");
        }
        String compact = text.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        if (compact.length() > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException("too long");
        }
        Matcher matcher = TERM.matcher(compact);
        List<Term> terms = new ArrayList<>();
        int position = 0;
        while (position < compact.length()) {
            if (!matcher.find(position) || matcher.start() != position || matcher.end() == position) {
                throw new IllegalArgumentException("unexpected text at " + position);
            }
            if (matcher.group(1) == null && !terms.isEmpty()) {
                throw new IllegalArgumentException("missing + or - before a term");
            }
            int sign = "-".equals(matcher.group(1)) ? -1 : 1;
            if (matcher.group(6) != null) {
                terms.add(new Term(sign, 0, number(matcher.group(6), 0, 100_000), 0, true));
            } else {
                int count = matcher.group(2).isEmpty() ? 1 : number(matcher.group(2), 1, MAXIMUM_DICE);
                int sides = "%".equals(matcher.group(3)) ? 100 : number(matcher.group(3), 2, MAXIMUM_SIDES);
                int keep = (matcher.group(4) == null) ? 0 : number(matcher.group(5), 1, count);
                terms.add(new Term(sign, count, sides, keep, "h".equals(matcher.group(4))));
            }
            if (terms.size() > MAXIMUM_TERMS) {
                throw new IllegalArgumentException("too many terms");
            }
            position = matcher.end();
        }
        if (terms.stream().noneMatch(Term::isDice)) {
            throw new IllegalArgumentException("no dice");
        }
        return new DiceExpression(terms);
    }

    /**
     * @param text an expression
     *
     * @return {@code true} if {@link #parse(String)} would accept it
     */
    public static boolean isValid(final @Nullable String text) {
        try {
            parse(text);
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private static int number(final String digits, final int minimum, final int maximum) {
        if (digits.length() > 6) {
            throw new IllegalArgumentException("number too large");
        }
        int value = MathUtility.parseInt(digits, -1);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException("number out of range: " + value);
        }
        return value;
    }

    public List<Term> getTerms() {
        return terms;
    }

    /**
     * @return the expression written tidily, such as "2d6 + 1"
     */
    public String describe() {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < terms.size(); index++) {
            text.append(terms.get(index).describe(index == 0));
        }
        return text.toString();
    }

    /**
     * @param die rolls one die: given its number of sides, returns a value from 1 to that number
     *
     * @return the roll
     */
    public Roll roll(final IntUnaryOperator die) {
        List<TermRoll> rolls = new ArrayList<>();
        int total = 0;
        for (Term term : terms) {
            if (!term.isDice()) {
                rolls.add(new TermRoll(term, List.of(), List.of(), term.sides()));
                total += term.sign() * term.sides();
                continue;
            }
            List<Integer> dice = new ArrayList<>();
            for (int index = 0; index < term.count(); index++) {
                dice.add(die.applyAsInt(term.sides()));
            }
            List<Integer> kept = new ArrayList<>(dice);
            if (term.keep() > 0) {
                kept.sort(term.high() ? Comparator.reverseOrder() : Comparator.naturalOrder());
                kept = new ArrayList<>(kept.subList(0, term.keep()));
                Collections.sort(kept);
            }
            int value = kept.stream().mapToInt(Integer::intValue).sum();
            rolls.add(new TermRoll(term, List.copyOf(dice), List.copyOf(kept), value));
            total += term.sign() * value;
        }
        return new Roll(describe(), List.copyOf(rolls), total);
    }
}
