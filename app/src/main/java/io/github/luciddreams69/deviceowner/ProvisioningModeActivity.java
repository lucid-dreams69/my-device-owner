package io.github.luciddreams69.deviceowner;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

import java.util.ArrayList;

/**
 * Called by the setup wizard after it downloads this app from the QR code.
 * We answer "fully managed device", i.e. make this app the device owner.
 */
public class ProvisioningModeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent request = getIntent();
        PersistableBundle extras = ProvisioningLog.adminExtras(request);
        ProvisioningLog.add(this, "GET_PROVISIONING_MODE", extras);

        ArrayList<Integer> allowed = request.getIntegerArrayListExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES);
        if (allowed != null
                && !allowed.contains(DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE)) {
            // This app only supports device-owner mode, not work profiles.
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        Intent result = new Intent().putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE,
                DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE);
        if (extras != null) {
            result.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, extras);
        }
        setResult(RESULT_OK, result);
        finish();
    }
}
