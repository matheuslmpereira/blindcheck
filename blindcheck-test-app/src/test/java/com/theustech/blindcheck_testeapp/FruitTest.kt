package com.theustech.blindcheck_testeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FruitTest {
    @Test
    fun deterministicFruits_areNotEmptyAndAllHaveNonBlankLabels() {
        assertTrue(deterministicFruits.isNotEmpty())
        deterministicFruits.forEach { fruit ->
            assertTrue("fruit '${fruit.name}' has blank name", fruit.name.isNotBlank())
            assertTrue("fruit '${fruit.name}' has blank description", fruit.description.isNotBlank())
        }
    }

    @Test
    fun deterministicFruits_containsExpectedItems() {
        val names = deterministicFruits.map { it.name }
        assertTrue("Banana" in names)
        assertTrue("Laranja" in names)
        assertTrue("Uva" in names)
    }

    @Test
    fun deterministicFruits_hasNoDuplicateNames() {
        val names = deterministicFruits.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun fruit_equalityIsStructural() {
        val a = Fruit("Banana", "Fruta amarela, doce e facil de descascar.")
        val b = Fruit("Banana", "Fruta amarela, doce e facil de descascar.")
        assertEquals(a, b)
    }
}
