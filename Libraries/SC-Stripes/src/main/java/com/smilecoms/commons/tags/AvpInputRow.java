package com.smilecoms.commons.tags;

import com.smilecoms.commons.IListGenerator;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.util.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.JspTag;
import javax.servlet.jsp.tagext.SimpleTag;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * 
 * 
 * 
 e.g.

 <tr class="odd">
 <td>IMPI</td>
 <td>9807@ims.smilecoms.com</td>
 <td><stripes:text name="xxx" maxlength="25" size="25" value="9807@ims.smilecoms.com"/></td>
 </tr>
 * 
 * 
 */
public class AvpInputRow implements SimpleTag {

    JspTag parent;
    AVP avp;
    JspContext c;
    int avpIndex;
    String namePrefix;
    String value;
    HttpServletRequest request;
    private static final Logger log = LoggerFactory.getLogger(AvpInputRow.class);

    @Override
    public void doTag() throws JspException, IOException {
        if (avp == null) {
            return;
        }
        if (avp.isUserDefined() != null && avp.isUserDefined() && avp.getInputType() != null && !avp.getInputType().isEmpty()) {
            StringBuilder s = new StringBuilder();
            s.append("<tr><td>");
            s.append(getFriendlyName(avp.getAttribute()));
            s.append("</td><td>");

            if (isAVPAllowed()) {
                if (avp.getValidationRule() == null || avp.getValidationRule().isEmpty()) {
                    log.debug("AVP [{}] has no validation rule. Defaulting to 20|.*|none", avp.getAttribute());
                    avp.setValidationRule("text|20|.*|none");
                }
                if (avp.getValidationRule().startsWith("dynamiclist|")) {
                    s.append(getDynamicListField());
                } else if (avp.getValidationRule().startsWith("text|")) {
                    s.append(getTextField());
                } else if (avp.getValidationRule().startsWith("password|")) {
                    s.append(getPasswordField());
                } else if (avp.getValidationRule().startsWith("option|")) {
                    s.append(getOptionField());
                } else if (avp.getValidationRule().startsWith("validatedoption|")) {
                    s.append(getValidatedOptionField());
                } else if (avp.getValidationRule().startsWith("datalist|")) {
                    s.append(getDataListField());
                } else if (avp.getValidationRule().startsWith("photo|")) {
                    s.append(getPhotoField());
                }
            } else {
                s.append(getReadOnlyTextField());
            }

            s.append("</td></tr>");
            s.append(getHiddenNameField());

            c.getOut().write(s.toString());
        }

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

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public void setAvp(AVP avp) {
        this.avp = avp;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public void setAvpIndex(int avpIndex) {
        this.avpIndex = avpIndex;
    }

    private String getSize(String rule) {
        return "36";
    }

    private String getTextField() {
        StringBuilder s = new StringBuilder();
        String id = namePrefix + "_" + avpIndex;
        s.append("<input type=\"text\" name=\"");
        s.append(namePrefix);
        s.append("[");
        s.append(avpIndex);
        s.append("].value\" maxlength=\"");
        s.append(getMaxLength(avp.getValidationRule()));
        s.append("\" size=\"");
        s.append(getSize(avp.getValidationRule()));
        s.append("\" value=\"");
        s.append(value.isEmpty() ? StringEscapeUtils.escapeHtml(avp.getValue()) : value);
        s.append("\" id=\"");
        s.append(id);
        s.append("\" onkeyup=\"");
        String js = getValidationJavascript(id, avp.getValidationRule());
        s.append(js);
        s.append("\"/>");
        return s.toString();
    }

    private String getPasswordField() {
        StringBuilder s = new StringBuilder();
        String id = namePrefix + "_" + avpIndex;
        s.append("<input type=\"password\" name=\"");
        s.append(namePrefix);
        s.append("[");
        s.append(avpIndex);
        s.append("].value\" maxlength=\"");
        s.append(getMaxLength(avp.getValidationRule()));
        s.append("\" size=\"");
        s.append(getSize(avp.getValidationRule()));
        s.append("\" value=\"");
        s.append(value.isEmpty() ? StringEscapeUtils.escapeHtml(avp.getValue()) : value);
        s.append("\" id=\"");
        s.append(id);
        s.append("\" onkeyup=\"");
        String js = getValidationJavascript(id, avp.getValidationRule());
        s.append(js);
        s.append("\"/>");
        return s.toString();
    }

    private String getReadOnlyTextField() {
        StringBuilder s = new StringBuilder();
        String id = namePrefix + "_" + avpIndex;

        s.append("<input type=\"text\" readonly=\"true\" name=\"");
        s.append(namePrefix);
        s.append("[");
        s.append(avpIndex);
        s.append("].value\" size=\"");
        s.append(getSize(avp.getValidationRule()));
        s.append("\" value=\"");
        s.append(value.isEmpty() ? avp.getValue() : value);
        s.append("\" id=\"");
        s.append(id);
        s.append("\"/>");
        return s.toString();
    }

    private String getOptionField() {
        StringBuilder s = new StringBuilder();
        s.append("<select name=\"");
        s.append(namePrefix);
        s.append("[");
        s.append(avpIndex);
        s.append("].value\">");
        String[] options = avp.getValidationRule().split("\\|");
        boolean firstOne = true;
        for (String option : options) {
            if (firstOne) {
                firstOne = false;
                continue;
            }
            if (option.equals(value.isEmpty() ? avp.getValue() : value)) {
                s.append("<option selected>");
            } else {
                s.append("<option>");
            }
            s.append(option);
            s.append("</option>");
        }
        s.append("</select>");

        return s.toString();
    }

    private String getValidatedOptionField() {
        StringBuilder s = new StringBuilder();
        String[] options = avp.getValidationRule().split("\\|");
        s.append("<select name=\"");
        s.append(namePrefix);
        s.append("[");
        s.append(avpIndex);
        s.append("].value\"");
        s.append(" onchange=\"validate(this,'");
        s.append(options[1]);
        s.append("','none')\">");
        boolean firstOne = true;
        boolean secondOne = true;
        for (String option : options) {
            if (firstOne) {
                firstOne = false;
                continue;
            }
            if (secondOne) {
                secondOne = false;
                continue;
            }
            if (option.equals(value.isEmpty() ? avp.getValue() : value)) {
                s.append("<option selected>");
            } else {
                s.append("<option>");
            }
            s.append(option);
            s.append("</option>");
        }
        s.append("</select>");

        return s.toString();
    }

    private String getHiddenNameField() {
        StringBuilder s = new StringBuilder();
        s.append("<input type=\"hidden\" name=\"");
        s.append(namePrefix);
        s.append("[");
        s.append(avpIndex);
        s.append("].attribute\" value=\"");
        s.append(avp.getAttribute());
        s.append("\"/>");
        return s.toString();
    }

    private String getValidationJavascript(String id, String rule) {
        StringBuilder s = new StringBuilder();
        s.append("if(skipValidate != true){validate(document.getElementById('");
        s.append(id);
        s.append("'),'");
        s.append(getRegex(rule));
        s.append("','");
        s.append(getOtherRules(rule));
        s.append("');}");
        return s.toString();
    }

    private String getRegex(String rule) {
        if (rule.startsWith("datalist|")) {
            return rule.split("\\|")[3];
        } else {
            return rule.split("\\|")[2];
        }
    }

    private String getOtherRules(String rule) {
        if (rule.startsWith("datalist|")) {
            return rule.split("\\|")[4];
        } else {
            return rule.split("\\|")[3];
        }
    }

    private String getMaxLength(String rule) {
        if (rule.startsWith("datalist|")) {
            return rule.split("\\|")[2];
        } else {
            return rule.split("\\|")[1];
        }
    }

    public void setValue(String value) {
        this.value = value;
    }

    private List<String> getPossibleOptionsFromListGenerator(String className) {
        List ret;
        try {
            ClassLoader myCL = this.getClass().getClassLoader();
            Class listGeneratorClass = myCL.loadClass(className);
            IListGenerator generator = (IListGenerator) listGeneratorClass.newInstance();
            ret = generator.getList();
        } catch (Exception e) {
            log.error("Error using list generator:", e);
            return new ArrayList();
        }
        return ret;
    }

    private String getDynamicListField() {
        StringBuilder s = new StringBuilder();
        s.append("<select name=\"");
        s.append(namePrefix);
        s.append("[");
        s.append(avpIndex);
        s.append("].value\">");
        List<String> options = getPossibleOptionsFromListGenerator(avp.getValidationRule().split("\\|")[1]);
        for (String option : options) {
            if (option.equals(value.isEmpty() ? avp.getValue() : value)) {
                s.append("<option selected>");
            } else {
                s.append("<option>");
            }
            s.append(option);
            s.append("</option>");
        }
        s.append("</select>");

        return s.toString();
    }

    private String getDataListField() {
        StringBuilder s = new StringBuilder();
        String listName = Utils.getUUID();
        String id = namePrefix + "_" + avpIndex;
        s.append("<input type=\"text\" name=\"");
        s.append(namePrefix);
        s.append("[");
        s.append(avpIndex);
        s.append("].value\" list=\"");
        s.append(listName);
        s.append("\" value=\"");
        s.append(value.isEmpty() ? avp.getValue() : value);
        s.append("\" id=\"");
        s.append(id);
        s.append("\" onkeyup=\"");
        String js = getValidationJavascript(id, avp.getValidationRule());
        s.append(js);
        s.append("\"/>");
        List<String> options = getPossibleOptionsFromListGenerator(avp.getValidationRule().split("\\|")[1]);
        s.append("<datalist id=\"");
        s.append(listName);
        s.append("\">");
        for (String option : options) {
            s.append("<option value=\"");
            s.append(option);
            s.append("\"/>");
        }
        s.append("</datalist>");
        return s.toString();
    }

    private boolean isAVPAllowed() {
        if (!BaseUtils.getBooleanProperty("env.service.role.based.avps.enabled", false)) {
            return true;
        }
        List<String> roles = Utils.getListFromCRDelimitedString(avp.getProvisionRoles());
        for (String friendlyRoleName : roles) {
            if (request.isUserInRole(Utils.getUnfriendlyRoleName(friendlyRoleName))) {
                return true;
            }
        }
        return false;
    }

    private String getPhotoField() {

        String avpName = namePrefix + "[" + avpIndex + "].value";
        StringBuilder s = new StringBuilder();
        String guid = value.isEmpty() ? StringEscapeUtils.escapeHtml(avp.getValue()) : value;

        s.append("<table class=\"clear\"><tr>");

        if (!avp.getAttribute().equalsIgnoreCase("Fingerprints")) {
            s.append("<td><video id=\"video\" class=\"video\" width=\"256\" height=\"192\" autoplay></video><canvas id=\"canvas\" width=\"1024\" height=\"768\" style=\"display:none\"></canvas></td>");
        }

        s.append("<td align=\"center\"><div align=\"center\" id=\"imgDiv").append(avpIndex).append("\"><img class=\"thumb\" src=\"");

        if (guid.isEmpty()) {
            s.append("images/upload.png");
        } else {
            if (avp.getAttribute().equalsIgnoreCase("Fingerprints")) {
                s.append("images/dummy-fingerprint.jpg");
            } else {
                s.append(request.getContextPath());
                s.append("/GetImageDataServlet;jsessionid=");
                s.append(request.getRequestedSessionId());
                s.append("?photoGuid=");
                s.append(guid);
            }
        }

        s.append("\" id=\"imgFile").append(avpIndex).append("\"/></div>");
        s.append("<input type=\"file\" class=\"file\" id=\"file").append(avpIndex).append("\" name=\"file").append(avpIndex).append("\" style=\"visibility: hidden;\"");
        s.append(" onkeyup=\"");
        String js = getPhotoValidationJavascript("file" + avpIndex, "photoGuid" + avpIndex, avp.getValidationRule());
        s.append(js);
        s.append("\"/>");
        s.append("<input type=\"button\" class=\"file\" value=\"Browse ...\" onclick=\"javascript:document.getElementById('file").append(avpIndex).append("').click();\"/>");
        if (!avp.getAttribute().equalsIgnoreCase("Fingerprints")) { // No Webcams for Fingerprints...
            s.append("<input value=\"WebCam\" class=\"file\" id=\"snap\" type=\"button\" onclick=\"snapAndUpload('file").append(avpIndex).append("');\"/>");
        }
        s.append("<input type=\"hidden\"  id=\"photoGuid").append(avpIndex).append("\" name=\"").append(avpName).append("\" value=\"").append(guid).append("\"/>");
        s.append("<script type=\"text/javascript\">document.getElementById('file").append(avpIndex).append("').addEventListener('change', uploadDocument, false);</script></td></tr></table>");

        return s.toString();
    }

    private String getPhotoValidationJavascript(String fileId, String guidId, String rule) {

        if (rule.contains("|optional")) {
            return "";
        }
        StringBuilder s = new StringBuilder();
        s.append("if(skipValidate != true){var photoGuid = document.getElementById('");
        s.append(guidId);
        s.append("').value; console.log('Photoguid is ' + photoGuid); ");
        s.append("var file = document.getElementById('");
        s.append(fileId);
        s.append("'); ");
        s.append("if (photoGuid == '') { file.className='validationNotOk'; console.log('Validation not OK')} else {file.className='validationOk'; console.log('Validation OK')}}");
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
