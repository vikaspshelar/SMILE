/* Copyright 2005-2006 Tim Fennell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sourceforge.stripes.tag.layout.buffered;

import net.sourceforge.stripes.exception.StripesJspException;
import net.sourceforge.stripes.tag.StripesTagSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.DynamicAttributes;
import java.util.Stack;

/**
 * Renders a named layout, optionally overriding one or more components in the layout. Any
 * attributes provided to the class other than 'name' will be placed into page context during
 * the evaluation of the layout, making them available to other tags, and in EL.
 *
 * @author Tim Fennell
 * @since Stripes 1.1
 */
public class LayoutRenderTag extends StripesTagSupport implements BodyTag, DynamicAttributes {
    private String name;
    private LayoutContext context = new LayoutContext();

    /** Gets the name of the layout to be used. */
    public String getName() { return name; }

    /** Sets the name of the layout to be used. */
    public void setName(String name) { this.name = name; }

    /** Used by the JSP container to provide the tag with dynamic attributes. */
    public void setDynamicAttribute(String uri, String localName, Object value) throws JspException {
        this.context.getParameters().put(localName, value);
    }

    /**
     * Allows nested tags to register their contents for rendering in the layout.
     *
     * @param name the name of the component to be overridden in the layout
     * @param contents the output that will be used
     */
    public void addComponent(String name, String contents) {
        this.context.getComponents().put(name, contents);
    }

    /**
     * Pushes the values of any dynamic attributes into page context attributes for
     * the duration of the tag.
     *
     * @return EVAL_BODY_BUFFERED in all cases
     */
    @Override
    public int doStartTag() throws JspException {
        pushPageContextAttributes(this.context.getParameters());
        return EVAL_BODY_BUFFERED;
    }

    /**
     * Discards the body content since it is not used. Input from nested LayoutComponent tags is
     * captured through a different mechanism.
     */
    public void setBodyContent(BodyContent bodyContent) { /* Don't use it */ }

    /** Does nothing. */
    public void doInitBody() throws JspException { /* Do nothing. */ }

    /**
     * Does nothing.
     * @return SKIP_BODY in all cases.
     */
    public int doAfterBody() throws JspException { return SKIP_BODY; }

    /**
     * Invokes the named layout, providing it with the overridden components and provided
     * parameters.
     * @return EVAL_PAGE in all cases.
     * @throws JspException if any exceptions are encountered processing the request
     */
    @Override
    @SuppressWarnings("unchecked")
	public int doEndTag() throws JspException {
        try {
            if (!name.startsWith("/")) {
                throw new StripesJspException("The name= attribute of the layout-render tag must be " +
                    "an absolute path, starting with a forward slash (/). Please modify the " +
                    "layout-render tag with the name '" + name + "' accordingly.");
            }

            HttpServletRequest request = (HttpServletRequest) getPageContext().getRequest();

            // Put the components into the request, for the definition tag to use.. using a stack
            // to allow for the same layout to be nested inside itself :o
            String attributeName = LayoutDefinitionTag.PREFIX + this.name;
            Stack<LayoutContext> stack =
                    (Stack<LayoutContext>) request.getAttribute(attributeName);
            if (stack == null) {
                stack = new Stack<LayoutContext>();
                request.setAttribute(attributeName, stack);
            }

            stack.push(this.context);

            // Now wrap the JSPWriter, and include the target JSP
            BodyContent content = getPageContext().pushBody();
            getPageContext().include(this.name, false);
            getPageContext().popBody();
            getPageContext().getOut().write(content.getString());

            // Check that the layout actually got rendered as some containers will
            // just quietly ignore includes of non-existent pages!
            if (!this.context.isRendered()) {
                throw new StripesJspException(
                    "Attempt made to render a layout that does not exist. The layout name " +
                    "provided was '" + this.name + "'. Please check that a JSP/view exists at " +
                    "that location within your web application."
                );
            }


            stack.pop();
            popPageContextAttributes(); // remove any dynattrs from page scope

            // Clean up in case the tag gets pooled
            this.context = new LayoutContext();
        }
        catch (StripesJspException sje) { throw sje; }
        catch (Exception e) {
            throw new StripesJspException(
                "An exception was raised while invoking a layout. The layout used was " +
                "'" + this.name + "'. The following information was supplied to the render " +
                "tag: " + this.context.toString(), e);
        }

        return EVAL_PAGE;
    }
}
