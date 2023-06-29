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
package org.openhab.binding.worxlandroid.internal;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.net.http.HttpUtil;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.RawType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * This class holds various channel values conversion methods
 *
 * @author Gaël L'hopital - Initial contribution
 *
 */
@NonNullByDefault
public class ChannelTypeUtils {

    public static State toStringType(String pattern, Object... parms) {
        return toStringType(String.format(pattern, parms));
    }

    public static State toStringType(@Nullable Enum<?> value) {
        return (value == null) ? UnDefType.NULL : new StringType(value.name());
    }

    public static State toStringType(@Nullable String value) {
        return (value == null) ? UnDefType.NULL : new StringType(value);
    }

    public static State toDateTimeType(@Nullable ZonedDateTime zonedDateTime) {
        return (zonedDateTime == null) ? UnDefType.NULL : new DateTimeType(zonedDateTime);
    }

    public static State toDateTimeType(Optional<ZonedDateTime> zonedDateTime) {
        return zonedDateTime.map(zdt -> (State) new DateTimeType(zdt)).orElse(UnDefType.NULL);
    }

    public static State toQuantityType(@Nullable Number value, Unit<?> unit) {
        return value == null ? UnDefType.NULL : new QuantityType<>(value, unit);
    }

    public static State toRawType(@Nullable String pictureUrl) {
        if (pictureUrl != null) {
            RawType picture = HttpUtil.downloadImage(pictureUrl);
            if (picture != null) {
                return picture;
            }
        }
        return UnDefType.NULL;
    }
}
