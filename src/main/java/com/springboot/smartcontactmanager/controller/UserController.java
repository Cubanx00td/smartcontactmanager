package com.springboot.smartcontactmanager.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.springboot.smartcontactmanager.dao.ContactRepository;
import com.springboot.smartcontactmanager.dao.UserRepository;
import com.springboot.smartcontactmanager.entities.Contact;
import com.springboot.smartcontactmanager.entities.User;
import com.springboot.smartcontactmanager.helper.Message;

import org.springframework.data.domain.Pageable;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	// method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("USERNAME " + userName);
		
		User user = this.userRepository.getUserByUserName(userName);
		System.out.println("USER " + user);
		
		model.addAttribute("user", user);
	}
	
	// user dash board
	@GetMapping("/index")
	public String dashboard(Model model) {
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}
	
	//open add form 
	@GetMapping("/add-contact")
	public String openAddContact(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}
	
	//processing contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute("contact") Contact contact, @RequestParam("profileImage") MultipartFile file, Principal principal, RedirectAttributes redirectAttributes) {
		try {
//			System.out.println("DATA: " + contact);
			
			String userName = principal.getName();
			User user = this.userRepository.getUserByUserName(userName);
			
			
			//uploading file
			if(file.isEmpty()) {
				System.out.println("nothing found");
				contact.setImagePath("contact.png");
			}
			else {
				contact.setImagePath(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/image").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			
			
			contact.setUser(user);
			user.getContacts().add(contact);
			this.userRepository.save(user);
			System.out.println("Added to database");
			redirectAttributes.addFlashAttribute("message", new Message("Successfully added", "alert-success"));
			return "redirect:/user/add-contact";
		}catch(Exception e) {
			System.out.println("Error " + e.getMessage());
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("message", new Message("Something went wrong!  " + e.getMessage(), "alert-danger"));
			return "redirect:/user/add-contact";
		}
	}
	
	
	//show contacts
	//per page = 5
	//current page = 0
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page")Integer page, Model  model, Principal principal) {
		model.addAttribute("title", "Show Contacts");
		
		//contact list using user Repository
//		String userName = principal.getName();
//		List<Contact> userContacts = this.userRepository.getUserByUserName(userName).getContacts();
		
		
		//contact list using contact Repository
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		Pageable pageable = PageRequest.of(page, 3);
		Page<Contact> userContacts = this.contactRepository.findContactsByUser(user.getId(), pageable);
		
		model.addAttribute("contacts", userContacts);
		model.addAttribute("currentPage", page);
		
		model.addAttribute("totalPages", userContacts.getTotalPages());
		return "normal/show_contacts";
	}
	
	@GetMapping("/contact/{cId}")
	public String showContactDetails(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		System.out.println("cId: " + cId);
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}
		
		return "normal/contact_detail";
	}
	
	//delete contact
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId")Integer cId, Model model, Principal principal, RedirectAttributes redirectAttributes){
		Optional<Contact> contactOptional =  this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		//
		if(contact.getUser().getId() == user.getId()) {
			contact.setUser(null);
			this.contactRepository.delete(contact);
			redirectAttributes.addFlashAttribute("message", new Message("Contact deleted successfully...", "alert-danger"));
		}
		
		/* alternate way
		 * orphan removal = true in Contact
		 * user.getContacts().remove(contact);
		 * override equals method in Contact
		 */
		return "redirect:/user/show-contacts/0";	
	}
	
	//open update form
	@RequestMapping("/update/{cId}")
	public String updateContact(@PathVariable("cId")Integer cId, Model model) {
		model.addAttribute("title", "Update Contact");
		Contact contact = this.contactRepository.findById(cId).get();
		model.addAttribute("contact", contact);
		return "normal/update_form";
	}
	
	//updating contact
	@RequestMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file, Model model, Principal principal, RedirectAttributes redirectAttributes) {
		try {
			
			//old contact details
			Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
			if(!file.isEmpty()) {
				//delete old photo
				File deleteFile = new ClassPathResource("static/image").getFile();
				File file1 = new File(deleteFile, oldContactDetail.getImagePath());
				file1.delete();
				//update new photo
				contact.setImagePath(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/image").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			else {
				contact.setImagePath(oldContactDetail.getImagePath());
			}
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			redirectAttributes.addFlashAttribute("message", new Message("Your contact is updated", "alert-success"));
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("Contact Name: " + contact.getName());
		System.out.println("Contact Id: " + contact.getcId());
		return "redirect:/user/update/" + contact.getcId();
	}
	
	//user home
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title", "Profile page");
		return "normal/profile";
	}

	// user settings
	@GetMapping("/settings")
	public String openSettings(Model model){
		model.addAttribute("title", "Settings");
		return "normal/settings";
	}

	// open change password form
	@GetMapping("/change-password")
	public String openChangePassword(Model model){
		model.addAttribute("title", "Change Your Password");
		return "normal/change_password";
	}

	// process change password
	@PostMapping("/process-change-password")
	public String changePassword(@RequestParam ("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword, RedirectAttributes redirectAttributes, Principal principal){
		System.out.println("Old Password: " + oldPassword);
		System.out.println("New Password: " + newPassword);

		String userName = principal.getName();
		User currUser = this.userRepository.getUserByUserName(userName);
		System.out.println(currUser.getPassword());


		if(this.bCryptPasswordEncoder.matches(oldPassword, currUser.getPassword())){
			//change the password
			currUser.setPassword(bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currUser);
			redirectAttributes.addFlashAttribute("message", new Message("Your password is updated", "alert-success"));
		}
		else{
			redirectAttributes.addFlashAttribute("message", new Message("Your old password is not correct. \n Password not changed", "alert-danger"));
			return "redirect:/user/change-password";
		}



		return "redirect:/user/index";
	}
}
