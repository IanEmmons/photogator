package org.virginiaso.photogator;

import java.awt.desktop.PreferencesEvent;
import java.awt.desktop.PreferencesHandler;
import java.awt.event.ActionEvent;

public class SettingsDesktopHandler implements PreferencesHandler {
	private Photogator photogator;

	public SettingsDesktopHandler(Photogator photogator) {
		this.photogator = photogator;
	}

	@Override
	public void handlePreferences(PreferencesEvent evt) {
		photogator.settingsBtnAction(new ActionEvent(evt.getSource(),
			ActionEvent.ACTION_FIRST, "HandlePreferences"));
	}
}
