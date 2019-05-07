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

package org.ballerinalang.net.http.actions.websocketconnector;

import io.netty.channel.ChannelFuture;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.connector.TempCallableUnitCallback;
import org.ballerinalang.model.NativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.http.WebSocketConstants;
import org.ballerinalang.net.http.WebSocketOpenConnectionInfo;
import org.ballerinalang.net.http.WebSocketUtil;
import org.wso2.transport.http.netty.contract.websocket.WebSocketConnection;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * {@code Get} is the GET action implementation of the HTTP Connector.
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "http",
        functionName = "externClose",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = WebSocketConstants.WEBSOCKET_CONNECTOR,
                             structPackage = "ballerina/http"),
        args = {
                @Argument(name = "statusCode", type = TypeKind.INT),
                @Argument(name = "reason", type = TypeKind.STRING),
                @Argument(name = "timeoutInSecs", type = TypeKind.INT)
        }
)
public class Close implements NativeCallableUnit {

    @Override
    public void execute(Context context, CallableUnitCallback callback) {
//        try {
//            BMap<String, BValue> webSocketConnector = (BMap<String, BValue>) context.getRefArgument(0);
//            int statusCode = (int) context.getIntArgument(0);
//            String reason = context.getStringArgument(0);
//            int timeoutInSecs = (int) context.getIntArgument(1);
//            WebSocketOpenConnectionInfo connectionInfo = (WebSocketOpenConnectionInfo) webSocketConnector
//                    .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
//            CountDownLatch countDownLatch = new CountDownLatch(1);
//            ChannelFuture closeFuture =
//                    initiateConnectionClosure(context, statusCode, reason, connectionInfo, countDownLatch);
//            waitForTimeout(context, timeoutInSecs, countDownLatch);
//            closeFuture.channel().close().addListener(future -> {
//                WebSocketUtil.setListenerOpenField(connectionInfo);
//                callback.notifySuccess();
//            });
//        } catch (Exception e) {
//            context.setReturnValues(HttpUtil.getError(context, e));
//            callback.notifySuccess();
//        }
    }

    public static void externClose(Strand strand, ObjectValue wsConnection, int statusCode, String reason,
                                   int timeoutInSecs) {
        //TODO : TempCallableUnitCallback is temporary fix to handle non blocking call
        TempCallableUnitCallback callback = new TempCallableUnitCallback();
        try {
            strand.block();
            WebSocketOpenConnectionInfo connectionInfo = (WebSocketOpenConnectionInfo) wsConnection
                    .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
            CountDownLatch countDownLatch = new CountDownLatch(1);
            ChannelFuture closeFuture =
                    initiateConnectionClosure(strand, callback, statusCode, reason, connectionInfo, countDownLatch);
            waitForTimeout(strand, callback, timeoutInSecs, countDownLatch);
            closeFuture.channel().close().addListener(future -> {
                WebSocketUtil.setListenerOpenField(connectionInfo);
                callback.notifySuccess();
            });
        } catch (Exception e) {
            strand.setReturnValues(HttpUtil.getError(e.getMessage()));
            strand.resume();
            //TODO remove this call back
            callback.setReturnValues(HttpUtil.getError(e.getMessage()));
            callback.notifySuccess();
        }
    }

    private static ChannelFuture initiateConnectionClosure(Strand strand,
                                                           TempCallableUnitCallback callback,
                                                           int statusCode, String reason,
                                                           WebSocketOpenConnectionInfo connectionInfo,
                                                           CountDownLatch latch)
            throws IllegalAccessException {
        WebSocketConnection webSocketConnection = connectionInfo.getWebSocketConnection();
        ChannelFuture closeFuture;
        if (statusCode < 0) {
            closeFuture = webSocketConnection.initiateConnectionClosure();
        } else {
            closeFuture = webSocketConnection.initiateConnectionClosure(statusCode, reason);
        }
        return closeFuture.addListener(future -> {
            Throwable cause = future.cause();
            if (!future.isSuccess() && cause != null) {
                strand.setReturnValues(HttpUtil.getError(cause));
                //TODO remove this call back
                callback.setReturnValues(HttpUtil.getError(cause));
            } else {
                strand.setReturnValues(null);
                //TODO remove this call back
                callback.setReturnValues(null);
            }
            latch.countDown();
        });
    }

    private static void waitForTimeout(Strand strand, TempCallableUnitCallback callback, int timeoutInSecs,
                                       CountDownLatch latch) {
        try {
            if (timeoutInSecs < 0) {
                latch.await();
            } else {
                boolean countDownReached = latch.await(timeoutInSecs, TimeUnit.SECONDS);
                if (!countDownReached) {
                    String errMsg = String.format(
                            "Could not receive a WebSocket close frame from remote endpoint within %d seconds",
                            timeoutInSecs);
                    strand.setReturnValues(HttpUtil.getError(errMsg));
                    //TODO remove this call back
                    callback.setReturnValues(HttpUtil.getError(errMsg));
                }
            }
        } catch (InterruptedException err) {
            strand.setReturnValues(HttpUtil.getError("Connection interrupted while closing the connection"));
            //TODO remove this call back
            callback.setReturnValues(HttpUtil.getError("Connection interrupted while closing the connection"));
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isBlocking() {
        return false;
    }
}