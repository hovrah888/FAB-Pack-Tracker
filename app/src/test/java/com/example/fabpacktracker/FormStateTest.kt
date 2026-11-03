package com.example.fabpacktracker

import com.example.fabpacktracker.viewmodel.FormState
import org.junit.Test

import org.junit.Assert.*

class FormStateTest {

    // прогон salesCount
    @Test // пройдет, для пустого поля salesCount значение будет 0
    fun `empty sales count means zero`() {
        val form = FormState(salesCount = "")   // подготовка
        val result = form.salesCountValue       // действие
        assertEquals(0, result)                 // проверка
    }

    @Test // пройдет, ведь 0 являеться валидным значением для продаж
    // тут была дыра
    fun `zero sales must be true`(){
        assertEquals(0, FormState(salesCount = "0").salesCountValue)
    }

    @Test // пройдет, ведь вернет null, так как продажи не могут быть отрицательными
    fun `negative sales count is rejected`() {
        assertNull(FormState(salesCount = "-5").salesCountValue)
    }

    @Test // пройдет, ведь значение int
    fun `valid sales count return int`(){
        assertEquals(1, FormState(salesCount = "1").salesCountValue)
    }

    @Test // пройдет, ведь вернет null, так как строка не допустима в поле продажи
    fun `invalid text(str) in sales count returns null`(){
        assertNull(FormState(salesCount = "aaa").salesCountValue)
    }

    @Test
    fun `double sales count is rejected`() {
        assertNull(FormState(salesCount = "1.5").salesCountValue)
    }

    @Test
    fun `sales count with spaces is rejected`(){
        assertEquals(12, FormState(salesCount = " 12 ").salesCountValue)
    }

    // прогон price

    @Test
    fun `zero price is valid`(){ // c 0 он упадет, ты был прав
        assertEquals(0.0, FormState(price = "0").priceValue)
    }

    @Test
    fun `empty price must be zero`(){ // уже не падает, я обновил priceValue
        assertEquals(0.0, FormState(price = "").priceValue)
    }

    @Test // пройдет, ведь значение с точкой преобразуеться в значение с точкой
    fun `price with dot is valid`(){
        val form = FormState(price = "19.99")
        assertNotNull(form.priceValue) // после этого priceValue!! должен быть безопасен
        assertEquals(19.99, form.priceValue!!, 0.001)
    }

    @Test // пройдет, ведь значение с запятой преобразуеться в значение с точкой
    fun `price with comma is valid`(){
        val form = FormState(price = "19,99")
        assertNotNull(form.priceValue)
        assertEquals(19.99, form.priceValue!!, 0.001)
    }

    @Test // пройдет, ведь вернет null, так как цена не может быть отрицательной
    fun `negative price is rejected`(){
        assertNull(FormState(price = "-15.0").priceValue)
    }

    @Test // пройдет, ведь вернет null, так как цена на может быть строкой
    fun `invalid text(str) in price returns null`(){
        assertNull(FormState(price = "aaa").priceValue)
    }

    @Test
    fun `price with spaces is rejected`(){
        assertEquals(12.0, FormState(price= " 12 ").priceValue)
    }

    // isValid

    @Test // пройдет, форма валидна полностью
    fun `valid form returns true`() {
        val form = FormState(
            name = "Pack",
            price = "10.0",
            salesCount = "5"
        )
        assertTrue(form.isValid)
    }

    @Test // пройдет, в форме есть ошибка - пустое поле имя
    fun `blank name returns false`() {
        val form = FormState(
            name = "   ",
            price = "10.0",
            salesCount = "5"
        )
        assertFalse(form.isValid)
    }

    @Test // пройдет, в форме есть ошибка в фоле цены
    fun `invalid price returns false`() {
        val form = FormState(
            name = "Awesome Pack",
            price = "invalid_price",
            salesCount = "5"
        )
        assertFalse(form.isValid)
    }

    @Test
    fun `price with comma returns true`() {
        val form = FormState(
            name = "Awesome Pack",
            price = "12,5",
            salesCount = "5"
        )
        assertTrue(form.isValid)
    }

    @Test // пройдет, в форме есть ошибка в фоле цены
    fun `invalid sales returns false`() {
        val form = FormState(
            name = "Awesome Pack",
            price = "1.0",
            salesCount = "aaa"
        )
        assertFalse(form.isValid)
    }

    @Test // пройдет, ведь цену и продажи уже можно оставлять пустыми
    fun `only name in form is allowed`() {
        val form = FormState(
            name = "Awesome Pack",
        )
        assertTrue(form.isValid)
    }

}