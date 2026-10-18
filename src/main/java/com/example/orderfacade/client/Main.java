package com.example.orderfacade.client;

import com.example.orderfacade.config.ShopFactory;
import com.example.orderfacade.facade.OrderFacade;
import com.example.orderfacade.facade.OrderProcessingException;
import com.example.orderfacade.model.Address;
import com.example.orderfacade.model.Cart;
import com.example.orderfacade.model.Customer;
import com.example.orderfacade.model.Order;

public class Main {

    public static void main(String[] args) {
        OrderFacade shop = ShopFactory.createDemoOrderFacade();

        Customer alice = new Customer("C-100", "Alice Johnson", "alice@example.com",
                new Address("12 Rose Street", "Berlin", "Germany"));
        Customer bob = new Customer("C-200", "Bob Miller", "bob@example.com",
                new Address("4 Oak Avenue", "Paris", "France"));
        Customer carol = new Customer("C-300", "Carol Smith", "carol@example.com",
                new Address("", "Warsaw", "Poland"));
        Customer dave = new Customer("C-400", "Dave Brown", "dave@example.com",
                new Address("8 River Road", "Sao Paulo", "Brazil"));

        title("1. Successful order");
        Order first = shop.placeOrder(new Cart().add("LAPTOP-15", 1).add("HEADPHONES", 2), alice);
        System.out.println("Placed: " + first);
        System.out.println("Laptops left in stock: " + shop.getAvailableStock("LAPTOP-15"));

        title("2. Not enough stock");
        tryToPlace(shop, new Cart().add("KEYBOARD", 3), alice);
        System.out.println("Keyboards in stock: " + shop.getAvailableStock("KEYBOARD"));

        title("3. Payment declined (stock is released again)");
        tryToPlace(shop, new Cart().add("PHONE-X", 1), bob);
        System.out.println("Phones in stock: " + shop.getAvailableStock("PHONE-X"));

        title("4. Shipping fails after payment (payment is refunded, stock is released)");
        tryToPlace(shop, new Cart().add("PHONE-X", 1), carol);
        System.out.println("Phones in stock: " + shop.getAvailableStock("PHONE-X"));

        title("5. Destination we do not ship to");
        tryToPlace(shop, new Cart().add("HEADPHONES", 1), dave);

        title("6. Cancel the first order");
        Order cancelled = shop.cancelOrder(first.getId());
        System.out.println("Cancelled: " + cancelled);
        System.out.println("Laptops left in stock: " + shop.getAvailableStock("LAPTOP-15"));

        title("7. Cancel it again");
        try {
            shop.cancelOrder(first.getId());
        } catch (OrderProcessingException e) {
            System.out.println("Refused [" + e.getReason() + "]: " + e.getMessage());
        }
    }

    private static void tryToPlace(OrderFacade shop, Cart cart, Customer customer) {
        try {
            System.out.println("Placed: " + shop.placeOrder(cart, customer));
        } catch (OrderProcessingException e) {
            System.out.println("Rejected [" + e.getReason() + "]: " + e.getMessage());
        }
    }

    private static void title(String text) {
        System.out.println();
        System.out.println("--- " + text + " ---");
    }
}
