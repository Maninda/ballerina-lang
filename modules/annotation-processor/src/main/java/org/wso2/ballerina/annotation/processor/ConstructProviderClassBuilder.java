/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.ballerina.annotation.processor;

import org.ballerinalang.util.repository.BuiltinPackageRepository;
import org.wso2.ballerina.annotation.processor.holders.ActionHolder;
import org.wso2.ballerina.annotation.processor.holders.ConnectorHolder;
import org.wso2.ballerina.annotation.processor.holders.FunctionHolder;
import org.wso2.ballerina.annotation.processor.holders.PackageHolder;
import org.wso2.ballerina.annotation.processor.holders.TypeConvertorHolder;
import org.wso2.ballerina.core.exception.BallerinaException;
import org.wso2.ballerina.core.model.BLangPackage;
import org.wso2.ballerina.core.model.GlobalScope;
import org.wso2.ballerina.core.model.NativeUnit;
import org.wso2.ballerina.core.model.SymbolName;
import org.wso2.ballerina.core.model.SymbolScope;
import org.wso2.ballerina.core.model.types.SimpleTypeName;
import org.wso2.ballerina.core.model.types.TypeEnum;
import org.wso2.ballerina.core.nativeimpl.NativeConstructLoader;
import org.wso2.ballerina.core.nativeimpl.NativePackageProxy;
import org.wso2.ballerina.core.nativeimpl.NativeUnitProxy;
import org.wso2.ballerina.core.nativeimpl.annotations.Argument;
import org.wso2.ballerina.core.nativeimpl.annotations.BallerinaAction;
import org.wso2.ballerina.core.nativeimpl.annotations.BallerinaConnector;
import org.wso2.ballerina.core.nativeimpl.annotations.BallerinaFunction;
import org.wso2.ballerina.core.nativeimpl.annotations.BallerinaTypeConvertor;
import org.wso2.ballerina.core.nativeimpl.annotations.ReturnType;
import org.wso2.ballerina.core.nativeimpl.connectors.AbstractNativeConnector;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * Builder class to generate the ballerina constructs provider class.
 * The class generated by this builder will register all the annotated 
 * classes as {@link NativeUnitProxy}s to the global symbol table, via java SPI.
 */
public class ConstructProviderClassBuilder {
    
    private static final String SERVICES = "services" + File.separator;
    private static final String META_INF = "META-INF" + File.separator;
    private static final String GLOBAL_SCOPE = "globalScope";
    private static final String PACKAGE_SCOPE = "nativePackage";
    private static final String PACKAGE_REPO = "pkgRepo";
    private static final String DEFINE_METHOD = "define";
    private static final String EMPTY = "";

    
    private Writer sourceFileWriter;
    private String className;
    private String packageName;
    private String balSourceDir;
    private String nativeUnitClass = NativeUnit.class.getSimpleName();
    private String symbolNameClass = SymbolName.class.getSimpleName();
    private String nativeProxyClass = NativeUnitProxy.class.getSimpleName();
    private String builtinPackageRepositoryClass = BuiltinPackageRepository.class.getSimpleName();
    private String pkgProxyClass = NativePackageProxy.class.getSimpleName();
    private String pkgClass = BLangPackage.class.getSimpleName();
    
    private Map<String, PackageHolder> nativePackages;
    private String symbolNameStr = "new %s(\"%s\")";
    private final String importPkg = "import " + GlobalScope.class.getCanonicalName() + ";\n" + 
                                     "import " + NativeUnitProxy.class.getCanonicalName() + ";\n" + 
                                     "import " + SymbolName.class.getCanonicalName() + ";\n" + 
                                     "import " + NativeConstructLoader.class.getCanonicalName() + ";\n" +
                                     "import " + SimpleTypeName.class.getCanonicalName() + ";\n" +
                                     "import " + AbstractNativeConnector.class.getCanonicalName() + ";\n" +
                                     "import " + NativeUnit.class.getCanonicalName() + ";\n\n" +
                                     "import " + BLangPackage.class.getCanonicalName() + ";\n\n" +
                                     "import " + BuiltinPackageRepository.class.getCanonicalName() + ";\n\n" +
                                     "import " + NativePackageProxy.class.getCanonicalName() + ";\n\n";
    
    /**
     * Create a construct provider builder.
     * 
     * @param filer {@link Filer} of the current processing environment
     * @param packageName Package name of the generated construct provider class
     * @param className Class name of the generated construct provider class
     * @param srcDir 
     * @param targetDir
     */
    public ConstructProviderClassBuilder(Filer filer, String packageName, String className, String srcDir) {
        this.packageName = packageName;
        this.className = className;
        this.balSourceDir = srcDir;
        
        // Initialize the class writer. 
        initClassWriter(filer);
        
        // Create config file in META-INF/services directory
        createServiceMetaFile(filer);
    }
    
    /**
     * Initialize the class writer. Write static codes of the including:
     * <ul>
     * <li>The package name</li>
     * <li>Package imports</li>
     * <li>Class definition</li>
     * <li>Public constructor with no parameters</li>
     * <li>Method name for load() method</li>
     * </ul>
     * @param filer
     */
    private void initClassWriter(Filer filer) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("package " + packageName + ";\n\n");
        stringBuilder.append(importPkg);
        stringBuilder.append("public class " + className + 
                " implements " + NativeConstructLoader.class.getSimpleName() + " {\n\n");
        stringBuilder.append("public " + className + "() {}\n\n");
        stringBuilder.append("public void load(" + GlobalScope.class.getSimpleName() + " globalScope) {\n\n");
        stringBuilder.append(builtinPackageRepositoryClass + " " + PACKAGE_REPO + " = new " + 
                builtinPackageRepositoryClass + "(" + className + ".class);\n\n");
        
        try {
            JavaFileObject javaFile = filer.createSourceFile(packageName + "." + className);
            sourceFileWriter = javaFile.openWriter();
            sourceFileWriter.write(stringBuilder.toString());
        } catch (IOException e) {
            throw new BallerinaException("failed to initialize source generator: " + e.getMessage());
        }
    }
    
    
    /**
     * Create the configuration file in META-INF/services, required for java service
     * provider api.
     * 
     * @param filer {@link Filer} associated with this annotation processor.
     */
    private void createServiceMetaFile(Filer filer) {
        Writer configWriter = null;
        try {
            //Find the location of the resource/META-INF directory.
            FileObject metaFile = filer.createResource(StandardLocation.CLASS_OUTPUT, "",  META_INF + SERVICES + 
                    NativeConstructLoader.class.getCanonicalName());
            configWriter = metaFile.openWriter();
            configWriter.write(packageName + "." + className);
        } catch (IOException e) {
            throw new BallerinaException("error while generating config file: " + e.getMessage());
        } finally {
            if (configWriter != null) {
                try {
                    configWriter.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
    
    /**
     * Add the package map to the builder.
     * 
     * @param nativePackages Packages map
     */
    public void addNativePackages(Map<String, PackageHolder> nativePackages) {
        this.nativePackages = nativePackages;
    }
    
    /**
     * Build the class. Append the remaining implemented methods and and write the source
     * file to the target (package) location.
     */
    public void build() {
        try {
            for (PackageHolder pkgHolder : nativePackages.values()) {
                String nativePkgName = pkgHolder.getPackageName();
                String pkgInsertionStr = 
                        GLOBAL_SCOPE + ".define(new " + symbolNameClass + "(\"" + nativePkgName + "\"),\n" +
                        "\tnew " + pkgProxyClass + "(() -> {\n" + 
                        "\t\t" + pkgClass + " " + PACKAGE_SCOPE + " = new " + pkgClass + "(" + GLOBAL_SCOPE 
                        + ");\n" + 
                        "\t\t" + PACKAGE_SCOPE + ".setPackagePath(\"" + nativePkgName + "\");\n";
                sourceFileWriter.write(pkgInsertionStr);
                writeFunctions(pkgHolder.getFunctions());
                writeConnectors(pkgHolder.getConnectors());
                writeTypeConvertors(pkgHolder.getTypeConvertors());
                String pkgInsertionEndStr = "\t" + PACKAGE_SCOPE + ".setPackageRepository(" + PACKAGE_REPO + ");\n" +
                        "\treturn nativePackage;\n\t}, " + GLOBAL_SCOPE + ")\n);\n\n";
                sourceFileWriter.write(pkgInsertionEndStr);
            }
            
            writeBuiltInBalPackages();
            
            sourceFileWriter.write("}\n}\n");
        } catch (IOException e) {
            throw new BallerinaException("error while writing source to file: " + e.getMessage());
        } finally {
            if (sourceFileWriter != null) {
                try {
                    sourceFileWriter.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
    
    /**
     * Write the built-in non-native packages to the provider class.
     * This method will traverse through the built-in ballerina files generated by
     * {@link NativeBallerinaFileBuilder} and will add packages for only non-native
     * bal packages to the provider class.
     */
    private void writeBuiltInBalPackages() {
        //FIXME #1843
        Path source = Paths.get(balSourceDir);
        File srcDir = new File(source.toUri());
        
        if (!srcDir.exists()) {
            return;
        }
        
        if (!srcDir.isDirectory() && srcDir.canRead()) {
            throw new BallerinaException("error while reading built-in packages. ballerina source path '" +
                srcDir.getPath() + "' is not a directory, or may not have read permissions");
        }
        
        // Traverse through built-in ballerina files and identify the packages
        List<String> builtInPackages = new ArrayList<String>();
        try {
            Files.walkFileTree(source, new PackageFinder(source, builtInPackages));
        } catch (IOException e) {
            throw new BallerinaException("error while reading built-in packages: " + e.getMessage());
        }
        
        // Insert the non-native packages to the construct provider class 
        for (String builtInPkg : builtInPackages) {
            if (nativePackages.containsKey(builtInPkg)) {
                continue;
            }
            String pkgInsertionStr = 
                    GLOBAL_SCOPE + ".define(new " + symbolNameClass + "(\"" + builtInPkg + "\"),\n" +
                    "\tnew " + pkgProxyClass + "(() -> {\n" + 
                    "\t\t" + pkgClass + " " + PACKAGE_SCOPE + " = new " + pkgClass + "(" + GLOBAL_SCOPE + ");\n" +
                    "\t\t" + PACKAGE_SCOPE + ".setPackagePath(\"" + builtInPkg + "\");\n" + 
                    "\t\t" + PACKAGE_SCOPE + ".setPackageRepository(" + PACKAGE_REPO + ");\n" +
                    "\t\treturn nativePackage;\n\t}, " + GLOBAL_SCOPE + ")\n);\n\n";
            try {
                sourceFileWriter.write(pkgInsertionStr);
            } catch (IOException e) {
                throw new BallerinaException("error while writing source to file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Write all the function defining to the provider class.
     * 
     * @param functions Function holders array containing ballerina function annotations
     */
    private void writeFunctions(FunctionHolder[] functions) {
        for (FunctionHolder functionHolder : functions) {
            BallerinaFunction function = functionHolder.getBalFunction();
            String pkgName = function.packageName();
            String className = functionHolder.getClassName();
            String functionQualifiedName = Utils.getFunctionQualifiedName(function);
            writeNativeConstruct(pkgName, function.functionName(), functionQualifiedName, className,
                function.args(), function.returnType());
        }
    }
    
    /**
     * Write all the type convertors defining to the provider class.
     * 
     * @param typeConvertors Type convertor holders array containing ballerina type convertor annotations
     */
    private void writeTypeConvertors(TypeConvertorHolder[] typeConvertors) {
        for (TypeConvertorHolder typeConvertorHolder : typeConvertors) {
            BallerinaTypeConvertor typeConvertor = typeConvertorHolder.getBalTypeConvertor();
            String pkgName = typeConvertor.packageName();
            String className = typeConvertorHolder.getClassName();
            String typeConvertorQualifiedName = Utils.getTypeConverterQualifiedName(typeConvertor);
            writeNativeConstruct(pkgName, typeConvertor.typeConverterName(), 
                typeConvertorQualifiedName, className, typeConvertor.args(), typeConvertor.returnType());
        }
    }

    /**
     * Write all the type connectors defining to the provider class.
     * 
     * @param connectors Connector holders array containing ballerina connector annotations
     */
    public void writeConnectors(ConnectorHolder[] connectors) {
        String connectorVarName = "nativeConnector";
        for  (ConnectorHolder con : connectors) {
            BallerinaConnector balConnector = con.getBalConnector();
            String connectorName = balConnector.connectorName();
            String connectorPkgName = balConnector.packageName();
            String connectorClassName = con.getClassName();
            StringBuilder strBuilder = new StringBuilder();
            
            // Add all the actions of this connector, ad generate the insertion string
            for (ActionHolder action : con.getActions()) {
                BallerinaAction balAction = action.getBalAction();
                String actionQualifiedName = Utils.getActionQualifiedName(balAction, connectorName, connectorPkgName);
                String actionPkgName = balAction.packageName();
                String actionClassName = action.getClassName();
                String actionAddStr = getConstructInsertStr(connectorVarName, "addAction", actionPkgName, 
                    balAction.actionName(), actionQualifiedName, null, null, actionClassName, balAction.args(),
                    balAction.returnType(), "nativeAction", null, nativeUnitClass, "nativeActionClass", connectorName,
                    connectorPkgName);
                strBuilder.append(actionAddStr);
            }
            
            // Generate the connector insertion string with the actions as 
            String nativeConnectorClassName = AbstractNativeConnector.class.getSimpleName();
            String symbolScopClass = SymbolScope.class.getName() + ".class";
            String connectorAddStr = getConstructInsertStr(PACKAGE_SCOPE, DEFINE_METHOD, connectorPkgName, connectorName,
                connectorName, symbolScopClass, PACKAGE_SCOPE, connectorClassName, balConnector.args(), null,
                connectorVarName, strBuilder.toString(), nativeConnectorClassName, "nativeConnectorClass", null, null);
            try {
                sourceFileWriter.write(connectorAddStr);
            } catch (IOException e) {
                throw new BallerinaException("failed to write to source file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Write a native construct to the generated ConstructProviderClass source file.
     * 
     * @param packageName Package name of the construct
     * @param constructName Simple name of the construct
     * @param constructQualifiedName Qualified name of the construct
     * @param constructImplClassName Name of the construct implementation class
     * @param arguments Input parameters for the native construct
     * @param returnTypes Return types of the native construct
     */
    public void writeNativeConstruct(String packageName, String constructName, String constructQualifiedName, 
            String constructImplClassName, Argument[] arguments, ReturnType[] returnTypes) {
        String functionSupplier = getConstructInsertStr(PACKAGE_SCOPE, DEFINE_METHOD, packageName, constructName, 
            constructQualifiedName, null, null, constructImplClassName, arguments, returnTypes, 
            "nativeCallableUnit", null, nativeUnitClass, "nativeUnitClass", null, null);
        try {
            sourceFileWriter.write(functionSupplier);
        } catch (IOException e) {
            throw new BallerinaException("failed to write to source file: " + e.getMessage());
        }
    }
    
    /**
     * Create the string representation of java source, of the construct insertion to the provided scope.
     * 
     * @param scope Scope to which the construct is added
     * @param constructPkgName Package name of the construct
     * @param constructName Simple name of the construct
     * @param constructQualifiedName Qualified name of the construct
     * @param constructArgType Input parameter class for the parameterized constructor of this construct impl class
     * @param constructArg  Input parameter for the parameterized constructor of this construct impl class
     * @param constructImplClassName Name of the construct implementation class
     * @param arguments Input parameters for the native construct
     * @param returnTypes Return types of the native construct
     * @param constructVarName Name of the variable that holds the instance of this construct in generated class
     * @param scopeElements Child elements insertion string for the current construct. Only applicable for connectors
     * @param nativeUnitClass Class type of the current construct instance
     * @param nativeUnitClassVarName Name of the temp variable which holds the class of the native construct in the 
     * generated source.
     * @param enclosingScopeName Parent scope. Current construct will be added to this enclosingScope in the generated
     * source.
     * @param enclosingScopePkg Package name of the parent scope
     * @return
     */
    private String getConstructInsertStr(String scope, String addMethod, String constructPkgName, String constructName,
            String constructQualifiedName, String constructArgType, String constructArg, String constructImplClassName,
            Argument[] arguments, ReturnType[] returnTypes, String constructVarName, 
            String scopeElements, String nativeUnitClass, String nativeUnitClassVarName, String enclosingScopeName,
            String enclosingScopePkg) {
        String createSymbolStr = String.format(symbolNameStr, symbolNameClass, constructQualifiedName);
        String retrunTypesArrayStr = getReturnTypes(returnTypes);
        String argsTypesArrayStr = getArgTypes(arguments, enclosingScopeName, enclosingScopePkg);
        String supplierInsertStr = getConstructSuplierInsertionStr(constructVarName, nativeUnitClassVarName);
        if (constructArgType == null) {
            constructArgType = EMPTY;
        }
        if (constructArg == null) {
            constructArg = EMPTY;
        }
        if (scopeElements == null) {
            scopeElements = EMPTY;
        }
        return String.format(supplierInsertStr, scope, addMethod, createSymbolStr, nativeProxyClass, nativeUnitClass,
            constructImplClassName, nativeUnitClass, constructArgType, constructArg, constructName, constructPkgName, 
            argsTypesArrayStr, retrunTypesArrayStr, arguments.length, createSymbolStr, scopeElements);
    }
    
    
    /**
     * Get the return types array construction string.
     * 
     * @param returnTypes Array of return types
     * @return Return types array construction string
     */
    private String getReturnTypes(ReturnType[] returnTypes) {
        String simpleTypeNameClass = SimpleTypeName.class.getSimpleName();
        StringBuilder sb = new StringBuilder("new " + simpleTypeNameClass + "[]{");
        if (returnTypes != null) {
            int returnCount = 0;
            for (ReturnType returnType : returnTypes) {
                String bType;
                boolean isArray = false;
                // For non-array types.
                if (!returnType.type().equals(TypeEnum.ARRAY)) {
                    bType = returnType.type().getName();
                } else {
                    isArray = true;
                    bType = returnType.elementType().getName();
                }
                sb.append("new " + simpleTypeNameClass + "(\"" + bType + "\", " + isArray + ")");
                if (returnCount < returnTypes.length - 1) {
                    sb.append(",");
                }
                returnCount++;
            }
        }
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Get the argument types array construction string.
     * 
     * @param arguments Array of arguments
     * @param enclosingScopePkg 
     * @param enclosingScopeName 
     * @return Argument types array construction string
     */
    private String getArgTypes(Argument[] arguments, String enclosingScopeName, String enclosingScopePkg) {
        String simpleTypeNameClass = SimpleTypeName.class.getSimpleName();
        StringBuilder sb = new StringBuilder("new " + simpleTypeNameClass + "[]{");
        if (arguments != null) {
            int argCount = 0;
            for (Argument argType : arguments) {
                TypeEnum bType;
                boolean isArray = false;
                // For non-array types.
                if (!argType.type().equals(TypeEnum.ARRAY)) {
                    bType = argType.type();
                } else {
                    isArray = true;
                    bType = argType.elementType();
                }

                // If the argument is a connector, create the symbol name with connector name and package
                if (bType == TypeEnum.CONNECTOR) {
                    sb.append("new " + simpleTypeNameClass + "(\"" + enclosingScopeName + "\",\"" + enclosingScopePkg +
                        "\", " + isArray + ")");
                } else {
                    sb.append("new " + simpleTypeNameClass + "(\"" + bType.getName() + "\", " + isArray + ")");
                }

                if (argCount < arguments.length - 1) {
                    sb.append(", ");
                }
                argCount++;
            }
        }
        sb.append("}");
        return sb.toString();
        
    }
    
    private String getConstructSuplierInsertionStr(String nativeUnitVarName, String classVarName) {
        return "\t\t%s.%s(%s,%n" +
               "\t\t    new %s(() -> {%n" +
               "\t\t        %s " + nativeUnitVarName + " = null;%n" +
               "\t\t        try {%n" +
               "\t\t            Class<?> " + classVarName + " = Class.forName(\"%s\");%n" +
               "\t\t            " + nativeUnitVarName + " = ((%s) " + classVarName + 
               ".getConstructor(%s).newInstance(%s));%n" +
               "\t\t            " + nativeUnitVarName + ".setName(\"%s\");%n" +
               "\t\t            " + nativeUnitVarName + ".setPackagePath(\"%s\");%n" +
               "\t\t            " + nativeUnitVarName + ".setArgTypeNames(%s);%n" +
               "\t\t            " + nativeUnitVarName + ".setReturnParamTypeNames(%s);%n" +
               "\t\t            " + nativeUnitVarName + ".setStackFrameSize(%s);%n" +
               "\t\t            " + nativeUnitVarName + ".setSymbolName(%s);%n" +
               "\t\t            %s" +
               "\t\t            } catch (Exception ignore) {%n" +
               "\t\t        }%n" +
               "\t\t        return " + nativeUnitVarName + ";%n" +
               "\t\t    })%n" +
               "\t\t);%n%n";
    }
}
