package org.gb.miner.stratumminer.stratum;

import com.fasterxml.jackson.databind.JsonNode;
import org.gb.miner.stratumminer.MinyaException;

/**
 * Created by Ben David on 01/08/2017.
 */

public class StratumJsonResultStandard extends StratumJsonResult {
    public final static String TEST_PATT = "{\"error\": null, \"jsonrpc\": \"2.0\", \"id\": 2, \"result\": true}";
    public final boolean result;

    public StratumJsonResultStandard(JsonNode i_json_node) throws MinyaException {
        super(i_json_node);
        this.result = i_json_node.get("result").asBoolean();
        return;
    }
}