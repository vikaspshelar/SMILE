package com.smilecoms.commons.interceptors;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMLDeclarationWritingInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    private static final Logger log = LoggerFactory.getLogger(XMLDeclarationWritingInterceptor.class);
    public XMLDeclarationWritingInterceptor() {
        super(Phase.PRE_STREAM);
        addBefore(StaxOutInterceptor.class.getName());
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        log.debug("Ensuring XML Declaration is added");
        message.put("org.apache.cxf.stax.force-start-document", Boolean.TRUE);
    }
}
