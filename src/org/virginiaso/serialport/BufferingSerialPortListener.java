package org.virginiaso.serialport;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class BufferingSerialPortListener implements SerialPortEventListener {
	private final Consumer<String> lstnr;
	private final SerialPort port;
	private byte[] buffer;
	private int bufferLength;
	private PrintStream errLog;

	public BufferingSerialPortListener(SerialPort serialPort, Consumer<String> listener, PrintStream errorLog) {
		lstnr = listener;
		port = serialPort;
		buffer = new byte[256];	// needs to be long enough for the largest message
		bufferLength = 0;
		errLog = errorLog;
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		if(event.isRXCHAR() && event.getEventValue() > 0) {
			try {
				byte[] bytesRecieved = port.readBytes();
				for (byte b : bytesRecieved) {
					if (b == 0) {
						lstnr.accept(new String(buffer, 0, bufferLength, StandardCharsets.UTF_8));
						bufferLength = 0;
					} else {
						buffer[bufferLength++] = b;
					}
				}
			} catch (SerialPortException ex) {
				ex.printStackTrace(errLog);
			}
		}
	}
}
