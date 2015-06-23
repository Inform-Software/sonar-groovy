/*
 * Copyright 2010 the original author or authors.
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
package org.codenarc.util

/**
 * Path-related utility methods.
 *
 * @author Chris Mair
 * @version $Revision: 290 $ - $Date: 2010-01-17 05:33:12 +0300 (Вс, 17 янв 2010) $
 */
class PathUtil {

    static String getName(String path) {
        if (!path) {
            return null
        }
        int separatorIndex1 = path.lastIndexOf('/');
        int separatorIndex2 = path.lastIndexOf('\\');
        int separatorIndex = [separatorIndex1, separatorIndex2].max();
        return (separatorIndex == -1) ? path : path.substring(separatorIndex + 1);
    }

    // Private constructor to prevent instantiation. All members are static.
    private PathUtil() { }
}