package com.benatti.nicolas.cryptoexchanges;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Toolbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity {

    // elementi grafici (e affini)
    private Toolbar toolbar;
    private EditText from, to;
    private Spinner fromSpinner, toSpinner;
    private List<String> fromSpinnerData, toSpinnerData;
    private ArrayAdapter<String> fromSpinnerAdpt, toSpinnerAdpt;
    private Button refreshButton, swapButton;

    // elenco dei cambi valuta (modificato durante aggiornamento dati)
    private Map<Pair, Double> exchanges;

    // richieste di conversione da mandare al server
    private  ArrayList<String> requests;

    // task che prende i dati dei cambi dal server (una volta all'avvio e ogni volta che lo sceglie l'utente)
    private RetrieveCurrencyData retrieveDataTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        from = findViewById(R.id.from_input);
        to = findViewById(R.id.to_input);
        fromSpinner = findViewById(R.id.from_spinner);
        toSpinner = findViewById(R.id.to_spinner);
        refreshButton = findViewById(R.id.refresh_button);
        swapButton = findViewById(R.id.swap_button);
        exchanges = new ConcurrentHashMap<>();

        // riempi gli spinner

        fromSpinnerData = new ArrayList<>();
        toSpinnerData = new ArrayList<>();

        fillSpinnersData(fromSpinnerData, toSpinnerData);

        fromSpinnerAdpt = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fromSpinnerData);
        toSpinnerAdpt = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, toSpinnerData);

        fromSpinnerAdpt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        toSpinnerAdpt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        fromSpinner.setAdapter(fromSpinnerAdpt);
        toSpinner.setAdapter(toSpinnerAdpt);

        // la conversione di default è Bitcoin-Euro
        fromSpinner.setSelection(getIndexOf(fromSpinner, "Bitcoin\t\t[" + Utils.currencyCodes.get("Bitcoin") + "]"));
        toSpinner.setSelection(getIndexOf(toSpinner, "Euro\t\t[" + Utils.currencyCodes.get("Euro") + "]"));

        // prendi i dati dei cambi-valuta

        requests = new ArrayList<>();

        // non vengono eseguite richieste "simmetriche" (es. BTC-EUR, ma non EUR-BTC)
        for (int i = 0; i < Utils.currencyCodes.size(); ++i) {
            for(int j = i+1; j < Utils.currencyCodes.size(); ++j) {

                //Log.d("DUMP", Utils.currencyCodes.get(Utils.currencyNames.get(i)) + ", " + Utils.currencyCodes.get(Utils.currencyNames.get(j)));

                requests.add("https://forex.1forge.com/1.0.3/convert?from=" +
                        Utils.currencyCodes.get(Utils.currencyNames.get(i)) + "&to=" +
                        Utils.currencyCodes.get(Utils.currencyNames.get(j)) + "&quantity=1&api_key=" + Utils.apiKey);
            }
        }

        // delega il fetching dei dati a thread secondario

        updateExchanges(requests);

        Log.d("CAMBI_VALUTA_FINALI", exchanges.toString());

        // === listener del campo di testo ===

        from.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                Log.d("LISTENER_DATA", s.toString());

                Log.d("SPINNER_VALUE", fromSpinner.getSelectedItem() + ", " + toSpinner.getSelectedItem());

                /*String[] fromCoin = ((String)fromSpinner.getSelectedItem()).split("\t\t");
                String[] toCoin = ((String)toSpinner.getSelectedItem()).split("\t\t");

                if(fromCoin[0].equals(toCoin[0])) {
                    Log.d("EQUAL", "onTextChanged: valori uguali");
                    to.setText(from.getText());
                }
                else {
                    if (!s.toString().isEmpty()) {
                        double conversionResult = convert(Double.parseDouble(s.toString()),
                                fromCoin[1].substring(1, fromCoin[1].length() - 1),
                                toCoin[1].substring(1, fromCoin[1].length() - 1));

                        //to.setText(String.format(Locale.US, "%.2f", conversionResult));

                        formatConversionResult(conversionResult, "%.2f");
                    } else
                        to.setText("");
                }*/

                conversionTask(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}

        });

        // =======================================================

        // === listener di cambio valore degli spinner ===
        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("SPINNER_VALUE_CHANGED", fromSpinner.getSelectedItem().toString());

                conversionTask(from.getText().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("SPINNER_VALUE_CHANGED", toSpinner.getSelectedItem().toString());

                conversionTask(from.getText().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * esegue il processo di conversione
     * [acquisizione dati] -> [conversione] -> [visualizzazione]
     */
    protected void conversionTask(String s) {

        String[] fromCoin = ((String)fromSpinner.getSelectedItem()).split("\t\t");
        String[] toCoin = ((String)toSpinner.getSelectedItem()).split("\t\t");

        if(fromCoin[0].equals(toCoin[0])) {
            Log.d("EQUAL", "onTextChanged: valori uguali");
            to.setText(from.getText());
        }
        else {
            if (!s.isEmpty()) {
                double conversionResult = convert(Double.parseDouble(s.toString()),
                        fromCoin[1].substring(1, fromCoin[1].length() - 1),
                        toCoin[1].substring(1, fromCoin[1].length() - 1));

                //to.setText(String.format(Locale.US, "%.2f", conversionResult));

                formatConversionResult(conversionResult, "%.2f");
            } else
                to.setText("");
        }
    }

    /* === gestione del menu opzioni === */

    /**
     * codice eseguito al momento della creazione del menu
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater minf = getMenuInflater();
        minf.inflate(R.menu.items, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * eseguito all'entrata nel menu opzioni
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        // per ora c'è solo un'opzione
        switch (item.getItemId()) {

            case R.id.stats:

                Intent openStatsPage = new Intent(this, StatsViewer.class);
                startActivity(openStatsPage);

                break;

            default:
                break;
        }

        return true;
    }

    // ==========================================

    /**
     * riempie gli spinner
     * @param fsd
     * @param tsd
     */
    public void fillSpinnersData(List<String> fsd, List<String> tsd) {

        for(String key : Utils.currencyCodes.keySet()) {

            fsd.add(key + "\t\t[" + Utils.currencyCodes.get(key) + "]");
            tsd.add(key + "\t\t[" + Utils.currencyCodes.get(key) + "]");
        }
    }

    /**
     * esegue una conversione
     * @param fromImport importo di partenza
     * @param fromCoin moneta di partenza
     * @param toCoin moneta di arrivo
     * @return importo di arrivo
     */
    public double convert(double fromImport, String fromCoin, String toCoin) {

        Log.d("FUNCDATA", fromImport + ", " + fromCoin + ", " + toCoin);

        double res = exchanges.get(new Pair<>(fromCoin, toCoin));

        return res * fromImport;
    }

    private int getIndexOf(Spinner sp, String val) {

        for(int i = 0; i < sp.getCount(); ++i) {
            if(sp.getItemAtPosition(i).equals(val))
                return i;
        }

        return -1;
    }

    // eseguita quando si preme il pulsante di refresh, aggiorna i dati sui cambi valuta
    private void updateExchanges(ArrayList<String> requests) {

        ArrayList<String> responses = null;

        try {
            responses = new RetrieveCurrencyData().execute(requests.toArray(new String[0])).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        Log.d("CAMBI-VALUTA-JSON", responses.toString());

        // riempi la mappa dei cambi

        JSONObject parser = null;

        for(String response : responses) {

            try {
                parser = new JSONObject(response);

                // recupera i nomi delle monete
                String[] splitted = parser.getString("text").split(" ");
                String fromCoin = splitted[1], toCoin = splitted[splitted.length-1];
                double quantity = parser.getDouble("value");

                exchanges.put(new Pair<String, String>(fromCoin, toCoin), quantity);
            }
            catch(JSONException e) {

                Log.d("ERROR", "error in parsing JSON response");
            }
        }

        // calcola i cambi simmetrici

        for (Pair it : exchanges.keySet()) {

            exchanges.put(new Pair(it.second, it.first), 1 / exchanges.get(it));
        }
    }

    /**
     * wrapper del metodo eseguito dal refreshButton
     */
    public void grabData(View v) {

        updateExchanges(requests);
    }

    private void formatConversionResult(double conversionResult, String format) {

        to.setText(String.format(Locale.US, format, conversionResult));
    }

    /**
     * scambia le posizione delle valute
     * @param v vista
     */
    public void swapSpinnersContent(View v) {

        if(from.getText().toString().isEmpty() || to.getText().toString().isEmpty())
            return;

        int tmp = fromSpinner.getSelectedItemPosition();

        fromSpinner.setSelection(toSpinner.getSelectedItemPosition());
        toSpinner.setSelection(tmp);

        Log.d("STRINGS", fromSpinner.getSelectedItem().toString().split("\t\t")[0]);

        String fromCurrCode = Utils.currencyCodes.get(fromSpinner.getSelectedItem().toString().split("\t\t")[0]);
        String toCurrCode = Utils.currencyCodes.get(toSpinner.getSelectedItem().toString().split("\t\t")[0]);

        Log.d("CIAO", from.getText().toString());

        // riesegui la conversione
        double convRes = convert(Double.parseDouble(from.getText().toString()), fromCurrCode, toCurrCode);

        formatConversionResult(convRes, "%.2f");
    }
}
