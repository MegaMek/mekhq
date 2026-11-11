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
package mekhq.campaign.personnel.training;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.Skills;

/**
 * A campaign and people to price training against: a mocked campaign with real {@link CampaignOptions} and generous
 * funds, and mocked people whose skills are real {@link Skill}s of the real default {@link SkillType}s, so prices come
 * from the game's own skill math rather than numbers written into the tests.
 *
 * <p>Every option that changes a price starts at its neutral value: no Reasoning multiplier, an XP cost multiplier of
 * 1, no training fees, no ability cap. Each person is an active MekWarrior with a talent multiplier of 1.</p>
 */
final class TrainingFixture {
    static final LocalDate TODAY = LocalDate.of(3052, 6, 1);
    static final Money PLENTY = Money.of(1_000_000_000_000.0);

    final Campaign campaign = mockCampaign();
    final CampaignOptions options = new CampaignOptions();
    final Finances finances = mock(Finances.class);

    TrainingFixture() {
        SkillType.initializeTypes();
        options.set(CampaignOption.USE_REASONING_XP_MULTIPLIER, false);
        options.set(CampaignOption.XP_COST_MULTIPLIER, 1.0);
        options.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, false);
        options.set(CampaignOption.USE_SPA_TRAINING_COSTS, false);
        options.set(CampaignOption.CAP_TOTAL_SPAS, false);
        options.set(CampaignOption.USE_ABILITIES, true);
        options.set(CampaignOption.USE_EDGE, true);
        options.set(CampaignOption.USE_ARTILLERY, true);
        options.set(CampaignOption.PERSONNEL_LOG_SKILL_GAIN, false);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaign.getPlayerForce().getFinances()).thenReturn(finances);
        when(finances.getBalance()).thenReturn(PLENTY);
        when(finances.debit(any(TransactionType.class), any(LocalDate.class), any(Money.class), anyString()))
              .thenReturn(true);
    }

    /** @return the campaign's settings, read now; build it after changing any option */
    TrainingContext context() {
        return new TrainingContext(campaign);
    }

    /** @return an empty plan with generous funds */
    TrainingPlan plan() {
        return plan(PLENTY);
    }

    TrainingPlan plan(Money funds) {
        return new TrainingPlan(context(), funds);
    }

    /**
     * @param xp the person's XP
     *
     * @return an active MekWarrior with no skills, whose XP prices are not scaled by talent or Reasoning
     */
    Person person(int xp) {
        Person person = mock(Person.class);
        Skills skills = new Skills();
        when(person.getId()).thenReturn(UUID.randomUUID());
        when(person.getXP()).thenReturn(xp);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        when(person.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        when(person.getFullName()).thenReturn("Test Person");
        when(person.getFullTitle()).thenReturn("MekWarrior Test Person");
        when(person.getHyperlinkedName()).thenReturn("Test Person");
        PersonnelOptions personnelOptions = mock(PersonnelOptions.class);
        when(person.getOptions()).thenReturn(personnelOptions);
        // Counting abilities walks this; an empty walk means the person has none
        when(person.getOptions(PersonnelOptions.LVL3_ADVANTAGES))
              .thenAnswer(invocation -> Collections.emptyEnumeration());
        when(person.getSkills()).thenReturn(skills);
        when(person.getSkill(anyString())).thenAnswer(invocation -> skills.getSkill(invocation.getArgument(0)));
        when(person.hasSkill(anyString())).thenAnswer(invocation -> skills.hasSkill(invocation.getArgument(0)));
        when(person.getTalentBasedXpCostMultiplier(anyBoolean(), any())).thenReturn(1.0);
        when(person.getReasoningXpCostMultiplier(anyBoolean())).thenReturn(1.0);
        return person;
    }

    /**
     * @param startXp the person's XP
     *
     * @return a person, as {@link #person(int)}, whose XP goes down as it is spent and whose skills really improve, so
     *       a commit can be checked against the result
     */
    Person livePerson(int startXp) {
        Person person = person(startXp);
        AtomicInteger xp = new AtomicInteger(startXp);
        when(person.getXP()).thenAnswer(invocation -> xp.get());
        doAnswer(invocation -> xp.addAndGet(-(int) invocation.getArgument(0))).when(person).spendXP(anyInt());
        doAnswer(invocation -> xp.addAndGet(-(int) invocation.getArgument(1))).when(person)
              .spendXPOnSkills(any(), anyInt());
        doAnswer(invocation -> {
            person.getSkill(invocation.getArgument(0)).improve();
            return null;
        }).when(person).improveSkill(anyString());
        return person;
    }

    /**
     * Gives a person a skill.
     *
     * @return the skill, which the person's {@code getSkill} returns
     */
    static Skill giveSkill(Person person, String skillName, int level) {
        Skill skill = new Skill(SkillType.getType(skillName), level, 0);
        person.getSkills().addSkill(skillName, skill);
        return skill;
    }

    /** @return the XP the game charges to raise a skill from a level, before any multiplier */
    static int improvementCost(String skillName, int level) {
        return new Skill(SkillType.getType(skillName), level, 0).getCostToImprove();
    }
}
