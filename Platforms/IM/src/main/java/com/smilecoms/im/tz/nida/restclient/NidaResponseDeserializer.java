/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.tz.nida.restclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

/**
 *
 * @author mukosi
 */
public class NidaResponseDeserializer extends StdDeserializer<NidaResponse> {
    
    public NidaResponseDeserializer() {
        this(null);
    }
 
    public NidaResponseDeserializer(Class<?> vc) {
        super(vc);
    }
    
    @Override
    public NidaResponse deserialize(com.fasterxml.jackson.core.JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
      
        JsonNode responseNode = jp.getCodec().readTree(jp);
        NidaResponse nidaResponse = new NidaResponse();
                
        nidaResponse.setCode(responseNode.get("code").textValue());
        nidaResponse.setTransactionId(responseNode.get("id").textValue());
             
        if(nidaResponse.getCode() != null && nidaResponse.getCode().equalsIgnoreCase("00")) { //All is successfull here
            nidaResponse.setFirstName(responseNode.get("profile").get("firstName").textValue());
            nidaResponse.setMiddleName(responseNode.get("profile").get("middleName").textValue());
            nidaResponse.setLastName(responseNode.get("profile").get("lastName").textValue());
            nidaResponse.setOtherNames(responseNode.get("profile").get("otherNames").textValue());
            nidaResponse.setGender(responseNode.get("profile").get("sex").textValue());
            nidaResponse.setDateOfBirth(responseNode.get("profile").get("dateOfBirth").textValue());
            nidaResponse.setPlaceOfBirth(responseNode.get("profile").get("placeOfBirth").textValue());
            nidaResponse.setResidentRegion(responseNode.get("profile").get("residentRegion").textValue());
            nidaResponse.setResidentDistrict(responseNode.get("profile").get("residentDistrict").textValue());
            nidaResponse.setResidentWard(responseNode.get("profile").get("residentWard").textValue());
            nidaResponse.setResidentVillage(responseNode.get("profile").get("residentVillage").textValue());
            nidaResponse.setResidentStreet(responseNode.get("profile").get("residentStreet").textValue());
            nidaResponse.setResidentHouseNo(responseNode.get("profile").get("residentHouseNo").textValue());
            nidaResponse.setResidentPostalAddress(responseNode.get("profile").get("residentPostalAddress").textValue());
            nidaResponse.setResidentPostCode(responseNode.get("profile").get("residentPostCode").textValue());
            nidaResponse.setBirthCountry(responseNode.get("profile").get("birthCountry").textValue());
            nidaResponse.setBirthRegion(responseNode.get("profile").get("birthRegion").textValue());
            nidaResponse.setBirthDistrict(responseNode.get("profile").get("birthDistrict").textValue());
            nidaResponse.setBirthWard(responseNode.get("profile").get("birthWard").textValue());
            nidaResponse.setBirthCertificateNo(responseNode.get("profile").get("birthCertificateNo").textValue());
            nidaResponse.setNationality(responseNode.get("profile").get("nationality").textValue());
            nidaResponse.setPhoneNumber(responseNode.get("profile").get("phoneNumber").textValue());
            nidaResponse.setPhoto(responseNode.get("profile").get("photo").textValue());
            nidaResponse.setSignature(responseNode.get("profile").get("signature").textValue());
            
        } else  {
            // nidaResponse.setStatus(responseNode.get("Status").textValue());
        }
        
        return nidaResponse;
    }
        
}
