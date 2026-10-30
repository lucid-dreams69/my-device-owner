package io.github.luciddreams69.deviceowner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Status screen plus a few reversible policies to try once the app is device owner. */
public class MainActivity extends Activity {

    private DevicePolicyManager dpm;
    private ComponentName admin;

    private TextView status;
    private TextView statusDetail;
    private TextView info;
    private TextView log;
    private Switch cameraSwitch;
    private Switch screenshotSwitch;
    private EditText lockInfo;
    private Button saveLockInfo;
    private Button lockNow;
    private Button removeOwner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dpm = getSystemService(DevicePolicyManager.class);
        admin = AdminReceiver.component(this);

        // Android 15 draws apps edge to edge; keep content clear of the status/nav bars.
        findViewById(R.id.root).setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsets.CONSUMED;
        });

        status = findViewById(R.id.status);
        statusDetail = findViewById(R.id.status_detail);
        info = findViewById(R.id.info);
        log = findViewById(R.id.log);
        cameraSwitch = findViewById(R.id.camera_switch);
        screenshotSwitch = findViewById(R.id.screenshot_switch);
        lockInfo = findViewById(R.id.lock_info);
        saveLockInfo = findViewById(R.id.save_lock_info);
        lockNow = findViewById(R.id.lock_now);
        removeOwner = findViewById(R.id.remove_owner);

        saveLockInfo.setOnClickListener(v -> {
            String text = lockInfo.getText().toString().trim();
            dpm.setDeviceOwnerLockScreenInfo(admin, text.isEmpty() ? null : text);
            toast(R.string.lock_info_saved);
        });
        lockNow.setOnClickListener(v -> dpm.lockNow());
        removeOwner.setOnClickListener(v -> confirmRemoveOwner());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean owner = dpm.isDeviceOwnerApp(getPackageName());

        status.setText(owner ? R.string.status_owner : R.string.status_not_owner);
        statusDetail.setText(owner
                ? getString(R.string.status_owner_detail)
                : getString(R.string.status_not_owner_detail, admin.flattenToShortString()));

        info.setText(getString(R.string.info_format,
                Build.MANUFACTURER + " " + Build.MODEL,
                Build.VERSION.RELEASE, Build.VERSION.SDK_INT,
                Build.VERSION.SECURITY_PATCH,
                versionName(),
                admin.flattenToShortString()));

        String events = ProvisioningLog.read(this);
        log.setText(events.isEmpty() ? getString(R.string.log_empty) : events);

        bindSwitch(cameraSwitch, owner, owner && dpm.getCameraDisabled(admin),
                (b, checked) -> dpm.setCameraDisabled(admin, checked));
        bindSwitch(screenshotSwitch, owner, owner && dpm.getScreenCaptureDisabled(admin),
                (b, checked) -> dpm.setScreenCaptureDisabled(admin, checked));

        CharSequence current = owner ? dpm.getDeviceOwnerLockScreenInfo() : null;
        if (!lockInfo.hasFocus()) lockInfo.setText(current);
        lockInfo.setEnabled(owner);
        saveLockInfo.setEnabled(owner);
        lockNow.setEnabled(owner);
        removeOwner.setEnabled(owner);
    }

    private void bindSwitch(Switch view, boolean enabled, boolean checked,
                            CompoundButton.OnCheckedChangeListener onChange) {
        view.setOnCheckedChangeListener(null);
        view.setChecked(checked);
        view.setEnabled(enabled);
        view.setOnCheckedChangeListener(onChange);
    }

    private void confirmRemoveOwner() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_owner_title)
                .setMessage(R.string.remove_owner_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.remove_owner_confirm, (d, w) -> removeOwner())
                .show();
    }

    @SuppressWarnings("deprecation")
    private void removeOwner() {
        // Undo our policies first so nothing is left behind on the phone.
        dpm.setCameraDisabled(admin, false);
        dpm.setScreenCaptureDisabled(admin, false);
        dpm.setDeviceOwnerLockScreenInfo(admin, null);
        dpm.clearDeviceOwnerApp(getPackageName());
        lockInfo.setText(null);
        toast(R.string.remove_owner_done);
        refresh();
    }

    private String versionName() {
        try {
            String name = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return TextUtils.isEmpty(name) ? "?" : name;
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }

    private void toast(int message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
