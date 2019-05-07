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
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.mime.util.EntityBodyHandler;
import org.ballerinalang.mime.util.MimeUtil;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;

/**
 * Set the entity body with JSON data.
 *
 * @since 0.963.0
 */
@BallerinaFunction(orgName = "ballerina", packageName = "mime",
        functionName = "setJson",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "Entity", structPackage = "ballerina/mime"),
        args = {@Argument(name = "jsonContent", type = TypeKind.JSON), @Argument(name = "contentType",
                type = TypeKind.STRING)},
        isPublic = true
)
public class SetJson extends BlockingNativeCallableUnit {
    @Override
    public void execute(Context context) {
//        BMap<String, BValue> entityStruct = (BMap<String, BValue>) context.getRefArgument(FIRST_PARAMETER_INDEX);
//        BValue jsonContent = context.getNullableRefArgument(SECOND_PARAMETER_INDEX);
//        String contentType = context.getStringArgument(FIRST_PARAMETER_INDEX);
//        EntityBodyHandler.addMessageDataSource(entityStruct, jsonContent);
//        MimeUtil.setMediaTypeToEntity(entityStruct, contentType);
//        context.setReturnValues();
    }

    public void setJson(Strand strand, ObjectValue entityObj, Object jsonContent, String contentType) {
        EntityBodyHandler.addMessageDataSource(entityObj, jsonContent);
        MimeUtil.setMediaTypeToEntity(entityObj, contentType);
    }
}