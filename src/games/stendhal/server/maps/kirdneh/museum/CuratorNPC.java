/* $Id$ */
/***************************************************************************
 *                   (C) Copyright 2003-2024 - Stendhal                    *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.server.maps.kirdneh.museum;

import java.util.Map;

import games.stendhal.common.Direction;
import games.stendhal.server.core.config.ZoneConfigurator;
import games.stendhal.server.core.engine.StendhalRPZone;
import games.stendhal.server.entity.RPEntity;
import games.stendhal.server.entity.npc.SpeakerNPC;


/**
 * Builds a Curator NPC in Kirdneh museum .
 *
 * @author kymara
 */
public class CuratorNPC implements ZoneConfigurator {

	//
	// ZoneConfigurator
	//

	/**
	 * Configure a zone.
	 *
	 * @param zone
	 *            The zone to be configured.
	 * @param attributes
	 *            Configuration attributes.
	 */
	@Override
	public void configureZone(final StendhalRPZone zone,
			final Map<String, String> attributes) {
		buildNPC(zone);
	}

	private void buildNPC(final StendhalRPZone zone) {
		final SpeakerNPC npc = new SpeakerNPC("Hazel") {

			@Override
			protected void createPath() {
				setPath(null);
			}

			@Override
			protected void createDialog() {
				addGreeting("Welcome to Kirdneh Museum.");
				addJob("I am the curator of this museum. That means I organize the displays and look for new #exhibits.");
				addHelp("This is a place for rare artefacts and special #exhibits.");
				addReply("exhibits","Perhaps you'd have a knack for finding rare items and would like to do a #task for me.");
				// remaining behaviour defined in games.stendhal.server.maps.quests.WeeklyItemQuest
				addGoodbye("Good bye, it was pleasant talking with you.");
			}

			@Override
			protected void onGoodbye(RPEntity player) {
				setDirection(Direction.RIGHT);
			}
		};

		npc.setEntityClass("curatornpc");
		npc.setPosition(2, 38);
		npc.setDirection(Direction.RIGHT);
		npc.initHP(100);
		npc.setDescription("You see Hazel, the curator of Kirdneh museum.");
		zone.add(npc);
	}
}
