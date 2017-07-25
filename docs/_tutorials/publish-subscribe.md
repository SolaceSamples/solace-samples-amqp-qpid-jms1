---
layout: tutorials
title: Publish/Subscribe
summary: Demonstrates the publish/subscribe message exchange pattern
icon: publish-subscribe-icon.png
---

This tutorial will show you to how to connect a JMS 1.1 API client to a Solace Message Router using AMQP, add a topic subscription and publish a message matching this topic subscription. This is the publish/subscribe message exchange pattern as illustrated here:

![Sample Image Text]({{ site.baseurl }}/images/publish-subscribe-icon.png)

This tutorial is available in [GitHub]({{ site.repository }}){:target="_blank"} along with the other [Solace Getting Started AMQP Tutorials]({{ site.links-get-started-amqp }}){:target="_top"}.

At the end, this tutorial walks through downloading and running the sample from source.

This tutorial focuses on using a non-Solace JMS API implementation. For using the Solace JMS API see [Solace Getting Started JMS Tutorials]({{ site.links-get-started-jms }}){:target="_blank"}.

## Assumptions

This tutorial assumes the following:

* You are familiar with Solace messaging [core concepts]({{ site.docs-core-concepts }}){:target="_top"}.
* You have access to a running Solace message router with the following configuration:
    * Enabled “default” message VPN
    * Enabled “default” client username

One simple way to get access to a Solace message router is to start a Solace VMR load [as outlined here]({{ site.docs-vmr-setup }}){:target="_top"}. By default the Solace VMR will run with the “default” message VPN configured and ready for messaging. Going forward, this tutorial assumes that you are using the Solace VMR. If you are using a different Solace message router configuration, adapt the instructions to match your configuration.

## Goals

The goal of this tutorial is to demonstrate how to use a JMS 1.1 API over AMQP using the Solace Message Router. This tutorial will show you:

1. How to build and send a message on a topic
2. How to subscribe to a topic and receive a message

## Solace message router properties

In order to send or receive messages to a Solace message router, you need to know a few details about how to connect to the router. Specifically, you need to know the following:

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
<td>This is the address clients use when connecting to the Solace Message Router to send and receive messages. For a Solace VMR this there is only a single interface so the IP is the same as the management IP address. For Solace message router appliances this is the host address of the message-backbone. The port number must match the port number for the plain text AMQP service on the router.</td>
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

JMS is a standard API for sending and receiving messages. As such, in addition to information provided on the Solace developer portal, you may also look at some external sources for more details about JMS. The following are good places to start:

1. [http://java.sun.com/products/jms/docs.html](http://java.sun.com/products/jms/docs.html){:target="_blank"}.
2. [https://en.wikipedia.org/wiki/Java_Message_Service](https://en.wikipedia.org/wiki/Java_Message_Service){:target="_blank"}
3. [https://docs.oracle.com/javaee/7/tutorial/partmessaging.htm#GFIRP3](https://docs.oracle.com/javaee/7/tutorial/partmessaging.htm#GFIRP3){:target="_blank"}

The last (Oracle docs) link points you to the JEE official tutorials which provide a good introduction to JMS.

This tutorial focuses on using [JMS 1.1 (April 12, 2002)]({{ site.links-jms1-specification }}){:target="_blank"}, for [JMS 2.0 (May 21, 2013)]({{ site.links-jms2-specification }}){:target="_blank"} see [Solace Getting Started AMQP JMS 2.0 Tutorials]({{ site.links-get-started-amqp-jms2 }}){:target="_blank"}.

## Obtaining JMS 1.1 API

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

There are three parameters for establishing the JMS connection: the Solace Message Router host name with the AMQP service port number, the client username and the optional password.

*TopicPublisher.java/TopicSubscriber.java*
```java
final String SOLACE_USERNAME = "clientUsername";
final String SOLACE_PASSWORD = "password";

ConnectionFactory connectionFactory = new JmsConnectionFactory(SOLACE_USERNAME, SOLACE_PASSWORD, solaceHost);
Connection connection = connectionFactory.createConnection();
```

Next, a session needs to be created. The session will be non-transacted using the acknowledge mode that automatically acknowledges a client's receipt of a message.

*TopicPublisher.java/TopicSubscriber.java*
```java
Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
```

At this point the application is connected to the Solace Message Router and ready to publish messages.

## Publishing messages

In order to publish a message to a topic a JMS message *MessageProducer* needs to be created.

![]({{ site.baseurl }}/images/publish-subscribe-details-2.png)

*TopicPublisher.java*
```java
final String TOPIC_NAME = "T/GettingStarted/pubsub";
Topic topic = session.createTopic(TOPIC_NAME);
MessageProducer messageProducer = session.createProducer(topic);
```

Now we can publish the message.

*TopicPublisher.java*
```java
TextMessage message = session.createTextMessage("Hello world!");
messageProducer.send(topic, message,
        DeliveryMode.NON_PERSISTENT,
        Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
```

Now if you execute the `TopicPublisher.java` program it will successfully publish a message, but another application is required to receive it.

## Receiving messages

In order to receive a message from a topic a JMS *MessageConsumer* needs to be created.

![]({{ site.baseurl }}/images/publish-subscribe-details-1.png)

*TopicSubscriber.java*
```java
final String TOPIC_NAME = "T/GettingStarted/pubsub";
Topic topic = session.createTopic(TOPIC_NAME);
MessageConsumer messageConsumer = session.createConsumer(topic);
```

We will be using the anonymous inner class for receiving messages asynchronously.

*TopicSubscriber.java*
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

If you execute the `TopicSubscriber.java` program, it will block at the `latch.await()` call until a message is received. Now if you execute the `TopicPublisher.java` program that publishes a message, the `TopicSubscriber.java` program will resume and print out the received message.

## Summary

Combining the example source code shown above results in the following source code files:

*   [TopicPublisher.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/TopicPublisher.java){:target="_blank"}
*   [TopicSubscriber.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/TopicSubscriber.java){:target="_blank"}

### Getting the Source

Clone the GitHub repository containing the Solace samples.

```
git clone {{ site.repository }}
cd {{ site.baseurl | remove: '/'}}
```

### Building

You can build and run both example files directly from Eclipse.

If you prefer to use the command line, build a jar file that includes all dependencies by executing the following:

```sh
mvn compile
mvn assembly:single
```
or

```sh
./gradlew assemble
```

The examples can be run as:

```sh
java -cp ./target/solace-samples-amqp-jms1-1.0.1-SNAPSHOT-jar-with-dependencies.jar  com.solace.samples.TopicSubscriber amqp://SOLACE_HOST:AMQP_PORT
java -cp ./target/solace-samples-amqp-jms1-1.0.1-SNAPSHOT-jar-with-dependencies.jar  com.solace.samples.TopicPublisher amqp://SOLACE_HOST:AMQP_PORT
```

or

```sh
cd build/staged/bin
./topicSubscriber amqp://SOLACE_HOST:AMQP_PORT
./topicPublisher amqp://SOLACE_HOST:AMQP_PORT
```

### Sample Output

First start the `TopicSubscriber` so that it is up and waiting for published messages. You can start multiple instances of this application, and all of them will receive published messages.

```sh
$ java -cp ./target/solace-samples-amqp-jms1-1.0.1-SNAPSHOT-jar-with-dependencies.jar  com.solace.samples.TopicSubscriber amqp://SOLACE_HOST:AMQP_PORT
TopicSubscriber is connecting to Solace router amqp://SOLACE_HOST:AMQP_PORT...
Connected to the Solace router.
Awaiting message...
```

Then you can start the `TopicPublisher` to publish a message.
```sh
$  java -cp ./target/solace-samples-amqp-jms1-1.0.1-SNAPSHOT-jar-with-dependencies.jar com.solace.samples.TopicPublisher amqp://SOLACE_HOST:AMQP_PORT
TopicPublisher is connecting to Solace router amqp://SOLACE_HOST:AMQP_PORT...
Connected to the Solace router.
Sending message 'Hello world!' to topic 'T/GettingStarted/pubsub'...
Sent successfully. Exiting...
```

Notice how the published message is received by the `TopicSubscriber`.

```sh
Awaiting message...
TextMessage received: 'Hello world!'
Message Content:
JmsTextMessage { org.apache.qpid.jms.provider.amqp.message.AmqpJmsTextMessageFacade@18c1752a }
```

With that you now know how to use the JMS 1.1 API over AMQP using the Solace Message Router to implement the publish/subscribe message exchange pattern.

If you have any issues with publishing and receiving messages, check the [Solace community]({{ site.links-community }}){:target="_top"} for answers to common issues seen.
