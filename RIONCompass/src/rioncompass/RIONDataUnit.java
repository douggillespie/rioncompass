package rioncompass;

import Array.sensors.ArraySensorDataUnit;
import Array.sensors.ArraySensorFieldType;
import PamController.masterReference.MasterReferencePoint;
import PamUtils.LatLong;
import PamguardMVC.PamDataUnit;
import geoMag.MagneticVariation;

public class RIONDataUnit extends PamDataUnit implements ArraySensorDataUnit {

	// angle data are in the order Pitch, Roll, Heading in degrees
	public static final int RION_HEADING = 2;
	public static final int RION_ROLL = 1;
	public static final int RION_PITCH = 0;
	
	private double[] angleData;

	public RIONDataUnit(long timeMilliseconds, int streamerMap, double[] angleData) {
		super(timeMilliseconds);
		setChannelBitmap(streamerMap);
		this.angleData = angleData;
	}

	@Override
	public Double getField(int streamer, ArraySensorFieldType fieldtype) {
		switch (fieldtype) {
		case HEADING:
			Double head = getRIONField(2);
			if (head != null) {
				head += getMagDev();
			}
			return head;
		case HEIGHT:
			return null;
		case PITCH:
			return getRIONField(0);
		case ROLL:
			return getRIONField(1);
		default:
			break;
		
		}
		return null;
	}

	public Double getRIONField(int i) {
		if (angleData == null || angleData.length <= i) {
			return null;
		}
		else {
			return angleData[i];
		}
	}

	public double getMagDev() {
		LatLong latLong = MasterReferencePoint.getLatLong();
		if (latLong == null) {
			return 0;
		}
		return MagneticVariation.getInstance().getVariation(getTimeMilliseconds(), latLong.getLatitude(), latLong.getLongitude());
	}

}
