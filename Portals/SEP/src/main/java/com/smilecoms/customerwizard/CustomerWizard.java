/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.customerwizard;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Javassist;
import com.smilecoms.commons.util.Utils;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author paul
 */
public class CustomerWizard {

    private Step firstStep;
    private long lastPopulated = 0;
    private static final Logger log = LoggerFactory.getLogger(CustomerWizard.class);
    private static CustomerWizard me = new CustomerWizard();
    // A question 
    private static final String QUESTION = "help";
    // An option to a question
    private static final String OPTION = "list";
    // A link to another step
    private static final String STEP_LINK = "link";
    // A trouble ticket 
    private static final String TICKET = "flag"; // red
    // User input field for a trouble ticket
    private static final String TICKET_USER_FIELD = "info";
    // Fixed value for a trouble ticket field
    private static final String TICKET_FIXED_FIELD = "wizard";
    // Explanation that can be displayed directly
    private static final String EXPLANATION = "flag-blue";
    // Drop down list for a user defined ticket field
    private static final String DROPDOWN = "password";
    // Link to an extranl web page for more info
    private static final String WEB_LINK = "weblink";
    // a "floating" TT Fixed field that can be a child of any option and floats down to the leaf
    private static final String FF_TT_FIXED_FIELD = "freemind_butterfly";
    // Unknown
    private static final String UNKNOWN = "unknown";

    private CustomerWizard() {
        log.debug("In constuctor for CustomerWizard");
        try {
            populate();
        } catch (Exception e) {
            log.warn("Error: ", e);
            log.warn("Exception was thrown when initialising static object, ignore error and initialise static object: {}", e.toString());
            //throw new RuntimeException(e);
        }
    }

    public static CustomerWizard getInstance() {
        if (System.currentTimeMillis() > me.lastPopulated + BaseUtils.getIntProperty("env.troubleticket.customerwizard.refresh.interval", 300000)) {
            me.populate();
        }
        return me;
    }

    public Step getFirstStep() {
        return firstStep;
    }

    private void populate() {
        try {
            String wizardXML = BaseUtils.getProperty("env.troubleticket.wizardxml");
            wizardXML = wizardXML.trim();
            log.debug("wizardXML is [{}]", wizardXML);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new StringReader(wizardXML)));
            doc.getDocumentElement().normalize();
            log.debug("Root element :" + doc.getDocumentElement().getNodeName());
            Element map = doc.getDocumentElement();
            if (!map.getNodeName().equals("map")) {
                throw new Exception("Invalid mindmap. Root element must be of type <map>");
            }
            Node firstNode = map.getElementsByTagName("node").item(0);
            String nodeType = getNodeType((Element) firstNode);
            if (!nodeType.equals(QUESTION)) {
                throw new Exception("The mind map must have a question as its root node");
            }
            firstStepId = ((Element) firstNode).getAttribute("ID");
            firstStep = makeStepFromQuestionElement((Element) firstNode);
            lastPopulated = System.currentTimeMillis();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private String firstStepId;

    /**
     *
     * FUNCTIONS TO MAKE OUR OWN STEP OBJECTS FROM DOM ELEMENTS
     *
     */
    private Step makeStepFromQuestionElement(Element questionElement) {
        //log.debug("In makeStepFromQuestionElement for element:");
        logNode(questionElement, "makeStepFromQuestionElement");
        Question question = makeQuestion(questionElement);

        Element grandparent = getGrandParentNode(questionElement);
        String grandparentId = null;
        if (grandparent != null) {
            grandparentId = grandparent.getAttribute("ID");
        }

        Step step = new Step(question, question.getQuestionId(), grandparentId);
        List<Element> optionNodes = getNodesChildrenElementsThatAreOfTypeNode(questionElement);
        for (Element optionNode : optionNodes) {
            String childType = getNodeType(optionNode);
            if (!childType.equals(OPTION)) {
                log.warn("Child Node of type [{}]:", childType);
                logNode(optionNode, "");
                throw new RuntimeException("Question elements must only have sub elements that are options/list items (" + step.getQuestion().getQuestionText() + ")");
            }
            logNode(optionNode, "Adding Option");
            if (optionNode.getAttribute("TEXT").startsWith("com.smilecoms.customerwizard")) {
                log.debug("this is a javaassist option");
                String javaAssistCode = BaseUtils.getProperty("env.sep.customerwizard." + optionNode.getAttribute("TEXT"));
                if (javaAssistCode.isEmpty()) {
                    log.warn("Child Node of type [{}]:", childType);
                    logNode(optionNode, "");
                    throw new RuntimeException("jaaassist option with no property for the javaassist code");
                }
                try {
                    log.debug("Calling runtime compiled code to get the options [{}]", javaAssistCode);
                    String options = (String) Javassist.runCode(new Class[]{}, javaAssistCode, new Object[]{});
                    log.debug("Options are [{}]", options);
                    String[] optionList = options.split("\\|");
                    for (String option : optionList) {
                        optionNode.setAttribute("TEXT", option);
                        optionNode.setAttribute("ID", "fakedID_" + Utils.getUUID());
                        step.addOption(makeOption(optionNode));
                    }
                } catch (Throwable e) {
                    log.warn("Error getting info", e);
                }
            } else {
                log.debug("This is a normal option");
                step.addOption(makeOption(optionNode));
            }
        }
        return step;
    }

    private String getOptionsNextId(Element optionElement) {

        List<Element> x = getNodesChildrenElementsThatAreOfTypeNode(optionElement);
        if (x.isEmpty()) {
            // GO back around in a loop!
            return firstStepId;
        }

        for (Element child : x) {
            String childType = getNodeType(child);
            String nextId = child.getAttribute("ID");
            if (childType.equals(QUESTION)) {
                makeStepFromQuestionElement(child);
                return nextId;
            } else if (childType.equals(STEP_LINK)) {
                // The next step is somewhere else. The LINK is the next ID
                nextId = child.getAttribute("LINK").substring(1);
                return nextId;
            } else if (childType.equals(TICKET)) {
                makeTTLeaf(child);
                return nextId;
            } else if (childType.equals(EXPLANATION)) {
                makeExplanationLeaf(child);
                return nextId;
            } else if (childType.equals(WEB_LINK)) {
                makeWebLinkLeaf(child);
                return nextId;
            }
        }

        log.warn("Have an option without a next step.....");
        return "";  //problem if we get here
    }

    /**
     *
     * FUNCTIONS TO MAKE OUR OWN QUESTION/OPTION OBJECTS
     *
     */
    private Question makeQuestion(Element questionElement) {
        String ID = questionElement.getAttribute("ID");
        String TEXT = questionElement.getAttribute("TEXT");
        Question question = new Question(TEXT, ID);
        return question;
    }

    private Option makeOption(Element optionElement) {
        String ID = optionElement.getAttribute("ID");
        String TEXT = optionElement.getAttribute("TEXT");
        Element parent = getParentNode(optionElement);
        String parentID = parent.getAttribute("ID");
        Option option = new Option(TEXT, getOptionsNextId(optionElement), ID, parentID);

        NodeList attributeList = optionElement.getElementsByTagName("attribute");
        
        if (attributeList != null && attributeList.getLength() > 0) {
            log.debug("Option node has atrributes");

            for (int i = 0; i < attributeList.getLength(); i++) {
                String attributeName = attributeList.item(i).getAttributes().getNamedItem("NAME").getNodeValue();
                String attributeValue = attributeList.item(i).getAttributes().getNamedItem("VALUE").getNodeValue();
                log.debug("attribute [{}] with value [{}]", new Object[]{attributeName, attributeValue});
                
                if (!attributeName.equalsIgnoreCase("ValueToFieldName")) {
                    log.warn("unknown option attribute [{}]. Ignoring it", attributeName);
                } else {
                    option.addFastFowardTTFixedField(attributeValue + ":" + TEXT);
                }
            }
        }
        //check for fast forward fixed TT children
        log.debug("looking for fast forward fields");
        List<Element> childNodes = getNodesChildrenElementsThatAreOfTypeNode(optionElement);
        for (Element e : childNodes) {
            TEXT = e.getAttribute("TEXT");
            String TYPE = getNodeType(e);
            if (TYPE.equals(FF_TT_FIXED_FIELD)) {
                log.debug("Found fast forward field");
                option.addFastFowardTTFixedField(TEXT);
            }
        }
        return option;
    }

    /**
     *
     * FUNCTIONS TO MAKE OUR OWN LEAF OBJECTS FROM DOM ELEMENTS
     *
     */
    private Leaf makeTTLeaf(Element leafElement) {
        if (leafElement == null) {
            return null;
        }
        String ID = leafElement.getAttribute("ID");
        String name = leafElement.getAttribute("TEXT");
        Element grandparent = getGrandParentNode(leafElement);
        String grandparentId = null;
        if (grandparent != null) {
            grandparentId = grandparent.getAttribute("ID");
        }
        Leaf leaf = new Leaf(Leaf.LEAF_TYPE.TT, ID, grandparentId, name, null);
        List<Element> leafDataElements = getNodesChildrenElementsThatAreOfTypeNode(leafElement);
        for (Element lde : leafDataElements) {
            String TEXT = lde.getAttribute("TEXT");
            String TYPE = getNodeType(lde);
            if (TYPE.equals(TICKET_USER_FIELD)) {
                leaf.addLeafDataItem(makeTTUserFieldLeafDataItem(TEXT));
            } else if (TYPE.equals(TICKET_FIXED_FIELD)) {
                leaf.addLeafDataItem(makeTTFixedFieldLeafDataItem(TEXT));
            } else if (TYPE.equals(EXPLANATION)) {
                leaf.addLeafDataItem(makeTTExplanationLeafDataItem(TEXT));
            } else if (TYPE.equals(DROPDOWN)) {
                leaf.addLeafDataItem(makeTTDropdownLeafDataItem(TEXT));
            }
        }
        return leaf;
    }

    private Leaf makeExplanationLeaf(Element leafElement) {
        String ID = leafElement.getAttribute("ID");
        String name = leafElement.getAttribute("TEXT");
        Element grandparent = getGrandParentNode(leafElement);
        String grandparentId = null;
        if (grandparent != null) {
            grandparentId = grandparent.getAttribute("ID");
        }
        Leaf leaf = new Leaf(Leaf.LEAF_TYPE.EXPLANATION, ID, grandparentId, name, makeTTLeaf(getChildNode(leafElement)));
        return leaf;
    }

    private Leaf makeWebLinkLeaf(Element leafElement) {
        String ID = leafElement.getAttribute("ID");
        String name = leafElement.getAttribute("TEXT");
        Element grandparent = getGrandParentNode(leafElement);
        String grandparentId = null;
        if (grandparent != null) {
            grandparentId = grandparent.getAttribute("ID");
        }
        Leaf leaf = new Leaf(Leaf.LEAF_TYPE.EXTERNAL_RESOURCE, ID, grandparentId, name, makeTTLeaf(getChildNode(leafElement)));
        String link = leafElement.getAttribute("LINK");
        leaf.addLeafDataItem(makeWebLinkLeafDataItem(link));
        return leaf;
    }

    /**
     *
     * FUNCTIONS TO MAKE OUR OWN LEAF DATA ITEM OBJECTS FROM VALUES OBTAINED
     * FROM AN ELEMENT
     *
     */
    private LeafDataItem makeTTFixedFieldLeafDataItem(String text) {
        try {
            String[] arr = text.split(":");
            LeafDataItem ldi = new LeafDataItem(LeafDataItem.DATA_ITEM_TYPE.TT_FIXED_FIELD, arr[0].trim(), arr[1].trim());
            return ldi;
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("There is a poblem in the mindmap trying to create a Fixed TT field for [{}]", text);
            throw new RuntimeException("There is a poblem in the mindmap trying to create a Fixed TT field for " + text);
        }
    }

    private LeafDataItem makeTTUserFieldLeafDataItem(String text) {
        String[] arr = text.split(":");
        LeafDataItem ldi = new LeafDataItem(LeafDataItem.DATA_ITEM_TYPE.TT_USER_FIELD, arr[0].trim(), arr[1].trim());
        return ldi;
    }

    private LeafDataItem makeTTDropdownLeafDataItem(String text) {
        String[] arr = text.split(":");
        LeafDataItem ldi = new LeafDataItem(LeafDataItem.DATA_ITEM_TYPE.TT_DROPDOWN_FIELD, arr[0].trim(), arr[1].trim());
        return ldi;
    }

    private LeafDataItem makeWebLinkLeafDataItem(String text) {
        LeafDataItem ldi = new LeafDataItem(LeafDataItem.DATA_ITEM_TYPE.WEB_LINK, "url", text);
        return ldi;
    }

    private LeafDataItem makeTTExplanationLeafDataItem(String text) {
        LeafDataItem ldi = new LeafDataItem(LeafDataItem.DATA_ITEM_TYPE.TT_EXPLANATION, "explanation", text);
        return ldi;
    }

    /**
     *
     *
     * DOM HELPERS
     *
     *
     *
     */
    private List<Element> getNodesChildrenElementsThatAreOfTypeNode(Node node) {
        List<Element> ret = new ArrayList<Element>();
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            throw new RuntimeException("Cannot get sub nodes of a node that is not an element");
        }
        Element e = (Element) node;
        NodeList nodes = e.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE && nodes.item(i).getNodeName().equals("node")) {
                ret.add((Element) nodes.item(i));
            }
        }
        return ret;
    }

    private Element getChildNode(Element node) {
        List<Element> children = getNodesChildrenElementsThatAreOfTypeNode(node);
        if (children.isEmpty()) {
            return null;
        }
        return children.get(0);
    }

    private Element getParentNode(Element node) {
        if (node == null) {
            return null;
        }
        try {
            return (Element) node.getParentNode();
        } catch (Exception e) {
            return null;
        }
    }

    private Element getGrandParentNode(Element node) {
        return getParentNode(getParentNode(node));
    }

    private String getNodeType(Element node) {
        if (node.hasAttribute("LINK") && node.getAttribute("LINK").startsWith("#")) {
            return STEP_LINK;
        }
        if (node.hasAttribute("LINK") && !node.getAttribute("LINK").startsWith("#")) {
            return WEB_LINK;
        }
        Element iconElement = getNodesIconElement(node);
        if (iconElement == null) {
            return UNKNOWN;
        }
        return getIconType(iconElement);
    }

    private String getIconType(Element iconNode) {
        if (!iconNode.hasAttribute("BUILTIN")) {
            return UNKNOWN;
        }
        return iconNode.getAttribute("BUILTIN");
    }

    private Element getNodesIconElement(Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            log.warn("Node is not an element:");
            logNode(node, "");
            throw new RuntimeException("Node is not an element");
        }
        Element e = (Element) node;
        NodeList nodes = e.getElementsByTagName("icon");
        if (nodes.getLength() == 0) {
            return null;
        }
        return (Element) nodes.item(0);
    }

    private void logNode(Node node, String prefix) {
        log.debug(prefix + " : " + ((Element) node).getAttribute("TEXT"));
    }

}
