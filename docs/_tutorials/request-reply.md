---
layout: tutorials
title: Request/Reply
summary: Demonstrates the request/reply message exchange pattern
icon: request-reply-icon.png
---

This tutorial builds on the basic concepts introduced in the [persistence with queues tutorial]({{ site.baseurl }}/persistence-with-queues){:target="_blank"}, and will show you how to send a request, reply to it, and receive the reply. This the request/reply message exchange pattern as illustrated here:

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
    * A durable queue with the name `amqp/tutorial/queue` exists on the `default` message VPN.
         * See [Configuring Queues]({{ site.docs-confugure-queues }}){:target="_blank"} for details on how to configure durable queues on Solace Message Routers with Solace CLI.
         * See [Management Tools]({{ site.docs-management-tools }}){:target="_top"} for other tools for configure durable queues.

One simple way to get access to a Solace message router is to start a Solace VMR load [as outlined here]({{ site.docs-vmr-setup }}){:target="_top"}. By default the Solace VMR will run with the “default” message VPN configured and ready for messaging. Going forward, this tutorial assumes that you are using the Solace VMR. If you are using a different Solace message router configuration, adapt the instructions to match your configuration.

## Goals

The goal of this tutorial is to demonstrate how to use JMS 1.1 API over AMQP using the Solace Message Router. This tutorial will show you:

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

## Obtaining JMS 1.1 API

This tutorial depends on you having the [Apache Qpid JMS client](https://qpid.apache.org/components/jms/index.html) downloaded and installed for your project, and the instructions in this tutorial assume you successfully done it. If your environment differs then adjust the build instructions appropriately.

The easiest way to do it through Maven. See the project's *pom.xml* file for details.

## Connecting to the Solace Message Router

In order to send or receive messages, an application must start a JMS connection.

There is only one required parameter for establishing the JMS connection: the Solace Message Router host name with the AMQP service port number. The value of this parameter is loaded in the examples by the `javax.naming.InitialContext.InitialContext()` from the *jndi.properties* project's file, but of course it could be assigned directly in the application by assigning the corresponding environment variable.

*jndi.properties*
~~~
java.naming.factory.initial = org.apache.qpid.jms.jndi.JmsInitialContextFactory
connectionfactory.solaceConnectionLookup = amqp://192.168.123.45:8555
~~~

Because the request/reply pattern uses the point-to-point messaging model, the specialized `QueueConnectionFactory` and `QueueConnection` are used.

*SimpleRequestor.java/SimpleReplier.java*
~~~java
Context initialContext = new InitialContext();
QueueConnectionFactory factory = (QueueConnectionFactory) initialContext.lookup("solaceConnectionLookup");

try (QueueConnection connection = factory.createQueueConnection()) {
    connection.setExceptionListener(new QueueConnectionExceptionListener());
    connection.start();
...
~~~

The target for  messages will be a JMS Queue, therefore a session of the `javax.jms.QueueSession` type needs to be created. The session will be non-transacted using the acknowledge mode that automatically acknowledges a client's receipt of a message.

*SimpleRequestor.java/SimpleReplier.java*
~~~java
try (QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)) {
...
~~~

At this point the application is connected to the Solace Message Router and ready to send and receive request and reply messages.

## Sending a request

In order to send a request to a queue a JMS queue sender needs to be created.

![]({{ site.baseurl }}/images/request-reply-details-2.png)

We assign its delivery mode to `non-persistent` for better performance.

The name of the queue is loaded by the `javax.naming.InitialContext.InitialContext()` from the *jndi.properties* project's file. It must exist on the Solace Message Router as a `durable queue`.

*jndi.properties*
~~~
queue.queueLookup = amqp/tutorial/queue
~~~

*SimpleRequestor.java*
~~~java
Queue target = (Queue) initialContext.lookup("queueLookup");
try (QueueSender requestSender = session.createSender(target)) {
    requestSender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
...
~~~

Also, it is necessary to allocate a temporary queue for receiving the reply.

*SimpleRequestor.java*
~~~java
TemporaryQueue replyQueue = session.createTemporaryQueue();
try (QueueReceiver replyReceiver = session.createReceiver(replyQueue)) {
    replyReceiver.setMessageListener(this);
~~~

Because the `SimpleRequestor` class will be receiving replies, it needs to implement `javax.jms.MessageListener`:

*SimpleRequestor.java*
~~~java
public class SimpleRequestor implements MessageListener {
<...>
    @Override
    public void onMessage(Message message) {
        try {
            LOG.info("Received reply: \"{}\"", ((TextMessage) message).getText());
<...>
~~~

The request must have two properties assigned: `JMSReplyTo` and `JMSCorrelationID`.

The `JMSReplyTo` property needs to have the value of the temporary queue for receiving the reply that was already created.

The `JMSCorrelationID` property needs to have an unique value so the requestor to correlate the request with the subsequent reply.

The figure below outlines the exchange of messages and the role of both properties.


![]({{ site.baseurl }}/images/request-reply-details-1.png)


*SimpleRequestor.java*
~~~java
TextMessage request = session.createTextMessage("Request with String Data");
request.setJMSReplyTo(replyQueue);
request.setJMSCorrelationID(UUID.randomUUID().toString());
~~~

Now send the request:

*SimpleRequestor.java*
~~~java
requestSender.send(request);
~~~

## Receiving a request

In order to receive a request from a queue a JMS queue receiver needs to be created.

The name of the queue is loaded by the `javax.naming.InitialContext.InitialContext()` from the *jndi.properties* project's file, and it is the same as the one to which we send requests.

*jndi.properties*
~~~
queue.queueLookup = amqp/tutorial/queue
~~~

*SimpleReplier.java*
~~~java
Queue source = (Queue) initialContext.lookup("queueLookup");
try (QueueReceiver requestConsumer = session.createReceiver(source)) {
...
~~~

This is how we receive requests sent to the queue.

*SimpleReplier.java*
~~~java
Message request = requestConsumer.receive();
~~~

## Replying to a request

To reply to a received request a JMS queue sender needs to be created.

![Request-Reply_diagram-3]({{ site.baseurl }}/images/request-reply-details-3.png)

We assign its delivery mode to `non-persistent` for better performance.

The JMS queue sender is created without its target queue as it will be assigned from the `JMSReplyTo` property value of the received request.

*SimpleReplier.java*
~~~java
try (QueueSender replySender = session.createSender(null)) {
replySender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
...
~~~

The reply message must have the `JMSCorrelationID` property value assigned from the received request.

*SimpleReplier.java*
~~~java
Message request = requestConsumer.receive();
if (request instanceof TextMessage) {
    TextMessage requestTextMessage = (TextMessage) request;
    TextMessage replyMessage = session.createTextMessage(String.format("Reply to \"%s\"", requestTextMessage.getText()));
    replyMessage.setJMSCorrelationID(request.getJMSCorrelationID());
...
~~~

Now we can send the reply message.

We must send it to the temporary queue that was created by the requestor. We need to create an instance of the `org.apache.qpid.jms.JmsTemporaryQueue` class for the reply destination and assign it a name from the request `JMSReplyTo` property because of the Apache Qpid JMS implementation.

*SimpleReplier.java*
~~~java
Destination replyDestination = new JmsTemporaryQueue(((Queue) request.getJMSReplyTo()).getQueueName());
replySender.send(replyDestination, replyMessage);
~~~

The reply will be received in a separate thread by the `SimpleRequestor.onMessage` routine.

## Summarizing

Combining the example source code shown above results in the following source code files:

*   [SimpleRequestor.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/SimpleRequestor.java){:target="_blank"}
*   [SimpleReplier.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/SimpleReplier.java){:target="_blank"}

### Getting the Source

Clone the GitHub repository containing the Solace samples.

```
git clone {{ site.repository }}
cd {{ site.baseurl | remove: '/'}}
```

### Building

Modify the *jndi.properties* file to reflect your Solace Message Router host and port number for the AMQP service.

You can build and run both example files directly from Eclipse.

To build a jar file that includes all dependencies execute the following:

~~~sh
mvn assembly:single
~~~

Then the examples can be executed as:

~~~sh
java -cp ./target/solace-samples-amqp-jms1-1.0.1-SNAPSHOT-jar-with-dependencies.jar  com.solace.samples.SimpleReplier
java -cp ./target/solace-samples-amqp-jms1-1.0.1-SNAPSHOT-jar-with-dependencies.jar  com.solace.samples.SimpleRequestor
~~~

### Sample Output

First start the `SimpleReplier` so that it is up and waiting for requests.

~~~sh
$ java -cp ./target/solace-samples-amqp-jms1-1.0.1-SNAPSHOT-jar-with-dependencies.jar com.solace.samples.SimpleReplier
2017-06-28T17:04:47,941 INFO sasl.SaslMechanismFinder - Best match for SASL auth was: SASL-ANONYMOUS
2017-06-28T17:04:47,970 INFO jms.JmsConnection - Connection ID:a3c65ee4-4f12-4ab9-a8d8-d2b2252e93ee:1 connected to remote Broker: amqp://192.168.123.45:8555
2017-06-28T17:04:48,015 INFO samples.SimpleReplier - Waiting for a request...
~~~

Then you can start the `SimpleRequestor` to send the request and receive the reply.
~~~sh
$ java -cp ./target/solace-samples-amqp-jms1-1.0.1-SNAPSHOT-jar-with-dependencies.jar com.solace.samples.SimpleRequestor
2017-06-28T17:05:35,636 INFO sasl.SaslMechanismFinder - Best match for SASL auth was: SASL-ANONYMOUS
2017-06-28T17:05:35,663 INFO jms.JmsConnection - Connection ID:cf6970f3-6188-421b-9d93-9ac89f639d74:1 connected to remote Broker: amqp://192.168.123.45:8555
2017-06-28T17:05:35,743 INFO samples.SimpleRequestor - Request message sent successfully, waiting for a reply...
2017-06-28T17:05:35,771 INFO samples.SimpleRequestor - Received reply: "Reply to "Request with String Data""
~~~

Notice how the request is received by the `SimpleReplier` and replied to.

~~~sh
...
2017-06-28T17:04:48,015 INFO samples.SimpleReplier - Waiting for a request...
2017-06-28T17:05:35,758 INFO samples.SimpleReplier - Received AMQP request with string data: "Request with String Data"
2017-06-28T17:05:35,763 INFO samples.SimpleReplier - Request Message replied successfully.
~~~

Now you know how to use JMS 1.1 API over AMQP using the Solace Message Router to implement the request/reply message exchange pattern.

If you have any issues sending and receiving request or reply, check the [Solace community]({{ site.links-community }}){:target="_top"} for answers to common issues seen.
