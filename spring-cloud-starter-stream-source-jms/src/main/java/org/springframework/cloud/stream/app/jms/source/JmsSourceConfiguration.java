/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app.jms.source;

import javax.jms.ConnectionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.jms.JmsProperties.Listener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;

/**
 * A source that receives messages from JMS.
 *
 * @author Gary Russell
 * @author Chris Schaefer
 */
@EnableBinding(Source.class)
@EnableConfigurationProperties(JmsSourceProperties.class)
public class JmsSourceConfiguration {

	@Autowired
	private Source channels;

	@Autowired
	private JmsSourceProperties properties;

	@Autowired
	private JmsProperties jmsProperties;

	@Autowired
	private ConnectionFactory connectionFactory;

	@Bean
	public JmsMessageDrivenEndpoint adapter() {
		return new JmsMessageDrivenEndpoint(container(), listener());
	}

	@Bean
	public AbstractMessageListenerContainer container() {
		AbstractMessageListenerContainer container;
		Listener listenerProperties = this.jmsProperties.getListener();
		if (this.properties.isSessionTransacted()) {
			DefaultMessageListenerContainer dmlc = new DefaultMessageListenerContainer();
			dmlc.setSessionTransacted(true);
			if (listenerProperties.getConcurrency() != null) {
				dmlc.setConcurrentConsumers(listenerProperties.getConcurrency());
			}
			if (listenerProperties.getMaxConcurrency() != null) {
				dmlc.setMaxConcurrentConsumers(listenerProperties.getMaxConcurrency());
			}
			container = dmlc;
		}
		else {
			SimpleMessageListenerContainer smlc = new SimpleMessageListenerContainer();
			smlc.setSessionTransacted(false);
			if (listenerProperties != null  && listenerProperties.getConcurrency() != null) {
				smlc.setConcurrentConsumers(listenerProperties.getConcurrency());
			}
			container = smlc;
		}
		container.setConnectionFactory(this.connectionFactory);
		if (this.properties.getClientId() != null) {
			container.setClientId(this.properties.getClientId());
		}
		container.setDestinationName(this.properties.getDestination());
		if (this.properties.getMessageSelector() != null) {
			container.setMessageSelector(this.properties.getMessageSelector());
		}
		container.setPubSubDomain(this.jmsProperties.isPubSubDomain());
		if (this.properties.getMessageSelector() != null
				&& listenerProperties.getAcknowledgeMode() != null) {
			container.setSessionAcknowledgeMode(listenerProperties.getAcknowledgeMode().getMode());
		}
		if (this.properties.getSubscriptionDurable() != null) {
			container.setSubscriptionDurable(this.properties.getSubscriptionDurable());
		}
		if (this.properties.getSubscriptionName() != null) {
			container.setSubscriptionName(this.properties.getSubscriptionName());
		}
		if (this.properties.getSubscriptionShared() != null) {
			container.setSubscriptionShared(this.properties.getSubscriptionShared());
		}
		return container;
	}

	@Bean
	public ChannelPublishingJmsMessageListener listener() {
		ChannelPublishingJmsMessageListener listener = new ChannelPublishingJmsMessageListener();
		listener.setRequestChannel(channels.output());
		return listener;
	}

}
