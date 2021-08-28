package com.smilecoms.commons.tags;

import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.Photograph;
import com.smilecoms.commons.sca.SCAString;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.Utils;
import java.io.IOException;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.JspTag;
import javax.servlet.jsp.tagext.SimpleTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvpDisplayRow implements SimpleTag {

    private static final Logger log = LoggerFactory.getLogger(AvpDisplayRow.class);
    JspTag parent;
    AVP avp;
    JspContext c;

    @Override
    public void doTag() throws JspException, IOException {
        if (avp == null) {
            return;
        }
        StringBuilder s = new StringBuilder();

        s.append("<tr><td>");
        String val = avp.getValue();
        if (val == null) {
            val = "<b>Attribute Value Is Missing!!!</b>";
        }
        s.append(getFriendlyName(avp.getAttribute()));
        s.append("</td><td>");
        if (avp.getInputType() != null && avp.getInputType().equals("photo")) {
            s.append(getPhoto());
        } else {
            s.append(val);
        }
        s.append("</td></tr>");

        c.getOut().write(s.toString());
    }

    @Override
    public void setParent(JspTag parent) {
        this.parent = parent;
    }

    @Override
    public void setJspContext(JspContext pc) {
        c = pc;
    }

    @Override
    public void setJspBody(JspFragment jspBody) {
    }

    @Override
    public JspTag getParent() {
        return parent;
    }

    public void setAvp(AVP avp) {
        this.avp = avp;
    }

    private String getPhoto() {
        if (avp.getValue().isEmpty()) {
            return "<b>No " + avp.getAttribute() + " Found</b>";
        }
        SCAString photo = new SCAString();
        photo.setString(avp.getValue());
        try {
            Photograph p = SCAWrapper.getUserSpecificInstance().getPhoto(photo);
            Utils.createTempFile(p.getPhotoGuid(), Utils.decodeBase64(p.getData()));
        } catch (Exception e) {
            log.debug("Couldnt get image from SCA. Probably exists locally", e.toString());
        }
        StringBuilder s = new StringBuilder();
        if(avp.getAttribute().equalsIgnoreCase("Fingerprints")) {
            s.append("<a href=\"/sep/images/dummy-fingerprint.jpg\" target=\"_blank\">");
            s.append("<img id=\"imgfile${loop.index}\" class=\"thumb\" src=\"/sep/images/dummy-fingerprint.jpg\"/>");
            s.append("</a>");
        } else {
            s.append("<a href=\"/sep/GetImageDataServlet?photoGuid=");
            s.append(avp.getValue());
            s.append("\" target=\"_blank\">");
            s.append("<img class=\"thumb\" src=\"/sep/GetImageDataServlet?photoGuid=");
            s.append(avp.getValue());
            s.append("\"/></a>");
        }
        return s.toString();
    }


    private Object getFriendlyName(String attribute) {
        String localised = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "avp." + attribute);
        if (localised.startsWith("?")) {
            return attribute;
        }
        return localised;
    }
}
