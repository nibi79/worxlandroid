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
package org.openhab.binding.worxlandroid.internal.api.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidErrorCodes;
import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidStatusCodes;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link ProductItemStatus} class
 *
 * @author Gaël L'hopital - Initial contribution
 *
 */
public class ProductItemStatus {
    public class US {
        public int enabled;
        public String stat;
    }

    public class Ots {
        @SerializedName("wtm")
        public int duration = -1;
        private int bc = -1;

        public boolean getEdgeCut() {
            return bc == 1;
        }
    }

    public class Battery {
        @SerializedName("t")
        public double temp = -1;
        @SerializedName("v")
        public double voltage = -1;
        @SerializedName("p")
        public int level = -1;
        @SerializedName("nr")
        public int chargeCycle = -1;
        @SerializedName("c")
        public Boolean charging;
        public int m;
    }

    public class St {
        @SerializedName("b")
        public int totalBladeTime = -1;
        @SerializedName("d")
        public int totalDistance = -1;
        @SerializedName("wt")
        public int totalTime = -1;
        public int bl;
    }

    public class Rain {
        @SerializedName("s")
        public Boolean raining;
        @SerializedName("cnt")
        public int counter = -1;
    }

    public class Schedule {
        @SerializedName("m")
        public int scheduleMode = -1;
        @SerializedName("p")
        public int timeExtension = -1;
        public int distm;
        public Ots ots;
        public List<List<String>> d;
        public List<List<String>> dd;
    }

    public class Al {
        public int lvl;
        public int t;
    }

    public class Modules {
        @SerializedName("US")
        public US uS;
    }

    public class Features {
        public double autoLock;
        public double bluetoothControl;
        public boolean bluetoothPairing;
        public String chassis;
        public double digitalFenceSettings;
        public String displayType;
        public String inputType;
        public boolean lock;
        public boolean mqtt;
        public boolean multiZone;
        public boolean multiZonePercentage;
        public int multiZoneZones;
        public double oneTimeScheduler;
        public double pauseOverWire;
        public boolean rainDelay;
        public double rainDelayStart;
        public double safeGoHome;
        public double schedulerTwoSlots;
        public boolean unrestrictedMowingTime;
        public String wifiPairing;
    }

    public class Accessories {
        public boolean ultrasonic;
    }

    public class MqttTopics {
        public String commandIn;
        public String commandOut;
    }

    public class SetupLocation {
        public double latitude;
        public double longitude;
    }

    public class City {
        public int id;
        public int countryId;
        public String name;
        public double latitude;
        public double longitude;
        public String createdAt;
        public String updatedAt;
    }

    public class Dat {
        public String mac = "";
        public String fw = "";
        @SerializedName("bt")
        public Battery battery;
        @SerializedName("dmp")
        public double[] dataMotionProcessor = { -1, -1, -1 }; // pitch, roll, yaw
        public St st;
        @SerializedName("ls")
        public WorxLandroidStatusCodes statusCode = WorxLandroidStatusCodes.UNKNOWN;
        @SerializedName("le")
        public WorxLandroidErrorCodes errorCode = WorxLandroidErrorCodes.UNKNOWN;
        @SerializedName("lz")
        public int lastZone = -1;
        @SerializedName("rsi")
        public int wifiQuality;
        private int lk = -1;

        public int fwb;
        public String conn;
        public int act;

        public int tr;

        public Rain rain;
        public Modules modules;

        public boolean isLocked() {
            return lk == 1;
        }
    }

    public class Cfg {
        private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        public int id = -1;
        public String lg = ""; // en, fr...
        private String dt = ""; // "dt": "13/03/2020",
        private String tm = ""; // "tm": "17:09:34"
        public int cmd = -1;
        public Schedule sc;
        @SerializedName("mz")
        public List<Integer> multiZones = List.of();
        @SerializedName("mzv")
        public List<Integer> multizoneAllocations = List.of();
        @SerializedName("rd")
        public int rainDelay = -1;
        @SerializedName("sn")
        public String serialNumber = "";
        public int mzk;
        public Al al;
        public int tq;
        public Modules modules;

        public ZonedDateTime getDateTime(ZoneId zoneId) {
            return dt.isEmpty() || tm.isEmpty() ? null
                    : ZonedDateTime.of(LocalDateTime.parse("%s %s".formatted(dt, tm), formatter), zoneId);
        }
    }

    public class Payload {
        public Cfg cfg;
        public Dat dat;
    }

    public class LastStatus {
        public ZonedDateTime timestamp;
        public Payload payload;
    }

    public class AutoSchedule {
        public int boost;
        public String grassType;
        public boolean irrigation;
        public Map<String, String> nutrition;
        public String soilType;
    }

    public String id;
    public String uuid;
    public int productId;
    public int userId;
    public String serialNumber;
    public String macAddress;
    public String name;
    public boolean locked;
    public double firmwareVersion;
    public boolean firmwareAutoUpgrade;
    public boolean pushNotifications;
    public String sim;
    public String pushNotificationsLevel;
    public boolean test;
    public boolean iotRegistered;
    public boolean mqttRegistered;
    public String pinCode;
    public String registeredAt;
    public boolean online;
    public String mqttEndpoint;
    public String appSettings;
    public int protocol;
    public String pendingRadioLinkValidation;
    public List<String> capabilities;
    public List<String> capabilitiesAvailable;
    public Features features;
    public Accessories accessories;
    public MqttTopics mqttTopics;
    public boolean warrantyRegistered;
    public String purchasedAt;
    public String warrantyExpiresAt;
    public SetupLocation setupLocation;
    public City city;
    public ZoneId timeZone;
    public double lawnSize;
    public double lawnPerimeter;
    public AutoSchedule autoScheduleSettings;
    public boolean autoSchedule;
    public boolean improvement;
    public boolean diagnostic;
    public int distanceCovered;
    public int mowerWorkTime;
    public int bladeWorkTime;
    public int bladeWorkTimeReset;
    public ZonedDateTime bladeWorkTimeResetAt;
    public int batteryChargeCycles;
    public int batteryChargeCyclesReset;
    public ZonedDateTime batteryChargeCyclesResetAt;
    public ZonedDateTime createdAt;
    public ZonedDateTime updatedAt;
    public LastStatus lastStatus;
}
