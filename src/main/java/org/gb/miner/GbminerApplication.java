package org.gb.miner;

import org.gb.miner.stratumminer.MinerService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GbminerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GbminerApplication.class, args);

        MinerService minerService = new MinerService();
        minerService.startMiner();

    }
}
