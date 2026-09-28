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
package mekhq.campaign.mission.utilities;

import java.time.LocalDate;

import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConStrategicObjective;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.mission.contract.utilities.ContractScore;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioStatus;

/**
 * A read-only snapshot of a completed contract's performance, gathered for the {@code CompleteMissionDialog} debrief.
 *
 * <p>It rolls up the contract's resolved scenarios into a victory/draw/defeat record, reads the StratCon strategic
 * objectives where they exist, exposes the contract ledger totals, and - the point of the class - recommends the
 * {@link MissionStatus} the contract's performance implies so the dialog can pre-select it while still letting the
 * player override.</p>
 *
 * <p>The recommendation follows the game's own success rule: the canonical contract score
 * ({@link ContractScore#getContractScore(boolean, AbstractContract)}) must reach the required victory points, exactly
 * as {@code CampaignNewDayManager} tests before ending a contract early. Ending a contract before its end date without
 * meeting that target is a breach, not a success - completing the strategic objectives does not by itself carry the
 * contract.</p>
 *
 * <p>Nothing here mutates the contract; build one with {@link #from(AbstractContract, boolean, LocalDate)} and read
 * the getters.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ContractDebriefStatistics {
    private final int resolvedScenarios;
    private final int victories;
    private final int decisiveVictories;
    private final int draws;
    private final int defeats;
    private final int decisiveDefeats;

    private final boolean hasStratConObjectives;
    private final int objectivesCompleted;
    private final int objectivesFailed;
    private final int objectivesTotal;

    private final int contractScore;
    private final int requiredVictoryPoints;
    private final boolean hasVictoryPointTarget;
    private final boolean endedEarly;

    private final Money totalContractPay;
    private final Money salvageWonValue;

    private final MissionStatus recommendedStatus;

    private ContractDebriefStatistics(AbstractContract contract, boolean useMaplessMode, LocalDate today) {
        int resolved = 0;
        int overallVictories = 0;
        int decisiveWins = 0;
        int drawCount = 0;
        int overallDefeats = 0;
        int decisiveLosses = 0;
        for (Scenario scenario : contract.getScenarios()) {
            ScenarioStatus status = scenario.getStatus();
            if (status.isCurrent()) {
                continue;
            }

            resolved++;
            if (status.isOverallVictory()) {
                overallVictories++;
                if (status.isDecisiveVictory()) {
                    decisiveWins++;
                }
            } else if (status.isOverallDefeat()) {
                overallDefeats++;
                if (status.isDecisiveDefeat()) {
                    decisiveLosses++;
                }
            } else if (status.isDraw()) {
                drawCount++;
            }
        }
        this.resolvedScenarios = resolved;
        this.victories = overallVictories;
        this.decisiveVictories = decisiveWins;
        this.draws = drawCount;
        this.defeats = overallDefeats;
        this.decisiveDefeats = decisiveLosses;

        int completed = 0;
        int failed = 0;
        int total = 0;
        StratConCampaignState stratConState = contract.getStratConCampaignState();
        if (stratConState != null) {
            for (StratConTrackState track : stratConState.getTracks()) {
                for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
                    total++;
                    if (objective.isObjectiveCompleted(track)) {
                        completed++;
                    } else if (objective.isObjectiveFailed(track)) {
                        failed++;
                    }
                }
            }
        }
        this.objectivesTotal = total;
        this.objectivesCompleted = completed;
        this.objectivesFailed = failed;
        this.hasStratConObjectives = total > 0;

        // The canonical contract score: StratCon victory points when tracked, otherwise the aggregate scenario score.
        this.contractScore = ContractScore.getContractScore(useMaplessMode, contract);
        this.requiredVictoryPoints = contract.getRequiredVictoryPoints();
        this.hasVictoryPointTarget = requiredVictoryPoints > 0;

        // The player is ending the contract early if its end date is known and still in the future.
        LocalDate endDate = contract.getEndingDate();
        this.endedEarly = (endDate != null) && (today != null) && today.isBefore(endDate);

        this.totalContractPay = contract.getTotalPay();
        this.salvageWonValue = contract.getSalvagedByUnitValue();

        this.recommendedStatus = evaluateRecommendedStatus();
    }

    /**
     * @param contract        the completed contract to summarise
     * @param useMaplessMode  whether StratCon mapless mode is enabled, which changes how the contract score is derived
     * @param today           the campaign's current date, used to detect an early contract termination
     *
     * @return a fresh snapshot of the contract's combat record, objectives, ledger, and recommended outcome
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static ContractDebriefStatistics from(AbstractContract contract, boolean useMaplessMode, LocalDate today) {
        return new ContractDebriefStatistics(contract, useMaplessMode, today);
    }

    /**
     * Recommends the {@link MissionStatus} the contract's performance implies.
     *
     * <p>The win condition mirrors the game's own: the contract score must reach the required victory points (for a
     * contract without a positive target, a non-negative score, or no fighting at all, stands in). When that is met the
     * contract is a success, whether it runs to term or ends early. When it is not met:</p>
     *
     * <ul>
     *   <li>ending the contract <em>early</em> is a {@link MissionStatus#BREACH} - its terms were abandoned before they
     *   were fulfilled, regardless of how the strategic objectives fared;</li>
     *   <li>running to term but falling short is a {@link MissionStatus#PARTIAL} where there was some progress (a
     *   positive score or at least one strategic objective secured), otherwise a {@link MissionStatus#FAILED}.</li>
     * </ul>
     *
     * @return the recommended status; never {@link MissionStatus#ACTIVE}
     *
     * @author Illiani
     * @since 0.51.01
     */
    private MissionStatus evaluateRecommendedStatus() {
        boolean meetsTarget;
        if (hasVictoryPointTarget) {
            meetsTarget = contractScore >= requiredVictoryPoints;
        } else if (resolvedScenarios == 0) {
            // No target and nothing fought: nothing was lost, so treat the contract as fulfilled.
            meetsTarget = true;
        } else {
            meetsTarget = contractScore >= 0;
        }

        if (meetsTarget) {
            return MissionStatus.SUCCESS;
        }

        // The contract's terms were not met.
        if (endedEarly) {
            return MissionStatus.BREACH;
        }

        boolean anyProgress = (contractScore > 0) || (hasStratConObjectives && (objectivesCompleted > 0));
        return anyProgress ? MissionStatus.PARTIAL : MissionStatus.FAILED;
    }

    /**
     * @return the whole-number percentage of resolved scenarios that ended in an overall victory, or {@code 0} when no
     *       scenarios were resolved
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int getWinRatePercent() {
        if (resolvedScenarios == 0) {
            return 0;
        }
        return (int) Math.round((victories * 100.0) / resolvedScenarios);
    }

    public int getResolvedScenarios() {
        return resolvedScenarios;
    }

    public int getVictories() {
        return victories;
    }

    public int getDecisiveVictories() {
        return decisiveVictories;
    }

    public int getDraws() {
        return draws;
    }

    public int getDefeats() {
        return defeats;
    }

    public int getDecisiveDefeats() {
        return decisiveDefeats;
    }

    public boolean hasStratConObjectives() {
        return hasStratConObjectives;
    }

    public int getObjectivesCompleted() {
        return objectivesCompleted;
    }

    public int getObjectivesFailed() {
        return objectivesFailed;
    }

    public int getObjectivesTotal() {
        return objectivesTotal;
    }

    /**
     * @return the canonical contract score - StratCon victory points when tracked, otherwise the aggregate scenario
     *       score - which is what the required-victory-point target is measured against
     */
    public int getContractScore() {
        return contractScore;
    }

    public int getRequiredVictoryPoints() {
        return requiredVictoryPoints;
    }

    public boolean hasVictoryPointTarget() {
        return hasVictoryPointTarget;
    }

    /** @return {@code true} if the target was met, i.e. the score reached the required victory points */
    public boolean isVictoryPointTargetMet() {
        return contractScore >= requiredVictoryPoints;
    }

    /** @return {@code true} if the player is ending the contract before its end date */
    public boolean isEndedEarly() {
        return endedEarly;
    }

    public Money getTotalContractPay() {
        return totalContractPay;
    }

    public Money getSalvageWonValue() {
        return salvageWonValue;
    }

    public MissionStatus getRecommendedStatus() {
        return recommendedStatus;
    }
}
