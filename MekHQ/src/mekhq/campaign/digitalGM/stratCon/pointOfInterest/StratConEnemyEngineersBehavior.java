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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static mekhq.campaign.enums.DailyReportType.BATTLE;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityIntel;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityTier;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityFactory;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;

/**
 * The behavior of enemy engineers: an enemy work party, sent to build a new outpost (see
 * {@code StratConEnemyFacilityActivity}).
 *
 * <p>Deploying a formation onto their hex makes the usual scenario roll. With no scenario, the engineers scatter. With
 * one, the formation is ambushed by their escort: winning drives them off, and anything else lets them finish.
 * Engineers left alone until they leave finish too. Finishing leaves an enemy Outpost, of a type picked as for any
 * facility that turns up mid-contract, on their hex, which the player already knows the position of - unless the
 * sector has reached its facility cap by then.</p>
 *
 * <p>Driving them off pays no combat bonus.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConEnemyEngineersBehavior extends StratConContestedPointOfInterestBehavior {
    /** The behavior ID the enemy engineers definition names. */
    public static final String BEHAVIOR_ID = "enemyEngineers";

    /** The type ID of the enemy engineers definition. */
    public static final String TYPE_ID = "EnemyEngineers";

    /**
     * Their escort ambushes the deploying formation in a template suited to its unit type.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConEnemyEngineersBehavior() {
        super(BEHAVIOR_ID, null, NoScenarioOutcome.SECURE);
    }

    @Override
    protected boolean isCombatBonusPaid(AbstractContract contract) {
        return false;
    }

    /**
     * Their escort is always an ambush: a Crisis, and never a Turning Point.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isScenarioAnAmbush() {
        return true;
    }

    /**
     * Winning drives them off; anything else lets them finish their outpost.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void onLinkedScenarioEnded(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          boolean isVictory, Campaign campaign) {
        if (isVictory) {
            super.onLinkedScenarioEnded(pointOfInterest, track, true, campaign);
            return;
        }

        buildOutpost(pointOfInterest, track, campaign);
    }

    /**
     * Left alone, they finish their outpost.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void onExpired(StratConPointOfInterest pointOfInterest, StratConTrackState track, Campaign campaign) {
        buildOutpost(pointOfInterest, track, campaign);
    }

    /**
     * Replaces the engineers with an enemy Outpost of a random type, at full garrison.
     *
     * @param pointOfInterest the engineers
     * @param track           the sector they are in
     * @param campaign        the current campaign
     *
     * @return the new facility, or {@code null} if none could be made or something else already stands on the hex
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Nullable StratConFacility buildOutpost(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          Campaign campaign) {
        StratConCoords coords = pointOfInterest.getCoords();
        StratConPointOfInterestRules.withdrawPointOfInterest(track, pointOfInterest);

        // The sector may have filled up while they worked; the cap still holds.
        if ((coords == null)
                  || (track.getFacility(coords) != null)
                  || !StratConContractInitializer.hasRoomForFacility(track)) {
            return null;
        }

        // Made like any other facility that turns up mid-contract, so the contract's profile picks the type and it
        // rolls traits; only the tier differs, as engineers raise no more than an Outpost.
        AbstractContract contract = StratConPointOfInterestRules.getContract(track, campaign);
        StratConFacility facility = (contract == null) ?
                                          StratConFacilityFactory.getRandomHostileFacility() :
                                          StratConContractInitializer.createMidContractFacility(campaign,
                                                contract,
                                                ForceAlignment.Opposing);
        if (facility == null) {
            return null;
        }

        facility.setTier(FacilityTier.OUTPOST);
        facility.setGarrison(facility.getGarrisonMaximum());
        facility.raiseIntel(FacilityIntel.LOCATED);
        track.addFacility(coords, facility);

        if (contract != null) {
            StratConContractInitializer.connectFacilitiesToRoads(track, contract, campaign);
        }

        addReport(BATTLE, "built.report", pointOfInterest, track, campaign);
        return facility;
    }
}
