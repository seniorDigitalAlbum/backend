// src/main/java/com/chimaenono/dearmind/auth/JwtTokenBlacklistService.java
package com.chimaenono.dearmind.auth;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/** 인메모리 블랙리스트 (운영은 Redis 권장) */
@Service
public class JwtTokenBlacklistService {

    // token -> expireAt(Unix epoch seconds)
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    public void blacklistToken(String token, long ttlSeconds) {
        long nowSec = System.currentTimeMillis() / 1000L;
        long expireAt = nowSec + Math.max(ttlSeconds, 0);
        blacklist.put(token, expireAt);
    }

    public boolean isTokenBlacklisted(String token) {
        Long exp = blacklist.get(token);
        if (exp == null) return false;
        long nowSec = System.currentTimeMillis() / 1000L;
        if (exp <= nowSec) { blacklist.remove(token); return false; }
        return true;
    }

    public void removeFromBlacklist(String token) { blacklist.remove(token); }

    public int getBlacklistSize() { return blacklist.size(); }
}
