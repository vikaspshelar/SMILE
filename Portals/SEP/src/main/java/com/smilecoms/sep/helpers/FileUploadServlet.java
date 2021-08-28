/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.helpers;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Utils;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class FileUploadServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(FileUploadServlet.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        log.debug("In doPost on FileUploadServlet.");

        try {
            response.setContentType("text/plain");
            
            //Check if we are deleting a file, just return;
            String deletePhoto = request.getParameter("deleteFile");
            if (deletePhoto != null && deletePhoto.equalsIgnoreCase("yes") && request.getParameter("currentGuid") != null) {
                log.debug("Deleting tmp file [{}]", request.getParameter("currentGuid"));
                Utils.deleteTempFile(request.getParameter("currentGuid"));
                response.setContentType("text/plain");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("done");
                return;
            }

            List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
            
            
            for (FileItem item : items) {

                log.debug("Item Info. Name [{}] FieldName [{}]", item.getName(), item.getFieldName());

                byte[] fileData;
                String fileName;
                if (item.getFieldName().equals("snap.jpg")) {
                    log.debug("This is a webcam base64 encoded image");
                    String imgBase64 = Utils.parseStreamToString(item.getInputStream(), "utf-8").replace("data:image/jpeg;base64", "");
                    fileData = Utils.decodeBase64(imgBase64);
                    fileName = "snap.jpg";
                } else {
                    if (!item.getFieldName().matches("^file.*")) {
                        continue;
                    }
                    fileName = item.getName().replaceAll("\\.jpeg$", ".jpg").toLowerCase();
                    fileData = Utils.parseStreamToByteArray(item.getInputStream());
                }

                long maxFileSize = BaseUtils.getLongProperty("env.sep.photo.upload.max.bytes", 5 * 1024 * 1024);
                if (fileData.length > maxFileSize) {
                    response.setStatus(500);
                    response.getWriter().write("Error : Max photo file size exceeded. File size is " + fileData.length);
                    return;
                }

                if (fileName.toUpperCase().matches("^FINGERPRINTS.*.DATA$")) {
                    log.debug("This is a fingerprint file");
                    // Check the version number of the fingerprints application that created the fingerprint
                    double version = ByteBuffer.wrap(fileData, 0, 8).getDouble();
                    log.debug("Fingerprint version is [{}]", version);
                    
                    if (!BaseUtils.getPropertyAsList("env.fingerprints.application.version").contains(String.valueOf(version))) { // version != BaseUtils.getDoubleProperty("env.fingerprints.application.version")) {
                        // Exit with error - this fingerprint version is not supported.
                        response.setStatus(500);
                        response.getWriter().write("Error : Fingerprints were created with an unsupported application version[" + version + "], the current supported version is [" + BaseUtils.getDoubleProperty("env.fingerprints.application.version") + "].");
                        return;
                    }
                    
                    if(version == 1.3) {
                        // Can only be used for international customers.
                        String nationality = request.getParameter("nationality");
                        if(nationality == null || nationality.isEmpty()) {
                            response.setStatus(500);
                            response.getWriter().write("Error : Nationality must be supplied when using fingerprint version 1.3.");
                            return;
                        }
                        
                        if(nationality.equalsIgnoreCase(BaseUtils.getProperty("env.locale.country.for.language.en"))) { //Is  a citizen
                            response.setStatus(500);
                            response.getWriter().write("Error : Fingerprints taken from a noncitizen cannot be used on a citizen customer - version " + version);
                            return;
                        }
                    }
                }
                String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
                File tmpFile = Utils.createTempFile(Utils.getUUID() + "." + fileExtension, fileData);
                response.getWriter().write(tmpFile.getName() + "\r\n");
                log.debug("Wrote tmp file to [{}] of size [{}]", tmpFile.getAbsolutePath(), fileData.length);
            }
        } catch (Exception e) {
            log.warn("Error in FileUploadServlet", e);
            response.setStatus(500);
            response.getWriter().write("Error : " + e.toString());
        }
    }
}
