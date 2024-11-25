package rioncompass;

import PamModel.PamDependency;
import PamModel.PamPluginInterface;

public class RIONPlugin implements PamPluginInterface {

	private String jarFile;

	@Override
	public String getDefaultName() {
		return "RION digital compass";
	}

	@Override
	public String getHelpSetName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setJarFile(String jarFile) {
		this.jarFile = jarFile;
	}

	@Override
	public String getJarFile() {
		return jarFile;
	}

	@Override
	public String getDeveloperName() {
		return "Douglas Gillespie";
	}

	@Override
	public String getContactEmail() {
		return "support@pamguard.org";
	}

	@Override
	public String getVersion() {
		return RIONControl.version;
	}

	@Override
	public String getPamVerDevelopedOn() {
		return "2.1.06";
	}

	@Override
	public String getPamVerTestedOn() {
		return "2.1.06";
	}

	@Override
	public String getAboutText() {
		return "Serial port readout of heading, pitch and roll from a RION HCM508 compass module";
	}

	@Override
	public String getClassName() {
		return RIONControl.class.getName();
	}

	@Override
	public String getDescription() {
		return "RION HCM508 serial compass";
	}

	@Override
	public String getMenuGroup() {
		return "Sensors";
	}

	@Override
	public String getToolTip() {
		return "Serial port readout of a RION HCM508 compass module";
	}

	@Override
	public PamDependency getDependency() {
		return null;
	}

	@Override
	public int getMinNumber() {
		return 0;
	}

	@Override
	public int getMaxNumber() {
		return 0;
	}

	@Override
	public int getNInstances() {
		return 0;
	}

	@Override
	public boolean isItHidden() {
		return false;
	}

	@Override
	public int allowedModes() {
		return PamPluginInterface.ALLMODES;
	}

}
