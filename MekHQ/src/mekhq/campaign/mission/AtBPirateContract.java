package mekhq.campaign.mission;

import java.io.Serializable;

import mekhq.campaign.Campaign;
import mekhq.gui.view.LanceAssignmentView;

// Derived class from AtBContract
// Meant to handle pirate contract specific logic
public class AtBPirateContract extends AtBContract implements Serializable 
{
	private static final long serialVersionUID = -4370183074881214700L;

	public AtBPirateContract()
	{
		this(null);
	}
	
	public AtBPirateContract(String name)
	{
		super(name);
	}
	
	// override of initContractDetails from AtBContract
	// pirates do not require any lances to be deployed, nor does anyone pay them overhead compensation
	public void initContractDetails(Campaign campaign)
	{
		allyBotName = getEmployerName(campaign.getEra());
        enemyBotName = getEnemyName(campaign.getEra());
        setOverheadComp(AtBContract.OH_NONE);
		setRequiredLances(0);
	}
	
	// override of getRequiredLanceType from AtBContract
	// pirates don't require any kind of lance when looting and pillaging
	public static int getRequiredLanceType(int missionType)
	{
		return LanceAssignmentView.ROLE_NONE;
	}
}
