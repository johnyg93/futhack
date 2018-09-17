package com.jg.monitor.logic;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


/**
 * Created by Jonatan on 28/03/2017.
 */
@Singleton
public class FUTLogic implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(FUTLogic.class.getName());
//    private Integer[] players = { 197445, 183907, 181872 };

//    private Integer[] players = { 194765 };

//    private Map<Integer,Integer> players = new HashMap<>();

    private Integer[] players = { 155862, 138956, 184344, 167664, 184941, 192985, 41236, 188567, 194765, 189509, 181872, 190460, 178518, 195864 };

//    private Map<Integer, Integer> players = new HashMap<>();

    private Integer maxPrice = 40_000;
    private long moneyOnAccount = 200_000;
    private long counter = Long.parseLong(System.getProperty("transfers.counter"));

    private static long dailyLimitsCounter = 0;
    private static long hourlyLimitsCounter = 0;
    private static LocalDateTime dailyLimitTimestamp = LocalDateTime.now();
    private static LocalDateTime hourlyLimitTimestamp = LocalDateTime.now();

    private static final String X_UT_PHISHING_TOKEN = System.getProperty("transfers.phishing-token");
    private static final String X_UT_SID = System.getProperty("transfers.sid");
    private static final String TRANSFERS_URL = "https://utas.external.s2.fut.ea.com/ut/game/fifa18/transfermarket?start=0&num=36&type=player&maskedDefId=";
    private static final String BID_URL = "https://utas.external.s2.fut.ea.com/ut/game/fifa18/trade/";

    private static List<String> bought = new ArrayList<>();
    private static Random random = new Random();
    private static boolean check = true;

//    @PostConstruct
//    private void init() {
//        players.put(197445, 30000); //Alaba
//        players.put(181872, 60000); //Vidal
//        players.put(183907, 30000); //Boateng
//        players.put(138956, 50000); //Chiellini
//        players.put(167664, 30000); //Higuain
//        players.put(194765, 70000); //Griezmann
//        players.put(178518, 60000); //Nainggolan
//    }

    public void checkTransfers() {
        if (check == false) {
            return;
        }
        try {
            HttpClient client = buildHttpClient();

            for (Integer id : players) {
                Thread.sleep(new Double( 500 + 5000 * random.nextDouble()).longValue());
                JsonArray items = getOffers(client, id, maxPrice);
                if (items == null || items.size() == 0) {
                    continue;
                }
                for (int i = 0; i < 1; i++) {
                    JsonObject player = items.getJsonObject(i);
                    buyPlayer(client, player, maxPrice);
                }
            }
        }
        catch (Exception e) {
            check = false;
            e.printStackTrace();
        }
    }

    private void buyPlayer(HttpClient client, JsonObject player, Integer playerPrice) throws Exception {
        StringBuilder bld = new StringBuilder(BID_URL);
        String auctionId = player.getJsonString("tradeIdStr").getString();
        bld.append(auctionId);
        bld.append("/bid?sku_b=FFT18");

        int price = player.getInt("buyNowPrice");

//        int rareflag = player.getJsonObject("itemData").getInt("rareflag");


        if (price <= playerPrice /*&& rareflag == 3*/) {
            HttpPut request = new HttpPut(bld.toString());
            request.addHeader("X-UT-PHISHING-TOKEN", X_UT_PHISHING_TOKEN);
            request.addHeader("X-UT-SID", X_UT_SID);
            request.addHeader("Easw-Session-Data-Nucleus-Id", "1005934569332");

            JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();
            bodyBuilder.add("bid", price);
            String requestBody = bodyBuilder.build().toString();
            LOGGER.info("bid: " + requestBody);
            request.setEntity(new StringEntity(requestBody));
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String responseString = EntityUtils.toString(entity);
                JsonReader jsonReader = Json.createReader(new StringReader(responseString));
                JsonObject obj = (JsonObject) jsonReader.read();
                jsonReader.close();
                LOGGER.warning("SUCCESS: " + obj.toString());
                moneyOnAccount = obj.getInt("credits");
                bought.add(auctionId);
                LOGGER.info("MONEY: " + moneyOnAccount);
            }
        }
    }

    private JsonArray getOffers(HttpClient client, Integer playerId, Integer price) throws IOException {
        counter++;
        dailyLimitsCounter++;
        hourlyLimitsCounter++;

        if (dailyLimitsCounter < 5000 && hourlyLimitsCounter < 500) {

            StringBuilder bld = new StringBuilder(TRANSFERS_URL);
            bld.append(playerId);
            bld.append("&maxb=");
            bld.append(price);
            bld.append("&_=");
            bld.append(counter);

            HttpGet request = new HttpGet(bld.toString());
            request.addHeader("X-UT-PHISHING-TOKEN", X_UT_PHISHING_TOKEN);
            request.addHeader("X-UT-SID", X_UT_SID);
            request.addHeader("Easw-Session-Data-Nucleus-Id", "1005934569332");


            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String responseString = EntityUtils.toString(entity);
                JsonReader jsonReader = Json.createReader(new StringReader(responseString));
                JsonObject obj = (JsonObject) jsonReader.read();
                jsonReader.close();
                LOGGER.info("bids for: " + playerId + ": " + obj.toString());
                if (obj.containsKey("auctionInfo")) {
                    JsonArray arr = obj.getJsonArray("auctionInfo");
                    return arr;
                }
            } else {
                LOGGER.info("null entity");
            }
        }

        if (LocalDateTime.now().isAfter(hourlyLimitTimestamp.plusHours(1L))) {
            hourlyLimitsCounter = 0;
            hourlyLimitTimestamp = LocalDateTime.now();
        }
        if (LocalDateTime.now().isAfter(dailyLimitTimestamp.plusDays(1L))) {
            dailyLimitsCounter = 0;
            dailyLimitTimestamp = LocalDateTime.now();
        }

        return null;
    }

    private org.apache.http.client.HttpClient buildHttpClient() {
        return HttpClients
                .custom()
                .setDefaultRequestConfig(
                        RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .build();
    }
}
