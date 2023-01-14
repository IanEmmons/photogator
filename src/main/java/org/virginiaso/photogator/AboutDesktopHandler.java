package org.virginiaso.photogator;

import java.awt.desktop.AboutEvent;
import java.awt.desktop.AboutHandler;
import java.awt.event.ActionEvent;

public class AboutDesktopHandler implements AboutHandler {
	private Photogator photogator;

	public AboutDesktopHandler(Photogator photogator) {
		this.photogator = photogator;
	}

	@Override
	public void handleAbout(AboutEvent evt) {
		photogator.aboutBtnAction(new ActionEvent(evt.getSource(),
			ActionEvent.ACTION_FIRST, "HandleAbout"));
	}
}
