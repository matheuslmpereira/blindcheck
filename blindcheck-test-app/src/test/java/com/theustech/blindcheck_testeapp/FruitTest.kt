package com.theustech.blindcheck_testeapp

import org.junit.Assert.assertEquals
import org.junit.Test

class FruitTest {
    @Test
    fun fruit_keepsDeterministicLabelAndDescription() {
        val fruit = Fruit(
            name = "Banana",
            description = "Fruta amarela, doce e facil de descascar.",
        )

        assertEquals("Banana", fruit.name)
        assertEquals("Fruta amarela, doce e facil de descascar.", fruit.description)
    }
}
