/*
 * Copyright (C) 2014 Arthur D'Andréa Alemar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tetris.util.functional;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import tetris.util.functional.CartesianProduct.Pair;
/**
 *
 * @author Arthur D'Andréa Alemar
 */
public class CartesianProductTest {
    
    @Test
    public void testNext() {
        List<Integer> list1 = new ArrayList<>();
        list1.add(1);
        list1.add(2);
        List<Integer> list2 = new ArrayList<>();
        list2.add(3);
        list2.add(4);
        list2.add(5);

        CartesianProduct<Integer> cartesian = new CartesianProduct<>(list1, list2);
        ArrayList<Pair<Integer>> result = Lists.newArrayList(cartesian);
        assertThat(result, contains(
                new Pair(1, 3), new Pair(1, 4), new Pair(1, 5),
                new Pair(2, 3), new Pair(2, 4), new Pair(2, 5)));
    }
}
