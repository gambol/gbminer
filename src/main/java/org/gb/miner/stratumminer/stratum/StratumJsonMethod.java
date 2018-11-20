package org.gb.miner.stratumminer.stratum;

import com.fasterxml.jackson.databind.JsonNode;
import org.gb.miner.stratumminer.MinyaException;

/**
 * Created by Ben David on 01/08/2017.
 */

public class StratumJsonMethod extends StratumJson {
    public final Long id;

    public StratumJsonMethod(JsonNode i_json_node) throws MinyaException {
        if (i_json_node.has("id")) {
            this.id = i_json_node.get("id").isNull() ? null : i_json_node.get("id").asLong();
        } else {
            this.id = null;
        }
        if (!i_json_node.has("method")) {
            throw new MinyaException();
        }
    }
}