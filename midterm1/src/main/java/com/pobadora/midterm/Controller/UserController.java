package com.pobadora.midterm.Controller;

import com.pobadora.midterm.Service.GoogleContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
public class UserController {
    @Autowired
    GoogleContactService gserv;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("message", "Welcome, this is the landing page.");
        return "index";
    }

    @GetMapping("/user-info")
    public String googleHome(Model model, @AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User != null) {
            String fullName = oAuth2User.getAttribute("name");
            String email = oAuth2User.getAttribute("email");
            String picture = oAuth2User.getAttribute("picture");

            String[] names = fullName != null ? fullName.split(" ", 2) : new String[]{"", ""};
            String firstName = names[0];
            String lastName = names.length > 1 ? names[1] : "";

            model.addAttribute("fullName", fullName);
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("email", email);
            model.addAttribute("picture", picture);
        }
        return "googleHome";
    }

    @GetMapping("/contacts")
    public String getContacts(Model model, @AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User != null) {
            try {
                Map<String, Object> contacts = gserv.getContacts(oAuth2User);
                List<Map<String, String>> formattedContacts = gserv.formatContacts(contacts);
                model.addAttribute("contacts", formattedContacts);
            } catch (Exception e) {
                model.addAttribute("error", "Failed to fetch contacts: " + e.getMessage());
            }
        }
        return "contacts";
    }

    @PostMapping("/contacts/add")
    public ResponseEntity<?> addContact(@AuthenticationPrincipal OAuth2User oAuth2User,
                                         @RequestParam String name,
                                         @RequestParam String email,
                                         @RequestParam String phone) {
        try {
            gserv.addContact(oAuth2User, name, email, phone);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Contact added successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Failed to add contact: " + e.getMessage()));
        }
    }

    @PostMapping("/contacts/edit")
    public ResponseEntity<?> editContact(@AuthenticationPrincipal OAuth2User oAuth2User,
                                        @RequestParam String resourceName,
                                        @RequestParam String name,
                                        @RequestParam String email,
                                        @RequestParam String phone) {
        try {
            System.out.println("Received edit request:");
            System.out.println("Resource Name: " + resourceName);
            System.out.println("New Name: " + name);
            System.out.println("New Email: " + email);
            System.out.println("New Phone: " + phone);

            gserv.updateContact(oAuth2User, resourceName, name, email, phone);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Contact updated successfully"));
        } catch (Exception e) {
            System.err.println("Failed to update contact: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Failed to update contact: " + e.getMessage()));
        }
    }

    @PostMapping("/contacts/delete")
    public ResponseEntity<?> deleteContact(@AuthenticationPrincipal OAuth2User oAuth2User,
                                            @RequestParam String resourceName) {
        try {
            gserv.deleteContact(oAuth2User, resourceName);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Contact deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Failed to delete contact: " + e.getMessage()));
        }
    }



}
