package org.gb.miner.stratumminer.worker;

import org.gb.miner.stratumminer.MiningWork;

/**
 * Created by Ben David on 01/08/2017.
 */

public interface IWorkerEvent {
    public void onNonceFound(MiningWork i_work, int i_nonce);
}
