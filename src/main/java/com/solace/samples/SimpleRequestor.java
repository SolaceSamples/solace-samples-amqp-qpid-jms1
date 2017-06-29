/**
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
 */

/**
 *  Solace AMQP JMS 1.1 Examples: SimpleRequestor
 */

package com.solace.samples;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.UUID;
import java.util.concurrent.Semaphore;

import javax.jms.DeliveryMode;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Sends a request message using JMS 1.1 API over AMQP 1.0 and receives a reply to it. Solace Message Router is used as
 * the message broker.
 * 
 * The queues used for requests must exist on the message broker.
 * 
 * This is the Requestor in the Request/Reply messaging pattern.
 */
public class SimpleRequestor implements MessageListener {

    private static final Logger LOG = LogManager.getLogger(SimpleRequestor.class.getName());

    // connectionfactory.solaceConnectionLookup in file "jndi.properties"
    final String SOLACE_CONNECTION_LOOKUP = "solaceConnectionLookup";
    // queue.queueLookup in file "jndi.properties"
    // this is the request queue
    final String QUEUE_LOOKUP = "queueLookup";

    // session parameters
    final int ACK_MODE = Session.AUTO_ACKNOWLEDGE;
    final boolean IS_TRANSACTED = false;

    // semaphore for signaling of the reply arrival
    final static Semaphore replyLatch = new Semaphore(0);

    private void run() {
        try {
            // pick up properties from the "jndi.properties" file
            Context initialContext = new InitialContext(); //
            QueueConnectionFactory factory = (QueueConnectionFactory) initialContext.lookup(SOLACE_CONNECTION_LOOKUP);

            // establish connection that uses the Solace Message Router as a message broker
            try (QueueConnection connection = factory.createQueueConnection()) {
                connection.setExceptionListener(new QueueConnectionExceptionListener());
                connection.start();

                // the target for requests: a queue that already exists on the message broker
                Queue target = (Queue) initialContext.lookup(QUEUE_LOOKUP);

                // create session and message sender
                try (QueueSession session = connection.createQueueSession(IS_TRANSACTED, ACK_MODE);
                        QueueSender requestSender = session.createSender(target)) {
                    requestSender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                    // the source for replies: a temporary queue
                    TemporaryQueue replyQueue = session.createTemporaryQueue();
                    
                    // reuse the session and create receiver for reply messages
                    try (QueueReceiver replyReceiver = session.createReceiver(replyQueue)) {
                        replyReceiver.setMessageListener(this);

                        // prepare request
                        TextMessage request = session.createTextMessage("Request with String Data");
                        request.setJMSReplyTo(replyQueue);
                        request.setJMSCorrelationID(UUID.randomUUID().toString());

                        // send request
                        requestSender.send(request);
                        LOG.info("Request message sent successfully, waiting for a reply...");
                        // the current thread blocks at the next statement until the replyLatch is released
                        replyLatch.acquire();
                    }
                }
            } catch (JMSException ex) {
                LOG.error(ex);
            } catch (InterruptedException ex) {
                LOG.error(ex);
            }

            initialContext.close();
        } catch (NamingException ex) {
            LOG.error(ex);
        }
    }

    private static class QueueConnectionExceptionListener implements ExceptionListener {
        @Override
        public void onException(JMSException ex) {
            LOG.error(ex);
            replyLatch.release();
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            LOG.info("Received reply: \"{}\"", ((TextMessage) message).getText());
        } catch (JMSException ex) {
            LOG.error(ex);
        }
        replyLatch.release();
    }

    public static void main(String[] args) {
        new SimpleRequestor().run();
    }

}
