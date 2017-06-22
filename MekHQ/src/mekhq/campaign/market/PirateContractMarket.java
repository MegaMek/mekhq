package mekhq.campaign.market;

import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Set;

import megamek.client.RandomSkillsGenerator;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBPirateContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Faction.Tag;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planets;
import mekhq.campaign.universe.RandomFactionGenerator;

// This class handles functionality relating to "contract" generation 
// for the pirate-specific contract market
public class PirateContractMarket extends ContractMarket 
{
	private static final long serialVersionUID = 3597397030650454074L;
	
	Random random;

	public PirateContractMarket()
	{		
		super();
		random = new Random();
	}
	
	@Override
	public void generateContractOffers(Campaign campaign, boolean newCampaign) {
		if ((getContractGenerationPeriod() == TYPE_ATBMONTHLY && campaign.getCalendar().get(Calendar.DAY_OF_MONTH) == 1) ||
				newCampaign) {
			Contract[] list = getContracts().toArray(new Contract[getContracts().size()]);
			for (Contract c : list) {
				removeContract(c);
			}

			// per AtB, pirates always engage in 1-month mini-"contracts", or get a single mercenary contract
			// subject to some restrictions
			// They are also always treated as having an F unit rating.
			
			AtBContract c = generateAtBContract(campaign, IUnitRating.DRAGOON_F);
			
			if (c != null) 
			{
				getContracts().add(c);
			}
			
	        if (campaign.getCampaignOptions().getContractMarketReportRefresh()) {
	            campaign.addReport("<a href='CONTRACT_MARKET'>Contract market updated</a>");
	        }
		}
	}
	
	@Override
	protected AtBContract generateAtBContract(Campaign campaign, int unitRatingMod)
	{
		// pirate "contract" generation works differently per AtB
		// First, we roll 1d10 to figure out the mission type.
		// if we roll a 10, then generate a standard mercenary contract (rerolling Garrison duty) 
		// otherwise, 
		//		the employer is always "Pirate" 
		//		the mission type is determined from a separate table
		// 		the location is 1d6-2 jumps away
		//		the enemy is:
		//			for guerilla, extraction, objective, the planet's current owner
		//			for "planetary assault" (break force), even odds of the planet's current owner, mercs or pirates
		//			for "pirate hunt" (of you), even odds of the planet's current owner, mercs or pirates
		//		calculate jump path as normal
		//		set ally skill to your company's skill / rating to F (although it doesn't matter: you're on your own)
		//		set enemy rating as normal 
		//		length is 1 month
		//		contract clauses are independent command, 100% salvage, 0% support, battle loss, transport, overhead
		//		payment multiplier is 0, nobody is paying you for piracy
		//		calculate parts availability level as normal
		//		required lances are 0
		//		"init contract details", "calculatecontract"
		
		int roll = random.nextInt(10);
		
		if(roll == 9)
		{
			String employerCode = RandomFactionGenerator.getInstance().getEmployer();
			return generateAtBContract(campaign, employerCode, unitRatingMod, 3);
		}
		
		return generateAtBContract(campaign);
	}
	
	// overload of generateAtBContract from the base class (ContractMarket.java)
	// returns an instance of an AtBPirateContract instead of an AtBContract
	// takes only a campaign object, as a unit rating mod is unnecessary in this context
	protected AtBPirateContract generateAtBContract(Campaign campaign) 
	{
		AtBPirateContract contract = new AtBPirateContract("New Pirate Contract");
		incrementIDAndAddContract(contract);

        contract.setEmployerCode("PIR", campaign.getEra());
		contract.setMissionType(findAtBMissionType());

		pickPlanetAndEnemy(campaign, contract);
		
		boolean isAttacker = contract.getMissionType() != AtBContract.MT_PIRATEHUNTING;
		
		JumpPath jp = null;
		try {
			jp = campaign.calculateJumpPath(campaign.getCurrentPlanet(), contract.getPlanet());
		} catch (NullPointerException ex) {
			// could not calculate jump path; leave jp null
		}
		if (jp == null) {
			return null;
		}

		contract.setAllySkill(RandomSkillsGenerator.L_GREEN);
		contract.setAllyQuality(IUnitRating.DRAGOON_F);
		setEnemyRating(contract, isAttacker, campaign.getCalendar().get(Calendar.YEAR));

		setAtBContractClauses(contract);
		contract.setMultiplier(0);
		contract.calculatePartsAvailabilityLevel(campaign);
       
		contract.initContractDetails(campaign);
        contract.calculateContract(campaign);
		return contract;
	}
	
	// pirate "contracts" are always Independent Command, no support, keep all salvage
	// and always last exactly one month
	protected void setAtBContractClauses(AtBContract contract)
	{
		contract.setCommandRights(Contract.COM_INDEP);
		contract.setSalvageExchange(false);
		contract.setSalvagePct(100);
		contract.setStraightSupport(0);
		contract.setBattleLossComp(0);
		contract.setTransportComp(0);
		contract.setLength(1);
	}
	
	protected int findAtBMissionType() 
	{
		// generate a number 0-8 to simulate a 1d10 that doesn't produce a 10 result 
		// we're here because we didn't generate a standard mercenary contract
		int roll = random.nextInt(9); 
		
		int[] pirateContractTypes = { 
				AtBContract.MT_GUERRILLAWARFARE,
				AtBContract.MT_EXTRACTIONRAID, AtBContract.MT_EXTRACTIONRAID,
				AtBContract.MT_OBJECTIVERAID, AtBContract.MT_OBJECTIVERAID, AtBContract.MT_OBJECTIVERAID, AtBContract.MT_OBJECTIVERAID,
				AtBContract.MT_PLANETARYASSAULT, // "Break Force"
				AtBContract.MT_PIRATEHUNTING // "Lay Low" - you're the pirates
		};
		
		return pirateContractTypes[roll];
	}
	
	private void pickPlanetAndEnemy(Campaign campaign, AtBContract contract)
	{
		// get planet: AtB states that it's always 1d6-2 jumps away. We thus limit our initial search to 
		// 120 ly (4 jumps maximum distance), and then refine it further
		List<Planet> targets = Planets.getInstance().getNearbyPlanets(campaign.getCurrentPlanet(), 120);
		if(targets.size() > 0)
		{
			boolean validTarget = false;
			
			
			while(!validTarget)
			{
				Planet target = Utilities.getRandomItem(targets);
				Set<Faction> planetOwners = target.getFactionSet(Utilities.getDateTimeDay(campaign.getCalendar()));
				// strip out all "abandoned" owners, as pirates aren't going to be raiding planets with nobody home
				// with the exception of 'pirate hunting' contracts, where some mercs can come after you
				planetOwners.removeIf(owner -> owner.is(Tag.ABANDONED));
				if(planetOwners.size() == 0 && contract.getMissionType() == AtBContract.MT_PIRATEHUNTING)
				{
					planetOwners.add(Faction.getFaction("MERC"));
				}
				
				if(planetOwners.size() > 0)
				{
					// make sure the planet is actually within 4 jumps at most
					JumpPath jp = null;
					try 
					{
						jp = campaign.calculateJumpPath(campaign.getCurrentPlanet(), target);
					} 
					catch (NullPointerException ex) 
					{
						continue;
					}
					
					if(jp == null || jp.getJumps() > 4)
					{
						continue;
					}
					
					Faction owner = Utilities.getRandomItem(planetOwners);
					// some factions (most notable, the "TerraCap Confederation" return null when getId() is called
					// we bullet proof against this situation and, in such a case, will simply attempt to get another target
					if(owner.getId() == null)
					{
						continue;
					}
					
					contract.setPlanetName(target.getId());
					contract.setEnemyCode(Faction.getFactionCode(owner.getId()));
					
					
					validTarget = true;
				}
			}
		}
		else
		{
			//somehow, we didn't find a planet within 120 years?
			return;
		}
	}
}
