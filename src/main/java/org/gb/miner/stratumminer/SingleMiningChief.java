package org.gb.miner.stratumminer;

import lombok.extern.slf4j.Slf4j;
import org.gb.miner.stratumminer.connection.IConnectionEvent;
import org.gb.miner.stratumminer.connection.IMiningConnection;
import org.gb.miner.stratumminer.connection.StratumMiningConnection;
import org.gb.miner.stratumminer.worker.CpuMiningWorker;
import org.gb.miner.stratumminer.worker.IMiningWorker;
import org.gb.miner.stratumminer.worker.IWorkerEvent;

import java.util.Observable;
import java.util.Observer;

import static org.gb.miner.stratumminer.Constants.STATUS_CONNECTING;
import static org.gb.miner.stratumminer.Constants.STATUS_NOT_MINING;

/**
 * Created by Ben David on 01/08/2017.
 */

@Slf4j
public class SingleMiningChief implements Observer {


    private static final long DEFAULT_SCAN_TIME = 5000;
    private static final long DEFAULT_RETRY_PAUSE = 30000;

    //private Worker worker;
    private IMiningConnection mc;
    private IMiningWorker imw;
    private long lastWorkTime;
    private long lastWorkHashes;
    private float speed = 0;            //khash/s
    public long accepted = 0;
    public long rejected = 0;
    public int priority = 1;
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

    public class EventListener extends Observable implements IConnectionEvent, IWorkerEvent {
        private SingleMiningChief _parent;
        private int _number_of_accept;
        private int _number_of_all;

        EventListener(SingleMiningChief i_parent) {
            this._parent = i_parent;
            this.resetCounter();
        }

        public void resetCounter() {
            this._number_of_accept = this._number_of_all = 0;
        }

        @Override
        public void onNewWork(MiningWork i_work) {
            try {
                log.info("bNew work detected!");
                setChanged();
                notifyObservers(IMiningWorker.Notification.NEW_BLOCK_DETECTED);
                setChanged();
                notifyObservers(IMiningWorker.Notification.NEW_WORK);
                //新規ワークの開始
                synchronized (this) {
                    this._parent._worker.doWork(i_work);
                }
            } catch (MinyaException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSubmitResult(MiningWork i_listener, int i_nonce, boolean i_result) {
            this._number_of_accept += (i_result ? 1 : 0);
            this._number_of_all++;
            log.info("SubmitStatus:" + (i_result ? "Accepted" : "Reject") + "(" + this._number_of_accept + "/" + this._number_of_all + ")");
            setChanged();
            notifyObservers(i_result ? IMiningWorker.Notification.POW_TRUE : IMiningWorker.Notification.POW_FALSE);
        }

        public boolean onDisconnect() {
            //再接続するならtrue
            return false;
        }

        @Override
        public void onNonceFound(MiningWork i_work, int i_nonce) {
            try {
                this._parent._connection.submitWork(i_work, i_nonce);
            } catch (MinyaException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public SingleMiningChief(IMiningConnection i_connection, IMiningWorker i_worker) throws MinyaException {
        log.info( "Miner:Miner()");
        status = STATUS_CONNECTING;
        speed = 0.0f;

        this._connection = i_connection;
        this._worker = i_worker;
        this._eventlistener = new EventListener(this);
        this._connection.addListener(this._eventlistener);
        this._worker.addListener(this._eventlistener);

    }

    public void startMining() throws MinyaException {
        //コネクションを接続
        log.info("LC", "Starting Worker Thread");
        ((StratumMiningConnection) _connection).addObserver(this);
        ((CpuMiningWorker) _worker).addObserver(this);
        MiningWork first_work = this._connection.connect();
        //情報リセット
        this._eventlistener.resetCounter();
        //初期ワークがあるならワーク開始
        if (first_work != null) {
            synchronized (this) {
                this._worker.doWork(first_work);
            }
        }
    }

    public void stopMining() throws MinyaException {
        //コネクションを切断
        log.info("LC", "Miner:stop()");
        this._connection.disconnect();
        //ワーカーを停止
        //log.info("SingleMiningChief","before this._worker.stopWork() ");
        this._worker.stopWork();
        speed = 0;
    }

    public void update(Observable o, Object arg) {
        log.info("on update. o:{}, arg:{}", o, arg);
    }
}