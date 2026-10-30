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
package mekhq.campaign.mission.contract.io;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.annotation.Nullable;
import megamek.client.ui.util.PlayerColour;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ChaosActOfPiracy;
import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.contract.ChaosGameWorlds;
import mekhq.campaign.mission.contract.contractData.*;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.mission.contract.io.ContractNpcJson.IconJson;

/**
 * The JSON form of one contract offer, as written by {@link ContractExchange}.
 *
 * <p>Only the offer itself is carried: what the employer is proposing, and to whom. Everything that belongs to the
 * campaign holding the offer is left out - its id, mission status, the player's chosen negotiator, any negotiation
 * already under way, salvage and support tallies, StratCon state and scenarios - so the receiving campaign gets a clean
 * offer, the same as one fresh from the market. The NPCs travel as {@link ContractNpcJson} snapshots.</p>
 *
 * <p>Enum values are written by name. An unknown name (from a newer build, say) reads as {@code null} and is replaced
 * by a safe default, so one unrecognized value never costs the player the whole offer.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public record ContractOfferJson(@Nullable String type, @Nullable ContractSearchType searchType, String name,
      boolean nameIsOperationCodename, String description, double scale, double salvageTaperMultiplier,
      int requiredVictoryPoints, int trackCount, List<Integer> scenarioSchedule, @Nullable ContractNature nature,
      int sharesPercent, Set<ObfuscatableIntel> obfuscatedIntel, Set<ContractCharacteristic> characteristics,
      @Nullable EmployerJson employer, @Nullable EnemyJson enemy, @Nullable TermsJson terms,
      @Nullable ContractObjectiveType playerObjective, @Nullable ContractObjectiveType opposingObjective,
      @Nullable FinanceJson finance, @Nullable ScheduleJson schedule, @Nullable String targetSystemId,
      @Nullable String targetPlanetId, @Nullable RentedFacilitiesData rentedFacilities,
      @Nullable NonNegotiableTermsData nonNegotiableTerms, @Nullable ContractMoraleLevel morale) {

    /**
     * The concrete contract classes an offer may be, keyed by the name written to the file. Restricting the type to
     * this list (rather than instantiating any class name the file names) keeps a shared file from constructing
     * arbitrary objects.
     */
    private static final Map<String, Supplier<AbstractContract>> CONTRACT_TYPES = Map.of(
          ChaosContract.class.getSimpleName(), ChaosContract::new,
          ChaosActOfPiracy.class.getSimpleName(), ChaosActOfPiracy::new,
          ChaosGameWorlds.class.getSimpleName(), ChaosGameWorlds::new);

    /**
     * The employer, and the NPCs representing them.
     */
    public record EmployerJson(@Nullable ChaosEmployerType type, String factionCode, String anchorFactionCode,
          @Nullable String sponsorFactionCode, String displayName, @Nullable SkillLevel forceSkill,
          int equipmentRating, @Nullable IconJson camouflage, @Nullable CamouflageJson camouflageTransform,
          @Nullable PlayerColour color, @Nullable ContractNpcJson negotiator, @Nullable ContractNpcJson liaison) {}

    /**
     * The opposition, and the NPC commanding it.
     */
    public record EnemyJson(String factionCode, @Nullable String sponsorFactionCode, String displayName,
          @Nullable SkillLevel forceSkill, int equipmentRating, @Nullable IconJson camouflage,
          @Nullable CamouflageJson camouflageTransform, @Nullable PlayerColour color, boolean batchallAccepted,
          @Nullable ContractNpcJson opposingCommander) {}

    /**
     * How a camouflage is laid onto units.
     *
     * @param rotationAngle the rotation, in degrees
     * @param scale         the scale times 10 (10 is unscaled)
     */
    public record CamouflageJson(int rotationAngle, int scale) {}

    /**
     * The step each negotiable term sits at.
     */
    public record TermsJson(@Nullable ChaosContractStepsTable payRate, @Nullable ChaosContractStepsTable support,
          @Nullable ChaosContractStepsTable transport, @Nullable ChaosContractStepsTable salvageRights,
          @Nullable ChaosContractStepsTable commandRights) {}

    /**
     * The offer's payments, in the default currency.
     */
    public record FinanceJson(@Nullable BigDecimal transport, @Nullable BigDecimal monthlyPay,
          @Nullable BigDecimal combatPay) {}

    /**
     * When the offer runs.
     */
    public record ScheduleJson(@Nullable LocalDate startDate, @Nullable LocalDate endDate, int lengthInMonths) {}

    // region Export

    /**
     * Captures an offer.
     *
     * @param contract   the offer
     * @param searchType the market board the offer is listed on
     *
     * @return the offer's JSON form
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static ContractOfferJson from(AbstractContract contract, ContractSearchType searchType) {
        ContractTermsData terms = contract.getContractTerms();
        ContractObjectiveData objectives = contract.getObjectiveData();
        ContractFinanceData finance = contract.getContractFinanceData();
        ContractScheduleData schedule = contract.getScheduleData();
        SystemsTargetData target = contract.getSystemsTargetData();
        MoraleData morale = contract.getMoraleData();

        return new ContractOfferJson(contract.getClass().getSimpleName(),
              searchType,
              contract.getName(),
              contract.isNameOperationCodename(),
              contract.getDescription(),
              contract.getExactScale(),
              contract.getSalvageTaperMultiplier(),
              contract.getRequiredVictoryPoints(),
              contract.getTrackCount(),
              List.copyOf(contract.getScenarioSchedule()),
              contract.getNature(),
              contract.getSharesPercent(),
              Set.copyOf(contract.getObfuscatedIntel()),
              Set.copyOf(contract.getCharacteristics()),
              employer(contract.getEmployerData()),
              enemy(contract.getEnemyData()),
              terms == null ? null : new TermsJson(terms.payRate(), terms.support(), terms.transport(),
                    terms.salvageRights(), terms.commandRights()),
              objectives == null ? null : objectives.playerObjectiveType(),
              objectives == null ? null : objectives.opposingObjectiveType(),
              finance == null ? null : new FinanceJson(amount(finance.transport()), amount(finance.monthlyPay()),
                    amount(finance.combatPay())),
              schedule == null ? null : new ScheduleJson(schedule.startDate(), schedule.endDate(),
                    schedule.lengthInMonths()),
              target == null ? null : target.systemId(),
              target == null ? null : target.planetId(),
              contract.getRentedFacilitiesData(),
              contract.getNonNegotiableTermsData(),
              morale == null ? null : morale.moraleLevel());
    }

    private static @Nullable EmployerJson employer(@Nullable EmployerData data) {
        if (data == null) {
            return null;
        }
        return new EmployerJson(data.type(), data.factionCode(), data.anchorFactionCode(), data.sponsorFactionCode(),
              data.displayName(), data.forceSkill(), data.equipmentRating(), icon(data.camouflage()),
              transform(data.camouflage()), data.color(), ContractNpcJson.from(data.negotiator()),
              ContractNpcJson.from(data.liaison()));
    }

    private static @Nullable EnemyJson enemy(@Nullable EnemyData data) {
        if (data == null) {
            return null;
        }
        return new EnemyJson(data.factionCode(), data.sponsorFactionCode(), data.displayName(), data.forceSkill(),
              data.equipmentRating(), icon(data.camouflage()), transform(data.camouflage()), data.color(),
              data.batchallAccepted(), ContractNpcJson.from(data.opposingCommander()));
    }

    private static @Nullable IconJson icon(@Nullable Camouflage camouflage) {
        return camouflage == null ? null : new IconJson(camouflage.getCategory(), camouflage.getFilename());
    }

    private static @Nullable CamouflageJson transform(@Nullable Camouflage camouflage) {
        return camouflage == null ? null : new CamouflageJson(camouflage.getRotationAngle(), camouflage.getScale());
    }

    private static @Nullable BigDecimal amount(@Nullable Money money) {
        return money == null ? null : money.getAmount();
    }

    // endregion Export

    // region Import

    /**
     * Rebuilds the offer for the given campaign. The offer gets a fresh id and no status, and its NPCs are rebuilt from
     * their snapshots.
     *
     * @param campaign the campaign the offer is being imported into
     *
     * @return the rebuilt offer, or {@code null} if the file names a contract type this build does not have
     *
     * @author Illiani
     * @since 0.51.01
     */
    public @Nullable AbstractContract toContract(Campaign campaign) {
        Supplier<AbstractContract> constructor = type == null ? null : CONTRACT_TYPES.get(type);
        if (constructor == null) {
            return null;
        }

        AbstractContract contract = constructor.get();
        contract.setCampaignOptions(campaign.getCampaignOptions());
        contract.setContractId(UUID.randomUUID());
        contract.setStatus(null);
        contract.setContractName(Objects.requireNonNullElse(name, ""));
        contract.setNameOperationCodename(nameIsOperationCodename);
        contract.setDescription(Objects.requireNonNullElse(description, ""));
        contract.setScale(scale);
        contract.setSalvageTaperMultiplier(salvageTaperMultiplier <= 0 ? 1.0 : salvageTaperMultiplier);
        contract.setRequiredVictoryPoints(requiredVictoryPoints);
        contract.setTrackCount(trackCount);
        contract.setScenarioSchedule(withoutNulls(scenarioSchedule));
        contract.setNature(Objects.requireNonNullElse(nature, ContractNature.NORMAL));
        contract.setSharesPercent(sharesPercent);
        contract.setObfuscatedIntel(enumSet(ObfuscatableIntel.class, obfuscatedIntel));
        contract.setCharacteristics(enumSet(ContractCharacteristic.class, characteristics));

        if (employer != null) {
            contract.setEmployerData(new EmployerData(
                  Objects.requireNonNullElse(employer.type(), ChaosEmployerType.LOCAL_SYSTEM_OWNER),
                  employer.factionCode(),
                  employer.anchorFactionCode(),
                  employer.sponsorFactionCode(),
                  employer.displayName(),
                  employer.negotiator() == null ? null : employer.negotiator().toPerson(campaign),
                  employer.liaison() == null ? null : employer.liaison().toPerson(campaign),
                  Objects.requireNonNullElse(employer.forceSkill(), SkillLevel.REGULAR),
                  employer.equipmentRating(),
                  camouflage(employer.camouflage(), employer.camouflageTransform()),
                  Objects.requireNonNullElse(employer.color(), PlayerColour.BLUE)));
        }
        if (enemy != null) {
            contract.setEnemyData(new EnemyData(enemy.factionCode(),
                  enemy.sponsorFactionCode(),
                  enemy.displayName(),
                  Objects.requireNonNullElse(enemy.forceSkill(), SkillLevel.REGULAR),
                  enemy.equipmentRating(),
                  enemy.opposingCommander() == null ? null : enemy.opposingCommander().toPerson(campaign),
                  camouflage(enemy.camouflage(), enemy.camouflageTransform()),
                  Objects.requireNonNullElse(enemy.color(), PlayerColour.RED),
                  enemy.batchallAccepted()));
        }
        if (terms != null) {
            contract.setContractTerms(new ContractTermsData(step(terms.payRate()), step(terms.support()),
                  step(terms.transport()), step(terms.salvageRights()), step(terms.commandRights())));
        }
        contract.setObjectiveData(new ContractObjectiveData(
              Objects.requireNonNullElse(playerObjective, ContractObjectiveType.GARRISON_DUTY),
              Objects.requireNonNullElse(opposingObjective, ContractObjectiveType.UNDEFINED)));
        if (finance != null) {
            contract.setContractFinanceData(new ContractFinanceData(money(finance.transport()),
                  money(finance.monthlyPay()), money(finance.combatPay())));
        }
        if (schedule != null) {
            contract.setScheduleData(new ContractScheduleData(schedule.startDate(), schedule.endDate(),
                  schedule.lengthInMonths()));
        }
        contract.setSystemsTargetData(new SystemsTargetData(targetSystemId, targetPlanetId));
        contract.setRentedFacilitiesData(Objects.requireNonNullElse(rentedFacilities,
              new RentedFacilitiesData(0, 0, 0)));
        if ((nonNegotiableTerms != null) && nonNegotiableTerms.anyLocked()) {
            contract.setNonNegotiableTermsData(nonNegotiableTerms);
        }
        contract.setMoraleData(new MoraleData(Objects.requireNonNullElse(morale, ContractMoraleLevel.STALEMATE)));

        return contract;
    }

    private static Camouflage camouflage(@Nullable IconJson icon, @Nullable CamouflageJson transform) {
        if (icon == null) {
            return new Camouflage();
        }
        Camouflage camouflage = new Camouflage(icon.category(), icon.filename());
        if (transform != null) {
            camouflage.setRotationAngle(transform.rotationAngle());
            camouflage.setScale(transform.scale());
        }
        return camouflage;
    }

    private static ChaosContractStepsTable step(@Nullable ChaosContractStepsTable step) {
        return Objects.requireNonNullElse(step, ChaosContractStepsTable.STEP_SEVEN);
    }

    private static Money money(@Nullable BigDecimal amount) {
        return amount == null ? Money.zero() : Money.of(amount.doubleValue());
    }

    private static <E extends Enum<E>> EnumSet<E> enumSet(Class<E> type, @Nullable Set<E> values) {
        EnumSet<E> set = EnumSet.noneOf(type);
        if (values != null) {
            // An unrecognized name reads as a null entry; drop it rather than fail.
            values.stream().filter(Objects::nonNull).forEach(set::add);
        }
        return set;
    }

    private static List<Integer> withoutNulls(@Nullable List<Integer> values) {
        List<Integer> result = new ArrayList<>();
        if (values != null) {
            values.stream().filter(Objects::nonNull).forEach(result::add);
        }
        return result;
    }

    // endregion Import
}
