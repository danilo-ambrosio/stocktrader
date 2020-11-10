package com.whz.portfolio.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.whz.portfolio.infrastructure.StockQuoteData;

import io.vlingo.actors.Logger;
import io.vlingo.common.serialization.JsonSerialization;

public enum QuotesCache {

	INSTANCE();

	private final Logger logger = Logger.basicLogger();
	private final String EXCHANGE_NAME = "logs";
	private Map<String, StockQuoteData> stockQuotes = new HashMap<>();
	private Connection connection;

	private QuotesCache() {
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost("localhost");
			connection = factory.newConnection();
			Channel channel = connection.createChannel();

			channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
			String queueName = channel.queueDeclare().getQueue();
			channel.queueBind(queueName, EXCHANGE_NAME, "");

			logger.debug("Quotes cache is waiting for messages..");

			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				String message = new String(delivery.getBody(), "UTF-8");
				StockQuoteData stockQuoteData = deserialized(message);
				stockQuotes.put(stockQuoteData.symbol, stockQuoteData);
				logger.debug(
						"Quotes cache received '" + stockQuoteData.symbol + "' with a length of: " + message.length());
			};
			channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
			});
		} catch (IOException | TimeoutException e) {
			e.printStackTrace();
		}

	}

	private StockQuoteData deserialized(String data) {
		return JsonSerialization.deserialized(data, StockQuoteData.class);
	}

	public Collection<StockQuoteData> get() {
		return stockQuotes.values();
	}

	public StockQuoteData get(String id) {
		return stockQuotes.get(id);
	}

	public ArrayList<StockQuoteData> get(Collection<String> ids) {
		ArrayList<StockQuoteData> result = new ArrayList<>();
		ids.forEach(id -> {
			StockQuoteData data = stockQuotes.get(id);
			if (data != null) {
				result.add(data);
			}
		});
		return result;
	}

	public static void main(String[] args) {
		StockQuoteData data = INSTANCE.deserialized("{'symbol':'INTC','displayName':'Intel'}");
		System.out.println(data);
	}

	public void cleanUp() {
		try {
			connection.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
