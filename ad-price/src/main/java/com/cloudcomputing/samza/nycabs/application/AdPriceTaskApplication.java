package com.cloudcomputing.samza.nycabs.application;

import com.cloudcomputing.samza.nycabs.AdPriceTask;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.samza.application.TaskApplication;
import org.apache.samza.application.descriptors.TaskApplicationDescriptor;
import org.apache.samza.serializers.JsonSerde;
import org.apache.samza.system.kafka.descriptors.KafkaSystemDescriptor;
import org.apache.samza.system.kafka.descriptors.KafkaInputDescriptor;
import org.apache.samza.system.kafka.descriptors.KafkaOutputDescriptor;
import org.apache.samza.task.StreamTaskFactory;

import java.util.List;
import java.util.Map;

public class AdPriceTaskApplication implements TaskApplication {
    // Consider modify this zookeeper address, localhost may not be a good choice.
    // If this task application is executing in slave machine.
    private static final List<String> KAFKA_CONSUMER_ZK_CONNECT = ImmutableList.of(":2181"); // TODO: Fill in

    // Consider modify the bootstrap servers address. This example only cover one address.
    private static final List<String> KAFKA_PRODUCER_BOOTSTRAP_SERVERS = 
            ImmutableList.of(":9092"); // TODO: Fill in
    private static final Map<String, String> KAFKA_DEFAULT_STREAM_CONFIGS = ImmutableMap.of("replication.factor", "1");

    @Override
    public void describe(TaskApplicationDescriptor taskApplicationDescriptor) {
        // Define a system descriptor for Kafka.
        KafkaSystemDescriptor kafkaSystemDescriptor =
                new KafkaSystemDescriptor("kafka").withConsumerZkConnect(KAFKA_CONSUMER_ZK_CONNECT)
                        .withProducerBootstrapServers(KAFKA_PRODUCER_BOOTSTRAP_SERVERS)
                        .withDefaultStreamConfigs(KAFKA_DEFAULT_STREAM_CONFIGS);

        // Hint about streams, please refer to AdPriceConfig.java
        // We need one input stream "ad-click", one output stream "ad-price".
        
        // Define your input and output descriptor in here.
        // Reference solution:
        //  https://github.com/apache/samza-hello-samza/blob/master/src/main/java/samza/examples/wikipedia/task/application/WikipediaStatsTaskApplication.java
        KafkaInputDescriptor adClickInputDescriptor = 
                kafkaSystemDescriptor.getInputDescriptor("ad-click", new JsonSerde<>());
        
        KafkaInputDescriptor adPriceOutputDescriptor = 
                kafkaSystemDescriptor.getOutputDescriptor("ad-price", new JsonSerde<>());
        // Bound you descriptor with your taskApplicationDescriptor in here.
        // Please refer to the same link.
        taskApplicationDescriptor.withDefaultSystem(kafkaSystemDescriptor);
        taskApplicationDescriptor.withInputStream(adClickInputDescriptor);
        taskApplicationDescriptor.withOutputStream(adPriceOutputDescriptors);

        taskApplicationDescriptor.withTaskFactory((StreamTaskFactory)() -> new AdPriceTask());
    }
}
