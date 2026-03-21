/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 */
package mekhq.service.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a mission proposal from the AI.
 */
public class MissionProposal {
    @JsonProperty("title") public String title;
    @JsonProperty("briefing") public String briefing;
    @JsonProperty("missionType") public String missionType; // e.g., "GARRISON_DUTY"
    @JsonProperty("employerCode") public String employerCode; // e.g., "FS"
    @JsonProperty("enemyCode") public String enemyCode; // e.g., "DC"
    @JsonProperty("planetName") public String planetName; // Optional planet name
    @JsonProperty("difficulty") public int difficulty; // 1-10
    @JsonProperty("lengthWeeks") public int lengthWeeks;
}
