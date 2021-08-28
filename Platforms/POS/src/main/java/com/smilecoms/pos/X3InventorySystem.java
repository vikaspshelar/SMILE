/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.props.PropertyFetchException;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StUnitCreditSpecificationLookupVerbosity;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.sca.UnitCreditSpecificationList;
import com.smilecoms.commons.sca.UnitCreditSpecificationQuery;
import com.smilecoms.commons.util.Utils;
import static com.smilecoms.pos.POSManager.PAYMENT_STATUS_DELAYED_PAYMENT;
import static com.smilecoms.pos.POSManager.PAYMENT_STATUS_PAID;
import static com.smilecoms.pos.POSManager.PAYMENT_STATUS_PENDING_DELIVERY;
import static com.smilecoms.pos.POSManager.PAYMENT_STATUS_PENDING_PAYMENT;
import static com.smilecoms.pos.POSManager.PAYMENT_STATUS_PENDING_VERIFICATION;
import static com.smilecoms.pos.POSManager.PAYMENT_STATUS_QUOTE;
import static com.smilecoms.pos.POSManager.PAYMENT_STATUS_SHOP_PICKUP;
import com.smilecoms.pos.db.op.DAO;
import com.smilecoms.xml.schema.pos.InventoryItem;
import com.smilecoms.xml.schema.pos.InventoryList;
import com.smilecoms.xml.schema.pos.InventoryQuery;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class X3InventorySystem implements InventorySystem {

    private static final Logger log = LoggerFactory.getLogger(X3InventorySystem.class);
    private LazyX3Connection inventorySystemConnection = null;

    @Override
    public boolean doesLocationExist(String location) throws Exception {
        if (inventorySystemConnection == null) {
            inventorySystemConnection = new LazyX3Connection(true);
        }
        return X3Helper.doesLocationExist(inventorySystemConnection, location);
    }

    @Override
    public InventoryItem getInventoryItem(EntityManager em, String itemNumber, String warehouseId, int salesPersonCustomerId, long recipientAccountId, String currency) throws Exception {
        InventoryQuery iq = new InventoryQuery();
        iq.setSalesPersonCustomerId(salesPersonCustomerId);
        iq.setWarehouseId(warehouseId);
        iq.setStringMatch(itemNumber);
        iq.setRecipientAccountId(recipientAccountId);
        iq.setCurrency(currency);
        InventoryList allInventory = getInventory(em, iq);
        for (InventoryItem item : allInventory.getInventoryItems()) {
            if (item.getItemNumber().equals(itemNumber) && (item.getWarehouseId().equals(warehouseId) || item.getWarehouseId().isEmpty())) {
                log.debug("Found item [{}] in inventory system",  item.getItemNumber());
                if(BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled",  false)) {
                    // Verify the bundle pricing hierachy is configured correctly.
                    item =  X3Helper.getUnitCreditInventoryItemPrice(item);
                } 
                return item;
            }
        }
        log.debug("Did not find item in inventory system");
        throw new Exception("Inventory item not found -- item number [" + itemNumber + "] sub-warehouse [" + warehouseId + "]");
    }

    @Override
    public String getInventoryWarehouseId(String serialNumber) throws Exception {
        return X3Helper.getInventoryWarehouseId(inventorySystemConnection, serialNumber);
    }

    @Override
    public InventoryList getInventory(EntityManager em, InventoryQuery inventoryQuery) throws Exception {
        InventoryList ret = new InventoryList();

        List<InventoryItem> itemsInWarehouse = new ArrayList<>();
        X3Helper.populateInventory(inventorySystemConnection, inventoryQuery.getWarehouseId(), inventoryQuery.getStringMatch().toUpperCase(), itemsInWarehouse, inventoryQuery.getCurrency());

        for (InventoryItem item : itemsInWarehouse) {
            ret.getInventoryItems().add(item);
        }

        log.debug("Finished getInventory for string match [{}]", inventoryQuery.getStringMatch());
        return ret;
    }

    @Override
    public List<InventoryItem> getSubItems(EntityManager em, String itemNumber, String serialNumber, String warehouseId, int salesPersonCustomerId, long recipientAccountId, String currency) throws Exception {
        // Call X3 and get the sub items that make up the kit (or empty list if its not a kit)
        return X3Helper.getSubItems(inventorySystemConnection, itemNumber, warehouseId, currency);
    }

    @Override
    public List<String> getDimensions(String channel, String saleLocation) throws Exception {
        // Look at offline data for performance reasons
        return X3Helper.getDimensionsOffline(channel, saleLocation);
    }

    @Override
    public void verifySerialNumbers(EntityManager em, List<String[]> serialNumbers, String warehouseId) throws Exception {
        if (BaseUtils.getBooleanProperty("env.is.training.environment", false)) {
            // For testing
            return;
        }
        X3Helper.verifySerialNumbers(inventorySystemConnection, em, serialNumbers, warehouseId);
    }

    @Override
    public String getAvailableSerialNumber(EntityManager em, String itemNumber, String warehouseId) throws Exception {
        log.debug("Sub Item number [{}] needs an auto populated serial number under warehouse [{}]", itemNumber, warehouseId);
        String serialNumber = null;
        int tries = 0;
        try {
            while (tries < 50) {
                tries++;
                serialNumber = DAO.getRandomSerialNumber(em, itemNumber, warehouseId);
                String status = DAO.getLastSaleStatusForSerialNumber(em, serialNumber);
                if (status == null) {
                    break;
                }
                log.debug("Serial [{}] has a last sale of status [{}]", serialNumber, status);
                if (status.equals(PAYMENT_STATUS_PAID)
                        || status.equals(PAYMENT_STATUS_PENDING_PAYMENT)
                        || status.equals(PAYMENT_STATUS_DELAYED_PAYMENT)
                        || status.equals(PAYMENT_STATUS_QUOTE)
                        || status.equals(PAYMENT_STATUS_SHOP_PICKUP)
                        || status.equals(PAYMENT_STATUS_PENDING_VERIFICATION)
                        || status.equals(PAYMENT_STATUS_PENDING_DELIVERY)) {

                } else {
                    break;
                }
            }
            if (serialNumber == null) {
                throw new Exception("No unsold serials available");
            }
        } catch (Exception e) {
            log.debug("Error getting a serial number: ", e);
            throw new Exception("No stock available for item number -- " + itemNumber + " in " + warehouseId);
        }
        return serialNumber;
    }

    @Override
    public void initialise() throws Exception {
        log.debug("In initialise for X3 Inventory System");
        if (inventorySystemConnection == null) {
            inventorySystemConnection = new LazyX3Connection();
        }
    }

    @Override
    public void close() {
        log.debug("In close for X3 Inventory System");
        if (inventorySystemConnection != null) {
            try {
                inventorySystemConnection.close();
                inventorySystemConnection = null;
            } catch (Exception e) {
            }
        }
    }

    @Override
    public String getWarehouseForLocation(String location) throws Exception {
        try {
            return BaseUtils.getSubProperty("env.pos.location.based.warehouses", location);
        } catch (PropertyFetchException e) {
            throw new Exception("Location does not have a warehouse -- " + location);
        }
    }

    @Override
    public String getLocationForWarehouse(String warehouseId) throws Exception {
        for (String row : BaseUtils.getPropertyAsList("env.pos.location.based.warehouses")) {
            String[] bits = row.split("=");
            if (bits.length == 2) {
                String whid = bits[1].trim();
                if (whid.equals(warehouseId)) {
                    return bits[0].trim();
                }
            }
        }
        throw new Exception("Warehouse does not have a location -- " + warehouseId);
    }

}
