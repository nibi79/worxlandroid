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
package org.openhab.binding.worxlandroid.internal.vo;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link ScheduledDay}
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class ScheduledDay {
    private static final int DURATION_0 = 0;
    private static final int DURATION_DEFAULT = 15;

    private boolean enable;
    private int hour;
    private int minutes;
    private int duration;
    private int durationRestore = DURATION_DEFAULT;
    private boolean edgecut;

    public int getHour() {
        return hour;
    }

    public void setHours(int hour) {
        this.hour = hour;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public int getDuration() {
        return duration;
    }

    /**
     * duration = 0 disables this scheduled day (enable=false).
     * duration > 0 enables this scheduled day (enable=true).
     *
     * @param duration
     */
    public void setDuration(int duration) {
        if (duration == DURATION_0) {
            storeDuration();
            this.enable = false;
        } else {
            this.enable = true;
        }

        this.duration = duration;
    }

    public boolean isEdgecut() {
        return edgecut;
    }

    public void setEdgecut(boolean edgecut) {
        this.edgecut = edgecut;
    }

    public boolean isEnable() {
        return enable;
    }

    /**
     * Enable/Disables scheduling using duration.
     *
     * @param enable
     */
    public void setEnable(boolean enable) {
        this.enable = enable;

        if (enable && duration == DURATION_0) {
            restoreDuration();
        } else {
            storeDuration();
            this.duration = DURATION_0;
        }
    }

    /**
     * Retores duration from durationRestore.
     */
    private void storeDuration() {
        if (this.duration > DURATION_0) {
            this.durationRestore = this.duration;
        }
    }

    /**
     * Stores duration to durationRestore for restore,
     */
    private void restoreDuration() {
        this.duration = this.durationRestore;
    }

    public Object[] getArray() {
        return new Object[] { "%d:%02d".formatted(hour, minutes), duration, edgecut ? 1 : 0 };
    }
}
