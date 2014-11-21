package edu.colostate.cs.storm.spout;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import edu.colostate.cs.storm.Constants;

import java.io.*;
import java.util.Map;

/**
 * This is the base spout that reads from Kinesis and emits the parsed data
 * into the topology.
 * It further acts as the timeline simulator which emits periodic ticks.
 * Author: Thilina
 * Date: 10/31/14
 */
public class BaseSpout extends BaseRichSpout {

    private SpoutOutputCollector collector;
    private BufferedReader bufferedReader;
    private long tickStartTimeStamp;

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        // instead of defining a new serializable data type, we'll emit a set of named data values as a tuple.
        outputFieldsDeclarer.declare(new Fields(Constants.InputTupleFields.ID,
                Constants.InputTupleFields.TIMESTAMP,
                Constants.InputTupleFields.VALUE,
                Constants.InputTupleFields.PROPERTY,
                Constants.InputTupleFields.PLUG_ID,
                Constants.InputTupleFields.HOUSEHOLD_ID,
                Constants.InputTupleFields.HOUSE_ID));
    }

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        collector = spoutOutputCollector;
        // TODO: To be replaced by reading from Kinesis code.
        // This spout will be merged with the Kinesis spout eventually.
        try {
            bufferedReader = new BufferedReader(new FileReader
                    (new File("/Users/thilina/csu/classes/581/project/data/sorted100M.csv")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void nextTuple() {
        String input = null;
        try {
            input = bufferedReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        long ts = 0;
        if (input != null) {
            // tokenize the data
            String[] values = tokenize(input);
            if(!isValid(values)){
                return;
            }
            ts = Long.parseLong(values[1]);
            // initialize the tick counter
            if(tickStartTimeStamp == 0){
                tickStartTimeStamp = ts;
            }
            // reset tick counter
            if(tickStartTimeStamp + 15l < ts){
                tickStartTimeStamp = ts;
                // emit a tick tuple every 15 seconds.
                collector.emit(new Values(0, // id
                        ts, // timestamp in seconds.
                        0.0, // value
                        Constants.TICK_TUPLE, // reading type
                        "", // plug id
                        "", // household id
                        ""));
            }

            // emit. Do a type cast when required.
            collector.emit(new Values(values[0], // id
                    ts, // timestamp in seconds.
                    Double.parseDouble(values[2]), // value
                    Integer.parseInt(values[3]), // reading type
                    values[4], // plug id
                    values[5], // household id
                    values[6])); // house id
        }
    }

    private String[] tokenize(String tuple) {
        return tuple.split(",");
    }

    private boolean isValid(String[] values){
        if(values[4] == null || values[5] == null || values[6] == null){
            return false;
        }
        if(values[4].replace(" ", "").length() == 0 ||
                values[5].replace(" ", "").length() == 0 ||
                values[6].replace(" ", "").length() == 0){
            return false;
        }
        return true;
    }
}