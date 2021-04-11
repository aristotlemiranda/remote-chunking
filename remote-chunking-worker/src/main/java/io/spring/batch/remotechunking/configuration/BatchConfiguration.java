
package io.spring.batch.remotechunking.configuration;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.Session;
import javax.sql.DataSource;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.integration.chunk.ChunkProcessorChunkHandler;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.support.PeriodicTrigger;

import io.spring.batch.remotechunking.domain.Transaction;

/**
 * @author Michael Minella
 */
@Configuration
public class BatchConfiguration {

	private static JMSContext context;

	@Configuration
	@Profile("worker")
	public static class WorkerConfiguration {
		
		@Bean(name = PollerMetadata.DEFAULT_POLLER)
		public PollerMetadata defaultPoller() {

			PollerMetadata pollerMetadata = new PollerMetadata();
			pollerMetadata.setTrigger(new PeriodicTrigger(10));
			return pollerMetadata;
		}
		
		
		//@Bean
		public static ConnectionFactory connectionFactory() {
			ConnectionFactory connectionFactory = new JmsConnectionFactory(
					"admin", 
					"admin", 
					"amqp://localhost:5672");
			
			context = connectionFactory.createContext(Session.CLIENT_ACKNOWLEDGE);
			//context.createQueue("requests");
			//context.acknowledge();
			return connectionFactory;
		}

		
		@Bean
		public Queue requestQueue() {
			return new Queue("requests", false);
		}

		@Bean
		public Queue repliesQueue() {
			return new Queue("replies", false);
		}

		@Bean
		public TopicExchange exchange() {
			return new TopicExchange("remote-chunking-exchange");
		}

		@Bean
		Binding repliesBinding(TopicExchange exchange) {
			return BindingBuilder.bind(repliesQueue()).to(exchange).with("replies");
		}

		@Bean
		Binding requestBinding(TopicExchange exchange) {
			return BindingBuilder.bind(requestQueue()).to(exchange).with("requests");
		}

		@Bean
		public DirectChannel requests() {
			return new DirectChannel();
		}

		@Bean
		public DirectChannel replies() {
			return new DirectChannel();
		}

		@Bean
		public IntegrationFlow mesagesIn() {
			return IntegrationFlows.from(Jms.inboundAdapter(connectionFactory()).destination("requests")).channel(requests()).get();
		}

		@Bean("outgoing")
		public IntegrationFlow outgoingReplies() {
			return IntegrationFlows.from("replies").handle(Jms.outboundAdapter(connectionFactory()).destination("replies")).get();
		}

		@Bean

		@ServiceActivator(inputChannel = "requests", outputChannel = "replies", sendTimeout = "10000")
		public ChunkProcessorChunkHandler<Transaction> chunkProcessorChunkHandler() {
			ChunkProcessorChunkHandler<Transaction> chunkProcessorChunkHandler = new ChunkProcessorChunkHandler<>();
			chunkProcessorChunkHandler.setChunkProcessor(new SimpleChunkProcessor<>((transaction) -> {
				System.out.println(">> processing transaction: " + transaction);
				Thread.sleep(5);
				return transaction;
			}, writer(null)));

			return chunkProcessorChunkHandler;
		}

		@Bean
		public JdbcBatchItemWriter<Transaction> writer(DataSource dataSource) {
			return new JdbcBatchItemWriterBuilder<Transaction>().dataSource(dataSource).beanMapped()
					.sql("INSERT INTO TRANSACTION (ACCOUNT, AMOUNT, TIMESTAMP) VALUES (:account, :amount, :timestamp)")
					.build();
		}
	}
}
