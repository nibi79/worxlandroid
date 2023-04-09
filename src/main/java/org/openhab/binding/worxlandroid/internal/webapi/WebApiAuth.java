/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.worxlandroid.internal.webapi;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * {@link WebApiAuth} authorization data dor Worx Ladroid API.
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class WebApiAuth {

    private String accessType;
    private String accessToken;
    private String refreshToken;
    private int expireIn;
    private @Nullable LocalDateTime expire;

    /**
     * @param accessType
     * @param accessToken
     * @param refreshToken
     * @param i
     */
    public WebApiAuth(String accessType, String accessToken, String refreshToken, int i) {
        this.accessType = accessType;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        setExpire(i);
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * @return
     */
    public int getExpireIn() {
        // TODO NB
        return expireIn;
    }

    public @Nullable LocalDateTime getExpireDate() {
        return expire;
    }

    public void setExpire(int expire) {
        // TODO NB
        this.expire = LocalDateTime.now().plusSeconds(expire - 120);
    }

    public boolean isTokenValid() {

        ChronoLocalDateTime<?> c = this.expire;

        if (c == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(c)) {
            return false;
        }

        return true;
    }

    /**
     * @return authorization string: 'Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ...'
     */
    public String getAuthorization() {
        return String.format("%s %s", accessType, accessToken);
    }
}
