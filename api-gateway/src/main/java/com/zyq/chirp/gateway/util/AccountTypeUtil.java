package com.zyq.chirp.gateway.util;

public class AccountTypeUtil {
    public static boolean isUsername(String username) {
        String regx = "[a-zA-Z0-9]+";
        return username.matches(regx);
    }

    public static boolean isEmail(String email) {
        String regx = "^[a-z0-9A-Z]+[- | a-z0-9A-Z . _]+@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-z]{2,}$";
        return email.matches(regx);
    }

    public static AccountType identifyType(String account) {
        if (isUsername(account)) {
            return AccountType.USERNAME;
        } else if (isEmail(account)) {
            return AccountType.EMAIL;
        } else {
            return AccountType.UNKNOWN;
        }
    }

}
