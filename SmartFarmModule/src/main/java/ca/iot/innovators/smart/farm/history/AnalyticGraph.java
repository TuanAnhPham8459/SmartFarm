package ca.iot.innovators.smart.farm.history;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import ca.iot.innovators.smart.farm.R;
import ca.iot.innovators.smart.farm.firestore.BaseFireStoreManager;
import ca.iot.innovators.smart.farm.firestore.FireStoreService;
import ca.iot.innovators.smart.farm.firestore.SensorDataModel;
import ca.iot.innovators.smart.farm.utils.UnitUtils;


public class AnalyticGraph extends Fragment {

    private static final String TAG = "AnalyticGraph";
    private TextView temperatureTab;
    private TextView humidityTab;
    private TextView soilTab;
    private TextView lightTab;

    private TextView hourTab;
    private TextView dayTab;
    private TextView weekTab;
    private TextView monthTab;
    private TextView customTab;

    private TextView tvSensorTitle;
    private TextView tvMinValue;
    private TextView tvMaxValue;
    private TextView tvAvgValue;
    private TextView tvDateLabel;
    private ImageView btnRefresh;

    private HorizontalScrollView hsvSensorTabs, hsvPeriodTabs;

    private LineChart chart;
    private RecyclerView rvRecentRecords;
    private RecordAdapter recordAdapter;
    private final List<SensorDataModel> recentRecordList = new ArrayList<>();

    private long baseTimestamp = 0;
    private long customStartTime = 0;
    private long customEndTime = 0;
    private String selectedSensor = "Temperature";
    private String selectedPeriod = "Hour";

    private ListenerRegistration historyListener;

    public void refreshForNewDevice() {
        if (!isAdded()) return;
        
        // Reset UI
        if (chart != null) chart.clear();
        if (tvMinValue != null) tvMinValue.setText("--");
        if (tvMaxValue != null) tvMaxValue.setText("--");
        if (tvAvgValue != null) tvAvgValue.setText("--");
        recentRecordList.clear();
        if (recordAdapter != null) recordAdapter.notifyDataSetChanged();
        
        // Restart data fetching
        updateGraph();
    }

    public AnalyticGraph(){}



    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {


        View view = inflater.inflate(
                R.layout.analytic_graph,
                container,
                false);



        // Sensor buttons
        temperatureTab =
                view.findViewById(R.id.temperatureTab);

        humidityTab =
                view.findViewById(R.id.humidityTab);

        soilTab =
                view.findViewById(R.id.soilTab);

        lightTab =
                view.findViewById(R.id.lightTab);



        // Period buttons
        hourTab =
                view.findViewById(R.id.todayTab);

        dayTab =
                view.findViewById(R.id.weekTab);

        weekTab =
                view.findViewById(R.id.monthTab);

        monthTab =
                view.findViewById(R.id.yearTab);

        customTab =
                view.findViewById(R.id.customTab);

        tvSensorTitle = view.findViewById(R.id.tvSensorTitle);
        tvMinValue = view.findViewById(R.id.tvMinValue);
        tvMaxValue = view.findViewById(R.id.tvMaxValue);
        tvAvgValue = view.findViewById(R.id.tvAvgValue);
        tvDateLabel = view.findViewById(R.id.tvDateLabel);

        hsvSensorTabs = view.findViewById(R.id.hsvSensorTabs);
        hsvPeriodTabs = view.findViewById(R.id.hsvPeriodTabs);

        btnRefresh = view.findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Updating Analytics...", Toast.LENGTH_SHORT).show();
            // Show loading state
            chart.clear();
            tvMinValue.setText("--");
            tvMaxValue.setText("--");
            tvAvgValue.setText("--");
            updateGraph();
        });

        chart =
                view.findViewById(R.id.tempChart);

        rvRecentRecords = view.findViewById(R.id.recentRecords);
        rvRecentRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        recordAdapter = new RecordAdapter(recentRecordList);
        rvRecentRecords.setAdapter(recordAdapter);
        rvRecentRecords.setNestedScrollingEnabled(false);

        setupSensorButtons();

        setupPeriodButtons();

        setupChart();


        return view;

    }




    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupSensorButtons();
        setupPeriodButtons();
        setupChart();
        updateGraph(); // Fetch real data and start listening immediately
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            updateGraph();
        } else {
            if (historyListener != null) {
                historyListener.remove();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (historyListener != null) {
            historyListener.remove();
        }
    }

    private void setupSensorButtons(){


        temperatureTab.setOnClickListener(v ->
        {
            selectedSensor = "Temperature";
            selectSensor(temperatureTab);
            revealCards(hsvSensorTabs, temperatureTab);
            updateGraph();
        });



        humidityTab.setOnClickListener(v ->
        {
            selectedSensor = "Humidity";
            selectSensor(humidityTab);
            revealCards(hsvSensorTabs, humidityTab);
            updateGraph();
        });



        soilTab.setOnClickListener(v ->
        {
            selectedSensor = "Soil Moisture";
            selectSensor(soilTab);
            revealCards(hsvSensorTabs, soilTab);
            updateGraph();
        });



        lightTab.setOnClickListener(v ->
        {
            selectedSensor = "Light";
            selectSensor(lightTab);
            revealCards(hsvSensorTabs, lightTab);
            updateGraph();
        });


        selectSensor(temperatureTab);

    }





    private void setupPeriodButtons(){


        hourTab.setOnClickListener(v ->
        {
            selectedPeriod="Hour";
            selectPeriod(hourTab);
            revealCards(hsvPeriodTabs, hourTab);
            updateGraph();
        });



        dayTab.setOnClickListener(v ->
        {
            selectedPeriod="24 Hours";
            selectPeriod(dayTab);
            revealCards(hsvPeriodTabs, dayTab);
            updateGraph();
        });



        weekTab.setOnClickListener(v ->
        {
            selectedPeriod="7 Days";
            selectPeriod(weekTab);
            revealCards(hsvPeriodTabs, weekTab);
            updateGraph();
        });



        monthTab.setOnClickListener(v ->
        {
            selectedPeriod="Month";
            selectPeriod(monthTab);
            revealCards(hsvPeriodTabs, monthTab);
            updateGraph();
        });



        customTab.setOnClickListener(v ->
        {
            selectedPeriod="Custom";
            selectPeriod(customTab);
            revealCards(hsvPeriodTabs, customTab);
            showDateRangePicker();
        });



        selectPeriod(hourTab);

    }





    private void selectSensor(TextView selected){


        TextView[] tabs =
                {
                        temperatureTab,
                        humidityTab,
                        soilTab,
                        lightTab
                };


        for(TextView tab:tabs){

            tab.setBackgroundResource(
                    R.drawable.unselected_tab_background
            );

            tab.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)
            );

        }



        selected.setBackgroundResource(
                R.drawable.history_tab_background
        );


        selected.setTextColor(
                Color.WHITE
        );

    }






    private void selectPeriod(TextView selected){


        TextView[] tabs =
                {
                        hourTab,
                        dayTab,
                        weekTab,
                        monthTab,
                        customTab
                };


        for(TextView tab:tabs){

            tab.setBackgroundResource(
                    R.drawable.unselected_tab_background
            );

            tab.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)
            );

        }



        selected.setBackgroundResource(
                R.drawable.history_tab_background
        );


        selected.setTextColor(
                Color.WHITE
        );

    }






    private void updateGraph(){
        if (historyListener != null) {
            historyListener.remove();
        }

        String deviceId = FireStoreService.getInstance().getSelectedDeviceId();
        if (deviceId == null) {
            chart.clear();
            tvMinValue.setText("--");
            tvMaxValue.setText("--");
            tvAvgValue.setText("--");
            return;
        }

        long startTime = System.currentTimeMillis();
        long endTime = Long.MAX_VALUE;

        switch (selectedPeriod) {
            case "Hour":
                startTime -= 3600000;
                break;
            case "24 Hours":
                startTime -= 86400000;
                break;
            case "7 Days":
                startTime -= 604800000L;
                break;
            case "Month":
                startTime -= 2592000000L;
                break;
            case "Custom":
                if (customStartTime > 0 && customEndTime > 0) {
                    startTime = customStartTime;
                    endTime = customEndTime;
                } else {
                    // Default to last 7 days if custom not set
                    startTime -= 604800000L;
                }
                break;
        }

        String unit;
        boolean useFahrenheit = UnitUtils.isFahrenheit(requireContext());

        switch (selectedSensor) {
            case "Humidity":
            case "Soil Moisture":
                unit = "%";
                break;
            case "Light":
                unit = " lx";
                break;
            default:
                unit = UnitUtils.getTemperatureUnit(useFahrenheit);
                break;
        }
        tvSensorTitle.setText(String.format("%s (%s)", selectedSensor, unit));

        historyListener = FireStoreService.getInstance().getSensorDataManager().listenToHistory(deviceId, startTime, endTime, new BaseFireStoreManager.FireStoreCallbackWithMetadata<List<SensorDataModel>>() {
            @Override
            public void onSuccess(List<SensorDataModel> data, boolean isFromCache) {
                if (isFromCache && tvDateLabel != null) {
                    tvDateLabel.setText(tvDateLabel.getText() + " (Offline/Cached)");
                }
                
                // Re-read units in case they changed in settings
                boolean currentFahrenheit = UnitUtils.isFahrenheit(requireContext());
                String currentUnit;
                switch (selectedSensor) {
                    case "Humidity":
                    case "Soil Moisture":
                        currentUnit = "%";
                        break;
                    case "Light":
                        currentUnit = " lx";
                        break;
                    default:
                        currentUnit = UnitUtils.getTemperatureUnit(currentFahrenheit);
                        break;
                }
                
                processHistoryData(data, currentUnit, currentFahrenheit);
                if (tvSensorTitle != null) {
                    tvSensorTitle.setText(String.format("%s (%s)", selectedSensor, currentUnit));
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to listen to history: " + errorMessage);
            }
        });
    }

    private void showDateRangePicker() {
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .build();

        picker.show(getChildFragmentManager(), "DATE_RANGE_PICKER");

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first != null && selection.second != null) {
                customStartTime = selection.first;
                // Set end time to the very end of the selected day (23:59:59)
                customEndTime = selection.second + 86399999;
                updateGraph();
            }
        });
    }

    private void processHistoryData(List<SensorDataModel> rawData, String unit, boolean useFahrenheit) {
        if (rawData == null || rawData.isEmpty()) {
            chart.clear();
            tvMinValue.setText("--");
            tvMaxValue.setText("--");
            tvAvgValue.setText("--");
            recentRecordList.clear();
            recordAdapter.notifyDataSetChanged();
            return;
        }

        // Update Recent Records list (latest 10 raw entries)
        recentRecordList.clear();
        int recordCount = Math.min(rawData.size(), 10);
        for (int i = 0; i < recordCount; i++) {
            // rawData is sorted ASC, so we take from the end for "recent"
            recentRecordList.add(rawData.get(rawData.size() - 1 - i));
        }
        recordAdapter.setUseFahrenheit(useFahrenheit);
        recordAdapter.notifyDataSetChanged();

        // Determine bucket size based on period
        long bucketSize;
        if (selectedPeriod.equals("Custom")) {
            long diff = customEndTime - customStartTime;
            if (diff <= 86400000) { // 1 day
                bucketSize = 60000; // 1 minute
            } else if (diff <= 604800000L) { // 7 days
                bucketSize = 3600000; // 1 hour
            } else {
                bucketSize = 86400000; // 1 day
            }
        } else {
            switch (selectedPeriod) {
                case "Hour":
                    bucketSize = 60000; // 1 minute
                    break;
                case "24 Hours":
                    bucketSize = 3600000; // 1 hour
                    break;
                case "7 Days":
                case "Month":
                    bucketSize = 86400000; // 1 day
                    break;
                default:
                    bucketSize = 60000;
            }
        }

        // Aggregate data into buckets
        Map<Long, List<Double>> buckets = new TreeMap<>(); // Sorted by timestamp
        for (SensorDataModel record : rawData) {
            long bucketKey = (record.getTimestamp() / bucketSize) * bucketSize;
            double value;
            switch (selectedSensor) {
                case "Humidity":
                    value = record.getHumidity();
                    break;
                case "Light":
                    value = record.getLight();
                    break;
                default:
                    value = UnitUtils.convertTemperature(record.getTemperature(), useFahrenheit);
                    break;
            }

            List<Double> bucket = buckets.get(bucketKey);
            if (bucket == null) {
                bucket = new ArrayList<>();
                buckets.put(bucketKey, bucket);
            }
            bucket.add(value);
        }

        // Calculate averages per bucket and prepare entries
        ArrayList<Entry> entries = new ArrayList<>();
        baseTimestamp = buckets.keySet().iterator().next(); // First bucket is base

        double totalMin = Double.MAX_VALUE;
        double totalMax = Double.MIN_VALUE;
        double totalSum = 0;
        int count = 0;

        for (Map.Entry<Long, List<Double>> entry : buckets.entrySet()) {
            double sum = 0;
            for (Double val : entry.getValue()) sum += val;
            double avg = sum / entry.getValue().size();

            float xValue = (float) (entry.getKey() - baseTimestamp);
            entries.add(new Entry(xValue, (float) avg));

            if (avg < totalMin) totalMin = avg;
            if (avg > totalMax) totalMax = avg;
            totalSum += avg;
            count++;
        }

        double totalAvg = totalSum / count;

        tvMinValue.setText(String.format(Locale.getDefault(), "%.1f%s", totalMin, unit));
        tvMaxValue.setText(String.format(Locale.getDefault(), "%.1f%s", totalMax, unit));
        tvAvgValue.setText(String.format(Locale.getDefault(), "%.1f%s", totalAvg, unit));

        // Update date label with current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        String dateStr = dateFormat.format(new Date());
        tvDateLabel.setText(dateStr);

        // Configure X-axis with the dynamic time formatter
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new TimeAxisValueFormatter(baseTimestamp, selectedPeriod));

        LineDataSet dataSet = new LineDataSet(entries, selectedSensor);
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(Color.parseColor("#4CAF50")); // Green theme
        dataSet.setCircleColor(Color.parseColor("#4CAF50"));

        chart.setData(new LineData(dataSet));
        chart.invalidate();
    }
    private void setupChart(){
        int textColor = ContextCompat.getColor(requireContext(), R.color.text_primary);
        
        // Remove description text
        chart.getDescription()
                .setEnabled(false);

        // Remove right axis
        chart.getAxisRight()
                .setEnabled(false);

        // X axis settings
        XAxis xAxis =
                chart.getXAxis();

        xAxis.setPosition(
                XAxis.XAxisPosition.BOTTOM
        );

        xAxis.setGranularity(1f);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setTextColor(textColor);
        xAxis.setAxisLineColor(textColor);

        // Y axis settings
            YAxis yAxis =
                chart.getAxisLeft();

        yAxis.setAxisMinimum(0f);
        yAxis.setTextColor(textColor);
        yAxis.setAxisLineColor(textColor);
        
        // Grid lines
        int gridColor = ContextCompat.getColor(requireContext(), R.color.light_gray);
        if ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            gridColor = Color.parseColor("#33FFFFFF"); // Subtle white for dark mode
        }
        xAxis.setGridColor(gridColor);
        yAxis.setGridColor(gridColor);

        // Legend
        chart.getLegend().setTextColor(textColor);

        // Animation
        chart.animateX(1000);

        chart.invalidate();

    }

    /**
     * Smoothly scrolls the HorizontalScrollView to bring the selected tab into view and reveal neighbors.
     */
    private void revealCards(HorizontalScrollView hsv, View selectedView) {
        int screenWidth = hsv.getWidth();
        int viewX = selectedView.getLeft();
        int viewWidth = selectedView.getWidth();
        
        // Target: move the view towards the center area to reveal more on both sides
        int targetX = viewX - (screenWidth / 2) + (viewWidth / 2);
        hsv.smoothScrollTo(targetX, 0);
    }

    /**
     * Formatter for the X-axis that converts time offsets back into readable date/time strings.
     */
    private static class TimeAxisValueFormatter extends ValueFormatter {
        private final long baseTimestamp;
        private final String period;
        private final SimpleDateFormat hourFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        private final SimpleDateFormat dayFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        private final SimpleDateFormat weekFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

        public TimeAxisValueFormatter(long baseTimestamp, String period) {
            this.baseTimestamp = baseTimestamp;
            this.period = period;
        }

        @Override
        public String getFormattedValue(float value) {
            long actualTimestamp = baseTimestamp + (long) value;
            Date date = new Date(actualTimestamp);

            switch (period) {
                case "Hour":
                    return hourFormat.format(date);
                case "24 Hours":
                    return dayFormat.format(date);
                case "7 Days":
                    return weekFormat.format(date);
                case "Month":
                    return monthFormat.format(date);
                default:
                    return hourFormat.format(date);
            }
        }
    }

    private class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {
        private final List<SensorDataModel> records;
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault());
        private boolean useFahrenheit = false;

        public RecordAdapter(List<SensorDataModel> records) {
            this.records = records;
        }

        public void setUseFahrenheit(boolean useFahrenheit) {
            this.useFahrenheit = useFahrenheit;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor_record, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SensorDataModel record = records.get(position);
            holder.tvTime.setText(timeFormat.format(new Date(record.getTimestamp())));

            double value;
            String unit;
            switch (selectedSensor) {
                case "Humidity":
                    value = record.getHumidity();
                    unit = "%";
                    break;
                case "Light":
                    value = record.getLight();
                    unit = " lx";
                    break;
                default:
                    value = UnitUtils.convertTemperature(record.getTemperature(), useFahrenheit);
                    unit = UnitUtils.getTemperatureUnit(useFahrenheit);
                    break;
            }
            holder.tvValue.setText(String.format(Locale.getDefault(), "%.1f%s", value, unit));
        }

        @Override
        public int getItemCount() {
            return records.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvValue;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tvRecordTime);
                tvValue = itemView.findViewById(R.id.tvRecordValue);
            }
        }
    }

}
