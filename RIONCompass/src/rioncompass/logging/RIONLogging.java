package rioncompass.logging;

import java.sql.Types;

import javax.sql.rowset.RowSetMetaDataImpl;

import Array.sensors.ArraySensorFieldType;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;
import generalDatabase.SQLTypes;
import rioncompass.RIONControl;
import rioncompass.RIONDataUnit;

public class RIONLogging extends SQLLogging {

	private RIONControl rionControl;
	
	private PamTableItem magHeading, trueHeading, pitch, roll;

	public RIONLogging(RIONControl rionControl, PamDataBlock pamDataBlock) {
		super(pamDataBlock);
		this.rionControl = rionControl;
		PamTableDefinition tableDef = new PamTableDefinition(rionControl.getUnitName());
		tableDef.addTableItem(magHeading = new PamTableItem("Mag Heading", Types.REAL));
		tableDef.addTableItem(trueHeading = new PamTableItem("True Heading", Types.REAL));
		tableDef.addTableItem(pitch = new PamTableItem("Pitch", Types.REAL));
		tableDef.addTableItem(roll = new PamTableItem("Roll", Types.REAL));
		setTableDefinition(tableDef);
	}

	@Override
	public void setTableData(SQLTypes sqlTypes, PamDataUnit pamDataUnit) {
		RIONDataUnit rionDataUnit = (RIONDataUnit) pamDataUnit;
		// units always have all three fields, so no need for nulls just make sure to cast to float 
		setDoubleField(magHeading, rionDataUnit.getRIONField(RIONDataUnit.RION_HEADING));
		setDoubleField(trueHeading, rionDataUnit.getField(0, ArraySensorFieldType.HEADING));
		setDoubleField(pitch, rionDataUnit.getRIONField(RIONDataUnit.RION_PITCH));
		setDoubleField(roll, rionDataUnit.getRIONField(RIONDataUnit.RION_ROLL));
	}
	
	private void setDoubleField(PamTableItem tableItem, Double value) {
		if (value == null) {
			tableItem.setValue(null);
		}
		else {
			tableItem.setValue(value.floatValue());
		}
	}

	@Override
	protected PamDataUnit createDataUnit(SQLTypes sqlTypes, long timeMilliseconds, int databaseIndex) {
		double[] angleData = new double[3];
		angleData[RIONDataUnit.RION_HEADING] = magHeading.getDoubleValue();
		angleData[RIONDataUnit.RION_PITCH] = pitch.getDoubleValue();
		angleData[RIONDataUnit.RION_ROLL] = roll.getDoubleValue();
		RIONDataUnit rionDataUnit = new RIONDataUnit(timeMilliseconds, rionControl.getRionParameters().streamerMap, angleData);
		rionDataUnit.setDatabaseIndex(databaseIndex);
		return rionDataUnit;
	}

}
