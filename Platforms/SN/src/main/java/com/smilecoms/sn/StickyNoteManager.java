package com.smilecoms.sn;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.xml.schema.sn.Done;
import com.smilecoms.xml.sn.SNError;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.ejb.Stateless;
import javax.jws.WebService;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.sn.PlatformInteger;
import com.smilecoms.xml.schema.sn.PlatformString;

import javax.jws.HandlerChain;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * Sticky Notes allow users in SEP to attach arbitrary (though structured)
 * information onto things like devices, customers, agents, sites and agent
 * managers. This makes it possible to add, for example, a warning to a site, a
 * complaint from a customer etc. We don't however want to add code every time
 * business decides on a new type of information that can be attached onto
 * something - we would rather be able to configure them.<br/><br/>
 * Configuration of Sticky Notes is done using Toad or similar tool and
 * modifying and adding rows in certain tables. This is done as follows:<br/>
 * 1) sticky_note_entity_config holds all of the types of entities that are
 * allowed to have sticky notes attached to them. These are fixed and cannot be
 * changed through configuration. The table just shows you which entities have
 * been enabled with Sticky notes from a development perspective.<br/>
 * 2) sticky_note_type_config holds all of the types of sticky notes that are
 * available. You can add more types if you want. Lets use an example and say we
 * want to configure a sticky note type to be used to attach on agents and agent
 * managers to capture a nomination for "Smile Representative of the year". The
 * data to be captured should be a text box to capture "Reasoning", and a drop
 * down box to capture if the agent/agent manager knows that they have been
 * nominated - Call it "Secret Nomination", and can be Y or N. To configure the
 * sticky note type "Smile Representative of the year" just add a row into
 * sticky_note_type_config with the type_name being "Smile Representative of the
 * year"<br/>
 * 3) The sticky_note_entity_type_mapping table is used to say which entity
 * types are applicable to which entities. For our example, we would add 2 rows,
 * one with entity "Agent" and type_id of "Smile Representative of the year",
 * and another one for "AgentManager" and type_id of "Smile Representative of
 * the year". Now that this is configured, we have enough info in the tables for
 * the applications to know that Agents and Agent Managers can have Sticky Notes
 * of the type "Smile Representative of the year" attached. Next we need to
 * configure what those notes contain in terms of fields...<br/>
 * 4) sticky_note_field_config is used to configure the fields that should be
 * captured for the note. For our example, we need to add two rows, the first
 * with field_name of "Reasoning" and field_type of B (B stands for text Box),
 * the second with field_name of "Secret Nomination" and field type of Y|N ( Y|N
 * means a yes/no drop down box. In fact its a drop down box listing the |
 * delimited values provided). Both should have a type_name value of "Smile
 * Representative of the year". Other field types are D for a date field and L
 * for a text line field.<br/><br/>
 *
 * Thats it! If you now opened an agent or agent manager in SEP, and selected to
 * attach a note, your "Smile Representative of the year" note type would be
 * listed and if you captured one, you would see a Y/N drop down and a text box
 * with the appropriate field names.<br/><br/>
 *
 * Sticky notes themselves are stored in the tables sticky_note (for all the
 * main data about a note), and sticky_note_field (to store the data about the
 * fields) and their values. Their structure is fairly self explanatory. Whats
 * key to know is that the configuration is not used once a note has been added
 * to an entity. All of the config data for a note is saved in the sticky_note
 * and sticky_note_field tables so even if you delete all of the configuration
 * tables, you would still be able to read and edit old notes. The same holds
 * true for changes: changing a config for a note type will only impact new
 * notes created of that type.
 *
 * @author PCB
 */
@WebService(serviceName = "SN", portName = "SNSoap", endpointInterface = "com.smilecoms.xml.sn.SNSoap", targetNamespace = "http://xml.smilecoms.com/SN", wsdlLocation = "SNServiceDefinition.wsdl")
@Stateless
@HandlerChain(file = "/handler.xml")
public class StickyNoteManager extends SmileWebService {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;
    @PersistenceContext(unitName = "SNPU")
    private EntityManager em;
    // Query to get the notes attached to an entity
    private static final String SQL_QUERY_SELECT_ENTITY_NOTES = "SELECT N FROM StickyNote N WHERE N.entityId = :entityId AND N.entityType = :entityType order by N.lastModified";
    // Query to get the fields for a note
    private static final String SQL_QUERY_SELECT_NOTES_FIELDS = "SELECT F FROM StickyNoteField F WHERE F.stickyNote = :stickyNote";

    /**
     * Returns a list of all of the types (and each types fields) applicable to
     * the requested entity.
     *
     * @param entityName
     * @return StickyNoteTypeList
     * @throws com.smilecoms.xml.sn.SNError
     */
    public com.smilecoms.xml.schema.sn.StickyNoteTypeList getStickyNoteTypeList(PlatformString entityName) throws SNError {
        setContext(entityName, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        com.smilecoms.xml.schema.sn.StickyNoteTypeList retXMLTypeList = new com.smilecoms.xml.schema.sn.StickyNoteTypeList();
        try {
            StickyNoteEntityConfig DBEntity = JPAUtils.findAndThrowENFE(em, StickyNoteEntityConfig.class, entityName.getString());
            Collection<StickyNoteTypeConfig> types = DBEntity.getStickyNoteTypeConfigCollection();
            Iterator<StickyNoteTypeConfig> typesIt = types.iterator();
            StickyNoteTypeConfig DBType;
            com.smilecoms.xml.schema.sn.StickyNoteType XMLType;
            int cntTypes = 0;
            while (typesIt.hasNext()) {
                // go through mappings and get each note type
                DBType = typesIt.next();
                XMLType = new com.smilecoms.xml.schema.sn.StickyNoteType();
                XMLType.setTypeName(DBType.getTypeName());
                XMLType.setDisplayPriority(DBType.getDisplayPriority());
                XMLType.setAllowedRoles(DBType.getAllowedRoles());

                // get the fields for the note type
                Collection fields = DBType.getStickyNoteFieldConfigCollection();
                Iterator fieldsIt = fields.iterator();
                int cntFields = 0;
                com.smilecoms.xml.schema.sn.StickyNoteFieldType XMLFieldType;
                StickyNoteFieldConfig DBFieldType;
                while (fieldsIt.hasNext()) {
                    DBFieldType = (StickyNoteFieldConfig) fieldsIt.next();
                    XMLFieldType = new com.smilecoms.xml.schema.sn.StickyNoteFieldType();
                    XMLFieldType.setFieldName(DBFieldType.getStickyNoteFieldConfigPK().getFieldName());
                    XMLFieldType.setFieldType(DBFieldType.getFieldType());
                    XMLType.getFieldTypes().add(XMLFieldType);
                    cntFields++;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Sticky Note Type " + XMLType.getTypeName() + " has " + cntFields + " fields.");
                }
                retXMLTypeList.getStickyNoteTypes().add(XMLType);
                cntTypes++;
            }
            if (log.isDebugEnabled()) {
                log.debug("Entity with name " + entityName + " has " + cntTypes + " sticky note types");
            }
        } catch (Exception e) {
            throw processError(SNError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return retXMLTypeList;
    }

    /**
     * Attach a aticky note to an entity
     *
     * @param newStickyNote
     * @return The created sticky note
     * @throws com.smilecoms.xml.sn.SNError
     */
    public com.smilecoms.xml.schema.sn.StickyNote addStickyNote(com.smilecoms.xml.schema.sn.NewStickyNote newStickyNote) throws SNError {
        setContext(newStickyNote, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        com.smilecoms.xml.schema.sn.StickyNote retXMLStickyNote = null;
        try {
            StickyNote DBNote = new StickyNote();
            DBNote.setCreatedBy(newStickyNote.getCreatedBy());
            DBNote.setCreatedDatetime(Utils.getCurrentJavaDate());
            DBNote.setLastModified(Utils.getCurrentJavaDate());
            DBNote.setEntityId(newStickyNote.getEntityId());
            DBNote.setEntityType(newStickyNote.getEntityType());
            DBNote.setLastModifiedBy(newStickyNote.getCreatedBy());
            DBNote.setTypeName(newStickyNote.getTypeName());
            DBNote.setLastModified(Utils.getCurrentJavaDate());
            if (log.isDebugEnabled()) {
                log.debug("About to persist new sticky note for entityID " + newStickyNote.getEntityId() + " on entity " + newStickyNote.getEntityType());
            }
            em.persist(DBNote);
            em.flush();
            em.refresh(DBNote);

            if (log.isDebugEnabled()) {
                log.debug("Persisted new sticky note for entityID " + newStickyNote.getEntityId() + " on entity " + newStickyNote.getEntityType());
            }
            // Now persist fields
            List XMLFields = newStickyNote.getFields();
            Iterator XMLFieldsIt = XMLFields.iterator();
            com.smilecoms.xml.schema.sn.NewStickyNoteField f;
            while (XMLFieldsIt.hasNext()) {
                f = (com.smilecoms.xml.schema.sn.NewStickyNoteField) XMLFieldsIt.next();
                StickyNoteField DBField = new StickyNoteField();
                if (f.getFieldData().length() > 2000) {
                    f.setFieldData(f.getFieldData().substring(0, 2000));
                }
                DBField.setFieldData(f.getFieldData());
                DBField.setFieldName(f.getFieldName());
                DBField.setFieldType(f.getFieldType());
                DBField.setStickyNote(DBNote);
                if (f.getFieldType().equals("P")) {
                    DBField.setDocumentData(Utils.decodeBase64(f.getDocumentData()));
                }
                if (log.isDebugEnabled()) {
                    log.debug("About to persist a field with name " + f.getFieldName());
                }
                em.persist(DBField);
            }

            retXMLStickyNote = getStickyNote(makePlatformInteger(DBNote.getStickyNoteId()));
        } catch (Exception e) {
            throw processError(SNError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return retXMLStickyNote;
    }

    /**
     * Modify an existing sticky note
     *
     * @param msn
     * @return The modified sticky note
     * @throws com.smilecoms.xml.sn.SNError
     */
    public com.smilecoms.xml.schema.sn.StickyNote modifyStickyNote(com.smilecoms.xml.schema.sn.StickyNote msn) throws SNError {
        setContext(msn, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        com.smilecoms.xml.schema.sn.StickyNote retXMLStickyNote = null;
        try {
            StickyNote DBNote = JPAUtils.findAndThrowENFE(em, StickyNote.class, msn.getNoteId());
            JPAUtils.checkLastModified(msn.getVersion(), DBNote.getVersion());
            DBNote.setLastModifiedBy(msn.getLastModifiedBy());
            DBNote.setLastModified(Utils.getCurrentJavaDate());
            if (log.isDebugEnabled()) {
                log.debug("About to persist modified sticky note for entityID " + msn.getEntityId() + " on entity " + msn.getEntityType());
            }
            em.persist(DBNote);
            if (log.isDebugEnabled()) {
                log.debug("Persisted modified sticky note for entityID " + msn.getEntityId() + " on entity " + msn.getEntityType());
            }
            // Now persist fields
            List XMLFields = msn.getFields();
            Iterator XMLFieldsIt = XMLFields.iterator();
            com.smilecoms.xml.schema.sn.StickyNoteField f = null;
            boolean addNewPhotos = false;

            while (XMLFieldsIt.hasNext()) {
                f = (com.smilecoms.xml.schema.sn.StickyNoteField) XMLFieldsIt.next();
                if (f.getFieldType() != null && f.getFieldType().equals("P")) {
                    addNewPhotos = true;
                    continue; //Dont search if phototograph, will add it as new sticky note field instead of modifying
                }
                StickyNoteField DBField = JPAUtils.findAndThrowENFE(em, StickyNoteField.class, f.getFieldId());
                DBField.setFieldData(f.getFieldData());
                em.persist(DBField);
            }

            if (addNewPhotos) {
                Collection<StickyNoteField> fields = DBNote.getStickyNoteFieldCollection();
                for (StickyNoteField field : fields) {
                    if (f.getFieldType() != null && field.getFieldType().equals("P")) {
                        log.debug("Going to remove sticky note field {}", field.getStickyNoteFieldId());
                        em.createNativeQuery("delete from sticky_note_field where sticky_note_field_id = ?").setParameter(1, field.getStickyNoteFieldId()).executeUpdate();
                        em.flush();
                    }
                }
                
                XMLFieldsIt = XMLFields.iterator();//re-initialise iterator

                while (XMLFieldsIt.hasNext()) {
                    
                    f = (com.smilecoms.xml.schema.sn.StickyNoteField) XMLFieldsIt.next();
                    if (f.getFieldType() != null && f.getFieldType().equals("P")) {
                        StickyNoteField DBField = new StickyNoteField();
                        DBField.setFieldData(f.getFieldData());
                        DBField.setFieldName(f.getFieldName());
                        DBField.setFieldType(f.getFieldType());
                        DBField.setStickyNote(DBNote);
                        DBField.setDocumentData(Utils.decodeBase64(f.getDocumentData()));
                        log.debug("Going to add new sticky note field");
                        em.persist(DBField);
                        em.flush();
                    }
                }
            }
            retXMLStickyNote = getStickyNote(makePlatformInteger(DBNote.getStickyNoteId()));
        } catch (Exception e) {
            throw processError(SNError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return retXMLStickyNote;
    }

    /**
     * Return a list of sticky notes that have been attached to a specific
     * entity
     *
     * @param entityIdentifier
     * @return StickyNoteList
     * @throws com.smilecoms.xml.sn.SNError
     */
    public com.smilecoms.xml.schema.sn.StickyNoteList getEntitiesStickyNotes(com.smilecoms.xml.schema.sn.EntityIdentifier entityIdentifier) throws SNError {
        setContext(entityIdentifier, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        com.smilecoms.xml.schema.sn.StickyNoteList retXMLNotes = new com.smilecoms.xml.schema.sn.StickyNoteList();
        try {
            Query q = em.createQuery(SQL_QUERY_SELECT_ENTITY_NOTES);
            q.setParameter("entityId", entityIdentifier.getEntityId());
            q.setParameter("entityType", entityIdentifier.getEntityType());
            List notes = q.getResultList();
            Iterator it = notes.iterator();
            StickyNote s;
            while (it.hasNext()) {
                s = (StickyNote) it.next();
                retXMLNotes.getStickyNotes().add(getStickyNote(s));
            }
        } catch (Exception e) {
            throw processError(SNError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return retXMLNotes;
    }

    /**
     * Return the info for a specific sticky note type
     *
     * @param stickyNoteTypeName
     * @return StickyNoteType
     * @throws com.smilecoms.xml.sn.SNError
     */
    public com.smilecoms.xml.schema.sn.StickyNoteType getStickyNoteType(PlatformString stickyNoteTypeName) throws SNError {
        setContext(stickyNoteTypeName, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        com.smilecoms.xml.schema.sn.StickyNoteType XMLType = new com.smilecoms.xml.schema.sn.StickyNoteType();
        try {
            StickyNoteTypeConfig DBType;
            DBType = JPAUtils.findAndThrowENFE(em, StickyNoteTypeConfig.class, stickyNoteTypeName.getString());
            XMLType.setTypeName(DBType.getTypeName());
            XMLType.setDisplayPriority(DBType.getDisplayPriority());
            XMLType.setAllowedRoles(DBType.getAllowedRoles());

            // get the fields for the note type
            Collection fields = DBType.getStickyNoteFieldConfigCollection();
            Iterator fieldsIt = fields.iterator();
            int cntFields = 0;
            com.smilecoms.xml.schema.sn.StickyNoteFieldType XMLFieldType;
            StickyNoteFieldConfig DBFieldType;
            while (fieldsIt.hasNext()) {
                DBFieldType = (StickyNoteFieldConfig) fieldsIt.next();
                XMLFieldType = new com.smilecoms.xml.schema.sn.StickyNoteFieldType();
                XMLFieldType.setFieldName(DBFieldType.getStickyNoteFieldConfigPK().getFieldName());
                XMLFieldType.setFieldType(DBFieldType.getFieldType());
                XMLType.getFieldTypes().add(XMLFieldType);
                cntFields++;
            }
            if (log.isDebugEnabled()) {
                log.debug("Sticky Note Type " + XMLType.getTypeName() + " has " + cntFields + " fields.");
            }
        } catch (Exception e) {
            throw processError(SNError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return XMLType;
    }

    /**
     * Returns the details of a specific sticky note by note id
     *
     * @param stickyNoteID
     * @return StickyNote
     * @throws com.smilecoms.xml.sn.SNError
     */
    public com.smilecoms.xml.schema.sn.StickyNote getStickyNote(PlatformInteger stickyNoteID) throws SNError {
        setContext(stickyNoteID, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        com.smilecoms.xml.schema.sn.StickyNote XMLNote = new com.smilecoms.xml.schema.sn.StickyNote();
        try {
            if (log.isDebugEnabled()) {
                logStart();
            }
            StickyNote DBNote = JPAUtils.findAndThrowENFE(em, StickyNote.class, stickyNoteID.getInteger());
            XMLNote = getStickyNote(DBNote);
        } catch (Exception e) {
            throw processError(SNError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return XMLNote;
    }

    /**
     * Returns the details of a specific sticky as an XML bean object based on
     * the passed in JPA StickyNote
     *
     * @param DBNote
     * @return StickyNote
     * @throws com.smilecoms.xml.sn.SNError
     */
    private com.smilecoms.xml.schema.sn.StickyNote getStickyNote(StickyNote DBNote) throws SNError {
        if (log.isDebugEnabled()) {
            logStart();
        }
        com.smilecoms.xml.schema.sn.StickyNote XMLNote = new com.smilecoms.xml.schema.sn.StickyNote();
        try {

            XMLNote.setCreatedBy(DBNote.getCreatedBy());
            XMLNote.setCreatedDateTime(Utils.getDateAsXMLGregorianCalendar(DBNote.getCreatedDatetime()));
            XMLNote.setEntityId(DBNote.getEntityId());
            XMLNote.setEntityType(DBNote.getEntityType());
            XMLNote.setLastModified(Utils.getDateAsXMLGregorianCalendar(DBNote.getLastModified()));
            XMLNote.setLastModifiedBy(DBNote.getLastModifiedBy());
            XMLNote.setNoteId(DBNote.getStickyNoteId());
            XMLNote.setTypeName(DBNote.getTypeName());
            XMLNote.setVersion(DBNote.getVersion());
            addFieldsToNote(DBNote, XMLNote);
        } catch (Exception e) {
            throw processError(SNError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return XMLNote;
    }

    /**
     * Gets the fields associated to the JPA DBNote and adds them onto the XML
     * bean StickyNote
     *
     * @param DBNote
     * @param XMLNote
     */
    private void addFieldsToNote(StickyNote DBNote, com.smilecoms.xml.schema.sn.StickyNote XMLNote) {
        if (log.isDebugEnabled()) {
            logStart();
        }
        Query q = em.createQuery(SQL_QUERY_SELECT_NOTES_FIELDS);
        q.setParameter("stickyNote", DBNote);
        List fields = q.getResultList();
        Iterator fieldsIt = fields.iterator();
        StickyNoteField f;
        com.smilecoms.xml.schema.sn.StickyNoteField XMLField;
        int cnt = 0;
        while (fieldsIt.hasNext()) {
            f = (StickyNoteField) fieldsIt.next();
            XMLField = new com.smilecoms.xml.schema.sn.StickyNoteField();
            XMLField.setFieldData(f.getFieldData());
            XMLField.setFieldId(f.getStickyNoteFieldId());
            XMLField.setFieldName(f.getFieldName());
            XMLField.setFieldType(f.getFieldType());
            XMLField.setNoteId(DBNote.getStickyNoteId());
            if (f.getFieldType() != null && f.getFieldType().equals("P")) {
                try {
                    XMLField.setDocumentData(Utils.encodeBase64(f.getDocumentData()));
                } catch (Exception ex) {
                    log.warn("SN: Failed to add photograph [{}] will ignore and continue, reason is {}", f.getFieldName(), ex.toString());
                }
            }

            XMLNote.getFields().add(XMLField);
            cnt++;
        }
        if (log.isDebugEnabled()) {
            logEnd();
        }
    }

    /**
     * Called by the parent class to get an instance if the entity Manager.
     *
     * @return EntityManager for the underlying DB connection
     */
    protected EntityManager getEntityManager() {
        return em;
    }

    /**
     * Utility method to make a complex Integer object from a int.
     *
     * @param integerToUse Integer to put in the complex object
     * @return The resulting complex object
     */
    private com.smilecoms.xml.schema.sn.PlatformInteger makePlatformInteger(int i) {
        com.smilecoms.xml.schema.sn.PlatformInteger ret = new com.smilecoms.xml.schema.sn.PlatformInteger();
        ret.setInteger(i);
        ret.setPlatformContext((com.smilecoms.xml.schema.sn.PlatformContext) getContext());
        return ret;
    }

    public Done isUp(String isUpRequest) throws SNError {
        if (!BaseUtils.isPropsAvailable()) {
            throw createError(SNError.class, "Properties are not available so this platform will be reported as down");
        }
        return makeDone();
    }

    /**
     * Utility method to make a complex boolean object with value TRUE.
     *
     * @return The resulting complex object
     */
    private com.smilecoms.xml.schema.sn.Done makeDone() {
        com.smilecoms.xml.schema.sn.Done done = new com.smilecoms.xml.schema.sn.Done();
        done.setDone(com.smilecoms.xml.schema.sn.StDone.TRUE);
        return done;
    }

    public Done deleteStickyNote(PlatformInteger stickyNoteIDToDelete) throws SNError {
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            StickyNote DBNote = JPAUtils.findAndThrowENFE(em, StickyNote.class, stickyNoteIDToDelete.getInteger());
            String eventMsg = stickyNoteIDToDelete.getInteger() + "|" + DBNote.getEntityType() + "|" + DBNote.getEntityId() + "|" + DBNote.getTypeName();

            Collection<StickyNoteField> fields = DBNote.getStickyNoteFieldCollection();
            for (StickyNoteField field : fields) {
                em.remove(field);
            }
            em.remove(DBNote);
            PlatformEventManager.createEvent("SN", "deleteStickyNote", stickyNoteIDToDelete.getPlatformContext().getOriginatingIdentity(), stickyNoteIDToDelete.getPlatformContext().getOriginatingIP() + ": " + eventMsg);
        } catch (Exception e) {
            throw processError(SNError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }
}