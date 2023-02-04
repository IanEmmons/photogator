package org.virginiaso.serialport;

import java.io.PrintStream;
import java.util.function.Consumer;

import jssc.SerialPort;
import jssc.SerialPortException;

public class SerialPortReader implements AutoCloseable
{
	private static final int BAUD_RATE = SerialPort.BAUDRATE_57600;
	private static final int DATA_BITS = SerialPort.DATABITS_8;
	private static final int STOP_BITS = SerialPort.STOPBITS_1;
	private static final int PARITY = SerialPort.PARITY_NONE;
	private static final int FLOW_CTRL_MODE = SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT;

	private Consumer<String> lstnr;
	private SerialPort port;

	public SerialPortReader(String serialPortName, Consumer<String> listener, PrintStream errorLog)
		throws SerialPortException
	{
		lstnr = listener;
		port = new SerialPort(serialPortName);
		if (!port.openPort())
		{
			throw new SerialPortException(port, "openPort", "Unable to open serial port");
		}
		if (!port.setParams(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY))
		{
			throw new SerialPortException(port, "setParams", "Unable to set serial port parameters");
		}
		if (!port.setFlowControlMode(FLOW_CTRL_MODE))
		{
			throw new SerialPortException(port, "setFlowControlMode", "Unable to set serial port flow control");
		}
		port.addEventListener(new BufferingSerialPortListener(port, lstnr, errorLog), SerialPort.MASK_RXCHAR);
	}

	public String getSerialPortName()
	{
		return port.getPortName();
	}

	@Override
	public void close() throws SerialPortException
	{
		if (port != null)
		{
			port.removeEventListener();
			port.closePort();
			port = null;
		}
	}
}
