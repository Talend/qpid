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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.apache.qpid.AMQException;
import org.apache.qpid.framing.ChannelOpenBody;
import org.apache.qpid.framing.ChannelOpenOkBody;
import org.apache.qpid.framing.MethodRegistry;
import org.apache.qpid.framing.ProtocolVersion;
import org.apache.qpid.framing.amqp_0_9.MethodRegistry_0_9;
import org.apache.qpid.framing.amqp_0_91.MethodRegistry_0_91;
import org.apache.qpid.framing.amqp_8_0.MethodRegistry_8_0;
import org.apache.qpid.protocol.AMQConstant;
import org.apache.qpid.server.protocol.v0_8.AMQChannel;
import org.apache.qpid.server.protocol.v0_8.AMQProtocolSession;
import org.apache.qpid.server.protocol.v0_8.state.StateAwareMethodListener;
import org.apache.qpid.server.util.ConnectionScopedRuntimeException;
import org.apache.qpid.server.virtualhost.VirtualHostImpl;

public class ChannelOpenHandler implements StateAwareMethodListener<ChannelOpenBody>
{
    private static final Logger _logger = Logger.getLogger(ChannelOpenHandler.class);

    private static ChannelOpenHandler _instance = new ChannelOpenHandler();

    public static ChannelOpenHandler getInstance()
    {
        return _instance;
    }

    private ChannelOpenHandler()
    {
    }

    public void methodReceived(final AMQProtocolSession<?> connection,
                               ChannelOpenBody body,
                               int channelId) throws AMQException
    {
        VirtualHostImpl virtualHost = connection.getVirtualHost();

        // Protect the broker against out of order frame request.
        if (virtualHost == null)
        {
            throw new AMQException(AMQConstant.COMMAND_INVALID, "Virtualhost has not yet been set. ConnectionOpen has not been called.", null);
        }
        _logger.info("Connecting to: " + virtualHost.getName());

        final AMQChannel channel = new AMQChannel(connection,channelId, virtualHost.getMessageStore());

        connection.addChannel(channel);

        ChannelOpenOkBody response;

        ProtocolVersion pv = connection.getProtocolVersion();

        if(pv.equals(ProtocolVersion.v8_0))
        {
            MethodRegistry_8_0 methodRegistry = (MethodRegistry_8_0) MethodRegistry.getMethodRegistry(ProtocolVersion.v8_0);
            response = methodRegistry.createChannelOpenOkBody();

        }
        else if(pv.equals(ProtocolVersion.v0_9))
        {
            MethodRegistry_0_9 methodRegistry = (MethodRegistry_0_9) MethodRegistry.getMethodRegistry(ProtocolVersion.v0_9);
            UUID uuid = UUID.randomUUID();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(output);
            try
            {
                dataOut.writeLong(uuid.getMostSignificantBits());
                dataOut.writeLong(uuid.getLeastSignificantBits());
                dataOut.flush();
                dataOut.close();
            }
            catch (IOException e)
            {
                // This *really* shouldn't happen as we're not doing any I/O
                throw new ConnectionScopedRuntimeException("I/O exception when writing to byte array", e);
            }

            // should really associate this channelId to the session
            byte[] channelName = output.toByteArray();

            response = methodRegistry.createChannelOpenOkBody(channelName);
        }
        else if(pv.equals(ProtocolVersion.v0_91))
        {
            MethodRegistry_0_91 methodRegistry = (MethodRegistry_0_91) MethodRegistry.getMethodRegistry(ProtocolVersion.v0_91);
            UUID uuid = UUID.randomUUID();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(output);
            try
            {
                dataOut.writeLong(uuid.getMostSignificantBits());
                dataOut.writeLong(uuid.getLeastSignificantBits());
                dataOut.flush();
                dataOut.close();
            }
            catch (IOException e)
            {
                // This *really* shouldn't happen as we're not doing any I/O
                throw new ConnectionScopedRuntimeException("I/O exception when writing to byte array", e);
            }

            // should really associate this channelId to the session
            byte[] channelName = output.toByteArray();

            response = methodRegistry.createChannelOpenOkBody(channelName);
        }
        else
        {
            throw new AMQException(AMQConstant.INTERNAL_ERROR, "Got channel open for protocol version not catered for: " + pv, null);
        }


        connection.writeFrame(response.generateFrame(channelId));
    }
}
