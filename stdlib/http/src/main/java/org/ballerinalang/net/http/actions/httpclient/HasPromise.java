/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.connector.TempCallableUnitCallback;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.wso2.transport.http.netty.contract.HttpClientConnector;
import org.wso2.transport.http.netty.contract.HttpClientConnectorListener;
import org.wso2.transport.http.netty.message.ResponseHandle;

/**
 * {@code HasPromise} action can be used to check whether a push promise is available.
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "http",
        functionName = "hasPromise",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = HttpConstants.HTTP_CALLER,
                structPackage = "ballerina/http")
)
public class HasPromise extends AbstractHTTPAction {

    @Override
    public void execute(Context context, CallableUnitCallback callback) {

//        BMap<String, BValue> handleStruct = ((BMap<String, BValue>) context.getRefArgument(1));
//
//        ResponseHandle responseHandle = (ResponseHandle) handleStruct.getNativeData(HttpConstants.TRANSPORT_HANDLE);
//        if (responseHandle == null) {
//            throw new BallerinaException("invalid http handle");
//        }
//        BMap<String, BValue> bConnector = (BMap<String, BValue>) context.getRefArgument(0);
//        HttpClientConnector clientConnector = (HttpClientConnector) ((BMap<String, BValue>) bConnector.values()[0])
//                .getNativeData(HttpConstants.HTTP_CLIENT);
//        clientConnector.hasPushPromise(responseHandle).
//                setPromiseAvailabilityListener(new PromiseAvailabilityCheckListener(context, callback));
    }

    public static void hasPromise(Strand strand, ObjectValue clientObj, ObjectValue handleObj) {
        //TODO : TempCallableUnitCallback is temporary fix to handle non blocking call
        TempCallableUnitCallback callback = new TempCallableUnitCallback();

        ResponseHandle responseHandle = (ResponseHandle) handleObj.getNativeData(HttpConstants.TRANSPORT_HANDLE);
        if (responseHandle == null) {
            throw new BallerinaException("invalid http handle");
        }
        HttpClientConnector clientConnector = (HttpClientConnector) clientObj.getNativeData(HttpConstants.HTTP_CLIENT);
        clientConnector.hasPushPromise(responseHandle).
                setPromiseAvailabilityListener(new PromiseAvailabilityCheckListener(strand, callback));
        //TODO This is temporary fix to handle non blocking call
        callback.sync();
    }

    private static class PromiseAvailabilityCheckListener implements HttpClientConnectorListener {

        private Strand strand;
        //TODO : TempCallableUnitCallback is temporary fix to handle non blocking call
        private TempCallableUnitCallback callback;

        PromiseAvailabilityCheckListener(Strand strand, TempCallableUnitCallback callback) {
            this.strand = strand;
            this.callback = callback;
        }

        @Override
        public void onPushPromiseAvailability(boolean isPromiseAvailable) {
            strand.setReturnValues(isPromiseAvailable);
            strand.resume();
            //TODO remove this call back
            callback.setReturnValues(isPromiseAvailable);
            callback.notifySuccess();
        }
    }
}