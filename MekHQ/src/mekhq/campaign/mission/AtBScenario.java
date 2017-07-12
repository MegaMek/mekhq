/*
 * AtBScenario.java
 *
 * Copyright (C) 2014-2016 MegaMek team
 * Copyright (c) 2014 Carl Spain. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.mission;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Collectors;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.client.RandomNameGenerator;
import megamek.client.RandomSkillsGenerator;
import megamek.client.RandomUnitGenerator;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.HomeEdge;
import megamek.client.bot.princess.PrincessException;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.IStartingPositions;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.PlanetaryConditions;
import megamek.common.Player;
import megamek.common.UnitType;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.AtBConfiguration;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.mission.atb.IAtBScenario;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planets;

/**
 * @author Neoancient
 *
 */
public abstract class AtBScenario extends Scenario implements IAtBScenario {
    private static final long serialVersionUID = 1148105510264408943L;

    public static final int BASEATTACK = 0;
    public static final int EXTRACTION = 1;
    public static final int CHASE = 2;
    public static final int HOLDTHELINE = 3;
    public static final int BREAKTHROUGH = 4;
    public static final int HIDEANDSEEK = 5;
    public static final int STANDUP = 6;
    public static final int RECONRAID = 7;
    public static final int PROBE = 8;

    public static final int OFFICERDUEL = 9; //Special Mission
    public static final int ACEDUEL = 10; //Special Mission
    public static final int AMBUSH = 11; //Special Mission
    public static final int CIVILIANHELP = 12; //Special Mission
    public static final int ALLIEDTRAITORS = 13; //Special Mission
    public static final int PRISONBREAK = 14; //Special Mission
    public static final int STARLEAGUECACHE1 = 15; //Special Mission
    public static final int STARLEAGUECACHE2 = 16; //Special Mission

    public static final int ALLYRESCUE = 17; //Big Battle
    public static final int CIVILIANRIOT = 18; //Big Battle
    public static final int CONVOYRESCUE = 19; //Big Battle
    public static final int CONVOYATTACK = 20; //Big Battle
    public static final int PIRATEFREEFORALL = 21; //Big Battle

    public static final int TER_HILLS = 0;
    public static final int TER_BADLANDS = 1;
    public static final int TER_WETLANDS = 2;
    public static final int TER_LIGHTURBAN = 3;
    public static final int TER_FLATLANDS = 4;
    public static final int TER_WOODED = 5;
    public static final int TER_HEAVYURBAN = 6;
    public static final int TER_COASTAL = 7;
    public static final int TER_MOUNTAINS = 8;
    public static final String[] terrainTypes = {"Hills", "Badlands", "Wetlands",
        "Light Urban", "Flatlands", "Wooded", "Heavy Urban", "Coastal",
        "Mountains"
    };

    public static final int FORCE_MEK = 0;
    public static final int FORCE_VEHICLE = 1;
    public static final int FORCE_MIXED = 2;
    public static final int FORCE_NOVA = 3;
    public static final int FORCE_VEENOVA = 4;
    public static final int FORCE_INFANTRY = 5;
    public static final int FORCE_BA = 6;
    public static final int FORCE_AERO = 7;
    public static final int FORCE_PROTOMEK = 8;
    public static final String[] forceTypeNames = {
        "Mek", "Vehicle", "Mixed", "Nova", "Nova", "Infantry",
        "Battle Armor", "Aerospace", "ProtoMek"
    };

    public final int[] terrainChart = {
            TER_HILLS, TER_BADLANDS, TER_WETLANDS, TER_LIGHTURBAN,
            TER_HILLS, TER_FLATLANDS, TER_WOODED, TER_HEAVYURBAN,
            TER_COASTAL, TER_WOODED, TER_MOUNTAINS
    };
    
    public final String[] antiRiotWeapons = {
            "ISERSmallLaser", "Small Laser", "Small Laser Prototype",
            "ISSmallPulseLaser", "ISSmallXPulseLaser", "Small Re-engineered Laser",
            "ISSmallVSPLaser", "CLERMicroLaser", "CLERSmallLaser",
            "ER Small Laser (CP)", "CLHeavySmallLaser",
            "CLImprovedSmallHeavyLaser", "ClSmall Laser",
            "CLERSmallPulseLaser", "CLMicroPulseLaser", "CLSmallPulseLaser",
            "CLSmallChemLaser",
            "Heavy Machine Gun", "Light Machine Gun", "Machine Gun",
            "Heavy Rifle", "Light Rifle", "Medium Rifle",
            "CLHeavyMG", "CLLightMG", "CLMG",
            "Flamer", "ER Flamer", "CLFlamer", "CLERFlamer",
            "Vehicle Flamer", "Heavy Flamer", "CLVehicleFlamer", "CLHeavyFlamer"
        };
    
    /* The starting position chart in the AtB rules includes the four
     * corner positions as well, but this creates some conflict with
     * setting the home edge for the bot, which only includes the four
     * sides.
     */
    protected static final int [] startPos = {
        Board.START_N, Board.START_E, Board.START_S, Board.START_W
    };

    private boolean attacker;
    private int lanceForceId; // -1 if scenario is not generated for a specific lance (special mission, big battle)
    private int lanceRole; /* set when scenario is created in case it is changed for the next week before the scenario is resolved;
                            specifically affects scenarios generated for scout lances, in which the deployment may be delayed
                            for slower units */
    private int terrainType;
    private int light;
    private int weather;
    private int wind;
    private int fog;
    private int atmosphere;
    private float gravity;
    private int start;
    private int deploymentDelay;
    private int mapSizeX;
    private int mapSizeY;
    private String map;
    private int lanceCount;
    private int rerollsRemaining;
    private int enemyHome;
    
    ArrayList<Entity> alliesPlayer;
    ArrayList<BotForce> botForces;
    ArrayList<String> alliesPlayerStub;
    ArrayList<BotForceStub> botForceStubs;
    
    /* Special missions cannot generate the enemy until the unit is
     * added, but needs the Campaign object which is not passed
     * by addForce or addUnit. Instead we generate all possibilities
     * (one for each weight class) when the scenario is created and
     * choose the correct one when a unit is deployed.
     */

    ArrayList<ArrayList<Entity>> specMissionEnemies;

    /* Big battles have a similar problem for attached allies. Though
     * we could generate the maximum number (4) and remove them as
     * the player deploys additional units, they would be lost if
     * any units are undeployed.
     */

    ArrayList<Entity> bigBattleAllies;

    /* Units that need to be tracked for possible contract breaches
     * (for destruction), or bonus rolls (for survival).
     */
    ArrayList<UUID> attachedUnitIds;
    ArrayList<UUID> survivalBonus;

    HashMap<UUID, Entity> entityIds;

    private static ResourceBundle defaultResourceBundle = ResourceBundle.getBundle("mekhq.resources.AtBScenarioBuiltIn", new EncodeControl()); //$NON-NLS-1$
    
    public AtBScenario () {
        super();
        lanceForceId = -1;
        lanceRole = Lance.ROLE_UNASSIGNED;
        alliesPlayer = new ArrayList<Entity>();
        botForces = new ArrayList<BotForce>();
        alliesPlayerStub = new ArrayList<String>();
        botForceStubs = new ArrayList<BotForceStub>();
        attachedUnitIds = new ArrayList<UUID>();
        survivalBonus = new ArrayList<UUID>();
        entityIds = new HashMap<UUID, Entity>();

        light = PlanetaryConditions.L_DAY;
        weather = PlanetaryConditions.WE_NONE;
        wind = PlanetaryConditions.WI_NONE;
        fog = PlanetaryConditions.FOG_NONE;
        atmosphere = PlanetaryConditions.ATMO_STANDARD;
        gravity = (float)1.0;
        deploymentDelay = 0;
        lanceCount = 0;
        rerollsRemaining = 0;
    }
    
    public void initialize(Campaign c, Lance lance, boolean attacker, Date date) {
        this.attacker = attacker;

        alliesPlayer = new ArrayList<Entity>();
        botForces = new ArrayList<BotForce>();
        alliesPlayerStub = new ArrayList<String>();
        botForceStubs = new ArrayList<BotForceStub>();
        attachedUnitIds = new ArrayList<UUID>();
        survivalBonus = new ArrayList<UUID>();
        entityIds = new HashMap<UUID, Entity>();

        if (null == lance) {
            lanceForceId = -1;
            lanceRole = Lance.ROLE_UNASSIGNED;
        } else {
            this.lanceForceId = lance.getForceId();
            lanceRole = lance.getRole();
            setMissionId(lance.getMissionId());

            for (UUID id : c.getForce(lance.getForceId()).getAllUnits()) {
                entityIds.put(id, c.getUnit(id).getEntity());
            }
        }

        light = PlanetaryConditions.L_DAY;
        weather = PlanetaryConditions.WE_NONE;
        wind = PlanetaryConditions.WI_NONE;
        fog = PlanetaryConditions.FOG_NONE;
        atmosphere = PlanetaryConditions.ATMO_STANDARD;
        gravity = (float)1.0;
        deploymentDelay = 0;
        setDate(date);
        lanceCount = 0;
        rerollsRemaining = 0;
        
        if (isStandardMission()) {
        	setName(getScenarioTypeDescription() + (isAttacker() ? " (Attacker)" : " (Defender)"));
        } else {
        	setName(getScenarioTypeDescription());
        }
        
    	initBattle(c);
    }
    
    public String getDesc() {
        return getScenarioTypeDescription() + (isStandardMission() ? (attacker ? " (Attacker)" : " (Defender)") : "");
    }
    
    @Override
    public boolean isStandardMission() {
    	return !isSpecialMission() && !isBigBattle();
    }
    
    @Override
    public boolean isSpecialMission() {
    	return false;
    }
    
    @Override
    public boolean isBigBattle() {
    	return false;
    }

    @Override
    public ResourceBundle getResourceBundle() {
    	return defaultResourceBundle;
    }
    
    /**
     * Determines battle conditions: terrain, weather, map.
     *
     * @param campaign
     */
    private void initBattle(Campaign campaign) {
        setTerrain();
        if (campaign.getCampaignOptions().getUseLightConditions()) {
            setLightConditions();
        }
        if (campaign.getCampaignOptions().getUseWeatherConditions()) {
            setWeather();
        }
        if (campaign.getCampaignOptions().getUsePlanetaryConditions() &&
                null != campaign.getMission(getMissionId())) {
            setPlanetaryConditions(campaign.getMission(getMissionId()), campaign);
        }
        setMapSize();
        setMapFile();
        if (isStandardMission()) {
            lanceCount = 1;
        } else if (isBigBattle()) {
            lanceCount = 2;
        }

        if (null != getLance(campaign)) {
            getLance(campaign).refreshCommander(campaign);
            if (null != getLance(campaign).getCommander(campaign).getSkill(SkillType.S_TACTICS)) {
                rerollsRemaining = getLance(campaign).getCommander(campaign).getSkill(SkillType.S_TACTICS).getLevel();
            }
        }
    }

    public void setTerrain() {
    	terrainType = terrainChart[Compute.d6(2) - 2];
    }

    public void setLightConditions() {
        light = PlanetaryConditions.L_DAY;

        int roll = Compute.randomInt(10) + 1;
        if (roll < 6) light = PlanetaryConditions.L_DAY;
        else if (roll < 8) light = PlanetaryConditions.L_DUSK;
        else if (roll == 8) light = PlanetaryConditions.L_FULL_MOON;
        else if (roll == 9) light = PlanetaryConditions.L_MOONLESS;
        else light = PlanetaryConditions.L_PITCH_BLACK;
    }

    public void setWeather() {
        weather = PlanetaryConditions.WE_NONE;
        wind = PlanetaryConditions.WI_NONE;
        fog = PlanetaryConditions.FOG_NONE;

        int roll = Compute.randomInt(10) + 1;
        int r2 = Compute.d6();
        if (roll < 6) return;
        else if (roll == 6) {
            if (r2 < 4) weather = PlanetaryConditions.WE_LIGHT_RAIN;
            else if (r2 < 6) weather = PlanetaryConditions.WE_MOD_RAIN;
            else weather = PlanetaryConditions.WE_HEAVY_RAIN;
        } else if (roll == 7) {
            if (r2 < 4) weather = PlanetaryConditions.WE_LIGHT_SNOW;
            else if (r2 < 6) weather = PlanetaryConditions.WE_MOD_SNOW;
            else weather = PlanetaryConditions.WE_HEAVY_SNOW;
        } else if (roll == 8) {
            if (r2 < 4) wind = PlanetaryConditions.WI_LIGHT_GALE;
            else if (r2 < 6) wind = PlanetaryConditions.WI_MOD_GALE;
            else wind = PlanetaryConditions.WI_STRONG_GALE;
        } else if (roll == 9) {
            if (r2 == 1) wind = PlanetaryConditions.WI_STORM;
            else if (r2 == 2) weather = PlanetaryConditions.WE_DOWNPOUR;
            else if (r2 == 3) weather = PlanetaryConditions.WE_SLEET;
            else if (r2 == 4) weather = PlanetaryConditions.WE_ICE_STORM;
            else if (r2 == 5) wind = PlanetaryConditions.WI_TORNADO_F13; // tornadoes are classified as wind rather than weather.
            else if (r2 == 6) wind = PlanetaryConditions.WI_TORNADO_F4;
        } else {
            if (r2 < 5) fog = PlanetaryConditions.FOG_LIGHT;
            else fog = PlanetaryConditions.FOG_HEAVY;
        }
    }

    public void setPlanetaryConditions(Mission mission, Campaign campaign) {
        if (null != mission) {
            Planet p = Planets.getInstance().getPlanets().get(mission.getPlanetName());
            if (null != p) {
                atmosphere = Utilities.nonNull(p.getPressure(Utilities.getDateTimeDay(campaign.getCalendar())), atmosphere);
                gravity = Utilities.nonNull(p.getGravity(), gravity).floatValue();
            }
        }
    }

    public void setMapSize() {
        int roll = Compute.randomInt(20) + 1;
        if (roll < 6) {
            mapSizeX = 20;
            mapSizeY = 10;
        } else if (roll < 11) {
            mapSizeX = 10;
            mapSizeY = 20;
        } else if (roll < 13) {
            mapSizeX = 30;
            mapSizeY = 10;
        } else if (roll < 15) {
            mapSizeX = 10;
            mapSizeY = 30;
        } else if (roll < 19) {
            mapSizeX = 20;
            mapSizeY = 20;
        } else if (roll == 19) {
            mapSizeX = 40;
            mapSizeY = 10;
        } else {
            mapSizeX = 10;
            mapSizeY = 40;
        }
    }

    public int getMapX() {
        int base = mapSizeX + 5 * lanceCount;

        return (base > 20) ? base : 20;
    }

    public int getMapY() {
        int base = mapSizeY + 5 * lanceCount;

        return (base > 20) ? base : 20;
    }

	public int getBaseMapX() {
		return (5 * getLanceCount()) + getMapSizeX();
	}

	public int getBaseMapY() {
		return (5 * getLanceCount()) + getMapSizeY();
	}
    
    public void setMapFile() {
        final String[][] maps = {
            {"Sandy-hills", "Hills-craters", "Hills",
                "Wooded-hills", "Cliffs", "Town-hills"}, //hills
            {"Sandy-valley", "Rocky-valley", "Light-craters",
                "Heavy-craters", "Rubble-mountain", "Cliffs"}, //badlands
            {"Muddy-swamp", "Lake-marsh", "Lake-high",
                "Wooded-lake", "Swamp", "Wooded-swamp"}, //wetlands
            {"Town-mining", "Town-wooded", "Town-generic",
                "Town-farming", "Town-ruin", "Town-mountain"}, //light urban
            {"Savannah", "Dust-bowl", "Sandy-hills",
                "Town-ruin", "Town-generic", "Some-trees"}, //flatlands
            {"Some-trees", "Wooded-lake", "Woods-medium",
                "Wooded-hills", "Woods-deep", "Wooded-valley"}, //wooded
            {"Fortress-city", "Fortress-city", "Town-concrete",
                "Town-concrete", "City-high", "City-dense"}, //heavy urban
            {"River-huge", "Woods-river", "Sandy-river",
                "Rubble-river", "Seaport", "River-wetlands"}, //coastal
            {"Mountain-lake", "Cliffs-lake", "Rubble-mountain",
                "Cliffs", "Mountain-medium", "Mountain-high"} //mountains
        };

        map = maps[terrainType][Compute.d6() - 1];
    }

    public boolean canRerollTerrain() {
        return canRerollMap();
    }

    public boolean canRerollMapSize() {
        return true;
    }

    public boolean canRerollMap() {
    	return true;
    }

    public boolean canRerollLight() {
        return true;
    }

    public boolean canRerollWeather() {
    	return true;
    }

    /**
     * Determines whether a unit is eligible to deploy to the scenario. The
     * default is true, but some special missions and big battles restrict
     * the participants.
     *
     * @param unit
     * @param campaign
     * @return true if the unit is eligible, otherwise false
     */
    public boolean canDeploy(Unit unit, Campaign campaign) {
        if (isBigBattle() && (getForces(campaign).getAllUnits().size() > 7)) {
        	return false;
        } else if (isSpecialMission() && (getForces(campaign).getAllUnits().size() > 0)) {
        	return false;
        }
        
        return true;
    }

    /**
     * Determines whether a force is eligible to deploy to a scenario by
     * checking all units contained in the force
     *
     * @param force
     * @param campaign
     * @return true if the force is eligible to deploy, otherwise false
     */
    public boolean canDeploy(Force force, Campaign campaign) {
        Vector<UUID> units = force.getAllUnits();
        if (isBigBattle() &&
                getForces(campaign).getAllUnits().size() + units.size() > 8) {
            return false;
        } else if (isSpecialMission() &&
                getForces(campaign).getAllUnits().size() + units.size() > 0) {
            return false;
        }
        for (UUID id : units) {
            if (!canDeploy(campaign.getUnit(id), campaign)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether a list of units is eligible to deploy to the scenario.
     *
     * @param units
     * @param campaign
     * @return true if all units in the list are eligible, otherwise false
     */
    public boolean canDeployUnits(Vector<Unit> units, Campaign campaign) {
        if (isBigBattle()) {
            return getForces(campaign).getAllUnits().size() + units.size() <= 8;
        } else if (isSpecialMission()) {
            return getForces(campaign).getAllUnits().size() + units.size() <= 1;
        }
        for (Unit unit : units) {
            if (!canDeploy(unit, campaign)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether a list of forces is eligible to deploy to the scenario.
     *
     * @param units
     * @param campaign
     * @return true if all units in all forces in the list are eligible, otherwise false
     */
    public boolean canDeployForces(Vector<Force> forces, Campaign c) {
        int total = 0;
        for (Force force : forces) {
            Vector<UUID> units = force.getAllUnits();
            total += units.size();
            if (isBigBattle()) {
                return getForces(c).getAllUnits().size() + units.size() <= 8;
            } else if (isSpecialMission()) {
                return getForces(c).getAllUnits().size() + units.size() <= 0;
            }
            for (UUID id : units) {
                if (!canDeploy(c.getUnit(id), c)) {
                    return false;
                }
            }
        }
        if (isBigBattle()) {
            return getForces(c).getAllUnits().size() + total <= 8;
        } else if (isSpecialMission()) {
            return getForces(c).getAllUnits().size() + total <= 0;
        }
        return true;
    }

    /**
     * Corrects the enemy (special missions) and allies (big battles)
     * as necessary based on player deployments. This ought to be called
     * when the scenario details are displayed or the scenario is started.
     *
     * @param campaign
     */
    public void refresh(Campaign campaign) {
        if (isStandardMission()) {
            return;
        }
        Vector<UUID> deployed = getForces(campaign).getAllUnits();
        if (isBigBattle()) {
            int numAllies = Math.min(4, 8 - deployed.size());
            alliesPlayer.clear();
            for (int i = 0; i < numAllies; i++) {
                alliesPlayer.add(bigBattleAllies.get(i));
            }
        } else {
            if (deployed.size() == 0) {
                return;
            }
            int weight = campaign.getUnit(deployed.get(0)).getEntity().getWeightClass() - 1;
            /* In the event that Star League Cache 1 generates a primitive 'Mech,
             * the player can keep the 'Mech without a battle so no enemy
             * units are generated.
             */
            
            if(specMissionEnemies == null ){
                setForces(campaign);
            }
            
            if (specMissionEnemies != null && botForces.get(0) != null 
                    && specMissionEnemies.get(weight) != null) {
                botForces.get(0).setEntityList(specMissionEnemies.get(weight));
            }
        }
    }

    /**
     * Determines enemy and allied forces for the scenario. The forces for a standard
     * battle are based on the player's deployed lance. The enemy forces for
     * special missions depend on the weight class of the player's deployed
     * unit and the number of allies in big battles varies according to the
     * number the player deploys. Since the various possibilities are rather
     * limited, all possibilities are generated and the most appropriate is
     * chosen rather than rerolling every time the player changes. This is
     * both for efficiency and to prevent shopping.
     *
     * @param campaign
     */
    public void setForces(Campaign campaign) {
        if (isStandardMission()) {
            setStandardMissionForces(campaign);
        } else if (isSpecialMission()) {
            setSpecialMissionForces(campaign);
        } else {
            setBigBattleForces(campaign);
        }
    }

    /**
     * Generates attached allied units (bot or player controlled), the main
     * enemy force, any enemy reinforcements, and any additional forces
     * (such as civilian).
     *
     * @param campaign
     */
    private void setStandardMissionForces(Campaign campaign) {
        /* Find the number of attached units required by the command rights clause */
        int attachedUnitWeight = EntityWeightClass.WEIGHT_MEDIUM;
        if (lanceRole == Lance.ROLE_SCOUT || lanceRole == Lance.ROLE_TRAINING) {
            attachedUnitWeight = EntityWeightClass.WEIGHT_LIGHT;
        }
        int numAttachedPlayer = 0;
        int numAttachedBot = 0;
        if (getContract(campaign).getMissionType() == AtBContract.MT_CADREDUTY) {
            numAttachedPlayer = 3;
        } else if (campaign.getFactionCode().equals("MERC")) {
            if (getContract(campaign).getCommandRights() == Contract.COM_INTEGRATED) {
                numAttachedBot = 2;
            }
            if (getContract(campaign).getCommandRights() == Contract.COM_HOUSE) {
                numAttachedBot = 1;
            }
            if (getContract(campaign).getCommandRights() == Contract.COM_LIAISON) {
                numAttachedPlayer = 1;
            }
        }

        /* The entities in the attachedAllies list will be added to the player's forces
         * in MM and don't require a separate BotForce */
        for (int i = 0; i < numAttachedPlayer; i++) {
            Entity en = getEntity(getContract(campaign).getEmployerCode(),
                    getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
                    UnitType.MEK, attachedUnitWeight, campaign);
            if (null != en) {
                alliesPlayer.add(en);
                attachedUnitIds.add(UUID.fromString(en.getExternalIdAsString()));
            } else {
                System.out.println("Entity for player-controlled allies is null");
            }
        }

        /* The allyBot list will be passed to the BotForce constructor */
        ArrayList<Entity> allyEntities = new ArrayList<Entity>();
        for (int i = 0; i < numAttachedBot; i++) {
            Entity en = getEntity(getContract(campaign).getEmployerCode(),
                    getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
                    UnitType.MEK, attachedUnitWeight, campaign);
            if (null != en) {
                allyEntities.add(en);
                attachedUnitIds.add(UUID.fromString(en.getExternalIdAsString()));
            } else {
                System.err.println("Entity for ally bot is null");
            }
        }

        ArrayList<Entity> enemyEntities = new ArrayList<Entity>();
        
        setExtraMissionForces(campaign, allyEntities, enemyEntities);
        
        /* Possible enemy reinforcements */
        int roll = Compute.d6();
        if (roll > 3) {
            ArrayList<Entity> reinforcements = new ArrayList<Entity>();
            if (roll == 6) {
                addLance(reinforcements, getContract(campaign).getEnemyCode(),
                    getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
                    EntityWeightClass.WEIGHT_MEDIUM, EntityWeightClass.WEIGHT_ASSAULT, campaign, 8);
            } else {
                addLance(reinforcements, getContract(campaign).getEnemyCode(),
                        getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
                        EntityWeightClass.WEIGHT_LIGHT, EntityWeightClass.WEIGHT_ASSAULT, campaign, 6);
            }
            /* Must set per-entity start pos for units after start of scenarios. Reinforcements
             * arrive from the enemy home edge, which is not necessarily the start pos. */
            final int enemyDir = enemyHome;
            reinforcements.stream().filter(Objects::nonNull).forEach(en -> {
                en.setStartingPos(enemyDir);
            });
            BotForce bf = getEnemyBotForce(getContract(campaign), enemyHome, enemyHome, reinforcements);
            bf.setName(bf.getName() + " (Reinforcements)");
            addBotForce(bf);
        }

        if (campaign.getCampaignOptions().getUseDropShips()) {
        	if (canAddDropShips()) {
                boolean dropshipFound = false;
                for (UUID id : campaign.getForces().getAllUnits()) {
                    if ((campaign.getUnit(id).getEntity().getEntityType() & Entity.ETYPE_DROPSHIP) != 0 &&
                            campaign.getUnit(id).isAvailable()) {
                        addUnit(id);
                        campaign.getUnit(id).setScenarioId(getId());
                        dropshipFound = true;
                        break;
                    }
                }
                if (!dropshipFound) {
                    Entity dropship = getEntityByName("Leopard (2537)",
                            getContract(campaign).getEmployerCode(),
                            getContract(campaign).getAllySkill(),
                            campaign);
                    alliesPlayer.add(dropship);
                    attachedUnitIds.add(UUID.fromString(dropship.getExternalIdAsString()));
                }
                for (int i = 0; i < Compute.d6() - 3; i++) {
                    addLance(enemyEntities, getContract(campaign).getEnemyCode(),
                            getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
                            UnitMarket.getRandomWeight(UnitType.MEK, getContract(campaign).getEnemyCode(),
                                campaign.getCampaignOptions().getRegionalMechVariations()),
                            EntityWeightClass.WEIGHT_ASSAULT, campaign);
                }
            } else if (getLanceRole() == Lance.ROLE_SCOUT) {
                /* Set allied forces to deploy in (6 - speed) turns just as player's units,
                 * but only if not deploying by dropship.
                 */
                alliesPlayer.stream().filter(Objects::nonNull).forEach(entity -> {
                    int speed = entity.getWalkMP();
                    if (entity.getJumpMP() > 0) {
                        if (entity instanceof megamek.common.Infantry) {
                            speed = entity.getJumpMP();
                        } else {
                            speed++;
                        }
                    }
                    entity.setDeployRound(Math.max(0, 6 - speed));
                });
                allyEntities.stream().filter(Objects::nonNull).forEach(entity -> {
                    int speed = entity.getWalkMP();
                    if (entity.getJumpMP() > 0) {
                        if (entity instanceof megamek.common.Infantry) {
                            speed = entity.getJumpMP();
                        } else {
                            speed++;
                        }
                    }
                    entity.setDeployRound(Math.max(0, 6 - speed));
                });
            }
        }
    }

	@Override
	public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities, ArrayList<Entity> enemyEntities) {
        int enemyStart;
        int playerHome;
        
        start = playerHome = startPos[Compute.randomInt(4)];
        enemyStart = start + 4;
        
        if (enemyStart > 8) {
            enemyStart -= 8;
        }
        
        enemyHome = enemyStart;

        if (allyEntities.size() > 0) {
            addBotForce(getAllyBotForce(getContract(campaign), start, playerHome, allyEntities));
        }
        
        addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
        addBotForce(getEnemyBotForce(getContract(campaign), enemyHome, enemyHome, enemyEntities));
	}
    
    public boolean canAddDropShips() {
    	return false;
    }
    
    /**
     * Generate four sets of forces: one for each weight class the player
     * can choose to deploy.
     *
     * @param campaign
     */
    private void setSpecialMissionForces(Campaign campaign) {
        //enemy must always be the first on the botforce list so we can find it on refresh()
        specMissionEnemies = new ArrayList<ArrayList<Entity>>();
        
        ArrayList<Entity> enemyEntities = new ArrayList<Entity>();
        ArrayList<Entity> allyEntities = new ArrayList<Entity>();
        
        setExtraMissionForces(campaign, allyEntities, enemyEntities);
    }

    public ArrayList<ArrayList<Entity>> getSpecMissionEnemies() {
		return specMissionEnemies;
	}

	/**
     * Generates enemy forces and four allied units that may be used if the player
     * deploys fewer than eight of his or her own units.
     *
     * @param campaign
     */
    private void setBigBattleForces(Campaign campaign) {
        ArrayList<Entity> enemyEntities = new ArrayList<Entity>();
        ArrayList<Entity> allyEntities = new ArrayList<Entity>();
        
        setExtraMissionForces(campaign, allyEntities, enemyEntities);
        
        bigBattleAllies = new ArrayList<Entity>();
        
        for (Entity en : alliesPlayer) {
            bigBattleAllies.add(en);
        }
    }

    protected void addEnemyForce(ArrayList<Entity> list, int weightClass, Campaign c) {
        addEnemyForce(list, weightClass, EntityWeightClass.WEIGHT_ASSAULT, 0, 0, c);
    }

    /**
     * Generates the enemy force based on the weight class of the lance deployed
     * by the player. Certain scenario types may set a maximum weight class for
     * enemy units or modify the roll.
     *
     * @param list            All generated enemy entities are added to this list.
     * @param weightClass    The weight class of the player's lance.
     * @param maxWeight        The maximum weight class of each generated enemy entity
     * @param rollMod        Modifier to the enemy lances roll.
     * @param weightMod        Modifier to the weight class of enemy lances.
     * @param campaign
     */
    protected void addEnemyForce(ArrayList<Entity> list, int weightClass,
            int maxWeight, int rollMod, int weightMod, Campaign campaign) {
        String org = AtBConfiguration.ORG_IS;
        if (getContract(campaign).getEnemyCode().equals("CS")
                || getContract(campaign).getEnemyCode().equals("WOB")) {
            org = AtBConfiguration.ORG_CS;
        } else {
            Faction f = Faction.getFaction(getContract(campaign).getEnemyCode());
            if (f != null && f.isClan()) {
                org = AtBConfiguration.ORG_CLAN;
            }
        }
        
        String lances = campaign.getAtBConfig().selectBotLances(org, weightClass, rollMod/20f);
        int maxLances = Math.min(lances.length(), campaign.getCampaignOptions().getSkillLevel() + 1);
        
        for (int i = 0; i < maxLances; i++) {
            addEnemyLance(list, decodeWeightStr(lances, i) + weightMod,
                    maxWeight, campaign);
        }
    }

    /**
     * Generates an enemy lance of a given weight class.
     *
     * @param list            Generated enemy entities are added to this list.
     * @param weight        Weight class of the enemy lance.
     * @param maxWeight        Maximum weight of enemy entities.
     * @param campaign
     */
    private void addEnemyLance(ArrayList<Entity> list, int weight, int maxWeight, Campaign campaign) {
        if (weight < EntityWeightClass.WEIGHT_LIGHT) {
            weight = EntityWeightClass.WEIGHT_LIGHT;
        }
        if (weight > EntityWeightClass.WEIGHT_ASSAULT) {
            weight = EntityWeightClass.WEIGHT_ASSAULT;
        }
        addLance(list, getContract(campaign).getEnemyCode(),
                getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
                weight, maxWeight, campaign);
        lanceCount++;
    }

    /**
     * Determines the most appropriate RAT and uses it to generate a random Entity
     *
     * @param faction        The faction code to use for locating the correct RAT and assigning a crew name
     * @param skill            The RandomSkillGenerator constant that represents the skill level of the overall force.
     * @param quality        The equipment rating of the force.
     * @param unitType        The UnitTableData constant for the type of unit to generate.
     * @param weightClass    The weight class of the unit to generate
     * @param campaign
     * @return                A new Entity with crew.
     */
    protected Entity getEntity(String faction, int skill, int quality, int unitType, int weightClass, Campaign campaign) {
        MechSummary ms = null;
        if (unitType == UnitType.TANK) {
            if (campaign.getCampaignOptions().getOpforUsesVTOLs()) {
                ms = campaign.getUnitGenerator()
                        .generate(faction, unitType, weightClass, campaign.getCalendar()
                                .get(Calendar.YEAR), quality, IUnitGenerator.MIXED_TANK_VTOL, null);            
            } else {
                ms = campaign.getUnitGenerator()
                        .generate(faction, unitType, weightClass, campaign.getCalendar()
                                .get(Calendar.YEAR), quality, v -> !v.getUnitType().equals("VTOL"));
            }
        } else {
            ms = campaign.getUnitGenerator()
                    .generate(faction, unitType, weightClass, campaign.getCalendar()
                            .get(Calendar.YEAR), quality);
        }

        if (ms == null) {
            return null;
        }
        return createEntityWithCrew(faction, skill, campaign, ms);
    }

    /**
     * @param faction Faction to use for name generation
     * @param skill Skill rating of the crew
     * @param campaign The campaign instance
     * @param ms Which entity to generate
     * @return An crewed entity
     */
    protected Entity createEntityWithCrew(String faction, int skill,
            Campaign campaign, MechSummary ms) {
        Entity en = null;
        try {
            en = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
        } catch (Exception ex) {
            en = null;
            MekHQ.logError("Unable to load entity: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage());
            MekHQ.logError(ex);
            return null;
        }

        en.setOwner(campaign.getPlayer());
        en.setGame(campaign.getGame());

        Faction f = Faction.getFaction(faction);

        RandomNameGenerator rng = RandomNameGenerator.getInstance();
        rng.setChosenFaction(f.getNameGenerator());
        String crewName = rng.generate();

        RandomSkillsGenerator rsg = new RandomSkillsGenerator();
        rsg.setMethod(RandomSkillsGenerator.M_TAHARQA);
        rsg.setLevel(skill);

        if (f.isClan()) {
            rsg.setType(RandomSkillsGenerator.T_CLAN);
        }
        int[] skills = rsg.getRandomSkills(en);

        if (f.isClan() && Compute.d6(2) > 8 - getContract(campaign).getEnemySkill()
                + skills[0] + skills[1]) {
            int phenotype;
            switch (UnitType.determineUnitTypeCode(en)) {
            case UnitType.MEK:
                phenotype = Bloodname.P_MECHWARRIOR;
                break;
            case UnitType.BATTLE_ARMOR:
                phenotype = Bloodname.P_ELEMENTAL;
                break;
            case UnitType.AERO:
                phenotype = Bloodname.P_AEROSPACE;
                break;
            case UnitType.PROTOMEK:
                phenotype = Bloodname.P_PROTOMECH;
                break;
            default:
                phenotype = -1;
            }
            if (phenotype >= 0) {
                crewName += " " + Bloodname.randomBloodname(faction, phenotype, campaign.getCalendar().get(Calendar.YEAR)).getName();
            }
        }

        en.setCrew(new Crew(en.getCrew().getCrewType(), crewName,
                            Compute.getFullCrewSize(en),
                            skills[0], skills[1]));

        UUID id = UUID.randomUUID();
        while (null != entityIds.get(id)) {
            id = UUID.randomUUID();
        }
        en.setExternalIdAsString(id.toString());
        entityIds.put(id, en);
        
        return en;
    }

    /**
     * Generates a new Entity without using a RAT. Used for turrets and employer-assigned
     * Leopard Dropships.
     *
     * @param name            Full name (chassis + model) of the entity to generate.
     * @param fName            Faction code to use for crew name generation
     * @param skill            RandomSkillsGenerator.L_* constant for the average force skill level.
     * @param campaign
     * @return                A new Entity
     */
    protected Entity getEntityByName(String name, String fName, int skill, Campaign campaign) {
        MechSummary mechSummary = MechSummaryCache.getInstance().getMech(
                name);
        if (mechSummary == null) {
            return null;
        }

        MechFileParser mechFileParser = null;
        try {
            mechFileParser = new MechFileParser(mechSummary.getSourceFile(), mechSummary.getEntryName());
        } catch (Exception ex) {
            MekHQ.logError(ex);
            MekHQ.logError("Unable to load unit: " + name);
        }
        if (mechFileParser == null) {
            return null;
        }

        Entity en = mechFileParser.getEntity();

        en.setOwner(campaign.getPlayer());
        en.setGame(campaign.getGame());

        Faction faction = Faction.getFaction(fName);

        RandomNameGenerator rng = RandomNameGenerator.getInstance();
        rng.setChosenFaction(faction.getNameGenerator());

        RandomSkillsGenerator rsg = new RandomSkillsGenerator();
        rsg.setMethod(RandomSkillsGenerator.M_TAHARQA);
        rsg.setLevel(skill);

        if (faction.isClan()) rsg.setType(RandomSkillsGenerator.T_CLAN);
        int[] skills = rsg.getRandomSkills(en);
        en.setCrew(new Crew(en.getCrew().getCrewType(), rng.generate(),
                            Compute.getFullCrewSize(en),
                            skills[0], skills[1]));

        UUID id = UUID.randomUUID();
        while (null != entityIds.get(id)) {
            id = UUID.randomUUID();
        }
        en.setExternalIdAsString(id.toString());
        entityIds.put(id, en);

        return en;
    }

    /**
     * Units that exceed the maximum weight for individual entities in the scenario
     * are replaced in the lance by two lighter units.
     *
     * @param weights        A string of single-character letter codes for the weights of the units in the lance (e.g. "LMMH")
     * @param maxWeight        The maximum weight allowed for the force by the parameters of the scenario type
     * @return                A new String of the same format as weights
     */
    private String adjustForMaxWeight(String weights, int maxWeight) {
        String retVal = weights;
        if (maxWeight == EntityWeightClass.WEIGHT_HEAVY) {
            //Hide and Seek (defender)
            retVal = weights.replaceAll("A", "LM");
        } else if (maxWeight == EntityWeightClass.WEIGHT_MEDIUM) {
            //Probe, Recon Raid (attacker)
            retVal = weights.replaceAll("A", "MM");
            retVal = retVal.replaceAll("H", "LM");
        }
        return retVal;
    }

    /**
     * Adjust weights of units in a lance for factions that do not fit the typical
     * weight distribution.
     *
     * @param weights        A string of single-character letter codes for the weights of the units in the lance (e.g. "LMMH")
     * @param faction        The code of the faction to which the force belongs.
     * @return                A new String of the same format as weights
     */
    private String adjustWeightsForFaction(String weights, String faction) {
        /* Official AtB rules only specify DC, LA, and FWL; I have added
         * variations for some Clans.
         */
        String retVal = weights;
        if (faction.equals("DC")) {
            retVal = weights.replaceFirst("MM", "LH");
        }
        if ((faction.equals("LA") ||
                faction.equals("CCO") || faction.equals("CGB"))
                && weights.matches("[LM]{3,}")) {
            retVal = weights.replaceFirst("M", "H");
        }
        if (faction.equals("FWL") || faction.equals("CIH")) {
            retVal = weights.replaceFirst("HA", "HH");
        }
        return retVal;
    }

    /*
     * Convenience functions overloaded to provide default values.
     */
    protected void addLance(ArrayList<Entity> list, String faction,
            int skill, int quality, int weightClass, Campaign campaign) {
        addLance(list, faction, skill, quality, weightClass,
                EntityWeightClass.WEIGHT_ASSAULT, campaign, 0);
    }

    protected void addLance(ArrayList<Entity> list, String faction,
            int skill, int quality, int weightClass, int maxWeight, Campaign c) {
        addLance(list, faction, skill, quality, weightClass,
                maxWeight, c, 0);
    }

    /**
     *
     * Generates a lance of the indicated weight class. If the faction is Clan,
     * calls addStar instead. If the faction is CS/WoB, calls addLevelII.
     *
     * @param list            Generated Entities are added to this list.
     * @param faction        The faction code to use in generating the Entity
     * @param skill            The overall skill level of the force
     * @param quality        The force's equipment level
     * @param weightClass    The weight class of the lance or equivalent to generate
     * @param campaign
     * @param arrivalTurn    The turn in which the Lance is deployed in the scenario.
     */
    private void addLance(ArrayList<Entity> list, String faction, int skill, int quality, int weightClass,
            int maxWeight, Campaign campaign, int arrivalTurn) {
        if (Faction.getFaction(faction).isClan()) {
            addStar(list, faction, skill, quality, weightClass, maxWeight, campaign, arrivalTurn);
            return;
        }
        if (faction.equals("CS") || faction.equals("WOB")) {
            addLevelII(list, faction, skill, quality, weightClass, maxWeight, campaign, arrivalTurn);
            return;
        }

        String weights = adjustForMaxWeight(campaign.getAtBConfig()
                .selectBotUnitWeights(AtBConfiguration.ORG_IS, weightClass), maxWeight);

        int forceType = FORCE_MEK;
        if (campaign.getCampaignOptions().getUseVehicles()) {
            int totalWeight = campaign.getCampaignOptions().getOpforLanceTypeMechs() +
                    campaign.getCampaignOptions().getOpforLanceTypeMixed() +
                    campaign.getCampaignOptions().getOpforLanceTypeVehicles();
            if (totalWeight <= 0) {
                forceType = FORCE_MEK;
            } else {
                int roll = Compute.randomInt(totalWeight);
                if (roll < campaign.getCampaignOptions().getOpforLanceTypeVehicles()) {
                    forceType = FORCE_VEHICLE;
                } else if (roll < campaign.getCampaignOptions().getOpforLanceTypeVehicles() +
                        campaign.getCampaignOptions().getOpforLanceTypeMixed()) {
                    forceType = FORCE_MIXED;
                }
            }
        }
        if (forceType == FORCE_MEK && campaign.getCampaignOptions().getRegionalMechVariations()) {
            weights = adjustWeightsForFaction(weights, faction);
        }

        int[] unitTypes = new int[weights.length()];
        for (int i = 0; i < unitTypes.length; i++) {
            unitTypes[i] = (forceType == FORCE_VEHICLE)?UnitType.TANK:UnitType.MEK;
        }
        /* Distribute vehicles randomly(-ish) through mixed units */
        if (forceType == FORCE_MIXED) {
            for (int i = 0; i < weights.length() / 2; i++) {
                int j = Compute.randomInt(weights.length());
                while (unitTypes[j] == UnitType.TANK) {
                    j++;
                    if (j >= weights.length()) {
                        j = 0;
                    }
                }
                unitTypes[j] = UnitType.TANK;
            }
        }

        for (int i = 0; i < weights.length(); i++) {
            Entity en = getEntity(faction, skill, quality, unitTypes[i],
                    decodeWeightStr(weights, i),
                    campaign);
            if (null != en) {
                en.setDeployRound(arrivalTurn);
            }
            list.add(en);
            if (unitTypes[i] == UnitType.TANK && campaign.getCampaignOptions().getDoubleVehicles()) {
                en = getEntity(faction, skill, quality, unitTypes[i],
                        decodeWeightStr(weights, i),
                        campaign);
                if (null != en) {
                    en.setDeployRound(arrivalTurn);
                }
                list.add(en);
            }
        }
    }

    /**
     * Generates a Star of the indicated weight class.
     *
     * @param list            Generated Entities are added to this list.
     * @param faction        The faction code to use in generating the Entity
     * @param skill            The overall skill level of the force
     * @param quality        The force's equipment level
     * @param weightClass    The weight class of the lance or equivalent to generate
     * @param campaign
     * @param arrivalTurn    The turn in which the Lance is deployed in the scenario.
     */
    private void addStar(ArrayList<Entity> list, String faction, int skill, int quality, int weightClass, int maxWeight, Campaign campaign, int arrivalTurn) {
        int forceType = FORCE_MEK;
        /* 1 chance in 12 of a Nova, per AtB rules; CHH/CSL
         * close to 1/2, no chance for CBS. Added a chance to encounter
         * a vehicle Star in Clan second-line (rating C or lower) units,
         * or all unit ratings for CHH/CSL and CBS.
         */
        int roll = Compute.d6(2);
        int novaTarget = 11;
        if (faction.equals("CHH") || faction.equals("CSL")) {
            novaTarget = 8;
        } else if (faction.equals("CBS")) {
            novaTarget = 13;
        }
        int vehicleTarget = 4;
        if (!faction.equals("CHH") || !faction.equals("CSL")
                && !faction.equals("CBS")) {
            vehicleTarget -= quality;
        }

        if (roll >= novaTarget) {
            forceType = FORCE_NOVA;
        } else if (campaign.getCampaignOptions().getClanVehicles() &&
                roll <= vehicleTarget) {
            forceType = FORCE_VEHICLE;
        }

        String weights = adjustForMaxWeight(campaign.getAtBConfig()
                .selectBotUnitWeights(AtBConfiguration.ORG_CLAN, weightClass), maxWeight);

        int unitType = (forceType == FORCE_VEHICLE)?UnitType.TANK:UnitType.MEK;

        if (campaign.getCampaignOptions().getRegionalMechVariations()) {
            if (unitType == UnitType.MEK) {
                weights = adjustWeightsForFaction(weights, faction);
            }
            /* medium vees are rare among the Clans, FM:CC, p. 8 */
            if (unitType == UnitType.TANK) {
                weights = adjustWeightsForFaction(weights, "DC");
            }
        }

        int unitsPerPoint;
        switch (unitType) {
        case UnitType.TANK:
        case UnitType.AERO:
            unitsPerPoint = 2;
            break;
        case UnitType.PROTOMEK:
            unitsPerPoint = 5;
            break;
        case UnitType.MEK:
        case UnitType.INFANTRY:
        case UnitType.BATTLE_ARMOR:
        default:
            unitsPerPoint = 1;
            break;
        }

        /* Ensure Novas use Frontline tables to get best chance at Omnis */
        int tmpQuality = quality;
        if (forceType == FORCE_NOVA && quality < IUnitRating.DRAGOON_B) {
            tmpQuality = IUnitRating.DRAGOON_B;
        }
        for (int point = 0; point < weights.length(); point++) {
            for (int unit = 0; unit < unitsPerPoint; unit++) {
                Entity en = getEntity(faction, skill, tmpQuality, unitType,
                        decodeWeightStr(weights, point),
                        campaign);
                if (null != en) {
                    en.setDeployRound(arrivalTurn);
                }
                list.add(en);
            }
        }
        if (forceType == FORCE_NOVA || forceType == FORCE_VEENOVA) {
            unitType = forceType == FORCE_VEENOVA? UnitType.INFANTRY:UnitType.BATTLE_ARMOR;
            for (int i = 0; i < 5; i++) {
                Entity en = getEntity(faction, skill, quality,
                        unitType, -1, campaign);
                if (null != en) {
                    en.setDeployRound(arrivalTurn);
                }
                list.add(en);
            }
        }
    }

    /**
     * Generates a ComStar/WoB Level II of the indicated weight class.
     *
     * @param list            Generated Entities are added to this list.
     * @param faction        The faction code to use in generating the Entity
     * @param skill            The overall skill level of the force
     * @param quality        The force's equipment level
     * @param weightClass    The weight class of the lance or equivalent to generate
     * @param campaign
     * @param arrivalTurn    The turn in which the Lance is deployed in the scenario.
     */
    private void addLevelII(ArrayList<Entity> list, String faction, int skill, int quality, int weightClass,
            int maxWeight, Campaign campaign, int arrivalTurn) {
        String weights = adjustForMaxWeight(campaign.getAtBConfig()
                .selectBotUnitWeights(AtBConfiguration.ORG_CS, weightClass), maxWeight);

        int forceType = FORCE_MEK;
        int roll = Compute.d6();
        if (roll < 4) {
            forceType = FORCE_VEHICLE;
        } else if (roll < 6) {
            forceType = FORCE_MIXED;
        }

        int[] unitTypes = new int[weights.length()];
        for (int i = 0; i < unitTypes.length; i++) {
            unitTypes[i] = (forceType == FORCE_VEHICLE)?UnitType.TANK:UnitType.MEK;
        }
        /* Distribute vehicles randomly(-ish) through mixed units */
        if (forceType == FORCE_MIXED) {
            for (int i = 0; i < weights.length() / 2; i++) {
                int j = Compute.randomInt(weights.length());
                while (unitTypes[j] == UnitType.TANK) {
                    j++;
                    if (j >= weights.length()) {
                        j = 0;
                    }
                }
                unitTypes[j] = UnitType.TANK;
            }
        }

        for (int i = 0; i < weights.length(); i++) {
            Entity en = getEntity(faction, skill, quality, unitTypes[i],
                    decodeWeightStr(weights, i),
                    campaign);
            if (null != en) {
                en.setDeployRound(arrivalTurn);
            }
            list.add(en);
            if (unitTypes[i] == UnitType.TANK && campaign.getCampaignOptions().getDoubleVehicles()) {
                en = getEntity(faction, skill, quality, unitTypes[i],
                        decodeWeightStr(weights, i),
                        campaign);
                if (null != en) {
                    en.setDeployRound(arrivalTurn);
                }
                list.add(en);
            }
        }
    }

    /**
     * Translates character code in the indicated position to the appropriate weight
     * class constant.
     *
     * @param s        A String of single-character codes that indicate the weight classes of the units in a lance (e.g. "LMMH")
     * @param i        The index of the code to be translated
     * @return        The value used by UnitTableData to find the correct RAT for the weight class
     */
    private int decodeWeightStr(String s, int i) {
        switch (s.charAt(i)) {
        case 'L': return EntityWeightClass.WEIGHT_LIGHT;
        case 'M': return EntityWeightClass.WEIGHT_MEDIUM;
        case 'H': return EntityWeightClass.WEIGHT_HEAVY;
        case 'A': return EntityWeightClass.WEIGHT_ASSAULT;
        }
        return 0;
    }

    /**
     * Generates the indicated number of civilian entities.
     *
     * @param list        Generated entities are added to this list
     * @param num        The number of civilian entities to generate
     * @param campaign
     */
    protected void addCivilianUnits(ArrayList<Entity> list, int num, Campaign campaign) {
        RandomUnitGenerator.getInstance().setChosenRAT("CivilianUnits");
        ArrayList<MechSummary> msl = RandomUnitGenerator.getInstance().generate(num);
        
        List<Entity> entities = msl.stream().map(ms -> createEntityWithCrew("IND",
                RandomSkillsGenerator.L_GREEN, campaign, ms))
                .collect(Collectors.<Entity>toList());
        list.addAll(entities);
    }

    /* Convenience methods for frequently-used arguments */
    protected BotForce getAllyBotForce(AtBContract c, int start, int home, ArrayList<Entity> entities) {
        return new BotForce(c.getAllyBotName(), 1, start, home, entities,
                c.getAllyCamoCategory(),
                c.getAllyCamoFileName(),
                c.getAllyColorIndex());
    }

    protected BotForce getEnemyBotForce(AtBContract c, int start, ArrayList<Entity> entities) {
        return getEnemyBotForce(c, start, start, entities);
    }

    protected BotForce getEnemyBotForce(AtBContract c, int start, int home, ArrayList<Entity> entities) {
        return new BotForce(c.getEnemyBotName(), 2, start, home, entities,
                c.getEnemyCamoCategory(),
                c.getEnemyCamoFileName(),
                c.getEnemyColorIndex());
    }
	
	public ArrayList<String> generateEntityStub(ArrayList<Entity> entities) {
        ArrayList<String> stub = new ArrayList<String>();
        for (Entity en : entities) {
            if (null == en) {
                stub.add("<html><font color='red'>No random assignment table found for faction</font></html>");
            } else {
                stub.add("<html>" + en.getCrew().getName() + " (" +
                        en.getCrew().getGunnery() + "/" +
                        en.getCrew().getPiloting() + "), " +
                        "<i>" + en.getShortName() + "</i>" +
                        "</html>");
            }
        }
        return stub;
    }

    public BotForceStub generateBotStub(BotForce bf) {
        return new BotForceStub("<html>" +
                    bf.getName() + " <i>" +
                    ((bf.getTeam() == 1)?"Allied":"Enemy") + "</i>" +
                    " Start: " + IStartingPositions.START_LOCATION_NAMES[bf.getStart()] +
                    "</html>",
                    generateEntityStub(bf.getEntityList()));
    }

    @Override
    public void generateStub(Campaign c) {
        super.generateStub(c);
        for (BotForce bf : botForces) {
            botForceStubs.add(generateBotStub(bf));
        }
        alliesPlayerStub = generateEntityStub(alliesPlayer);

        botForces.clear();
        alliesPlayer.clear();
        if (null != bigBattleAllies) {
            bigBattleAllies.clear();
        }
        if (null != specMissionEnemies) {
            specMissionEnemies.clear();
        }
    }

    public void doPostResolution(Campaign c, int contractBreaches, int bonusRolls) {
            getContract(c).addPlayerMinorBreaches(contractBreaches);
            for (int i = 0; i < bonusRolls; i++) {
                getContract(c).doBonusRoll(c);
            }
            
            if (RECONRAID == getScenarioType() && attacker &&
                    (getStatus() == S_VICTORY || getStatus() == S_MVICTORY)) {
                for (int i = 0; i < Compute.d6() - 2; i++) {
                    getContract(c).doBonusRoll(c);
                }
            }
    }

    @Override
    protected void writeToXmlEnd(PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "attacker", attacker);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "lanceForceId", lanceForceId);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "lanceRole", lanceRole);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "terrainType", terrainType);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "light", light);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "weather", weather);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "wind", wind);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "fog", fog);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "atmosphere", atmosphere);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "gravity", gravity);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "start", start);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "deploymentDelay", deploymentDelay);

        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<mapSize>"
                + mapSizeX + "," + mapSizeY
                +"</mapSize>");

        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "map", map);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "lanceCount", lanceCount);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "rerollsRemaining", rerollsRemaining);

        if (null != bigBattleAllies && bigBattleAllies.size() > 0) {
            pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<bigBattleAllies>");
            for (Entity entity : bigBattleAllies) {
                if (entity != null) {
                    pw1.println(writeEntityWithCrewToXmlString(entity, indent+2, bigBattleAllies));
                }
            }
            pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</bigBattleAllies>");
        } else if (alliesPlayer.size() > 0) {
            pw1.println(MekHqXmlUtil.indentStr(indent+1)+"<alliesPlayer>");
            for (Entity entity : alliesPlayer) {
                if (entity != null) {
                    pw1.println(writeEntityWithCrewToXmlString(entity, indent+2, alliesPlayer));
                }
            }
            pw1.println(MekHqXmlUtil.indentStr(indent+1)+"</alliesPlayer>");
        }

        for (BotForce botForce : botForces) {
            pw1.println(MekHqXmlUtil.indentStr(indent+1)+"<botForce>");
            botForce.writeToXml(pw1, indent+1);
            pw1.println(MekHqXmlUtil.indentStr(indent+1)+"</botForce>");
        }

        if (alliesPlayerStub.size() > 0) {
            pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<alliesPlayerStub>");
            for (String stub : alliesPlayerStub) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2,
                        "entityStub", MekHqXmlUtil.escape(stub));
            }
            pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</alliesPlayerStub>");
        }

        for (BotForceStub bot : botForceStubs) {
            pw1.println(MekHqXmlUtil.indentStr(indent+1)
                    + "<botForceStub name=\""
                    + MekHqXmlUtil.escape(bot.name) + "\">");
            for (String entity : bot.getEntityList()) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2,
                        "entityStub", MekHqXmlUtil.escape(entity));
            }
            pw1.println(MekHqXmlUtil.indentStr(indent+1)
                    + "</botForceStub>");
        }

        if (attachedUnitIds.size() > 0) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "attachedUnits", getCsvFromList(attachedUnitIds));
        }
        if (survivalBonus.size() > 0) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "survivalBonus", getCsvFromList(survivalBonus));
        }

        if (null != specMissionEnemies && specMissionEnemies.size() > 0) {
            pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<specMissionEnemies>");
            for (int i = 0; i < specMissionEnemies.size(); i++) {
                pw1.println(MekHqXmlUtil.indentStr(indent+2) + "<playerWeight class=\"" + i + "\">");
                for (Entity entity : specMissionEnemies.get(i)) {
                    if (entity != null) {
                        pw1.println(writeEntityWithCrewToXmlString(entity, indent+3, specMissionEnemies.get(i)));
                    }
                }
                pw1.println(MekHqXmlUtil.indentStr(indent+2) + "</playerWeight>");
            }
            pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</specMissionEnemies>");
        }

        super.writeToXmlEnd(pw1, indent);
    }

    /* MekHqXmlUtil.writeEntityToXmlString does not include the crew,
     * as crew is handled by the Person class in MekHQ. This utility
     * function will insert a pilot tag (and also a deployment attribute,
     * which is also not added by the MekHqXmlUtil method).
     */
    public static String writeEntityWithCrewToXmlString(Entity tgtEnt, int indentLvl, ArrayList<Entity> list) {
        String retVal = MekHqXmlUtil.writeEntityToXmlString(tgtEnt, indentLvl, list);
        
        String crew = MekHqXmlUtil.indentStr(indentLvl+1) + "<crew crewType=\""
                    + tgtEnt.getCrew().getCrewType().toString().toLowerCase()
                    + "\" size=\"" + tgtEnt.getCrew().getSize();
        if (tgtEnt.getCrew().getInitBonus() != 0) {
            crew += "\" initB=\""
                + String.valueOf(tgtEnt.getCrew().getInitBonus());
        }
        if (tgtEnt.getCrew().getCommandBonus() != 0) {
            crew += "\" commandB=\""
                + String.valueOf(tgtEnt.getCrew().getCommandBonus());
        }
        if (tgtEnt instanceof Mech) {
            crew += "\" autoeject=\"" + ((Mech)tgtEnt).isAutoEject();
        }
        crew += "\" ejected=\"" + tgtEnt.getCrew().isEjected() + "\">\n";
        for (int i = 0; i < tgtEnt.getCrew().getSlotCount(); i++) {
            crew += MekHqXmlUtil.indentStr(indentLvl+2) + "<crewMember slot=\"" + i 
                    + "\" name=\""
                    + MekHqXmlUtil.escape(tgtEnt.getCrew().getName(i))
                    + "\" nick=\""
                    + MekHqXmlUtil.escape(tgtEnt.getCrew().getNickname(i))
                    + "\" gunnery=\""
                    + tgtEnt.getCrew().getGunnery(i)
                    + "\" piloting=\""
                    + tgtEnt.getCrew().getPiloting(i);

            if (tgtEnt.getCrew().getToughness(i) != 0) {
                crew += "\" toughness=\""
                    + String.valueOf(tgtEnt.getCrew().getToughness(i));
            }
            if (tgtEnt.getCrew().isDead(i) || tgtEnt.getCrew().getHits(i) >= Crew.DEATH) {
                crew +="\" hits=\"Dead";
            } else if (tgtEnt.getCrew().getHits(i) > 0) {
                crew += "\" hits=\""
                    + String.valueOf(tgtEnt.getCrew().getHits(i));
            }

            crew += "\" externalId=\""
                     + tgtEnt.getCrew().getExternalIdAsString(i);
            crew += "\"/>\n";
        }
        crew += MekHqXmlUtil.indentStr(indentLvl+1) + "</crew>\n";
        
        return retVal.replaceFirst(">", " deployment=\"" +
            tgtEnt.getDeployRound() + "\">\n" + crew + "\n");
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) throws ParseException {
        super.loadFieldsFromXmlNode(wn);
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("attacker")) {
                attacker = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("lanceForceId")) {
                lanceForceId = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("lanceRole")) {
                lanceRole = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("terrainType")) {
                terrainType = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("light")) {
                light = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("weather")) {
                weather = Integer.parseInt(wn2.getTextContent());
                if(weather > PlanetaryConditions.WE_SIZE || weather < PlanetaryConditions.WE_NONE)
                {
                	weather = PlanetaryConditions.WE_NONE;
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("wind")) {
                wind = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("fog")) {
                fog = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("atmosphere")) {
                atmosphere = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("gravity")) {
                gravity = Float.parseFloat(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("start")) {
                start = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("deploymentDelay")) {
                deploymentDelay = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("mapSize")) {
                String []xy = wn2.getTextContent().split(",");
                mapSizeX = Integer.parseInt(xy[0]);
                mapSizeY = Integer.parseInt(xy[1]);
            } else if (wn2.getNodeName().equalsIgnoreCase("map")) {
                map = wn2.getTextContent().trim();
            } else if (wn2.getNodeName().equalsIgnoreCase("lanceCount")) {
                lanceCount = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("rerollsRemaining")) {
                rerollsRemaining = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("alliesPlayer")) {
                NodeList nl2 = wn2.getChildNodes();
                for (int i = 0; i < nl2.getLength(); i++) {
                    Node wn3 = nl2.item(i);
                    if (wn3.getNodeName().equalsIgnoreCase("entity")) {
                        Entity en = null;
                        try {
                            en = MekHqXmlUtil.getEntityFromXmlString(wn3);
                            if (wn3.getAttributes().getNamedItem("deployment") != null) {
                                en.setDeployRound(Math.max(0,
                                        Integer.parseInt(wn3.getAttributes().getNamedItem("deployment").getTextContent())));
                            }
                        } catch (Exception e) {
                            MekHQ.logError("Error loading allied unit in scenario");
                            MekHQ.logError(e);
                        }
                        if (en != null) {
                            alliesPlayer.add(en);
                            entityIds.put(UUID.fromString(en.getExternalIdAsString()), en);
                        }
                    }
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("bigBattleAllies")) {
                bigBattleAllies = new ArrayList<Entity>();
                NodeList nl2 = wn2.getChildNodes();
                for (int i = 0; i < nl2.getLength(); i++) {
                    Node wn3 = nl2.item(i);
                    if (wn3.getNodeName().equalsIgnoreCase("entity")) {
                        Entity en = null;
                        try {
                            en = MekHqXmlUtil.getEntityFromXmlString(wn3);
                            if (wn3.getAttributes().getNamedItem("deployment") != null) {
                                en.setDeployRound(Math.max(0,
                                        Integer.parseInt(wn3.getAttributes().getNamedItem("deployment").getTextContent())));
                            }
                        } catch (Exception e) {
                            MekHQ.logError("Error loading allied unit in scenario");
                            MekHQ.logError(e);
                        }
                        if (en != null) {
                            bigBattleAllies.add(en);
                            entityIds.put(UUID.fromString(en.getExternalIdAsString()), en);
                        }
                    }
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("specMissionEnemies")) {
                specMissionEnemies = new ArrayList<ArrayList<Entity>>();
                for (int i = 0; i < 4; i++) {
                    specMissionEnemies.add(new ArrayList<Entity>());
                }
                NodeList nl2 = wn2.getChildNodes();
                for (int i = 0; i < nl2.getLength(); i++) {
                    Node wn3 = nl2.item(i);
                    if (wn3.getNodeName().equalsIgnoreCase("playerWeight")) {
                        int weightClass = Integer.parseInt(wn3.getAttributes().getNamedItem("class").getTextContent());
                        NodeList nl3 = wn3.getChildNodes();
                        for (int j = 0; j < nl3.getLength(); j++) {
                            Node wn4 = nl3.item(j);
                            if (wn4.getNodeName().equalsIgnoreCase("entity")) {
                                Entity en = null;
                                try {
                                    en = MekHqXmlUtil.getEntityFromXmlString(wn4);
                                    if (wn4.getAttributes().getNamedItem("deployment") != null) {
                                        en.setDeployRound(Math.max(0,
                                                Integer.parseInt(wn4.getAttributes().getNamedItem("deployment").getTextContent())));
                                    }
                                } catch (Exception e) {
                                    MekHQ.logError("Error loading allied unit in scenario");
                                    MekHQ.logError(e);
                                }
                                if (null != en) {
                                    specMissionEnemies.get(weightClass).add(en);
                                    entityIds.put(UUID.fromString(en.getExternalIdAsString()), en);
                                }
                            }
                        }
                    }
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("botForce")) {
                BotForce bf = new BotForce();
                try {
                    bf.setFieldsFromXmlNode(wn2);
                } catch (Exception e) {
                    MekHQ.logError("Error loading allied unit in scenario");
                    MekHQ.logError(e);
                    bf = null;
                }
                if (null != bf) {
                    addBotForce(bf);
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("alliesPlayerStub")) {
                alliesPlayerStub = getEntityStub(wn2);
            } else if (wn2.getNodeName().equalsIgnoreCase("botForceStub")) {
                String name = MekHqXmlUtil.unEscape(wn2.getAttributes().getNamedItem("name").getTextContent());
                ArrayList<String> stub = getEntityStub(wn2);
                botForceStubs.add(new BotForceStub(name, stub));
            } else if (wn2.getNodeName().equalsIgnoreCase("attachedUnits")) {
                String ids[] = wn2.getTextContent().split(",");
                for (String s : ids) {
                    attachedUnitIds.add(UUID.fromString(s));
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("survivalBonus")) {
                String ids[] = wn2.getTextContent().split(",");
                for (String s : ids) {
                    survivalBonus.add(UUID.fromString(s));
                }
            }
        }
        /* In the event a discrepancy occurs between a RAT entry and the unit lookup name,
         * remove the entry from the list of entities that give survival bonuses
         * to avoid an critical error that prevents battle resolution.
         */
        ArrayList<UUID> toRemove = new ArrayList<UUID>();
        for (UUID uid : survivalBonus) {
            if (!entityIds.containsKey(uid)) {
                toRemove.add(uid);
            }
        }
        survivalBonus.removeAll(toRemove);
    }

    private ArrayList<String> getEntityStub(Node wn) {
        ArrayList<String> stub = new ArrayList<String>();
        NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("entityStub")) {
                stub.add(MekHqXmlUtil.unEscape(wn2.getTextContent()));
            }
        }
        return stub;
    }

    protected String getCsvFromList(ArrayList<? extends Object> list) {
        String retVal = new String();
        for (int i = 0; i < list.size(); i++) {
            retVal += list.get(i).toString();
            if (i < list.size() - 1) {
                retVal += ",";
            }
        }
        return retVal;
    }

    public AtBContract getContract(Campaign c) {
        return (AtBContract)c.getMission(getMissionId());
    }

    public int getLanceForceId() {
        return lanceForceId;
    }

    public int getLanceRole() {
        return lanceRole;
    }

    public Lance getLance(Campaign c) {
        return c.getLances().get(lanceForceId);
    }

    public void setLance(Lance l) {
        lanceForceId = l.getForceId();
    }

    public boolean isAttacker() {
        return attacker;
    }

    public void setAttacker(boolean attacker) {
        this.attacker = attacker;
    }

    public ArrayList<Entity> getAlliesPlayer() {
        return alliesPlayer;
    }

    public ArrayList<UUID> getAttachedUnitIds() {
        return attachedUnitIds;
    }

    public ArrayList<UUID> getSurvivalBonusIds() {
        return survivalBonus;
    }

    public Entity getEntity(UUID id) {
        return entityIds.get(id);
    }

    public void addBotForce(BotForce botForce) {
    	botForces.add(botForce);
    }
    
    public BotForce getBotForce(int i) {
        return botForces.get(i);
    }

    public int getNumBots() {
        if (isCurrent()) {
            return botForces.size();
        } else {
            return botForceStubs.size();
        }
    }

    public ArrayList<String> getAlliesPlayerStub() {
        return alliesPlayerStub;
    }

    public ArrayList<BotForceStub> getBotForceStubs() {
        return botForceStubs;
    }

    public int getTerrainType() {
        return terrainType;
    }

    public void setTerrainType(int terrainType) {
        this.terrainType = terrainType;
    }

    public int getLight() {
        return light;
    }

    public void setLight(int light) {
        this.light = light;
    }

    public int getWeather() {
        return weather;
    }

    public void setWeather(int weather) {
        this.weather = weather;
    }

    public int getWind() {
        return wind;
    }

    public void setWind(int wind) {
        this.wind = wind;
    }

    public int getFog() {
        return fog;
    }

    public void setFog(int fog) {
        this.fog = fog;
    }

    public int getAtmosphere() {
        return atmosphere;
    }

    public void setAtmosphere(int atmosphere) {
        this.atmosphere = atmosphere;
    }

    public float getGravity() {
        return gravity;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getDeploymentDelay() {
        return deploymentDelay;
    }

    public void setDeploymentDelay(int delay) {
        this.deploymentDelay = delay;
    }

    public int getMapSizeX() {
		return mapSizeX;
	}

	public void setMapSizeX(int mapSizeX) {
		this.mapSizeX = mapSizeX;
	}

	public int getMapSizeY() {
		return mapSizeY;
	}

	public void setMapSizeY(int mapSizeY) {
		this.mapSizeY = mapSizeY;
	}

	public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public int getLanceCount() {
        return lanceCount;
    }

    public void setLanceCount(int lanceCount) {
        this.lanceCount = lanceCount;
    }

    public int getRerollsRemaining() {
        return rerollsRemaining;
    }

    public void useReroll() {
        rerollsRemaining--;
    }

    public class BotForce implements Serializable, MekHqXmlSerializable {
        /**
         *
         */
        private static final long serialVersionUID = 8259058549964342518L;

        private String name;
        private ArrayList<Entity> entityList;
        private int team;
        private int start;
        private String camoCategory;
        private String camoFileName;
        private int colorIndex;
        private BehaviorSettings behaviorSettings;

        public BotForce() {
            this.entityList = new ArrayList<Entity>();
            try {
                behaviorSettings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR.getCopy();
            } catch (PrincessException ex) {
                MekHQ.logError("Error getting Princess default behaviors");
                MekHQ.logError(ex);
            }
        };

        public BotForce(String name, int team, int start, ArrayList<Entity> entityList) {
            this(name, team, start, start, entityList, Player.NO_CAMO, null, -1);
        }

        public BotForce(String name, int team, int start, int home, ArrayList<Entity> entityList) {
            this(name, team, start, home, entityList, Player.NO_CAMO, null, -1);
        }

        public BotForce(String name, int team, int start, int home, ArrayList<Entity> entityList,
                String camoCategory, String camoFileName, int colorIndex) {
            this.name = name;
            this.team = team;
            this.start = start;
            this.entityList = entityList;
            this.camoCategory = camoCategory;
            this.camoFileName = camoFileName;
            this.colorIndex = colorIndex;
            try {
                behaviorSettings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR.getCopy();
            } catch (PrincessException ex) {
                MekHQ.logError("Error getting Princess default behaviors");
                MekHQ.logError(ex);
            }
            behaviorSettings.setHomeEdge(findHomeEdge(home));
        }

        /* Convert from MM's Board to Princess's HomeEdge */
        public HomeEdge findHomeEdge(int start) {
            switch (start) {
            case Board.START_N:
                return HomeEdge.NORTH;
            case Board.START_S:
                return HomeEdge.SOUTH;
            case Board.START_E:
                return HomeEdge.EAST;
            case Board.START_W:
                return HomeEdge.WEST;
            case Board.START_NW:
                return (Compute.randomInt(2) == 0)?HomeEdge.NORTH:HomeEdge.WEST;
            case Board.START_NE:
                return (Compute.randomInt(2) == 0)?HomeEdge.NORTH:HomeEdge.EAST;
            case Board.START_SW:
                return (Compute.randomInt(2) == 0)?HomeEdge.SOUTH:HomeEdge.WEST;
            case Board.START_SE:
                return (Compute.randomInt(2) == 0)?HomeEdge.SOUTH:HomeEdge.EAST;
            default:
                return HomeEdge.getHomeEdge(Compute.randomInt(4));
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ArrayList<Entity> getEntityList() {
            return entityList;
        }

        public void setEntityList(ArrayList<Entity> entityList) {
            this.entityList = entityList;
        }

        public int getTeam() {
            return team;
        }

        public void setTeam(int team) {
            this.team = team;
        }

        public int getStart() {
            return start;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public String getCamoCategory() {
            return camoCategory;
        }

        public void setCamoCategory(String camoCategory) {
            this.camoCategory = camoCategory;
        }

        public String getCamoFileName() {
            return camoFileName;
        }

        public void setCamoFileName(String camoFileName) {
            this.camoFileName = camoFileName;
        }

        public int getColorIndex() {
            return colorIndex;
        }

        public void setColorIndex(int index) {
            colorIndex = index;
        }

        public BehaviorSettings getBehaviorSettings() {
            return behaviorSettings;
        }

        public void setBehaviorSettings(BehaviorSettings behaviorSettings) {
            this.behaviorSettings = behaviorSettings;
        }

        public void setHomeEdge(int i) {
            behaviorSettings.setHomeEdge(findHomeEdge(1));
        }

        public void writeToXml(PrintWriter pw1, int indent) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "name", MekHqXmlUtil.escape(name));
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "team", team);
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "start", start);
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "camoCategory", MekHqXmlUtil.escape(camoCategory));
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "camoFileName", MekHqXmlUtil.escape(camoFileName));
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "colorIndex", colorIndex);

            pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<entities>");
            for (Entity en : entityList) {
                if (en != null) {
                    pw1.println(writeEntityWithCrewToXmlString(en, indent + 2, entityList));
                }
            }
            pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</entities>");

            pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<behaviorSettings>");
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "forcedWithdrawal", behaviorSettings.isForcedWithdrawal());
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "goHome", behaviorSettings.shouldGoHome());
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "autoFlee", behaviorSettings.shouldAutoFlee());
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "selfPreservationIndex", behaviorSettings.getSelfPreservationIndex());
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "fallShameIndex", behaviorSettings.getFallShameIndex());
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "hyperAggressionIndex", behaviorSettings.getHyperAggressionIndex());
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "homeEdge", behaviorSettings.getHomeEdge().ordinal());
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "herdMentalityIndex", behaviorSettings.getHerdMentalityIndex());
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "braveryIndex", behaviorSettings.getBraveryIndex());
            pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</behaviorSettings>");
        }

        public void setFieldsFromXmlNode(Node wn) {
            NodeList nl = wn.getChildNodes();
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    name = MekHqXmlUtil.unEscape(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("team")) {
                    team = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("start")) {
                    start = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("camoCategory")) {
                    camoCategory = MekHqXmlUtil.unEscape(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("camoFileName")) {
                    camoFileName = MekHqXmlUtil.unEscape(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("colorIndex")) {
                    colorIndex = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("entities")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int i = 0; i < nl2.getLength(); i++) {
                        Node wn3 = nl2.item(i);
                        if (wn3.getNodeName().equalsIgnoreCase("entity")) {
                            Entity en = null;
                            try {
                                en = MekHqXmlUtil.getEntityFromXmlString(wn3);
                                if (wn3.getAttributes().getNamedItem("deployment") != null) {
                                    en.setDeployRound(Math.max(0,
                                            Integer.parseInt(wn3.getAttributes().getNamedItem("deployment").getTextContent())));
                                }
                            } catch (Exception e) {
                                MekHQ.logError("Error loading allied unit in scenario");
                                MekHQ.logError(e);
                            }
                            if (en != null) {
                                entityList.add(en);
                                entityIds.put(UUID.fromString(en.getExternalIdAsString()), en);
                            }
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("behaviorSettings")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int i = 0; i < nl2.getLength(); i++) {
                        Node wn3 = nl2.item(i);
                        if (wn3.getNodeName().equalsIgnoreCase("forcedWithdrawal")) {
                            behaviorSettings.setForcedWithdrawal(Boolean.parseBoolean(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("goHome")) {
                            behaviorSettings.setGoHome(Boolean.parseBoolean(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("autoFlee")) {
                            behaviorSettings.setAutoFlee(Boolean.parseBoolean(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("selfPreservationIndex")) {
                            behaviorSettings.setSelfPreservationIndex(Integer.parseInt(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("fallShameIndex")) {
                            behaviorSettings.setFallShameIndex(Integer.parseInt(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("hyperAggressionIndex")) {
                            behaviorSettings.setHyperAggressionIndex(Integer.parseInt(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("homeEdge")) {
                            behaviorSettings.setHomeEdge(Integer.parseInt(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("herdMentalityIndex")) {
                            behaviorSettings.setHerdMentalityIndex(Integer.parseInt(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("braveryIndex")) {
                            behaviorSettings.setBraveryIndex(Integer.parseInt(wn3.getTextContent()));
                        }
                    }
                }
            }
        }

    }

    public int getEnemyHome() {
		return enemyHome;
	}

	public void setEnemyHome(int enemyHome) {
		this.enemyHome = enemyHome;
	}

	public class BotForceStub {
        private String name;
        private ArrayList<String> entityList;

        public BotForceStub(String name, ArrayList<String> entityList) {
            this.name = name;
            this.entityList = entityList;
        }

        public String getName() {
            return name;
        }

        public ArrayList<String> getEntityList() {
            return entityList;
        }
    }

}
