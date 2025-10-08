package com.megamart.useradminserver.config;

import com.megamart.useradminserver.entity.Admin;
import com.megamart.useradminserver.entity.User;
import com.megamart.useradminserver.repository.AdminRepository;
import com.megamart.useradminserver.repository.UserRepository;
import com.megamart.useradminserver.client.AuthServiceClient;
import com.megamart.useradminserver.dto.SyncUserRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String SUPER_ADMIN_EMAIL = "superadmin@megamart.com";
    private static final String ADMIN_EMAIL = "admin@megamart.com";
    private static final String ORDER_MANAGER_EMAIL = "ordermanager@megamart.com";
    private static final String CUSTOMER_MANAGER_EMAIL = "customermanager@megamart.com";
    private static final String PRODUCT_MANAGER_EMAIL = "productmanager@megamart.com";
    private static final String CUSTOMER_EMAIL = "customer@megamart.com";
    private static final String ADMIN_PASSWORD = "admin123";
    
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthServiceClient authServiceClient;

    @Override
    public void run(String... args) throws Exception {
        // Create default super admin if not exists
        if (!adminRepository.existsByEmail(SUPER_ADMIN_EMAIL)) {
            Admin superAdmin = new Admin();
            superAdmin.setName("Super Admin");
            superAdmin.setEmail(SUPER_ADMIN_EMAIL);
            String hashedPassword = passwordEncoder.encode(ADMIN_PASSWORD);
            superAdmin.setPassword(hashedPassword);
            superAdmin.setRole(Admin.AdminRole.super_admin);
            superAdmin.setPermissions(Arrays.asList(Admin.AdminPermission.values()));
            
            adminRepository.save(superAdmin);
            syncWithAuthService(SUPER_ADMIN_EMAIL, hashedPassword, "super_admin");
            log.info("Default super admin created: {} / {}", SUPER_ADMIN_EMAIL, ADMIN_PASSWORD);
        }

        // Create default admin if not exists
        if (!adminRepository.existsByEmail(ADMIN_EMAIL)) {
            Admin admin = new Admin();
            admin.setName("Admin User");
            admin.setEmail(ADMIN_EMAIL);
            String hashedPassword = passwordEncoder.encode(ADMIN_PASSWORD);
            admin.setPassword(hashedPassword);
            admin.setRole(Admin.AdminRole.admin);
            admin.setPermissions(Arrays.asList(
                Admin.AdminPermission.manage_products,
                Admin.AdminPermission.manage_orders,
                Admin.AdminPermission.manage_customers,
                Admin.AdminPermission.view_analytics
            ));
            
            adminRepository.save(admin);
            syncWithAuthService(ADMIN_EMAIL, hashedPassword, "admin");
            log.info("Default admin created: {} / {}", ADMIN_EMAIL, ADMIN_PASSWORD);
        }

        // Create order manager if not exists
        if (!adminRepository.existsByEmail(ORDER_MANAGER_EMAIL)) {
            Admin orderManager = new Admin();
            orderManager.setName("Order Manager");
            orderManager.setEmail(ORDER_MANAGER_EMAIL);
            String hashedPassword = passwordEncoder.encode(ADMIN_PASSWORD);
            orderManager.setPassword(hashedPassword);
            orderManager.setRole(Admin.AdminRole.order_manager);
            orderManager.setPermissions(Arrays.asList(
                Admin.AdminPermission.manage_orders,
                Admin.AdminPermission.view_analytics
            ));
            
            adminRepository.save(orderManager);
            syncWithAuthService(ORDER_MANAGER_EMAIL, hashedPassword, "order_manager");
            log.info("Order manager created: {} / {}", ORDER_MANAGER_EMAIL, ADMIN_PASSWORD);
        }

        // Create customer manager if not exists
        if (!adminRepository.existsByEmail(CUSTOMER_MANAGER_EMAIL)) {
            Admin customerManager = new Admin();
            customerManager.setName("Customer Manager");
            customerManager.setEmail(CUSTOMER_MANAGER_EMAIL);
            String hashedPassword = passwordEncoder.encode(ADMIN_PASSWORD);
            customerManager.setPassword(hashedPassword);
            customerManager.setRole(Admin.AdminRole.customer_manager);
            customerManager.setPermissions(Arrays.asList(
                Admin.AdminPermission.manage_customers,
                Admin.AdminPermission.view_analytics
            ));
            
            adminRepository.save(customerManager);
            syncWithAuthService(CUSTOMER_MANAGER_EMAIL, hashedPassword, "customer_manager");
            log.info("Customer manager created: {} / {}", CUSTOMER_MANAGER_EMAIL, ADMIN_PASSWORD);
        }

        // Create product manager if not exists
        if (!adminRepository.existsByEmail(PRODUCT_MANAGER_EMAIL)) {
            Admin productManager = new Admin();
            productManager.setName("Product Manager");
            productManager.setEmail(PRODUCT_MANAGER_EMAIL);
            String hashedPassword = passwordEncoder.encode(ADMIN_PASSWORD);
            productManager.setPassword(hashedPassword);
            productManager.setRole(Admin.AdminRole.product_manager);
            productManager.setPermissions(Arrays.asList(
                Admin.AdminPermission.manage_products,
                Admin.AdminPermission.view_analytics
            ));
            
            adminRepository.save(productManager);
            syncWithAuthService(PRODUCT_MANAGER_EMAIL, hashedPassword, "product_manager");
            log.info("Product manager created: {} / {}", PRODUCT_MANAGER_EMAIL, ADMIN_PASSWORD);
        }

        // Create test customer if not exists
        if (!userRepository.existsByEmail(CUSTOMER_EMAIL)) {
            User customer = new User();
            customer.setName("Test Customer");
            customer.setUsername("testcustomer");
            customer.setEmail(CUSTOMER_EMAIL);
            String hashedPassword = passwordEncoder.encode("Customer123!");
            customer.setPassword(hashedPassword);
            customer.setRole(User.Role.customer);
            
            userRepository.save(customer);
            syncWithAuthService(CUSTOMER_EMAIL, hashedPassword, "customer");
            log.info("Test customer created: {} / Customer123!", CUSTOMER_EMAIL);
        }
    }

    private void syncWithAuthService(String email, String hashedPassword, String role) {
        try {
            SyncUserRequestDto request = new SyncUserRequestDto(email, hashedPassword, role);
            authServiceClient.syncUser(request);
            log.info("Synced with auth service: {} as {}", email, role);
        } catch (Exception e) {
            log.error("Failed to sync {} with auth service: {}", email, e.getMessage());
        }
    }
}