package mekhq.campaign.autoResolve.scenarioResolver.components;

import megamek.common.Entity;

import java.util.List;

public record AutoResolveForce(int team, String forceName, int id, List<Entity> units) {}
