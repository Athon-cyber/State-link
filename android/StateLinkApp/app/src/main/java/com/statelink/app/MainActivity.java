package com.statelink.app;

import android.app.AlertDialog;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/**
 * MainActivity — the single screen of the StateLink app.
 *
 * Responsibilities:
 *  - Keep the screen always on via WakeLock + FLAG_KEEP_SCREEN_ON
 *  - Poll the desktop server at http://localhost:8765/stats every second
 *  - Display CPU, Memory, Disk, Network speed, and Battery in cards
 *  - Allow switching between 3 built-in themes at runtime
 */
public class MainActivity extends AppCompatActivity {

    // ── UI references ────────────────────────────────────
    private View connectionDot;
    private TextView connectionText;

    private TextView cpuValue, cpuSubtext;
    private TextView memoryValue, memorySubtext;
    private TextView diskValue, diskSubtext;
    private TextView networkDownValue, networkUpValue;
    private TextView batteryPercent, batteryStatus;
    private LinearLayout batteryCard;

    private ProgressBar cpuBar, memoryBar, diskBar;

    // ── WakeLock ─────────────────────────────────────────
    private PowerManager.WakeLock wakeLock;

    // ── Network / threading ──────────────────────────────
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;

    private static final String STATS_URL = "http://localhost:8765/stats";
    private static final int POLL_INTERVAL_MS = 1000; // 1 second

    // ── Theme manager ────────────────────────────────────
    private ThemeManager themeManager;

    // ══════════════════════════════════════════════════════
    //  LIFECYCLE
    // ══════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ── Apply theme BEFORE super.onCreate ──
        themeManager = new ThemeManager(this);
        setTheme(themeManager.getCurrentThemeResId());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Screen always-on (two layers: flag + wakeLock)
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        acquireWakeLock();

        // Wire up views
        initViews();

        // Theme switcher button
        findViewById(R.id.btn_theme).setOnClickListener(v -> showThemePicker());

        // Refresh button (manual reconnect)
        findViewById(R.id.btn_refresh).setOnClickListener(v -> {
            updateConnectionUI(false);
            Toast.makeText(this, "Reconnecting...", Toast.LENGTH_SHORT).show();
        });

        // Start polling loop
        startPolling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
        networkExecutor.shutdownNow();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    // ══════════════════════════════════════════════════════
    //  WAKE LOCK — screen always on
    // ══════════════════════════════════════════════════════

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                    | PowerManager.ON_AFTER_RELEASE,
                "StateLink:ScreenAlwaysOn"
            );
            wakeLock.acquire();
        }
    }

    // ══════════════════════════════════════════════════════
    //  VIEW BINDING
    // ══════════════════════════════════════════════════════

    private void initViews() {
        // Connection indicator
        connectionDot = findViewById(R.id.connection_dot);
        connectionText = findViewById(R.id.connection_text);

        // CPU
        cpuValue = findViewById(R.id.cpu_value);
        cpuSubtext = findViewById(R.id.cpu_subtext);
        cpuBar = findViewById(R.id.cpu_bar);

        // Memory
        memoryValue = findViewById(R.id.memory_value);
        memorySubtext = findViewById(R.id.memory_subtext);
        memoryBar = findViewById(R.id.memory_bar);

        // Disk
        diskValue = findViewById(R.id.disk_value);
        diskSubtext = findViewById(R.id.disk_subtext);
        diskBar = findViewById(R.id.disk_bar);

        // Network
        networkDownValue = findViewById(R.id.network_down_value);
        networkUpValue = findViewById(R.id.network_up_value);

        // Battery
        batteryPercent = findViewById(R.id.battery_percent);
        batteryStatus = findViewById(R.id.battery_status);
        batteryCard = findViewById(R.id.battery_card);
    }

    // ══════════════════════════════════════════════════════
    //  NETWORK POLLING
    // ══════════════════════════════════════════════════════

    private void startPolling() {
        networkExecutor.execute(() -> {
            while (running) {
                try {
                    fetchAndUpdate();
                } catch (Exception ignored) {
                    // Network errors are handled inside fetchAndUpdate
                }
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    private void fetchAndUpdate() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(STATS_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8")
                );
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
                reader.close();

                JSONObject stats = new JSONObject(body.toString());

                if ("ok".equals(stats.optString("status"))) {
                    final JSONObject finalStats = stats;
                    mainHandler.post(() -> {
                        updateAllCards(finalStats);
                        updateConnectionUI(true);
                    });
                    return;
                }
            }

            // Any non-200 response = disconnected
            mainHandler.post(() -> updateConnectionUI(false));

        } catch (Exception e) {
            mainHandler.post(() -> updateConnectionUI(false));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // ══════════════════════════════════════════════════════
    //  UI UPDATES
    // ══════════════════════════════════════════════════════

    private void updateConnectionUI(boolean connected) {
        if (connected) {
            connectionDot.setBackgroundResource(R.drawable.dot_green);
            connectionText.setText(R.string.connected);
            connectionText.setTextColor(0xFF4CAF50);
        } else {
            connectionDot.setBackgroundResource(R.drawable.dot_red);
            connectionText.setText(R.string.waiting_pc);
            connectionText.setTextColor(0xFFF44336);
        }
    }

    private void updateAllCards(JSONObject s) {
        updateCpu(s);
        updateMemory(s);
        updateDisk(s);
        updateNetwork(s);
        updateBattery(s);
    }

    private void updateCpu(JSONObject s) {
        double cpu = s.optDouble("cpu", 0);
        int cores = s.optInt("cpu_cores", 0);

        cpuValue.setText(String.format("%.1f%%", cpu));
        cpuSubtext.setText(cores + " cores");

        int pct = (int) Math.round(cpu);
        cpuBar.setProgress(pct);
        setBarColor(cpuBar, cpu);
    }

    private void updateMemory(JSONObject s) {
        double mem = s.optDouble("memory", 0);
        double used = s.optDouble("memory_used_gb", 0);
        double total = s.optDouble("memory_total_gb", 0);

        memoryValue.setText(String.format("%.1f%%", mem));
        memorySubtext.setText(String.format("%.1f / %.1f GB", used, total));

        int pct = (int) Math.round(mem);
        memoryBar.setProgress(pct);
        setBarColor(memoryBar, mem);
    }

    private void updateDisk(JSONObject s) {
        double disk = s.optDouble("disk", 0);
        double used = s.optDouble("disk_used_gb", 0);
        double total = s.optDouble("disk_total_gb", 0);

        diskValue.setText(String.format("%.1f%%", disk));
        diskSubtext.setText(String.format("%.1f / %.1f GB", used, total));

        int pct = (int) Math.round(disk);
        diskBar.setProgress(pct);
        setBarColor(diskBar, disk);
    }

    private void updateNetwork(JSONObject s) {
        double down = s.optDouble("network_down_kbs", 0);
        double up = s.optDouble("network_up_kbs", 0);

        networkDownValue.setText(formatSpeed(down) + " ↓");
        networkUpValue.setText(formatSpeed(up) + " ↑");
    }

    private void updateBattery(JSONObject s) {
        double battery = s.optDouble("battery", -1);

        if (battery >= 0) {
            batteryCard.setVisibility(View.VISIBLE);
            batteryPercent.setText(String.format("%.0f%%", battery));

            boolean charging = s.optBoolean("battery_charging", false);
            if (charging) {
                batteryStatus.setText(R.string.charging);
            } else {
                batteryStatus.setText(R.string.on_battery);
            }
        } else {
            // Desktop PC — no battery
            batteryCard.setVisibility(View.GONE);
        }
    }

    // ══════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════

    private void setBarColor(ProgressBar bar, double pct) {
        Drawable d = bar.getProgressDrawable();
        if (d == null) return;

        int color;
        if (pct < 50) {
            color = 0xFF4CAF50;
        } else if (pct < 80) {
            color = 0xFFFF9800;
        } else {
            color = 0xFFF44336;
        }
        d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private String formatSpeed(double kbps) {
        if (kbps < 0.1) {
            return "0 KB/s";
        } else if (kbps < 1024) {
            return String.format("%.1f KB/s", kbps);
        } else {
            return String.format("%.1f MB/s", kbps / 1024.0);
        }
    }

    // ══════════════════════════════════════════════════════
    //  THEME PICKER
    // ══════════════════════════════════════════════════════

    private void showThemePicker() {
        String[] themeNames = {
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_blue),
        };

        new AlertDialog.Builder(this)
            .setTitle(R.string.select_theme)
            .setItems(themeNames, (dialog, which) -> {
                themeManager.setTheme(which);
                // Recreate the activity to apply the new theme
                recreate();
            })
            .show();
    }
}
