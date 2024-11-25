package rioncompass;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.Timer;

import com.fazecast.jSerialComm.SerialPort;

import PamController.PamController;
import PamUtils.PamCalendar;
import PamguardMVC.PamProcess;
import rioncompass.logging.RIONLogging;
import warnings.QuickWarning;

/**
 * Main process for acquiring data from sensors.  
 * @author dg50
 *
 */
public class RIONProcess extends PamProcess {

	private RIONControl rionControl;
	private Timer timer;

	private SerialPort serialPort;
	
	private RIONCommands rionCommands;
	
	private Thread acquireThread;
	
	private RIONDataBlock rionDataBlock;

	private QuickWarning serialWarning;
	private long sendT;

	public RIONProcess(RIONControl rionControl) {
		super(rionControl, null);
		this.rionControl = rionControl;
		serialWarning = new QuickWarning(rionControl.getUnitName());
		
		rionCommands = new RIONCommands();
		
		rionDataBlock = new RIONDataBlock(rionControl.getUnitName(), this, 1);
		rionDataBlock.SetLogging(new RIONLogging(rionControl, rionDataBlock));
		addOutputDataBlock(rionDataBlock);
		
		timer = new Timer(3000, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				pollData();
			}

		});
	}

	@Override
	public void pamStart() {
	}

	@Override
	public void pamStop() {
	}

	/**
	 * Stop any running process and start another. 
	 */
	public void killAndLaunch() {
		killOld();
		launchNew();
	}

	private void pollData() {
		writeCommand(rionCommands.requestData);
	}
	
	private synchronized void killOld() {
		timer.stop();
		if (serialPort != null) {
			serialPort.closePort(); // this will cause the reading thread to close too. 
		}
		if (acquireThread != null) {
			try {
				acquireThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private synchronized void launchNew() {
		
		rionDataBlock.setChannelMap(rionControl.getRionParameters().streamerMap);
		// don' tlaunch in viewe rmode!
		if (PamController.getInstance().getRunMode() != PamController.RUN_NORMAL) {
			return;
		}
		
		RIONParameters params = rionControl.getRionParameters();
		if (params.runMode == RIONMode.IDLE) {
			return;
		}
		
		serialPort = SerialPort.getCommPort(params.serialPort);
		if (serialPort == null) {
			serialWarning.setWarning("Serial port " + params.serialPort + " is not available", 2);
			return;
		}
		serialPort.setBaudRate(params.baud);
		serialPort.setNumDataBits(8);
		serialPort.setNumStopBits(0);
		serialPort.setParity(SerialPort.NO_PARITY);
		serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

		boolean isOpen = serialPort.openPort();
		if (isOpen == false) {
			serialWarning.setWarning("Unable to open serial port " + params.serialPort, 2);
			serialPort = null;
			return;
		}

		acquireThread = new Thread(new ReadThread(), rionControl.getUnitName());
		acquireThread.start();

		if (params.runMode == RIONMode.AUTO) {
			writeCommand(rionCommands.autoMode);
//			writeCommand(rionCommands.prepPolling);
		}
		else if (params.runMode == RIONMode.ANSREPLY) {
			writeCommand(rionCommands.ansReplyMode);
//			writeCommand(rionCommands.prepPolling);
			timer.setDelay(params.pollInterval);
			timer.start();
		}

		

		serialWarning.clearWarning();
	}

	private synchronized int writeCommand(byte[] command) {
		if (serialPort == null) {
			return -1;
		}
		int wrote = serialPort.writeBytes(command, command.length);
		sendT = System.currentTimeMillis();
		return wrote;
	}

	private class ReadThread implements Runnable {

		@Override
		public void run() {
			int maxLen = 64;
			byte[] data = new byte[64];
			byte[] oneByte = new byte[1];
			byte startChar = 0x68;
			int dataLen = 0;
			int stringLen = 0;
			boolean wereLost = true;
			while (serialPort.isOpen()) {
				int avail = serialPort.bytesAvailable();
				if (avail == 0) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						//						e.printStackTrace();
					}
					continue;
				}
				//				System.out.printf(makeByteString(data));
				for (int i = 0; i < avail; i++) {
					serialPort.readBytes(oneByte, 1);
					if (wereLost) {
						if (oneByte[0] != startChar) {
							continue;
						}
						else {
							wereLost = false;
							dataLen = 0;
						}
					}
					if (dataLen == maxLen) {
						dataLen = 0;
						continue;
					}
					//					if (oneByte[0] == startChar) {
					//						dataLen = 0;
					//					}
					data[dataLen++] = oneByte[0];
					if (dataLen == 2) {
						stringLen = data[1];
					}
					if (dataLen == stringLen+1 && stringLen > 2) {
						byte[] thisData = Arrays.copyOf(data, stringLen+1);
						byte chkSum = rionCommands.getCheckSum(thisData);
						if (chkSum != thisData[stringLen]) {
							wereLost = true;
						}
						else {
							interpretRIONData(thisData);
						}
						dataLen = 0;
					}

				}
			}
			System.out.println("RION Compass read thread exited");
		}

	}

	public void interpretRIONData(byte[] thisData) {
		int commandWord = Byte.toUnsignedInt(thisData[3]);
		long rxt = System.currentTimeMillis()-sendT;
		if (commandWord == 0x84) {
			double[] angles = interpretAngles(thisData);
//			System.out.printf("RX: \"%s\" P=%3.2f, R=%3.2f, H=%3.2f %dms\n" , rionCommands.makeByteString(thisData), 
//					angles[0], angles[1], angles[2], rxt);
			RIONDataUnit rionDataUnit = new RIONDataUnit(PamCalendar.getTimeInMillis(), rionControl.getRionParameters().streamerMap, angles);
			rionDataBlock.addPamData(rionDataUnit);
			//		}
			//		else {
			//			System.out.printf("RX: \"%s\" checksum %02X\n" , makeByteString(thisData), Byte.toUnsignedInt(chkSum));
			//		}

		}
		else {
			System.out.printf("RX: \"%s\"  %dms\n" , rionCommands.makeByteString(thisData), rxt); 
		}
	}

	/**
	 * Interpret three angles from as standard data string
	 * @param thisData
	 * @return
	 */
	private double[] interpretAngles(byte[] thisData) {
		double[] angles = new double[3];
		for (int i = 0; i < 3; i++) {
			angles[i] = interpretAngle(thisData, 4+i*3);
		}
		return angles;
	}

	/**
	 * Pull an angle out of three bytes of data. format is to write the first two bytes as a hex number then
	 * a decimal point and the third number. for the heading (0-360) the first part of the first byte is 
	 * always going to be zero. For pitch and roll, which go from -180 to 180 the first digit is 1 for negative
	 * values. 
	 * @param thisData byte array from sensor
	 * @param offset offset into byte array
	 * @return angle in decimal degrees
	 */
	private double interpretAngle(byte[] thisData, int offset) {
		if (offset+2 >= thisData.length) {
			return Double.NaN;
		}
		// write three bytes as a string with a decimal point after byte 2.
		String str = String.format("%02X%02X.%02X", Byte.toUnsignedInt(thisData[offset]), Byte.toUnsignedInt(thisData[offset+1]), Byte.toUnsignedInt(thisData[offset+2]));
		try {
			// read it as a double
			double angle = Double.valueOf(str);
			if (angle >= 1000) {
				// for pitch and roll, the first text digit will be 1 if it's negative, in which case angle>0
				// to take off that first digit (=1000) and reverse sign. 
				angle = -(angle-1000);
			}
			return angle;
		}
		catch (NumberFormatException e) {
			System.out.println("Invalid number format exception in compass data " + str);
			return Double.NaN;
		}

	}

	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		if (changeType == PamController.INITIALIZATION_COMPLETE) {
			killAndLaunch();
		}
		if (changeType == PamController.DESTROY_EVERYTHING) {
			killOld();
		}
	}

	/**
	 * Set a new baud rate on the current comm port. 
	 * @param port 
	 * @param oldBaud
	 * @param newBaud
	 * @return
	 */
	public boolean setBaud(String port, int oldBaud, int newBaud) {
		/**
		 * Setting baud rate is disabled, since on advice from RM, on a long cable, if you set it to 
		 * a high baud, it becomes impossible to send a command to set it back to a low baud. This is 
		 * problematic if the sensor is potted into an array!
		 */
//		killOld();
//		if (tryBaud(port, oldBaud, newBaud)) {
//			return true;
//		}
//		// then try a current baud of all other bauds and see if any will get
//		// a sensible response...
//		for (int i = 0; i < RIONCommands.availableBauds.length; i++) {
//			if (tryBaud(port, RIONCommands.availableBauds[i], newBaud)) {
//				System.out.printf("Success setting RION Baud on port %s from %d to %d\n", port, RIONCommands.availableBauds[i], newBaud);
//				return true;
//			}
//		}
		return false;
	}

	/**
	 * Try setting a baud rate and see if we get a reply. 
	 * @param port
	 * @param oldBaud
	 * @param newBaud
	 * @return
	 */
	private boolean tryBaud(String port, int oldBaud, int newBaud) {
		serialPort = SerialPort.getCommPort(port);
		if (serialPort == null) {
			return false;
		}
		serialPort.setBaudRate(oldBaud);
		serialPort.setNumDataBits(8);
		serialPort.setNumStopBits(0);
		serialPort.setParity(SerialPort.NO_PARITY);
		serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

		boolean isOpen = serialPort.openPort();
		if (isOpen == false) {
			return false;
		}
		int baudCode = rionCommands.codeForBaud(newBaud);
		if (baudCode < 0) {
			return false;
		}
		byte[] setBaud = rionCommands.makeBaudCommand(baudCode);
		System.out.printf("Changing baud from %d to %d: Command %s\n", oldBaud, newBaud, rionCommands.makeByteString(setBaud));
		int wrote =	writeCommand(setBaud);
		serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 1000);
		byte[] inDat = new byte[6];
		int bytesRead = -1;
		try {
			bytesRead = serialPort.readBytes(inDat, inDat.length);
		}
		catch (Exception e) {
			System.out.printf("Error chaning baud rate from %d to %d: %s\n", oldBaud, newBaud, e.getMessage());
		}
		serialPort.closePort();
		System.out.println(rionCommands.makeByteString(inDat));
		return rionCommands.baudOK(inDat);
		
	}
}
