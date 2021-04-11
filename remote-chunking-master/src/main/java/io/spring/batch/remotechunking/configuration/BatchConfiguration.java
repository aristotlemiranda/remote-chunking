
package io.spring.batch.remotechunking.configuration;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.Session;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.integration.chunk.ChunkMessageChannelItemWriter;
import org.springframework.batch.integration.chunk.RemoteChunkHandlerFactoryBean;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
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
	
	@Bean
	public static ConnectionFactory connectionFactory() {
		ConnectionFactory connectionFactory = new JmsConnectionFactory(
				"admin", 
				"admin", 
				"amqp://localhost:5672");
		context = connectionFactory.createContext(Session.CLIENT_ACKNOWLEDGE);
		//context.createQueue("requests");
		context.acknowledge();
		return connectionFactory;
	}
	
	@Configuration
	@Profile("!worker")
	public static class MasterConfiguration {

		@Autowired
		private JobBuilderFactory jobBuilderFactory;

		@Autowired
		private StepBuilderFactory stepBuilderFactory;
		
		
		@Autowired
		JmsTemplate jmsTemplate;


		@Bean(name = PollerMetadata.DEFAULT_POLLER)
		public PollerMetadata defaultPoller() {

			PollerMetadata pollerMetadata = new PollerMetadata();
			pollerMetadata.setTrigger(new PeriodicTrigger(10));
			return pollerMetadata;
		}
		
		@Bean
		public DirectChannel requests() {
			return new DirectChannel();
		}

		@Bean
		public IntegrationFlow outboundFlow(JmsTemplate jmsTemplate) {
			return IntegrationFlows.from("requests").handle(Jms.outboundAdapter(jmsTemplate).destination("requests"))
					.get();
		}

		@Bean
		public MessagingTemplate messagingTemplate() {
			MessagingTemplate template = new MessagingTemplate();
			template.setDefaultChannel(requests());
			template.setReceiveTimeout(2000);
			return template;
		}

		@Bean
		@StepScope
		public ChunkMessageChannelItemWriter<Transaction> itemWriter() {
			ChunkMessageChannelItemWriter<Transaction> chunkMessageChannelItemWriter = new ChunkMessageChannelItemWriter<>();
			chunkMessageChannelItemWriter.setMessagingOperations(messagingTemplate());
			chunkMessageChannelItemWriter.setReplyChannel(replies());
			return chunkMessageChannelItemWriter;
		}

		@Bean
		public RemoteChunkHandlerFactoryBean<Transaction> chunkHandler() {
			RemoteChunkHandlerFactoryBean<Transaction> remoteChunkHandlerFactoryBean = new RemoteChunkHandlerFactoryBean<>();
			remoteChunkHandlerFactoryBean.setChunkWriter(itemWriter());
			remoteChunkHandlerFactoryBean.setStep(step1());
			return remoteChunkHandlerFactoryBean;
		}

		@Bean
		public QueueChannel replies() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow replyFlow(ConnectionFactory connectionFactory) {
			return IntegrationFlows.from(Jms.inboundAdapter(connectionFactory).destination("replies")).channel(replies()).get();
		}

		@Bean

		@StepScope
		public FlatFileItemReader<Transaction> fileTransactionReader(

				@Value("#{jobParameters['inputFlatFile']}") Resource resource) {

			return new FlatFileItemReaderBuilder<Transaction>().saveState(false).resource(resource).delimited()
					.names(new String[] { "account", "amount", "timestamp" }).fieldSetMapper(fieldSet -> {
						Transaction transaction = new Transaction();

						transaction.setAccount(fieldSet.readString("account"));
						transaction.setAmount(fieldSet.readBigDecimal("amount"));
						transaction.setTimestamp(fieldSet.readDate("timestamp", "yyyy-MM-dd HH:mm:ss"));

						return transaction;
					}).build();
		}

		@Bean
		public TaskletStep step1() {
			return this.stepBuilderFactory.get("step1").<Transaction, Transaction>chunk(100)
					.reader(fileTransactionReader(null)).writer(itemWriter()).build();
		}

		@Bean
		public Job remoteChunkingJob() {
			return this.jobBuilderFactory.get("remoteChunkingJob").start(step1()).build();
		}
	}
}
