/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.ui.table.cell;

import org.junit.Test;
import org.onosproject.ui.table.CellFormatter;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link EnumFormatter}.
 */
public class EnumFormatterTest {

    enum TestEnum {
        ADDED,
        PENDING_ADD,
        WAITING_AUDIT_COMPLETE
    }

    private CellFormatter fmt = EnumFormatter.INSTANCE;

    @Test
    public void nullValue() {
        assertEquals("null value", "", fmt.format(null));
    }

    @Test
    public void noUnderscores() {
        assertEquals("All caps", "Added", fmt.format(TestEnum.ADDED));
    }

    @Test
    public void underscores() {
        assertEquals("All caps with underscores",
                     "Pending Add", fmt.format(TestEnum.PENDING_ADD));
    }

    @Test
    public void multiUnderscores() {
        assertEquals("All caps with underscores",
                     "Waiting Audit Complete",
                     fmt.format(TestEnum.WAITING_AUDIT_COMPLETE));
    }

}
