package rioncompass;

/**
 * A few static command strings used for the RION Compass and
 * checksum functions. 
 * See HCM508Bhighaccuracytiltcompensated3Ddigitalcompass.1.pdf
 * @author dg50
 */
public class RIONCommands {

	public final byte[] prepPolling = {0x68, 0x04, 0x00, 0x07, 0x0B}; 
	public final byte[] autoMode = {0x68, 0x05, 0x00, 0x0C, 0x01, 0x00}; 
	public final byte[] ansReplyMode = {0x68, 0x05, 0x00, 0x0C, 0x00, 0x00}; 
	public final byte[] requestData = {0x68, 0x04, 0x00, 0x04, 0x8}; 
	public final static int[] availableBauds = {2400, 4800, 9600, 19200, 38400, 115200};
	public final static byte[] baudOK = {0x68, 0x05, 0, (byte)0x8B, 0, (byte)0x90}; 
	
	// bauds have codes 0 to 5 associated with them. 
	
	public RIONCommands() {
		super();
		autoMode[5] = getCheckSum(autoMode);
		ansReplyMode[5] = getCheckSum(ansReplyMode);
	}

	/**
	 * Get a data checksum. In all data, the checksum is the XOR of 
	 * all but the first and generally the last items. Second item is 
	 * always the data length, so will use that to say how much data to include. 
	 * @param data
	 * @return
	 */
	public byte getCheckSum(byte[] data) {
		if (data == null || data.length < 2) {
			return 0;
		}
		int len = Byte.toUnsignedInt(data[1]);
		if (len > data.length-1) {
			// this is going to crash!
			len = data.length-1;
		}
		byte cs = data[1];
		for (int i = 2; i <= len-1; i++) {
			cs += Byte.toUnsignedInt(data[i]); 
		}
		return cs;
	}

	/**
	 * Write a RION byte array as a hex string. 
	 * @param data
	 * @return
	 */
	public String makeByteString(byte[] data) {
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
	
	public int codeForBaud(int baud) {
		for (int i = 0; i < availableBauds.length; i++) {
			if (baud == availableBauds[i]) {
				return i;
			}
		}
		return -1;
	}
	
	public byte[] makeBaudCommand(int baudCode) {
		byte[] bc = {0x68, 0x05, 00, 0xB, (byte) baudCode, 0};
		bc[5] = getCheckSum(bc);
		return bc;
	}
	
	/**
	 * Is the reply from the set baud command OK ? 
	 * @param rxBytes
	 * @return
	 */
	public boolean baudOK(byte[] rxBytes) {
		if (rxBytes.length != baudOK.length) {
			return false;
		}
		for (int i = 0; i < rxBytes.length; i++) {
			if (rxBytes[i] != baudOK[i]) {
				return false;
			}
		}
		return true;
	}
}
