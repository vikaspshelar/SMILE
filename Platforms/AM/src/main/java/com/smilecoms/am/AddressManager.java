/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am;

import com.smilecoms.am.db.model.AvailableIPAddress;
import com.smilecoms.am.db.model.AvailableNumber;
import com.smilecoms.am.db.model.MnpPortRequest;
import com.smilecoms.am.db.op.DAO;
import com.smilecoms.am.np.PortInEventHandler;
import com.smilecoms.am.np.MnpHelper;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.am.AMError;
import com.smilecoms.xml.am.AMSoap;
import com.smilecoms.xml.schema.am.AvailableIPs;
import com.smilecoms.xml.schema.am.AvailableIPsQuery;
import com.smilecoms.xml.schema.am.AvailableNumberRange;
import com.smilecoms.xml.schema.am.Done;
import com.smilecoms.xml.schema.am.FreeIPQuery;
import com.smilecoms.xml.schema.am.IssueIPQuery;
import com.smilecoms.xml.schema.am.NumberList;
import com.smilecoms.xml.schema.am.IssuedIPsQuery;
import com.smilecoms.xml.schema.am.IssuedIPsResult;
import com.smilecoms.xml.schema.am.NumberReservationData;
import com.smilecoms.xml.schema.am.NumbersQuery;
import com.smilecoms.xml.schema.am.PlatformString;
import com.smilecoms.xml.schema.am.PortInEvent;
import com.smilecoms.xml.schema.am.PortOrdersList;
import com.smilecoms.xml.schema.am.PortOrdersQuery;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@WebService(serviceName = "AM", portName = "AMSoap", endpointInterface = "com.smilecoms.xml.am.AMSoap", targetNamespace = "http://xml.smilecoms.com/AM", wsdlLocation = "AMServiceDefinition.wsdl")
@Stateless
@HandlerChain(file = "/handler.xml")
public class AddressManager extends SmileWebService implements AMSoap {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;
    @PersistenceContext(unitName = "AMPU")
    private EntityManager em;
    private static final Random random = new Random();
    private static final Lock portInLock = new java.util.concurrent.locks.ReentrantLock();
    private static final Lock portOutLock = new java.util.concurrent.locks.ReentrantLock();

    /**
     * Returns a number that is not issued. This method selects a random number
     * from the set of unissued numbers. A count of uniisued numbers is first
     * created. This number is multiplied by a random number where 1 =< n < 0
     * and rounded down to give a number between 0 and count-1. A number is then
     * selected from the set of available numbers using this random index.
     *
     *
     *
     *
     *
     *
     * @param numberOfNumbers How many numbers to return
     * @return AvailableNumbers a list of available phone numbers
     */
    @Override
    public NumberList getAvailableNumbers(NumbersQuery numbersQuery) throws AMError {
        setContext(numbersQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        //A number that has not been issued to a customer yet
        NumberList numbersAvailable = new NumberList();
        //How many number to return - defined in Properties

        try {

            int numberOfResults = numbersQuery.getResultLimit();

            if (log.isDebugEnabled()) {
                log.debug("Number Of Results: " + numberOfResults);
            }

            Collection<AvailableNumber> numbers;
            if (numbersQuery.getICCID() != null && !numbersQuery.getICCID().isEmpty()) {
                log.warn("Going to look using: getNumberFromTableByICCID");
                Date releasedBefore = Utils.getFutureDate(Calendar.DATE, -1 * BaseUtils.getIntProperty("env.am.used.number.hold.days", 365));
                numbers = DAO.getNumberFromTableByICCID(em, numbersQuery.getPriceLimitCents(), numbersQuery.getICCID(), releasedBefore);
            } else {
                 log.warn("Going to look using: getRandomAvailableNumbers");
                numbers = getRandomAvailableNumbers(
                        numberOfResults, numbersQuery.getPriceLimitCents(), numbersQuery.getPattern(),
                        numbersQuery.getOwnedByCustomerProfileId(),
                        numbersQuery.getOwnedByOrganisationId());
            }

            for (AvailableNumber number : numbers) {
                com.smilecoms.xml.schema.am.Number numXML = new com.smilecoms.xml.schema.am.Number();
                numXML.setIMPU(number.getIMPU());
                numXML.setPriceCents(number.getPriceCents());
                numXML.setOwnedByCustomerProfileId(number.getOwnedByCustomerProfileId());
                numXML.setOwnedByOrganisationId(number.getOwnedByOrganisationId());
                numXML.setICCID(number.getICCID() == null ? "" : number.getICCID());
                numbersAvailable.getNumbers().add(numXML);
            }
            numbersAvailable.setNumberOfNumbers(numbersAvailable.getNumbers().size());
        } catch (Exception e) {
            throw processError(AMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }

        return numbersAvailable;
    }

    /**
     * Issue a number by setting the Issued field in the available_number table
     * to 1. Set the issued date to the current date.
     *
     * @param number A number that must be issued
     * @return Done when the method completes successfully
     */
    @Override
    public com.smilecoms.xml.schema.am.Done issueNumber(PlatformString number) throws AMError {
        setContext(number, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            int numberOfNumbersIssued = DAO.makeNumberIssued(em, number.getString());
            if (log.isDebugEnabled()) {
                log.debug("numberOfNumbersIssued: " + numberOfNumbersIssued);
            }
            if (numberOfNumbersIssued == 0) {
                log.debug("Number has already been issued");
                throw new Exception("The number has been issued to another subscriber or does not exist -- " + number.getString());
            }
            createEvent(number.getString());
        } catch (Exception e) {
            throw processError(AMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    /**
     * Free up a number that was previously issued
     *
     * @param numberToFree e.g. 0871112222
     * @return Done
     * @throws com.smilecoms.xml.am.AMError
     */
    @Override
    public Done freeNumber(PlatformString numberToFree) throws AMError {
        setContext(numberToFree, wsctx);
        if (log.isDebugEnabled()) {
            logStart(numberToFree.getString());
        }
        try {
            AvailableNumber number = em.find(AvailableNumber.class, numberToFree.getString());
            number.setIssued(0);
            number.setIssuedDateTime(null);
            number.setReleasedDateTime(new Date());
            em.persist(number);
            createEvent(numberToFree.getString());
        } catch (Exception e) {
            throw processError(AMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done reserveNumber(NumberReservationData numberReservationData) throws AMError {
        setContext(numberReservationData, wsctx);
        if (log.isDebugEnabled()) {
            logStart(numberReservationData.getIMPU());
        }
        try {
            AvailableNumber number = em.find(AvailableNumber.class, numberReservationData.getIMPU());
            number.setOwnedByCustomerProfileId(numberReservationData.getOwnedByCustomerProfileId());
            number.setOwnedByOrganisationId(numberReservationData.getOwnedByOrganisationId());
            em.persist(number);
            createEvent(numberReservationData.getIMPU());
        } catch (Exception e) {
            throw processError(AMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public NumberList getIssuedNumbers(NumbersQuery issuedNumbersQuery) throws AMError {
        setContext(issuedNumbersQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        NumberList result = new NumberList();
        try {
            String pattern = issuedNumbersQuery.getPattern();
            int resultLimit = issuedNumbersQuery.getResultLimit();

            if (log.isDebugEnabled()) {
                log.debug("pattern: [" + pattern + "]");
                log.debug("resultLimit: [" + resultLimit + "]");
            }

            List<String> dataList = DAO.getIssuedNumbers(em, pattern, resultLimit);
            if (log.isDebugEnabled()) {
                log.debug("dataListSize [{}] ", dataList.size());
            }
            for (String num : dataList) {
                com.smilecoms.xml.schema.am.Number numXML = new com.smilecoms.xml.schema.am.Number();
                numXML.setIMPU(num);
                result.getNumbers().add(numXML);
            }

        } catch (Exception e) {
            throw processError(AMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    @Override
    public Done isUp(String isUpRequest) throws AMError {
        if (!BaseUtils.isPropsAvailable()) {
            throw createError(AMError.class, "Properties are not available so this platform will be reported as down");
        }
        return makeDone();
    }

    /**
     * Utility method to make a complex boolean object with value TRUE.
     *
     * @return The resulting complex object
     */
    private com.smilecoms.xml.schema.am.Done makeDone() {
        com.smilecoms.xml.schema.am.Done done = new com.smilecoms.xml.schema.am.Done();
        done.setDone(com.smilecoms.xml.schema.am.StDone.TRUE);
        return done;
    }

    @Override
    public AvailableIPs getAvailableIPs(AvailableIPsQuery availableIPsQuery) throws AMError {
        setContext(availableIPsQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        //A number that has not been issued to a customer yet
        AvailableIPs ipsAvailable = new AvailableIPs();
        //How many number to return - defined in Properties

        int numberOfResults = availableIPsQuery.getNumberOfIPs();
        String impiString = availableIPsQuery.getIMPI();
        String regionString = availableIPsQuery.getRegion();
        String apnList = availableIPsQuery.getApnList();
        String applicableApnRegex = availableIPsQuery.getApplicableApnRegex();
        String privateOnly = availableIPsQuery.getPrivateOnly();

        if (apnList.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("APN list is empty so we set it to default APN: [{}]", BaseUtils.getProperty("env.staticip.apnname", "internet"));
            }
            apnList = BaseUtils.getProperty("env.staticip.apnname", "internet");
        }

        try {

            if (apnList.matches(applicableApnRegex)) {
                log.debug("Apn list matches applicableApnRegex");
            } else {
                String msg = String.format("The APN configured [%s] is not compatible with the static IP bundle APN configuration [%s] - please assign this SIM to the correct APN", apnList, applicableApnRegex);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "AM", msg);
                log.warn(msg);
                throw new Exception("The APN configured is not compatible with the static IP bundle APN configuration - please assign this SIM to the correct APN");
            }

            if (log.isDebugEnabled()) {
                log.debug("Number Of Results: [{}] IMPI: [{}]", numberOfResults, impiString);
                log.debug("Region: [{}]", regionString);
                log.debug("apnList: [{}]", apnList);
                log.debug("applicableApnRegex: [{}]", applicableApnRegex);
                log.debug("privateOnly: [{}]", privateOnly);
            }

            Collection<String> ips = getRandomAvailableIPs(numberOfResults, impiString, regionString, apnList, privateOnly);

            ipsAvailable.getIPs().addAll(ips);

            if (ipsAvailable.getIPs().isEmpty()) {
                String msg = String.format("No available IPs for this region [%s] and APN [%s] - probably need more IPs assigned", regionString, apnList);
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "AM", msg);
                log.warn(msg);
                throw new Exception("No available IPs for this region - probably need more IPs assigned");
            }

        } catch (Exception e) {
            throw processError(AMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }

        return ipsAvailable;
    }

    @Override
    public Done issueIP(IssueIPQuery issueIPQuery) throws AMError {
        setContext(issueIPQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            String ipToIssue = issueIPQuery.getIPToIssue();
            String impi = issueIPQuery.getIMPI();

            int numberOfIPsIssued = DAO.makeIPIssued(em, ipToIssue, impi);
            if (log.isDebugEnabled()) {
                log.debug("numberOfIPsIssued: " + numberOfIPsIssued);
                log.debug("IPToIssue: " + ipToIssue);
                log.debug("IMPI: " + impi);
            }
            if (numberOfIPsIssued == 0) {
                log.debug("IP has already been issued");
                throw new Exception("The IP has been issued to another subscriber or does not exist -- " + ipToIssue);
            }
            createEvent(ipToIssue);
        } catch (Exception e) {
            throw processError(AMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done freeIP(FreeIPQuery freeIPQuery) throws AMError {
        setContext(freeIPQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart(freeIPQuery.getIPToFree());
        }
        try {
            DAO.makeIPFree(em, freeIPQuery.getIPToFree(), freeIPQuery.getIMPI());
            createEvent(freeIPQuery.getIPToFree());
        } catch (Exception e) {
            throw processError(AMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public IssuedIPsResult getIssuedIPs(IssuedIPsQuery issuedIPsQuery) throws AMError {
        setContext(issuedIPsQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        IssuedIPsResult result = new IssuedIPsResult();
        try {
            String pattern = issuedIPsQuery.getPattern();
            int resultLimit = issuedIPsQuery.getResultLimit();

            if (log.isDebugEnabled()) {
                log.debug("pattern: [" + pattern + "]");
                log.debug("resultLimit: [" + resultLimit + "]");
            }

            List<String> dataList = DAO.getIssuedIPs(em, pattern, resultLimit);
            if (log.isDebugEnabled()) {
                log.debug("dataListSize [{}] ", dataList.size());
            }
            result.getIPs().addAll(dataList);

        } catch (Exception e) {
            throw processError(AMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    private Set<AvailableNumber> getRandomAvailableNumbers(int numberOfResults, int priceLimitCents, String pattern, int custId, int orgId) {

        //map of distinct numbers
        Set<AvailableNumber> numberObjectList = new HashSet();
        Set<String> numberSet = new HashSet();
        //A random number for a user
        List<AvailableNumber> listFromTable;
        //A random index with its maximum value the length ot the list of unissued numbers
        int randomUnissuedIndex;
        //A number from the list of available numbers
        AvailableNumber number;
        if (pattern == null || pattern.isEmpty()) {
            pattern = "%";
        }

        Date releasedBefore = Utils.getFutureDate(Calendar.DATE, -1 * BaseUtils.getIntProperty("env.am.used.number.hold.days", 365));

        listFromTable = DAO.getNumbersFromTable(em, priceLimitCents, pattern, custId, orgId, releasedBefore);

        while (numberSet.size() < numberOfResults && numberSet.size() < listFromTable.size()) {
            randomUnissuedIndex = random.nextInt(listFromTable.size());
            if (log.isDebugEnabled()) {
                log.debug("randomUnissuedIndex: " + randomUnissuedIndex);
            }
            number = listFromTable.get(randomUnissuedIndex);

            if (numberSet.add(number.getIMPU())) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding number: " + number.getIMPU());
                }
                numberObjectList.add(number);
            }
        }
        return numberObjectList;
    }

    private Set<String> getRandomAvailableIPs(int numberOfResults, String impi, String region, String apnList, String privateOnly) {

        Set<String> ipList = new HashSet();
        List<AvailableIPAddress> listFromTable;
        int randomUnissuedIndex;
        String ip;

        listFromTable = DAO.getIPsFromTable(em, impi, region, apnList);

        if (privateOnly != null && !privateOnly.isEmpty() && Boolean.valueOf(privateOnly)) {
            log.debug("Only private IPs can be assigned - checking if first IP is private (We are assume IPs in an APN are either all private or all public)");
            try {
                String firstIP = listFromTable.get(0).getIPAddress();
                if (!InetAddress.getByName(firstIP).isSiteLocalAddress()) {
                    log.debug("This IP [{}] is not private so we can not allocate", firstIP);
                    return ipList;
                }
            } catch (UnknownHostException ex) {
                log.warn("Error converted IP to Inetaddress, will not allocate IP");
                return ipList;
            }
        }

        while (ipList.size() < numberOfResults && ipList.size() < listFromTable.size()) {
            randomUnissuedIndex = random.nextInt(listFromTable.size());
            if (log.isDebugEnabled()) {
                log.debug("randomUnissuedIndex: " + randomUnissuedIndex);
            }
            ip = (listFromTable.get(randomUnissuedIndex)).getIPAddress();
            if (log.isDebugEnabled()) {
                log.debug("Adding IP: " + ip);
            }
            ipList.add(ip);
        }
        return ipList;
    }

    @Override
    public PortInEvent handlePortInEvent(PortInEvent portInEventRequest) throws AMError {
        setContext(portInEventRequest, wsctx);
        try {

            //Are we processing inbound or outbound portation request?
            if (portInEventRequest.getPortingDirection() == null) {
                // For some messages - such as NPExecuteCancel, NPExecuteBroadcast it is not possible to detemine the direction of the port. 
                // so pass the message to both inbound and outbound state machines.
                log.warn("This message has no porting direction set, so will be passed to both state machines for handling - (type = {}, port order id = {})", portInEventRequest.getMessageType(), portInEventRequest.getPortingOrderId());
                try {
                    portInLock.lock();
                    PortInEventHandler.getPortInStateMachine().handleState(em, portInEventRequest);

                } catch (Exception ex) {
                    log.error("Error while trying to invoke PortInStateMachine.handleState(...)", ex);
                    throw ex;
                } finally {
                    portInLock.unlock();
                }

                try {
                    portOutLock.lock();
                    PortInEventHandler.getPortOutStateMachine().handleState(em, portInEventRequest);
                } catch (Exception ex) {
                    log.error("Error while trying to invoke PortOutStateMachine.handleState(...)", ex);
                    throw ex;
                } finally {
                    portOutLock.unlock();
                }

                return portInEventRequest;

            } else if (portInEventRequest.getPortingDirection().equals(MnpHelper.MNP_PORTING_DIRECTION_IN)) {
                log.debug("Passing message to the inbound state machine (type = {}, port order id = {})", portInEventRequest.getMessageType(), portInEventRequest.getPortingOrderId());
                try {
                    portInLock.lock();
                    return PortInEventHandler.getPortInStateMachine().handleState(em, portInEventRequest);
                } catch (Exception ex) {
                    log.error("Error while trying to invoke PortInStateMachine.handleState(...)", ex);
                    throw ex;
                } finally {
                    portInLock.unlock();
                }
            } else if (portInEventRequest.getPortingDirection().equals(MnpHelper.MNP_PORTING_DIRECTION_OUT)) {
                log.debug("Passing message to the outbound state machine (type = {}, port order id = {})", portInEventRequest.getMessageType(), portInEventRequest.getPortingOrderId());
                try {
                    portOutLock.lock();
                    return PortInEventHandler.getPortOutStateMachine().handleState(em, portInEventRequest);
                } catch (Exception ex) {
                    log.error("Error while trying to invoke PortOutStateMachine.handleState(...)", ex);
                    throw ex;
                } finally {
                    portOutLock.unlock();
                }
            }
        } catch (Exception ex) {
            throw processError(AMError.class, ex);
        }

        return null;
    }

    @Override
    public Done addAvailableNumberRange(AvailableNumberRange availableNumberRange) throws AMError {
        setContext(availableNumberRange, wsctx);
        try {
            long fromE164 = Long.parseLong(availableNumberRange.getPhoneNumberRange().getPhoneNumberStart());
            long toE164 = Long.parseLong(availableNumberRange.getPhoneNumberRange().getPhoneNumberEnd());

            for (long num = fromE164; num <= toE164; num++) {
                AvailableNumber availableNumber = new AvailableNumber();
                String impu = Utils.getPublicIdentityForPhoneNumber(Utils.makeNationalDirectDial(String.valueOf(num)));
                availableNumber.setIMPU(impu);
                availableNumber.setOwnedByCustomerProfileId(availableNumberRange.getOwnedByCustomerProfileId());
                availableNumber.setOwnedByOrganisationId(availableNumberRange.getOwnedByOrganisationId());
                //availableNumber.setIssuedDateTime(null);
                availableNumber.setIssued(0);
                availableNumber.setPriceCents(availableNumberRange.getPriceCents());
                //availableNumber.setReleasedDateTime(null);

                if (availableNumberRange.getOwnedByCustomerProfileId() > 0) {
                    log.debug("Adding available number [{}] as belonging to customer profile [{}]", impu, availableNumberRange.getOwnedByCustomerProfileId());
                }

                if (availableNumberRange.getOwnedByOrganisationId() > 0) {
                    log.debug("Adding available number [{}] as belonging to organisation [{}]", impu, availableNumberRange.getOwnedByOrganisationId());
                }

                DAO.insertAvailableNumber(em, availableNumber);
            }
        } catch (Exception ex) {
            throw processError(AMError.class, ex);
        }
        return makeDone();
    }

    @Override
    public PortOrdersList getPortOrders(PortOrdersQuery portOrdersQuery) throws AMError {
        setContext(portOrdersQuery, wsctx);
        try {
            PortOrdersList pList = new PortOrdersList();
            Collection<MnpPortRequest> portOrders = null;

            if (portOrdersQuery.getPortingOrderId() != null && !portOrdersQuery.getPortingOrderId().isEmpty()) {
                if (portOrdersQuery.getPortingDirection() != null && !portOrdersQuery.getPortingDirection().isEmpty()) {
                    portOrders = DAO.getPortOrdersByPortOrderIdAndPortingDirection(em, portOrdersQuery.getPortingOrderId(), portOrdersQuery.getPortingDirection());
                } else {// No direction given so get all port orders that match the given ID.
                    portOrders = DAO.getPortOrdersByPortOrderId(em, portOrdersQuery.getPortingOrderId());
                }

            } else if ((portOrdersQuery.getCustomerProfileId() != null && portOrdersQuery.getCustomerProfileId() > 0)
                    && (portOrdersQuery.getPortingDirection() != null && !portOrdersQuery.getPortingDirection().isEmpty())
                    && (portOrdersQuery.getPortingState() != null && !portOrdersQuery.getPortingState().isEmpty())
                    && (portOrdersQuery.getProcessingState() != null && !portOrdersQuery.getProcessingState().isEmpty())) {
                // Search by CustomerProfileId, PortingDirection, PortingState and ProcessingState
                portOrders = DAO.getPortOrdersByCustomerProfileIdPortingDirectionPortingState(em,
                        portOrdersQuery.getCustomerProfileId(),
                        portOrdersQuery.getPortingDirection(),
                        portOrdersQuery.getPortingState());
                pList.setNumberOfPortationEvents(portOrders.size());
            } else if ((portOrdersQuery.getOrganisationId() != null && portOrdersQuery.getOrganisationId() > 0)
                    && (portOrdersQuery.getPortingDirection() != null && !portOrdersQuery.getPortingDirection().isEmpty())
                    && (portOrdersQuery.getPortingState() != null && !portOrdersQuery.getPortingState().isEmpty())
                    && (portOrdersQuery.getProcessingState() != null && !portOrdersQuery.getProcessingState().isEmpty())) {
                // Search by CustomerProfileId, PortingDirection, PortingState and ProcessingState
                portOrders = DAO.getPortOrdersByOrganisationIdPortingDirectionPortingState(em,
                        portOrdersQuery.getOrganisationId(),
                        portOrdersQuery.getPortingDirection(),
                        portOrdersQuery.getPortingState());
                pList.setNumberOfPortationEvents(portOrders.size());
            } else if (portOrdersQuery.getCustomerProfileId() != null && portOrdersQuery.getCustomerProfileId() > 0) {
                portOrders = DAO.getPortOrdersByCustomerProfileId(em, portOrdersQuery.getCustomerProfileId());
            } else if (portOrdersQuery.getOrganisationId() != null && portOrdersQuery.getOrganisationId() > 0) {
                portOrders = DAO.getPortOrdersByOrganisationId(em, portOrdersQuery.getOrganisationId());
            } else {
                throw new Exception("Do not know how to search for PortOrders.");
            }

            if (portOrders == null) {
                pList.setNumberOfPortationEvents(0);
            } else {
                for (MnpPortRequest dpPortation : portOrders) {
                    PortInEvent pEvent = new PortInEvent();
                    MnpHelper.synchDBPortRequestToPortInEvent(pEvent, dpPortation);
                    pEvent.getPortRequestForms().addAll(DAO.getPortOrderForms(em, pEvent.getPortingOrderId()));
                    pList.getPortInEvents().add(pEvent);
                }
                pList.setNumberOfPortationEvents(portOrders.size());
            }
            return pList;
        } catch (Exception ex) {
            throw processError(AMError.class, ex);
        }
    }
}
