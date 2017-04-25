package org.virginiaso.windgauge;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.virginiaso.serialport.ArduinoEvent;

public class VoltageReadingEvent extends ArduinoEvent {
	public static final Pattern MSG_PATTERN = Pattern.compile(
		"^VoltageReading:([0-9]+),([0-9]+),(-?[0-9]*\\.[0-9]+)$", Pattern.CASE_INSENSITIVE);
	private static final String MSG_FMT = "%1$3d. (%2$s) %3$g mV at %4$8d ms%n";

	private final long seqNum;
	private final long arduinoTime;
	private final double voltage;

	public VoltageReadingEvent(Matcher msgMatcher) {
		seqNum = Long.parseLong(msgMatcher.group(1));
		arduinoTime = Long.parseLong(msgMatcher.group(2));
		voltage = Double.parseDouble(msgMatcher.group(3));
	}

	public String format() {
		return String.format(MSG_FMT, seqNum, formatWallClockTime(), voltage, arduinoTime);
	}
}
