/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.net.http.nativeimpl.request;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.uri.URIUtil;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

/**
 * Get the Query params from HTTP message and return a map.
 *
 * @since 0.961.0
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "http",
        functionName = "getMatrixParams",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "Request",
                             structPackage = "ballerina/http"),
        args = {@Argument(name = "path", type = TypeKind.STRING)},
        returnType = {@ReturnType(type = TypeKind.MAP, elementType = TypeKind.STRING)},
        isPublic = true
)
public class GetMatrixParams extends BlockingNativeCallableUnit {
    @Override
    public void execute(Context context) {
//        BMap<String, BValue> requestStruct  = ((BMap<String, BValue>) context.getRefArgument(0));
//        String path = context.getStringArgument(0);
//        HttpCarbonMessage httpCarbonMessage = HttpUtil.getCarbonMsg(requestStruct, null);
//        context.setReturnValues(URIUtil.getMatrixParamsMap(path, httpCarbonMessage));
    }

    public static MapValue<String, Object> getMatrixParams(Strand strand, ObjectValue requestObj, String path) {
        HttpCarbonMessage httpCarbonMessage = HttpUtil.getCarbonMsg(requestObj, null);
        return URIUtil.getMatrixParamsMap(path, httpCarbonMessage);
    }
}