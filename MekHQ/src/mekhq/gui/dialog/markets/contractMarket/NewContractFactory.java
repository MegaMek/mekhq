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
package mekhq.gui.dialog.markets.contractMarket;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.UUID;

import megamek.client.ui.util.PlayerColour;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.contract.contractData.*;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * Builds a blank {@link AbstractContract} for the GM "Create new" flow. Every data record is populated with a sensible,
 * fully-initialized default so the editor can present the contract without any missing sections, and so it is a valid
 * offer the moment it is added to the market. The game master then reshapes it in {@link ContractEditorDialog}.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class NewContractFactory {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosContractMarketDialog";

    private static final int DEFAULT_LENGTH_MONTHS = 6;
    /** A neutral mid-table step (1.0 base pay, House command) used as the starting value for every negotiable term. */
    private static final ChaosContractStepsTable DEFAULT_STEP = ChaosContractStepsTable.STEP_SEVEN;

    private NewContractFactory() {}

    /**
     * Creates a fully-initialized blank contract seeded from the campaign's current faction, location, and date. Its
     * NPCs are intentionally left unset: they are generated (from the GM's final faction choices) when the editor is
     * confirmed, then overridden with any name/portrait the GM supplied.
     *
     * @param campaign the active campaign
     *
     * @return a new {@link ChaosContract} with default data in every section and a fresh id
     */
    public static AbstractContract createBlank(Campaign campaign) {
        final int year = campaign.getGameYear();
        final LocalDate today = campaign.getLocalDate();

        final Faction employerFaction = campaign.getPlayerForce().getFaction();
        final Faction enemyFaction = Factions.getInstance().getFaction(Faction.PIRATE_FACTION_CODE);

        final String employerCode = employerFaction == null ? "" : employerFaction.getShortName();
        final String employerName = employerFaction == null ? "" : employerFaction.getFullName(year);
        final String enemyCode = enemyFaction == null ? Faction.PIRATE_FACTION_CODE : enemyFaction.getShortName();
        final String enemyName = enemyFaction == null ? "" : enemyFaction.getFullName(year);

        final PlanetarySystem currentSystem = campaign.getCurrentSystem();
        final String systemId = currentSystem == null ? null : currentSystem.getId();

        final AbstractContract contract = new ChaosContract();
        contract.setContractId(UUID.randomUUID());
        contract.setContractName(getTextAt(RESOURCE_BUNDLE, "create.contractMarket.defaultName"));
        contract.setDescription("");
        // A freshly created offer has not been accepted, so it carries no status - matching a generated market offer
        // (AbstractContractGeneration#performFinalTasks). The editor only exposes status once a contract is accepted.
        contract.setStatus(null);
        contract.setScale(1);
        contract.setTrackCount(0);
        contract.setRequiredCombatElements(0);
        contract.setRequiredVictoryPoints(0);

        contract.setScheduleData(new ContractScheduleData(today, today.plusMonths(DEFAULT_LENGTH_MONTHS),
              DEFAULT_LENGTH_MONTHS));
        contract.setSystemsTargetData(new SystemsTargetData(systemId, null));

        // NPCs are left null here; the editor generates them from the GM's final factions on confirm.
        contract.setEmployerData(new EmployerData(ChaosEmployerType.LOCAL_SYSTEM_OWNER, employerCode, employerCode,
              null, employerName, null, null, SkillLevel.REGULAR, 0, new Camouflage(), PlayerColour.BLUE));
        contract.setEnemyData(new EnemyData(enemyCode, null, enemyName, SkillLevel.REGULAR, 0, null,
              new Camouflage(), PlayerColour.RED, true));

        contract.setContractTerms(new ContractTermsData(DEFAULT_STEP, DEFAULT_STEP, DEFAULT_STEP, DEFAULT_STEP,
              DEFAULT_STEP));
        contract.setObjectiveData(new ContractObjectiveData(ContractObjectiveType.GARRISON_DUTY,
              ContractObjectiveType.UNDEFINED));
        contract.setContractFinanceData(new ContractFinanceData(Money.zero(), Money.zero(), Money.zero()));
        contract.setRentedFacilitiesData(new RentedFacilitiesData(0, 0, 0));
        contract.setMoraleData(new MoraleData(ContractMoraleLevel.STALEMATE, null, Money.zero()));

        return contract;
    }
}
