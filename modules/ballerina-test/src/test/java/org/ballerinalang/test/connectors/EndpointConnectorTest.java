/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.test.connectors;

import org.ballerinalang.launcher.util.BAssertUtil;
import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BRunUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.util.exceptions.BLangRuntimeException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test class for endpoint connector combination.
 */
public class EndpointConnectorTest {
    CompileResult result;
    CompileResult resultNegative;

    @BeforeClass()
    public void setup() {
        result = BCompileUtil.compile("test-src/connectors/endpoint-connector.bal");
        resultNegative = BCompileUtil.compile("test-src/connectors/endpoint-connector-negative.bal");
    }

    @Test(description = "Test simple endpoint creation")
    public void testEndpointWithConnector() {
        BValue[] returns = BRunUtil.invoke(result, "testEndpointWithConnector");

        Assert.assertEquals(returns.length, 1);
        Assert.assertSame(returns[0].getClass(), BString.class);
        Assert.assertEquals(returns[0].stringValue(), "Foo-val1");
    }

    @Test(description = "Test create statement as connector parameter")
    public void testCreateAsConnectorParam() {
        BValue[] returns = BRunUtil.invoke(result, "testCreateAsConnectorParam");

        Assert.assertEquals(returns.length, 1);
        Assert.assertSame(returns[0].getClass(), BString.class);
        Assert.assertEquals(returns[0].stringValue(), "Bar-val1-val2-Foo-val1");
    }

    @Test(description = "Test using connector as variable reference in endpoint")
    public void testConnectorAsVarRef() {
        BValue[] returns = BRunUtil.invoke(result, "testConnectorAsVarRef");

        Assert.assertEquals(returns.length, 1);
        Assert.assertSame(returns[0].getClass(), BString.class);
        Assert.assertEquals(returns[0].stringValue(), "Bar-val1-val2-Foo-val1");
    }

    @Test(description = "Test using endpoint type as base connector")
    public void testBaseConnectorEndpointType() {
        BValue[] returns = BRunUtil.invoke(result, "testBaseConnectorEndpointType");

        Assert.assertEquals(returns.length, 1);
        Assert.assertSame(returns[0].getClass(), BString.class);
        Assert.assertEquals(returns[0].stringValue(), "FooFilter1-val1-val2-Foo-val1");
    }

    @Test(description = "Test casting connectors to base type")
    public void testCastingConnectorToBaseType() {
        BValue[] returns = BRunUtil.invoke(result, "testCastingConnectorToBaseType");

        Assert.assertEquals(returns.length, 1);
        Assert.assertSame(returns[0].getClass(), BString.class);
        Assert.assertEquals(returns[0].stringValue(), "FooFilter2-val1-para3-" +
                "FooFilter1-val1-para2-FooFilter2-val1-para1-Foo-val1-");
    }

    @Test(description = "Test bind connection with endpoint")
    public void testBindConnectionToEndpoint() {
        BValue[] returns = BRunUtil.invoke(result, "testBindConnectionToEndpoint");

        Assert.assertEquals(returns.length, 1);
        Assert.assertSame(returns[0].getClass(), BString.class);
        Assert.assertEquals(returns[0].stringValue(), "FooFilter1-val1-para2-FooFilter2-val1-para1-Foo-val1-");
    }

    @Test(description = "Test empty endpoint invocation", expectedExceptions = BLangRuntimeException.class,
            expectedExceptionsMessageRegExp = "error: error, message: action invocation on empty endpoint.*")
    public void testEmptyEndpointInvocation() {
        BRunUtil.invoke(result, "testEmptyEndpointInvocation");
    }

    @Test(description = "Test endpoint, connectors with errors")
    public void testConnectorNegativeCases() {
        Assert.assertEquals(resultNegative.getErrorCount(), 9);
        BAssertUtil.validateError(resultNegative, 0, "invalid action invocation on connector: 'Foo', expect endpoint",
                27, 17);
        BAssertUtil.validateError(resultNegative, 1, "incompatible types: expected 'Bar', found 'Foo'", 32, 9);
        BAssertUtil.validateError(resultNegative, 2, "unknown type 'TestConnector'", 37, 5);
        BAssertUtil.validateError(resultNegative, 3, "cannot assign a value to endpoint 'en'", 47, 5);
        BAssertUtil.validateError(resultNegative, 4, "cannot assign a value to endpoint 'en'", 49, 5);
        BAssertUtil.validateError(resultNegative, 5, "incompatible types: expected 'Foo', found 'string'", 49, 10);
        BAssertUtil.validateError(resultNegative, 6, "cannot assign a value to endpoint 'en'", 50, 5);
        BAssertUtil.validateError(resultNegative, 7, "unreachable code", 59, 5);
        BAssertUtil.validateError(resultNegative, 8, "this function must return a result", 53, 1);
    }
}
