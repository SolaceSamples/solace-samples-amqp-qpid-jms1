---
layout: tutorials
title: Request/Reply
summary: Demonstrates the request/reply message exchange pattern
icon: request-reply-icon.png
---

This tutorial builds on the basic concepts introduced in the [publish/subscribe tutorial]({{ site.baseurl }}/publish-subscribe){:target="_blank"}, and will show you how to send a request, reply to it, and receive the reply with Apache Qpid JMS 1.1 client using AMQP and Solace Message Router. This the request/reply message exchange pattern as illustrated here:

![Sample Image Text]({{ site.baseurl }}/images/request-reply-icon.png)

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

One simple way to get access to a Solace message router is to start a Solace VMR load [as outlined here]({{ site.docs-vmr-setup }}){:target="_top"}. By default the Solace VMR will run with the “default” message VPN configured and ready for messaging. Going forward, this tutorial assumes that you are using the Solace VMR. If you are using a different Solace message router configuration, adapt the instructions to match your configuration.

## Goals

The goal of this tutorial is to demonstrate how to use Apache Qpid JMS 1.1 over AMQP using the Solace Message Router. This tutorial will show you:

1. How to build and send a request message
2. How to receive a request message and respond to it

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
<td>The “default” Solace message router Message VPN that this client will connect to.</td>
</tr>
<tr>
<td>Client Username</td>
<td>String</td>
<td>The “default” client username.</td>
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

As in the [publish/subscribe tutorial]({{ site.baseurl }}/publish-subscribe){:target="_blank"} tutorial, an application must start a JMS connection and a session.

*TopicPublisher.java/TopicSubscriber.java*
```java
final String SOLACE_USERNAME = "clientUsername";
final String SOLACE_PASSWORD = "password";

ConnectionFactory connectionFactory = new JmsConnectionFactory(SOLACE_USERNAME, SOLACE_PASSWORD, solaceHost);
Connection connection = connectionFactory.createConnection();
Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
```

At this point the application is connected to the Solace Message Router and ready to send and receive request and reply messages.

## Sending a request

In order to send a request in the *Requestor* a JMS *MessageProducer* needs to be created.

![]({{ site.baseurl }}/images/request-reply-details-2.png)

*BasicRequestor.java*
```java
final String REQUEST_TOPIC_NAME = "T/GettingStarted/requests";

Topic requestTopic = session.createTopic(REQUEST_TOPIC_NAME);
MessageProducer requestProducer = session.createProducer(requestTopic);
```

Also, it is necessary to allocate a temporary queue for receiving the reply and to create a JMS *MessageConsumer* for it.

*BasicRequestor.java*
```java
TemporaryQueue replyToQueue = session.createTemporaryQueue();
MessageConsumer replyConsumer = session.createConsumer(replyToQueue);
```

The request must have two properties assigned: `JMSReplyTo` and `JMSCorrelationID`.

The `JMSReplyTo` property needs to have the value of the temporary queue for receiving the reply that was already created.

The `JMSCorrelationID` property needs to have an unique value so the requestor to correlate the request with the subsequent reply.

The figure below outlines the exchange of messages and the role of both properties.


![]({{ site.baseurl }}/images/request-reply-details-1.png)


*BasicRequestor.java*
```java
TextMessage request = session.createTextMessage("Sample Request");
String correlationId = UUID.randomUUID().toString();
request.setJMSCorrelationID(correlationId);
```

Now send the request:

*BasicRequestor.java*
```java
requestProducer.send(requestTopic, request, DeliveryMode.NON_PERSISTENT,
        Message.DEFAULT_PRIORITY,
        Message.DEFAULT_TIME_TO_LIVE);
```

## Receiving a request

In order to receive a request from a queue a JMS *MessageConsumer* needs to be created.

*BasicReplier.java*
```java
final String REQUEST_TOPIC_NAME = "T/GettingStarted/requests";

Topic requestTopic = session.createTopic(REQUEST_TOPIC_NAME);
MessageConsumer requestConsumer = session.createConsumer(requestTopic);
```

As in the [publish/subscribe tutorial]({{ site.baseurl }}/publish-subscribe){:target="_blank"}, we will be using the anonymous inner class for receiving messages asynchronously.

*BasicReplier.java*
```java
requestConsumer.setMessageListener(new MessageListener() {
    @Override
    public void onMessage(Message request) {
...
});
```

## Replying to a request

To reply to a received request a JMS *MessageProducer* needs to be created in the *Replier*.

![Request-Reply_diagram-3]({{ site.baseurl }}/images/request-reply-details-3.png)

The JMS *MessageProducer* is created without its target queue as it will be assigned from the `JMSReplyTo` property value of the received request.

*BasicReplier.java*
```java
MessageProducer replyProducer = session.createProducer(null);
```

The reply message must have the `JMSCorrelationID` property value assigned from the received request.

*BasicReplier.java*
```java
TextMessage reply = session.createTextMessage();
String text = "Sample response";
reply.setText(text);

reply.setJMSCorrelationID(request.getJMSCorrelationID());
```

Now we can send the reply message.

We must send it to the temporary queue that was created by the requestor. We need to create an instance of the `org.apache.qpid.jms.JmsDestination` class for the reply destination and assign it a name from the request `JMSReplyTo` property because of the Apache Qpid JMS implementation.

*BasicReplier.java*
```java
Destination replyDestination = request.getJMSReplyTo();
String replyDestinationName = ((JmsDestination) replyDestination).getName();
replyDestination = new JmsTemporaryQueue(replyDestinationName);
replyProducer.send(replyDestination, reply, DeliveryMode.NON_PERSISTENT,
        Message.DEFAULT_PRIORITY,
        Message.DEFAULT_TIME_TO_LIVE);
```

The reply will be received by `BasicRequestor` in a main thread after the following call unblocks:

```java
final int REPLY_TIMEOUT_MS = 10000;
Message reply = replyConsumer.receive(REPLY_TIMEOUT_MS);
```

## Summarizing

Combining the example source code shown above results in the following source code files:

*   [BasicRequestor.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/BasicRequestor.java){:target="_blank"}
*   [BasicReplier.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/BasicReplier.java){:target="_blank"}

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
./basicReplier amqp://SOLACE_HOST:AMQP_PORT
./basicRequestor amqp://SOLACE_HOST:AMQP_PORT
```


### Sample Output

First start the `BasicReplier` so that it is up and waiting for requests.

```sh
$ basicReplier amqp://SOLACE_HOST:AMQP_PORT
BasicReplier is connecting to Solace router amqp://SOLACE_HOST:AMQP_PORT...
Connected to the Solace router with client username 'clientUsername'.
Awaiting request...
```

Then you can start the `BasicRequestor` to send the request and receive the reply.
```sh
$ basicRequestor amqp://SOLACE_HOST:AMQP_PORT
BasicRequestor is connecting to Solace router amqp://SOLACE_HOST:AMQP_PORT...
Connected to the Solace router with client username 'clientUsername'.
Sending request 'Sample Request' to topic 'T/GettingStarted/requests'...
Sent successfully. Waiting for reply...
TextMessage response received: 'Sample response'
Message Content:
JmsTextMessage { org.apache.qpid.jms.provider.amqp.message.AmqpJmsTextMessageFacade@64bf3bbf }
```

Notice how the request is received by the `BasicReplier` and replied to.

```sh
Awaiting request...
Received request, responding...
Responded successfully. Exiting...
```

Now you know how to use Apache Qpid JMS 1.1 over AMQP using the Solace Message Router to implement the request/reply message exchange pattern.

If you have any issues sending and receiving request or reply, check the [Solace community]({{ site.links-community }}){:target="_top"} for answers to common issues seen.
