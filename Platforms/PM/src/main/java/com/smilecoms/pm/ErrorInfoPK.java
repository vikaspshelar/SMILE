/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pm;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Embeddable
public class ErrorInfoPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "CLASS_NAME")
    private String className;
    @Basic(optional = false)
    @NotNull
    @Column(name = "METHOD_NAME")
    private String methodName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EXCEPTION_NAME")
    private String exceptionName;

    public ErrorInfoPK() {
    }

    public ErrorInfoPK(String className, String methodName, String exceptionName) {
        this.className = className;
        this.methodName = methodName;
        this.exceptionName = exceptionName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getExceptionName() {
        return exceptionName;
    }

    public void setExceptionName(String exceptionName) {
        this.exceptionName = exceptionName;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (className != null ? className.hashCode() : 0);
        hash += (methodName != null ? methodName.hashCode() : 0);
        hash += (exceptionName != null ? exceptionName.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ErrorInfoPK)) {
            return false;
        }
        ErrorInfoPK other = (ErrorInfoPK) object;
        if ((this.className == null && other.className != null) || (this.className != null && !this.className.equals(other.className))) {
            return false;
        }
        if ((this.methodName == null && other.methodName != null) || (this.methodName != null && !this.methodName.equals(other.methodName))) {
            return false;
        }
        if ((this.exceptionName == null && other.exceptionName != null) || (this.exceptionName != null && !this.exceptionName.equals(other.exceptionName))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pm.ErrorInfoPK[ className=" + className + ", methodName=" + methodName + ", exceptionName=" + exceptionName + " ]";
    }
    
}
