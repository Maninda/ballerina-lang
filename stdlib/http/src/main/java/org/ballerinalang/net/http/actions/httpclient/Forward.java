/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.net.http.actions.httpclient;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.connector.TempCallableUnitCallback;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.net.http.DataContext;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import java.util.Locale;

import static org.ballerinalang.net.http.HttpConstants.CLIENT_ENDPOINT_SERVICE_URI;
import static org.ballerinalang.net.http.HttpUtil.checkRequestBodySizeHeadersAvailability;

/**
 * {@code Forward} action can be used to invoke an http call with incoming request httpVerb.
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "http",
        functionName = "nativeForward"
)
public class Forward extends AbstractHTTPAction {

    @Override
    public void execute(Context context, CallableUnitCallback callback) {
//        DataContext dataContext = new DataContext(context, callback, createOutboundRequestMsg(context));
//        // Execute the operation
//        executeNonBlockingAction(dataContext, false);
    }

    public static void nativeForward(Strand strand, ObjectValue clientObj, String url, MapValue config, String path, ObjectValue requestObj) {
        //TODO : TempCallableUnitCallback is temporary fix to handle non blocking call
        TempCallableUnitCallback callback = new TempCallableUnitCallback();

        String serviceUri = clientObj.get(CLIENT_ENDPOINT_SERVICE_URI).toString();
        HttpCarbonMessage outboundRequestMsg = createOutboundRequestMsg(serviceUri, path, requestObj);
        DataContext dataContext = new DataContext(strand, false, callback, clientObj, requestObj, outboundRequestMsg);
        // Execute the operation
        executeNonBlockingAction(dataContext, false);
    }

    protected static HttpCarbonMessage createOutboundRequestMsg(String serviceUri, String path,
                                                                ObjectValue requestObj) {
        if (requestObj.getNativeData(HttpConstants.REQUEST) == null &&
                !HttpUtil.isEntityDataSourceAvailable(requestObj)) {
            throw new BallerinaException("invalid inbound request parameter");
        }
        HttpCarbonMessage outboundRequestMsg = HttpUtil
                .getCarbonMsg(requestObj, HttpUtil.createHttpCarbonMessage(true));

        if (HttpUtil.isEntityDataSourceAvailable(requestObj)) {
            HttpUtil.enrichOutboundMessage(outboundRequestMsg, requestObj);
            prepareOutboundRequest(serviceUri, path, outboundRequestMsg,
                                   !checkRequestBodySizeHeadersAvailability(outboundRequestMsg));
            outboundRequestMsg.setProperty(HttpConstants.HTTP_METHOD, requestObj.get(HttpConstants.HTTP_REQUEST_METHOD).toString());
        } else {
            prepareOutboundRequest(serviceUri, path, outboundRequestMsg,
                                   !checkRequestBodySizeHeadersAvailability(outboundRequestMsg));
            String httpVerb = (String) outboundRequestMsg.getProperty(HttpConstants.HTTP_METHOD);
            outboundRequestMsg.setProperty(HttpConstants.HTTP_METHOD, httpVerb.trim().toUpperCase(Locale.getDefault()));
        }
        return outboundRequestMsg;
    }
}