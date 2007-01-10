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
using System;
using System.Collections;
using System.Text.RegularExpressions;
using System.Threading;
using log4net;
using Qpid.Buffer;
using Qpid.Client.Message;
using Qpid.Collections;
using Qpid.Framing;
using Qpid.Messaging;

namespace Qpid.Client
{
    public class AmqChannel : Closeable, IChannel
    {
        private const int BASIC_CONTENT_TYPE = 60;

        private static readonly ILog _logger = LogManager.GetLogger(typeof (AmqChannel));

        private static int _nextSessionNumber = 0;

        private int _sessionNumber;
        
        // Used in the consume method. We generate the consume tag on the client so that we can use the nowait feature.
        private int _nextConsumerNumber = 1;

        internal const int DEFAULT_PREFETCH = MessageConsumerBuilder.DEFAULT_PREFETCH_HIGH;

        private AMQConnection _connection;

        private bool _transacted;

        private AcknowledgeMode _acknowledgeMode;

        private ushort _channelId;

        private int _defaultPrefetch = DEFAULT_PREFETCH;

        private BlockingQueue _queue = new LinkedBlockingQueue();

        private Dispatcher _dispatcher;

        private MessageFactoryRegistry _messageFactoryRegistry;

        /// <summary>
        /// Set of all producers created by this session
        /// </summary>
        private Hashtable _producers = Hashtable.Synchronized(new Hashtable());

        /// <summary>
        /// Maps from consumer tag to JMSMessageConsumer instance
        /// </summary>
        private Hashtable _consumers = Hashtable.Synchronized(new Hashtable());

        private ArrayList _replayFrames = new ArrayList();

        /// <summary>
        /// The counter of the _next producer id. This id is generated by the session and used only to allow the
        /// producer to identify itself to the session when deregistering itself.
        ///
        /// Access to this id does not require to be synchronized since according to the JMS specification only one
        /// thread of control is allowed to create producers for any given session instance.
        /// </summary>
        private long _nextProducerId;

        /// <summary>
        /// Responsible for decoding a message fragment and passing it to the appropriate message consumer.
        /// </summary>
        private class Dispatcher
        {
            private int _stopped = 0;

            private AmqChannel _containingChannel;
            
            public Dispatcher(AmqChannel containingChannel)
            {
                _containingChannel = containingChannel;
            }
            
            /// <summary>
            /// Runs the dispatcher. This is intended to be Run in a separate thread.
            /// </summary>
            public void RunDispatcher()
            {
                UnprocessedMessage message;

                while (_stopped == 0 && (message = (UnprocessedMessage)_containingChannel._queue.DequeueBlocking()) != null)
                {
                    //_queue.size()
                    DispatchMessage(message);
                }                

                _logger.Info("Dispatcher thread terminating for channel " + _containingChannel._channelId);
            }

            private void DispatchMessage(UnprocessedMessage message)
            {
                if (message.DeliverBody != null)
                {
                    BasicMessageConsumer consumer = (BasicMessageConsumer) _containingChannel._consumers[message.DeliverBody.ConsumerTag];

                    if (consumer == null)
                    {
                        _logger.Warn("Received a message from queue " + message.DeliverBody.ConsumerTag + " without a handler - ignoring...");
                    }
                    else
                    {
                        consumer.NotifyMessage(message, _containingChannel.ChannelId);
                    }
                }
                else
                {
                    try
                    {
                        // Bounced message is processed here, away from the mina thread
                        AbstractQmsMessage bouncedMessage = _containingChannel._messageFactoryRegistry.
                            CreateMessage(0, false, message.ContentHeader, message.Bodies);

                        int errorCode = message.BounceBody.ReplyCode;
                        string reason = message.BounceBody.ReplyText;
                        _logger.Debug("Message returned with error code " + errorCode + " (" + reason + ")");

                        _containingChannel._connection.ExceptionReceived(new AMQUndeliveredException(errorCode, "Error: " + reason, bouncedMessage));
                    }
                    catch (Exception e)
                    {
                        _logger.Error("Caught exception trying to raise undelivered message exception (dump follows) - ignoring...", e);
                    }
                }
            }

            public void StopDispatcher()
            {
                Interlocked.Exchange(ref _stopped, 1);                
            }
        }

        internal AmqChannel(AMQConnection con, ushort channelId, bool transacted, AcknowledgeMode acknowledgeMode, int defaultPrefetch) :
            this(con, channelId, transacted, acknowledgeMode, MessageFactoryRegistry.NewDefaultRegistry(), defaultPrefetch)
        {
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="AmqChannel"/> class.
        /// </summary>
        /// <param name="con">The con.</param>
        /// <param name="channelId">The channel id.</param>
        /// <param name="transacted">if set to <c>true</c> [transacted].</param>
        /// <param name="acknowledgeMode">The acknowledge mode.</param>
        /// <param name="messageFactoryRegistry">The message factory registry.</param>
        internal AmqChannel(AMQConnection con, ushort channelId, bool transacted, AcknowledgeMode acknowledgeMode,
                            MessageFactoryRegistry messageFactoryRegistry, int defaultPrefetch)
        {
            _sessionNumber = Interlocked.Increment(ref _nextSessionNumber);
            _connection = con;
            _transacted = transacted;
            if (transacted)
            {
                _acknowledgeMode = AcknowledgeMode.SessionTransacted;
            }
            else
            {
                _acknowledgeMode = acknowledgeMode;
            }
            _channelId = channelId;
            _messageFactoryRegistry = messageFactoryRegistry;
        }

        public IBytesMessage CreateBytesMessage()
        {
            lock (_connection.FailoverMutex)
            {
                CheckNotClosed();
                try
                {
                    return (IBytesMessage)_messageFactoryRegistry.CreateMessage("application/octet-stream");
                }
                catch (AMQException e)
                {
                    throw new QpidException("Unable to create message: " + e);
                }
            }
        }

        public IMessage CreateMessage()
        {
            lock (_connection.FailoverMutex)
            {
                CheckNotClosed();
                try
                {
                    // TODO: this is supposed to create a message consisting only of message headers
                    return (IBytesMessage)_messageFactoryRegistry.CreateMessage("application/octet-stream");
                }
                catch (AMQException e)
                {
                    throw new QpidException("Unable to create message: " + e);
                }
            }
        }

        public ITextMessage CreateTextMessage()
        {
            lock (_connection.FailoverMutex)
            {
                CheckNotClosed();

                try
                {
                    return (ITextMessage)_messageFactoryRegistry.CreateMessage("text/plain");
                }
                catch (AMQException e)
                {
                    throw new QpidException("Unable to create message: " + e);
                }
            }
        }

        public ITextMessage CreateTextMessage(string text)
        {
            lock (_connection.FailoverMutex)
            {
                CheckNotClosed();
                try
                {
                    ITextMessage msg = (ITextMessage)_messageFactoryRegistry.CreateMessage("text/plain");
                    msg.Text = text;
                    return msg;
                }
                catch (AMQException e)
                {
                    throw new QpidException("Unable to create message: " + e);
                }
            }
        }

        public bool Transacted
        {
            get
            {
                CheckNotClosed();
                return _transacted;
            }
        }

        public AcknowledgeMode AcknowledgeMode
        {
            get
            {
                CheckNotClosed();
                return _acknowledgeMode;
            }
        }

        public void Commit()
        {
            // FIXME: Fail over safety. Needs FailoverSupport?
            CheckNotClosed();
            CheckTransacted(); // throws IllegalOperationException if not a transacted session

            try
            {
                // Acknowledge up to message last delivered (if any) for each consumer.
                // Need to send ack for messages delivered to consumers so far.
                foreach (BasicMessageConsumer consumer  in _consumers.Values)
                {
                    // Sends acknowledgement to server.
                    consumer.AcknowledgeLastDelivered();
                }

                // Commits outstanding messages sent and outstanding acknowledgements.
                _connection.ConvenientProtocolWriter.SyncWrite(TxCommitBody.CreateAMQFrame(_channelId), typeof(TxCommitOkBody));
            }
            catch (AMQException e)
            {
                throw new QpidException("Failed to commit", e);
            }
        }

        public void Rollback()
        {
            // FIXME: Fail over safety. Needs FailoverSupport?
            CheckNotClosed();
            CheckTransacted(); // throws IllegalOperationException if not a transacted session

            try
            {
                _connection.ConvenientProtocolWriter.SyncWrite(
                        TxRollbackBody.CreateAMQFrame(_channelId), typeof(TxRollbackOkBody));
            }
            catch (AMQException e)
            {
                throw new QpidException("Failed to rollback", e);
            }
        }

        public override void Close()
        {
            lock (_connection.FailoverMutex)
            {
                // We must close down all producers and consumers in an orderly fashion. This is the only method
                // that can be called from a different thread of control from the one controlling the session

                lock (_closingLock)
                {
                    SetClosed();

                    // we pass null since this is not an error case
                    CloseProducersAndConsumers(null);

                    try
                    {
                        _connection.CloseSession(this);
                    }
                    catch (AMQException e)
                    {
                        throw new QpidException("Error closing session: " + e);
                    }
                    finally
                    {
                        _connection.DeregisterSession(_channelId);
                    }
                }
            }
        }

        private void SetClosed()
        {
            Interlocked.Exchange(ref _closed, CLOSED);
        }

        /// <summary>
        /// Close all producers or consumers. This is called either in the error case or when closing the session normally.
        /// <param name="amqe">the exception, may be null to indicate no error has occurred</param>
        ///
        private void CloseProducersAndConsumers(AMQException amqe)
        {
            try
            {
                CloseProducers();
            }
            catch (QpidException e)
            {
                _logger.Error("Error closing session: " + e, e);
            }
            try
            {
                CloseConsumers(amqe);
            }
            catch (QpidException e)
            {
                _logger.Error("Error closing session: " + e, e);
            }
        }

        /**
         * Called when the server initiates the closure of the session
         * unilaterally.
         * @param e the exception that caused this session to be closed. Null causes the
         */
        public void ClosedWithException(Exception e)
        {
            lock (_connection.FailoverMutex)
            {
                // An AMQException has an error code and message already and will be passed in when closure occurs as a
                // result of a channel close request
                SetClosed();
                AMQException amqe;
                if (e is AMQException)
                {
                    amqe = (AMQException) e;
                }
                else
                {
                    amqe = new AMQException("Closing session forcibly", e);
                }
                _connection.DeregisterSession(_channelId);
                CloseProducersAndConsumers(amqe);
            }
        }

        /// <summary>
        /// Called to close message producers cleanly. This may or may <b>not</b> be as a result of an error. There is
        /// currently no way of propagating errors to message producers (this is a JMS limitation).
        /// </summary>
        private void CloseProducers()
        {
            _logger.Info("Closing producers on session " + this);
            // we need to clone the list of producers since the close() method updates the _producers collection
            // which would result in a concurrent modification exception
            ArrayList clonedProducers = new ArrayList(_producers.Values);
            
            foreach (BasicMessageProducer prod in clonedProducers)
            {
                _logger.Info("Closing producer " + prod);
                prod.Close();
            }
            // at this point the _producers map is empty
        }

        /// <summary>
        /// Called to close message consumers cleanly. This may or may <b>not</b> be as a result of an error.
        /// <param name="error">not null if this is a result of an error occurring at the connection level</param>
        ///
        private void CloseConsumers(Exception error)
        {
            if (_dispatcher != null)
            {
                _dispatcher.StopDispatcher();
            }
            // we need to clone the list of consumers since the close() method updates the _consumers collection
            // which would result in a concurrent modification exception
            ArrayList clonedConsumers = new ArrayList(_consumers.Values);

            foreach (BasicMessageConsumer con in clonedConsumers)
            {             
                if (error != null)
                {
                    con.NotifyError(error);
                }
                else
                {
                    con.Close();
                }
            }
            // at this point the _consumers map will be empty
        }

        public void Recover()
        {
            CheckNotClosed();
            CheckNotTransacted(); // throws IllegalOperationException if not a transacted session

            // TODO: This cannot be implemented using 0.8 semantics
            throw new NotImplementedException();
        }                

        public void Run()
        {
            throw new NotImplementedException();
        }

        public IMessagePublisher CreatePublisher(string exchangeName, string routingKey, DeliveryMode deliveryMode,
                                               long timeToLive, bool immediate, bool mandatory, int priority)
        {
            _logger.Debug(string.Format("Using new CreatePublisher exchangeName={0}, exchangeClass={1} routingKey={2}",
                              exchangeName, "none", routingKey));
            return CreateProducerImpl(exchangeName, routingKey, deliveryMode,
                timeToLive, immediate, mandatory, priority);
        }

        public IMessagePublisher CreateProducerImpl(string exchangeName, string routingKey,
                                                    DeliveryMode deliveryMode,
                                                    long timeToLive, bool immediate, bool mandatory, int priority)
        {
            lock (_closingLock)
            {
                CheckNotClosed();

                try
                {
                    return new BasicMessageProducer(exchangeName, routingKey, _transacted, _channelId,
                                                    this, GetNextProducerId(),
                                                    deliveryMode, timeToLive, immediate, mandatory, priority);
                }
                catch (AMQException e)
                {
                    _logger.Error("Error creating message producer: " + e, e);
                    throw new QpidException("Error creating message producer", e);
                }
            }
        }

        public IMessageConsumer CreateConsumer(string queueName,
                                               int prefetchLow,
                                               int prefetchHigh,
                                               bool noLocal,
                                               bool exclusive,
                                               bool durable,
                                               string subscriptionName)
        {
            _logger.Debug(String.Format("CreateConsumer queueName={0} prefetchLow={1} prefetchHigh={2} noLocal={3} exclusive={4} durable={5} subscriptionName={6}",
                                  queueName, prefetchLow, prefetchHigh, noLocal, exclusive, durable, subscriptionName));
            return CreateConsumerImpl(queueName, prefetchLow, prefetchHigh, noLocal, exclusive, durable, subscriptionName);
        }

        private IMessageConsumer CreateConsumerImpl(string queueName,
                                                    int prefetchLow,
                                                    int prefetchHigh,
                                                    bool noLocal,
                                                    bool exclusive,
                                                    bool durable,
                                                    string subscriptionName)
        {
            if (durable || subscriptionName != null)
            {
                throw new NotImplementedException(); // TODO: durable subscriptions.
            }

            lock (_closingLock)
            {
                CheckNotClosed();
               
                BasicMessageConsumer consumer = new BasicMessageConsumer(_channelId, queueName, noLocal,
                                                                         _messageFactoryRegistry, this,
                                                                         prefetchHigh, prefetchLow, exclusive);
                try
                {
                    RegisterConsumer(consumer);
                }
                catch (AMQException e)
                {
                    throw new QpidException("Error registering consumer: " + e, e);
                }

                return consumer;
            }
        }

        public IFieldTable CreateFieldTable()
        {
            return new FieldTable();
        }

        public void Unsubscribe(String name)
        {
            throw new NotImplementedException(); // FIXME
        }

        private void CheckTransacted()
        {
            if (!Transacted)
            {
                throw new InvalidOperationException("Channel is not transacted");
            }
        }

        private void CheckNotTransacted()
        {
            if (Transacted)
            {
                throw new InvalidOperationException("Channel is transacted");
            }
        }

        public void MessageReceived(UnprocessedMessage message)
        {
            if (_logger.IsDebugEnabled)
            {
                _logger.Debug("Message received in session with channel id " + _channelId);
            }            
            _queue.EnqueueBlocking(message);         
        }

        public int DefaultPrefetch
        {
            get
            {
                return _defaultPrefetch;
            }
            set
            {
                _defaultPrefetch = value;
            }
        }        

        public ushort ChannelId
        {
            get
            {
                return _channelId;
            }
        }

        public AMQConnection Connection
        {
            get
            {
                return _connection;
            }
        }
        
        internal void Start()
        {
            _dispatcher = new Dispatcher(this);
            Thread dispatcherThread = new Thread(new ThreadStart(_dispatcher.RunDispatcher));
            dispatcherThread.IsBackground = true;
            dispatcherThread.Start();
        }

        internal void Stop()
        {
            if (_dispatcher != null)
            {
                _dispatcher.StopDispatcher();
            }
        }

        internal void RegisterConsumer(string consumerTag, IMessageConsumer consumer)
        {
            _consumers[consumerTag] =  consumer;
        }

        /// <summary>
        /// Called by the MessageConsumer when closing, to deregister the consumer from the
        /// map from consumerTag to consumer instance.
        /// </summary>
        /// <param name="consumerTag">the consumer tag, that was broker-generated</param>        
        internal void DeregisterConsumer(string consumerTag)
        {
            _consumers.Remove(consumerTag);
        }

        internal void RegisterProducer(long producerId, IMessagePublisher publisher)
        {
            _producers[producerId] = publisher;
        }

        internal void DeregisterProducer(long producerId)
        {
            _producers.Remove(producerId);
        }

        private long GetNextProducerId()
        {
            return ++_nextProducerId;
        }
        
        public void Dispose()
        {
            Close();
        }

        /**
         * Called to mark the session as being closed. Useful when the session needs to be made invalid, e.g. after
         * failover when the client has veoted resubscription.
         *
         * The caller of this method must already hold the failover mutex.
         */
        internal void MarkClosed()
        {
            SetClosed();
            _connection.DeregisterSession(_channelId);
            MarkClosedProducersAndConsumers();
        }

        private void MarkClosedProducersAndConsumers()
        {
            try
            {
                // no need for a markClosed* method in this case since there is no protocol traffic closing a producer
                CloseProducers();
            }
            catch (QpidException e)
            {
                _logger.Error("Error closing session: " + e, e);
            }
            try
            {
                MarkClosedConsumers();
            }
            catch (QpidException e)
            {
                _logger.Error("Error closing session: " + e, e);
            }
        }

        private void MarkClosedConsumers()
        {
            if (_dispatcher != null)
            {
                _dispatcher.StopDispatcher();
            }
            // we need to clone the list of consumers since the close() method updates the _consumers collection
            // which would result in a concurrent modification exception
            ArrayList clonedConsumers = new ArrayList(_consumers.Values);

            foreach (BasicMessageConsumer consumer in clonedConsumers)
            {
                consumer.MarkClosed();
            }
            // at this point the _consumers map will be empty
        }

        /**
         * Replays frame on fail over.
         * 
         * @throws AMQException
         */
        internal void ReplayOnFailOver()
        {
            _logger.Debug(string.Format("Replaying frames for channel {0}", _channelId));
            foreach (AMQFrame frame in _replayFrames)
            {
                _logger.Debug(string.Format("Replaying frame=[{0}]", frame));
                _connection.ProtocolWriter.Write(frame);
            }
        }

        /// <summary>
        /// Callers must hold the failover mutex before calling this method.
        /// </summary>
        /// <param name="consumer"></param>
        void RegisterConsumer(BasicMessageConsumer consumer)
        {
            String consumerTag = ConsumeFromQueue(consumer.QueueName, consumer.NoLocal,
                                           consumer.Exclusive, consumer.AcknowledgeMode);
            consumer.ConsumerTag = consumerTag;
            _consumers.Add(consumerTag, consumer);
        }

        public void Bind(string queueName, string exchangeName, string routingKey, IFieldTable args)
        {
            DoBind(queueName, exchangeName, routingKey, (FieldTable)args);            
        }

        public void Bind(string queueName, string exchangeName, string routingKey)
        {
            DoBind(queueName, exchangeName, routingKey, new FieldTable());
        }

        internal void DoBind(string queueName, string exchangeName, string routingKey, FieldTable args)
        {

            _logger.Debug(string.Format("QueueBind queueName={0} exchangeName={1} routingKey={2}, arg={3}",
                                    queueName, exchangeName, routingKey, args));

            AMQFrame queueBind = QueueBindBody.CreateAMQFrame(_channelId, 0,
                                                              queueName, exchangeName,
                                                              routingKey, true, args);
            _replayFrames.Add(queueBind);

            lock (_connection.FailoverMutex)
            {
                _connection.ProtocolWriter.Write(queueBind);
            }
        }

        private String ConsumeFromQueue(String queueName, bool noLocal, bool exclusive, AcknowledgeMode acknowledgeMode)
        {
            // Need to generate a consumer tag on the client so we can exploit the nowait flag.
            String tag = string.Format("{0}-{1}", _sessionNumber, _nextConsumerNumber++);
            
            AMQFrame basicConsume = BasicConsumeBody.CreateAMQFrame(_channelId, 0,
                                                                  queueName, tag, noLocal,
                                                                  acknowledgeMode == AcknowledgeMode.NoAcknowledge,
                                                                  exclusive, true, new FieldTable());

            _replayFrames.Add(basicConsume);

            _connection.ProtocolWriter.Write(basicConsume);
            return tag;
        }

        public void DeleteExchange(string exchangeName)
        {
            throw new NotImplementedException(); // FIXME
        }

        public void DeleteQueue()
        {
            throw new NotImplementedException(); // FIXME
        }

        public MessageConsumerBuilder CreateConsumerBuilder(string queueName)
        {
            return new MessageConsumerBuilder(this, queueName);
        }

        public MessagePublisherBuilder CreatePublisherBuilder()
        {
            return new MessagePublisherBuilder(this);
        }

        internal void BasicPublish(string exchangeName, string routingKey, bool mandatory, bool immediate,
                                   AbstractQmsMessage message, DeliveryMode deliveryMode, int priority, uint timeToLive,
                                   bool disableTimestamps)
        {
            DoBasicPublish(exchangeName, routingKey, mandatory, immediate, message, deliveryMode, timeToLive, priority, disableTimestamps);
        }

        private void DoBasicPublish(string exchangeName, string routingKey, bool mandatory, bool immediate, AbstractQmsMessage message, DeliveryMode deliveryMode, uint timeToLive, int priority, bool disableTimestamps)
        {
            AMQFrame publishFrame = BasicPublishBody.CreateAMQFrame(_channelId, 0, exchangeName,
                                                                    routingKey, mandatory, immediate);

            long currentTime = 0;
            if (!disableTimestamps)
            {
                currentTime = DateTime.UtcNow.Ticks;
                message.Timestamp = currentTime;
            }

            ByteBuffer buf = message.Data;
            byte[] payload = null;
            if (buf != null)
            {
                payload = new byte[buf.remaining()];
                buf.get(payload);
            }
            BasicContentHeaderProperties contentHeaderProperties = message.ContentHeaderProperties;

            if (timeToLive > 0)
            {
                if (!disableTimestamps)
                {
                    contentHeaderProperties.Expiration = (uint)currentTime + timeToLive;
                }
            }
            else
            {
                contentHeaderProperties.Expiration = 0;
            }
            contentHeaderProperties.SetDeliveryMode(deliveryMode);
            contentHeaderProperties.Priority = (byte)priority;

            ContentBody[] contentBodies = CreateContentBodies(payload);
            AMQFrame[] frames = new AMQFrame[2 + contentBodies.Length];
            for (int i = 0; i < contentBodies.Length; i++)
            {
                frames[2 + i] = ContentBody.CreateAMQFrame(_channelId, contentBodies[i]);
            }
            if (contentBodies.Length > 0 && _logger.IsDebugEnabled)
            {
                _logger.Debug(string.Format("Sending content body frames to {{exchangeName={0} routingKey={1}}}", exchangeName, routingKey));
            }

            // weight argument of zero indicates no child content headers, just bodies
            AMQFrame contentHeaderFrame = ContentHeaderBody.CreateAMQFrame(_channelId, BASIC_CONTENT_TYPE, 0, contentHeaderProperties,
                                                                           (uint)payload.Length);
            if (_logger.IsDebugEnabled)
            {
                _logger.Debug(string.Format("Sending content header frame to  {{exchangeName={0} routingKey={1}}}", exchangeName, routingKey));
            }

            frames[0] = publishFrame;
            frames[1] = contentHeaderFrame;
            CompositeAMQDataBlock compositeFrame = new CompositeAMQDataBlock(frames);

            lock (_connection.FailoverMutex) {
                _connection.ProtocolWriter.Write(compositeFrame);
            }   
        }

        /// <summary>
        /// Create content bodies. This will split a large message into numerous bodies depending on the negotiated
        /// maximum frame size.
        /// </summary>
        /// <param name="payload"></param>
        /// <returns>return the array of content bodies</returns>
        private ContentBody[] CreateContentBodies(byte[] payload)
        {
            if (payload == null)
            {
                return null;
            }
            else if (payload.Length == 0)
            {
                return new ContentBody[0];
            }
            // we substract one from the total frame maximum size to account for the end of frame marker in a body frame
            // (0xCE byte).
            long framePayloadMax = Connection.MaximumFrameSize - 1;
            int lastFrame = (payload.Length % framePayloadMax) > 0 ? 1 : 0;
            int frameCount = (int)(payload.Length / framePayloadMax) + lastFrame;
            ContentBody[] bodies = new ContentBody[frameCount];

            if (frameCount == 1)
            {
                bodies[0] = new ContentBody();
                bodies[0].Payload = payload;
            }
            else
            {
                long remaining = payload.Length;
                for (int i = 0; i < bodies.Length; i++)
                {
                    bodies[i] = new ContentBody();
                    byte[] framePayload = new byte[(remaining >= framePayloadMax) ? (int)framePayloadMax : (int)remaining];
                    Array.Copy(payload, (int)framePayloadMax * i, framePayload, 0, framePayload.Length);
                    bodies[i].Payload = framePayload;
                    remaining -= framePayload.Length;
                }
            }
            return bodies;
        }

        public string GenerateUniqueName()
        {
            string result = _connection.ProtocolSession.GenerateQueueName();            
            return Regex.Replace(result, "[^a-z0-9_]", "_");
        }

        public void DeclareQueue(string queueName, bool isDurable, bool isExclusive, bool isAutoDelete)
        {
            DoQueueDeclare(queueName, isDurable, isExclusive, isAutoDelete);
        }

        private void DoQueueDeclare(string queueName, bool isDurable, bool isExclusive, bool isAutoDelete)
        {
            _logger.Debug(string.Format("DeclareQueue name={0} durable={1} exclusive={2}, auto-delete={3}",
                                        queueName, isDurable, isExclusive, isAutoDelete));

            AMQFrame queueDeclare = QueueDeclareBody.CreateAMQFrame(_channelId, 0, queueName,
                                                                    false, isDurable, isExclusive,
                                                                    isAutoDelete, true, null);

            _replayFrames.Add(queueDeclare);

            lock (_connection.FailoverMutex)
            {
                _connection.ProtocolWriter.Write(queueDeclare);
            }
        }

        public void DeclareExchange(String exchangeName, String exchangeClass)
        {
            _logger.Debug(string.Format("DeclareExchange vame={0} exchangeClass={1}", exchangeName, exchangeClass));

            DeclareExchange(_channelId, 0, exchangeName, exchangeClass, false, false, false, false, true, null);
        }

        // AMQP-level method.
        private void DeclareExchange(ushort channelId, ushort ticket, string exchangeName, 
                                     string exchangeClass, bool passive, bool durable, 
                                     bool autoDelete, bool xinternal, bool noWait, FieldTable args)
        {
            _logger.Debug(String.Format("DeclareExchange channelId={0} exchangeName={1} exchangeClass={2}",
                                            _channelId, exchangeName, exchangeClass));

            AMQFrame declareExchange = ExchangeDeclareBody.CreateAMQFrame(
                channelId, ticket, exchangeName, exchangeClass, passive, durable, autoDelete, xinternal, noWait, args);

            _replayFrames.Add(declareExchange);
            
            if (noWait)
            {
                lock (_connection.FailoverMutex)
                {
                    _connection.ProtocolWriter.Write(declareExchange);
                }
            }
            else
            {
                throw new NotImplementedException("Don't use nowait=false with DeclareExchange");
//                _connection.ConvenientProtocolWriter.SyncWrite(declareExchange, typeof (ExchangeDeclareOkBody));
            }
        }

        /**
         * Acknowledge a message or several messages. This method can be called via AbstractJMSMessage or from
         * a BasicConsumer. The former where the mode is CLIENT_ACK and the latter where the mode is
         * AUTO_ACK or similar.
         *
         * @param deliveryTag the tag of the last message to be acknowledged
         * @param multiple    if true will acknowledge all messages up to and including the one specified by the
         *                    delivery tag
         */
        public void AcknowledgeMessage(ulong deliveryTag, bool multiple)
        {
            AMQFrame ackFrame = BasicAckBody.CreateAMQFrame(_channelId, deliveryTag, multiple);
            if (_logger.IsDebugEnabled)
            {
                _logger.Debug("Sending ack for delivery tag " + deliveryTag + " on channel " + _channelId);
            }
            // FIXME: lock FailoverMutex here?
            _connection.ProtocolWriter.Write(ackFrame);
        }
    }
}
