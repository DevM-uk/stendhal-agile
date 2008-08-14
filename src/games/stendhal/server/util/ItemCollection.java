package games.stendhal.server.util;

import games.stendhal.common.Grammar;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * ItemCollection is a collection of items with associated amount.
 * It can be constructed from a semicolon-separated quest state string.
 * Item names and amounts are separated by equal signs, so the format is:
 * item1=5;item2=1;C=3;D=1
 *
 * @author Martin Fuchs
 */
@SuppressWarnings("serial")
public class ItemCollection extends TreeMap<String, Integer> {

    /**
     * Construct an ItemCollection from a quest state string in
     * the form "item1=n1;item2=n2;...".
     * @param str
     */
    public void addFromQuestStateString(final String str) {
	    if (str != null) {
	        final List<String> items = Arrays.asList(str.split(";"));

    		for (final String item : items) {
    			final String[] pair = item.split("=");

    			if (pair.length == 2) {
        			addItem(pair[0], Integer.parseInt(pair[1]));
    			}
    		}
		}
	}

    /**
     * Return the items as quest state string.
     * @return semicolon separated states list
     */
    public String toStringForQuestState() {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (final Map.Entry<String, Integer> e : entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(';');
            }

            sb.append(e.getKey());
            sb.append("=");
            sb.append(e.getValue());
        }

        return sb.toString();
    }

	/**
	 * Remove the specified amount of items from the collection.
	 * @param itemName
	 * @param amount
	 * @return true if amount has been updated
	 */
	public boolean removeItem(final String itemName, final int amount) {
    	Integer curAmount = get(itemName);

    	if (curAmount != null) {
        	if (curAmount >= amount) {
        		curAmount -= amount;

        		if (curAmount > 0) {
                    put(itemName, curAmount);
        		} else {
        		    remove(itemName);
        		}

        		return true;
        	} else {
        		return false;
        	}
        }

    	return false;
	}

    /**
     * Add the specified amount of items to the collection.
     * @param itemName
     * @param amount
     */
    public void addItem(final String itemName, final int amount) {
        final Integer curAmount = get(itemName);

        if (curAmount != null) {
            put(itemName, curAmount + amount);
        } else {
            put(itemName, amount);
        }
    }

    /**
     * @return a String list containing the items in the format "xxx=n".
     */
    public List<String> toStringList() {
        final List<String> result = new LinkedList<String>();

        for (final Map.Entry<String, Integer> item : entrySet()) {
            result.add(item.getKey() + '=' + item.getValue());
        }

        return result;
    }

    /**
     * @return a String list containing the items in the format "n #xxx, ...".
     */
    public List<String> toStringListWithHash() {
        final List<String> result = new LinkedList<String>();

        for (final Map.Entry<String, Integer> item : entrySet()) {
            result.add(Grammar.quantityplnounWithHash(item.getValue(), item.getKey()));
        }

        return result;
    }

}
