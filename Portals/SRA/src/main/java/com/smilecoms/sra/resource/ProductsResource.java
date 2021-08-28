/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.sca.beans.ProductBean;
import com.smilecoms.commons.sca.helpers.Permissions;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author lesiba
 */
@Path("products")
public class ProductsResource extends Resource {

    @Context
    private javax.servlet.http.HttpServletRequest request;

    /*@GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{productInstanceId}")
    public ProductResource getProductInstance(@PathParam("productInstanceId") int productInstanceId) {
        start(request);
        try {
            return new ProductResource(productInstanceId);
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }*/

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{productInstanceId}")
    public ProductResource getProductInstance(@PathParam("productInstanceId") int productInstanceId, @QueryParam("verbosity") String verbosityString) {
        start(request);
        try {
            StProductInstanceLookupVerbosity verbosity;
            if (verbosityString != null) {
                verbosity = StProductInstanceLookupVerbosity.fromValue(verbosityString);
            } else {
                verbosity = StProductInstanceLookupVerbosity.MAIN;
            }
            return new ProductResource(productInstanceId, verbosity);
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }

    @PUT
    @Path("{productInstanceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProductResource modifyProductInstance(@PathParam("productInstanceId") int productInstanceId, ProductResource product) {
        start(request);
        try {
            checkPermissions(Permissions.ADD_PRODUCT_INSTANCE);
            product.getProduct().setProductInstanceId(productInstanceId);
            return new ProductResource(ProductBean.modifyProductInstance(product.getProduct()));
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }
    
}
