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

import static org.openhab.binding.asterisk.internal.AsteriskBindingConstants.State.INACTIVE;
import static org.openhab.binding.asterisk.internal.AsteriskBindingConstants.State.toState;

import org.asteriskjava.live.*;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.asterisk.internal.AsteriskBindingConstants.Direction;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringListType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.reflections.vfs.Vfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AsteriskHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Eric de Boer - Initial contribution
 */
@NonNullByDefault
public class AsteriskHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(AsteriskHandler.class);

    private @Nullable AsteriskServer asteriskServer;

    static {
        // Make sure the Reflections library knows how to handle OSGI BundleResources.
        Vfs.addDefaultURLTypes(new BundleResourceURLType());
    }

    public AsteriskHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // if (CHANNEL_1.equals(channelUID.getId())) {
        // if (command instanceof RefreshType) {
        // // TODO: handle data refresh
        // }
        //
        // // TODO: handle command
        //
        // // Note: if communication with thing fails for some reason,
        // // indicate that by setting the status with detail information:
        // // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        // // "Could not control device at IP address x.x.x.x");
        // }
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);
        AsteriskThingConfiguration config = getConfigAs(AsteriskThingConfiguration.class);

        scheduler.execute(() -> {
            asteriskServer = new DefaultAsteriskServer(config.hostname, config.port, config.user, config.secret);

            try {
                asteriskServer.initialize();
                updateStatus(ThingStatus.ONLINE);
            } catch (ManagerCommunicationException e) {
                // TODO update reason for failure
                logger.error("Error while logging in", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Can not access device as username and/or password are invalid");
            }

            thing.getChannels().forEach(this::createChannel);
        });
    }

    private void createChannel(Channel channel) {
        if (asteriskServer == null) {
            return;
        }
        ChannelUID channelUID = channel.getUID();
        AsteriskChannelConfiguration channelConfig = channel.getConfiguration().as(AsteriskChannelConfiguration.class);

        asteriskServer.addAsteriskServerListener(new AbstractAsteriskServerListener() {
            @NonNullByDefault({})
            @Override
            public void onNewAsteriskChannel(AsteriskChannel asteriskChannel) {
                asteriskChannel.addPropertyChangeListener("state", propertyChangeEvent -> {
                    CallerId extension = asteriskChannel.getCallerId();
                    if (channelConfig.extension != null && !channelConfig.extension.equals(extension.getNumber())) {
                        return;
                    }

                    CallerId external = new CallerId(null, null);

                    if (asteriskChannel.getDialingChannel() != null) {
                        external = asteriskChannel.getDialingChannel().getCallerId();
                        if (channelConfig.direction == Direction.OUTGOING) {
                            return;
                        }
                    } else if (asteriskChannel.getDialedChannel() != null) {
                        external = asteriskChannel.getDialedChannel().getCallerId();
                        if (channelConfig.direction == Direction.INCOMING) {
                            return;
                        }
                    }

                    if (channelConfig.external != null && !channelConfig.external.equals(extension.getNumber())) {
                        return;
                    }
                    ChannelState channelState = (ChannelState) propertyChangeEvent.getNewValue();

                    if (channelConfig.state != null) {
                        AsteriskBindingConstants.State state = toState(channelState);
                        if (!channelConfig.state.contains(state) && INACTIVE != state) {
                            return;
                        }
                    }

                    if (CoreItemFactory.SWITCH.equals(channel.getAcceptedItemType())) {
                        postCommand(channelUID, isActive(channelState) ? OnOffType.ON : OnOffType.OFF);
                    } else {
                        publishChannelIfLinked(channelUID, determineState(channel, channelState, extension, external));
                    }

                });
            }

            @Override
            public void onNewAgent(@Nullable AsteriskAgent agent) {
            }

            @Override
            public void onNewQueueEntry(@Nullable AsteriskQueueEntry entry) {
            }
        });
    }

    private State determineState(Channel channel, ChannelState state, CallerId extension, CallerId external) {

        @Nullable
        String acceptedItemType = channel.getAcceptedItemType();

        if (acceptedItemType == null) {
            logger.warn("Cannot determine item-type for channel '{}'", channel.getUID());
            return UnDefType.UNDEF;
        }

        switch (acceptedItemType) {
            case CoreItemFactory.CALL:
                return isActive(state) ? new StringListType(getValue(external), getValue(extension)) : UnDefType.UNDEF;
            case CoreItemFactory.SWITCH:
                return isActive(state) ? OnOffType.ON : OnOffType.OFF;
            case CoreItemFactory.NUMBER:
                return isActive(state) && external.getNumber() != null ? new DecimalType(external.getNumber())
                        : UnDefType.UNDEF;
            case CoreItemFactory.STRING:
                return isActive(state) ? new StringType(getValue(external)) : UnDefType.UNDEF;
            default:
                logger.warn("Unsupported item-type '{}'", acceptedItemType);
                return UnDefType.UNDEF;
        }
    }

    private boolean isActive(ChannelState state) {
        return !(state == ChannelState.DOWN || state == ChannelState.HUNGUP);
    }

    private String getValue(CallerId destination) {
        return destination.getName() != null ? destination.getName() : destination.getNumber();
    }

    private void publishChannelIfLinked(ChannelUID channelUID, State state) {
        if (isLinked(channelUID.getId())) {
            try {
                updateState(channelUID, state);
            } catch (Exception ex) {
                logger.error("Can't update state for channel {} : {}", channelUID, ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void dispose() {
        if (asteriskServer != null) {
            asteriskServer.shutdown();
        }
        super.dispose();
    }
}
