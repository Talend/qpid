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

/**
 * This file provides a simple test (and example) of basic
 * functionality including declaring an exchange and a queue, binding
 * these together, publishing a message and receiving that message
 * asynchronously.
 */

#include <algorithm>
#include <iostream>
#include <sstream>
#include <vector>

#include "TestOptions.h"
#include "qpid/client/Connection.h"
#include "qpid/client/Message.h"
#include "qpid/client/Session_0_10.h"
#include "qpid/client/SubscriptionManager.h"

using namespace qpid;
using namespace qpid::client;
using namespace qpid::sys;
using std::string;

typedef std::vector<std::string> StringSet;

struct Args : public qpid::TestOptions {
    bool init, transfer, check;//actions
    uint size;
    bool durable;
    uint queues;
    string base;
    uint msgsPerTx;
    uint txCount;
    uint totalMsgCount;

    Args() : init(true), transfer(true), check(true), 
             size(256), durable(true), queues(2), 
             base("tx-test"), msgsPerTx(1), txCount(1), totalMsgCount(10)
    {
        addOptions()            

            ("init", optValue(init, "yes|no"), "Declare queues and populate one with the initial set of messages.")
            ("transfer", optValue(transfer, "yes|no"), "'Move' messages from one queue to another using transactions to ensure no message loss.")
            ("check", optValue(check, "yes|no"), "Check that the initial messages are all still available.")
            ("size", optValue(size, "N"), "message size")
            ("durable", optValue(durable, "yes|no"), "use durable messages")
            ("queues", optValue(queues, "N"), "number of queues")
            ("queue-base-name", optValue(base, "<name>"), "base name for queues")
            ("messages-per-tx", optValue(msgsPerTx, "N"), "number of messages transferred per transaction")
            ("tx-count", optValue(txCount, "N"), "number of transactions per 'agent'")
            ("total-messages", optValue(totalMsgCount, "N"), "total number of messages in 'circulation'");
    }
};

const std::string chars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");

std::string generateData(uint size)
{
    if (size < chars.length()) {
        return chars.substr(0, size);
    }   
    std::string data;
    for (uint i = 0; i < (size / chars.length()); i++) {
        data += chars;
    }
    data += chars.substr(0, size % chars.length());
    return data;
}

void generateSet(const std::string& base, uint count, StringSet& collection)
{
    for (uint i = 0; i < count; i++) {
        std::ostringstream out;
        out << base << "-" << (i+1);
        collection.push_back(out.str());
    }
}

Args opts;

struct Client 
{
    Connection connection;
    Session_0_10 session;

    Client() 
    {
        opts.open(connection);
        session = connection.newSession();
    }

    ~Client() 
    {
        session.close();
        connection.close();
    }
};

struct Transfer : public Client, public Runnable
{
    std::string src;
    std::string dest;
    Thread thread;

    Transfer(const std::string& to, const std::string& from) : src(to), dest(from) {}

    void run() 
    {
        try {
        
            session.txSelect();
            SubscriptionManager subs(session);
            
            LocalQueue lq(AckPolicy(0));//manual acking
            subs.setFlowControl(opts.msgsPerTx, SubscriptionManager::UNLIMITED, true);
            subs.subscribe(lq, src);
            
            for (uint t = 0; t < opts.txCount; t++) {
                Message in;
                Message out("", dest);
                for (uint m = 0; m < opts.msgsPerTx; m++) {
                    in = lq.pop();
                    out.setData(in.getData());
                    out.getMessageProperties().setCorrelationId(in.getMessageProperties().getCorrelationId());
                    session.messageTransfer(arg::content=out);
                }
                in.acknowledge();
                session.txCommit();
            }
        } catch(const std::exception& e) {
            std::cout << "Transfer interrupted: " << e.what() << std::endl;
        }
    }
};

struct Controller : public Client 
{
    StringSet ids;
    StringSet queues;

    Controller()
    {
        generateSet(opts.base, opts.queues, queues);
        generateSet("msg", opts.totalMsgCount, ids);
    }

    void init() 
    {
        //declare queues
        for (StringSet::iterator i = queues.begin(); i != queues.end(); i++) {
            session.queueDeclare(arg::queue=*i, arg::durable=opts.durable).sync();
        }

        Message msg(generateData(opts.size), *queues.begin());
        if (opts.durable) {
            msg.getDeliveryProperties().setDeliveryMode(framing::PERSISTENT);
        }

        //publish messages
        for (StringSet::iterator i = ids.begin(); i != ids.end(); i++) {
            msg.getMessageProperties().setCorrelationId(*i);
            session.messageTransfer(arg::content=msg);
        }
    }

    void transfer()
    {
        boost::ptr_vector<Transfer> agents(opts.queues);
        //launch transfer agents
        for (StringSet::iterator i = queues.begin(); i != queues.end(); i++) {
            StringSet::iterator next = i + 1;
            if (next == queues.end()) next = queues.begin();

            std::cout << "Transfering from " << *i << " to " << *next << std::endl;
            agents.push_back(new Transfer(*i, *next));
            agents.back().thread = Thread(agents.back());
        }

        for (boost::ptr_vector<Transfer>::iterator i = agents.begin(); i != agents.end(); i++) {
            i->thread.join();
        }
    }

    void check() 
    {
        SubscriptionManager subs(session);
        subs.setFlowControl(SubscriptionManager::UNLIMITED, SubscriptionManager::UNLIMITED, false);
        subs.setConfirmMode(false);

        StringSet drained;
        //drain each queue and verify the correct set of messages are available
        for (StringSet::iterator i = queues.begin(); i != queues.end(); i++) {
            //subscribe, allocate credit and flush
            LocalQueue lq(AckPolicy(0));//manual acking
            subs.subscribe(lq, *i, *i);
            session.messageFlush(arg::destination=*i).sync();

            uint count(0);
            while (!lq.empty()) {
                Message m = lq.pop();
                //add correlation ids of received messages to drained
                drained.push_back(m.getMessageProperties().getCorrelationId());
                ++count;
            }
            std::cout << "Drained " << count << " messages from " << *i << std::endl;
        }

        sort(ids.begin(), ids.end());
        sort(drained.begin(), drained.end());

        //check that drained == ids
        StringSet missing;
        set_difference(ids.begin(), ids.end(), drained.begin(), drained.end(), back_inserter(missing)); 

        StringSet extra;
        set_difference(drained.begin(), drained.end(), ids.begin(), ids.end(), back_inserter(extra)); 

        if (missing.empty() && extra.empty()) {
            std::cout << "All expected messages were retrieved." << std::endl;            
        } else {
            if (!missing.empty()) {
                std::cout << "The following ids were missing:" << std::endl;
                for (StringSet::iterator i = missing.begin(); i != missing.end(); i++) {
                    std::cout << "    '" << *i << "'" << std::endl;                
                }            
            }
            if (!extra.empty()) {
                std::cout << "The following extra ids were encountered:" << std::endl;
                for (StringSet::iterator i = extra.begin(); i != extra.end(); i++) {
                    std::cout << "    '" << *i << "'" << std::endl;     
                }            
            }
        }
    }
};

int main(int argc, char** argv)
{
    try {
        opts.parse(argc, argv);
        Controller controller;
        if (opts.init) controller.init(); 
        if (opts.transfer) controller.transfer();
        if (opts.check) controller.check();
        return 0;
    } catch(const std::exception& e) {
	std::cout << e.what() << std::endl;
    }
    return 1;
}
