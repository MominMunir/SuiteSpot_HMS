package com.suitespot.controller;

import com.suitespot.entity.Room;
import com.suitespot.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @GetMapping
    public String listRooms(@RequestParam(required = false) String type, Model model) {
        if (type != null && !type.isEmpty()) {
            try {
                model.addAttribute("rooms", roomService.getRoomsByType(Room.RoomType.valueOf(type)));
                model.addAttribute("selectedType", type);
            } catch (IllegalArgumentException e) {
                model.addAttribute("rooms", roomService.getAllRooms());
            }
        } else {
            model.addAttribute("rooms", roomService.getAllRooms());
        }
        model.addAttribute("roomTypes", Room.RoomType.values());
        model.addAttribute("roomStatuses", Room.RoomStatus.values());
        return "rooms/list";
    }

    @GetMapping("/{id}")
    public String viewRoom(@PathVariable Long id, Model model) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        model.addAttribute("room", room);
        return "rooms/view";
    }

    @GetMapping("/new")
    public String newRoomForm(Model model) {
        model.addAttribute("room", new Room());
        model.addAttribute("roomTypes", Room.RoomType.values());
        model.addAttribute("roomStatuses", Room.RoomStatus.values());
        return "rooms/form";
    }

    @PostMapping
    public String saveRoom(@ModelAttribute Room room) {
        if (room.getId() == null) {
            roomService.createRoom(room);
        } else {
            roomService.updateRoom(room.getId(), room);
        }
        return "redirect:/rooms";
    }

    @GetMapping("/{id}/edit")
    public String editRoomForm(@PathVariable Long id, Model model) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        model.addAttribute("room", room);
        model.addAttribute("roomTypes", Room.RoomType.values());
        model.addAttribute("roomStatuses", Room.RoomStatus.values());
        return "rooms/form";
    }

    @PostMapping("/{id}")
    public String updateRoom(@PathVariable Long id, @ModelAttribute Room roomDetails) {
        // Ensure the ID is set from path variable
        roomDetails.setId(id);
        roomService.updateRoom(id, roomDetails);
        return "redirect:/rooms/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return "redirect:/rooms";
    }

    @PostMapping("/{id}/status")
    public String updateRoomStatus(@PathVariable Long id, @RequestParam Room.RoomStatus status) {
        roomService.updateRoomStatus(id, status);
        return "redirect:/rooms/" + id;
    }
}
