/*
 * Copyright 2009 Max Ishchenko
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

package net.ishchenko.idea.nginx.formatter;

import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.formatting.Indent;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.formatter.FormattingDocumentModelImpl;
import com.intellij.psi.formatter.PsiBasedFormattingModel;
import net.ishchenko.idea.nginx.formatter.blocks.NginxBlock;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 13.07.2009
 * Time: 22:51:23
 */
public class NginxFormattingModelBuilder implements FormattingModelBuilder {


    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {

        PsiElement element = formattingContext.getPsiElement();
        ASTNode node = element.getNode();
        assert node != null;
        PsiFile containingFile = element.getContainingFile();
        ASTNode astNode = containingFile.getNode();
        assert astNode != null;
        NginxBlock nginxBlock = new NginxBlock(node, Indent.getAbsoluteNoneIndent(), null);
        return new PsiBasedFormattingModel(containingFile, nginxBlock, FormattingDocumentModelImpl.createOn(containingFile));
    }


    @Override
    public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
        return null;
    }

}
