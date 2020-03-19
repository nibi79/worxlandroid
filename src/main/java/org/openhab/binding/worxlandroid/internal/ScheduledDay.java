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
package org.openhab.binding.worxlandroid.internal;

/**
 * {@link ScheduledDay}
 *
 * @author Nils - Initial contribution
 */
public class ScheduledDay {

    private int hour;
    private int minutes;
    private int duration;
    private boolean edgecut;

    /**
     *
     */
    public ScheduledDay() {
        super();
    }

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

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isEdgecut() {
        return edgecut;
    }

    public void setEdgecut(boolean edgecut) {
        this.edgecut = edgecut;
    }

}
