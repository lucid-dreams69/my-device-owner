package io.github.luciddreams69.deviceowner;

import android.app.Activity;
import android.os.Bundle;

/**
 * Called once this app is device owner, before the setup wizard continues.
 * A real company app would apply its starting policies here; we only log it.
 */
public class PolicyComplianceActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ProvisioningLog.add(this, "ADMIN_POLICY_COMPLIANCE",
                ProvisioningLog.adminExtras(getIntent()));
        setResult(RESULT_OK);
        finish();
    }
}
