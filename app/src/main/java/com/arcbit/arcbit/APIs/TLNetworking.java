package com.arcbit.arcbit.APIs;

import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import info.guardianproject.netcipher.NetCipher;

public class TLNetworking {
    private static final String TAG = TLNetworking.class.getName();

    public static Integer HTTP_LOCAL_ERROR_CODE = 999;
    public static String HTTP_LOCAL_ERROR_MSG = "HTTPLocalErrorMsg";
    public static String HTTP_ERROR_CODE = "HTTPErrorCode";
    public static String HTTP_ERROR_MSG = "HTTPErrorMsg";
    public static String HTTP_MSG = "Msg";

    public TLNetworking() {
    }

    public HttpURLConnection getHttpURLConnection(String URL) throws Exception {
        return NetCipher.getHttpsURLConnection(URL);
        //return (HttpURLConnection)new URL(URL).openConnection(); // can use this if we stop supporting  API <= 19
    }

    public JSONObject postURL(String request, String urlParameters, int requestRetry) throws Exception {
        return this.postURLCall(request, urlParameters, requestRetry, "application/x-www-form-urlencoded");
    }

    public JSONObject postURL(String request, String urlParameters) throws Exception {
        return this.postURLCall(request, urlParameters, 2, "application/x-www-form-urlencoded");
    }

    public JSONObject postURLNotJSON(String request, String urlParameters) throws Exception {
        return this.postURLCallNotJSON(request, urlParameters, 2, "application/x-www-form-urlencoded");
    }

    public JSONObject postURLJson(String request, String urlParameters) throws Exception {
        return this.postURLCall(request, urlParameters, 2, "application/json");
    }

    private JSONObject postURLCallNotJSON(String urlString, String urlParameters, int requestRetry, String contentType) throws Exception {
        Log.d(TAG, "TLNetworking postURLCall " + urlString);
        String error = null;
        int errorCode = 0;

        for(int ii = 0; ii < requestRetry; ++ii) {
            HttpURLConnection connection = getHttpURLConnection(urlString);

            try {
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", contentType);
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");
                connection.setUseCaches(false);
                connection.setConnectTimeout('\uea60');
                connection.setReadTimeout('\uea60');
                connection.connect();
                DataOutputStream e = new DataOutputStream(connection.getOutputStream());
                e.writeBytes(urlParameters);
                e.flush();
                e.close();
                connection.setInstanceFollowRedirects(false);
                if(connection.getResponseCode() == 200) {
                    String var10 = IOUtils.toString(connection.getInputStream(), "UTF-8");
                    JSONObject obj = new JSONObject();
                    obj.put(HTTP_MSG, var10);
                    return obj;
                }

                error = IOUtils.toString(connection.getErrorStream(), "UTF-8");
                errorCode = connection.getResponseCode();
                Thread.sleep(5000L);
            } catch (Exception var14) {
                throw new Exception("Network error" + var14.getMessage());
            } finally {
                connection.disconnect();
            }
        }

        JSONObject obj = new JSONObject();
        obj.put(HTTP_ERROR_CODE, errorCode);
        obj.put(HTTP_ERROR_MSG, error);
        return obj;
    }

    private JSONObject postURLCall(String urlString, String urlParameters, int requestRetry, String contentType) throws Exception {
        String error = null;
        int errorCode = 0;

        for(int ii = 0; ii < requestRetry; ++ii) {
            HttpURLConnection connection = getHttpURLConnection(urlString);

            try {
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", contentType);
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");
                connection.setUseCaches(false);
                connection.setConnectTimeout('\uea60');
                connection.setReadTimeout('\uea60');
                connection.connect();
                DataOutputStream e = new DataOutputStream(connection.getOutputStream());
                e.writeBytes(urlParameters);
                e.flush();
                e.close();
                connection.setInstanceFollowRedirects(false);
                if(connection.getResponseCode() == 200) {
                    String var10 = IOUtils.toString(connection.getInputStream(), "UTF-8");
                    return new JSONObject(var10);
                }

                error = IOUtils.toString(connection.getErrorStream(), "UTF-8");
                errorCode = connection.getResponseCode();
                Thread.sleep(5000L);
            } catch (Exception var14) {
                throw new Exception("Network error" + var14.getMessage());
            } finally {
                connection.disconnect();
            }
        }

        JSONObject obj = new JSONObject();
        obj.put(HTTP_ERROR_CODE, errorCode);
        obj.put(HTTP_ERROR_MSG, error);
        return obj;
    }

    public JSONObject getURL(String URL, String cookie) throws Exception {
        return this.getURLCall(URL, cookie);
    }

    public JSONObject getURL(String URL) throws Exception {
        return this.getURLCall(URL, (String)null);
    }

    private JSONObject getURLCall(String urlString, String cookie) throws Exception {
        Log.d(TAG, "TLNetworking getURLCall " + urlString);
        String error = null;
        int errorCode = 0;

        for(int ii = 0; ii < 2; ++ii) {
            HttpURLConnection connection = getHttpURLConnection(urlString);

            try {
                connection.setRequestMethod("GET");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");
                connection.setConnectTimeout('\uea60');
                connection.setReadTimeout('\uea60');
                if(cookie != null) {
                    connection.setRequestProperty("cookie", cookie);
                }

                connection.setInstanceFollowRedirects(false);
                connection.connect();
                if(connection.getResponseCode() == 200) {
                    String e = IOUtils.toString(connection.getInputStream(), "UTF-8");
                    return new JSONObject(e);
                }

                error = IOUtils.toString(connection.getErrorStream(), "UTF-8");
                errorCode = connection.getResponseCode();
                Thread.sleep(5000L);
            } catch (Exception var11) {
                throw new Exception("Network error" + var11.getMessage());
            } finally {
                connection.disconnect();
            }
        }

        JSONObject obj = new JSONObject();
        obj.put(HTTP_ERROR_CODE, errorCode);
        obj.put(HTTP_ERROR_MSG, error);
        return obj;
    }

    public Object getURLNotJSON(String URL) throws Exception {
        return this.getURLCallNotJSON(URL, (String)null);
    }

    private Object getURLCallNotJSON(String urlString, String cookie) throws Exception {
        Log.d(TAG, "TLNetworking getURLCallNotJSON " + urlString);
        String error = null;
        int errorCode = 0;

        for(int ii = 0; ii < 2; ++ii) {
            HttpURLConnection connection = getHttpURLConnection(urlString);

            try {
                connection.setRequestMethod("GET");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");
                connection.setConnectTimeout('\uea60');
                connection.setReadTimeout('\uea60');
                if(cookie != null) {
                    connection.setRequestProperty("cookie", cookie);
                }

                connection.setInstanceFollowRedirects(false);
                connection.connect();
                if(connection.getResponseCode() == 200) {
                    String e = IOUtils.toString(connection.getInputStream(), "UTF-8");
                    return e;
                }

                error = IOUtils.toString(connection.getErrorStream(), "UTF-8");
                errorCode = connection.getResponseCode();
                Thread.sleep(5000L);
            } catch (Exception var11) {
                throw new Exception("Network error" + var11.getMessage());
            } finally {
                connection.disconnect();
            }
        }

        JSONObject obj = new JSONObject();
        obj.put(HTTP_ERROR_CODE, errorCode);
        obj.put(HTTP_ERROR_MSG, error);
        return obj;
    }
}
