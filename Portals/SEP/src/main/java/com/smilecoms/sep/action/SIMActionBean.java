/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.action;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.NumberList;
import com.smilecoms.commons.sca.NumbersQuery;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.Sale;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.sep.helpers.RestServices;
import com.smilecoms.sep.helpers.ug.UCCHelper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpSession;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


/**
 *
 * @author PCB
 * 
 */
public class SIMActionBean extends SmileActionBean {     
    @DefaultHandler
    public Resolution showProvisionSIM() {
        checkPermissions(Permissions.PROVISION_SIM_CARD);
        return getDDForwardResolution("/sim/provision_sim.jsp");
    }

    public Resolution provisionSIM() {
        checkPermissions(Permissions.PROVISION_SIM_CARD);
        SCAWrapper.getUserSpecificInstance().provisionSIMCard(getNewSIMCardData());
        setPageMessage("sim.provisioned.successfully");
        return getDDForwardResolution("/sim/provision_sim.jsp");
    }

    public Resolution performSIMSwap() {
        checkPermissions(Permissions.SIM_SWAP);
        SCAWrapper.getUserSpecificInstance().performSIMSwap(getSIMSwapRequest());
        setPageMessage("sim.swapped.successfully");
        return getDDForwardResolution("/sim/perform_sim_swap.jsp");
    }

    public Resolution showPerformSIMSwap() {
        checkPermissions(Permissions.SIM_SWAP);
        return getDDForwardResolution("/sim/perform_sim_swap.jsp");
    }
    
    
    public Resolution showVerifyRegulatorSim() {        
        return getDDForwardResolution("/sim/verify_sim_approval.jsp");
    }      
    
     public Resolution fetchNINData () {
        
        if(!BaseUtils.getProperty("env.country.name").equals("Tanzania")) {
            localiseErrorAndAddToGlobalErrors("error.system");
            return getDDForwardResolution("/sim/verify_sim_approval.jsp");            
        }
        
        CustomerQuery custQuery = new CustomerQuery();
        custQuery.setIdentityNumber(getParameter("customerNIN"));
        custQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        custQuery.setResultLimit(1);
        Customer cust = SCAWrapper.getUserSpecificInstance().getCustomer(custQuery);
            
    //    List<String> impus = getCustomersIMPUs(cust.getCustomerId());
        
    //    log.warn("impus.size() = [{}]", impus.size());
    //    if(impus.size()>0)
    //        setOwnNumbers(impus);
        
        NumbersQuery q = new NumbersQuery();
        q.setResultLimit(50);
        q.setPriceLimitCents(0);
        // Dont list any numbers owned by somebody
        q.setOwnedByCustomerProfileId(0);
        q.setOwnedByOrganisationId(0);
        NumberList list = SCAWrapper.getUserSpecificInstance().getAvailableNumbers(q);
        List<String> ret = new ArrayList();

        for (com.smilecoms.commons.sca.Number num : list.getNumbers()) {
            ret.add(Utils.getFriendlyPhoneNumber(num.getIMPU()));
        }            
        
        log.warn("ret.size() = [{}]", ret.size());
        if(ret.size()>0)
            setAvailableNumbers(ret);
        
        return getDDForwardResolution("/sim/verify_sim_approval.jsp");
        
     }
    
    public Resolution verifySimRegistration () {
        
        if(!BaseUtils.getProperty("env.country.name").equals("Tanzania")) {
            localiseErrorAndAddToGlobalErrors("error.system");
            return getDDForwardResolution("/sim/verify_sim_approval.jsp");
            
        }        
            String response="";            
            
            CustomerQuery query = new CustomerQuery();
            query.setSSOIdentity(getUser());
            query.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            query.setResultLimit(1);
            Customer agent = SCAWrapper.getUserSpecificInstance().getCustomer(query);
            
            CustomerQuery custQuery = new CustomerQuery();
            custQuery.setIdentityNumber(getParameter("customerNIN"));
            custQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            custQuery.setResultLimit(1);
            Customer cust = SCAWrapper.getUserSpecificInstance().getCustomer(custQuery);
                    
            int agentCode = agent.getCustomerId();
            String agentNIN = agent.getIdentityNumber().trim();
            
            String agentMSISDN = Utils.getCleanDestination(agent.getAlternativeContact1()).trim();
            
            String conversationId = String.valueOf(agent.getCustomerId()) + "-" + UUID.randomUUID().toString().substring(0, 8);
            
            ArrayList otherNumbers = new ArrayList();
            
            if(!Utils.getCleanDestination(cust.getAlternativeContact1()).isEmpty() && cust.getAlternativeContact1()!= null)
                otherNumbers.add(Utils.getCleanDestination(cust.getAlternativeContact1()).trim());
            
            if(!Utils.getCleanDestination(cust.getAlternativeContact2()).isEmpty() && cust.getAlternativeContact2()!= null )
                otherNumbers.add(Utils.getCleanDestination(cust.getAlternativeContact2()).trim());
            
             try {
                    URL url = new URL("http://10.24.64.20:8090/additionalSIMCard");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");

                    String nums="";
                    for(Object otherNumber: otherNumbers) {

                        if(nums.isEmpty())
                            nums = "\""+ otherNumber + "\"";
                        else
                            nums += ",\""+ otherNumber + "\"";
                    }
                                        
                    String input = "{"
                            + "\"agentCode\":\"" + agentCode + "\","
                            + "\"agentNIN\": \"" + agentNIN + "\","
                            + "\"agentMSISDN\":\"" + agentMSISDN + "\","
                            + "\"conversationId\":\"" + conversationId + "\","
                            + "\"customerMSISDN\":\"" + getParameter("customerMSISDN").trim() + "\","
                            + "\"customerNIN\":\"" + getParameter("customerNIN").trim() + "\","
                            + "\"reasonCode\":\"" + getParameter("addSimReasonCode").trim() + "\","
                            + "\"registrationCategoryCode\": \"" + getParameter("simRegistrationCategory").trim() + "\","                        
                            + "\"iccid\": \"" + getParameter("iccid").trim() + "\","                        
                            + "\"registrationType\": \"Existing\"," ;
                    
                        if(!getParameter("simRegistrationCategory").equals("2000")) {    
                           input += "\"otherNumbers\": [" + nums + "]";
                        } else {
                            input += "\"otherNumbers\": []";
                        } 
                        
                        input += "}";
                    
                    OutputStream os = conn.getOutputStream();
                    os.write(input.getBytes());
                    os.flush();

                    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode() + " - "
                                    + conn.getResponseMessage());
                    }
                    
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                                    (conn.getInputStream())));

                    
                    JSONParser parser = new JSONParser(); 
                    JSONObject json = new JSONObject();
                    
                    while ((response = br.readLine()) != null) {                        
                        try {
                            json = (JSONObject) parser.parse(response);
                        } catch (Exception e) { log.warn("Problem parsing response [{}]", response);}
                    } 
                    
                log.debug("TCRA AddSimVerify INPUT: [{}]", input);    
                log.debug("TCRA AddSimVerify RESPONSE: [{}]", json.toJSONString());
                
                String customerNIN="", customerMSISDN="", customerICCID="", tcraResponseDesc="";
                try {
                    JSONObject jsonInput = (JSONObject) parser.parse(input);
                    customerNIN= jsonInput.get("customerNIN").toString();
                    customerMSISDN= jsonInput.get("customerMSISDN").toString();
                    customerICCID= jsonInput.get("iccid").toString();
                    tcraResponseDesc= json.get("responseDescription").toString();
                    
                } catch (Exception e) {}
                
                storeAdditionalSimData(input, json.toJSONString(), customerNIN, customerMSISDN, customerICCID, tcraResponseDesc);
                                        
                if(!json.get("responseCode").equals("150")) {
                    if(json.get("responseCode").equals("999")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("151")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("152")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("153")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("154")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("155")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("156")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("000")) {                        
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                       
                    } else {
                        localiseErrorAndAddToGlobalErrors("system.error");
                        
                    }
                     
                    return getDDForwardResolution("/sim/verify_sim_approval.jsp");                   
                } else {
                    //log.warn("Returned: [{}]", json.toJSONString());
                    
                    setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                }                    
                    
                conn.disconnect();
              } catch (MalformedURLException e) {

                    e.printStackTrace();
                    localiseErrorAndAddToGlobalErrors("System error");
                     
                    return getDDForwardResolution("/sim/verify_sim_approval.jsp");  

              } catch (IOException e) {

                    e.printStackTrace();
                    localiseErrorAndAddToGlobalErrors("System error");
                     
                    return getDDForwardResolution("/sim/verify_sim_approval.jsp");  
             }                         
            
            
            return showVerifyRegulatorSim();            
    }
    
    public void storeAdditionalSimData(String tcraInput,String tcraResponse, String customerNIN, String customerMSISDN, String customerICCID, String tcraResponseDesc)
    {
        PreparedStatement ps = null;
        Connection conn = null;
        try {
                conn = JPAUtils.getNonJTAConnection("jdbc/SmileDB");
                conn.setAutoCommit(false);
                String query="insert into additional_sim_webservice_info (REQUEST_INPUT, RETURNED_RESPONSE, CUSTOMER_NIN, CUSTOMER_MSISDN,CUSTOMER_ICCID,TCRA_RESPONSE) values (?,?,?,?,?,?)";
                ps = conn.prepareStatement(query);              
                ps.setString(1, tcraInput);
                ps.setString(2, tcraResponse);
                ps.setString(3, customerNIN);
                ps.setString(4, customerMSISDN);
                ps.setString(5, customerICCID);
                ps.setString(6, tcraResponseDesc);
                
                log.warn("SQL: [{}]", ps.toString());
                int del = ps.executeUpdate();
                
                log.warn("Returned: [{}] ", del);
                ps.close();
                conn.commit();
                conn.close();
            } catch (Exception ex) {
                log.error("Error occured creating tcra data: "+ex);
            }
            finally
                {
                    if (ps != null) 
                    {
                        try {
                                ps.close();
                        } catch (SQLException ex) {
                                log.error("Error closing the prepared statement " + ex);
                        }
                    }
                    if (conn != null) 
                    {
                        try {
                                conn.close();
                        } catch (SQLException ex) {
                                log.error("Error closing the connection " + ex);
                        }
                    }
                }
        
    }
    
    
    private List<String> getCustomersIMPUs(int customerProfileId) {
        List<String> impus = new ArrayList<String>();
        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(customerProfileId);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP);
        Customer cust = SCAWrapper.getAdminInstance().getCustomer(q);

        // Build up an xml doc of the phone numbers and activation codes for each
        for (ProductInstance pi : cust.getProductInstances()) {
            boolean foundOne = false;
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                ServiceInstance si = m.getServiceInstance();
                if (si.getCustomerId() == customerProfileId) {
                    log.debug("Looking at service instance id [{}]", si.getServiceInstanceId());
                    for (AVP avp : si.getAVPs()) {
                        log.debug("Looking at AVP [{}]", avp.getAttribute());
                        if (avp.getAttribute().equals("PublicIdentity")) {
                            log.debug("Found publicIdentity [{}]", avp.getValue());
                            impus.add(Utils.getFriendlyPhoneNumber(avp.getValue()));                            
                            foundOne = true;
                            break;
                        }
                    }
                }
                if (foundOne) {
                    break;
                }
            }
        }
        return impus;
    }

    public Resolution showCheckSimCompliance() {
        checkPermissions(Permissions.SIM_COMPLIANCE_CHECK);
        return getDDForwardResolution("/sim/check_sim_compliance.jsp");
    }
    
    public Resolution performSIMComplianceCheck() {
        checkPermissions(Permissions.SIM_COMPLIANCE_CHECK);
        setSIMVerifyResponse(RestServices.getSimComplianceDetails(getSIMVerifyRequest()));
        return getDDForwardResolution("/sim/check_sim_compliance.jsp");
    }
    
    public Resolution getPdf() {
        log.debug("Inside getPdf() for Control number: "+getSIMVerifyRequest().getStackholderVerifyReq().getControlNumber());
        long controlNumber=getSIMVerifyRequest().getStackholderVerifyReq().getControlNumber();
        String fileLocation = System.getProperty("java.io.tmpdir") + File.separator + "/"+controlNumber+".txt";
        byte[] pdf = null;
        try {
            pdf = Utils.decodeBase64(new String(Files.readAllBytes(Paths.get(fileLocation))));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        StreamingResolution res = new StreamingResolution("application/pdf", new ByteArrayInputStream(pdf));
        res.setFilename(controlNumber+".pdf");
        return res;
    }
    
    public Resolution showImeiStatusCheck() {
        checkPermissions(Permissions.SIM_IMEI_CHECK);
        return getDDForwardResolution("/sim/check_imei_status.jsp");
    }
    
    public Resolution showImeiStatusChange() {
        checkPermissions(Permissions.SIM_IMEI_CHANGE);
        return getDDForwardResolution("/sim/change_imei_status_getinfo.jsp");
    }
    
    public Resolution PerformIMEICheck() {
        checkPermissions(Permissions.SIM_IMEI_CHECK);
        log.debug("inside perform imei check with imei : "+getIMEICheckRequest().getImei());
        setIMEICheckResponse(UCCHelper.checkIMEIStatus(getIMEICheckRequest()));
        return getDDForwardResolution("/sim/check_imei_status.jsp");
    }

    public Resolution GetDetailsForIMEIStatusChange() {
        checkPermissions(Permissions.SIM_IMEI_CHANGE);
        log.debug("inside GetDetailsForIMEIStatusChange with : "+getIMEIStatusChangeGetinfoRequest().getEquipmentType());
        return getDDForwardResolution("/sim/change_imei_status.jsp");
    }
    
    public Resolution PerformIMEIStatusChange() {
        checkPermissions(Permissions.SIM_IMEI_CHANGE);
        log.debug("inside PerformIMEIStatusChange with imei : "+getIMEIStatusChangeRequest().getImei()+ " & Status:"+getIMEIStatusChangeRequest().getStatus()
        +" & EquipmentType:"+getIMEIStatusChangeRequest().getEquipmentType()+ " & Comment:"+getIMEIStatusChangeRequest().getComment()
        +" & Action:"+getIMEIStatusChangeRequest().getAction() +" & imeiend:"+getIMEIStatusChangeRequest().getImeiend());

        boolean flag=UCCHelper.changeIMEIStatus(getIMEIStatusChangeRequest());
        if(flag)
        {
            String msg=getIMEIStatusChangeRequest().getAction().equalsIgnoreCase("CRE")?"imei.blocked.successfully":"imei.unblocked.successfully";
            setPageMessage(msg);
        } else {
            String msg=getIMEIStatusChangeRequest().getAction().equalsIgnoreCase("CRE")?"imei.block.failed":"imei.unblock.failed";
            setPageMessage(msg);
        }        
        return getDDForwardResolution("/sim/change_imei_status.jsp");
    }
    
    private List<String> availableNumbers;

    public List<String> getAvailableNumbers() {
        return availableNumbers;
    }

    public void setAvailableNumbers(List<String> availableNumbers) {
        this.availableNumbers = availableNumbers;
    }
    
    private List<String> ownNumbers;

    public List<String> getOwnNumbers() {
        return ownNumbers;
    }

    public void setOwnNumbers(List<String> ownNumbers) {
        this.ownNumbers = ownNumbers;
    }
    
    private String regulatorResponse;

    public String getRegulatorResponse() {
        return regulatorResponse;
    }

    public void setRegulatorResponse(String regulatorResponse) {
        this.regulatorResponse = regulatorResponse;
    }
}
