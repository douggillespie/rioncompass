package rioncompass;

import Array.sensors.ArraySensorDataBlock;
import Array.sensors.ArraySensorFieldType;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class RIONDataBlock extends PamDataBlock<RIONDataUnit> implements ArraySensorDataBlock {

	public RIONDataBlock(String dataName, PamProcess parentProcess, int channelMap) {
		super(RIONDataUnit.class, dataName, parentProcess, channelMap);
		
		setNaturalLifetime(600);
		setMixedDirection(PamDataBlock.MIX_OUTOFDATABASE);
	}

	@Override
	public boolean hasSensorField(ArraySensorFieldType fieldType) {
		switch (fieldType) {
		case HEIGHT:
			return false;
		case HEADING:
			return true;
		case PITCH:
			return true;
		case ROLL:
			return true;
		default:
			break;
		}
		return false;
	}

	@Override
	public int getStreamerBitmap() {
		return getChannelMap();
	}

	@Override
	public int getNumSensorGroups() {
		return 1;
	}

}
