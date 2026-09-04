package com.rm.blokhead.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormCatalogTest {

    @Test
    fun `parses all 32 pieces`() {
        assertEquals(32, FormCatalog.allForms.size)
    }

    @Test
    fun `first piece is a single cube`() {
        val cube = FormCatalog.allForms[0]
        assertEquals(listOf(1, 1, 1), cube.dimensions.toList())
        assertEquals(1, cube.numCubes)
    }

    @Test
    fun `second piece is a two-cube domino`() {
        val smallI = FormCatalog.allForms[1]
        assertEquals(listOf(2, 1, 1), smallI.dimensions.toList())
        assertEquals(2, smallI.numCubes)
    }

    @Test
    fun `piece cube counts follow the 1-1-2-7-21 distribution`() {
        val byCubeCount = FormCatalog.allForms.groupingBy { it.numCubes }.eachCount()
        assertEquals(1, byCubeCount[1])
        assertEquals(1, byCubeCount[2])
        assertEquals(2, byCubeCount[3])
        assertEquals(7, byCubeCount[4])
        assertEquals(21, byCubeCount[5])
    }

    @Test
    fun `every piece has at least one cube and a matching dimensions volume`() {
        for (form in FormCatalog.allForms) {
            assertTrue(form.numCubes > 0)
            assertEquals(form.cubes.size, form.dimensions[0] * form.dimensions[1] * form.dimensions[2])
        }
    }
}
