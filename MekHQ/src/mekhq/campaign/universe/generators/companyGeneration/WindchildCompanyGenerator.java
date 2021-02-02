/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe.generators.companyGeneration;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.universe.enums.CompanyGenerationType;

public class WindchildCompanyGenerator extends AbstractCompanyGenerator {
    //region Constructors
    public WindchildCompanyGenerator() {
        super(CompanyGenerationType.WINDCHILD);
    }
    //endregion Constructors

    @Override
    protected void generateCommandingOfficer(Person commandingOfficer, int numMechWarriors) {
        commandingOfficer.setCommander(true);
        commandingOfficer.improveSkill(SkillType.S_GUN_MECH);
        commandingOfficer.improveSkill(SkillType.S_PILOT_MECH);
        assignRandomOfficerSkillIncrease(commandingOfficer, 2);

        if (getOptions().isAutomaticallyAssignRanks()) {
            if (numMechWarriors > 36) {
                commandingOfficer.setRankLevel(Ranks.RWO_MAX + (getOptions().getFaction().isComStarOrWoB() ? 7 : 8));
            } else if (numMechWarriors > 12) {
                commandingOfficer.setRankLevel(Ranks.RWO_MAX + (getOptions().getFaction().isComStarOrWoB() ? 7 : 5));
            } else if (numMechWarriors > 4) {
                commandingOfficer.setRankLevel(Ranks.RWO_MAX + 4);
            } else {
                commandingOfficer.setRankLevel(Ranks.RWO_MAX + 3);
            }
        }
    }
}
