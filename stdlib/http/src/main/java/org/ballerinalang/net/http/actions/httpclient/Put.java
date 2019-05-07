/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import static org.ballerinalang.net.http.HttpConstants.CLIENT_ENDPOINT_SERVICE_URI;


/**
 * {@code Put} is the PUT action implementation of the HTTP Connector.
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "http",
        functionName = "nativePut"
)
public class Put extends AbstractHTTPAction {

    @Override
    public void execute(Context context, CallableUnitCallback callback) {
//        DataContext dataContext = new DataContext(context, callback, createOutboundRequestMsg(context));
        // Execute the operation
//        executeNonBlockingAction(dataContext, false);
    }

    public static void nativePut(Strand strand, ObjectValue clientObj, String url, MapValue config, String path, ObjectValue requestObj) {
        //TODO : TempCallableUnitCallback is temporary fix to handle non blocking call
        TempCallableUnitCallback callback = new TempCallableUnitCallback();

        HttpCarbonMessage outboundRequestMsg = createOutboundRequestMsg(clientObj, path, requestObj);
        outboundRequestMsg.setProperty(HttpConstants.HTTP_METHOD, HttpConstants.HTTP_METHOD_PUT);
        DataContext dataContext = new DataContext(strand, false, callback, clientObj, requestObj, outboundRequestMsg);
        // Execute the operation
        executeNonBlockingAction(dataContext, false);
    }
}