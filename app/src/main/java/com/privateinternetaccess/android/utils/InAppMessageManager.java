/*
 *  Copyright (c) 2020 Private Internet Access, Inc.
 *
 *  This file is part of the Private Internet Access Android Client.
 *
 *  The Private Internet Access Android Client is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  The Private Internet Access Android Client is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with the Private
 *  Internet Access Android Client.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.privateinternetaccess.android.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.privateinternetaccess.account.model.response.MessageInformation;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.model.events.SettingsUpdateEvent;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.drawer.AccountActivity;
import com.privateinternetaccess.android.ui.drawer.AllowedAppsActivity;
import com.privateinternetaccess.android.ui.drawer.DedicatedIPActivity;
import com.privateinternetaccess.android.ui.drawer.ServerListActivity;
import com.privateinternetaccess.android.ui.drawer.SettingsActivity;
import com.privateinternetaccess.android.ui.drawer.TrustedWifiActivity;
import com.privateinternetaccess.android.ui.drawer.settings.AboutActivity;
import com.privateinternetaccess.android.ui.features.WebviewActivity;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.privateinternetaccess.android.pia.handlers.PiaPrefHandler.GEO_SERVERS_ACTIVE;

public class InAppMessageManager {
    private static final String DEFAULT_LOCALE = "en-US";

    private static final String KEY_KILLSWITCH = "killswitch";
    private static final String KEY_NMT = "nmt";
    private static final String KEY_MACE = "mace";
    private static final String KEY_PERAPP = "perappsettings";
    private static final String KEY_PORT_FORWARD = "pf";
    private static final String KEY_GEO = "geo";
    private static final String KEY_URI = "uri";

    public static final String KEY_OVPN = "ovpn";
    public static final String KEY_WG = "wg";

    private static final String VIEW_SETTINGS = "settings";
    private static final String VIEW_ACCOUNT = "account";
    private static final String VIEW_REGIONS = "regions";
    private static final String VIEW_DIP = "dip";
    private static final String VIEW_ABOUT = "about";

    public static final String EXTRA_KEY = "key";

    private static MessageInformation activeMessage;
    private static String basicMessage;

    public static void setActiveMessage(MessageInformation message) {
        activeMessage = message;
    }

    @Nullable
    public static MessageInformation getActiveMessage(Context context) {
        if (activeMessage == null)
            return null;

        Set<String> dismissedIds = PiaPrefHandler.getDismissedIds(context);
        return !dismissedIds.contains(Long.toString(activeMessage.getId())) ? activeMessage : null;
    }

    public static void createMessage(Context context) {

    }

    public static void dismissMessage(Context context) {
        if (activeMessage != null && !TextUtils.isEmpty(Long.toString(activeMessage.getId()))) {
            PiaPrefHandler.addDismissedId(context, Long.toString(activeMessage.getId()));
        }
    }

    @Nullable
    public static SpannableStringBuilder showMessage(Context context, ClickableSpan clickable) {
        if (activeMessage == null || TextUtils.isEmpty(Long.toString(activeMessage.getId()))) {
            return null;
        }

        String localizedMessage = getStringForLocalization(activeMessage.getMessage());

        if (activeMessage.getLink() == null || activeMessage.getLink().getText() == null) {
            return new SpannableStringBuilder(localizedMessage);
        }

        String localizedLink = getStringForLocalization(activeMessage.getLink().getText());

        int indexOf = localizedMessage.indexOf(localizedLink);

        if (indexOf == -1) {
            return new SpannableStringBuilder(localizedMessage);
        }

        SpannableStringBuilder spanTxt = new SpannableStringBuilder(localizedMessage);
        spanTxt.setSpan(clickable, indexOf, indexOf + localizedLink.length(), 0);

        Set<String> dismissedIds = PiaPrefHandler.getDismissedIds(context);
        return !dismissedIds.contains(Long.toString(activeMessage.getId())) ? spanTxt : null;
    }

    public static void handleLink(Context context, MessageInformation message) {
        if (message.getLink().getAction() == null) {
            return;
        }

        Map<String, Boolean> settings = message.getLink().getAction().getSettings();
        String view = message.getLink().getAction().getView();
        String uri = message.getLink().getAction().getUri();

        if (settings != null && !settings.isEmpty()) {
            if (settings.containsKey(KEY_KILLSWITCH)) {
                toggleKillSwitch(context, settings.get(KEY_KILLSWITCH));
                settingsUpdatedMessage(context);
            }
            else if (settings.containsKey(KEY_NMT)) {
                toggleNmt(context, settings.get(KEY_NMT));
                settingsUpdatedMessage(context);
            }
            else if (settings.containsKey(KEY_MACE)) {
                Prefs.with(context).set(PiaPrefHandler.PIA_MACE, settings.get(KEY_MACE));
                settingsUpdatedMessage(context);
            }
            else if (settings.containsKey(KEY_PERAPP)) {
                Intent i = new Intent(context, AllowedAppsActivity.class);
                context.startActivity(i);
            }
            else if (settings.containsKey(KEY_PORT_FORWARD)) {
                PiaPrefHandler.setPortForwardingEnabled(context, settings.get(KEY_PORT_FORWARD));
                settingsUpdatedMessage(context);
            }
            else if (settings.containsKey(KEY_OVPN)) {
                Intent i = new Intent(context, SettingsActivity.class);
                i.putExtra(EXTRA_KEY, KEY_OVPN);
                context.startActivity(i);
            }
            else if (settings.containsKey(KEY_WG)) {
                Intent i = new Intent(context, SettingsActivity.class);
                i.putExtra(EXTRA_KEY, KEY_WG);
                context.startActivity(i);
            }
            else if (settings.containsKey(KEY_GEO)) {
                Prefs.with(context).set(GEO_SERVERS_ACTIVE, settings.get(KEY_GEO));
                settingsUpdatedMessage(context);
            }

        }
        else if (!TextUtils.isEmpty(uri)) {
            Intent i = new Intent(context, WebviewActivity.class);
            i.putExtra(WebviewActivity.EXTRA_URL, uri);
            context.startActivity(i);
        }
        else if (view != null && !view.isEmpty()) {
            if (view.equals(VIEW_SETTINGS)) {
                Intent i = new Intent(context, SettingsActivity.class);
                context.startActivity(i);
            }
            else if (view.equals(VIEW_REGIONS)) {
                Intent i = new Intent(context, ServerListActivity.class);
                context.startActivity(i);
            }
            else if (view.equals(VIEW_ACCOUNT)) {
                Intent i = new Intent(context, AccountActivity.class);
                context.startActivity(i);
            }
            else if (view.equals(VIEW_ABOUT)) {
                Intent i = new Intent(context, AboutActivity.class);
                context.startActivity(i);
            }
            else if (view.equals(VIEW_DIP)) {
                Intent i = new Intent(context, DedicatedIPActivity.class);
                context.startActivity(i);
            }
        }

        EventBus.getDefault().post(new SettingsUpdateEvent());
    }

    private static void settingsUpdatedMessage(Context context) {
        Toaster.l(context, R.string.settings_updated);
    }

    @Nullable
    private static String getStringForLocalization(Map<String, String> messages) {
        String localization = Locale.getDefault().toLanguageTag();

        if (messages.containsKey(localization)) {
            return messages.get(localization);
        }
        else if (messages.containsKey(DEFAULT_LOCALE)) {
            return messages.get(DEFAULT_LOCALE);
        }

        return null;
    }

    private static void toggleNmt(Context context, boolean enable) {
        if (!enable) {
            Prefs.with(context).set(PiaPrefHandler.NETWORK_MANAGEMENT, false);
        }
        else {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                Prefs.with(context).set(PiaPrefHandler.NETWORK_MANAGEMENT, true);
            }
            else {
                Intent i = new Intent(context, TrustedWifiActivity.class);
                context.startActivity(i);
            }
        }
    }

    private static void toggleKillSwitch(Context context, boolean enable) {
        if (!enable) {
            Prefs.with(context).set(PiaPrefHandler.KILLSWITCH, false);

            IVPN vpn = PIAFactory.getInstance().getVPN(context);
            if(vpn.isKillswitchActive()){
                vpn.stopKillswitch();
            }
        }
        else {
            Prefs.with(context).set(PiaPrefHandler.KILLSWITCH, true);
        }
    }
}
