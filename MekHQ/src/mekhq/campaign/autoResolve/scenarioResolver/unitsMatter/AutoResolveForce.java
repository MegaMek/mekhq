package mekhq.campaign.autoResolve.scenarioResolver.unitsMatter;

import megamek.common.Entity;

import java.util.List;

public record AutoResolveForce(int team, String forceName, int id, List<Entity> units) {}
