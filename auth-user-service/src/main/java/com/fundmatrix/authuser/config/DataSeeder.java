package com.fundmatrix.authuser.config;

import org.hibernate.annotations.Comment;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fundmatrix.authuser.domain.User;
import com.fundmatrix.authuser.domain.enums.Role;
import com.fundmatrix.authuser.domain.enums.UserStatus;
import com.fundmatrix.authuser.repository.UserRepository;


@Component
public class DataSeeder implements ApplicationRunner {
	 private static final String DEMO_PASSWORD = "Password@123";
	 
	 private final PasswordEncoder passwordEncoder;
	 
	 private final UserRepository userRepository;
	 
	 
	public DataSeeder(PasswordEncoder passwordEncoder, UserRepository userRepository) {
		super();
		this.passwordEncoder = passwordEncoder;
		this.userRepository = userRepository;
	}


	@Override
	public void run(ApplicationArguments args) throws Exception {
		// TODO Auto-generated method stub
		
		 if (userRepository.count() > 0) {
	           
	            return;
		 }

	        // ---- Staff & admin users ----
	        user("System Administrator", "admin@fundmatrix.io", "9000000001", Role.ADMIN);
	        user("Olivia Operations", "ops@fundmatrix.io", "9000000002", Role.FUND_OPS);
	        user("Aakash Accountant", "accountant@fundmatrix.io", "9000000003", Role.FUND_ACCOUNTANT);
	        user("Chitra Compliance", "compliance@fundmatrix.io", "9000000004", Role.COMPLIANCE);

	        // ---- Distributors + their login users ----
	        User arnoldUser = user("Arnold (WealthBridge)", "arnold@wealthbridge.io", "9100000001", Role.DISTRIBUTOR);
	        User meeraUser = user("Meera (FinSmart)", "meera@finsmart.io", "9100000002", Role.DISTRIBUTOR);
	      

	        // ---- Investors ----
	        User ravi = user("Ravi Kumar", "ravi@example.com", "9200000001", Role.INVESTOR);
	        User priya = user("Priya Sharma", "priya@example.com", "9200000002", Role.INVESTOR);
	        User sanjay = user("Sanjay Mehta", "sanjay@example.com", "9200000003", Role.INVESTOR);
		
	}
	
	
	 private User user(String name, String email, String phone, Role role) {
	        return userRepository.save(User.builder()
	                .name(name).email(email).phone(phone).role(role).status(UserStatus.ACTIVE)
	                .password(passwordEncoder.encode(DEMO_PASSWORD)).build());
	    }

}
