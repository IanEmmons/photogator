package org.virginiaso.serialport;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ArduinoEvent
{
	private static class RegisteredMsgType
	{
		private static final String REGEX_FIELD_NAME = "MSG_PATTERN";
		private final Class<? extends ArduinoEvent> msgType;
		private final Pattern msgPattern;
		private final Constructor<? extends ArduinoEvent> msgCtor;

		public RegisteredMsgType(Class<? extends ArduinoEvent> messageType)
		{
			try
			{
				msgType = messageType;
				Field f = msgType.getField(REGEX_FIELD_NAME);
				if (f.getType().isAssignableFrom(Pattern.class))
				{
					msgPattern = (Pattern) f.get(null);
				}
				else
				{
					throw new IllegalStateException(String.format(
						"The field %1$s of class %2$s is of type %3$s (should be of type %4$s)",
						REGEX_FIELD_NAME, msgType.getName(), f.getType().getName(), Pattern.class.getName()));
				}
				msgCtor = msgType.getConstructor(Matcher.class);
			}
			catch (NoSuchMethodException | SecurityException | NoSuchFieldException | IllegalArgumentException
				| IllegalAccessException ex)
			{
				throw new IllegalStateException(String.format(
					"Unable to register message type %1$s due to %2$s:  %3$s",
					messageType.getName(), ex.getClass().getSimpleName(), ex.getMessage()), ex);
			}
		}

		public Pattern getMsgPattern()
		{
			return msgPattern;
		}

		public ArduinoEvent invokeCtor(Matcher matcher)
		{
			try
			{
				return msgCtor.newInstance(matcher);
			}
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException ex)
			{
				throw new RuntimeException("Unable to invoke registered message type's constructor:", ex);
			}
		}
	}

	private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("h:mm:ss a");
	private static final Map<Class<? extends ArduinoEvent>, RegisteredMsgType> registeredMsgTypes = new HashMap<>();

	private Date wallClockTime;

	public static void registerMsgType(Class<? extends ArduinoEvent> messageType)
	{
		if (!registeredMsgTypes.containsKey(messageType))
		{
			registeredMsgTypes.put(messageType, new RegisteredMsgType(messageType));
		}
	}

	public static ArduinoEvent parse(String eventMsg)
	{
		ArduinoEvent result = null;
		for (RegisteredMsgType mt : registeredMsgTypes.values())
		{
			Matcher matcher = mt.getMsgPattern().matcher(eventMsg);
			if (matcher.matches())
			{
				result = mt.invokeCtor(matcher);
				break;
			}
		}
		return result;
	}

	protected ArduinoEvent()
	{
		wallClockTime = new Date();
	}

	protected String formatWallClockTime()
	{
		return TIME_FMT.format(wallClockTime);
	}

	public abstract String format();
}
