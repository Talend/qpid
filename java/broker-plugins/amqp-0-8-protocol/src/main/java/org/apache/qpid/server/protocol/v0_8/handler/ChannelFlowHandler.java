/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.server.protocol.v0_8.handler;

import org.apache.log4j.Logger;

import org.apache.qpid.AMQException;
import org.apache.qpid.framing.AMQMethodBody;
import org.apache.qpid.framing.ChannelFlowBody;
import org.apache.qpid.framing.MethodRegistry;
import org.apache.qpid.server.protocol.v0_8.AMQChannel;
import org.apache.qpid.server.protocol.v0_8.AMQProtocolSession;
import org.apache.qpid.server.protocol.v0_8.state.StateAwareMethodListener;

public class ChannelFlowHandler implements StateAwareMethodListener<ChannelFlowBody>
{
    private static final Logger _logger = Logger.getLogger(ChannelFlowHandler.class);

    private static ChannelFlowHandler _instance = new ChannelFlowHandler();

    public static ChannelFlowHandler getInstance()
    {
        return _instance;
    }

    private ChannelFlowHandler()
    {
    }

    public void methodReceived(final AMQProtocolSession<?> connection,
                               ChannelFlowBody body,
                               int channelId) throws AMQException
    {


        AMQChannel channel = connection.getChannel(channelId);

        if (channel == null)
        {
            throw body.getChannelNotFoundException(channelId, connection.getMethodRegistry());
        }
        channel.sync();
        channel.setSuspended(!body.getActive());
        _logger.debug("Channel.Flow for channel " + channelId + ", active=" + body.getActive());

        MethodRegistry methodRegistry = connection.getMethodRegistry();
        AMQMethodBody responseBody = methodRegistry.createChannelFlowOkBody(body.getActive());
        connection.writeFrame(responseBody.generateFrame(channelId));
    }
}
