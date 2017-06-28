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
 *  Solace AMQP JMS 1.1 Samples: SimpleReplier
 */

package com.solace.samples;

import org.apache.logging.log4j.Logger;
import org.apache.qpid.jms.JmsTemporaryQueue;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.TimeUnit;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
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
 * Receives a request message using JMS 1.1 API over AMQP 1.0 and replies to it. Solace Message Router is used as the
 * message broker.
 * 
 * Two queues are used: one for request and the other for reply. Both queues must exist on the message broker.
 * 
 * This is the Replier in the Request/Reply messaging pattern.
 */
public class SimpleReplier {

    private static final Logger LOG = LogManager.getLogger(SimpleReplier.class.getName());

    // connectionfactory.solaceConnectionLookup in file "jndi.properties"
    final String SOLACE_CONNECTION_LOOKUP = "solaceConnectionLookup";
    // queue.queueLookup in file "jndi.properties"
    // this is the request queue
    final String QUEUE_LOOKUP = "queueLookup";

    // session parameters
    final int ACK_MODE = Session.AUTO_ACKNOWLEDGE;
    final boolean TRANSACTED = false;

    private void run() {
        try {
            // pick up properties from the "jndi.properties" file
            Context initialContext = new InitialContext(); //
            QueueConnectionFactory factory = (QueueConnectionFactory) initialContext
                    .lookup(SOLACE_CONNECTION_LOOKUP);

            // establish connection that uses the Solace Message Router as a message broker
            try (QueueConnection connection = factory.createQueueConnection()) {
                connection.setExceptionListener(new QueueConnectionExceptionListener());
                connection.start();

                // the source for requests: a queue that already exists on the broker
                Queue source = (Queue) initialContext.lookup(QUEUE_LOOKUP);

                // create session and subscribe to requests
                // create sender that will be used to reply to the received requests
                try (QueueSession session = connection.createQueueSession(TRANSACTED, ACK_MODE);
                        QueueReceiver requestConsumer = session.createReceiver(source);
                        QueueSender replySender = session.createSender(null)) {
                    replySender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                    LOG.info("Waiting for a request...");
                    // the current thread blocks here until a request arrives
                    Message request = requestConsumer.receive();
                    if (request instanceof TextMessage) {
                        TextMessage requestTextMessage = (TextMessage) request;
                        LOG.info("Received AMQP request with string data: \"{}\"", requestTextMessage.getText());
                        // prepare reply with received string data
                        TextMessage reply = session.createTextMessage(
                                String.format("Reply to \"%s\"", requestTextMessage.getText()));
                        reply.setJMSCorrelationID(request.getJMSCorrelationID());
                        // workaround as the Apache Qpid JMS API sets JMSReplyTo to a non-temporary queue 
                        // should be: JmsTemporaryQueue replyTo = (JmsTemporaryQueue) request.getJMSReplyTo();
                        Destination replyTo = new JmsTemporaryQueue(((Queue) request.getJMSReplyTo()).getQueueName());
                        // send the reply
                        replySender.send(replyTo, reply);
                        LOG.info("Request Message replied successfully.");
                    } else {
                        LOG.warn("Unexpected data type in request: \"{}\", nothing replied.", request.toString());
                    }
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException ex) {
                    LOG.error(ex);
                }
            }

            initialContext.close();
        } catch (NamingException ex) {
            LOG.error(ex);
        } catch (JMSException ex) {
            LOG.error(ex);
        }
    }

    private static class QueueConnectionExceptionListener implements ExceptionListener {
        @Override
        public void onException(JMSException ex) {
            LOG.error(ex);
        }

    }

    public static void main(String[] args) {
        new SimpleReplier().run();
    }

}
