package edu.northeastern.numad24sp_team33_final.markets;

import static edu.northeastern.numad24sp_team33_final.markets.AssetStatusActivity.TimeRange.DAY;
import static edu.northeastern.numad24sp_team33_final.markets.AssetStatusActivity.TimeRange.MONTH;
import static edu.northeastern.numad24sp_team33_final.markets.AssetStatusActivity.TimeRange.WEEK;
import static edu.northeastern.numad24sp_team33_final.markets.AssetStatusActivity.TimeRange.YEAR;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.crazzyghost.alphavantage.AlphaVantage;
import com.crazzyghost.alphavantage.Config;
import com.crazzyghost.alphavantage.parameters.Interval;
import com.crazzyghost.alphavantage.parameters.OutputSize;
import com.crazzyghost.alphavantage.timeseries.response.StockUnit;
import com.crazzyghost.alphavantage.timeseries.response.TimeSeriesResponse;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

import edu.northeastern.numad24sp_team33_final.R;

public class AssetStatusActivity extends AppCompatActivity {
    public static final String TICKER_SYMBOL_KEY = "tickerSymbolKey";
    private String tickerSymbol;
    private TimeRange currentTimeRange;
    private LineChart lineChart;
    private Config apiConfig = Config.builder()
            .key("VHPNXPBURA235OV5")
            .timeOut(10)
            .build();
    private AlphaVantage alphaVantageApi = AlphaVantage.api();
    private Thread assetDataFetcherThread;
    private Handler assetDataHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_status);
        alphaVantageApi.init(apiConfig);
        lineChart = findViewById(R.id.lineChart);
        TextView assetTitleTextView = findViewById(R.id.assetTitle);

        Button buttonDayView = findViewById(R.id.buttonDayView);
        buttonDayView.setOnClickListener(view -> setupGraphForTimeRange(DAY));

        Button buttonWeekView = findViewById(R.id.buttonWeekView);
        buttonWeekView.setOnClickListener(view -> setupGraphForTimeRange(WEEK));

        Button buttonMonthView = findViewById(R.id.buttonMonthView);
        buttonMonthView.setOnClickListener(view -> setupGraphForTimeRange(MONTH));

        Button buttonYearView = findViewById(R.id.buttonYearView);
        buttonYearView.setOnClickListener(view -> setupGraphForTimeRange(YEAR));

        Button refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(view -> setupGraph());

        // Load ticket symbol from state
        tickerSymbol = getIntent().getExtras().getString(TICKER_SYMBOL_KEY, "AAPL");
        assetTitleTextView.setText(tickerSymbol);

        // Default to Day View time range
        setupGraphForTimeRange(DAY);
    }

    private void setupGraphForTimeRange(TimeRange timeRange) {
        // Update Graph with time range
        currentTimeRange = timeRange;
        setupGraph();
    }

    private void setupGraph() {
        assetDataFetcherThread = new Thread(new AssetDataFetcherRunnable());
        assetDataFetcherThread.start();
    }

    private void loadAssetDataFromTimeRange(TimeSeriesResponse response) {
        List<StockUnit> stockUnits = response.getStockUnits();

        if (stockUnits != null && !stockUnits.isEmpty()) {
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < stockUnits.size(); i++) {
                StockUnit unit = stockUnits.get(i);
                float value = (float) unit.getClose();
                entries.add(new Entry(i, value));
            }

            LineDataSet dataSet = new LineDataSet(entries, "Stock Price");
            LineData lineData = new LineData(dataSet);
            lineChart.setData(lineData);
            lineChart.invalidate(); // refresh chart
        } else {
            Toast.makeText(this, "Failed to fetch data for the specified time range", Toast.LENGTH_SHORT).show();
        }
    }

    public class AssetDataFetcherRunnable implements Runnable {
        @Override
        public void run() {
            TimeSeriesResponse timeSeriesResponse = fetchTimeSeriesResponseByTimeRange();

            try {
                assetDataHandler.postAtFrontOfQueue(() -> loadAssetDataFromTimeRange(timeSeriesResponse));
            } catch (Exception ex) {
                Log.i(AssetStatusActivity.class.getName(), "Unable to pull Stock data: ");
            }
        }

        private TimeSeriesResponse fetchTimeSeriesResponseByTimeRange() {
            switch (currentTimeRange) {
                case DAY:
                default:
                    return alphaVantageApi
                            .timeSeries()
                            .intraday()
                            .forSymbol(tickerSymbol)
                            .interval(Interval.THIRTY_MIN)
                            .outputSize(OutputSize.FULL)
                            .fetchSync();
                case WEEK:
                    return alphaVantageApi
                            .timeSeries()
                            .daily()
                            .forSymbol(tickerSymbol)
                            .fetchSync();
                case MONTH:
                    return alphaVantageApi
                            .timeSeries()
                            .weekly()
                            .forSymbol(tickerSymbol)
                            .fetchSync();
                case YEAR:
                    return alphaVantageApi
                            .timeSeries()
                            .monthly()
                            .forSymbol(tickerSymbol)
                            .fetchSync();
            }
        }
    }

    protected enum TimeRange {
        DAY,
        WEEK,
        MONTH,
        YEAR
    }
}