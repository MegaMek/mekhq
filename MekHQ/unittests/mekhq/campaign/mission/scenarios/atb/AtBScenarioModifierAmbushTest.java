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
package mekhq.campaign.mission.scenarios.atb;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.client.bot.princess.CardinalEdge;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.BotForce;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Guards {@link AtBScenarioModifierApplicator#setupAmbush} against #6043 (convoy escort units starting hidden): only
 * the modifier's own force is hidden, a force with a destination edge is left alone, and no more than half of the
 * force is hidden.
 */
class AtBScenarioModifierAmbushTest {

    private final List<BotForce> botForces = new ArrayList<>();
    private final Map<BotForce, ScenarioForceTemplate> botForceTemplates = new HashMap<>();
    private Campaign campaign;
    private AtBDynamicScenario scenario;

    @BeforeEach
    void setUp() {
        GameOptions gameOptions = mock(GameOptions.class);
        when(gameOptions.booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)).thenReturn(true);
        campaign = mock(Campaign.class);
        when(campaign.getGameOptions()).thenReturn(gameOptions);

        scenario = mock(AtBDynamicScenario.class);
        when(scenario.getBotForces()).thenReturn(botForces);
        when(scenario.getBotForceTemplates()).thenReturn(botForceTemplates);
    }

    private static ScenarioForceTemplate forceTemplate(String name) {
        ScenarioForceTemplate template = new ScenarioForceTemplate();
        template.setForceName(name);
        template.setArrivalTurn(0);
        template.setDestinationZone(CardinalEdge.NONE.getIndex());
        return template;
    }

    private static Entity groundUnit() {
        return mock(Entity.class);
    }

    private static Entity aeroUnit() {
        Entity entity = mock(Entity.class);
        when(entity.isAero()).thenReturn(true);
        return entity;
    }

    private void addBotForce(ScenarioForceTemplate template, List<Entity> entities) {
        BotForce botForce = mock(BotForce.class);
        when(botForce.getFullEntityList(campaign)).thenReturn(entities);
        botForces.add(botForce);
        botForceTemplates.put(botForce, template);
    }

    @Test
    void onlyTheModifierForceIsHidden() {
        ScenarioForceTemplate ambushForce = forceTemplate("Ambushers");
        Entity ambusher = groundUnit();
        addBotForce(ambushForce, List.of(ambusher, groundUnit()));

        Entity convoyUnit = groundUnit();
        addBotForce(forceTemplate("Convoy"), List.of(convoyUnit, groundUnit()));

        AtBScenarioModifierApplicator.setupAmbush(scenario, campaign, ambushForce);

        verify(ambusher).setHidden(true);
        verify(convoyUnit, never()).setHidden(anyBoolean());
    }

    @Test
    void forceSharingTheModifierForceNameIsNotHidden() {
        ScenarioForceTemplate ambushForce = forceTemplate("Ambushers");
        addBotForce(ambushForce, List.of(groundUnit(), groundUnit()));

        Entity namesakeUnit = groundUnit();
        addBotForce(forceTemplate("Ambushers"), List.of(namesakeUnit, groundUnit()));

        AtBScenarioModifierApplicator.setupAmbush(scenario, campaign, ambushForce);

        verify(namesakeUnit, never()).setHidden(anyBoolean());
    }

    @Test
    void forceWithDestinationEdgeIsNotHidden() {
        ScenarioForceTemplate ambushForce = forceTemplate("Ambushers");
        ambushForce.setDestinationZone(CardinalEdge.NORTH.getIndex());
        Entity unit = groundUnit();
        addBotForce(ambushForce, List.of(unit, groundUnit()));

        AtBScenarioModifierApplicator.setupAmbush(scenario, campaign, ambushForce);

        verify(unit, never()).setHidden(anyBoolean());
    }

    @Test
    void atMostHalfTheForceIsHidden() {
        ScenarioForceTemplate ambushForce = forceTemplate("Ambushers");
        List<Entity> units = List.of(groundUnit(), groundUnit(), groundUnit(), groundUnit());
        addBotForce(ambushForce, units);

        AtBScenarioModifierApplicator.setupAmbush(scenario, campaign, ambushForce);

        verify(units.get(0)).setHidden(true);
        verify(units.get(1)).setHidden(true);
        verify(units.get(2), never()).setHidden(anyBoolean());
        verify(units.get(3), never()).setHidden(anyBoolean());
    }

    @Test
    void airborneUnitsAreNotHidden() {
        ScenarioForceTemplate ambushForce = forceTemplate("Ambushers");
        Entity aero = aeroUnit();
        Entity ground = groundUnit();
        addBotForce(ambushForce, List.of(aero, ground));

        AtBScenarioModifierApplicator.setupAmbush(scenario, campaign, ambushForce);

        verify(aero, never()).setHidden(anyBoolean());
        verify(ground).setHidden(true);
    }
}
