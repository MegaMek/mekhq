package mekhq.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;

public class PartsAcquisitionService {
	private static Map<String, List<IAcquisitionWork>> acquisitionMap = null;
	private static Map<String, PartCountInfo> partCountInfoMap = new HashMap<String, PartCountInfo>();
	
	private static int inTransitCount = 0;
	private static int onOrderCount = 0;
	private static int totalMissingCount = 0;
	private static int requiredCount = 0;
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

	public static int getTotalMissingCount() {
		return totalMissingCount;
	}

	public static void setTotalMissingCount(int totalMissingCount) {
		PartsAcquisitionService.totalMissingCount = totalMissingCount;
	}

	public static int getRequiredCount() {
		return requiredCount;
	}

	public static void setRequiredCount(int requiredCount) {
		PartsAcquisitionService.requiredCount = requiredCount;
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
		
		for (List<IAcquisitionWork> awList : acquisitionMap.values()) {
			Part part = awList.get(0).getAcquisitionPart();
			
			String[] inventories = campaign.getPartInventory(part);

			int inTransit = Integer.parseInt(inventories[1]);
			int onOrder = Integer.parseInt(inventories[2]);
			int omniPod = 0;
			
			int missing = Math.max(0, awList.size() - inTransit - onOrder);
			
			if (!part.isOmniPodded()) {
				part.setOmniPodded(true);
				inventories = campaign.getPartInventory(part);

				if (Integer.parseInt(inventories[0]) > 0) {
					omniPod = Integer.parseInt(inventories[0]);
				}

				part.setOmniPodded(false);
			}
			
			PartCountInfo pci = new PartCountInfo();
			pci.setRequiredCount(awList.size());
			pci.setMissingCount(missing);
			pci.setInTransitCount(inTransit);
			pci.setOnOrderCount(onOrder);
			pci.setOmniPodCount(omniPod);
			pci.setStickerPrice(part.getStickerPrice());

			partCountInfoMap.put(awList.get(0).getAcquisitionDisplayName(), pci);
		}
		
		inTransitCount = 0;
		onOrderCount = 0;
		totalMissingCount = 0;
		requiredCount = 0;
		missingTotalPrice = 0;
		
		for (PartCountInfo pci : partCountInfoMap.values()) {
			inTransitCount += pci.getInTransitCount();
			onOrderCount += pci.getOnOrderCount();
			totalMissingCount += pci.getMissingCount();
			requiredCount += pci.getRequiredCount();

			if (pci.getMissingCount() > 0) {
				missingTotalPrice += (pci.getMissingCount() * pci.getStickerPrice());
			}
		}		
	}
	
	public static class PartCountInfo {
		private int requiredCount;
		private int missingCount;
		private int inTransitCount;
		private int onOrderCount;
		private int omniPodCount;
		private long stickerPrice;

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

		public long getStickerPrice() {
			return stickerPrice;
		}

		public void setStickerPrice(long stickerPrice) {
			this.stickerPrice = stickerPrice;
		}
	}
}
