package rioncompass.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import Array.ArrayManager;
import Array.PamArray;
import PamView.dialog.PamDialog;
import PamView.dialog.PamDialogPanel;
import PamView.dialog.PamGridBagContraints;
import PamView.dialog.warn.WarnOnce;
import rioncompass.RIONCommands;
import rioncompass.RIONControl;
import rioncompass.RIONMode;
import rioncompass.RIONParameters;
import serialComms.SerialPortPanel;

public class RIONDialogPanel implements PamDialogPanel {
	
	private RIONControl rionControl;
	
	private JPanel mainPanel;
	
	private SerialPortPanel serialPortPanel;
	
//	private JComboBox<String> streamerList;
	
	private JComboBox<RIONMode> readMode;
	
	private JTextField readInterval;
	
	private JButton setBaudButton;
	
	private JCheckBox[] streamers;

	public RIONDialogPanel(RIONControl rionControl) {
		super();
		this.rionControl = rionControl;
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		serialPortPanel = new SerialPortPanel("Serial Port", true, false, false, false, false);
		serialPortPanel.setAvailableBauds(RIONCommands.availableBauds);
		mainPanel.add(serialPortPanel.getPanel());
		setBaudButton = new JButton("Set now");
		GridBagConstraints cb = new PamGridBagContraints();
		cb.gridy = 1;
		cb.gridx = 2;
		/**
		 * Setting baud rate is disabled, since on advice from RM, on a long cable, if you set it to 
		 * a high baud, it becomes impossible to send a command to set it back to a low baud. This is 
		 * problematic if the sensor is potted into an array!
		 */
//		serialPortPanel.getPanel().add(setBaudButton, cb);
//		setBaudButton.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				setBaud();
//			}
//		});
		
		JPanel optsPanel = new JPanel(new GridBagLayout());
		optsPanel.setBorder(new TitledBorder("Options"));
		GridBagConstraints c = new PamGridBagContraints();
		optsPanel.add(new JLabel(" Read mode ", JLabel.RIGHT), c);
		c.gridx++;
		c.gridwidth = 2;
		optsPanel.add(readMode = new JComboBox<RIONMode>(), c);
		c.gridx = 0;
		c.gridwidth = 1;
		c.gridy++;
		optsPanel.add(new JLabel(" Read interval ", JLabel.RIGHT), c);
		c.gridx++;
		optsPanel.add(readInterval = new JTextField(5), c);
		c.gridx++;
		optsPanel.add(new JLabel(" s ", JLabel.LEFT), c);
		c.gridx = 0;
		c.gridy++;
//		optsPanel.add(new JLabel(" Read mode ", JLabel.RIGHT), c);
//		c.gridx++;
		c.gridwidth = 3;
		c.gridx = 0;
//		c.gridy++;
		PamArray currentArray = ArrayManager.getArrayManager().getCurrentArray();
		int nStreamers = currentArray.getNumStreamers();
		streamers = new JCheckBox[nStreamers];
		JPanel streamerPanel = new JPanel(new GridBagLayout());
		optsPanel.add(streamerPanel, c);
		GridBagConstraints sc = new PamGridBagContraints();
		sc.gridy = 1;
		sc.ipady = 0;
		sc.insets = new Insets(0, 2, 0, 2);
		streamerPanel.add(new JLabel("Streamers "), sc);
//		sc.anchor = GridBagConstraints.BASELINE_TRAILING;
		sc.fill = GridBagConstraints.HORIZONTAL;
		for (int i = 0; i < nStreamers; i++) {
			sc.gridx++;
//			sc.gridy = 0;
//			streamerPanel.add(new JLabel(" "+i), sc);
			sc.gridy = 1;
			streamerPanel.add(streamers[i] = new JCheckBox(""+i), sc);
			streamers[i].setToolTipText(currentArray.getStreamer(i).getStreamerName());
		}
		
//		optsPanel.add(streamerList = new JComboBox<String>(), c);
		mainPanel.add(optsPanel);
		
		fillLists();
		
		readMode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				enableControls();
			}
		});
	}

	/**
	 * Set baud rate on remote compass. 
	 */
	protected void setBaud() {
		int newBaud = serialPortPanel.getBaudRate();
		String newPort = serialPortPanel.getPort();
		rionControl.setBaud(newPort, newBaud);
	}

	protected void enableControls() {
		Object rm = readMode.getSelectedItem();
		readInterval.setEnabled(rm == RIONMode.ANSREPLY);		
	}

	private void fillLists() {
		RIONMode[] modes = RIONMode.values();
		readMode.removeAllItems();
		for (int i = 0; i < modes.length; i++) {
			readMode.addItem(modes[i]);
		}
//		streamerList.removeAllItems();
//		PamArray currentArray = ArrayManager.getArrayManager().getCurrentArray();
//		if (currentArray != null) {
//			int nS = currentArray.getNumStreamers();
//			for (int i = 0; i < nS; i++) {
//				String str = String.format("%d - %s", i, currentArray.getStreamer(i).getStreamerName());
//				streamerList.addItem(str);
//			}
//		}
	}

	@Override
	public JComponent getDialogComponent() {
		return mainPanel;
	}

	@Override
	public void setParams() {
		fillLists();
		RIONParameters params = rionControl.getRionParameters();
		serialPortPanel.setPort(params.serialPort);
		serialPortPanel.setBaudRate(params.baud);
		readMode.setSelectedItem(params.runMode);
		readInterval.setText(String.format("%3.1f", params.pollInterval / 1000.));
//		if (streamerList.getItemCount() > params.streamerNumber) {
//			streamerList.setSelectedIndex(params.streamerNumber);
//		}
		for (int i = 0; i < streamers.length; i++) {
			streamers[i].setSelected((1<<i & params.streamerMap) != 0);
		}
		enableControls();
	}

	@Override
	public boolean getParams() {
		RIONParameters params = rionControl.getRionParameters();
		params.serialPort = serialPortPanel.getPort();
		if (params.serialPort == null) {
			return PamDialog.showWarning(null, rionControl.getUnitName(), "No serial port selected");
		}
		params.baud = serialPortPanel.getBaudRate();
		params.runMode = (RIONMode) readMode.getSelectedItem();
		if (params.runMode == RIONMode.ANSREPLY) {
			try {
				params.pollInterval = (int) (Double.valueOf(readInterval.getText()) * 1000.);
			}
			catch (NumberFormatException e) {
				return PamDialog.showWarning(null, rionControl.getUnitName(), "Invalid readout interval");
			}
		}
		params.streamerMap = 0;
		for (int i = 0; i < streamers.length; i++) {
			if (streamers[i].isSelected());
			params.streamerMap |= (1<<i);
		}
		if (params.streamerMap == 0) {
			int ans = WarnOnce.showWarning(rionControl.getUnitName(),
					"you should probably associate the compass data with at least one hydrophone streamer", WarnOnce.OK_CANCEL_OPTION);
			if (ans == WarnOnce.CANCEL_OPTION) {
				return false;
			}
		}
//		params.streamerNumber = streamerList.getSelectedIndex();
		return true;
	}

}
