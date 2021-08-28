/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.np.tz;


import com.smilecoms.xml.schema.am.PhoneNumberRange;
import com.smilecoms.xml.schema.am.PortInEvent;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mukosi
 */
public class TZPortingHelper {
    
    public static final String CUSTOMER_TYPE_INDIVIDUAL = "individual";
    public static final String CUSTOMER_TYPE_ORGANISATION = "organisation";
    public static final String TZMNP_ERROR_CODE_1 = "1"; // The MSISDN is not a valid number on the Donor’s network
    public static final String TZMNP_ERROR_CODE_2 = "2"; // The MSISDN and the identifier  do not match (for subscriber)
    public static final String TZMNP_ERROR_CODE_3 = "3"; // The MSISDN and the Corporate Registration Number do not match (for Corporate)
    public static final String TZMNP_ERROR_CODE_4 = "4"; // The MSISDN is already subject to suspension for outgoing and incoming calls, i.e. not active
    public static final String TZMNP_ERROR_CODE_5 = "5"; // Not Applicaple to Smile: The subscriber has outstanding airtime loan
    public static final String TZMNP_ERROR_CODE_6 = "6"; // Not Applicaple to Smile: The subscriber has outstanding mobile money loan
    public static final String TZMNP_ERROR_CODE_7 = "7"; // Not Applicaple to Smile: The subscriber is post-paid and has more than one bill cycle outstanding
    public static final String TZMNP_ERROR_CODE_8 = "8"; // The subscriber is new in the current network
    public static final String TZMNP_ERROR_CODE_10 = "8"; // General error code to be used for any exception while processing a request.
      
    
    public static st.systor.np.commontypes.RoutingInfoList  mapXMLRoutingInformationListToNPCDB(com.smilecoms.xml.schema.am.RoutingInfoList inRoutingInfoList) {
        st.systor.np.commontypes.RoutingInfoList outRoutingInfoList = new st.systor.np.commontypes.RoutingInfoList();
        
        st.systor.np.commontypes.RoutingInfo npcdbRoutingInfo = null;
        st.systor.np.commontypes.PhoneNumberRange npcdbPhoneNumberRange = null;
        
        for(com.smilecoms.xml.schema.am.RoutingInfo inRoutingInfo : inRoutingInfoList.getRoutingInfo()) {
            npcdbRoutingInfo = new st.systor.np.commontypes.RoutingInfo();
            npcdbPhoneNumberRange = new st.systor.np.commontypes.PhoneNumberRange();
            npcdbPhoneNumberRange.setPhoneNumberStart(inRoutingInfo.getPhoneNumberRange().getPhoneNumberStart());
            npcdbPhoneNumberRange.setPhoneNumberEnd(inRoutingInfo.getPhoneNumberRange().getPhoneNumberEnd());
            
            npcdbRoutingInfo.setPhoneNumberRange(npcdbPhoneNumberRange);
            npcdbRoutingInfo.setRoutingNumber(inRoutingInfo.getRoutingNumber());
            
            outRoutingInfoList.getRoutingInfo().add(npcdbRoutingInfo);
        }
        return outRoutingInfoList;
    }
    
    public static boolean isEmergencyRestore(PortInEvent context) {
        return (context.getEmergencyRestoreId() != null && !context.getEmergencyRestoreId().isEmpty());
    }
    
    public static st.systor.np.commontypes.PhoneNumberList  mapXMLRoutingInformationListToNPCDBPhoneNumberList(com.smilecoms.xml.schema.am.RoutingInfoList inRoutingInfoList) {
        st.systor.np.commontypes.PhoneNumberList phoneList = new st.systor.np.commontypes.PhoneNumberList();
        
        st.systor.np.commontypes.PhoneNumberRange npcdbPhoneNumberRange = null;
        
        for(com.smilecoms.xml.schema.am.RoutingInfo inRoutingInfo : inRoutingInfoList.getRoutingInfo()) {
            npcdbPhoneNumberRange = new st.systor.np.commontypes.PhoneNumberRange();
            npcdbPhoneNumberRange.setPhoneNumberStart(inRoutingInfo.getPhoneNumberRange().getPhoneNumberStart());
            npcdbPhoneNumberRange.setPhoneNumberEnd(inRoutingInfo.getPhoneNumberRange().getPhoneNumberEnd());
            
            phoneList.getPhoneNumberRange().add(npcdbPhoneNumberRange);
        }
        return phoneList;
    }
    
    public static List <PhoneNumberRange>  mapRoutingInformationListToAMPhoneNumberList(com.smilecoms.xml.schema.am.RoutingInfoList inRoutingInfoList) {
        
        List <PhoneNumberRange> phoneNumberRangeList = new ArrayList();
        
        PhoneNumberRange currentNumberRangeEntry = null;
                
        for(com.smilecoms.xml.schema.am.RoutingInfo inRoutingInfo : inRoutingInfoList.getRoutingInfo()) {
            currentNumberRangeEntry = new PhoneNumberRange();
            currentNumberRangeEntry.setPhoneNumberStart(inRoutingInfo.getPhoneNumberRange().getPhoneNumberStart());
            currentNumberRangeEntry.setPhoneNumberEnd(inRoutingInfo.getPhoneNumberRange().getPhoneNumberEnd());
            
            phoneNumberRangeList.add(currentNumberRangeEntry);
        }
        return phoneNumberRangeList;
    }
    
}
