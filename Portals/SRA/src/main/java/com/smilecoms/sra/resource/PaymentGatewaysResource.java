/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.beans.SaleBean;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.sra.helpers.RequestParser;
import com.smilecoms.sra.helpers.SRAException;
import com.smilecoms.sra.helpers.paymentgateway.ICellulantPaymentNotificationData;
import com.smilecoms.sra.helpers.paymentgateway.IPesapalPaymentNotificationData;
import com.smilecoms.sra.helpers.paymentgateway.PaymentNotificationData;
import com.smilecoms.sra.helpers.paymentgateway.PaymentNotifactionFactory;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
@Path("paymentnotification")
public class PaymentGatewaysResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewaysResource.class);
    @Context
    private javax.servlet.http.HttpServletRequest request;

    @Path("processpayment")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public <T> T processPaymentNotification(@Context UriInfo uriInfo, PaymentNotificationData rcd) {
        start(request);
        T pn;
        try {
            checkPermissions(Permissions.MAKE_SALE);

            RequestParser parser = new RequestParser(uriInfo);
            int saleId = parser.getParamAsInt("orderId");

            log.debug("The SaleID is {}", saleId);

            if (saleId == 0) {
                log.warn("SaleID is 0");
                throw new SRAException(Response.Status.UNAUTHORIZED);
            }

            log.debug("This is a payment notification request for sale id [{}]", saleId);
            PaymentGatewayResource pgr = PaymentGatewayResource.getPaymentGatewayResourceBySaleId(saleId);
            SaleBean sale = pgr.getSale();

            if (sale.getPaymentGatewayCode() == null || sale.getPaymentGatewayCode().isEmpty()) {
                log.debug("Payment gateway is unknown");
                throw new SRAException(Response.Status.UNAUTHORIZED);
            }
            log.debug("Gateway code is {}", sale.getPaymentGatewayCode());

            log.debug("Calling SCA to process payment notification");
            SCAWrapper.getAdminInstance().processPaymentNotification(getPaymentNotificationData(sale, rcd));

            log.debug("SCA process payment notification comple, going to gateway response");

            pn = PaymentNotifactionFactory.createPaymentNotificationResponse(getPaymentNotificationData(sale, rcd));

            log.debug("Finished processing gateway response");

        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
        return pn;
    }

    @Path("processpaymentV2")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public <T> T processPaymentNotificationV2(@Context UriInfo uriInfo, String json) {
        start(request);
        T pn = null;
        try {
            checkPermissions(Permissions.MAKE_SALE);
            log.debug("Raw JSON String is: {}", json);
            com.smilecoms.commons.sca.PaymentNotificationData cr = PaymentNotifactionFactory.getPaymentNotificationData(json, request);
            log.debug("Calling SCA to process payment notification for {} with sale id {}", cr.getPaymentGatewayCode(), cr.getSaleId());
            SCAWrapper.getAdminInstance().processPaymentNotification(cr);
            log.debug("Creating response to return back to caller");
            pn = PaymentNotifactionFactory.createPaymentNotificationResponse(cr);

        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
        return pn;
    }

    @Deprecated
    private com.smilecoms.commons.sca.PaymentNotificationData getPaymentNotificationData(SaleBean sale, PaymentNotificationData rcd) {
        throw new UnsupportedOperationException("This option has been deprecated");
    }

}
