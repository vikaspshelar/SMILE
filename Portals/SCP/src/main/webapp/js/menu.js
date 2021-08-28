function showProfileSubmenu(subMenuItem) {
    document.getElementById('sub_menu').style.display = "block";
    document.getElementById('help_sub_menu').style.display = "none";
    document.getElementById('recharge_sub_menu').style.display = "none";

    if (subMenuItem === 'DetailsPage') {
        document.getElementById("details_menu_active").className = "details_menu_active";

        document.getElementById("me2u_menu_active").className = "me2u_menu";
        document.getElementById("password_menu_active").className = "password_menu";
        document.getElementById("accounts_menu_active").className = "accounts_menu";
        document.getElementById("saleslead_referal_menu_active").className = "saleslead_referal_menu";
        document.getElementById("sim_verify_menu_active").className = "sim_verify_menu";
    }
    if (subMenuItem === 'AccountsPage') {
        document.getElementById("accounts_menu_active").className = "accounts_menu_active";

        document.getElementById("details_menu_active").className = "details_menu";
        document.getElementById("me2u_menu_active").className = "me2u_menu";
        document.getElementById("password_menu_active").className = "password_menu";
        document.getElementById("saleslead_referal_menu_active").className = "saleslead_referal_menu";
        document.getElementById("sim_verify_menu_active").className = "sim_verify_menu";

    }
    if (subMenuItem === 'ChangePasswordPage') {
        document.getElementById("password_menu_active").className = "password_menu_active";

        document.getElementById("details_menu_active").className = "details_menu";
        document.getElementById("me2u_menu_active").className = "me2u_menu";
        document.getElementById("accounts_menu_active").className = "accounts_menu";
        document.getElementById("saleslead_referal_menu_active").className = "saleslead_referal_menu";
        document.getElementById("sim_verify_menu_active").className = "sim_verify_menu";
    }
    if (subMenuItem === 'Me2UPage') {
        document.getElementById("me2u_menu_active").className = "me2u_menu_active";

        document.getElementById("details_menu_active").className = "details_menu";
        document.getElementById("password_menu_active").className = "password_menu";
        document.getElementById("accounts_menu_active").className = "accounts_menu";
        document.getElementById("saleslead_referal_menu_active").className = "saleslead_referal_menu";
        document.getElementById("sim_verify_menu_active").className = "sim_verify_menu";
    }
    if (subMenuItem === 'SalesleadReferalPage') {
        document.getElementById("saleslead_referal_menu_active").className = "saleslead_referal_menu_active";

        document.getElementById("details_menu_active").className = "details_menu";
        document.getElementById("password_menu_active").className = "password_menu";
        document.getElementById("accounts_menu_active").className = "accounts_menu";
        document.getElementById("me2u_menu_active").className = "me2u_menu";
        document.getElementById("sim_verify_menu_active").className = "sim_verify_menu";
    }
    if (subMenuItem === 'SimVerifyPage') {
        document.getElementById("sim_verify_menu_active").className = "sim_verify_menu_active";

        document.getElementById("details_menu_active").className = "details_menu";
        document.getElementById("password_menu_active").className = "password_menu";
        document.getElementById("accounts_menu_active").className = "accounts_menu";
        document.getElementById("me2u_menu_active").className = "me2u_menu";        
        document.getElementById("saleslead_referal_menu_active").className = "saleslead_referal_menu";
    }
}

function showHelpSubmenu(subMenuItem) {
    document.getElementById('help_sub_menu').style.display = "block";
    document.getElementById('sub_menu').style.display = "none";
    document.getElementById('recharge_sub_menu').style.display = "none";

    if (subMenuItem === 'FAQPage') {
        document.getElementById("faq_menu_active").className = "faq_menu_active";

        document.getElementById("troubletickets_menu_active").className = "troubletickets_menu";
        document.getElementById("customercare_menu_active").className = "customercare_menu";
        document.getElementById("supportdesk_menu_active").className = "supportdesk_menu";
    }
    if (subMenuItem === 'ContactUsPage') {
        document.getElementById("customercare_menu_active").className = "customercare_menu_active";

        document.getElementById("faq_menu_active").className = "faq_menu";
        document.getElementById("troubletickets_menu_active").className = "troubletickets_menu";
        document.getElementById("supportdesk_menu_active").className = "supportdesk_menu";
    }
    if (subMenuItem === 'SupportDeskPage' || subMenuItem === 'Help') {
        document.getElementById("supportdesk_menu_active").className = "supportdesk_menu_active";

        document.getElementById("faq_menu_active").className = "faq_menu";
        document.getElementById("troubletickets_menu_active").className = "troubletickets_menu";
        document.getElementById("customercare_menu_active").className = "customercare_menu";
    }
    if (subMenuItem === 'TroubleTicketsPage') {
        document.getElementById("troubletickets_menu_active").className = "troubletickets_menu_active";

        document.getElementById("faq_menu_active").className = "faq_menu";
        document.getElementById("customercare_menu_active").className = "customercare_menu";
        document.getElementById("supportdesk_menu_active").className = "supportdesk_menu";
    }
}

function showRechargeSubmenu(subMenuItem) {
    document.getElementById('recharge_sub_menu').style.display = "block";
    document.getElementById('help_sub_menu').style.display = "none";
    document.getElementById('sub_menu').style.display = "none";

    if (subMenuItem === 'VoucherPage') {
        document.getElementById("voucher_recharge_menu_active").className = "voucher_recharge_menu_active";
        document.getElementById("paymentgateway_recharge_menu_active").className = "paymentgateway_recharge_menu";
    }
    if (subMenuItem === 'AccountsPGWPage') {
        document.getElementById("paymentgateway_recharge_menu_active").className = "paymentgateway_recharge_menu_active";
        document.getElementById("voucher_recharge_menu_active").className = "voucher_recharge_menu";
    }
}

function makeMenuActive(menuName) {

    if (menuName === 'Transaction History') {
        document.getElementById("trans_menu_active").className = "trans_menu_active";

        document.getElementById("bundle_menu_active").className = "bundle_menu";
        document.getElementById("recharge_menu_active").className = "recharge_menu";
        document.getElementById("profile_menu_active").className = "profile_menu";
        document.getElementById("help_menu_active").className = "help_menu";
    }
    if (menuName === 'Buy Smile Bundle') {
        document.getElementById("bundle_menu_active").className = "bundle_menu_active";

        document.getElementById("trans_menu_active").className = "trans_menu";
        document.getElementById("recharge_menu_active").className = "recharge_menu";
        document.getElementById("profile_menu_active").className = "profile_menu";
        document.getElementById("help_menu_active").className = "help_menu";
    }

    if (menuName.indexOf('Help') === 0) {
        //Help_TroubleTicketsPage, Help_SupportDeskPagePage
        var subMenuItem = menuName.substring(menuName.indexOf('_') + 1);
        document.getElementById("help_menu_active").className = "help_menu_active";
        if (subMenuItem.length > 0) {
            showHelpSubmenu(subMenuItem);
        }

        document.getElementById("trans_menu_active").className = "trans_menu";
        document.getElementById("bundle_menu_active").className = "bundle_menu";
        document.getElementById("recharge_menu_active").className = "recharge_menu";
        document.getElementById("profile_menu_active").className = "profile_menu";
    }
    if (menuName.indexOf('Profile') === 0) {
        var subMenuItem = menuName.substring(menuName.indexOf('_') + 1);
        document.getElementById("profile_menu_active").className = "profile_menu_active";

        if (subMenuItem.length > 0) {
            showProfileSubmenu(subMenuItem);
        }
        document.getElementById("trans_menu_active").className = "trans_menu";
        document.getElementById("bundle_menu_active").className = "bundle_menu";
        document.getElementById("recharge_menu_active").className = "recharge_menu";
        document.getElementById("help_menu_active").className = "help_menu";
    }
    if (menuName.indexOf('Recharge') === 0) {
        var subMenuItem = menuName.substring(menuName.indexOf('_') + 1);
        document.getElementById("recharge_menu_active").className = "recharge_menu_active";
        if (subMenuItem.length > 0) {
            showRechargeSubmenu(subMenuItem);
        }
        document.getElementById("trans_menu_active").className = "trans_menu";
        document.getElementById("bundle_menu_active").className = "bundle_menu";
        document.getElementById("profile_menu_active").className = "profile_menu";
        document.getElementById("help_menu_active").className = "help_menu";
    }
    if (menuName === 'Verify Simcard(s)') {
        document.getElementById("sim_verify_menu_active").className = "sim_verify_menu_active";        
        
        document.getElementById("trans_menu_active").className = "trans_menu";
        document.getElementById("bundle_menu_active").className = "bundle_menu";
        document.getElementById("recharge_menu_active").className = "recharge_menu";
        document.getElementById("profile_menu_active").className = "profile_menu";
        document.getElementById("help_menu_active").className = "help_menu";
    }
    
}

function makeBreadCrumbMenuActive(menuName) {
    if (menuName === 'ConfirmTransaction') {
        document.getElementById("breadcrumb_one_active").className = "breadcrumb one active";

        document.getElementById("breadcrumb_four_active").className = "breadcrumb four";
    }
    if (menuName === 'TransactionSummary') {
        document.getElementById("breadcrumb_four_active").className = "breadcrumb four active";

        document.getElementById("breadcrumb_one_active").className = "breadcrumb one";
    }

}