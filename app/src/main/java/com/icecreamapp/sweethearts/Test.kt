Quick Service Restaurant

Context

You're building an ordering system for a quick-service restaurant inside a retail store (like Subway in Walmart). Customers can customize menu items, add them to cart, and place an order.

Problem

Implement the cart and pricing system.

data class Topping(val id: String, val name: String, val priceDelta: Int)
data class MenuItem(val sku: String, val name: String, val basePrice: Int, val toppings: List<Topping>)

data class CartLine(val sku: String, val toppings: List<String>)
data class Cart(val lines: MutableList<CartLine>)

Requirements

Price a line item = base price + chosen toppings.

Price the cart = sum of all line totals.

If the same item is ordered twice, apply a BOGO (buy-one-get-one) discount (the cheaper one is free).

Deliverables

Implement:

fun priceLine(menu: Map<String, MenuItem>, line: CartLine): Int {
    var priceInt = 0

    val menuItem = menu.get(line.sku)

    priceInt += menuItem?.basePrice ?: 0

    for (i in 0 .. (line?.toppings?.size - 1)) {
        val sku = line.toppings[i]

        val topping = menuItem?.toppings?.filter { it.id == sku }?.firstOrNull()

        if (topping != null) {
            priceInt += topping?.priceDelta
        }
    }

    return priceInt
}

fun priceCart(menu: Map<String, MenuItem>, cart: Cart): Int {
    var priceCart = 0

    val allItemsCost: MutableList<Int> = mutableListOf()
    var lowestPrice = Int.MAX

    for (i in 0 .. cart.lines.size - 1) {
        priceCart += priceLine(menu, cart.lines[i])

        allItemsCost.add(priceCart)
    }

    var afterDiscountCost = allItemsCost

    priceCart = priceCart - lowestPrice

    return priceCart
}