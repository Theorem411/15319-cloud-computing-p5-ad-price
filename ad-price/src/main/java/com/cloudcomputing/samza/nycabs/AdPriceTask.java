package com.cloudcomputing.samza.nycabs;

import com.google.common.io.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.samza.context.Context;
import org.apache.samza.storage.kv.KeyValueStore;
import org.apache.samza.system.IncomingMessageEnvelope;
import org.apache.samza.system.OutgoingMessageEnvelope;
import org.apache.samza.task.InitableTask;
import org.apache.samza.task.MessageCollector;
import org.apache.samza.task.StreamTask;
import org.apache.samza.task.TaskCoordinator;

import org.codehaus.jackson.map.ObjectMapper;



/**
 * Consumes the stream of ad-click.
 * Outputs a stream which handles static file and one stream
 * and gives a stream of revenue distribution.
 */
public class AdPriceTask implements StreamTask, InitableTask {

    /*
       Define per task state here. (kv stores etc)
       READ Samza API part in Writeup to understand how to start
    */
    private KeyValueStore<String, Map<String, Object>> adsInfo;

    public List<String> readFile(String path) {
        try {
            InputStream in = Resources.getResource(path).openStream();
            List<String> lines = new ArrayList<>();
            String line = null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
            return lines;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(Context context) throws Exception {
        // Initialize (maybe kv store and static data?)
        adsInfo = (KeyValueStore<String, Map<String, Object>>) context.getTaskContext().getStore("ads-info");
        // read NYCstoreAds.json into adsInfo
        List<String> adsInfoRawString = readFile("NYCstoreAds.json");
        System.out.println("Reading store ads info file from " + Resources.getResource("NYCstoreAds.json").toString());
        System.out.println("AdsInfo raw string size: " + adsInfoRawString.size());

        for (String rawString : adsInfoRawString) {
            Map<String, Object> mapResult;
            ObjectMapper mapper = new ObjectMapper();
            try {
                mapResult = mapper.readValue(rawString, HashMap.class);
                String storeId = mapResult.get("storeId").toString();
                adsInfo.put(storeId, mapResult);
            } catch (Exception e) {
                System.out.println("Failed at parse user info :" + rawString);
            }
        }
    }

    private void processAdClick(Map<String, Object> msg, MessageCollector collector) {
        Integer userId = (Integer) msg.get("userId");
        String storeId = msg.get("storeId").toString();
        String name = msg.get("name").toString();
        Boolean clicked = msg.get("cab").toString().equals("true") ? true : false;
        // TODO: ad-price calculation logic
        // retrieve store ads info
        Map<String, Object> adsMap = adsInfo.get(storeId);
        Integer adPrice = (Integer) adsMap.get("adPrice");

        Double ad;
        Double cab;
        if (clicked) {
            // 2:8
            ad = 0.8 * adPrice;
            cab = 0.2 * adPrice;
        } else {
            // 5:5
            ad = 0.5 * adPrice;
            cab = 0.5 * adPrice;
        }

        Map<String, Object> outputMessage = new HashMap<>();
        outputMessage.put("userId", userId);
        outputMessage.put("storeId", storeId);
        outputMessage.put("ad", ad);
        outputMessage.put("cab", cab);

        OutgoingMessageEnvelope envelope = 
                new OutgoingMessageEnvelope(AdPriceConfig.AD_PRICE_STREAM, outputMessage);
        collector.send(envelope);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(IncomingMessageEnvelope envelope, MessageCollector collector, TaskCoordinator coordinator) {
        /*
        All the messsages are partitioned by userId, which means the messages
        sharing the same userId will arrive at the same task, similar to the
        approach that MapReduce sends all the key value pairs with the same key
        into the same reducer.
        */
        String incomingStream = envelope.getSystemStreamPartition().getStream();

        if (incomingStream.equals(AdPriceConfig.AD_CLICK_STREAM.getStream())) {
            // Handle Ad-click messages
            processAdClick((Map<String, Object>) envelope.getMessage(), collector);
        } else {
            throw new IllegalStateException("Unexpected input stream: " + envelope.getSystemStreamPartition());
        }
    }
}
