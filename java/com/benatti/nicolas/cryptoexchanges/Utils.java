package com.benatti.nicolas.cryptoexchanges;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Utils {

    // list of currencies supported by the app
    // NOTE: read from settings file
    public static Map<String, String> currencyCodes = new HashMap<String, String>(){{
        put("Bitcoin", "BTC");
        put("Ethereum", "ETH");
        put("Litecoin", "LTC");
        put("Euro", "EUR");
        put("Sterlina", "GBP");
    }};

    public static List<String> getCoinNames() {

        List<String> res = new ArrayList<>();
        res.addAll(currencyCodes.keySet());

        return res;
    }

    public static final String apiKey = "nC9Z83w4vNaTWjB3C1ixc7zTz8M5u0L4";
}
