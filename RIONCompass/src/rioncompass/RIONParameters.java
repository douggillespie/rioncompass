package rioncompass;

import java.io.Serializable;

import serialComms.jserialcomm.PJSerialComm;

public class RIONParameters implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;
		
	public String serialPort = PJSerialComm.getDefaultSerialPortName();

	public int baud = 2400;
	
	public int pollInterval = 1000;
	
	public RIONMode runMode = RIONMode.ANSREPLY;
	
	public int streamerMap = 0;

	@Override
	public RIONParameters clone() {
		try {
			return (RIONParameters) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
		
}
