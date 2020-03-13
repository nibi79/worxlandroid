/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link WebApiException} is a class for handling the Worx Landroid API exceptions
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class WebApiException extends Exception {

    private static final long serialVersionUID = 1L;

    private static final int UNKNOWN = 0;

    private final int errorCode;
    private final String errorMsg;

    public WebApiException(int errorCode, String errorMsg, Throwable cause) {
        super(errorMsg, cause);
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public WebApiException(int errorCode, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public WebApiException(String errorMsg, Throwable cause) {
        super(errorMsg);
        this.errorCode = UNKNOWN;
        this.errorMsg = errorMsg;
    }

    public WebApiException(String errorMsg) {
        super(errorMsg);
        this.errorCode = UNKNOWN;
        this.errorMsg = errorMsg;
    }

    public WebApiException(Throwable cause) {
        super(cause.getMessage(), cause);
        this.errorCode = UNKNOWN;
        this.errorMsg = cause.getMessage();
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

}