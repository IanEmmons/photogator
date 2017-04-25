package org.virginiaso.photogator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.virginiaso.serialport.ArduinoEvent;

public class BeamBrokenEvent extends ArduinoEvent {
	public static final Pattern MSG_PATTERN = Pattern.compile(
		"^BeamBroken:([0-9]+),([0-9]+),([0-9]+)$", Pattern.CASE_INSENSITIVE);
	private static final String MSG_FMT = "%1$3d. (%2$s) %3$s at %4$8d ms%n";
	private static final long START_SENSOR_ID = 2;
	private static final long FINISH_SENSOR_ID = 3;
	private static final String START_SENSOR_NAME = "Start ";
	private static final String FINISH_SENSOR_NAME = "Finish";
	private static final String UNKNOWN_SENSOR_NAME_FMT = "Sensor %1$d";
	private static final String DIFFERENCE_FMT = "%n   %1$7.3f seconds%n%n";

	private final long seqNum;
	private final long sensorId;
	private final long arduinoTime;

	public BeamBrokenEvent(Matcher msgMatcher) {
		seqNum = Long.parseLong(msgMatcher.group(1));
		sensorId = Long.parseLong(msgMatcher.group(2));
		arduinoTime = Long.parseLong(msgMatcher.group(3));
	}

	private String getSensorName() {
		String sensorName;
		if (sensorId == START_SENSOR_ID) {
			sensorName = START_SENSOR_NAME;
		} else if (sensorId == FINISH_SENSOR_ID) {
			sensorName = FINISH_SENSOR_NAME;
		} else {
			sensorName = String.format(UNKNOWN_SENSOR_NAME_FMT, sensorId);
		}
		return sensorName;
	}

	public String format() {
		return String.format(MSG_FMT, seqNum, formatWallClockTime(), getSensorName(), arduinoTime);
	}

	public boolean follows(BeamBrokenEvent lastEvent) {
		return (lastEvent != null)
			&& (lastEvent.sensorId == START_SENSOR_ID)
			&& (sensorId == FINISH_SENSOR_ID);
	}

	public String difference(BeamBrokenEvent lastEvent) {
		double elapsedSeconds = (arduinoTime - lastEvent.arduinoTime) / 1000.0;
		return String.format(DIFFERENCE_FMT, elapsedSeconds);
	}
}
