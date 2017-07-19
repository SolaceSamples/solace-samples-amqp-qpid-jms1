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
 *  Solace AMQP JMS 1.1 Examples: TopicPublisher
 */

package com.solace.samples;

import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Publishes a messages to a topic using JMS 1.1 API over AMQP 1.0. Solace Message Router is used as the message broker.
 * 
 * This is the Publisher in the Publish/Subscribe messaging pattern.
 */
public class TopicPublisher {

    final String TOPIC_NAME = "T/GettingStarted/pubsub";

    final String SOLACE_CONNECTION_LOOKUP = "solaceConnectionLookup";

    private void run(String... args) throws JMSException, NamingException {
        String solaceHost = args[0];
        System.out.printf("TopicPublisher is connecting to Solace router %s...%n", solaceHost);

        // Programmatically create the connection factory using default settings
        Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        env.put("connectionfactory." + SOLACE_CONNECTION_LOOKUP, "amqp://" + solaceHost);
        Context initialContext = new InitialContext(env);
        ConnectionFactory connectionFactory = (ConnectionFactory) initialContext.lookup(SOLACE_CONNECTION_LOOKUP);

        // Create connection to the Solace router
        Connection connection = connectionFactory.createConnection();

        // Create a non-transacted, Auto ACK session.
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        System.out.println("Connected to the Solace router.");

        // Create the publishing topic programmatically
        Topic topic = session.createTopic(TOPIC_NAME);

        // Create the message producer for the created topic
        MessageProducer messageProducer = session.createProducer(topic);

        // Create the message
        TextMessage message = session.createTextMessage("Hello world!");

        System.out.printf("Sending message '%s' to topic '%s'...%n", message.getText(), topic.toString());

        // Send the message
        messageProducer.send(message,
                DeliveryMode.NON_PERSISTENT,
                Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
        System.out.println("Sent successfully. Exiting...");

        // Close everything in the order reversed from the opening order
        // NOTE: as the interfaces below extend AutoCloseable,
        // with them it's possible to use the "try-with-resources" Java statement
        // see details at https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
        messageProducer.close();
        session.close();
        connection.close();
        // this needs to be close explicitly, it does not extend AutoCloseable
        initialContext.close();
    }

    public static void main(String[] args) throws JMSException, NamingException {
        if (args.length < 1) {
            System.out.println("Usage: TopicPublisher <msg_backbone_ip:amqp_port>");
            System.exit(-1);
        }
        new TopicPublisher().run(args);
    }

}
