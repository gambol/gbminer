package org.gb.miner.stratumminer;//package org.gb.miner.stratumminer;
//
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Message;
//import android.util.Log;
//
//import org.gb.miner.stratumminer.connection.IMiningConnection;
//import org.gb.miner.stratumminer.connection.StratumMiningConnection;
//import org.gb.miner.stratumminer.worker.CpuMiningWorker;
//import org.gb.miner.stratumminer.worker.IMiningWorker;
//
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.Observable;
//import java.util.Observer;
//
//import static org.gb.miner.stratumminer.Constants.DEFAULT_PASS;
//import static org.gb.miner.stratumminer.Constants.DEFAULT_USER;
//
///**
// * Created by Tal on 03/08/2017.
// */
//
//public class Miner implements Observer {
//    private static final long DEFAULT_SCAN_TIME = 5000;
//    private static final long DEFAULT_RETRY_PAUSE = 30000;
//
//    //private Worker worker;
//    private IMiningConnection mc;
//    private IMiningWorker imw;
//    private SingleMiningChief smc;
//    private long lastWorkTime;
//    private long lastWorkHashes;
//    private float speed=0;			//khash/s
//    public long accepted=0;
//    public long rejected=0;
//    public int priority=1;
//    private Handler mainHandler;
//    private Console console;
//    private Thread t;
//
//    public String status="Not Mining";
//    final int MSG_UIUPDATE = 1;
//    final int MSG_TERMINATED = 2;
//    final int MSG_SPEED_UPDATE = 3;
//    final int MSG_STATUS_UPDATE = 4;
//    final int MSG_ACCEPTED_UPDATE = 5;
//    final int MSG_REJECTED_UPDATE = 6;
//    final int MSG_CONSOLE_UPDATE = 7;
//
//
//    public Miner(String url, String usr, String pass, long scanTime, long retryPause,
//                 int nThread, double throttle, int pri, Handler h, Console c) {
//        log.info("LC", "Miner:Miner()");
//        status="Connecting";
//        speed=0.0f;
//        mainHandler=h;
//        console = c;
//        priority=pri;
//
//        if (nThread < 1) {
//            log.info("LC", "Invalid number of threads:"+nThread);
//            console.write("Miner: Invalid number of threads");
//        }
//        if (throttle <= 0.0 || throttle > 1.0) {
//            log.info("LC", "Invalid throttle:"+ throttle);
//            console.write("Miner:Invalid throttle");
//        }
//        if (scanTime < 1L) {
//            log.info("LC", "Invalid scan time");
//            console.write("Miner:Invalid scan time");
//        }
//        if (retryPause < 0L) {
//            log.info("LC", "Invalid retry pause:"+retryPause);
//            console.write("Miner: Invalid retry pause");
//        }
//
//        try {
//            mc = new StratumMiningConnection(url,DEFAULT_USER,DEFAULT_PASS);
//            //mc = new StratumMiningConnection(url,usr,pass);
//            imw = new CpuMiningWorker(nThread);
//            smc = new SingleMiningChief(mc,imw,console);
//            //worker = new Worker(new URL(url), auth, scanTime, retryPause, nThread, throttle, console);
//            console.write("Miner: Worker created");
//        } catch (MinyaException e) {
//            log.info("LC", "Invalid URL:");
//            console.write("Miner: Invalid url");
//        }
//
//    }
//
//    public void start()
//    {
//        log.info("LC", "Miner:start()");
//
//        t = new Thread(imw);
//        worker.addObserver(this);
//        t.setPriority(priority);
//        log.info("LC", "Starting Worker Thread");
//        console.write("Miner: Starting worker thread, priority: "+priority);
//        t.start();
//    }
//
//
//    public void stop () {
//        log.info("LC", "Miner:stop()");
//        console.write("Miner: Worker stopping...");
//        worker.stop();
//        t.interrupt();
//        speed=0;
//    }
//
//    public void update(Observable o, Object arg) {
//        Message msg = new Message();
//        Bundle bundle = new Bundle();
//        msg.setData(bundle); // ensure msg has something
//
//        Worker.Notification n = (Worker.Notification) arg;
//        if (n == Worker.Notification.SYSTEM_ERROR) {
//            log.info("LC", "system error");
//            android.util.log.info("LC","System error");
//            console.write("Miner: System error");
//
//            status="Error";
//            bundle.putString("status", status);
//            msg.arg1=MSG_STATUS_UPDATE;
//            msg.setData(bundle);
//            mainHandler.sendMessage(msg);
//        }
//        else if (n == Worker.Notification.PERMISSION_ERROR) {
//            log.info("LC", "permission error");
//            android.util.log.info("LC","Permission error");
//            console.write("Miner: Permission error");
//
//            status="Error";
//            bundle.putString("status", status);
//            msg.arg1=MSG_STATUS_UPDATE;
//            msg.setData(bundle);
//            mainHandler.sendMessage(msg);
//        }
//        else if (n== Worker.Notification.TERMINATED) {
//            log.info("LC", "Miner: Worker terminated");
//            console.write("Miner: Worker terminated");
//            status="Terminated";
//
//            bundle.putString("status", status);
//            msg.arg1=MSG_STATUS_UPDATE;
//            msg.setData(bundle);
//            mainHandler.sendMessage(msg);
//
//            msg = new Message();
//            msg.arg1=MSG_TERMINATED;
//            mainHandler.sendMessage(msg);
//        }
//        else if (n == Worker.Notification.AUTHENTICATION_ERROR) {
//            android.util.log.info("LC", "Invalid worker username or password");
//            status="Error";
//            console.write("Miner: Authenticaion error");
//            bundle.putString("status", status);
//            msg.arg1=MSG_STATUS_UPDATE;
//            msg.setData(bundle);
//            mainHandler.sendMessage(msg);
//        }
//        else if (n == Worker.Notification.CONNECTION_ERROR) {
//            android.util.log.info("LC", "Connection error, retrying in " + worker.getRetryPause()/1000L + " seconds");
//            status="Error";
//            console.write("Miner: Connection error");
//            bundle.putString("status", status);
//            msg.arg1=MSG_STATUS_UPDATE;
//            msg.setData(bundle);
//            mainHandler.sendMessage(msg);
//        }
//        else if (n == Worker.Notification.COMMUNICATION_ERROR) {
//            android.util.log.info("LC", "Communication error");
//            status="Error";
//            console.write("Miner: Communication error");
//            bundle.putString("status", status);
//            msg.arg1=MSG_STATUS_UPDATE;
//            msg.setData(bundle);
//            mainHandler.sendMessage(msg);
//        }
//        else if (n == Worker.Notification.LONG_POLLING_FAILED) {
//            android.util.log.info("LC", "Long polling failed");
//            status="Not Mining";
//            console.write("Miner: Long polling failed");
//            bundle.putString("status", status);
//            msg.arg1=MSG_STATUS_UPDATE;
//            msg.setData(bundle);
//            mainHandler.sendMessage(msg);
//        }
//        else if (n == Worker.Notification.LONG_POLLING_ENABLED) {
//            android.util.log.info("LC", "Long polling enabled");
//            status="Mining";
//            console.write("Miner: Long polling enabled");
//            console.write("Miner: Speed updates as work is completed");
//            bundle.putString("status", status);
//            msg.arg1=MSG_STATUS_UPDATE;
//            msg.setData(bundle);
//            mainHandler.sendMessage(msg);
//        }
//        else if (n == Worker.Notification.NEW_BLOCK_DETECTED) {
//            android.util.log.info("LC", "LONGPOLL detected new block");
//            status="Mining";
//            console.write("Miner: LONGPOLL detected new block");
//
//            bundle.putString("status", status);
//            msg.arg1=MSG_STATUS_UPDATE;
//            msg.setData(bundle);
//            mainHandler.sendMessage(msg);
//        }
//        else if (n == Worker.Notification.POW_TRUE) {
//            android.util.log.info("LC", "PROOF OF WORK RESULT: true");
//            status="Mining";
//            console.write("Miner: PROOF OF WORK RESULT: true");
//            accepted+=1;
//
//            bundle.putString("status", status);
//            msg.arg1=MSG_STATUS_UPDATE;
//            msg.setData(bundle);
//            mainHandler.sendMessage(msg);
//
//            msg= new Message();
//            bundle = new Bundle();
//            msg.arg1=MSG_ACCEPTED_UPDATE;
//            bundle.putLong("accepted", accepted);
//            mainHandler.sendMessage(msg);
//
//        }
//        else if (n == Worker.Notification.POW_FALSE) {
//            android.util.log.info("LC", "PROOF OF WORK RESULT: false");
//            status="Mining";
//            rejected+=1;
//            bundle.putString("status", status);
//            msg.arg1=MSG_STATUS_UPDATE;
//            msg.setData(bundle);
//            mainHandler.sendMessage(msg);
//
//            msg= new Message();
//            bundle = new Bundle();
//            msg.arg1=MSG_REJECTED_UPDATE;
//            bundle.putLong("rejected", rejected);
//        }
//        else if (n == Worker.Notification.NEW_WORK) {
//            if (lastWorkTime > 0L) {
//                long hashes = worker.getHashes() - lastWorkHashes;
//                speed = (float) hashes / Math.max(1, System.currentTimeMillis() - lastWorkTime);
//                android.util.log.info("LC", String.format("%d hashes, %.6f khash/s", hashes, speed));
//                status="Mining";
//                console.write("Miner: "+String.format("%d hashes, %.6f khash/s", hashes, speed));
//
//                bundle.putString("status", status);
//                msg.arg1=MSG_STATUS_UPDATE;
//                msg.setData(bundle);
//                mainHandler.sendMessage(msg);
//
//                msg = new Message();
//                bundle = new Bundle();
//                bundle.putFloat("speed", speed);
//                msg.arg1=MSG_SPEED_UPDATE;
//                msg.setData(bundle);
//                mainHandler.sendMessage(msg);
//            }
//            lastWorkTime = System.currentTimeMillis();
//            lastWorkHashes = worker.getHashes();
//
//        }
//    }
//}