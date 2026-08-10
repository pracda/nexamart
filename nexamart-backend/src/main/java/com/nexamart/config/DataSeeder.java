package com.nexamart.config;

import com.nexamart.auth.Role;
import com.nexamart.auth.User;
import com.nexamart.auth.UserRepository;
import com.nexamart.catalog.Category;
import com.nexamart.catalog.CategoryRepository;
import com.nexamart.catalog.Product;
import com.nexamart.catalog.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, CategoryRepository categoryRepository,
                       ProductRepository productRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = userRepository.save(new User("admin@nexamart.dev", passwordEncoder.encode("password123"), "Nexa Admin", Role.ADMIN));
        User seller = userRepository.save(new User("seller@nexamart.dev", passwordEncoder.encode("password123"), "Sam Seller", Role.SELLER));
        userRepository.save(new User("buyer@nexamart.dev", passwordEncoder.encode("password123"), "Bailey Buyer", Role.BUYER));

        Category electronics = categoryRepository.save(new Category("Electronics"));
        Category home = categoryRepository.save(new Category("Home & Kitchen"));
        Category fashion = categoryRepository.save(new Category("Fashion"));

        productRepository.save(new Product("Wireless Headphones - Red", "Over-ear wireless headphones with 30hr battery life.",
                new BigDecimal("39.99"), 50, null, electronics, seller));
        productRepository.save(new Product("Wireless Headphones - Black", "Compact wireless earbuds with noise cancellation.",
                new BigDecimal("59.99"), 30, null, electronics, seller));
        productRepository.save(new Product("USB-C Charging Cable", "6ft braided USB-C fast charging cable.",
                new BigDecimal("9.99"), 200, null, electronics, seller));
        productRepository.save(new Product("Stainless Steel Water Bottle", "Insulated 32oz bottle keeps drinks cold 24 hours.",
                new BigDecimal("18.50"), 80, null, home, seller));
        productRepository.save(new Product("Non-Stick Frying Pan 10\"", "Durable non-stick frying pan, oven safe to 450F.",
                new BigDecimal("24.99"), 40, null, home, seller));
        productRepository.save(new Product("Men's Cotton T-Shirt", "Classic fit crew neck t-shirt, 100% cotton.",
                new BigDecimal("14.99"), 150, null, fashion, seller));
        productRepository.save(new Product("Women's Running Shoes", "Lightweight breathable running shoes.",
                new BigDecimal("49.99"), 60, null, fashion, seller));
        productRepository.save(new Product("Smart Watch", "Fitness tracking smart watch with heart-rate monitor.",
                new BigDecimal("89.99"), 25, null, electronics, seller));

        System.out.println("Seed data loaded. Demo accounts (password: password123):");
        System.out.println("  admin@nexamart.dev / seller@nexamart.dev / buyer@nexamart.dev");
    }
}
