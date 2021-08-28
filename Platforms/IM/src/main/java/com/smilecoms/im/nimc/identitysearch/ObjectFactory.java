
package com.smilecoms.im.nimc.identitysearch;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the nimc.identitysearch package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ChangePassword_QNAME = new QName("http://IdentitySearch.nimc/", "changePassword");
    private final static QName _ChangePasswordResponse_QNAME = new QName("http://IdentitySearch.nimc/", "changePasswordResponse");
    private final static QName _CreateToken_QNAME = new QName("http://IdentitySearch.nimc/", "createToken");
    private final static QName _CreateTokenResponse_QNAME = new QName("http://IdentitySearch.nimc/", "createTokenResponse");
    private final static QName _CreateTokenString_QNAME = new QName("http://IdentitySearch.nimc/", "createTokenString");
    private final static QName _CreateTokenStringResponse_QNAME = new QName("http://IdentitySearch.nimc/", "createTokenStringResponse");
    private final static QName _GetPermissionByLevel_QNAME = new QName("http://IdentitySearch.nimc/", "getPermissionByLevel");
    private final static QName _GetPermissionByLevelResponse_QNAME = new QName("http://IdentitySearch.nimc/", "getPermissionByLevelResponse");
    private final static QName _SearchByDemo_QNAME = new QName("http://IdentitySearch.nimc/", "searchByDemo");
    private final static QName _SearchByDemoPhone_QNAME = new QName("http://IdentitySearch.nimc/", "searchByDemoPhone");
    private final static QName _SearchByDemoPhoneResponse_QNAME = new QName("http://IdentitySearch.nimc/", "searchByDemoPhoneResponse");
    private final static QName _SearchByDemoResponse_QNAME = new QName("http://IdentitySearch.nimc/", "searchByDemoResponse");
    private final static QName _SearchByDocumentNumber_QNAME = new QName("http://IdentitySearch.nimc/", "searchByDocumentNumber");
    private final static QName _SearchByDocumentNumberResponse_QNAME = new QName("http://IdentitySearch.nimc/", "searchByDocumentNumberResponse");
    private final static QName _SearchByFinger_QNAME = new QName("http://IdentitySearch.nimc/", "searchByFinger");
    private final static QName _SearchByFingerResponse_QNAME = new QName("http://IdentitySearch.nimc/", "searchByFingerResponse");
    private final static QName _SearchByNIN_QNAME = new QName("http://IdentitySearch.nimc/", "searchByNIN");
    private final static QName _SearchByNINResponse_QNAME = new QName("http://IdentitySearch.nimc/", "searchByNINResponse");
    private final static QName _SearchByPhoto_QNAME = new QName("http://IdentitySearch.nimc/", "searchByPhoto");
    private final static QName _SearchByPhotoResponse_QNAME = new QName("http://IdentitySearch.nimc/", "searchByPhotoResponse");
    private final static QName _UpdateUserSELF_QNAME = new QName("http://IdentitySearch.nimc/", "updateUserSELF");
    private final static QName _UpdateUserSELFResponse_QNAME = new QName("http://IdentitySearch.nimc/", "updateUserSELFResponse");
    private final static QName _VerifyFingerWithData_QNAME = new QName("http://IdentitySearch.nimc/", "verifyFingerWithData");
    private final static QName _VerifyFingerWithDataResponse_QNAME = new QName("http://IdentitySearch.nimc/", "verifyFingerWithDataResponse");
    private final static QName _VerifyPhotoWithData_QNAME = new QName("http://IdentitySearch.nimc/", "verifyPhotoWithData");
    private final static QName _VerifyPhotoWithDataResponse_QNAME = new QName("http://IdentitySearch.nimc/", "verifyPhotoWithDataResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: nimc.identitysearch
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ChangePassword }
     * 
     */
    public ChangePassword createChangePassword() {
        return new ChangePassword();
    }

    /**
     * Create an instance of {@link ChangePasswordResponse }
     * 
     */
    public ChangePasswordResponse createChangePasswordResponse() {
        return new ChangePasswordResponse();
    }

    /**
     * Create an instance of {@link CreateToken }
     * 
     */
    public CreateToken createCreateToken() {
        return new CreateToken();
    }

    /**
     * Create an instance of {@link CreateTokenResponse }
     * 
     */
    public CreateTokenResponse createCreateTokenResponse() {
        return new CreateTokenResponse();
    }

    /**
     * Create an instance of {@link CreateTokenString }
     * 
     */
    public CreateTokenString createCreateTokenString() {
        return new CreateTokenString();
    }

    /**
     * Create an instance of {@link CreateTokenStringResponse }
     * 
     */
    public CreateTokenStringResponse createCreateTokenStringResponse() {
        return new CreateTokenStringResponse();
    }

    /**
     * Create an instance of {@link GetPermissionByLevel }
     * 
     */
    public GetPermissionByLevel createGetPermissionByLevel() {
        return new GetPermissionByLevel();
    }

    /**
     * Create an instance of {@link GetPermissionByLevelResponse }
     * 
     */
    public GetPermissionByLevelResponse createGetPermissionByLevelResponse() {
        return new GetPermissionByLevelResponse();
    }

    /**
     * Create an instance of {@link SearchByDemo }
     * 
     */
    public SearchByDemo createSearchByDemo() {
        return new SearchByDemo();
    }

    /**
     * Create an instance of {@link SearchByDemoPhone }
     * 
     */
    public SearchByDemoPhone createSearchByDemoPhone() {
        return new SearchByDemoPhone();
    }

    /**
     * Create an instance of {@link SearchByDemoPhoneResponse }
     * 
     */
    public SearchByDemoPhoneResponse createSearchByDemoPhoneResponse() {
        return new SearchByDemoPhoneResponse();
    }

    /**
     * Create an instance of {@link SearchByDemoResponse }
     * 
     */
    public SearchByDemoResponse createSearchByDemoResponse() {
        return new SearchByDemoResponse();
    }

    /**
     * Create an instance of {@link SearchByDocumentNumber }
     * 
     */
    public SearchByDocumentNumber createSearchByDocumentNumber() {
        return new SearchByDocumentNumber();
    }

    /**
     * Create an instance of {@link SearchByDocumentNumberResponse }
     * 
     */
    public SearchByDocumentNumberResponse createSearchByDocumentNumberResponse() {
        return new SearchByDocumentNumberResponse();
    }

    /**
     * Create an instance of {@link SearchByFinger }
     * 
     */
    public SearchByFinger createSearchByFinger() {
        return new SearchByFinger();
    }

    /**
     * Create an instance of {@link SearchByFingerResponse }
     * 
     */
    public SearchByFingerResponse createSearchByFingerResponse() {
        return new SearchByFingerResponse();
    }

    /**
     * Create an instance of {@link SearchByNIN }
     * 
     */
    public SearchByNIN createSearchByNIN() {
        return new SearchByNIN();
    }

    /**
     * Create an instance of {@link SearchByNINResponse }
     * 
     */
    public SearchByNINResponse createSearchByNINResponse() {
        return new SearchByNINResponse();
    }

    /**
     * Create an instance of {@link SearchByPhoto }
     * 
     */
    public SearchByPhoto createSearchByPhoto() {
        return new SearchByPhoto();
    }

    /**
     * Create an instance of {@link SearchByPhotoResponse }
     * 
     */
    public SearchByPhotoResponse createSearchByPhotoResponse() {
        return new SearchByPhotoResponse();
    }

    /**
     * Create an instance of {@link UpdateUserSELF }
     * 
     */
    public UpdateUserSELF createUpdateUserSELF() {
        return new UpdateUserSELF();
    }

    /**
     * Create an instance of {@link UpdateUserSELFResponse }
     * 
     */
    public UpdateUserSELFResponse createUpdateUserSELFResponse() {
        return new UpdateUserSELFResponse();
    }

    /**
     * Create an instance of {@link VerifyFingerWithData }
     * 
     */
    public VerifyFingerWithData createVerifyFingerWithData() {
        return new VerifyFingerWithData();
    }

    /**
     * Create an instance of {@link VerifyFingerWithDataResponse }
     * 
     */
    public VerifyFingerWithDataResponse createVerifyFingerWithDataResponse() {
        return new VerifyFingerWithDataResponse();
    }

    /**
     * Create an instance of {@link VerifyPhotoWithData }
     * 
     */
    public VerifyPhotoWithData createVerifyPhotoWithData() {
        return new VerifyPhotoWithData();
    }

    /**
     * Create an instance of {@link VerifyPhotoWithDataResponse }
     * 
     */
    public VerifyPhotoWithDataResponse createVerifyPhotoWithDataResponse() {
        return new VerifyPhotoWithDataResponse();
    }

    /**
     * Create an instance of {@link SearchResponseDemo }
     * 
     */
    public SearchResponseDemo createSearchResponseDemo() {
        return new SearchResponseDemo();
    }

    /**
     * Create an instance of {@link DemoData }
     * 
     */
    public DemoData createDemoData() {
        return new DemoData();
    }

    /**
     * Create an instance of {@link TokenObject }
     * 
     */
    public TokenObject createTokenObject() {
        return new TokenObject();
    }

    /**
     * Create an instance of {@link LoginObject }
     * 
     */
    public LoginObject createLoginObject() {
        return new LoginObject();
    }

    /**
     * Create an instance of {@link LoginMessage }
     * 
     */
    public LoginMessage createLoginMessage() {
        return new LoginMessage();
    }

    /**
     * Create an instance of {@link DemoMapPermission }
     * 
     */
    public DemoMapPermission createDemoMapPermission() {
        return new DemoMapPermission();
    }

    /**
     * Create an instance of {@link AccessType }
     * 
     */
    public AccessType createAccessType() {
        return new AccessType();
    }

    /**
     * Create an instance of {@link RequestType }
     * 
     */
    public RequestType createRequestType() {
        return new RequestType();
    }

    /**
     * Create an instance of {@link DemoDataMandatory }
     * 
     */
    public DemoDataMandatory createDemoDataMandatory() {
        return new DemoDataMandatory();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ChangePassword }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "changePassword")
    public JAXBElement<ChangePassword> createChangePassword(ChangePassword value) {
        return new JAXBElement<ChangePassword>(_ChangePassword_QNAME, ChangePassword.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ChangePasswordResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "changePasswordResponse")
    public JAXBElement<ChangePasswordResponse> createChangePasswordResponse(ChangePasswordResponse value) {
        return new JAXBElement<ChangePasswordResponse>(_ChangePasswordResponse_QNAME, ChangePasswordResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreateToken }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "createToken")
    public JAXBElement<CreateToken> createCreateToken(CreateToken value) {
        return new JAXBElement<CreateToken>(_CreateToken_QNAME, CreateToken.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreateTokenResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "createTokenResponse")
    public JAXBElement<CreateTokenResponse> createCreateTokenResponse(CreateTokenResponse value) {
        return new JAXBElement<CreateTokenResponse>(_CreateTokenResponse_QNAME, CreateTokenResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreateTokenString }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "createTokenString")
    public JAXBElement<CreateTokenString> createCreateTokenString(CreateTokenString value) {
        return new JAXBElement<CreateTokenString>(_CreateTokenString_QNAME, CreateTokenString.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreateTokenStringResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "createTokenStringResponse")
    public JAXBElement<CreateTokenStringResponse> createCreateTokenStringResponse(CreateTokenStringResponse value) {
        return new JAXBElement<CreateTokenStringResponse>(_CreateTokenStringResponse_QNAME, CreateTokenStringResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPermissionByLevel }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "getPermissionByLevel")
    public JAXBElement<GetPermissionByLevel> createGetPermissionByLevel(GetPermissionByLevel value) {
        return new JAXBElement<GetPermissionByLevel>(_GetPermissionByLevel_QNAME, GetPermissionByLevel.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPermissionByLevelResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "getPermissionByLevelResponse")
    public JAXBElement<GetPermissionByLevelResponse> createGetPermissionByLevelResponse(GetPermissionByLevelResponse value) {
        return new JAXBElement<GetPermissionByLevelResponse>(_GetPermissionByLevelResponse_QNAME, GetPermissionByLevelResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchByDemo }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "searchByDemo")
    public JAXBElement<SearchByDemo> createSearchByDemo(SearchByDemo value) {
        return new JAXBElement<SearchByDemo>(_SearchByDemo_QNAME, SearchByDemo.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchByDemoPhone }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "searchByDemoPhone")
    public JAXBElement<SearchByDemoPhone> createSearchByDemoPhone(SearchByDemoPhone value) {
        return new JAXBElement<SearchByDemoPhone>(_SearchByDemoPhone_QNAME, SearchByDemoPhone.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchByDemoPhoneResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "searchByDemoPhoneResponse")
    public JAXBElement<SearchByDemoPhoneResponse> createSearchByDemoPhoneResponse(SearchByDemoPhoneResponse value) {
        return new JAXBElement<SearchByDemoPhoneResponse>(_SearchByDemoPhoneResponse_QNAME, SearchByDemoPhoneResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchByDemoResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "searchByDemoResponse")
    public JAXBElement<SearchByDemoResponse> createSearchByDemoResponse(SearchByDemoResponse value) {
        return new JAXBElement<SearchByDemoResponse>(_SearchByDemoResponse_QNAME, SearchByDemoResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchByDocumentNumber }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "searchByDocumentNumber")
    public JAXBElement<SearchByDocumentNumber> createSearchByDocumentNumber(SearchByDocumentNumber value) {
        return new JAXBElement<SearchByDocumentNumber>(_SearchByDocumentNumber_QNAME, SearchByDocumentNumber.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchByDocumentNumberResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "searchByDocumentNumberResponse")
    public JAXBElement<SearchByDocumentNumberResponse> createSearchByDocumentNumberResponse(SearchByDocumentNumberResponse value) {
        return new JAXBElement<SearchByDocumentNumberResponse>(_SearchByDocumentNumberResponse_QNAME, SearchByDocumentNumberResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchByFinger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "searchByFinger")
    public JAXBElement<SearchByFinger> createSearchByFinger(SearchByFinger value) {
        return new JAXBElement<SearchByFinger>(_SearchByFinger_QNAME, SearchByFinger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchByFingerResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "searchByFingerResponse")
    public JAXBElement<SearchByFingerResponse> createSearchByFingerResponse(SearchByFingerResponse value) {
        return new JAXBElement<SearchByFingerResponse>(_SearchByFingerResponse_QNAME, SearchByFingerResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchByNIN }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "searchByNIN")
    public JAXBElement<SearchByNIN> createSearchByNIN(SearchByNIN value) {
        return new JAXBElement<SearchByNIN>(_SearchByNIN_QNAME, SearchByNIN.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchByNINResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "searchByNINResponse")
    public JAXBElement<SearchByNINResponse> createSearchByNINResponse(SearchByNINResponse value) {
        return new JAXBElement<SearchByNINResponse>(_SearchByNINResponse_QNAME, SearchByNINResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchByPhoto }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "searchByPhoto")
    public JAXBElement<SearchByPhoto> createSearchByPhoto(SearchByPhoto value) {
        return new JAXBElement<SearchByPhoto>(_SearchByPhoto_QNAME, SearchByPhoto.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchByPhotoResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "searchByPhotoResponse")
    public JAXBElement<SearchByPhotoResponse> createSearchByPhotoResponse(SearchByPhotoResponse value) {
        return new JAXBElement<SearchByPhotoResponse>(_SearchByPhotoResponse_QNAME, SearchByPhotoResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateUserSELF }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "updateUserSELF")
    public JAXBElement<UpdateUserSELF> createUpdateUserSELF(UpdateUserSELF value) {
        return new JAXBElement<UpdateUserSELF>(_UpdateUserSELF_QNAME, UpdateUserSELF.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateUserSELFResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "updateUserSELFResponse")
    public JAXBElement<UpdateUserSELFResponse> createUpdateUserSELFResponse(UpdateUserSELFResponse value) {
        return new JAXBElement<UpdateUserSELFResponse>(_UpdateUserSELFResponse_QNAME, UpdateUserSELFResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link VerifyFingerWithData }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "verifyFingerWithData")
    public JAXBElement<VerifyFingerWithData> createVerifyFingerWithData(VerifyFingerWithData value) {
        return new JAXBElement<VerifyFingerWithData>(_VerifyFingerWithData_QNAME, VerifyFingerWithData.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link VerifyFingerWithDataResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "verifyFingerWithDataResponse")
    public JAXBElement<VerifyFingerWithDataResponse> createVerifyFingerWithDataResponse(VerifyFingerWithDataResponse value) {
        return new JAXBElement<VerifyFingerWithDataResponse>(_VerifyFingerWithDataResponse_QNAME, VerifyFingerWithDataResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link VerifyPhotoWithData }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "verifyPhotoWithData")
    public JAXBElement<VerifyPhotoWithData> createVerifyPhotoWithData(VerifyPhotoWithData value) {
        return new JAXBElement<VerifyPhotoWithData>(_VerifyPhotoWithData_QNAME, VerifyPhotoWithData.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link VerifyPhotoWithDataResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://IdentitySearch.nimc/", name = "verifyPhotoWithDataResponse")
    public JAXBElement<VerifyPhotoWithDataResponse> createVerifyPhotoWithDataResponse(VerifyPhotoWithDataResponse value) {
        return new JAXBElement<VerifyPhotoWithDataResponse>(_VerifyPhotoWithDataResponse_QNAME, VerifyPhotoWithDataResponse.class, null, value);
    }

}
