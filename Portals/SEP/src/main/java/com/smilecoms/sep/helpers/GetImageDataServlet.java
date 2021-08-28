/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.helpers;

import com.smilecoms.commons.util.Utils;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class GetImageDataServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(GetImageDataServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        try {
            String photoGuid = request.getParameter("photoGuid");
            log.debug("In doGet on GetImageDataServlet. Photo guid to retrieve [{}]", photoGuid);
            int lastdot = photoGuid.lastIndexOf(".");
            String fileExtension = photoGuid.substring(lastdot + 1);

            if (fileExtension.equalsIgnoreCase("pdf")) {  // To cater for contract document attachments.
                response.setContentType("application/pdf");
            } else if(fileExtension.equalsIgnoreCase("xlsx") || fileExtension.equalsIgnoreCase("xls")){
                response.setContentType("application/vnd.ms-excel");
            } else {
                response.setContentType("image/" + fileExtension);
            }

            response.getOutputStream().write(Utils.getDataFromTempFile(photoGuid));
        } catch (Exception e) {
            try {
                log.warn("Error in GetImageDataServlet:", e);
                response.setContentType("image/png");
                response.getOutputStream().write(Utils.parseStreamToByteArray(getServletContext().getResourceAsStream("/images/error.png")));
            } catch (Exception ex) {
                log.warn("Error in GetImageDataServlet Exception handler:", ex);
            }
        }
    }
}
