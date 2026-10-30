package io.github.luciddreams69.deviceowner;

import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/** The component Android makes "device owner". Its name goes into the QR code. */
public class AdminReceiver extends DeviceAdminReceiver {

    static ComponentName component(Context context) {
        return new ComponentName(context, AdminReceiver.class);
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        ProvisioningLog.add(context, "DEVICE_ADMIN_ENABLED", null);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        ProvisioningLog.add(context, "DEVICE_ADMIN_DISABLED", null);
    }

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        ProvisioningLog.add(context, "PROFILE_PROVISIONING_COMPLETE",
                ProvisioningLog.adminExtras(intent));
    }
}
