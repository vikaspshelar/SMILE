/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.helpers;

/**
 *
 * @author paul
 */
public enum Permissions {
    // Account functionality
    REMOVE_ACCOUNT_VOUCHER_LOCK("Remove voucher lock on an account to allow the account to use voucher redemption functionality"),
    VIEW_ACCOUNT("View the basic information of an account. E.g. its balance, unit credits"),
    VIEW_ACCOUNT_HISTORY("View the account history e.g. transfers, service usage, unit credit purchases"),
    REDEEM_STRIP("Redeem a voucher (scratch card) on behalf of a customer"),
    SEND_STRIPS_TO_DISTRIBUTOR("Allow a SEP frontend user to send voucher strips belonging to an invoice to the distibutor."),
    TRANSFER_FUNDS("Transfer from one of the logged in users accounts to any account except System/Management accounts"),
    TRANSFER_GRAPH("Generate a pdf spider diagram showing an accounts transfers and their details such as IP addresses"),
    PURCHASE_UNIT_CREDIT("Allows purchasing unit-credits (bundles) against someone elses account in SEP paid for by the account getting the UC. Works in conjunction with the individual unit credits cnfiguration which determines which unit credits can be purchased by the logged in user"),
    PURCHASE_UNIT_CREDIT_PAID_BY_ANY_ACCOUNT("Purchase unit-credits (bundles) in SEP against someone elses account and pay for it from any account. Works in conjunction with the individual unit credits configuration which determines which unit credits can be purchased by the logged in user"),
    PURCHASE_UNIT_CREDIT_PAID_BY_LOGGED_IN_USERS_ACCOUNT("Purchase unit-credits (bundles) in SEP against someone elses account and pay for it from the logged in users account. Works in conjunction with the individual unit credits configuration which determines which unit credits can be purchased by the logged in user"),
    PURCHASE_UNIT_CREDIT_PAID_BY_CUSTOMERS_ACCOUNT("Purchase unit-credits (bundles) in SEP against someone elses account and pay for it by another account owned by the recipient. Works in conjunction with the individual unit credits configuration which determines which unit credits can be purchased by the logged in user"),
    CREATE_ACCOUNT("Create a new unassociated account via the 'Create Account' menu"),
    TRANSFER_FUNDS_FROM_ANY_ACCOUNT("Authority to transfer from ANY Smile account (including System/Management accounts) to any other account"),
    SPLIT_UNIT_CREDIT_INSTANCE_WITHIN_ORG("Break up a unit credit and assign the bit to another account within an organisation related to the original unit credit"),
    SPLIT_UNIT_CREDIT_INSTANCE_ANYWHERE("Break up a unit credit and assign the bit to another account that need not be related to the original unit credit"),
    CHANGE_ACCOUNT_STATUS("Change the status of an account e.g. from 'Allow All Usage' to 'Disallow All Usage'"),
    CHANGE_ACCOUNT_SERVICES_STATUS("Change all service statuses of an account"),
    OVERRIDE_RESTRICTIVE_ACCOUNT_STATUS("Override a restrictive status of an account e.g. from 'Allow All Usage' to 'Disallow All Usage'"),
    MANAGE_FUTURE_TRANSFERS("Access to the scheduling, cancelling of future transfers against any non-system account"),
    MANAGE_FUTURE_UC_PURCHASES("Access to the scheduling, cancelling of future unit credit (bundle) purchases against any non-system account"),
    REVERSE_BALANCE_TRANSFER("Reverse a balance transfer that has previosuly been done"),
    DELETE_UNIT_CREDIT_INSTANCE("Reverse the purchasing of a unit credit (bundle). Has to happen on the day on which the bundle was purchased"),
    // Prepaid Strip Functionality (scratch cards)
    BULK_CHANGE_STRIP_STATUS_TO_EX("Once strips are created OR generated (have a 'GE' status) they needed to be extracted for printing which gives them an 'EX' status, this permission is needed to transition the strips to the 'EX' status"),
    BULK_CHANGE_STRIP_STATUS_TO_WH("This permission is needed for strips with EX status to be sent and received in Warehouse, which gives them the 'WH' status"),
    BULK_CHANGE_STRIP_STATUS_TO_DC("Permission to transition strips to a 'DC' status which mark them as redeemable, strips must be preceded with a 'WH' status"),
    CREATE_STRIPS("Permission for the 'Vouchers' menu which authorizes the creation of strips (scratch cards) in SEP, giving them a 'GE' status"),
    VIEW_STRIP("Permission authorizing the viewing of detailed information about a specific strip (scratch card)"),
    //Customer functionality    
    SEND_PASSWORD_RESET_LINK("Send the customer a link via email to change their password"),
    VIEW_CUSTOMER("View a customers profile information"),
    ADD_CUSTOMER("Register a new customer"),
    EDIT_CUSTOMER_DATA("Modify basic details of a customer such as name, surname, phone number"),
    EDIT_CUSTOMER_PASSWORD("Directly set a new password for a customer without their intervention"),
    EDIT_CUSTOMER_PERMISSIONS("Remove or add users to specific security groups (role) in SEP, e.g. adding a user 'bob' to the group 'SDNationalHead' requires this permission"),
    EDIT_CUSTOMER_ADDRESS("Update a customers physical or postal address"),
    EDIT_CUSTOMER_NATIONAL_ID_NUMBER("Update a customers National ID Number"),
    EDIT_CUSTOMER_PHOTOS("Update a customers documents/photographs"),
    ADD_CUSTOMER_PHOTOS("Add a customers documents/photographs"),
    EDIT_CUSTOMER_ACCOUNT_MANAGER("Change the assigned account manager of a customer"),
    EDIT_CUSTOMER_OPT_IN_LEVEL("Change the Messaging Opt-In level of a customer"),
    EDIT_CUSTOMER_EMAIL_ADDRESS("Change the Email address of a customer"),
    EDIT_SEP_USER_EMAIL_ADDRESS("Change the Email address of a customer who has access permissions to SEP"),
    EDIT_CUSTOMER_WAREHOUSE_ID("Change the warehouse Id configuration for a salesperson"),
    EDIT_CUSTOMER_KYC_STATUS_FROM_VERIFIED("Change KYC status from verified to something else"),
    EDIT_CUSTOMER_KYC_STATUS_TO_VERIFIED("Change KYC status to verified"),
    EDIT_CUSTOMER_KYC_STATUS_TO_PENDING("Change KYC status to pending"),
    ADD_PRODUCT_INSTANCE("Provision a product against a customer"),
    ADD_SERVICE_INSTANCE("Add new services onto an existing product instance"),
    REMOVE_PRODUCT_INSTANCE("Allows the removal/deletion of a product instance (i.e. deprovision a product)"),
    REMOVE_SERVICE_INSTANCE("Remove a service instance off a product instance"),
    TEMPORARILY_DEACTIVATE_SERVICE_INSTANCE("Temporarily deactivate a specific service instance, the status of the provisioned service becomes TD(Temporal Deactivated)"),
    REACTIVATE_SERVICE_INSTANCE("A de-activated service can be re-activated by a user with this permission, making it available for use again the status becomes AC(Active)"),
    CHANGE_SERVICE_INSTANCE_CUSTOMER("A service associated to a specific customer can be changed to be associated with another customer. I.e. change the user of the service"),
    CHANGE_PRODUCT_INSTANCE_CUSTOMER("Change a customer associated with a specific product. I.e. change the owner of the product"),
    CHANGE_SERVICE_INSTANCE_ANY_ACCOUNT("During service provisioning a service can be associated with a specific account, this permission allows changing the account the service charges to after the service has been provisioned"),
    USE_PRODUCT_PROMOTION_CODE("Provisioning a product with a configured promotion code. This works in tandem with the configuration of what provisioning codes can be used by what roles"),
    CHANGE_SERVICE_INSTANCE_SPECIFICATION("Allows the changing of a provisioned service to be re-configured to a different service if alternatives are available under that product"),
    CHANGE_SERVICE_INSTANCE_CONFIGURATION("Allows the changing of a provisioned service instances attributes"),
    TROUBLE_TICKETING("Permission needed for customer care support related functionalities in SEP such as creating and modifying trouble tickets"),
    LOAD_MIND_MAP("Permission needed for customer care support to load a new mind map"),
    MAKE_CUSTOMER_AN_ADMINISTRATOR("Permission needed to add OR remove a SEP user into OR from the 'Administrator' role or security group"),
    VIEW_PRODUCT_OR_SERVICE_INSTANCES("Permission needed for viewing a user's OR customer's provisioned products OR services in SEP"),
    CHANGE_SERVICE_INSTANCE_EXISTING_ACCOUNT("Allows a provisioned service to be associated with a different account owned by the same customer owning the existing account"),
    BULK_EMAIL("Permission required when sending bulk email in SEP"),
    BULK_SMS("Permission required when sending bulk SMS in SEP"),
    TRACK_CUSTOMER("Track the actions/screens seen by a customer on MySmile in order to aid them in the use of MySmile"),
    EMAIL("Permission needed to send an individual email to a customer from within SEP"),
    IGNORE_PP_SALE_STATUS_ON_SIM("Ignore whether a SIM is pending payment during provisioning"),
    VIEW_CUSTOMER_OTHER_MNO("View a customers profile information who belongs to other MNO"),
    //Organisation Functionality
    VIEW_ORGANISATION("View the basic profile information of an organisatisation"),
    ADD_ORGANISATION("Register a new organisation"),
    EDIT_ORGANISATION("Modify an organisations basic details"),
    EDIT_ORGANISATION_ADDRESS("Modify an organisation's address"),
    ADD_CUSTOMER_ROLES_TO_ORGANISATION("Associate a customer with a specific organisation"),
    ADD_ONESELF_TO_ORGANISATION("Allow the logged in user to add themselves into an Orgainsation"),
    EDIT_ORGANISATION_PERMISSIONS("Removing OR adding roles (security groups) that are permitted to modify an organisations details"),
    EDIT_ORGANISATION_PHOTOS("Update an organisation's documents captured during registration"),
    // Product Catalog
    VIEW_PRODUCT_CATALOG("View Smile's commercial product catalog"),
    EDIT_UNIT_CREDIT_INSTANCE("Edit properties of a unit credit instance (bundle) such as start date, expiry date, service instance id or account number"),
    EDIT_UNLIMITED_UNIT_CREDIT_EXPIRY("Change the expiry date of an unlimited unit credit"),
    EDIT_UNIT_CREDIT_HINT("Change the hint of a unit credit"),
    ADD_PRODUCT_SPECIFYING_KIT("Add a product instance and specify the Kit and device it comes with instead of using the sale data"),
    ADD_PRODUCT_SPECIFYING_REFERRAL_CODE("Add a product instance and specify a referral code"),
    UPLOAD_INTERCONNECT_RATE_CARD("Upload rating data"),
    BULK_PROVISION_PRODUCT_INSTANCE("Bulk provision SIM cards by supplying a list of ICCIDs"),
    // Sticky Notes
    STICKY_NOTES("Add or remove sticky notes on a customer, account or product instance"),
    // SIM Cards
    PROVISION_SIM_CARD("Permission needed to provision a new SIM Card on the back-end. This is not required in order to provision a product on a SIM card"),
    SIM_SWAP("Do a SIM Swap"),
    SIM_COMPLIANCE_CHECK("Allow to do SIM Complaince Check with the TZ Police force server"),
    SIM_IMEI_CHECK("Check the status of the IMEI"),
    SIM_IMEI_CHANGE("Change the status of the IMEI"),
    // System admin    
    REFRESH_PROPERTIES("Refreshing HOBIT system properties and caches"),
    UPDATE_TICKER("Adding or removing information that neededs to be relayed or broadcasted to all SEP users on the top ticker"),
    MODIFY_ROLES_PERMISSIONS("Change what roles can perform what functionality in SEP"),
    RUN_GENERAL_QUERY("Use the 'General Queries' functionality in SEP"),
    RUN_GENERAL_TASK("Use the 'General Tasks' functionality in SEP"),
    TEST_TRIGGER_EMAILS("Send logged in user all the trigger emails as a test"),
    // PABX    
    CALL_CENTRE_PABX("Make use of Smile's telephony services provided by the PABX system which is integrated with SEP"),
    //EPC functions
    EPC_ADMIN("Management of Smile's services at the EPC architecture level. E.g. purge users from the PGW/MME"),
    // Sales
    APPROVE_PROMO_CODE("Approve the use of a promotion code on another persons sale"),
    ATTRIBUTE_SALE("Attribute a sale to another salesperson"),
    MAKE_SALE("Permission needed by anyone who needs to make sale or view sales data using the POS interface provided by SEP"),
    MAKE_SALE_USD("Permission needed by anyone who needs to make a USD sale"),
    MAKE_SALE_EUR("Permission needed by anyone who needs to make a EUR sale"),
    ALLOW_NONSTOCK_ITEM_SALE_WITHNO_RECIPIENT_ACCOUNT("Make sale with no account id specified for none stock item"),
    VIEW_CASH_IN("View the outstanding cash-in data for a user"),
    PROCESS_CASH_IN("Process a cash-in for a user"),
    PROVISION_GOLDEN_NUMBER("Provision a golden number"),
    PROCESS_CASH_IN_BY_PROXY("Permission for the  logged in person to cash in on behalf of someone else not present and without their password"),
    PROCESS_BANK_DEPOSIT_CASH_IN("Process a bank deposit cash-in for the currently logged in user"),
    PROCESS_BANK_PAYMENT("Permission was needed to process sales of payment type bank transfer OR cheque (This process is deprecated)"),
    PROCESS_BANK_PAYMENT_VERIFICATION("Permission was needed to verify processed sales of payment type bank transfer OR cheque (This process is deprecated as well, X3 is handling this)"),
    VIEW_SALE("Permission to view sales information in SEP"),
    USE_SALE_PROMOTION_CODE("A sales person needs this permission in order to use a promotion code in a Sale. Works in tandem with the sales promotion code configuration which determines which promotion codes can be user by which roles"),
    REVERSE_SALE("Permission to do a sale reversal in SEP other than clearing bureau and airtime"),
    RESEND_SALE_TO_X3("Delete the sale from integration tables and resend it to X3 from scratch"),
    RESEND_CASHIN_TO_X3("Delete the cashin from integration tables and resend it to X3 from scratch"),
    REVERSE_SALE_CLEARING_BUREAU("Permission to reverse a sale made by clearing bureau"),
    REVERSE_SALE_AIRTIME("Permission to reverse a sale paid by airtime"),
    SALE_RETURNS("Permission needed in order to create a return on a sale."),
    SALE_RETURNS_REPLACEMENTS("Permission needed in order to create a return and replacement on a sale."),
    MAKE_SALE_TAX_EXEMPT("This permission allows a sale to be exempted from tax, it is activated by ticking the box 'Customer is Tax Exempt' when making a sale"),
    PROCESS_SHORT_CASH_IN("Permission needed to process short cash-ins when the 'CASH RECEIPTED DOES NOT EQUAL CASH REQUIRED'"),
    REGENERATE_INVOICE("Permission to allow regeneration of an invoice in SEP. This should never be needed under normal circumstances"),
    MAKE_SALE_CREDIT_ACCOUNT("Make a Credit Account sale"),
    MAKE_SALE_CONTRACT("Make a Contract sale"),
    MAKE_SALE_DELIVERY_SERVICE("Make a Delivery Service sale"),
    MODIFY_TILL_ID("Change the tillId (e.g. POS Bank name) of a sale after its been made"),
    MODIFY_PAYMENT_TRANSACTION_DATA("Change the payment transaction data (e.g. POS receipt number) of a sale after its been made"),
    OVERRIDE_CREDIT_ACCOUNT_NUMBER("Manually specify a credit account number to post a sale to when making a sale in SEP. Overrides the credit account number of the Organisation"),
    MAKE_SALE_CLEARING_BUREAU("Make a clearing bureau sale via SEP"),
    MAKE_SALE_PAYMENT_GATEWAY("Payment gateway sales for gateway testing from SEP"),
    UPLOAD_BANK_STATEMENT("Upload a bank statement for processing in X3"),
    SEND_DELIVERY_NOTE("Email a delivery note for a sale"),
    VIEW_SOLD_STOCK_LOCATION_ALL("Get which organisation is in posession of a serial number - allowed to look at all sold stock"),
    VIEW_SOLD_STOCK_LOCATION_MD("See stock sold to own Org Id"),
    VIEW_SOLD_STOCK_LOCATION_SD("See stock sold to own Org Id"),
    VIEW_SOLD_STOCK_LOCATION_ICP("See stock held by own Org Id"),
    EDIT_SOLD_STOCK_LOCATION_ALL("Specify which organisation is in posession of a serial number - can edit all sold stock"),
    EDIT_SOLD_STOCK_USED_AS_REPLACEMENT("Allow an ICP to speficy item was used as a replacement item"),
    EDIT_SOLD_STOCK_LOCATION_MD("Specify which organisation is in posession of a serial number - stock sold to own Org Id"),
    EDIT_SOLD_STOCK_LOCATION_SD("Specify which organisation is in posession of a serial number - stock sold to own Org Id"),
    EDIT_SOLD_STOCK_LOCATION_ICP("Only allow the transfer of stock within the same ICP organisation (old stock at Kaduna)."),
    CAMPAIGN_MANAGEMENT("Access to campaign management functionality"),
    CAMPAIGN_REFERREL("Access to Referrel Campaign"),
    PROVISION_INVALID_ICCID("Provision invalid ICCIDs such as those used by services and start with 00000000"),
    MAKE_SALE_AIRTIME_PAYMENT("Make Airtime sale"),
    SALE_TO_DIRECT_AIRTIME_ACCOUNT("Allow staff to sale airtime to Direct Airtime Account."),
    MAKE_DIRECT_AIRTIME_SALE("Make Direct Airtime Sale"),
    //Salesleads
    VIEW_SALESLEADS_FOR_ASSIGNEE("Allow a SEP user to view sales leads for another user in SEP"),
    // Workflow
    DELETE_PROCESS_DEFINITION("Delete the definition/configuration of a process"),
    DELETE_PROCESS_INSTANCE("Delete an instance of a running process"),
    KICK_OFF_PROCESS("Kick off an instance of a process/workflow"),
    EDIT_ORGANISATION_CONTRACT("Modify an organisation's contract"),
    EDIT_CUSTOMER_CONTRACT("Update a customers contract"),
    // MNP
    CREATE_PORTING_ORDER("Allow a SEP User to port a number into Smile."),
    DO_EMERGENCY_RESTORE("Allow a SEP User to undo a ported out number."),
    DO_NUMBER_RETURN("Allow SEP user to return a number to the range holder."),
    DO_RING_FENCE("Allow SEP user to return a number to the range holder."),
    // SRA
    SRA_RADIUS("Allow access to Radius AAA functionality on SRA"),
    RESET_NIRA_PASSWORD("Allow Uganda staff to reset NIRA password themselves."),
    REMOVE_SIM_UNBUNDLING_RESTRICTION("Allow staff to remove  SIM restriction on services of a product.");
    private final String description;

    Permissions(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return this.name();
    }
    

}
