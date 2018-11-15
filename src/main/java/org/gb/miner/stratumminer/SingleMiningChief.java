package org.gb.miner.stratumminer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import org.gb.miner.stratumminer.connection.IConnectionEvent;
import org.gb.miner.stratumminer.connection.IMiningConnection;
import org.gb.miner.stratumminer.connection.StratumMiningConnection;
import org.gb.miner.stratumminer.worker.CpuMiningWorker;
import org.gb.miner.stratumminer.worker.IMiningWorker;
import org.gb.miner.stratumminer.worker.IWorkerEvent;

import java.util.Observable;
import java.util.Observer;

import static org.gb.miner.stratumminer.Constants.*;

/**
 * Created by Ben David on 01/08/2017.
 */

public class SingleMiningChief implements Observer
{


    private static final long DEFAULT_SCAN_TIME = 5000;
    private static final long DEFAULT_RETRY_PAUSE = 30000;

    //private Worker worker;
    private IMiningConnection mc;
    private IMiningWorker imw;
    private long lastWorkTime;
    private long lastWorkHashes;
    private float speed=0;			//khash/s
    public long accepted=0;
    public long rejected=0;
    public int priority=1;
    private Handler mainHandler;
    private Console console;
    public IMiningConnection _connection;
    public IMiningWorker _worker;
    //private IMiningWorker _worker;
    private EventListener _eventlistener;

    public String status = STATUS_NOT_MINING;
    final int MSG_UIUPDATE = 1;
    final int MSG_TERMINATED = 2;
    final int MSG_SPEED_UPDATE = 3;
    final int MSG_STATUS_UPDATE = 4;
    final int MSG_ACCEPTED_UPDATE = 5;
    final int MSG_REJECTED_UPDATE = 6;
    final int MSG_CONSOLE_UPDATE = 7;

    public class EventListener extends Observable implements IConnectionEvent,IWorkerEvent
    {
        private SingleMiningChief _parent;
        private int _number_of_accept;
        private int _number_of_all;

        EventListener(SingleMiningChief i_parent)
        {
            this._parent=i_parent;
            this.resetCounter();
        }
        public void resetCounter()
        {
            this._number_of_accept = this._number_of_all=0;
        }

        @Override
        public void onNewWork(MiningWork i_work)
        {
            try {
                log.info("New work detected!");
                console.write("New work detected!");
                setChanged();
                notifyObservers(IMiningWorker.Notification.NEW_BLOCK_DETECTED);
                setChanged();
                notifyObservers(IMiningWorker.Notification.NEW_WORK);
                //新規ワークの開始
                synchronized(this){
                    this._parent._worker.doWork(i_work);
                }
            } catch (MinyaException e){
                e.printStackTrace();
            }
        }
        @Override
        public void onSubmitResult(MiningWork i_listener, int i_nonce,boolean i_result)
        {
            this._number_of_accept+=(i_result?1:0);
            this._number_of_all++;
            log.info("SubmitStatus:"+(i_result?"Accepted":"Reject")+"("+this._number_of_accept+"/"+this._number_of_all+")");
            setChanged();
            notifyObservers(i_result ? IMiningWorker.Notification.POW_TRUE : IMiningWorker.Notification.POW_FALSE);
        }
        public boolean onDisconnect()
        {
            //再接続するならtrue
            return false;
        }
        @Override
        public void onNonceFound(MiningWork i_work, int i_nonce)
        {
            try {
                this._parent._connection.submitWork(i_work,i_nonce);
            } catch (MinyaException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
    public SingleMiningChief(IMiningConnection i_connection,IMiningWorker i_worker, Console cons, Handler h) throws MinyaException
    {
        log.info("LC", "Miner:Miner()");
        status= STATUS_CONNECTING;
        speed=0.0f;
        mainHandler=h;
        this.console = cons;
        this._connection=i_connection;
        this._worker=i_worker;
        this._eventlistener=new EventListener(this);
        this._connection.addListener(this._eventlistener);
        this._worker.addListener(this._eventlistener);

    }
    public void startMining() throws MinyaException
    {
        //コネクションを接続
        log.info("LC", "Starting Worker Thread");
        console.write("Miner: Starting worker thread, priority: "+priority);
        ((StratumMiningConnection)_connection).addObserver(this);
        ((CpuMiningWorker)_worker).addObserver(this);
        MiningWork first_work=this._connection.connect();
        //情報リセット
        this._eventlistener.resetCounter();
        //初期ワークがあるならワーク開始
        if(first_work!=null){
            synchronized(this){
                this._worker.doWork(first_work);
            }
        }
    }
    public void stopMining() throws MinyaException
    {
        //コネクションを切断
        log.info("LC", "Miner:stop()");
        console.write("Miner: Worker stopping...");
        console.write("Miner: Worker cooling down");
        console.write("Miner: This can take a few minutes");
        this._connection.disconnect();
        //ワーカーを停止
        //log.info("SingleMiningChief","before this._worker.stopWork() ");
        this._worker.stopWork();
        speed=0;
    }

    public void update(Observable o, Object arg) {
        Message msg = new Message();
        Bundle bundle = new Bundle();
        msg.setData(bundle); // ensure msg has something


        IMiningWorker.Notification n = (IMiningWorker.Notification) arg;
        if (n == IMiningWorker.Notification.SYSTEM_ERROR) {
            log.info("LC", "system error");
            android.util.log.info("LC","System error");
            console.write("Miner: System error");
            status = STATUS_ERROR;
            bundle.putString("status", status);
            msg.arg1=MSG_STATUS_UPDATE;
            msg.setData(bundle);
            mainHandler.sendMessage(msg);
        }
        else if (n == IMiningWorker.Notification.PERMISSION_ERROR) {
            log.info("LC", "permission error");
            android.util.log.info("LC","Permission error");
            console.write("Miner: Permission error");
            status= STATUS_ERROR;;
            bundle.putString("status", status);
            msg.arg1=MSG_STATUS_UPDATE;
            msg.setData(bundle);
            mainHandler.sendMessage(msg);
        }
        else if (n== IMiningWorker.Notification.TERMINATED) {
            log.info("LC", "Miner: Worker terminated");
            console.write("Miner: Worker terminated");
            status= STATUS_TERMINATED;
            bundle.putString("status", status);
            msg.arg1=MSG_STATUS_UPDATE;
            msg.setData(bundle);
            mainHandler.sendMessage(msg);

            msg = new Message();
            msg.arg1=MSG_TERMINATED;
            mainHandler.sendMessage(msg);

            msg = new Message();
            bundle = new Bundle();
            bundle.putFloat("speed", 0);
            msg.arg1=MSG_SPEED_UPDATE;
            msg.setData(bundle);
            mainHandler.sendMessage(msg);
        }
        else if (n== IMiningWorker.Notification.CONNECTING) {
            log.info("LC", "Miner: Worker connecting");
            console.write("Miner: Worker connecting");
            status= STATUS_CONNECTING;
            bundle.putString("status", status);
            msg.arg1=MSG_STATUS_UPDATE;
            msg.setData(bundle);
            mainHandler.sendMessage(msg);

        }
        else if (n == IMiningWorker.Notification.AUTHENTICATION_ERROR) {
            android.util.log.info("LC", "Invalid worker username or password");
            status= STATUS_ERROR;
            console.write("Miner: Authentication error");
            bundle.putString("status", status);
            msg.arg1=MSG_STATUS_UPDATE;
            msg.setData(bundle);
            mainHandler.sendMessage(msg);
        }
        else if (n == IMiningWorker.Notification.CONNECTION_ERROR) {
            android.util.log.info("LC", "Connection error, retrying in " + 3000/1000L + " seconds");
            status= STATUS_ERROR;
            console.write("Miner: Connection error");
            bundle.putString("status", status);
            msg.arg1=MSG_STATUS_UPDATE;
            msg.setData(bundle);
            mainHandler.sendMessage(msg);
        }
        else if (n == IMiningWorker.Notification.COMMUNICATION_ERROR) {
            android.util.log.info("LC", "Communication error");
            status= STATUS_ERROR;
            console.write("Miner: Communication error");
            bundle.putString("status", status);
            msg.arg1=MSG_STATUS_UPDATE;
            msg.setData(bundle);
            mainHandler.sendMessage(msg);
        }
        else if (n == IMiningWorker.Notification.LONG_POLLING_FAILED) {
            android.util.log.info("LC", "Long polling failed");
            status= STATUS_NOT_MINING;
            console.write("Miner: Long polling failed");
            bundle.putString("status", status);
            msg.arg1=MSG_STATUS_UPDATE;
            msg.setData(bundle);
            mainHandler.sendMessage(msg);
        }
        else if (n == IMiningWorker.Notification.LONG_POLLING_ENABLED) {
            android.util.log.info("LC", "Long polling enabled");
            status= STATUS_MINING;
            console.write("Miner: Long polling enabled");
            console.write("Miner: Speed updates as work is completed");
            bundle.putString("status", status);
            msg.arg1=MSG_STATUS_UPDATE;
            msg.setData(bundle);
            mainHandler.sendMessage(msg);
        }
        else if (n == IMiningWorker.Notification.NEW_BLOCK_DETECTED) {
            android.util.log.info("LC", "Detected new block");
            status = STATUS_MINING;
            console.write("Miner: Detected new block");
            bundle.putString("status", status);
            msg.arg1=MSG_STATUS_UPDATE;
            msg.setData(bundle);
            mainHandler.sendMessage(msg);
        }
        else if (n == IMiningWorker.Notification.POW_TRUE) {
            android.util.log.info("LC", "PROOF OF WORK RESULT: true");
            status=STATUS_MINING;
            console.write("Miner: PROOF OF WORK RESULT: true");
            accepted+=1;
            bundle.putString("status", status);
            msg.arg1=MSG_STATUS_UPDATE;
            msg.setData(bundle);
            mainHandler.sendMessage(msg);

            msg= new Message();
            bundle = new Bundle();
            msg.arg1=MSG_ACCEPTED_UPDATE;
            bundle.putLong("accepted", accepted);
            mainHandler.sendMessage(msg);

        }
        else if (n == IMiningWorker.Notification.POW_FALSE) {
            android.util.log.info("LC", "PROOF OF WORK RESULT: false");
            status= STATUS_MINING;
            rejected+=1;
            bundle.putString("status", status);
            msg.arg1=MSG_STATUS_UPDATE;
            msg.setData(bundle);
            mainHandler.sendMessage(msg);

            msg= new Message();
            bundle = new Bundle();
            msg.arg1=MSG_REJECTED_UPDATE;
            bundle.putLong("rejected", rejected);
            mainHandler.sendMessage(msg);
        }
        else if (n == IMiningWorker.Notification.SPEED) {
            android.util.log.info("LC", "Speed Update");
            if (status.equals(STATUS_TERMINATED) || status.equals(STATUS_NOT_MINING)) {
                speed = 0;
                //log.info("Thread.ActiveCount()" , "" + Thread.activeCount());
            } else {
                speed = (float) ((CpuMiningWorker)_worker).get_speed();
            }
            bundle.putFloat("speed", speed);
            msg.arg1 = MSG_SPEED_UPDATE;
            msg.setData(bundle);
            mainHandler.sendMessage(msg);
        }
        else if (n == IMiningWorker.Notification.NEW_WORK) {
            if (lastWorkTime > 0L) {
                long hashes = _worker.getNumberOfHash() - lastWorkHashes;
                speed = (float) ((CpuMiningWorker)_worker).get_speed();
//                speed = (float) hashes / (System.currentTimeMillis() - lastWorkTime)/1000;
//                speed = (float) hashes / Math.max(1, System.currentTimeMillis() - lastWorkTime)/1000;
                //speed= (float) hashes/((System.currentTimeMillis()-this._last_time)/1000);
                //android.util.log.info("LC", String.format("%d hashes, %.6f khash/s", hashes, speed));
                android.util.log.info("LC", String.format("%d hashes, %.6f hash/s", hashes, speed));
                status= STATUS_MINING;
                //console.write("Miner: "+String.format("%d hashes, %.6f khash/s", hashes, speed));
                console.write("Miner: "+String.format("%d Hashes, %.6f Hash/s", hashes, speed));

                bundle.putString("status", status);
                msg.arg1=MSG_STATUS_UPDATE;
                msg.setData(bundle);
                mainHandler.sendMessage(msg);

                msg = new Message();
                bundle = new Bundle();
                bundle.putFloat("speed", speed);
                msg.arg1=MSG_SPEED_UPDATE;
                msg.setData(bundle);
                mainHandler.sendMessage(msg);
            }
            lastWorkTime = System.currentTimeMillis();
            lastWorkHashes = _worker.getNumberOfHash();

        }
    }
}