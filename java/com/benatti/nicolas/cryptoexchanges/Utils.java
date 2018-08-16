package com.benatti.nicolas.cryptoexchanges;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    //TODO: erase
    public static ArrayList<String> currencyNames = new ArrayList<>(Arrays.asList("Bitcoin", "Ethereum", "Litecoin", "Euro"));

    public static Map<String, String> currencyCodes = new HashMap<String, String>(){{
        put("Bitcoin", "BTC");
        put("Ethereum", "ETH");
        put("Litecoin", "LTC");
        put("Euro", "EUR");
    }};

    public static final String apiKey = "nC9Z83w4vNaTWjB3C1ixc7zTz8M5u0L4";
}
