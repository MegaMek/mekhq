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
import mekhq.campaign.mission.newContract.contractGeneration.EmployerModifierData;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;

public abstract class AbstractContractManager {
    private final static MMLogger LOGGER = MMLogger.create(AbstractContractManager.class);

    private String employerFactionCode;
    EmployerModifierData employerModifierData;
    private List<AbstractContractObjective> contractObjectives;

    private String targetSystemId;
    private JumpPath cachedJumpPath = new JumpPath();

    public AbstractContractManager() {
    }

    public String getEmployerFactionCode() {
        return employerFactionCode;
    }

    public void setEmployerFactionCode(String employerFactionCode) {
        this.employerFactionCode = employerFactionCode;
    }

    public Faction getEmployerFaction() {
        return Factions.getInstance().getFaction(employerFactionCode);
    }

    public List<AbstractContractObjective> getContractAllObjectivesDirect() {
        return contractObjectives;
    }

    public List<AbstractContractObjective> getContractAllObjectivesCopy() {
        return new ArrayList<>(contractObjectives);
    }

    public @Nullable AbstractContractObjective getContractObjective(int index) {
        try {
            return contractObjectives.get(index);
        } catch (IndexOutOfBoundsException e) {
            LOGGER.error("Index out of bounds: {}", index);
            return null;
        }
    }

    public void setContractObjectives(List<AbstractContractObjective> contractObjectives) {
        this.contractObjectives = contractObjectives;
    }

    public void addContractObjective(AbstractContractObjective contractObjective) {
        contractObjective.setParentContractManager(this);
        contractObjectives.add(contractObjective);
    }

    public void removeContractObjective(AbstractContractObjective contractObjective) {
        contractObjectives.remove(contractObjective);
    }

    public String getTargetSystemId() {
        return targetSystemId;
    }

    public void setTargetSystemId(String targetSystemId) {
        this.targetSystemId = targetSystemId;
    }

    public PlanetarySystem getTargetSystem() {
        return Systems.getInstance().getSystemById(targetSystemId);
    }

    public JumpPath getCachedJumpPathDirect() {
        return cachedJumpPath;
    }

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

    public void setCachedJumpPath(JumpPath cachedJumpPath) {
        this.cachedJumpPath = cachedJumpPath;
    }

    public EmployerModifierData getEmployerModifierData() {
        return employerModifierData;
    }

    public void setEmployerModifierData(EmployerModifierData employerModifierData) {
        this.employerModifierData = employerModifierData;
    }
}
