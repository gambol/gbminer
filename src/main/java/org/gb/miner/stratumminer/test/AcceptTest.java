package org.gb.miner.stratumminer.test;

import org.gb.miner.stratumminer.MiningWork;
import org.gb.miner.stratumminer.connection.TestStratumMiningConnection;

/**
 * Created by Ben David on 01/08/2017.
 */

public class AcceptTest
{
    public static void main(String[] args)
    {
        try {
            TestStratumMiningConnection twf=new TestStratumMiningConnection(0);
            MiningWork mw=twf.getWork();
            mw.dump();
            long start = System.currentTimeMillis();
            System.out.println(System.currentTimeMillis()-start);
            return;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}