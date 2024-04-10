package edu.northeastern.numad24sp_team33_final.markets;

import static edu.northeastern.numad24sp_team33_final.markets.AssetStatusActivity.TimeRange.DAY;
import static edu.northeastern.numad24sp_team33_final.markets.AssetStatusActivity.TimeRange.MONTH;
import static edu.northeastern.numad24sp_team33_final.markets.AssetStatusActivity.TimeRange.WEEK;
import static edu.northeastern.numad24sp_team33_final.markets.AssetStatusActivity.TimeRange.YEAR;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import edu.northeastern.numad24sp_team33_final.BettingManager;
import edu.northeastern.numad24sp_team33_final.R;
import edu.northeastern.numad24sp_team33_final.Bet;

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
    private TextView pointTextView, maxBetPointsView;
    private EditText currentPointGuessView;
    private final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private int userPoints = 0;
    private TextView tvGuessLowStatus, tvGuessHighStatus;
    private BettingManager bettingManager;
    private TextView tvRewardRatioLow, tvRewardRatioHigh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_status);
        alphaVantageApi.init(apiConfig);
        lineChart = findViewById(R.id.lineChart);
        fetchUserPoints();
        currentPointGuessView = findViewById(R.id.currentPointGuessView);
        maxBetPointsView = findViewById(R.id.maxBetPointsView);
        bettingManager = new BettingManager();
        tickerSymbol = getIntent().getExtras().getString(TICKER_SYMBOL_KEY, "AAPL");

        tvGuessLowStatus = findViewById(R.id.tvGuessLowStatus);
        tvGuessHighStatus = findViewById(R.id.tvGuessHighStatus);
        tvRewardRatioLow = findViewById(R.id.tvRewardRatioLow);
        tvRewardRatioHigh = findViewById(R.id.tvRewardRatioHigh);

        ZonedDateTime nowInET = ZonedDateTime.now(ZoneId.of("America/New_York"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        String formattedDate = nowInET.format(formatter);
        fetchAndUpdateBettingStatus(tickerSymbol, formattedDate);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //Dan Debug lines
//        int betAmount = Integer.parseInt(currentPointGuessView.getText().toString());
//        Log.d("BettingSystem", "Placing bet. User ID: " + userId + ", Bet Amount: " + betAmount);
//        bettingManager.placeBet(tickerSymbol, true, userId, betAmount);
        //Dan Debug lines

        Button guessHighButton = findViewById(R.id.guessHighButton);
        guessHighButton.setOnClickListener(view -> {
            Log.d("BettingSystem", "High bet button clicked");
            int betAmount = Integer.parseInt(currentPointGuessView.getText().toString());
            bettingManager.placeBet(tickerSymbol, true, userId, betAmount, formattedDate, this::updateAfterBet);
        });

        Button guessLowButton = findViewById(R.id.guessLowButton);
        guessLowButton.setOnClickListener(view -> {
            Log.d("BettingSystem", "Low bet button clicked");
            int betAmount = Integer.parseInt(currentPointGuessView.getText().toString());
            bettingManager.placeBet(tickerSymbol, false, userId, betAmount, formattedDate, this::updateAfterBet);
        });

        currentPointGuessView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    int value = Integer.parseInt(s.toString());
                    if (value > userPoints) {
                        currentPointGuessView.setText(String.valueOf(userPoints)); // Set to max if over userPoints
                    } else if (value < 0) {
                        currentPointGuessView.setText("0"); // Set to 0 if negative
                    }
                }
            }
        });

        ImageButton increaseGuessButton = findViewById(R.id.increaseGuessButton);
        increaseGuessButton.setOnClickListener(view -> adjustBetAmount(true));

        ImageButton decreaseGuessButton = findViewById(R.id.decreaseGuessButton);
        decreaseGuessButton.setOnClickListener(view -> adjustBetAmount(false));

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

        TextView assetTitleTextView = findViewById(R.id.assetTitle);
        assetTitleTextView.setText(tickerSymbol);

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

    private void fetchUserPoints() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("Firebase", "Data fetched successfully");
                if (dataSnapshot.exists() && dataSnapshot.child("points").getValue(Integer.class) != null) {
                    userPoints = dataSnapshot.child("points").getValue(Integer.class);
                    updateUIWithUserPoints();
                    adjustCurrentPointGuessView();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("Firebase", "Error fetching user points: " + databaseError.getMessage());
            }
        });
    }

    private void updateUIWithUserPoints() {
        if (maxBetPointsView != null) {
            maxBetPointsView.setText(String.format("Max points: %d", userPoints));
        }
    }

    private void adjustCurrentPointGuessView() {
        runOnUiThread(() -> {
            if (!currentPointGuessView.getText().toString().isEmpty()) {
                int currentBet = Integer.parseInt(currentPointGuessView.getText().toString());
                if (currentBet > userPoints) {
                    currentPointGuessView.setText(String.valueOf(userPoints));
                }
            }
        });
    }

    private void adjustBetAmount(boolean increase) {
        int currentBet = 0;
        if (!currentPointGuessView.getText().toString().isEmpty()) {
            currentBet = Integer.parseInt(currentPointGuessView.getText().toString());
        }
        if (increase) {
            currentBet += 10;
        } else {
            currentBet -= 10;
        }

        currentBet = Math.max(0, currentBet);
        currentBet = Math.min(currentBet, userPoints);
        currentPointGuessView.setText(String.valueOf(currentBet));
    }

    private void fetchAndUpdateBettingStatus(String assetId, String date) {
        DatabaseReference betsRef = FirebaseDatabase.getInstance().getReference("bets").child(assetId).child(date.replace("/", "-"));
        betsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long totalLow = 0, totalHigh = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Bet bet = snapshot.getValue(Bet.class);
                    if (bet != null) {
                        if (bet.isHigh()) totalHigh += bet.getAmount();
                        else totalLow += bet.getAmount();
                    }
                }

                final long finalTotalLow = totalLow;
                final long finalTotalHigh = totalHigh;
                final double rewardRatioLow = totalHigh > 0 ? (double) (totalLow + totalHigh) / totalLow : 0;
                final double rewardRatioHigh = totalLow > 0 ? (double) (totalLow + totalHigh) / totalHigh : 0;

                // Updating the UI
                runOnUiThread(() -> {
                    tvGuessLowStatus.setText(String.format("Low: %d", finalTotalLow));
                    tvGuessHighStatus.setText(String.format("High: %d", finalTotalHigh));
                    tvRewardRatioLow.setText(String.format("Reward Ratio: %.2f", rewardRatioLow));
                    tvRewardRatioHigh.setText(String.format("Reward Ratio: %.2f", rewardRatioHigh));
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("Firebase", "Failed to read value.", databaseError.toException());
            }
        });
    }

    private void updateAfterBet() {
        fetchUserPoints();
        runOnUiThread(() -> {

            if (!currentPointGuessView.getText().toString().isEmpty()) {
                int currentBet = Integer.parseInt(currentPointGuessView.getText().toString());
                if (currentBet > userPoints) {
                    currentPointGuessView.setText(String.valueOf(userPoints));
                }
            }
            checkAndDisableBettingIfNeeded();
        });
    }

    private boolean isPastResetTime() {
        ZonedDateTime nyNow = ZonedDateTime.now(ZoneId.of("America/New_York"));
        LocalTime resetTime = LocalTime.of(16, 0); // When market close, 4pm ET
        return nyNow.toLocalTime().isAfter(resetTime);
    }

    private void checkAndDisableBettingIfNeeded() {
        boolean pastResetTime = isPastResetTime();

        Button guessHighButton = findViewById(R.id.guessHighButton);
        Button guessLowButton = findViewById(R.id.guessLowButton);

        guessHighButton.setEnabled(!pastResetTime);
        guessLowButton.setEnabled(!pastResetTime);

        if (pastResetTime) {
            Toast.makeText(this, "Betting is closed for today. Come back tomorrow!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndDisableBettingIfNeeded();
    }
}