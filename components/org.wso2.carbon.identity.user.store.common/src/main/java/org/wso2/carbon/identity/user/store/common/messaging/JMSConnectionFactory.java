/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.user.store.common.messaging;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

public class JMSConnectionFactory {

    private static Log LOGGER = LogFactory.getLog(JMSConnectionFactory.class);

    private ActiveMQConnectionFactory connectionFactory;
    private boolean transactedSession = false;

    /**
     * Create connection factory
     * @param messageBrokerURL message broker URL
     * @return
     */
    public ActiveMQConnectionFactory createActiveMQConnectionFactory(String messageBrokerURL) {
        if (null != this.connectionFactory) {
            return this.connectionFactory;
        }
        this.connectionFactory = new ActiveMQConnectionFactory(messageBrokerURL);

        return this.connectionFactory;
    }

    /**
     * Create connection
     * @return JMS connection
     * @throws JMSConnectionException
     */
    public Connection createConnection() throws JMSConnectionException {
        if (null == connectionFactory) {
            throw new JMSConnectionException(
                    "Connection cannot be establish to the broker. Connection Factory is null. Please "
                            + "check the Please check the broker libs provided");
        }
        Connection connection = null;
        try {
            connection = this.connectionFactory.createQueueConnection();
            return connection;

        } catch (JMSException e) {
            // Need to close the connection in the case if durable subscriptions
            if (null != connection) {
                try {
                    connection.close();
                } catch (Exception ex) {
                    LOGGER.error("Error while closing the connection", ex);
                }
            }
            throw new JMSConnectionException("Error occurred while creating queue connection", e);
        }
    }

    /**
     * Start the jms connection to start the message delivery.
     *
     * @param connection Connection that need to be started
     * @throws JMSConnectionException Thrown when starting jms connection
     */
    public void start(Connection connection) throws JMSConnectionException {
        try {
            connection.start();
        } catch (JMSException e) {
            throw new JMSConnectionException("JMS Exception while starting connection", e);
        }
    }

    /**
     * Create a message consumer for particular session and destination.
     *
     * @param session     JMS Session to create the consumer
     * @param destination JMS destination which the consumer should listen to
     * @return Message Consumer, who is listening in particular destination with the given session
     * @throws JMSConnectionException Thrown when creating jms message consumer
     */
    public MessageConsumer createMessageConsumer(Session session, Destination destination)
            throws JMSConnectionException {
        try {
            return session.createConsumer(destination);
        } catch (Exception e) {
            throw new JMSConnectionException("JMS Exception while creating consumer for the destination", e);
        }
    }

    /**
     * Create message producer
     * @param session JMS Session
     * @param destination Destination queue or topic
     * @return
     * @throws JMSConnectionException
     */
    public MessageProducer createMessageProducer(Session session, Destination destination)
            throws JMSConnectionException {
        try {
            return session.createProducer(destination);
        } catch (JMSException e) {
            throw new JMSConnectionException("JMS Exception while creating the producer for the destination", e);
        }
    }

    /**
     * Create message producer
     * @param session JMS Session
     * @param destination Destination queue or topic
     * @param deliveryMode delivery mode
     * @return
     * @throws JMSConnectionException
     */
    public MessageProducer createMessageProducer(Session session, Destination destination, int deliveryMode)
            throws JMSConnectionException {
        MessageProducer producer;
        try {
            producer = session.createProducer(destination);
            producer.setDeliveryMode(deliveryMode);
        } catch (JMSException e) {
            throw new JMSConnectionException("JMS Exception while creating the producer for the destination", e);
        }
        return producer;
    }

    /**
     * To create the destination.
     *
     * @param session         relevant session to create the destination
     * @param destinationName Destination jms destination
     * @return the destination that is created from session
     * @throws JMSConnectionException Thrown when looking up destination
     */
    public Destination createQueueDestination(Session session, String destinationName) throws JMSConnectionException {
        Destination destination = null;
        try {
            destination = session.createQueue(destinationName);
        } catch (JMSException e) {
            throw new JMSConnectionException("Error occurred while creating queue destination", e);
        }

        return destination;
    }

    /**
     * To create the topic destination.
     *
     * @param session         relevant session to create the destination
     * @param destinationName Destination jms destination
     * @return the destination that is created from session
     * @throws JMSConnectionException Thrown when looking up destination
     */
    public Destination createTopicDestination(Session session, String destinationName) throws JMSConnectionException {
        Destination destination = null;
        try {
            destination = session.createTopic(destinationName);
        } catch (JMSException e) {
            throw new JMSConnectionException("Error occurred while creating topic destination", e);
        }

        return destination;
    }

    /**
     * To get a session with the given connection.
     *
     * @param connection Connection that is needed to create the session
     * @return Session that is created from the connection
     * @throws JMSConnectionException Thrown when creating jms session
     */
    public Session getSession(Connection connection) throws JMSConnectionException {
        return createSession(connection);
    }

    /**
     * To create a session from the given connection.
     *
     * @param connection Specific connection which we is needed for creating session
     * @return session created from the given connection
     * @throws JMSConnectionException Thrown when creating jms session
     */
    public Session createSession(Connection connection) throws JMSConnectionException {
        try {
            return connection.createSession(transactedSession, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException e) {
            throw new JMSConnectionException("JMS Exception while obtaining session for factory", e);
        }
    }

    /**
     * Close the jms connection.
     *
     * @param connection JMS connection that need to be closed
     * @throws JMSConnectionException Thrown when closing jms connection
     */
    public void closeConnection(Connection connection) throws JMSConnectionException {
        try {
            if (null != connection) {
                connection.close();
            }
        } catch (JMSException e) {
            throw new JMSConnectionException("JMS Exception while closing the connection", e);
        }
    }

    /**
     * To close the session.
     *
     * @param session JMS session that need to be closed
     * @throws JMSConnectionException Thrown when closing jms session
     */
    public void closeSession(Session session) throws JMSConnectionException {
        try {
            if (null != session) {
                session.close();
            }
        } catch (JMSException e) {
            throw new JMSConnectionException("JMS Exception while closing the session", e);
        }
    }

    /**
     * To close the message consumer.
     *
     * @param messageConsumer Message consumer that need to be closed
     * @throws JMSConnectionException Thrown when closing jms message consumer
     */
    public void closeMessageConsumer(MessageConsumer messageConsumer) throws JMSConnectionException {
        try {
            if (null != messageConsumer) {
                messageConsumer.close();
            }
        } catch (JMSException e) {
            throw new JMSConnectionException("JMS Exception while closing the subscriber", e);
        }
    }

    /**
     * To close the message producer.
     *
     * @param messageProducer Message producer that need to be closed
     * @throws JMSConnectionException Thrown when closing jms message producer
     */
    public void closeMessageProducer(MessageProducer messageProducer) throws JMSConnectionException {
        try {
            if (messageProducer != null) {
                messageProducer.close();
            }
        } catch (JMSException e) {
            throw new JMSConnectionException("JMS Exception while closing the producer", e);
        }
    }

}
