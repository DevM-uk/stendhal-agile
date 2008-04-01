package games.stendhal.server.maps.kalavan.citygardens;

import games.stendhal.common.Grammar;
import games.stendhal.server.core.config.ZoneConfigurator;
import games.stendhal.server.core.engine.SingletonRepository;
import games.stendhal.server.core.engine.StendhalRPZone;
import games.stendhal.server.entity.item.StackableItem;
import games.stendhal.server.core.pathfinder.FixedPath;
import games.stendhal.server.core.pathfinder.Node;
import games.stendhal.server.entity.npc.ConversationPhrases;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.entity.npc.behaviour.adder.ProducerAdder;
import games.stendhal.server.entity.npc.behaviour.impl.ProducerBehaviour;
import games.stendhal.server.entity.player.Player;
import games.stendhal.server.util.TimeUtil;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Builds the gardener in Kalavan city gardens.
 *
 * @author kymara
 */
public class GardenerNPC implements ZoneConfigurator {

	private static final String QUEST_SLOT = "sue_swap_kalavan_city_scroll";

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
	public void configureZone(StendhalRPZone zone,
			Map<String, String> attributes) {
		buildNPC(zone, attributes);
	}

	private void buildNPC(StendhalRPZone zone, Map<String, String> attributes) {
		SpeakerNPC npc = new SpeakerNPC("Sue") {

			@Override
			protected void createPath() {
				List<Node> nodes = new LinkedList<Node>();
				nodes.add(new Node(100, 123));
				nodes.add(new Node(110, 123));
				nodes.add(new Node(110, 110));
				nodes.add(new Node(119, 110));
				nodes.add(new Node(119, 122));
				nodes.add(new Node(127, 122));
				nodes.add(new Node(127, 111));
				nodes.add(new Node(118, 111));
				nodes.add(new Node(118, 123));
				setPath(new FixedPath(nodes, true));
			}

			@Override
			protected void createDialog() {
				class SpecialProducerBehaviour extends ProducerBehaviour { 
					SpecialProducerBehaviour(String productionActivity,
                        String productName, Map<String, Integer> requiredResourcesPerItem,
											 int productionTimePerItem) {
						super(QUEST_SLOT, productionActivity, productName,
							  requiredResourcesPerItem, productionTimePerItem, false);
					}

					@Override
						public boolean askForResources(SpeakerNPC npc, Player player, int amount) {
						if (getMaximalAmount(player) < amount) {
							npc.say("I would only " + getProductionActivity() + " you "
									+ Grammar.quantityplnoun(amount, getProductName())
									+ " if you bring me "
									+ getRequiredResourceNamesWithHashes(amount) + ".");
							return false;
						} else {
							setAmount(amount);
							npc.say("Then I'll want "
									+ getRequiredResourceNamesWithHashes(amount)
									+ ". Did you bring that?");
							return true;
						}
					}
					@Override
						public boolean transactAgreedDeal(SpeakerNPC npc, Player player) {
						if (getMaximalAmount(player) < amount) {
							// The player tried to cheat us by placing the resource
							// onto the ground after saying "yes"
							npc.say("Hey! I'm over here! You'd better not be trying to trick me...");
							return false;
						} else {
							for (Map.Entry<String, Integer> entry : getRequiredResourcesPerItem().entrySet()) {
                                int amountToDrop = amount * entry.getValue();
                                player.drop(entry.getKey(), amountToDrop);
							}
							long timeNow = new Date().getTime();
							player.setQuest(QUEST_SLOT, amount + ";" + getProductName() + ";"
											+ timeNow);
							npc.say("Thanks for the "
									+ amount
									+ " lunches! Come back in "
									+ getApproximateRemainingTime(player) + ", when I've finished eating.");
							return true;
						}
					}
					@Override
						public void giveProduct(SpeakerNPC npc, Player player) {
						String orderString = player.getQuest(QUEST_SLOT);
						String[] order = orderString.split(";");
						int numberOfProductItems = Integer.parseInt(order[0]);
						// String productName = order[1];
						long orderTime = Long.parseLong(order[2]);
						long timeNow = new Date().getTime();
						if (timeNow - orderTime < getProductionTime(numberOfProductItems) * 1000) {
							npc.say("Hello again! I'm still eating lunch. Come back in "
									+ getApproximateRemainingTime(player) + " to get your scrolls.");
						} else {
                        StackableItem products = (StackableItem) SingletonRepository.getEntityManager().getItem(
                                        getProductName());

                        products.setQuantity(numberOfProductItems);

                        if (isProductBound()) {
							products.setBoundTo(player.getName());
                        }

                        player.equip(products, true);
                        npc.say("Welcome back! I've finished lunch. In exchange here you have "
								+ Grammar.quantityplnoun(numberOfProductItems,
                                                        getProductName()) + ".");
                        player.setQuest(QUEST_SLOT, "done");
                        // give some XP as a little bonus for industrious workers
                        player.addXP(15*numberOfProductItems);
                        player.notifyWorldAboutChanges();
						}
					}
				}
				addReply(ConversationPhrases.YES_MESSAGES, "Very warm...");
				addReply(ConversationPhrases.NO_MESSAGES, "It's better than rain!");
				addJob("I am the gardener. I hope you like the flowerbeds.");
				addHelp("If you bring me some #lunch I'll #swap you for a scroll.");
				addOffer("I haven't anything to trade yet. Wait till the autumn when I'll have some cuttings and seeds.");
				addReply("lunch", "Tea and a sandwich, please!");
				addReply("sandwich", "Mmm.. I'd like a ham and cheese one.");
				addReply("kalavan city scroll", "It's a magic scroll that would take you back to Kalavan. Just don't ask me how it works!");
				Map<String, Integer> requiredResources = new TreeMap<String, Integer>();	// use sorted TreeMap instead of HashMap
				requiredResources.put("tea", 1);
				requiredResources.put("sandwich", 1);

				ProducerBehaviour behaviour = new SpecialProducerBehaviour("swap", "kalavan city scroll", requiredResources, 10 * 60);

				new ProducerAdder().addProducer(this, behaviour,
				        "Fine day, isn't it?");
				addQuest("I'd love a cup of #tea, it's thirsty work, gardening.");
				addReply("tea", "Old Granny Graham may brew you a cup. She's in that big cottage over there.");
				addGoodbye("Bye. Enjoy the rest of the gardens.");
			}
		};

		npc.setEntityClass("gardenernpc");
		npc.setPosition(100, 123);
		npc.initHP(100);
		zone.add(npc);
	}
}
