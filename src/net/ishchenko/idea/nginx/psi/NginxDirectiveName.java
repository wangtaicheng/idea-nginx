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

package net.ishchenko.idea.nginx.psi;

import com.intellij.psi.PsiNamedElement;

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 09.07.2009
 * Time: 21:03:39
 */

/**
 * PsiNamedElement is implemented solely for quick documentation lookup purposes.
 * Ctrl+q just won't work if PsiNamedElement is not implemented
 */
public interface NginxDirectiveName extends NginxPsiElement, PsiNamedElement {

    NginxDirective getDirective();

}
