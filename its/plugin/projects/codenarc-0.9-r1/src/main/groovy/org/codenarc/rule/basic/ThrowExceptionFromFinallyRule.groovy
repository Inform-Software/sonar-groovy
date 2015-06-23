/*
 * Copyright 2009 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codenarc.rule.basic

import org.codehaus.groovy.ast.stmt.ThrowStatement
import org.codenarc.rule.AbstractAstVisitorRule

/**
 * Rule that checks for throwing an exception from within a finally block
 *
 * @author Chris Mair
 * @version $Revision: 139 $ - $Date: 2009-05-08 05:55:09 +0400 (Пт, 08 май 2009) $
 */
class ThrowExceptionFromFinallyBlockRule extends AbstractAstVisitorRule {
    String name = 'ThrowExceptionFromFinallyBlock'
    int priority = 2
    Class astVisitorClass = ThrowExceptionFromFinallyBlockAstVisitor
}

class ThrowExceptionFromFinallyBlockAstVisitor extends AbstractFinallyAstVisitor  {
    void visitThrowStatement(ThrowStatement throwStatement) {
        if (isFirstVisit(throwStatement) && isStatementWithinFinally(throwStatement)) {
            addViolation(throwStatement)
        }
        super.visitThrowStatement(throwStatement)
    }

}