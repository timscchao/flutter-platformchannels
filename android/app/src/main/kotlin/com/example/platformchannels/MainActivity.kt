package com.example.platformchannels

import android.content.*
import android.os.BatteryManager
import android.os.Build
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {

    companion object {
        private const val BATTERY_CHANNEL = "samples.flutter.io/battery"
        private const val CHARGING_CHANNEL = "samples.flutter.io/charging"
    }

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, Companion.BATTERY_CHANNEL).setMethodCallHandler {
                call, result ->
            // Note: this method is invoked on the main thread.
            if (call.method == "getBatteryLevel") {
                val batteryLevel = getBatteryLevel()
                if (batteryLevel != -1) {
                    result.success(batteryLevel)
                } else {
                    result.error("UNAVAILABLE", "Battery level not available.", null)
                }
            } else {
                result.notImplemented()
            }
        }
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, Companion.CHARGING_CHANNEL).setStreamHandler(
            object : EventChannel.StreamHandler {
                var receiver : BroadcastReceiver? = null
                override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                    receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent) {
                            var status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                            if (status == BatteryManager.BATTERY_STATUS_UNKNOWN) {
                                events.error("UNAVAILABLE", "Charging status unavailable", null);
                            } else {
                                val isCharging =
                                    status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                            status == BatteryManager.BATTERY_STATUS_FULL
                                events.success(if (isCharging) "charging" else "discharging")
                            }
                        }
                    }
                    ContextWrapper(applicationContext).registerReceiver(
                        receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                    )
                }
                override fun onCancel(arguments: Any?) {
                    ContextWrapper(applicationContext).unregisterReceiver(receiver)
                    receiver = null
                }
            }
        )
    }

    private fun getBatteryLevel(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            val intent = ContextWrapper(applicationContext).registerReceiver(
                null, IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED
                )
            )
            intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / intent.getIntExtra(
                BatteryManager.EXTRA_SCALE,
                -1
            )
        }
    }
}
