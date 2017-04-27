package mekhq.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;

public class PartsAcquisitionService {
	private static Map<String, List<IAcquisitionWork>> acquisitionMap = null;
	private static Map<String, PartCountInfo> partCountInfoMap = new HashMap<String, PartCountInfo>();

	private static int inTransitCount = 0;
	private static int onOrderCount = 0;
	private static int omniPodCount = 0;
	private static int missingCount = 0;
	private static int requiredCount = 0;
	private static int unavailableCount = 0;
	private static long missingTotalPrice = 0;

	private PartsAcquisitionService() {
	}

	public static Map<String, List<IAcquisitionWork>> getAcquisitionMap() {
		return acquisitionMap;
	}

	public static void setAcquisitionMap(Map<String, List<IAcquisitionWork>> acquisitionMap) {
		PartsAcquisitionService.acquisitionMap = acquisitionMap;
	}

	public static Map<String, PartCountInfo> getPartCountInfoMap() {
		return partCountInfoMap;
	}

	public static void setPartCountInfoMap(Map<String, PartCountInfo> partCountInfoMap) {
		PartsAcquisitionService.partCountInfoMap = partCountInfoMap;
	}

	public static int getInTransitCount() {
		return inTransitCount;
	}

	public static void setInTransitCount(int inTransitCount) {
		PartsAcquisitionService.inTransitCount = inTransitCount;
	}

	public static int getOnOrderCount() {
		return onOrderCount;
	}

	public static void setOnOrderCount(int onOrderCount) {
		PartsAcquisitionService.onOrderCount = onOrderCount;
	}

	public static int getOmniPodCount() {
		return omniPodCount;
	}

	public static void setOmniPodCount(int omniPodCount) {
		PartsAcquisitionService.omniPodCount = omniPodCount;
	}

	public static int getMissingCount() {
		return missingCount;
	}

	public static void setMissingCount(int missingCount) {
		PartsAcquisitionService.missingCount = missingCount;
	}

	public static int getRequiredCount() {
		return requiredCount;
	}

	public static void setRequiredCount(int requiredCount) {
		PartsAcquisitionService.requiredCount = requiredCount;
	}

	public static int getUnavailableCount() {
		return unavailableCount;
	}

	public static void setUnavailableCount(int unavailableCount) {
		PartsAcquisitionService.unavailableCount = unavailableCount;
	}

	public static long getMissingTotalPrice() {
		return missingTotalPrice;
	}

	public static void setMissingTotalPrice(long missingTotalPrice) {
		PartsAcquisitionService.missingTotalPrice = missingTotalPrice;
	}

	public static void buildPartsList(Campaign campaign) {
		acquisitionMap = new HashMap<String, List<IAcquisitionWork>>();
		List<Unit> unitList = campaign.getServiceableUnits();

		for (Unit unit : unitList) {
			ArrayList<IAcquisitionWork> unitPartsList = campaign.getAcquisitionsForUnit(unit.getId());

			for (IAcquisitionWork aw : unitPartsList) {
				if (null == aw.getAcquisitionPart()) {
					continue;
				}

				List<IAcquisitionWork> awList = acquisitionMap.get(aw.getAcquisitionDisplayName());

				if (null == awList) {
					awList = new ArrayList<IAcquisitionWork>();
					acquisitionMap.put(aw.getAcquisitionDisplayName(), awList);
				}

				awList.add(aw);
			}
		}

		generateSummaryCounts(campaign);
	}

	public static void generateSummaryCounts(Campaign campaign) {
		partCountInfoMap = new HashMap<String, PartCountInfo>();

		Person admin = campaign.getLogisticsPerson();

		for (List<IAcquisitionWork> awList : acquisitionMap.values()) {
			IAcquisitionWork awFirst = awList.get(0);
			Part part = awFirst.getAcquisitionPart();
			TargetRoll target = campaign.getTargetForAcquisition(awFirst, admin);
			PartCountInfo pci = new PartCountInfo();

			int[] inventories = getInventories(part, campaign, pci);

			int inTransit = inventories[1];
			int onOrder = inventories[2];
			int omniPod = 0;

			if (!part.isOmniPodded()) {
				part.setOmniPodded(true);
				inventories = getInventories(part, campaign, pci);

				if (inventories[0] > 0) {
					omniPod = inventories[0];
				}

				part.setOmniPodded(false);
			}

			int missing = Math.max(0, awList.size() - inTransit - onOrder);

			pci.setKey(awFirst.getAcquisitionDisplayName());
			pci.setRequiredCount(awList.size());
			pci.setStickerPrice(part.getStickerPrice());
			pci.setMissingCount(missing);

			if (target.getValue() == TargetRoll.IMPOSSIBLE) {
				pci.setCanBeAcquired(false);
				pci.setFailedMessage(target.getPlainDesc());
			} else {
				pci.setInTransitCount(inTransit);
				pci.setOnOrderCount(onOrder);
				pci.setOmniPodCount(omniPod);
			}

			partCountInfoMap.put(awList.get(0).getAcquisitionDisplayName(), pci);
		}

		inTransitCount = 0;
		onOrderCount = 0;
		omniPodCount = 0;
		missingCount = 0;
		requiredCount = 0;
		unavailableCount = 0;
		missingTotalPrice = 0;

		//campaign.addReport("***START: generateSummaryCounts");
		
		for (PartCountInfo pci : partCountInfoMap.values()) {
			inTransitCount += pci.getInTransitCount();
			onOrderCount += pci.getOnOrderCount();
			missingCount += pci.getMissingCount();
			requiredCount += pci.getRequiredCount();
			omniPodCount += pci.getOmniPodCount();

			if (pci.getMissingCount() > 0) {
				if (!pci.isCanBeAcquired()) {
					unavailableCount += pci.getMissingCount();
				} else {
					missingTotalPrice += (pci.getMissingCount() * pci.getStickerPrice());
				}
			}

			//campaign.addReport(pci.toString());
		}
		
		//campaign.addReport("***END: generateSummaryCounts");
	}

	private static int[] getInventories(Part part, Campaign campaign, PartCountInfo pci) {
		String[] inventories = campaign.getPartInventory(part);
		int[] parsedInventories = new int[inventories.length];

		int idx = 0;

		for (String s : inventories) {
			if (s.indexOf(" ") > -1) {
				parsedInventories[idx] = Integer.parseInt(s.substring(0, s.indexOf(" ")));
				pci.setCountModifier(s.substring(s.indexOf(" ") + 1));
			} else {
				parsedInventories[idx] = Integer.parseInt(s);
			}

			idx++;
		}

		return parsedInventories;
	}

	public static class PartCountInfo {
		private String key;
		private int requiredCount;
		private int missingCount;
		private int inTransitCount;
		private int onOrderCount;
		private String countModifier = "";
		private int omniPodCount;
		private long stickerPrice;
		private String failedMessage;
		private boolean canBeAcquired = true;

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public int getRequiredCount() {
			return requiredCount;
		}

		public void setRequiredCount(int requiredCount) {
			this.requiredCount = requiredCount;
		}

		public int getMissingCount() {
			return missingCount;
		}

		public void setMissingCount(int missingCount) {
			this.missingCount = missingCount;
		}

		public int getInTransitCount() {
			return inTransitCount;
		}

		public void setInTransitCount(int inTransitCount) {
			this.inTransitCount = inTransitCount;
		}

		public int getOnOrderCount() {
			return onOrderCount;
		}

		public void setOnOrderCount(int onOrderCount) {
			this.onOrderCount = onOrderCount;
		}

		public int getOmniPodCount() {
			return omniPodCount;
		}

		public void setOmniPodCount(int omniPodCount) {
			this.omniPodCount = omniPodCount;
		}

		public String getCountModifier() {
			return countModifier;
		}

		public void setCountModifier(String countModifier) {
			this.countModifier = countModifier;
		}

		public long getStickerPrice() {
			return stickerPrice;
		}

		public void setStickerPrice(long stickerPrice) {
			this.stickerPrice = stickerPrice;
		}

		public String getFailedMessage() {
			return failedMessage;
		}

		public void setFailedMessage(String failedMessage) {
			this.failedMessage = failedMessage;
		}

		public boolean isCanBeAcquired() {
			return canBeAcquired;
		}

		public void setCanBeAcquired(boolean canBeAcquired) {
			this.canBeAcquired = canBeAcquired;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();

			sb.append(key);
			sb.append("{");
			sb.append("requiredCount=" + requiredCount);
			sb.append(",missingCount=" + missingCount);
			sb.append(",inTransitCount=" + inTransitCount);
			sb.append(",onOrderCount=" + onOrderCount);
			sb.append(",omniPodCount=" + omniPodCount);
			sb.append(",countModifier='" + countModifier + "'");
			sb.append(",stickerPrice=" + stickerPrice);
			sb.append(",failedMessage='" + failedMessage + "'");
			sb.append(",canBeAcquired=" + canBeAcquired);
			sb.append("}");
			
			return sb.toString();
		}
	}
}
