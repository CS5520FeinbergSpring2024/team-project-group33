package edu.northeastern.numad24sp_team33_final.markets;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.northeastern.numad24sp_team33_final.R;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

public class AssetStatusActivity extends AppCompatActivity {
    public static final String TICKER_SYMBOL_KEY = "ticketSymbolKey";
    private String tickerSymbol;
    private TextView assetTitleTextView;
    private TimeRange currentTimeRange;
    private LineChart lineChart;
    private Button buttonDayView, buttonWeekView, buttonMonthView, buttonYearView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_status);

        assetTitleTextView = findViewById(R.id.assetTitle);
        lineChart = findViewById(R.id.lineChart);
        buttonDayView = findViewById(R.id.buttonDayView);
        buttonDayView.setOnClickListener(view -> setupGraphForTimeRange(TimeRange.DAY));

        buttonWeekView = findViewById(R.id.buttonWeekView);
        buttonWeekView.setOnClickListener(view -> setupGraphForTimeRange(TimeRange.WEEK));

        buttonMonthView = findViewById(R.id.buttonMonthView);
        buttonMonthView.setOnClickListener(view -> setupGraphForTimeRange(TimeRange.MONTH));

        buttonYearView = findViewById(R.id.buttonYearView);
        buttonYearView.setOnClickListener(view -> setupGraphForTimeRange(TimeRange.YEAR));

        // Load ticket symbol from state
        tickerSymbol = savedInstanceState.getString(TICKER_SYMBOL_KEY, "AAPL");
        assetTitleTextView.setText(tickerSymbol);

        // Default to Day View time range
        setupGraphForTimeRange(TimeRange.DAY);
    }

    private void setupGraphForTimeRange(TimeRange timeRange) {
        // Update Graph with time range
        loadAssetDataFromTimeRange(timeRange);
        currentTimeRange = timeRange;
    }

    private void loadAssetDataFromTimeRange(TimeRange timeRange) {
        List<HistoricalQuote> historicalQuotes = fetchHistoricalData(timeRange);

        if (historicalQuotes != null && !historicalQuotes.isEmpty()) {
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < historicalQuotes.size(); i++) {
                HistoricalQuote quote = historicalQuotes.get(i);
                float value = (float) quote.getClose().doubleValue();
                entries.add(new Entry(i, value));
            }

            LineDataSet dataSet = new LineDataSet(entries, "Stock Price");
            LineData lineData = new LineData(dataSet);
            lineChart.setData(lineData);
            lineChart.invalidate(); // refresh chart
        } else {
            Toast.makeText(this, "Failed to fetch data for the specified time range: " + timeRange, Toast.LENGTH_SHORT).show();
        }
    }

    private List<HistoricalQuote> fetchHistoricalData(TimeRange timeRange) {
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        Interval interval = timeRange == TimeRange.YEAR ? Interval.WEEKLY : Interval.DAILY;

        switch (timeRange) {
            case DAY:
                from.add(Calendar.DAY_OF_YEAR, -1);
            case WEEK:
                from.add(Calendar.WEEK_OF_YEAR, -1);
            case MONTH:
                from.add(Calendar.MONTH, -1);
            case YEAR:
                from.add(Calendar.YEAR, -1);
        }

        try {
            Stock currentStock = YahooFinance.get(tickerSymbol, from, to, interval);
            return currentStock.getHistory();
        } catch (Exception ex) {
            Toast.makeText(this, "Unable to get stock details for " + tickerSymbol, Toast.LENGTH_LONG).show();
            return new ArrayList<>();
        }
    }

    private enum TimeRange {
        DAY,
        WEEK,
        MONTH,
        YEAR
    }
}