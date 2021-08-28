/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.rating;

import com.smilecoms.bm.db.model.ServiceRate;
import com.smilecoms.commons.base.BaseUtils;
import java.util.*;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class PrefixMapNode {

    private Map<String, PrefixMapNode> children = null;
    private boolean hasChildren = false;
    private String leafKey = null;
    private final Map<Integer, ServiceRate> leafData = new HashMap<>();
    private int indexForChildrenLookup; // also consider this to be the length of this leafs key (if it is a leaf)
    private static final Logger log = LoggerFactory.getLogger(PrefixMapNode.class);

    public PrefixMapNode() {
        this(0);
    }

    private PrefixMapNode(int indexForChildrenLookup) {
        this.indexForChildrenLookup = indexForChildrenLookup;
    }

    public void populate(String key, ServiceRate rateData) {
        if (key.equals("0") && BaseUtils.getBooleanProperty("env.bm.rates.0.shortcut", true)) {
            log.debug("Key is 0 so its a shortcut for 1,2,3,4,5,6,7,8,9");
            populate("1".toCharArray(), rateData);
            populate("2".toCharArray(), rateData);
            populate("3".toCharArray(), rateData);
            populate("4".toCharArray(), rateData);
            populate("5".toCharArray(), rateData);
            populate("6".toCharArray(), rateData);
            populate("7".toCharArray(), rateData);
            populate("8".toCharArray(), rateData);
            populate("9".toCharArray(), rateData);
        } else {
            populate(key.toCharArray(), rateData);
        }
    }

    private void populate(char[] originalKey, ServiceRate rateData) {

        if (indexForChildrenLookup == originalKey.length) {
            //This is the leaf node for a key
            leafKey = new String(originalKey);
            leafData.put(rateData.getServiceRateId(), rateData);
            return;
        }
        if (!hasChildren) {
            hasChildren = true;
            children = new HashMap<>(10);
        }
        char nextChar = originalKey[indexForChildrenLookup];
        String keyOfChildNode = String.valueOf(nextChar);
        PrefixMapNode child = children.get(keyOfChildNode);
        if (child == null) {
            child = new PrefixMapNode(indexForChildrenLookup + 1);
            children.put(keyOfChildNode, child);
        }

        child.populate(originalKey, rateData);
    }

    public PrefixMapNode getBestMatch(String key) {
        PrefixMapNode bestNode = getDeepestLeaf(key.toCharArray());
        return bestNode;
    }

    private PrefixMapNode getDeepestLeaf(char[] key) {

        //key.length == indexForChildrenLookup is due to:
        // maxkeyidex = key.length - 1;
        // myindex = indexForChildrenLookup -1 ;
        // if (myindex == maxkeyidex) is same as  key.length - 1 == indexForChildrenLookup -1 which is  key.length == indexForChildrenLookup
        // i.e. this instance is as deep as we need go
        if (!hasChildren || key.length == indexForChildrenLookup) {
            //This is a leaf so we cant look at our children
            return this;
        }

        char myChildrensChar = key[indexForChildrenLookup];
        String keyOfChildNode = String.valueOf(myChildrensChar);
        PrefixMapNode child = children.get(keyOfChildNode);
        if (child == null) {
            // No matching children. I'm the best you can get
            return this;
        }
        PrefixMapNode deepest = child.getDeepestLeaf(key);
        if (deepest.getLeafKey() == null) {
            // Child has found something deeper, but it has no data. Return me instead
            return this;
        }
        return deepest;
    }

    public Map<Integer, ServiceRate> getAllLeafData() {
        return leafData;
    }

    public String getLeafKey() {
        return leafKey;
    }
}
