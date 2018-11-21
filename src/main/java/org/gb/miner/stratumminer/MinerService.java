package org.gb.miner.stratumminer;

import lombok.extern.slf4j.Slf4j;
import org.gb.miner.stratumminer.connection.IMiningConnection;
import org.gb.miner.stratumminer.connection.StratumMiningConnection;
import org.gb.miner.stratumminer.worker.CpuMiningWorker;
import org.gb.miner.stratumminer.worker.IMiningWorker;

import static org.gb.miner.stratumminer.Constants.*;

/**
 * Created by Tal on 03/08/2017.
 */
@Slf4j
public class MinerService {

    IMiningConnection mc;
    IMiningWorker imw;
    SingleMiningChief smc;
    //Miner miner;
    // String news=null;
    Boolean running = false;
    float speed = 0;
    int accepted = 0;
    int rejected = 0;
    String status = STATUS_NOT_MINING;
    String cString = "";
    //int baseThreadCount = Thread.activeCount();


    public MinerService() {
        log.info("Service: MinerService()");
    }


    public void startMiner() {
        log.info("MinerService:startMiner()");
        String url, user, pass;
        speed = 0;
        accepted = 0;
        rejected = 0;

        log.info("Service: Start mining");
        url = "stratum+tcp://stratum-ltc.antpool.com:8888";
        user = "gambol85.a1";
        pass = "x";

//        if (settings.getBoolean(PREF_DONATE, DEFAULT_DONATE)==true)
//        {
//            console.write("Main: Donate mode");
//            url=DONATE_URL;
//            user=DONATE_USER;
//            pass=DONATE_PASS;
//        }
//        else
//        {
//            url = settings.getString(PREF_URL, DEFAULT_URL);
//            user = settings.getString(PREF_USER, DEFAULT_USER);
//            pass = settings.getString(PREF_PASS, DEFAULT_PASS);
//        }

        try {
            mc = new StratumMiningConnection(url, user, pass);
            int nThread = 1;
            imw = new CpuMiningWorker(nThread, DEFAULT_RETRYPAUSE, DEFAULT_PRIORITY);
            smc = new SingleMiningChief(mc, imw);
            smc.startMining();
            running = true;
        } catch (MinyaException e) {
            e.printStackTrace();
        }

//        miner = new Miner(url,
//                user+":"+
//                        pass,
//                settings.getLong(PREF_SCANTIME, DEFAULT_SCANTIME),
//                settings.getLong(PREF_RETRYPAUSE, DEFAULT_RETRYPAUSE),
//                settings.getInt(PREF_THREAD, DEFAULT_THREAD),
//                settings.getFloat(PREF_THROTTLE, DEFAULT_THROTTLE),
//                settings.getInt(PREF_PRIORITY, DEFAULT_PRIORITY),
//                serviceHandler, console);
//        miner.start();
    }


}