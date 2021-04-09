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

import java.util.List;

import org.openhab.binding.asterisk.internal.AsteriskBindingConstants.Direction;
import org.openhab.binding.asterisk.internal.AsteriskBindingConstants.State;

/**
 * The {@link AsteriskChannelConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Eric de Boer - Initial contribution
 */
public class AsteriskChannelConfiguration {

    /**
     * Sample configuration parameter. Replace with your own.
     */
    public Direction direction;
    public List<State> state;
    public String extension;
    public String external;
}
