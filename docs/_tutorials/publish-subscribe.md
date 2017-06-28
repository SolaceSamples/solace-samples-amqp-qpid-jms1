---
layout: tutorials
title: Publish/Subscribe
summary: Demonstrates the publish/subscribe message exchange pattern
icon: publish-subscribe-icon.png
---

This tutorial will demonstrate to you to how to connect a JMS 1.1 API client to a Solace Message Router using AMQP, add a topic subscription and publish a message matching this topic subscription. This the publish/subscribe message exchange pattern as illustrated here:

![Sample Image Text]({{ site.baseurl }}/images/publish-subscribe-icon.png)

## Assumptions

This tutorial assumes the following:

* You are familiar with Solace [core concepts]({{ site.docs-core-concepts }}){:target="_top"}.
* You have access to a running Solace message router with the following configuration:
    * Enabled “default” message VPN
    * Enabled “default” client username

One simple way to get access to a Solace message router is to start a Solace VMR load [as outlined here]({{ site.docs-vmr-setup }}){:target="_top"}. By default the Solace VMR will run with the “default” message VPN configured and ready for messaging. Going forward, this tutorial assumes that you are using the Solace VMR. If you are using a different Solace message router configuration, adapt the instructions to match your configuration.

## Goals

The goal of this tutorial is to demonstrate how to use JMS 1.1 API over AMQP using the Solace Message Router. This tutorial will show you:

1. How to build and send a message on a topic
2. How to subscribe to a topic and receive a message

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

*TopicPublisher.java/TopicSubscriber.java*
~~~java
Context initialContext = new InitialContext();
TopicConnectionFactory factory = (TopicConnectionFactory) initialContext.lookup("solaceConnectionLookup");

try (TopicConnection connection = factory.createTopicConnection()) {
    connection.setExceptionListener(new TopicConnectionExceptionListener());
    connection.start();
...
~~~

The target for publishing message will be a JMS Topic, therefore a session of the `javax.jms.TopicSession` type needs to be created. The session will be non-transacted with the acknowledge mode that automatically acknowledges a client's receipt of a message.

*TopicPublisher.java/TopicSubscriber.java*
~~~java
try (TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE)) {
...
~~~

At this point the application is connected to the Solace Message Router and ready to publish messages.

## Publishing a message

In order to publish a message to a topic a JMS topic publisher needs to be created. We assign its delivery mode to “non-persistent” for better performance.

The name of the topic is loaded by the `javax.naming.InitialContext.InitialContext()` from the *jndi.properties* project's file.

*jndi.properties*
~~~
topic.topicLookup = amqp.tutorial.topic
~~~

*TopicPublisher.java*
~~~java
Topic target = (Topic) initialContext.lookup("topicLookup");
try (javax.jms.TopicPublisher publisher = session.createPublisher(target)) {
    publisher.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
...
~~~

Now we can publish the message.

*TopicPublisher.java*
~~~java
publisher.publish(session.createTextMessage("Message with String Data"));
~~~

Now if you execute the `TopicPublisher.java` program, it will successfully publish a message. But of course we need another application to receive that message.

## Receiving a message

In order to receive a message from a topic a JMS topic subscriber needs to be created.

The name of the topic is loaded by the `javax.naming.InitialContext.InitialContext()` from the *jndi.properties* project's file and its name is the same as the one we publish messages to.

*jndi.properties*
~~~
topic.topicLookup = amqp.tutorial.topic
~~~

*TopicSubscriber.java*
~~~java
Topic source = (Topic) initialContext.lookup("topicLookup");
try (javax.jms.TopicSubscriber publisher = session.createSubscriber(source)) {
...
~~~

This is how we receive messages published to the subscribed topic.

*TopicSubscriber.java*
~~~java
Message message = subscriber.receive();
~~~

If you execute the `TopicSubscriber.java` program, it will block at the `subscriber.receive()` call until a message is received. Now if you execute the `TopicPublisher.java` that publishes a message, the `TopicSubscriber.java` program will resume and print out the received message.

## Summarizing

Combining the example source code shown above results in the following source code files:

*   [TopicPublisher.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/TopicPublisher.java){:target="_blank"}
*   [TopicSubscriber.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/TopicSubscriber.java){:target="_blank"}

## Building

Modify the *jndi.properties* file to reflect your Solace Message Router host and port number for the AMQP service.

You can build and run both example files directly from Eclipse.

To build a jar file that includes all dependencies execute the following:

~~~sh
mvn assembly:single
~~~

Then the examples can be executes as:

~~~sh
java -cp ./target/solace-samples-amqp-jms1-1.0.1-SNAPSHOT-jar-with-dependencies.jar  com.solace.samples.TopicSubscriber
java -cp ./target/solace-samples-amqp-jms1-1.0.1-SNAPSHOT-jar-with-dependencies.jar  com.solace.samples.TopicPublisher
~~~

## Sample Output

First start the `TopicSubscriber.exe` so that it is up and waiting for published messages. Of course you can start multiple instances of this application, all of them will receive a published message at once.

~~~sh
$ java -cp ./target/solace-samples-amqp-jms1-1.0.1-SNAPSHOT-jar-with-dependencies.jar  com.solace.samples.TopicSubscriber
2017-06-27T17:23:25,074 INFO sasl.SaslMechanismFinder - Best match for SASL auth was: SASL-ANONYMOUS
2017-06-27T17:23:25,107 INFO jms.JmsConnection - Connection ID:a76e8496-eee3-4a5c-a908-eac0e4789dc2:1 connected to remote Broker: amqp://192.168.133.16:8555
2017-06-27T17:23:25,137 INFO samples.TopicSubscriber - Waiting for a message...
~~~

Then you can start the `TopicPublisher.exe` to publish a message.
~~~sh
$  java -cp ./target/solace-samples-amqp-jms1-1.0.1-SNAPSHOT-jar-with-dependencies.jar com.solace.samples.TopicPublisher
2017-06-27T17:27:34,339 INFO sasl.SaslMechanismFinder - Best match for SASL auth was: SASL-ANONYMOUS
2017-06-27T17:27:34,364 INFO jms.JmsConnection - Connection ID:254fe96f-bf46-4d96-a4f1-74a25981785e:1 connected to remote Broker: amqp://192.168.133.16:8555
2017-06-27T17:27:34,401 INFO samples.TopicPublisher - Message published successfully.
~~~

Notice how the published message is received by the the `TopicSubscriber.exe`.

~~~sh
...
2017-06-27T17:27:05,405 INFO samples.TopicSubscriber - Waiting for a message...
2017-06-27T17:27:34,418 INFO samples.TopicSubscriber - Received message with string data: "Message with String Data"
~~~

With that now you know how to use JMS 1.1 API over AMQP using the Solace Message Router to implement the publish/subscribe message exchange pattern.

If you have any issues publishing and receiving a message, check the [Solace community]({{ site.links-community }}){:target="_top"} for answers to common issues seen.
