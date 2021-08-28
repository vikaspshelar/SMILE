/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pm;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "error_info")
@NamedQueries({
    @NamedQuery(name = "ErrorInfo.findAll", query = "SELECT e FROM ErrorInfo e")})
public class ErrorInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ErrorInfoPK errorInfoPK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ERROR_CODE")
    private String errorCode;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ERROR_TYPE")
    private String errorType;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ERROR_DESCRIPTION")
    private String errorDescription;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ERROR_SEVERITY")
    private String errorSeverity;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ROLLBACK")
    private String rollback;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RESOLUTION")
    private String resolution;

    public ErrorInfo() {
    }

    public ErrorInfo(ErrorInfoPK errorInfoPK) {
        this.errorInfoPK = errorInfoPK;
    }

    public ErrorInfo(ErrorInfoPK errorInfoPK, String errorCode, String errorType, String errorDescription, String errorSeverity, String rollback, String resolution) {
        this.errorInfoPK = errorInfoPK;
        this.errorCode = errorCode;
        this.errorType = errorType;
        this.errorDescription = errorDescription;
        this.errorSeverity = errorSeverity;
        this.rollback = rollback;
        this.resolution = resolution;
    }

    public ErrorInfo(String className, String methodName, String exceptionName) {
        this.errorInfoPK = new ErrorInfoPK(className, methodName, exceptionName);
    }

    public ErrorInfoPK getErrorInfoPK() {
        return errorInfoPK;
    }

    public void setErrorInfoPK(ErrorInfoPK errorInfoPK) {
        this.errorInfoPK = errorInfoPK;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getErrorSeverity() {
        return errorSeverity;
    }

    public void setErrorSeverity(String errorSeverity) {
        this.errorSeverity = errorSeverity;
    }

    public String getRollback() {
        return rollback;
    }

    public void setRollback(String rollback) {
        this.rollback = rollback;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (errorInfoPK != null ? errorInfoPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ErrorInfo)) {
            return false;
        }
        ErrorInfo other = (ErrorInfo) object;
        if ((this.errorInfoPK == null && other.errorInfoPK != null) || (this.errorInfoPK != null && !this.errorInfoPK.equals(other.errorInfoPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pm.ErrorInfo[ errorInfoPK=" + errorInfoPK + " ]";
    }
    
}
