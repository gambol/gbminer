package org.gb.miner.stratumminer.connection;

import org.gb.miner.stratumminer.MiningWork;

/**
 * Created by Ben David on 01/08/2017.
 */

public interface IConnectionEvent
{
    public void onNewWork(MiningWork i_new_work);
    public void onSubmitResult(MiningWork i_listener, int i_nonce, boolean i_result);

}
