package com.example

import com.example.data.model.Importance
import com.example.data.model.ShoppingItem
import com.example.data.model.Zone
import com.example.domain.CommandAction
import com.example.domain.RouteOptimizer
import com.example.domain.ShoppingParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testBareLeadingNumber_4Lapte_2Carnat() {
        // "4 lapte" -> quantity "4 buc", name "Lapte"
        val milk = ShoppingParser.parseSingleItem("4 lapte")
        assertEquals("Lapte", milk.name)
        assertEquals("4 buc", milk.quantity)
        assertFalse(milk.name.contains("4"))

        // "2 carnat" -> quantity "2 buc", diacritics "Cârnaț", store "Moldovan"
        val sausage = ShoppingParser.parseSingleItem("2 carnat")
        assertEquals("Cârnaț", sausage.name)
        assertEquals("2 buc", sausage.quantity)
        assertEquals("moldovan", sausage.subLocation)
        assertFalse(sausage.name.contains("2"))
    }

    @Test
    fun testBreadRules_PanemarVsMega() {
        // Plain "paine" or "paine taraneasca" -> Panemar
        val panemarBread = ShoppingParser.parseSingleItem("paine")
        assertEquals("panemar", panemarBread.subLocation)
        assertEquals("Pâine", panemarBread.name)

        val countryBread = ShoppingParser.parseSingleItem("paine taraneasca")
        assertEquals("panemar", countryBread.subLocation)

        // "paine domneasca" or "paine boiereasca" -> Mega Image
        val megaBread = ShoppingParser.parseSingleItem("paine domneasca")
        assertEquals("mega", megaBread.subLocation)

        val boiereascaBread = ShoppingParser.parseSingleItem("paine boiereasca")
        assertEquals("mega", boiereascaBread.subLocation)
    }

    @Test
    fun testMultiUnitParsing_kg_litru_bucati_set() {
        val apples = ShoppingParser.parseSingleItem("2 kg mere")
        assertEquals("Mere", apples.name)
        assertEquals("2 kg", apples.quantity)
        assertEquals(2.0, apples.weightKg, 0.01)

        val napkins = ShoppingParser.parseSingleItem("1 set servetele mega")
        assertEquals("Șervețele", napkins.name)
        assertEquals("1 set", napkins.quantity)
        assertEquals("mega", napkins.subLocation)
    }

    @Test
    fun testVolumeInLiters_andPriceEstimation() {
        val milk = ShoppingParser.parseSingleItem("1 litru lapte")
        assertEquals(1.0, milk.volumeLiters, 0.1) // 1L volume
        assertEquals(4.0, milk.estimatedPriceRon, 0.01) // 4 lei per milk

        val bread = ShoppingParser.parseSingleItem("paine")
        assertEquals(13.0, bread.estimatedPriceRon, 0.01) // 13 lei Panemar artisan bread

        val potatoes = ShoppingParser.parseSingleItem("1 kg cartofi")
        assertEquals(5.0, potatoes.estimatedPriceRon, 0.01) // 5 lei / kg potatoes

        val paper = ShoppingParser.parseSingleItem("1 pachet hartie igienica mega")
        assertEquals(4.0, paper.volumeLiters, 0.1)
        assertEquals(18.0, paper.estimatedPriceRon, 0.01)
    }

    @Test
    fun testImportance3Levels() {
        val normalItem = ShoppingParser.parseSingleItem("lapte")
        assertEquals(Importance.NORMAL, normalItem.importance)

        val moderatItem = ShoppingParser.parseSingleItem("oua moderat")
        assertEquals(Importance.MODERAT, moderatItem.importance)

        val urgentItem = ShoppingParser.parseSingleItem("paine urgent")
        assertEquals(Importance.IMPORTANT, urgentItem.importance)
    }

    @Test
    fun testRouteOptimizer_8kg_10L_limits() {
        // Backpack max: 8kg weight, 10L volume
        val items = listOf(
            ShoppingItem(id = 1, name = "Apă", weightKg = 6.0, volumeLiters = 6.0, zone = Zone.DIANA, subLocation = "mega", importance = Importance.NORMAL),
            ShoppingItem(id = 2, name = "Hârtie", weightKg = 0.8, volumeLiters = 4.0, zone = Zone.DIANA, subLocation = "mega", importance = Importance.NORMAL),
            ShoppingItem(id = 3, name = "Lapte", weightKg = 2.0, volumeLiters = 2.0, zone = Zone.DIANA, subLocation = "mega", importance = Importance.NORMAL)
        )

        val result = RouteOptimizer.optimizeTrips(items)
        // Item 1 (6kg, 6L) + Item 2 (0.8kg, 4L) = 6.8kg, 10L (fits in Trip 1)
        // Item 3 (2kg, 2L) exceeds 10L (10 + 2 = 12L > 10L), so starts Trip 2!
        assertEquals(2, result.trips.size)
        assertEquals(1, result.trips[0].tripNumber)
        assertEquals(2, result.trips[1].tripNumber)
        assertTrue(result.trips[0].totalWeightKg <= 8.0)
        assertTrue(result.trips[0].totalVolumeLiters <= 10.0)
    }
}
