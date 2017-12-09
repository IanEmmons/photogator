package org.virginiaso.serialport;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeartBeatEvent extends ArduinoEvent {
	public static final Pattern MSG_PATTERN = Pattern.compile(
		"^HeartBeat$", Pattern.CASE_INSENSITIVE);

	public HeartBeatEvent(@SuppressWarnings("unused") Matcher msgMatcher) {
		// Nothing to do for this message type
	}
}
