package mekhq.campaign.digitalGM.stratCon.deployment;

/**
 * The odds of a single reinforcement attempt: the final target number the roll must meet on two six-sided dice, and
 * the resulting success probability. Produced by {@link DeploymentEvaluator} so the inspector can show a force's real
 * chance of arriving before the player commits to it.
 *
 * @param finalTargetNumber  the number the 2d6 roll must meet or beat, after support-point and contract modifiers
 * @param successProbability the probability, in {@code [0.0, 1.0]}, of meeting that target
 *
 * @author Illiani
 * @since 0.51.01
 */
public record ReinforcementRoll(int finalTargetNumber, double successProbability) {}
