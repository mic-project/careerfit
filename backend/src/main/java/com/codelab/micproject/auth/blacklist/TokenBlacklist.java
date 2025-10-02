package com.codelab.micproject.auth.blacklist;

public interface TokenBlacklist {
    void blacklist(String jti, long secondsTTL);
    boolean isBlacklisted(String jti);
}
