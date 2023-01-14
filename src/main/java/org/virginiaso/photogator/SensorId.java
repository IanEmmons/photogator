package org.virginiaso.photogator;

import java.util.Arrays;

public enum SensorId
{
	START(2, "Start "),	// Pin 2 on the Arduino
	FINISH(3, "Finish");	// Pin 3 on the Arduino

	private final long sensorIdNum;
	private final String sensorName;

	public static SensorId getById(long idNum)
	{
		return Arrays.stream(SensorId.values())
			.filter(id -> (id.sensorIdNum == idNum))
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException(
				"Unrecognized sensor ID number: \"%1$d\"".formatted(idNum)));
	}

	private SensorId(long idNum, String name)
	{
		sensorIdNum = idNum;
		sensorName = name;
	}

	public String getName()
	{
		return sensorName;
	}
}
