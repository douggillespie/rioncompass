package rioncompass;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortTimeoutException;

import PamguardMVC.debug.Debug;
import serialComms.jserialcomm.PJSerialComm;
import serialComms.jserialcomm.PJSerialException;
import serialComms.jserialcomm.PJSerialLineListener;

public class RIONTest {

	public static void main(String[] args) {
		new RIONTest().run();

	}

	private void run2() {
		// TODO Auto-generated method stub
		String cPort = PJSerialComm.getDefaultSerialPortName();
		SerialPort serialPort = SerialPort.getCommPort(cPort);
		serialPort.addDataListener(new SerialPortDataListener() {

			@Override
			public void serialEvent(SerialPortEvent arg0) {
				System.out.println("Serial event " + arg0);
			}

			@Override
			public int getListeningEvents() {
				// TODO Auto-generated method stub
				return 0;
			}
		});
		byte[] cmdB = {0x68, 0x04, 0x00, 0x04, 0x8}; 
		byte[] prepB = {0x68, 0x04, 0x00, 0x07, 0x0B};
		serialPort.setBaudRate(2400);
		serialPort.setNumDataBits(8);
		serialPort.setNumStopBits(0);
		serialPort.setParity(SerialPort.NO_PARITY);
		serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

		boolean isOpen = serialPort.openPort();
		if (isOpen == false) {
			System.out.println("Unable to open com port " + cPort);
		}
		OutputStream outputStream = serialPort.getOutputStream();
		InputStream inputStream = serialPort.getInputStream();
		try {
			outputStream.write(prepB);
			Thread.sleep(1000);
			byte[] data = inputStream.readAllBytes();
			if (data != null) {
				System.out.printf("Read %d bytes\n", data.length);
			}
			else {
				System.out.printf("Mo returned data");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		serialPort.closePort();
	}

	private PJSerialComm pjSerialComm;
	private volatile long sendT;

	private void run() {
		Debug.setPrintDebug(false); // make sure the class instantiates static members. 
		try {
			String cPort = PJSerialComm.getDefaultSerialPortName();
			boolean portExists = PJSerialComm.portExists(cPort);
			pjSerialComm = PJSerialComm.openSerialPort(cPort, 2400);
		} catch (PJSerialException e) {
			System.out.println(e.getMessage());
			return;
		}

		OutputStream outputStream = pjSerialComm.getOutputStream();

		/**
		 * Example that works in their software
		 * TX: 68 04 00 07 0B 
		 * RX: 68 06 00 87 00 00 8D 
		 * TX: 68 04 00 04 08 
		 * RX: 68 0D 00 84 10 02 87 10 00 85 01 59 22 3B 
		 * TX: 68 04 00 04 08 
		 * RX: 68 0D 00 84 10 02 87 10 00 85 01 59 13 2C 
		 * TX: 68 04 00 04 08 
		 * RX: 68 0D 00 84 10 02 87 10 00 86 01 59 09 23 
		 * TX: 68 04 00 04 08 
		 * RX: 68 0D 00 84 10 02 87 10 00 86 01 59 09 23 
		 * TX: 68 04 00 04 08 
		 * RX: 68 0D 00 84 10 02 88 10 
		 */

//		byte[] cmdB = {0x68, 0x04, 0x00, 0x04, 0x8}; 
//		byte[] prepB = {0x68, 0x04, 0x00, 0x07, 0x0B};
//		byte[] prepC = {0x68, 0x04, 0x00, 0x07, 0x0B};

		RIONCommands commands = new RIONCommands();

		Thread t = new Thread(new ReadThread());
		t.start();
		
		SerialPort basePort = pjSerialComm.getSerialPort(); 
		try {
			//			dos.writeChars(prep);
//			System.out.println("Send command " + makeByteString(prepB));
			outputStream.write(commands.autoMode);
			Thread.sleep(1000);
			for (int i = 0; i < 5; i++) {
//				System.out.println("Send command " + i + " : " + makeByteString(cmdB));
//				outputStream.write(cmdB);
				sendT = System.currentTimeMillis();
				Thread.sleep(1000);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		pjSerialComm.closePort();

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
			SerialPort basePort = pjSerialComm.getSerialPort();
			boolean wereLost = true;
			while (basePort.isOpen()) {
				int avail = basePort.bytesAvailable();
				if (avail == 0) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					continue;
				}
//				System.out.printf(makeByteString(data));
				for (int i = 0; i < avail; i++) {
					basePort.readBytes(oneByte, 1);
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
						byte chkSum = getCheckSum(thisData, stringLen);
						if (chkSum != thisData[stringLen]) {
							wereLost = true;
							continue;
						}
						int commandWord = Byte.toUnsignedInt(thisData[3]);
						long rxt = System.currentTimeMillis()-sendT;
						if (commandWord == 0x84) {
						double[] angles = interpretAngles(thisData);
						System.out.printf("RX: \"%s\" checksum %02X P=%3.2f, R=%3.2f, H=%3.2f %dms\n" , makeByteString(thisData), Byte.toUnsignedInt(chkSum),
								angles[0], angles[1], angles[2], rxt);
						}
						else {
							System.out.printf("RX: \"%s\" checksum %02X\n" , makeByteString(thisData), Byte.toUnsignedInt(chkSum));
						}
						dataLen = 0;
					}
					
				}
			}
			System.out.println("Read thread exited");
		}

		private double[] interpretAngles(byte[] thisData) {
			double[] angles = new double[3];
			for (int i = 0; i < 3; i++) {
				angles[i] = interpretAngle(thisData, 4+i*3);
			}
			return angles;
		}
		
		private double interpretAngle(byte[] thisData, int offset) {
			if (offset+2 >= thisData.length) {
				return Double.NaN;
			}
			String str = String.format("%02X%02X.%02X", Byte.toUnsignedInt(thisData[offset]), Byte.toUnsignedInt(thisData[offset+1]), Byte.toUnsignedInt(thisData[offset+2]));
			try {
				double angle = Double.valueOf(str);
				if (angle >= 1000) {
					angle = -(angle-1000);
				}
				return angle;
			}
			catch (NumberFormatException e) {
				System.out.println("Invalid number format exception in compass data " + str);
				return Double.NaN;
			}
			
		}
		
	}
	
	private int readAndPrint(InputStream inputStream) throws IOException {
		int n = inputStream.available();
		byte[] data = null;
		int read = -1;
	
		try {
			data = inputStream.readNBytes(n);
		}
		catch (SerialPortTimeoutException e) {
			
		}
		if (data == null) {
			return -1;
		}
		System.out.println("RX:"+makeByteString(data));
		
		return data.length;
	}

	private String makeByteString(byte[] data) {
		try {
			String str = "";
			for (int i = 0; i < data.length; i++) {
				str += String.format("%02X ", data[i]);
			}
			return str;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private byte getCheckSum(byte[] data, int nB) {
		byte cs = 0;
		for (int i = 1; i < nB; i++) {
			cs += Byte.toUnsignedInt(data[i]); 
		}
		return cs;
//		return (byte) (cs & 0x000000FF);
	}
	private void writeByteArray(OutputStream os, byte[] data, boolean addCR) throws IOException {
		for (int i = 0; i < data.length; i++) {
			os.write(Byte.toUnsignedInt(data[i]));
		}
	}
}
