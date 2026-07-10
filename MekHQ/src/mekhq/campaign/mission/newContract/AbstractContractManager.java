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
package mekhq.campaign.mission.newContract;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.mission.newContract.contractGeneration.ContractPayData;
import mekhq.campaign.mission.newContract.contractGeneration.EmployerModifierData;
import mekhq.campaign.mission.newContract.contractGeneration.NegotiationsData;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;

/**
 * Base class for the in-progress state of a contract as it is generated: the employer, objectives, target system,
 * cached jump path, negotiation modifiers, negotiated terms, pay, and the high-risk/covert flags. Subclasses supply the
 * concrete contract flavor.
 */
public abstract class AbstractContractManager {
    private final static MMLogger LOGGER = MMLogger.create(AbstractContractManager.class);

    private String employerFactionCode;
    private String mercenaryEmployerFactionCode;
    private EmployerModifierData employerModifierData;
    private List<AbstractContractObjective> contractObjectives;
    private ContractPayData contractPayData;

    private String targetSystemId;
    private JumpPath cachedJumpPath = new JumpPath();
    private boolean isHighRisk;
    private boolean isCovert;
    private NegotiationsData negotiationsData;

    /**
     * Creates an empty contract manager.
     */
    public AbstractContractManager() {
    }

    /**
     * @return the employer faction's short code
     */
    public String getEmployerFactionCode() {
        return employerFactionCode;
    }

    /**
     * @param employerFactionCode the employer faction's short code
     */
    public void setEmployerFactionCode(String employerFactionCode) {
        this.employerFactionCode = employerFactionCode;
    }

    /**
     * @return the employer faction resolved from the stored faction code
     */
    public Faction getEmployerFaction() {
        return Factions.getInstance().getFaction(employerFactionCode);
    }

    /**
     * @return the live list of contract objectives (not a copy)
     */
    public List<AbstractContractObjective> getContractAllObjectivesDirect() {
        return contractObjectives;
    }

    /**
     * @return a defensive copy of the contract objectives
     */
    public List<AbstractContractObjective> getContractAllObjectivesCopy() {
        return new ArrayList<>(contractObjectives);
    }

    /**
     * Returns the objective at the given index.
     *
     * @param index the objective index
     *
     * @return the objective, or {@code null} if the index is out of bounds
     */
    public @Nullable AbstractContractObjective getContractObjective(int index) {
        try {
            return contractObjectives.get(index);
        } catch (IndexOutOfBoundsException e) {
            LOGGER.error("Index out of bounds: {}", index);
            return null;
        }
    }

    /**
     * @param contractObjectives the contract objectives to set
     */
    public void setContractObjectives(List<AbstractContractObjective> contractObjectives) {
        this.contractObjectives = contractObjectives;
    }

    /**
     * Adds an objective, setting this manager as its parent.
     *
     * @param contractObjective the objective to add
     */
    public void addContractObjective(AbstractContractObjective contractObjective) {
        contractObjective.setParentContractManager(this);
        contractObjectives.add(contractObjective);
    }

    /**
     * Removes an objective.
     *
     * @param contractObjective the objective to remove
     */
    public void removeContractObjective(AbstractContractObjective contractObjective) {
        contractObjectives.remove(contractObjective);
    }

    /**
     * @return the target system's id
     */
    public String getTargetSystemId() {
        return targetSystemId;
    }

    /**
     * @param targetSystemId the target system's id
     */
    public void setTargetSystemId(String targetSystemId) {
        this.targetSystemId = targetSystemId;
    }

    /**
     * @return the target planetary system resolved from the stored system id
     */
    public PlanetarySystem getTargetSystem() {
        return Systems.getInstance().getSystemById(targetSystemId);
    }

    /**
     * @return the cached jump path without recalculating it
     */
    public JumpPath getCachedJumpPathDirect() {
        return cachedJumpPath;
    }

    /**
     * Returns the cached jump path, recalculating it from the given current system to the target if the cache no longer
     * starts at the current system.
     *
     * @param currentSystem the campaign's current system
     * @param campaign      the campaign, used to calculate the jump path
     *
     * @return the (possibly refreshed) cached jump path
     */
    public JumpPath getCachedJumpPathWithUpdate(PlanetarySystem currentSystem, Campaign campaign) {
        PlanetarySystem firstSystem = cachedJumpPath.getFirstSystem();
        if (firstSystem == null) {
            return cachedJumpPath;
        }

        String firstSystemId = firstSystem.getId();
        String currentSystemId = currentSystem.getId();
        boolean shouldUpdateCache = !firstSystemId.equals(currentSystemId);

        if (shouldUpdateCache) {
            cachedJumpPath = campaign.calculateJumpPath(currentSystem, getTargetSystem());
        }

        return cachedJumpPath;
    }

    /**
     * @param cachedJumpPath the jump path to cache
     */
    public void setCachedJumpPath(JumpPath cachedJumpPath) {
        this.cachedJumpPath = cachedJumpPath;
    }

    /**
     * @return the accumulated employer negotiation modifiers
     */
    public EmployerModifierData getEmployerModifierData() {
        return employerModifierData;
    }

    /**
     * @param employerModifierData the accumulated employer negotiation modifiers
     */
    public void setEmployerModifierData(EmployerModifierData employerModifierData) {
        this.employerModifierData = employerModifierData;
    }

    /**
     * @return the contract's payment breakdown
     */
    public ContractPayData getContractPayData() {
        return contractPayData;
    }

    /**
     * @param contractPayData the contract's payment breakdown
     */
    public void setContractPayData(ContractPayData contractPayData) {
        this.contractPayData = contractPayData;
    }

    /**
     * @return {@code true} if the contract is high-risk
     */
    public boolean isHighRisk() {
        return isHighRisk;
    }

    /**
     * @param isHighRisk whether the contract is high-risk
     */
    public void setHighRisk(boolean isHighRisk) {
        this.isHighRisk = isHighRisk;
    }

    /**
     * @return {@code true} if the contract is covert
     */
    public boolean isCovert() {
        return isCovert;
    }

    /**
     * @param isCovert whether the contract is covert
     */
    public void setCovert(boolean isCovert) {
        this.isCovert = isCovert;
    }

    /**
     * @return the negotiated contract terms
     */
    public NegotiationsData getNegotiationsData() {
        return negotiationsData;
    }

    /**
     * @param negotiationsData the negotiated contract terms
     */
    public void setNegotiationsData(NegotiationsData negotiationsData) {
        this.negotiationsData = negotiationsData;
    }

    /**
     * Retrieves the mercenary employer faction's short code.
     *
     * @return the mercenary employer faction's short code
     */
    public String getMercenaryEmployerFactionCode() {
        return mercenaryEmployerFactionCode;
    }

    /**
     * Sets the mercenary employer faction's short code.
     *
     * @param mercenaryEmployerFactionCode the mercenary employer faction's short code
     */
    public void setMercenaryEmployerFactionCode(String mercenaryEmployerFactionCode) {
        this.mercenaryEmployerFactionCode = mercenaryEmployerFactionCode;
    }
}
