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
 *   8-0
 */

package org.apache.qpid.framing.amqp_8_0;

import java.io.DataOutput;
import java.io.IOException;

import org.apache.qpid.AMQException;
import org.apache.qpid.codec.MarkableDataInput;
import org.apache.qpid.framing.AMQFrameDecodingException;
import org.apache.qpid.framing.AMQMethodBody;
import org.apache.qpid.framing.AMQMethodBodyImpl;
import org.apache.qpid.framing.AMQMethodBodyInstanceFactory;
import org.apache.qpid.framing.BasicRecoverOkBody;
import org.apache.qpid.framing.MethodDispatcher;

public class BasicRecoverOkBodyImpl extends AMQMethodBodyImpl implements BasicRecoverOkBody
{
    private static final AMQMethodBodyInstanceFactory FACTORY_INSTANCE = new AMQMethodBodyInstanceFactory()
    {
        public AMQMethodBody newInstance(MarkableDataInput in, long size) throws AMQFrameDecodingException, IOException
        {
            return new BasicRecoverOkBodyImpl(in);
        }
    };

    public static AMQMethodBodyInstanceFactory getFactory()
    {
        return FACTORY_INSTANCE;
    }

    public static final int CLASS_ID =  60;
    public static final int METHOD_ID = 101;

    // Fields declared in specification

    // Constructor
    public BasicRecoverOkBodyImpl(MarkableDataInput buffer) throws AMQFrameDecodingException, IOException
    {
    }

    public BasicRecoverOkBodyImpl(
                            )
    {
    }

    public int getClazz()
    {
        return CLASS_ID;
    }

    public int getMethod()
    {
        return METHOD_ID;
    }


    protected int getBodySize()
    {
        int size = 0;
        return size;
    }

    public void writeMethodPayload(DataOutput buffer) throws IOException
    {
    }

    public boolean execute(MethodDispatcher dispatcher, int channelId) throws AMQException
	{
    return ((MethodDispatcher_8_0)dispatcher).dispatchBasicRecoverOk(this, channelId);
	}

    public String toString()
    {
        StringBuilder buf = new StringBuilder("[BasicRecoverOkBodyImpl: ");
        buf.append("]");
        return buf.toString();
    }

}
