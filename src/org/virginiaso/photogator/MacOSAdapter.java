package org.virginiaso.photogator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EventObject;
import java.util.function.Consumer;

public class MacOSAdapter
{
	private static class AppleEventHandler implements InvocationHandler
	{
		private Consumer<EventObject> handler;

		public AppleEventHandler(Consumer<EventObject> handler)
		{
			this.handler = handler;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
		{
			//Photogator.ERR_LOG.format("AppleEventHandler:  Method name:  %1$s%n", method.getName());
			//Photogator.ERR_LOG.format("AppleEventHandler:  Args length:  %1$d%n", args.length);
			//Photogator.ERR_LOG.format("AppleEventHandler:  Args[0] type:  %1$s%n", args[0].getClass().getName());

			handler.accept((EventObject) args[0]);
			return null;
		}
	}

	private static final boolean initSuccess;

	private static final Class<?> aboutHandlerClass;
	private static final Class<?> prefsHandlerClass;

	private static final Method setAboutHandlerMethod;
	private static final Method setPrefsHandlerMethod;

	private static final Object appObj;

	static
	{
		boolean success = false;
		Class<?> tmpAboutHandlerClass = null;
		Class<?> tmpPrefsHandlerClass = null;
		Method tmpSetAboutHandlerMethod = null;
		Method tmpSetPrefsHandlerMethod = null;
		Object tmpAppObj = null;

		if (isMacOsX())
		{
			try
			{
				Class<?> appClass = Class.forName("com.apple.eawt.Application");
				tmpAboutHandlerClass = Class.forName("com.apple.eawt.AboutHandler");
				tmpPrefsHandlerClass = Class.forName("com.apple.eawt.PreferencesHandler");

				Method appClassFactoryMethod = appClass.getMethod("getApplication");
				tmpSetAboutHandlerMethod = appClass.getMethod("setAboutHandler", tmpAboutHandlerClass);
				tmpSetPrefsHandlerMethod = appClass.getMethod("setPreferencesHandler", tmpPrefsHandlerClass);

				tmpAppObj = appClassFactoryMethod.invoke(null);

				success = true;
			}
			catch (ClassNotFoundException | NoSuchMethodException | SecurityException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
			{
				ex.printStackTrace(Photogator.ERR_LOG);
			}
		}

		initSuccess = success;

		aboutHandlerClass = initSuccess ? tmpAboutHandlerClass : null;
		prefsHandlerClass = initSuccess ? tmpPrefsHandlerClass : null;
		setAboutHandlerMethod = initSuccess ? tmpSetAboutHandlerMethod : null;
		setPrefsHandlerMethod = initSuccess ? tmpSetPrefsHandlerMethod : null;

		appObj = initSuccess ? tmpAppObj : null;
	}

	private MacOSAdapter()
	{
	}

	public static void setAboutMenuAction(Consumer<EventObject> handler)
	{
		try
		{
			if (initSuccess)
			{
				Object aboutProxy = Proxy.newProxyInstance(MacOSAdapter.class.getClassLoader(),
					new Class<?>[] { aboutHandlerClass },
					new AppleEventHandler(handler));
				setAboutHandlerMethod.invoke(appObj, aboutProxy);
			}
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
		{
			ex.printStackTrace(Photogator.ERR_LOG);
		}
	}

	public static void setPreferencesMenuAction(Consumer<EventObject> handler)
	{
		try
		{
			if (initSuccess)
			{
				Object prefsProxy = Proxy.newProxyInstance(MacOSAdapter.class.getClassLoader(),
					new Class<?>[] { prefsHandlerClass },
					new AppleEventHandler(handler));
				setPrefsHandlerMethod.invoke(appObj, prefsProxy);
			}
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
		{
			ex.printStackTrace(Photogator.ERR_LOG);
		}
	}

	private static boolean isMacOsX()
	{
		String osName = System.getProperty("os.name");
		return osName != null && osName.toLowerCase().startsWith("mac os x");
	}
}
