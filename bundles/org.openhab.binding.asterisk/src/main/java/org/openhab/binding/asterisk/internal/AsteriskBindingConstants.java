/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.asterisk.internal;

import org.asteriskjava.live.ChannelState;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link AsteriskBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Eric de Boer - Initial contribution
 */
@NonNullByDefault
public class AsteriskBindingConstants {

    private static final String BINDING_ID = "asterisk";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_ASTERISK = new ThingTypeUID(BINDING_ID, "asterisk");

    public enum Direction {
        INCOMING,
        OUTGOING
    }

    public enum State {
        RINGING,
        DIALING,
        ACTIVE,
        INACTIVE;

        public static State toState(ChannelState channelState) {
            switch (channelState) {
                case DIALING:
                case RING:
                    return DIALING;
                case RINGING:
                    return RINGING;
                case UP:
                    return ACTIVE;
                case DOWN:
                case RSRVD:
                case OFFHOOK:
                case BUSY:
                case DIALING_OFFHOOK:
                case PRERING:
                case HUNGUP:
                    return INACTIVE;
            }
            return INACTIVE;
        }
    }
}
