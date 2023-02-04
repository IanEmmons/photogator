package org.virginiaso.photogator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.virginiaso.serialport.ArduinoEvent;

public class BeamBrokenEvent extends ArduinoEvent
{
	public static final Pattern MSG_PATTERN = Pattern.compile(
		"^BeamBroken:([0-9]+),([0-9]+),([0-9]+)$", Pattern.CASE_INSENSITIVE);
	private static final String MSG_FMT = "%1$3d. (%2$s) %3$s at %4$8d ms%n";
	private static final String DIFFERENCE_FMT = "%n   %1$7.2f seconds (#%2$d to #%3$d)%n%n";

	private final long seqNum;
	private final SensorId sensorId;
	private final long arduinoTime;

	public BeamBrokenEvent(Matcher msgMatcher)
	{
		seqNum = Long.parseLong(msgMatcher.group(1));
		sensorId = SensorId.getById(Long.parseLong(msgMatcher.group(2)));
		arduinoTime = Long.parseLong(msgMatcher.group(3));
	}

	@Override
	public String format()
	{
		return MSG_FMT.formatted(seqNum, formatWallClockTime(), sensorId.getName(), arduinoTime);
	}

	public String formatDifference(BeamBrokenEvent lastEvent)
	{
		double elapsedSeconds = (arduinoTime - lastEvent.arduinoTime) / 1000.0;
		return DIFFERENCE_FMT.formatted(elapsedSeconds, lastEvent.seqNum, seqNum);
	}

	public SensorId getSensorId()
	{
		return sensorId;
	}
}
