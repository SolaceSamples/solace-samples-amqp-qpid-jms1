---
layout: tutorials
title: Persistence with Queues
summary: Demonstrates persistent messages for guaranteed delivery.
icon: I_dev_Persistent.svg
---

This tutorial builds on the basic concepts introduced in the [publish/subscribe tutorial]({{ site.baseurl }}/publish-subscribe){:target="_blank"}, and will show you how to send and receive persistent messages with Apache Qpid JMS 1.1 client using AMQP and Solace Message Router.

This tutorial is available in [GitHub]({{ site.repository }}){:target="_blank"} along with the other [Solace Getting Started AMQP Tutorials]({{ site.links-get-started-amqp }}){:target="_top"}.

At the end, this tutorial walks through downloading and running the sample from source.

This tutorial focuses on using a non-Solace JMS API implementation. For using the Solace JMS API see [Solace Getting Started JMS Tutorials]({{ site.links-get-started-jms }}){:target="_blank"}.

## Assumptions

This tutorial assumes the following:

* You are familiar with Solace [core concepts]({{ site.docs-core-concepts }}){:target="_top"}.
* You have access to a running Solace message router with the following configuration:
    * Enabled `default` message VPN
    * Enabled `default` client username
    * Enabled `default` client profile with guaranteed messaging permissions.

One simple way to get access to a Solace message router is to start a Solace VMR load [as outlined here]({{ site.docs-vmr-setup }}){:target="_top"}. By default the Solace VMR will run with the `default` message VPN configured and ready for messaging. Going forward, this tutorial assumes that you are using the Solace VMR. If you are using a different Solace message router configuration, adapt the instructions to match your configuration.

## Goals

The goal of this tutorial is to demonstrate how to use Apache Qpid JMS 1.1 over AMQP using the Solace Message Router. This tutorial will show you:

1.  How to send a persistent message to a durable queue on the Solace message router
2.  How to bind to this queue and receive a persistent message

## Solace message router properties

In order to send or receive messages to a Solace message router, you need to know a few details of how to connect to the Solace message router. Specifically you need to know the following:

<table>
<tbody>
<tr>
<th>Resource</th>
<th>Value</th>
<th>Description</th>
</tr>
<tr>
<td>Host</td>
<td>String of the form <code>DNS name</code> or <code>IP:Port</code></td>
<td>This is the address client’s use when connecting to the Solace Message Router to send and receive messages. For a Solace VMR this there is only a single interface so the IP is the same as the management IP address. For Solace message router appliances this is the host address of the message-backbone. The port number must match the port number for the plain text AMQP service on the router.</td>
</tr>
<tr>
<td>Message VPN</td>
<td>String</td>
<td>The `default` Solace message router Message VPN that this client will connect to.</td>
</tr>
<tr>
<td>Client Username</td>
<td>String</td>
<td>The `default` client username.</td>
</tr>
</tbody>
</table>

## Java Messaging Service (JMS) Introduction

JMS is a standard API for sending and receiving messages. As such, in addition to information provided on the Solace developer portal, you may also look at some external sources for more details about JMS. The following are good places to start

1. [http://java.sun.com/products/jms/docs.html](http://java.sun.com/products/jms/docs.html){:target="_blank"}.
2. [https://en.wikipedia.org/wiki/Java_Message_Service](https://en.wikipedia.org/wiki/Java_Message_Service){:target="_blank"}
3. [https://docs.oracle.com/javaee/7/tutorial/partmessaging.htm#GFIRP3](https://docs.oracle.com/javaee/7/tutorial/partmessaging.htm#GFIRP3){:target="_blank"}

The last (Oracle docs) link points you to the JEE official tutorials which provide a good introduction to JMS.

This tutorial focuses on using [JMS 1.1 (April 12, 2002)]({{ site.links-jms1-specification }}){:target="_blank"}, for [JMS 2.0 (May 21, 2013)]({{ site.links-jms2-specification }}){:target="_blank"} see [Solace Getting Started AMQP JMS 2.0 Tutorials]({{ site.links-get-started-amqp-jms2 }}){:target="_blank"}.

## Obtaining Apache Qpid JMS 1.1

This tutorial assumes you have downloaded and successfully installed the [Apache Qpid JMS client](https://qpid.apache.org/components/jms/index.html). If your environment differs from the example, then adjust the build instructions appropriately.

The easiest way to install it is through Gradle or Maven.

### Get the API: Using Gradle

```
dependencies {
    compile("org.apache.qpid:qpid-jms-client:0.23.+")
}
```

### Get the API: Using Maven

```
<dependency>
    <groupId>org.apache.qpid</groupId>
    <artifactId>qpid-jms-client</artifactId>
    <version>[0.23,)</version>
</dependency>
```

## Connecting to the Solace Message Router

In order to send or receive messages, an application must start a JMS connection.

For establishing the JMS connection you need to know the Solace Message Router host name with the AMQP service port number, the client username and optional password.

*QueueProducer.java/QueueConsumer.java*
```java
final String SOLACE_USERNAME = "clientUsername";
final String SOLACE_PASSWORD = "password";

ConnectionFactory connectionFactory = new JmsConnectionFactory(SOLACE_USERNAME, SOLACE_PASSWORD, solaceHost);
Connection connection = connectionFactory.createConnection();
```

Created a non-transacted session. Use two different session acknowledge modes: one that automatically acknowledges a client's receipt of a message, and the other that requires the client acknowledge to call `message.acknowledge()` for that.

*QueueProducer.java*
```java
Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
```

*QueueConsumer.java*
```java
Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
```

At this point the application is connected to the Solace Message Router and ready to send and receive messages.

## Sending a persistent message to a queue

In order to send a message to a queue a JMS *MessageProducer* needs to be created.

![sending-message-to-queue]({{ site.baseurl }}/images/persistence-with-queues-details-2.png)

There is no difference in the actual method calls to the JMS `MessageProducer` when sending a JMS `persistent` message as compared to a JMS `non-persistent` message shown in the [publish/subscribe tutorial]({{ site.baseurl }}/publish-subscribe){:target="_blank"}. The difference in the JMS `persistent` message is that the Solace Message Router will acknowledge the message once it is successfully stored on the message router and the `MessageProducer.send()` call will not return until it has successfully received this acknowledgement. This means that in JMS, all calls to the `MessageProducer.send()` are blocking calls and they wait for message confirmation from the Solace message router before proceeding. This is outlined in the JMS 1.1 specification and Solace JMS adheres to this requirement.

The queue for sending messages will be created on the Solace router as a `durable queue`.

See [Configuring Queues]({{ site.docs-confugure-queues }}){:target="_blank"} for details on how to configure durable queues on Solace Message Routers with Solace CLI.

See [Management Tools]({{ site.docs-management-tools }}){:target="_top"} for other tools for configure durable queues.

*QueueProducer.java*
```java
final String QUEUE_NAME = "Q/tutorial";

Queue queue = session.createQueue(QUEUE_NAME);
MessageProducer messageProducer = session.createProducer(null);
```

Now send the message:

*QueueProducer.java*
```java
TextMessage message = session.createTextMessage("Hello world Queues!");
messageProducer.send(queue, message, DeliveryMode.PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
```

## Receiving a persistent message from a queue

In order to receive a persistent message from a queue a JMS *MessageConsumer* needs to be created.

![]({{ site.baseurl }}/images/persistence-with-queues-details-1.png)

The name of the queue is the same as the one to which we send messages.

*QueueConsumer.java*
```java
final String QUEUE_NAME = "Q/tutorial";

Queue queue = session.createQueue(QUEUE_NAME);
MessageConsumer messageConsumer = session.createConsumer(queue);
```

As in the [publish/subscribe tutorial]({{ site.baseurl }}/publish-subscribe){:target="_blank"}, we will be using the anonymous inner class for receiving messages asynchronously, with an addition of the `message.acknowledge()` call.

*QueueConsumer.java*
```java
messageConsumer.setMessageListener(new MessageListener() {
    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                System.out.printf("TextMessage received: '%s'%n", ((TextMessage) message).getText());
            } else {
                System.out.println("Message received.");
            }

            message.acknowledge();

            System.out.printf("Message Content:%n%s%n", message.toString());
            latch.countDown(); // unblock the main thread
        } catch (Exception ex) {
            System.out.println("Error processing incoming message.");
            ex.printStackTrace();
        }
    }
});
connection.start();
latch.await();
```

## Summarizing

Combining the example source code shown above results in the following source code files:

*   [QueueProducer.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/QueueProducer.java){:target="_blank"}
*   [QueueConsumer.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/QueueConsumer.java){:target="_blank"}

### Getting the Source

Clone the GitHub repository containing the Solace samples.

```
git clone {{ site.repository }}
cd {{ site.baseurl | remove: '/'}}
```

### Building

You can build and run both example files directly from Eclipse or with Gradle.

```sh
./gradlew assemble
```

The examples can be run as:

```sh
cd build/staged/bin
./queueConsumer amqp://SOLACE_HOST:AMQP_PORT
./queueProducer amqp://SOLACE_HOST:AMQP_PORT
```

### Sample Output

First start the `QueueConsumer` so that it is up and waiting for messages.

```sh
$ queueConsumer amqp://SOLACE_HOST:AMQP_PORT
QueueConsumer is connecting to Solace router amqp://SOLACE_HOST:AMQP_PORT...
Awaiting message...
```

Then you can start the `QueueProducer` to send the message.

```sh
$ queueProducer amqp://SOLACE_HOST:AMQP_PORT
QueueProducer is connecting to Solace router amqp://amqp://SOLACE_HOST:AMQP_PORT...
Connected with username 'clientUsername'.
Sending message 'Hello world Queues!' to queue 'Q/tutorial'...
Sent successfully. Exiting...
```

Notice how the message is received by the `QueueConsumer`.

```sh
Awaiting message...
TextMessage received: 'Hello world Queues!'
Message Content:
JmsTextMessage { org.apache.qpid.jms.provider.amqp.message.AmqpJmsTextMessageFacade@529bd520 }
```

Now you know how to use Apache Qpid JMS 1.1 over AMQP using the Solace Message Router to send and receive persistent messages from a queue.

If you have any issues sending and receiving message or reply, check the [Solace community]({{ site.links-community }}){:target="_top"} for answers to common issues seen.
