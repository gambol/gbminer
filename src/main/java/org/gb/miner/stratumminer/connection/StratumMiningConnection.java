package org.gb.miner.stratumminer.connection;

import lombok.extern.slf4j.Slf4j;
import org.gb.miner.stratumminer.MiningWork;
import org.gb.miner.stratumminer.MinyaException;
import org.gb.miner.stratumminer.StratumMiningWork;
import org.gb.miner.stratumminer.stratum.*;
import org.gb.miner.stratumminer.worker.IMiningWorker;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Observable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.gb.miner.stratumminer.Constants.CLIENT_NAME_STRING;

/**
 * Created by Ben David on 01/08/2017.
 */

@Slf4j
public class StratumMiningConnection extends Observable implements IMiningConnection {
    private class SubmitOrder {
        public SubmitOrder(long i_id, StratumMiningWork i_work, int i_nonce) {
            this.id = i_id;
            this.work = i_work;
            this.nonce = i_nonce;
            return;
        }

        public final long id;
        public final MiningWork work;
        public final int nonce;
    }

    /**
     * 非同期受信スレッド
     */
    private class AsyncRxSocketThread extends Thread {
        private ArrayList<SubmitOrder> _submit_q = new ArrayList<SubmitOrder>();
        private ArrayList<StratumJson> _json_q = new ArrayList<StratumJson>();
        private StratumMiningConnection _parent;

        public AsyncRxSocketThread(StratumMiningConnection i_parent) throws SocketException {
            this._parent = i_parent;
            this._parent._sock.setSoTimeout(100);
        }

        public void run() {
//            try {
//                _parent._sock = new StratumSocket(_parent._server);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            try {
//                this._parent._sock.setSoTimeout(100);
//            } catch (SocketException e) {
//                e.printStackTrace();
//            }
            for (; ; ) {
                try {
                    StratumJson json = this._parent._sock.recvStratumJson();
                    if (json == null) {
                        Thread.sleep(1);
                        continue;
                    }
                    this.onJsonRx(json);
                } catch (SocketTimeoutException e) {
                    if (this.isInterrupted()) {
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    //割り込みでスレッド終了
                    break;
                }
            }
        }

        /**
         * JSON Parsing
         */
        private void onJsonRx(StratumJson i_json) {
            Class<?> iid = i_json.getClass();

            log.debug("onJsonRx:" + iid.getName());


            if (iid == StratumJsonMethodGetVersion.class) {
            } else if (iid == StratumJsonMethodMiningNotify.class) {
                this._parent.cbNewMiningNotify((StratumJsonMethodMiningNotify) i_json);
            } else if (iid == StratumJsonMethodReconnect.class) {
            } else if (iid == StratumJsonMethodSetDifficulty.class) {
                this._parent.cbNewMiningDifficulty((StratumJsonMethodSetDifficulty) i_json);
            } else if (iid == StratumJsonMethodShowMessage.class) {
            } else if (iid == StratumJsonResultStandard.class) {
                //submit_qを探索してsubmitIDと一致したらjson_qに入れないでコール
                {
                    StratumJsonResultStandard sjson = (StratumJsonResultStandard) i_json;
                    SubmitOrder so = null;
                    synchronized (this._submit_q) {
                        for (SubmitOrder i : this._submit_q) {
                            if (i.id == sjson.id) {
                                //submit_qから取り外し
                                this._submit_q.remove(i);
                                so = i;
                                break;
                            }
                        }
                    }
                    if (so != null) {
                        this._parent.cbSubmitRecv(so, sjson);
                    }
                }
                synchronized (this._json_q) {
                    this._json_q.add(i_json);
                }
                this.semaphore.release();
            } else if (iid == StratumJsonResultSubscribe.class) {
                synchronized (this._json_q) {
                    this._json_q.add(i_json);
                }
                this.semaphore.release();
            }
            return;
        }

        private Semaphore semaphore = new Semaphore(0);

        /**
         * JSON Result
         */
        public StratumJson waitForJsonResult(long i_id, Class<?> i_class, int i_wait_for_msec) {
            long time_out = i_wait_for_msec;
            do {
                long s = System.currentTimeMillis();
                try {
                    if (!semaphore.tryAcquire(time_out, TimeUnit.MILLISECONDS)) {
                        return null;
                    }
                } catch (InterruptedException e) {
                    return null;
                }
                synchronized (this._json_q) {
                    //受信キューをスキャン
                    for (StratumJson json : this._json_q) {
                        if (!(json.getClass() == i_class)) {
                            continue;
                        }
                        StratumJsonResult jr = (StratumJsonResult) json;
                        if (jr.id == null) {
                            continue;
                        }
                        if (jr.id != i_id) {
                            continue;
                        }
                        this._json_q.remove(json);
                        return json;
                    }
                }
                time_out -= (System.currentTimeMillis() - s);
            } while (time_out > 0);
            return null;
        }

        /**
         * Submit ID
         */
        public void addSubmitOrder(SubmitOrder i_submit_id) {
            synchronized (this._submit_q) {
                this._submit_q.add(i_submit_id);
            }
        }
    }

    private static class SocketParams {
        StratumSocket _sock;
        URI _server;

        SocketParams(StratumSocket sock, URI server) {
            this._sock = sock;
            this._server = server;
        }
    }

//    public interface AsyncResponse {
//        void proccessFinish(StratumSocket sock_res);
//    }


    private final String CLIENT_NAME = CLIENT_NAME_STRING;
    private String _uid;
    private String _pass;
    private URI _server;
    private StratumSocket _sock = null;
    private AsyncRxSocketThread _rx_thread;

    public StratumMiningConnection(String i_url, String i_userid, String i_password) throws MinyaException {
        this._pass = i_password;
        this._uid = i_userid;
        try {
            this._server = new URI(i_url);
        } catch (URISyntaxException e) {
            throw new MinyaException(e);
        }
    }

    private StratumJsonMethodSetDifficulty _last_difficulty = null;
    private StratumJsonMethodMiningNotify _last_notify = null;

    /**
     * Server Connection
     *
     * @throws MinyaException
     */
    public MiningWork connect() throws MinyaException {
        setChanged();
        notifyObservers(IMiningWorker.Notification.CONNECTING);
        //Connect to host
        try {
            MiningWork ret = null;

            this._sock = new StratumSocket(this._server);

            //this._sock = new StratumSocket(this._server);
            this._rx_thread = new AsyncRxSocketThread(this);
            this._rx_thread.start();
            //3回トライ
            int i;

            //subscribe
            StratumJsonResultSubscribe subscribe = null;
            {
                for (i = 0; i < 3; i++) {
                    log.info("Request Stratum subscribe...");
                    subscribe = (StratumJsonResultSubscribe) this._rx_thread.waitForJsonResult(this._sock.subscribe(CLIENT_NAME), StratumJsonResultSubscribe.class, 3000);
                    if (subscribe == null || subscribe.error != null) {
                        log.warn("StratumMiningConnection Stratum subscribe error.");
                        continue;
                    }
                    break;
                }
                if (i == 3) {
                    throw new MinyaException("Stratum subscribe error.");
                }
            }

            //Authorize and make  a 1st work.
            for (i = 0; i < 3; i++) {
                log.info("Request Stratum authorize...");
                StratumJsonResultStandard auth = (StratumJsonResultStandard) this._rx_thread.waitForJsonResult(this._sock.authorize(this._uid, this._pass), StratumJsonResultStandard.class, 3000);
                if (auth == null || auth.error != null) {
                    log.warn("StratumMiningConnection Stratum authorize error.");
                    continue;
                }
                if (!auth.result) {
                    log.warn("StratumMiningConnection Stratum authorize result error.");
                }
                synchronized (this._data_lock) {
                    //worker builderの構築
                    this._work_builder = new StratumWorkBuilder(subscribe);
                    //事前に受信したメッセージがあれば設定
                    if (this._last_difficulty != null) {
                        this._work_builder.setDiff(this._last_difficulty);
                    }
                    if (this._last_notify != null) {
                        this._work_builder.setNotify(this._last_notify);
                    }
                    ret = this._work_builder.buildMiningWork();
                }
                //Complete!
                log.info("Stratum authorize complete!");
                log.info("StratumMiningConnection", "Stratum authorize complete!");
                return ret;
            }
            setChanged();
            notifyObservers(IMiningWorker.Notification.AUTHENTICATION_ERROR);
            throw new MinyaException("Stratum authorize process failed.");
        } catch (UnknownHostException e) {
            setChanged();
            notifyObservers(IMiningWorker.Notification.CONNECTION_ERROR);
            throw new MinyaException(e);
        } catch (IOException e) {
            throw new MinyaException(e);
        }
    }

    public void disconnect() throws MinyaException {
        try {
            //threadの停止
            this._rx_thread.interrupt();
            this._rx_thread.join();
            //Socketの停止
            this._sock.close();
            setChanged();
            notifyObservers(IMiningWorker.Notification.TERMINATED);
            synchronized (this._data_lock) {
                this._work_builder = null;
            }
        } catch (IOException e) {
            throw new MinyaException(e);
        } catch (InterruptedException e) {
            throw new MinyaException(e);
        }
    }

    private Object _data_lock = new Object();
    private StratumWorkBuilder _work_builder = null;

    /**
     * 現在のMiningWorkを生成して返す。
     *
     * @return
     */
    public MiningWork getWork() {
        MiningWork work = null;
        synchronized (this._data_lock) {
            if (this._work_builder == null) {
                return null;
            }
            try {
                work = this._work_builder.buildMiningWork();
            } catch (MinyaException e) {
                return null;
            }
        }
        return work;
    }

    private ArrayList<IConnectionEvent> _as_listener = new ArrayList<IConnectionEvent>();

    /**
     * この関数は非同期コールスレッドと衝突するのでconnect前に実行する事。
     */
    public void addListener(IConnectionEvent i_listener) {
        this._as_listener.add(i_listener);
        return;
    }

    /**
     * Threadからのコールバック(Thread)
     *
     * @throws MinyaException
     */
    private void cbNewMiningNotify(StratumJsonMethodMiningNotify i_notify) {
        synchronized (this._data_lock) {
            if (this._work_builder == null) {
                this._last_notify = i_notify;
                return;
            }
        }
        //notifyを更新
        try {
            log.info("Receive new job:" + i_notify.job_id);
            setChanged();
            notifyObservers(IMiningWorker.Notification.NEW_WORK);
            this._work_builder.setNotify(i_notify);
        } catch (MinyaException e) {
            log.info("Catch Exception:\n" + e.getMessage());
            log.info("StratumMiningConnection", "Catch Exception:\n" + e.getMessage());
        }
        MiningWork w = this.getWork();
        if (w == null) {
            return;
        }
        //登録されているlistenerをコール
        for (IConnectionEvent i : this._as_listener) {
            i.onNewWork(w);
        }
    }

    private void cbNewMiningDifficulty(StratumJsonMethodSetDifficulty i_difficulty) {
        log.info("Receive set difficulty:" + i_difficulty.difficulty);
        synchronized (this._data_lock) {
            if (this._work_builder == null) {
                this._last_difficulty = i_difficulty;
                return;
            }
        }
        //notifyを更新
        try {
            this._work_builder.setDiff(i_difficulty);
        } catch (MinyaException e) {
            log.info("Catch Exception:\n" + e.getMessage());
            log.info("StratumMiningConnection", "Catch Exception:\n" + e.getMessage());
        }
        MiningWork w = this.getWork();
        if (w == null) {
            return;
        }
        //登録されているlistenerをコール
        for (IConnectionEvent i : this._as_listener) {
            i.onNewWork(w);
        }
    }

    private void cbSubmitRecv(SubmitOrder so, StratumJsonResultStandard i_result) {
        log.info("SubmitResponse " + so.nonce + " [" + (i_result.result ? "Accepted" : "Rejected") + "]");
        log.info("StratumMiningConnection", "SubmitResponse " + so.nonce + " [" + (i_result.result ? "Accepted" : "Rejected") + "]");
        //登録されているlistenerをコール
        for (IConnectionEvent i : this._as_listener) {
            i.onSubmitResult(so.work, so.nonce, i_result.error == null);
        }
    }

    public void submitWork(MiningWork i_work, int i_nonce) throws MinyaException {
        if (!(i_work instanceof StratumMiningWork)) {
            throw new MinyaException();
        }
        StratumMiningWork w = (StratumMiningWork) i_work;
        String ntime = w.data.getStr(StratumMiningWork.INDEX_OF_NTIME, 4);
        //Stratum送信
        try {
            long id = this._sock.submit(i_nonce, this._uid, w.job_id, w.xnonce2, ntime);
            SubmitOrder so = new SubmitOrder(id, w, i_nonce);
            this._rx_thread.addSubmitOrder(so);
            log.info("submit. Found! nonce:{}. job_id:{}" , so.nonce , w.job_id);
        } catch (IOException e) {
            throw new MinyaException(e);
        }
    }

}
