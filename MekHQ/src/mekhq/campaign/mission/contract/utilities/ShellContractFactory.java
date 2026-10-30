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
package mekhq.campaign.mission.contract.utilities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nullable;
import megamek.client.ui.util.PlayerColour;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.ShellContract;
import mekhq.campaign.mission.contract.contractData.*;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.personnel.Person;

/**
 * Builds and updates {@link ShellContract}s from the handful of details the player enters for one.
 *
 * <p>Every data record a contract carries is populated, so a shell contract can be shown, saved, and completed
 * anywhere a generated contract can. The terms the player does not choose are pinned to values that keep the rest of
 * the contract automation quiet: no straight support, no battle loss compensation, and no transport reimbursement.
 * The pay rate step is left at the neutral 1.0 multiplier, as the shell's pay is entered directly rather than derived
 * from it. Salvage and command rights are the player's to choose, and default to the least restrictive terms: full
 * salvage and independent command.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ShellContractFactory {
    /** Pay rate step for a shell contract: the neutral 1.0 multiplier, since its pay is entered directly. */
    static final ChaosContractStepsTable PAY_RATE_STEP = ChaosContractStepsTable.STEP_SEVEN;
    /** Support and transport step for a shell contract: no straight support, battle loss compensation, or transport */
    static final ChaosContractStepsTable NO_SUPPORT_OR_TRANSPORT_STEP = ChaosContractStepsTable.STEP_ONE;
    /** Salvage rights a new shell contract starts with: 100% salvage, not exchange */
    public static final ChaosContractStepsTable DEFAULT_SALVAGE_RIGHTS_STEP = ChaosContractStepsTable.STEP_THIRTEEN;
    /** Command rights a new shell contract starts with */
    public static final ContractCommandRights DEFAULT_COMMAND_RIGHTS = ContractCommandRights.INDEPENDENT;

    private ShellContractFactory() {}

    /**
     * The details the player supplies for a shell contract.
     *
     * @param name              the contract's name
     * @param description       the contract's description; may be blank
     * @param objectiveType     what the contract is for; flavor only, as nothing is generated from it
     * @param employerCode      the employer's faction code
     * @param employerName      the employer's display name
     * @param enemyCode         the enemy's faction code
     * @param enemyName         the enemy's display name
     * @param systemId          the target system's id, or {@code null} when the contract names none
     * @param planetId          the target planet's id, or {@code null} when the contract names none
     * @param startDate         the day the contract begins
     * @param lengthInMonths    the contract's length in months; ignored when {@code openEnded}
     * @param openEnded         whether the contract runs until the player completes it, with no end date
     * @param monthlyPay        the pay credited at the start of each month the contract is active
     * @param salvageRightsStep the step that sets the contract's salvage rights
     * @param commandRights     the contract's command rights
     */
    public record ShellContractDetails(String name, String description, ContractObjectiveType objectiveType,
          String employerCode, String employerName, String enemyCode, String enemyName, @Nullable String systemId,
          @Nullable String planetId, LocalDate startDate, int lengthInMonths, boolean openEnded, Money monthlyPay,
          ChaosContractStepsTable salvageRightsStep, ContractCommandRights commandRights) {}

    /**
     * Creates a new, active shell contract from the player's details.
     *
     * @param details   the player's details
     * @param liaison   the employer's liaison, or {@code null} if one could not be generated
     * @param commander the opposing commander, or {@code null} if one could not be generated
     *
     * @return the new contract, not yet added to the campaign
     */
    public static ShellContract create(ShellContractDetails details, @Nullable Person liaison,
          @Nullable Person commander) {
        ShellContract contract = new ShellContract();
        contract.setContractId(UUID.randomUUID());
        contract.setStatus(MissionStatus.ACTIVE);
        contract.setScale(1);
        contract.setRentedFacilitiesData(new RentedFacilitiesData(0, 0, 0));

        contract.setEmployerData(new EmployerData(ChaosEmployerType.LOCAL_SYSTEM_OWNER, details.employerCode(),
              details.employerCode(), null, details.employerName(), null, liaison, SkillLevel.REGULAR, 0,
              new Camouflage(), PlayerColour.BLUE));
        contract.setEnemyData(new EnemyData(details.enemyCode(), null, details.enemyName(), SkillLevel.REGULAR, 0,
              commander, new Camouflage(), PlayerColour.RED, true));

        applyDetails(contract, details);
        return contract;
    }

    /**
     * Writes the player's details onto a shell contract, keeping everything they do not cover - the employer's and
     * enemy's NPCs, skill, equipment, and camouflage, and the contract's status and scenarios - as it stands. When the
     * employer or enemy faction changes, its NPC is replaced with the one supplied.
     *
     * @param contract  the contract to update
     * @param details   the player's details
     * @param liaison   the employer's liaison to use if the employer faction changed
     * @param commander the opposing commander to use if the enemy faction changed
     */
    public static void update(ShellContract contract, ShellContractDetails details, @Nullable Person liaison,
          @Nullable Person commander) {
        EmployerData employer = contract.getEmployerData();
        Person keptLiaison = employer.factionCode().equals(details.employerCode()) ? employer.liaison() : liaison;
        contract.setEmployerData(new EmployerData(employer.type(), details.employerCode(), details.employerCode(),
              employer.sponsorFactionCode(), details.employerName(), employer.negotiator(), keptLiaison,
              employer.forceSkill(), employer.equipmentRating(), employer.camouflage(), employer.color()));

        EnemyData enemy = contract.getEnemyData();
        Person keptCommander = enemy.factionCode().equals(details.enemyCode()) ? enemy.opposingCommander() : commander;
        contract.setEnemyData(new EnemyData(details.enemyCode(), enemy.sponsorFactionCode(), details.enemyName(),
              enemy.forceSkill(), enemy.equipmentRating(), keptCommander, enemy.camouflage(), enemy.color(),
              enemy.batchallAccepted()));

        applyDetails(contract, details);
    }

    private static void applyDetails(ShellContract contract, ShellContractDetails details) {
        contract.setContractName(details.name());
        contract.setDescription(details.description());
        contract.setObjectiveData(new ContractObjectiveData(details.objectiveType(), ContractObjectiveType.UNDEFINED));
        contract.setSystemsTargetData(new SystemsTargetData(details.systemId(), details.planetId()));

        LocalDate startDate = details.startDate();
        contract.setScheduleData(details.openEnded()
                                       ? new ContractScheduleData(startDate, null, 0)
                                       : new ContractScheduleData(startDate,
                                             startDate.plusMonths(details.lengthInMonths()),
                                             details.lengthInMonths()));

        contract.setContractTerms(new ContractTermsData(PAY_RATE_STEP, NO_SUPPORT_OR_TRANSPORT_STEP,
              NO_SUPPORT_OR_TRANSPORT_STEP, details.salvageRightsStep(),
              getCommandRightsStep(details.commandRights())));
        contract.setContractFinanceData(new ContractFinanceData(Money.zero(), details.monthlyPay(), Money.zero()));
    }

    /**
     * @return the steps offered as salvage rights choices: one per distinct salvage outcome, from none to all
     */
    public static List<ChaosContractStepsTable> getSalvageRightsSteps() {
        List<ChaosContractStepsTable> steps = new ArrayList<>();
        for (ChaosContractStepsTable step : ChaosContractStepsTable.values()) {
            boolean duplicatesPrevious = !steps.isEmpty()
                                               && (steps.getLast().getSalvageMultiplier() == step.getSalvageMultiplier())
                                               && (steps.getLast().isExchangeSalvage() == step.isExchangeSalvage());
            if (!duplicatesPrevious) {
                steps.add(step);
            }
        }
        return steps;
    }

    /**
     * @param commandRights the command rights wanted
     *
     * @return the lowest step granting those command rights
     */
    public static ChaosContractStepsTable getCommandRightsStep(ContractCommandRights commandRights) {
        for (ChaosContractStepsTable step : ChaosContractStepsTable.values()) {
            if (step.getContractCommandRights() == commandRights) {
                return step;
            }
        }
        return PAY_RATE_STEP;
    }
}
