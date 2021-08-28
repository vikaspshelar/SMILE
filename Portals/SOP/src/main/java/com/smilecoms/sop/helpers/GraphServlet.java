/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sop.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class GraphServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(GraphServlet.class);

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        log.info("Generating RRD Graph");
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        String location = request.getParameter("loc");
        String name = request.getParameter("name");
        String type = request.getParameter("type");
        String width = request.getParameter("width");
        if (width == null) {
            width = "1000";
        }
        String height = request.getParameter("height");
        if (height == null) {
            height = "200";
        }
        String starthoursback = request.getParameter("starthoursback");
        if (starthoursback == null) {
            starthoursback = "25";
        }
        String endhoursback = request.getParameter("endhoursback");
        if (endhoursback == null) {
            endhoursback = "0";
        }
        long start = System.currentTimeMillis();
        byte[] graph = RRDHelper.getGraph(location, name, type, Integer.parseInt(width), Integer.parseInt(height), Integer.parseInt(starthoursback),
                Integer.parseInt(endhoursback));
        long end = System.currentTimeMillis();
        BufferedInputStream input = null;
        BufferedOutputStream output = null;
        response.setContentLength(graph.length);
        try {
            input = new BufferedInputStream(new ByteArrayInputStream(graph));
            output = new BufferedOutputStream(response.getOutputStream());
            byte[] buffer = new byte[8192];
            for (int length; (length = input.read(buffer)) > 0;) {
                output.write(buffer, 0, length);
            }
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException logOrIgnore) {
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException logOrIgnore) {
                }
            }
        }
        log.info("Finished generating RRD Graph in [{}]ms", end - start);
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
