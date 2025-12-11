package com.suitespot.controller;

import com.suitespot.entity.User;
import com.suitespot.entity.Booking;
import com.suitespot.entity.SystemSettings;
import com.suitespot.entity.Bill;
import com.suitespot.service.UserService;
import com.suitespot.service.BookingService;
import com.suitespot.service.RoomService;
import com.suitespot.service.SystemSettingsService;
import com.suitespot.repository.BillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private SystemSettingsService systemSettingsService;

    @Autowired
    private BillRepository billRepository;

    // Dashboard
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        try {
            List<Booking> totalBookings = bookingService.getAllBookings();
            List<User> totalUsers = userService.getAllUsers();
            List<Booking> pendingBookings = bookingService.getBookingsByStatus(Booking.BookingStatus.PENDING);

            model.addAttribute("totalBookings", totalBookings != null ? totalBookings.size() : 0);
            model.addAttribute("totalUsers", totalUsers != null ? totalUsers.size() : 0);
            model.addAttribute("pendingBookings", pendingBookings != null ? pendingBookings.size() : 0);
            model.addAttribute("totalRooms", roomService.getAllRooms() != null ? roomService.getAllRooms().size() : 0);
            model.addAttribute("recentBookings", totalBookings != null ? totalBookings.stream().limit(5).toList() : List.of());

            return "admin/dashboard";
        } catch (Exception e) {
            // Fallback values if there's an error
            model.addAttribute("totalBookings", 0);
            model.addAttribute("totalUsers", 0);
            model.addAttribute("pendingBookings", 0);
            model.addAttribute("totalRooms", 0);
            model.addAttribute("recentBookings", List.of());
            return "admin/dashboard";
        }
    }

    // User Management
    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users/list";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("roles", User.Role.values());
        return "admin/users/form";
    }

    @PostMapping("/users")
    public String createUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String fullName,
            @RequestParam String password,
            @RequestParam User.Role role,
            RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(username, email, fullName, password, role);
            redirectAttributes.addFlashAttribute("success", "User created successfully!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create user: " + e.getMessage());
            return "redirect:/admin/users/new";
        }
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        model.addAttribute("roles", User.Role.values());
        return "admin/users/form";
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User userDetails, RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, userDetails);
            redirectAttributes.addFlashAttribute("success", "User updated successfully!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update user");
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete user");
            return "redirect:/admin/users";
        }
    }

    // System Settings
    @GetMapping("/settings")
    public String settingsPage(Model model) {
        model.addAttribute("settings", systemSettingsService.getSettings());
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String updateSettings(@ModelAttribute SystemSettings settings, RedirectAttributes redirectAttributes) {
        try {
            systemSettingsService.updateSettings(settings);
            redirectAttributes.addFlashAttribute("success", "Settings updated successfully!");
            return "redirect:/admin/settings";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update settings");
            return "redirect:/admin/settings";
        }
    }

    @PostMapping("/settings/reset")
    public String resetSettings(RedirectAttributes redirectAttributes) {
        try {
            systemSettingsService.resetToDefaults();
            redirectAttributes.addFlashAttribute("success", "Settings reset to defaults!");
            return "redirect:/admin/settings";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reset settings");
            return "redirect:/admin/settings";
        }
    }

    // Database Backup
    @Autowired
    private com.suitespot.service.DatabaseBackupService databaseBackupService;

    @GetMapping("/backup")
    public String backupPage(Model model) {
        // Default backup location
        String defaultBackupLocation = System.getProperty("user.home") + File.separator + "suite_spot_backups";
        model.addAttribute("backupLocation", defaultBackupLocation);
        
        // List existing backups and convert to safe format
        try {
            File[] backupFiles = databaseBackupService.listBackups(defaultBackupLocation);
            if (backupFiles != null && backupFiles.length > 0) {
                List<java.util.Map<String, Object>> backups = new java.util.ArrayList<>();
                for (File file : backupFiles) {
                    java.util.Map<String, Object> backupInfo = new java.util.HashMap<>();
                    backupInfo.put("name", file.getName());
                    backupInfo.put("size", file.length());
                    backupInfo.put("path", file.getAbsolutePath());
                    backupInfo.put("lastModified", java.nio.file.Files.getLastModifiedTime(
                        java.nio.file.Paths.get(file.getAbsolutePath())).toInstant());
                    backups.add(backupInfo);
                }
                model.addAttribute("backups", backups);
            } else {
                model.addAttribute("backups", new java.util.ArrayList<>());
            }
        } catch (Exception e) {
            model.addAttribute("backups", new java.util.ArrayList<>());
        }
        
        return "admin/backup";
    }

    @PostMapping("/backup/create")
    public String createBackup(
            @RequestParam String backupLocation,
            @RequestParam(defaultValue = "full") String backupType,
            RedirectAttributes redirectAttributes) {
        try {
            String backupPath = databaseBackupService.createBackup(backupLocation, backupType);
            
            // Verify backup
            boolean isValid = databaseBackupService.verifyBackup(backupPath);
            if (isValid) {
                redirectAttributes.addFlashAttribute("success", 
                    "Backup created successfully at: " + backupPath);
            } else {
                redirectAttributes.addFlashAttribute("error", 
                    "Backup created but verification failed. Please check manually.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Failed to create backup: " + e.getMessage());
        }
        return "redirect:/admin/backup";
    }

    // Reports
    @GetMapping("/reports")
    public String reportsPage(Model model) {
        try {
            // Get statistics for reports
            List<Booking> allBookings = bookingService.getAllBookings();
            List<Booking> confirmedBookings = bookingService.getBookingsByStatus(Booking.BookingStatus.CONFIRMED);
            List<Booking> checkedInBookings = bookingService.getBookingsByStatus(Booking.BookingStatus.CHECKED_IN);
            List<Booking> checkedOutBookings = bookingService.getBookingsByStatus(Booking.BookingStatus.CHECKED_OUT);
            List<Booking> cancelledBookings = bookingService.getBookingsByStatus(Booking.BookingStatus.CANCELLED);
            
            // Calculate total revenue from all bills
            List<Bill> allBills = billRepository.findAll();
            BigDecimal totalRevenue = allBills.stream()
                    .filter(bill -> bill.getTotalAmount() != null)
                    .map(Bill::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            model.addAttribute("totalBookings", allBookings != null ? allBookings.size() : 0);
            model.addAttribute("confirmedBookings", confirmedBookings != null ? confirmedBookings.size() : 0);
            model.addAttribute("checkedInBookings", checkedInBookings != null ? checkedInBookings.size() : 0);
            model.addAttribute("checkedOutBookings", checkedOutBookings != null ? checkedOutBookings.size() : 0);
            model.addAttribute("cancelledBookings", cancelledBookings != null ? cancelledBookings.size() : 0);
            model.addAttribute("totalUsers", userService.getAllUsers() != null ? userService.getAllUsers().size() : 0);
            model.addAttribute("totalRooms", roomService.getAllRooms() != null ? roomService.getAllRooms().size() : 0);
            model.addAttribute("totalRevenue", totalRevenue);
            
            return "admin/reports";
        } catch (Exception e) {
            // Fallback values if there's an error
            model.addAttribute("totalBookings", 0);
            model.addAttribute("confirmedBookings", 0);
            model.addAttribute("checkedInBookings", 0);
            model.addAttribute("checkedOutBookings", 0);
            model.addAttribute("cancelledBookings", 0);
            model.addAttribute("totalUsers", 0);
            model.addAttribute("totalRooms", 0);
            model.addAttribute("totalRevenue", BigDecimal.ZERO);
            model.addAttribute("error", "Error loading reports: " + e.getMessage());
            return "admin/reports";
        }
    }
}
