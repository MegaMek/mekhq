package mekhq.gui;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class CampaignFileFilter extends FileFilter {
		
	public CampaignFileFilter() {

	}

	@Override
	public boolean accept(File dir) {
		if (dir.isDirectory()) {
			return true;
		}
		return dir.getName().endsWith(".xml") || dir.getName().endsWith(".cpnx");
	}

	@Override
	public String getDescription() {
		return "campaign file (.cpnx, .xml)";
	}
}