package com.suitespot.controller;

import com.suitespot.entity.Guest;
import com.suitespot.service.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/guests")
public class GuestController {

    @Autowired
    private GuestService guestService;

    @GetMapping
    public String listGuests(@RequestParam(required = false) String search, Model model) {
        if (search != null && !search.isEmpty()) {
            model.addAttribute("guests", guestService.searchGuests(search));
            model.addAttribute("search", search);
        } else {
            model.addAttribute("guests", guestService.getAllGuests());
        }
        return "guests/list";
    }

    @GetMapping("/{id}")
    public String viewGuest(@PathVariable Long id, Model model) {
        Guest guest = guestService.getGuestById(id)
                .orElseThrow(() -> new RuntimeException("Guest not found"));
        model.addAttribute("guest", guest);
        return "guests/view";
    }

    @GetMapping("/new")
    public String newGuestForm(Model model) {
        model.addAttribute("guest", new Guest());
        return "guests/form";
    }

    @PostMapping
    public String saveGuest(@ModelAttribute Guest guest) {
        if (guest.getId() == null) {
            guestService.createGuest(guest);
        } else {
            guestService.updateGuest(guest.getId(), guest);
        }
        return "redirect:/guests";
    }

    @GetMapping("/{id}/edit")
    public String editGuestForm(@PathVariable Long id, Model model) {
        Guest guest = guestService.getGuestById(id)
                .orElseThrow(() -> new RuntimeException("Guest not found"));
        model.addAttribute("guest", guest);
        return "guests/form";
    }

    @PostMapping("/{id}")
    public String updateGuest(@PathVariable Long id, @ModelAttribute Guest guestDetails) {
        // Ensure the ID is set from path variable
        guestDetails.setId(id);
        guestService.updateGuest(id, guestDetails);
        return "redirect:/guests/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteGuest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
        guestService.deleteGuest(id);
            redirectAttributes.addFlashAttribute("success", "Guest deleted successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/guests";
    }
}
