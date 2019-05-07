/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.net.http.nativeimpl.connection;

import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.connector.TempCallableUnitCallback;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.http.DataContext;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.http.caching.ResponseCacheControlObj;
import org.ballerinalang.net.http.nativeimpl.pipelining.PipelinedResponse;
import org.ballerinalang.net.http.util.CacheUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import static org.ballerinalang.net.http.HttpConstants.HTTP_STATUS_CODE;
import static org.ballerinalang.net.http.HttpConstants.RESPONSE_CACHE_CONTROL_FIELD;
import static org.ballerinalang.net.http.nativeimpl.pipelining.PipeliningHandler.executePipeliningLogic;
import static org.ballerinalang.net.http.nativeimpl.pipelining.PipeliningHandler.pipeliningRequired;
import static org.ballerinalang.net.http.nativeimpl.pipelining.PipeliningHandler.setPipeliningListener;

/**
 * Extern function to respond back the caller with outbound response.
 *
 * @since 0.96
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "http",
        functionName = "nativeRespond",
        args = {@Argument(name = "connection", type = TypeKind.OBJECT),
                @Argument(name = "res", type = TypeKind.OBJECT, structType = "Response",
                        structPackage = "ballerina/http")},
        returnType = @ReturnType(type = TypeKind.RECORD, structType = "HttpConnectorError",
                structPackage = "ballerina/http"),
        isPublic = true
)
public class Respond extends ConnectionAction {

    private static final Logger log = LoggerFactory.getLogger(Respond.class);

    @Override
    public void execute(Context context, CallableUnitCallback callback) {
//        BMap<String, BValue> connectionStruct = (BMap<String, BValue>) context.getRefArgument(0);
//        HttpCarbonMessage inboundRequestMsg = HttpUtil.getCarbonMsg(connectionStruct, null);
//        DataContext dataContext = new DataContext(context, callback, inboundRequestMsg);
//        BMap<String, BValue> outboundResponseStruct = (BMap<String, BValue>) context.getRefArgument(1);
//        HttpCarbonMessage outboundResponseMsg = HttpUtil
//                .getCarbonMsg(outboundResponseStruct, HttpUtil.createHttpCarbonMessage(false));
//        outboundResponseMsg.setPipeliningEnabled(inboundRequestMsg.isPipeliningEnabled());
//        outboundResponseMsg.setSequenceId(inboundRequestMsg.getSequenceId());
//        setCacheControlHeader(outboundResponseStruct, outboundResponseMsg);
//        HttpUtil.prepareOutboundResponse(context, inboundRequestMsg, outboundResponseMsg, outboundResponseStruct);
//        HttpUtil.checkFunctionValidity(connectionStruct, inboundRequestMsg, outboundResponseMsg);
//
//        // Based on https://tools.ietf.org/html/rfc7232#section-4.1
//        if (CacheUtils.isValidCachedResponse(outboundResponseMsg, inboundRequestMsg)) {
//            outboundResponseMsg.setProperty(HTTP_STATUS_CODE, HttpResponseStatus.NOT_MODIFIED.code());
//            outboundResponseMsg.removeHeader(HttpHeaderNames.CONTENT_LENGTH.toString());
//            outboundResponseMsg.removeHeader(HttpHeaderNames.CONTENT_TYPE.toString());
//            outboundResponseMsg.waitAndReleaseAllEntities();
//            outboundResponseMsg.completeMessage();
//        }
//
//        Optional<ObserverContext> observerContext = ObserveUtils.getObserverContextOfCurrentFrame(context);
//        observerContext.ifPresent(ctx -> ctx.addTag(TAG_KEY_HTTP_STATUS_CODE, String.valueOf
//                (outboundResponseStruct.get(RESPONSE_STATUS_CODE_FIELD))));
//        try {
//            if (pipeliningRequired(inboundRequestMsg)) {
//                if (log.isDebugEnabled()) {
//                    log.debug("Pipelining is required. Sequence id of the request: {}",
//                            inboundRequestMsg.getSequenceId());
//                }
//                PipelinedResponse pipelinedResponse = new PipelinedResponse(inboundRequestMsg, outboundResponseMsg,
//                        dataContext, outboundResponseStruct);
//                setPipeliningListener(outboundResponseMsg);
//                executePipeliningLogic(inboundRequestMsg.getSourceContext(), pipelinedResponse);
//            } else {
//                sendOutboundResponseRobust(dataContext, inboundRequestMsg, outboundResponseStruct, outboundResponseMsg);
//            }
//        } catch (EncoderException e) {
//            //Exception is already notified by http transport.
//            log.debug("Couldn't complete outbound response", e);
//        }
    }

    public static void nativeRespond(Strand strand, ObjectValue connectionObj, ObjectValue outboundResponseObj) {
        //TODO : TempCallableUnitCallback is temporary fix to handle non blocking call
        TempCallableUnitCallback callback = new TempCallableUnitCallback();

        HttpCarbonMessage inboundRequestMsg = HttpUtil.getCarbonMsg(connectionObj, null);
        DataContext dataContext = new DataContext(strand, false, callback, null, null, inboundRequestMsg);
        HttpCarbonMessage outboundResponseMsg = HttpUtil
                .getCarbonMsg(outboundResponseObj, HttpUtil.createHttpCarbonMessage(false));
        outboundResponseMsg.setPipeliningEnabled(inboundRequestMsg.isPipeliningEnabled());
        outboundResponseMsg.setSequenceId(inboundRequestMsg.getSequenceId());
        setCacheControlHeader(outboundResponseObj, outboundResponseMsg);
        HttpUtil.prepareOutboundResponse(connectionObj, inboundRequestMsg, outboundResponseMsg, outboundResponseObj);
        HttpUtil.checkFunctionValidity(connectionObj, inboundRequestMsg, outboundResponseMsg);

        // Based on https://tools.ietf.org/html/rfc7232#section-4.1
        if (CacheUtils.isValidCachedResponse(outboundResponseMsg, inboundRequestMsg)) {
            outboundResponseMsg.setProperty(HTTP_STATUS_CODE, HttpResponseStatus.NOT_MODIFIED.code());
            outboundResponseMsg.removeHeader(HttpHeaderNames.CONTENT_LENGTH.toString());
            outboundResponseMsg.removeHeader(HttpHeaderNames.CONTENT_TYPE.toString());
            outboundResponseMsg.waitAndReleaseAllEntities();
            outboundResponseMsg.completeMessage();
        }

        //TODO Fix with Observability
//        Optional<ObserverContext> observerContext = ObserveUtils.getObserverContextOfCurrentFrame(strand);
//        observerContext.ifPresent(ctx -> ctx.addTag(TAG_KEY_HTTP_STATUS_CODE, String.valueOf
//                (outboundResponseObj.get(RESPONSE_STATUS_CODE_FIELD))));
        try {
            if (pipeliningRequired(inboundRequestMsg)) {
                if (log.isDebugEnabled()) {
                    log.debug("Pipelining is required. Sequence id of the request: {}",
                            inboundRequestMsg.getSequenceId());
                }
                PipelinedResponse pipelinedResponse = new PipelinedResponse(inboundRequestMsg, outboundResponseMsg,
                        dataContext, outboundResponseObj);
                setPipeliningListener(outboundResponseMsg);
                executePipeliningLogic(inboundRequestMsg.getSourceContext(), pipelinedResponse);
            } else {
                sendOutboundResponseRobust(dataContext, inboundRequestMsg, outboundResponseObj, outboundResponseMsg);
            }
            //TODO : Temporary block the call. Remove this callback once the support is available
            callback.sync();
        } catch (EncoderException e) {
            //Exception is already notified by http transport.
            log.debug("Couldn't complete outbound response", e);
        }
    }

    private static void setCacheControlHeader(ObjectValue outboundRespObj, HttpCarbonMessage outboundResponse) {
        ObjectValue cacheControl = (ObjectValue) outboundRespObj.get(RESPONSE_CACHE_CONTROL_FIELD);
        if (cacheControl != null &&
                outboundResponse.getHeader(HttpHeaderNames.CACHE_CONTROL.toString()) == null) {
            ResponseCacheControlObj respCC = new ResponseCacheControlObj(cacheControl);
            outboundResponse.setHeader(HttpHeaderNames.CACHE_CONTROL.toString(), respCC.buildCacheControlDirectives());
        }
    }
}