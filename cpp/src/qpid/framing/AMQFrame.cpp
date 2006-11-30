
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
#include <qpid/framing/AMQFrame.h>
#include <qpid/QpidError.h>

using namespace qpid::framing;

// This only works as a static as the version is currently fixed to 8.0
// TODO: When the class is version-aware this will need to change
AMQP_MethodVersionMap AMQFrame::versionMap(8,0);

// AMQP version management change - kpvdr 2-11-17
// TODO: Make this class version-aware
AMQFrame::AMQFrame() {}

// AMQP version management change - kpvdr 2006-11-17
// TODO: Make this class version-aware
AMQFrame::AMQFrame(u_int16_t _channel, AMQBody* _body) :
channel(_channel), body(_body)
{}

// AMQP version management change - kpvdr 2006-11-17
// TODO: Make this class version-aware
AMQFrame::AMQFrame(u_int16_t _channel, AMQBody::shared_ptr& _body) :
channel(_channel), body(_body)
{}

AMQFrame::~AMQFrame() {}

u_int16_t AMQFrame::getChannel(){
    return channel;
}

AMQBody::shared_ptr& AMQFrame::getBody(){
    return body;
}

void AMQFrame::encode(Buffer& buffer)
{
    buffer.putOctet(body->type());
    buffer.putShort(channel);    
    buffer.putLong(body->size());
    body->encode(buffer);
    buffer.putOctet(0xCE);
}

AMQBody::shared_ptr AMQFrame::createMethodBody(Buffer& buffer){
    u_int16_t classId = buffer.getShort();
    u_int16_t methodId = buffer.getShort();
    // AMQP version management change - kpvdr 2006-11-16
    // TODO: Make this class version-aware and link these hard-wired numbers to that version
    AMQBody::shared_ptr body(versionMap.createMethodBody(classId, methodId, 8, 0));
    // Origianl stmt:
	// AMQBody::shared_ptr body(createAMQMethodBody(classId, methodId));
    return body;
}

u_int32_t AMQFrame::size() const{
    if(!body.get()) THROW_QPID_ERROR(INTERNAL_ERROR, "Attempt to get size of frame with no body set!");
    return 1/*type*/ + 2/*channel*/ + 4/*body size*/ + body->size() + 1/*0xCE*/;
}

bool AMQFrame::decode(Buffer& buffer)
{    
    if(buffer.available() < 7) return false;
    buffer.record();
    u_int32_t bufSize = decodeHead(buffer);

    if(buffer.available() < bufSize + 1){
        buffer.restore();
        return false;
    }
    decodeBody(buffer, bufSize);
    u_int8_t end = buffer.getOctet();
    if(end != 0xCE) THROW_QPID_ERROR(FRAMING_ERROR, "Frame end not found");
    return true;
}

u_int32_t AMQFrame::decodeHead(Buffer& buffer){    
    type = buffer.getOctet();
    channel = buffer.getShort();
    return buffer.getLong();
}

void AMQFrame::decodeBody(Buffer& buffer, uint32_t bufSize)
{    
    switch(type)
    {
    case METHOD_BODY:
	body = createMethodBody(buffer);
	break;
    case HEADER_BODY: 
	body = AMQBody::shared_ptr(new AMQHeaderBody()); 
	break;
    case CONTENT_BODY: 
	body = AMQBody::shared_ptr(new AMQContentBody()); 
	break;
    case HEARTBEAT_BODY: 
	body = AMQBody::shared_ptr(new AMQHeartbeatBody()); 
	break;
    default:
	string msg("Unknown body type: ");
	msg += type;
	THROW_QPID_ERROR(FRAMING_ERROR, msg);
    }
    body->decode(buffer, bufSize);
}

std::ostream& qpid::framing::operator<<(std::ostream& out, const AMQFrame& t)
{
    out << "Frame[channel=" << t.channel << "; ";
    if (t.body.get() == 0)
        out << "empty";
    else
        out << *t.body;
    out << "]";
    return out;
}

