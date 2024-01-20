package org.virginiaso.photogator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.LayoutManager;

import javax.swing.JPanel;

public final class ImagePanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	private transient Image image;

	public ImagePanel(String rsrcName)
	{
		initComponents(rsrcName);
	}

	public ImagePanel(String rsrcName, LayoutManager layout)
	{
		super(layout);
		initComponents(rsrcName);
	}

	public ImagePanel(String rsrcName, boolean isDoubleBuffered)
	{
		super(isDoubleBuffered);
		initComponents(rsrcName);
	}

	public ImagePanel(String rsrcName, LayoutManager layout, boolean isDoubleBuffered)
	{
		super(layout, isDoubleBuffered);
		initComponents(rsrcName);
	}

	private void initComponents(String rsrcName)
	{
		image = Photogator.loadImageRsrc(rsrcName);
		try
		{
			Thread.sleep(200);		// Pause to ensure asynchronous image loading completes
		}
		catch (InterruptedException ex)
		{
			// Ignore
			ex.printStackTrace(Photogator.ERR_LOG);
		}
		int imgWidth = image.getWidth(null);
		int imgHeight = image.getHeight(null);
		setMinimumSize(new Dimension(imgWidth / 2, imgHeight / 2));
		setMaximumSize(new Dimension(imgWidth * 2, imgHeight * 2));
		setPreferredSize(new Dimension(imgWidth, imgHeight));
	}

	@Override
	public void paint(Graphics g)
	{
		g.drawImage(image, 0, 0, getWidth(), getHeight(), Color.WHITE, null);
	}
}
