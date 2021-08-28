/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.paymentgateway;

/**
 *
 * @author sabza
 */
public class PaymentGatewayManagerFactory {

    public static PaymentGatewayManager createPaymentGatewayManager(GatewayCodes gc) {
        PaymentGatewayManager pgm;
        switch (gc) {
            case DIAMOND_BANK:
                pgm = new DiamondBankManager();
                break;
            case DUMMY_BANK:
                pgm = new DummyBankManager();
                break;
            case GEMEX:
                pgm = new GemexManager();
                break;
            case PESAPAL:
                pgm = new PesapalManager();
                break;
            case ACCESS_BANK:
                pgm = new AccessBankManager();
                break;
            case WEB_PAY_DIRECT:
                pgm = new WebPAYDirectManager();
                break;
            case YO_PAYMENTS:
                pgm = new YoPaymentsManager();
                break; 
            case PAYSTACK:
                pgm = new PaystackManager();
                break;
            case CELLULANT:
                pgm = new CellulantManager();
                break;
            case SELCOM:
                pgm = new SelcomManager();
                break;
            default:
                throw new NullPointerException("Gateway does not exist [" + gc + "]");
        }
        return pgm;
    }
}
