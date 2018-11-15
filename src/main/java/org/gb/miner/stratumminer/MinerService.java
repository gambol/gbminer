package org.gb.miner.stratumminer;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.util.Log;
import android.widget.Toast;
import org.gb.miner.stratumminer.connection.IMiningConnection;
import org.gb.miner.stratumminer.connection.StratumMiningConnection;
import org.gb.miner.stratumminer.worker.CpuMiningWorker;
import org.gb.miner.stratumminer.worker.IMiningWorker;

import static org.gb.miner.stratumminer.Constants.*;

/**
 * Created by Tal on 03/08/2017.
 */

public class MinerService extends Service {

    IMiningConnection mc;
    IMiningWorker imw;
    SingleMiningChief smc;
    //Miner miner;
    Console console;
   // String news=null;
    Boolean running=false;
    float speed=0;
    int accepted=0;
    int rejected=0;
    String status= STATUS_NOT_MINING;
    String cString="";
    //int baseThreadCount = Thread.activeCount();

    Handler serviceHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle=msg.getData();
            log.info("LC", "Service: handleMessage() "+msg.arg1);

            if(msg.arg1==MSG_CONSOLE_UPDATE) { cString = bundle.getString("console"); }
            else if(msg.arg1==MSG_SPEED_UPDATE) { speed = bundle.getFloat("speed"); }
            else if(msg.arg1==MSG_STATUS_UPDATE) { status = bundle.getString("status"); }
            else if(msg.arg1==MSG_ACCEPTED_UPDATE) { accepted = (int) bundle.getLong("accepted"); }
            else if(msg.arg1==MSG_REJECTED_UPDATE) { rejected = (int) bundle.getLong("rejected"); }
            else if(msg.arg1==MSG_TERMINATED) {	running=false; }
            super.handleMessage(msg);
        }
    };
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        MinerService getService() {
            return MinerService.this;
        }
    }

    public MinerService() {
        log.info("LC", "Service: MinerService()");
    }


    public void startMiner()
    {
        console = new Console(serviceHandler);
        log.info("LC", "MinerService:startMiner()");
        SharedPreferences settings = getSharedPreferences(PREF_TITLE, 0);
        String url, user, pass;
        speed=0;
        accepted=0;
        rejected=0;

        console.write("Service: Start mining");
        url = settings.getString(PREF_URL, DEFAULT_URL);
        user = settings.getString(PREF_USER, DEFAULT_USER);
        pass = settings.getString(PREF_PASS, DEFAULT_PASS);

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
            mc = new StratumMiningConnection(url,user,pass);
            int nThread =  settings.getInt(PREF_THREAD, DEFAULT_THREAD);
            imw = new CpuMiningWorker(nThread,DEFAULT_RETRYPAUSE,DEFAULT_PRIORITY,console);
            smc = new SingleMiningChief(mc,imw,console,serviceHandler);
            smc.startMining();
            running =true;
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

    public void stopMiner()
    {
        log.info("LC", "Service: onBind()");
        console.write("Service: Stopping mining");
        Toast.makeText(this,"Worker cooling down, this can take a few minutes",Toast.LENGTH_LONG).show();
        running=false;
        try {
            smc.stopMining();
        } catch (MinyaException e) {
            e.printStackTrace();
        }
//        int lastThreadCount = Thread.activeCount();
//        while (Thread.activeCount() != baseThreadCount) {
//            if (Thread.activeCount() == lastThreadCount) {
//                lastThreadCount = Thread.activeCount();
//                continue;
//            }
//            log.info("Thread.ActiveCount()" , "" + Thread.activeCount());
//            lastThreadCount = Thread.activeCount();
//        }
//        miner.stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        log.info("LC", "Service: onBind()");

        return mBinder;
    }



}