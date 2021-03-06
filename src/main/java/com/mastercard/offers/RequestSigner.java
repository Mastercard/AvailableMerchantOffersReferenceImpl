package com.mastercard.offers;

import com.mastercard.developer.interceptors.OkHttp2OAuth1Interceptor;
import com.mastercard.developer.utils.AuthenticationUtils;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.openapitools.client.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.List;

public class RequestSigner {

    private static final Logger log = LoggerFactory.getLogger(RequestSigner.class);

    /**
     * Intercepts and signs the request. Using Stage GW for testing as SBX is not viable for this Service
     * @param client
     * @throws IOException
     * @throws NoSuchProviderException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     */
    public static void signRequest(ApiClient client, ApplicationProps props) throws IOException, NoSuchProviderException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        log.info("Props="+props);

        String consumerKey = props.getProperty("consumerKey");
        String signingKeyFilePath = props.getP12FilePath("signingKeyFilePath");
        log.info("signingKeyFilePath="+signingKeyFilePath);

        String signingKeyAlias = props.getProperty("keyalias");
        String signingKeyPassword = props.getProperty("keystorepassword");
        PrivateKey signingKey = AuthenticationUtils.loadSigningKey(signingKeyFilePath, signingKeyAlias, signingKeyPassword);

        client.setBasePath(props.getProperty("basepath"));
        client.setDebugging(Boolean.valueOf(props.getProperty("debug")));
        client.setReadTimeout(Integer.parseInt(props.getProperty("readtimeout")));

        List<Interceptor> interceptors = client.getHttpClient().networkInterceptors();
        interceptors.add(new ForceJsonResponseInterceptor());
        interceptors.add(new OkHttp2OAuth1Interceptor(consumerKey, signingKey));
    }

    /**Add "Format=JSON" to the request for the service/gateway to return a JSON response.*/
    private static class ForceJsonResponseInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            String withJsonFormatUrl = withJsonFormat(originalRequest.uri().toString());
            Request newRequest = originalRequest.newBuilder().url(withJsonFormatUrl).build();
            return chain.proceed(newRequest);
        }

        private String withJsonFormat(String uri) {
            StringBuilder newUri = new StringBuilder(uri);
            newUri.append(uri.contains("?") ? "&" : "?");
            newUri.append("Format=JSON");
            return newUri.toString();
        }
    }
}
