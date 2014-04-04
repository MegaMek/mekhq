package mekhq.campaign.personnel;

import mekhq.campaign.Campaign;

public class RankTranslator {
	// Old Factions Codes
	public static final int RT_SL =  0;
	public static final int RT_FS =  1;
	public static final int RT_LA =  2;
	public static final int RT_FWL = 3;
	public static final int RT_CC =  4;
	public static final int RT_DC =  5;
	public static final int RT_CL =  6;
	public static final int RT_CUSTOM = 7;
	public static final int RT_NUM = 8;
	
	public static final String[] oldRankNames = {
		"Star League", "Federated Sun", "Lyran Alliance", "Free Worlds League",
		"Capellan Confederation", "Draconis Combine", "Clan", "Custom"
	};
	
	public static final String[][] oldRankSystems = {
		{"None","Recruit","Private","Corporal","Sergeant","Master Sergeant","Warrant Officer","Lieutenant","Captain","Major","Colonel","Lt. General","Major General","General","Commanding General"},
		{"None","Recruit","Private","Private, FC","Corporal","Sergeant","Sergeant Major","Command Sergeant-Major","Cadet","Subaltern","Leftenant","Captain","Major","Leftenant Colonel","Colonel","Leftenant General","Major General","General","Marshal","Field Marshal","Marshal of the Armies"},
		{"None","Recruit","Private","Private, FC","Corporal","Senior Corporal","Sergeant","Staff Sergeant","Sergeant Major","Staff Sergeant Major","Senior Sergeant Major","Warrant Officer","Warrant Officer, FC","Senior Warrant Officer","Chief Warrant Officer","Cadet","Leutnant","First Leutnant","Hauptmann","Kommandant","Hauptmann-Kommandant","Leutnant-Colonel","Colonel","Leutnant-General","Hauptmann-General","Kommandant-General","General","General of the Armies","Archon"},
		{"None","Recruit","Private","Private, FC","Corporal","Sergeant","Staff Sergeant","Master Sergeant","Sergeant Major","Lieutenant","Captain","Force Commander","Lieutenant Colonel","Colonel","General","Marshal","Captain-General"},
		{"None","Shia-ben-bing","San-ben-bing","Si-ben-bing","Yi-si-ben-bing","Sao-wei","Sang-wei","Sao-shao","Zhong-shao","Sang-shao","Jiang-jun","Sang-jiang-jun"},
		{"None","Hojuhei","Heishi","Gunjin","Go-cho","Gunsho","Shujin","Kashira","Sho-ko","Chu-i","Tai-i","Sho-sa","Chu-sa","Tai-sa","Sho-sho","Tai-sho","Tai-shu","Gunji-no-Kanrei"},
		{"None","Point","Point Commander","Star Commander","Star Captain","Star Colonel","Galaxy Commander","Khan","ilKhan"}
	};
	
	private Campaign campaign;
	
	public RankTranslator(Campaign c) {
		campaign = c;
	}
	
	public int getNewRank(int oldSystem, int oldRank) throws ArrayIndexOutOfBoundsException {
		Ranks ranks = Ranks.getRanksFromSystem(RankTranslator.translateRankSystem(oldSystem, campaign.getFactionCode()));
		String rankName;
		
		// Try and acquire the rank name...
		try {
			rankName = oldRankSystems[oldSystem][oldRank];
			
			for (int rankNum = Ranks.RE_MIN; rankNum < Ranks.RC_NUM; rankNum++) {
				if (ranks.getRank(rankNum).getName(Ranks.RPROF_MW).equals(rankName)) {
					return rankNum;
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw e;
		}
		
		// If we didn't find anything to translate to, then we can kick them as Rank "None"
		return 0;
	}
	
	public static int translateRankSystem(int old, String faction) {
		switch(old) {
			case RT_SL: return Ranks.RS_SL;
			case RT_FS: return Ranks.RS_FS;
			case RT_LA: return Ranks.RS_LA;
			case RT_FWL: return Ranks.RS_FWL;
			case RT_CC: return Ranks.RS_CC;
			case RT_DC: return Ranks.RS_DC;
			case RT_CL: return Ranks.RS_CL;
			case RT_CUSTOM:
				switch (faction) {
					case "WOB": return Ranks.RS_WOB;
					case "FC": return Ranks.RS_FC;
					case "CS": return Ranks.RS_COM;
					case "CDS":
					case "CGB":
					case "CHH":
					case "CJF":
					case "CNC":
					case "CSJ":
					case "CSV":
					case "CW":
						return Ranks.RS_CL;
					case "OA": return Ranks.RS_OA;
					case "MH": return Ranks.RS_MH;
					case "TC": return Ranks.RS_TC;
					case "MOC": return Ranks.RS_MOC;
					case "FRR": return Ranks.RS_FRR;
				}
				return Ranks.RS_CUSTOM;
			default: return Ranks.RS_SL;
		}
	}
}
