package com.smilecoms.sep.action;

import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.sca.*;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import net.sourceforge.stripes.action.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stripes action bean for all note related actions
 *
 * @author PCB
 *
 */
public class NoteActionBean extends SmileActionBean {

    private Map<String, String> fieldValues;

    public Map<String, String> getFieldValues() {
        return fieldValues;
    }

    public void setFieldValues(Map<String, String> f) {
        this.fieldValues = f;
    }
    private Map<String, String> fieldTypes;

    public Map<String, String> getFieldTypes() {
        return fieldTypes;
    }

    public void setFieldTypes(Map<String, String> f) {
        this.fieldTypes = f;
    }
    private List<Photograph> photographs = new ArrayList<>();

    public List<Photograph> getPhotographs() {
        return photographs;
    }

    public void setPhotographs(List<Photograph> photographs) {
        this.photographs = photographs;
    }

    /* *************************************************************************************
     Actions to do something with the instance data and then send the browser somewhere
     ************************************************************************************** */
    /**
     * Delete a note
     *
     * @return Resolution The Stripes resolution object to tell Stripes where to
     * go next
     */
    public Resolution deleteNote() {
        checkCSRF();
        setStickyNote(SCAWrapper.getUserSpecificInstance().getStickyNote(makeSCAInteger(getStickyNote().getNoteId())));
        setStickyNoteType(SCAWrapper.getUserSpecificInstance().getStickyNoteType(makeSCAString(getStickyNote().getTypeName())));
        if (!isAllowedEmptyAllowsAll(getStickyNoteType().getAllowedRoles())) {
            localiseErrorAndAddToGlobalErrors("sticky.note.not.allowed");
            return retrieveNote();
        }
        SCAWrapper.getUserSpecificInstance().deleteStickyNote(makeSCAInteger(getStickyNote().getNoteId()));
        setPageMessage("deleted.stickynote.successfully");
        setStickyNoteList(SCAWrapper.getUserSpecificInstance().getEntitiesStickyNotes(getStickyNoteEntityIdentifier()));
        return getDDForwardResolution("/note/view_notes.jsp");
    }

    /**
     * Display a note by getting it from the ESB
     *
     * @return Resolution The Stripes resolution object to tell Stripes where to
     * go next
     */
    public Resolution retrieveNote() {
        setStickyNote(SCAWrapper.getUserSpecificInstance().getStickyNote(makeSCAInteger(getStickyNote().getNoteId())));

        if (getStickyNote().getTypeName().equals("Photographs")) {
            photographs.clear();
            writePhotographsDataToFile(getStickyNote().getFields());
        }

        return getDDForwardResolution("/note/view_note.jsp");
    }

    /**
     * Display a note for editing by getting it from the ESB
     *
     * @return Resolution The Stripes resolution object to tell Stripes where to
     * go next
     */
    public Resolution editNote() {
        setStickyNote(SCAWrapper.getUserSpecificInstance().getStickyNote(makeSCAInteger(getStickyNote().getNoteId())));
        setStickyNoteType(SCAWrapper.getUserSpecificInstance().getStickyNoteType(makeSCAString(getStickyNote().getTypeName())));
        if (!isAllowedEmptyAllowsAll(getStickyNoteType().getAllowedRoles())) {
            localiseErrorAndAddToGlobalErrors("sticky.note.not.allowed");
            return retrieveNote();
        }
        
        if (getStickyNote().getTypeName().equals("Photographs")) {
            photographs.clear();
            writePhotographsDataToFile(getStickyNote().getFields());
        }

        return getDDForwardResolution("/note/edit_note.jsp");
    }

    /**
     * Send user to form to capture a new note
     *
     * @return Resolution The Stripes resolution object to tell Stripes where to
     * go next
     */
    public Resolution addNote() {
        setStickyNoteType(SCAWrapper.getUserSpecificInstance().getStickyNoteType(makeSCAString(getStickyNoteType().getTypeName())));
        return getDDForwardResolution("/note/add_note.jsp");
    }

    /**
     * Send user back to view the entity they just attached a note to
     *
     * @return Resolution The Stripes resolution object to tell Stripes where to
     * go next
     */
    public Resolution backToEntity() {
        String entity = getStickyNoteEntityIdentifier().getEntityType();
        if (entity.equalsIgnoreCase("Customer")) {
            RedirectResolution rdr = new RedirectResolution("/Customer.action");
            rdr.addParameter("retrieveCustomer", "");
            rdr.addParameter("customerQuery.customerId", getStickyNoteEntityIdentifier().getEntityId());
            return rdr;
        }
        if (entity.equalsIgnoreCase("Account")) {
            RedirectResolution rdr = new RedirectResolution("/Account.action");
            rdr.addParameter("retrieveAccount", "");
            rdr.addParameter("accountQuery.accountId", getStickyNoteEntityIdentifier().getEntityId());
            return rdr;
        }
        if (entity.equalsIgnoreCase("ProductInstance")) {
            RedirectResolution rdr = new RedirectResolution("/ProductCatalog.action");
            rdr.addParameter("retrieveProductInstance", "");
            rdr.addParameter("productInstance.productInstanceId", getStickyNoteEntityIdentifier().getEntityId());
            return rdr;
        }
        // catch all
        return getDDForwardResolution("/index.jsp");
    }

    /**
     * Modify a note via the ESB
     *
     * @return Resolution The Stripes resolution object to tell Stripes where to
     * go next
     */
    public Resolution modifyNote() {

        
        
        boolean mustUpdateDocuments = false;
        Map fV = getFieldValues();
        Map fT = getFieldTypes();
        String fType;
        String fieldName = "";

        if (fV == null) {
            // All values are blank
            this.localiseErrorAndAddToGlobalErrors("blank.stickynote.fields");
            setStickyNote(SCAWrapper.getUserSpecificInstance().getStickyNote(makeSCAInteger(getStickyNote().getNoteId())));
            return getDDForwardResolution("");
        }
        StickyNoteField field;
        Set ids = fV.keySet();
        Iterator it = ids.iterator();
        while (it.hasNext()) {
            field = new StickyNoteField();
            String id = (String) it.next();
            String value = "";
            if (fV != null && fV.get(id) != null) {
                value = (String) fV.get(id);

                if (fT != null && fT.get(id) != null) {
                    fType = (String) fT.get(id);
                    fieldName = (String) fT.get(id + "_P");
                    if (fType.equals("P")) {
                        mustUpdateDocuments = true;
                        continue;
                    }
                }
            }
            field.setFieldData(value);
            field.setFieldId(Integer.parseInt(id));
            field.setNoteId(getStickyNote().getNoteId());
            field.setDocumentData("");
            getStickyNote().getFields().add(field);
        }

        if (mustUpdateDocuments) {
            for (Photograph photo : getPhotographs()) {
                try {
                    field = new StickyNoteField();
                    field.setDocumentData(Utils.encodeBase64(Utils.getDataFromTempFile(photo.getPhotoGuid())));
                    field.setFieldData("Document type=" + photo.getPhotoType() + ",PhotoGuid=" + photo.getPhotoGuid());
                    field.setFieldName(fieldName);
                    field.setFieldType("P");//=P
                    field.setFieldId(0);
                    field.setNoteId(getStickyNote().getNoteId());
                    getStickyNote().getFields().add(field);
                } catch (Exception ex) {
                    log.warn("Failed to add photograph will ignore and continue, reason is {}", ex.toString());
                }
            }
        }

        setStickyNoteType(SCAWrapper.getUserSpecificInstance().getStickyNoteType(makeSCAString(SCAWrapper.getUserSpecificInstance().getStickyNote(makeSCAInteger(getStickyNote().getNoteId())).getTypeName())));
        if (!isAllowedEmptyAllowsAll(getStickyNoteType().getAllowedRoles())) {
            localiseErrorAndAddToGlobalErrors("sticky.note.not.allowed");
            return retrieveNote();
        }
        getStickyNote().setLastModifiedBy(getUser());
        setStickyNote(SCAWrapper.getUserSpecificInstance().modifyStickyNote(getStickyNote()));
        setPageMessage("modified.stickynote.successfully", getStickyNote().getTypeName());
        return getDDForwardResolution("/note/view_note.jsp");
    }

    /**
     * Attach a note via the ESB
     *
     * @return Resolution The Stripes resolution object to tell Stripes where to
     * go next
     */
    public Resolution attachNote() {
        Map fV = getFieldValues();
        Map fT = getFieldTypes();
        NewStickyNoteField field;
        Set names = fT.keySet();
        Iterator it = names.iterator();
        boolean mustSaveDocuments = false;
        String fieldName = "";

        while (it.hasNext()) {
            field = new NewStickyNoteField();
            String name = (String) it.next();
            String value = "";
            if (fV != null && fV.get(name) != null) {
                value = (String) fV.get(name);
            }
            String fType = (String) fT.get(name);

            field.setFieldData(value);
            field.setFieldName(name);
            field.setFieldType(fType);
            field.setDocumentData("");
            if (fType.equalsIgnoreCase("P")) {
                mustSaveDocuments = true;
                fieldName = name;
                continue;//skip processing of photographs in this block
            }
            getNewStickyNote().getFields().add(field);
        }

        if (mustSaveDocuments) {
            for (Photograph photo : getPhotographs()) {
                try {
                    field = new NewStickyNoteField();
                    field.setDocumentData(Utils.encodeBase64(Utils.getDataFromTempFile(photo.getPhotoGuid())));
                    field.setFieldData("Document type=" + photo.getPhotoType() + ",PhotoGuid=" + photo.getPhotoGuid());
                    field.setFieldName(fieldName);
                    field.setFieldType("P");//=P
                    getNewStickyNote().getFields().add(field);
                } catch (Exception ex) {
                    log.warn("Failed to add photograph will ignore and continue, reason is {}", ex.toString());
                }
            }
        }

        getNewStickyNote().setCreatedBy(getUser());
        
        setStickyNoteType(SCAWrapper.getUserSpecificInstance().getStickyNoteType(makeSCAString(getNewStickyNote().getTypeName())));
        if (!isAllowedEmptyAllowsAll(getStickyNoteType().getAllowedRoles())) {
            localiseErrorAndAddToGlobalErrors("sticky.note.not.allowed");
            return addNote();
        }
        
        setStickyNote(SCAWrapper.getUserSpecificInstance().addStickyNote(getNewStickyNote()));
        if (getStickyNote() != null) {
            if (getStickyNote().getTypeName().equals("Photographs")) {
                photographs.clear();
                writePhotographsDataToFile(getStickyNote().getFields());
            }

            setPageMessage("added.stickynote.successfully", getStickyNote().getTypeName());
            return getDDForwardResolution("/note/view_note.jsp");
        } else {
            return addNote();
        }

    }

    private void writePhotographsDataToFile(List<StickyNoteField> stickyNoteFields) {
        for (StickyNoteField snf : stickyNoteFields) {
            Photograph p = null;
            if (snf.getFieldType().equals("P")) {
                try {
                    p = new Photograph();
                    p.setData(snf.getDocumentData());
                    String fieldData = snf.getFieldData();
                    String[] data = fieldData.split(",");
                    String photoTypeAttrib = data[0].split("=")[1];
                    String photoGuiAttrib = data[1].split("=")[1];
                    p.setPhotoGuid(photoGuiAttrib);
                    p.setPhotoType(photoTypeAttrib);
                    Utils.createTempFile(p.getPhotoGuid(), Utils.decodeBase64(p.getData()));
                } catch (Exception ex) {
                    log.warn("Failed to add photograph [{}] will ignore and continue, reason is {}", p.getPhotoGuid(), ex.toString());
                }
                photographs.add(p);
            }
        }
    }
}
