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
package mekhq.campaign.digitalGM.stratCon.facility;

import java.time.LocalDate;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import megamek.common.annotations.Nullable;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState.LocalDateAdapter;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;

/**
 * An order that takes time: a formation holding its position on or next to a hex until the order completes, as
 * {@link FacilityOperation#RECON} and {@link FacilityOperation#BUILD} need. Orders that resolve at once are never
 * stored.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConFacilityOrder {
    private FacilityOperation operation;
    private int formationId;
    private StratConCoords targetCoords;
    private LocalDate completionDate;
    private String definitionId;

    public StratConFacilityOrder() {
    }

    /**
     * @param operation      the order
     * @param formationId    the ID of the formation carrying it out
     * @param targetCoords   the hex the order acts on
     * @param completionDate the day the order completes
     * @param definitionId   for {@link FacilityOperation#BUILD}, the ID of the facility definition to build; otherwise
     *                       {@code null}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConFacilityOrder(FacilityOperation operation, int formationId, StratConCoords targetCoords,
          LocalDate completionDate, @Nullable String definitionId) {
        this.operation = operation;
        this.formationId = formationId;
        this.targetCoords = targetCoords;
        this.completionDate = completionDate;
        this.definitionId = definitionId;
    }

    public FacilityOperation getOperation() {
        return operation;
    }

    public void setOperation(FacilityOperation operation) {
        this.operation = operation;
    }

    public int getFormationId() {
        return formationId;
    }

    public void setFormationId(int formationId) {
        this.formationId = formationId;
    }

    public StratConCoords getTargetCoords() {
        return targetCoords;
    }

    public void setTargetCoords(StratConCoords targetCoords) {
        this.targetCoords = targetCoords;
    }

    @XmlJavaTypeAdapter(value = LocalDateAdapter.class)
    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDate completionDate) {
        this.completionDate = completionDate;
    }

    public @Nullable String getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(@Nullable String definitionId) {
        this.definitionId = definitionId;
    }
}
