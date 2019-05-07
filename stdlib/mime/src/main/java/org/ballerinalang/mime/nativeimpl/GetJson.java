/*
*  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/

package org.ballerinalang.mime.nativeimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.jvm.JSONParser;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.TypeChecker;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.RefValue;
import org.ballerinalang.jvm.values.connector.TempCallableUnitCallback;
import org.ballerinalang.mime.util.EntityBodyHandler;
import org.ballerinalang.mime.util.MimeUtil;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.wso2.ballerinalang.compiler.util.TypeTags;

import static org.ballerinalang.mime.util.EntityBodyHandler.isStreamingRequired;

/**
 * Get the entity body in JSON form.
 *
 * @since 0.963.0
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "mime",
        functionName = "getJson",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "Entity", structPackage = "ballerina/mime"),
        returnType = {@ReturnType(type = TypeKind.JSON), @ReturnType(type = TypeKind.RECORD)},
        isPublic = true
)
public class GetJson extends AbstractGetPayloadHandler {

    @Override
    @SuppressWarnings("unchecked")
    public void execute(Context context, CallableUnitCallback callback) {
//        try {
//            BRefType<?> result;
//            BMap<String, BValue> entity = (BMap<String, BValue>) context.getRefArgument(FIRST_PARAMETER_INDEX);
//            BValue dataSource = EntityBodyHandler.getMessageDataSource(entity);
//            if (dataSource != null) {
//                // If the value is already a JSON, then return as it is.
//                if (isJSON(dataSource)) {
//                    result = (BRefType<?>) dataSource;
//                } else {
//                    // Else, build the JSON from the string representation of the payload.
//                    BString payload = MimeUtil.getMessageAsString(dataSource);
//                    result = JsonParser.parse(payload.stringValue());
//                }
//                setReturnValuesAndNotify(context, callback, result);
//                return;
//            }
//
//            if (isStreamingRequired(entity)) {
//                result = EntityBodyHandler.constructJsonDataSource(entity);
//                updateDataSourceAndNotify(context, callback, entity, result);
//            } else {
//                constructNonBlockingDataSource(context, callback, entity, SourceType.JSON);
//            }
//        } catch (Exception ex) {
//            createErrorAndNotify(context, callback,
//                                 "Error occurred while extracting json data from entity: " + ex.getMessage());
//        }
    }

    public static void getJson(Strand strand, ObjectValue entityObj) {
        //TODO : TempCallableUnitCallback is temporary fix to handle non blocking call
        TempCallableUnitCallback callback = new TempCallableUnitCallback();

        try {
            RefValue result;
            Object dataSource = EntityBodyHandler.getMessageDataSource(entityObj);
            if (dataSource != null) {
                // If the value is already a JSON, then return as it is.
                if (isJSON(dataSource)) {
                    result = (RefValue) dataSource;
                } else {
                    // Else, build the JSON from the string representation of the payload.
                    String payload = MimeUtil.getMessageAsString(dataSource);
                    result = (RefValue) JSONParser.parse(payload);
                }
                setReturnValuesAndNotify(strand, callback, result);
                return;
            }

            if (isStreamingRequired(entityObj)) {
                result = (RefValue) EntityBodyHandler.constructJsonDataSource(entityObj);
                updateDataSourceAndNotify(strand, callback, entityObj, result);
            } else {
                constructNonBlockingDataSource(strand, callback, entityObj, SourceType.JSON);
            }
        } catch (Exception ex) {
            createErrorAndNotify(strand, callback,
                                 "Error occurred while extracting json data from entity: " + ex.getMessage());
        }
    }

    private static boolean isJSON(Object value) {
        // If the value is string, it could represent any type of payload.
        // Therefore it needs to be parsed as JSON.
        return TypeChecker.getType(value).getTag() != TypeTags.STRING && MimeUtil.isJSONCompatible(
                TypeChecker.getType(value));
    }
}