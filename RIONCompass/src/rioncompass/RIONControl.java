package rioncompass;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenuItem;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamView.dialog.GenericSwingDialog;
import rioncompass.swing.RIONDialogPanel;

public class RIONControl extends PamControlledUnit implements PamSettings {

	public static String unitType = "RION Compass";

	public static String version = "1.01";
	
	private RIONProcess rionProcess;
	
	private RIONParameters rionParameters = new RIONParameters();
	
	public RIONControl(String unitName) {
		super(unitType, unitName);
		addPamProcess(rionProcess = new RIONProcess(this));
		
		PamSettingManager.getInstance().registerSettings(this);
	}

	/**
	 * @return the rionParameters
	 */
	public RIONParameters getRionParameters() {
		return rionParameters;
	}

	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName() + " options ...");
		menuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				showSettings(parentFrame);
			}
		});
		return menuItem;
		
	}

	protected void showSettings(Frame parentFrame) {
		boolean ans = GenericSwingDialog.showDialog(parentFrame, getUnitName(), new RIONDialogPanel(this));
		if (ans) {
			rionProcess.killAndLaunch();
		}		
	}

	@Override
	public Serializable getSettingsReference() {
		return rionParameters;
	}

	@Override
	public long getSettingsVersion() {
		return RIONParameters.serialVersionUID;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		rionParameters = ((RIONParameters) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}

	public boolean setBaud(String port, int newBaud) {
		return rionProcess.setBaud(port, rionParameters.baud, newBaud);
	}

}
