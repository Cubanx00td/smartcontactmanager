package com.springboot.smartcontactmanager.controller;


import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.springboot.smartcontactmanager.dao.UserRepository;
import com.springboot.smartcontactmanager.entities.User;
import com.springboot.smartcontactmanager.helper.Message;

@Controller
public class HomeController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("title", "Home - Smart Contact Manager");
		return "home";
	}
	
	@GetMapping("/about")
	public String about(Model model) {
		model.addAttribute("title", "About");
		return "about";
	}
	
	@GetMapping("/signup")
	public String signup(Model model) {
		model.addAttribute("title", "Register");
		model.addAttribute("user", new User());
		return "signup";
	}
	
	@PostMapping("/do_register")
	public String registerUser(@Valid@ModelAttribute("user") User user, BindingResult bindingResult, @RequestParam(value="agreement", defaultValue="false") boolean agreement, Model model, RedirectAttributes redirectAttributes) {
		try {
			if(!agreement) {
				System.out.println("You have not agreed to the terms and conditions");
				throw new Exception("You have not agreed to the terms and conditions");
			}
			if(bindingResult.hasErrors()) {
				System.out.println("ERROR" + bindingResult.toString());
				model.addAttribute("user", user);
				return "signup";
			}
			
			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setImageUrl("default.png");
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			
			System.out.println("Agreement " + agreement);
			System.out.println(user);
			
			this.userRepository.save(user);
			model.addAttribute("user", new User());
			redirectAttributes.addFlashAttribute("message", new Message("Successfully Registered", "alert-success"));
			return "redirect:/signup";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			model.addAttribute("user", user);
			redirectAttributes.addFlashAttribute("message", new Message("Something went wrong!  " + e.getMessage(), "alert-danger"));

			return "redirect:/signup";
		}
		
	}
	
	@GetMapping("/signin")
	public String customLogin(Model model) {
		return "signin";
	}
}
