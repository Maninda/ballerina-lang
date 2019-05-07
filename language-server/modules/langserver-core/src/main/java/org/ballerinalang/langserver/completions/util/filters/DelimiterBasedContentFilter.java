/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.langserver.completions.util.filters;

import org.ballerinalang.langserver.LSGlobalContextKeys;
import org.ballerinalang.langserver.common.UtilSymbolKeys;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.common.utils.FilterUtils;
import org.ballerinalang.langserver.compiler.DocumentServiceKeys;
import org.ballerinalang.langserver.compiler.LSContext;
import org.ballerinalang.langserver.completions.CompletionKeys;
import org.ballerinalang.langserver.completions.SymbolInfo;
import org.ballerinalang.langserver.index.LSIndexException;
import org.ballerinalang.langserver.index.LSIndexImpl;
import org.ballerinalang.langserver.index.dao.BPackageSymbolDAO;
import org.ballerinalang.langserver.index.dao.DAOType;
import org.ballerinalang.langserver.index.dto.BFunctionSymbolDTO;
import org.ballerinalang.langserver.index.dto.BObjectTypeSymbolDTO;
import org.ballerinalang.langserver.index.dto.BPackageSymbolDTO;
import org.ballerinalang.langserver.index.dto.BRecordTypeSymbolDTO;
import org.ballerinalang.langserver.index.dto.OtherTypeSymbolDTO;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ballerinalang.compiler.semantics.model.types.BFutureType;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Filter the actions, functions and types in a package.
 */
public class DelimiterBasedContentFilter extends AbstractSymbolFilter {

    private static final Logger logger = LoggerFactory.getLogger(DelimiterBasedContentFilter.class);

    @Override
    public Either<List<CompletionItem>, List<SymbolInfo>> filterItems(LSContext ctx) {

        List<String> poppedTokens = CommonUtil.popNFromList(CommonUtil.getPoppedTokenStrings(ctx), 3);

        String delimiter = "";
        for (String poppedToken : poppedTokens) {
            if (poppedToken.equals(UtilSymbolKeys.DOT_SYMBOL_KEY)
                    || poppedToken.equals(UtilSymbolKeys.PKG_DELIMITER_KEYWORD)
                    || poppedToken.equals(UtilSymbolKeys.RIGHT_ARROW_SYMBOL_KEY)
                    || poppedToken.equals(UtilSymbolKeys.LEFT_ARROW_SYMBOL_KEY)
                    || poppedToken.equals(UtilSymbolKeys.BANG_SYMBOL_KEY)) {
                delimiter = poppedToken;
                break;
            }
        }
        String symbolToken;
        ArrayList<SymbolInfo> returnSymbolsInfoList = new ArrayList<>();
        if (poppedTokens.lastIndexOf(delimiter) > 0) {
            // get token before delimiter
            symbolToken = poppedTokens.get(poppedTokens.lastIndexOf(delimiter) - 1);
        } else {
            // get token after delimiter
            symbolToken = poppedTokens.get(poppedTokens.lastIndexOf(delimiter) + 1);
        }
        List<SymbolInfo> visibleSymbols = ctx.get(CompletionKeys.VISIBLE_SYMBOLS_KEY);
        SymbolInfo symbol = FilterUtils.getVariableByName(symbolToken, visibleSymbols);
        boolean isWorkerReceive = UtilSymbolKeys.LEFT_ARROW_SYMBOL_KEY.equals(delimiter);
        boolean isActionInvocation = UtilSymbolKeys.RIGHT_ARROW_SYMBOL_KEY.equals(delimiter)
                && CommonUtil.isClientObject(symbol.getScopeEntry().symbol);
        boolean isWorkerSend = !isActionInvocation && UtilSymbolKeys.RIGHT_ARROW_SYMBOL_KEY.equals(delimiter);

        if (UtilSymbolKeys.DOT_SYMBOL_KEY.equals(delimiter) || UtilSymbolKeys.BANG_SYMBOL_KEY.equals(delimiter)
                || isActionInvocation) {
            returnSymbolsInfoList.addAll(FilterUtils.getInvocationAndFieldSymbolsOnVar(ctx, symbolToken, delimiter,
                    visibleSymbols));
        } else if (isWorkerSend || isWorkerReceive) {
            List<SymbolInfo> filteredList = visibleSymbols.stream()
                    .filter(symbolInfo -> symbolInfo.getScopeEntry().symbol.type instanceof BFutureType
                    && ((BFutureType) symbolInfo.getScopeEntry().symbol.type).workerDerivative)
                    .collect(Collectors.toList());
            returnSymbolsInfoList.addAll(filteredList);
        } else if (UtilSymbolKeys.PKG_DELIMITER_KEYWORD.equals(delimiter)) {
            // We are filtering the package functions, actions and the types
            Either<List<CompletionItem>, List<SymbolInfo>> filteredList = this.getActionsFunctionsAndTypes(ctx,
                    symbolToken, delimiter);
            if (filteredList.isLeft()) {
                return Either.forLeft(filteredList.getLeft());
            }
            returnSymbolsInfoList.addAll(filteredList.getRight());
        }

        return Either.forRight(returnSymbolsInfoList);
    }

    /**
     * Get the actions, functions and types.
     * @param context               Text Document Service context (Completion Context)
     * @param pkgName               Package name to evaluate against
     * @param delimiter             Delimiter
     * @return {@link ArrayList}    List of filtered symbol info
     */
    private Either<List<CompletionItem>, List<SymbolInfo>> getActionsFunctionsAndTypes(
            LSContext context, String pkgName, String delimiter) {

        LSIndexImpl lsIndex = context.get(LSGlobalContextKeys.LS_INDEX_KEY);
        // Extract the package symbol
        String relativePath = context.get(DocumentServiceKeys.RELATIVE_FILE_PATH_KEY);
        BLangPackage currentBLangPkg = context.get(DocumentServiceKeys.CURRENT_BLANG_PACKAGE_CONTEXT_KEY);
        BLangPackage sourceOwnerPkg = CommonUtil.getSourceOwnerBLangPackage(relativePath, currentBLangPkg);
        Optional bLangImport = CommonUtil.getCurrentFileImports(sourceOwnerPkg, context)
                .stream()
                .filter(importPkg -> importPkg.getAlias().getValue().equals(pkgName))
                .findFirst();
        
        try {
            String realPackageName;
            String realOrgName;
            if (bLangImport.isPresent()) {
                // There is an added import statement.
                realPackageName = CommonUtil.getPackageNameComponentsCombined(((BLangImportPackage) bLangImport.get()));
                realOrgName = ((BLangImportPackage) bLangImport.get()).getOrgName().getValue();
            } else {
                realPackageName = pkgName;
                realOrgName = "";
            }
            
            BPackageSymbolDTO dto = new BPackageSymbolDTO.BPackageSymbolDTOBuilder()
                    .setName(realPackageName)
                    .setOrgName(realOrgName)
                    .build();
            List<BPackageSymbolDTO> result = ((BPackageSymbolDAO) lsIndex.getDaoFactory().get(DAOType.PACKAGE_SYMBOL))
                    .get(dto);
            
            if (result.isEmpty()) {
                // There is no package entry found in the index.
                return this.filterSymbolsOnFallback(context, pkgName, delimiter);
            } else {
                // Package entry found in the index. content is searched LSIndex.
                HashMap<Integer, ArrayList<CompletionItem>> completionMap = new HashMap<>();

                BPackageSymbolDAO pkgSymbolDAO = ((BPackageSymbolDAO) lsIndex.getDaoFactory()
                        .get(DAOType.PACKAGE_SYMBOL));
                ArrayList<BFunctionSymbolDTO> funcDTOs = new ArrayList<>(pkgSymbolDAO.getFunctions(dto, -1, false,
                        false, false));
                ArrayList<BRecordTypeSymbolDTO> recordDTOs = new ArrayList<>(pkgSymbolDAO.getRecords(dto, false));
                ArrayList<OtherTypeSymbolDTO> otherTypeDTOs = new ArrayList<>(pkgSymbolDAO.getOtherTypes(dto));
                ArrayList<BObjectTypeSymbolDTO> objDTOs = new ArrayList<>(pkgSymbolDAO.getObjects(dto, false));
                ArrayList<BObjectTypeSymbolDTO> clientEpDTOs = new ArrayList<>(pkgSymbolDAO.getClientEndpoints(dto));

                if (bLangImport.isPresent()) {
                    List<CompletionItem> completionItems = funcDTOs.stream()
                            .map(BFunctionSymbolDTO::getCompletionItem)
                            .collect(Collectors.toList());
                    completionItems.addAll(recordDTOs.stream()
                            .map(BRecordTypeSymbolDTO::getCompletionItem)
                            .collect(Collectors.toList()));
                    completionItems.addAll(objDTOs.stream()
                            .map(BObjectTypeSymbolDTO::getCompletionItem)
                            .collect(Collectors.toList()));
                    completionItems.addAll(clientEpDTOs.stream()
                            .map(BObjectTypeSymbolDTO::getCompletionItem)
                            .collect(Collectors.toList()));
                    completionItems.addAll(otherTypeDTOs.stream()
                            .map(OtherTypeSymbolDTO::getCompletionItem)
                            .collect(Collectors.toList()));
                    
                    return Either.forLeft(completionItems);
                }
                
                funcDTOs.forEach(fDto ->
                        CommonUtil.populateIdCompletionMap(completionMap, fDto.getPackageId(),
                                fDto.getCompletionItem()));
                recordDTOs.forEach(rDto ->
                        CommonUtil.populateIdCompletionMap(completionMap, rDto.getPackageId(),
                                rDto.getCompletionItem()));
                objDTOs.forEach(objDto ->
                        CommonUtil.populateIdCompletionMap(completionMap, objDto.getPackageId(),
                                objDto.getCompletionItem()));
                otherTypeDTOs.forEach(otherDto ->
                        CommonUtil.populateIdCompletionMap(completionMap, otherDto.getPackageId(),
                                otherDto.getCompletionItem()));
                
                return Either.forLeft(CommonUtil.fillCompletionWithPkgImport(completionMap, context));
            }
        } catch (LSIndexException e) {
            logger.warn("Error retrieving Completion Items from Index DB.");
            return this.filterSymbolsOnFallback(context, pkgName, delimiter);
        }
    }
    
    private Either<List<CompletionItem>, List<SymbolInfo>> filterSymbolsOnFallback(LSContext context,
                                                                                   String pkgName, String delimiter) {
        List<SymbolInfo> visibleSymbols = context.get(CompletionKeys.VISIBLE_SYMBOLS_KEY);
        List<SymbolInfo> filteredSymbols = FilterUtils.getInvocationAndFieldSymbolsOnVar(context, pkgName,
                delimiter, visibleSymbols);
        return Either.forRight(filteredSymbols);
    }
}