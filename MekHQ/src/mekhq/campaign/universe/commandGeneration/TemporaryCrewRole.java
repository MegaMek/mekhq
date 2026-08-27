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
package mekhq.campaign.universe.commandGeneration;

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * The crew roles a campaign can fill with temporary crew instead of named people, each tied to the campaign option
 * that switches it on.
 *
 * <p>The campaign options dialog offers these eight toggles; the Command Designer offers the same eight so a player
 * building a starting force can decide there and then whether, for example, a tank's driver and gunner are named
 * warriors or an anonymous crew drawn from a pool. The generator reads the campaign options when it assembles each
 * unit's crew, so the designer's choices are written to the campaign before any unit is built.</p>
 *
 * <p>Declared in the order the campaign options dialog lists them, which is also the order the designer shows them.
 * </p>
 */
public enum TemporaryCrewRole {
    INFANTRY(PersonnelRole.SOLDIER, CampaignOption.USE_BLOB_INFANTRY, "UseBlobInfantry"),
    BATTLE_ARMOR(PersonnelRole.BATTLE_ARMOUR, CampaignOption.USE_BLOB_BATTLE_ARMOR, "UseBlobBattleArmor"),
    GROUND_VEHICLE_CREW(PersonnelRole.VEHICLE_CREW_GROUND, CampaignOption.USE_BLOB_VEHICLE_CREW_GROUND,
          "UseBlobVehicleCrewGround"),
    VTOL_CREW(PersonnelRole.VEHICLE_CREW_VTOL, CampaignOption.USE_BLOB_VEHICLE_CREW_VTOL, "UseBlobVehicleCrewVTOL"),
    NAVAL_VEHICLE_CREW(PersonnelRole.VEHICLE_CREW_NAVAL, CampaignOption.USE_BLOB_VEHICLE_CREW_NAVAL,
          "UseBlobVehicleCrewNaval"),
    VESSEL_PILOTS(PersonnelRole.VESSEL_PILOT, CampaignOption.USE_BLOB_VESSEL_PILOT, "UseBlobVesselPilot"),
    VESSEL_GUNNERS(PersonnelRole.VESSEL_GUNNER, CampaignOption.USE_BLOB_VESSEL_GUNNER, "UseBlobVesselGunner"),
    VESSEL_CREW(PersonnelRole.VESSEL_CREW, CampaignOption.USE_BLOB_VESSEL_CREW, "UseBlobVesselCrew");

    private final PersonnelRole personnelRole;
    private final CampaignOption<Boolean> campaignOption;
    private final String labelKey;

    TemporaryCrewRole(PersonnelRole personnelRole, CampaignOption<Boolean> campaignOption, String labelKey) {
        this.personnelRole = personnelRole;
        this.campaignOption = campaignOption;
        this.labelKey = labelKey;
    }

    /**
     * @return the crew role whose seats this toggle lets temporary crew fill
     */
    public PersonnelRole getPersonnelRole() {
        return personnelRole;
    }

    /**
     * @return the campaign option that switches temporary crew on for this role
     */
    public CampaignOption<Boolean> getCampaignOption() {
        return campaignOption;
    }

    /**
     * @return the resource-bundle key suffix for this toggle's label and tooltip, shared with the campaign options
     *       dialog so the same setting reads the same in both places
     */
    public String getLabelKey() {
        return labelKey;
    }
}
