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

/*
 * This file is auto-generated by Qpid Gentools v.0.1 - do not modify.
 * Supported AMQP version:
 *   0-91
 */

package org.apache.qpid.framing.amqp_0_91;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.qpid.codec.MarkableDataInput;
import org.apache.qpid.framing.*;
import org.apache.qpid.framing.abstraction.ProtocolVersionMethodConverter;
import org.apache.qpid.protocol.AMQConstant;


public class MethodRegistry_0_91 extends MethodRegistry
{

    private static final Logger _log = LoggerFactory.getLogger(MethodRegistry.class);

    private ProtocolVersionMethodConverter _protocolVersionConverter = new MethodConverter_0_91();

    private final AMQMethodBodyInstanceFactory[][] _factories = new AMQMethodBodyInstanceFactory[91][];

    public MethodRegistry_0_91()
    {
        this(new ProtocolVersion((byte)0,(byte)91));
    }

    public MethodRegistry_0_91(ProtocolVersion pv)
    {
        super(pv);



        // Register method body instance factories for the Connection class.

        _factories[10] = new AMQMethodBodyInstanceFactory[52];

        _factories[10][10] = ConnectionStartBodyImpl.getFactory();
        _factories[10][11] = ConnectionStartOkBodyImpl.getFactory();
        _factories[10][20] = ConnectionSecureBodyImpl.getFactory();
        _factories[10][21] = ConnectionSecureOkBodyImpl.getFactory();
        _factories[10][30] = ConnectionTuneBodyImpl.getFactory();
        _factories[10][31] = ConnectionTuneOkBodyImpl.getFactory();
        _factories[10][40] = ConnectionOpenBodyImpl.getFactory();
        _factories[10][41] = ConnectionOpenOkBodyImpl.getFactory();
        _factories[10][50] = ConnectionCloseBodyImpl.getFactory();
        _factories[10][51] = ConnectionCloseOkBodyImpl.getFactory();



        // Register method body instance factories for the Channel class.

        _factories[20] = new AMQMethodBodyInstanceFactory[42];

        _factories[20][10] = ChannelOpenBodyImpl.getFactory();
        _factories[20][11] = ChannelOpenOkBodyImpl.getFactory();
        _factories[20][20] = ChannelFlowBodyImpl.getFactory();
        _factories[20][21] = ChannelFlowOkBodyImpl.getFactory();
        _factories[20][40] = ChannelCloseBodyImpl.getFactory();
        _factories[20][41] = ChannelCloseOkBodyImpl.getFactory();



        // Register method body instance factories for the Exchange class.

        _factories[40] = new AMQMethodBodyInstanceFactory[24];

        _factories[40][10] = ExchangeDeclareBodyImpl.getFactory();
        _factories[40][11] = ExchangeDeclareOkBodyImpl.getFactory();
        _factories[40][20] = ExchangeDeleteBodyImpl.getFactory();
        _factories[40][21] = ExchangeDeleteOkBodyImpl.getFactory();
        _factories[40][22] = ExchangeBoundBodyImpl.getFactory();
        _factories[40][23] = ExchangeBoundOkBodyImpl.getFactory();



        // Register method body instance factories for the Queue class.

        _factories[50] = new AMQMethodBodyInstanceFactory[52];

        _factories[50][10] = QueueDeclareBodyImpl.getFactory();
        _factories[50][11] = QueueDeclareOkBodyImpl.getFactory();
        _factories[50][20] = QueueBindBodyImpl.getFactory();
        _factories[50][21] = QueueBindOkBodyImpl.getFactory();
        _factories[50][30] = QueuePurgeBodyImpl.getFactory();
        _factories[50][31] = QueuePurgeOkBodyImpl.getFactory();
        _factories[50][40] = QueueDeleteBodyImpl.getFactory();
        _factories[50][41] = QueueDeleteOkBodyImpl.getFactory();
        _factories[50][50] = QueueUnbindBodyImpl.getFactory();
        _factories[50][51] = QueueUnbindOkBodyImpl.getFactory();



        // Register method body instance factories for the Basic class.

        _factories[60] = new AMQMethodBodyInstanceFactory[112];

        _factories[60][10] = BasicQosBodyImpl.getFactory();
        _factories[60][11] = BasicQosOkBodyImpl.getFactory();
        _factories[60][20] = BasicConsumeBodyImpl.getFactory();
        _factories[60][21] = BasicConsumeOkBodyImpl.getFactory();
        _factories[60][30] = BasicCancelBodyImpl.getFactory();
        _factories[60][31] = BasicCancelOkBodyImpl.getFactory();
        _factories[60][40] = BasicPublishBodyImpl.getFactory();
        _factories[60][50] = BasicReturnBodyImpl.getFactory();
        _factories[60][60] = BasicDeliverBodyImpl.getFactory();
        _factories[60][70] = BasicGetBodyImpl.getFactory();
        _factories[60][71] = BasicGetOkBodyImpl.getFactory();
        _factories[60][72] = BasicGetEmptyBodyImpl.getFactory();
        _factories[60][80] = BasicAckBodyImpl.getFactory();
        _factories[60][90] = BasicRejectBodyImpl.getFactory();
        _factories[60][100] = BasicRecoverBodyImpl.getFactory();
        _factories[60][110] = BasicRecoverSyncBodyImpl.getFactory();
        _factories[60][111] = BasicRecoverSyncOkBodyImpl.getFactory();



        // Register method body instance factories for the Tx class.

        _factories[90] = new AMQMethodBodyInstanceFactory[32];

        _factories[90][10] = TxSelectBodyImpl.getFactory();
        _factories[90][11] = TxSelectOkBodyImpl.getFactory();
        _factories[90][20] = TxCommitBodyImpl.getFactory();
        _factories[90][21] = TxCommitOkBodyImpl.getFactory();
        _factories[90][30] = TxRollbackBodyImpl.getFactory();
        _factories[90][31] = TxRollbackOkBodyImpl.getFactory();
    }

    public AMQMethodBody convertToBody(MarkableDataInput in, long size)
        throws AMQFrameDecodingException, IOException
    {
        int classId = in.readUnsignedShort();
        int methodId = in.readUnsignedShort();

        AMQMethodBodyInstanceFactory bodyFactory;
        try
        {
            bodyFactory = _factories[classId][methodId];
        }
        catch(NullPointerException e)
        {
            throw new AMQFrameDecodingException(AMQConstant.COMMAND_INVALID,
                "Class " + classId + " unknown in AMQP version 0-91"
                 + " (while trying to decode class " + classId + " method " + methodId + ".");
        }
        catch(IndexOutOfBoundsException e)
        {
            if(classId >= _factories.length)
            {
                throw new AMQFrameDecodingException(AMQConstant.COMMAND_INVALID,
                    "Class " + classId + " unknown in AMQP version 0-91"
                     + " (while trying to decode class " + classId + " method " + methodId + ".");

            }
            else
            {
                throw new AMQFrameDecodingException(AMQConstant.COMMAND_INVALID,
                    "Method " + methodId + " unknown in AMQP version 0-91"
                     + " (while trying to decode class " + classId + " method " + methodId + ".");

            }
        }

        if (bodyFactory == null)
        {
            throw new AMQFrameDecodingException(AMQConstant.COMMAND_INVALID,
                "Method " + methodId + " unknown in AMQP version 0-91"
                 + " (while trying to decode class " + classId + " method " + methodId + ".");
        }

        return bodyFactory.newInstance(in, size);
    }

    public int getMaxClassId()
    {
        return 90;
    }

    public int getMaxMethodId(int classId)
    {
        return _factories[classId].length - 1;
    }



    public ConnectionStartBody createConnectionStartBody(
                                final short versionMajor,
                                final short versionMinor,
                                final FieldTable serverProperties,
                                final byte[] mechanisms,
                                final byte[] locales
                                )
    {
        return new ConnectionStartBodyImpl(
                                versionMajor,
                                versionMinor,
                                serverProperties,
                                mechanisms,
                                locales
                                );
    }

    public ConnectionStartOkBody createConnectionStartOkBody(
                                final FieldTable clientProperties,
                                final AMQShortString mechanism,
                                final byte[] response,
                                final AMQShortString locale
                                )
    {
        return new ConnectionStartOkBodyImpl(
                                clientProperties,
                                mechanism,
                                response,
                                locale
                                );
    }

    public ConnectionSecureBody createConnectionSecureBody(
                                final byte[] challenge
                                )
    {
        return new ConnectionSecureBodyImpl(
                                challenge
                                );
    }

    public ConnectionSecureOkBody createConnectionSecureOkBody(
                                final byte[] response
                                )
    {
        return new ConnectionSecureOkBodyImpl(
                                response
                                );
    }

    public ConnectionTuneBody createConnectionTuneBody(
                                final int channelMax,
                                final long frameMax,
                                final int heartbeat
                                )
    {
        return new ConnectionTuneBodyImpl(
                                channelMax,
                                frameMax,
                                heartbeat
                                );
    }

    public ConnectionTuneOkBody createConnectionTuneOkBody(
                                final int channelMax,
                                final long frameMax,
                                final int heartbeat
                                )
    {
        return new ConnectionTuneOkBodyImpl(
                                channelMax,
                                frameMax,
                                heartbeat
                                );
    }

    public ConnectionOpenBody createConnectionOpenBody(
                                final AMQShortString virtualHost,
                                final AMQShortString capabilities,
                                final boolean insist
                                )
    {
        return new ConnectionOpenBodyImpl(
                                virtualHost,
                                capabilities,
                                insist
                                );
    }

    public ConnectionOpenOkBody createConnectionOpenOkBody(
                                final AMQShortString knownHosts
                                )
    {
        return new ConnectionOpenOkBodyImpl(
                                knownHosts
                                );
    }

    public ConnectionCloseBody createConnectionCloseBody(
                                final int replyCode,
                                final AMQShortString replyText,
                                final int classId,
                                final int methodId
                                )
    {
        return new ConnectionCloseBodyImpl(
                                replyCode,
                                replyText,
                                classId,
                                methodId
                                );
    }

    public ConnectionCloseOkBody createConnectionCloseOkBody(
                                )
    {
        return new ConnectionCloseOkBodyImpl(
                                );
    }




    public ChannelOpenBody createChannelOpenBody(
                                final AMQShortString outOfBand
                                )
    {
        return new ChannelOpenBodyImpl(
                                outOfBand
                                );
    }

    public ChannelOpenOkBody createChannelOpenOkBody(
                                final byte[] channelId
                                )
    {
        return new ChannelOpenOkBodyImpl(
                                channelId
                                );
    }

    public ChannelFlowBody createChannelFlowBody(
                                final boolean active
                                )
    {
        return new ChannelFlowBodyImpl(
                                active
                                );
    }

    public ChannelFlowOkBody createChannelFlowOkBody(
                                final boolean active
                                )
    {
        return new ChannelFlowOkBodyImpl(
                                active
                                );
    }

    public ChannelCloseBody createChannelCloseBody(
                                final int replyCode,
                                final AMQShortString replyText,
                                final int classId,
                                final int methodId
                                )
    {
        return new ChannelCloseBodyImpl(
                                replyCode,
                                replyText,
                                classId,
                                methodId
                                );
    }

    public ChannelCloseOkBody createChannelCloseOkBody(
                                )
    {
        return new ChannelCloseOkBodyImpl(
                                );
    }




    public ExchangeDeclareBody createExchangeDeclareBody(
                                final int ticket,
                                final AMQShortString exchange,
                                final AMQShortString type,
                                final boolean passive,
                                final boolean durable,
                                final boolean autoDelete,
                                final boolean internal,
                                final boolean nowait,
                                final FieldTable arguments
                                )
    {
        return new ExchangeDeclareBodyImpl(
                                ticket,
                                exchange,
                                type,
                                passive,
                                durable,
                                autoDelete,
                                internal,
                                nowait,
                                arguments
                                );
    }

    public ExchangeDeclareOkBody createExchangeDeclareOkBody(
                                )
    {
        return new ExchangeDeclareOkBodyImpl(
                                );
    }

    public ExchangeDeleteBody createExchangeDeleteBody(
                                final int ticket,
                                final AMQShortString exchange,
                                final boolean ifUnused,
                                final boolean nowait
                                )
    {
        return new ExchangeDeleteBodyImpl(
                                ticket,
                                exchange,
                                ifUnused,
                                nowait
                                );
    }

    public ExchangeDeleteOkBody createExchangeDeleteOkBody(
                                )
    {
        return new ExchangeDeleteOkBodyImpl(
                                );
    }

    public ExchangeBoundBody createExchangeBoundBody(
                                final AMQShortString exchange,
                                final AMQShortString routingKey,
                                final AMQShortString queue
                                )
    {
        return new ExchangeBoundBodyImpl(
                                exchange,
                                routingKey,
                                queue
                                );
    }

    public ExchangeBoundOkBody createExchangeBoundOkBody(
                                final int replyCode,
                                final AMQShortString replyText
                                )
    {
        return new ExchangeBoundOkBodyImpl(
                                replyCode,
                                replyText
                                );
    }




    public QueueDeclareBody createQueueDeclareBody(
                                final int ticket,
                                final AMQShortString queue,
                                final boolean passive,
                                final boolean durable,
                                final boolean exclusive,
                                final boolean autoDelete,
                                final boolean nowait,
                                final FieldTable arguments
                                )
    {
        return new QueueDeclareBodyImpl(
                                ticket,
                                queue,
                                passive,
                                durable,
                                exclusive,
                                autoDelete,
                                nowait,
                                arguments
                                );
    }

    public QueueDeclareOkBody createQueueDeclareOkBody(
                                final AMQShortString queue,
                                final long messageCount,
                                final long consumerCount
                                )
    {
        return new QueueDeclareOkBodyImpl(
                                queue,
                                messageCount,
                                consumerCount
                                );
    }

    public QueueBindBody createQueueBindBody(
                                final int ticket,
                                final AMQShortString queue,
                                final AMQShortString exchange,
                                final AMQShortString routingKey,
                                final boolean nowait,
                                final FieldTable arguments
                                )
    {
        return new QueueBindBodyImpl(
                                ticket,
                                queue,
                                exchange,
                                routingKey,
                                nowait,
                                arguments
                                );
    }

    public QueueBindOkBody createQueueBindOkBody(
                                )
    {
        return new QueueBindOkBodyImpl(
                                );
    }

    public QueuePurgeBody createQueuePurgeBody(
                                final int ticket,
                                final AMQShortString queue,
                                final boolean nowait
                                )
    {
        return new QueuePurgeBodyImpl(
                                ticket,
                                queue,
                                nowait
                                );
    }

    public QueuePurgeOkBody createQueuePurgeOkBody(
                                final long messageCount
                                )
    {
        return new QueuePurgeOkBodyImpl(
                                messageCount
                                );
    }

    public QueueDeleteBody createQueueDeleteBody(
                                final int ticket,
                                final AMQShortString queue,
                                final boolean ifUnused,
                                final boolean ifEmpty,
                                final boolean nowait
                                )
    {
        return new QueueDeleteBodyImpl(
                                ticket,
                                queue,
                                ifUnused,
                                ifEmpty,
                                nowait
                                );
    }

    public QueueDeleteOkBody createQueueDeleteOkBody(
                                final long messageCount
                                )
    {
        return new QueueDeleteOkBodyImpl(
                                messageCount
                                );
    }

    public QueueUnbindBody createQueueUnbindBody(
                                final int ticket,
                                final AMQShortString queue,
                                final AMQShortString exchange,
                                final AMQShortString routingKey,
                                final FieldTable arguments
                                )
    {
        return new QueueUnbindBodyImpl(
                                ticket,
                                queue,
                                exchange,
                                routingKey,
                                arguments
                                );
    }

    public QueueUnbindOkBody createQueueUnbindOkBody(
                                )
    {
        return new QueueUnbindOkBodyImpl(
                                );
    }




    public BasicQosBody createBasicQosBody(
                                final long prefetchSize,
                                final int prefetchCount,
                                final boolean global
                                )
    {
        return new BasicQosBodyImpl(
                                prefetchSize,
                                prefetchCount,
                                global
                                );
    }

    public BasicQosOkBody createBasicQosOkBody(
                                )
    {
        return new BasicQosOkBodyImpl(
                                );
    }

    public BasicConsumeBody createBasicConsumeBody(
                                final int ticket,
                                final AMQShortString queue,
                                final AMQShortString consumerTag,
                                final boolean noLocal,
                                final boolean noAck,
                                final boolean exclusive,
                                final boolean nowait,
                                final FieldTable arguments
                                )
    {
        return new BasicConsumeBodyImpl(
                                ticket,
                                queue,
                                consumerTag,
                                noLocal,
                                noAck,
                                exclusive,
                                nowait,
                                arguments
                                );
    }

    public BasicConsumeOkBody createBasicConsumeOkBody(
                                final AMQShortString consumerTag
                                )
    {
        return new BasicConsumeOkBodyImpl(
                                consumerTag
                                );
    }

    public BasicCancelBody createBasicCancelBody(
                                final AMQShortString consumerTag,
                                final boolean nowait
                                )
    {
        return new BasicCancelBodyImpl(
                                consumerTag,
                                nowait
                                );
    }

    public BasicCancelOkBody createBasicCancelOkBody(
                                final AMQShortString consumerTag
                                )
    {
        return new BasicCancelOkBodyImpl(
                                consumerTag
                                );
    }

    public BasicPublishBody createBasicPublishBody(
                                final int ticket,
                                final AMQShortString exchange,
                                final AMQShortString routingKey,
                                final boolean mandatory,
                                final boolean immediate
                                )
    {
        return new BasicPublishBodyImpl(
                                ticket,
                                exchange,
                                routingKey,
                                mandatory,
                                immediate
                                );
    }

    public BasicReturnBody createBasicReturnBody(
                                final int replyCode,
                                final AMQShortString replyText,
                                final AMQShortString exchange,
                                final AMQShortString routingKey
                                )
    {
        return new BasicReturnBodyImpl(
                                replyCode,
                                replyText,
                                exchange,
                                routingKey
                                );
    }

    public BasicDeliverBody createBasicDeliverBody(
                                final AMQShortString consumerTag,
                                final long deliveryTag,
                                final boolean redelivered,
                                final AMQShortString exchange,
                                final AMQShortString routingKey
                                )
    {
        return new BasicDeliverBodyImpl(
                                consumerTag,
                                deliveryTag,
                                redelivered,
                                exchange,
                                routingKey
                                );
    }

    public BasicGetBody createBasicGetBody(
                                final int ticket,
                                final AMQShortString queue,
                                final boolean noAck
                                )
    {
        return new BasicGetBodyImpl(
                                ticket,
                                queue,
                                noAck
                                );
    }

    public BasicGetOkBody createBasicGetOkBody(
                                final long deliveryTag,
                                final boolean redelivered,
                                final AMQShortString exchange,
                                final AMQShortString routingKey,
                                final long messageCount
                                )
    {
        return new BasicGetOkBodyImpl(
                                deliveryTag,
                                redelivered,
                                exchange,
                                routingKey,
                                messageCount
                                );
    }

    public BasicGetEmptyBody createBasicGetEmptyBody(
                                final AMQShortString clusterId
                                )
    {
        return new BasicGetEmptyBodyImpl(
                                clusterId
                                );
    }

    public BasicAckBody createBasicAckBody(
                                final long deliveryTag,
                                final boolean multiple
                                )
    {
        return new BasicAckBodyImpl(
                                deliveryTag,
                                multiple
                                );
    }

    public BasicRejectBody createBasicRejectBody(
                                final long deliveryTag,
                                final boolean requeue
                                )
    {
        return new BasicRejectBodyImpl(
                                deliveryTag,
                                requeue
                                );
    }

    public BasicRecoverBody createBasicRecoverBody(
                                final boolean requeue
                                )
    {
        return new BasicRecoverBodyImpl(
                                requeue
                                );
    }

    public BasicRecoverSyncBody createBasicRecoverSyncBody(
                                final boolean requeue
                                )
    {
        return new BasicRecoverSyncBodyImpl(
                                requeue
                                );
    }

    public BasicRecoverSyncOkBody createBasicRecoverSyncOkBody(
                                )
    {
        return new BasicRecoverSyncOkBodyImpl(
                                );
    }




    public TxSelectBody createTxSelectBody(
                                )
    {
        return new TxSelectBodyImpl(
                                );
    }

    public TxSelectOkBody createTxSelectOkBody(
                                )
    {
        return new TxSelectOkBodyImpl(
                                );
    }

    public TxCommitBody createTxCommitBody(
                                )
    {
        return new TxCommitBodyImpl(
                                );
    }

    public TxCommitOkBody createTxCommitOkBody(
                                )
    {
        return new TxCommitOkBodyImpl(
                                );
    }

    public TxRollbackBody createTxRollbackBody(
                                )
    {
        return new TxRollbackBodyImpl(
                                );
    }

    public TxRollbackOkBody createTxRollbackOkBody(
                                )
    {
        return new TxRollbackOkBodyImpl(
                                );
    }



    public ProtocolVersionMethodConverter getProtocolVersionMethodConverter()
    {
        return _protocolVersionConverter;
    }

}
