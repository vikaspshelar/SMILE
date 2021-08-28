/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.customerwizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author paul
 */
public class Leaf {

    public enum LEAF_TYPE {

        TT,
        EXTERNAL_RESOURCE,
        EXPLANATION
    }
    private LEAF_TYPE myType;
    private String id;
    private static final Map<String, Leaf> allLeaves = new HashMap<String, Leaf>();
    private String parentStepId;
    private String leafName;
    private Leaf ttLeaf;
    
    public Leaf(LEAF_TYPE type, String id, String parentStepId, String leafName, Leaf ttLeaf) {
        myType = type;
        this.id = id;
        this.parentStepId = parentStepId;
        this.leafName = leafName;
        this.ttLeaf = ttLeaf;
        allLeaves.put(id, this);
    }
    private List<LeafDataItem> dataItems = new ArrayList<LeafDataItem>();

    protected void addLeafDataItem(LeafDataItem dataItem) {
        dataItems.add(dataItem);
    }

    public List<LeafDataItem> getLeafDataItems() {
        return dataItems;
    }

    public String getType() {
        return myType.toString();
    }

    public static Leaf getLeafById(String leafId) {
        Leaf leaf = allLeaves.get(leafId);
        return leaf;
    }

    public Step getParentStep() {
        if (parentStepId == null) {
            return null;
        }
        return Step.getStepById(parentStepId);
    }
    
    public String getLeafId() {
        return id;
    }

    public String getLeafName() {
        return leafName;
    }
    
    public Leaf getTTLeaf() {
        return ttLeaf;
    }
    
}
