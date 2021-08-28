/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import com.smilecoms.xml.schema.pos.InventoryItem;
import com.smilecoms.xml.schema.pos.InventoryList;
import com.smilecoms.xml.schema.pos.InventoryQuery;
import java.util.List;
import javax.persistence.EntityManager;

/**
 *
 * @author paul
 */
public interface InventorySystem {

    public void initialise() throws Exception;
    
    public void close();
    
    public InventoryItem getInventoryItem(EntityManager em, String itemNumber, String warehouseId, int salesPersonCustomerId, long recipientAccountId, String currency) throws Exception;

    public InventoryList getInventory(EntityManager em, InventoryQuery inventoryQuery) throws Exception;

    public List<InventoryItem> getSubItems(EntityManager em, String itemNumber, String serialNumber, String warehouseId, int salesPersonCustomerId, long recipientAccountId, String currency) throws Exception;

    public List<String> getDimensions(String channel, String saleLocation) throws Exception;
    
    public boolean doesLocationExist(String location) throws Exception;

    public String getAvailableSerialNumber(EntityManager em, String itemNumber, String warehouseId) throws Exception;
    
    public void verifySerialNumbers(EntityManager em, List<String[]> serialNumbers, String warehouseId) throws Exception;

    public String getInventoryWarehouseId(String serialNumber) throws Exception;

    public String getWarehouseForLocation(String warehouseId) throws Exception;
    
    public String getLocationForWarehouse(String location) throws Exception;
}
