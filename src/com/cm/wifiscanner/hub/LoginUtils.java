
package com.cm.wifiscanner.hub;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.cm.wifiscanner.legacy.PrefsHelper;

public class LoginUtils {

    private static final String TAG = "LoginUtil";

    private static final String CHALLENGE = "GET / HTTP/1.0\nUser-Agent: Wget/1.12(cygwin)\r\nAccept: */*\r\nHost: 1.1.1.1\r\nConnection: Close\r\n\r\n";
    private static final String LOGIN_FMT = "GET http://%s:%d/logon?username=%s&response=%s&userurl=%s HTTP/1.0\r\nUser-Agent: Wget/1.12(cygwin)\r\nAccept: */*\r\nHost: %s:%d\r\nConnection: Close\r\n\r\n";
    private static final String LOGOUT_FMT = "GET /logoff HTTP/1.0\nUser-Agent: Wget/1.12(cygwin)\r\nAccept: */*\r\nHost: %s\r\nConnection: Close\r\n\r\n";
    private static final String PRELOGIN = "GET /prelogin HTTP/1.0";

    private static final int CACHE_LEN = 4096;
    private final byte[] CACHE = new byte[CACHE_LEN];
    private String mServer;
    private int mPort;
    private Context mContext;
    private static LoginUtils sInstance;

    private LoginUtils(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized LoginUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LoginUtils(context);
        }

        return sInstance;
    }

    private String talkWithServer(String host, int port, String content) {
        try {
            Socket socket = new Socket(host, port);
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            int contentLen = content.length();
            content.getBytes(0, contentLen, CACHE, 0);
            os.write(CACHE, 0, contentLen);

            int recv = 0, received = 0;
            recv = is.read(CACHE, received, CACHE_LEN);
            if (recv != -1) {
                CACHE[recv] = 0;
            }

            socket.close();
            if (recv == -1) {
                return null;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return new String(CACHE);
    }

    private String getChallenage() {
        String result = talkWithServer("1.1.1.1", 80, CHALLENGE);
        result = URLDecoder.decode(result);
        if (TextUtils.isEmpty(result)) {
            return null;
        }

        final String LOCATION = "Location:";
        int index = result.indexOf(LOCATION);
        if (index < 0) {
            return null;
        }

        int startIndex = index + LOCATION.length();
        int endIndex = startIndex + 5;

        char[] buf = result.toCharArray();
        while (buf[startIndex] == ' ') {
            startIndex++;
            endIndex++;
        }

        String subString = result.substring(startIndex, endIndex);
        if (subString.compareToIgnoreCase("http:") == 0) {
            int serverStart = endIndex;
            int portStart = result.indexOf(":", endIndex + 1);
            int portEnd = result.indexOf("/", portStart + 1);
            if (portStart < 0 || portEnd < 0) {
                Log.e(TAG, "portStart < 0 || portEnd < 0");
                return null;
            }

            while (buf[serverStart] == ' ') {
                serverStart++;
            }

            while (buf[serverStart] == '/') {
                serverStart++;
            }

            mServer = result.substring(serverStart, portStart);

            while (buf[portStart] == ':') {
                portStart++;
            }
            String port = result.substring(portStart, portEnd);

            mPort = Integer.parseInt(port);
        } else {
            return null;
        }

        final String CHALLENGE = "challenge";
        index = result.indexOf(CHALLENGE);
        index += CHALLENGE.length();
        endIndex = result.indexOf("\n", index + 1);
        if (endIndex < 0 || index < 0) {
            Log.e(TAG, "challenge:endIndex < 0 || index < 0");
            return null;
        }

        int start = index;
        while (buf[start] == '=') {
            start++;
        }

        String challenge = result.substring(start, start + 32);
        challenge = challenge.toUpperCase(Locale.getDefault());

        PrefsHelper.getInstance(mContext).saveServer(mServer);
        PrefsHelper.getInstance(mContext).savePort(mPort);

        return challenge;
    }

    private byte[] ASC2Hex(final char[] str) {
        int nstrlen = str.length;
        int nlen = 0;
        if (nstrlen % 2 != 0) {
            nlen = -1;
            return null;
        }

        nlen = nstrlen / 2;

        byte[] Hex = new byte[nlen];
        for (int i = 0; i < nlen; i++) {
            int pos = i * 2;

            int value = (int)(charToByte(str[pos]) << 4 | charToByte(str[pos + 1]));
            Hex[i] = (byte)(value & 0xFF);
        }

        return Hex;
    }

    private byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    private String getNewChallenage(String nameSecret, String challenge) {
        challenge = challenge.toUpperCase();
        byte[] src = ASC2Hex(challenge.toCharArray());

        byte[] newBytes = new byte[256];
        System.arraycopy(src, 0, newBytes, 0, 16);

        byte[] nameBytes = nameSecret.getBytes();
        System.arraycopy(nameBytes, 0, newBytes, 16, nameBytes.length);

        return toMd5(newBytes, 16 + nameBytes.length);
    }

    private String getResponse(String pwd, String newChallenge) {
        newChallenge = newChallenge.toUpperCase();
        byte[] src = ASC2Hex(newChallenge.toCharArray());

        byte[] newBytes = new byte[256];
        newBytes[0] = 0;

        byte[] pwdBytes = pwd.getBytes();
        System.arraycopy(pwdBytes, 0, newBytes, 1, pwdBytes.length);

        System.arraycopy(src, 0, newBytes, pwdBytes.length + 1, 16);
        return toMd5(newBytes, 16 + pwdBytes.length + 1);
    }

    private String toMd5(byte[] bytes, int len) {
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(bytes, 0, len);

            byte[] result = algorithm.digest();
            StringBuilder hexString = new StringBuilder();

            for (byte b : result) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    hex = '0' + hex;
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean loginHub(String name, String pwd, String url) {
        String content, newChanllenge, response;
        String challenge = getChallenage();

        if (TextUtils.isEmpty(challenge)) {
            return false;
        }

        newChanllenge = getNewChallenage("591wificom", challenge);
        response = getResponse(pwd, newChanllenge);
        response = response.toLowerCase();

        content = String.format(Locale.getDefault(), LOGIN_FMT, mServer, mPort, name, response,
                url, mServer, mPort);

        talkWithServer(mServer, mPort, content);
        talkWithServer(mServer, mPort, PRELOGIN);

        return true;
    }

    public void logoutHub() {
        String content = String.format(LOGOUT_FMT, mServer);
        String server = PrefsHelper.getInstance(mContext).getServer(mServer);
        int port = PrefsHelper.getInstance(mContext).getPort(mPort);

        talkWithServer(server, port, content);
    }
}
