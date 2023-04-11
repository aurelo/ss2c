package com.kanezi.springsocial2cloud.security;

import com.kanezi.springsocial2cloud.fomanticUI.Toast;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Value
@Log4j2
public class AppUserController2 {

    AppUserService appUserService;

    @GetMapping("/user/auth")
    public String showUserRAuthorities(@AuthenticationPrincipal AppUser appUser, Model model) {
        model.addAttribute("appUser", appUser);
        return "app-user/auth";
    }

    @DeleteMapping("/user/auth/{authority}")
    public String deleteAuthority(@PathVariable String authority,@AuthenticationPrincipal AppUser appUser, Model model, RedirectAttributes attributes) {
        log.info("authority: {}", authority);
        model.addAttribute("appUser", appUser);
        attributes.addFlashAttribute("toast", Toast.success("Auth", authority));

        appUserService.removeAuthority(appUser, authority);

        return "redirect:/user/auth";
        //return "app-user/auth";
    }

}
