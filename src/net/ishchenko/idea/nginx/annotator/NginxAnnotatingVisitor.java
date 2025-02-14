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

package net.ishchenko.idea.nginx.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import com.intellij.util.Range;
import net.ishchenko.idea.nginx.NginxBundle;
import net.ishchenko.idea.nginx.NginxKeywordsManager;
import net.ishchenko.idea.nginx.configurator.NginxServerDescriptor;
import net.ishchenko.idea.nginx.configurator.NginxServersConfiguration;
import net.ishchenko.idea.nginx.psi.*;

import java.util.Optional;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 14.07.2009
 * Time: 16:04:13
 */

public class NginxAnnotatingVisitor extends NginxElementVisitor implements Annotator {

    private AnnotationHolder holder;

    private NginxKeywordsManager keywords;
    private NginxServersConfiguration configuration;

    public NginxAnnotatingVisitor() {
        this.keywords = NginxKeywordsManager.getInstance();
        this.configuration = NginxServersConfiguration.getInstance();
    }

    @Override
    public synchronized void annotate(PsiElement psiElement, AnnotationHolder holder) {
        this.holder = holder;
        psiElement.accept(this);
        this.holder = null;
    }

    @Override
    public void visitDirective(NginxDirective node) {

        if(node.isInChaosContext()) {
            return; // directive resides in context like charset_map where almost arbitrary contents are possible
        }
        if(!this.checkNameIsLegal(node.getDirectiveName())) {
            return; // name is not known - no point for further investigation
        }

        // ok, now we know that directive does exist. let's do some more advanced checks.
        this.checkDeprecatedDirectives(node);
        this.checkParentContext(node);
        this.checkChildContext(node);
        this.checkValueCount(node);

    }

    @Override
    public void visitComplexValue(NginxComplexValue node) {

        if(node.getDirective()
                .isInChaosContext()) {
            return;
        }

        String directiveName = node.getDirective()
                .getNameString();
        if(this.keywords.checkBooleanKeyword(directiveName)) {
            this.checkBooleanValue(node, directiveName);
        }
    }

    @Override
    public void visitInnerVariable(NginxInnerVariable node) {

        // should I cut $ in NginxInnerVariable itself?
        // get references will return 1 for itself
        if(!this.keywords.isValidInnerVariable(node.getName())
           && ((node.getReference() != null && node.getReference()
                                                       .resolve() == null)
               || node.getReference() == null)) {
            this.holder.newAnnotation(HighlightSeverity.ERROR, NginxBundle.message("annotator.variable.notexists", node.getText()))
                    .range(node.getTextRange());
        }

    }

    private void checkDeprecatedDirectives(NginxDirective node) {
        String name = node.getNameString();

        if(NginxKeywordsManager.OPENRESTY_DEPRECATED_KEYWORDS.contains(name)) {
            this.holder.newAnnotation(HighlightSeverity.WARNING, NginxBundle.message("annotator.directive.openresty.deprecated", name))
                    .range(node.getTextRange());
        }
    }

    private void checkValueCount(NginxDirective node) {

        String nameString = node.getNameString();

        // here comes ugly workaround for ambiguous directives.
        // todo: resolve ambiguity properly
        if("server".equals(nameString)) {
            NginxContext parentContext = node.getParentContext();
            if(parentContext != null && parentContext.getDirective() != null) {
                if("upstream".equals(parentContext.getDirective()
                        .getNameString())) {
                    return;
                }
            }
        }

        int realRange = node.getValues()
                .size();
        Set<Range<Integer>> expectedRanges = this.keywords.getValueRange(nameString);

        Optional<Range<Integer>> possibleRanges = expectedRanges.stream()
                .filter(range -> range.isWithin(realRange))
                .findFirst();

        Range<Integer> expectedRange = possibleRanges.orElse(
                expectedRanges.stream()
                        .min((range1, range2) -> range1.getFrom() - range2.getFrom() - (range1.getTo() - range2.getTo()))
                        .get() // assume that there is always a value
        );

        if(!expectedRange.isWithin(realRange)) {

            String rangeString;
            if(expectedRange.getFrom()
                    .equals(expectedRange.getTo())) {
                rangeString = expectedRange.getFrom()
                        .toString();
            } else {
                rangeString = "[" + expectedRange.getFrom() + ", " + expectedRange.getTo() + "]";
            }
            String message = NginxBundle.message("annotator.directive.wrongnumberofvalues", nameString, rangeString, realRange);

            for(NginxComplexValue nginxComplexValue : node.getValues()) {
                this.holder.newAnnotation(HighlightSeverity.ERROR, message)
                        .range(nginxComplexValue.getTextRange());
            }
            if(node.getValues()
                    .isEmpty()) {
                this.holder.newAnnotation(HighlightSeverity.ERROR, message)
                        .range(node.getDirectiveName()
                                .getTextRange());
            }

        }

    }

    private void checkChildContext(NginxDirective node) {
        if(node.hasContext() && !this.keywords.checkCanHaveChildContext(node.getNameString())) {
            this.holder.newAnnotation(HighlightSeverity.ERROR, NginxBundle.message("annotator.directive.canthavecontext", node.getNameString())
                    )
                    .range(node.getTextRange());
        }
    }

    private void checkParentContext(NginxDirective node) {
        NginxContext parentContext = node.getParentContext();
        if(parentContext == null) {
            // top level directive checks are made only main file. other files can be potentially included
            if(this.nodeInMainConfig(node) && !this.keywords.checkCanResideInMainContext(node.getNameString())) {
                this.holder.newAnnotation(HighlightSeverity.WARNING, NginxBundle.message("annotator.directive.cantbeinmain", node.getNameString()))
                        .range(node.getTextRange());
            }
        } else {
            NginxDirective parent = parentContext.getDirective();
            if(!this.keywords.checkCanHaveParentContext(node.getNameString(), parent.getNameString())) {
                this.holder.newAnnotation(HighlightSeverity.WARNING, node.getNameString() + " cant reside in " + parent.getNameString())
                        .range(node.getTextRange());
            }

        }
    }

    private boolean nodeInMainConfig(NginxDirective node) {
        boolean isInMainConfig = false;
        NginxServerDescriptor[] serversDescriptors = this.configuration.getServersDescriptors();
        for(NginxServerDescriptor serversDescriptor : serversDescriptors) {
            if(serversDescriptor.getConfigPath()
                    .equals(node.getContainingFile()
                            .getVirtualFile()
                            .getPath())) {
                isInMainConfig = true;
                break;
            }
        }
        return isInMainConfig;
    }

    private void checkBooleanValue(NginxComplexValue node, String directiveName) {
        if(node.isFirstValue()) {
            if(!("on".equals(node.getText()) || "off".equals(node.getText()))) {
                this.holder.newAnnotation(HighlightSeverity.ERROR, NginxBundle.message("annotator.expected.boolean"))
                        .range(node.getTextRange());
            }
        } else {
            this.holder.newAnnotation(HighlightSeverity.ERROR, NginxBundle.message("annotator.not.boolean", directiveName))
                    .range(node.getTextRange());
        }
    }

    private boolean checkNameIsLegal(NginxDirectiveName node) {

        if(this.keywords.getKeywords()
                .contains(node.getText())) {
            return true;
        } else {
            this.holder.newAnnotation(HighlightSeverity.WARNING, NginxBundle.message("annotator.directive.unknown", node.getText()))
                    .range(node.getTextRange());
            return false;
        }

    }

}



