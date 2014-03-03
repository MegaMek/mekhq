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
	
	private static final String[][] oldRankSystems = {
		{"None","Recruit","Private","Corporal","Sergeant","Master Sergeant","Warrant Officer","Lieutenant","Captain","Major","Colonel","Lt. General","Major General","General","Commanding General"},
		{"None","Recruit","Private","Private, FC","Corporal","Sergeant","Sergeant-Major","Command Sergeant-Major","Cadet","Subaltern","Leftenant","Captain","Major","Leftenant Colonel","Colonel","Leftenant General","Major General","General","Marshal","Field Marshal","Marshal of the Armies"},
		{"None","Recruit","Private","Private, FC","Corporal","Senior Corporal","Sergeant","Staff Sergeant","Sergeant Major","Staff Sergeant Major","Senior Sergeant Major","Warrant Officer","Warrant Officer, FC","Senior Warrant Officer","Chief Warrant Officer","Cadet","Leutnant","First Leutnant","Hauptmann","Kommandant","Hauptmann-Kommandant","Leutnant-Colonel","Colonel","Leutnant-General","Hauptmann-General","Kommandant-General","General","General of the Armies","Archon"},
		{"None","Recruit","Private","Private, FC","Corporal","Sergeant","Staff Sergeant","Master Sergeant","Sergeant Major","Lieutenant","Captain","Force Commander","Lieutenant Colonel","Colonel","General","Marshal","Captain-General"},
		{"None","Shia-ben-bing","San-ben-bing","Si-ben-bing","Yi-si-ben-bing","Sao-wei","Sang-wei","Sao-shao","Zhong-shao","Sang-shao","Jiang-jun","Sang-jiang-jun"},
		{"None","Hojuhei","Heishi","Gunjin","Go-cho","Gunsho","Shujin","Kashira","Sho-ko","Chu-i","Tai-i","Sho-sa","Chu-sa","Tai-sa","Sho-sho","Tai-sho","Tai-shu","Gunji-no-Kanrei"},
		{"None","Point","Point Commander","Star Commander","Star Captain","Star Colonel","Galaxy Commander","Khan","ilKhan"}
	};
	
	private Campaign campaign;
	
	RankTranslator(Campaign c) {
		campaign = c;
	}
	
	public int getNewRank(int oldSystem, int oldRank) {
		Ranks ranks = campaign.getRanks();
		String rankName = oldRankSystems[oldSystem][oldRank];
		for (int rankNum = Ranks.RE_MIN; rankNum < Ranks.RC_NUM; rankNum++) {
			if (ranks.getRank(rankNum).getName(Ranks.RPROF_MW).equals(rankName)) {
				return rankNum;
			}
		}
		
		// If we didn't find anything to translate to, then we can kick them as Rank "None"
		return 0;
	}
}
