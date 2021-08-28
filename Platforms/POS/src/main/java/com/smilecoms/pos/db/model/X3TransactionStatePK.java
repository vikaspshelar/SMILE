/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.model;

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
public class X3TransactionStatePK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "TRANSACTION_TYPE")
    private String transactionType;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TABLE_NAME")
    private String tableName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRIMARY_KEY")
    private int primaryKey;

    public X3TransactionStatePK() {
    }

    public X3TransactionStatePK(String transactionType, String tableName, int primaryKey) {
        this.transactionType = transactionType;
        this.tableName = tableName;
        this.primaryKey = primaryKey;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(int primaryKey) {
        this.primaryKey = primaryKey;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (transactionType != null ? transactionType.hashCode() : 0);
        hash += (tableName != null ? tableName.hashCode() : 0);
        hash += (int) primaryKey;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof X3TransactionStatePK)) {
            return false;
        }
        X3TransactionStatePK other = (X3TransactionStatePK) object;
        if ((this.transactionType == null && other.transactionType != null) || (this.transactionType != null && !this.transactionType.equals(other.transactionType))) {
            return false;
        }
        if ((this.tableName == null && other.tableName != null) || (this.tableName != null && !this.tableName.equals(other.tableName))) {
            return false;
        }
        if (this.primaryKey != other.primaryKey) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pos.db.model.X3TransactionStatePK[ transactionType=" + transactionType + ", tableName=" + tableName + ", primaryKey=" + primaryKey + " ]";
    }
    
}
