/**
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 * 
 * package io.spring.batch.remotechunking.configuration;
 * 
 * import javax.sql.DataSource;
 * 
 * import io.spring.batch.remotechunking.domain.Transaction;
 * 
 * import java.util.concurrent.Executor; import
 * java.util.concurrent.ThreadPoolExecutor;
 * 
 * import javax.jms.ConnectionFactory; import javax.jms.JMSContext; import
 * javax.jms.Session;
 * 
 * import org.springframework.amqp.core.AmqpTemplate; import
 * org.springframework.batch.core.Job; import
 * org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
 * import org.springframework.batch.core.configuration.annotation.StepScope;
 * import org.springframework.batch.core.launch.support.RunIdIncrementer; import
 * org.springframework.batch.core.step.tasklet.TaskletStep; import
 * org.springframework.batch.integration.chunk.
 * RemoteChunkingManagerStepBuilderFactory; import
 * org.springframework.batch.integration.chunk.RemoteChunkingWorkerBuilder;
 * import org.springframework.batch.integration.config.annotation.
 * EnableBatchIntegration; import org.springframework.batch.item.ItemProcessor;
 * import org.springframework.batch.item.database.JdbcBatchItemWriter; import
 * org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
 * import org.springframework.batch.item.file.FlatFileItemReader; import
 * org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder; import
 * org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.beans.factory.annotation.Value; import
 * org.springframework.context.annotation.Bean; import
 * org.springframework.context.annotation.Configuration; import
 * org.springframework.context.annotation.Profile; import
 * org.springframework.core.io.Resource; import
 * org.springframework.integration.amqp.dsl.Amqp; import
 * org.springframework.integration.channel.DirectChannel; import
 * org.springframework.integration.channel.QueueChannel; import
 * org.springframework.integration.config.EnableIntegration; import
 * org.springframework.integration.core.MessagingTemplate; import
 * org.springframework.integration.dsl.IntegrationFlow; import
 * org.springframework.integration.dsl.IntegrationFlows; import
 * org.springframework.integration.jms.dsl.Jms; import
 * org.springframework.integration.scheduling.PollerMetadata; import
 * org.springframework.jms.core.JmsTemplate; import
 * org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor; import
 * org.springframework.scheduling.support.PeriodicTrigger; import
 * org.apache.activemq.command.ActiveMQTopic; import
 * org.apache.qpid.jms.JmsConnectionFactory;
 * 
 *//**
	 * @author Michael Minella
	 *//*
		 * @Configuration
		 * 
		 * @EnableIntegration
		 * 
		 * @EnableBatchIntegration public class NotWorking {
		 * 
		 * @Configuration
		 * 
		 * @Profile("!worker") public static class MasterConfiguration {
		 * 
		 * @Autowired private JobBuilderFactory jobBuilderFactory;
		 * 
		 * @Autowired private RemoteChunkingManagerStepBuilderFactory
		 * remoteChunkingMasterStepBuilderFactory;
		 * 
		 * private JMSContext context;
		 * 
		 * @Bean public DirectChannel requests() { return new DirectChannel(); }
		 * 
		 * @Bean public MessagingTemplate messagingTemplate() { MessagingTemplate
		 * template = new MessagingTemplate(); template.setDefaultChannel(requests());
		 * template.setReceiveTimeout(2000); return template; }
		 * 
		 * @Bean public ConnectionFactory connectionFactory() { ConnectionFactory
		 * connectionFactory = new JmsConnectionFactory("admin", "admin",
		 * "amqp://localhost:5672"); context =
		 * connectionFactory.createContext(Session.CLIENT_ACKNOWLEDGE);
		 * context.createQueue("test.batch"); context.acknowledge(); return
		 * connectionFactory; }
		 * 
		 * @Bean public ActiveMQTopic mqTopic() { return new
		 * ActiveMQTopic("test/batch"); }
		 * 
		 * @Bean public QueueChannel jmsOutChannel() { return new QueueChannel(); }
		 * 
		 * @Bean(name = PollerMetadata.DEFAULT_POLLER) public PollerMetadata
		 * defaultPoller() {
		 * 
		 * PollerMetadata pollerMetadata = new PollerMetadata();
		 * pollerMetadata.setTrigger(new PeriodicTrigger(10)); return pollerMetadata; }
		 * 
		 * @Bean public IntegrationFlow outboundFlow(ConnectionFactory
		 * connectionFactory) { return IntegrationFlows.from(jmsOutChannel())
		 * .handle(Jms.outboundAdapter(connectionFactory).destination("test.batch")).get
		 * (); }
		 * 
		 * @Bean public QueueChannel replies() { return new QueueChannel(); }
		 * 
		 * @Bean public IntegrationFlow inboundFlow(ConnectionFactory connectionFactory)
		 * { return
		 * IntegrationFlows.from(Jms.inboundAdapter(connectionFactory).destination(
		 * "test.batch") .configureJmsTemplate(template ->
		 * template.receiveTimeout(-1))).channel("test/batch").get(); }
		 * 
		 * @Bean
		 * 
		 * @StepScope public FlatFileItemReader<Transaction> fileTransactionReader(
		 * 
		 * @Value("#{jobParameters['inputFlatFile']}") Resource resource) {
		 * 
		 * return new
		 * FlatFileItemReaderBuilder<Transaction>().saveState(false).resource(resource).
		 * delimited() .names(new String[] { "account", "amount", "timestamp"
		 * }).fieldSetMapper(fieldSet -> { Transaction transaction = new Transaction();
		 * 
		 * transaction.setAccount(fieldSet.readString("account"));
		 * transaction.setAmount(fieldSet.readBigDecimal("amount"));
		 * transaction.setTimestamp(fieldSet.readDate("timestamp",
		 * "yyyy-MM-dd HH:mm:ss"));
		 * 
		 * return transaction; }).build(); }
		 * 
		 * @Bean public TaskletStep masterStep() { return
		 * this.remoteChunkingMasterStepBuilderFactory.get("masterStep").<Transaction,
		 * Transaction>chunk(1)
		 * .reader(fileTransactionReader(null)).outputChannel(requests()).inputChannel(
		 * replies()).build(); }
		 * 
		 * @Bean public Job remoteChunkingJob() { return
		 * this.jobBuilderFactory.get("remoteChunkingJob").incrementer(new
		 * RunIdIncrementer()) .start(masterStep()).build(); } }
		 * 
		 * @Configuration
		 * 
		 * @Profile("worker") public static class WorkerConfiguration {
		 * 
		 * @Autowired private RemoteChunkingWorkerBuilder<Transaction, Transaction>
		 * workerBuilder;
		 * 
		 * @Autowired JmsTemplate jmsTemplate;
		 * 
		 * private JMSContext context;
		 * 
		 * @Bean(name = PollerMetadata.DEFAULT_POLLER) public PollerMetadata
		 * defaultPoller() {
		 * 
		 * PollerMetadata pollerMetadata = new PollerMetadata();
		 * pollerMetadata.setTrigger(new PeriodicTrigger(1)); return pollerMetadata; }
		 * 
		 * @Bean(name = "asyncExecutor") public Executor asyncExecutor() {
		 * ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		 * executor.setCorePoolSize(3); executor.setMaxPoolSize(3);
		 * executor.setQueueCapacity(100);
		 * executor.setThreadNamePrefix("AsynchThread-"); executor.initialize(); return
		 * executor; }
		 * 
		 * @Bean public ConnectionFactory connectionFactory() { ConnectionFactory
		 * connectionFactory = new JmsConnectionFactory("admin", "admin",
		 * "amqp://localhost:5672"); context =
		 * connectionFactory.createContext(Session.CLIENT_ACKNOWLEDGE);
		 * context.acknowledge();
		 * 
		 * return connectionFactory; }
		 * 
		 * @Bean public DirectChannel requests() { return new DirectChannel(); }
		 * 
		 * @Bean public DirectChannel replies() { return new DirectChannel(); }
		 * 
		 * @Bean public IntegrationFlow inboundFlow(ConnectionFactory connectionFactory)
		 * { return
		 * IntegrationFlows.from(Jms.inboundAdapter(connectionFactory).destination(
		 * "test.batch") .configureJmsTemplate(template ->
		 * template.receiveTimeout(-1))).channel(requests()).get(); }
		 * 
		 * @Bean public ActiveMQTopic mqTopic() { return new
		 * ActiveMQTopic("test/batch"); }
		 * 
		 * @Bean public QueueChannel jmsOutChannel() { return new QueueChannel(); }
		 * 
		 * @Bean public IntegrationFlow outboundFlow(ConnectionFactory
		 * connectionFactory) { return IntegrationFlows.from(jmsOutChannel())
		 * .handle(Jms.outboundAdapter(connectionFactory).destination("test.batch")).get
		 * (); }
		 * 
		 * @Bean public IntegrationFlow integrationFlow() { return
		 * this.workerBuilder.itemProcessor(processor()).itemWriter(writer(null)).
		 * inputChannel(requests()) .outputChannel(replies()).build(); }
		 * 
		 * @Bean public ItemProcessor<Transaction, Transaction> processor() { return
		 * transaction -> { System.out.println("processing transaction = " +
		 * transaction); return transaction; }; }
		 * 
		 * @Bean public JdbcBatchItemWriter<Transaction> writer(DataSource dataSource) {
		 * return new
		 * JdbcBatchItemWriterBuilder<Transaction>().dataSource(dataSource).beanMapped()
		 * .sql("INSERT INTO TRANSACTION (ACCOUNT, AMOUNT, TIMESTAMP) VALUES (:account, :amount, :timestamp)"
		 * ) .build(); } } }
		 */