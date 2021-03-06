package edu.colostate.cs.storm.topology;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import edu.colostate.cs.storm.Constants;
import edu.colostate.cs.storm.bolt.HouseLoadPredictorBolt;
import edu.colostate.cs.storm.bolt.PlugLoadPredictorBolt;
import edu.colostate.cs.storm.bolt.ReportBolt;
import edu.colostate.cs.storm.spout.S3Spout;

/**
 * Author: Thilina
 * Date: 10/16/14
 */
public class LoadPredictionTopology {
    public static void main(String[] args) {
        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("spout", new S3Spout(), 1);
        builder.setBolt("predict-house", new HouseLoadPredictorBolt(), 2).fieldsGrouping("spout",
                Constants.Streams.POWER_GRID_DATA,
                new Fields(Constants.DataFields.HOUSE_ID)).allGrouping("spout",
                Constants.Streams.CUSTOM_TICK_TUPLE).globalGrouping("spout", Constants.Streams.PERF_PUNCTUATION_STREAM);
        builder.setBolt("predict-plug", new PlugLoadPredictorBolt(), 2).fieldsGrouping("spout",
                Constants.Streams.POWER_GRID_DATA,
                new Fields(Constants.DataFields.HOUSE_ID)).allGrouping("spout",
                Constants.Streams.CUSTOM_TICK_TUPLE).globalGrouping("spout", Constants.Streams.PERF_PUNCTUATION_STREAM);
        builder.setBolt("report", new ReportBolt(), 1).
                allGrouping("predict-house", Constants.Streams.HOUSE_LOAD_PREDICTION).
                globalGrouping("predict-house", Constants.Streams.PERF_PUNCTUATION_STREAM).
                allGrouping("predict-plug", Constants.Streams.PLUG_LOAD_PREDICTION);

        Config conf = new Config();
        conf.put(Config.TOPOLOGY_DEBUG, false);
        // output parameters are as follows
        // slice_length(in seconds) s3bucket s3infile s3outfile
        if (args != null && args.length > 1) {
            conf.setNumWorkers(4);
            conf.put(Constants.SLICE_LENGTH, Long.parseLong(args[1]));
            conf.put(Constants.S3_BUCKET_NAME, args[2]);
            conf.put(Constants.S3_KEY, args[3]);
            conf.put(Constants.S3_OUTPUT_KEY, args[4]);
            if (args.length > 5) {
                conf.put(Constants.MODE, args[5]);
            }
            try {
                StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
            } catch (AlreadyAliveException e) {
                e.printStackTrace();
            } catch (InvalidTopologyException e) {
                e.printStackTrace();
            }
        } else {
            conf.setMaxTaskParallelism(5);
            LocalCluster cluster = new LocalCluster();
            conf.put(Constants.SLICE_LENGTH, Long.parseLong(args[1]));
            cluster.submitTopology(args[0], conf, builder.createTopology());
            try {
                Thread.sleep(60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cluster.shutdown();
            //}
        }
    }
}

