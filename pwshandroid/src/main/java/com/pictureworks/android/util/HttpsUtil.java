package com.pictureworks.android.util;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;


/**
 * 连接方式
 */
public class HttpsUtil {
    private static AsyncHttpClient client;

    public static void init(Context context) {
        if (client == null) {
            synchronized (client) {
                if (client == null) {
                    client = new AsyncHttpClient();
                    client.setTimeout(30000); // 设置链接超时，如果不设置，默认为10s
                    client.setMaxConnections(10);//设置最大的链接数量

                    InputStream ins = null;
                    try {
                        ins = context.getAssets().open("192.168.8.3.cer");
                        CertificateFactory cerFactory = CertificateFactory
                                .getInstance("X.509");
                        Certificate cer = cerFactory.generateCertificate(ins);
                        KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
                        keyStore.load(null, null);
                        keyStore.setCertificateEntry("trust", cer);

                        SSLSocketFactory socketFactory = new SSLSocketFactory(keyStore);
                        Scheme sch = new Scheme("https", socketFactory, 443);
                        client.getHttpClient().getConnectionManager().getSchemeRegistry().register(sch);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            ins.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public static void post(String urlString, RequestParams params, JsonHttpResponseHandler res) {
        client.post(urlString, params, res);
    }

    public static void post(String urlString, JsonHttpResponseHandler res) {
        client.post(urlString, res);
    }

    public static void get(String urlString, RequestParams params, JsonHttpResponseHandler res) // 带参数，获取json对象或者数组
    {
        client.get(urlString, params, res);
    }

    public static void get(String urlString, BinaryHttpResponseHandler bHandler) // 下载数据使用，会返回byte数据
    {
        client.get(urlString, bHandler);
    }

    public static void get(String urlString, RequestParams params, BinaryHttpResponseHandler bHandler) {//用户下载照片时使用
        client.get(urlString, params, bHandler);
    }

    public static void destoryallrequests(boolean arg) {
        client.cancelAllRequests(arg);
    }

}
