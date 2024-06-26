--[[
 ***************************************************************************
 *                    Copyright © 2020-2023 - Arianne                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************
]]


local brokerName = "Sawyer"
local broker = nil

local function initNPC()
	broker = entities:create({
		type = "SpeakerNPC",
		name = brokerName,
		description = "You see " .. brokerName ..". He owns and runs the pawn shop.",
		outfit = {
			layers = "body=0,head=0,eyes=3,hair=14,dress=5,hat=11",
			colors = {
				body = SkinColor.DARK,
				eyes = 0x0000ff
			}
		},
		pos = {18, 5},
		idleDir = Direction.LEFT
	})

	-- dialogue
	broker:addGreeting("Welcome to the Deniran Pawn Shop.")
	broker:addGoodbye()
	broker:addHelp("If you want to pawn something, check my blackboard for a list of what I buy.")
	broker:addJob("I own this pawn shop. If you want to sell something, check my blackboard for a list of what I buy.")
	broker:addQuest("Well... I don't have anything you can help me with right now.")
	broker:addOffer("Please check the blackboard for a list of items that I buy.")

	broker:add(
		ConversationStates.ATTENDING,
		"golden blade",
		nil,
		ConversationStates.ATTENDING,
		nil,
		function(player, sentence, raiser)
			broker:say("Not a chance pal. That thing doesn't leave my sight.")
			broker:say("!me sternly looks you up and down.")
		end)

	game:add(broker)
end

local function initShop()
	-- shop sign
	local sign = entities:create({
		type = "ShopSign",
		name = "deniranpawnbuy",
		title = "Deniran Pawn Shop",
		caption = brokerName .. " buys the following items",
		seller = false
	})
	sign:setEntityClass("blackboard")
	sign:setPosition(18, 8)

	game:add(sign);
end


local zone = "int_deniran_pawn_shop"

if game:setZone(zone) then
	initNPC()
	if broker ~= nil then
		initShop()
	end
else
	logger:error("Could not set zone: " .. zone)
end
