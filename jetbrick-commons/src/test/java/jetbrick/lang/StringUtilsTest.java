/**
 * Copyright 2013-2014 Guoqiang Chen, Shanghai, China. All rights reserved.
 *
 * Email: subchen@gmail.com
 * URL: http://subchen.github.io/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrick.lang;

import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testSplitQuote() {
        Assert.assertArrayEquals(new String[] { "aa" }, StringUtils.splitCSV("aa"));
        Assert.assertArrayEquals(new String[] { "aa", "bb" }, StringUtils.splitCSV("aa, bb"));
        Assert.assertArrayEquals(new String[] { "aa", "bb", "cc, dd" }, StringUtils.splitCSV("aa, bb, 'cc, dd'"));
        Assert.assertArrayEquals(new String[] { "aa", "bb", ",cc,, dd," }, StringUtils.splitCSV("aa, bb, ',cc,, dd,'"));
    }

}
